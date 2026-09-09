import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { JuradoInformanteService } from '../services/jurado-informante.service';

/**
 * Secretaría · Etapa 7. Expedientes cuyo Jurado Informante ya fue solicitado por el estudiante:
 * recepción, dictamen de designación, archivo del expediente y Dictamen de Expedito.
 */
@Component({
  selector: 'app-jurado-informante-bandeja',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 7 · Jurado Informante</p>
          <h1 class="page-title">Trámite del Jurado Informante</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <p class="text-[12px] text-slate-500 mb-3">
          Recepciona el expediente, elabora el dictamen de designación, archívalo cuando el
          Jurado dé su conformidad y emite el Dictamen de Expedito que habilita la sustentación.
        </p>

        <div class="data-table">
          <table class="w-full text-sm">
            <thead><tr class="text-slate-400 text-left">
              <th class="py-2 px-3 font-medium">Doctorando</th>
              <th class="py-2 px-3 font-medium">Solicitud</th>
              <th class="py-2 px-3 font-medium">Jurado</th>
              <th class="py-2 px-3 font-medium">Dictamen</th>
              <th class="py-2 px-3 font-medium">Expedito</th>
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
                    <span [class]="chip(r.numJurado >= 3 ? (r.informeFinalRevisado ? 'ok' : 'curso') : 'pend')">
                      {{ r.numJurado }} / 3 @if (r.informeFinalRevisado) { · conforme }
                    </span>
                  </td>
                  <td class="py-2 px-3">
                    <span [class]="chip(r.estadoDictamen === 'FIRMADO' ? 'ok' : (r.estadoDictamen === 'ELABORADO' ? 'curso' : 'pend'))">
                      {{ estadoLabel(r.estadoDictamen) }}
                    </span>
                  </td>
                  <td class="py-2 px-3">
                    @if (r.informeFinalArchivado) {
                      <span [class]="chip(r.estadoExpedito === 'FIRMADO' ? 'ok' : (r.estadoExpedito === 'ELABORADO' ? 'curso' : 'pend'))">
                        {{ estadoLabel(r.estadoExpedito) }}
                      </span>
                    } @else {
                      <span class="text-[11px] text-slate-300">—</span>
                    }
                  </td>
                  <td class="py-2 px-3 text-[12px]" [ngClass]="r.pendiente === 'Cerrado' ? 'text-emerald-600' : 'text-slate-600'">{{ r.pendiente }}</td>
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
                  Aún no hay expedientes con el Jurado Informante solicitado.
                </td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class JuradoInformanteBandejaComponent implements OnInit {
  private _svc = inject(JuradoInformanteService);
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
    this._router.navigate(['/admin/jurado-informante', r.tesisId]);
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
