"""Application configuration and feature flags.

Ported from ContextManager.java - centralized config for the IxUI application.
"""

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
