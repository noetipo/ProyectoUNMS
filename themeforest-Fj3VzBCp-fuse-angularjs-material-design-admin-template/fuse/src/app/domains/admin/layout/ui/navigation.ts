import { Tree, TreeItem, TreeItemGroup } from '@angular/aria/tree';
import { CdkMonitorFocus } from '@angular/cdk/a11y';
import { NgTemplateOutlet } from '@angular/common';
import { afterNextRender, Component, effect, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import {
  isActive,
  IsActiveMatchOptions,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
} from '@angular/router';
import { filter, take } from 'rxjs';
import {
  NavigationItem,
} from '@/app/domains/admin/layout/data/navigation';
import { MenuService } from '@/app/providers/services/setup/menu.service';
import { OauthService } from '@/app/providers/services/oauth/oauth.service';
import { RoleSelectionService } from '@/app/providers/services/setup/role-selection.service';

@Component({
  selector: 'navigation',
  imports: [
    MatIcon,
    MatProgressSpinner,
    NgTemplateOutlet,
    RouterLinkActive,
    Tree,
    TreeItem,
    TreeItemGroup,
    RouterLink,
    CdkMonitorFocus,
  ],
  template: `
    @if (activeRoleName(); as role) {
      <div class="mx-4 mb-3 flex items-center gap-2 rounded-lg bg-primary-600/10 px-3 py-2 ring-1 ring-inset ring-primary-300/40">
        <mat-icon svgIcon="shield-check" class="size-3.5 shrink-0 text-primary-600" />
        <span class="flex-1 truncate text-xs font-semibold text-primary-700">{{ role.name }}</span>
      </div>
    }

    @if (loading()) {
      <div class="flex justify-center py-8">
        <mat-spinner diameter="28" />
      </div>
    } @else {
      <div class="flex flex-col gap-y-4">
        @for (section of navigation(); track section.id) {
          <div class="flex flex-col px-4">
            <!-- Section title -->
            <div class="px-2.5 py-1.5 text-sm font-semibold text-primary-400">
              {{ section.label }}

              @if (section.description) {
                <div class="text-xs font-medium text-neutral-400">
                  {{ section.description }}
                </div>
              }
            </div>

            <!-- Section content -->
            <ul
              ngTree
              class="mt-1 flex flex-col gap-y-1"
              [nav]="true"
              #tree="ngTree"
            >
              <ng-template
                [ngTemplateOutlet]="treeNodes"
                [ngTemplateOutletContext]="{
                  nodes: section.children,
                  parent: tree,
                }"
              />
            </ul>

            <!-- Menu item template (recursive) -->
            <ng-template
              let-nodes="nodes"
              let-parent="parent"
              #treeNodes
            >
              @for (node of nodes; track node.id) {
                <a
                  cdkMonitorElementFocus
                  ngTreeItem
                  routerLinkActive="bg-primary-500/20 text-primary-300 font-semibold"
                  class="navigation-item flex cursor-pointer items-center gap-x-2 rounded-lg px-2.5 py-2 select-none hover:bg-neutral-700/10 dark:hover:bg-neutral-300/10"
                  [parent]="parent"
                  [value]="node.id"
                  [label]="node.label"
                  [disabled]="node.disabled"
                  [selectable]="!node.children"
                  [(expanded)]="node.expanded"
                  [routerLink]="node.route"
                  [routerLinkActiveOptions]="
                    node.activeOptions ?? { exact: true }
                  "
                  (click)="$event.preventDefault()"
                  #rla="routerLinkActive"
                  #treeItem="ngTreeItem"
                >
                  <!-- Icon -->
                  @if (node.icon) {
                    <mat-icon
                      class="pointer-events-none size-4"
                      [svgIcon]="node.icon"
                    />
                  }

                  <!-- Label -->
                  <div class="flex flex-auto flex-col font-medium">
                    {{ node.label }}

                    @if (node.description) {
                      <div class="text-xs">{{ node.description }}</div>
                    }
                  </div>

                  <!-- Badge -->
                  @if (node.badge) {
                    <div
                      class="rounded bg-pink-400 px-1.5 py-0.5 text-xs font-semibold dark:bg-pink-700"
                    >
                      {{ node.badge }}
                    </div>
                  }

                  <!-- Expand icon -->
                  @if (node.children && node.children.length > 0) {
                    <mat-icon
                      svgIcon="chevron-right"
                      class="pointer-events-none size-4 transition-[rotate]"
                      [class.rotate-90]="node.expanded"
                    />
                  }
                </a>

                <!-- Children -->
                @if (node.children && node.children.length > 0) {
                  <ul
                    class="flex flex-col gap-y-1 [&_ul>.navigation-item]:pl-14.5 [&>.navigation-item]:pl-8.5"
                    [class.hidden]="!node.expanded"
                    [class.mt-1]="node.expanded"
                    role="group"
                  >
                    <ng-template
                      ngTreeItemGroup
                      [ownedBy]="treeItem"
                      #group="ngTreeItemGroup"
                    >
                      <ng-template
                        [ngTemplateOutlet]="treeNodes"
                        [ngTemplateOutletContext]="{
                          nodes: node.children,
                          parent: group,
                        }"
                      />
                    </ng-template>
                  </ul>
                }
              }
            </ng-template>
          </div>
        }

        @if (!loading() && navigation().length === 0) {
          <p class="px-6 py-4 text-sm text-neutral-400">Sin módulos asignados</p>
        }
      </div>
    }
  `,
})
export class Navigation implements OnInit {
  private router = inject(Router);
  private menuService = inject(MenuService);
  private oauthService = inject(OauthService);
  private roleSelection = inject(RoleSelectionService);

  protected navigation = signal<NavigationItem[]>([]);
  protected loading = signal(true);
  protected activeRoleName = toSignal(
    this.roleSelection.role$,
    { initialValue: null }
  );

  protected navigationEnd = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      take(1)
    )
  );

  constructor() {
    effect(() => {
      const navigationEnd = this.navigationEnd();
      if (!navigationEnd) return;
      this.navigation.set(this.expandActiveRoute(this.navigation()));
    });

    // Carga inicial del menú GARANTIZADA en el navegador. afterNextRender solo
    // corre en el cliente y tras el render, así el menú no queda vacío por el
    // timing de SSR/hydration (el effect por sí solo no era fiable).
    afterNextRender(() => {
      this.loadMenu(this.roleSelection.current?.id ?? null);
    });

    // Recargar menú cuando CAMBIA el rol seleccionado (salta la emisión inicial).
    let primeraEmision = true;
    effect(() => {
      const role = this.activeRoleName();
      if (primeraEmision) {
        primeraEmision = false;
        return;
      }
      this.loadMenu(role?.id ?? null);
    });
  }

  ngOnInit(): void {
    // La carga inicial la dispara afterNextRender (solo navegador).
  }

  loadMenu(roleId: string | null = null): void {
    this.oauthService.check().subscribe((authenticated) => {
      if (!authenticated) {
        queueMicrotask(() => this.loading.set(false));
        return;
      }

      this.loading.set(true);
      const menu$ = roleId
        ? this.menuService.getMenuByRole$(roleId)
        : this.menuService.getMenu$();

      menu$.subscribe({
        next: (items) => {
          // queueMicrotask evita NG0100 cuando el HTTP transfer-state devuelve
          // la respuesta sincrónicamente durante el primer ciclo de CD en SSR/hydration
          queueMicrotask(() => {
            const mapped = this.menuService.mapToNavigation(items);
            this.navigation.set(mapped);
            this.loading.set(false);
            this.navigation.set(this.expandActiveRoute(this.navigation()));
          });
        },
        error: () => {
          queueMicrotask(() => this.loading.set(false));
        },
      });
    });
  }

  expandActiveRoute(items: NavigationItem[]): NavigationItem[] {
    for (const item of items) {
      if (item.children?.length) {
        item.children = this.expandActiveRoute(item.children);
        if (item.children.some((child) => child.expanded)) {
          item.expanded = true;
        }
      }

      if (
        item.route &&
        isActive(
          item.route,
          this.router,
          this.isActiveOption(item.activeOptions ?? { exact: true })
        )()
      ) {
        item.expanded = true;
      }
    }
    return items;
  }

  isActiveOption(
    options: { exact: boolean } | IsActiveMatchOptions
  ): IsActiveMatchOptions {
    if ('exact' in options) {
      return options.exact
        ? { paths: 'exact', queryParams: 'exact', fragment: 'ignored', matrixParams: 'ignored' }
        : { paths: 'subset', queryParams: 'subset', fragment: 'ignored', matrixParams: 'ignored' };
    }
    return options;
  }
}
