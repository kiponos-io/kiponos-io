#!/usr/bin/env bash
# Install Kiponos Agent Kit: pip package + agent skill(s).
set -euo pipefail

KIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_SRC="$KIT_DIR/skill"
SKILL_NAME="kiponos-live"

usage() {
  cat <<'EOF'
Usage: ./install.sh [TARGET...]

Default: pip install -e .  +  grok-user, cursor-user, claude-user skills

Targets:
  pip            pip install -e this kit (SDK + MCP entry points)
  grok-user      ~/.grok/skills/kiponos-live/
  cursor-user    ~/.cursor/skills/kiponos-live/
  claude-user    ~/.claude/skills/kiponos-live/
  grok-project   ./.grok/skills/kiponos-live/  (cwd)
  all-skills     all *user skill targets

Examples:
  ./install.sh
  ./install.sh pip grok-user
EOF
}

install_skill() {
  local dest="$1"
  mkdir -p "$(dirname "$dest")"
  rm -rf "$dest"
  mkdir -p "$dest"
  # skill root is skill/ with SKILL.md inside
  cp -a "$SKILL_SRC/." "$dest/"
  echo "Skill -> $dest"
}

do_pip() {
  python3 -m pip install -e "$KIT_DIR" -q
  echo "pip: kiponos + kiponos-mcp + kiponos-cli installed (editable)"
  if command -v kiponos-mcp >/dev/null 2>&1; then
    echo "     kiponos-mcp: $(command -v kiponos-mcp)"
  else
    echo "     (ensure ~/.local/bin or venv bin is on PATH)"
  fi
}

print_mcp_hint() {
  cat <<'EOF'

--- MCP snippet (Grok ~/.grok/config.toml) ---
[mcp_servers.kiponos]
command = "kiponos-mcp"
env = { KIPONOS_ID = "REPLACE", KIPONOS_ACCESS = "REPLACE", KIPONOS = "['my-app']['v1.0.0']['dev']['base']" }
enabled = true
# Get REPLACE values from https://kiponos.io → Connect
-----------------------------------------------
EOF
}

targets=("$@")
if [[ ${#targets[@]} -eq 0 ]]; then
  targets=(pip grok-user cursor-user claude-user)
fi

for t in "${targets[@]}"; do
  case "$t" in
    pip) do_pip ;;
    grok-user)    install_skill "$HOME/.grok/skills/$SKILL_NAME" ;;
    cursor-user)  install_skill "$HOME/.cursor/skills/$SKILL_NAME" ;;
    claude-user)  install_skill "$HOME/.claude/skills/$SKILL_NAME" ;;
    grok-project) install_skill "$(pwd)/.grok/skills/$SKILL_NAME" ;;
    all-skills)
      install_skill "$HOME/.grok/skills/$SKILL_NAME"
      install_skill "$HOME/.cursor/skills/$SKILL_NAME"
      install_skill "$HOME/.claude/skills/$SKILL_NAME"
      ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown target: $t" >&2; usage; exit 1 ;;
  esac
done

print_mcp_hint
echo "Done. Verify: kiponos-cli status   (after exporting KIPONOS_* )"
