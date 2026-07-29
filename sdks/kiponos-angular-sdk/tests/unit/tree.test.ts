import { describe, expect, it } from "vitest";
import {
  applyValueAt,
  getValueAt,
  isKeyNode,
  listFoldersAt,
  listKeysAt,
  resolveNode,
} from "../../src/core/tree";
import type { ConfigTree } from "../../src/core/types";

describe("config tree", () => {
  it("distinguishes keys and folders", () => {
    expect(isKeyNode({ value: "x" })).toBe(true);
    expect(isKeyNode({ a: { value: "1" } })).toBe(false);
  });

  it("get/set style local ops", () => {
    const tree: ConfigTree = {};
    applyValueAt(tree, ["ui"], "theme", "dark");
    expect(getValueAt(tree, ["ui"], "theme")).toBe("dark");
    expect(listKeysAt(tree, ["ui"])).toEqual(["theme"]);
    expect(listFoldersAt(tree, [])).toEqual(["ui"]);
    expect(resolveNode(tree, ["ui"])).toEqual({ theme: { value: "dark" } });
  });
});
