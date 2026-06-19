# Public Sandbox (planned)

## Goal

Let anyone run `golden/java` **before signing up**, using intentional public read-only tokens — then convert to their own TeamPro account for the full dashboard experience.

## Why signup is still required for the full experience

SDK tokens allow programmatic **read** (and write, when permitted) against the config tree. Viewing and editing the online dashboard — the visual config tree, team collaboration, locks, and admin features — requires a **web user account**.

So the funnel is:

```
Public sandbox (read-only tokens in golden) → "./gradlew run works instantly"
        ↓
Sign up free TeamPro → dashboard, own config tree, team features
        ↓
Replace placeholders with your tokens → full read/write for your app
```

## Planned public account structure

```
kiponos-public (team)
├── golden/          # Values matching golden/java demo
├── community/       # Shared developer resources (moderated write)
└── announcements/   # SDK version pins, notices
```

Profile example: `['kiponos-public']['community']['shared']['golden']`

## Security model (in progress)

| Capability | Status |
|------------|--------|
| Read-only tokens for public tree | Planned |
| Contributor write (moderated) | Planned |
| Folder lock (no delete/rename) | UI + server ready; metadata persistence in progress |
| Key lock (no delete/rename) | UI + server ready; metadata persistence in progress |
| Value lock (no change) | UI + server ready; metadata persistence in progress |
| Read-only web users | Planned after auth integration |

Team admins will lock folders, keys, and values from the dashboard. Locked resources cannot be modified until unlocked by an authorized role.

## Until launch

Use placeholders in [`golden/java/build.gradle`](../golden/java/build.gradle) with tokens from your own TeamPro account. See [`GETTING-STARTED.md`](GETTING-STARTED.md).