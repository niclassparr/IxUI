"""Data service providing entity access via PostgreSQL with mock data fallback.

When a database connection is available (DATABASE_URL env var or default
postgresql://postgres:@localhost:5432/ixui) all operations read from / write
to the real database tables defined in ixui_73_260312.sql.

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

logger = logging.getLogger(__name__)

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
    NameValue(id=1, name="nw_hostname", value="209990"),
    NameValue(id=2, name="nw_gateway", value="192.168.0.1"),
    NameValue(id=3, name="nw_multicastdev", value="eth0"),
    NameValue(id=4, name="nw_dns1", value="8.8.4.4"),
    NameValue(id=5, name="nw_dns2", value=""),
    NameValue(id=6, name="nw_eth0_bootproto", value="static"),
    NameValue(id=7, name="nw_eth0_onboot", value="yes"),
    NameValue(id=8, name="nw_eth0_ipaddr", value="192.168.0.73"),
    NameValue(id=9, name="nw_eth0_netmask", value="255.255.255.0"),
    NameValue(id=10, name="nw_eth0_mac", value="00:22:ab:80:7b:5e"),
    NameValue(id=11, name="nw_eth1_bootproto", value="static"),
    NameValue(id=12, name="nw_eth1_onboot", value="yes"),
    NameValue(id=13, name="nw_eth1_ipaddr", value="172.16.0.1"),
    NameValue(id=14, name="nw_eth1_netmask", value="255.255.255.0"),
    NameValue(id=15, name="nw_eth1_mac", value="00:22:ab:80:7b:5f"),
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


# ---------------------------------------------------------------------------
# DataService
# ---------------------------------------------------------------------------


class DataService:
    """Entity access layer.

    Uses PostgreSQL when available; falls back to in-memory mock data otherwise.
    """

    def __init__(self) -> None:
        # In-memory mock data (used as fallback when DB is unavailable)
        self._interfaces = copy.deepcopy(_INTERFACES)
        self._services = copy.deepcopy(_SERVICES)
        self._routes = copy.deepcopy(_ROUTES)
        self._settings = copy.deepcopy(_SETTINGS)
        self._configs = copy.deepcopy(_CONFIGS)
        self._forced_contents = copy.deepcopy(_FORCED_CONTENTS)
        self._packages = copy.deepcopy(_PACKAGES)
        self._media = copy.deepcopy(_MEDIA)
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

    def _db_get_interfaces(self, conn, *, is_interfaces: bool) -> list[Interface]:
        from backend.db import db_cursor
        interfaces: list[Interface] = []
        try:
            with db_cursor(conn) as cur:
                cur.execute("SELECT * FROM interfaces ORDER BY pos;")
                rows = cur.fetchall()
                for row in rows:
                    pos = row["pos"]
                    name = row["name"]
                    itype = row["type"]
                    active = bool(row["active"])
                    multiband = bool(row["multiband"]) if row["multiband"] is not None else False
                    # Determine EMM for interfaces with DSC behaviour
                    emm = False
                    network_num = 0
                    if is_interfaces:
                        # Fetch EMM flag
                        cur.execute(
                            "SELECT emm FROM config_emm WHERE interface_pos = %s;",
                            (pos,),
                        )
                        emm_row = cur.fetchone()
                        if emm_row is not None:
                            emm = (emm_row["emm"] or 0) > 0
                        # Fetch network_num for modulator interfaces
                        if itype == "mod":
                            cur.execute(
                                "SELECT network_num FROM config_eqam"
                                " WHERE interface_pos = %s;",
                                (pos,),
                            )
                            eqam_row = cur.fetchone()
                            if eqam_row is not None:
                                network_num = eqam_row["network_num"] or 1
                            else:
                                network_num = 1
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
                    return Config(
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                    )

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
                    )
                elif table == "config_dvbudp":
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        in_ip=row.get("in_ip"),
                        in_port=row.get("in_port") or 0,
                    )
                elif table == "config_dsc":
                    cur.execute(
                        "SELECT emm FROM config_emm WHERE interface_pos = %s;",
                        (interface_pos,),
                    )
                    emm_row = cur.fetchone()
                    cfg = Config(
                        id=row_id,
                        interface_pos=interface_pos,
                        interface_name=iface_name,
                        interface_active=iface_active,
                        emm=emm_row["emm"] if emm_row else 0,
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
                    cur.execute(
                        "INSERT INTO config_dvbudp (interface_pos, in_ip, in_port)"
                        " SELECT %s, %s, %s"
                        " WHERE NOT EXISTS"
                        "   (SELECT 1 FROM config_dvbudp WHERE interface_pos = %s);",
                        (pos, cfg.in_ip, cfg.in_port, pos),
                    )
                    cur.execute(
                        "UPDATE config_dvbudp"
                        " SET in_ip = %s, in_port = %s"
                        " WHERE interface_pos = %s;",
                        (cfg.in_ip, cfg.in_port, pos),
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
        return [s for s in self._services if s.interface_pos == interface_pos]

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
        self._services.extend(services)
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
                        type=row["type"],
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
                    svc_type = svc.type
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
                            (svc.key, sid, out_ip, svc.name, svc.epg_url, svc.key),
                        )

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
        return list(self._routes)

    def update_routes(self, routes: list[Route]) -> Response:
        """Replace all routes."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_update_routes(conn, routes)
        self._routes = routes
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
                        service_type=row.get("service_type") or "TV",
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
                            route.epg_url,
                            route.hls_enable,
                            route.id,
                        ),
                    )
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
        """Replace all settings."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_update_settings(conn, settings)
        self._settings = settings
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
                    cur.execute(
                        "UPDATE nv SET value = %s WHERE id = %s;",
                        (nv.value, nv.id),
                    )
            return Response(success=True)
        except Exception as exc:
            logger.error("DB update_settings error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

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
        except Exception as exc:
            logger.error("DB get_unit_info error: %s", exc)
        finally:
            conn.close()
        return UnitInfo(
            serial=serial or "",
            version=version or "",
            hostname=hostname or "",
            cloud=config.enable_cloud,
            forced_content=config.enable_forced_content,
            software_update=config.enable_software_update,
            hls_output=config.enable_hls_output,
            portal=config.enable_portal,
        )

    # ------------------------------------------------------------------
    # Network status (mock only – actual data comes from OS/network tools)
    # ------------------------------------------------------------------

    def get_network_status(self) -> list[IpMac]:
        """Return basic IP/MAC list."""
        return copy.deepcopy(_NETWORK_HOSTS)

    def get_network_status2(self) -> dict[str, IpStatus]:
        """Return enriched network status for gateway, DNS and public IP reachability."""
        return {
            "gateway": IpStatus(ip="192.168.0.1", status="online"),
            "dns1": IpStatus(ip="8.8.4.4", status="online"),
            "dns2": IpStatus(ip="", status="offline"),
            "public": IpStatus(ip="203.0.113.10", status="online"),
        }

    # ------------------------------------------------------------------
    # Interface types & status
    # ------------------------------------------------------------------

    def get_interface_types(self) -> list[str]:
        """Return known interface type identifiers."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_interface_types(conn)
        return ["dvbs", "dvbt", "dvbc", "ip", "hdmi", "asi"]

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
        allowed = {"reboot", "restart-services", "factory-reset", "update-epg", "wnet"}
        if command not in allowed:
            return Response(success=False, error=f"Unknown command: {command}")
        return Response(success=True)

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
        self._forced_contents = {str(fc.id): fc for fc in contents}
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
                        override_index=row.get("signal_override"),
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
                        " name = %s"
                        " WHERE id = %s;",
                        (fc.enabled, fc.name, fc.id),
                    )
            return Response(success=True)
        except Exception as exc:
            logger.error("DB save_forced_contents error: %s", exc)
            return Response(success=False, error=str(exc))
        finally:
            conn.close()

    # ------------------------------------------------------------------
    # Feature check
    # ------------------------------------------------------------------

    def get_enabled_type(self, type_name: str) -> bool:
        """Check whether a named feature/type is enabled."""
        result = config.is_feature_enabled(type_name)
        return result if result is not None else False

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
        return {
            "status": "connected",
            "cloud_id": "cloud-ixui-2024-001",
            "last_sync": "2024-06-01T12:34:56Z",
            "endpoint": "https://cloud.example.com/api/v1",
        }

    def _db_get_cloud_details(self, conn) -> dict[str, str]:
        from backend.db import db_cursor
        details: dict[str, str] = {}
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT name, value FROM nv WHERE name LIKE 'ixcloud_%';",
                )
                for row in cur.fetchall():
                    key = row["name"].replace("ixcloud_", "")
                    details[key] = row.get("value") or ""
        except Exception as exc:
            logger.error("DB get_cloud_details error: %s", exc)
        finally:
            conn.close()
        return details or {
            "status": "unknown",
            "cloud_id": "",
            "last_sync": "",
            "endpoint": "",
        }

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
        """Return HLS-capable interfaces."""
        conn = self._get_connection()
        if conn is not None:
            return self._db_get_hls_interfaces(conn)
        return [
            iface for iface in self._interfaces
            if iface.active and iface.type in _HLS_CAPABLE_TYPES
        ]

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
        for svc in services:
            idx = existing_by_id.get(svc.id)
            if idx is not None:
                self._services[idx] = svc
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
                                "UPDATE routes SET service_key = %s WHERE id = %s;",
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
                                "  out_sid, out_ip, output_name, epg_id)"
                                " SELECT %s, %s, 'None', 'None', %s, %s, %s, %s"
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

            return Response(success=True)
        except Exception as exc:
            logger.error("DB save_hls_wizard_services error: %s", exc)
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
