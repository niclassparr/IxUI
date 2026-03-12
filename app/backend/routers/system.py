"""System router – unit info, commands, features, and JSON diagnostics."""

from fastapi import APIRouter, HTTPException, status

from backend.models import ForcedContent, Media, Package, Response, UnitInfo
from backend.services.data_service import data_service

router = APIRouter(prefix="/api/system", tags=["system"])


@router.get("/unit-info", response_model=UnitInfo)
async def get_unit_info() -> UnitInfo:
    """Return unit hardware and feature information."""
    return data_service.get_unit_info()


@router.post("/command", response_model=Response)
async def run_command(body: dict) -> Response:
    """Execute a named system command."""
    command = body.get("command", "")
    return data_service.run_command(command)


@router.get("/json-info")
async def get_json_info() -> dict:
    """Return aggregated JSON diagnostic information."""
    info = data_service.get_unit_info()
    return {
        "unit": info.model_dump(),
        "interfaces": [i.model_dump() for i in data_service.get_interfaces()],
        "routes": [r.model_dump() for r in data_service.get_routes()],
        "settings": [s.model_dump() for s in data_service.get_settings()],
    }


@router.get("/feature/{name}")
async def get_feature(name: str) -> dict:
    """Check whether a named feature flag is enabled."""
    enabled = data_service.get_enabled_type(name)
    return {"feature": name, "enabled": enabled}


@router.get("/cloud-details")
async def get_cloud_details() -> dict:
    """Return cloud connection details."""
    return data_service.get_cloud_details()


@router.get("/update-packages")
async def get_update_packages() -> list[Package]:
    """Return available software update packages."""
    return data_service.get_update_packages()


@router.post("/update-packages", response_model=Response)
async def update_packages(body: list[Package]) -> Response:
    """Install selected software packages."""
    return data_service.update_packages(body)


@router.get("/update-result")
async def get_update_result() -> dict:
    """Return the result of the last software update."""
    return {"result": data_service.get_update_result()}


@router.get("/forced-contents")
async def get_forced_contents() -> dict:
    """Return all forced-content entries."""
    return data_service.get_forced_contents()


@router.put("/forced-contents", response_model=Response)
async def save_forced_contents(body: list[ForcedContent]) -> Response:
    """Save forced content entries."""
    return data_service.save_forced_contents(body)


@router.get("/media")
async def get_media() -> list[Media]:
    """Return available media library items."""
    return data_service.get_media()
