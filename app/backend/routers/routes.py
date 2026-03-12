"""Routes router – output route management and bitrate monitoring."""

from fastapi import APIRouter

from backend.models import Bitrate, Response, Route
from backend.services.data_service import data_service

router = APIRouter(prefix="/api/routes", tags=["routes"])


@router.get("/")
async def list_routes() -> list[Route]:
    """Return all output routes."""
    return data_service.get_routes()


@router.put("/", response_model=Response)
async def update_routes(body: list[Route]) -> Response:
    """Replace the full set of output routes."""
    return data_service.update_routes(body)


@router.get("/bitrates")
async def get_bitrates() -> list[Bitrate]:
    """Return current bitrate measurements per active interface."""
    return data_service.get_bitrates()
