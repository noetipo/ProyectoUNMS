import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { ParentModule } from '../models/parent-module';

@Component({
  selector: 'app-parent-module-list',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule, MatButtonModule],
  template: `
    <!-- ── Desktop table ── -->
    <div class="hidden md:block overflow-x-auto">
      <table class="data-table">
        <thead>
          <tr>
            <th class="w-8">#</th>
            <th>Título</th>
            <th>Ícono</th>
            <th>Ruta</th>
            <th class="w-14">Orden</th>
            <th class="w-20">Estado</th>
            <th class="w-20 text-right">Acciones</th>
          </tr>
        </thead>
        <tbody>
          @for (item of parentModules; track item.id; let i = $index) {
            <tr [class.anim-flash]="item.id === highlightedId">
              <td class="text-slate-400 tabular-nums">{{ i + 1 }}</td>
              <td class="font-medium text-slate-700">{{ item.title }}</td>
              <td>
                @if (item.icon) {
                  <code class="px-1.5 py-px bg-slate-100 text-slate-500 rounded font-mono text-[11px]">{{ item.icon }}</code>
                } @else { <span class="text-slate-300">—</span> }
              </td>
              <td class="max-w-[200px]">
                @if (item.link) {
                  <span class="text-slate-400 truncate block font-mono text-[11px]" [title]="item.link">{{ item.link }}</span>
                } @else { <span class="text-slate-300">—</span> }
              </td>
              <td class="text-center">{{ item.moduleOrder ?? '—' }}</td>
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
                </div>
              </td>
            </tr>
          }
          @empty {
            <tr>
              <td colspan="7" class="text-center">
                <div class="table-empty">
                  <mat-icon svgIcon="layout-grid" class="size-10 text-slate-200" />
                  <p class="table-empty__text">No hay módulos padres registrados</p>
                  <p class="table-empty__subtext">Crea el primero con el botón "Nuevo módulo padre"</p>
                </div>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>

    <!-- ── Mobile cards ── -->
    <div class="md:hidden divide-y divide-slate-100">
      @for (item of parentModules; track item.id) {
        <div class="px-5 py-3 anim-row-in" [class.anim-flash]="item.id === highlightedId">
          <div class="flex items-start justify-between gap-3">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium"
                  [class]="item.status ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                  <span class="w-1.5 h-1.5 rounded-full" [class]="item.status ? 'bg-emerald-500' : 'bg-slate-400'"></span>
                  {{ item.status ? 'Activo' : 'Inactivo' }}
                </span>
                @if (item.moduleOrder != null) {
                  <span class="text-xs text-slate-400">#{{ item.moduleOrder }}</span>
                }
              </div>
              <p class="font-medium text-slate-800 truncate text-sm">{{ item.title }}</p>
              @if (item.link) {
                <p class="text-xs font-mono text-slate-400 mt-1 truncate">{{ item.link }}</p>
              }
            </div>
            <div class="flex items-center gap-0.5 shrink-0">
              <button mat-icon-button class="!w-7 !h-7" (click)="eventEdit.emit(item.id!)">
                <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
              </button>
              <button mat-icon-button class="!w-7 !h-7" (click)="eventDelete.emit(item.id!)">
                <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
              </button>
            </div>
          </div>
        </div>
      }
      @empty {
        <div class="flex flex-col items-center gap-2 py-16 anim-fade-in">
          <mat-icon svgIcon="layout-grid" class="size-10 text-slate-200" />
          <p class="text-sm text-slate-400">No hay módulos padres registrados</p>
        </div>
      }
    </div>
  `,
})
export class ParentModuleListComponent {
  @Input() parentModules: ParentModule[] = [];
  @Input() highlightedId: string | null = null;
  @Output() eventEdit   = new EventEmitter<string>();
  @Output() eventDelete = new EventEmitter<string>();
}
