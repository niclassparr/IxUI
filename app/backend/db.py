"""Database connection helper for the IxUI backend.

Provides a simple connection factory backed by psycopg2.
Returns None when no DATABASE_URL is configured so the application
can fall back to mock data gracefully.

Default connection parameters (overridden by DATABASE_URL env var):
    host: 127.0.0.1, port: 5432, dbname: ixui, user: postgres, password: "postgres"
"""

from __future__ import annotations

import logging
import os
from contextlib import contextmanager
from time import monotonic
from typing import Generator

logger = logging.getLogger(__name__)

# Default connection URL matching the local docker-compose database.
# Use 127.0.0.1 instead of localhost to avoid slow IPv6 fallback on Windows.
# Override with the DATABASE_URL environment variable.
_DEFAULT_URL = "postgresql://postgres:postgres@127.0.0.1:5432/ixui"
_FAILURE_COOLDOWN_SECONDS = 5.0
_failure_cache: dict[str, float | str | None] = {
    "timestamp": 0.0,
    "url": None,
}


def get_db_url() -> str | None:
    """Return the configured database URL, or None if not set."""
    return os.environ.get("DATABASE_URL", _DEFAULT_URL) or None


def get_connection():
    """Return a new psycopg2 connection, or None if the DB is unavailable."""
    url = get_db_url()
    if not url:
        logger.warning("DATABASE_URL is not configured – using demo data")
        return None

    now = monotonic()
    last_failure_url = _failure_cache["url"]
    last_failure_timestamp = float(_failure_cache["timestamp"] or 0.0)
    if last_failure_url == url and (now - last_failure_timestamp) < _FAILURE_COOLDOWN_SECONDS:
        return None

    try:
        import psycopg2
        import psycopg2.extras  # noqa: F401 – register UUID/json adapters

        conn = psycopg2.connect(url, connect_timeout=2)
        conn.autocommit = False
        _failure_cache["timestamp"] = 0.0
        _failure_cache["url"] = None
        return conn
    except (psycopg2.Error, OSError) as exc:
        _failure_cache["timestamp"] = monotonic()
        _failure_cache["url"] = url
        logger.warning("Cannot connect to database (%s): %s", url.split('@')[-1] if '@' in url else url, exc)
        return None


def check_connection() -> bool:
    """Test the database connection at startup. Returns True if successful."""
    url = get_db_url()
    if not url:
        logger.warning("No DATABASE_URL configured – app will serve demo data only")
        return False
    try:
        import psycopg2
        conn = psycopg2.connect(url, connect_timeout=2)
        cur = conn.cursor()
        cur.execute("SELECT current_database(), current_user;")
        db_name, db_user = cur.fetchone()
        # Verify critical tables exist
        cur.execute(
            "SELECT table_name FROM information_schema.tables"
            " WHERE table_schema = 'public'"
            " ORDER BY table_name;"
        )
        tables = [row[0] for row in cur.fetchall()]
        cur.close()
        conn.close()
        logger.info(
            "Database connection OK – db=%s user=%s tables=%s",
            db_name, db_user, tables,
        )
        if "interfaces" not in tables:
            logger.warning(
                "Table 'interfaces' not found in database!"
                " Schema may not have been loaded. Found tables: %s", tables
            )
            return False
        return True
    except (psycopg2.Error, OSError) as exc:
        logger.error(
            "Database connection FAILED – app will serve demo data only. Error: %s", exc
        )
        return False


@contextmanager
def db_cursor(conn) -> Generator:
    """Context manager that yields a dict-row cursor and commits/rolls back."""
    import psycopg2.extras

    cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        yield cur
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        cur.close()
