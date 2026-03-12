"""FastAPI application entry point for the IxUI backend.

Mounts all API routers, serves static frontend assets, and provides
a catch-all route for SPA (single-page application) support.
"""

from pathlib import Path

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles

from backend.config import config
from backend.routers import auth, interfaces, routes, settings, system

app = FastAPI(title="IxUI", version="3.2.1")

# -- CORS for development --------------------------------------------------
app.add_middleware(
    CORSMiddleware,
    allow_origins=config.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# -- API routers ------------------------------------------------------------
app.include_router(auth.router)
app.include_router(interfaces.router)
app.include_router(routes.router)
app.include_router(settings.router)
app.include_router(system.router)

# -- Static files & SPA catch-all ------------------------------------------
_FRONTEND_DIR = Path(__file__).resolve().parent.parent / "frontend"
_DIST_DIR = _FRONTEND_DIR / "dist"
_STATIC_DIR = _FRONTEND_DIR / "static"

# Prefer the Vite build output (dist/); fall back to source (index.html).
if _DIST_DIR.is_dir():
    _SERVE_DIR = _DIST_DIR
    _INDEX_HTML = _DIST_DIR / "index.html"
    # Serve Vite build assets (JS/CSS with hashed names)
    _ASSETS_DIR = _DIST_DIR / "assets"
    if _ASSETS_DIR.is_dir():
        app.mount("/assets", StaticFiles(directory=str(_ASSETS_DIR)), name="assets")
else:
    _SERVE_DIR = _FRONTEND_DIR
    _INDEX_HTML = _FRONTEND_DIR / "index.html"

if _STATIC_DIR.is_dir():
    app.mount("/static", StaticFiles(directory=str(_STATIC_DIR)), name="static")


@app.get("/health")
async def health() -> dict[str, str]:
    """Health-check endpoint."""
    return {"status": "ok"}


@app.api_route("/{full_path:path}", methods=["GET"], include_in_schema=False)
async def spa_catch_all(request: Request, full_path: str):
    """Serve index.html for any non-API GET request (SPA support).

    API routes are already registered with higher priority, so this
    catch-all only triggers for paths that don't match an API endpoint.
    """
    if _INDEX_HTML.is_file():
        return FileResponse(str(_INDEX_HTML))
    return JSONResponse(
        {"detail": "Frontend not built yet. Run the frontend build first."},
        status_code=404,
    )
