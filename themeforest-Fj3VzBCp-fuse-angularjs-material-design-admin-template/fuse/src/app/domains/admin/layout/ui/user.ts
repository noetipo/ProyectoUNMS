import { Component, computed, inject } from '@angular/core';
import { MatPseudoCheckbox } from '@angular/material/core';
import { MatIcon } from '@angular/material/icon';
import { MatDivider } from '@angular/material/list';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { Router } from '@angular/router';
import { Scheme, Theming } from '@/app/core/theming';
import { OauthService } from '@/app/providers/services/oauth/oauth.service';

@Component({
  selector: 'user',
  imports: [
    MatDivider,
    MatIcon,
    MatMenu,
    MatMenuItem,
    MatPseudoCheckbox,
    MatMenuTrigger,
  ],
  template: `
    <button
      class="flex w-full cursor-pointer items-center gap-x-3 rounded-xl p-2 text-left hover:bg-neutral-700/10 dark:hover:bg-neutral-300/10"
      [matMenuTriggerFor]="userMenu"
    >
      <!-- Avatar con iniciales -->
      <div class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary-600 text-sm font-bold text-white">
        {{ initials() }}
      </div>
      <div class="flex min-w-0 flex-auto flex-col select-none">
        <div class="truncate font-medium">{{ fullName() }}</div>
        <div class="text-on-surface-variant truncate text-sm">
          {{ email() }}
        </div>
      </div>
      <mat-icon class="size-4" svgIcon="ellipsis-vertical" />
    </button>

    <mat-menu class="min-w-60" xPosition="before" yPosition="above" #userMenu="matMenu">
      <!-- Header del menú -->
      <button class="py-2 [&>span]:flex [&>span]:items-center" mat-menu-item>
        <div class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary-600 text-sm font-bold text-white">
          {{ initials() }}
        </div>
        <div class="ml-3 flex min-w-0 flex-auto flex-col select-none">
          <div class="truncate font-medium">{{ fullName() }}</div>
          <div class="text-on-surface-variant truncate text-xs">{{ email() }}</div>
        </div>
      </button>

      <mat-divider />

      <button mat-menu-item>
        <mat-icon svgIcon="user-round" />
        Mi perfil
      </button>

      <mat-divider />

      <button
        mat-menu-item
        [matMenuTriggerFor]="appearanceMenu"
      >
        <mat-icon svgIcon="sun-moon" />
        Apariencia
      </button>

      <mat-divider />

      <button mat-menu-item (click)="signOut()">
        <mat-icon svgIcon="log-out" />
        Cerrar sesión
      </button>
    </mat-menu>

    <mat-menu #appearanceMenu="matMenu">
      @for (item of schemes; track item.value) {
        <button mat-menu-item (click)="updateScheme(item.value)">
          <mat-pseudo-checkbox
            appearance="minimal"
            class="mr-2"
            [state]="scheme() === item.value ? 'checked' : 'unchecked'"
          />
          <span>{{ item.label }}</span>
        </button>
      }
    </mat-menu>
  `,
})
export class User {
  private theming = inject(Theming);
  private oauthService = inject(OauthService);
  private router = inject(Router);

  protected scheme = computed(() => this.theming.scheme());
  protected schemes: { label: string; value: Scheme }[] = [
    { label: 'Claro', value: 'light' },
    { label: 'Oscuro', value: 'dark' },
    { label: 'Sistema', value: 'system' },
  ];

  protected fullName = computed(() => {
    const user = this.oauthService.currentUser();
    if (!user) return 'Usuario';
    return `${user.firstName} ${user.lastName}`.trim();
  });

  protected email = computed(() => {
    return this.oauthService.currentUser()?.email ?? '';
  });

  protected initials = computed(() => {
    const user = this.oauthService.currentUser();
    if (!user) return '?';
    const first = user.firstName?.[0] ?? '';
    const last = user.lastName?.[0] ?? '';
    const combined = (first + last).toUpperCase();
    return combined || (user.username?.[0]?.toUpperCase() ?? '?');
  });

  updateScheme(scheme: Scheme) {
    this.theming.scheme.set(scheme);
  }

  signOut(): void {
    this.oauthService.signOut();
    this.router.navigateByUrl('/sign-in');
  }
}
