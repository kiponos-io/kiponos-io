import {
  DestroyRef,
  Injectable,
  Inject,
  Optional,
  inject,
  signal,
  computed,
  type Signal,
  type WritableSignal,
} from "@angular/core";
import { Observable, Subject } from "rxjs";
import { Kiponos } from "../core/kiponos";
import { KiponosClient } from "../core/kiponos-client";
import type { FromEnvOptions } from "../core/env";
import type {
  ConfigFolderCreatedEvent,
  ConfigFolderDeletedEvent,
  ConfigItemSavedEvent,
  ConfigKeyCreatedEvent,
  ConfigKeyDeletedEvent,
  ConfigKeyRenamedEvent,
  ConfigTree,
  ConfigValUpdatedEvent,
  KiponosFolder,
  KiponosStatus,
  OnChangeHandler,
  Unsubscribe,
} from "../core/types";
import {
  KIPONOS_CLIENT,
  KIPONOS_CONFIG,
  type ProvideKiponosConfig,
} from "./tokens";

/**
 * Angular DI surface over KiponosClient — Java SDK API parity:
 * get / set / path / folderOrCreate / after* listeners.
 *
 * Prefer signals for template bindings; Observables for RxJS pipelines.
 */
@Injectable()
export class KiponosService {
  private readonly destroyRef = inject(DestroyRef);
  private readonly ownedClient: KiponosClient | null;
  private readonly clientRef: KiponosClient | null;

  /** Connection status (signal). */
  readonly status: WritableSignal<KiponosStatus> = signal<KiponosStatus>("idle");
  /** Last error (signal). */
  readonly error: WritableSignal<Error | null> = signal<Error | null>(null);
  /** True when status === 'ready'. */
  readonly ready: Signal<boolean> = computed(() => this.status() === "ready");
  /** Tree mutation epoch — bump on any local/remote tree change. */
  readonly treeEpoch: WritableSignal<number> = signal(0);

  private readonly status$ = new Subject<KiponosStatus>();
  private readonly change$ = new Subject<{
    key: string;
    value: string | undefined;
    folders: readonly string[];
    source: string;
    delta: Record<string, unknown>;
  }>();

  constructor(
    @Optional() @Inject(KIPONOS_CLIENT) externalClient: KiponosClient | null,
    @Optional() @Inject(KIPONOS_CONFIG) config: ProvideKiponosConfig | null
  ) {
    const cfg: ProvideKiponosConfig = config ?? {};
    let client: KiponosClient | null = externalClient ?? cfg.client ?? null;
    let owned: KiponosClient | null = null;

    if (!client && cfg.fromEnv) {
      try {
        const opts: FromEnvOptions =
          typeof cfg.fromEnv === "object" ? cfg.fromEnv : {};
        owned = Kiponos.createFromEnv({ ...opts, autoConnect: false });
        client = owned;
      } catch (e) {
        this.error.set(e instanceof Error ? e : new Error(String(e)));
      }
    }

    if (!client && !cfg.fromEnv && !externalClient) {
      this.error.set(
        new Error(
          "[@kiponos/angular] KiponosService needs provideKiponos({ client }) " +
            "or provideKiponos({ fromEnv: true }) on Node."
        )
      );
    }

    this.ownedClient = owned;
    this.clientRef = client;

    if (client) {
      this.status.set(client.status);
      this.error.set(client.error);

      const unsubStatus = client.onStatus((s) => {
        this.status.set(s);
        this.error.set(client!.error);
        this.status$.next(s);
      });
      const unsubTree = client.onTreeChanged(() => {
        this.treeEpoch.update((n) => n + 1);
      });
      const unsubChange = client.onChange((key, value, folders, source, delta) => {
        this.change$.next({ key, value, folders, source, delta });
      });

      const autoConnect = cfg.autoConnect !== false;
      if (
        autoConnect &&
        client.status !== "ready" &&
        client.status !== "connecting"
      ) {
        void client.connect().catch((e: Error) => {
          this.error.set(e);
          this.status.set("error");
        });
      }

      this.destroyRef.onDestroy(() => {
        unsubStatus();
        unsubTree();
        unsubChange();
        if (this.ownedClient) {
          try {
            this.ownedClient.disconnect();
          } catch {
            /* ignore */
          }
        }
        this.status$.complete();
        this.change$.complete();
      });
    }
  }

  /** Underlying client (null if not configured). */
  get client(): KiponosClient | null {
    return this.clientRef;
  }

  get teamId(): string {
    return this.clientRef?.teamId ?? "";
  }

  get profile(): string {
    return this.clientRef?.profile ?? "";
  }

  get basePath(): string {
    return this.clientRef?.basePath ?? "";
  }

  /** Status as Observable (for async pipe / RxJS). */
  statusChanges(): Observable<KiponosStatus> {
    return this.status$.asObservable();
  }

  /** Catch-all change stream as Observable. */
  changes(): Observable<{
    key: string;
    value: string | undefined;
    folders: readonly string[];
    source: string;
    delta: Record<string, unknown>;
  }> {
    return this.change$.asObservable();
  }

  async connect(): Promise<void> {
    if (!this.clientRef) throw new Error("Kiponos client not available");
    return this.clientRef.connect();
  }

  disconnect(): void {
    this.clientRef?.disconnect();
  }

  waitUntilReady(timeoutMs?: number): Promise<void> {
    if (!this.clientRef) {
      return Promise.reject(new Error("Kiponos client not available"));
    }
    return this.clientRef.waitUntilReady(timeoutMs);
  }

  get(
    key: string,
    defaultValue?: string,
    ...folders: string[]
  ): string | undefined {
    if (!this.clientRef) return defaultValue;
    return this.clientRef.get(key, defaultValue, ...folders);
  }

  getPath(path: string, defaultValue?: string): string | undefined {
    if (!this.clientRef) return defaultValue;
    return this.clientRef.getPath(path, defaultValue);
  }

  set(
    key: string,
    value: string | number | boolean,
    ...folders: string[]
  ): Promise<string> {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.set(key, value, ...folders);
  }

  setPath(path: string, value: string | number | boolean): Promise<string> {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.setPath(path, value);
  }

  path(...folders: string[]): KiponosFolder {
    if (!this.clientRef) throw new Error("Kiponos client not available");
    return this.clientRef.path(...folders);
  }

  ensurePath(...folders: string[]): Promise<void> {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.ensurePath(...folders);
  }

  mkdir(folderName: string, ...parentFolders: string[]): Promise<string> {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.mkdir(folderName, ...parentFolders);
  }

  deleteKey(key: string, ...folders: string[]): Promise<string> {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.deleteKey(key, ...folders);
  }

  deleteFolder(
    folderName: string,
    ...parentFolders: string[]
  ): Promise<string | null> {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.deleteFolder(folderName, ...parentFolders);
  }

  listKeys(...folders: string[]): string[] {
    return this.clientRef?.listKeys(...folders) ?? [];
  }

  listFolders(...folders: string[]): string[] {
    return this.clientRef?.listFolders(...folders) ?? [];
  }

  dump(...folders: string[]): ConfigTree {
    return this.clientRef?.dump(...folders) ?? {};
  }

  afterValueUpdated(
    handler: (e: ConfigValUpdatedEvent) => void
  ): Unsubscribe {
    return this.clientRef?.afterValueUpdated(handler) ?? (() => undefined);
  }

  afterKeyCreated(handler: (e: ConfigKeyCreatedEvent) => void): Unsubscribe {
    return this.clientRef?.afterKeyCreated(handler) ?? (() => undefined);
  }

  afterKeyDeleted(handler: (e: ConfigKeyDeletedEvent) => void): Unsubscribe {
    return this.clientRef?.afterKeyDeleted(handler) ?? (() => undefined);
  }

  afterKeyRenamed(handler: (e: ConfigKeyRenamedEvent) => void): Unsubscribe {
    return this.clientRef?.afterKeyRenamed(handler) ?? (() => undefined);
  }

  afterItemSaved(handler: (e: ConfigItemSavedEvent) => void): Unsubscribe {
    return this.clientRef?.afterItemSaved(handler) ?? (() => undefined);
  }

  afterFolderCreated(
    handler: (e: ConfigFolderCreatedEvent) => void
  ): Unsubscribe {
    return this.clientRef?.afterFolderCreated(handler) ?? (() => undefined);
  }

  afterFolderDeleted(
    handler: (e: ConfigFolderDeletedEvent) => void
  ): Unsubscribe {
    return this.clientRef?.afterFolderDeleted(handler) ?? (() => undefined);
  }

  onChange(handler: OnChangeHandler): Unsubscribe {
    return this.clientRef?.onChange(handler) ?? (() => undefined);
  }

  /**
   * Live leaf as a Signal — re-computes when treeEpoch bumps.
   * Java get style; use in templates: `theme()`.
   *
   * @example
   * theme = this.kip.value('ui/theme', { defaultValue: 'dark' });
   */
  value(
    keyOrPath: string,
    options: { defaultValue?: string; folders?: string[] } = {}
  ): Signal<string | undefined> {
    const { defaultValue, folders } = options;
    return computed(() => {
      // Depend on tree + ready so we re-read after bootstrap and deltas
      this.treeEpoch();
      this.status();
      if (!this.clientRef) return defaultValue;
      if (folders) {
        return this.clientRef.get(keyOrPath, defaultValue, ...folders);
      }
      if (keyOrPath.includes("/") || keyOrPath.includes(".")) {
        return this.clientRef.getPath(keyOrPath, defaultValue);
      }
      return this.clientRef.get(keyOrPath, defaultValue);
    });
  }

  /**
   * Live integer Signal (Java getInt).
   */
  valueInt(
    keyOrPath: string,
    defaultValue = 0,
    folders?: string[]
  ): Signal<number> {
    const raw = this.value(keyOrPath, {
      defaultValue: String(defaultValue),
      folders,
    });
    return computed(() => {
      const v = raw();
      if (v === undefined || v === "") return defaultValue;
      const n = parseInt(v, 10);
      return Number.isFinite(n) ? n : defaultValue;
    });
  }
}
