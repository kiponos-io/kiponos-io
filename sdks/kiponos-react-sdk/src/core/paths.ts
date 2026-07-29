/**
 * Profile and JsonPath helpers — aligned with Python agent_client + Java SDK.
 */

/** `['App']['rel']['env']['cfg']` → `$.rootAccount['apps']…` base JsonPath. */
export function profileToBasePath(profile: string): string {
  const parts = [...(profile || "").matchAll(/\['([^']*)'\]/g)].map((m) => m[1]);
  if (parts.length < 4) {
    throw new Error(
      "Invalid Kiponos profile. Expected ['AppName']['Release']['Env']['ConfigName'], got: " +
        JSON.stringify(profile)
    );
  }
  const [app, rel, env, cfg] = parts;
  return (
    `$.rootAccount['apps']['${app}']['rels']['${rel}']` +
    `['envs']['${env}']['cfgs']['${cfg}']`
  );
}

/** Append folder segments to a JsonPath base. */
export function joinPath(base: string, ...folders: string[]): string {
  let path = base;
  for (const f of folders) {
    if (!f) continue;
    const safe = f.replace(/'/g, "\\'");
    path = `${path}['${safe}']`;
  }
  return path;
}

/** `'a/b/c'` or `'a.b.c'` → `['a','b','c']`. */
export function parseDottedOrSlash(path: string): string[] {
  const p = (path || "").trim().replace(/^\/+|\/+$/g, "");
  if (!p) return [];
  if (p.includes("/")) return p.split("/").filter(Boolean);
  if (p.includes(".")) return p.split(".").filter(Boolean);
  return [p];
}

/** Extract `['x']` segments from a JsonPath suffix. */
export function foldersFromJsonPath(rel: string): string[] {
  if (!rel) return [];
  return [...rel.matchAll(/\['([^']*)'\]/g)].map((m) => m[1]);
}

/** Relative folders under basePath from an absolute basePath field. */
export function foldersFromBase(
  basePath: string,
  eventBase: string | undefined
): string[] {
  if (!eventBase) return [];
  let rel = eventBase;
  if (rel.startsWith(basePath)) {
    rel = rel.slice(basePath.length);
  }
  return foldersFromJsonPath(rel);
}
