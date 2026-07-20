import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { SupervisionProyectoService } from '../services/supervision-proyecto.service';

/** Bandeja del TUTOR: proyectos de sus tutorandos para supervisar (Etapa 4). */
@Component({
  selector: 'app-supervision-bandeja',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 4</p>
          <h1 class="page-title">Supervisión de proyectos</h1>
        </div>
      </div>
      <div class="page-content p-6">
        <div class="data-table">
          <table class="w-full text-sm">
            <thead><tr class="text-slate-400 text-left">
              <th class="py-2 px-3 font-medium">Tutorando</th>
              <th class="py-2 px-3 font-medium">Programa</th>
              <th class="py-2 px-3 font-medium">Título</th>
              <th class="py-2 px-3 font-medium">Estado</th>
              <th></th>
            </tr></thead>
            <tbody>
              @for (r of rows(); track r.tesisId) {
                <tr class="border-t border-slate-100">
                  <td class="py-2 px-3 text-slate-700">{{ r.estudianteApellidos }}, {{ r.estudianteNombres }}<br><span class="text-[11px] text-slate-400">{{ r.codigoSistema }}</span></td>
                  <td class="py-2 px-3 text-slate-500">{{ r.programaNombre }}</td>
                  <td class="py-2 px-3 text-slate-600 max-w-[320px] truncate">{{ r.tituloTesis }}</td>
                  <td class="py-2 px-3"><span class="status-badge px-2 py-0.5 rounded text-[11px] font-bold bg-sky-100 text-sky-700">{{ r.estado }}</span></td>
                  <td class="py-2 px-3 text-right">
                    <button mat-flat-button color="primary" class="!h-8 !text-xs" (click)="supervisar(r.tesisId)"><mat-icon svgIcon="eye" class="size-3.5 mr-1" /> Supervisar</button>
                  </td>
                </tr>
              }
              @if (!rows().length && !loading()) {
                <tr><td colspan="5" class="py-6 text-center text-sm text-slate-400">No tienes tutorandos con proyecto en elaboración.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class SupervisionBandejaComponent implements OnInit {
  private _svc = inject(SupervisionProyectoService);
  private _router = inject(Router);

  protected rows = signal<any[]>([]);
  protected loading = signal(true);

  ngOnInit(): void {
    this._svc.bandeja$().subscribe({
      next: (res) => { const d = res?.data ?? res; this.rows.set(d?.content ?? d ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  supervisar(tesisId: string): void { this._router.navigate(['/admin/supervision-proyecto', tesisId]); }
}
