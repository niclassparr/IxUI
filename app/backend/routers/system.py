"""System router – unit info, commands, features, and JSON diagnostics."""

from fastapi import APIRouter, HTTPException, status

from backend.models import Response, UnitInfo
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
