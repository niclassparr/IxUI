"""Application configuration and feature flags.

Ported from ContextManager.java - centralized config for the IxUI application.
"""

import os
import tempfile
from dataclasses import dataclass, field


@dataclass
class AppConfig:
    """Main application configuration with feature flags."""

    app_name: str = "ixui"
    enable_cloud: bool = True
    enable_forced_content: bool = True
    enable_software_update: bool = False
    enable_hls_output: bool = True
    enable_portal: bool = True

    # Server settings
    host: str = "0.0.0.0"
    port: int = 8000
    debug: bool = False
    cors_origins: list[str] = field(
        default_factory=lambda: ["http://localhost:3000", "http://localhost:5173"]
    )

    # Database – defaults match the local docker-compose PostgreSQL instance.
    database_url: str = field(
        default_factory=lambda: os.environ.get(
            "DATABASE_URL", "postgresql://postgres:postgres@127.0.0.1:5432/ixui"
        )
    )
    require_database: bool = field(
        default_factory=lambda: os.environ.get(
            "IXUI_REQUIRE_DATABASE", "false"
        ).lower() in {"1", "true", "yes", "on"}
    )

    # Compatibility/runtime integration settings.
    enable_system_commands: bool = field(
        default_factory=lambda: os.environ.get(
            "IXUI_ENABLE_SYSTEM_COMMANDS", "false"
        ).lower() in {"1", "true", "yes", "on"}
    )
    enable_streamer_socket: bool = field(
        default_factory=lambda: os.environ.get(
            "IXUI_ENABLE_STREAMER_SOCKET", "true"
        ).lower() in {"1", "true", "yes", "on"}
    )
    system_command_path: str = field(
        default_factory=lambda: os.environ.get(
            "IXUI_SYSTEM_COMMAND_PATH", "/usr/bin/ixuiconf"
        )
    )
    streamer_socket_host: str = field(
        default_factory=lambda: os.environ.get(
            "IXUI_STREAMER_SOCKET_HOST", "127.0.0.1"
        )
    )
    streamer_socket_port: int = field(
        default_factory=lambda: int(os.environ.get("IXUI_STREAMER_SOCKET_PORT", "8100"))
    )
    streamer_socket_timeout_seconds: float = field(
        default_factory=lambda: float(
            os.environ.get("IXUI_STREAMER_SOCKET_TIMEOUT_SECONDS", "1.5")
        )
    )
    artifact_dir: str = field(
        default_factory=lambda: os.environ.get(
            "IXUI_ARTIFACT_DIR", os.path.join(tempfile.gettempdir(), "ixui")
        )
    )

    def is_feature_enabled(self, feature_name: str) -> bool | None:
        """Check if a named feature flag is enabled. Returns None if unknown."""
        feature_map = {
            "cloud": self.enable_cloud,
            "forced_content": self.enable_forced_content,
            "software_update": self.enable_software_update,
            "hls_output": self.enable_hls_output,
            "portal": self.enable_portal,
        }
        return feature_map.get(feature_name)


# Singleton config instance
config = AppConfig()
