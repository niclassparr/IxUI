"""Maintenance, export, and compatibility helpers for IxUI.

This service provides a safe abstraction for command-style operations and
implements backup/restore/document generation in a way that still works when
the legacy runtime tools are not available.
"""

from __future__ import annotations

import json
import logging
import subprocess
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from backend.config import config
from backend.models import Package
from backend.models import Response
from backend.services.data_service import data_service
from backend.services.interface_runtime_service import interface_runtime_service

logger = logging.getLogger(__name__)


class MaintenanceService:
    """Compatibility service for operational workflows."""

    _COMMAND_MAP: dict[str, list[str]] = {
        "poweroff": ["--poweroff"],
        "reboot": ["--reboot"],
        "netrestart": ["--netrestart"],
        "reset": ["--reset"],
        "allstart": ["--allstart"],
        "allstop": ["--allstop"],
        "push-config": ["--dvbconf"],
        "ixcloud-connect": ["--ixcloud-connect"],
        "ixcloud-disconnect": ["--ixcloud-disconnect"],
        "wnet": ["--wnet"],
        "restart-services": ["--restart-services"],
        "factory-reset": ["--reset"],
        "update-epg": ["--update-epg"],
        # Legacy page action; real parity for interface update is a later step.
        "update-interfaces": ["--update-interfaces"],
    }

    def _artifact_dir(self) -> Path:
        path = Path(config.artifact_dir)
        path.mkdir(parents=True, exist_ok=True)
        return path

    def _staged_backup_path(self) -> Path:
        return self._artifact_dir() / "ixui_backup.json"

    def _update_packages_path(self) -> Path:
        return self._artifact_dir() / "ixui_check_sw.json"

    def _update_result_path(self) -> Path:
        return self._artifact_dir() / "ixui_update_sw.txt"

    def _clear_compatibility_artifacts(self) -> None:
        for path in [
            self._staged_backup_path(),
            self._update_packages_path(),
            self._update_result_path(),
        ]:
            try:
                if path.is_file():
                    path.unlink()
            except OSError:
                logger.warning("Could not remove compatibility artifact %s", path)

    def _current_timestamp(self) -> str:
        return datetime.now(timezone.utc).isoformat()

    def _current_date(self) -> str:
        return datetime.now(timezone.utc).strftime("%Y-%m-%d")

    def _build_backup_payload(self) -> dict[str, Any]:
        payload = data_service.export_backup_state()
        payload["app"] = config.app_name
        payload["schema_version"] = 1
        payload["backup_date"] = self._current_timestamp()
        return payload

    def _read_backup_snapshot(self, path: Path) -> dict[str, Any]:
        with path.open("r", encoding="utf-8") as handle:
            payload = json.load(handle)
        if not isinstance(payload, dict):
            raise ValueError("Backup file must contain a JSON object")
        return payload

    def _summarize_backup_snapshot(
        self, snapshot: dict[str, Any], *, filename: str | None = None
    ) -> dict[str, Any]:
        unit = snapshot.get("unit") or {}
        return {
            "valid": True,
            "filename": filename,
            "backup_date": snapshot.get("backup_date"),
            "serial": unit.get("serial", ""),
            "setting_count": len(snapshot.get("settings") or []),
            "interface_count": len(snapshot.get("interfaces") or []),
            "route_count": len(snapshot.get("routes") or []),
            "forced_content_count": len(snapshot.get("forced_contents") or []),
        }

    def create_backup_file(self) -> tuple[Path, str]:
        """Generate a backup JSON file and return the saved path and download name."""
        snapshot = self._build_backup_payload()
        backup_path = self._staged_backup_path()
        backup_path.write_text(json.dumps(snapshot, indent=2), encoding="utf-8")

        serial = (snapshot.get("unit") or {}).get("serial") or "no_serial"
        filename = f"ixui_backup_{serial}_{self._current_date()}.json"
        return backup_path, filename

    def store_uploaded_backup(self, filename: str, content: bytes) -> dict[str, Any]:
        """Validate and persist an uploaded backup file."""
        try:
            snapshot = json.loads(content.decode("utf-8"))
        except Exception as exc:
            raise ValueError("Invalid backup JSON") from exc

        if not isinstance(snapshot, dict):
            raise ValueError("Invalid backup JSON")

        backup_path = self._staged_backup_path()
        backup_path.write_text(json.dumps(snapshot, indent=2), encoding="utf-8")
        return self._summarize_backup_snapshot(snapshot, filename=filename)

    def get_staged_backup_info(self) -> dict[str, Any] | None:
        """Return metadata about the currently staged backup file, if any."""
        backup_path = self._staged_backup_path()
        if not backup_path.is_file():
            return None
        snapshot = self._read_backup_snapshot(backup_path)
        return self._summarize_backup_snapshot(snapshot, filename=backup_path.name)

    def restore_staged_backup(self) -> Response:
        """Restore state from the currently staged backup file."""
        backup_path = self._staged_backup_path()
        if not backup_path.is_file():
            return Response(success=False, error="No staged backup file found")

        try:
            snapshot = self._read_backup_snapshot(backup_path)
        except Exception as exc:
            return Response(success=False, error=str(exc))

        return data_service.restore_backup_state(snapshot)

    def generate_document_pdf(self) -> tuple[bytes, str]:
        """Generate a lightweight PDF installation summary."""
        backup_payload = self._build_backup_payload()
        unit = backup_payload.get("unit") or {}
        interfaces = backup_payload.get("interfaces") or []
        routes = backup_payload.get("routes") or []
        settings = backup_payload.get("settings") or []

        lines = [
            "IxUI Installation Document",
            "",
            f"Generated: {self._current_timestamp()}",
            f"Serial: {unit.get('serial', '')}",
            f"Version: {unit.get('version', '')}",
            f"Hostname: {unit.get('hostname', '')}",
            "",
            f"Interfaces: {len(interfaces)}",
            f"Routes: {len(routes)}",
            f"Settings: {len(settings)}",
            "",
            "Interface Summary:",
        ]

        for entry in interfaces[:20]:
            iface = entry.get("interface") or {}
            lines.append(
                f"- {iface.get('position', '')}: {iface.get('name', '')} [{iface.get('type', '')}]"
            )

        if len(interfaces) > 20:
            lines.append(f"... {len(interfaces) - 20} more interface(s)")

        pdf_bytes = self._build_simple_pdf(lines)
        serial = unit.get("serial") or "no_serial"
        filename = f"installation_{serial}_{self._current_date()}.pdf"
        return pdf_bytes, filename

    def _compatibility_update_catalog(self) -> list[Package]:
        return [
            Package(name="ixui-core", version="3.2.2", installed=False),
            Package(name="ixui-hls-plugin", version="1.4.1", installed=False),
            Package(name="ixui-cloud-agent", version="2.0.3", installed=False),
        ]

    def _write_update_packages(self, packages: list[Package]) -> None:
        payload = [package.model_dump() for package in packages]
        self._update_packages_path().write_text(
            json.dumps(payload, indent=2),
            encoding="utf-8",
        )

    def _write_update_result(self, text: str) -> None:
        self._update_result_path().write_text(text, encoding="utf-8")

    def _read_update_packages(self) -> list[Package]:
        path = self._update_packages_path()
        if not path.is_file():
            return []
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            return []
        if not isinstance(payload, list):
            return []
        return [Package.model_validate(item) for item in payload]

    def _read_update_result(self) -> str:
        path = self._update_result_path()
        if not path.is_file():
            return "No software update has been run yet."
        try:
            return path.read_text(encoding="utf-8")
        except Exception:
            return "An error occurred during reading the results of the update."

    def _execute_external_command(self, command: str) -> Response:
        args = self._COMMAND_MAP.get(command)
        if args is None:
            return Response(success=False, error=f"Unknown command: {command}")

        command_path = Path(config.system_command_path)
        if not config.enable_system_commands or not command_path.is_file():
            logger.info(
                "Simulating command %s because IXUI system commands are disabled or unavailable",
                command,
            )
            return Response(success=True)

        try:
            completed = subprocess.run(
                [str(command_path), *args],
                check=False,
                capture_output=True,
                text=True,
                timeout=60,
            )
        except Exception as exc:
            logger.error("Command %s failed to execute: %s", command, exc)
            return Response(success=False, error=str(exc))

        if completed.returncode != 0:
            error = (completed.stderr or completed.stdout or "Command failed").strip()
            return Response(success=False, error=error)

        return Response(success=True)

    def _execute_update_command(self, command: str, *args: str) -> Response:
        command_path = Path(config.system_command_path)
        if not config.enable_system_commands or not command_path.is_file():
            logger.info(
                "Simulating update command %s because IXUI system commands are disabled or unavailable",
                " ".join([command, *args]),
            )
            return Response(success=True)

        try:
            completed = subprocess.run(
                [str(command_path), f"--{command}", *args],
                check=False,
                capture_output=True,
                text=True,
                timeout=300,
            )
        except Exception as exc:
            logger.error(
                "Update command %s failed to execute: %s",
                " ".join([command, *args]),
                exc,
            )
            return Response(success=False, error=str(exc))

        if completed.returncode == 0:
            return Response(success=True)
        if completed.returncode == 2:
            return Response(success=False, error="Network error, please check your Internet access.")
        return Response(success=False, error="An error occured while checking for available updates.")

    def run_command(self, command: str) -> Response:
        """Run a safe allowlisted command or simulate it in compatibility mode."""
        if command in {"ixcloud-connect", "ixcloud-disconnect"} and not data_service.get_enabled_type(
            "cloud"
        ):
            return Response(success=False, error="The cloud function is disabled.")

        response = self._execute_external_command(command)
        if not response.success:
            return response

        if command == "ixcloud-connect":
            state_response = data_service.set_cloud_connection_state(True)
            if not state_response.success:
                return state_response
        elif command == "ixcloud-disconnect":
            state_response = data_service.set_cloud_connection_state(False)
            if not state_response.success:
                return state_response
        elif command == "allstart":
            return interface_runtime_service.start_all_interfaces()
        elif command == "allstop":
            return interface_runtime_service.stop_all_interfaces()
        elif command == "poweroff":
            return interface_runtime_service.poweroff_runtime()
        elif command == "reboot":
            return interface_runtime_service.reboot_runtime()
        elif command == "update-interfaces":
            inventory_response = data_service.reconcile_interface_inventory()
            if not inventory_response.success:
                return inventory_response
            return interface_runtime_service.update_interface_inventory()
        elif command in {"reset", "factory-reset"}:
            data_service.reset_demo_state()
            interface_runtime_service.reset_runtime_state()
            self._clear_compatibility_artifacts()

        return response

    def push_config(self) -> Response:
        """Apply pending configuration and clear the config-changed marker on success."""
        response = self._execute_external_command("push-config")
        if response.success:
            data_service.clear_config_changed()
        return response

    def check_for_updates(self) -> Response:
        """Run the legacy-style software update discovery step."""
        response = self._execute_update_command("check-sw")
        if not response.success:
            return response

        packages = self._compatibility_update_catalog()
        self._write_update_packages(packages)
        self._write_update_result(
            f"[{self._current_timestamp()}] Update check completed. {len(packages)} package(s) available."
        )
        return Response(success=True)

    def get_update_packages(self) -> list[Package]:
        """Return the packages discovered by the most recent check run."""
        return self._read_update_packages()

    def install_update_packages(self, packages: list[Package]) -> Response:
        """Install selected packages from the most recent discovery step."""
        selected_packages = [package for package in packages if package.update]
        if not selected_packages:
            return Response(success=False, error="No packages selected.")

        response = self._execute_update_command(
            "update-sw",
            *[package.name for package in selected_packages],
        )
        if not response.success:
            return response

        discovered_packages = self._read_update_packages()
        selected_names = {package.name for package in selected_packages}
        remaining_packages = [
            package for package in discovered_packages if package.name not in selected_names
        ]
        self._write_update_packages(remaining_packages)

        result_lines = [
            f"[{self._current_timestamp()}] Update finished successfully.",
            "Installed packages:",
            *[f"- {package.name} {package.version or ''}".rstrip() for package in selected_packages],
        ]
        self._write_update_result("\n".join(result_lines))
        return Response(success=True)

    def get_update_result(self) -> str:
        """Return the result text from the most recent update operation."""
        return self._read_update_result()

    def _build_simple_pdf(self, lines: list[str]) -> bytes:
        """Generate a small valid PDF without external dependencies."""

        def _escape(text: str) -> str:
            return (
                text.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
            )

        content_lines = [
            "BT",
            "/F1 11 Tf",
            "72 760 Td",
            "14 TL",
        ]
        for line in lines:
            content_lines.append(f"({_escape(line)}) Tj")
            content_lines.append("T*")
        content_lines.append("ET")
        content = "\n".join(content_lines).encode("latin-1", errors="replace")

        objects = [
            b"1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            b"2 0 obj\n<< /Type /Pages /Count 1 /Kids [3 0 R] >>\nendobj\n",
            (
                b"3 0 obj\n"
                b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                b"/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\n"
                b"endobj\n"
            ),
            b"4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n",
            (
                f"5 0 obj\n<< /Length {len(content)} >>\nstream\n".encode("ascii")
                + content
                + b"\nendstream\nendobj\n"
            ),
        ]

        pdf = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
        offsets = [0]
        for obj in objects:
            offsets.append(len(pdf))
            pdf.extend(obj)

        xref_start = len(pdf)
        pdf.extend(f"xref\n0 {len(objects) + 1}\n".encode("ascii"))
        pdf.extend(b"0000000000 65535 f \n")
        for offset in offsets[1:]:
            pdf.extend(f"{offset:010d} 00000 n \n".encode("ascii"))
        pdf.extend(
            (
                f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\n"
                f"startxref\n{xref_start}\n%%EOF"
            ).encode("ascii")
        )
        return bytes(pdf)


maintenance_service = MaintenanceService()