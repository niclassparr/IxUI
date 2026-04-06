"""System router – unit info, commands, exports, and diagnostics."""

from fastapi import APIRouter, File, HTTPException, Query, UploadFile, status
from fastapi.responses import FileResponse, StreamingResponse

from backend.models import (
    ForcedContent,
    ForcedContentOverrideRequest,
    Media,
    Package,
    Response,
    UnitInfo,
)
from backend.services.auth_service import auth_service
from backend.services.data_service import data_service
from backend.services.maintenance_service import maintenance_service

router = APIRouter(prefix="/api/system", tags=["system"])


def _validate_optional_session(session_key: str | None) -> None:
    if session_key and not auth_service.validate_session(session_key):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid session",
        )


@router.get("/unit-info", response_model=UnitInfo)
async def get_unit_info() -> UnitInfo:
    """Return unit hardware and feature information."""
    return data_service.get_unit_info()


@router.post("/command", response_model=Response)
async def run_command(body: dict) -> Response:
    """Execute a named system command."""
    command = body.get("command", "")
    return maintenance_service.run_command(command)


@router.get("/config-status")
async def get_config_status(
    session_key: str | None = Query(default=None),
) -> dict[str, bool]:
    """Return whether configuration changes are waiting to be pushed."""
    _validate_optional_session(session_key)
    return {"config_changed": data_service.get_config_changed()}


@router.post("/push-config", response_model=Response)
async def push_config(
    session_key: str | None = Query(default=None),
) -> Response:
    """Apply pending configuration changes to the runtime layer."""
    _validate_optional_session(session_key)
    return maintenance_service.push_config()


@router.get("/backup/download")
async def download_backup(
    session_key: str | None = Query(default=None),
):
    """Generate and download a JSON backup snapshot."""
    _validate_optional_session(session_key)
    backup_path, filename = maintenance_service.create_backup_file()
    return FileResponse(
        str(backup_path),
        media_type="application/json",
        filename=filename,
    )


@router.post("/backup/upload")
async def upload_backup(
    file: UploadFile = File(...),
    session_key: str | None = Query(default=None),
) -> dict:
    """Upload and stage a backup file for validation or restore."""
    _validate_optional_session(session_key)
    content = await file.read()
    try:
        return maintenance_service.store_uploaded_backup(file.filename or "backup.json", content)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.get("/backup/staged-info")
async def get_staged_backup_info(
    session_key: str | None = Query(default=None),
) -> dict:
    """Return metadata about the currently staged backup file."""
    _validate_optional_session(session_key)
    info = maintenance_service.get_staged_backup_info()
    if info is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No staged backup")
    return info


@router.post("/backup/restore", response_model=Response)
async def restore_backup(
    session_key: str | None = Query(default=None),
) -> Response:
    """Restore state from the currently staged backup file."""
    _validate_optional_session(session_key)
    return maintenance_service.restore_staged_backup()


@router.get("/document/pdf")
async def download_document_pdf(
    session_key: str | None = Query(default=None),
):
    """Generate and download a lightweight installation PDF."""
    _validate_optional_session(session_key)
    pdf_bytes, filename = maintenance_service.generate_document_pdf()
    headers = {"Content-Disposition": f'attachment; filename="{filename}"'}
    return StreamingResponse(
        iter([pdf_bytes]),
        media_type="application/pdf",
        headers=headers,
    )


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
    return maintenance_service.get_update_packages()


@router.post("/update/check", response_model=Response)
async def check_update_packages() -> Response:
    """Run software update discovery and stage the available package list."""
    return maintenance_service.check_for_updates()


@router.post("/update-packages", response_model=Response)
async def update_packages(body: list[Package]) -> Response:
    """Install selected software packages."""
    return maintenance_service.install_update_packages(body)


@router.get("/update-result")
async def get_update_result() -> dict:
    """Return the result of the last software update."""
    return {"result": maintenance_service.get_update_result()}


@router.get("/forced-contents")
async def get_forced_contents() -> dict:
    """Return all forced-content entries."""
    return data_service.get_forced_contents()


@router.put("/forced-contents", response_model=Response)
async def save_forced_contents(body: list[ForcedContent]) -> Response:
    """Save forced content entries."""
    return data_service.save_forced_contents(body)


@router.get("/forced-contents/enabled")
async def get_enabled_forced_contents() -> list[ForcedContent]:
    """Return enabled forced content entries for the live control workflow."""
    return data_service.get_enabled_forced_contents()


@router.post("/forced-contents/{forced_content_id}/override", response_model=Response)
async def save_forced_content_override_status(
    forced_content_id: int,
    body: ForcedContentOverrideRequest,
) -> Response:
    """Persist live forced content override state."""
    return data_service.save_forced_content_override_status(
        forced_content_id,
        body.override_index,
    )


@router.get("/media")
async def get_media() -> list[Media]:
    """Return available media library items."""
    return data_service.get_media()
