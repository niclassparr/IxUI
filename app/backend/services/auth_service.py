"""Session management service backed by PostgreSQL (users / user_sessions tables).

Falls back to in-memory storage when the database is unavailable so that the
application continues to work without a running PostgreSQL instance (e.g. in
tests or local development without a DB).

DB schema (from ixui_73_260312.sql):
  users        (id, name, password, edit, view)
  user_sessions(id, created, user_id, session_key)
"""

import logging
import secrets
import time

logger = logging.getLogger(__name__)


class AuthService:
    """Handles login, logout, and session validation.

    When a database connection can be established the service stores sessions
    in the ``user_sessions`` table and validates passwords against the
    ``users`` table.  When the database is unreachable it falls back to an
    in-memory credential store so the application remains usable.
    """

    # Fallback demo credentials used when no DB is available
    _FALLBACK_USERS: dict[str, str] = {"admin": "admin"}

    # Session expiry: 24 hours (in-memory fallback only)
    _SESSION_TTL: int = 86400

    def __init__(self) -> None:
        # In-memory fallback session store
        self._sessions: dict[str, dict] = {}

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _get_connection(self):
        from backend.db import get_connection
        return get_connection()

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def login(self, username: str, password: str) -> str | None:
        """Authenticate and return a session key, or None on failure."""
        conn = self._get_connection()
        if conn is not None:
            return self._login_db(conn, username, password)
        return self._login_memory(username, password)

    def validate_session(self, session_key: str) -> bool:
        """Check if a session key is valid and not expired."""
        conn = self._get_connection()
        if conn is not None:
            return self._validate_db(conn, session_key)
        return self._validate_memory(session_key)

    def logout(self, session_key: str) -> None:
        """Invalidate a session."""
        conn = self._get_connection()
        if conn is not None:
            self._logout_db(conn, session_key)
        # Always clean up in-memory store too
        self._sessions.pop(session_key, None)

    # ------------------------------------------------------------------
    # DB-backed implementations
    # ------------------------------------------------------------------

    def _login_db(self, conn, username: str, password: str) -> str | None:
        from backend.db import db_cursor
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT id, password FROM users WHERE name = %s;",
                    (username,),
                )
                row = cur.fetchone()
                if row is None or row["password"] != password:
                    return None
                uid = row["id"]
                session_key = secrets.token_hex(32)
                # Remove stale sessions older than 10 days
                cur.execute(
                    "DELETE FROM user_sessions"
                    " WHERE user_id = %s AND created < NOW() - INTERVAL '10 days';",
                    (uid,),
                )
                cur.execute(
                    "INSERT INTO user_sessions (user_id, session_key)"
                    " VALUES (%s, %s);",
                    (uid, session_key),
                )
            return session_key
        except Exception as exc:
            logger.error("DB login error: %s", exc)
            return None
        finally:
            conn.close()

    def _validate_db(self, conn, session_key: str) -> bool:
        from backend.db import db_cursor
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "SELECT id FROM user_sessions WHERE session_key = %s;",
                    (session_key,),
                )
                return cur.fetchone() is not None
        except Exception as exc:
            logger.error("DB validate_session error: %s", exc)
            return False
        finally:
            conn.close()

    def _logout_db(self, conn, session_key: str) -> None:
        from backend.db import db_cursor
        try:
            with db_cursor(conn) as cur:
                cur.execute(
                    "DELETE FROM user_sessions WHERE session_key = %s;",
                    (session_key,),
                )
        except Exception as exc:
            logger.error("DB logout error: %s", exc)
        finally:
            conn.close()

    # ------------------------------------------------------------------
    # In-memory fallback implementations
    # ------------------------------------------------------------------

    def _login_memory(self, username: str, password: str) -> str | None:
        if self._FALLBACK_USERS.get(username) == password:
            session_key = secrets.token_hex(32)
            self._sessions[session_key] = {
                "username": username,
                "created_at": time.time(),
            }
            return session_key
        return None

    def _validate_memory(self, session_key: str) -> bool:
        session = self._sessions.get(session_key)
        if session is None:
            return False
        if time.time() - session["created_at"] > self._SESSION_TTL:
            del self._sessions[session_key]
            return False
        return True


# Singleton instance
auth_service = AuthService()
