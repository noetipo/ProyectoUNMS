import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { CentroLaboralService } from '../../services/centro-laboral.service';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { CentroLaboral, PaginatedResponse } from '../../models/student.models';
import { CentroLaboralNewComponent } from './centro-laboral-new.component';
import { CentroLaboralEditComponent } from './centro-laboral-edit.component';

const ROW_STAGGER_MS = 55;
const DIALOG_BASE  = { enterAnimationDuration: '0ms', exitAnimationDuration: '220ms' };
const DIALOG_PANEL = ['dialog-rounded', 'dialog-anim'];

@Component({
  selector: 'app-centro-laboral-container',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatButtonModule, MatIconModule,
    PaginationControlsComponent,
  ],
  template: `
    <div class="page">

      <!-- ── Header ── -->
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Posgrado</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Centros Laborales</span>
          </div>
          <h1 class="page-title">Centros Laborales</h1>
        </div>
        <button type="button" class="btn-dark" (click)="onNew()">
          <mat-icon svgIcon="plus" class="size-3.5" />
          Nuevo centro
        </button>
      </div>

      <!-- ── Toolbar ── -->
      <div class="page-toolbar">
        <form [formGroup]="filterForm">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="search" placeholder="Buscar centro laboral..." />
          </div>
        </form>
        @if (!loading()) {
          <span class="text-[11px] text-slate-400 anim-fade-in">
            {{ paginatedResponse().totalElements }} registro(s)
          </span>
        }
      </div>

      <!-- ── Content ── -->
      <div class="page-content">
        @if (loading()) {
          <div class="divide-y divide-slate-50">
            @for (_ of skeletonRows; track $index; let i = $index) {
              <div class="skeleton-row" [style.opacity]="1 - i * 0.1">
                <div class="anim-shimmer h-2 w-4 rounded shrink-0"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="80 + (i % 3) * 20"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="200 + (i % 4) * 30"></div>
                <div class="anim-shimmer h-2 rounded flex-1"></div>
                <div class="anim-shimmer h-3.5 w-12 rounded-full shrink-0"></div>
                <div class="anim-shimmer h-3.5 w-10 rounded shrink-0"></div>
              </div>
            }
          </div>
        } @else {

          <!-- Desktop table -->
          <div class="hidden md:block overflow-x-auto">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="w-8">#</th>
                  <th class="w-28">Código</th>
                  <th>Nombre</th>
                  <th>Descripción</th>
                  <th class="w-20">Estado</th>
                  <th class="w-20 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (item of visibleItems(); track item.id; let i = $index) {
                  <tr [class.anim-flash]="item.id === highlightedId()">
                    <td class="text-slate-400 tabular-nums">{{ (currentPage() * pageSize()) + i + 1 }}</td>
                    <td class="font-mono text-[11px] text-slate-400">{{ item.codigoSistema }}</td>
                    <td class="font-medium text-slate-700">{{ item.nombre }}</td>
                    <td class="text-slate-500 text-sm max-w-[280px] truncate">{{ item.descripcion ?? '—' }}</td>
                    <td>
                      <span class="status-badge"
                        [class.status-badge--active]="item.active"
                        [class.status-badge--inactive]="!item.active">
                        <span class="status-dot"
                          [class.status-dot--active]="item.active"
                          [class.status-dot--inactive]="!item.active"></span>
                        {{ item.active ? 'Activo' : 'Inactivo' }}
                      </span>
                    </td>
                    <td>
                      <div class="row-actions">
                        <button mat-icon-button class="!w-7 !h-7" (click)="onEdit(item)">
                          <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                        </button>
                        <button mat-icon-button class="!w-7 !h-7" (click)="onDelete(item)">
                          <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                }
                @empty {
                  <tr>
                    <td colspan="6" class="text-center">
                      <div class="table-empty">
                        <mat-icon svgIcon="building-office" class="size-10 text-slate-200" />
                        <p class="table-empty__text">Sin centros laborales registrados</p>
                        <p class="table-empty__subtext">Crea el primero con el botón "Nuevo centro"</p>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Mobile cards -->
          <div class="md:hidden divide-y divide-slate-100">
            @for (item of visibleItems(); track item.id) {
              <div class="px-5 py-3 anim-row-in" [class.anim-flash]="item.id === highlightedId()">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex-1 min-w-0">
                    <p class="font-mono text-[11px] text-slate-400">{{ item.codigoSistema }}</p>
                    <p class="font-medium text-slate-800 text-sm truncate">{{ item.nombre }}</p>
                    @if (item.descripcion) {
                      <p class="text-xs text-slate-500 truncate mt-0.5">{{ item.descripcion }}</p>
                    }
                  </div>
                  <div class="flex items-center gap-0.5 shrink-0">
                    <button mat-icon-button class="!w-7 !h-7" (click)="onEdit(item)">
                      <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                    </button>
                    <button mat-icon-button class="!w-7 !h-7" (click)="onDelete(item)">
                      <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            }
            @empty {
              <div class="flex flex-col items-center gap-2 py-16 anim-fade-in">
                <mat-icon svgIcon="building-office" class="size-10 text-slate-200" />
                <p class="text-sm text-slate-400">Sin centros laborales registrados</p>
              </div>
            }
          </div>

        }
      </div>

      <!-- ── Pagination ── -->
      @if (!loading()) {
        <div class="page-footer">
          <pagination-controls
            [totalItems]="paginatedResponse().totalElements"
            [itemsPerPage]="pageSize()"
            [currentPage]="currentPage()"
            (paginationChange)="onPageChange($event)"
          />
        </div>
      }

    </div>
  `,
})
export class CentroLaboralContainerComponent implements OnInit, OnDestroy {
  private _svc     = inject(CentroLaboralService);
  private _confirm = inject(ConfirmDialogService);
  private _dialog  = inject(MatDialog);
  private _fb      = inject(UntypedFormBuilder);

  protected paginatedResponse = signal<PaginatedResponse<CentroLaboral>>({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 10 });
  protected visibleItems      = signal<CentroLaboral[]>([]);
  protected currentPage       = signal(0);
  protected pageSize          = signal(10);
  protected loading           = signal(false);
  protected highlightedId     = signal<string | null>(null);

  protected readonly skeletonRows = Array(8).fill(null);
  private _revealTimers: ReturnType<typeof setTimeout>[] = [];

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ search: [''] });
    this.filterForm.valueChanges.subscribe(() => { this.currentPage.set(0); this.load(); });
    this.load();
  }

  ngOnDestroy(): void {
    this._clearTimers();
  }

  load(): void {
    this.loading.set(true);
    const params = { page: this.currentPage(), size: this.pageSize(), ...this.filterForm.value };
    this._svc.getWithQuery$(params).subscribe({
      next: res => {
        this.paginatedResponse.set(res);
        this.loading.set(false);
        this._revealRows(res.content ?? []);
      },
      error: () => this.loading.set(false),
    });
  }

  onPageChange(event: PaginationEvent): void {
    this.currentPage.set(event.page);
    this.pageSize.set(event.size);
    this.load();
  }

  onNew(): void {
    const ref = this._dialog.open(CentroLaboralNewComponent, { ...DIALOG_BASE, width: '480px', panelClass: DIALOG_PANEL });
    ref.afterClosed().subscribe(dto => {
      if (!dto) return;
      this._svc.create(dto).subscribe(item => { this.load(); this._flashRow(item.id); });
    });
  }

  onEdit(item: CentroLaboral): void {
    const ref = this._dialog.open(CentroLaboralEditComponent, { ...DIALOG_BASE, width: '480px', panelClass: DIALOG_PANEL, data: item });
    ref.afterClosed().subscribe(dto => {
      if (!dto) return;
      this._svc.update(item.id, dto).subscribe(() => { this.load(); this._flashRow(item.id); });
    });
  }

  onDelete(item: CentroLaboral): void {
    this._confirm.confirmDelete()
      .then(() => this._svc.delete(item.id).subscribe(() => this.load()))
      .catch(() => {});
  }

  private _revealRows(items: CentroLaboral[]): void {
    this._clearTimers();
    this.visibleItems.set([]);
    items.forEach((item, i) => {
      const t = setTimeout(() => this.visibleItems.update(prev => [...prev, item]), i * ROW_STAGGER_MS);
      this._revealTimers.push(t);
    });
  }

  private _flashRow(id: string): void {
    this.highlightedId.set(id);
    setTimeout(() => this.highlightedId.set(null), 1400);
  }

  private _clearTimers(): void {
    this._revealTimers.forEach(t => clearTimeout(t));
    this._revealTimers = [];
  }
}