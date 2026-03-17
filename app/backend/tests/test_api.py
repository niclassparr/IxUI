"""Tests for the IxUI backend API endpoints."""

import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

# Ensure the app package is importable
sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent))

from backend.main import app
from backend.services.auth_service import AuthService
from backend.services.data_service import DataService


@pytest.fixture()
def client():
    return TestClient(app)


@pytest.fixture()
def auth_service():
    return AuthService()


@pytest.fixture()
def data_service():
    return DataService()


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------


def test_health(client):
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json() == {"status": "ok"}


# ---------------------------------------------------------------------------
# Auth
# ---------------------------------------------------------------------------


class TestAuth:
    def test_login_success(self, client):
        r = client.post("/api/auth/login", json={"username": "admin", "password": "admin"})
        assert r.status_code == 200
        body = r.json()
        assert body["success"] is True
        assert body["session_key"] is not None
        assert body["error"] is None

    def test_login_failure(self, client):
        r = client.post("/api/auth/login", json={"username": "admin", "password": "wrong"})
        assert r.status_code == 200
        body = r.json()
        assert body["success"] is False
        assert body["session_key"] is None

    def test_validate_session(self, client):
        r = client.post("/api/auth/login", json={"username": "admin", "password": "admin"})
        key = r.json()["session_key"]
        r = client.get(f"/api/auth/validate?session_key={key}")
        assert r.status_code == 200
        assert r.json()["valid"] is True

    def test_validate_invalid_session(self, client):
        r = client.get("/api/auth/validate?session_key=bogus")
        assert r.status_code == 200
        assert r.json()["valid"] is False

    def test_logout(self, client):
        r = client.post("/api/auth/login", json={"username": "admin", "password": "admin"})
        key = r.json()["session_key"]
        r = client.post(f"/api/auth/logout?session_key={key}")
        assert r.status_code == 204
        r = client.get(f"/api/auth/validate?session_key={key}")
        assert r.json()["valid"] is False


# ---------------------------------------------------------------------------
# Auth service unit tests
# ---------------------------------------------------------------------------


class TestAuthService:
    def test_login_returns_key(self, auth_service):
        key = auth_service.login("admin", "admin")
        assert key is not None
        assert len(key) == 64

    def test_login_bad_password(self, auth_service):
        assert auth_service.login("admin", "bad") is None

    def test_validate_and_logout(self, auth_service):
        key = auth_service.login("admin", "admin")
        assert auth_service.validate_session(key) is True
        auth_service.logout(key)
        assert auth_service.validate_session(key) is False


# ---------------------------------------------------------------------------
# Interfaces
# ---------------------------------------------------------------------------


class TestInterfaces:
    def test_list_interfaces(self, client):
        r = client.get("/api/interfaces/")
        assert r.status_code == 200
        data = r.json()
        assert len(data) == 4
        positions = {i["position"] for i in data}
        assert positions == {"A1", "A2", "B1", "C1"}

    def test_get_interface(self, client):
        r = client.get("/api/interfaces/A1")
        assert r.status_code == 200
        assert r.json()["position"] == "A1"
        assert r.json()["type"] == "dvbs"

    def test_get_interface_not_found(self, client):
        r = client.get("/api/interfaces/Z9")
        assert r.status_code == 404

    def test_get_config(self, client):
        r = client.get("/api/interfaces/A1/config/dvbs")
        assert r.status_code == 200
        cfg = r.json()
        assert cfg["interface_pos"] == "A1"
        assert cfg["freq"] == 12188

    def test_get_services(self, client):
        r = client.get("/api/interfaces/A1/services")
        assert r.status_code == 200
        services = r.json()
        assert len(services) == 2
        names = {s["name"] for s in services}
        assert "CNN International" in names

    def test_interface_status(self, client):
        r = client.get("/api/interfaces/A1/status")
        assert r.status_code == 200
        assert r.json()["status"] == "locked"

    def test_start_scan(self, client):
        r = client.post("/api/interfaces/A1/scan")
        assert r.status_code == 200
        assert r.json()["success"] is True

    def test_get_interface_types(self, client):
        r = client.get("/api/interfaces/types/all")
        assert r.status_code == 200
        types = r.json()
        assert "dvbs" in types
        assert "dvbt" in types

    def test_streamer_status(self, client):
        r = client.get("/api/interfaces/A1/streamer-status/dvbs")
        assert r.status_code == 200
        data = r.json()
        assert "bitrate" in data
        assert "status" in data

    def test_tuner_status(self, client):
        r = client.get("/api/interfaces/A1/tuner-status/dvbs")
        assert r.status_code == 200
        data = r.json()
        assert "locked" in data
        assert "signal_strength" in data


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------


class TestRoutes:
    def test_list_routes(self, client):
        r = client.get("/api/routes/")
        assert r.status_code == 200
        data = r.json()
        assert len(data) == 4

    def test_update_routes(self, client):
        r = client.get("/api/routes/")
        routes = r.json()
        r = client.put("/api/routes/", json=routes)
        assert r.status_code == 200
        assert r.json()["success"] is True

    def test_get_bitrates(self, client):
        r = client.get("/api/routes/bitrates")
        assert r.status_code == 200
        data = r.json()
        assert len(data) > 0
        assert "interface_pos" in data[0]
        assert "bitrate" in data[0]


# ---------------------------------------------------------------------------
# Settings
# ---------------------------------------------------------------------------


class TestSettings:
    def test_get_settings(self, client):
        r = client.get("/api/settings/")
        assert r.status_code == 200
        data = r.json()
        assert len(data) == 15
        names = {s["name"] for s in data}
        assert "nw_hostname" in names
        assert "nw_eth0_ipaddr" in names
        assert "nw_eth1_bootproto" in names

    def test_update_settings(self, client):
        r = client.get("/api/settings/")
        settings = r.json()
        r = client.put("/api/settings/", json=settings)
        assert r.status_code == 200
        assert r.json()["success"] is True

    def test_network_status(self, client):
        r = client.get("/api/settings/network-status")
        assert r.status_code == 200
        data = r.json()
        assert len(data) == 2
        assert "ip" in data[0]
        assert "mac" in data[0]

    def test_network_status2(self, client):
        r = client.get("/api/settings/network-status2")
        assert r.status_code == 200
        data = r.json()
        assert "gateway" in data
        assert data["gateway"]["ip"] == "192.168.0.1"
        assert data["public"]["status"] == "online"


# ---------------------------------------------------------------------------
# System
# ---------------------------------------------------------------------------


class TestSystem:
    def test_unit_info(self, client):
        r = client.get("/api/system/unit-info")
        assert r.status_code == 200
        info = r.json()
        assert info["serial"] == "IXU-2024-001"
        assert info["version"] == "3.2.1"
        assert info["hostname"] == "ixui-unit-1"
        assert info["cloud"] is True

    def test_run_command_allowed(self, client):
        r = client.post("/api/system/command", json={"command": "reboot"})
        assert r.status_code == 200
        assert r.json()["success"] is True

    def test_run_command_network_reload(self, client):
        r = client.post("/api/system/command", json={"command": "wnet"})
        assert r.status_code == 200
        assert r.json()["success"] is True

    def test_run_command_unknown(self, client):
        r = client.post("/api/system/command", json={"command": "rm-rf"})
        assert r.status_code == 200
        assert r.json()["success"] is False

    def test_json_info(self, client):
        r = client.get("/api/system/json-info")
        assert r.status_code == 200
        data = r.json()
        assert "unit" in data
        assert "interfaces" in data
        assert "routes" in data
        assert "settings" in data

    def test_feature_enabled(self, client):
        r = client.get("/api/system/feature/cloud")
        assert r.status_code == 200
        assert r.json()["enabled"] is True

    def test_feature_disabled(self, client):
        r = client.get("/api/system/feature/software_update")
        assert r.status_code == 200
        assert r.json()["enabled"] is False

    def test_feature_unknown(self, client):
        r = client.get("/api/system/feature/nonexistent")
        assert r.status_code == 200
        assert r.json()["enabled"] is False


# ---------------------------------------------------------------------------
# Data service unit tests
# ---------------------------------------------------------------------------


class TestDataService:
    def test_get_interfaces(self, data_service):
        ifaces = data_service.get_interfaces()
        assert len(ifaces) == 4

    def test_get_interface_by_position(self, data_service):
        iface = data_service.get_interface("A1")
        assert iface is not None
        assert iface.name == "Tuner A1"

    def test_get_interface_not_found(self, data_service):
        assert data_service.get_interface("Z9") is None

    def test_get_config(self, data_service):
        cfg = data_service.get_config("A1", "dvbs")
        assert cfg is not None
        assert cfg.freq == 12188

    def test_set_config(self, data_service):
        from backend.models import Config

        cfg = Config(id=99, interface_pos="A1", freq=11000)
        resp = data_service.set_config(cfg, "dvbs")
        assert resp.success is True
        updated = data_service.get_config("A1", "dvbs")
        assert updated.freq == 11000

    def test_get_services(self, data_service):
        services = data_service.get_services("A1")
        assert len(services) == 2

    def test_get_routes(self, data_service):
        routes = data_service.get_routes()
        assert len(routes) == 4

    def test_run_command_allowed(self, data_service):
        resp = data_service.run_command("reboot")
        assert resp.success is True

    def test_run_command_network_reload(self, data_service):
        resp = data_service.run_command("wnet")
        assert resp.success is True

    def test_run_command_disallowed(self, data_service):
        resp = data_service.run_command("dangerous")
        assert resp.success is False

    def test_interface_scan_valid(self, data_service):
        resp = data_service.interface_scan("A1")
        assert resp.success is True

    def test_interface_scan_invalid(self, data_service):
        resp = data_service.interface_scan("Z9")
        assert resp.success is False

    def test_get_unit_info(self, data_service):
        info = data_service.get_unit_info()
        assert info.serial == "IXU-2024-001"

    def test_get_network_status(self, data_service):
        hosts = data_service.get_network_status()
        assert len(hosts) == 2
        assert hosts[0].ip == "192.168.0.73"

    def test_get_bitrates(self, data_service):
        bitrates = data_service.get_bitrates()
        assert len(bitrates) > 0

    def test_get_forced_contents(self, data_service):
        fc = data_service.get_forced_contents()
        assert len(fc) == 3

    def test_get_enabled_type(self, data_service):
        assert data_service.get_enabled_type("cloud") is True
        assert data_service.get_enabled_type("software_update") is False
        assert data_service.get_enabled_type("nonexistent") is False
