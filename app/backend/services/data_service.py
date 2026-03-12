"""Mock data service providing demo data for all entities.

Replaces the DB and streamer-manager calls from the Java backend.
All data is held in memory and reset on restart.
"""

from __future__ import annotations

import copy
import random

from backend.config import config
from backend.models import (
    Bitrate,
    Config,
    ForcedContent,
    Interface,
    IpMac,
    IpStatus,
    Media,
    NameValue,
    Package,
    Response,
    Route,
    Service,
    StreamerStatus,
    TunerStatus,
    UnitInfo,
)

# ---------------------------------------------------------------------------
# Demo data fixtures
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
]

_SERVICES: list[Service] = [
    Service(
        id=1, interface_pos="A1", name="CNN International", sid=1001,
        type="TV", lang="eng", enabled=True,
        all_langs=["eng", "spa", "fra"], scrambled=False, found=True,
        prefered_lcn=1, hls_url="/hls/cnn/index.m3u8",
    ),
    Service(
        id=2, interface_pos="A1", name="BBC World News", sid=1002,
        type="TV", lang="eng", enabled=True,
        all_langs=["eng", "ara"], scrambled=False, found=True,
        prefered_lcn=2, hls_url="/hls/bbc/index.m3u8",
    ),
    Service(
        id=3, interface_pos="A2", name="France 24", sid=2001,
        type="TV", lang="fra", enabled=True,
        all_langs=["fra", "eng"], scrambled=False, found=True,
        prefered_lcn=3,
    ),
    Service(
        id=4, interface_pos="A2", name="DW News", sid=2002,
        type="TV", lang="deu", enabled=True,
        all_langs=["deu", "eng"], scrambled=True, found=True,
        prefered_lcn=4, key="0123456789ABCDEF",
    ),
    Service(
        id=5, interface_pos="C1", name="Al Jazeera English", sid=3001,
        type="TV", lang="eng", enabled=True,
        all_langs=["eng", "ara"], scrambled=False, found=True,
        prefered_lcn=5,
    ),
    Service(
        id=6, interface_pos="C1", name="Classic FM", sid=3002,
        type="Radio", lang="eng", enabled=True,
        all_langs=["eng"], scrambled=False, found=True,
        prefered_lcn=6, radio_url="http://stream.classicfm.net/live",
    ),
]

_ROUTES: list[Route] = [
    Route(
        id=1, service_id=1, service_name="CNN International", service_type="TV",
        interface_pos="A1", interface_type="dvbs", lcn=1, out_sid=101,
        out_ip="239.1.1.1", hls_enable=True, output_name="Output 1",
    ),
    Route(
        id=2, service_id=2, service_name="BBC World News", service_type="TV",
        interface_pos="A1", interface_type="dvbs", lcn=2, out_sid=102,
        out_ip="239.1.1.2", hls_enable=True, output_name="Output 2",
    ),
    Route(
        id=3, service_id=3, service_name="France 24", service_type="TV",
        interface_pos="A2", interface_type="dvbt", lcn=3, out_sid=201,
        out_ip="239.1.2.1", hls_enable=False, output_name="Output 3",
    ),
    Route(
        id=4, service_id=5, service_name="Al Jazeera English", service_type="TV",
        interface_pos="C1", interface_type="ip", lcn=5, out_sid=301,
        out_ip="239.1.3.1", hls_enable=True, output_name="Output 4",
    ),
]

_SETTINGS: list[NameValue] = [
    NameValue(id=1, name="hostname", value="ixui-unit-1"),
    NameValue(id=2, name="ip", value="192.168.1.100"),
    NameValue(id=3, name="netmask", value="255.255.255.0"),
    NameValue(id=4, name="gateway", value="192.168.1.1"),
    NameValue(id=5, name="dns", value="8.8.8.8"),
    NameValue(id=6, name="ntp", value="pool.ntp.org"),
    NameValue(id=7, name="timezone", value="Europe/London"),
    NameValue(id=8, name="output_mode", value="dvbt"),
    NameValue(id=9, name="output_frequency", value="474000"),
    NameValue(id=10, name="output_bandwidth", value="8"),
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
}

_FORCED_CONTENTS: dict[str, ForcedContent] = {
    "emergency": ForcedContent(id=1, name="Emergency Broadcast", enabled=False, override_index=0),
    "maintenance": ForcedContent(id=2, name="Maintenance Notice", enabled=False, override_index=1),
    "promo": ForcedContent(id=3, name="Promotional Content", enabled=True, override_index=2),
}

_NETWORK_HOSTS: list[IpMac] = [
    IpMac(ip="192.168.1.100", mac="00:1A:2B:3C:4D:5E"),
    IpMac(ip="192.168.1.101", mac="00:1A:2B:3C:4D:5F"),
    IpMac(ip="192.168.1.102", mac="00:1A:2B:3C:4D:60"),
]

_PACKAGES: list[Package] = [
    Package(name="ixui-core", version="3.2.1", installed=True),
    Package(name="ixui-hls-plugin", version="1.4.0", installed=True),
    Package(name="ixui-cloud-agent", version="2.0.3", installed=False),
    Package(name="ixui-epg-grabber", version="1.1.0", installed=False),
]

_MEDIA: list[Media] = [
    Media(id=1, name="Startup Logo", url="/media/startup-logo.png"),
    Media(id=2, name="Channel Banner", url="/media/channel-banner.jpg"),
    Media(id=3, name="Emergency Slide", url="/media/emergency-slide.png"),
    Media(id=4, name="Maintenance Card", url="/media/maintenance-card.mp4"),
]

_HLS_CAPABLE_TYPES: frozenset[str] = frozenset({"dvbs", "dvbt", "ip"})

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


class DataService:
    """Provides mock data for all entities, replacing the Java backend DB layer."""

    def __init__(self) -> None:
        self._interfaces = copy.deepcopy(_INTERFACES)
        self._services = copy.deepcopy(_SERVICES)
        self._routes = copy.deepcopy(_ROUTES)
        self._settings = copy.deepcopy(_SETTINGS)
        self._configs = copy.deepcopy(_CONFIGS)
        self._forced_contents = copy.deepcopy(_FORCED_CONTENTS)
        self._packages = copy.deepcopy(_PACKAGES)
        self._media = copy.deepcopy(_MEDIA)
        self._last_update_result: str = "No updates have been installed yet."

    # -- Interfaces ---------------------------------------------------------

    def get_interfaces(self, *, is_interfaces: bool = True) -> list[Interface]:
        """Return all interfaces, optionally filtered to real interfaces only."""
        if is_interfaces:
            return [i for i in self._interfaces if i.type != "virtual"]
        return list(self._interfaces)

    def get_interface(self, position: str) -> Interface | None:
        """Return a single interface by position, or None."""
        for iface in self._interfaces:
            if iface.position == position:
                return iface
        return None

    # -- Configs ------------------------------------------------------------

    def get_config(self, interface_pos: str, interface_type: str) -> Config | None:
        """Return tuning config for the given interface position."""
        return self._configs.get(interface_pos)

    def set_config(self, cfg: Config, interface_type: str) -> Response:
        """Persist an updated interface config."""
        self._configs[cfg.interface_pos] = cfg
        return Response(success=True)

    # -- Services -----------------------------------------------------------

    def get_services(self, interface_pos: str) -> list[Service]:
        """Return services belonging to the given interface."""
        return [s for s in self._services if s.interface_pos == interface_pos]

    def save_services(
        self,
        services: list[Service],
        interface_type: str,
        interface_pos: str,
    ) -> Response:
        """Replace services for the given interface."""
        self._services = [
            s for s in self._services if s.interface_pos != interface_pos
        ]
        self._services.extend(services)
        return Response(success=True)

    # -- Routes -------------------------------------------------------------

    def get_routes(self) -> list[Route]:
        """Return all output routes."""
        return list(self._routes)

    def update_routes(self, routes: list[Route]) -> Response:
        """Replace all routes."""
        self._routes = routes
        return Response(success=True)

    # -- Settings -----------------------------------------------------------

    def get_settings(self) -> list[NameValue]:
        """Return all system settings."""
        return list(self._settings)

    def update_settings(self, settings: list[NameValue]) -> Response:
        """Replace all settings."""
        self._settings = settings
        return Response(success=True)

    # -- Unit info ----------------------------------------------------------

    def get_unit_info(self) -> UnitInfo:
        """Return unit hardware/feature information."""
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

    # -- Network status -----------------------------------------------------

    def get_network_status(self) -> list[IpMac]:
        """Return basic IP/MAC list."""
        return copy.deepcopy(_NETWORK_HOSTS)

    def get_network_status2(self) -> dict[str, IpStatus]:
        """Return enriched network status keyed by IP."""
        return {
            h.ip: IpStatus(ip=h.ip, mac=h.mac, status="online")
            for h in _NETWORK_HOSTS
        }

    # -- Interface types & status -------------------------------------------

    def get_interface_types(self) -> list[str]:
        """Return known interface type identifiers."""
        return ["dvbs", "dvbt", "dvbc", "ip", "hdmi", "asi"]

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

    # -- Streamer / tuner status --------------------------------------------

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

    # -- Bitrates -----------------------------------------------------------

    def get_bitrates(self) -> list[Bitrate]:
        """Return current bitrate measurements per active interface."""
        return [
            Bitrate(interface_pos=iface.position, bitrate=random.randint(1000, 20000))
            for iface in self._interfaces
            if iface.active
        ]

    # -- Commands -----------------------------------------------------------

    def run_command(self, command: str) -> Response:
        """Simulate running a system command."""
        allowed = {"reboot", "restart-services", "factory-reset", "update-epg"}
        if command not in allowed:
            return Response(success=False, error=f"Unknown command: {command}")
        return Response(success=True)

    # -- Forced content -----------------------------------------------------

    def get_forced_contents(self) -> dict[str, ForcedContent]:
        """Return all forced-content entries."""
        return copy.deepcopy(self._forced_contents)

    def save_forced_contents(self, contents: list[ForcedContent]) -> Response:
        """Save forced content entries."""
        self._forced_contents = {str(fc.id): fc for fc in contents}
        return Response(success=True)

    # -- Feature check ------------------------------------------------------

    def get_enabled_type(self, type_name: str) -> bool:
        """Check whether a named feature/type is enabled."""
        result = config.is_feature_enabled(type_name)
        return result if result is not None else False

    # -- Interface log ------------------------------------------------------

    def get_interface_log(self, position: str) -> str:
        """Return mock log text for the given interface."""
        return _INTERFACE_LOGS.get(
            position,
            f"[INFO] No log data available for interface {position}\n",
        )

    # -- Cloud --------------------------------------------------------------

    def get_cloud_details(self) -> dict[str, str]:
        """Return cloud connection details."""
        return {
            "status": "connected",
            "cloud_id": "cloud-ixui-2024-001",
            "last_sync": "2024-06-01T12:34:56Z",
            "endpoint": "https://cloud.example.com/api/v1",
        }

    # -- Software update ----------------------------------------------------

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

    # -- HLS ----------------------------------------------------------------

    def get_hls_interfaces(self) -> list[Interface]:
        """Return HLS-capable interfaces."""
        return [
            iface for iface in self._interfaces
            if iface.active and iface.type in _HLS_CAPABLE_TYPES
        ]

    def save_hls_wizard_services(self, services: list[Service]) -> Response:
        """Save HLS wizard service configuration."""
        for svc in services:
            for i, existing in enumerate(self._services):
                if existing.id == svc.id:
                    self._services[i] = svc
                    break
        return Response(success=True)

    # -- Media --------------------------------------------------------------

    def get_media(self) -> list[Media]:
        """Return available media library items."""
        return copy.deepcopy(self._media)


# Singleton instance
data_service = DataService()
