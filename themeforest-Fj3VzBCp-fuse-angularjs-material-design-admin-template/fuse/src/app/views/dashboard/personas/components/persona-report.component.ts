import { CommonModule } from '@angular/common';
import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { debounceTime } from 'rxjs';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { PersonaGradosDialogComponent } from '@/app/shared/persona-grados/persona-grados-dialog.component';
import { PersonaFiltros, PersonaService } from '../services/persona.service';

const ROW_STAGGER_MS = 55;

@Component({
  selector: 'app-persona-report',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatTooltipModule, PaginationControlsComponent,
  ],
  template: `
    <div class="page">

      <!-- ── Header ── -->
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Académico</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Personas</span>
          </div>
          <h1 class="page-title">Personas</h1>
        </div>
        <button type="button" class="btn-dark" (click)="nuevo()">
          <mat-icon svgIcon="plus" class="size-3.5" />
          Nueva persona
        </button>
      </div>

      <!-- ── Toolbar ── -->
      <div class="page-toolbar">
        <form [formGroup]="filterForm" class="flex flex-wrap items-center gap-2">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="search" placeholder="Buscar persona..." />
          </div>
          <select formControlName="tipoPerfil" class="filter-select">
            <option value="">Todos los perfiles</option>
            <option value="ESTUDIANTE">Estudiante</option>
            <option value="DOCENTE">Docente</option>
            <option value="AMBOS">Ambos</option>
          </select>
          <select formControlName="activo" class="filter-select">
            <option value="true">Activos</option>
            <option value="false">Inactivos</option>
            <option value="">Todos</option>
          </select>
        </form>
        @if (!loading()) {
          <span class="text-[11px] text-slate-400 anim-fade-in">{{ total() }} registro(s)</span>
        }
      </div>

      <!-- ── Content ── -->
      <div class="page-content">
        @if (loading()) {
          <div class="divide-y divide-slate-50">
            @for (_ of skeletonRows; track $index; let i = $index) {
              <div class="skeleton-row" [style.opacity]="1 - i * 0.1">
                <div class="anim-shimmer h-2 w-4 rounded shrink-0"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="90 + (i % 3) * 30"></div>
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
                  <th class="w-32">Documento</th>
                  <th>Nombre</th>
                  <th>Email</th>
                  <th>Perfiles</th>
                  <th class="w-20">Estado</th>
                  <th class="w-20 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (item of visibleItems(); track item.id; let i = $index) {
                  <tr [class.anim-flash]="item.id === highlightedId()">
                    <td class="text-slate-400 tabular-nums">{{ (page() * size()) + i + 1 }}</td>
                    <td class="font-mono text-[11px] text-slate-400">{{ item.numeroDocumento ?? '—' }}</td>
                    <td class="font-medium text-slate-700">{{ item.nombres }} {{ item.apellidos }}</td>
                    <td class="text-slate-500 text-sm">{{ item.emailPersonal ?? '—' }}</td>
                    <td class="text-slate-500 text-sm">{{ (item.perfiles ?? []).join(', ') || '—' }}</td>
                    <td>
                      <span class="status-badge"
                        [class.status-badge--active]="item.activo"
                        [class.status-badge--inactive]="!item.activo">
                        <span class="status-dot"
                          [class.status-dot--active]="item.activo"
                          [class.status-dot--inactive]="!item.activo"></span>
                        {{ item.activo ? 'Activo' : 'Inactivo' }}
                      </span>
                    </td>
                    <td>
                      <div class="row-actions">
                        <button mat-icon-button matTooltip="Ver grados académicos" class="!w-7 !h-7" (click)="verGrados(item)">
                          <mat-icon svgIcon="graduation-cap" class="text-sky-500 size-3.5" />
                        </button>
                        <button mat-icon-button matTooltip="Editar" class="!w-7 !h-7" (click)="editar(item)">
                          <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                        </button>
                        @if (item.activo) {
                          <button mat-icon-button matTooltip="Eliminar" class="!w-7 !h-7" (click)="eliminar(item)">
                            <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
                @empty {
                  <tr>
                    <td colspan="7" class="text-center">
                      <div class="table-empty">
                        <mat-icon svgIcon="heroicons_outline:identification" class="size-10 text-slate-200" />
                        <p class="table-empty__text">No hay personas registradas</p>
                        <p class="table-empty__subtext">Registra la primera con el botón "Nueva persona"</p>
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
                    <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium"
                      [class]="item.activo ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                      <span class="w-1.5 h-1.5 rounded-full" [class]="item.activo ? 'bg-emerald-500' : 'bg-slate-400'"></span>
                      {{ item.activo ? 'Activo' : 'Inactivo' }}
                    </span>
                    <p class="font-medium text-slate-800 truncate text-sm mt-1">{{ item.nombres }} {{ item.apellidos }}</p>
                    <p class="text-xs text-slate-500 truncate mt-0.5">{{ item.numeroDocumento }} · {{ (item.perfiles ?? []).join(', ') || '—' }}</p>
                  </div>
                  <div class="flex items-center gap-0.5 shrink-0">
                    <button mat-icon-button class="!w-7 !h-7" (click)="verGrados(item)">
                      <mat-icon svgIcon="graduation-cap" class="text-sky-500 size-3.5" />
                    </button>
                    <button mat-icon-button class="!w-7 !h-7" (click)="editar(item)">
                      <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                    </button>
                    @if (item.activo) {
                      <button mat-icon-button class="!w-7 !h-7" (click)="eliminar(item)">
                        <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
                      </button>
                    }
                  </div>
                </div>
              </div>
            }
            @empty {
              <div class="flex flex-col items-center gap-2 py-16 anim-fade-in">
                <mat-icon svgIcon="heroicons_outline:identification" class="size-10 text-slate-200" />
                <p class="text-sm text-slate-400">No hay personas registradas</p>
              </div>
            }
          </div>

        }
      </div>

      <!-- ── Pagination ── -->
      @if (!loading()) {
        <div class="page-footer">
          <pagination-controls
            [totalItems]="total()" [itemsPerPage]="size()" [currentPage]="page()"
            (paginationChange)="onPage($event)" />
        </div>
      }

    </div>
  `,
})
export class PersonaReportComponent implements OnInit, OnDestroy {
  private _service = inject(PersonaService);
  private _router = inject(Router);
  private _confirm = inject(ConfirmDialogService);
  private _dialog = inject(MatDialog);
  private _fb = inject(UntypedFormBuilder);

  protected rows = signal<any[]>([]);
  protected visibleItems = signal<any[]>([]);
  protected total = signal(0);
  protected page = signal(0);
  protected size = signal(20);
  protected loading = signal(false);
  protected highlightedId = signal<string | null>(null);

  protected readonly skeletonRows = Array(8).fill(null);
  private _revealTimers: ReturnType<typeof setTimeout>[] = [];

  /** Id de una persona recién registrada (llega por router state) para resaltarla. */
  private _pendingHighlightId: string | null = null;

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    // Al volver del formulario tras registrar, resaltamos la fila nueva.
    const state = this._router.lastSuccessfulNavigation()?.extras.state as { registeredId?: string } | undefined;
    this._pendingHighlightId = state?.registeredId ?? null;

    this.filterForm = this._fb.group({ search: [''], tipoPerfil: [''], activo: ['true'] });
    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => { this.page.set(0); this.load(); });
    this.load();
  }

  ngOnDestroy(): void { this._clearTimers(); }

  load(): void {
    this.loading.set(true);
    const v = this.filterForm.value;
    const f: PersonaFiltros = {
      search: v.search || undefined,
      tipoPerfil: v.tipoPerfil || undefined,
      activo: v.activo === '' ? undefined : v.activo === 'true',
      page: this.page(), size: this.size(),
    };
    this._service.listar$(f).subscribe({
      next: (res) => {
        const data = res?.data ?? res;
        this.rows.set(data?.content ?? []);
        this.total.set(data?.total ?? data?.totalElements ?? 0);
        this.loading.set(false);
        this._revealRows(data?.content ?? []);
        this._applyPendingHighlight(data?.content ?? []);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PaginationEvent): void { this.page.set(e.page); this.size.set(e.size); this.load(); }

  nuevo(): void { this._router.navigate(['/admin/personas/nueva']); }
  editar(row: any): void { this._router.navigate(['/admin/personas', row.id]); }

  verGrados(row: any): void {
    this._dialog.open(PersonaGradosDialogComponent, {
      panelClass: ['dialog-anim'], autoFocus: false, width: '560px', maxWidth: '92vw',
      data: { id: row.id, nombre: `${row.nombres ?? ''} ${row.apellidos ?? ''}`.trim() },
    });
  }

  eliminar(row: any): void {
    this._confirm.confirmDelete({
      title: 'Eliminar persona',
      message: `¿Desactivar a ${row.nombres} ${row.apellidos}? (borrado lógico)`,
    })
      .then(() => this._service.eliminar$(row.id).subscribe({ next: () => this.load() }))
      .catch(() => {});
  }

  private _revealRows(items: any[]): void {
    this._clearTimers();
    this.visibleItems.set([]);
    items.forEach((item, i) => {
      const t = setTimeout(() => this.visibleItems.update(prev => [...prev, item]), i * ROW_STAGGER_MS);
      this._revealTimers.push(t);
    });
  }

  /** Resalta (una sola vez) la fila de la persona recién registrada, si está en la página. */
  private _applyPendingHighlight(items: any[]): void {
    const id = this._pendingHighlightId;
    if (!id) { return; }
    this._pendingHighlightId = null;
    if (!items.some(it => it.id === id)) { return; }
    // Espera a que las filas terminen de revelarse antes de disparar el flash.
    const t = setTimeout(() => {
      this.highlightedId.set(id);
      const clear = setTimeout(() => this.highlightedId.set(null), 2000);
      this._revealTimers.push(clear);
    }, items.length * ROW_STAGGER_MS + 60);
    this._revealTimers.push(t);
  }

  private _clearTimers(): void {
    this._revealTimers.forEach(t => clearTimeout(t));
    this._revealTimers = [];
  }
}