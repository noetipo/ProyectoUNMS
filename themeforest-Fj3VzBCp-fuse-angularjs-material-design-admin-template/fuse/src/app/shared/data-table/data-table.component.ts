import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  PaginationControlsComponent, PaginationEvent,
} from '@/app/shared/pagination-controls/pagination-controls.component';

export interface TableColumn {
  header: string;
  /** texto a mostrar en la celda */
  cell: (row: any) => string;
  /** si retorna una clase, la celda se muestra como badge con esa clase */
  badgeClass?: (row: any) => string | null;
  align?: 'left' | 'right' | 'center';
}

export interface TableAction {
  action: string;          // identificador emitido
  icon: string;            // svgIcon
  tooltip?: string;
  show?: (row: any) => boolean;
}

export interface TableActionEvent {
  action: string;
  row: any;
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule, PaginationControlsComponent],
  template: `
    <div class="rounded-xl border border-slate-100 dark:border-slate-700 overflow-hidden bg-white dark:bg-slate-800">
      <table class="w-full text-sm">
        <thead class="bg-slate-50 dark:bg-slate-900/40 text-slate-500 dark:text-slate-400">
          <tr>
            @for (c of columns; track c.header) {
              <th class="px-4 py-2.5 text-left font-medium text-xs uppercase tracking-wide"
                  [class.text-right]="c.align === 'right'">{{ c.header }}</th>
            }
            @if (actions.length) {
              <th class="px-4 py-2.5 text-right font-medium text-xs uppercase tracking-wide">Acciones</th>
            }
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-50 dark:divide-slate-700">
          @if (loading) {
            @for (_ of skeleton; track $index) {
              <tr>
                <td [attr.colspan]="colspan" class="px-4 py-3">
                  <div class="anim-shimmer h-3 rounded w-full"></div>
                </td>
              </tr>
            }
          } @else if (!rows.length) {
            <tr>
              <td [attr.colspan]="colspan" class="px-4 py-10 text-center text-slate-400">
                <mat-icon svgIcon="heroicons_outline:inbox" class="size-8 opacity-40 mb-1" />
                <div class="text-sm">No hay registros</div>
              </td>
            </tr>
          } @else {
            @for (row of rows; track $index) {
              <tr class="hover:bg-slate-50/60 dark:hover:bg-slate-700/30 transition-colors">
                @for (c of columns; track c.header) {
                  <td class="px-4 py-2.5 text-slate-700 dark:text-slate-200"
                      [class.text-right]="c.align === 'right'">
                    @if (c.badgeClass && c.badgeClass(row)) {
                      <span class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
                            [class]="c.badgeClass(row)">{{ c.cell(row) }}</span>
                    } @else {
                      {{ c.cell(row) }}
                    }
                  </td>
                }
                @if (actions.length) {
                  <td class="px-4 py-2.5 text-right whitespace-nowrap">
                    @for (a of actions; track a.action) {
                      @if (!a.show || a.show(row)) {
                        <button type="button"
                                class="inline-flex items-center justify-center w-8 h-8 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700"
                                [matTooltip]="a.tooltip || ''"
                                (click)="actionClick.emit({ action: a.action, row })">
                          <mat-icon [svgIcon]="a.icon" class="size-4" />
                        </button>
                      }
                    }
                  </td>
                }
              </tr>
            }
          }
        </tbody>
      </table>

      @if (!loading && rows.length) {
        <div class="px-4 py-2 border-t border-slate-50 dark:border-slate-700">
          <pagination-controls
            [totalItems]="total"
            [itemsPerPage]="size"
            [currentPage]="page"
            (paginationChange)="pageChange.emit($event)" />
        </div>
      }
    </div>
  `,
})
export class DataTableComponent {
  @Input() columns: TableColumn[] = [];
  @Input() rows: any[] = [];
  @Input() actions: TableAction[] = [];
  @Input() loading = false;
  @Input() total = 0;
  @Input() page = 0;
  @Input() size = 20;

  @Output() actionClick = new EventEmitter<TableActionEvent>();
  @Output() pageChange = new EventEmitter<PaginationEvent>();

  protected readonly skeleton = Array(6).fill(null);

  get colspan(): number {
    return this.columns.length + (this.actions.length ? 1 : 0);
  }
}