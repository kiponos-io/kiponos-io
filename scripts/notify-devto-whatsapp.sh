#!/usr/bin/env bash
# WhatsApp notify after dev.to publish (+ optional Crunchbase).
# URLs must be on their own line with real newlines (not literal \n).
set -euo pipefail

TO="${WHATSAPP_NOTIFY_TO:-me}"
FAMILY_AGENT_ROOT="${FAMILY_AGENT_ROOT:-$HOME/family-agent}"
SEND_JS="$FAMILY_AGENT_ROOT/.grok/skills/whatsapp/scripts/send.mjs"

if [[ "${WHATSAPP_NOTIFY:-1}" == "0" ]]; then
  exit 0
fi

title="${1:-}"
url="${2:-}"
id="${3:-}"
md_path="${4:-}"
crunchbase="${5:-done}"
extra="${6:-}"

if [[ -z "$title" || -z "$url" ]]; then
  echo "notify-devto-whatsapp: missing title or url" >&2
  echo "Usage: notify-devto-whatsapp.sh TITLE URL [ID] [MD_PATH] [crunchbase_status] [extra_line]" >&2
  exit 1
fi

basename=""
[[ -n "$md_path" ]] && basename=$(basename "$md_path" .md)

# Real newlines via heredoc — WhatsApp auto-links URLs on their own line.
read -r -d '' msg <<EOF || true
dev.to published ✓  Crunchbase: ${crunchbase}

${title}

${url}

id: ${id:-n/a}
EOF

[[ -n "$basename" ]] && msg="${msg}
file: ${basename}"
[[ -n "$extra" ]] && msg="${msg}
${extra}"

if [[ ! -f "$SEND_JS" ]]; then
  echo "notify-devto-whatsapp: send.mjs not found at $SEND_JS" >&2
  exit 1
fi

if node "$SEND_JS" --to "$TO" --message "$msg"; then
  echo "WhatsApp sent to $TO"
else
  echo "WhatsApp notify failed (daemon running? cd family-agent && npm run daemon)" >&2
  exit 1
fi