/**
 * Minimal usage sketch (not a full Angular CLI app).
 * Copy patterns into your standalone Angular project.
 */
import { Component } from "@angular/core";
import {
  provideKiponos,
  injectKiponos,
  Kiponos,
  type ProvideKiponosConfig,
} from "@kiponos/angular";

// --- Node / SSR bootstrap ---
// const client = Kiponos.createFromEnv();
// await client.connect();
// provideKiponos({ client, autoConnect: false })

// --- Component ---
@Component({
  standalone: true,
  selector: "app-theme",
  template: `
    <span data-theme="{{ theme() }}">theme={{ theme() }}</span>
    <button [disabled]="!kip.ready()" (click)="toggle()">Toggle</button>
    <p>status={{ kip.status() }}</p>
  `,
})
export class ThemeComponent {
  readonly kip = injectKiponos();
  readonly theme = this.kip.value("ui/theme", { defaultValue: "dark" });

  constructor() {
    this.kip.afterValueUpdated((e) => {
      console.log("value updated", e.key, e.value);
    });
  }

  async toggle() {
    const cur = this.theme() ?? "dark";
    await this.kip.ensurePath("ui");
    await this.kip.path("ui").set("theme", cur === "dark" ? "light" : "dark");
  }
}

// Document config shape for app.config.ts
export const exampleProvideConfig: ProvideKiponosConfig = {
  // client: prebuilt,
  fromEnv: true,
  autoConnect: true,
};

void Kiponos; // silence unused when only reading this file
void provideKiponos;
