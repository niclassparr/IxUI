"""Compatibility-backed runtime state for interface workflows."""

from __future__ import annotations

import hashlib
import logging
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from threading import RLock

from backend.config import config
from backend.models import (
    CiMenuItem,
    Config,
    Interface,
    Response,
    Service,
    ServiceStatus,
    StreamerStatus,
    TunerStatus,
)
from backend.services.data_service import data_service
from backend.services.streamer_socket_service import streamer_socket_service

logger = logging.getLogger(__name__)

_DVB_TYPES = {"dvbs", "dvbt", "dvbc", "dvbudp", "dsc"}
_IP_LIKE_TYPES = {"ip", "hls2ip", "infostreamer", "infoch", "webradio", "istr"}


@dataclass
class _RuntimeState:
    streamer_running: bool = False
    last_command: str = "stop"
    last_scan_time: str | None = None
    scanned_services: list[Service] = field(default_factory=list)
    log_lines: list[str] = field(default_factory=list)
    ci_menu_open: bool = False
    ci_menu_context: str = "root"
    ci_osd: str = "No active MMI dialog"


class InterfaceRuntimeService:
    """Provide deterministic compatibility behavior for runtime-backed pages."""

    def __init__(self) -> None:
        self._states: dict[str, _RuntimeState] = {}
        self._lock = RLock()
        self._system_powered_off = False

    def list_interfaces(self) -> list[Interface]:
        """Return interfaces with runtime-derived status values."""
        return [self._decorate_interface(iface) for iface in data_service.get_interfaces()]

    def get_interface(self, position: str) -> Interface | None:
        """Return a single interface with runtime-derived status values."""
        iface = data_service.get_interface(position)
        if iface is None:
            return None
        return self._decorate_interface(iface)

    def note_config_saved(self, position: str, interface_type: str, cfg: Config) -> None:
        """Record config-save activity in the runtime state."""
        iface = data_service.get_interface(position)
        if iface is None:
            return

        with self._lock:
            state = self._get_state_locked(position, iface)
            state.streamer_running = bool(cfg.interface_active)
            state.last_command = "save-config"
            self._append_log_locked(
                state,
                position,
                f"Configuration saved for {interface_type}; active={bool(cfg.interface_active)}",
            )

    def note_services_saved(self, position: str, services: list[Service]) -> None:
        """Record service-save activity and keep scan results in sync."""
        iface = data_service.get_interface(position)
        if iface is None:
            return

        with self._lock:
            state = self._get_state_locked(position, iface)
            state.scanned_services = self._normalize_services(position, services)
            state.last_command = "save-services"
            self._append_log_locked(
                state,
                position,
                f"Saved {len(services)} service definition(s)",
            )

    def apply_interface(self, position: str, interface_type: str) -> Response:
        """Apply the current interface configuration to the runtime layer."""
        iface = data_service.get_interface(position)
        if iface is None:
            return Response(success=False, error=f"Interface {position} not found")

        cfg = data_service.get_config(position, interface_type)
        if not self._config_is_usable(cfg, interface_type):
            return Response(success=False, error="Interface configuration is incomplete")

        with self._lock:
            state = self._get_state_locked(position, iface)
            state.streamer_running = bool(cfg.interface_active)
            state.last_command = "apply"
            self._append_log_locked(
                state,
                position,
                f"Applied {interface_type} configuration to runtime",
            )
        return Response(success=True)

    def start_scan(self, position: str) -> Response:
        """Run a deterministic compatibility scan and cache the discovered services."""
        iface = data_service.get_interface(position)
        if iface is None:
            return Response(success=False, error=f"Interface {position} not found")

        live_result = streamer_socket_service.start_scan(position)
        if live_result is False:
            return Response(success=False, error="Failed to start live scan")

        with self._lock:
            state = self._get_state_locked(position, iface)
            services = self._build_scan_services(iface) if live_result is None else []
            state.scanned_services = services
            state.last_scan_time = self._current_timestamp()
            state.last_command = "scan"
            self._append_log_locked(state, position, "Started compatibility scan")
            if live_result is None:
                self._append_log_locked(
                    state,
                    position,
                    f"Scan complete; discovered {len(services)} service(s)",
                )
        return Response(success=True)

    def get_scan_result(self, position: str) -> list[Service]:
        """Return the most recent scan result for an interface."""
        iface = data_service.get_interface(position)
        if iface is None:
            return []

        live_services = streamer_socket_service.get_scan_result(position)
        if live_services is not None:
            normalized = self._normalize_services(position, live_services)
            with self._lock:
                state = self._get_state_locked(position, iface)
                state.scanned_services = normalized
            return [service.model_copy(deep=True) for service in normalized]

        with self._lock:
            state = self._get_state_locked(position, iface)
            if state.scanned_services:
                return [service.model_copy(deep=True) for service in state.scanned_services]

            persisted_services = self._normalize_services(position, data_service.get_services(position))
            if persisted_services:
                return persisted_services

            services = self._build_scan_services(iface)
            state.scanned_services = services
            return [service.model_copy(deep=True) for service in services]

    def get_scan_time(self, position: str) -> str | None:
        """Return the most recent scan timestamp for an interface."""
        iface = data_service.get_interface(position)
        if iface is None:
            return None

        with self._lock:
            state = self._get_state_locked(position, iface)
            return state.last_scan_time

    def get_current_emm_list(self, position: str, is_dsc: bool = False) -> dict[str, object]:
        """Return compatibility EMM allocation data for the interface edit page."""
        iface = data_service.get_interface(position)
        if iface is None:
            return {"selected": 0, "free": [], "used": [], "entries": []}

        cfg = data_service.get_config(position, iface.type)
        selected = int(cfg.emm or 0) if cfg is not None else 0

        used_values: set[int] = set()
        for other_iface in data_service.get_interfaces():
            other_cfg = data_service.get_config(other_iface.position, other_iface.type)
            if other_cfg is None or not other_cfg.emm:
                continue
            if other_iface.position == position:
                continue
            used_values.add(int(other_cfg.emm))

        max_slots = 16 if is_dsc else 8
        entries = [
            {"value": 0, "label": "Disabled", "in_use": False}
        ]
        for index in range(1, max_slots + 1):
            entries.append(
                {
                    "value": index,
                    "label": f"EMM {index}",
                    "in_use": index in used_values,
                }
            )

        return {
            "selected": selected,
            "free": [index for index in range(1, max_slots + 1) if index not in used_values],
            "used": sorted(used_values),
            "entries": entries,
        }

    def update_multiband_type(self, position: str, interface_type: str) -> Response:
        """Update the selected interface type for multiband hardware."""
        response = data_service.update_interface_multiband_type(position, interface_type)
        if response.success:
            iface = data_service.get_interface(position)
            if iface is not None:
                with self._lock:
                    state = self._get_state_locked(position, iface)
                    self._append_log_locked(
                        state,
                        position,
                        f"Switched multiband runtime type to {interface_type}",
                    )
        return response

    def get_interface_status(self, position: str) -> str:
        """Return the runtime-derived status string for the interface."""
        live_status = self._live_interface_status(position)
        if live_status is not None:
            return live_status

        iface = self.get_interface(position)
        if iface is None:
            return "unknown"
        return iface.status or "unknown"

    def get_streamer_status(self, position: str, interface_type: str) -> StreamerStatus:
        """Return deterministic streamer status derived from current config and state."""
        iface = data_service.get_interface(position)
        if iface is None:
            return StreamerStatus(bitrate=0, status="stopped")

        live_status = streamer_socket_service.get_streamer_status(
            position,
            interface_type,
            infoch_name=self._infoch_name(position, iface),
            service_names=[
                service.name
                for service in data_service.get_services(position)
                if service.enabled and service.name
            ],
        )
        if live_status is not None:
            live_interface_status = self._live_interface_status(position)
            if live_interface_status:
                return live_status.model_copy(update={"status": live_interface_status})
            return live_status

        with self._lock:
            state = self._get_state_locked(position, iface)
            running = bool(iface.active and state.streamer_running and not self._system_powered_off)

        active_services = [service for service in self.get_scan_result(position) if service.enabled]
        service_rows = self._build_service_status_rows(position, interface_type, active_services, running)
        bitrate = sum(max(row.bitrate, row.selected_bitrate, row.download_bitrate) for row in service_rows)
        capacity = self._stream_capacity(interface_type)
        mux_load = min(100, round((bitrate / capacity) * 100)) if capacity and bitrate else 0
        ca_services = sum(1 for service in active_services if service.scrambled)
        if interface_type.lower() == "dsc" and active_services:
            ca_services = len(active_services)
        ca_pids = ca_services * 3 if running else 0
        status = "powered off" if self._system_powered_off else ("running" if running else "stopped")

        return StreamerStatus(
            bitrate=bitrate,
            status=status,
            pid=self._stable_pid(position, interface_type) if running else None,
            mux_load=mux_load,
            max_mux_load=100 if capacity else 0,
            ca_services=ca_services if running else 0,
            ca_pids=ca_pids,
            services=service_rows,
        )

    def get_tuner_status(self, position: str, interface_type: str) -> TunerStatus:
        """Return deterministic tuner status derived from config and service state."""
        iface = data_service.get_interface(position)
        if iface is None:
            return TunerStatus(locked=False, signal_strength=0, snr=0, ber=0)

        live_status = streamer_socket_service.get_tuner_status(position, interface_type)
        if live_status is not None:
            return live_status

        cfg = data_service.get_config(position, interface_type)
        services = self.get_scan_result(position)
        service_count = len([service for service in services if service.enabled])
        locked = bool(
            iface.active and not self._system_powered_off and self._config_is_usable(cfg, interface_type)
        )

        if interface_type.lower() == "dsc":
            with self._lock:
                state = self._get_state_locked(position, iface)
                menu_title, menu_items = self._build_ci_menu_locked(state)

            ci_status = self._build_ci_status(locked, service_count)
            if self._system_powered_off:
                ca_text = "Descrambler is unavailable while the system is powered off."
            elif ci_status & 4:
                ca_text = f"Descrambling {service_count} service(s)"
            elif ci_status & 2:
                ca_text = "CAM detected, waiting for active services"
            else:
                ca_text = "No CAM detected"

            return TunerStatus(
                locked=locked,
                frequency=int(cfg.freq or 0) if cfg is not None else 0,
                signal_strength=0,
                snr=0,
                ber=0,
                ci_status=ci_status,
                ca_emm=bool(cfg.emm) if cfg is not None else False,
                ca_text=ca_text,
                ca_osd=state.ci_osd,
                menu_title=menu_title,
                menu_items=menu_items,
                ci_menu_open=state.ci_menu_open,
            )

        if locked:
            signal_strength = min(9900, 5600 + (service_count * 650))
            snr = min(2800, 1180 + (service_count * 140))
            ber = 0 if service_count else 1
        else:
            signal_strength = 0
            snr = 0
            ber = 0

        return TunerStatus(
            locked=locked,
            frequency=int(cfg.freq or 0) if cfg is not None else 0,
            signal_strength=signal_strength,
            snr=snr,
            ber=ber,
        )

    def get_interface_log(self, position: str) -> str:
        """Return the current compatibility log for an interface."""
        iface = data_service.get_interface(position)
        if iface is None:
            return f"[INFO] No log data available for interface {position}\n"

        live_log = streamer_socket_service.get_interface_log(position)
        if live_log is not None:
            with self._lock:
                state = self._get_state_locked(position, iface)
                state.log_lines = live_log.splitlines()[-200:]
                self._write_log_lines_to_file(position, state.log_lines)
            return live_log

        with self._lock:
            state = self._get_state_locked(position, iface)
            self._sync_log_state_from_file_locked(position, state)
            return "\n".join(state.log_lines).rstrip() + "\n"

    def run_interface_command(self, position: str, command: str) -> Response:
        """Handle interface runtime commands used by the status and log pages."""
        iface = data_service.get_interface(position)
        if iface is None:
            return Response(success=False, error=f"Interface {position} not found")

        normalized = command.strip().lower()
        if normalized == "start":
            normalized = "stream"

        live_result = streamer_socket_service.run_interface_command(position, normalized)
        if live_result is False:
            return Response(success=False, error="Streamer command failed")

        with self._lock:
            state = self._get_state_locked(position, iface)

            if normalized == "stream":
                self._system_powered_off = False
                state.streamer_running = True
                state.last_command = "stream"
                self._append_log_locked(state, position, "Streamer started")
            elif normalized == "stop":
                state.streamer_running = False
                state.last_command = "stop"
                self._append_log_locked(state, position, "Streamer stopped")
            elif normalized == "restart":
                self._system_powered_off = False
                state.streamer_running = True
                state.last_command = "restart"
                self._append_log_locked(state, position, "Streamer restarted")
            elif normalized == "log":
                state.last_command = "log"
                self._append_log_locked(state, position, "Log requested")
            elif normalized.startswith("mmi_"):
                if (iface.type or "").lower() != "dsc":
                    return Response(success=False, error="CI menu is only available for DSC interfaces")
                response = self._handle_ci_command_locked(state, position, normalized)
                if not response.success:
                    return response
            else:
                return Response(success=False, error=f"Unknown interface command: {command}")

        return Response(success=True)

    def start_all_interfaces(self) -> Response:
        """Start all active interfaces in the compatibility runtime."""
        interfaces = data_service.get_interfaces()
        with self._lock:
            self._system_powered_off = False
            for iface in interfaces:
                state = self._get_state_locked(iface.position, iface)
                state.streamer_running = bool(iface.active)
                state.last_command = "allstart"
                self._append_log_locked(state, iface.position, "Global start command executed")
        return Response(success=True)

    def stop_all_interfaces(self) -> Response:
        """Stop all interfaces in the compatibility runtime."""
        interfaces = data_service.get_interfaces()
        with self._lock:
            self._system_powered_off = False
            for iface in interfaces:
                state = self._get_state_locked(iface.position, iface)
                state.streamer_running = False
                state.last_command = "allstop"
                self._append_log_locked(state, iface.position, "Global stop command executed")
        return Response(success=True)

    def poweroff_runtime(self) -> Response:
        """Power off the compatibility runtime."""
        interfaces = data_service.get_interfaces()
        with self._lock:
            self._system_powered_off = True
            for iface in interfaces:
                state = self._get_state_locked(iface.position, iface)
                state.streamer_running = False
                state.last_command = "poweroff"
                state.ci_menu_open = False
                state.ci_menu_context = "root"
                state.ci_osd = "System powered off"
                self._append_log_locked(state, iface.position, "System powered off")
        return Response(success=True)

    def reboot_runtime(self) -> Response:
        """Reboot the compatibility runtime and restore active interfaces."""
        interfaces = data_service.get_interfaces()
        with self._lock:
            self._system_powered_off = False
            for iface in interfaces:
                state = self._get_state_locked(iface.position, iface)
                state.streamer_running = bool(iface.active)
                state.last_command = "reboot"
                state.ci_menu_context = "root"
                state.ci_osd = "System rebooted"
                self._append_log_locked(state, iface.position, "System rebooted")
        return Response(success=True)

    def update_interface_inventory(self) -> Response:
        """Refresh runtime state against the current interface inventory."""
        interfaces = data_service.get_interfaces()
        known_positions = {iface.position for iface in interfaces}
        with self._lock:
            stale_positions = [position for position in self._states if position not in known_positions]
            for position in stale_positions:
                del self._states[position]

            for iface in interfaces:
                state = self._get_state_locked(iface.position, iface)
                state.streamer_running = bool(iface.active)
                state.last_command = "update-interfaces"
                self._append_log_locked(state, iface.position, "Interface inventory refreshed")

        return Response(success=True)

    def reset_runtime_state(self) -> None:
        """Reset all compatibility runtime state back to defaults."""
        with self._lock:
            self._states = {}
            self._system_powered_off = False
            self._clear_log_files()

    def _decorate_interface(self, iface: Interface) -> Interface:
        live_status = self._live_interface_status(iface.position)
        if live_status is not None:
            return iface.model_copy(update={"status": live_status})

        with self._lock:
            state = self._get_state_locked(iface.position, iface)
            status = self._status_text(iface, state)
        return iface.model_copy(update={"status": status})

    def _live_interface_status(self, position: str) -> str | None:
        status = streamer_socket_service.get_interface_status(position)
        if status is None:
            return None

        cleaned = status.strip()
        if not cleaned:
            return None
        if cleaned.upper() == "N/A":
            return cleaned
        return cleaned.lower()

    def _infoch_name(self, position: str, iface: Interface) -> str:
        cfg = data_service.get_interface_infoch(position)
        if cfg is not None and cfg.interface_name:
            return cfg.interface_name
        return iface.name

    def _get_state_locked(self, position: str, iface: Interface) -> _RuntimeState:
        state = self._states.get(position)
        if state is None:
            initial_log = self._read_log_lines_from_file(position)
            if not initial_log:
                initial_log = data_service.get_interface_log(position).strip().splitlines()
            if not initial_log:
                initial_log = [self._format_log_line(position, "Runtime state initialized")]
            state = _RuntimeState(
                streamer_running=bool(iface.active),
                last_command="start" if iface.active else "stop",
                log_lines=list(initial_log),
            )
            self._write_log_lines_to_file(position, state.log_lines)
            self._states[position] = state
        return state

    def _log_dir(self) -> Path:
        path = Path(config.artifact_dir) / "interface-logs"
        path.mkdir(parents=True, exist_ok=True)
        return path

    def _log_file_path(self, position: str) -> Path:
        return self._log_dir() / f"{position.lower()}.log"

    def _read_log_lines_from_file(self, position: str) -> list[str]:
        log_path = self._log_file_path(position)
        if not log_path.is_file():
            return []

        try:
            return log_path.read_text(encoding="utf-8", errors="ignore").splitlines()[-200:]
        except OSError as exc:
            logger.warning("Failed to read interface log file %s: %s", log_path, exc)
            return []

    def _write_log_lines_to_file(self, position: str, lines: list[str]) -> None:
        log_path = self._log_file_path(position)
        payload = "\n".join(lines).rstrip()
        if payload:
            payload += "\n"

        try:
            log_path.write_text(payload, encoding="utf-8")
        except OSError as exc:
            logger.warning("Failed to write interface log file %s: %s", log_path, exc)

    def _sync_log_state_from_file_locked(self, position: str, state: _RuntimeState) -> None:
        file_lines = self._read_log_lines_from_file(position)
        if file_lines:
            state.log_lines = file_lines

    def _clear_log_files(self) -> None:
        log_dir = Path(config.artifact_dir) / "interface-logs"
        if not log_dir.is_dir():
            return

        for log_path in log_dir.glob("*.log"):
            try:
                log_path.unlink()
            except OSError as exc:
                logger.warning("Failed to remove interface log file %s: %s", log_path, exc)

    def _status_text(self, iface: Interface, state: _RuntimeState) -> str:
        if self._system_powered_off:
            return "powered off"

        if not iface.active:
            return "disabled"

        interface_type = (iface.type or "").lower()
        if not state.streamer_running:
            return "idle"

        if interface_type == "dsc":
            return "locked"

        if interface_type in _DVB_TYPES:
            cfg = data_service.get_config(iface.position, iface.type)
            return "locked" if self._config_is_usable(cfg, interface_type) else "unlocked"
        if interface_type in _IP_LIKE_TYPES:
            return "connected"
        return "running"

    def _build_scan_services(self, iface: Interface) -> list[Service]:
        existing = self._normalize_services(iface.position, data_service.get_services(iface.position))
        if existing:
            return existing

        interface_type = (iface.type or "").lower()
        prefix_map = {
            "dvbs": "Satellite",
            "dvbt": "Terrestrial",
            "dvbc": "Cable",
            "ip": "IP",
            "infoch": "Info Channel",
            "webradio": "Radio",
            "hls2ip": "HLS",
            "infostreamer": "Info and Radio",
            "dvbhdmi": "HDMI",
            "hdmi2ip": "HDMI",
        }
        prefix = prefix_map.get(interface_type, "Service")
        count_map = {
            "dvbs": 4,
            "dvbt": 4,
            "dvbc": 4,
            "infostreamer": 5,
            "dvbhdmi": 1,
            "hdmi2ip": 1,
            "webradio": 1,
        }
        count = count_map.get(interface_type, 2)
        base_sid = (sum(ord(char) for char in f"{iface.position}:{iface.type}") % 4000) + 1000

        services: list[Service] = []
        for index in range(count):
            service_type = "RADIO" if interface_type in {"webradio", "infoch"} and index == count - 1 else "TV_SD"
            if interface_type in {"dvbhdmi", "hdmi2ip"}:
                service_type = "TV_HD"
            elif interface_type == "infostreamer":
                service_type = "RADIO"
            service_id = base_sid + index
            service = Service(
                id=service_id,
                interface_pos=iface.position,
                name=f"{prefix} {index + 1}",
                sid=service_id,
                type=service_type,
                lang="eng",
                enabled=True,
                all_langs=["eng"],
                scrambled=False,
                key=f"{iface.position}-{service_id}",
                prefered_lcn=index + 1,
                found=True,
            )
            if interface_type == "hls2ip":
                service.hls_url = (
                    f"https://demo.local/hls/{iface.position.lower()}/service-{index + 1}/index.m3u8"
                )
            elif interface_type in {"webradio", "infoch"}:
                stream_url = f"https://demo.local/stream/{iface.position.lower()}/{index + 1}.m3u8"
                service.radio_url = stream_url
                service.webradio_url = stream_url
            elif interface_type == "infostreamer":
                service.radio_url = f"https://demo.local/audio/{iface.position.lower()}/{index + 1}.mp3"
                service.show_pres = index % 2 == 0
            services.append(service)
        return services

    def _build_service_status_rows(
        self,
        position: str,
        interface_type: str,
        services: list[Service],
        running: bool,
    ) -> list[ServiceStatus]:
        lowered = interface_type.lower()
        route_by_service = {
            route.service_id: route for route in data_service.get_routes() if route.interface_pos == position
        }

        if lowered == "infoch":
            if not services:
                return []
            bitrate = 2_500_000 if running else 0
            return [ServiceStatus(name=services[0].name, bitrate=bitrate)]

        if lowered in {"hls2ip", "webradio"}:
            if not services:
                return []
            first = services[0]
            if lowered == "webradio":
                bitrate = 192_000 if running else 0
                return [
                    ServiceStatus(
                        name=first.name,
                        bitrate=bitrate,
                        buffer_level=650 if running else 0,
                    )
                ]

            base_bitrate = 3_600_000 if running else 0
            return [
                ServiceStatus(
                    name=first.name,
                    bitrate=base_bitrate,
                    download_bitrate=base_bitrate + (450_000 if running else 0),
                    selected_bitrate=base_bitrate,
                    segment_counter=144 if running else 0,
                    num_stream_switches=1 if running else 0,
                    num_segments_missed=0,
                    buffer_level=850 if running else 0,
                )
            ]

        capacity = self._stream_capacity(interface_type)
        rows: list[ServiceStatus] = []
        for index, service in enumerate(services):
            bitrate = self._estimate_service_bitrate(service, lowered, index, running)
            route = route_by_service.get(service.id)
            mux_load = min(100, round((bitrate / capacity) * 100)) if capacity and bitrate else 0
            rows.append(
                ServiceStatus(
                    name=service.name,
                    scrambled=bool(service.scrambled),
                    destination=(route.out_ip or route.output_name) if route is not None else None,
                    bitrate=bitrate,
                    discontinuity=0 if running else 0,
                    source=position,
                    mux_load=mux_load,
                    max_mux_load=100 if capacity else 0,
                )
            )
        return rows

    def _estimate_service_bitrate(
        self,
        service: Service,
        interface_type: str,
        index: int,
        running: bool,
    ) -> int:
        if not running:
            return 0

        if interface_type == "dsc":
            return 4_800_000 + (index * 650_000)
        if interface_type == "mod":
            return 4_200_000 + (index * 500_000)
        if service.type.lower() == "radio":
            return 256_000
        return 3_200_000 + (index * 450_000)

    def _stream_capacity(self, interface_type: str) -> int:
        lowered = interface_type.lower()
        if lowered == "dsc":
            return 65_000_000
        if lowered == "mod":
            return 45_000_000
        if lowered == "hls2ip":
            return 12_000_000
        if lowered == "webradio":
            return 384_000
        if lowered == "infoch":
            return 8_000_000
        return 40_000_000

    def _build_ci_status(self, locked: bool, service_count: int) -> int:
        if not locked:
            return 0
        if service_count > 0:
            return 6
        return 2

    def _handle_ci_command_locked(
        self,
        state: _RuntimeState,
        position: str,
        command: str,
    ) -> Response:
        state.last_command = command

        if command == "mmi_open":
            self._system_powered_off = False
            state.ci_menu_open = True
            state.ci_menu_context = "root"
            state.ci_osd = "Select an item from the CI menu."
            self._append_log_locked(state, position, "Opened CI menu")
            return Response(success=True)

        if command == "mmi_close":
            state.ci_menu_open = False
            state.ci_menu_context = "root"
            state.ci_osd = "CI menu closed"
            self._append_log_locked(state, position, "Closed CI menu")
            return Response(success=True)

        if not command.startswith("mmi_answer"):
            return Response(success=False, error=f"Unknown CI command: {command}")

        if not state.ci_menu_open:
            return Response(success=False, error="CI menu is not open")

        parts = command.split(maxsplit=1)
        if len(parts) != 2:
            return Response(success=False, error="CI menu answer is missing an item id")

        try:
            answer_id = int(parts[1])
        except ValueError:
            return Response(success=False, error="CI menu answer id must be numeric")

        if state.ci_menu_context == "root":
            if answer_id == 0:
                state.ci_menu_open = False
                state.ci_osd = "CI menu closed"
                self._append_log_locked(state, position, "Closed CI menu from root")
                return Response(success=True)
            if answer_id == 1:
                state.ci_menu_context = "subscription"
                state.ci_osd = "Subscription valid until 2026-12-31"
            elif answer_id == 2:
                state.ci_menu_context = "entitlements"
                state.ci_osd = "EMM queue healthy; last update 12s ago"
            elif answer_id == 3:
                state.ci_menu_context = "module"
                state.ci_osd = "Demo CAM firmware 7.3.0"
            else:
                return Response(success=False, error=f"Unknown CI menu item: {answer_id}")

            self._append_log_locked(state, position, f"Selected CI menu item {answer_id}")
            return Response(success=True)

        if answer_id == 0:
            state.ci_menu_context = "root"
            state.ci_osd = "Returned to the main CI menu"
            self._append_log_locked(state, position, "Returned to CI menu root")
            return Response(success=True)

        if state.ci_menu_context == "subscription":
            state.ci_osd = "Subscription refreshed successfully"
        elif state.ci_menu_context == "entitlements":
            state.ci_osd = "Triggered EMM refresh"
        else:
            state.ci_osd = "Module information updated"

        self._append_log_locked(state, position, f"Confirmed CI submenu action {answer_id}")
        return Response(success=True)

    def _build_ci_menu_locked(self, state: _RuntimeState) -> tuple[str, list[CiMenuItem]]:
        if not state.ci_menu_open:
            return "", []

        if state.ci_menu_context == "subscription":
            return (
                "Subscription Status",
                [
                    CiMenuItem(id=1, label="Refresh Status"),
                    CiMenuItem(id=0, label="Back"),
                ],
            )
        if state.ci_menu_context == "entitlements":
            return (
                "Entitlements",
                [
                    CiMenuItem(id=1, label="Force EMM Refresh"),
                    CiMenuItem(id=0, label="Back"),
                ],
            )
        if state.ci_menu_context == "module":
            return (
                "Module Information",
                [CiMenuItem(id=0, label="Back")],
            )

        return (
            "Common Interface",
            [
                CiMenuItem(id=1, label="Subscription Status"),
                CiMenuItem(id=2, label="Entitlements"),
                CiMenuItem(id=3, label="Module Information"),
                CiMenuItem(id=0, label="Close Menu"),
            ],
        )

    def _normalize_services(self, position: str, services: list[Service]) -> list[Service]:
        normalized: list[Service] = []
        for index, service in enumerate(services):
            service_id = int(service.id or service.sid or (index + 1))
            sid = int(service.sid or service_id)
            normalized.append(
                service.model_copy(
                    update={
                        "id": service_id,
                        "sid": sid,
                        "interface_pos": service.interface_pos or position,
                        "key": service.key or f"{position}-{sid}",
                        "prefered_lcn": service.prefered_lcn or (index + 1),
                        "found": True,
                    },
                    deep=True,
                )
            )
        return normalized

    def _config_is_usable(self, cfg: Config | None, interface_type: str) -> bool:
        if cfg is None:
            return False

        lowered = interface_type.lower()
        if lowered in {"dvbs", "dvbt", "dvbc"}:
            return bool(cfg.freq)
        if lowered in {"ip", "dvbudp"}:
            return bool(cfg.in_ip and cfg.in_port)
        if lowered in {"webradio", "infoch"}:
            return bool(cfg.interface_name or cfg.webradio_url or cfg.pres_url)
        if lowered in {"infostreamer", "istr"}:
            return bool(cfg.pres_url)
        if lowered == "hls2ip":
            return bool(cfg.max_bitrate)
        return True

    def _append_log_locked(self, state: _RuntimeState, position: str, message: str) -> None:
        state.log_lines.append(self._format_log_line(position, message))
        if len(state.log_lines) > 200:
            state.log_lines = state.log_lines[-200:]
        self._write_log_lines_to_file(position, state.log_lines)

    def _format_log_line(self, position: str, message: str) -> str:
        timestamp = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M:%S")
        return f"[{timestamp}] {position}: {message}"

    def _current_timestamp(self) -> str:
        return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")

    def _stable_pid(self, position: str, interface_type: str) -> int:
        digest = hashlib.sha1(f"{position}:{interface_type}".encode("utf-8")).hexdigest()
        return 1000 + (int(digest[:6], 16) % 8000)


interface_runtime_service = InterfaceRuntimeService()