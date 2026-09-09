import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { previsualizarBlob } from '@/app/shared/perfil-completo/preview.util';
import { CorreccionDialogComponent } from './correccion-dialog.component';
import { MiProyectoService } from '../services/mi-proyecto.service';
import { ExpedienteService } from '../../expediente/services/expediente.service';
import { ActividadItem, FASE_LABEL } from '../models/proyecto.model';

const COLUMNAS_KANBAN = [
  { v: 'PENDIENTE', l: 'Pendiente', dot: 'bg-slate-400' },
  { v: 'EN_CURSO', l: 'En curso', dot: 'bg-amber-500' },
  { v: 'HECHA', l: 'Hecha', dot: 'bg-emerald-500' },
];
const ORDEN_ESTADOS = ['PENDIENTE', 'EN_CURSO', 'HECHA'];

/**
 * "Ejecución de tesis" — Etapas 6 y 7: informe final, evaluaciones de avance del asesor y el
 * trámite del Jurado Informante.
 *
 * <p>Vivía dentro del editor "Mi proyecto" (2026-08-27: "regresas al paso 2, se presta a
 * confusiones... eso supuestamente está cerrado"), lo que mandaba al doctorando de vuelta a una
 * pestaña que ya se marca como terminada una vez que el asesor da su carta de opinión favorable.
 * Ahora es su propia pestaña, que aparece solo cuando el proyecto queda aprobado (Etapa 6).</p>
 */
@Component({
  selector: 'app-ejecucion-tesis',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Proceso de Tesis</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Ejecución de tesis</span>
          </div>
          <h1 class="page-title">Ejecución de la tesis</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <div class="mx-auto max-w-[1440px] space-y-3">
          <div class="rounded-xl border border-sky-200 bg-sky-50 px-4 py-3 text-[12.5px] text-sky-800 flex items-start gap-2.5">
            <mat-icon svgIcon="info" class="size-4.5 shrink-0 mt-px" />
            <span class="flex-1">
              Aquí ejecutas tu tesis según el plan de actividades aprobado: tu asesor evalúa tu
              avance periódicamente (verás sus notas más abajo). Cuando termines, sube tu
              <b>informe final en PDF</b> con el botón "Subir informe final" — tu asesor lo revisa
              y, si lo aprueba, podrás solicitar el Jurado Informante para que lo evalúe.
            </span>
          </div>
          @if (e(); as x) {
            @if (!x.proyectoAprobado) {
              <div class="rounded-xl bg-amber-50 border border-amber-200 px-4 py-3 text-[12.5px] text-amber-700 flex items-center gap-2.5">
                <mat-icon svgIcon="lock" class="size-5 shrink-0" />
                <span class="flex-1">Esta pestaña se habilita cuando tu proyecto quede aprobado tras la defensa.</span>
                <button mat-stroked-button class="!h-8 !text-[11.5px] shrink-0" (click)="irACierre()">
                  <mat-icon svgIcon="send" class="size-3.5 mr-1" /> Ir a Cierre y envío
                </button>
              </div>
            } @else {

            <section class="rounded-xl border border-slate-200 bg-white p-4 mb-3">
              <div class="flex items-center gap-2 mb-1">
                <mat-icon svgIcon="layout-grid" class="size-4 text-[#8C1D2E]" />
                <p class="text-[13px] font-bold text-slate-700">Plan de actividades</p>
                <span class="ml-auto text-[11px] font-bold" [ngClass]="planPct() >= 100 ? 'text-emerald-600' : 'text-[#8C1D2E]'">{{ planPct() }}% completado</span>
              </div>
              <div class="h-1.5 rounded bg-slate-100 overflow-hidden mb-3">
                <div class="h-full rounded" [ngClass]="planPct() >= 100 ? 'bg-emerald-500' : 'bg-[#8C1D2E]'" [style.width.%]="planPct()"></div>
              </div>
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                @for (col of columnas; track col.v) {
                  <div class="rounded-lg bg-slate-50 border border-slate-100 p-2.5 min-h-[84px]">
                    <p class="flex items-center gap-1.5 text-[10.5px] font-bold tracking-wide text-slate-400 mb-2">
                      <span class="size-2 rounded-full" [ngClass]="col.dot"></span>{{ col.l }}
                      <span class="ml-auto text-slate-400 font-mono">{{ actsPorEstado(col.v).length }}</span>
                    </p>
                    <div class="space-y-1.5">
                      @for (a of actsPorEstado(col.v); track a.id ?? a.nombre) {
                        <div class="rounded-md bg-white border border-slate-200 px-2.5 py-2 shadow-sm">
                          <p class="text-[12px] text-slate-700 leading-tight">{{ a.nombre }}</p>
                          @if (a.fase) { <span class="inline-block mt-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-slate-100 text-slate-500">{{ faseLabel(a.fase) }}</span> }
                          <div class="flex items-center gap-1 mt-1.5">
                            <button type="button" class="flex-1 h-6 rounded border border-slate-200 text-slate-400 hover:bg-slate-100 disabled:opacity-30 disabled:pointer-events-none flex items-center justify-center"
                                    [disabled]="!puedeMover(a, -1)" (click)="mover(a, -1)" title="Mover a la columna anterior">
                              <mat-icon svgIcon="chevron-left" class="size-3.5" />
                            </button>
                            <button type="button" class="flex-1 h-6 rounded border border-slate-200 text-slate-400 hover:bg-slate-100 disabled:opacity-30 disabled:pointer-events-none flex items-center justify-center"
                                    [disabled]="!puedeMover(a, 1)" (click)="mover(a, 1)" title="Mover a la columna siguiente">
                              <mat-icon svgIcon="chevron-right" class="size-3.5" />
                            </button>
                          </div>
                        </div>
                      }
                      @if (!actsPorEstado(col.v).length) { <p class="text-[11px] text-slate-300 italic">Sin actividades</p> }
                    </div>
                  </div>
                }
              </div>
              @if (!x.actividades?.length) {
                <p class="text-[11.5px] text-slate-400 mt-2">Tu plan de actividades aún no tiene actividades registradas.</p>
              }
            </section>

            <section class="rounded-xl border border-slate-200 bg-white p-4">
              <div class="flex items-center gap-2 mb-2">
                <mat-icon svgIcon="rocket" class="size-4 text-[#8C1D2E]" />
                <p class="text-[13px] font-bold text-slate-700">Informe final</p>
              </div>
              @if (x.informeFinalAprobado) {
                <div class="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-[12.5px] text-emerald-700 flex items-center gap-1.5">
                  <mat-icon svgIcon="badge-check" class="size-4 shrink-0" /> Tu asesor aprobó el informe final.
                </div>
                <!-- Etapa 7: solicitar Jurado Informante -->
                <div class="mt-2">
                  @if (!x.juradoInformanteSolicitado) {
                    <button mat-flat-button class="!h-8 !text-[12px] !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" (click)="solicitarJurado()">
                      <mat-icon svgIcon="send" class="size-3.5 mr-1" /> Solicitar Jurado Informante
                    </button>
                  } @else if (x.informeFinalRevisado) {
                    <div class="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-[12.5px] text-emerald-700 flex items-center gap-1.5">
                      <mat-icon svgIcon="badge-check" class="size-4 shrink-0" /> El Jurado Informante aprobó tu informe final.
                    </div>
                    @if (sustentacionHabilitada()) {
                      <div class="rounded-lg border border-[#8C1D2E]/25 bg-[#FDF6F7] px-3 py-2 mt-1.5 flex items-center gap-2 flex-wrap">
                        <mat-icon svgIcon="badge-check" class="size-4 shrink-0 text-[#8C1D2E]" />
                        <span class="text-[12.5px] text-[#8C1D2E] flex-1">Ya estás expedito para sustentar.</span>
                        <button mat-flat-button class="!h-8 !text-[12px] !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" (click)="irASustentacion()">
                          <mat-icon svgIcon="rocket" class="size-3.5 mr-1" /> Ir a Sustentación
                        </button>
                      </div>
                    } @else {
                      <div class="rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 mt-1.5 text-[12px] text-slate-500 flex items-center gap-1.5">
                        <mat-icon svgIcon="clock" class="size-3.5 shrink-0 text-amber-500" />
                        Secretaría está cerrando este expediente y emitiendo tu Dictamen de Expedito. Te avisaremos por aquí cuando puedas solicitar tu Jurado de Sustentación.
                      </div>
                    }
                  } @else {
                    <span class="text-[11px] text-slate-400">Jurado Informante solicitado — en evaluación.</span>
                  }
                  <!-- Evaluaciones del Jurado Informante -->
                  @if (x.evaluacionesJuradoInforme?.length) {
                    <div class="mt-2 space-y-1.5">
                      @for (jr of x.evaluacionesJuradoInforme; track jr.id) {
                        <div class="rounded-lg border border-slate-100 bg-white px-3 py-2">
                          <div class="flex items-center gap-2 flex-wrap">
                            <b class="text-[12px] text-slate-700">Jurado {{ jr.orden }}</b>
                            @if (jr.presidente) { <span class="text-[10px] text-[#8C1D2E] font-bold">Presidente</span> }
                            <span class="px-1.5 py-0.5 rounded text-[10px] font-bold" [ngClass]="revEstadoCls(jr.estado)">{{ revEstadoLabel(jr.estado) }}</span>
                            @if (jr.puntaje != null) { <span class="text-[10.5px] text-slate-400">{{ jr.puntaje }}/20</span> }
                          </div>
                          @if (jr.comentario) { <p class="text-[12px] text-slate-600 mt-1"><span class="font-semibold text-rose-500">Observación:</span> {{ jr.comentario }}</p> }
                          @if (jr.respuesta) { <p class="text-[12px] text-slate-700 mt-1 rounded-md bg-sky-50 border border-sky-100 px-2 py-1.5"><span class="font-semibold text-sky-600">Tu respuesta:</span> {{ jr.respuesta }}</p> }
                          @if (jr.estado === 'OBSERVADO' && !jr.respuesta) {
                            <button mat-flat-button class="!h-7 !text-[11px] mt-2 !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" (click)="responderJurado(jr)">
                              <mat-icon svgIcon="reply" class="size-3.5 mr-1" /> Levantar observación
                            </button>
                          }
                        </div>
                      }
                    </div>
                  }
                </div>
              } @else if (x.informeFinalSubido) {
                <!-- El envío es de una sola vez (se confirma antes de subirlo): no hay botón para
                     reemplazarlo, así el archivo que revisa el asesor no puede cambiar en silencio. -->
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-700">INFORME ENVIADO</span>
                  <span class="text-[12px] text-slate-500">Esperando la revisión de tu asesor.</span>
                  <button mat-stroked-button class="!h-8 !text-[12px] !text-[#8C1D2E] ml-auto" (click)="verInforme()">
                    <mat-icon svgIcon="file-search" class="size-3.5 mr-1" /> Ver informe enviado
                  </button>
                </div>
              } @else {
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-700">SIN ENVIAR</span>
                  <button mat-stroked-button class="!h-8 !text-[12px] !text-[#8C1D2E]" (click)="informeInput.click()">
                    <mat-icon svgIcon="upload" class="size-3.5 mr-1" /> Subir informe final (PDF)
                  </button>
                  <input #informeInput type="file" accept="application/pdf" class="hidden" (change)="subirInforme($event)" />
                </div>
              }
              @if (x.avances?.length) {
                <div class="mt-3">
                  <p class="text-[11px] font-bold text-slate-400 mb-1.5">EVALUACIONES DE AVANCE DEL ASESOR</p>
                  <div class="space-y-1.5">
                    @for (av of x.avances; track av.id) {
                      <div class="rounded-lg bg-slate-50 border border-slate-100 px-3 py-1.5 text-[12px]">
                        <b class="text-slate-700">{{ av.fechaEvaluacion | date:'dd/MM/yyyy' }}</b>
                        <span class="text-slate-400 ml-1">rúbrica {{ av.puntajeTotal }}/16 · plan {{ av.porcentajePlan }}%</span>
                        @if (av.comentario) { <p class="text-slate-600">{{ av.comentario }}</p> }
                      </div>
                    }
                  </div>
                </div>
              }
            </section>
            }
          } @else {
            <p class="text-sm text-slate-400">Cargando…</p>
          }
        </div>
      </div>
    </div>
  `,
})
export class EjecucionTesisComponent implements OnInit {
  private _svc = inject(MiProyectoService);
  private _expedienteSvc = inject(ExpedienteService);
  private _router = inject(Router);
  private _toast = inject(NotificationService);
  private _confirm = inject(ConfirmDialogService);
  private _dialog = inject(MatDialog);

  protected e = signal<any | null>(null);
  /** El Dictamen de Expedito ya está firmado: se puede solicitar el Jurado de Sustentación. */
  protected sustentacionHabilitada = signal(false);

  protected readonly columnas = COLUMNAS_KANBAN;
  protected faseLabel = (v?: string) => (v ? FASE_LABEL[v] ?? v : '');

  ngOnInit(): void {
    this.cargar();
    this._expedienteSvc.miExpediente$().subscribe({
      next: (res: any) => this.sustentacionHabilitada.set(!!(res?.data ?? res)?.sustentacionHabilitada),
      error: () => {},
    });
  }

  private cargar(): void {
    this._svc.editor$().subscribe({
      next: (res: any) => this.e.set(res?.data ?? res ?? null),
      error: () => this._toast.error('No se pudo cargar tu proyecto'),
    });
  }

  irACierre(): void {
    this._router.navigate(['/admin/mi-tesis/cierre']);
  }

  irASustentacion(): void {
    this._router.navigate(['/admin/mi-tesis/sustentacion']);
  }

  verInforme(): void {
    this._svc.descargarDocumento$('informe-final').subscribe({
      next: (blob) => previsualizarBlob(this._dialog, blob, 'Informe final'),
      error: () => this._toast.error('No se pudo abrir el documento'),
    });
  }

  solicitarJurado(): void {
    this._confirm.confirmSave({
      title: 'Solicitar Jurado Informante',
      message: 'Se enviará tu expediente (informe final aprobado + Turnitin) para que el Coordinador designe el Jurado Informante. ¿Continuar?',
    }).then(() => {
      this._svc.solicitarJuradoInformante$().subscribe({
        next: () => { this._toast.success('Jurado Informante solicitado'); this.cargar(); },
        error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo enviar la solicitud'),
      });
    }).catch(() => {});
  }

  responderJurado(jr: any): void {
    if (!jr.id) return;
    this._dialog.open(CorreccionDialogComponent, {
      width: '480px', autoFocus: true,
      data: { titulo: 'Levantar observación · Jurado ' + jr.orden },
    }).afterClosed().subscribe((texto: string | null) => {
      if (!texto) return;
      this._svc.responderJuradoInforme$(jr.id, texto).subscribe({
        next: () => { this._toast.success('Respuesta enviada al jurado'); this.cargar(); },
        error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo enviar la respuesta'),
      });
    });
  }

  subirInforme(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    // Antes se subía apenas se elegía el archivo: un doble clic o el archivo equivocado se
    // enviaba sin darle chance al alumno de darse cuenta. Ahora confirma con el nombre a la
    // vista — y como el envío no admite reemplazo después, esta es la única oportunidad de revisarlo.
    this._confirm.confirmSave({
      title: 'Confirmar informe final',
      message: `Vas a enviar "${file.name}" como tu informe final.`,
      details: ['Verifica que sea el archivo correcto: no podrás reemplazarlo después', 'Tu asesor lo revisará para aprobarlo'],
      confirmLabel: 'Enviar informe',
    }).then(() => {
      this._svc.subirInformeFinal$(file).subscribe({
        next: () => { this._toast.success('Informe final subido'); this.cargar(); },
        error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo subir el informe'),
      });
    }).catch(() => {});
  }

  actsPorEstado(estado: string): ActividadItem[] {
    return (this.e()?.actividades ?? []).filter((a: ActividadItem) => (a.estado ?? 'PENDIENTE') === estado);
  }

  planPct(): number {
    const acts: ActividadItem[] = this.e()?.actividades ?? [];
    if (!acts.length) return 0;
    const hechas = acts.filter((a) => a.estado === 'HECHA').length;
    return Math.round((hechas * 100) / acts.length);
  }

  puedeMover(a: ActividadItem, delta: number): boolean {
    const i = ORDEN_ESTADOS.indexOf(a.estado ?? 'PENDIENTE') + delta;
    return i >= 0 && i < ORDEN_ESTADOS.length;
  }

  mover(a: ActividadItem, delta: number): void {
    if (!a.id || !this.puedeMover(a, delta)) return;
    const next = ORDEN_ESTADOS[ORDEN_ESTADOS.indexOf(a.estado ?? 'PENDIENTE') + delta];
    this._svc.actualizarActividad$(a.id, { estado: next } as any).subscribe({
      next: () => this.cargar(),
      error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo actualizar la actividad'),
    });
  }

  revEstadoLabel(e?: string): string {
    return e === 'CONFORME' ? '✓ CONFORME' : e === 'OBSERVADO' ? '⚑ OBSERVADO' : 'POR EVALUAR';
  }
  revEstadoCls(e?: string): string {
    return e === 'CONFORME' ? 'bg-emerald-100 text-emerald-700'
      : e === 'OBSERVADO' ? 'bg-rose-100 text-rose-700'
      : 'bg-slate-100 text-slate-500';
  }
}
