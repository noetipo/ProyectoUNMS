import { CommonModule } from '@angular/common';
import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { debounceTime } from 'rxjs';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { MetricCard, MetricCardsComponent } from '@/app/shared/metric-cards/metric-cards.component';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { PersonaService } from '../../personas/services/persona.service';
import { EstudianteFiltros, EstudianteService } from '../services/estudiante.service';
import { exportToCsv } from '@/app/shared/utils/export-csv';

const ROW_STAGGER_MS = 55;

@Component({
  selector: 'app-estudiante-report',
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
            <span class="breadcrumb__current">Estudiantes</span>
          </div>
          <h1 class="page-title">Estudiantes</h1>
        </div>
        <div class="flex items-center gap-2">
          <button type="button" class="btn-ghost" (click)="exportar()">
            <mat-icon svgIcon="heroicons_outline:arrow-down-tray" class="size-3.5" />
            Exportar
          </button>
          <button type="button" class="btn-dark" (click)="nuevo()">
            <mat-icon svgIcon="plus" class="size-3.5" />
            Nuevo estudiante
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
            <input formControlName="search" placeholder="Buscar estudiante..." />
          </div>
          <select formControlName="facultadId" class="filter-select">
            <option value="">Todas las facultades</option>
            @for (f of facultadOptions(); track f.value) { <option [value]="f.value">{{ f.label }}</option> }
          </select>
          <select formControlName="programaId" class="filter-select">
            <option value="">Todos los programas</option>
            @for (p of programaOptions(); track p.value) { <option [value]="p.value">{{ p.label }}</option> }
          </select>
          <select formControlName="condicion" class="filter-select">
            <option value="">Toda condición</option>
            <option value="REGULAR">Regular</option>
            <option value="SANCIONADO">Sancionado</option>
            <option value="EGRESADO">Egresado</option>
            <option value="RETIRADO">Retirado</option>
          </select>
          <select formControlName="nivel" class="filter-select">
            <option value="">Todo nivel</option>
            <option value="MAESTRIA">Maestría</option>
            <option value="DOCTORADO">Doctorado</option>
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
                  <th>Estudiante</th>
                  <th class="w-28">Matrícula</th>
                  <th>Programa</th>
                  <th class="w-28">Condición</th>
                  <th class="w-20 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (item of visibleItems(); track item.personaId; let i = $index) {
                  <tr [class.anim-flash]="item.personaId === highlightedId()">
                    <td class="text-slate-400 tabular-nums">{{ (page() * size()) + i + 1 }}</td>
                    <td class="font-medium text-slate-700">{{ item.nombres }} {{ item.apellidos }}</td>
                    <td class="font-mono text-[11px] text-slate-400">{{ item.codMatricula ?? '—' }}</td>
                    <td class="text-slate-500 text-sm max-w-[260px] truncate">{{ item.programaNombre ?? '—' }}</td>
                    <td>
                      <span class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
                        [class]="condicionBadge(item.condicion)">{{ item.condicion ?? '—' }}</span>
                    </td>
                    <td>
                      <div class="row-actions">
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
                        <mat-icon svgIcon="heroicons_outline:academic-cap" class="size-10 text-slate-200" />
                        <p class="table-empty__text">No hay estudiantes</p>
                        <p class="table-empty__subtext">Registra uno con el botón "Nuevo estudiante"</p>
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
                    <span class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
                      [class]="condicionBadge(item.condicion)">{{ item.condicion ?? '—' }}</span>
                    <p class="font-medium text-slate-800 truncate text-sm mt-1">{{ item.nombres }} {{ item.apellidos }}</p>
                    <p class="text-xs text-slate-500 truncate mt-0.5">{{ item.codMatricula ?? '—' }} · {{ item.programaNombre ?? '—' }}</p>
                  </div>
                  <div class="flex items-center gap-0.5 shrink-0">
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
                <mat-icon svgIcon="heroicons_outline:academic-cap" class="size-10 text-slate-200" />
                <p class="text-sm text-slate-400">No hay estudiantes</p>
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
export class EstudianteReportComponent implements OnInit, OnDestroy {
  private _service = inject(EstudianteService);
  private _personas = inject(PersonaService);
  private _router = inject(Router);
  private _confirm = inject(ConfirmDialogService);
  private _fb = inject(UntypedFormBuilder);

  protected rows = signal<any[]>([]);
  protected visibleItems = signal<any[]>([]);
  protected total = signal(0);
  protected page = signal(0);
  protected size = signal(20);
  protected loading = signal(false);
  protected cards = signal<MetricCard[]>([]);
  protected highlightedId = signal<string | null>(null);
  protected programaOptions = signal<{ value: string; label: string }[]>([]);
  protected facultadOptions = signal<{ value: string; label: string }[]>([]);

  protected readonly skeletonRows = Array(8).fill(null);
  protected readonly condicionBadge = condicionBadge;
  private _revealTimers: ReturnType<typeof setTimeout>[] = [];

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ search: [''], facultadId: [''], programaId: [''], condicion: [''], nivel: [''] });
    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => { this.page.set(0); this.load(); });
    // Cascada Facultad → Programa.
    this.filterForm.get('facultadId')!.valueChanges.subscribe((facId: string) => {
      this.filterForm.get('programaId')!.setValue('', { emitEvent: false });
      this._cargarProgramas(facId);
    });
    this._personas.listFacultades$().subscribe({
      next: (res: any) => {
        const facs = res?.data?.content ?? res?.data ?? res ?? [];
        this.facultadOptions.set(facs.map((f: any) => ({ value: f.id, label: f.nombre })));
      },
    });
    this._cargarProgramas('');
    this.cargarResumen();
    this.load();
  }

  private _cargarProgramas(facultadId?: string): void {
    this._personas.listProgramas$(facultadId || undefined).subscribe({
      next: (res) => {
        const progs = res?.data ?? res ?? [];
        this.programaOptions.set(progs.map((p: any) => ({ value: p.id, label: p.nombre })));
      },
    });
  }

  ngOnDestroy(): void { this._clearTimers(); }

  load(): void {
    this.loading.set(true);
    const v = this.filterForm.value;
    const f: EstudianteFiltros = {
      search: v.search || undefined,
      facultadId: v.facultadId || undefined,
      programaId: v.programaId || undefined,
      condicion: v.condicion || undefined,
      nivel: v.nivel || undefined,
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
          { label: 'Regulares', value: d.regulares ?? 0, accent: 'text-emerald-600' },
          { label: 'Con tesis activa', value: d.conTesisActiva ?? 0, accent: 'text-blue-600' },
          { label: 'Egresados', value: d.egresados ?? 0, accent: 'text-slate-500' },
        ]);
      },
    });
  }

  onPage(e: PaginationEvent): void { this.page.set(e.page); this.size.set(e.size); this.load(); }

  nuevo(): void { this._router.navigate(['/admin/personas/nueva']); }
  editar(row: any): void { this._router.navigate(['/admin/personas', row.personaId]); }

  eliminar(row: any): void {
    this._confirm.confirmDelete({
      title: 'Eliminar estudiante',
      message: `¿Desactivar a ${row.nombres} ${row.apellidos}? (borrado lógico de la persona)`,
    })
      .then(() => this._personas.eliminar$(row.personaId).subscribe({ next: () => this.load() }))
      .catch(() => {});
  }

  exportar(): void {
    exportToCsv('estudiantes', this.rows(), [
      { key: 'numeroDocumento', label: 'Documento' },
      { key: 'nombres', label: 'Nombres' },
      { key: 'apellidos', label: 'Apellidos' },
      { key: 'codMatricula', label: 'Matrícula' },
      { key: 'programaNombre', label: 'Programa' },
      { key: 'condicion', label: 'Condición' },
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

function condicionBadge(c: string): string {
  switch (c) {
    case 'REGULAR': return 'bg-emerald-50 text-emerald-700';
    case 'RETIRADO': return 'bg-red-50 text-red-700';
    case 'SANCIONADO': return 'bg-amber-50 text-amber-700';
    case 'EGRESADO': return 'bg-slate-100 text-slate-600';
    default: return 'bg-slate-100 text-slate-500';
  }
}