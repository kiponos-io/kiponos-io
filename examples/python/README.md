# Kiponos Python examples

Parity demos for the Java public examples under `examples/java/`.

| ID | Pattern |
|----|---------|
| `pattern-strategy-live-router` | Live Strategy |
| `pattern-decorator-live-chain` | Live Decorator |
| `pattern-chain-live-fraud` | Live Chain of Responsibility |
| `pattern-state-live-order` | Live State Machine |
| `pattern-factory-live-channel` | Live Factory Method |

Each folder: pure logic tests (`pytest`) + optional live hub via `agent-kit` when `KIPONOS_*` is set.

## Agentic wave

`examples/python/agentic-*` — one Python peer per calendar article, same leaf as Java / React-Node / Angular-Node. See [`../AGENTIC-WAVE.md`](../AGENTIC-WAVE.md).

```bash
cd examples/python/pattern-strategy-live-router
python3 -m pytest -q
python3 strategy_live_router.py
```
