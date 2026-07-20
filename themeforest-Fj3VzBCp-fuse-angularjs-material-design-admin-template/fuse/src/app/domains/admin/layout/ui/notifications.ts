import { CdkConnectedOverlay, CdkOverlayOrigin } from '@angular/cdk/overlay';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatDivider } from '@angular/material/divider';
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
  imports: [MatIconButton, MatIcon, CdkConnectedOverlay, CdkOverlayOrigin, MatDivider],
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
      <div
        class="z-10 flex max-h-120 w-full max-w-xs flex-col overflow-y-auto rounded-lg bg-white shadow-(--mat-sys-level2) dark:bg-neutral-800"
      >
        <!-- Header -->
        <div class="flex items-center gap-x-3 bg-neutral-100 p-4 pl-6 dark:bg-neutral-800">
          <mat-icon class="size-4.5" svgIcon="bell" />
          <div class="text-xl font-semibold tracking-tighter">Notificaciones</div>
          @if (notifications().length > 0) {
            <span class="ml-auto rounded-full bg-rose-100 px-2 py-0.5 text-xs font-bold text-rose-700">
              {{ notifications().length }}
            </span>
          }
        </div>
        <mat-divider />

        <!-- List -->
        <div class="flex flex-col">
          @for (n of notifications(); track n.id; let last = $last) {
            <button
              type="button"
              class="flex gap-x-3 py-3 pr-4 pl-6 text-left hover:bg-neutral-50 dark:hover:bg-neutral-700"
              (click)="abrir(n)"
            >
              <mat-icon [svgIcon]="n.icon || 'bell'" class="mt-0.5 size-4.5 text-[#8C1D2E]" />
              <div class="flex-auto">
                @if (n.title) {
                  <div class="font-semibold">{{ n.title }}</div>
                }
                <div class="line-clamp-2 text-sm">{{ n.description }}</div>
                @if (n.fecha) {
                  <div class="mt-1 text-xs text-neutral-500">{{ n.fecha }}</div>
                }
              </div>
            </button>
            @if (!last) {
              <mat-divider
                class="[--mat-divider-color:var(--color-neutral-200)] dark:[--mat-divider-color:var(--color-neutral-700)]"
              />
            }
          }
          @if (!notifications().length) {
            <div class="py-8 text-center text-sm text-neutral-400">
              No tienes notificaciones pendientes.
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

  protected open = signal(false);
  protected notifications = signal<Notif[]>([]);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this._http.get<any>(environment.url + END_POINTS.notificaciones.base).subscribe({
      next: (res) => this.notifications.set(res?.data ?? res ?? []),
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
    if (n.link) {
      this._router.navigateByUrl(n.link);
    }
    this.toggle(false);
  }
}
