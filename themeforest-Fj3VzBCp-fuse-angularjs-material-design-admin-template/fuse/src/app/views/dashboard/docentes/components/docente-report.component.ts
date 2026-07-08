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
import { MetricCard, MetricCardsComponent } from '@/app/shared/metric-cards/metric-cards.component';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { PersonaGradosDialogComponent } from '@/app/shared/persona-grados/persona-grados-dialog.component';
import { GRADOS_ACADEMICOS, GRADOS_ACADEMICOS_LABELS } from '../../personas/models/persona.model';
import { PersonaService } from '../../personas/services/persona.service';
import { DocenteFiltros, DocenteService } from '../services/docente.service';
import { exportToCsv } from '@/app/shared/utils/export-csv';

const ROW_STAGGER_MS = 55;

@Component({
  selector: 'app-docente-report',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule, MatTooltipModule,
    MetricCardsComponent, PaginationControlsComponent,
  ],
  template: `
    <div class="page">

      <!-- ── Header ── -->
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Académico</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Docentes</span>
          </div>
          <h1 class="page-title">Docentes</h1>
        </div>
        <div class="flex items-center gap-2">
          <button type="button" class="btn-ghost" (click)="exportar()">
            <mat-icon svgIcon="heroicons_outline:arrow-down-tray" class="size-3.5" />
            Exportar
          </button>
          <button type="button" class="btn-dark" (click)="nuevo()">
            <mat-icon svgIcon="plus" class="size-3.5" />
            Nuevo docente
          </button>
        </div>
      </div>

      <!-- ── Métricas ── -->
      <div class="px-6 pt-4">
        <app-metric-cards [cards]="cards()" />
      </div>

      <!-- ── Toolbar ── -->
      <div class="page-toolbar">
        <form [formGroup]="filterForm" class="flex flex-wrap items-center gap-2">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="search" placeholder="Buscar docente..." />
          </div>
          <select formControlName="grado" class="filter-select">
            <option value="">Todo grado</option>
            @for (g of gradosOpciones; track g) {
              <option [value]="g">{{ gradoLabel(g) }}</option>
            }
          </select>
          <select formControlName="categoria" class="filter-select">
            <option value="">Toda categoría</option>
            <option value="PRINCIPAL">Principal</option>
            <option value="ASOCIADO">Asociado</option>
            <option value="AUXILIAR">Auxiliar</option>
          </select>
          <select formControlName="condicion" class="filter-select">
            <option value="">Toda condición</option>
            <option value="NOMBRADO">Nombrado</option>
            <option value="CONTRATADO">Contratado</option>
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
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="120 + (i % 3) * 30"></div>
                <div class="anim-shimmer h-2 rounded flex-1"></div>
                <div class="anim-shimmer h-3.5 w-16 rounded-full shrink-0"></div>
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
                  <th>Docente</th>
                  <th>Grado · Categoría</th>
                  <th class="w-28">Condición</th>
                  <th class="w-32">Carga</th>
                  <th class="w-20 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (item of visibleItems(); track item.personaId; let i = $index) {
                  <tr [class.anim-flash]="item.personaId === highlightedId()">
                    <td class="text-slate-400 tabular-nums">{{ (page() * size()) + i + 1 }}</td>
                    <td class="font-medium text-slate-700">{{ item.nombres }} {{ item.apellidos }}</td>
                    <td class="text-slate-500 text-sm">{{ gradoLabel(item.gradoAcademico) }} · {{ item.categoria ?? '—' }}</td>
                    <td class="text-slate-500 text-sm">{{ item.condicion ?? '—' }}</td>
                    <td class="text-slate-500 text-sm">{{ item.asesorias ?? 0 }} ases · {{ item.jurados ?? 0 }} jur</td>
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
                    <td colspan="6" class="text-center">
                      <div class="table-empty">
                        <mat-icon svgIcon="heroicons_outline:user-group" class="size-10 text-slate-200" />
                        <p class="table-empty__text">No hay docentes</p>
                        <p class="table-empty__subtext">Registra uno con el botón "Nuevo docente"</p>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Mobile cards -->
          <div class="md:hidden divide-y divide-slate-100">
            @for (item of visibleItems(); track item.personaId) {
              <div class="px-5 py-3 anim-row-in" [class.anim-flash]="item.personaId === highlightedId()">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex-1 min-w-0">
                    <p class="font-medium text-slate-800 truncate text-sm">{{ item.nombres }} {{ item.apellidos }}</p>
                    <p class="text-xs text-slate-500 truncate mt-0.5">{{ gradoLabel(item.gradoAcademico) }} · {{ item.categoria ?? '—' }} · {{ item.condicion ?? '—' }}</p>
                    <p class="text-xs text-slate-400 truncate mt-0.5">{{ item.asesorias ?? 0 }} ases · {{ item.jurados ?? 0 }} jur</p>
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
                <mat-icon svgIcon="heroicons_outline:user-group" class="size-10 text-slate-200" />
                <p class="text-sm text-slate-400">No hay docentes</p>
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
export class DocenteReportComponent implements OnInit, OnDestroy {
  private _service = inject(DocenteService);
  private _personas = inject(PersonaService);
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
  protected cards = signal<MetricCard[]>([]);
  protected highlightedId = signal<string | null>(null);

  protected readonly skeletonRows = Array(8).fill(null);
  private _revealTimers: ReturnType<typeof setTimeout>[] = [];

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ search: [''], grado: [''], categoria: [''], condicion: [''] });
    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => { this.page.set(0); this.load(); });
    this.cargarResumen();
    this.load();
  }

  ngOnDestroy(): void { this._clearTimers(); }

  load(): void {
    this.loading.set(true);
    const v = this.filterForm.value;
    const f: DocenteFiltros = {
      search: v.search || undefined,
      grado: v.grado || undefined,
      categoria: v.categoria || undefined,
      condicion: v.condicion || undefined,
      page: this.page(), size: this.size(),
    };
    this._service.list$(f).subscribe({
      next: (res) => {
        const data = res?.data ?? res;
        this.rows.set(data?.content ?? []);
        this.total.set(data?.total ?? data?.totalElements ?? 0);
        this.loading.set(false);
        this._revealRows(data?.content ?? []);
      },
      error: () => this.loading.set(false),
    });
  }

  cargarResumen(): void {
    this._service.resumen$().subscribe({
      next: (res) => {
        const d = res?.data ?? res ?? {};
        this.cards.set([
          { label: 'Total', value: d.total ?? 0 },
          { label: 'Doctores', value: d.doctores ?? 0, accent: 'text-blue-600' },
          { label: 'Asesorando', value: d.asesorando ?? 0, accent: 'text-emerald-600' },
          { label: 'En jurados', value: d.enJurados ?? 0, accent: 'text-violet-600' },
        ]);
      },
    });
  }

  onPage(e: PaginationEvent): void { this.page.set(e.page); this.size.set(e.size); this.load(); }

  nuevo(): void { this._router.navigate(['/admin/personas/nueva']); }
  editar(row: any): void { this._router.navigate(['/admin/personas', row.personaId]); }

  protected readonly gradosOpciones = GRADOS_ACADEMICOS;

  gradoLabel(grado?: string): string {
    return grado ? (GRADOS_ACADEMICOS_LABELS[grado] ?? grado) : '—';
  }

  verGrados(row: any): void {
    this._dialog.open(PersonaGradosDialogComponent, {
      panelClass: ['dialog-anim'], autoFocus: false, width: '560px', maxWidth: '92vw',
      data: { id: row.personaId, nombre: `${row.nombres ?? ''} ${row.apellidos ?? ''}`.trim() },
    });
  }

  eliminar(row: any): void {
    this._confirm.confirmDelete({
      title: 'Eliminar docente',
      message: `¿Desactivar a ${row.nombres} ${row.apellidos}? (borrado lógico de la persona)`,
    })
      .then(() => this._personas.eliminar$(row.personaId).subscribe({ next: () => this.load() }))
      .catch(() => {});
  }

  exportar(): void {
    exportToCsv('docentes', this.rows(), [
      { key: 'numeroDocumento', label: 'Documento' },
      { key: 'nombres', label: 'Nombres' },
      { key: 'apellidos', label: 'Apellidos' },
      { key: 'gradoAcademico', label: 'Grado' },
      { key: 'categoria', label: 'Categoría' },
      { key: 'condicion', label: 'Condición' },
      { key: 'asesorias', label: 'Asesorías' },
      { key: 'jurados', label: 'Jurados' },
      { key: 'carga', label: 'Carga' },
    ]);
  }

  private _revealRows(items: any[]): void {
    this._clearTimers();
    this.visibleItems.set([]);
    items.forEach((item, i) => {
      const t = setTimeout(() => this.visibleItems.update(prev => [...prev, item]), i * ROW_STAGGER_MS);
      this._revealTimers.push(t);
    });
  }

  private _clearTimers(): void {
    this._revealTimers.forEach(t => clearTimeout(t));
    this._revealTimers = [];
  }
}