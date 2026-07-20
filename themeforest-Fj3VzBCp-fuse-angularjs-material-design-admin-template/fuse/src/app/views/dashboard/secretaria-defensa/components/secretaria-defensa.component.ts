import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { SecretariaDefensaService } from '../services/secretaria-defensa.service';

/**
 * Secretaría · Etapa 5 (Defensa) — paso 1: bandeja de solicitudes de aprobación recibidas.
 * La Secretaría recibe el expediente y comunica al Coordinador del Programa.
 */
@Component({
  selector: 'app-secretaria-defensa',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 5 · Defensa del proyecto</p>
          <h1 class="page-title">Solicitudes de aprobación recibidas</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <p class="text-[12px] text-slate-500 mb-3">
          Expedientes enviados por el estudiante (solicitud de aprobación). Recíbelos y comunícalos al Coordinador del Programa para que designe los revisores.
        </p>

        <div class="data-table">
          <table class="w-full text-sm">
            <thead><tr class="text-slate-400 text-left">
              <th class="py-2 px-3 font-medium">Estudiante</th>
              <th class="py-2 px-3 font-medium">Programa</th>
              <th class="py-2 px-3 font-medium">Título</th>
              <th class="py-2 px-3 font-medium">Fecha solicitud</th>
              <th class="py-2 px-3 font-medium">Estado</th>
              <th></th>
            </tr></thead>
            <tbody>
              @for (r of rows(); track r.tesisId) {
                <tr class="border-t border-slate-100">
                  <td class="py-2 px-3 text-slate-700">{{ r.estudianteApellidos }}, {{ r.estudianteNombres }}<br><span class="text-[11px] text-slate-400">{{ r.codigoSistema }}</span></td>
                  <td class="py-2 px-3 text-slate-500">{{ r.programaNombre }}</td>
                  <td class="py-2 px-3 text-slate-600 max-w-[300px] truncate">{{ r.tituloTesis }}</td>
                  <td class="py-2 px-3 text-slate-500">{{ r.fechaSolicitud | date:'dd/MM/yyyy' }}</td>
                  <td class="py-2 px-3">
                    <span class="status-badge px-2 py-0.5 rounded text-[11px] font-bold"
                          [ngClass]="r.recibido ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'">
                      {{ r.recibido ? 'RECIBIDO' : 'POR RECIBIR' }}
                    </span>
                  </td>
                  <td class="py-2 px-3 text-right">
                    @if (!r.recibido) {
                      <button mat-flat-button color="primary" class="!h-8 !text-xs" (click)="recibir(r)">
                        <mat-icon svgIcon="check" class="size-3.5 mr-1" /> Recibir y comunicar al Coordinador
                      </button>
                    } @else {
                      <span class="text-[11px] text-slate-400">Comunicado al Coordinador</span>
                    }
                  </td>
                </tr>
              }
              @if (!rows().length && !loading()) {
                <tr><td colspan="6" class="py-6 text-center text-sm text-slate-400">No hay solicitudes de aprobación por recibir.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class SecretariaDefensaComponent implements OnInit {
  private _svc = inject(SecretariaDefensaService);
  private _confirm = inject(ConfirmDialogService);
  private _toast = inject(NotificationService);

  protected rows = signal<any[]>([]);
  protected loading = signal(true);

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.loading.set(true);
    this._svc.bandeja$().subscribe({
      next: (res) => { const d = res?.data ?? res; this.rows.set(d?.content ?? d ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  recibir(r: any): void {
    this._confirm.confirmSave({
      title: 'Recibir expediente',
      message: 'Se marcará el expediente como recibido y se comunicará al Coordinador del Programa para que designe los revisores.',
    }).then(() => {
      this._svc.recibir$(r.tesisId).subscribe({
        next: () => { this._toast.success('Expediente recibido y comunicado al Coordinador'); this.cargar(); },
        error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo recibir el expediente'),
      });
    }).catch(() => {});
  }
}
