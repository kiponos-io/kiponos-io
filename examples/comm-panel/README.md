# CommPanel Demo

Swing desktop demo: window title, size, and position driven by live Kiponos config.

## Run

1. Replace placeholders in `build.gradle` (same as golden — tokens + profile from Kiponos.io Connect).
2. Create `Demo/CommPanel` config in your dashboard (the app creates folders if missing via SDK).
3. `./gradlew run`

Main class: `io.kiponos.demo.control.CommPanelMain`

## Note

This example is less polished than `golden/java/`. Use golden for first SDK verification.