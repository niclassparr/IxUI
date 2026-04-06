"""Service-centric state builder for the HLS wizard."""

from __future__ import annotations

import logging

from backend.models import HlsWizardState, Interface, Response, Route, Service
from backend.services.data_service import data_service
from backend.services.interface_runtime_service import interface_runtime_service

logger = logging.getLogger(__name__)

_EXCLUDED_SOURCE_TYPES = {"hls2ip", "virtual"}


class HlsWizardService:
    """Build and persist the service-centric HLS wizard state."""

    def get_state(self, *, force_scan: bool = False) -> HlsWizardState:
        """Return the current HLS wizard state, optionally forcing a fresh scan."""
        output_interfaces = data_service.get_hls_interfaces()
        source_interfaces = self._get_source_interfaces()

        services = self._collect_services(source_interfaces, force_scan=force_scan)
        selected_names = self._get_selected_names(data_service.get_routes())

        selected_services = [service for service in services if self._service_name(service) in selected_names]
        available_services = [service for service in services if self._service_name(service) not in selected_names]

        filters = sorted(
            {
                tag
                for service in services
                for tag in (service.filters or [])
                if tag
            },
            key=str.lower,
        )

        scanned_at_candidates = [
            interface_runtime_service.get_scan_time(interface.position)
            for interface in source_interfaces
        ]
        scanned_at = max((value for value in scanned_at_candidates if value), default=None)

        return HlsWizardState(
            max_services=len(output_interfaces),
            scanned_at=scanned_at,
            warning="Saving will replace the current HLS interface mapping.",
            output_interfaces=output_interfaces,
            source_interfaces=source_interfaces,
            available_services=available_services,
            selected_services=selected_services,
            filters=filters,
        )

    def save_services(self, services: list[Service]) -> Response:
        """Persist HLS wizard selections after enforcing capacity."""
        max_services = len(data_service.get_hls_interfaces())
        enabled_services = [service for service in services if service.enabled]

        if max_services <= 0:
            return Response(success=False, error="No HLS output interfaces are configured")

        if len(enabled_services) > max_services:
            return Response(
                success=False,
                error=f"Too many services selected for HLS output ({len(enabled_services)}/{max_services})",
            )

        normalized = [self._decorate_service(service, interface_type="unknown") for service in services]
        response = data_service.save_hls_wizard_services(normalized)
        if response.success:
            selected_names = {
                service.name
                for service in normalized
                if service.enabled and (service.name or "").strip()
            }
            flags_response = data_service.set_hls_selection_flags(selected_names)
            if not flags_response.success:
                return flags_response
        return response

    def _get_source_interfaces(self) -> list[Interface]:
        interfaces = interface_runtime_service.list_interfaces()
        return [
            interface
            for interface in interfaces
            if interface.active and interface.type not in _EXCLUDED_SOURCE_TYPES
        ]

    def _collect_services(
        self,
        source_interfaces: list[Interface],
        *,
        force_scan: bool,
    ) -> list[Service]:
        services_by_key: dict[str, Service] = {}

        for interface in source_interfaces:
            if force_scan:
                interface_runtime_service.start_scan(interface.position)

            scanned_services = interface_runtime_service.get_scan_result(interface.position)
            for service in scanned_services:
                decorated = self._decorate_service(service, interface_type=interface.type)
                services_by_key[self._dedupe_key(decorated)] = decorated

        return sorted(
            services_by_key.values(),
            key=lambda service: (service.name.lower(), service.interface_pos, service.sid),
        )

    def _decorate_service(self, service: Service, *, interface_type: str) -> Service:
        base_filters = list(service.filters or [])
        derived_filters = [
            service.interface_pos,
            interface_type,
            service.type,
            service.lang,
            "scrambled" if service.scrambled else "fta",
        ]
        for language in service.all_langs or []:
            derived_filters.append(language)

        unique_filters: list[str] = []
        seen_filters: set[str] = set()
        for value in [*base_filters, *derived_filters]:
            token = (value or "").strip()
            if not token:
                continue
            lowered = token.lower()
            if lowered in seen_filters:
                continue
            seen_filters.add(lowered)
            unique_filters.append(token)

        sid = int(service.sid or service.id or 0)
        key = service.key or f"{service.interface_pos}:{sid}:{service.name}"
        hls_url = service.hls_url or f"/hls/{service.interface_pos}/{sid}/index.m3u8"

        return service.model_copy(
            update={
                "sid": sid,
                "id": int(service.id or sid),
                "key": key,
                "hls_url": hls_url,
                "filters": unique_filters,
                "found": True,
            },
            deep=True,
        )

    def _get_selected_names(self, routes: list[Route]) -> set[str]:
        selected_names = {
            route.service_name
            for route in routes
            if route.hls_enable or route.interface_type == "hls2ip"
        }
        return {
            self._service_name_from_value(name)
            for name in selected_names
        }

    def _service_name(self, service: Service) -> str:
        return self._service_name_from_value(service.name)

    def _dedupe_key(self, service: Service) -> str:
        return f"{self._service_name(service)}|{service.interface_pos}|{service.sid}"

    def _service_name_from_value(self, value: str) -> str:
        return (value or "").strip().lower()


hls_wizard_service = HlsWizardService()