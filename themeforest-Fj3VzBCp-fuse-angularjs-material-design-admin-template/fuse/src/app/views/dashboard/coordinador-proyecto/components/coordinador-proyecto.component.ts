import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { CoordinadorProyectoService } from '../services/coordinador-proyecto.service';
import { DesignarRevisoresDialogComponent } from './designar-revisores-dialog.component';
import { DesignarJuradoInformeDialogComponent } from './designar-jurado-informe-dialog.component';
import { DesignarJuradoSustentacionDialogComponent } from './designar-jurado-sustentacion-dialog.component';
import { DetalleDesignacionDialogComponent } from './detalle-designacion-dialog.component';

/**
 * Coordinador · Etapa 5 (Defensa) — paso 2: designa los 2 revisores del proyecto.
 * Recibe los proyectos que la Secretaría ya recepcionó.
 */
@Component({
  selector: 'app-coordinador-proyecto',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 5 · Defensa del proyecto</p>
          <h1 class="page-title">Designación de revisores</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <p class="text-[12px] text-slate-500 mb-3">
          Proyectos con expediente recepcionado por Secretaría. Designa <b>2 revisores</b> (Jurado Informante) para que evalúen el proyecto.
        </p>

        <div class="data-table">
          <table class="w-full text-sm">
            <thead><tr class="text-slate-400 text-left">
              <th class="py-2 px-3 font-medium">Estudiante</th>
              <th class="py-2 px-3 font-medium">Programa</th>
              <th class="py-2 px-3 font-medium">Título</th>
              <th class="py-2 px-3 font-medium">Recepción</th>
              <th class="py-2 px-3 font-medium">Revisores</th>
              <th class="py-2 px-3 font-medium">Defensa</th>
              <th></th>
            </tr></thead>
            <tbody>
              @for (r of rows(); track r.tesisId) {
                <tr class="border-t border-slate-100"
                    [ngClass]="r.tesisId === resaltado() ? 'bg-amber-50 ring-1 ring-inset ring-amber-300' : ''">
                  <td class="py-2 px-3 text-slate-700">{{ r.estudianteApellidos }}, {{ r.estudianteNombres }}<br><span class="text-[11px] text-slate-400">{{ r.codigoSistema }}</span></td>
                  <td class="py-2 px-3 text-slate-500">{{ r.programaNombre }}</td>
                  <td class="py-2 px-3 text-slate-600 max-w-[280px] truncate">{{ r.tituloTesis }}</td>
                  <td class="py-2 px-3 text-slate-500">{{ r.fechaRecepcion | date:'dd/MM/yyyy' }}</td>
                  <td class="py-2 px-3">
                    @if (r.revisoresDesignados) {
                      <button type="button" class="inline-flex items-center gap-1 hover:underline" (click)="verDetalle(r)"
                              title="Ver revisores designados">
                        <span class="px-2 py-0.5 rounded text-[11px] font-bold"
                              [ngClass]="r.revisoresConformes ? 'bg-emerald-100 text-emerald-700' : 'bg-sky-100 text-sky-700'">
                          {{ r.revisoresConformes ? 'CONFORMES' : 'DESIGNADOS' }}
                        </span>
                        <mat-icon svgIcon="eye" class="size-3.5 text-slate-400" />
                      </button>
                    } @else {
                      <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-700">{{ r.numRevisores }} / 2</span>
                    }
                  </td>
                  <td class="py-2 px-3">
                    @if (r.defensaProgramada) {
                      <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-700">{{ r.fechaDefensa | date:'dd/MM/yyyy' }}</span>
                    } @else {
                      <span class="text-[11px] text-slate-400">—</span>
                    }
                  </td>
                  <td class="py-2 px-3 text-right">
                    @if (!r.revisoresDesignados) {
                      <button mat-flat-button color="primary" class="!h-8 !text-xs" (click)="designar(r)">
                        <mat-icon svgIcon="user-plus" class="size-3.5 mr-1" /> Designar revisores
                      </button>
                    } @else if (r.revisoresConformes && !r.defensaProgramada) {
                      <span class="text-[11px] text-slate-400">Lista para defensa · la programa Secretaría</span>
                    } @else if (r.juradoInformanteSolicitado && !r.juradoInformeDesignado) {
                      <button mat-flat-button color="primary" class="!h-8 !text-xs" (click)="designarJuradoInforme(r)">
                        <mat-icon svgIcon="user-plus" class="size-3.5 mr-1" /> Jurado Informante
                      </button>
                    } @else if (r.expedienteSustentacionRecibido && !r.juradoSustentacionDesignado) {
                      <button mat-flat-button color="primary" class="!h-8 !text-xs" (click)="designarJuradoSustentacion(r)">
                        <mat-icon svgIcon="user-plus" class="size-3.5 mr-1" /> Jurado de Sustentación
                      </button>
                    } @else if (r.juradoSustentacionDesignado) {
                      <span class="text-[11px] text-emerald-600 font-semibold">Jurado de Sustentación designado</span>
                    } @else if (r.informeFinalRevisado) {
                      <span class="text-[11px] text-emerald-600 font-semibold">Informe aprobado por jurado</span>
                    } @else if (r.juradoInformeDesignado) {
                      <span class="text-[11px] text-slate-400">Jurado Informante evaluando</span>
                    } @else if (r.defensaProgramada) {
                      <span class="text-[11px] text-emerald-600 font-semibold">Defensa programada</span>
                    } @else {
                      <span class="text-[11px] text-slate-400">En revisión del jurado</span>
                    }
                  </td>
                </tr>
              }
              @if (!rows().length && !loading()) {
                <tr><td colspan="7" class="py-6 text-center text-sm text-slate-400">No hay proyectos recepcionados pendientes.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class CoordinadorProyectoComponent implements OnInit {
  private _svc = inject(CoordinadorProyectoService);
  private _toast = inject(NotificationService);
  private _dialog = inject(MatDialog);

  private _route = inject(ActivatedRoute);

  protected rows = signal<any[]>([]);
  protected loading = signal(true);
  /** ?tesis={id} — llega desde el tablero de seguimiento para señalar de qué alumno se trata. */
  protected resaltado = signal<string | null>(null);

  ngOnInit(): void {
    this.resaltado.set(this._route.snapshot.queryParamMap.get('tesis'));
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this._svc.bandeja$().subscribe({
      next: (res) => { const d = res?.data ?? res; this.rows.set(d?.content ?? d ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  designar(r: any): void {
    this._dialog.open(DesignarRevisoresDialogComponent, {
      width: '520px', maxWidth: '92vw', maxHeight: '90vh', autoFocus: false,
      data: { tesisId: r.tesisId, estudiante: r.estudianteApellidos + ', ' + r.estudianteNombres, titulo: r.tituloTesis },
    }).afterClosed().subscribe((ok) => { if (ok) this.cargar(); });
  }

  designarJuradoInforme(r: any): void {
    this._dialog.open(DesignarJuradoInformeDialogComponent, {
      width: '520px', maxWidth: '92vw', maxHeight: '90vh', autoFocus: false,
      data: { tesisId: r.tesisId, estudiante: r.estudianteApellidos + ', ' + r.estudianteNombres, titulo: r.tituloTesis },
    }).afterClosed().subscribe((ok) => { if (ok) this.cargar(); });
  }

  designarJuradoSustentacion(r: any): void {
    this._dialog.open(DesignarJuradoSustentacionDialogComponent, {
      width: '520px', maxWidth: '92vw', maxHeight: '90vh', autoFocus: false,
      data: { tesisId: r.tesisId, estudiante: r.estudianteApellidos + ', ' + r.estudianteNombres, titulo: r.tituloTesis },
    }).afterClosed().subscribe((ok) => { if (ok) this.cargar(); });
  }

  verDetalle(r: any): void {
    this._dialog.open(DetalleDesignacionDialogComponent, {
      width: '480px', maxWidth: '92vw', maxHeight: '85vh', autoFocus: false,
      data: {
        tesisId: r.tesisId, estudiante: r.estudianteApellidos + ', ' + r.estudianteNombres, titulo: r.tituloTesis,
        defensaProgramada: r.defensaProgramada, juradoInformeDesignado: r.juradoInformeDesignado,
      },
    });
  }
}
