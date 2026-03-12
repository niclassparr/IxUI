"""Interfaces router – tuners, IP inputs, configs, services, scans."""

from fastapi import APIRouter, HTTPException, status

from backend.models import Config, Response, Service
from backend.services.data_service import data_service

router = APIRouter(prefix="/api/interfaces", tags=["interfaces"])


@router.get("/types/all")
async def get_interface_types() -> list[str]:
    """Return all known interface type identifiers."""
    return data_service.get_interface_types()


@router.get("/")
async def list_interfaces():
    """Return all hardware interfaces."""
    return data_service.get_interfaces()


@router.get("/{position}")
async def get_interface(position: str):
    """Return a single interface by its position code."""
    iface = data_service.get_interface(position)
    if iface is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Interface {position} not found",
        )
    return iface


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
    return data_service.set_config(body, interface_type)


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
    return data_service.save_services(body, iface.type, position)


@router.post("/{position}/scan", response_model=Response)
async def start_scan(position: str) -> Response:
    """Start a channel scan on the given interface."""
    return data_service.interface_scan(position)


@router.get("/{position}/status")
async def get_status(position: str) -> dict[str, str]:
    """Return the current status of an interface."""
    return {"status": data_service.interface_status(position)}


@router.get("/{position}/streamer-status/{interface_type}")
async def get_streamer_status(position: str, interface_type: str):
    """Return streamer process status for the interface."""
    return data_service.get_streamer_status(position, interface_type)


@router.get("/{position}/tuner-status/{interface_type}")
async def get_tuner_status(position: str, interface_type: str):
    """Return tuner lock/signal status for the interface."""
    return data_service.get_tuner_status(position, interface_type)
