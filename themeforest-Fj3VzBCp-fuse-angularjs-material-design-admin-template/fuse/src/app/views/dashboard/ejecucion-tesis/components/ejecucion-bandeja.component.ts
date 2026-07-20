import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { EjecucionTesisService } from '../services/ejecucion-tesis.service';

/** Asesor · Etapa 6 — bandeja de tesis en ejecución. */
@Component({
  selector: 'app-ejecucion-bandeja',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 6 · Ejecución de la tesis</p>
          <h1 class="page-title">Ejecución de la tesis</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <p class="text-[12px] text-slate-500 mb-3">
          Tesis en ejecución (proyecto aprobado). Evalúa el avance con la rúbrica y, con el plan 100% ejecutado, aprueba el informe final.
        </p>

        <div class="data-table">
          <table class="w-full text-sm">
            <thead><tr class="text-slate-400 text-left">
              <th class="py-2 px-3 font-medium">Estudiante</th>
              <th class="py-2 px-3 font-medium">Título</th>
              <th class="py-2 px-3 font-medium">Avance del plan</th>
              <th class="py-2 px-3 font-medium">Evaluaciones</th>
              <th class="py-2 px-3 font-medium">Informe final</th>
              <th></th>
            </tr></thead>
            <tbody>
              @for (r of rows(); track r.tesisId) {
                <tr class="border-t border-slate-100">
                  <td class="py-2 px-3 text-slate-700">{{ r.estudianteApellidos }}, {{ r.estudianteNombres }}<br><span class="text-[11px] text-slate-400">{{ r.codigoSistema }}</span></td>
                  <td class="py-2 px-3 text-slate-600 max-w-[240px] truncate">{{ r.tituloTesis }}</td>
                  <td class="py-2 px-3">
                    <div class="flex items-center gap-2">
                      <div class="h-1.5 w-24 rounded bg-slate-100 overflow-hidden">
                        <div class="h-full rounded" [ngClass]="r.porcentajePlan >= 100 ? 'bg-emerald-500' : 'bg-[#8C1D2E]'" [style.width.%]="r.porcentajePlan"></div>
                      </div>
                      <span class="text-[11px] font-bold" [ngClass]="r.porcentajePlan >= 100 ? 'text-emerald-600' : 'text-[#8C1D2E]'">{{ r.porcentajePlan }}%</span>
                    </div>
                  </td>
                  <td class="py-2 px-3 text-slate-500">{{ r.numAvances }}</td>
                  <td class="py-2 px-3">
                    <span class="px-2 py-0.5 rounded text-[11px] font-bold" [ngClass]="r.informeFinalAprobado ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                      {{ r.informeFinalAprobado ? 'APROBADO' : 'PENDIENTE' }}
                    </span>
                  </td>
                  <td class="py-2 px-3 text-right">
                    <button mat-flat-button color="primary" class="!h-8 !text-xs" (click)="abrir(r)">
                      <mat-icon svgIcon="clipboard-check" class="size-3.5 mr-1" /> Ver / evaluar
                    </button>
                  </td>
                </tr>
              }
              @if (!rows().length && !loading()) {
                <tr><td colspan="6" class="py-6 text-center text-sm text-slate-400">No tienes tesis en ejecución.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class EjecucionBandejaComponent implements OnInit {
  private _svc = inject(EjecucionTesisService);
  private _router = inject(Router);

  protected rows = signal<any[]>([]);
  protected loading = signal(true);

  ngOnInit(): void {
    this._svc.bandeja$().subscribe({
      next: (res) => { this.rows.set((res?.data ?? res) ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  abrir(r: any): void { this._router.navigate(['/admin/ejecucion-tesis', r.tesisId]); }
}
