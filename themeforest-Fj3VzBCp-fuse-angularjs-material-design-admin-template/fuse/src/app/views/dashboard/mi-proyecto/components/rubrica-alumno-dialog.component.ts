import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { previsualizarBlob } from '@/app/shared/perfil-completo/preview.util';
import { MiProyectoService } from '../services/mi-proyecto.service';

/**
 * "¿Cómo me evalúan?" — la rúbrica oficial, en blanco, para el doctorando.
 *
 * <p>Se arma con la misma definición que usa el revisor, así que no hay dos verdades: los mismos
 * criterios y los mismos puntajes por nivel. Sin notas ni evaluaciones de nadie — eso vive en la
 * tarjeta de cada revisor.</p>
 */
@Component({
  selector: 'app-rubrica-alumno-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="flex flex-col max-h-[85vh] w-[min(92vw,720px)]">

      <div class="flex items-center gap-2.5 px-5 py-3 border-b border-slate-100">
        <mat-icon svgIcon="scale" class="size-[18px] text-[#8C1D2E] shrink-0" />
        <div class="flex-1 min-w-0">
          <h2 class="text-[13.5px] font-semibold text-slate-800 truncate">Rúbrica de evaluación</h2>
          @if (r(); as d) {
            <p class="text-[11px] text-slate-400 truncate">
              {{ d.enfoqueLabel }}@if (d.version) { · v{{ d.version }} } · así evaluará cada revisor tu proyecto
            </p>
          }
        </div>
        <button mat-icon-button (click)="cerrar()" aria-label="Cerrar">
          <mat-icon svgIcon="x" class="size-4 text-slate-500" />
        </button>
      </div>

      <div class="flex-1 overflow-auto px-5 py-4">
        @if (cargando()) {
          <p class="text-sm text-slate-400">Cargando…</p>
        } @else if (r(); as d) {
          <table class="w-full text-[12px]">
            <thead>
              <tr class="text-slate-400">
                <th class="text-left font-semibold text-[9.5px] uppercase tracking-wide pb-1.5">Criterio</th>
                <th class="w-[54px] text-center font-semibold text-[9.5px] uppercase tracking-wide pb-1.5">Cumple</th>
                <th class="w-[54px] text-center font-semibold text-[9.5px] uppercase tracking-wide pb-1.5">Parcial</th>
                <th class="w-[54px] text-center font-semibold text-[9.5px] uppercase tracking-wide pb-1.5">No cump.</th>
              </tr>
            </thead>
            <tbody>
              @for (s of d.secciones; track s.key) {
                <tr>
                  <td colspan="4" class="pt-3 pb-1">
                    <span class="text-[10.5px] font-extrabold text-[#8C1D2E] uppercase tracking-wide">{{ s.titulo }}</span>
                    <span class="text-[10px] text-slate-400 ml-1.5">({{ s.subtotalMaximo }} pts)</span>
                  </td>
                </tr>
                @for (c of s.criterios; track c.key) {
                  <tr class="border-t border-slate-100 align-top">
                    <td class="py-1.5 pr-3">
                      <p class="text-[12px] font-semibold text-slate-700 leading-snug">{{ c.titulo }}</p>
                      <p class="text-[10.5px] text-slate-400 leading-snug">{{ c.descripcion }}</p>
                    </td>
                    <td class="py-1.5 text-center"><span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700">{{ c.cumple }}</span></td>
                    <td class="py-1.5 text-center"><span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-amber-50 text-amber-700">{{ c.parcial }}</span></td>
                    <td class="py-1.5 text-center"><span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-rose-50 text-rose-700">{{ c.noCumple }}</span></td>
                  </tr>
                }
              }
            </tbody>
          </table>

          <div class="flex items-center justify-between rounded-lg bg-[#FDF6F7] border border-[#8C1D2E]/15 px-3 py-2 mt-3">
            <span class="text-[11.5px] text-slate-600">Puntaje total</span>
            <span class="text-[12px] font-extrabold text-[#8C1D2E]">
              {{ d.puntajeTotal }} · aprueba con {{ d.puntajeAprobacion }}
            </span>
          </div>
        } @else {
          <p class="text-sm text-slate-400">No se pudo cargar la rúbrica.</p>
        }
      </div>

      <div class="flex items-center justify-between gap-3 px-5 py-3 border-t border-slate-100 bg-slate-50">
        <span class="text-[11px] text-slate-400">Cada revisor marca un nivel por criterio; la suma es tu puntaje.</span>
        @if (r()?.documentoDisponible) {
          <button mat-stroked-button class="!h-8 !text-[11.5px]" [disabled]="abriendo()" (click)="verWord()">
            <mat-icon svgIcon="file-search" class="size-3.5 mr-1" /> Ver el Word oficial
          </button>
        }
      </div>
    </div>
  `,
})
export class RubricaAlumnoDialogComponent {
  private _ref = inject<MatDialogRef<RubricaAlumnoDialogComponent>>(MatDialogRef);
  private _svc = inject(MiProyectoService);
  private _toast = inject(NotificationService);
  private _dialog = inject(MatDialog);

  protected r = signal<any | null>(null);
  protected cargando = signal(true);
  protected abriendo = signal(false);

  constructor() {
    this._svc.rubrica$().subscribe({
      next: (res: any) => { this.r.set(res?.data ?? res ?? null); this.cargando.set(false); },
      error: () => { this.cargando.set(false); this._toast.error('No se pudo cargar la rúbrica'); },
    });
  }

  /** El documento oficial, en el visor (se renderiza el Word, no se descarga a ciegas). */
  verWord(): void {
    this.abriendo.set(true);
    this._svc.rubricaDocumento$().subscribe({
      next: (blob) => {
        this.abriendo.set(false);
        previsualizarBlob(this._dialog, blob, 'Rúbrica oficial ' + (this.r()?.enfoqueLabel ?? ''));
      },
      error: () => { this.abriendo.set(false); this._toast.error('No se pudo abrir el documento'); },
    });
  }

  cerrar(): void { this._ref.close(); }
}
