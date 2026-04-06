"""Pydantic models ported from the shared/ Java classes.

These models define the data structures used across the IxUI application
for interfaces, services, routes, configuration, and system info.
"""

from pydantic import BaseModel, ConfigDict, Field


class Response(BaseModel):
    """Generic API response."""

    model_config = ConfigDict(populate_by_name=True)

    success: bool = True
    error: str | None = None


class NameValue(BaseModel):
    """Key-value setting entry."""

    model_config = ConfigDict(populate_by_name=True)

    id: int = 0
    name: str
    value: str | None = None


class Interface(BaseModel):
    """Hardware interface (tuner/IP input)."""

    model_config = ConfigDict(populate_by_name=True)

    position: str
    name: str
    type: str
    status: str | None = None
    active: bool = True
    emm: bool = False
    multi_band: bool = False
    network_num: int = 0


class Service(BaseModel):
    """A broadcast service (TV/radio channel)."""

    model_config = ConfigDict(populate_by_name=True)

    id: int = 0
    interface_pos: str = ""
    name: str = ""
    sid: int = 0
    type: str = "TV_SD"
    lang: str = "eng"
    enabled: bool = True
    all_langs: list[str] = Field(default_factory=list)
    radio_url: str | None = None
    show_pres: bool = False
    scrambled: bool = False
    epg_url: str | None = None
    key: str | None = None
    hls_url: str | None = None
    webradio_url: str | None = None
    prefered_lcn: int = 0
    filters: list[str] | None = None
    found: bool = True


class Route(BaseModel):
    """Output route mapping a service to a modulator/IP output."""

    model_config = ConfigDict(populate_by_name=True)

    id: int = 0
    service_id: int = 0
    service_name: str = ""
    service_type: str = "TV_SD"
    interface_pos: str = ""
    interface_type: str = ""
    lcn: int = 0
    descrambler_pos: str | None = None
    modulator_pos: str | None = None
    modulator_pos_net2: str | None = None
    out_sid: int = 0
    out_ip: str | None = None
    scrambled: bool = False
    output_name: str | None = None
    epg_url: str | None = None
    hls_enable: bool = False
    interface_multiband: bool = False


class Config(BaseModel):
    """Interface tuning/input configuration."""

    model_config = ConfigDict(populate_by_name=True)

    id: int = 0
    interface_pos: str = ""
    interface_name: str | None = None
    interface_active: bool = True
    freq: int = 0
    pol: str | None = None
    symb: int = 0
    del_sys: str | None = Field(default=None, alias="del")
    satno: int = 0
    lnb_type: str | None = None
    bw: int = 0
    emm: int = 0
    ch_types: list[str] | None = None
    ch_audiourls: list[str] | None = None
    pres_url: str | None = None
    hdmi_format: str | None = None
    constellation: str | None = None
    in_ip: str | None = None
    in_port: int = 0
    max_bitrate: int = 0
    gain: int = 0
    webradio_url: str | None = None


class UnitInfo(BaseModel):
    """Unit hardware and feature information."""

    model_config = ConfigDict(populate_by_name=True)

    serial: str = ""
    version: str = ""
    hostname: str = ""
    cloud: bool = False
    forced_content: bool = False
    software_update: bool = False
    hls_output: bool = False
    portal: bool = False


class SessionKeys(BaseModel):
    """Active session with associated keys."""

    model_config = ConfigDict(populate_by_name=True)

    token: str = ""
    keys: dict[str, str] = Field(default_factory=dict)
    cloud: bool = False


class ForcedContent(BaseModel):
    """Forced content override entry."""

    model_config = ConfigDict(populate_by_name=True)

    id: int = 0
    name: str = ""
    enabled: bool = False
    networks: int = 0
    ts_filename: str = ""
    operation_mode: int = 0
    signal_type: int = 0
    override_index: int = 0
    signal_status: int = 0
    com_status: bool = False
    volume: int = -1


class ForcedContentOverrideRequest(BaseModel):
    """Forced content live override payload."""

    model_config = ConfigDict(populate_by_name=True)

    override_index: int = 0


class Package(BaseModel):
    """Software package info."""

    model_config = ConfigDict(populate_by_name=True)

    name: str
    version: str | None = None
    installed: bool = False
    update: bool = False


class IpStatus(BaseModel):
    """IP address with connection status."""

    model_config = ConfigDict(populate_by_name=True)

    ip: str = ""
    mac: str = ""
    status: str = "unknown"


class IpMac(BaseModel):
    """IP-MAC address pair."""

    model_config = ConfigDict(populate_by_name=True)

    ip: str = ""
    mac: str = ""
    status: str = "unknown"


class CiMenuItem(BaseModel):
    """A selectable entry in the DSC CI menu."""

    model_config = ConfigDict(populate_by_name=True)

    id: int = 0
    label: str = ""


class ServiceStatus(BaseModel):
    """Per-service runtime statistics used by the interface status page."""

    model_config = ConfigDict(populate_by_name=True)

    name: str = ""
    scrambled: bool = False
    destination: str | None = None
    bitrate: int = 0
    discontinuity: int = 0
    source: str | None = None
    mux_load: int = 0
    max_mux_load: int = 0
    download_bitrate: int = 0
    selected_bitrate: int = 0
    segment_counter: int = 0
    num_stream_switches: int = 0
    num_segments_missed: int = 0
    buffer_level: int = 0


class StreamerStatus(BaseModel):
    """Streamer process status."""

    model_config = ConfigDict(populate_by_name=True)

    bitrate: int = 0
    status: str = "stopped"
    pid: int | None = None
    mux_load: int = 0
    max_mux_load: int = 0
    ca_services: int = 0
    ca_pids: int = 0
    services: list[ServiceStatus] = Field(default_factory=list)


class TunerStatus(BaseModel):
    """Tuner lock and signal quality."""

    model_config = ConfigDict(populate_by_name=True)

    locked: bool = False
    frequency: int = 0
    signal_strength: int = 0
    snr: int = 0
    ber: int = 0
    ci_status: int = 0
    ca_emm: bool = False
    ca_text: str = ""
    ca_osd: str = ""
    menu_title: str = ""
    menu_items: list[CiMenuItem] = Field(default_factory=list)
    ci_menu_open: bool = False


class Bitrate(BaseModel):
    """Per-interface bitrate measurement."""

    model_config = ConfigDict(populate_by_name=True)

    interface_pos: str = ""
    bitrate: int = 0


class Media(BaseModel):
    """Media resource entry."""

    model_config = ConfigDict(populate_by_name=True)

    id: int = 0
    title: str = ""
    internal_filename: str = ""
    name: str = ""
    url: str = ""


class Emm(BaseModel):
    """EMM (Entitlement Management Message) data."""

    model_config = ConfigDict(populate_by_name=True)

    entries: list[dict] = Field(default_factory=list)


class LoginRequest(BaseModel):
    """Login credentials."""

    model_config = ConfigDict(populate_by_name=True)

    username: str
    password: str


class LoginResponse(BaseModel):
    """Login result with optional session key."""

    model_config = ConfigDict(populate_by_name=True)

    session_key: str | None = None
    success: bool = False
    error: str | None = None


class PasswordChangeRequest(BaseModel):
    """Password change payload for the active session."""

    model_config = ConfigDict(populate_by_name=True)

    session_key: str | None = None
    old_password: str
    new_password: str


class ModulatorAssignment(BaseModel):
    """Network assignment for a modulator interface."""

    model_config = ConfigDict(populate_by_name=True)

    interface_pos: str
    network_num: int = 0


class DateTimeState(BaseModel):
    """Date/time configuration state exposed to the UI."""

    model_config = ConfigDict(populate_by_name=True)

    timezone: str = "UTC"
    ntp_enabled: bool = True
    current_date: str = ""
    current_time: str = ""
    timezones: list[str] = Field(default_factory=list)


class DateTimeUpdateRequest(BaseModel):
    """Date/time update payload."""

    model_config = ConfigDict(populate_by_name=True)

    timezone: str
    ntp_enabled: bool = True
    date: str | None = None
    time: str | None = None


class NetworkSettingsUpdateRequest(BaseModel):
    """Editable network settings payload."""

    model_config = ConfigDict(populate_by_name=True)

    settings: list[NameValue] = Field(default_factory=list)
    apply_changes: bool = False


class HlsWizardState(BaseModel):
    """Aggregated state used by the HLS wizard UI."""

    model_config = ConfigDict(populate_by_name=True)

    max_services: int = 0
    scanned_at: str | None = None
    warning: str | None = None
    output_interfaces: list[Interface] = Field(default_factory=list)
    source_interfaces: list[Interface] = Field(default_factory=list)
    available_services: list[Service] = Field(default_factory=list)
    selected_services: list[Service] = Field(default_factory=list)
    filters: list[str] = Field(default_factory=list)
