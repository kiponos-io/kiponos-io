/**
 * Minimal usage sketch (not a runnable app — copy into your CRA/Vite project).
 */
import React from "react";
import {
  KiponosProvider,
  useKiponos,
  useKiponosValue,
  useAfterValueUpdated,
} from "@kiponos/react";

function ThemeBadge() {
  // Live leaf — no Context prop-drilling for "theme"
  const theme = useKiponosValue("ui/theme", { defaultValue: "dark" });
  return <span data-theme={theme}>theme={theme}</span>;
}

function ThemeToggle() {
  const kip = useKiponos();
  return (
    <button
      disabled={!kip.ready}
      onClick={() => {
        const cur = kip.getPath("ui/theme", "dark");
        void kip.path("ui").set("theme", cur === "dark" ? "light" : "dark");
      }}
    >
      Toggle theme
    </button>
  );
}

function LiveLog() {
  useAfterValueUpdated((e) => {
    console.log("value updated", e.key, e.value);
  });
  return null;
}

export function App() {
  return (
    <KiponosProvider
      profile="['MyApp']['1.0.0']['Dev']['base']"
      idToken={process.env.REACT_APP_KIPONOS_ID!}
      accessToken={process.env.REACT_APP_KIPONOS_ACCESS!}
      // serverUrl="wss://kiponos.io/api/io-kiponos-sdk"
    >
      <LiveLog />
      <ThemeBadge />
      <ThemeToggle />
    </KiponosProvider>
  );
}
