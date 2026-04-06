"""Data service providing entity access via PostgreSQL with mock data fallback.

When a database connection is available (DATABASE_URL env var or default
postgresql://postgres:postgres@127.0.0.1:5432/ixui) all operations read from /
write to the real database tables defined in ixui_73_260312.sql.

When no database connection can be established the service falls back to the
in-memory mock data so the application remains usable in local development and
test environments without a running PostgreSQL instance.

DB schema tables used:
  interfaces, services, routes, nv, forced_content, users
  config_sat, config_ter, config_dvbc, config_dvbudp, config_dsc,
  config_hdmi, config_hls, config_istr, config_webradio, config_eqam
"""

from __future__ import annotations

import copy
import logging
import platform
import random
import shutil
import socket
import subprocess
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from typing import Any

try:
    import psutil
except ImportError:  # pragma: no cover - optional runtime enhancement
    psutil = None

try:
    from zoneinfo import ZoneInfo, available_timezones
except ImportError:  # pragma: no cover - fallback for older runtimes
    ZoneInfo = None
    available_timezones = None

from backend.config import config
from backend.models import (
    Bitrate,
    Config,
    DateTimeState,
    DateTimeUpdateRequest,
    ForcedContent,
    Interface,
    IpMac,
    IpStatus,
    Media,
    ModulatorAssignment,
    NameValue,
    NetworkSettingsUpdateRequest,
    Package,
    Response,
    Route,
    Service,
    StreamerStatus,
    TunerStatus,
    UnitInfo,
)

logger = logging.getLogger(__name__)

_NETWORK_PING_TIMEOUT_SECONDS = 1.0
_NETWORK_SOCKET_TIMEOUT_SECONDS = 0.75
_NETWORK_FALLBACK_PORTS: dict[str, tuple[int, ...]] = {
    "dns1": (53,),
    "dns2": (53,),
    "public": (53, 443, 80),
    "gateway": (),
}

_COMMON_TIMEZONES: tuple[str, ...] = (
    "UTC",
    "Europe/Stockholm",
    "Europe/London",
    "Europe/Berlin",
    "America/New_York",
    "Asia/Tokyo",
)

_CLOUD_DETAIL_DEFAULTS: dict[str, str] = {
    "ixcloud_enable": "true",
    "ixcloud_online": "false",
    "ixcloud_validate_date": "",
    "ixcloud_validate_message": "Cloud is offline.",
    "ixcloud_beaconid": "IXCLOUD-DEMO-001",
}

# ---------------------------------------------------------------------------
# Demo data fixtures (fallback when no DB is available)
# ---------------------------------------------------------------------------

_INTERFACES: list[Interface] = [
    Interface(
        position="A1", name="Tuner A1", type="dvbs",
        status="locked", active=True, emm=False, multi_band=False, network_num=1,
    ),
    Interface(
        position="A2", name="Tuner A2", type="dvbt",
        status="locked", active=True, emm=False, multi_band=False, network_num=1,
    ),
    Interface(
        position="B1", name="Tuner B1", type="dvbc",
        status="unlocked", active=False, emm=False, multi_band=False, network_num=2,
    ),
    Interface(
        position="C1", name="IP Input C1", type="ip",
        status="connected", active=True, emm=False, multi_band=False, network_num=1,
    ),
    Interface(
        position="D1", name="Descrambler D1", type="dsc",
        status="locked", active=True, emm=True, multi_band=False, network_num=1,
    ),
]

_SERVICE_TYPE_ALIASES = {
    "TV": "TV_SD",
    "TV_SD": "TV_SD",
    "TVHD": "TV_HD",
    "TV_HD": "TV_HD",
    "RADIO": "RADIO",
}


def _normalize_service_type(value: str | None) -> str:
    token = str(value or "").strip().upper().replace("-", "_")
    return _SERVICE_TYPE_ALIASES.get(token, "TV_SD")

_SERVICES: list[Service] = [
    Service(
        id=1, interface_pos="A1", name="CNN International", sid=1001,
        type="TV_SD", lang="eng", enabled=True,
        all_langs=["eng", "spa", "fra"], scrambled=False, found=True,
        prefered_lcn=1, hls_url="/hls/cnn/index.m3u8",
    ),
    Service(
        id=2, interface_pos="A1", name="BBC World News", sid=1002,
        type="TV_SD", lang="eng", enabled=True,
        all_langs=["eng", "ara"], scrambled=False, found=True,
        prefered_lcn=2, hls_url="/hls/bbc/index.m3u8",
    ),
    Service(
        id=3, interface_pos="A2", name="France 24", sid=2001,
        type="TV_SD", lang="fra", enabled=True,
        all_langs=["fra", "eng"], scrambled=False, found=True,
        prefered_lcn=3,
    ),
    Service(
        id=4, interface_pos="A2", name="DW News", sid=2002,
        type="TV_SD", lang="deu", enabled=True,
        all_langs=["deu", "eng"], scrambled=True, found=True,
        prefered_lcn=4, key="0123456789ABCDEF",
    ),
    Service(
        id=5, interface_pos="C1", name="Al Jazeera English", sid=3001,
        type="TV_SD", lang="eng", enabled=True,
        all_langs=["eng", "ara"], scrambled=False, found=True,
        prefered_lcn=5,
    ),
    Service(
        id=6, interface_pos="C1", name="Classic FM", sid=3002,
        type="RADIO", lang="eng", enabled=True,
        all_langs=["eng"], scrambled=False, found=True,
        prefered_lcn=6, radio_url="http://stream.classicfm.net/live",
    ),
    Service(
        id=7, interface_pos="D1", name="Encrypted Sports HD", sid=4001,
        type="TV_SD", lang="eng", enabled=True,
        all_langs=["eng", "swe"], scrambled=True, found=True,
        prefered_lcn=7, key="D1-4001",
    ),
    Service(
        id=8, interface_pos="D1", name="Encrypted Cinema", sid=4002,
        type="TV_SD", lang="eng", enabled=True,
        all_langs=["eng"], scrambled=True, found=True,
        prefered_lcn=8, key="D1-4002",
    ),
]

_ROUTES: list[Route] = [
    Route(
        id=1, service_id=1, service_name="CNN International", service_type="TV_SD",
        interface_pos="A1", interface_type="dvbs", lcn=1, out_sid=101,
        out_ip="239.1.1.1", hls_enable=True, output_name="Output 1",
    ),
    Route(
        id=2, service_id=2, service_name="BBC World News", service_type="TV_SD",
        interface_pos="A1", interface_type="dvbs", lcn=2, out_sid=102,
        out_ip="239.1.1.2", hls_enable=True, output_name="Output 2",
    ),
    Route(
        id=3, service_id=3, service_name="France 24", service_type="TV_SD",
        interface_pos="A2", interface_type="dvbt", lcn=3, out_sid=201,
        out_ip="239.1.2.1", hls_enable=False, output_name="Output 3",
    ),
    Route(
        id=4, service_id=5, service_name="Al Jazeera English", service_type="TV_SD",
        interface_pos="C1", interface_type="ip", lcn=5, out_sid=301,
        out_ip="239.1.3.1", hls_enable=True, output_name="Output 4",
    ),
]

_SETTINGS: list[NameValue] = [

    NameValue(id=1, name="dvbc_enable", value="true"),
    NameValue(id=2, name="dvbc_freq", value="330000000"),
    NameValue(id=3, name="dvbc_symb", value="6900000"),
    NameValue(id=4, name="dvbc_qam", value="QAM-256"),
    NameValue(id=5, name="dvbc_attenuation", value="10"),
    NameValue(id=6, name="dvbc_netid", value="41001"),
    NameValue(id=7, name="dvbc_orgnetid", value="40961"),
    NameValue(id=8, name="dvbc_netname", value="Net"),
    NameValue(id=9, name="dvbc_provider", value="Net"),
    NameValue(id=10, name="dvbc_net2_enable", value="false"),
    NameValue(id=11, name="dvbc_net2_freq", value="394000000"),
    NameValue(id=12, name="dvbc_net2_symb", value="6900000"),
    NameValue(id=13, name="dvbc_net2_qam", value="QAM-256"),
    NameValue(id=14, name="dvbc_net2_attenuation", value="0"),
    NameValue(id=15, name="dvbc_net2_netid", value="41002"),
    NameValue(id=16, name="dvbc_net2_orgnetid", value="40961"),
    NameValue(id=17, name="dvbc_net2_netname", value="Net2"),
    NameValue(id=18, name="dvbc_net2_provider", value="Net2"),
    NameValue(id=19, name="ip_enable", value="true"),
    NameValue(id=20, name="ip_startaddr", value="239.1.1.1:10000"),
    NameValue(id=21, name="nw_multicastdev", value="eth0"),
    NameValue(id=22, name="ip_ttl", value="2"),
    NameValue(id=23, name="ip_tos", value="0"),
    NameValue(id=24, name="dsc_services", value="8"),
    NameValue(id=25, name="dsc_bitrate", value="65000000"),
    NameValue(id=26, name="hls_server_ip", value="127.0.0.1"),
    NameValue(id=27, name="hls_inport", value="7000"),
    NameValue(id=28, name="hls_outport", value="8000"),
    NameValue(id=29, name="hls_services", value="10"),
    NameValue(id=30, name="hls_playback_prefix", value="http://[ip]"),
    NameValue(id=31, name="portal_enable", value="false"),
    NameValue(id=32, name="portal_server_ip", value="127.0.0.1"),
    NameValue(id=33, name="portal_url", value="http://host/tvportal"),
    NameValue(id=34, name="bitrate_tvsd", value="5000000"),
    NameValue(id=35, name="bitrate_tvhd", value="10000000"),
    NameValue(id=36, name="bitrate_radio", value="300000"),
    NameValue(id=37, name="hls_max_bitrate", value="10000000"),
    NameValue(id=38, name="hls_ba_enable", value="false"),
    NameValue(id=39, name="hls_ba_user", value=""),
    NameValue(id=40, name="hls_ba_passwd", value=""),
    NameValue(id=41, name="remux_enable", value="false"),
    NameValue(id=42, name="remux_audio_format", value="mp2"),
    NameValue(id=43, name="remux_audio_offset", value="200"),
    NameValue(id=44, name="remux_muxrate", value="10000000"),
    NameValue(id=45, name="nw_hostname", value="ixui-unit-1"),
    NameValue(id=46, name="nw_gateway", value="192.168.1.1"),
    NameValue(id=47, name="nw_dns1", value="8.8.8.8"),
    NameValue(id=48, name="nw_dns2", value="1.1.1.1"),
    NameValue(id=49, name="nw_eth0_onboot", value="yes"),
    NameValue(id=50, name="nw_eth0_bootproto", value="static"),
    NameValue(id=51, name="nw_eth0_ipaddr", value="192.168.0.73"),
    NameValue(id=52, name="nw_eth0_netmask", value="255.255.255.0"),
    NameValue(id=53, name="nw_eth0_mac", value="00:22:ab:80:7b:5e"),
    NameValue(id=54, name="nw_eth1_onboot", value="yes"),
    NameValue(id=55, name="nw_eth1_bootproto", value="static"),
    NameValue(id=56, name="nw_eth1_ipaddr", value="172.16.0.1"),
    NameValue(id=57, name="nw_eth1_netmask", value="255.255.255.0"),
    NameValue(id=58, name="nw_eth1_mac", value="00:22:ab:80:7b:5f"),
    NameValue(id=59, name="ntp_enable", value="true"),
    NameValue(id=60, name="timezone", value="Europe/Stockholm"),
    NameValue(id=61, name="hls_enable", value="true"),
    NameValue(id=62, name="ixcloud_enable", value="true"),
    NameValue(id=63, name="ixcloud_validate_url", value="http://ixcloud.ixanon.se/he_validate"),
    NameValue(id=64, name="ixcloud_ping_ip", value="10.8.0.1"),
    NameValue(id=65, name="forced_content_enable", value="false"),
    NameValue(id=66, name="clock_date", value="2026-02-09"),
    NameValue(id=67, name="clock_time", value="10:23"),
    NameValue(id=68, name="ixcloud_online", value="false"),
    NameValue(id=69, name="ixcloud_validate_date", value=""),
    NameValue(id=70, name="ixcloud_validate_message", value="Cloud is offline."),
    NameValue(id=71, name="ixcloud_beaconid", value="IXCLOUD-DEMO-001"),

]

_CONFIGS: dict[str, Config] = {
    "A1": Config(
        id=1, interface_pos="A1", interface_name="Tuner A1",
        interface_active=True, freq=12188, pol="H", symb=27500,
        del_sys="DVB-S2", satno=0, lnb_type="universal", bw=0, emm=0,
    ),
    "A2": Config(
        id=2, interface_pos="A2", interface_name="Tuner A2",
        interface_active=True, freq=474000, pol=None, symb=0,
        del_sys="DVB-T2", satno=0, lnb_type=None, bw=8, emm=0,
        constellation="64QAM",
    ),
    "B1": Config(
        id=3, interface_pos="B1", interface_name="Tuner B1",
        interface_active=False, freq=306000, pol=None, symb=6900,
        del_sys="DVB-C", satno=0, lnb_type=None, bw=8, emm=0,
        constellation="256QAM",
    ),
    "C1": Config(
        id=4, interface_pos="C1", interface_name="IP Input C1",
        interface_active=True, freq=0, pol=None, symb=0,
        del_sys=None, satno=0, lnb_type=None, bw=0, emm=0,
        in_ip="239.0.0.1", in_port=5000, max_bitrate=40000,
    ),
    "D1": Config(
        id=5, interface_pos="D1", interface_name="Descrambler D1",
        interface_active=True, freq=12188, pol="H", symb=27500,
        del_sys="DVB-S2", satno=0, lnb_type="universal", bw=0, emm=1,
    ),
}

_FORCED_CONTENTS: dict[str, ForcedContent] = {
    "1": ForcedContent(
        id=1,
        name="Emergency Broadcast",
        enabled=False,
        networks=1,
        ts_filename="emergency-slide.png",
        operation_mode=0,
        signal_type=0,
        override_index=0,
        signal_status=0,
        com_status=True,
        volume=100,
    ),
    "2": ForcedContent(
        id=2,
        name="Maintenance Notice",
        enabled=False,
        networks=2,
        ts_filename="maintenance-card.mp4",
        operation_mode=1,
        signal_type=0,
        override_index=0,
        signal_status=0,
        com_status=True,
        volume=80,
    ),
    "3": ForcedContent(
        id=3,
        name="Promotional Content",
        enabled=True,
        networks=3,
        ts_filename="channel-banner.jpg",
        operation_mode=0,
        signal_type=1,
        override_index=1,
        signal_status=1,
        com_status=True,
        volume=70,
    ),
    "4": ForcedContent(
        id=4,
        name="Info Slide",
        enabled=False,
        networks=0,
        ts_filename="startup-logo.png",
        operation_mode=1,
        signal_type=0,
        override_index=0,
        signal_status=0,
        com_status=True,
        volume=-1,
    ),
}

_NETWORK_HOSTS: list[IpMac] = [
    IpMac(ip="192.168.0.73", mac="00:22:AB:80:7B:5E"),
    IpMac(ip="172.16.0.1", mac="00:22:AB:80:7B:5F"),
]

_PACKAGES: list[Package] = [
    Package(name="ixui-core", version="3.2.1", installed=True),
    Package(name="ixui-hls-plugin", version="1.4.0", installed=True),
    Package(name="ixui-cloud-agent", version="2.0.3", installed=False),
    Package(name="ixui-epg-grabber", version="1.1.0", installed=False),
]

_MEDIA: list[Media] = [
    Media(
        id=1,
        title="Startup Logo",
        internal_filename="startup-logo.png",
        name="Startup Logo",
        url="/media/startup-logo.png",
    ),
    Media(
        id=2,
        title="Channel Banner",
        internal_filename="channel-banner.jpg",
        name="Channel Banner",
        url="/media/channel-banner.jpg",
    ),
    Media(
        id=3,
        title="Emergency Slide",
        internal_filename="emergency-slide.png",
        name="Emergency Slide",
        url="/media/emergency-slide.png",
    ),
    Media(
        id=4,
        title="Maintenance Card",
        internal_filename="maintenance-card.mp4",
        name="Maintenance Card",
        url="/media/maintenance-card.mp4",
    ),
]

_HLS_CAPABLE_TYPES: frozenset[str] = frozenset({"dvbs", "dvbt", "ip"})

_HLS_OUTPUT_INTERFACES: list[Interface] = [
    Interface(position="H1", name="HLS Output 1", type="hls2ip", active=False),
    Interface(position="H2", name="HLS Output 2", type="hls2ip", active=False),
    Interface(position="H3", name="HLS Output 3", type="hls2ip", active=False),
    Interface(position="H4", name="HLS Output 4", type="hls2ip", active=False),
]

_INTERFACE_LOGS: dict[str, str] = {
    "A1": (
        "[2024-06-01 08:00:01] A1: Tuner initialised – DVB-S2\n"
        "[2024-06-01 08:00:02] A1: LNB power ON, 13V vertical\n"
        "[2024-06-01 08:00:03] A1: Frequency 12188 MHz, Symbol rate 27500\n"
        "[2024-06-01 08:00:04] A1: Lock acquired – SNR 14.2 dB, BER 0\n"
        "[2024-06-01 08:00:05] A1: Service scan complete – 2 services found\n"
    ),
    "A2": (
        "[2024-06-01 08:00:01] A2: Tuner initialised – DVB-T2\n"
        "[2024-06-01 08:00:02] A2: Frequency 474000 kHz, Bandwidth 8 MHz\n"
        "[2024-06-01 08:00:03] A2: Lock acquired – SNR 11.8 dB, BER 1\n"
        "[2024-06-01 08:00:04] A2: Service scan complete – 2 services found\n"
    ),
    "B1": (
        "[2024-06-01 08:00:01] B1: Tuner initialised – DVB-C\n"
        "[2024-06-01 08:00:02] B1: Frequency 306000 kHz, Symbol rate 6900\n"
        "[2024-06-01 08:00:03] B1: No lock – signal not detected\n"
    ),
    "C1": (
        "[2024-06-01 08:00:01] C1: IP input initialised\n"
        "[2024-06-01 08:00:02] C1: Listening on 239.0.0.1:5000\n"
        "[2024-06-01 08:00:03] C1: Stream detected – bitrate 8500 kbps\n"
        "[2024-06-01 08:00:04] C1: Service scan complete – 2 services found\n"
    ),
}

# ---------------------------------------------------------------------------
# Interface-type → config table mapping (mirrors GWT setConfig/getConfig logic)
# ---------------------------------------------------------------------------

_CONFIG_TABLE: dict[str, str] = {
    "sat": "config_sat",
    "dvbs": "config_sat",
    "dvbs2": "config_sat",
    "ter": "config_ter",
    "dvbt": "config_ter",
    "dvbt2": "config_ter",
    "dvbc": "config_dvbc",
    "ip": "config_dvbudp",
    "dvbudp": "config_dvbudp",
    "dsc": "config_dsc",
    "hdmi": "config_hdmi",
    "hls": "config_hls",
    "hls2ip": "config_hls",
    "istr": "config_istr",
    "infostreamer": "config_istr",
    "webradio": "config_webradio",
}


def _iface_type_to_table(interface_type: str) -> str | None:
    return _CONFIG_TABLE.get(interface_type.lower())


_EMM_INTERFACE_TYPES = {"dvbs", "dvbs2", "dvbt", "dvbt2", "dvbc", "dvbudp", "dsc"}


def _supports_emm(interface_type: str | None) -> bool:
    return (interface_type or "").lower() in _EMM_INTERFACE_TYPES


def _uses_emm_slot_table(interface_type: str | None) -> bool:
    lowered = (interface_type or "").lower()
    return lowered in _EMM_INTERFACE_TYPES and lowered != "dsc"


def _as_bool(value: Any) -> bool:
    return str(value).strip().lower() in {"1", "true", "yes", "on"}


def _list_timezones(selected: str | None = None) -> list[str]:
    zones = set(_COMMON_TIMEZONES)
    if selected:
        zones.add(selected)
    if available_timezones is not None:
        try:
            zones.update(available_timezones())
        except Exception:
            pass
    return sorted(zones)


def _datetime_for_timezone(timezone_name: str | None) -> datetime:
    if ZoneInfo is not None and timezone_name:
        try:
            return datetime.now(ZoneInfo(timezone_name))
        except Exception:
            pass
    return datetime.now()


# ---------------------------------------------------------------------------
# DataService
# ---------------------------------------------------------------------------


class DataService:
    """Entity access layer.

    Uses PostgreSQL when available; falls back to in-memory mock data otherwise.
    """

    def __init__(self) -> None:
        self._interfaces: list[Interface] = []
        self._services: list[Service] = []
        self._routes: list[Route] = []
        self._settings: list[NameValue] = []
        self._configs: dict[str, Config] = {}
        self._forced_contents: dict[str, ForcedContent] = {}
        self._packages: list[Package] = []
        self._media: list[Media] = []
        self._config_changed = False
        self._last_update_result = "No updates have been installed yet."
        self.reset_demo_state()

    def reset_demo_state(self) -> None:
        """Restore the in-memory compatibility state back to fixture defaults."""
        self._interfaces = copy.deepcopy(_INTERFACES)
        self._services = copy.deepcopy(_SERVICES)
        self._routes = copy.deepcopy(_ROUTES)
        self._settings = copy.deepcopy(_SETTINGS)
        self._configs = copy.deepcopy(_CONFIGS)
        self._forced_contents = copy.deepcopy(_FORCED_CONTENTS)
        self._packages = copy.deepcopy(_PACKAGES)
        self._media = copy.deepcopy(_MEDIA)
        self._config_changed: bool = False
        self._last_update_result: str = "No updates have been installed yet."

    # ------------------------------------------------------------------
    # DB connection helper
    # ------------------------------------------------------------------

    def _get_connection(self):
        from backend.db import get_connection
        return get_connection()

    # ------------------------------------------------------------------
    # Interfaces
    # ------------------------------------------------------------------

    def get_interfaces(self, *, is_interfaces: bool = True) -> list[Interface]:
        """Return all interfaces, optionally filtered to real interfaces only."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_interfaces(conn, is_interfaces=is_interfaces)
        if is_interfaces:
            return [i for i in self._interfaces if i.type != "virtual"]
        return list(self._interfaces)

    def get_interface(self, position: str) -> Interface | None:
        """Return a single interface by position, or None."""
        conn = self._get_connection()
        if conn is not None:
            ifaces = self._db_get_interfaces(conn, is_interfaces=False)
            for iface in ifaces:
                if iface.position == position:
                    return iface
            return None
        for iface in self._interfaces:
            if iface.position == position:
                return iface
        return None

    def get_interface_infoch(self, position: str) -> Config | None:
        """Return the legacy info-channel config payload for an interface."""
        iface = self.get_interface(position)
        if iface is None or (iface.type or "").lower() != "infoch":
            return None

        return Config(
            interface_pos=position,
            interface_name=iface.name,
            interface_active=bool(iface.active),
        )

    def set_interface_infoch(self, cfg: Config) -> Response:
        """Persist legacy info-channel name and active state."""
        iface = self.get_interface(cfg.interface_pos)
        if iface is None or (iface.type or "").lower() != "infoch":
            return Response(success=False, error=f"Interface {cfg.interface_pos} is not an info channel")

        conn = self._get_connection()
        if conn is not None:
            return self._db_set_interface_infoch(conn, cfg)

        for index, current in enumerate(self._interfaces):
            if current.position != cfg.interface_pos:
                continue
            self._interfaces[index] = current.model_copy(
                update={
                    "name": cfg.interface_name or current.name,
                    "active": bool(cfg.interface_active),
                }
            )
            break

        existing_cfg = self._configs.get(cfg.interface_pos)
        if existing_cfg is not None:
            self._configs[cfg.interface_pos] = existing_cfg.model_copy(
                update={
                    "interface_name": cfg.interface_name or existing_cfg.interface_name,
                    "interface_active": bool(cfg.interface_active),
                }
            )

        return Response(success=True)

    def reconcile_interface_inventory(self) -> Response:
        """Reconcile interface inventory and companion rows after an update-interfaces run."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_reconcile_interface_inventory(conn)

        for index, iface in enumerate(self._interfaces):
            if iface.type in {"mod", "dsc"}:
                self._interfaces[index] = iface.model_copy(
                    update={"name": f"{iface.type.upper()}-{iface.position}"}
                )
        return Response(success=True)

    def _db_get_interfaces(self, conn, *, is_interfaces: bool) -> list[Interface]:
        from backend.db import db_cursor
        interfaces: list[Interface] = []
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT interfaces.pos, interfaces.name, interfaces.type,"
                    "       interfaces.active, interfaces.multiband,"
                    "       COALESCE(config_dsc.emm, config_emm.emm, 0) AS emm_value,"
                    "       config_eqam.network_num"
                    " FROM interfaces"
                    " LEFT JOIN config_dsc"
                    "   ON config_dsc.interface_pos = interfaces.pos"
                    "  AND interfaces.type = 'dsc'"
                    " LEFT JOIN config_emm"
                    "   ON config_emm.interface_pos = interfaces.pos"
                    "  AND interfaces.type <> 'dsc'"
                    " LEFT JOIN config_eqam"
                    "   ON config_eqam.interface_pos = interfaces.pos"
                    "  AND interfaces.type = 'mod'"
                    " ORDER BY interfaces.pos;"
                )
                rows = cur.fetchall()
                for row in rows:
                    pos = row["pos"]
                    name = row["name"]
                    itype = row["type"]
                    active = bool(row["active"])
                    multiband = bool(row["multiband"]) if row["multiband"] is not None else False
                    emm = False
                    network_num = 0
                    if is_interfaces:
                        emm = (row.get("emm_value") or 0) > 0
                        if itype == "mod":
                            network_num = row.get("network_num") or 1
                    interfaces.append(Interface(
                        position=pos,
                        name=name,
                        type=itype,
                        status=None,
                        active=active,
                        emm=emm,
                        multi_band=multiband,
                        network_num=network_num,
                    ))
        except Exception as exc:
            logger.error("DB get_interfaces error: %s", exc)
        finally:
            conn.close()
        return interfaces

    def _db_set_interface_infoch(self, conn, cfg: Config) -> Response:
        from backend.db import db_cursor

        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "UPDATE interfaces SET name = %s, active = %s"
                    " WHERE pos = %s AND type = 'infoch';",
                    (cfg.interface_name, cfg.interface_active, cfg.interface_pos),
                )
                if cur.rowcount == 0:
                    return Response(
                        success=False,
                        error=f"Interface {cfg.interface_pos} is not an info channel",
                    )
            return Response(success=True)
        except Exception as exc:
            logger.error("DB set_interface_infoch error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    def _db_reconcile_interface_inventory(self, conn) -> Response:
        from backend.db import db_cursor

        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT value FROM nv WHERE name = 'hls_max_bitrate' LIMIT 1;",
                )
                hls_row = cur.fetchone()
                hls_max_bitrate = int(hls_row.get("value") or 0) if hls_row else 0

                cur.execute(
                    "SELECT pos, name, type, active FROM interfaces ORDER BY pos;",
                )
                rows = cur.fetchall()

                infostreamer_sid = 9000
                hdmi_count = 0
                hdmi_sid = 9100

                for row in rows:
                    position = row["pos"]
                    interface_type = (row.get("type") or "").lower()
                    current_name = row.get("name")

                    replacement_name = self._inventory_display_name(
                        position,
                        interface_type,
                        current_name,
                        hdmi_index=hdmi_count + 1 if interface_type in {"dvbhdmi", "hdmi2ip"} else None,
                    )
                    if replacement_name != current_name:
                        cur.execute(
                            "UPDATE interfaces SET name = %s WHERE pos = %s;",
                            (replacement_name, position),
                        )

                    if interface_type == "hls2ip":
                        cur.execute(
                            "INSERT INTO config_hls (interface_pos, max_bitrate)"
                            " SELECT %s, %s"
                            " WHERE NOT EXISTS"
                            "   (SELECT 1 FROM config_hls WHERE interface_pos = %s);",
                            (position, hls_max_bitrate, position),
                        )
                    elif interface_type == "infostreamer":
                        cur.execute(
                            "INSERT INTO config_istr (interface_pos, presentation_url)"
                            " SELECT %s, ''"
                            " WHERE NOT EXISTS"
                            "   (SELECT 1 FROM config_istr WHERE interface_pos = %s);",
                            (position, position),
                        )
                        self._insert_default_infostreamer_services(
                            cur,
                            position,
                            infostreamer_sid,
                        )
                        infostreamer_sid += 5
                    elif interface_type == "dsc":
                        cur.execute(
                            "INSERT INTO config_dsc (interface_pos, emm)"
                            " SELECT %s, 1"
                            " WHERE NOT EXISTS"
                            "   (SELECT 1 FROM config_dsc WHERE interface_pos = %s);",
                            (position, position),
                        )
                    elif interface_type == "mod":
                        cur.execute(
                            "INSERT INTO config_eqam (interface_pos, network_num)"
                            " SELECT %s, 1"
                            " WHERE NOT EXISTS"
                            "   (SELECT 1 FROM config_eqam WHERE interface_pos = %s);",
                            (position, position),
                        )
                    elif interface_type in {"dvbhdmi", "hdmi2ip"}:
                        hdmi_count += 1
                        hdmi_sid += 1
                        cur.execute(
                            "INSERT INTO config_hdmi (interface_pos, format)"
                            " SELECT %s, 'auto'"
                            " WHERE NOT EXISTS"
                            "   (SELECT 1 FROM config_hdmi WHERE interface_pos = %s);",
                            (position, position),
                        )
                        self._insert_default_hdmi_service(
                            cur,
                            position,
                            hdmi_count,
                            hdmi_sid,
                        )

                cur.execute(
                    "UPDATE services"
                    " SET enable = FALSE"
                    " FROM interfaces"
                    " WHERE interfaces.pos = services.interface_pos"
                    "   AND interfaces.type IN ('mod', 'dsc')"
                    "   AND services.enable IS DISTINCT FROM FALSE;",
                )

            return Response(success=True)
        except Exception as exc:
            logger.error("DB reconcile_interface_inventory error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    def _inventory_display_name(
        self,
        position: str,
        interface_type: str,
        current_name: str | None,
        *,
        hdmi_index: int | None = None,
    ) -> str:
        name = (current_name or "").strip()
        placeholder = name in {"", "name?", "none"}

        if interface_type in {"mod", "dsc"}:
            return f"{interface_type.upper()}-{position}"
        if interface_type == "hls2ip" and placeholder:
            return f"HLS Output {position.upper()}"
        if interface_type in {"dvbhdmi", "hdmi2ip"} and placeholder:
            return f"Hdmi {hdmi_index or position.upper()}"
        if interface_type == "infostreamer" and placeholder:
            return f"Info and Radio {position.upper()}"
        if interface_type == "webradio" and placeholder:
            return f"Webradio {position.upper()}"
        return current_name or position

    def _insert_default_infostreamer_services(self, cur, position: str, start_sid: int) -> None:
        cur.execute(
            "SELECT 1 FROM services WHERE interface_pos = %s LIMIT 1;",
            (position,),
        )
        if cur.fetchone() is not None:
            return

        for offset in range(5):
            sid = start_sid + offset
            cur.execute(
                "INSERT INTO services"
                " (interface_pos, name, sid, type, lang, all_langs, enable, istr_url, istr_video, scrambled, key)"
                " VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);",
                (
                    position,
                    f"Info and Radio {offset + 1}",
                    sid,
                    "RADIO",
                    "",
                    [],
                    False,
                    "",
                    False,
                    False,
                    f"{position}-{sid}",
                ),
            )

    def _insert_default_hdmi_service(
        self,
        cur,
        position: str,
        hdmi_index: int,
        sid: int,
    ) -> None:
        cur.execute(
            "SELECT 1 FROM services WHERE interface_pos = %s LIMIT 1;",
            (position,),
        )
        if cur.fetchone() is not None:
            return

        cur.execute(
            "INSERT INTO services"
            " (interface_pos, name, sid, type, lang, all_langs, enable, istr_url, istr_video, scrambled, key)"
            " VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);",
            (
                position,
                f"Hdmi {hdmi_index}",
                sid,
                "TV_HD",
                "",
                [],
                False,
                None,
                False,
                False,
                f"{position}-{sid}",
            ),
        )

    # ------------------------------------------------------------------
    # Configs
    # ------------------------------------------------------------------

    def get_config(self, interface_pos: str, interface_type: str) -> Config | None:
        """Return tuning config for the given interface position."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_config(conn, interface_pos, interface_type)
        return self._configs.get(interface_pos)

    def set_config(self, cfg: Config, interface_type: str) -> Response:
        """Persist an updated interface config."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_set_config(conn, cfg, interface_type)
        self._configs[cfg.interface_pos] = cfg
        self._config_changed = True
        return Response(success=True)

    def _db_get_config(self, conn, interface_pos: str, interface_type: str) -> Config | None:
        from backend.db import db_cursor
        table = _iface_type_to_table(interface_type)
        cfg: Config | None = None
        try:
            with db_cursor(conn) as cur:
                # Base interface info
                cur.execute(
                    "SELECT name, active FROM interfaces WHERE pos = %s;",
                    (interface_pos,),
                )
                iface_row = cur.fetchone()
                iface_name = iface_row["name"] if iface_row else None
                iface_active = bool(iface_row["active"]) if iface_row else True
                emm_value = 0
                if _uses_emm_slot_table(interface_type):
                    emm_value = self._db_get_config_emm_value(cur, interface_pos)

                if table is None:
                    return Config(
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                    )

                from psycopg2 import sql as pgsql
                cur.execute(
                    pgsql.SQL("SELECT * FROM {} WHERE interface_pos = %s;").format(
                        pgsql.Identifier(table)
                    ),
                    (interface_pos,),
                )
                row = cur.fetchone()
                if row is None:
                    default_config = Config(
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        emm=emm_value,
                    )
                    if table == "config_dvbudp":
                        return default_config.model_copy(
                            update={
                                "in_ip": "0.0.0.0",
                                "in_port": 10000,
                            }
                        )
                    return default_config

                row_id = row.get("id", 0) or 0

                if table == "config_sat":
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        freq=row.get("freq") or 0,
                        pol=row.get("pol"),
                        symb=row.get("symb") or 0,
                        del_sys=row.get("del"),
                        satno=row.get("satno") or 0,
                        lnb_type=row.get("lnb_type"),
                        emm=emm_value,
                    )
                elif table == "config_ter":
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        freq=row.get("freq") or 0,
                        bw=row.get("bw") or 0,
                        del_sys=row.get("del"),
                        emm=emm_value,
                    )
                elif table == "config_dvbc":
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        freq=row.get("freq") or 0,
                        symb=row.get("symb") or 0,
                        del_sys=row.get("del"),
                        constellation=row.get("constellation"),
                        emm=emm_value,
                    )
                elif table == "config_dvbudp":
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        in_ip=row.get("in_ip"),
                        in_port=row.get("in_port") or 0,
                        emm=emm_value,
                    )
                elif table == "config_dsc":
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        emm=row.get("emm") or 0,
                    )
                elif table == "config_hdmi":
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        hdmi_format=row.get("format"),
                    )
                elif table == "config_hls":
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        max_bitrate=row.get("max_bitrate") or 0,
                    )
                elif table == "config_istr":
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        pres_url=row.get("presentation_url"),
                    )
                elif table == "config_webradio":
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        gain=row.get("gain") or 0,
                        webradio_url=row.get("webradio_url"),
                    )
        except Exception as exc:
            logger.error("DB get_config error: %s", exc)
            cfg = None
        finally:
            conn.close()
        return cfg

    def _db_get_config_emm_value(self, cur, interface_pos: str) -> int:
        cur.execute(
            "SELECT emm FROM config_emm WHERE interface_pos = %s LIMIT 1;",
            (interface_pos,),
        )
        row = cur.fetchone()
        return int(row.get("emm") or 0) if row else 0

    def _db_assign_config_emm_value(self, cur, interface_pos: str, emm_value: int) -> None:
        cur.execute(
            "UPDATE config_emm SET interface_pos = NULL WHERE interface_pos = %s;",
            (interface_pos,),
        )
        if emm_value <= 0:
            return

        cur.execute(
            "SELECT interface_pos FROM config_emm WHERE emm = %s LIMIT 1;",
            (emm_value,),
        )
        row = cur.fetchone()
        if row is None:
            cur.execute(
                "INSERT INTO config_emm (emm, interface_pos) VALUES (%s, %s);",
                (emm_value, interface_pos),
            )
            return

        assigned_interface = row.get("interface_pos")
        if assigned_interface and assigned_interface != interface_pos:
            raise ValueError(f"EMM slot {emm_value} is already in use by {assigned_interface}")

        cur.execute(
            "UPDATE config_emm SET interface_pos = %s WHERE emm = %s;",
            (interface_pos, emm_value),
        )

    def _db_set_config(self, conn, cfg: Config, interface_type: str) -> Response:
        from backend.db import db_cursor
        table = _iface_type_to_table(interface_type)
        try:
            with db_cursor(conn) as cur:
                # Update interface name/active flag
                cur.execute(
                    "UPDATE interfaces SET name = %s, active = %s WHERE pos = %s;",
                    (cfg.interface_name, cfg.interface_active, cfg.interface_pos),
                )

                if table is None:
                    return Response(success=True)

                pos = cfg.interface_pos

                if table == "config_sat":
                    cur.execute(
                        "DELETE FROM config_sat WHERE interface_pos = %s;",
                        (pos,),
                    )
                    cur.execute(
                        "INSERT INTO config_sat"
                        " (interface_pos, freq, pol, symb, del, satno, lnb_type)"
                        " VALUES (%s, %s, %s, %s, %s, %s, %s);",
                        (pos, cfg.freq, cfg.pol, cfg.symb, cfg.del_sys,
                         cfg.satno, cfg.lnb_type),
                    )

                elif table == "config_ter":
                    cur.execute(
                        "INSERT INTO config_ter (interface_pos, freq, bw, del)"
                        " SELECT %s, %s, %s, %s"
                        " WHERE NOT EXISTS"
                        "   (SELECT 1 FROM config_ter WHERE interface_pos = %s);",
                        (pos, cfg.freq, cfg.bw, cfg.del_sys, pos),
                    )
                    cur.execute(
                        "UPDATE config_ter"
                        " SET freq = %s, bw = %s, del = %s"
                        " WHERE interface_pos = %s;",
                        (cfg.freq, cfg.bw, cfg.del_sys, pos),
                    )

                elif table == "config_dvbc":
                    cur.execute(
                        "INSERT INTO config_dvbc"
                        " (interface_pos, freq, symb, del, constellation)"
                        " SELECT %s, %s, %s, %s, %s"
                        " WHERE NOT EXISTS"
                        "   (SELECT 1 FROM config_dvbc WHERE interface_pos = %s);",
                        (pos, cfg.freq, cfg.symb, cfg.del_sys,
                         cfg.constellation, pos),
                    )
                    cur.execute(
                        "UPDATE config_dvbc"
                        " SET freq = %s, symb = %s, del = %s, constellation = %s"
                        " WHERE interface_pos = %s;",
                        (cfg.freq, cfg.symb, cfg.del_sys, cfg.constellation, pos),
                    )

                elif table == "config_dvbudp":
                    in_ip = cfg.in_ip or "0.0.0.0"
                    in_port = int(cfg.in_port or 10000)
                    cur.execute(
                        "INSERT INTO config_dvbudp (interface_pos, in_ip, in_port)"
                        " SELECT %s, %s, %s"
                        " WHERE NOT EXISTS"
                        "   (SELECT 1 FROM config_dvbudp WHERE interface_pos = %s);",
                        (pos, in_ip, in_port, pos),
                    )
                    cur.execute(
                        "UPDATE config_dvbudp"
                        " SET in_ip = %s, in_port = %s"
                        " WHERE interface_pos = %s;",
                        (in_ip, in_port, pos),
                    )

                elif table == "config_dsc":
                    cur.execute(
                        "DELETE FROM config_dsc WHERE interface_pos = %s;",
                        (pos,),
                    )
                    if cfg.emm:
                        cur.execute(
                            "INSERT INTO config_dsc (interface_pos, emm)"
                            " VALUES (%s, %s);",
                            (pos, cfg.emm),
                        )

                elif table == "config_hdmi":
                    cur.execute(
                        "DELETE FROM config_hdmi WHERE interface_pos = %s;",
                        (pos,),
                    )
                    cur.execute(
                        "INSERT INTO config_hdmi (interface_pos, format)"
                        " VALUES (%s, %s);",
                        (pos, cfg.hdmi_format or "1080i50"),
                    )

                elif table == "config_hls":
                    cur.execute(
                        "DELETE FROM config_hls WHERE interface_pos = %s;",
                        (pos,),
                    )
                    cur.execute(
                        "INSERT INTO config_hls (interface_pos, max_bitrate)"
                        " VALUES (%s, %s);",
                        (pos, cfg.max_bitrate),
                    )

                elif table == "config_istr":
                    cur.execute(
                        "DELETE FROM config_istr WHERE interface_pos = %s;",
                        (pos,),
                    )
                    cur.execute(
                        "INSERT INTO config_istr (interface_pos, presentation_url)"
                        " VALUES (%s, %s);",
                        (pos, cfg.pres_url),
                    )

                elif table == "config_webradio":
                    cur.execute(
                        "DELETE FROM config_webradio WHERE interface_pos = %s;",
                        (pos,),
                    )
                    cur.execute(
                        "INSERT INTO config_webradio"
                        " (interface_pos, gain, webradio_url)"
                        " VALUES (%s, %s, %s);",
                        (pos, cfg.gain, cfg.webradio_url),
                    )

                if _uses_emm_slot_table(interface_type):
                    self._db_assign_config_emm_value(cur, pos, int(cfg.emm or 0))

                self._set_config_changed_value(cur, True)

            return Response(success=True)
        except Exception as exc:
            logger.error("DB set_config error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    # ------------------------------------------------------------------
    # Services
    # ------------------------------------------------------------------

    def get_services(self, interface_pos: str) -> list[Service]:
        """Return services belonging to the given interface."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_services(conn, interface_pos)
        return [
            service.model_copy(update={"type": _normalize_service_type(service.type)}, deep=True)
            for service in self._services
            if service.interface_pos == interface_pos
        ]

    def save_services(
        self,
        services: list[Service],
        interface_type: str,
        interface_pos: str,
    ) -> Response:
        """Replace services for the given interface."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_save_services(conn, services, interface_type, interface_pos)
        self._services = [
            s for s in self._services if s.interface_pos != interface_pos
        ]
        self._services.extend(
            [
                service.model_copy(update={"type": _normalize_service_type(service.type)}, deep=True)
                for service in services
            ]
        )
        self._config_changed = True
        return Response(success=True)

    def _db_get_services(self, conn, interface_pos: str) -> list[Service]:
        from backend.db import db_cursor
        services: list[Service] = []
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT * FROM services WHERE interface_pos = %s;",
                    (interface_pos,),
                )
                for row in cur.fetchall():
                    all_langs_raw = row.get("all_langs")
                    if all_langs_raw:
                        all_langs = list(all_langs_raw)
                    else:
                        all_langs = ["All"]
                    services.append(Service(
                        id=row["id"],
                        interface_pos=row["interface_pos"],
                        name=row["name"],
                        sid=row["sid"],
                        type=_normalize_service_type(row["type"]),
                        lang=row.get("lang") or "eng",
                        enabled=bool(row.get("enable", True)),
                        all_langs=all_langs,
                        radio_url=row.get("istr_url"),
                        show_pres=bool(row.get("istr_video", False)),
                        scrambled=bool(row.get("scrambled", False)),
                        epg_url=row.get("epg_id"),
                        key=row.get("key"),
                        hls_url=row.get("hls_url"),
                        webradio_url=row.get("webradio_url"),
                        found=True,
                    ))
        except Exception as exc:
            logger.error("DB get_services error: %s", exc)
        finally:
            conn.close()
        return services

    def _db_save_services(
        self,
        conn,
        services: list[Service],
        interface_type: str,
        interface_pos: str,
    ) -> Response:
        from backend.db import db_cursor
        if not services:
            return Response(success=True)
        try:
            with db_cursor(conn) as cur:
                # Delete existing services for this interface
                cur.execute(
                    "DELETE FROM services WHERE interface_pos = %s;",
                    (interface_pos,),
                )

                # Determine start IP for route insertion
                cur.execute(
                    "SELECT value FROM nv WHERE name = 'ip_startaddr';",
                )
                ip_row = cur.fetchone()
                ip_startaddr = (ip_row["value"] if ip_row else None) or "239.1.1.1:10000"
                address_port = ip_startaddr.split(":")
                ip_port = address_port[1] if len(address_port) > 1 else "10000"
                octets = address_port[0].split(".")

                for svc in services:
                    all_langs = list(svc.all_langs) if svc.all_langs else []

                    # Adapt type for infostreamer
                    svc_type = _normalize_service_type(svc.type)
                    if interface_type == "infostreamer":
                        svc_type = "TV_SD" if svc.show_pres else "RADIO"
                    if interface_type == "infoch":
                        svc.radio_url = svc.webradio_url
                        svc.webradio_url = ""

                    cur.execute(
                        "INSERT INTO services"
                        " (interface_pos, name, sid, type, lang, all_langs,"
                        "  enable, istr_url, istr_video, key, scrambled,"
                        "  hls_url, webradio_url, epg_id)"
                        " VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s,"
                        "         %s, %s, %s, %s);",
                        (
                            svc.interface_pos, svc.name, svc.sid, svc_type,
                            svc.lang or "eng", all_langs,
                            svc.enabled,
                            svc.radio_url, svc.show_pres,
                            svc.key,
                            svc.scrambled,
                            svc.hls_url, svc.webradio_url, svc.epg_url,
                        ),
                    )

                    if svc.enabled and svc.key:
                        sid = svc.sid
                        num = (sid + 255) % 255
                        out_ip = (
                            f"{octets[0]}.{octets[1]}.{octets[2]}.{num}"
                            f":{ip_port}"
                        ) if len(octets) >= 3 else f"239.1.1.{num}:{ip_port}"

                        cur.execute(
                            "INSERT INTO routes"
                            " (service_key, lcn, dsc_pos, mod_pos,"
                            "  out_sid, out_ip, output_name, epg_id)"
                            " SELECT %s, 0, 'None', 'None', %s, %s, %s, %s"
                            " WHERE NOT EXISTS"
                            "   (SELECT 1 FROM routes WHERE service_key = %s);",
                            (svc.key, sid, out_ip, svc.name, svc.epg_url or "", svc.key),
                        )

                self._set_config_changed_value(cur, True)

            return Response(success=True)
        except Exception as exc:
            logger.error("DB save_services error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    # ------------------------------------------------------------------
    # Routes
    # ------------------------------------------------------------------

    def get_routes(self) -> list[Route]:
        """Return all output routes."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_routes(conn)
        return [
            route.model_copy(update={"service_type": _normalize_service_type(route.service_type)}, deep=True)
            for route in self._routes
        ]

    def update_routes(self, routes: list[Route]) -> Response:
        """Replace all routes."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_update_routes(conn, routes)
        self._routes = routes
        self._config_changed = True
        return Response(success=True)

    def _db_get_routes(self, conn) -> list[Route]:
        from backend.db import db_cursor
        routes: list[Route] = []
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT routes.*, services.name AS service_name,"
                    "       services.id AS service_id,"
                    "       services.type AS service_type,"
                    "       services.scrambled,"
                    "       services.interface_pos,"
                    "       interfaces.type,"
                    "       interfaces.multiband"
                    " FROM routes"
                    " INNER JOIN services ON routes.service_key = services.key"
                    " INNER JOIN interfaces"
                    "   ON services.interface_pos = interfaces.pos"
                    " WHERE services.enable IS TRUE"
                    "   AND interfaces.active IS TRUE"
                    "   AND services.interface_pos IN (SELECT pos FROM interfaces)"
                    " ORDER BY lcn;",
                )
                for row in cur.fetchall():
                    routes.append(Route(
                        id=row["id"],
                        service_id=row.get("service_id") or 0,
                        service_name=row.get("service_name") or "",
                        service_type=_normalize_service_type(row.get("service_type")),
                        interface_pos=row.get("interface_pos") or "",
                        interface_type=row.get("type") or "",
                        lcn=row.get("lcn") or 0,
                        descrambler_pos=row.get("dsc_pos"),
                        modulator_pos=row.get("mod_pos"),
                        modulator_pos_net2=row.get("mod_pos_net2"),
                        out_sid=row.get("out_sid") or 0,
                        out_ip=row.get("out_ip"),
                        scrambled=bool(row.get("scrambled", False)),
                        output_name=row.get("output_name"),
                        epg_url=row.get("epg_id"),
                        hls_enable=bool(row.get("hls_enable", False)),
                        interface_multiband=bool(row.get("multiband", False)),
                    ))
        except Exception as exc:
            logger.error("DB get_routes error: %s", exc)
        finally:
            conn.close()
        return routes

    def _db_update_routes(self, conn, routes: list[Route]) -> Response:
        from backend.db import db_cursor
        try:
            with db_cursor(conn) as cur:
                for route in routes:
                    cur.execute(
                        "UPDATE routes SET"
                        " lcn = %s,"
                        " dsc_pos = %s,"
                        " mod_pos = %s,"
                        " mod_pos_net2 = %s,"
                        " out_sid = %s,"
                        " out_ip = %s,"
                        " output_name = %s,"
                        " epg_id = %s,"
                        " hls_enable = %s"
                        " WHERE id = %s;",
                        (
                            route.lcn,
                            route.descrambler_pos,
                            route.modulator_pos,
                            route.modulator_pos_net2,
                            route.out_sid,
                            route.out_ip,
                            route.output_name,
                            route.epg_url or "",
                            route.hls_enable,
                            route.id,
                        ),
                    )
                self._set_config_changed_value(cur, True)
            return Response(success=True)
        except Exception as exc:
            logger.error("DB update_routes error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    # ------------------------------------------------------------------
    # Settings (nv table)
    # ------------------------------------------------------------------

    def get_settings(self) -> list[NameValue]:
        """Return all system settings."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_settings(conn)
        return list(self._settings)

    def update_settings(self, settings: list[NameValue]) -> Response:
        """Upsert the supplied settings."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_update_settings(conn, settings)
        self._settings = self._merge_settings(self._settings, settings)
        self._config_changed = True
        return Response(success=True)

    def _db_get_settings(self, conn) -> list[NameValue]:
        from backend.db import db_cursor
        settings: list[NameValue] = []
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT id, name, value FROM nv"
                    " WHERE name != 'config_changed'"
                    " ORDER BY id;",
                )
                for row in cur.fetchall():
                    settings.append(NameValue(
                        id=row["id"],
                        name=row["name"],
                        value=row.get("value"),
                    ))
        except Exception as exc:
            logger.error("DB get_settings error: %s", exc)
        finally:
            conn.close()
        return settings

    def _db_update_settings(self, conn, settings: list[NameValue]) -> Response:
        from backend.db import db_cursor
        try:
            with db_cursor(conn) as cur:
                for nv in settings:
                    if nv.id:
                        cur.execute(
                            "UPDATE nv SET value = %s WHERE id = %s;",
                            (nv.value, nv.id),
                        )
                        if cur.rowcount == 0:
                            cur.execute(
                                "UPDATE nv SET value = %s WHERE name = %s;",
                                (nv.value, nv.name),
                            )
                    else:
                        cur.execute(
                            "UPDATE nv SET value = %s WHERE name = %s;",
                            (nv.value, nv.name),
                        )
                    if cur.rowcount == 0:
                        cur.execute(
                            "INSERT INTO nv (name, value) VALUES (%s, %s);",
                            (nv.name, nv.value),
                        )
                self._set_config_changed_value(cur, True)
            return Response(success=True)
        except Exception as exc:
            logger.error("DB update_settings error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    def _merge_settings(
        self,
        existing: list[NameValue],
        updates: list[NameValue],
    ) -> list[NameValue]:
        merged = [item.model_copy(deep=True) for item in existing]
        by_id = {item.id: index for index, item in enumerate(merged) if item.id}
        by_name = {item.name: index for index, item in enumerate(merged)}
        next_id = max((item.id for item in merged), default=0) + 1

        for update in updates:
            new_item = update.model_copy(deep=True)
            target_index = None
            if new_item.id and new_item.id in by_id:
                target_index = by_id[new_item.id]
            elif new_item.name in by_name:
                target_index = by_name[new_item.name]

            if target_index is None:
                if new_item.id == 0:
                    new_item = new_item.model_copy(update={"id": next_id})
                    next_id += 1
                merged.append(new_item)
                index = len(merged) - 1
                if new_item.id:
                    by_id[new_item.id] = index
                by_name[new_item.name] = index
                continue

            current = merged[target_index]
            merged[target_index] = current.model_copy(
                update={
                    "value": new_item.value,
                    "name": new_item.name,
                    "id": current.id or new_item.id,
                }
            )

        return merged

    def get_settings_map(self) -> dict[str, NameValue]:
        """Return all settings indexed by name."""
        return {setting.name: setting for setting in self.get_settings()}

    def get_network_settings(self) -> list[NameValue]:
        """Return network-related settings."""
        return [setting for setting in self.get_settings() if setting.name.startswith("nw_")]

    def update_network_settings(self, body: NetworkSettingsUpdateRequest) -> Response:
        """Update editable network settings and optionally apply them."""
        response = self.update_settings(body.settings)
        if not response.success or not body.apply_changes:
            return response
        return self.run_command("wnet")

    def get_datetime_state(self) -> DateTimeState:
        """Return date/time state used by the settings UI."""
        settings = self.get_settings_map()
        timezone_name = (
            settings.get("timezone").value
            if settings.get("timezone") and settings.get("timezone").value
            else "UTC"
        )
        ntp_enabled = _as_bool(
            settings.get("ntp_enable").value if settings.get("ntp_enable") else True
        )
        current_dt = _datetime_for_timezone(timezone_name)
        current_date = current_dt.strftime("%Y-%m-%d")
        current_time = current_dt.strftime("%H:%M")

        if not ntp_enabled:
            if settings.get("clock_date") and settings["clock_date"].value:
                current_date = settings["clock_date"].value or current_date
            if settings.get("clock_time") and settings["clock_time"].value:
                current_time = settings["clock_time"].value or current_time

        return DateTimeState(
            timezone=timezone_name,
            ntp_enabled=ntp_enabled,
            current_date=current_date,
            current_time=current_time,
            timezones=_list_timezones(timezone_name),
        )

    def save_datetime(self, body: DateTimeUpdateRequest) -> Response:
        """Persist date/time settings in the compatibility layer."""
        settings = self.get_settings_map()
        updates = [
            NameValue(
                id=settings.get("timezone").id if settings.get("timezone") else 0,
                name="timezone",
                value=body.timezone,
            ),
            NameValue(
                id=settings.get("ntp_enable").id if settings.get("ntp_enable") else 0,
                name="ntp_enable",
                value="true" if body.ntp_enabled else "false",
            ),
        ]

        if not body.ntp_enabled:
            updates.extend(
                [
                    NameValue(
                        id=settings.get("clock_date").id if settings.get("clock_date") else 0,
                        name="clock_date",
                        value=body.date,
                    ),
                    NameValue(
                        id=settings.get("clock_time").id if settings.get("clock_time") else 0,
                        name="clock_time",
                        value=body.time,
                    ),
                ]
            )

        return self.update_settings(updates)

    def get_modulators(self) -> list[ModulatorAssignment]:
        """Return modulator network assignments."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_modulators(conn)

        modulators: list[ModulatorAssignment] = []
        for iface in self.get_interfaces(is_interfaces=False):
            if iface.type != "mod":
                continue
            modulators.append(
                ModulatorAssignment(
                    interface_pos=iface.position,
                    network_num=iface.network_num if iface.network_num in {1, 2} else 0,
                )
            )
        return modulators

    def _db_get_modulators(self, conn) -> list[ModulatorAssignment]:
        from backend.db import db_cursor

        assignments: list[ModulatorAssignment] = []
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT pos FROM interfaces WHERE type = 'mod' ORDER BY pos;",
                )
                rows = cur.fetchall()
                for row in rows:
                    pos = row["pos"]
                    cur.execute(
                        "SELECT network_num FROM config_eqam WHERE interface_pos = %s;",
                        (pos,),
                    )
                    eqam_row = cur.fetchone()
                    network_num = eqam_row["network_num"] if eqam_row else 0
                    assignments.append(
                        ModulatorAssignment(interface_pos=pos, network_num=network_num)
                    )
        except Exception as exc:
            logger.error("DB get_modulators error: %s", exc)
        finally:
            conn.close()
        return assignments

    def save_modulators_config(self, assignments: list[ModulatorAssignment]) -> Response:
        """Persist modulator network assignments."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_save_modulators_config(conn, assignments)

        for assignment in assignments:
            for index, iface in enumerate(self._interfaces):
                if iface.position != assignment.interface_pos:
                    continue
                self._interfaces[index] = iface.model_copy(
                    update={"network_num": assignment.network_num}
                )
        self._config_changed = True
        return Response(success=True)

    def _db_save_modulators_config(
        self,
        conn,
        assignments: list[ModulatorAssignment],
    ) -> Response:
        from backend.db import db_cursor

        try:
            with db_cursor(conn) as cur:
                for assignment in assignments:
                    cur.execute(
                        "UPDATE config_eqam SET network_num = %s WHERE interface_pos = %s;",
                        (assignment.network_num, assignment.interface_pos),
                    )
                    if cur.rowcount == 0:
                        cur.execute(
                            "INSERT INTO config_eqam (interface_pos, network_num) VALUES (%s, %s);",
                            (assignment.interface_pos, assignment.network_num),
                        )
                self._set_config_changed_value(cur, True)
            return Response(success=True)
        except Exception as exc:
            logger.error("DB save_modulators_config error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    def get_config_changed(self) -> bool:
        """Return whether configuration changes are waiting to be applied."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_config_changed(conn)
        return self._config_changed

    def clear_config_changed(self) -> None:
        """Clear the pending configuration-changed marker."""
        conn = self._get_connection()
        if conn is not None:
            self._db_set_config_changed(conn, False)
            return
        self._config_changed = False

    def _db_get_config_changed(self, conn) -> bool:
        from backend.db import db_cursor

        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT value FROM nv WHERE name = 'config_changed' LIMIT 1;",
                )
                row = cur.fetchone()
                return _as_bool(row.get("value")) if row else False
        except Exception as exc:
            logger.error("DB get_config_changed error: %s", exc)
            return False
        finally:
            conn.close()

    def _db_set_config_changed(self, conn, changed: bool) -> None:
        from backend.db import db_cursor

        try:
            with db_cursor(conn) as cur:
                self._set_config_changed_value(cur, changed)
        except Exception as exc:
            logger.error("DB set_config_changed error: %s", exc)
        finally:
            conn.close()

    def _set_config_changed_value(self, cur, changed: bool) -> None:
        value = "true" if changed else "false"
        cur.execute(
            "UPDATE nv SET value = %s WHERE name = 'config_changed';",
            (value,),
        )
        if cur.rowcount == 0:
            cur.execute(
                "INSERT INTO nv (name, value) VALUES ('config_changed', %s);",
                (value,),
            )

    # ------------------------------------------------------------------
    # Unit info  (reads nv table keys: ui_serial, ui_swversion, nw_hostname)
    # ------------------------------------------------------------------

    def get_unit_info(self) -> UnitInfo:
        """Return unit hardware/feature information."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_unit_info(conn)
        return UnitInfo(
            serial="IXU-2024-001",
            version="3.2.1",
            hostname="ixui-unit-1",
            cloud=config.enable_cloud,
            forced_content=config.enable_forced_content,
            software_update=config.enable_software_update,
            hls_output=config.enable_hls_output,
            portal=config.enable_portal,
        )

    def _db_get_unit_info(self, conn) -> UnitInfo:
        from backend.db import db_cursor
        serial = None
        version = None
        hostname = None
        feature_values: dict[str, str | None] = {}
        try:
            with db_cursor(conn) as cur:
                cur.execute("SELECT name, value FROM nv;")
                for row in cur.fetchall():
                    name = row["name"]
                    value = row.get("value")
                    if name == "ui_serial":
                        serial = value
                    elif name == "ui_swversion":
                        version = value
                    elif name == "nw_hostname":
                        hostname = value
                    elif name in {
                        "ixcloud_enable",
                        "forced_content_enable",
                        "hls_enable",
                        "portal_enable",
                    }:
                        feature_values[name] = value
        except Exception as exc:
            logger.error("DB get_unit_info error: %s", exc)
        finally:
            conn.close()
        return UnitInfo(
            serial=serial or "",
            version=version or "",
            hostname=hostname or "",
            cloud=_as_bool(feature_values.get("ixcloud_enable", config.enable_cloud)),
            forced_content=_as_bool(
                feature_values.get("forced_content_enable", config.enable_forced_content)
            ),
            software_update=config.enable_software_update,
            hls_output=_as_bool(feature_values.get("hls_enable", config.enable_hls_output)),
            portal=_as_bool(feature_values.get("portal_enable", config.enable_portal)),
        )

    # ------------------------------------------------------------------
    # Network status / editable network settings
    # ------------------------------------------------------------------

    def get_network_status(self) -> list[IpMac]:
        """Return live-or-configured IP/MAC list for Ethernet devices."""
        settings = self.get_settings_map()
        status: list[IpMac] = []
        index = 0
        live_interfaces = self._get_live_network_interfaces()
        ordered_live_interfaces = self._ordered_live_network_interfaces(live_interfaces)

        while True:
            onboot_name = f"nw_eth{index}_onboot"
            if onboot_name not in settings:
                break
            configured_ip = settings.get(f"nw_eth{index}_ipaddr").value if settings.get(f"nw_eth{index}_ipaddr") else ""
            configured_mac = settings.get(f"nw_eth{index}_mac").value if settings.get(f"nw_eth{index}_mac") else ""
            live_interface = live_interfaces.get(f"eth{index}")
            if live_interface is None and index < len(ordered_live_interfaces):
                live_interface = ordered_live_interfaces[index]
            live_interface = live_interface or {}
            status.append(
                IpMac(
                    ip=live_interface.get("ip") or configured_ip,
                    mac=live_interface.get("mac") or configured_mac,
                    status=live_interface.get("status") or ("configured" if configured_ip or configured_mac else "unknown"),
                )
            )
            index += 1

        return status or copy.deepcopy(_NETWORK_HOSTS)

    def _get_live_network_interfaces(self) -> dict[str, dict[str, str]]:
        if psutil is None:
            return {}

        try:
            addresses = psutil.net_if_addrs()
            stats = psutil.net_if_stats()
        except Exception as exc:
            logger.warning("Failed to read live network interfaces: %s", exc)
            return {}

        result: dict[str, dict[str, str]] = {}
        for name, interface_addrs in addresses.items():
            ipv4 = ""
            mac = ""
            for address in interface_addrs:
                value = getattr(address, "address", "") or ""
                if getattr(address, "family", None) == socket.AF_INET and not ipv4:
                    ipv4 = value
                elif not mac and self._looks_like_mac_address(value):
                    mac = value

            interface_stats = stats.get(name)
            result[name] = {
                "ip": ipv4,
                "mac": mac,
                "status": "up" if interface_stats and interface_stats.isup else "down",
            }

        return result

    def _ordered_live_network_interfaces(
        self,
        live_interfaces: dict[str, dict[str, str]],
    ) -> list[dict[str, str]]:
        ordered = sorted(
            live_interfaces.items(),
            key=lambda item: (
                0 if item[0].lower().startswith("eth") else 1,
                0 if item[1].get("ip") else 1,
                item[0].lower(),
            ),
        )
        return [
            details
            for name, details in ordered
            if not self._is_loopback_interface(name, details)
        ]

    def _is_loopback_interface(self, name: str, details: dict[str, str]) -> bool:
        normalized_name = name.lower()
        if normalized_name.startswith("lo") or "loopback" in normalized_name:
            return True

        ip_address = details.get("ip") or ""
        return ip_address.startswith("127.")

    def _looks_like_mac_address(self, value: str | None) -> bool:
        if not value:
            return False

        normalized = value.replace("-", ":")
        parts = normalized.split(":")
        return len(parts) == 6 and all(len(part) in {1, 2} for part in parts)

    def get_network_status2(self) -> dict[str, IpStatus]:
        """Return enriched reachability status keyed by legacy names."""
        settings = self.get_settings_map()
        public_ip = "8.8.8.8"
        if settings.get("ixcloud_ping_ip") and settings["ixcloud_ping_ip"].value:
            public_ip = settings["ixcloud_ping_ip"].value or public_ip
        elif settings.get("ixcloud_pingip") and settings["ixcloud_pingip"].value:
            public_ip = settings["ixcloud_pingip"].value or public_ip

        targets = {
            "gateway": settings.get("nw_gateway").value if settings.get("nw_gateway") else None,
            "dns1": settings.get("nw_dns1").value if settings.get("nw_dns1") else None,
            "dns2": settings.get("nw_dns2").value if settings.get("nw_dns2") else None,
            "public": public_ip,
        }

        with ThreadPoolExecutor(max_workers=len(targets)) as executor:
            futures = {
                name: executor.submit(self._probe_network_host, name, ip)
                for name, ip in targets.items()
            }
            result: dict[str, IpStatus] = {}
            for name in targets:
                try:
                    result[name] = futures[name].result()
                except Exception as exc:
                    logger.warning("Network reachability probe failed for %s: %s", name, exc)
                    value = (targets.get(name) or "").strip()
                    result[name] = IpStatus(
                        ip=value,
                        mac="",
                        status="unknown" if value else "offline",
                    )
            return result

    def _probe_network_host(self, name: str, ip: str | None) -> IpStatus:
        value = (ip or "").strip()
        if not value:
            return IpStatus(ip="", mac="", status="offline")

        reachable = self._ping_host(value)
        if reachable is None:
            reachable = self._probe_network_host_via_socket(name, value)

        if reachable is None:
            status = "unknown"
        else:
            status = "online" if reachable else "offline"

        return IpStatus(ip=value, mac="", status=status)

    def _ping_host(self, host: str) -> bool | None:
        ping_executable = shutil.which("ping")
        if not ping_executable:
            return None

        timeout_ms = str(int(_NETWORK_PING_TIMEOUT_SECONDS * 1000))
        if platform.system().lower().startswith("win"):
            command = [ping_executable, "-n", "1", "-w", timeout_ms, host]
        else:
            command = [ping_executable, "-c", "1", "-W", str(int(_NETWORK_PING_TIMEOUT_SECONDS)), host]

        try:
            completed = subprocess.run(
                command,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                timeout=_NETWORK_PING_TIMEOUT_SECONDS + 0.5,
                check=False,
            )
        except (OSError, subprocess.TimeoutExpired):
            return False

        return completed.returncode == 0

    def _probe_network_host_via_socket(self, name: str, host: str) -> bool | None:
        ports = _NETWORK_FALLBACK_PORTS.get(name, ())
        if not ports:
            return None

        for port in ports:
            try:
                with socket.create_connection((host, port), timeout=_NETWORK_SOCKET_TIMEOUT_SECONDS):
                    return True
            except OSError:
                continue

        return False

    # ------------------------------------------------------------------
    # Interface types & status
    # ------------------------------------------------------------------

    def get_interface_types(self) -> list[str]:
        """Return known interface type identifiers."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_interface_types(conn)
        return ["dvbs", "dvbt", "dvbc", "ip", "hdmi", "asi"]

    def update_interface_multiband_type(self, interface_pos: str, interface_type: str) -> Response:
        """Update the selected interface type for multiband hardware."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_update_interface_multiband_type(conn, interface_pos, interface_type)

        for index, iface in enumerate(self._interfaces):
            if iface.position != interface_pos:
                continue
            if not iface.multi_band:
                return Response(success=False, error="Interface is not multiband")
            self._interfaces[index] = iface.model_copy(update={"type": interface_type})
            self._config_changed = True
            return Response(success=True)

        return Response(success=False, error=f"Interface {interface_pos} not found")

    def _db_get_interface_types(self, conn) -> list[str]:
        from backend.db import db_cursor
        types: list[str] = []
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT type FROM interfaces GROUP BY type ORDER BY type;",
                )
                for row in cur.fetchall():
                    types.append(row["type"])
        except Exception as exc:
            logger.error("DB get_interface_types error: %s", exc)
        finally:
            conn.close()
        return types or ["dvbs", "dvbt", "dvbc", "ip", "hdmi", "asi"]

    def _db_update_interface_multiband_type(
        self,
        conn,
        interface_pos: str,
        interface_type: str,
    ) -> Response:
        from backend.db import db_cursor

        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT multiband FROM interfaces WHERE pos = %s;",
                    (interface_pos,),
                )
                row = cur.fetchone()
                if row is None:
                    return Response(success=False, error=f"Interface {interface_pos} not found")
                if not bool(row.get("multiband", False)):
                    return Response(success=False, error="Interface is not multiband")

                cur.execute(
                    "UPDATE interfaces SET type = %s WHERE pos = %s;",
                    (interface_type, interface_pos),
                )
                self._set_config_changed_value(cur, True)

            return Response(success=True)
        except Exception as exc:
            logger.error("DB update_interface_multiband_type error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    def interface_status(self, position: str) -> str:
        """Return a human-readable status string for the given interface."""
        iface = self.get_interface(position)
        if iface is None:
            return "unknown"
        return iface.status or "idle"

    def interface_scan(self, position: str) -> Response:
        """Simulate starting a scan on the interface."""
        iface = self.get_interface(position)
        if iface is None:
            return Response(success=False, error=f"Interface {position} not found")
        return Response(success=True)

    # ------------------------------------------------------------------
    # Streamer / tuner status (runtime data – mock only)
    # ------------------------------------------------------------------

    def get_streamer_status(self, position: str, interface_type: str) -> StreamerStatus:
        """Return mock streamer status for the given interface."""
        iface = self.get_interface(position)
        if iface is None or not iface.active:
            return StreamerStatus(bitrate=0, status="stopped")
        return StreamerStatus(
            bitrate=random.randint(2000, 15000),
            status="running",
            pid=random.randint(1000, 9999),
        )

    def get_tuner_status(self, position: str, interface_type: str) -> TunerStatus:
        """Return mock tuner status for the given interface."""
        iface = self.get_interface(position)
        if iface is None or not iface.active:
            return TunerStatus(locked=False, signal_strength=0, snr=0, ber=0)
        return TunerStatus(
            locked=True,
            signal_strength=random.randint(60, 95),
            snr=random.randint(10, 20),
            ber=random.randint(0, 5),
        )

    # ------------------------------------------------------------------
    # Bitrates (from nv table bitrate_* keys when DB available)
    # ------------------------------------------------------------------

    def get_bitrates(self) -> list[Bitrate]:
        """Return current bitrate measurements per active interface."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_bitrates(conn)
        return [
            Bitrate(interface_pos=iface.position, bitrate=random.randint(1000, 20000))
            for iface in self._interfaces
            if iface.active
        ]

    def _db_get_bitrates(self, conn) -> list[Bitrate]:
        from backend.db import db_cursor
        bitrates: list[Bitrate] = []
        try:
            with db_cursor(conn) as cur:
                cur.execute("SELECT name, value FROM nv;")
                for row in cur.fetchall():
                    name = row["name"]
                    if name.startswith("bitrate_"):
                        try:
                            value = int(row.get("value") or 0)
                            iface_pos = name[len("bitrate_"):]
                            bitrates.append(Bitrate(interface_pos=iface_pos, bitrate=value))
                        except (ValueError, TypeError):
                            pass
        except Exception as exc:
            logger.error("DB get_bitrates error: %s", exc)
        finally:
            conn.close()
        return bitrates

    # ------------------------------------------------------------------
    # Commands
    # ------------------------------------------------------------------

    def run_command(self, command: str) -> Response:
        """Simulate running a system command."""
        allowed = {
            "reboot",
            "restart-services",
            "factory-reset",
            "update-epg",
            "poweroff",
            "netrestart",
            "reset",
            "allstart",
            "allstop",
            "ixcloud-connect",
            "ixcloud-disconnect",
            "wnet",
            "update-interfaces",
        }
        if command not in allowed:
            return Response(success=False, error=f"Unknown command: {command}")
        return Response(success=True)

    # ------------------------------------------------------------------
    # Backup/restore compatibility helpers
    # ------------------------------------------------------------------

    def export_backup_state(self) -> dict[str, Any]:
        """Return a structured snapshot suitable for backup export."""
        interfaces_payload: list[dict[str, Any]] = []

        for iface in self.get_interfaces(is_interfaces=False):
            config_payload = None
            if _iface_type_to_table(iface.type) is not None:
                cfg = self.get_config(iface.position, iface.type)
                if cfg is not None:
                    config_payload = cfg.model_dump(by_alias=True)

            services_payload = [
                service.model_dump(by_alias=True)
                for service in self.get_services(iface.position)
            ]
            interfaces_payload.append(
                {
                    "interface": iface.model_dump(by_alias=True),
                    "config": config_payload,
                    "services": services_payload,
                }
            )

        forced_contents = [
            forced_content.model_dump(by_alias=True)
            for _, forced_content in sorted(
                self.get_forced_contents().items(),
                key=lambda item: int(item[0]) if str(item[0]).isdigit() else str(item[0]),
            )
        ]

        return {
            "unit": self.get_unit_info().model_dump(by_alias=True),
            "settings": [
                setting.model_dump(by_alias=True) for setting in self.get_settings()
            ],
            "interfaces": interfaces_payload,
            "routes": [route.model_dump(by_alias=True) for route in self.get_routes()],
            "forced_contents": forced_contents,
        }

    def restore_backup_state(self, snapshot: dict[str, Any]) -> Response:
        """Apply a backup snapshot to the current backend state."""
        try:
            settings_payload = snapshot.get("settings") or []
            if settings_payload:
                response = self.update_settings(
                    [NameValue.model_validate(item) for item in settings_payload]
                )
                if not response.success:
                    return response

            for entry in snapshot.get("interfaces") or []:
                interface_payload = entry.get("interface") or {}
                position = interface_payload.get("position")
                interface_type = interface_payload.get("type")

                if not position or not interface_type:
                    continue

                config_payload = entry.get("config")
                if config_payload and _iface_type_to_table(interface_type) is not None:
                    cfg = Config.model_validate(config_payload)
                    cfg.interface_pos = position
                    response = self.set_config(cfg, interface_type)
                    if not response.success:
                        return response

                services_payload = entry.get("services") or []
                if services_payload:
                    response = self.save_services(
                        [Service.model_validate(item) for item in services_payload],
                        interface_type,
                        position,
                    )
                    if not response.success:
                        return response

            routes_payload = snapshot.get("routes") or []
            if routes_payload:
                response = self.update_routes(
                    [Route.model_validate(item) for item in routes_payload]
                )
                if not response.success:
                    return response

            forced_contents_payload = snapshot.get("forced_contents") or []
            if forced_contents_payload:
                response = self.save_forced_contents(
                    [
                        ForcedContent.model_validate(item)
                        for item in forced_contents_payload
                    ]
                )
                if not response.success:
                    return response

            return Response(success=True)
        except Exception as exc:
            logger.error("restore_backup_state error: %s", exc)
            return Response(success=False, error=str(exc))

    # ------------------------------------------------------------------
    # Forced content
    # ------------------------------------------------------------------

    def get_forced_contents(self) -> dict[str, ForcedContent]:
        """Return all forced-content entries."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_forced_contents(conn)
        return copy.deepcopy(self._forced_contents)

    def save_forced_contents(self, contents: list[ForcedContent]) -> Response:
        """Save forced content entries."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_save_forced_contents(conn, contents)
        existing = copy.deepcopy(self._forced_contents)
        merged: dict[str, ForcedContent] = {}
        for fc in contents:
            current = existing.get(str(fc.id))
            merged[str(fc.id)] = self._merge_forced_content(current, fc)
        self._forced_contents = merged
        self._config_changed = True
        return Response(success=True)

    def get_enabled_forced_contents(self) -> list[ForcedContent]:
        """Return enabled forced content entries for the live control panel."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_enabled_forced_contents(conn)

        return [
            self._with_runtime_forced_content(content)
            for _, content in sorted(self._forced_contents.items(), key=lambda item: int(item[0]))
            if content.enabled
        ]

    def save_forced_content_override_status(
        self,
        forced_content_id: int,
        override_index: int,
    ) -> Response:
        """Persist live forced content override state without marking config as changed."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_save_forced_content_override_status(conn, forced_content_id, override_index)

        current = self._forced_contents.get(str(forced_content_id))
        if current is None:
            return Response(success=False, error=f"Forced content {forced_content_id} not found")

        updated = current.model_copy(
            update={
                "override_index": override_index,
                "signal_status": self._forced_content_signal_status(current.signal_type, override_index, current.enabled),
                "com_status": True,
            }
        )
        self._forced_contents[str(forced_content_id)] = updated
        return Response(success=True)

    def _db_get_forced_contents(self, conn) -> dict[str, ForcedContent]:
        from backend.db import db_cursor
        result: dict[str, ForcedContent] = {}
        try:
            with db_cursor(conn) as cur:
                cur.execute("SELECT * FROM forced_content ORDER BY id;")
                for row in cur.fetchall():
                    fc_id = row["id"]
                    result[str(fc_id)] = ForcedContent(
                        id=fc_id,
                        name=row.get("name") or "",
                        enabled=bool(row.get("enable", False)),
                        networks=int(row.get("networks") or 0),
                        ts_filename=row.get("ts_filename") or "",
                        operation_mode=int(row.get("operation_mode") or 0),
                        signal_type=int(row.get("signal_type") or 0),
                        override_index=row.get("signal_override"),
                        signal_status=int(row.get("signal_status") or 0),
                        com_status=bool(row.get("com_status", False)),
                        volume=int(row.get("volume") or -1),
                    )
        except Exception as exc:
            logger.error("DB get_forced_contents error: %s", exc)
        finally:
            conn.close()
        return result

    def _db_save_forced_contents(self, conn, contents: list[ForcedContent]) -> Response:
        from backend.db import db_cursor
        try:
            with db_cursor(conn) as cur:
                for fc in contents:
                    cur.execute(
                        "UPDATE forced_content SET"
                        " enable = %s,"
                        " name = %s,"
                        " networks = %s,"
                        " volume = %s,"
                        " ts_filename = %s,"
                        " operation_mode = %s,"
                        " signal_type = %s"
                        " WHERE id = %s;",
                        (
                            fc.enabled,
                            fc.name,
                            fc.networks,
                            fc.volume,
                            fc.ts_filename,
                            fc.operation_mode,
                            fc.signal_type,
                            fc.id,
                        ),
                    )
                    if cur.rowcount == 0:
                        cur.execute(
                            "INSERT INTO forced_content"
                            " (id, enable, name, networks, volume, ts_filename, operation_mode, signal_type, signal_override, signal_status, com_status)"
                            " VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);",
                            (
                                fc.id,
                                fc.enabled,
                                fc.name,
                                fc.networks,
                                fc.volume,
                                fc.ts_filename,
                                fc.operation_mode,
                                fc.signal_type,
                                fc.override_index,
                                fc.signal_status,
                                fc.com_status,
                            ),
                        )
                self._set_config_changed_value(cur, True)
            return Response(success=True)
        except Exception as exc:
            logger.error("DB save_forced_contents error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    def _db_get_enabled_forced_contents(self, conn) -> list[ForcedContent]:
        from backend.db import db_cursor

        result: list[ForcedContent] = []
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT * FROM forced_content WHERE enable = TRUE ORDER BY id;",
                )
                for row in cur.fetchall():
                    content = ForcedContent(
                        id=row["id"],
                        name=row.get("name") or "",
                        enabled=bool(row.get("enable", False)),
                        networks=int(row.get("networks") or 0),
                        ts_filename=row.get("ts_filename") or "",
                        operation_mode=int(row.get("operation_mode") or 0),
                        signal_type=int(row.get("signal_type") or 0),
                        override_index=int(row.get("signal_override") or 0),
                        signal_status=int(row.get("signal_status") or 0),
                        com_status=bool(row.get("com_status", False)),
                        volume=int(row.get("volume") or -1),
                    )
                    result.append(self._with_runtime_forced_content(content))
        except Exception as exc:
            logger.error("DB get_enabled_forced_contents error: %s", exc)
        finally:
            conn.close()
        return result

    def _db_save_forced_content_override_status(
        self,
        conn,
        forced_content_id: int,
        override_index: int,
    ) -> Response:
        from backend.db import db_cursor

        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT id, signal_type, enable FROM forced_content WHERE id = %s;",
                    (forced_content_id,),
                )
                row = cur.fetchone()
                if row is None:
                    return Response(success=False, error=f"Forced content {forced_content_id} not found")

                signal_status = self._forced_content_signal_status(
                    int(row.get("signal_type") or 0),
                    override_index,
                    bool(row.get("enable", False)),
                )

                cur.execute(
                    "UPDATE forced_content SET signal_override = %s, signal_status = %s, com_status = TRUE WHERE id = %s;",
                    (override_index, signal_status, forced_content_id),
                )
            return Response(success=True)
        except Exception as exc:
            logger.error("DB save_forced_content_override_status error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    def _merge_forced_content(
        self,
        current: ForcedContent | None,
        incoming: ForcedContent,
    ) -> ForcedContent:
        if current is None:
            return self._with_runtime_forced_content(incoming)

        merged = current.model_copy(
            update={
                "enabled": incoming.enabled,
                "name": incoming.name,
                "networks": incoming.networks,
                "ts_filename": incoming.ts_filename,
                "operation_mode": incoming.operation_mode,
                "signal_type": incoming.signal_type,
                "volume": incoming.volume,
            }
        )
        return self._with_runtime_forced_content(merged)

    def _with_runtime_forced_content(self, content: ForcedContent) -> ForcedContent:
        return content.model_copy(
            update={
                "signal_status": self._forced_content_signal_status(
                    content.signal_type,
                    content.override_index,
                    content.enabled,
                ),
            }
        )

    def _forced_content_signal_status(
        self,
        signal_type: int,
        override_index: int,
        enabled: bool,
    ) -> int:
        if not enabled:
            return 0
        if override_index in {1, 2}:
            return override_index
        return 2 if signal_type == 1 else 1

    # ------------------------------------------------------------------
    # Feature check
    # ------------------------------------------------------------------

    def get_enabled_type(self, type_name: str) -> bool:
        """Check whether a named feature/type is enabled."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_enabled_type(conn, type_name)
        result = config.is_feature_enabled(type_name)
        return result if result is not None else False

    def _db_get_enabled_type(self, conn, type_name: str) -> bool:
        from backend.db import db_cursor

        nv_mapping = {
            "cloud": "ixcloud_enable",
            "forced_content": "forced_content_enable",
            "hls_output": "hls_enable",
            "hls": "hls_enable",
            "portal": "portal_enable",
            "dvbc": "dvbc_enable",
            "dvbc_net2": "dvbc_net2_enable",
            "ip": "ip_enable",
        }
        nv_name = nv_mapping.get(type_name)
        if nv_name is None:
            result = config.is_feature_enabled(type_name)
            return result if result is not None else False

        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT value FROM nv WHERE name = %s LIMIT 1;",
                    (nv_name,),
                )
                row = cur.fetchone()
                return _as_bool(row.get("value")) if row else False
        except Exception as exc:
            logger.error("DB get_enabled_type error: %s", exc)
            return False
        finally:
            conn.close()

    # ------------------------------------------------------------------
    # Interface log (mock only)
    # ------------------------------------------------------------------

    def get_interface_log(self, position: str) -> str:
        """Return mock log text for the given interface."""
        return _INTERFACE_LOGS.get(
            position,
            f"[INFO] No log data available for interface {position}\n",
        )

    # ------------------------------------------------------------------
    # Cloud
    # ------------------------------------------------------------------

    def get_cloud_details(self) -> dict[str, str]:
        """Return cloud connection details from nv table when DB available."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_cloud_details(conn)

        settings = self.get_settings_map()
        details = {
            **_CLOUD_DETAIL_DEFAULTS,
            "ixcloud_enable": "true" if self.get_enabled_type("cloud") else "false",
        }
        for key in _CLOUD_DETAIL_DEFAULTS:
            setting = settings.get(key)
            if setting and setting.value is not None:
                details[key] = setting.value or ""
        return details

    def _db_get_cloud_details(self, conn) -> dict[str, str]:
        from backend.db import db_cursor

        details = {
            **_CLOUD_DETAIL_DEFAULTS,
            "ixcloud_enable": "true" if config.enable_cloud else "false",
        }
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT name, value FROM nv WHERE name LIKE 'ixcloud_%';",
                )
                for row in cur.fetchall():
                    name = row["name"]
                    if name in _CLOUD_DETAIL_DEFAULTS or name == "ixcloud_enable":
                        details[name] = row.get("value") or ""
        except Exception as exc:
            logger.error("DB get_cloud_details error: %s", exc)
        finally:
            conn.close()
        return details

    def set_cloud_connection_state(self, connected: bool) -> Response:
        """Update compatibility cloud connection state without marking config as changed."""
        status_value = "true" if connected else "false"
        timestamp_value = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        message_value = (
            "Connected to cloud successfully."
            if connected
            else "Disconnected from cloud."
        )
        beacon_id = self.get_cloud_details().get("ixcloud_beaconid") or "IXCLOUD-DEMO-001"

        updates = {
            "ixcloud_online": status_value,
            "ixcloud_validate_date": timestamp_value,
            "ixcloud_validate_message": message_value,
            "ixcloud_beaconid": beacon_id,
        }

        conn = self._get_connection()
        if conn is not None:
            return self._db_set_cloud_connection_state(conn, updates)

        for name, value in updates.items():
            updated = False
            for index, setting in enumerate(self._settings):
                if setting.name != name:
                    continue
                self._settings[index] = setting.model_copy(update={"value": value})
                updated = True
                break
            if not updated:
                next_id = max((item.id for item in self._settings), default=0) + 1
                self._settings.append(NameValue(id=next_id, name=name, value=value))

        return Response(success=True)

    def _db_set_cloud_connection_state(
        self,
        conn,
        updates: dict[str, str],
    ) -> Response:
        from backend.db import db_cursor

        try:
            with db_cursor(conn) as cur:
                for name, value in updates.items():
                    cur.execute(
                        "UPDATE nv SET value = %s WHERE name = %s;",
                        (value, name),
                    )
                    if cur.rowcount == 0:
                        cur.execute(
                            "INSERT INTO nv (name, value) VALUES (%s, %s);",
                            (name, value),
                        )
            return Response(success=True)
        except Exception as exc:
            logger.error("DB set_cloud_connection_state error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    # ------------------------------------------------------------------
    # Software update (mock only – packages not in DB)
    # ------------------------------------------------------------------

    def get_update_packages(self) -> list[Package]:
        """Return available software packages."""
        return copy.deepcopy(self._packages)

    def update_packages(self, packages: list[Package]) -> Response:
        """Install selected packages."""
        for pkg in packages:
            for existing in self._packages:
                if existing.name == pkg.name:
                    existing.installed = True
                    existing.version = pkg.version or existing.version
                    break
        self._last_update_result = (
            f"Successfully installed {len(packages)} package(s)."
        )
        return Response(success=True)

    def get_update_result(self) -> str:
        """Return last update result."""
        return self._last_update_result

    # ------------------------------------------------------------------
    # HLS
    # ------------------------------------------------------------------

    def get_hls_interfaces(self) -> list[Interface]:
        """Return HLS output interface inventory."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_hls_interfaces(conn)
        if not config.enable_hls_output:
            return []
        return copy.deepcopy(_HLS_OUTPUT_INTERFACES)

    def _db_get_hls_interfaces(self, conn) -> list[Interface]:
        from backend.db import db_cursor
        interfaces: list[Interface] = []
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT * FROM interfaces WHERE type = 'hls2ip' ORDER BY pos;",
                )
                for row in cur.fetchall():
                    interfaces.append(Interface(
                        position=row["pos"],
                        name=row["name"],
                        type=row["type"],
                        active=bool(row["active"]),
                        multi_band=bool(row["multiband"]) if row["multiband"] is not None else False,
                    ))
        except Exception as exc:
            logger.error("DB get_hls_interfaces error: %s", exc)
        finally:
            conn.close()
        return interfaces

    def save_hls_wizard_services(self, services: list[Service]) -> Response:
        """Save HLS wizard service configuration."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_save_hls_wizard_services(conn, services)
        # Mock fallback
        existing_by_id = {s.id: i for i, s in enumerate(self._services)}
        selected_names = {svc.name for svc in services if svc.enabled}
        for svc in services:
            idx = existing_by_id.get(svc.id)
            if idx is not None:
                self._services[idx] = svc
        self._routes = [
            route.model_copy(update={"hls_enable": route.service_name in selected_names})
            for route in self._routes
        ]
        self._config_changed = True
        return Response(success=True)

    def set_hls_selection_flags(self, selected_names: set[str]) -> Response:
        """Synchronize route HLS flags after HLS wizard save."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_set_hls_selection_flags(conn, selected_names)

        self._routes = [
            route.model_copy(update={"hls_enable": route.service_name in selected_names})
            for route in self._routes
        ]
        self._config_changed = True
        return Response(success=True)

    def _db_save_hls_wizard_services(self, conn, services: list[Service]) -> Response:
        """Mirror the GWT saveHlsWizardServices logic."""
        from backend.db import db_cursor
        try:
            with db_cursor(conn) as cur:
                # Get all HLS interfaces
                cur.execute(
                    "SELECT pos FROM interfaces WHERE type = 'hls2ip' ORDER BY pos;",
                )
                hls_ifaces = [row["pos"] for row in cur.fetchall()]

                # Build route map: service_name -> route_id
                cur.execute(
                    "SELECT routes.id, services.name FROM routes"
                    " INNER JOIN services ON routes.service_key = services.key"
                    " ORDER BY routes.id;",
                )
                route_map: dict[str, int] = {
                    row["name"]: row["id"] for row in cur.fetchall()
                }

                # Get start IP
                cur.execute("SELECT value FROM nv WHERE name = 'ip_startaddr';")
                ip_row = cur.fetchone()
                ip_startaddr = (ip_row["value"] if ip_row else None) or "239.1.1.1:10000"
                address_port = ip_startaddr.split(":")
                ip_port = address_port[1] if len(address_port) > 1 else "10000"
                octets = address_port[0].split(".")

                enabled_services = [s for s in services if s.enabled]
                iface_idx = 0

                for iface_pos in hls_ifaces:
                    # Mark routes for this interface as temporary
                    cur.execute(
                        "UPDATE routes SET service_key = 'temp'"
                        " WHERE service_key LIKE %s;",
                        (iface_pos + ",%",),
                    )
                    # Disable all services for this interface
                    cur.execute(
                        "UPDATE services SET enable = FALSE"
                        " WHERE key LIKE %s;",
                        (iface_pos + ",%",),
                    )
                    # Mark interface inactive
                    cur.execute(
                        "UPDATE interfaces SET active = FALSE WHERE pos = %s;",
                        (iface_pos,),
                    )

                    if iface_idx < len(enabled_services):
                        svc = enabled_services[iface_idx]
                        iface_idx += 1
                        key = f"{iface_pos}, {svc.sid}"

                        # Insert service if not exists
                        cur.execute(
                            "INSERT INTO services"
                            " (interface_pos, name, sid, type, enable, key,"
                            "  scrambled, hls_url)"
                            " SELECT %s, %s, %s, 'TV_HD', %s, %s, %s, %s"
                            " WHERE NOT EXISTS"
                            "   (SELECT 1 FROM services WHERE key = %s);",
                            (iface_pos, svc.name, svc.sid, svc.enabled, key,
                             svc.scrambled, svc.hls_url, key),
                        )
                        # Update HLS URL
                        cur.execute(
                            "UPDATE services SET hls_url = %s WHERE key = %s;",
                            (svc.hls_url, key),
                        )
                        # Re-enable this service
                        cur.execute(
                            "UPDATE services SET enable = TRUE WHERE key = %s;",
                            (key,),
                        )
                        # Mark interface active with service name
                        cur.execute(
                            "UPDATE interfaces SET active = TRUE, name = %s"
                            " WHERE pos = %s;",
                            (svc.name, iface_pos),
                        )

                        # Update or insert route
                        if svc.name in route_map:
                            cur.execute(
                                "UPDATE routes SET service_key = %s, hls_enable = TRUE WHERE id = %s;",
                                (key, route_map[svc.name]),
                            )
                        else:
                            sid = svc.sid
                            num = (sid + 255) % 255
                            out_ip = (
                                f"{octets[0]}.{octets[1]}.{octets[2]}.{num}:{ip_port}"
                                if len(octets) >= 3
                                else f"239.1.1.{num}:{ip_port}"
                            )
                            cur.execute(
                                "INSERT INTO routes"
                                " (service_key, lcn, dsc_pos, mod_pos,"
                                "  out_sid, out_ip, output_name, epg_id, hls_enable)"
                                " SELECT %s, %s, 'None', 'None', %s, %s, %s, %s, TRUE"
                                " WHERE NOT EXISTS"
                                "   (SELECT 1 FROM routes WHERE service_key = %s);",
                                (key, svc.prefered_lcn, sid, out_ip, svc.name,
                                 svc.epg_url, key),
                            )

                    # Delete disabled services for this interface
                    cur.execute(
                        "DELETE FROM services"
                        " WHERE interface_pos = %s AND enable = FALSE;",
                        (iface_pos,),
                    )

                # Delete temp routes
                cur.execute(
                    "DELETE FROM routes WHERE service_key = 'temp';",
                )

                self._set_config_changed_value(cur, True)

            return Response(success=True)
        except Exception as exc:
            logger.error("DB save_hls_wizard_services error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    def _db_set_hls_selection_flags(self, conn, selected_names: set[str]) -> Response:
        from backend.db import db_cursor

        try:
            with db_cursor(conn) as cur:
                cur.execute("UPDATE routes SET hls_enable = FALSE WHERE hls_enable IS TRUE;")

                if selected_names:
                    cur.execute(
                        "SELECT routes.id, services.name FROM routes"
                        " INNER JOIN services ON routes.service_key = services.key;",
                    )
                    matching_ids = [
                        row["id"]
                        for row in cur.fetchall()
                        if (row.get("name") or "") in selected_names
                    ]
                    for route_id in matching_ids:
                        cur.execute(
                            "UPDATE routes SET hls_enable = TRUE WHERE id = %s;",
                            (route_id,),
                        )

                self._set_config_changed_value(cur, True)
            return Response(success=True)
        except Exception as exc:
            logger.error("DB set_hls_selection_flags error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    # ------------------------------------------------------------------
    # Media (mock only – media library not in DB schema)
    # ------------------------------------------------------------------

    def get_media(self) -> list[Media]:
        """Return available media library items."""
        return copy.deepcopy(self._media)


# Singleton instance
data_service = DataService()
