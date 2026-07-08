import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { Role } from '../models/role';

@Component({
  selector: 'app-role-list',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule, MatButtonModule],
  template: `
    <!-- ── Desktop table ── -->
    <div class="hidden md:block overflow-x-auto">
      <table class="data-table">
        <thead>
          <tr>
            <th class="w-8">#</th>
            <th>Nombre</th>
            <th>Descripción</th>
            <th class="w-20">Estado</th>
            <th class="w-28 text-right">Acciones</th>
          </tr>
        </thead>
        <tbody>
          @for (item of roles; track item.id; let i = $index) {
            <tr [class.anim-flash]="item.id === highlightedId">
              <td class="text-slate-400 tabular-nums">{{ i + 1 }}</td>
              <td class="font-medium text-slate-700">{{ item.name }}</td>
              <td class="text-slate-500 max-w-[260px] truncate">{{ item.description ?? '—' }}</td>
              <td>
                <span class="status-badge"
                  [class.status-badge--active]="item.status"
                  [class.status-badge--inactive]="!item.status">
                  <span class="status-dot"
                    [class.status-dot--active]="item.status"
                    [class.status-dot--inactive]="!item.status"></span>
                  {{ item.status ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td>
                <div class="row-actions">
                  <button mat-icon-button matTooltip="Editar" class="!w-7 !h-7" (click)="eventEdit.emit(item.id!)">
                    <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                  </button>
                  <button mat-icon-button matTooltip="Eliminar" class="!w-7 !h-7" (click)="eventDelete.emit(item.id!)">
                    <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
                  </button>
                  <button mat-icon-button matTooltip="Asignar módulos" class="!w-7 !h-7" (click)="eventAsignet.emit(item.id!)">
                    <mat-icon svgIcon="layout-grid" class="text-emerald-500 size-3.5" />
                  </button>
                </div>
              </td>
            </tr>
          }
          @empty {
            <tr>
              <td colspan="5" class="text-center">
                <div class="table-empty">
                  <mat-icon svgIcon="shield" class="size-10 text-slate-200" />
                  <p class="table-empty__text">No hay roles registrados</p>
                  <p class="table-empty__subtext">Crea el primer rol con el botón "Nuevo rol"</p>
                </div>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>

    <!-- ── Mobile cards ── -->
    <div class="md:hidden divide-y divide-slate-100">
      @for (item of roles; track item.id) {
        <div class="px-5 py-3 anim-row-in" [class.anim-flash]="item.id === highlightedId">
          <div class="flex items-start justify-between gap-3">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium"
                  [class]="item.status ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                  <span class="w-1.5 h-1.5 rounded-full" [class]="item.status ? 'bg-emerald-500' : 'bg-slate-400'"></span>
                  {{ item.status ? 'Activo' : 'Inactivo' }}
                </span>
              </div>
              <p class="font-medium text-slate-800 truncate text-sm">{{ item.name }}</p>
              @if (item.description) {
                <p class="text-xs text-slate-500 mt-1 truncate">{{ item.description }}</p>
              }
            </div>
            <div class="flex items-center gap-0.5 shrink-0">
              <button mat-icon-button class="!w-7 !h-7" (click)="eventEdit.emit(item.id!)">
                <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
              </button>
              <button mat-icon-button class="!w-7 !h-7" (click)="eventDelete.emit(item.id!)">
                <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
              </button>
              <button mat-icon-button class="!w-7 !h-7" (click)="eventAsignet.emit(item.id!)">
                <mat-icon svgIcon="layout-grid" class="text-emerald-500 size-3.5" />
              </button>
            </div>
          </div>
        </div>
      }
      @empty {
        <div class="flex flex-col items-center gap-2 py-16 anim-fade-in">
          <mat-icon svgIcon="shield" class="size-10 text-slate-200" />
          <p class="text-sm text-slate-400">No hay roles registrados</p>
        </div>
      }
    </div>
  `,
})
export class RoleListComponent {
  @Input() roles: Role[] = [];
  @Input() highlightedId: string | null = null;
  @Output() eventEdit    = new EventEmitter<string>();
  @Output() eventDelete  = new EventEmitter<string>();
  @Output() eventAsignet = new EventEmitter<string>();
}
