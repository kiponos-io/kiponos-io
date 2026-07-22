---
title: "Chain of Responsibility, But the Order Lives in the Hub"
published: false
tags: java, designpatterns, devops, kiponos
---

# Chain of Responsibility, But the Order Lives in the Hub

Fraud handlers should reorder when attackers do — without a release train.

This is the **dev.to** essay twin (unique prose) of the Medium Super Pattern series.
Clone the runnable example from the public repo — do not treat this post as the full source.

## Idea

Gang of Four gives structure. Kiponos gives a realtime policy tree so humans and remote SDKs
can change the pattern’s *inner selection* without redeploy.

## Try it

See the matching `examples/java/pattern-*` folder on
[github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).

Python parity lives under `examples/python/`.

## Moral

People should not have to ship a release to make a decision.
