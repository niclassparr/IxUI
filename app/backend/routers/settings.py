"""Settings router – system settings and network status."""

from fastapi import APIRouter

from backend.models import IpMac, IpStatus, NameValue, Response
from backend.services.data_service import data_service

router = APIRouter(prefix="/api/settings", tags=["settings"])


@router.get("/")
async def get_settings() -> list[NameValue]:
    """Return all system settings."""
    return data_service.get_settings()


@router.put("/", response_model=Response)
async def update_settings(body: list[NameValue]) -> Response:
    """Replace all system settings."""
    return data_service.update_settings(body)


@router.get("/network-status")
async def get_network_status() -> list[IpMac]:
    """Return basic IP/MAC address list."""
    return data_service.get_network_status()


@router.get("/network-status2")
async def get_network_status2() -> dict[str, IpStatus]:
    """Return enriched network status keyed by IP."""
    return data_service.get_network_status2()
