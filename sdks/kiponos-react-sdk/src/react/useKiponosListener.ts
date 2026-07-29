import { useEffect } from "react";
import type {
  ConfigFolderCreatedEvent,
  ConfigItemSavedEvent,
  ConfigKeyCreatedEvent,
  ConfigKeyDeletedEvent,
  ConfigKeyRenamedEvent,
  ConfigValUpdatedEvent,
  OnChangeHandler,
} from "../core/types";
import { useKiponosContext } from "./context";

/**
 * Register `afterValueUpdated` for the lifetime of the component.
 * Handler should be stable (useCallback) to avoid re-subscribe churn.
 */
export function useAfterValueUpdated(
  handler: (e: ConfigValUpdatedEvent) => void
): void {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterValueUpdated(handler);
  }, [client, handler]);
}

export function useAfterKeyCreated(
  handler: (e: ConfigKeyCreatedEvent) => void
): void {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterKeyCreated(handler);
  }, [client, handler]);
}

export function useAfterKeyDeleted(
  handler: (e: ConfigKeyDeletedEvent) => void
): void {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterKeyDeleted(handler);
  }, [client, handler]);
}

export function useAfterKeyRenamed(
  handler: (e: ConfigKeyRenamedEvent) => void
): void {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterKeyRenamed(handler);
  }, [client, handler]);
}

export function useAfterItemSaved(
  handler: (e: ConfigItemSavedEvent) => void
): void {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterItemSaved(handler);
  }, [client, handler]);
}

export function useAfterFolderCreated(
  handler: (e: ConfigFolderCreatedEvent) => void
): void {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterFolderCreated(handler);
  }, [client, handler]);
}

/** Catch-all change stream (Python `on_change` style). */
export function useKiponosOnChange(handler: OnChangeHandler): void {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.onChange(handler);
  }, [client, handler]);
}
