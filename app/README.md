# IxUI — Modern Web Application

A modern web application for managing IPTV/streaming infrastructure, migrated from a legacy Java GWT application to a **Python/FastAPI** backend with a **Lit/Tailwind CSS** frontend.

## Architecture

```
app/
├── backend/                  # Python FastAPI backend
│   ├── main.py              # Application entry point
│   ├── config.py            # Feature flags and configuration
│   ├── models.py            # Pydantic data models
│   ├── routers/             # API endpoint handlers
│   │   ├── auth.py          # Authentication (login/logout/session)
│   │   ├── interfaces.py    # Interface management
│   │   ├── routes.py        # Output route management
│   │   ├── settings.py      # System settings & network status
│   │   └── system.py        # Unit info, commands, features
│   ├── services/            # Business logic
│   │   ├── auth_service.py  # Session management
│   │   └── data_service.py  # Data access (mock data for demo)
│   └── tests/               # pytest test suite
│       └── test_api.py      # API and service tests
├── frontend/                 # Lit + Tailwind CSS frontend
│   ├── index.html           # SPA entry point
│   ├── package.json         # Node.js dependencies
│   ├── vite.config.js       # Vite build configuration
│   ├── src/
│   │   ├── app.js           # Main app shell (routing, layout)
│   │   ├── styles.css       # Tailwind CSS entry
│   │   ├── services/
│   │   │   └── api.js       # API client
│   │   └── components/      # Lit web components
│   │       ├── login-dialog.js
│   │       ├── nav-menu.js
│   │       ├── front-page.js
│   │       ├── interfaces-page.js
│   │       ├── routes-page.js
│   │       ├── settings-page.js
│   │       ├── network-page.js
│   │       └── commands-page.js
│   └── static/images/       # Static assets
└── GWT_APP/                  # Original legacy GWT application
```

## Quick Start

### Docker (recommended)

The easiest way to run the full stack (app + database) is with Docker Compose from
the **repository root**:

```bash
# First run / after code changes – build images and start everything
docker compose up --build

# Subsequent runs – start without rebuilding
docker compose up

# Run in the background
docker compose up --build -d

# Stop all containers
docker compose down

# Stop and remove the database volume (resets all data)
docker compose down -v
```

The app is available at **http://localhost:8000** once the containers are up.

> **How it works:** `Dockerfile` uses a multi-stage build – Node.js compiles the
> Vite/Lit frontend in the first stage, and the built assets are copied into a
> slim Python image that serves them via FastAPI.

#### Production: change the database password

The default database password is `postgres`. For production deployments set the
`POSTGRES_PASSWORD` environment variable before starting the stack:

```bash
export POSTGRES_PASSWORD=<your-strong-password>
docker compose up --build -d
```

---

### Manual Setup

#### Prerequisites

- Python 3.12+
- Node.js 18+

#### Backend

```bash
cd app
pip install -r backend/requirements.txt
PYTHONPATH=. python -m uvicorn backend.main:app --reload --port 8000
```

#### Frontend (Development)

```bash
cd app/frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:3000` and proxies API requests to the backend.

#### Frontend (Production Build)

```bash
cd app/frontend
npm run build
```

Then serve everything through the backend at `http://localhost:8000`.

### Run Tests

```bash
cd app
PYTHONPATH=. python -m pytest backend/tests/ -v
```

## API Endpoints

| Method | Endpoint                                      | Description                     |
|--------|-----------------------------------------------|---------------------------------|
| GET    | `/health`                                     | Health check                    |
| POST   | `/api/auth/login`                             | Login with credentials          |
| POST   | `/api/auth/logout`                            | Logout session                  |
| GET    | `/api/auth/validate`                          | Validate session key            |
| GET    | `/api/interfaces/`                            | List all interfaces             |
| GET    | `/api/interfaces/{pos}`                       | Get interface details           |
| GET    | `/api/interfaces/{pos}/config/{type}`         | Get interface config            |
| PUT    | `/api/interfaces/{pos}/config/{type}`         | Update interface config         |
| GET    | `/api/interfaces/{pos}/services`              | Get services for interface      |
| PUT    | `/api/interfaces/{pos}/services`              | Save services                   |
| POST   | `/api/interfaces/{pos}/scan`                  | Start channel scan              |
| GET    | `/api/interfaces/{pos}/status`                | Get interface status            |
| GET    | `/api/interfaces/{pos}/streamer-status/{type}`| Get streamer status             |
| GET    | `/api/interfaces/{pos}/tuner-status/{type}`   | Get tuner status                |
| GET    | `/api/interfaces/types/all`                   | List interface types            |
| GET    | `/api/routes/`                                | List all routes                 |
| PUT    | `/api/routes/`                                | Update routes                   |
| GET    | `/api/routes/bitrates`                        | Get current bitrates            |
| GET    | `/api/settings/`                              | Get all settings                |
| PUT    | `/api/settings/`                              | Update settings                 |
| GET    | `/api/settings/network-status`                | Get network status (IP/MAC)     |
| GET    | `/api/settings/network-status2`               | Get enriched network status     |
| GET    | `/api/system/unit-info`                       | Get unit information            |
| POST   | `/api/system/command`                         | Execute system command           |
| GET    | `/api/system/json-info`                       | Get aggregated JSON info        |
| GET    | `/api/system/feature/{name}`                  | Check feature flag status       |

## Technology Stack

- **Backend:** Python 3.12+, FastAPI, Pydantic v2, Uvicorn
- **Frontend:** Lit 3.x (web components), Tailwind CSS v4, Vite
- **Testing:** pytest, FastAPI TestClient

## Default Credentials

- Username: `admin`
- Password: `admin`

## Migration Notes

This application was migrated from a legacy Java GWT application. See `GWT_APP_MIGRATION_GUIDE.md` for the full migration guide and `GWT_APP/` for the original source code.
