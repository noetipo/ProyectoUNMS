import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { ProgramaDoctoradoService } from '../../services/programa-doctorado.service';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { ProgramaDoctorado, PaginatedResponse } from '../../models/student.models';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { ProgramaDoctoradoNewComponent } from './programa-doctorado-new.component';
import { ProgramaDoctoradoEditComponent } from './programa-doctorado-edit.component';

const ROW_STAGGER_MS = 55;
const DIALOG_BASE    = { enterAnimationDuration: '0ms', exitAnimationDuration: '220ms' };
const DIALOG_PANEL   = ['dialog-rounded', 'dialog-anim'];

@Component({
  selector: 'app-programa-doctorado',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
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
            <span class="breadcrumb__current">Programas de Doctorado</span>
          </div>
          <h1 class="page-title">Programas de Doctorado</h1>
        </div>
        <button type="button" class="btn-dark" (click)="onNew()">
          <mat-icon svgIcon="plus" class="size-3.5" />
          Nuevo programa
        </button>
      </div>

      <!-- ── Error ── -->
      @if (errorMsg()) {
        <div class="mx-5 mb-3 flex items-center gap-2 rounded-lg bg-rose-50 border border-rose-200 px-4 py-3 text-sm text-rose-700 anim-fade-in">
          <mat-icon svgIcon="info" class="size-4 shrink-0 text-rose-500" />
          {{ errorMsg() }}
        </div>
      }

      <!-- ── Toolbar ── -->
      <div class="page-toolbar">
        <form [formGroup]="filterForm">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="search" placeholder="Buscar programa..." />
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
              <div class="skeleton-row" [style.opacity]="1 - i * 0.15">
                <div class="anim-shimmer h-2 w-4 rounded shrink-0"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="160 + (i % 3) * 40"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="100 + (i % 2) * 30"></div>
                <div class="anim-shimmer h-3.5 w-12 rounded-full shrink-0"></div>
                <div class="anim-shimmer h-2 w-16 rounded shrink-0"></div>
                <div class="anim-shimmer h-3.5 w-10 rounded shrink-0"></div>
              </div>
            }
          </div>
        } @else {
          <table class="data-table">
            <thead>
              <tr>
                <th class="w-8">#</th>
                <th class="w-28">Código</th>
                <th>Nombre</th>
                <th>Descripción</th>
                <th class="w-20">Estado</th>
                <th class="w-28">Creado</th>
                <th class="w-20 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (p of visibleProgramas(); track p.id; let i = $index) {
                <tr [class.anim-flash]="p.id === highlightedId()">
                  <td class="text-slate-400 tabular-nums">{{ i + 1 }}</td>
                  <td class="font-mono text-[11px] text-slate-400">{{ p.codigoSistema }}</td>
                  <td class="font-medium text-slate-700 uppercase tracking-wide text-[12px]">
                    {{ p.nombre }}
                  </td>
                  <td class="text-slate-500 max-w-[220px] truncate">{{ p.descripcion ?? '—' }}</td>
                  <td>
                    <span class="status-badge"
                      [class.status-badge--active]="p.active"
                      [class.status-badge--inactive]="!p.active">
                      <span class="status-dot"
                        [class.status-dot--active]="p.active"
                        [class.status-dot--inactive]="!p.active"></span>
                      {{ p.active ? 'Activo' : 'Inactivo' }}
                    </span>
                  </td>
                  <td class="text-slate-400 text-[11px] tabular-nums">
                    {{ p.createdAt | date:'dd/MM/yyyy' }}
                  </td>
                  <td>
                    <div class="row-actions">
                      <button mat-icon-button matTooltip="Editar" class="!w-7 !h-7"
                        (click)="onEdit(p)">
                        <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                      </button>
                      <button mat-icon-button matTooltip="Eliminar" class="!w-7 !h-7"
                        (click)="onDelete(p)">
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
                      <mat-icon svgIcon="book-open" class="size-10 text-slate-200" />
                      <p class="table-empty__text">Sin programas registrados</p>
                      <p class="table-empty__subtext">Crea el primero con el botón "Nuevo programa"</p>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
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
export class ProgramaDoctoradoContainerComponent implements OnInit, OnDestroy {
  private _svc     = inject(ProgramaDoctoradoService);
  private _confirm = inject(ConfirmDialogService);
  private _dialog  = inject(MatDialog);
  private _fb      = inject(UntypedFormBuilder);

  protected paginatedResponse  = signal<PaginatedResponse<ProgramaDoctorado>>({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 10 });
  protected visibleProgramas   = signal<ProgramaDoctorado[]>([]);
  protected currentPage       = signal(0);
  protected pageSize          = signal(10);
  protected loading           = signal(false);
  protected errorMsg          = signal<string | null>(null);
  protected highlightedId     = signal<string | null>(null);

  protected readonly skeletonRows = Array(5).fill(null);
  private _staggerTimers: ReturnType<typeof setTimeout>[] = [];

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ search: [''] });
    this.filterForm.valueChanges.subscribe(() => {
      this.currentPage.set(0);
      this.load();
    });
    this.load();
  }

  ngOnDestroy(): void {
    this._clearTimers();
  }

  load(): void {
    this.loading.set(true);
    this.errorMsg.set(null);
    const params = { page: this.currentPage(), size: this.pageSize(), ...this.filterForm.value };
    this._svc.getWithQuery$(params).subscribe({
      next: res => {
        this.paginatedResponse.set(res);      // totalElements, totalPages, etc.
        this.loading.set(false);
        this._stagger(res.content ?? []);     // res.content es el array de ProgramaDoctorado[]
      },
      error: e => { this.loading.set(false); this.errorMsg.set(e.message); },
    });
  }

  onPageChange(event: PaginationEvent): void {
    this.currentPage.set(event.page);
    this.pageSize.set(event.size);
    this.load();
  }

  onNew(): void {
    const ref = this._dialog.open(ProgramaDoctoradoNewComponent, {
      ...DIALOG_BASE,
      width: '480px',
      panelClass: DIALOG_PANEL,
    });
    ref.afterClosed().subscribe((dto: Partial<ProgramaDoctorado> | undefined) => {
      if (!dto) return;
      this._svc.create(dto as any).subscribe({
        next: programa => {
          this.load();
          this._flashRow(programa.id);
        },
        error: e => this.errorMsg.set(e.message),
      });
    });
  }

  onEdit(p: ProgramaDoctorado): void {
    const ref = this._dialog.open(ProgramaDoctoradoEditComponent, {
      ...DIALOG_BASE,
      width: '480px',
      panelClass: DIALOG_PANEL,
      data: p,
    });
    ref.afterClosed().subscribe((dto: Record<string, unknown> | undefined) => {
      if (!dto) return;
      this._svc.update(p.id, dto as any).subscribe({
        next: programa => {
          this.load();
          this._flashRow(programa.id);
        },
        error: e => this.errorMsg.set(e.message),
      });
    });
  }

  onDelete(p: ProgramaDoctorado): void {
    this._confirm.confirmDelete()
      .then(() => {
        this._svc.delete(p.id).subscribe({
          next: () => this.load(),
          error: e => this.errorMsg.set(e.message),
        });
      })
      .catch(() => {});
  }

  private _stagger(items: ProgramaDoctorado[]): void {
    this._clearTimers();
    this.visibleProgramas.set([]);
    items.forEach((item, i) => {
      const t = setTimeout(() => {
        this.visibleProgramas.update(prev => [...prev, item]);
      }, i * ROW_STAGGER_MS);
      this._staggerTimers.push(t);
    });
  }

  private _flashRow(id: string): void {
    this.highlightedId.set(id);
    setTimeout(() => this.highlightedId.set(null), 1400);
  }

  private _clearTimers(): void {
    this._staggerTimers.forEach(t => clearTimeout(t));
    this._staggerTimers = [];
  }
}
