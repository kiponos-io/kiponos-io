import { describe, expect, it } from "vitest";
import {
  foldersFromBase,
  joinPath,
  parseDottedOrSlash,
  profileToBasePath,
} from "../../src/core/paths";

describe("profileToBasePath", () => {
  it("builds JsonPath for 4-segment profile", () => {
    const base = profileToBasePath("['MyApp']['1.0']['Dev']['base']");
    expect(base).toBe(
      "$.rootAccount['apps']['MyApp']['rels']['1.0']['envs']['Dev']['cfgs']['base']"
    );
  });

  it("rejects short profile", () => {
    expect(() => profileToBasePath("['a']['b']")).toThrow(/Invalid Kiponos profile/);
  });
});

describe("joinPath", () => {
  it("appends folders", () => {
    const base = profileToBasePath("['A']['1']['e']['c']");
    expect(joinPath(base, "ui", "theme")).toBe(
      base + "['ui']['theme']"
    );
  });
});

describe("parseDottedOrSlash", () => {
  it("parses slash and dotted", () => {
    expect(parseDottedOrSlash("a/b/c")).toEqual(["a", "b", "c"]);
    expect(parseDottedOrSlash("a.b.c")).toEqual(["a", "b", "c"]);
    expect(parseDottedOrSlash("solo")).toEqual(["solo"]);
  });
});

describe("foldersFromBase", () => {
  it("strips profile base", () => {
    const base = profileToBasePath("['A']['1']['e']['c']");
    const eventBase = base + "['ui']['panel']";
    expect(foldersFromBase(base, eventBase)).toEqual(["ui", "panel"]);
  });
});
