import { CdkConnectedOverlay, CdkOverlayOrigin } from '@angular/cdk/overlay';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router } from '@angular/router';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

interface Notif {
  id: string;
  title?: string | null;
  description: string;
  link?: string;
  icon?: string;
  fecha?: string | null;
}

@Component({
  selector: 'notifications',
  imports: [MatIconButton, MatIcon, CdkConnectedOverlay, CdkOverlayOrigin],
  template: `
    <span class="relative inline-flex">
      <button
        matIconButton
        cdkOverlayOrigin
        (click)="toggle()"
        #trigger="cdkOverlayOrigin"
      >
        <mat-icon svgIcon="bell" />
      </button>
      @if (notifications().length > 0) {
        <span
          class="pointer-events-none absolute top-1 right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-600 px-1 text-[9px] leading-none font-bold text-white ring-2 ring-white dark:ring-neutral-900"
        >
          {{ notifications().length }}
        </span>
      }
    </span>

    <ng-template
      cdkConnectedOverlay
      [cdkConnectedOverlayOrigin]="trigger"
      [cdkConnectedOverlayOpen]="open()"
      [cdkConnectedOverlayHasBackdrop]="true"
      [cdkConnectedOverlayBackdropClass]="'transparent'.split(' ')"
      (detach)="toggle(false)"
      (backdropClick)="toggle(false)"
    >
      <!-- Panel compacto: 300 px de ancho y filas de dos líneas. Antes era una ventana con
           cabecera de 20 px de título y filas de 3 líneas, desproporcionada para avisos cortos. -->
      <div
        class="z-10 mt-1 flex max-h-[26rem] w-[19rem] flex-col overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-lg dark:border-neutral-700 dark:bg-neutral-800"
      >
        <!-- Cabecera -->
        <div class="flex items-center gap-2 border-b border-neutral-100 px-3 py-2 dark:border-neutral-700">
          <mat-icon class="size-3.5 text-[#8C1D2E]" svgIcon="bell" />
          <span class="text-[12px] font-bold tracking-tight">Notificaciones</span>
          @if (notifications().length > 0) {
            <span class="ml-auto rounded-full bg-[#8C1D2E] px-1.5 text-[9.5px] font-bold leading-4 text-white">
              {{ notifications().length }}
            </span>
          }
        </div>

        <!-- Lista: separadores sutiles en vez de mat-divider, y una fila por aviso -->
        <div class="flex flex-col divide-y divide-neutral-100 overflow-y-auto dark:divide-neutral-700">
          @for (n of notifications(); track n.id) {
            <button
              type="button"
              class="group flex items-start gap-2 px-3 py-2 text-left transition hover:bg-[#FDF6F7] dark:hover:bg-neutral-700"
              (click)="abrir(n)"
            >
              <span class="mt-0.5 grid size-6 shrink-0 place-items-center rounded-lg bg-[#FDF6F7] dark:bg-neutral-700">
                <mat-icon [svgIcon]="n.icon || 'bell'" class="size-3.5 text-[#8C1D2E]" />
              </span>
              <span class="min-w-0 flex-auto">
                @if (n.title) {
                  <span class="block truncate text-[11.5px] font-semibold text-neutral-800 dark:text-neutral-100">{{ n.title }}</span>
                }
                <span class="line-clamp-2 block text-[10.5px] leading-snug text-neutral-500">{{ n.description }}</span>
              </span>
              @if (n.link) {
                <mat-icon svgIcon="chevron-right" class="mt-1 size-3.5 shrink-0 text-neutral-300 transition group-hover:text-[#8C1D2E]" />
              }
            </button>
          }
          @if (!notifications().length) {
            <div class="flex flex-col items-center gap-1 py-7 text-center">
              <mat-icon svgIcon="bell" class="size-6 text-neutral-200" />
              <p class="text-[11.5px] font-semibold text-neutral-500">Todo al día</p>
              <p class="text-[10.5px] text-neutral-400">No tienes avisos pendientes.</p>
            </div>
          }
        </div>
      </div>
    </ng-template>
  `,
})
export class Notifications implements OnInit {
  private _http = inject(HttpClient);
  private _router = inject(Router);
  private readonly navegador = isPlatformBrowser(inject(PLATFORM_ID));

  private static readonly VISTAS = 'notificaciones:vistas';

  protected open = signal(false);
  protected notifications = signal<Notif[]>([]);

  /**
   * Notificaciones ya abiertas, para no volver a mostrarlas y que la campanita no se aglomere.
   * No hay tabla de notificaciones en el backend (se calculan en caliente según el estado del
   * expediente), así que "leída" se guarda aquí, en el navegador. La huella incluye la
   * descripción, no solo el id: si la situación cambia (p. ej. una observación nueva del mismo
   * revisor), dice algo distinto y vuelve a aparecer en vez de quedar oculta para siempre.
   */
  private vistas = new Set<string>();

  ngOnInit(): void {
    if (this.navegador) {
      try {
        const guardado = localStorage.getItem(Notifications.VISTAS);
        if (guardado) this.vistas = new Set(JSON.parse(guardado));
      } catch { /* localStorage corrupto o inaccesible: se ignora */ }
    }
    this.cargar();
  }

  private huella(n: Notif): string {
    return n.id + '|' + n.description;
  }

  cargar(): void {
    this._http.get<any>(environment.url + END_POINTS.notificaciones.base).subscribe({
      next: (res) => {
        const todas: Notif[] = res?.data ?? res ?? [];
        this.notifications.set(todas.filter((n) => !this.vistas.has(this.huella(n))));
      },
      error: () => this.notifications.set([]),
    });
  }

  toggle(force: boolean | null = null) {
    const next = force === null ? !this.open() : force;
    this.open.set(next);
    if (next) {
      this.cargar(); // refresca al abrir
    }
  }

  abrir(n: Notif) {
    this.vistas.add(this.huella(n));
    this.notifications.update((list) => list.filter((x) => x !== n));
    if (this.navegador) {
      try { localStorage.setItem(Notifications.VISTAS, JSON.stringify([...this.vistas])); } catch { /* noop */ }
    }
    if (n.link) {
      this._router.navigateByUrl(n.link);
    }
    this.toggle(false);
  }
}
