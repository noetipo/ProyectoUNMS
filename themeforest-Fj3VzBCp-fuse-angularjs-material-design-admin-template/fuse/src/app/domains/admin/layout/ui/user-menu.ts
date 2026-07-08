import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIcon } from '@angular/material/icon';
import { MatDivider } from '@angular/material/list';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { Router } from '@angular/router';
import { environment } from '@/environments/environment';
import { OauthService } from '@/app/providers/services/oauth/oauth.service';
import { RoleSelectionService } from '@/app/providers/services/setup/role-selection.service';

interface RoleItem {
  id: string;
  name: string;
  code: string;
  description: string;
  selected: boolean;
}

@Component({
  selector: 'user-menu',
  imports: [MatIcon, MatDivider, MatMenu, MatMenuItem, MatMenuTrigger],
  template: `
    <!-- Avatar trigger -->
    <button
      class="flex size-8 shrink-0 cursor-pointer items-center justify-center rounded-full bg-primary-600 text-xs font-bold text-white hover:bg-primary-700 transition-colors"
      [matMenuTriggerFor]="topUserMenu"
      (menuOpened)="onMenuOpen()"
    >
      {{ initials() }}
    </button>

    <mat-menu #topUserMenu="matMenu" xPosition="before" yPosition="below" class="min-w-64">
      <!-- User header -->
      <div class="px-4 py-3 pointer-events-none">
        <div class="flex items-center gap-3">
          <div class="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-600 text-sm font-bold text-white">
            {{ initials() }}
          </div>
          <div class="min-w-0">
            <p class="truncate font-medium text-sm text-slate-800">{{ fullName() }}</p>
            <p class="truncate text-xs text-slate-500">{{ email() }}</p>
          </div>
        </div>
      </div>

      <mat-divider />

      <!-- Roles section — cada chip es clickeable -->
      <div class="px-4 py-2">
        <p class="text-[10px] font-semibold uppercase tracking-wider text-slate-400 mb-2 pointer-events-none">
          Roles asignados
        </p>

        @if (loadingRoles()) {
          <div class="flex items-center gap-2 py-1 pointer-events-none">
            <span class="inline-block h-2 w-2 rounded-full bg-slate-300 animate-pulse"></span>
            <span class="text-xs text-slate-400">Cargando roles…</span>
          </div>
        } @else if (roles().length === 0) {
          <span class="text-xs text-slate-400 italic pointer-events-none">Sin roles asignados</span>
        } @else {
          <div class="flex flex-wrap gap-1.5 pb-1">
            @for (role of roles(); track role.id) {
              <button
                type="button"
                class="inline-flex items-center rounded-full px-2.5 py-0.5 text-[11px] font-medium ring-1 ring-inset transition-colors cursor-pointer"
                [class]="activeRoleId() === role.id
                  ? 'bg-primary-600 text-white ring-primary-600'
                  : 'bg-primary-50 text-primary-700 ring-primary-200 hover:bg-primary-100'"
                (click)="selectRole(role); $event.stopPropagation()"
              >
                {{ role.name }}
              </button>
            }
          </div>
          @if (activeRoleId()) {
            <button
              type="button"
              class="mt-1 text-[10px] text-slate-400 hover:text-slate-600 underline"
              (click)="clearRole(); $event.stopPropagation()"
            >
              Ver menú completo
            </button>
          }
        }
      </div>

      <mat-divider />

      <!-- Actions -->
      <button mat-menu-item (click)="signOut()">
        <mat-icon svgIcon="log-out" />
        Cerrar sesión
      </button>
    </mat-menu>
  `,
})
export class UserMenu {
  private http = inject(HttpClient);
  private oauthService = inject(OauthService);
  private roleSelection = inject(RoleSelectionService);
  private router = inject(Router);
  private readonly baseUrl = environment.url.replace(/\/$/, '');

  protected loadingRoles = signal(false);
  protected roles = signal<RoleItem[]>([]);
  private rolesLoaded = false;

  protected activeRoleId = computed(() => this.roleSelection.current?.id ?? null);

  protected fullName = computed(() => {
    const user = this.oauthService.currentUser();
    if (!user) return 'Usuario';
    return `${user.firstName} ${user.lastName}`.trim();
  });

  protected email = computed(() => this.oauthService.currentUser()?.email ?? '');

  protected initials = computed(() => {
    const user = this.oauthService.currentUser();
    if (!user) return '?';
    const first = user.firstName?.[0] ?? '';
    const last = user.lastName?.[0] ?? '';
    const combined = (first + last).toUpperCase();
    return combined || (user.username?.[0]?.toUpperCase() ?? '?');
  });

  onMenuOpen(): void {
    if (this.rolesLoaded) return;
    const userId = this.oauthService.currentUser()?.id;
    if (!userId) return;

    this.loadingRoles.set(true);
    this.http
      .get<RoleItem[]>(`${this.baseUrl}/api/company-user-roles/user/${userId}`)
      .subscribe({
        next: (data) => {
          this.roles.set(data.filter((r) => r.selected));
          this.rolesLoaded = true;
          this.loadingRoles.set(false);
        },
        error: () => {
          this.loadingRoles.set(false);
        },
      });
  }

  selectRole(role: RoleItem): void {
    this.roleSelection.select({ id: role.id, name: role.name });
  }

  clearRole(): void {
    this.roleSelection.clear();
  }

  signOut(): void {
    this.roleSelection.clear();
    this.oauthService.signOut();
    this.router.navigateByUrl('/sign-in');
  }
}