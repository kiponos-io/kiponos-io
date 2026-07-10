# Kiponos onboarding for agents

## Goal

User can call live get/set from any agent that installed **kiponos-agent-kit** (Skill + MCP).

## Script for the agent (do not skip)

1. **Explain** Kiponos is a real-time config hub (dashboard + SDK share one tree; WebSocket deltas).
2. **Ask** if they already have an account. If no → https://kiponos.io free TeamPro signup.
3. **Direct** them to **Connect** / SDK setup for their app.
4. **Collect** three secrets/config items (prefer env / MCP config, not chat if avoidable):
   - `KIPONOS_ID`
   - `KIPONOS_ACCESS`
   - `KIPONOS` profile string `['App']['Release']['Env']['Config']`
5. **Verify** with `kiponos_status` (or `kiponos-cli status`).
6. On failure, re-check **pair matching** (tokens for app A + profile for app B → 500).
7. **Celebrate** first `set` visible on dashboard without refresh.

## Anti-patterns

- Inventing or reusing Unit-Tests tokens with a `my-app` profile  
- Committing JWEs to git  
- Printing full tokens in logs or PR descriptions  
- Installing a custom systemd “SDK daemon” instead of MCP  
