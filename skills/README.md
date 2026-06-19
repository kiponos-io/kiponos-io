# Kiponos Agent Skills

Ready-to-use [Agent Skills](https://agentskills.io) for integrating Kiponos.io into Java projects.

## Canonical skill

```
skills/kiponos/
├── SKILL.md                 # Main instructions (agentskills.io format)
├── references/              # Integration contract, Gradle, Maven, Spring Boot
├── assets/                  # kiponos.local.gradle.example
└── scripts/
```

**Skill name:** `kiponos`  
**Trigger:** "integrate Kiponos", "connect kiponos.io SDK", `/kiponos`

## Install

```bash
chmod +x skills/install.sh
./skills/install.sh                    # user-scoped: Grok, Cursor, Claude Code
./skills/install.sh grok-user          # Grok Build only
./skills/install.sh cursor-user        # Cursor only
./skills/install.sh grok-project       # project-scoped in this repo
```

| Platform | User scope | Project scope |
|----------|------------|---------------|
| **Grok Build** | `~/.grok/skills/kiponos/` | `.grok/skills/kiponos/` |
| **Cursor** | `~/.cursor/skills/kiponos/` | `.cursor/skills/kiponos/` |
| **Claude Code** | `~/.claude/skills/kiponos/` | `.claude/skills/kiponos/` |
| **GitHub Copilot / VS Code** | See [Copilot agent skills](https://docs.github.com/en/copilot/concepts/agents/about-agent-skills) | `.github/skills/` (copy skill folder) |
| **OpenAI Codex** | `~/.codex/skills/` | `.codex/skills/` |

Grok Build also discovers `~/.cursor/skills/` and `~/.claude/skills/` for compatibility.

## Usage

After install, ask your agent:

> Integrate Kiponos SDK into this project. Tokens and profile are in kiponos.local.gradle.

Or invoke explicitly: `/kiponos` (Grok), or mention "integrate kiponos" in Cursor/Claude.

## Design

- **One canonical skill** — `skills/kiponos/` is the single source of truth.
- **Platform paths differ only by install location** — same `SKILL.md` everywhere.
- **AGENTS.md** at repo root points agents here when the repo is cloned.
- **golden/java/** (coming) — runnable smoke test referenced by the skill.

Do not edit copies under `.grok/` or `.cursor/` directly; change `skills/kiponos/` and re-run `install.sh`.