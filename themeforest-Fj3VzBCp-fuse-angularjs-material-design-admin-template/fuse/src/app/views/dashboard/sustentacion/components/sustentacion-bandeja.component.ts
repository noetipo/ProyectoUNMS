import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { SustentacionService } from '../services/sustentacion.service';

/**
 * Secretaría · Etapa 8, la última del proceso. Expedientes con el Jurado de Sustentación
 * solicitado: recepción, dictamen de designación, programación del acto, acta y cierre.
 */
@Component({
  selector: 'app-sustentacion-bandeja',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 8 · Sustentación</p>
          <h1 class="page-title">Sustentación de tesis</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <p class="text-[12px] text-slate-500 mb-3">
          El último trámite: recepciona la solicitud, elabora el dictamen del Jurado, coordina
          modalidad/lugar/fecha del acto y, tras la sustentación, registra el Acta. Con eso la
          tesis queda concluida.
        </p>

        <div class="data-table">
          <table class="w-full text-sm">
            <thead><tr class="text-slate-400 text-left">
              <th class="py-2 px-3 font-medium">Doctorando</th>
              <th class="py-2 px-3 font-medium">Solicitud</th>
              <th class="py-2 px-3 font-medium">Jurado</th>
              <th class="py-2 px-3 font-medium">Dictamen</th>
              <th class="py-2 px-3 font-medium">Sustentación</th>
              <th class="py-2 px-3 font-medium">Pendiente</th>
              <th></th>
            </tr></thead>
            <tbody>
              @for (r of rows(); track r.tesisId) {
                <tr class="border-t border-slate-100">
                  <td class="py-2 px-3 text-slate-700">
                    {{ r.estudianteNombre }}<br>
                    <span class="text-[11px] text-slate-400">{{ r.codigoSistema }}</span>
                  </td>
                  <td class="py-2 px-3 text-slate-500 whitespace-nowrap">
                    {{ r.fechaSolicitud | date:'dd/MM/yyyy' }}
                    <br><span [class]="chip(r.expedienteRecibido ? 'ok' : 'pend')">{{ r.expedienteRecibido ? 'Recibido' : 'Por recibir' }}</span>
                  </td>
                  <td class="py-2 px-3">
                    <span [class]="chip(r.numJurado >= 3 ? 'ok' : 'pend')">{{ r.numJurado }} / 3</span>
                  </td>
                  <td class="py-2 px-3">
                    <span [class]="chip(r.estadoDictamen === 'FIRMADO' ? 'ok' : (r.estadoDictamen === 'ELABORADO' ? 'curso' : 'pend'))">
                      {{ estadoLabel(r.estadoDictamen) }}
                    </span>
                  </td>
                  <td class="py-2 px-3 whitespace-nowrap">
                    @if (r.sustentacionProgramada) {
                      <span [class]="chip(r.concluida ? 'ok' : 'curso')">{{ r.fechaSustentacion | date:'dd/MM/yyyy' }}</span>
                    } @else {
                      <span class="text-[11px] text-slate-300">—</span>
                    }
                  </td>
                  <td class="py-2 px-3 text-[12px]" [ngClass]="r.concluida ? 'text-emerald-600' : 'text-slate-600'">{{ r.pendiente }}</td>
                  <td class="py-2 px-3 text-right">
                    <div class="row-actions">
                      <button mat-icon-button class="!w-7 !h-7" title="Abrir el trámite" (click)="abrir(r)">
                        <mat-icon svgIcon="arrow-right" class="size-4 text-[#8C1D2E]" />
                      </button>
                    </div>
                  </td>
                </tr>
              }
              @if (!rows().length && !loading()) {
                <tr><td colspan="7" class="py-6 text-center text-sm text-slate-400">
                  Aún no hay expedientes con la sustentación solicitada.
                </td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class SustentacionBandejaComponent implements OnInit {
  private _svc = inject(SustentacionService);
  private _toast = inject(NotificationService);
  private _router = inject(Router);

  protected rows = signal<any[]>([]);
  protected loading = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.loading.set(true);
    this._svc.bandeja$().subscribe({
      next: (res) => { this.rows.set(res?.data ?? res ?? []); this.loading.set(false); },
      error: () => { this.loading.set(false); this._toast.error('No se pudo cargar la bandeja'); },
    });
  }

  protected abrir(r: any): void {
    this._router.navigate(['/admin/sustentacion', r.tesisId]);
  }

  protected estadoLabel(estado: string): string {
    return estado === 'FIRMADO' ? 'Firmado' : estado === 'ELABORADO' ? 'Elaborado' : 'Por elaborar';
  }

  protected chip(tipo: 'ok' | 'curso' | 'pend' | 'malo'): string {
    const base = 'inline-block px-2 py-0.5 rounded text-[11px] font-bold whitespace-nowrap ';
    switch (tipo) {
      case 'ok': return base + 'bg-emerald-100 text-emerald-700';
      case 'curso': return base + 'bg-amber-100 text-amber-700';
      case 'malo': return base + 'bg-rose-100 text-rose-600';
      default: return base + 'bg-[#8C1D2E]/10 text-[#8C1D2E]';
    }
  }
}
