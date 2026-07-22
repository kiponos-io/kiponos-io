#!/usr/bin/env python3
"""Super Pattern: Live Factory Method — Python parity."""
from __future__ import annotations
import os, sys
from typing import Protocol

_REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
sys.path.insert(0, os.path.join(_REPO, "agent-kit"))

class Notifier(Protocol):
    def channel(self) -> str: ...
    def send(self, message: str) -> str: ...

class EmailNotifier:
    def __init__(self, from_email: str): self.from_email = from_email
    def channel(self) -> str: return "email"
    def send(self, message: str) -> str: return f"email from={self.from_email} body={message}"

class SmsNotifier:
    def channel(self) -> str: return "sms"
    def send(self, message: str) -> str: return f"sms body={message}"

class PushNotifier:
    def channel(self) -> str: return "push"
    def send(self, message: str) -> str: return f"push body={message}"

class SlackNotifier:
    def __init__(self, hook: str): self.hook = hook
    def channel(self) -> str: return "slack"
    def send(self, message: str) -> str: return f"slack {self.hook} body={message}"

def create_notifier(product: str, from_email: str = "noreply@example.com", slack_hook: str = "#ops-alerts") -> Notifier:
    p = (product or "email").strip().lower()
    if p == "sms": return SmsNotifier()
    if p == "push": return PushNotifier()
    if p == "slack": return SlackNotifier(slack_hook)
    return EmailNotifier(from_email)

def main() -> int:
    msg = sys.argv[1] if len(sys.argv) > 1 else "Order 42 shipped"
    product, from_email, hook = "email", "noreply@example.com", "#ops-alerts"
    try:
        from kiponos import Kiponos
        with Kiponos.connect() as k:
            base = "patterns/factory/notify"
            if k.get(f"{base}/product") is None:
                k.set(f"{base}/product", product)
                k.set(f"{base}/from-email", from_email)
                k.set(f"{base}/slack-hook", hook)
            product = str(k.get(f"{base}/product") or product)
            from_email = str(k.get(f"{base}/from-email") or from_email)
            hook = str(k.get(f"{base}/slack-hook") or hook)
            print("(live hub) factory policy loaded")
    except Exception as ex:
        print(f"(offline demo) {ex.__class__.__name__}")
    n = create_notifier(product, from_email, hook)
    print("product:", n.channel())
    print("receipt:", n.send(msg))
    print("Java twin: examples/java/pattern-factory-live-channel")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
