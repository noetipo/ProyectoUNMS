import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { debounceTime } from 'rxjs';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { DictamenService } from '../services/dictamen.service';

// Convención de color del sistema: granate = acción pendiente (te toca a ti) · ámbar = en curso,
// esperando algo · esmeralda = resuelto · rosa = observado/rechazado.
const BADGE: Record<string, string> = {
  POR_ELABORAR: 'bg-[#FDF6F7] text-[#8C1D2E]',
  ELABORADO: 'bg-amber-50 text-amber-700',
  FIRMADO: 'bg-emerald-50 text-emerald-700',
  OBSERVADO: 'bg-rose-50 text-rose-600',
};
const PUNTO: Record<string, string> = {
  POR_ELABORAR: 'bg-[#8C1D2E]',
  ELABORADO: 'bg-amber-500',
  FIRMADO: 'bg-emerald-500',
  OBSERVADO: 'bg-rose-500',
};
// "Elaborado" se lee como terminado, y no lo está: falta subir el firmado del Director.
const LABEL: Record<string, string> = {
  POR_ELABORAR: 'Sin redactar', ELABORADO: 'Falta la firma', FIRMADO: 'Firmado', OBSERVADO: 'Observado',
};

@Component({
  selector: 'app-dictamenes-report',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule, PaginationControlsComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Proceso de Tesis</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Dictámenes de designación</span>
          </div>
          <h1 class="page-title">Dictámenes de designación de asesor</h1>
        </div>
      </div>

      <!-- Tarjetas-filtro: son la cola de trabajo, así que también sirven para filtrar.
           "Pendientes" = por elaborar + elaborados sin firma (el trámite no termina al generar). -->
      <div class="px-6 pt-4">
        @if (resumen(); as r) {
          <div class="grid grid-cols-3 gap-3">
            @for (c of tarjetas(r); track c.estado) {
              <button type="button" (click)="filtrar(c.estado)"
                      class="rounded-xl border p-4 text-left transition hover:border-slate-300"
                      [ngClass]="estadoActual() === c.estado ? 'border-[#8C1D2E]/40 bg-[#FDF6F7]' : 'border-slate-100'">
                <p class="text-2xl font-bold" [class]="c.color">{{ c.valor }}</p>
                <p class="text-xs text-slate-400">{{ c.label }}</p>
              </button>
            }
          </div>
        }
      </div>

      <div class="page-toolbar">
        <form [formGroup]="filterForm" class="flex flex-wrap items-center gap-2">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="buscar" placeholder="Buscar estudiante..." />
          </div>
          <!-- "Por elaborar" agrupa todo lo que sigue en manos de la Secretaría (redactar o
               subir la firma). El detalle de cada fila lo da su chip de estado. -->
          <select formControlName="estado" class="filter-select">
            <option value="PENDIENTES">Por elaborar</option>
            <option value="OBSERVADO">Observados</option>
            <option value="FIRMADO">Firmados</option>
            <option value="">Todos</option>
          </select>
        </form>
        @if (!loading()) { <span class="text-[11px] text-slate-400">{{ total() }} registro(s)</span> }
      </div>

      <div class="page-content">
        @if (loading()) {
          <p class="text-sm text-slate-400 p-6">Cargando…</p>
        } @else {
          <div class="overflow-x-auto">
            <table class="data-table">
              <thead>
                <tr><th class="w-8">#</th><th>Estudiante</th><th>Programa</th><th>Asesor / Co-asesor</th><th class="w-36 whitespace-nowrap">Estado</th><th class="w-20 text-right">Acción</th></tr>
              </thead>
              <tbody>
                @for (item of rows(); track item.tesisId; let i = $index) {
                  <tr>
                    <td class="text-slate-400 tabular-nums">{{ (page() * size()) + i + 1 }}</td>
                    <td>
                      <p class="font-medium text-slate-700">{{ item.estudianteApellidos }}, {{ item.estudianteNombres }}</p>
                      <p class="text-[11px] text-slate-400 truncate max-w-[280px]">{{ item.tituloTesis }}</p>
                    </td>
                    <td class="text-slate-500 text-sm">{{ item.programaNombre ?? '—' }}</td>
                    <td class="text-slate-500 text-xs">
                      <p class="truncate max-w-[240px]">{{ item.asesorNombre ?? '—' }}</p>
                      @if (item.coasesorNombre) {
                        <p class="truncate max-w-[240px] text-[11px] text-slate-400">Co-asesor: {{ item.coasesorNombre }}</p>
                      }
                    </td>
                    <td class="whitespace-nowrap">
                      <span [class]="chip(item.estadoDictamen)">
                        <span [class]="'size-1.5 rounded-full shrink-0 ' + punto(item.estadoDictamen)"></span>
                        {{ label(item.estadoDictamen) }}
                      </span>
                    </td>
                    <td class="text-right">
                      <!-- Convención de acciones de fila: iconos 7×7 con tooltip. El icono cambia
                           según el estado; granate = queda algo por hacer, gris = solo consulta. -->
                      <div class="row-actions">
                        @if (item.estadoDictamen === 'FIRMADO') {
                          <button mat-icon-button class="!w-7 !h-7" title="Revisar dictamen firmado" (click)="abrir(item)">
                            <mat-icon svgIcon="file-search" class="text-slate-400 size-3.5" />
                          </button>
                        } @else if (item.estadoDictamen === 'ELABORADO') {
                          <button mat-icon-button class="!w-7 !h-7" title="Subir dictamen firmado" (click)="abrir(item)">
                            <mat-icon svgIcon="upload" class="text-[#8C1D2E] size-3.5" />
                          </button>
                        } @else {
                          <button mat-icon-button class="!w-7 !h-7" title="Elaborar dictamen" (click)="abrir(item)">
                            <mat-icon svgIcon="stamp" class="text-[#8C1D2E] size-3.5" />
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
                @empty {
                  <tr><td colspan="6" class="text-center">
                    <div class="table-empty">
                      <mat-icon svgIcon="file-check" class="size-10 text-slate-200" />
                      <p class="table-empty__text">Sin dictámenes en este estado</p>
                      <p class="table-empty__subtext">Aparecen las tesis cuyo estudiante subió los dos documentos firmados</p>
                    </div>
                  </td></tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>

      @if (!loading()) {
        <div class="page-footer">
          <pagination-controls [totalItems]="total()" [itemsPerPage]="size()" [currentPage]="page()" (paginationChange)="onPage($event)" />
        </div>
      }
    </div>
  `,
})
export class DictamenesReportComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _svc = inject(DictamenService);
  private _router = inject(Router);

  protected rows = signal<any[]>([]);
  protected resumen = signal<any | null>(null);
  protected total = signal(0);
  protected page = signal(0);
  protected size = signal(20);
  protected loading = signal(false);
  filterForm!: UntypedFormGroup;

  /**
   * Clases completas del chip en UNA sola cadena: mezclar `class` estático con `[class]` deja el
   * resultado a merced del orden de aplicación y el `whitespace-nowrap` se perdía → el estado
   * "Falta la firma" partía en dos líneas y descuadraba la fila.
   */
  protected chip = (e: string) =>
    'inline-flex items-center gap-1.5 whitespace-nowrap px-2 py-0.5 rounded-full text-[11px] font-medium leading-5 '
    + (BADGE[e] ?? 'bg-slate-100 text-slate-500');
  protected punto = (e: string) => PUNTO[e] ?? 'bg-slate-300';
  protected label = (e: string) => LABEL[e] ?? e;

  /** Estado filtrado ahora mismo (para resaltar la tarjeta correspondiente). */
  protected estadoActual = signal('PENDIENTES');

  /**
   * Tres etapas EXCLUYENTES: cada dictamen se cuenta una sola vez y las cifras suman el total.
   * (El desplegable sí agrupa las dos primeras bajo "Por elaborar", porque ahí lo que se elige
   * es la cola de trabajo de la Secretaría, no la etapa.)
   */
  protected tarjetas(r: any) {
    return [
      { estado: 'POR_ELABORAR', label: 'Sin redactar', valor: r.porElaborar ?? 0, color: 'text-[#8C1D2E]' },
      { estado: 'ELABORADO', label: 'Falta la firma', valor: r.elaborados ?? 0, color: 'text-amber-600' },
      { estado: 'FIRMADO', label: 'Firmados', valor: r.firmados ?? 0, color: 'text-emerald-600' },
    ];
  }

  protected filtrar(estado: string): void {
    this.filterForm.patchValue({ estado });
  }

  ngOnInit(): void {
    this.filterForm = this._fb.group({ buscar: [''], estado: ['PENDIENTES'] });
    this.filterForm.valueChanges.subscribe((v) => this.estadoActual.set(v.estado ?? ''));
    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => { this.page.set(0); this.load(); });
    this.cargarResumen();
    this.load();
  }

  cargarResumen(): void {
    this._svc.resumen$().subscribe({ next: (res) => this.resumen.set(res?.data ?? res ?? null) });
  }

  load(): void {
    this.loading.set(true);
    const v = this.filterForm.value;
    this._svc.bandeja$(v.estado || undefined, undefined, undefined, v.buscar || undefined, this.page(), this.size()).subscribe({
      next: (res) => {
        const d = res?.data ?? res;
        this.rows.set(d?.content ?? []);
        this.total.set(d?.total ?? d?.totalElements ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PaginationEvent): void { this.page.set(e.page); this.size.set(e.size); this.load(); }

  abrir(item: any): void { this._router.navigate(['/admin/dictamenes', item.tesisId]); }
}
