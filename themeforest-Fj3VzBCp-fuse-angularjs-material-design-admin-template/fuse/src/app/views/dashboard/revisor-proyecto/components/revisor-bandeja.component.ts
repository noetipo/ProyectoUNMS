import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { RevisorProyectoService } from '../services/revisor-proyecto.service';

/** Revisor (Jurado Informante) · Etapa 5 — bandeja de proyectos asignados para evaluar. */
@Component({
  selector: 'app-revisor-bandeja',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 5 · Defensa del proyecto</p>
          <h1 class="page-title">Revisión de proyectos (jurado)</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <p class="text-[12px] text-slate-500 mb-3">
          Proyectos donde fuiste designado como revisor. Evalúa cada uno con la rúbrica y observa o da conformidad.
        </p>

        <div class="data-table">
          <table class="w-full text-sm">
            <thead><tr class="text-slate-400 text-left">
              <th class="py-2 px-3 font-medium">Estudiante</th>
              <th class="py-2 px-3 font-medium">Programa</th>
              <th class="py-2 px-3 font-medium">Título</th>
              <th class="py-2 px-3 font-medium">Mi evaluación</th>
              <th></th>
            </tr></thead>
            <tbody>
              @for (r of rows(); track r.tesisId) {
                <tr class="border-t border-slate-100">
                  <td class="py-2 px-3 text-slate-700">{{ r.estudianteApellidos }}, {{ r.estudianteNombres }}<br><span class="text-[11px] text-slate-400">{{ r.codigoSistema }}</span></td>
                  <td class="py-2 px-3 text-slate-500">{{ r.programaNombre }}</td>
                  <td class="py-2 px-3 text-slate-600 max-w-[280px] truncate">{{ r.tituloTesis }}</td>
                  <td class="py-2 px-3">
                    <span class="px-2 py-0.5 rounded text-[11px] font-bold" [ngClass]="estadoCls(r.miEstado)">{{ estadoLabel(r.miEstado) }}</span>
                    @if (r.miPuntaje != null) { <span class="ml-1 text-[11px] text-slate-400">{{ r.miPuntaje }}/20</span> }
                  </td>
                  <td class="py-2 px-3 text-right">
                    <button mat-flat-button color="primary" class="!h-8 !text-xs" (click)="abrir(r)">
                      <mat-icon svgIcon="clipboard-check" class="size-3.5 mr-1" /> {{ r.miEstado === 'CONFORME' ? 'Ver evaluación' : 'Evaluar' }}
                    </button>
                  </td>
                </tr>
              }
              @if (!rows().length && !loading()) {
                <tr><td colspan="5" class="py-6 text-center text-sm text-slate-400">No tienes proyectos asignados para revisar.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class RevisorBandejaComponent implements OnInit {
  private _svc = inject(RevisorProyectoService);
  private _router = inject(Router);

  protected rows = signal<any[]>([]);
  protected loading = signal(true);

  ngOnInit(): void {
    this._svc.bandeja$().subscribe({
      next: (res) => { const d = res?.data ?? res; this.rows.set(d ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  abrir(r: any): void { this._router.navigate(['/admin/revisor-proyecto', r.tesisId]); }

  estadoLabel(e: string): string {
    return e === 'CONFORME' ? '✓ CONFORME' : e === 'OBSERVADO' ? '⚑ OBSERVADO' : 'POR EVALUAR';
  }
  estadoCls(e: string): string {
    return e === 'CONFORME' ? 'bg-emerald-100 text-emerald-700'
      : e === 'OBSERVADO' ? 'bg-rose-100 text-rose-700'
      : 'bg-slate-100 text-slate-500';
  }
}
