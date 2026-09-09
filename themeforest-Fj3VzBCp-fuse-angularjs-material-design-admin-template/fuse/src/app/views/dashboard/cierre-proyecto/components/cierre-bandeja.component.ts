import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { CierreProyectoService } from '../services/cierre-proyecto.service';

/**
 * Secretaría · Etapa 5, tramo final. Proyectos cuya defensa ya está programada, con lo que
 * falta en cada uno: recepcionar rúbricas, registrar el resultado, dictaminar o archivar.
 */
@Component({
  selector: 'app-cierre-bandeja',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 5 · Defensa del proyecto</p>
          <h1 class="page-title">Cierre del proyecto</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <p class="text-[12px] text-slate-500 mb-3">
          Después de la defensa: recepciona las rúbricas de los revisores, registra el resultado del acto,
          emite el dictamen de aprobación y archiva el proyecto final. Al archivarlo, el doctorando pasa a la ejecución de la tesis.
        </p>

        <div class="data-table">
          <table class="w-full text-sm">
            <thead><tr class="text-slate-400 text-left">
              <th class="py-2 px-3 font-medium">Doctorando</th>
              <th class="py-2 px-3 font-medium">Defensa</th>
              <th class="py-2 px-3 font-medium">Rúbricas</th>
              <th class="py-2 px-3 font-medium">Resultado</th>
              <th class="py-2 px-3 font-medium">Dictamen</th>
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
                    {{ r.fechaDefensa | date:'dd/MM/yyyy' }}
                    @if (r.horaDefensa) { <span class="text-slate-400">· {{ r.horaDefensa }}</span> }
                    @if (r.modalidadLabel) { <br><span class="text-[11px] text-slate-400">{{ r.modalidadLabel }}</span> }
                  </td>
                  <td class="py-2 px-3">
                    <span [class]="chip(r.rubricasRecibidas >= r.rubricasEsperadas ? 'ok' : 'curso')">
                      {{ r.rubricasRecibidas }} de {{ r.rubricasEsperadas }}
                    </span>
                  </td>
                  <td class="py-2 px-3">
                    @if (r.resultadoLabel) {
                      <span [class]="chip(r.resultado === 'DESAPROBADO' ? 'malo' : 'ok')">{{ r.resultadoLabel }}</span>
                    } @else {
                      <span [class]="chip('pend')">Sin registrar</span>
                    }
                  </td>
                  <td class="py-2 px-3">
                    <span [class]="chip(r.estadoDictamen === 'FIRMADO' ? 'ok' : (r.estadoDictamen === 'ELABORADO' ? 'curso' : 'pend'))">
                      {{ estadoDictamen(r.estadoDictamen) }}
                    </span>
                    @if (r.dictamenNumero) { <br><span class="text-[11px] text-slate-400">{{ r.dictamenNumero }}</span> }
                  </td>
                  <td class="py-2 px-3 text-[12px]" [ngClass]="r.cerrado ? 'text-emerald-600' : 'text-slate-600'">{{ r.pendiente }}</td>
                  <td class="py-2 px-3 text-right">
                    <div class="row-actions">
                      <button mat-icon-button class="!w-7 !h-7" title="Abrir el cierre del proyecto" (click)="abrir(r)">
                        <mat-icon svgIcon="arrow-right" class="size-4 text-[#8C1D2E]" />
                      </button>
                    </div>
                  </td>
                </tr>
              }
              @if (!rows().length && !loading()) {
                <tr><td colspan="7" class="py-6 text-center text-sm text-slate-400">
                  Aún no hay proyectos con la defensa programada.
                </td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class CierreBandejaComponent implements OnInit {
  private _svc = inject(CierreProyectoService);
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
    this._router.navigate(['/admin/cierre-proyecto', r.tesisId]);
  }

  protected estadoDictamen(estado: string): string {
    return estado === 'FIRMADO' ? 'Firmado' : estado === 'ELABORADO' ? 'Elaborado' : 'Por elaborar';
  }

  /** Colores de la convención: ámbar en curso, esmeralda resuelto, granate pendiente, rosa rechazo. */
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
