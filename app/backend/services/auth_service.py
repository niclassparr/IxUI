"""In-memory session management service.

Provides authentication, session creation, validation, and logout.
Default credentials are stored in memory for the demo application.
"""

import secrets
import time


class AuthService:
    """Handles login, logout, and session validation with in-memory storage."""

    # Default demo credentials
    _USERS: dict[str, str] = {"admin": "admin"}

    # Session expiry: 24 hours
    _SESSION_TTL: int = 86400

    def __init__(self) -> None:
        # Map of session_key -> {"username": str, "created_at": float}
        self._sessions: dict[str, dict] = {}

    def login(self, username: str, password: str) -> str | None:
        """Authenticate and return a session key, or None on failure."""
        if self._USERS.get(username) == password:
            session_key = secrets.token_hex(32)
            self._sessions[session_key] = {
                "username": username,
                "created_at": time.time(),
            }
            return session_key
        return None

    def validate_session(self, session_key: str) -> bool:
        """Check if a session key is valid and not expired."""
        session = self._sessions.get(session_key)
        if session is None:
            return False
        if time.time() - session["created_at"] > self._SESSION_TTL:
            del self._sessions[session_key]
            return False
        return True

    def logout(self, session_key: str) -> None:
        """Invalidate a session."""
        self._sessions.pop(session_key, None)


# Singleton instance
auth_service = AuthService()
