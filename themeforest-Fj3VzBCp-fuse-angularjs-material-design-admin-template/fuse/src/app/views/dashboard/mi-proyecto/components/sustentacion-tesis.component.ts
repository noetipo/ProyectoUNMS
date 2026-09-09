import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { MiProyectoService } from '../services/mi-proyecto.service';

/**
 * "Sustentación" — Etapa 8, la última del proceso: el estudiante solicita su Jurado de
 * Sustentación (ya con el Dictamen de Expedito), sigue el trámite hasta que Secretaría programa
 * el acto y, tras la sustentación, ve el Acta y la conclusión de su tesis.
 *
 * <p>Pestaña propia (2026-08-28: "nada debe retroceder") para no reabrir "Ejecución de tesis",
 * que ya se dio por cerrada una vez que el Jurado Informante aprueba el informe final.</p>
 */
@Component({
  selector: 'app-sustentacion-tesis',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Proceso de Tesis</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Sustentación</span>
          </div>
          <h1 class="page-title">Sustentación de la tesis</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <div class="mx-auto max-w-[1440px] space-y-3">
          @if (e(); as x) {

            @if (!x.sustentacionSolicitada) {
              <div class="rounded-xl border border-sky-200 bg-sky-50 px-4 py-3 text-[12.5px] text-sky-800 flex items-start gap-2.5 mb-1">
                <mat-icon svgIcon="info" class="size-4.5 shrink-0 mt-px" />
                <span class="flex-1">
                  Este es el último trámite de tu proceso de titulación. Al solicitarlo, el
                  Coordinador designará a tu Jurado de Sustentación y Secretaría coordinará
                  modalidad, lugar y fecha del acto.
                </span>
              </div>
              <section class="rounded-xl border border-slate-200 bg-white p-4 text-center">
                <mat-icon svgIcon="rocket" class="!size-10 text-[#8C1D2E] mb-2" />
                <p class="text-[13px] font-bold text-slate-700 mb-1">Solicita tu Jurado de Sustentación</p>
                <p class="text-[12px] text-slate-500 mb-3 max-w-md mx-auto">
                  Ya estás expedito para sustentar tu tesis. Al confirmar, se enviará tu solicitud
                  junto con tu informe final aprobado y tu Dictamen de Expedito.
                </p>
                <button mat-flat-button class="!h-9 !text-[12.5px] !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" (click)="solicitar()">
                  <mat-icon svgIcon="send" class="size-3.5 mr-1" /> Solicitar Jurado de Sustentación
                </button>
              </section>
            } @else {

              <section class="rounded-xl border border-slate-200 bg-white p-4">
                <div class="flex items-center gap-2 mb-3">
                  <mat-icon svgIcon="rocket" class="size-4 text-[#8C1D2E]" />
                  <p class="text-[13px] font-bold text-slate-700">Trámite de sustentación</p>
                </div>

                <!-- Mini línea de tiempo -->
                <div class="flex items-center flex-wrap gap-y-2 mb-4">
                  @for (paso of pasos(); track paso.label; let last = $last) {
                    <div class="flex items-center gap-1.5">
                      <span class="size-5 rounded-full flex items-center justify-center text-[9px] font-bold shrink-0"
                            [ngClass]="paso.estado === 'hecho' ? 'bg-emerald-500 text-white'
                                     : paso.estado === 'actual' ? 'bg-[#8C1D2E] text-white' : 'bg-slate-100 text-slate-400 border border-dashed border-slate-300'">
                        @if (paso.estado === 'hecho') { <mat-icon svgIcon="check" class="size-3" /> } @else { {{ paso.num }} }
                      </span>
                      <span class="text-[11px] font-semibold" [class]="paso.estado === 'actual' ? 'text-[#8C1D2E]' : 'text-slate-500'">{{ paso.label }}</span>
                    </div>
                    @if (!last) { <div class="w-5 h-px bg-slate-200 mx-1.5"></div> }
                  }
                </div>

                @if (x.juradoSustentacion?.length) {
                  <p class="text-[11px] font-bold text-slate-400 mb-1.5">JURADO DE SUSTENTACIÓN</p>
                  <div class="space-y-1.5 mb-3">
                    @for (jr of x.juradoSustentacion; track jr.docenteId) {
                      <div class="flex items-center gap-2 rounded-lg bg-slate-50 border border-slate-100 px-3 py-1.5 text-[12px]">
                        <span class="size-5 rounded-full bg-slate-200 text-slate-600 text-[9px] font-bold flex items-center justify-center shrink-0">
                          {{ iniciales(jr.docenteNombre) }}
                        </span>
                        <span class="text-slate-700 flex-1">{{ jr.docenteNombre }}</span>
                        @if (jr.rol === 'PRESIDENTE') { <span class="text-[10px] text-[#8C1D2E] font-bold">PRESIDENTE</span> }
                      </div>
                    }
                  </div>
                }

                @if (x.sustentacionProgramada) {
                  <div class="rounded-lg border border-[#8C1D2E]/25 bg-[#FDF6F7] px-3 py-2.5">
                    <p class="text-[13px] font-bold text-[#8C1D2E]">
                      {{ x.fechaSustentacion | date:'EEEE d \\'de\\' MMMM \\'de\\' y' }}@if (x.horaSustentacion) { · {{ x.horaSustentacion }} }
                    </p>
                    <p class="text-[11.5px] text-slate-600 mt-0.5">
                      {{ x.modalidadSustentacionLabel }}
                      @if (x.lugarSustentacion) { · {{ x.lugarSustentacion }} }
                    </p>
                    @if (x.enlaceSustentacion) { <p class="text-[11.5px] text-slate-500 mt-0.5">Enlace: {{ x.enlaceSustentacion }}</p> }
                  </div>
                } @else if (x.actaSustentacionSubida) {
                  <div class="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2.5 text-[12px] text-rose-700 flex items-start gap-2">
                    <mat-icon svgIcon="circle-alert" class="size-4 shrink-0 mt-px" />
                    <span>
                      Tu sustentación del {{ x.fechaActaSustentacion | date:'dd/MM/yyyy' }} fue
                      <b>{{ x.resultadoSustentacionLabel?.toLowerCase() }}</b>. Secretaría coordinará contigo y tu Jurado un nuevo acto.
                    </span>
                  </div>
                } @else if (x.juradoSustentacion?.length) {
                  <div class="rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 text-[12px] text-slate-500 flex items-center gap-1.5">
                    <mat-icon svgIcon="clock" class="size-3.5 shrink-0 text-amber-500" />
                    Tu Jurado ya fue designado. Secretaría está elaborando el dictamen y coordinando fecha, hora y lugar del acto.
                  </div>
                } @else if (x.expedienteSustentacionRecibido) {
                  <div class="rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 text-[12px] text-slate-500 flex items-center gap-1.5">
                    <mat-icon svgIcon="clock" class="size-3.5 shrink-0 text-amber-500" />
                    Tu expediente fue recibido. Espera a que el Coordinador designe a tu Jurado de Sustentación.
                  </div>
                } @else {
                  <div class="rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 text-[12px] text-slate-500 flex items-center gap-1.5">
                    <mat-icon svgIcon="clock" class="size-3.5 shrink-0 text-amber-500" />
                    Tu solicitud fue enviada. Espera a que Secretaría la recepcione y la comunique al Coordinador.
                  </div>
                }
              </section>

              @if (x.tesisConcluida) {
                <section class="rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-center">
                  <mat-icon svgIcon="badge-check" class="!size-12 text-emerald-600 mb-2" />
                  <p class="text-[15px] font-bold text-emerald-800 mb-1">¡Tu proceso de titulación ha concluido!</p>
                  <p class="text-[12.5px] text-emerald-700">
                    Sustentación {{ x.resultadoSustentacionLabel?.toLowerCase() ?? 'registrada' }}
                    @if (x.fechaActaSustentacion) { el {{ x.fechaActaSustentacion | date:'dd/MM/yyyy' }} }.
                    Secretaría archivó tu Acta de sustentación.
                  </p>
                </section>
              }
            }
          } @else {
            <p class="text-sm text-slate-400">Cargando…</p>
          }
        </div>
      </div>
    </div>
  `,
})
export class SustentacionTesisComponent implements OnInit {
  private _svc = inject(MiProyectoService);
  private _router = inject(Router);
  private _toast = inject(NotificationService);
  private _confirm = inject(ConfirmDialogService);

  protected e = signal<any | null>(null);

  protected pasos = computed(() => {
    const x = this.e();
    const num = (hecho: boolean, actualCond: boolean, n: number) =>
      ({ num: n, estado: hecho ? 'hecho' : actualCond ? 'actual' : 'pendiente' });
    if (!x) return [];
    const solicitada = !!x.sustentacionSolicitada;
    const recibida = !!x.expedienteSustentacionRecibido;
    const designado = !!x.juradoSustentacion?.length;
    const programada = !!x.sustentacionProgramada;
    const concluida = !!x.tesisConcluida;
    return [
      { label: 'Solicitud enviada', ...num(solicitada, !solicitada, 1) },
      { label: 'Recepcionada', ...num(recibida, solicitada && !recibida, 2) },
      { label: 'Jurado designado', ...num(designado, recibida && !designado, 3) },
      { label: 'Programada', ...num(programada, designado && !programada, 4) },
      { label: 'Concluida', ...num(concluida, programada && !concluida, 5) },
    ];
  });

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this._svc.editor$().subscribe({
      next: (res: any) => this.e.set(res?.data ?? res ?? null),
      error: () => this._toast.error('No se pudo cargar tu trámite de sustentación'),
    });
  }

  solicitar(): void {
    this._confirm.confirmSave({
      title: 'Solicitar Jurado de Sustentación',
      message: 'Se enviará tu solicitud junto con tu informe final aprobado y tu Dictamen de Expedito, para que el Coordinador designe tu Jurado. ¿Continuar?',
    }).then(() => {
      this._svc.solicitarSustentacion$().subscribe({
        next: () => { this._toast.success('Jurado de Sustentación solicitado'); this.cargar(); },
        error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo enviar la solicitud'),
      });
    }).catch(() => {});
  }

  iniciales(nombre?: string): string {
    if (!nombre) return '—';
    const p = nombre.replace(/^(Dr\.|Dra\.|Mg\.|Lic\.)\s*/i, '').trim().split(/\s+/).filter(Boolean);
    return ((p[0]?.[0] ?? '') + (p[1]?.[0] ?? '')).toUpperCase() || '—';
  }
}
