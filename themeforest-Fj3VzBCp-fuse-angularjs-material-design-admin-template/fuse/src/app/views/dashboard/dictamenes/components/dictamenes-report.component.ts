import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { debounceTime } from 'rxjs';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { DictamenService } from '../services/dictamen.service';

const BADGE: Record<string, string> = {
  POR_ELABORAR: 'bg-amber-50 text-amber-700',
  ELABORADO: 'bg-sky-50 text-sky-700',
  FIRMADO: 'bg-emerald-50 text-emerald-700',
  OBSERVADO: 'bg-rose-50 text-rose-600',
};
const LABEL: Record<string, string> = {
  POR_ELABORAR: 'Por elaborar', ELABORADO: 'Elaborado', FIRMADO: 'Firmado', OBSERVADO: 'Observado',
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

      <div class="px-6 pt-4">
        @if (resumen(); as r) {
          <div class="grid grid-cols-3 gap-3">
            <div class="rounded-xl border border-slate-100 p-4"><p class="text-2xl font-bold text-amber-600">{{ r.porElaborar }}</p><p class="text-xs text-slate-400">Por elaborar</p></div>
            <div class="rounded-xl border border-slate-100 p-4"><p class="text-2xl font-bold text-sky-600">{{ r.elaborados }}</p><p class="text-xs text-slate-400">Elaborados</p></div>
            <div class="rounded-xl border border-slate-100 p-4"><p class="text-2xl font-bold text-emerald-600">{{ r.firmados }}</p><p class="text-xs text-slate-400">Firmados</p></div>
          </div>
        }
      </div>

      <div class="page-toolbar">
        <form [formGroup]="filterForm" class="flex flex-wrap items-center gap-2">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="buscar" placeholder="Buscar estudiante..." />
          </div>
          <select formControlName="estado" class="filter-select">
            <option value="POR_ELABORAR">Por elaborar</option>
            <option value="ELABORADO">Elaborados</option>
            <option value="FIRMADO">Firmados</option>
            <option value="OBSERVADO">Observados</option>
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
                <tr><th class="w-8">#</th><th>Estudiante</th><th>Programa</th><th>Asesor / Co-asesor</th><th class="w-28">Estado</th><th class="w-40 text-right">Acción</th></tr>
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
                      Asesor: {{ item.asesorNombre ?? '—' }}
                      @if (item.coasesorNombre) { <br/>Co-asesor: {{ item.coasesorNombre }} }
                    </td>
                    <td><span class="text-[11px] font-medium px-2 py-0.5 rounded-full" [class]="badge(item.estadoDictamen)">{{ label(item.estadoDictamen) }}</span></td>
                    <td class="text-right">
                      <button class="btn-dark !h-7 !text-xs !px-3" (click)="abrir(item)">
                        {{ item.estadoDictamen === 'FIRMADO' ? 'Ver' : (item.estadoDictamen === 'ELABORADO' ? 'Subir firmado' : 'Elaborar dictamen') }}
                      </button>
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

  protected badge = (e: string) => BADGE[e] ?? 'bg-slate-100 text-slate-500';
  protected label = (e: string) => LABEL[e] ?? e;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ buscar: [''], estado: ['POR_ELABORAR'] });
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
