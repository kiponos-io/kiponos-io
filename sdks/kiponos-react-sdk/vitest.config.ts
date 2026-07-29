import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "node",
    include: ["tests/**/*.test.ts"],
    // Unit tests are fast; e2e files set their own timeouts
    testTimeout: 15000,
    fileParallelism: false,
    sequence: { concurrent: false },
  },
});
