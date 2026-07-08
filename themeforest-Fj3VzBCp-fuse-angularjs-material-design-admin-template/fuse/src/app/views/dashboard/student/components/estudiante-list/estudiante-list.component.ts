import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EstudianteService } from '../../services/estudiante.service';
import { ProgramaDoctoradoService } from '../../services/programa-doctorado.service';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { Estudiante, ProgramaDoctorado, Condicion, PaginatedResponse } from '../../models/student.models';

const ROW_STAGGER_MS = 55;
const DIALOG_BASE  = { enterAnimationDuration: '0ms', exitAnimationDuration: '220ms' };

@Component({
  selector: 'app-estudiante-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
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
            <span class="breadcrumb__current">Estudiantes</span>
          </div>
          <h1 class="page-title">Estudiantes</h1>
        </div>
        <a routerLink="/admin/student/estudiantes/nuevo" class="btn-dark">
          <mat-icon svgIcon="user-plus" class="size-3.5" />
          Nuevo estudiante
        </a>
      </div>

      <!-- ── Toolbar / Filtros ── -->
      <div class="page-toolbar">
        <form [formGroup]="filterForm" class="flex items-center gap-3 flex-wrap flex-1">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="search" placeholder="Buscar nombre, DNI, matrícula…" />
          </div>
          <select formControlName="condicion"
            class="h-8 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-600 focus:outline-none focus:ring-1 focus:ring-primary-400">
            <option value="">Todas las condiciones</option>
            @for (c of condiciones; track c) {
              <option [value]="c">{{ condicionLabel[c] }}</option>
            }
          </select>
          <select formControlName="programaDoctoradoId"
            class="h-8 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-600 focus:outline-none focus:ring-1 focus:ring-primary-400 max-w-[220px]">
            <option value="">Todos los programas</option>
            @for (p of programas(); track p.id) {
              <option [value]="p.id">{{ p.nombre }}</option>
            }
          </select>
        </form>
        @if (!loading()) {
          <span class="text-[11px] text-slate-400 anim-fade-in">
            {{ paginatedResponse().totalElements }} registro(s)
          </span>
        }
        <button mat-stroked-button
          class="!h-8 !text-xs !rounded-lg !border-emerald-300 !text-emerald-700 shrink-0"
          [disabled]="exportando()"
          (click)="exportarExcel()">
          <mat-icon svgIcon="download" class="size-3.5 mr-1" />
          {{ exportando() ? 'Exportando…' : 'Excel' }}
        </button>
      </div>

      <!-- ── Content ── -->
      <div class="page-content">
        @if (loading()) {
          <div class="divide-y divide-slate-50">
            @for (_ of skeletonRows; track $index; let i = $index) {
              <div class="skeleton-row" [style.opacity]="1 - i * 0.1">
                <div class="anim-shimmer h-2 w-4 rounded shrink-0"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="130 + (i % 3) * 25"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="65 + (i % 4) * 12"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="140 + (i % 2) * 20"></div>
                <div class="anim-shimmer h-3.5 w-20 rounded-full shrink-0"></div>
                <div class="anim-shimmer h-3.5 w-16 rounded shrink-0"></div>
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
                  <th class="w-24">Código</th>
                  <th>Apellidos y Nombres</th>
                  <th class="w-28">Matrícula</th>
                  <th>Programa</th>
                  <th>Centro Laboral</th>
                  <th class="w-28">Condición</th>
                  <th class="w-24 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (est of visibleEstudiantes(); track est.id; let i = $index) {
                  <tr>
                    <td class="text-slate-400 tabular-nums">{{ (currentPage() * pageSize()) + i + 1 }}</td>
                    <td class="font-mono text-[11px] text-slate-400">{{ est.codigoSistema }}</td>
                    <td>
                      <span class="font-medium text-slate-700">
                        {{ est.apellidoPaterno }}{{ est.apellidoMaterno ? ' ' + est.apellidoMaterno : '' }}, {{ est.nombres }}
                      </span>
                      @if (est.numeroDocumento) {
                        <span class="block text-[11px] font-mono text-slate-400">{{ est.tipoDocumento }} {{ est.numeroDocumento }}</span>
                      }
                    </td>
                    <td class="font-mono text-[11px] text-slate-500">{{ est.codMatricula ?? '—' }}</td>
                    <td class="text-slate-500 text-[11px] max-w-[160px] truncate">
                      {{ est.programaDoctorado?.nombre ?? '—' }}
                    </td>
                    <td class="text-slate-500 text-[11px] max-w-[160px] truncate">
                      {{ est.centroLaboral?.nombre ?? '—' }}
                    </td>
                    <td>
                      @if (est.condicion) {
                        <span class="status-badge"
                          [class.status-badge--active]="est.condicion === 'REGULAR'"
                          [class.status-badge--inactive]="est.condicion !== 'REGULAR'">
                          <span class="status-dot"
                            [class.status-dot--active]="est.condicion === 'REGULAR'"
                            [class.status-dot--inactive]="est.condicion !== 'REGULAR'"></span>
                          {{ condicionLabel[est.condicion] }}
                        </span>
                      } @else {
                        <span class="text-slate-300">—</span>
                      }
                    </td>
                    <td>
                      <div class="row-actions">
                        <a mat-icon-button matTooltip="Ver detalle"
                          [routerLink]="['/admin/student/estudiantes', est.id]"
                          class="!w-7 !h-7">
                          <mat-icon svgIcon="eye" class="text-sky-500 size-3.5" />
                        </a>
                        <a mat-icon-button matTooltip="Editar"
                          [routerLink]="['/admin/student/estudiantes', est.id, 'editar']"
                          class="!w-7 !h-7">
                          <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                        </a>
                        <button mat-icon-button matTooltip="Eliminar" class="!w-7 !h-7"
                          (click)="onDelete(est)">
                          <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                }
                @empty {
                  <tr>
                    <td colspan="8" class="text-center">
                      <div class="table-empty">
                        <mat-icon svgIcon="graduation-cap" class="size-10 text-slate-200" />
                        <p class="table-empty__text">No hay estudiantes registrados</p>
                        <p class="table-empty__subtext">Crea el primero con el botón "Nuevo estudiante"</p>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Mobile cards -->
          <div class="md:hidden divide-y divide-slate-100">
            @for (est of visibleEstudiantes(); track est.id) {
              <div class="px-5 py-3 anim-row-in">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex-1 min-w-0">
                    @if (est.condicion) {
                      <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium mb-1"
                        [class]="est.condicion === 'REGULAR'
                          ? 'bg-emerald-50 text-emerald-700'
                          : 'bg-slate-100 text-slate-500'">
                        <span class="w-1.5 h-1.5 rounded-full"
                          [class]="est.condicion === 'REGULAR' ? 'bg-emerald-500' : 'bg-slate-400'"></span>
                        {{ condicionLabel[est.condicion] }}
                      </span>
                    }
                    <p class="font-medium text-slate-800 text-sm truncate">
                      {{ est.apellidoPaterno }}{{ est.apellidoMaterno ? ' ' + est.apellidoMaterno : '' }}, {{ est.nombres }}
                    </p>
                    @if (est.programaDoctorado) {
                      <p class="text-xs text-slate-500 truncate mt-0.5">{{ est.programaDoctorado.nombre }}</p>
                    }
                    @if (est.codMatricula) {
                      <p class="text-xs font-mono text-slate-400 mt-0.5">{{ est.codMatricula }}</p>
                    }
                  </div>
                  <div class="flex items-center gap-0.5 shrink-0">
                    <a mat-icon-button [routerLink]="['/admin/student/estudiantes', est.id]" class="!w-7 !h-7">
                      <mat-icon svgIcon="eye" class="text-sky-500 size-3.5" />
                    </a>
                    <a mat-icon-button [routerLink]="['/admin/student/estudiantes', est.id, 'editar']" class="!w-7 !h-7">
                      <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                    </a>
                    <button mat-icon-button class="!w-7 !h-7" (click)="onDelete(est)">
                      <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            }
            @empty {
              <div class="flex flex-col items-center gap-2 py-16 anim-fade-in">
                <mat-icon svgIcon="graduation-cap" class="size-10 text-slate-200" />
                <p class="text-sm text-slate-400">No hay estudiantes registrados</p>
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
export class EstudianteListComponent implements OnInit, OnDestroy {
  private _service     = inject(EstudianteService);
  private _progService = inject(ProgramaDoctoradoService);
  private _confirm     = inject(ConfirmDialogService);
  private _fb          = inject(UntypedFormBuilder);

  protected paginatedResponse  = signal<PaginatedResponse<Estudiante>>({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 10 });
  protected visibleEstudiantes = signal<Estudiante[]>([]);
  protected programas          = signal<ProgramaDoctorado[]>([]);
  protected currentPage        = signal(0);
  protected pageSize           = signal(10);
  protected loading            = signal(false);
  protected exportando         = signal(false);

  protected readonly skeletonRows = Array(8).fill(null);
  protected readonly condiciones  = Object.values(Condicion);

  protected readonly condicionLabel: Record<string, string> = {
    REGULAR:    'Regular',
    SANCIONADO: 'Sancionado',
    EGRESADO:   'Egresado',
    RETIRADO:   'Retirado',
  };

  private _staggerTimers: ReturnType<typeof setTimeout>[] = [];

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ search: [''], condicion: [''], programaDoctoradoId: [''] });
    this.filterForm.valueChanges.subscribe(() => {
      this.currentPage.set(0);
      this.load();
    });
    // Carga lista de programas para el select (sin unwrap, accede a .content)
    this._progService.getWithQuery$({ page: 0, size: 100 }).subscribe({
      next: res => this.programas.set(res.content),
    });
    this.load();
  }

  ngOnDestroy(): void {
    this._clearTimers();
  }

  load(): void {
    this.loading.set(true);
    const params: any = { page: this.currentPage(), size: this.pageSize(), ...this.filterForm.value };
    // Elimina params vacíos para no enviar condicion='' al backend
    Object.keys(params).forEach(k => { if (params[k] === '' || params[k] == null) delete params[k]; });

    this._service.getWithQuery$(params).subscribe({
      next: res => {
        this.paginatedResponse.set(res);      // contiene totalElements, totalPages, etc.
        this.loading.set(false);
        this._stagger(res.content ?? []);     // res.content es el array de Estudiante[]
      },
      error: () => this.loading.set(false),
    });
  }

  onPageChange(event: PaginationEvent): void {
    this.currentPage.set(event.page);
    this.pageSize.set(event.size);
    this.load();
  }

  onDelete(est: Estudiante): void {
    this._confirm.confirmDelete()
      .then(() => {
        this._service.delete(est.id).subscribe(() => {
          this.visibleEstudiantes.update(list => list.filter(e => e.id !== est.id));
          this.paginatedResponse.update(r => ({ ...r, totalElements: r.totalElements - 1 }));
        });
      })
      .catch(() => {});
  }

  exportarExcel(): void {
    this.exportando.set(true);
    const raw = this.filterForm.value;
    const params: Record<string, string> = {};
    Object.entries(raw).forEach(([k, v]) => { if (v) params[k] = String(v); });

    this._service.exportarExcel(params).subscribe({
      next: blob => {
        const url  = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href  = url;
        link.download = `estudiantes_${new Date().toISOString().slice(0, 10)}.xlsx`;
        link.click();
        URL.revokeObjectURL(url);
        this.exportando.set(false);
      },
      error: () => this.exportando.set(false),
    });
  }

  private _stagger(items: Estudiante[]): void {
    this._clearTimers();
    this.visibleEstudiantes.set([]);
    items.forEach((item, i) => {
      const t = setTimeout(() => {
        this.visibleEstudiantes.update(prev => [...prev, item]);
      }, i * ROW_STAGGER_MS);
      this._staggerTimers.push(t);
    });
  }

  private _clearTimers(): void {
    this._staggerTimers.forEach(t => clearTimeout(t));
    this._staggerTimers = [];
  }
}