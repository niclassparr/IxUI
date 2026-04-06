"""Settings router – system settings and network status."""

from fastapi import APIRouter

from backend.models import (
    DateTimeState,
    DateTimeUpdateRequest,
    IpMac,
    IpStatus,
    ModulatorAssignment,
    NameValue,
    NetworkSettingsUpdateRequest,
    PasswordChangeRequest,
    Response,
)
from backend.services.auth_service import auth_service
from backend.services.data_service import data_service

router = APIRouter(prefix="/api/settings", tags=["settings"])


@router.get("/")
async def get_settings() -> list[NameValue]:
    """Return all system settings."""
    return data_service.get_settings()


@router.put("/", response_model=Response)
async def update_settings(body: list[NameValue]) -> Response:
    """Upsert the supplied system settings."""
    return data_service.update_settings(body)


@router.get("/datetime", response_model=DateTimeState)
async def get_datetime_state() -> DateTimeState:
    """Return date/time configuration state."""
    return data_service.get_datetime_state()


@router.put("/datetime", response_model=Response)
async def update_datetime(body: DateTimeUpdateRequest) -> Response:
    """Persist date/time configuration."""
    return data_service.save_datetime(body)


@router.get("/modulators", response_model=list[ModulatorAssignment])
async def get_modulators() -> list[ModulatorAssignment]:
    """Return modulator network assignments."""
    return data_service.get_modulators()


@router.put("/modulators", response_model=Response)
async def update_modulators(body: list[ModulatorAssignment]) -> Response:
    """Persist modulator network assignments."""
    return data_service.save_modulators_config(body)


@router.post("/password", response_model=Response)
async def change_password(body: PasswordChangeRequest) -> Response:
    """Change password for the active session."""
    return auth_service.change_password(
        body.session_key or "",
        body.old_password,
        body.new_password,
    )


@router.get("/network", response_model=list[NameValue])
async def get_network_settings() -> list[NameValue]:
    """Return editable network settings."""
    return data_service.get_network_settings()


@router.put("/network", response_model=Response)
async def update_network_settings(body: NetworkSettingsUpdateRequest) -> Response:
    """Update editable network settings and optionally apply them."""
    return data_service.update_network_settings(body)


@router.get("/network-status")
async def get_network_status() -> list[IpMac]:
    """Return basic IP/MAC address list."""
    return data_service.get_network_status()


@router.get("/network-status2")
async def get_network_status2() -> dict[str, IpStatus]:
    """Return enriched network status keyed by IP."""
    return data_service.get_network_status2()
