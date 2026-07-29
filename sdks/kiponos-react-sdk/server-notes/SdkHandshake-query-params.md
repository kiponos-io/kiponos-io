# Server change required for browser SDK clients

## Problem

Browsers **cannot set custom HTTP headers** on the WebSocket handshake.
Java/Python SDKs send:

- `sdk-id-token`
- `sdk-access-token`
- `kiponos-id`
- `sdk-version`

The React SDK therefore puts the same fields on the **query string**:

```
wss://host/api/io-kiponos-sdk?sdk-id-token=…&sdk-access-token=…&kiponos-id=…&sdk-version=…
```

Until the server accepts query params, browser connects fail at handshake.

## Patch (`SdkHandshake.beforeHandshake`)

File (server): `io/kiponos/auth/websocket/sdk/SdkHandshake.java`

```java
// Prefer headers (Java/Python); fall back to query (browser React SDK).
String sdkIdToken = firstNonBlank(
    request.getHeaders().getFirst("sdk-id-token"),
    queryParam(request, "sdk-id-token"));
String sdkAccessToken = firstNonBlank(
    request.getHeaders().getFirst("sdk-access-token"),
    queryParam(request, "sdk-access-token"));
String kiponosId = firstNonBlank(
    request.getHeaders().getFirst("kiponos-id"),
    queryParam(request, "kiponos-id"));
```

Helpers:

```java
private static String queryParam(ServerHttpRequest request, String name) {
    if (!(request instanceof ServletServerHttpRequest)) return null;
    return ((ServletServerHttpRequest) request).getServletRequest().getParameter(name);
}

private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) return a;
    if (b != null && !b.isBlank()) return b;
    return null;
}
```

## Security note

Query tokens can appear in reverse-proxy access logs. Prefer short-lived
Connect tokens and log redaction. Future: one-time WS ticket via HTTPS POST.

## Deploy

This is a **server** change — deploy with normal kiponos-server release process.
The React package works end-to-end only after this lands on the target environment.
