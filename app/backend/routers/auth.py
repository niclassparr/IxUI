"""Authentication router – login, logout, session validation."""

from fastapi import APIRouter, HTTPException, Query, status

from backend.models import LoginRequest, LoginResponse
from backend.services.auth_service import auth_service

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post("/login", response_model=LoginResponse)
async def login(body: LoginRequest) -> LoginResponse:
    """Authenticate with username/password and receive a session key."""
    session_key = auth_service.login(body.username, body.password)
    if session_key is None:
        return LoginResponse(success=False, error="Invalid credentials")
    return LoginResponse(session_key=session_key, success=True)


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
async def logout(session_key: str = Query(...)) -> None:
    """Invalidate the given session key."""
    auth_service.logout(session_key)


@router.get("/validate")
async def validate(session_key: str = Query(...)) -> dict[str, bool]:
    """Check whether a session key is still valid."""
    return {"valid": auth_service.validate_session(session_key)}
