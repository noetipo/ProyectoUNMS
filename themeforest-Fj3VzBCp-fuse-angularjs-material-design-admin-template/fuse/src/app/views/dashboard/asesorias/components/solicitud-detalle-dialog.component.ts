import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Detalle de una solicitud de asesoría. El docente decide aquí, con el TEMA del doctorando a la
 * vista: en la tabla el título iba truncado y el mensaje solo en un tooltip, así que aceptar o
 * rechazar era casi a ciegas.
 */
@Component({
  selector: 'app-solicitud-detalle-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="w-[520px] max-w-full">

      <!-- Quién solicita -->
      <div class="relative px-5 pt-5 pb-4 bg-[#FDF6F7] border-b border-[#8C1D2E]/10">
        <button mat-icon-button mat-dialog-close class="!absolute !top-2 !right-2 !size-8">
          <mat-icon svgIcon="x" class="size-4 text-slate-400" />
        </button>
        <div class="flex items-start gap-3 pr-8">
          <div class="size-12 shrink-0 rounded-full bg-[#8C1D2E] text-white grid place-items-center
                      text-[15px] font-bold shadow-sm">{{ iniciales() }}</div>
          <div class="min-w-0">
            <h2 class="text-[15px] font-bold text-slate-800 leading-tight">
              {{ s.estudianteApellidos }}, {{ s.estudianteNombres }}
            </h2>
            <p class="text-[11.5px] text-slate-500 mt-0.5">
              {{ s.codigoSistema ?? '—' }} · {{ s.programaNombre ?? 'Programa no registrado' }}
            </p>
            <span class="inline-block mt-1 px-2 py-0.5 rounded text-[10px] font-bold"
                  [ngClass]="s.tipo === 'COASESOR' ? 'bg-sky-100 text-sky-700' : 'bg-white text-[#8C1D2E] border border-[#8C1D2E]/20'">
              TE SOLICITA COMO {{ s.tipo === 'COASESOR' ? 'CO-ASESOR' : 'ASESOR' }}
            </span>
          </div>
        </div>
      </div>

      <div class="px-5 py-4 space-y-4 max-h-[60vh] overflow-y-auto">

        <!-- El tema: lo que realmente decide -->
        <section>
          <p class="flex items-center gap-1.5 text-[10.5px] font-bold text-slate-400 uppercase tracking-wide mb-1.5">
            <mat-icon svgIcon="file-text" class="!size-3.5" /> Tema de tesis
          </p>
          <p class="text-[13.5px] font-semibold text-slate-800 leading-snug">
            {{ s.temaTitulo ?? s.tituloTentativo ?? 'Sin título registrado' }}
          </p>
          @if (s.temaTitulo && s.tituloTentativo && s.temaTitulo !== s.tituloTentativo) {
            <p class="text-[11.5px] text-slate-400 mt-0.5">
              Título indicado en la solicitud: {{ s.tituloTentativo }}
            </p>
          }
          <div class="flex flex-wrap gap-1.5 mt-2">
            @if (s.lineaNombre) {
              <span class="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[11px]">{{ s.lineaNombre }}</span>
            }
            @if (s.nivel) {
              <span class="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[11px]">{{ s.nivel | titlecase }}</span>
            }
          </div>
          @if (s.temaResumen) {
            <p class="text-[12.5px] text-slate-600 leading-relaxed mt-2 whitespace-pre-wrap">{{ s.temaResumen }}</p>
          } @else {
            <p class="text-[12px] text-slate-400 italic mt-2">El estudiante aún no registró un resumen del tema.</p>
          }
        </section>

        <!-- Mensaje del estudiante -->
        @if (s.mensaje) {
          <section>
            <p class="flex items-center gap-1.5 text-[10.5px] font-bold text-slate-400 uppercase tracking-wide mb-1.5">
              <mat-icon svgIcon="message-square" class="!size-3.5" /> Mensaje del estudiante
            </p>
            <p class="text-[12.5px] text-slate-600 leading-relaxed border-l-2 border-slate-200 pl-3 whitespace-pre-wrap">{{ s.mensaje }}</p>
          </section>
        }

        <!-- Procedencia y fecha -->
        <div class="flex flex-wrap items-center gap-x-5 gap-y-1 rounded-lg bg-slate-50 px-3 py-2 text-[11.5px] text-slate-500">
          @if (s.tutorNombre) {
            <span class="inline-flex items-center gap-1.5">
              <mat-icon svgIcon="user-round-pen" class="!size-3.5 text-slate-400" /> Tutor: <b class="text-slate-600">{{ s.tutorNombre }}</b>
            </span>
          }
          <span class="inline-flex items-center gap-1.5">
            <mat-icon svgIcon="clock" class="!size-3.5 text-slate-400" /> Solicitado el {{ s.fechaSolicitud | date:'dd/MM/yyyy' }}
          </span>
        </div>

        @if (s.estado !== 'PENDIENTE') {
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <p class="text-[12px] text-slate-600">
              Esta solicitud ya fue respondida: <b>{{ s.estado | titlecase }}</b>.
              @if (s.motivoRespuesta) { <span class="block mt-0.5">Motivo: {{ s.motivoRespuesta }}</span> }
            </p>
          </div>
        }
      </div>

      <div class="flex items-center justify-end gap-2 px-5 py-3 border-t border-slate-100 bg-slate-50/60">
        <button mat-stroked-button mat-dialog-close class="!h-8 !text-xs">Cerrar</button>
        @if (s.estado === 'PENDIENTE') {
          <button mat-stroked-button class="!h-8 !text-xs !text-rose-600 !border-rose-200"
                  (click)="ref.close('rechazar')">Rechazar</button>
          <button class="btn-dark !h-8 !text-xs !px-4" (click)="ref.close('aceptar')">
            <mat-icon svgIcon="check" class="size-3.5 mr-1" />
            Aceptar {{ s.tipo === 'COASESOR' ? 'co-asesoría' : 'asesoría' }}
          </button>
        }
      </div>
    </div>
  `,
})
export class SolicitudDetalleDialogComponent {
  protected ref = inject(MatDialogRef<SolicitudDetalleDialogComponent>);
  protected s = inject<any>(MAT_DIALOG_DATA);

  protected iniciales = computed(() => {
    const ap = (this.s?.estudianteApellidos ?? '').trim();
    const no = (this.s?.estudianteNombres ?? '').trim();
    return ((ap.charAt(0) || '') + (no.charAt(0) || '')).toUpperCase() || '—';
  });
}
