"""Kiponos Python SDK — real-time config get/set/subscribe for apps and agents.

Canonical wire protocol matches the Java SDK (Boot 3 ReadyMode).
"""

from .client import (
    AuthError,
    ConnectionError,
    ConnectionState,
    Kiponos,
    KiponosError,
    StaleConfigError,
    TimeoutError,
)
from .paths import join_json_path, parse_path, profile_to_base_path

__version__ = "0.3.0"
__all__ = [
    "Kiponos",
    "KiponosError",
    "ConnectionError",
    "TimeoutError",
    "AuthError",
    "StaleConfigError",
    "ConnectionState",
    "join_json_path",
    "parse_path",
    "profile_to_base_path",
    "__version__",
]
