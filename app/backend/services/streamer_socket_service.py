"""Legacy streamer socket adapter with safe fallback semantics."""

from __future__ import annotations

import logging
import socket
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass

from backend.config import config
from backend.models import CiMenuItem, Service, ServiceStatus, StreamerStatus, TunerStatus

logger = logging.getLogger(__name__)

_SOCKET_RETRY_BACKOFF_SECONDS = 2.0
_INTERFACE_TYPE_ALIASES = {
    "ip": "dvbudp",
    "istr": "infostreamer",
}


@dataclass(slots=True)
class _SocketCommandResult:
    available: bool
    success: bool
    payload: str = ""


class StreamerSocketService:
    """Access the legacy streamer socket on 127.0.0.1:8100 when available."""

    def __init__(self) -> None:
        self._retry_after = 0.0

    def get_interface_status(self, position: str) -> str | None:
        result = self._execute(f"interface/{position}/status get")
        if not result.available or not result.success:
            return None
        return result.payload.strip()

    def get_interface_log(self, position: str) -> str | None:
        result = self._execute(
            f"interface/{position}/log get",
            preserve_newlines=True,
        )
        if not result.available or not result.success:
            return None
        return result.payload.rstrip("\n")

    def run_interface_command(self, position: str, command: str) -> bool | None:
        result = self._execute(f"interface/{position}/command {command}")
        if not result.available:
            return None
        return result.success

    def start_scan(self, position: str) -> bool | None:
        stop_result = self._execute(f"interface/{position}/command stop")
        if not stop_result.available:
            return None
        if not stop_result.success:
            return False

        scan_result = self._execute(f"interface/{position}/command scan")
        if not scan_result.available:
            return None
        return scan_result.success

    def get_scan_result(self, position: str) -> list[Service] | None:
        result = self._execute(f"interface/{position}/scannerResult get")
        if not result.available or not result.success:
            return None
        return self._parse_scan_result_xml(position, result.payload)

    def get_streamer_status(
        self,
        position: str,
        interface_type: str,
        *,
        infoch_name: str = "",
        service_names: list[str] | None = None,
    ) -> StreamerStatus | None:
        result = self._execute(f"interface/{position}/streamerStatus get")
        if not result.available or not result.success:
            return None
        return self._parse_streamer_status_xml(
            interface_type,
            result.payload,
            infoch_name=infoch_name,
            service_names=service_names or [],
        )

    def get_tuner_status(self, position: str, interface_type: str) -> TunerStatus | None:
        result = self._execute(f"interface/{position}/tunerStatus get")
        if not result.available or not result.success:
            return None
        return self._parse_tuner_status_xml(interface_type, result.payload)

    def _execute(
        self,
        command: str,
        *,
        preserve_newlines: bool = False,
    ) -> _SocketCommandResult:
        if not config.enable_streamer_socket:
            return _SocketCommandResult(available=False, success=False)

        now = time.monotonic()
        if now < self._retry_after:
            return _SocketCommandResult(available=False, success=False)

        try:
            with socket.create_connection(
                (config.streamer_socket_host, config.streamer_socket_port),
                timeout=config.streamer_socket_timeout_seconds,
            ) as sock:
                sock.settimeout(config.streamer_socket_timeout_seconds)
                sock.sendall((command + "\r").encode("utf-8"))

                chunks: list[bytes] = []
                while True:
                    payload = sock.recv(4096)
                    if not payload:
                        break
                    chunks.append(payload)
        except OSError as exc:
            self._retry_after = now + _SOCKET_RETRY_BACKOFF_SECONDS
            logger.debug("Streamer socket command failed for %s: %s", command, exc)
            return _SocketCommandResult(available=False, success=False)

        self._retry_after = 0.0
        if not chunks:
            return _SocketCommandResult(available=True, success=False)

        decoded = b"".join(chunks).decode("utf-8", errors="replace")
        lines = decoded.splitlines()
        body = "\n".join(lines) if preserve_newlines else "".join(lines)

        if "100 OK" not in body:
            logger.debug("Streamer socket command returned non-OK response for %s", command)
            return _SocketCommandResult(available=True, success=False)

        _, _, payload = body.partition("100 OK")
        return _SocketCommandResult(
            available=True,
            success=True,
            payload=payload.lstrip("\r\n"),
        )

    def _parse_streamer_status_xml(
        self,
        interface_type: str,
        payload: str,
        *,
        infoch_name: str = "",
        service_names: list[str],
    ) -> StreamerStatus | None:
        root = self._parse_xml(payload)
        if root is None:
            return None

        normalized_type = self._normalize_interface_type(interface_type)

        if normalized_type == "infoch":
            sink = root.find(".//sink")
            if sink is None:
                return StreamerStatus(services=[])
            return StreamerStatus(
                services=[
                    ServiceStatus(
                        name=infoch_name,
                        bitrate=self._as_int(self._text(sink, "bitrate", "0")),
                    )
                ]
            )

        if normalized_type == "webradio":
            sink = root.find(".//sink")
            if sink is None:
                return StreamerStatus(services=[])
            return StreamerStatus(
                services=[
                    ServiceStatus(
                        name=service_names[0] if service_names else "",
                        bitrate=self._as_int(self._text(sink, "bitrate", "0")),
                        buffer_level=self._as_int(self._text(sink, "bufferlevel", "0")),
                    )
                ]
            )

        if normalized_type == "hls2ip":
            source = root.find(".//source")
            sink = root.find(".//sink")
            if source is None or sink is None:
                return StreamerStatus(services=[])

            name = service_names[0] if service_names else self._text(source, "userText", "")
            return StreamerStatus(
                services=[
                    ServiceStatus(
                        name=name,
                        download_bitrate=self._as_int(self._text(source, "download_bitrate", "0")),
                        selected_bitrate=self._as_int(self._text(source, "selected_bitrate", "0")),
                        segment_counter=self._as_int(self._text(source, "segmentCounter", "0")),
                        num_stream_switches=self._as_int(self._text(source, "num_stream_switches", "0")),
                        num_segments_missed=self._as_int(self._text(source, "num_segments_missed", "0")),
                        bitrate=self._as_int(self._text(sink, "bitrate", "0")),
                        buffer_level=self._as_int(self._text(sink, "bufferlevel", "0")),
                    )
                ]
            )

        if normalized_type in {"infostreamer", "hdmi2ip"}:
            services: list[ServiceStatus] = []
            for index, sink in enumerate(root.findall(".//sink")):
                services.append(
                    ServiceStatus(
                        name=service_names[index] if index < len(service_names) else f"Service {index + 1}",
                        bitrate=self._as_int(self._text(sink, "bitrate", "0")),
                    )
                )
            return StreamerStatus(services=services)

        services: list[ServiceStatus] = []
        for channel in root.findall(".//channel"):
            services.append(
                ServiceStatus(
                    name=self._text(channel, "userText", ""),
                    scrambled=self._as_bool(self._text(channel, "scrambledStream", "false")),
                    destination=self._text(channel, "destination", "") or None,
                    bitrate=self._as_int(self._text(channel, "bitRate", "0")),
                    discontinuity=self._as_int(self._text(channel, "discontinuityCounter", "0")),
                    source=self._text(channel, "source", "") or None,
                    mux_load=self._as_int(self._text(channel, "muxLoad", "0")),
                    max_mux_load=self._as_int(self._text(channel, "maxMuxLoad", "0")),
                )
            )

        if normalized_type in {"dsc", "mod"}:
            summary_tag = "dscStreamerStatus" if normalized_type == "dsc" else "eqamStreamerStatus"
            summary = root.find(f".//{summary_tag}")
            if summary is None:
                return StreamerStatus(services=services)
            return StreamerStatus(
                mux_load=self._as_int(self._text(summary, "muxLoad", "0")),
                max_mux_load=self._as_int(self._text(summary, "maxMuxLoad", "0")),
                ca_services=self._as_int(self._text(summary, "caServices", "0")) if normalized_type == "dsc" else 0,
                ca_pids=self._as_int(self._text(summary, "caPids", "0")) if normalized_type == "dsc" else 0,
                services=services,
            )

        return StreamerStatus(services=services)

    def _parse_tuner_status_xml(self, interface_type: str, payload: str) -> TunerStatus | None:
        root = self._parse_xml(payload)
        if root is None:
            return None

        normalized_type = self._normalize_interface_type(interface_type)
        if normalized_type in {"dvbs", "dvbt", "dvbc", "dvbudp"}:
            node = root.find(f".//{normalized_type}TunerStatus")
            if node is None:
                return None

            locked = self._as_bool(self._text(node, "locked", "false"))
            if normalized_type == "dvbudp":
                return TunerStatus(locked=locked)

            return TunerStatus(
                locked=locked,
                frequency=self._as_int(self._text(node, "frequency", "0")),
                signal_strength=self._as_int(self._text(node, "signalStrength", "0")),
                snr=self._as_int(self._text(node, "snr", "0")),
            )

        if normalized_type != "dsc":
            return None

        tuner_node = root.find(".//dscTunerStatus")
        if tuner_node is None:
            return None

        menu_node = root.find(".//caMmiMenu")
        menu_title_parts = []
        menu_items: list[CiMenuItem] = []
        if menu_node is not None:
            title = self._text(menu_node, "title", "")
            sub_title = self._text(menu_node, "subTitle", "")
            if title:
                menu_title_parts.append(title)
            if sub_title:
                menu_title_parts.append(sub_title)
            menu_items.append(CiMenuItem(id=0, label="Cancel"))
            for index, item_node in enumerate(menu_node.findall("./item"), start=1):
                label = (item_node.text or "").strip()
                if label:
                    menu_items.append(CiMenuItem(id=index, label=label))

        osd_messages: list[str] = []
        osd_node = root.find(".//caMmiOsd")
        if osd_node is not None:
            for item_node in osd_node.findall("./item"):
                message = (item_node.text or "").strip()
                if message:
                    osd_messages.append(message)

        return TunerStatus(
            ci_status=self._as_int(self._text(tuner_node, "ciStatus", "0")),
            ca_emm=self._as_bool(self._text(tuner_node, "caEmm", "false")),
            ca_text=self._text(tuner_node, "caText", ""),
            ca_osd="\n".join(osd_messages),
            menu_title=" - ".join(menu_title_parts),
            menu_items=menu_items,
            ci_menu_open=bool(menu_items),
        )

    def _parse_scan_result_xml(self, position: str, payload: str) -> list[Service] | None:
        root = self._parse_xml(payload)
        if root is None:
            return None

        services: list[Service] = []
        for service_node in root.findall(".//service"):
            sid = self._as_int(self._text(service_node, "serviceId", "0"))
            service_type = self._service_type_from_code(
                self._as_int(self._text(service_node, "serviceType", "0"))
            )
            pref_lcn = self._as_int(self._text(service_node, "prefLcn", "0"))
            original_network_id = self._text(service_node, "originalNetworkId", "0")
            transport_stream_id = self._text(service_node, "transportStreamId", "0")
            stream_url = self._empty_if_na(self._text(service_node, "streamUrl", ""))
            tags = self._empty_if_na(self._text(service_node, "tags", ""))

            languages: list[str] = []
            for stream_node in service_node.findall(".//stream"):
                language = self._text(stream_node, "language", "N/A")
                if language != "N/A" and language not in languages:
                    languages.append(language)

            services.append(
                Service(
                    id=0,
                    interface_pos=position,
                    name=self._text(service_node, "name", ""),
                    sid=sid,
                    type=service_type,
                    lang="All",
                    enabled=True,
                    all_langs=["All", *languages],
                    radio_url=stream_url or None,
                    show_pres=False,
                    scrambled=self._as_bool(self._text(service_node, "scrambled", "false")),
                    epg_url=f"{original_network_id}.{transport_stream_id}.{sid}",
                    key=f"{position}, {sid}",
                    hls_url=stream_url or None,
                    webradio_url=stream_url or None,
                    prefered_lcn=pref_lcn,
                    filters=[token.strip() for token in tags.split(",") if token.strip()] or None,
                    found=True,
                )
            )

        return services

    def _parse_xml(self, payload: str) -> ET.Element | None:
        cleaned = (payload or "").strip()
        if not cleaned:
            return None

        try:
            return ET.fromstring(cleaned)
        except ET.ParseError as exc:
            logger.debug("Failed to parse streamer socket XML payload: %s", exc)
            return None

    def _normalize_interface_type(self, interface_type: str) -> str:
        normalized = (interface_type or "").strip().lower()
        return _INTERFACE_TYPE_ALIASES.get(normalized, normalized)

    def _text(self, element: ET.Element | None, tag: str, default: str = "N/A") -> str:
        if element is None:
            return default

        node = element.find(f".//{tag}")
        if node is None or node.text is None:
            return default
        value = node.text.strip()
        return value if value else default

    def _as_int(self, value: str, default: int = 0) -> int:
        try:
            return int(value)
        except (TypeError, ValueError):
            return default

    def _as_bool(self, value: str) -> bool:
        return str(value).strip().lower() in {"1", "true", "yes", "on"}

    def _empty_if_na(self, value: str) -> str:
        return "" if value == "N/A" else value

    def _service_type_from_code(self, value: int) -> str:
        if value == 2:
            return "RADIO"
        if 25 <= value <= 31:
            return "TV_HD"
        return "TV_SD"


streamer_socket_service = StreamerSocketService()