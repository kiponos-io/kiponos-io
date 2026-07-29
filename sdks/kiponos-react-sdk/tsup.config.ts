import { defineConfig } from "tsup";

export default defineConfig({
  entry: {
    index: "src/index.ts",
    server: "src/server.ts",
  },
  format: ["esm", "cjs"],
  dts: true,
  sourcemap: true,
  clean: true,
  external: [
    "react",
    "react/jsx-runtime",
    "ws",
    "node:fs",
    "node:os",
    "node:path",
    "node:module",
    "fs",
    "os",
    "path",
  ],
  treeshake: true,
  target: "es2020",
});
