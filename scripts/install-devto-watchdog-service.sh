#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UNIT_SRC="$ROOT/scripts/devto-watchdog.user.service"
UNIT_DST="$HOME/.config/systemd/user/kiponos-devto-watchdog.service"

mkdir -p "$HOME/.config/devto" "$HOME/.config/systemd/user"
sed "s|%h|$HOME|g" "$UNIT_SRC" > "$UNIT_DST"

systemctl --user daemon-reload
systemctl --user enable --now kiponos-devto-watchdog.service

echo "Installed: $UNIT_DST"
echo "Status:    systemctl --user status kiponos-devto-watchdog"
echo "Logs:      journalctl --user -u kiponos-devto-watchdog -f"