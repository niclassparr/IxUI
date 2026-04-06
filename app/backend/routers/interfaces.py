"""Interfaces router – tuners, IP inputs, configs, services, and runtime actions."""

from fastapi import APIRouter, HTTPException, Query, status

from backend.models import Config, HlsWizardState, Interface, Response, Service
from backend.services.data_service import data_service
from backend.services.hls_wizard_service import hls_wizard_service
from backend.services.interface_runtime_service import interface_runtime_service

router = APIRouter(prefix="/api/interfaces", tags=["interfaces"])


@router.get("/types/all")
async def get_interface_types() -> list[str]:
    """Return all known interface type identifiers."""
    return data_service.get_interface_types()


@router.get("/hls/list")
async def get_hls_interfaces() -> list[Interface]:
    """Return HLS-capable interfaces."""
    return data_service.get_hls_interfaces()


@router.get("/hls/wizard", response_model=HlsWizardState)
async def get_hls_wizard_state() -> HlsWizardState:
    """Return the current HLS wizard state."""
    return hls_wizard_service.get_state(force_scan=False)


@router.post("/hls/scan", response_model=HlsWizardState)
async def scan_hls_wizard() -> HlsWizardState:
    """Scan source interfaces and return refreshed HLS wizard state."""
    return hls_wizard_service.get_state(force_scan=True)


@router.put("/hls/services", response_model=Response)
async def save_hls_wizard_services(body: list[Service]) -> Response:
    """Save HLS wizard service configuration."""
    return hls_wizard_service.save_services(body)


@router.get("/")
async def list_interfaces():
    """Return all hardware interfaces."""
    return interface_runtime_service.list_interfaces()


@router.get("/{position}")
async def get_interface(position: str):
    """Return a single interface by its position code."""
    iface = interface_runtime_service.get_interface(position)
    if iface is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Interface {position} not found",
        )
    return iface


@router.get("/{position}/log")
async def get_interface_log(position: str) -> dict[str, str]:
    """Return log text for the given interface."""
    return {"log": interface_runtime_service.get_interface_log(position)}


@router.get("/{position}/infoch", response_model=Config)
async def get_interface_infoch(position: str) -> Config:
    """Return the legacy info-channel config payload."""
    cfg = data_service.get_interface_infoch(position)
    if cfg is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Info channel {position} not found",
        )
    return cfg


@router.put("/{position}/infoch", response_model=Response)
async def set_interface_infoch(position: str, body: Config) -> Response:
    """Persist legacy info-channel name and active state."""
    body.interface_pos = position
    response = data_service.set_interface_infoch(body)
    if response.success:
        interface_runtime_service.note_config_saved(position, "infoch", body)
    return response


@router.get("/{position}/config/{interface_type}")
async def get_config(position: str, interface_type: str):
    """Return tuning configuration for an interface."""
    cfg = data_service.get_config(position, interface_type)
    if cfg is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Config for {position} not found",
        )
    return cfg


@router.put("/{position}/config/{interface_type}", response_model=Response)
async def set_config(position: str, interface_type: str, body: Config) -> Response:
    """Update tuning configuration for an interface."""
    body.interface_pos = position
    response = data_service.set_config(body, interface_type)
    if response.success:
        interface_runtime_service.note_config_saved(position, interface_type, body)
    return response


@router.get("/{position}/services")
async def get_services(position: str) -> list[Service]:
    """Return services discovered on the given interface."""
    return data_service.get_services(position)


@router.put("/{position}/services", response_model=Response)
async def save_services(position: str, body: list[Service]) -> Response:
    """Replace services for the given interface."""
    iface = data_service.get_interface(position)
    if iface is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Interface {position} not found",
        )
    response = data_service.save_services(body, iface.type, position)
    if response.success:
        interface_runtime_service.note_services_saved(position, body)
    return response


@router.get("/{position}/scan-time")
async def get_scan_time(position: str) -> dict[str, str | None]:
    """Return the timestamp of the most recent interface scan."""
    return {"scan_time": interface_runtime_service.get_scan_time(position)}


@router.get("/{position}/scan-result")
async def get_scan_result(position: str) -> list[Service]:
    """Return the most recent interface scan result."""
    return interface_runtime_service.get_scan_result(position)


@router.post("/{position}/apply", response_model=Response)
async def apply_interface(
    position: str,
    body: dict[str, str] | None = None,
) -> Response:
    """Apply the current interface configuration to the runtime layer."""
    interface_type = (body or {}).get("interface_type")
    iface = data_service.get_interface(position)
    if iface is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Interface {position} not found",
        )
    return interface_runtime_service.apply_interface(position, interface_type or iface.type)


@router.post("/{position}/scan", response_model=Response)
async def start_scan(position: str) -> Response:
    """Start a channel scan on the given interface."""
    return interface_runtime_service.start_scan(position)


@router.post("/{position}/command", response_model=Response)
async def run_interface_command(position: str, body: dict[str, str]) -> Response:
    """Execute a runtime command for a specific interface."""
    return interface_runtime_service.run_interface_command(position, body.get("command", ""))


@router.get("/{position}/emm")
async def get_current_emm_list(
    position: str,
    is_dsc: bool = Query(default=False),
) -> dict[str, object]:
    """Return compatibility EMM allocation data for the given interface."""
    return interface_runtime_service.get_current_emm_list(position, is_dsc)


@router.put("/{position}/multiband/{interface_type}", response_model=Response)
async def update_multiband_type(position: str, interface_type: str) -> Response:
    """Switch the active type for a multiband interface."""
    return interface_runtime_service.update_multiband_type(position, interface_type)


@router.get("/{position}/status")
async def get_status(position: str) -> dict[str, str]:
    """Return the current status of an interface."""
    return {"status": interface_runtime_service.get_interface_status(position)}


@router.get("/{position}/streamer-status/{interface_type}")
async def get_streamer_status(position: str, interface_type: str):
    """Return streamer process status for the interface."""
    return interface_runtime_service.get_streamer_status(position, interface_type)


@router.get("/{position}/tuner-status/{interface_type}")
async def get_tuner_status(position: str, interface_type: str):
    """Return tuner lock/signal status for the interface."""
    return interface_runtime_service.get_tuner_status(position, interface_type)
