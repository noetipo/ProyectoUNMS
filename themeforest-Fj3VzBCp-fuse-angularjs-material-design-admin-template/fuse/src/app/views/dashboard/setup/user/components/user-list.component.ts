import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { User } from '../models/user';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatSlideToggleModule, MatTooltipModule, MatButtonModule],
  template: `
    <!-- ── Desktop table ── -->
    <div class="hidden md:block overflow-x-auto">
      <table class="data-table">
        <thead>
          <tr>
            <th class="w-8">#</th>
            <th>Usuario</th>
            <th>Nombre / persona</th>
            <th class="w-28">DNI</th>
            <th>Email</th>
            <th>Roles</th>
            <th class="w-16 text-center">Estado</th>
            <th class="w-20 text-right">Acciones</th>
          </tr>
        </thead>
        <tbody>
          @for (item of users; track item.id; let i = $index) {
            <tr [class.anim-flash]="item.id === highlightedId">

              <td class="text-slate-400 tabular-nums">{{ i + 1 }}</td>

              <td class="font-medium text-slate-700">{{ item.username }}</td>

              <td class="text-slate-600">{{ item.personaNombre || (item.firstName + ' ' + item.lastName) || '—' }}</td>

              <td class="text-slate-500 font-mono text-[11px]">{{ item.numeroDocumento || '—' }}</td>

              <td class="text-slate-500 font-mono text-[11px]">{{ item.email }}</td>

              <td>
                @if (item.roles?.length) {
                  <div class="flex flex-wrap gap-1">
                    @for (r of item.roles; track r) {
                      <span class="text-[10px] font-medium px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">{{ r }}</span>
                    }
                  </div>
                } @else {
                  <span class="text-[11px] text-slate-300">Sin roles</span>
                }
              </td>

              <td class="text-center">
                <mat-slide-toggle
                  [checked]="item.active ?? !!item.enabled"
                  (change)="eventChangeState.emit(item.id!)"
                  color="primary"
                />
              </td>

              <td>
                <div class="row-actions">
                  <button mat-icon-button matTooltip="Asignar roles" class="!w-7 !h-7"
                    (click)="eventAssign.emit(item.userId ?? item.id)">
                    <mat-icon svgIcon="shield" class="text-emerald-500 size-3.5" />
                  </button>
                </div>
              </td>

            </tr>
          }
          @empty {
            <tr>
              <td colspan="8" class="text-center">
                <div class="table-empty">
                  <mat-icon svgIcon="user" class="size-10 text-slate-200" />
                  <p class="table-empty__text">No hay usuarios registrados</p>
                  <p class="table-empty__subtext">Crea el primero con el botón "Nuevo usuario"</p>
                </div>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>

    <!-- ── Mobile cards ── -->
    <div class="md:hidden divide-y divide-slate-100">
      @for (item of users; track item.id) {
        <div class="px-5 py-3 anim-row-in" [class.anim-flash]="item.id === highlightedId">
          <div class="flex items-start justify-between gap-3">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium"
                  [class]="(item.active ?? item.enabled) ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                  <span class="w-1.5 h-1.5 rounded-full"
                    [class]="(item.active ?? item.enabled) ? 'bg-emerald-500' : 'bg-slate-400'"></span>
                  {{ (item.active ?? item.enabled) ? 'Activo' : 'Inactivo' }}
                </span>
              </div>
              <p class="font-medium text-slate-800 truncate text-sm">{{ item.username }}</p>
              <p class="text-xs text-slate-500 mt-0.5">{{ item.personaNombre || (item.firstName + ' ' + item.lastName) }}</p>
              <p class="text-xs text-slate-400 mt-0.5">DNI: {{ item.numeroDocumento || '—' }}</p>
              <p class="text-xs font-mono text-slate-400 mt-0.5 truncate">{{ item.email }}</p>
              @if (item.roles?.length) {
                <div class="flex flex-wrap gap-1 mt-1">
                  @for (r of item.roles; track r) {
                    <span class="text-[10px] font-medium px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">{{ r }}</span>
                  }
                </div>
              }
            </div>
            <div class="flex items-center gap-1 shrink-0 pt-1">
              <mat-slide-toggle
                [checked]="item.active ?? !!item.enabled"
                (change)="eventChangeState.emit(item.id!)"
                color="primary"
              />
              <button mat-icon-button class="!w-7 !h-7" (click)="eventAssign.emit(item.userId ?? item.id)">
                <mat-icon svgIcon="shield" class="text-emerald-500 size-3.5" />
              </button>
            </div>
          </div>
        </div>
      }
      @empty {
        <div class="flex flex-col items-center gap-2 py-16 anim-fade-in">
          <mat-icon svgIcon="user" class="size-10 text-slate-200" />
          <p class="text-sm text-slate-400">No hay usuarios registrados</p>
        </div>
      }
    </div>
  `,
})
export class UserListComponent {
  @Input() users: User[] = [];
  @Input() highlightedId: string | null = null;
  @Output() eventAssign      = new EventEmitter<string>();
  @Output() eventChangeState = new EventEmitter<string>();
}
