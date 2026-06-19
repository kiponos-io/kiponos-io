#!/usr/bin/env bash
# Install the Kiponos agent skill to user- or project-scoped skill directories.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SKILL_SRC="$SCRIPT_DIR/kiponos"

usage() {
  cat <<'EOF'
Usage: skills/install.sh [TARGET...]

Targets (default: all user-scoped):
  grok-user      ~/.grok/skills/kiponos/
  cursor-user    ~/.cursor/skills/kiponos/
  claude-user    ~/.claude/skills/kiponos/
  grok-project   <repo>/.grok/skills/kiponos/
  cursor-project <repo>/.cursor/skills/kiponos/
  claude-project <repo>/.claude/skills/kiponos/

Examples:
  ./skills/install.sh                          # install to ~/.grok, ~/.cursor, ~/.claude
  ./skills/install.sh grok-user cursor-user    # Grok + Cursor only
  ./skills/install.sh grok-project             # install into this repo (project-scoped)
EOF
}

install_to() {
  local dest="$1"
  mkdir -p "$(dirname "$dest")"
  rm -rf "$dest"
  cp -a "$SKILL_SRC" "$dest"
  echo "Installed -> $dest"
}

targets=("$@")
if [[ ${#targets[@]} -eq 0 ]]; then
  targets=(grok-user cursor-user claude-user)
fi

for t in "${targets[@]}"; do
  case "$t" in
    grok-user)    install_to "$HOME/.grok/skills/kiponos" ;;
    cursor-user)  install_to "$HOME/.cursor/skills/kiponos" ;;
    claude-user)  install_to "$HOME/.claude/skills/kiponos" ;;
    grok-project) install_to "$REPO_ROOT/.grok/skills/kiponos" ;;
    cursor-project) install_to "$REPO_ROOT/.cursor/skills/kiponos" ;;
    claude-project) install_to "$REPO_ROOT/.claude/skills/kiponos" ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown target: $t" >&2; usage; exit 1 ;;
  esac
done