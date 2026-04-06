"""Tests for the IxUI backend API endpoints."""

import json
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
def auth_service(monkeypatch):
    monkeypatch.setattr(AuthService, "_get_connection", lambda self: None)
    return AuthService()


@pytest.fixture()
def data_service(monkeypatch):
    monkeypatch.setattr(DataService, "_get_connection", lambda self: None)
    return DataService()


@pytest.fixture()
def db_client(client):
    from backend.db import get_connection

    conn = get_connection()
    if conn is None:
        pytest.skip("Seeded PostgreSQL database is not available")

    try:
        cur = conn.cursor()
        cur.execute("SELECT count(*) FROM interfaces;")
        interface_count = int(cur.fetchone()[0])
        cur.close()
    finally:
        conn.close()

    if interface_count <= 5:
        pytest.skip("Connected database does not look like the seeded parity fixture")

    return client


def _login(client, *, password_candidates=("password", "admin")):
    return _login_with_password(client, password_candidates=password_candidates)[0]


def _login_with_password(client, *, password_candidates=("password", "admin")):
    for password in password_candidates:
        response = client.post(
            "/api/auth/login",
            json={"username": "admin", "password": password},
        )
        if response.status_code == 200 and response.json().get("success"):
            return response, password
    pytest.fail("Could not authenticate with any known admin credential")


def _get_sample_interface(client):
    response = client.get("/api/interfaces/")
    assert response.status_code == 200
    interfaces = response.json()
    assert len(interfaces) > 0
    return next((iface for iface in interfaces if iface.get("active")), interfaces[0])


def _get_interface_with_config(client):
    response = client.get("/api/interfaces/")
    assert response.status_code == 200
    for iface in response.json():
        config_response = client.get(
            f"/api/interfaces/{iface['position']}/config/{iface['type']}"
        )
        if config_response.status_code == 200:
            return iface, config_response
    pytest.fail("No interface with config endpoint available")


def _get_interface_with_services(client):
    response = client.get("/api/interfaces/")
    assert response.status_code == 200
    for iface in response.json():
        services_response = client.get(f"/api/interfaces/{iface['position']}/services")
        if services_response.status_code == 200:
            return iface, services_response
    pytest.fail("No interface with services endpoint available")


def _get_interface_by_type(client, interface_type):
    response = client.get("/api/interfaces/")
    assert response.status_code == 200
    iface = next((item for item in response.json() if item["type"] == interface_type), None)
    if iface is None:
        pytest.fail(f"No interface with type {interface_type} available")
    return iface


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
        body = _login(client).json()
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
        key = _login(client).json()["session_key"]
        r = client.get(f"/api/auth/validate?session_key={key}")
        assert r.status_code == 200
        assert r.json()["valid"] is True

    def test_validate_invalid_session(self, client):
        r = client.get("/api/auth/validate?session_key=bogus")
        assert r.status_code == 200
        assert r.json()["valid"] is False

    def test_logout(self, client):
        key = _login(client).json()["session_key"]
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

    def test_change_password(self, auth_service):
        key = auth_service.login("admin", "admin")
        response = auth_service.change_password(key, "admin", "secret")
        assert response.success is True
        assert auth_service.login("admin", "secret") is not None


# ---------------------------------------------------------------------------
# Interfaces
# ---------------------------------------------------------------------------


class TestInterfaces:
    def test_list_interfaces(self, client):
        r = client.get("/api/interfaces/")
        assert r.status_code == 200
        data = r.json()
        assert len(data) > 0
        assert all(item["position"] for item in data)
        assert all(item["type"] for item in data)

    def test_get_interface(self, client):
        iface = _get_sample_interface(client)
        r = client.get(f"/api/interfaces/{iface['position']}")
        assert r.status_code == 200
        assert r.json()["position"] == iface["position"]
        assert r.json()["type"] == iface["type"]

    def test_get_interface_not_found(self, client):
        r = client.get("/api/interfaces/Z9")
        assert r.status_code == 404

    def test_get_config(self, client):
        iface, response = _get_interface_with_config(client)
        cfg = response.json()
        assert response.status_code == 200
        assert cfg["interface_pos"] == iface["position"]

    def test_get_services(self, client):
        _, response = _get_interface_with_services(client)
        assert response.status_code == 200
        services = response.json()
        assert isinstance(services, list)
        if services:
            assert "name" in services[0]

    def test_interface_status(self, client):
        iface = _get_sample_interface(client)
        r = client.get(f"/api/interfaces/{iface['position']}/status")
        assert r.status_code == 200
        assert isinstance(r.json()["status"], str)

    def test_start_scan(self, client):
        iface = _get_sample_interface(client)
        r = client.post(f"/api/interfaces/{iface['position']}/scan")
        assert r.status_code == 200
        assert r.json()["success"] is True

    def test_apply_interface(self, client):
        iface = _get_sample_interface(client)
        r = client.post(
            f"/api/interfaces/{iface['position']}/apply",
            json={"interface_type": iface["type"]},
        )
        assert r.status_code == 200
        assert r.json()["success"] is True

    def test_scan_result_and_scan_time(self, client):
        iface = _get_sample_interface(client)
        start = client.post(f"/api/interfaces/{iface['position']}/scan")
        assert start.status_code == 200
        assert start.json()["success"] is True

        result = client.get(f"/api/interfaces/{iface['position']}/scan-result")
        assert result.status_code == 200
        services = result.json()
        assert isinstance(services, list)
        assert len(services) > 0
        assert "name" in services[0]

        scan_time = client.get(f"/api/interfaces/{iface['position']}/scan-time")
        assert scan_time.status_code == 200
        assert scan_time.json()["scan_time"] is not None

    def test_interface_command_and_log(self, client):
        iface = _get_sample_interface(client)
        command = client.post(
            f"/api/interfaces/{iface['position']}/command",
            json={"command": "stream"},
        )
        assert command.status_code == 200
        assert command.json()["success"] is True

        status = client.get(
            f"/api/interfaces/{iface['position']}/streamer-status/{iface['type']}"
        )
        assert status.status_code == 200
        assert status.json()["status"] == "running"

        log = client.get(f"/api/interfaces/{iface['position']}/log")
        assert log.status_code == 200
        assert "Streamer started" in log.json()["log"]

    def test_get_current_emm_list(self, client):
        iface = _get_sample_interface(client)
        r = client.get(f"/api/interfaces/{iface['position']}/emm")
        assert r.status_code == 200
        payload = r.json()
        assert "entries" in payload
        assert isinstance(payload["entries"], list)
        assert "selected" in payload

    def test_get_interface_types(self, client):
        r = client.get("/api/interfaces/types/all")
        assert r.status_code == 200
        types = r.json()
        assert len(types) > 0

    def test_hls_wizard_state(self, client):
        r = client.get("/api/interfaces/hls/wizard")
        assert r.status_code == 200
        payload = r.json()
        assert payload["max_services"] >= 0
        assert "available_services" in payload
        assert "selected_services" in payload
        assert "output_interfaces" in payload

    def test_hls_wizard_scan(self, client):
        r = client.post("/api/interfaces/hls/scan")
        assert r.status_code == 200
        payload = r.json()
        assert payload["scanned_at"] is not None
        assert isinstance(payload["filters"], list)

    def test_hls_wizard_save(self, client):
        state = client.get("/api/interfaces/hls/wizard").json()
        payload = [
            {**service, "enabled": True}
            for service in state["selected_services"]
        ] + [
            {**service, "enabled": False}
            for service in state["available_services"]
        ]

        r = client.put("/api/interfaces/hls/services", json=payload)
        assert r.status_code == 200
        assert r.json()["success"] is True

    def test_streamer_status(self, client):
        iface = _get_sample_interface(client)
        r = client.get(
            f"/api/interfaces/{iface['position']}/streamer-status/{iface['type']}"
        )
        assert r.status_code == 200
        data = r.json()
        assert "bitrate" in data
        assert "status" in data

    def test_tuner_status(self, client):
        iface = _get_sample_interface(client)
        r = client.get(
            f"/api/interfaces/{iface['position']}/tuner-status/{iface['type']}"
        )
        assert r.status_code == 200
        data = r.json()
        assert "locked" in data
        assert "signal_strength" in data

    def test_streamer_status_exposes_service_rows(self, client):
        client.post("/api/system/command", json={"command": "reset"})
        iface = _get_sample_interface(client)

        response = client.get(
            f"/api/interfaces/{iface['position']}/streamer-status/{iface['type']}"
        )
        assert response.status_code == 200
        payload = response.json()
        assert isinstance(payload.get("services"), list)
        assert len(payload["services"]) > 0
        assert "name" in payload["services"][0]
        assert "bitrate" in payload["services"][0]

    def test_dsc_status_supports_ci_menu_flow(self, client):
        client.post("/api/system/command", json={"command": "reset"})
        iface = _get_interface_by_type(client, "dsc")

        initial = client.get(
            f"/api/interfaces/{iface['position']}/tuner-status/{iface['type']}"
        )
        assert initial.status_code == 200
        initial_payload = initial.json()
        assert initial_payload["ci_menu_open"] is False
        assert "ca_text" in initial_payload

        opened = client.post(
            f"/api/interfaces/{iface['position']}/command",
            json={"command": "mmi_open"},
        )
        assert opened.status_code == 200
        assert opened.json()["success"] is True

        menu_state = client.get(
            f"/api/interfaces/{iface['position']}/tuner-status/{iface['type']}"
        )
        assert menu_state.status_code == 200
        menu_payload = menu_state.json()
        assert menu_payload["ci_menu_open"] is True
        assert menu_payload["menu_title"] == "Common Interface"
        assert len(menu_payload["menu_items"]) >= 1

        submenu = client.post(
            f"/api/interfaces/{iface['position']}/command",
            json={"command": "mmi_answer 1"},
        )
        assert submenu.status_code == 200
        assert submenu.json()["success"] is True

        submenu_state = client.get(
            f"/api/interfaces/{iface['position']}/tuner-status/{iface['type']}"
        )
        assert submenu_state.status_code == 200
        submenu_payload = submenu_state.json()
        assert submenu_payload["menu_title"] == "Subscription Status"
        assert "Subscription" in submenu_payload["ca_osd"]

        closed = client.post(
            f"/api/interfaces/{iface['position']}/command",
            json={"command": "mmi_close"},
        )
        assert closed.status_code == 200
        assert closed.json()["success"] is True

        final_state = client.get(
            f"/api/interfaces/{iface['position']}/tuner-status/{iface['type']}"
        )
        assert final_state.status_code == 200
        final_payload = final_state.json()
        assert final_payload["ci_menu_open"] is False

    def test_dsc_streamer_status_exposes_mux_usage(self, client):
        client.post("/api/system/command", json={"command": "reset"})
        iface = _get_interface_by_type(client, "dsc")

        response = client.get(
            f"/api/interfaces/{iface['position']}/streamer-status/{iface['type']}"
        )
        assert response.status_code == 200
        payload = response.json()
        assert payload["mux_load"] >= 0
        assert payload["max_mux_load"] >= 0
        assert payload["ca_services"] >= 0
        assert payload["ca_pids"] >= 0
        assert len(payload["services"]) > 0
        assert "mux_load" in payload["services"][0]


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------


class TestRoutes:
    def test_list_routes(self, client):
        r = client.get("/api/routes/")
        assert r.status_code == 200
        data = r.json()
        assert isinstance(data, list)

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
        assert len(data) > 0
        assert all("name" in setting for setting in data)

    def test_update_settings(self, client):
        r = client.get("/api/settings/")
        settings = r.json()
        r = client.put("/api/settings/", json=settings)
        assert r.status_code == 200
        assert r.json()["success"] is True

    def test_get_datetime_state(self, client):
        r = client.get("/api/settings/datetime")
        assert r.status_code == 200
        payload = r.json()
        assert payload["timezone"]
        assert "timezones" in payload

    def test_update_datetime_state(self, client):
        r = client.put(
            "/api/settings/datetime",
            json={
                "timezone": "UTC",
                "ntp_enabled": False,
                "date": "2026-02-10",
                "time": "11:45",
            },
        )
        assert r.status_code == 200
        assert r.json()["success"] is True

        state = client.get("/api/settings/datetime")
        assert state.status_code == 200
        assert state.json()["timezone"] == "UTC"
        assert state.json()["ntp_enabled"] is False

    def test_get_modulators(self, client):
        r = client.get("/api/settings/modulators")
        assert r.status_code == 200
        assert isinstance(r.json(), list)

    def test_update_modulators(self, client):
        r = client.put("/api/settings/modulators", json=[])
        assert r.status_code == 200
        assert r.json()["success"] is True

    def test_change_password(self, client):
        login_response, old_password = _login_with_password(
            client,
            password_candidates=("admin", "password"),
        )
        session_key = login_response.json()["session_key"]

        changed = client.post(
            "/api/settings/password",
            json={
                "session_key": session_key,
                "old_password": old_password,
                "new_password": "secret",
            },
        )
        assert changed.status_code == 200
        assert changed.json()["success"] is True

        login_with_new = client.post(
            "/api/auth/login",
            json={"username": "admin", "password": "secret"},
        )
        assert login_with_new.status_code == 200
        assert login_with_new.json()["success"] is True

        reset = client.post(
            "/api/settings/password",
            json={
                "session_key": login_with_new.json()["session_key"],
                "old_password": "secret",
                "new_password": old_password,
            },
        )
        assert reset.status_code == 200
        assert reset.json()["success"] is True

    def test_get_network_settings(self, client):
        r = client.get("/api/settings/network")
        assert r.status_code == 200
        data = r.json()
        assert len(data) > 0
        assert all(item["name"].startswith("nw_") for item in data)

    def test_update_network_settings(self, client):
        current = client.get("/api/settings/network").json()
        update = client.put(
            "/api/settings/network",
            json={"settings": current, "apply_changes": True},
        )
        assert update.status_code == 200
        assert update.json()["success"] is True

    def test_network_status(self, client):
        r = client.get("/api/settings/network-status")
        assert r.status_code == 200
        data = r.json()
        assert len(data) > 0
        assert "ip" in data[0]
        assert "mac" in data[0]
        assert "status" in data[0]

    def test_network_status2(self, client):
        r = client.get("/api/settings/network-status2")
        assert r.status_code == 200
        data = r.json()
        assert isinstance(data, dict)
        assert len(data) > 0
        sample = next(iter(data.values()))
        assert "status" in sample


# ---------------------------------------------------------------------------
# System
# ---------------------------------------------------------------------------


class TestSystem:
    def test_unit_info(self, client):
        r = client.get("/api/system/unit-info")
        assert r.status_code == 200
        info = r.json()
        assert "serial" in info
        assert "version" in info
        assert "hostname" in info
        assert isinstance(info["cloud"], bool)

    def test_run_command_allowed(self, client):
        r = client.post("/api/system/command", json={"command": "reboot"})
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

    def test_cloud_details_contract(self, client):
        r = client.get("/api/system/cloud-details")
        assert r.status_code == 200
        details = r.json()
        assert "ixcloud_enable" in details
        assert "ixcloud_online" in details
        assert "ixcloud_validate_date" in details
        assert "ixcloud_validate_message" in details
        assert "ixcloud_beaconid" in details

    def test_cloud_connect_disconnect_updates_status(self, client):
        disconnect = client.post("/api/system/command", json={"command": "ixcloud-disconnect"})
        assert disconnect.status_code == 200
        assert disconnect.json()["success"] is True

        offline = client.get("/api/system/cloud-details")
        assert offline.status_code == 200
        assert offline.json()["ixcloud_online"] == "false"

        connect = client.post("/api/system/command", json={"command": "ixcloud-connect"})
        assert connect.status_code == 200
        assert connect.json()["success"] is True

        online = client.get("/api/system/cloud-details")
        assert online.status_code == 200
        assert online.json()["ixcloud_online"] == "true"
        assert online.json()["ixcloud_validate_message"]

        reset = client.post("/api/system/command", json={"command": "ixcloud-disconnect"})
        assert reset.status_code == 200
        assert reset.json()["success"] is True

    def test_forced_content_list_save_and_control(self, client):
        listed = client.get("/api/system/forced-contents")
        assert listed.status_code == 200
        payload = listed.json()
        assert len(payload) == 4

        original_entries = [payload[key] for key in sorted(payload.keys(), key=int)]
        updated_entries = [dict(entry) for entry in original_entries]
        updated_entries[0]["enabled"] = True
        updated_entries[0]["name"] = "Emergency Override"
        updated_entries[0]["networks"] = 3
        updated_entries[0]["ts_filename"] = "emergency-slide.png"
        updated_entries[0]["operation_mode"] = 1
        updated_entries[0]["signal_type"] = 1
        updated_entries[0]["volume"] = 50

        saved = client.put("/api/system/forced-contents", json=updated_entries)
        assert saved.status_code == 200
        assert saved.json()["success"] is True

        enabled = client.get("/api/system/forced-contents/enabled")
        assert enabled.status_code == 200
        enabled_entries = enabled.json()
        assert len(enabled_entries) > 0
        target = next((entry for entry in enabled_entries if entry["id"] == 1), None)
        assert target is not None
        assert target["enabled"] is True

        override = client.post(
            "/api/system/forced-contents/1/override",
            json={"override_index": 2},
        )
        assert override.status_code == 200
        assert override.json()["success"] is True

        controlled = client.get("/api/system/forced-contents/enabled")
        assert controlled.status_code == 200
        controlled_entry = next(entry for entry in controlled.json() if entry["id"] == 1)
        assert controlled_entry["override_index"] == 2
        assert controlled_entry["signal_status"] == 2

        restored = client.put("/api/system/forced-contents", json=original_entries)
        assert restored.status_code == 200
        assert restored.json()["success"] is True

        reset_override = client.post(
            "/api/system/forced-contents/1/override",
            json={"override_index": 0},
        )
        assert reset_override.status_code == 200
        assert reset_override.json()["success"] is True

    def test_command_allstop_and_allstart_affect_runtime(self, client):
        iface = _get_sample_interface(client)

        stopped = client.post("/api/system/command", json={"command": "allstop"})
        assert stopped.status_code == 200
        assert stopped.json()["success"] is True

        streamer_stopped = client.get(
            f"/api/interfaces/{iface['position']}/streamer-status/{iface['type']}"
        )
        assert streamer_stopped.status_code == 200
        assert streamer_stopped.json()["status"] == "stopped"

        started = client.post("/api/system/command", json={"command": "allstart"})
        assert started.status_code == 200
        assert started.json()["success"] is True

        streamer_started = client.get(
            f"/api/interfaces/{iface['position']}/streamer-status/{iface['type']}"
        )
        assert streamer_started.status_code == 200
        assert streamer_started.json()["status"] == "running"

    def test_command_poweroff_and_reboot_affect_runtime(self, client):
        iface = _get_sample_interface(client)

        poweroff = client.post("/api/system/command", json={"command": "poweroff"})
        assert poweroff.status_code == 200
        assert poweroff.json()["success"] is True

        powered_off_status = client.get(f"/api/interfaces/{iface['position']}/status")
        assert powered_off_status.status_code == 200
        assert powered_off_status.json()["status"] == "powered off"

        reboot = client.post("/api/system/command", json={"command": "reboot"})
        assert reboot.status_code == 200
        assert reboot.json()["success"] is True

        rebooted_status = client.get(
            f"/api/interfaces/{iface['position']}/streamer-status/{iface['type']}"
        )
        assert rebooted_status.status_code == 200
        assert rebooted_status.json()["status"] == "running"

    def test_command_reset_restores_demo_defaults(self, client):
        settings = client.get("/api/settings/").json()
        hostname_setting = next(setting for setting in settings if setting["name"] == "nw_hostname")
        modified = [
            {**setting, "value": "changed-host"} if setting["name"] == "nw_hostname" else setting
            for setting in settings
        ]
        changed = client.put("/api/settings/", json=modified)
        assert changed.status_code == 200
        assert changed.json()["success"] is True

        reset = client.post("/api/system/command", json={"command": "reset"})
        assert reset.status_code == 200
        assert reset.json()["success"] is True

        restored_settings = client.get("/api/settings/").json()
        restored_hostname = next(setting for setting in restored_settings if setting["name"] == "nw_hostname")
        assert restored_hostname["value"] == hostname_setting["value"]

    def test_backup_download(self, client):
        r = client.get("/api/system/backup/download")
        assert r.status_code == 200
        assert r.headers["content-type"].startswith("application/json")
        payload = json.loads(r.content)
        assert payload["app"] == "ixui"
        assert "backup_date" in payload
        assert "interfaces" in payload

    def test_backup_upload_and_staged_info(self, client):
        exported = client.get("/api/system/backup/download").content
        upload = client.post(
            "/api/system/backup/upload",
            files={"file": ("backup.json", exported, "application/json")},
        )
        assert upload.status_code == 200
        info = upload.json()
        assert info["valid"] is True
        assert info["interface_count"] >= 1

        staged = client.get("/api/system/backup/staged-info")
        assert staged.status_code == 200
        assert staged.json()["valid"] is True

    def test_restore_staged_backup(self, client):
        exported = client.get("/api/system/backup/download").content
        client.post(
            "/api/system/backup/upload",
            files={"file": ("backup.json", exported, "application/json")},
        )
        restore = client.post("/api/system/backup/restore")
        assert restore.status_code == 200
        assert restore.json()["success"] is True

    def test_document_pdf(self, client):
        r = client.get("/api/system/document/pdf")
        assert r.status_code == 200
        assert r.headers["content-type"].startswith("application/pdf")
        assert r.content.startswith(b"%PDF")

    def test_update_check_install_and_result(self, client):
        check = client.post("/api/system/update/check")
        assert check.status_code == 200
        assert check.json()["success"] is True

        packages = client.get("/api/system/update-packages")
        assert packages.status_code == 200
        payload = packages.json()
        assert len(payload) > 0

        selected = [
            {**package, "update": index == 0}
            for index, package in enumerate(payload)
        ]
        install = client.post("/api/system/update-packages", json=selected)
        assert install.status_code == 200
        assert install.json()["success"] is True

        result = client.get("/api/system/update-result")
        assert result.status_code == 200
        assert "Update finished successfully" in result.json()["result"]

    def test_config_status_and_push_config(self, client):
        clear = client.post("/api/system/push-config")
        assert clear.status_code == 200
        assert clear.json()["success"] is True

        status = client.get("/api/system/config-status")
        assert status.status_code == 200
        assert status.json()["config_changed"] is False

        settings = client.get("/api/settings/").json()
        update = client.put("/api/settings/", json=settings)
        assert update.status_code == 200
        assert update.json()["success"] is True

        changed = client.get("/api/system/config-status")
        assert changed.status_code == 200
        assert changed.json()["config_changed"] is True

        push = client.post("/api/system/push-config")
        assert push.status_code == 200
        assert push.json()["success"] is True

        cleared = client.get("/api/system/config-status")
        assert cleared.status_code == 200
        assert cleared.json()["config_changed"] is False


# ---------------------------------------------------------------------------
# Data service unit tests
# ---------------------------------------------------------------------------


class TestDataService:
    def test_get_interfaces(self, data_service):
        ifaces = data_service.get_interfaces()
        assert len(ifaces) == 5
        assert any(iface.type == "dsc" for iface in ifaces)

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
        assert all(service.type in {"TV_SD", "TV_HD", "RADIO"} for service in services)

    def test_save_services_normalizes_service_types(self, data_service):
        from backend.models import Service

        response = data_service.save_services(
            [
                Service(
                    id=999,
                    interface_pos="A1",
                    name="Normalization Test",
                    sid=1999,
                    type="tv",
                    lang="eng",
                    enabled=True,
                    all_langs=["eng"],
                    key="A1-1999",
                )
            ],
            "dvbs",
            "A1",
        )

        assert response.success is True
        services = data_service.get_services("A1")
        assert len(services) == 1
        assert services[0].type == "TV_SD"

    def test_get_routes(self, data_service):
        routes = data_service.get_routes()
        assert len(routes) == 4

    def test_run_command_allowed(self, data_service):
        resp = data_service.run_command("reboot")
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
        assert hasattr(hosts[0], "status")

    def test_get_network_status_prefers_live_interfaces(self, data_service, monkeypatch):
        monkeypatch.setattr(
            DataService,
            "_get_live_network_interfaces",
            lambda self: {
                "eth0": {"ip": "10.0.0.10", "mac": "00:11:22:33:44:55", "status": "up"},
                "eth1": {"ip": "10.0.1.10", "mac": "00:11:22:33:44:66", "status": "down"},
            },
        )

        hosts = data_service.get_network_status()
        assert hosts[0].ip == "10.0.0.10"
        assert hosts[0].mac == "00:11:22:33:44:55"
        assert hosts[0].status == "up"
        assert hosts[1].ip == "10.0.1.10"
        assert hosts[1].status == "down"

    def test_get_network_status_fallback_skips_loopback_interfaces(self, data_service, monkeypatch):
        monkeypatch.setattr(
            DataService,
            "_get_live_network_interfaces",
            lambda self: {
                "lo": {"ip": "127.0.0.1", "mac": "", "status": "up"},
                "Ethernet": {"ip": "192.168.50.10", "mac": "00:11:22:33:44:77", "status": "up"},
            },
        )

        hosts = data_service.get_network_status()
        assert hosts[0].ip == "192.168.50.10"
        assert hosts[0].mac == "00:11:22:33:44:77"
        assert hosts[0].status == "up"

    def test_get_network_status2_uses_probe_results(self, data_service, monkeypatch):
        from backend.models import IpStatus

        def fake_probe(self, name, ip):
            statuses = {
                "gateway": "online",
                "dns1": "offline",
                "dns2": "unknown",
                "public": "online",
            }
            return IpStatus(ip=str(ip or ""), mac="", status=statuses[name])

        monkeypatch.setattr(DataService, "_probe_network_host", fake_probe)

        result = data_service.get_network_status2()
        assert result["gateway"].status == "online"
        assert result["dns1"].status == "offline"
        assert result["dns2"].status == "unknown"
        assert result["public"].status == "online"

    def test_get_bitrates(self, data_service):
        bitrates = data_service.get_bitrates()
        assert len(bitrates) > 0

    def test_scan_result_builds_default_services_for_empty_interface(self, monkeypatch):
        from backend.services.interface_runtime_service import interface_runtime_service

        monkeypatch.setattr(DataService, "_get_connection", lambda self: None)
        interface_runtime_service.reset_runtime_state()

        try:
            services = interface_runtime_service.get_scan_result("D1")
            assert len(services) > 0
            assert services[0].interface_pos == "D1"
            assert services[0].name
            assert services[0].type in {"TV_SD", "TV_HD", "RADIO"}
        finally:
            interface_runtime_service.reset_runtime_state()

    def test_interface_log_reads_runtime_file_updates(self, monkeypatch, tmp_path):
        from backend.config import config as runtime_config
        from backend.services.interface_runtime_service import interface_runtime_service

        monkeypatch.setattr(runtime_config, "artifact_dir", str(tmp_path))
        monkeypatch.setattr(DataService, "_get_connection", lambda self: None)
        interface_runtime_service.reset_runtime_state()

        try:
            log_dir = tmp_path / "interface-logs"
            log_dir.mkdir(parents=True, exist_ok=True)
            log_path = log_dir / "a1.log"
            log_path.write_text(
                "[2025-01-01 10:00:00] A1: External runtime line\n",
                encoding="utf-8",
            )

            log = interface_runtime_service.get_interface_log("A1")
            assert "External runtime line" in log

            response = interface_runtime_service.run_interface_command("A1", "stream")
            assert response.success is True
            assert "Streamer started" in log_path.read_text(encoding="utf-8")
        finally:
            interface_runtime_service.reset_runtime_state()

    def test_streamer_socket_service_parses_scan_result_xml(self):
        from backend.services.streamer_socket_service import streamer_socket_service

        services = streamer_socket_service._parse_scan_result_xml(
            "A1",
            """
            <response>
              <service>
                <name>SVT 1 HD</name>
                <serviceId>101</serviceId>
                <serviceType>25</serviceType>
                <prefLcn>7</prefLcn>
                <originalNetworkId>11</originalNetworkId>
                <transportStreamId>22</transportStreamId>
                <streamUrl>udp://239.1.1.1:1234</streamUrl>
                <tags>news, nordic</tags>
                <scrambled>true</scrambled>
                <stream><language>swe</language></stream>
                <stream><language>eng</language></stream>
              </service>
            </response>
            """,
        )

        assert services is not None
        assert len(services) == 1
        assert services[0].interface_pos == "A1"
        assert services[0].name == "SVT 1 HD"
        assert services[0].type == "TV_HD"
        assert services[0].sid == 101
        assert services[0].prefered_lcn == 7
        assert services[0].scrambled is True
        assert services[0].all_langs == ["All", "swe", "eng"]
        assert services[0].filters == ["news", "nordic"]
        assert services[0].hls_url == "udp://239.1.1.1:1234"
        assert services[0].epg_url == "11.22.101"

    def test_streamer_socket_service_parses_dsc_tuner_status_xml(self):
        from backend.services.streamer_socket_service import streamer_socket_service

        status = streamer_socket_service._parse_tuner_status_xml(
            "dsc",
            """
            <response>
              <dscTunerStatus>
                <ciStatus>6</ciStatus>
                <caEmm>true</caEmm>
                <caText>CAM ready</caText>
              </dscTunerStatus>
              <caMmiMenu>
                <title>Main menu</title>
                <subTitle>Choose option</subTitle>
                <item>Subscriptions</item>
                <item>Diagnostics</item>
              </caMmiMenu>
              <caMmiOsd>
                <item>Line one</item>
                <item>Line two</item>
              </caMmiOsd>
            </response>
            """,
        )

        assert status is not None
        assert status.ci_status == 6
        assert status.ca_emm is True
        assert status.ca_text == "CAM ready"
        assert status.ca_osd == "Line one\nLine two"
        assert status.menu_title == "Main menu - Choose option"
        assert status.ci_menu_open is True
        assert [item.label for item in status.menu_items] == [
            "Cancel",
            "Subscriptions",
            "Diagnostics",
        ]

    def test_interface_runtime_service_prefers_live_socket_data(self, monkeypatch, tmp_path):
        from backend.config import config as runtime_config
        from backend.models import Service, ServiceStatus, StreamerStatus, TunerStatus
        from backend.services.interface_runtime_service import interface_runtime_service
        from backend.services.streamer_socket_service import streamer_socket_service

        monkeypatch.setattr(runtime_config, "artifact_dir", str(tmp_path))
        monkeypatch.setattr(DataService, "_get_connection", lambda self: None)
        monkeypatch.setattr(
            streamer_socket_service,
            "get_interface_status",
            lambda position: "CONNECTED",
        )
        monkeypatch.setattr(
            streamer_socket_service,
            "start_scan",
            lambda position: True,
        )
        monkeypatch.setattr(
            streamer_socket_service,
            "get_scan_result",
            lambda position: [
                Service(
                    interface_pos=position,
                    name="Live service",
                    sid=501,
                    type="TV_SD",
                    enabled=True,
                )
            ],
        )
        monkeypatch.setattr(
            streamer_socket_service,
            "get_streamer_status",
            lambda position, interface_type, **kwargs: StreamerStatus(
                bitrate=4321,
                status="stopped",
                services=[ServiceStatus(name="Live service", bitrate=4321)],
            ),
        )
        monkeypatch.setattr(
            streamer_socket_service,
            "get_tuner_status",
            lambda position, interface_type: TunerStatus(
                locked=True,
                frequency=123000,
                signal_strength=87,
                snr=41,
            ),
        )
        monkeypatch.setattr(
            streamer_socket_service,
            "get_interface_log",
            lambda position: "live log line\nanother line",
        )
        monkeypatch.setattr(
            streamer_socket_service,
            "run_interface_command",
            lambda position, command: True,
        )

        interface_runtime_service.reset_runtime_state()

        try:
            assert interface_runtime_service.get_interface_status("A1") == "connected"

            response = interface_runtime_service.start_scan("A1")
            assert response.success is True

            services = interface_runtime_service.get_scan_result("A1")
            assert len(services) == 1
            assert services[0].name == "Live service"
            assert services[0].interface_pos == "A1"

            streamer_status = interface_runtime_service.get_streamer_status("A1", "dvbs")
            assert streamer_status.status == "connected"
            assert streamer_status.bitrate == 4321
            assert streamer_status.services[0].name == "Live service"

            tuner_status = interface_runtime_service.get_tuner_status("A1", "dvbs")
            assert tuner_status.locked is True
            assert tuner_status.frequency == 123000
            assert tuner_status.signal_strength == 87

            log = interface_runtime_service.get_interface_log("A1")
            assert log == "live log line\nanother line"
            log_path = tmp_path / "interface-logs" / "a1.log"
            assert "live log line" in log_path.read_text(encoding="utf-8")

            command_response = interface_runtime_service.run_interface_command("A1", "stream")
            assert command_response.success is True
        finally:
            interface_runtime_service.reset_runtime_state()

    def test_interface_runtime_service_returns_error_for_live_command_failure(self, monkeypatch):
        from backend.services.interface_runtime_service import interface_runtime_service
        from backend.services.streamer_socket_service import streamer_socket_service

        monkeypatch.setattr(DataService, "_get_connection", lambda self: None)
        monkeypatch.setattr(
            streamer_socket_service,
            "run_interface_command",
            lambda position, command: False,
        )

        interface_runtime_service.reset_runtime_state()

        try:
            response = interface_runtime_service.run_interface_command("A1", "stream")
            assert response.success is False
            assert response.error == "Streamer command failed"
        finally:
            interface_runtime_service.reset_runtime_state()

    def test_get_forced_contents(self, data_service):
        fc = data_service.get_forced_contents()
        assert len(fc) == 4

    def test_get_enabled_type(self, data_service):
        assert data_service.get_enabled_type("cloud") is True
        assert data_service.get_enabled_type("software_update") is False
        assert data_service.get_enabled_type("nonexistent") is False


class TestDbBackedParity:
    def test_db_backed_login_and_inventory(self, db_client):
        login = db_client.post(
            "/api/auth/login",
            json={"username": "admin", "password": "password"},
        )
        assert login.status_code == 200
        assert login.json()["success"] is True

        interfaces = db_client.get("/api/interfaces/")
        assert interfaces.status_code == 200
        payload = interfaces.json()
        assert len(payload) > 5
        assert any(item["type"] == "hls2ip" for item in payload)
        assert any(item["type"] == "mod" for item in payload)

    def test_db_backed_hls_status_payload(self, db_client):
        interface = _get_interface_by_type(db_client, "hls2ip")

        streamer = db_client.get(
            f"/api/interfaces/{interface['position']}/streamer-status/{interface['type']}"
        )
        assert streamer.status_code == 200
        streamer_payload = streamer.json()
        assert "services" in streamer_payload
        assert isinstance(streamer_payload["services"], list)
        if streamer_payload["services"]:
            row = streamer_payload["services"][0]
            assert "download_bitrate" in row
            assert "selected_bitrate" in row
            assert "buffer_level" in row

        tuner = db_client.get(
            f"/api/interfaces/{interface['position']}/tuner-status/{interface['type']}"
        )
        assert tuner.status_code == 200
        tuner_payload = tuner.json()
        assert "signal_strength" in tuner_payload
        assert "menu_items" in tuner_payload

    def test_db_backed_cloud_connect_disconnect(self, db_client):
        original = db_client.get("/api/system/cloud-details")
        assert original.status_code == 200
        original_payload = original.json()
        original_online = original_payload.get("ixcloud_online", "false")

        try:
            disconnected = db_client.post(
                "/api/system/command",
                json={"command": "ixcloud-disconnect"},
            )
            assert disconnected.status_code == 200
            assert disconnected.json()["success"] is True

            offline = db_client.get("/api/system/cloud-details")
            assert offline.status_code == 200
            assert offline.json()["ixcloud_online"] == "false"
            assert "Disconnected" in offline.json()["ixcloud_validate_message"]

            connected = db_client.post(
                "/api/system/command",
                json={"command": "ixcloud-connect"},
            )
            assert connected.status_code == 200
            assert connected.json()["success"] is True

            online = db_client.get("/api/system/cloud-details")
            assert online.status_code == 200
            assert online.json()["ixcloud_online"] == "true"
            assert "Connected" in online.json()["ixcloud_validate_message"]
        finally:
            restore_command = "ixcloud-connect" if original_online == "true" else "ixcloud-disconnect"
            db_client.post("/api/system/command", json={"command": restore_command})

    def test_db_backed_forced_content_round_trip(self, db_client):
        listed = db_client.get("/api/system/forced-contents")
        assert listed.status_code == 200
        payload = listed.json()
        original_entries = [payload[key] for key in sorted(payload.keys(), key=int)]
        updated_entries = [dict(entry) for entry in original_entries]
        updated_entries[0]["enabled"] = True
        updated_entries[0]["signal_type"] = 1

        try:
            saved = db_client.put("/api/system/forced-contents", json=updated_entries)
            assert saved.status_code == 200
            assert saved.json()["success"] is True

            override = db_client.post(
                f"/api/system/forced-contents/{updated_entries[0]['id']}/override",
                json={"override_index": 2},
            )
            assert override.status_code == 200
            assert override.json()["success"] is True

            enabled = db_client.get("/api/system/forced-contents/enabled")
            assert enabled.status_code == 200
            target = next(
                entry for entry in enabled.json() if entry["id"] == updated_entries[0]["id"]
            )
            assert target["override_index"] == 2
            assert target["signal_status"] == 2
        finally:
            db_client.put("/api/system/forced-contents", json=original_entries)
            db_client.post(
                f"/api/system/forced-contents/{updated_entries[0]['id']}/override",
                json={"override_index": original_entries[0]["override_index"]},
            )

    def test_db_backed_update_lifecycle(self, db_client):
        check = db_client.post("/api/system/update/check")
        assert check.status_code == 200
        assert check.json()["success"] is True

        packages = db_client.get("/api/system/update-packages")
        assert packages.status_code == 200
        package_payload = packages.json()
        assert len(package_payload) > 0

        selected = [
            {**package, "update": index == 0}
            for index, package in enumerate(package_payload)
        ]
        install = db_client.post("/api/system/update-packages", json=selected)
        assert install.status_code == 200
        assert install.json()["success"] is True

        result = db_client.get("/api/system/update-result")
        assert result.status_code == 200
        assert "Update finished successfully" in result.json()["result"]

    def test_db_backed_runtime_commands_affect_active_status(self, db_client):
        interface = next(
            item for item in db_client.get("/api/interfaces/").json() if item["active"]
        )

        try:
            stopped = db_client.post("/api/system/command", json={"command": "allstop"})
            assert stopped.status_code == 200
            assert stopped.json()["success"] is True

            streamer_stopped = db_client.get(
                f"/api/interfaces/{interface['position']}/streamer-status/{interface['type']}"
            )
            assert streamer_stopped.status_code == 200
            assert streamer_stopped.json()["status"] == "stopped"

            started = db_client.post("/api/system/command", json={"command": "allstart"})
            assert started.status_code == 200
            assert started.json()["success"] is True

            streamer_started = db_client.get(
                f"/api/interfaces/{interface['position']}/streamer-status/{interface['type']}"
            )
            assert streamer_started.status_code == 200
            assert streamer_started.json()["status"] == "running"
        finally:
            db_client.post("/api/system/command", json={"command": "allstart"})

    def test_db_backed_non_dsc_emm_round_trip(self, db_client):
        interface = _get_interface_by_type(db_client, "dvbudp")
        config_url = f"/api/interfaces/{interface['position']}/config/{interface['type']}"
        emm_url = f"/api/interfaces/{interface['position']}/emm?is_dsc=false"

        original = db_client.get(config_url)
        assert original.status_code == 200
        original_payload = original.json()
        original_emm = int(original_payload.get("emm") or 0)

        emm_list = db_client.get(emm_url)
        assert emm_list.status_code == 200
        available_slots = [
            entry["value"]
            for entry in emm_list.json()["entries"]
            if entry["value"] != original_emm and not entry["in_use"] and entry["value"] != 0
        ]
        assert available_slots
        target_slot = available_slots[0]

        try:
            updated_payload = {**original_payload, "emm": target_slot}
            saved = db_client.put(config_url, json=updated_payload)
            assert saved.status_code == 200
            assert saved.json()["success"] is True

            refreshed = db_client.get(config_url)
            assert refreshed.status_code == 200
            assert refreshed.json()["emm"] == target_slot

            refreshed_emm = db_client.get(emm_url)
            assert refreshed_emm.status_code == 200
            assert refreshed_emm.json()["selected"] == target_slot
        finally:
            restore = db_client.put(config_url, json={**original_payload, "emm": original_emm})
            assert restore.status_code == 200
            assert restore.json()["success"] is True
