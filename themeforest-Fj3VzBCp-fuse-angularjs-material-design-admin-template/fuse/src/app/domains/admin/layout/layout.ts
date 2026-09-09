import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavContent,
} from '@angular/material/sidenav';
import { RouterOutlet } from '@angular/router';
import { environment } from '@/environments/environment';
import { Media } from '@/app/core/media';
import { OauthService } from '@/app/providers/services/oauth/oauth.service';
import { LanguageSwitcher } from '@/app/domains/admin/layout/ui/language-switcher';
import { Notifications } from '@/app/domains/admin/layout/ui/notifications';
import { SchemeSwitcher } from '@/app/domains/admin/layout/ui/scheme-switcher';
import { AdminSidebar } from '@/app/domains/admin/layout/ui/sidebar';
import { UserMenu } from '@/app/domains/admin/layout/ui/user-menu';

/**
 * Roles en el orden en que identifican la ventana durante una demostración: se muestra el
 * primero que tenga el usuario, porque un docente suele acumular varios (DOCENTE + ASESOR…)
 * y el que interesa en el flujo es el más específico.
 */
const ROLES_VENTANA: { codigo: string; rol: string; color: string }[] = [
  { codigo: 'ESTUDIANTE', rol: 'Alumno', color: '#2563EB' },
  { codigo: 'SECRETARIA', rol: 'Secretaría', color: '#8C1D2E' },
  { codigo: 'COORDINADOR', rol: 'Coordinación', color: '#7C3AED' },
  { codigo: 'COORD_PROG', rol: 'Coordinación', color: '#7C3AED' },
  { codigo: 'COORD_SEC', rol: 'Coordinación', color: '#7C3AED' },
  { codigo: 'ASESOR', rol: 'Asesor', color: '#059669' },
  { codigo: 'REVISOR', rol: 'Revisor', color: '#D97706' },
  { codigo: 'PROF_TUTOR', rol: 'Tutor', color: '#0891B2' },
  { codigo: 'JURADO', rol: 'Jurado', color: '#475569' },
  { codigo: 'DOCENTE', rol: 'Docente', color: '#475569' },
  { codigo: 'ADMIN', rol: 'Administrador', color: '#334155' },
];

@Component({
  selector: 'admin-layout',
  imports: [
    MatIconModule,
    MatButtonModule,
    RouterOutlet,
    MatSidenavContainer,
    MatSidenav,
    MatSidenavContent,
    AdminSidebar,
    SchemeSwitcher,
    Notifications,
    LanguageSwitcher,
    UserMenu,
  ],
  template: `
    <mat-sidenav-container>
      <mat-sidenav
        class="w-70 border-r border-neutral-200 scheme-dark dark:border-neutral-800 dark:bg-neutral-900"
        [mode]="isMobile() ? 'over' : 'side'"
        [opened]="!isMobile()"
        [disableClose]="!isMobile()"
        fixedInViewport
        #sidenav="matSidenav"
      >
        <admin-sidebar />
      </mat-sidenav>

      <mat-sidenav-content>
        <!-- Toolbar -->
        <div class="flex items-center border-b px-4 py-2.5">
          <button
            matIconButton
            (click)="sidenav.toggle()"
          >
            <mat-icon svgIcon="panel-left" />
          </button>

          <!-- Spacer · durante las demostraciones lleva el rol de esta ventana (solo en desarrollo) -->
          <div class="flex-auto flex items-center justify-center">
            @if (ventana(); as v) {
              <span
                class="inline-flex h-10 items-center rounded-full px-7 text-[15px] font-extrabold tracking-[0.14em] text-white uppercase"
                [style.background]="v.color"
              >
                Ventana: {{ v.rol }}
              </span>
            }
          </div>

          <div class="flex items-center gap-x-2">
            <language-switcher />
            <scheme-switcher />
            <notifications />
            <user-menu />
          </div>
        </div>

        <!-- Content -->
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
})
export class AdminLayout {
  // Dependencies
  private media = inject(Media);
  private oauth = inject(OauthService);

  // State
  protected isMobile = computed(() =>
    this.media.match(`(max-width: 1023px)`)()
  );

  /**
   * Rótulo de la franja superior. Devuelve null en producción, de modo que la franja
   * exista únicamente mientras se enseña el sistema.
   */
  protected ventana = computed(() => {
    if (environment.production) return null;

    const user = this.oauth.currentUser();
    if (!user) return null;

    const codigos = user.roleCodes ?? [];
    return ROLES_VENTANA.find((r) => codigos.includes(r.codigo)) ?? null;
  });
}
