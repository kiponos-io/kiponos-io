import { inject } from "@angular/core";
import { KiponosService } from "./kiponos.service";

/**
 * Convenience inject() for components / services.
 *
 * @example
 * private readonly kip = injectKiponos();
 * theme = this.kip.value('ui/theme', { defaultValue: 'dark' });
 */
export function injectKiponos(): KiponosService {
  return inject(KiponosService);
}
