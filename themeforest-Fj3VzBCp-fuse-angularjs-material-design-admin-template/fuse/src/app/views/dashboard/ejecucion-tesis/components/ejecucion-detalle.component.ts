import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { EjecucionTesisService } from '../services/ejecucion-tesis.service';

const CRITERIOS = [
  { key: 'ejecucion', titulo: 'Ejecución del plan', desc: 'Cumplimiento del cronograma y actividades.' },
  { key: 'datos', titulo: 'Recolección y calidad de datos', desc: 'Rigor del trabajo de campo y calidad de los datos.' },
  { key: 'analisis', titulo: 'Análisis preliminar', desc: 'Procesamiento y análisis de la información.' },
  { key: 'interpretacion', titulo: 'Interpretación de resultados', desc: 'Coherencia con los objetivos de la investigación.' },
];

/** Asesor · Etapa 6 — evaluación de avance + aprobación del informe final. */
@Component({
  selector: 'app-ejecucion-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <button mat-button class="!text-slate-500 !text-xs !px-2 mb-1" (click)="volver()">
            <mat-icon svgIcon="chevron-left" class="size-3.5" /> Volver a la bandeja
          </button>
          <h1 class="page-title">Ejecución de la tesis</h1>
        </div>
      </div>

      <div class="page-content p-6">
        @if (d(); as data) {
          <div class="mx-auto max-w-[1440px]">
            <!-- Cabecera + avance del plan -->
            <section class="form-card mb-4">
              <p class="text-[13px] font-semibold text-slate-800">{{ data.proyecto.estudianteNombre }} <span class="text-slate-400 font-normal">· {{ data.proyecto.codigoSistema }}</span></p>
              <p class="text-[13px] font-bold text-[#8C1D2E]">{{ data.proyecto.titulo }}</p>
              <div class="flex items-center gap-2 mt-2 max-w-[420px]">
                <span class="text-[11px] text-slate-400 whitespace-nowrap">Avance del plan</span>
                <div class="h-2 flex-1 rounded bg-slate-100 overflow-hidden">
                  <div class="h-full rounded" [ngClass]="data.planCompleto ? 'bg-emerald-500' : 'bg-[#8C1D2E]'" [style.width.%]="data.porcentajePlan"></div>
                </div>
                <span class="text-sm font-extrabold" [ngClass]="data.planCompleto ? 'text-emerald-600' : 'text-[#8C1D2E]'">{{ data.porcentajePlan }}%</span>
              </div>
            </section>

            <div class="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-4 items-start">
              <!-- Cronograma + historial de avances -->
              <div class="space-y-4 min-w-0">
                <section class="form-card">
                  <header class="form-card__head"><h2 class="form-card__title !text-[#8C1D2E]">Plan de actividades</h2></header>
                  <div class="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                    @for (col of columnas; track col.v) {
                      <div class="rounded-lg bg-slate-50 border border-slate-100 p-2.5 min-h-[64px]">
                        <p class="flex items-center gap-1.5 text-[10px] font-bold tracking-wide text-slate-400 mb-1.5">
                          <span class="size-2 rounded-full" [ngClass]="col.dot"></span>{{ col.l }}
                          <span class="ml-auto text-slate-400 font-mono">{{ actsPorEstado(data.proyecto.actividades, col.v).length }}</span>
                        </p>
                        <div class="space-y-1.5">
                          @for (a of actsPorEstado(data.proyecto.actividades, col.v); track a.id ?? a.nombre) {
                            <div class="rounded-md bg-white border border-slate-200 px-2 py-1.5">
                              <p class="text-[11.5px] text-slate-700 leading-tight">{{ a.nombre }}</p>
                            </div>
                          }
                          @if (!actsPorEstado(data.proyecto.actividades, col.v).length) { <p class="text-[11px] text-slate-300 italic">Sin actividades</p> }
                        </div>
                      </div>
                    }
                  </div>
                  @if (!(data.proyecto.actividades?.length)) { <p class="text-[12px] text-slate-400 mt-2">Sin actividades.</p> }
                </section>

                <section class="form-card">
                  <header class="form-card__head"><h2 class="form-card__title">Historial de avances</h2></header>
                  @if (data.avances?.length) {
                    <div class="space-y-2">
                      @for (av of data.avances; track av.id) {
                        <div class="rounded-lg border border-slate-100 p-3">
                          <div class="flex items-center gap-2 flex-wrap">
                            <b class="text-[12px] text-slate-700">{{ av.fechaEvaluacion | date:'dd/MM/yyyy' }}</b>
                            <span class="text-[11px] text-slate-400">Rúbrica {{ av.puntajeTotal }}/{{ data.puntajeMaximo }}</span>
                            <span class="text-[11px] text-slate-400">· Plan {{ av.porcentajePlan }}%</span>
                          </div>
                          @if (av.comentario) { <p class="text-[12px] text-slate-600 mt-1">{{ av.comentario }}</p> }
                        </div>
                      }
                    </div>
                  } @else { <p class="text-[12px] text-slate-400">Aún no has registrado evaluaciones de avance.</p> }
                </section>
              </div>

              <!-- Rúbrica de avance + informe final -->
              <aside class="lg:sticky lg:top-4 self-start space-y-4">
                <section class="form-card">
                  <header class="form-card__head"><h2 class="form-card__title">Evaluar avance</h2></header>
                  <p class="text-[11px] text-slate-400 mb-3">Escala: Excelente (4) · Bueno (3) · Básico (2) · En proceso (1)</p>
                  <div class="space-y-3">
                    @for (cr of criterios; track cr.key) {
                      <div>
                        <p class="text-[12.5px] font-semibold text-slate-700">{{ cr.titulo }}</p>
                        <p class="text-[10.5px] text-slate-400 leading-tight mb-1.5">{{ cr.desc }}</p>
                        <div class="flex gap-1">
                          @for (n of [4,3,2,1]; track n) {
                            <button type="button" class="flex-1 h-8 rounded-lg text-[11px] font-bold border transition"
                                    [ngClass]="puntajes()[cr.key] === n ? 'bg-[#8C1D2E] text-white border-[#8C1D2E]' : 'text-slate-500 border-slate-200 hover:bg-slate-50'"
                                    (click)="set(cr.key, n)">{{ n }}</button>
                          }
                        </div>
                      </div>
                    }
                  </div>
                  <textarea class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm mt-3 focus:outline-none focus:border-[#8C1D2E]"
                            rows="3" [(ngModel)]="comentario" placeholder="Comentario del avance…"></textarea>
                  <button mat-flat-button class="!h-9 !text-[12px] !w-full mt-2 !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" [disabled]="!completa() || guardando()" (click)="registrar()">
                    <mat-icon svgIcon="plus" class="size-3.5 mr-1" /> Registrar avance
                  </button>
                  @if (!completa()) { <p class="text-[11px] text-amber-600 mt-1.5">Califica los 4 criterios.</p> }
                </section>

                <section class="form-card">
                  <header class="form-card__head"><h2 class="form-card__title">Informe final</h2></header>
                  @if (data.informeFinalAprobado) {
                    <div class="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-[12px] text-emerald-700 flex items-center gap-1.5">
                      <mat-icon svgIcon="badge-check" class="size-4 shrink-0" /> Informe final aprobado.
                    </div>
                  } @else {
                    <p class="text-[12px] text-slate-500 mb-2">
                      Requisitos: plan 100% ejecutado <b [ngClass]="data.planCompleto ? 'text-emerald-600' : 'text-amber-600'">({{ data.porcentajePlan }}%)</b>
                      e informe final subido <b [ngClass]="data.informeFinalSubido ? 'text-emerald-600' : 'text-amber-600'">({{ data.informeFinalSubido ? 'sí' : 'no' }})</b>.
                    </p>
                    <button mat-flat-button class="!h-9 !text-[12px] !w-full !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                            [disabled]="!data.planCompleto || !data.informeFinalSubido || guardando()" (click)="aprobar()">
                      <mat-icon svgIcon="badge-check" class="size-3.5 mr-1" /> Aprobar informe final
                    </button>
                  }
                </section>
              </aside>
            </div>
          </div>
        } @else {
          <p class="text-sm text-slate-400">Cargando…</p>
        }
      </div>
    </div>
  `,
})
export class EjecucionDetalleComponent implements OnInit {
  private _route = inject(ActivatedRoute);
  private _router = inject(Router);
  private _svc = inject(EjecucionTesisService);
  private _toast = inject(NotificationService);
  private _confirm = inject(ConfirmDialogService);

  protected readonly criterios = CRITERIOS;
  protected d = signal<any | null>(null);
  protected puntajes = signal<Record<string, number>>({});
  protected comentario = '';
  protected guardando = signal(false);
  private tesisId = '';

  protected completa = computed(() => CRITERIOS.every((c) => this.puntajes()[c.key] > 0));

  ngOnInit(): void {
    this.tesisId = this._route.snapshot.paramMap.get('tesisId') ?? '';
    this.cargar();
  }

  cargar(): void {
    this._svc.detalle$(this.tesisId).subscribe({
      next: (res) => this.d.set(res?.data ?? res),
      error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo cargar'),
    });
  }

  set(key: string, n: number): void { this.puntajes.set({ ...this.puntajes(), [key]: n }); }

  protected readonly columnas = [
    { v: 'PENDIENTE', l: 'Pendiente', dot: 'bg-slate-400' },
    { v: 'EN_CURSO', l: 'En curso', dot: 'bg-amber-500' },
    { v: 'HECHA', l: 'Hecha', dot: 'bg-emerald-500' },
  ];

  actsPorEstado(acts: any[] | undefined, estado: string): any[] {
    return (acts ?? []).filter((a) => (a.estado ?? 'PENDIENTE') === estado);
  }

  registrar(): void {
    if (!this.completa() || this.guardando()) return;
    this.guardando.set(true);
    this._svc.registrarAvance$(this.tesisId, this.puntajes(), this.comentario.trim()).subscribe({
      next: () => { this._toast.success('Avance registrado'); this.puntajes.set({}); this.comentario = ''; this.guardando.set(false); this.cargar(); },
      error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo registrar'); },
    });
  }

  aprobar(): void {
    this._confirm.confirmSave({
      title: 'Aprobar informe final',
      message: 'Emitirás la carta de aprobación del informe final. El estudiante podrá continuar con el trámite de Jurado Informante. ¿Continuar?',
    }).then(() => {
      this.guardando.set(true);
      this._svc.aprobarInforme$(this.tesisId).subscribe({
        next: () => { this._toast.success('Informe final aprobado'); this.guardando.set(false); this.cargar(); },
        error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo aprobar'); },
      });
    }).catch(() => {});
  }

  volver(): void { this._router.navigate(['/admin/ejecucion-tesis']); }
}
