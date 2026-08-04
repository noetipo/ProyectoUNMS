import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { RevisionProyectoService } from '../services/revision-proyecto.service';
import { ObservacionDialogComponent } from './observacion-dialog.component';
import { ActividadItem, FASE_LABEL, PROY_DEF, ProyectoEditor, RevisionItem, SeccionDef, chipRevision, tipoDot, tipoVerbo, ganttMeses, ganttColFecha, rangoFechas } from '../../mi-proyecto/models/proyecto.model';

/** Detalle del proyecto en modo revisión del ASESOR (por pasos): observar y aprobar por ítem. */
@Component({
  selector: 'app-revision-proyecto',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <button mat-button class="!text-slate-500 !text-xs !px-2 mb-1" (click)="volver()">
            <mat-icon svgIcon="chevron-left" class="size-3.5" /> Volver a la bandeja
          </button>
          <h1 class="page-title">Revisión del proyecto</h1>
        </div>
        @if (p(); as e) {
          <button mat-flat-button color="primary" class="ml-auto !h-9 !text-sm"
                  [disabled]="!todosConformes()" (click)="emitirCarta()">
            <mat-icon svgIcon="badge-check" class="size-3.5 mr-1" /> {{ e.cartaAsesor ? '✓ Carta emitida' : 'Emitir carta de opinión favorable' }}
          </button>
        }
      </div>

      <div class="page-content p-6">
        @if (p(); as e) {
          <div class="mx-auto max-w-[1440px] space-y-4">
            <!-- Cabecera -->
            <section class="form-card">
              <p class="text-[13px] font-semibold text-slate-800">{{ e.estudianteNombre }} <span class="text-slate-400 font-normal">· {{ e.codigoSistema }}</span></p>
              <p class="text-[12.5px] text-slate-500">{{ e.programaNombre }}</p>
              <div class="flex flex-wrap items-center gap-3 mt-2">
                <span class="px-2.5 py-1 rounded-lg text-[11px] font-bold bg-[#FDF6F7] text-[#8C1D2E] border border-[#8C1D2E]/20">
                  {{ e.enfoque === 'CUALITATIVO' ? 'Cualitativo' : 'Cuantitativo / mixto' }}
                </span>
                <div class="flex items-center gap-2 flex-1 min-w-[180px] max-w-[340px]">
                  <span class="text-[11px] text-slate-400 whitespace-nowrap">Avance del estudiante</span>
                  <div class="h-1.5 flex-1 rounded bg-slate-100 overflow-hidden">
                    <div class="h-full rounded" [ngClass]="e.avancePct >= 100 ? 'bg-emerald-500' : 'bg-[#8C1D2E]'" [style.width.%]="e.avancePct"></div>
                  </div>
                  <span class="text-sm font-extrabold" [ngClass]="e.avancePct >= 100 ? 'text-emerald-600' : 'text-[#8C1D2E]'">{{ e.avancePct }}%</span>
                </div>
              </div>
              @if (!e.cartaAsesor && !todosConformes()) { <p class="text-[11px] text-amber-600 mt-1">Da conformidad a todos los bloques del proyecto para poder emitir la carta.</p> }
            </section>

            @if (soloLectura()) {
              <div class="rounded-xl border border-sky-200 bg-sky-50 px-4 py-2.5 flex items-center gap-2 text-[12.5px] text-sky-800">
                <mat-icon svgIcon="eye" class="size-4 shrink-0" />
                <span><b>Eres co-asesor de esta tesis.</b> Tu acceso es de consulta: puedes ver el
                  proyecto y el historial de observaciones, pero la revisión la realiza el asesor.</span>
              </div>
            } @else if (cerrada()) {
              <div class="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2.5 flex items-center gap-2 text-[12.5px] text-emerald-700">
                <mat-icon svgIcon="badge-check" class="size-4 shrink-0" />
                @if (p()?.cartaAsesor) {
                  <span><b>Carta de opinión favorable emitida.</b> La revisión quedó cerrada: ya no puedes observar ni dar conformidad a los ítems.</span>
                } @else {
                  <span><b>El proyecto ya avanzó de etapa.</b> La revisión del asesor quedó cerrada: ya no puedes observar ni dar conformidad.</span>
                }
              </div>
            }

            <!-- Correcciones del estudiante por verificar (consolidado, de todas las secciones) -->
            @if (correcciones().length) {
              <section class="rounded-xl border border-sky-200 bg-sky-50 p-4">
                <p class="text-[12.5px] font-bold text-sky-700 mb-2.5 flex items-center gap-1.5">
                  <mat-icon svgIcon="rotate-ccw" class="size-4" /> El estudiante corrigió {{ correcciones().length }} ítem(s) · verifica y da conformidad
                </p>
                <div class="space-y-2.5">
                  @for (c of correcciones(); track c.campo) {
                    <div class="rounded-lg bg-white border border-sky-100 p-3">
                      <div class="flex items-center gap-2 flex-wrap mb-1">
                        <span class="text-[10px] font-bold text-slate-400">{{ c.seccion }}</span>
                        <b class="text-[12px] text-slate-700">{{ c.label }}</b>
                        <span class="ml-auto px-1.5 py-0.5 rounded text-[9px] font-bold" [ngClass]="chip(c.campo).cls">{{ chip(c.campo).label }}</span>
                      </div>
                      @if (c.observacion) {
                        <p class="text-[11.5px] text-slate-600"><span class="font-semibold text-rose-500">Observaste:</span> {{ c.observacion }}</p>
                      }
                      @if (c.correccion) {
                        <p class="text-[12px] text-slate-700 mt-1 rounded-md bg-sky-50 border border-sky-100 px-2 py-1.5 whitespace-pre-wrap"><span class="font-semibold text-sky-600">Corrigió a:</span> {{ c.correccion }}</p>
                      }
                      @if (!cerrada()) {
                        <div class="mt-2 flex gap-2">
                          <button type="button" class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold text-[#8C1D2E] bg-[#FDF6F7] border border-[#8C1D2E]/20 hover:bg-[#f7e9ec] transition" (click)="irACampo(c.campo)"><mat-icon svgIcon="arrow-right" class="size-3.5" /> Ver en su sección</button>
                          <button type="button" class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold text-white bg-emerald-600 hover:bg-emerald-700 shadow-sm transition" (click)="conforme(c.campo)"><mat-icon svgIcon="check" class="size-3.5" /> Dar conformidad</button>
                          <button type="button" class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold text-rose-600 bg-rose-50 border border-rose-200 hover:bg-rose-100 transition" (click)="observar(c.campo, c.label)"><mat-icon svgIcon="flag" class="size-3.5" /> Volver a observar</button>
                        </div>
                      }
                    </div>
                  }
                </div>
              </section>
            }

            <!-- Índice lateral de secciones + contenido -->
            <div class="grid grid-cols-1 lg:grid-cols-[214px_1fr] gap-4 items-start">
              <aside class="lg:sticky lg:top-4 self-start rounded-xl border border-slate-200 bg-white p-2">
                <div class="text-[10px] font-extrabold tracking-wider text-slate-400 px-2.5 pt-1.5 pb-2">SECCIONES DEL PROYECTO</div>
                @for (sec of secciones(); track sec.id; let i = $index) {
                  <button type="button" class="w-full flex items-center gap-2 px-2.5 py-2 rounded-lg text-left transition"
                          [ngClass]="paso() === i ? 'bg-[#8C1D2E] text-white' : 'text-slate-600 hover:bg-slate-50'"
                          (click)="irA(i)">
                    <span class="size-[7px] rounded-full shrink-0" [style.background]="paso() === i ? '#ffffff' : dotColorSec(sec)"></span>
                    <span class="flex-1 text-[11.5px] font-semibold truncate">{{ sec.titulo }}</span>
                    @if (secEstado(sec) === 'conforme') {
                      <mat-icon svgIcon="check" class="size-3.5" [ngClass]="paso() === i ? 'text-white' : 'text-emerald-600'" />
                    }
                  </button>
                }
                <div class="mx-1.5 mt-2 mb-1 px-3 py-2 rounded-lg bg-slate-50 text-[10.5px] text-slate-500 leading-relaxed">
                  🟢 sección conforme · 🟠 con observaciones · ⚪ sin revisar. Observa o da conformidad por ítem.
                </div>
              </aside>

              <div class="space-y-4 min-w-0">

            <!-- Sección del paso actual -->
            @if (secActual(); as sec) {
              <section class="form-card">
                <header class="form-card__head"><h2 class="form-card__title !text-[#8C1D2E]">{{ sec.titulo }}</h2></header>
                <div class="space-y-4">
                  @for (c of sec.campos; track c.k) {
                    <!-- id + scroll-mt: "Ver en su sección" salta directo al ítem, no solo a la sección. -->
                    <div [id]="'campo-' + c.k" class="scroll-mt-4 rounded-lg">
                      <div class="flex items-center gap-2 mb-1">
                        <label class="form-label !mb-0">{{ c.l }}</label>
                        <span class="ml-auto px-1.5 py-0.5 rounded text-[10px] font-bold" [ngClass]="chip(c.k).cls">{{ chip(c.k).label }}</span>
                      </div>
                      <div class="rounded-lg bg-slate-50 border border-slate-100 px-3 py-2 text-sm text-slate-700 whitespace-pre-wrap min-h-[38px]">{{ e.campos[c.k] || '—' }}</div>
                      @if (rev(c.k)?.eventos?.length) {
                        <div class="mt-1.5">
                          <button type="button" class="flex items-center gap-1 text-[11px] font-semibold text-[#8C1D2E] hover:underline" (click)="toggleHist(c.k)">
                            <mat-icon [svgIcon]="histOpen(c.k) ? 'chevron-down' : 'chevron-right'" class="size-3.5" /> {{ histOpen(c.k) ? 'Ocultar' : 'Ver' }} historial ({{ rev(c.k)!.eventos.length }})
                          </button>
                          @if (histOpen(c.k)) {
                            <div class="mt-1.5 rounded-lg bg-slate-50 border border-slate-100 p-3">
                              <ol class="ml-1.5 pl-4 border-l border-slate-200 space-y-3">
                                @for (ev of eventos(c.k); track $index) {
                                  <li class="relative">
                                    <span class="absolute -left-[21px] top-[3px] size-2.5 rounded-full ring-2 ring-slate-50" [ngClass]="tipoDot(ev.tipo)"></span>
                                    <div class="flex items-baseline gap-1.5 flex-wrap leading-tight">
                                      <b class="text-[11.5px] text-slate-700">{{ ev.autor }}</b>
                                      <span class="text-[11px] text-slate-500">{{ tipoVerbo(ev.tipo) }}</span>
                                      <span class="text-[10px] text-slate-400">· {{ ev.fecha }}</span>
                                    </div>
                                    @if (ev.texto) { <p class="text-[12px] mt-1 whitespace-pre-wrap" [ngClass]="ev.tipo === 'CORRECCIÓN' ? 'text-slate-700 rounded-md bg-sky-50 border border-sky-100 px-2 py-1.5' : 'text-slate-600'">{{ ev.texto }}</p> }
                                  </li>
                                }
                              </ol>
                            </div>
                          }
                        </div>
                      }
                      @if (!cerrada()) {
                        <div class="mt-1.5 flex gap-2 items-center">
                          <button type="button" class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold text-rose-600 bg-rose-50 border border-rose-200 hover:bg-rose-100 transition" (click)="observar(c.k, c.l)">
                            <mat-icon svgIcon="flag" class="size-3.5" /> {{ observado(c.k) ? 'Observar de nuevo' : 'Observar ítem' }}
                          </button>
                          @if (rev(c.k)?.estado === 'CONFORME') {
                            <span class="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-600"><mat-icon svgIcon="check" class="size-3.5" /> Conforme</span>
                          }
                        </div>
                      }
                    </div>
                  }
                </div>

                @if (sec.wObj && e.objetivos.length) {
                  <div class="mt-4 pt-3 border-t border-slate-100">
                    <label class="form-label">Objetivos específicos</label>
                    <ol class="list-decimal ml-5 text-[13px] text-slate-700 space-y-0.5">
                      @for (o of e.objetivos; track o.id ?? $index) { <li>{{ o.texto }}</li> }
                    </ol>
                  </div>
                }
                @if (sec.wPlan) {
                  <div class="mt-4 pt-3 border-t border-slate-100">
                    <div class="flex items-center gap-2 mb-1">
                      <label class="form-label !mb-0">Plan de actividades y presupuesto</label>
                      <span class="ml-auto px-1.5 py-0.5 rounded text-[10px] font-bold" [ngClass]="chip('plan').cls">{{ chip('plan').label }}</span>
                    </div>
                    <p class="text-[11px] text-slate-500 mb-2">{{ e.actividades.length }} actividades · {{ actHechas() }} hechas · {{ actEnCurso() }} en curso · Presupuesto S/ {{ e.presupuestoTotal | number:'1.2-2' }} · {{ e.financiamiento }} · Plan {{ e.planPublicado ? 'publicado' : 'no publicado' }}</p>

                    <!-- Cronograma (Gantt de 18 meses, solo lectura) -->
                    <div class="overflow-x-auto mb-2">
                      <div class="min-w-[600px]">
                        <div class="grid grid-cols-[minmax(140px,32%)_1fr_82px] items-end gap-2 pb-1 border-b border-slate-100">
                          <span class="text-[10px] font-bold tracking-wide text-slate-400">ACTIVIDAD · FASE</span>
                          <div class="grid" [style.grid-template-columns]="'repeat(' + (meses().length || 1) + ', 1fr)'">
                            @for (m of meses(); track m.idx) { <span class="text-[8px] text-center text-slate-300 font-mono">{{ m.label }}</span> }
                          </div>
                          <span class="text-[10px] font-bold tracking-wide text-slate-400 text-center">ESTADO</span>
                        </div>
                        @for (a of e.actividades; track a.id ?? $index) {
                          <div class="grid grid-cols-[minmax(140px,32%)_1fr_82px] items-center gap-2 py-1.5 border-b border-slate-50">
                            <div>
                              <p class="text-[12px] text-slate-700 leading-tight">{{ a.nombre }}</p>
                              <div class="flex items-center gap-2 mt-0.5">
                                <span class="inline-block px-1.5 py-0.5 rounded text-[9px] font-bold" [style.background]="faseBg(a.fase)" [style.color]="faseFg(a.fase)">{{ faseLabel(a.fase) }}</span>
                                @if (rangoFechas(a); as r) { <span class="text-[10px] text-slate-400">{{ r }}</span> }
                              </div>
                            </div>
                            <div class="grid h-2.5 rounded bg-slate-100" [style.grid-template-columns]="'repeat(' + (meses().length || 1) + ', 1fr)'">
                              @if (ganttCol(a); as gc) { <div class="h-full rounded" [style.grid-column]="gc" [style.background]="faseFg(a.fase)"></div> }
                            </div>
                            <span class="px-2 py-0.5 rounded-md text-[10px] font-bold text-center" [ngClass]="estadoCls(a.estado)">{{ estadoLabel(a.estado) }}</span>
                          </div>
                        }
                        @if (!e.actividades.length) { <p class="text-[12px] text-slate-400 py-2">El estudiante aún no ha registrado actividades.</p> }
                      </div>
                    </div>
                    @if (rev('plan')?.eventos?.length) {
                      <div class="mt-1.5">
                        <button type="button" class="flex items-center gap-1 text-[11px] font-semibold text-[#8C1D2E] hover:underline" (click)="toggleHist('plan')">
                          <mat-icon [svgIcon]="histOpen('plan') ? 'chevron-down' : 'chevron-right'" class="size-3.5" /> {{ histOpen('plan') ? 'Ocultar' : 'Ver' }} historial ({{ rev('plan')!.eventos.length }})
                        </button>
                        @if (histOpen('plan')) {
                          <div class="mt-1.5 rounded-lg bg-slate-50 border border-slate-100 p-3">
                            <ol class="ml-1.5 pl-4 border-l border-slate-200 space-y-3">
                              @for (ev of eventos('plan'); track $index) {
                                <li class="relative">
                                  <span class="absolute -left-[21px] top-[3px] size-2.5 rounded-full ring-2 ring-slate-50" [ngClass]="tipoDot(ev.tipo)"></span>
                                  <div class="flex items-baseline gap-1.5 flex-wrap leading-tight">
                                    <b class="text-[11.5px] text-slate-700">{{ ev.autor }}</b>
                                    <span class="text-[11px] text-slate-500">{{ tipoVerbo(ev.tipo) }}</span>
                                    <span class="text-[10px] text-slate-400">· {{ ev.fecha }}</span>
                                  </div>
                                  @if (ev.texto) { <p class="text-[12px] mt-1 whitespace-pre-wrap" [ngClass]="ev.tipo === 'CORRECCIÓN' ? 'text-slate-700 rounded-md bg-sky-50 border border-sky-100 px-2 py-1.5' : 'text-slate-600'">{{ ev.texto }}</p> }
                                </li>
                              }
                            </ol>
                          </div>
                        }
                      </div>
                    }
                    @if (!cerrada()) {
                      <div class="mt-1.5 flex gap-2 items-center">
                        <button type="button" class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold text-rose-600 bg-rose-50 border border-rose-200 hover:bg-rose-100 transition" (click)="observar('plan', 'Plan de actividades')">
                          <mat-icon svgIcon="flag" class="size-3.5" /> {{ observado('plan') ? 'Observar de nuevo' : 'Observar ítem' }}
                        </button>
                        @if (rev('plan')?.estado === 'CONFORME') {
                          <span class="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-600"><mat-icon svgIcon="check" class="size-3.5" /> Conforme</span>
                        }
                      </div>
                    }
                  </div>
                }
                @if (sec.wMatriz) {
                  <div class="mt-4 pt-3 border-t border-slate-100">
                    <div class="flex items-center gap-2 mb-2.5">
                      <span class="text-[12px] font-bold text-slate-700 flex-1">Matriz de consistencia <span class="font-normal text-slate-400">· generada con lo que escribió el estudiante</span></span>
                      <span class="text-[9.5px] font-extrabold px-2 py-0.5 rounded-full" [ngClass]="matrizConsistente() ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'">
                        {{ matrizConsistente() ? '✓ CONSISTENTE' : 'INCOMPLETA' }}
                      </span>
                    </div>
                    <div class="overflow-x-auto">
                      <div class="grid grid-cols-4 gap-px bg-slate-200 rounded-lg overflow-hidden text-[10.5px] min-w-[520px]">
                        <div class="bg-[#8C1D2E] text-white font-extrabold px-2.5 py-1.5">PROBLEMA</div>
                        <div class="bg-[#8C1D2E] text-white font-extrabold px-2.5 py-1.5">OBJETIVOS</div>
                        <div class="bg-[#8C1D2E] text-white font-extrabold px-2.5 py-1.5">HIPÓTESIS</div>
                        <div class="bg-[#8C1D2E] text-white font-extrabold px-2.5 py-1.5">VARIABLES / METODOLOGÍA</div>
                        <div class="bg-white px-2.5 py-2 text-slate-600 leading-relaxed">{{ matriz().problema }}</div>
                        <div class="bg-white px-2.5 py-2 text-slate-600 leading-relaxed">{{ matriz().objetivos }}</div>
                        <div class="bg-white px-2.5 py-2 text-slate-600 leading-relaxed">{{ matriz().hipotesis }}</div>
                        <div class="bg-white px-2.5 py-2 text-slate-600 leading-relaxed">{{ matriz().variables }}</div>
                      </div>
                    </div>
                  </div>
                }

                <!-- Acción por bloque: un clic da conformidad a todo el bloque y salta al siguiente -->
                @if (!cerrada()) {
                  <div class="mt-5 pt-4 border-t border-dashed border-slate-200 flex flex-wrap items-center gap-3">
                    @if (bloqueConforme(sec)) {
                      <span class="inline-flex items-center gap-1.5 text-[12.5px] font-bold text-emerald-600">
                        <mat-icon svgIcon="badge-check" class="size-4" /> Bloque conforme
                      </span>
                      @if (paso() < totalPasos() - 1) {
                        <button type="button" class="ml-auto inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-[12.5px] font-bold text-[#8C1D2E] bg-[#FDF6F7] border border-[#8C1D2E]/20 hover:bg-[#f7e9ec] transition" (click)="siguiente()">
                          Siguiente bloque <mat-icon svgIcon="arrow-right" class="size-4" />
                        </button>
                      }
                    } @else {
                      <span class="text-[11px] text-slate-400 order-2 sm:order-1">Un clic: da conformidad a los ítems de este bloque y salta al siguiente.</span>
                      <button type="button" [disabled]="guardandoBloque()"
                              class="order-1 sm:order-2 ml-auto inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-[13px] font-bold text-white bg-emerald-600 hover:bg-emerald-700 shadow-sm transition disabled:opacity-50"
                              (click)="conformeBloque()">
                        <mat-icon svgIcon="check" class="size-4" /> Conforme y continuar
                        <mat-icon svgIcon="arrow-right" class="size-4" />
                      </button>
                    }
                  </div>
                }
              </section>
            }

            <!-- Navegación de pasos -->
            <div class="flex items-center justify-between">
              <button mat-stroked-button class="!h-9 !text-sm !rounded-lg" [disabled]="paso() === 0" (click)="anterior()">
                <mat-icon svgIcon="chevron-left" class="size-4" /> Anterior
              </button>
              <span class="text-[12px] text-slate-400">Paso {{ paso() + 1 }} de {{ totalPasos() }}</span>
              @if (paso() < totalPasos() - 1) {
                <button mat-flat-button color="primary" class="!h-9 !text-sm !rounded-lg" (click)="siguiente()">
                  Siguiente <mat-icon svgIcon="chevron-right" class="size-4" />
                </button>
              } @else {
                <span class="w-[92px]"></span>
              }
            </div>
              </div>
            </div>
          </div>
        } @else {
          <p class="text-sm text-slate-400">Cargando…</p>
        }
      </div>
    </div>
  `,
  styles: [`
    /* Destello al saltar a un ítem, para no perder de vista cuál era. */
    .campo-destacado { animation: destello 2.2s ease-out; }
    @keyframes destello {
      0%   { box-shadow: 0 0 0 3px rgba(245, 158, 11, .55); background: rgba(254, 243, 199, .55); }
      70%  { box-shadow: 0 0 0 3px rgba(245, 158, 11, .25); background: rgba(254, 243, 199, .25); }
      100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0);     background: transparent; }
    }
    @media (prefers-reduced-motion: reduce) { .campo-destacado { animation: none; } }
  `],
})
export class RevisionProyectoComponent implements OnInit {
  private _route = inject(ActivatedRoute);
  private _router = inject(Router);
  private _svc = inject(RevisionProyectoService);
  private _confirm = inject(ConfirmDialogService);
  private _dialog = inject(MatDialog);
  private _toast = inject(NotificationService);

  protected p = signal<ProyectoEditor | null>(null);
  protected paso = signal(0);
  private tesisId = '';

  protected secciones = computed<SeccionDef[]>(() => {
    const e = this.p();
    if (!e) return [];
    const cual = e.enfoque === 'CUALITATIVO';
    return PROY_DEF.filter((s) => !(s.cuant && cual) && !(s.cual && !cual));
  });
  protected totalPasos = computed(() => Math.max(1, this.secciones().length));
  protected secActual = computed<SeccionDef | null>(() => this.secciones()[this.paso()] ?? null);

  ngOnInit(): void {
    this.tesisId = this._route.snapshot.paramMap.get('tesisId') ?? '';
    this.cargar();
  }

  cargar(): void {
    this._svc.detalle$(this.tesisId).subscribe({
      next: (res) => {
        this.p.set(res?.data ?? res);
        if (this.paso() >= this.totalPasos()) this.paso.set(this.totalPasos() - 1);
        // Deja visible el historial de todo ítem ya observado/revisado (para no observar dos veces sin darse cuenta).
        const conHist = (this.p()?.revisiones ?? []).filter((r) => (r.eventos?.length ?? 0) > 0).map((r) => r.campo);
        this.histSet.set(new Set([...this.histSet(), ...conHist]));
      },
      error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo cargar el proyecto'),
    });
  }

  // ── Pasos ──
  irA(i: number): void {
    this.paso.set(Math.max(0, Math.min(this.totalPasos() - 1, i)));
    if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  siguiente(): void { this.irA(this.paso() + 1); }
  anterior(): void { this.irA(this.paso() - 1); }

  /** Estado de revisión de una sección (para el punto del paso). */
  secEstado(sec: SeccionDef): 'conforme' | 'pendiente' | 'none' {
    const claves = sec.campos.map((c) => c.k);
    const revs = (this.p()?.revisiones ?? []).filter((r) => claves.includes(r.campo));
    if (revs.some((r) => r.estado === 'OBSERVADO' || r.estado === 'EN_CORRECCION' || r.estado === 'CORREGIDO')) return 'pendiente';
    if (revs.some((r) => r.estado === 'CONFORME')) return 'conforme';
    return 'none';
  }
  /** Color del punto de la sección en el índice lateral, según su estado de revisión. */
  dotColorSec(sec: SeccionDef): string {
    const s = this.secEstado(sec);
    return s === 'conforme' ? '#16A34A' : s === 'pendiente' ? '#D97706' : '#E2E8F0';
  }

  rev(k: string): RevisionItem | undefined { return this.p()?.revisiones?.find((r) => r.campo === k); }
  chip(k: string) { return chipRevision(this.rev(k)?.estado); }

  /** Ítems que el estudiante ya corrigió (estado CORREGIDO), para el panel de verificación del asesor. */
  protected correcciones = computed(() => {
    const e = this.p();
    if (!e) return [];
    return (e.revisiones ?? [])
      .filter((r) => r.estado === 'CORREGIDO')
      .map((r) => {
        const meta = this.campoMeta(r.campo);
        const evs = [...(r.eventos ?? [])].reverse();
        return {
          campo: r.campo,
          label: meta.label,
          seccion: meta.seccion,
          observacion: evs.find((ev) => ev.tipo === 'OBSERVACIÓN')?.texto,
          correccion: evs.find((ev) => ev.tipo === 'CORRECCIÓN')?.texto,
        };
      });
  });

  /** Etiqueta y sección legibles de un campo (o del plan). */
  private campoMeta(campo: string): { label: string; seccion: string } {
    if (campo === 'plan') return { label: 'Plan de actividades', seccion: 'V. Aspectos administrativos' };
    for (const s of PROY_DEF) {
      const c = s.campos.find((x) => x.k === campo);
      if (c) return { label: c.l, seccion: s.titulo };
    }
    return { label: campo, seccion: '' };
  }

  /** Navega a la sección que contiene el campo y baja hasta él, destacándolo un momento. */
  irACampo(campo: string): void {
    const secs = this.secciones();
    const idx = secs.findIndex((s) => (campo === 'plan' ? !!s.wPlan : s.campos.some((c) => c.k === campo)));
    if (idx >= 0) this.irA(idx);
    if (typeof window === 'undefined') return;
    setTimeout(() => {
      const el = document.getElementById('campo-' + campo);
      if (!el) return;
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('campo-destacado');
      setTimeout(() => el.classList.remove('campo-destacado'), 2400);
    }, 80);
  }
  /** El co-asesor entra en modo consulta: ve todo, no actúa sobre nada. */
  soloLectura(): boolean {
    return !!this.p()?.soloLectura;
  }

  /** Con la carta ya emitida, la revisión queda cerrada. */
  cerrada(): boolean {
    const p = this.p();
    // Sin permiso de escritura (co-asesor) se comporta igual que una revisión cerrada: se ocultan
    // observar, dar conformidad y la carta, con su propio aviso arriba.
    if (this.soloLectura()) return true;
    // Cerrada tras emitir la carta, o si el proyecto ya avanzó de etapa (flujo hacia adelante, sin retroceder).
    return !!p && (!!p.cartaAsesor || !!p.revisoresConformes || !!p.defensaProgramada
      || !!p.informeFinalAprobado || !!p.informeFinalRevisado);
  }
  /** ¿El ítem ya fue observado/revisado alguna vez? (para avisar que volver a observar es repetir). */
  observado(k: string): boolean {
    const e = this.rev(k)?.estado;
    return !!e && e !== 'SIN_REVISION';
  }

  // ── Mini-historial por campo (colapsable) ──
  protected tipoDot = tipoDot;
  protected tipoVerbo = tipoVerbo;
  private histSet = signal<Set<string>>(new Set());
  toggleHist(k: string): void { const s = new Set(this.histSet()); s.has(k) ? s.delete(k) : s.add(k); this.histSet.set(s); }
  histOpen(k: string): boolean { return this.histSet().has(k); }
  /** Eventos en orden cronológico (del más antiguo al más reciente), como línea de tiempo. */
  eventos(k: string) { return [...(this.rev(k)?.eventos ?? [])]; }

  // ── Cronograma (Gantt por fechas, solo lectura) ──
  protected rangoFechas = rangoFechas;
  protected meses = computed(() => ganttMeses(this.p()?.actividades ?? []));
  private minMesIdx = computed(() => this.meses()[0]?.idx ?? 0);
  private readonly FASE_COLOR: Record<string, { fg: string; bg: string }> = {
    PLANIFICACION: { fg: '#475569', bg: '#F1F5F9' },
    TRABAJO_CAMPO: { fg: '#1D4ED8', bg: '#DBEAFE' },
    ANALISIS: { fg: '#B45309', bg: '#FEF3C7' },
    REDACCION: { fg: '#15803D', bg: '#DCFCE7' },
  };
  protected faseLabel = (v?: string) => (v ? FASE_LABEL[v] ?? v : '—');
  actHechas(): number { return this.p()?.actividades?.filter((a) => a.estado === 'HECHA').length ?? 0; }
  actEnCurso(): number { return this.p()?.actividades?.filter((a) => a.estado === 'EN_CURSO').length ?? 0; }
  faseFg(fase?: string): string { return (fase && this.FASE_COLOR[fase]?.fg) || '#94A3B8'; }
  faseBg(fase?: string): string { return (fase && this.FASE_COLOR[fase]?.bg) || '#F1F5F9'; }
  ganttCol(a: ActividadItem): string | null {
    return ganttColFecha(a, this.minMesIdx());
  }
  estadoLabel(e?: string): string { return (e ?? 'PENDIENTE').replace('_', ' '); }
  estadoCls(e?: string): string {
    switch (e) {
      case 'HECHA': return 'bg-emerald-100 text-emerald-700';
      case 'EN_CURSO': return 'bg-amber-100 text-amber-700';
      default: return 'bg-slate-100 text-slate-500';
    }
  }

  // ── Matriz de consistencia (solo lectura, derivada de lo que escribió el estudiante) ──
  matriz(): { problema: string; objetivos: string; hipotesis: string; variables: string } {
    const e = this.p();
    const v = (k: string) => e?.campos?.[k]?.trim();
    const nObj = (e?.objetivos ?? []).filter((o) => o.texto && o.texto.trim()).length;
    return {
      problema: v('formulacion') || '— sin formulación del problema (I)',
      objetivos: (v('objGeneral') || '— sin objetivo general (I)') + (nObj ? ' · +' + nObj + ' específicos' : ''),
      hipotesis: e?.enfoque === 'CUALITATIVO' ? 'No aplica (enfoque cualitativo)' : (v('hipotesis') || '— sin hipótesis (III)'),
      variables: v('variables') || v('diseno') || '— sin variables (III) ni diseño (IV)',
    };
  }
  matrizConsistente(): boolean {
    const e = this.p();
    return !!(e?.campos?.['formulacion']?.trim() && e?.campos?.['objGeneral']?.trim());
  }

  observar(campo: string, label: string): void {
    this._dialog.open(ObservacionDialogComponent, { width: '480px', autoFocus: true, data: { titulo: label } })
      .afterClosed().subscribe((texto: string | null) => {
        if (!texto) return;
        this._svc.observar$(this.tesisId, campo, texto).subscribe({
          next: () => { this._toast.success('Ítem observado'); this.cargar(); },
          error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo observar'),
        });
      });
  }

  conforme(campo: string): void {
    this._svc.conforme$(this.tesisId, campo).subscribe({
      next: () => { this._toast.success('Ítem con conformidad'); this.cargar(); },
      error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo dar conformidad'),
    });
  }

  // ── Conformidad por bloque (un clic + auto-avance) ──
  protected guardandoBloque = signal(false);

  /** Todas las claves revisables (con conformidad) de las secciones activas + el plan. */
  private itemsRevisables(): string[] {
    const keys: string[] = [];
    for (const s of this.secciones()) {
      keys.push(...s.campos.map((c) => c.k));
      if (s.wPlan) keys.push('plan');
    }
    return keys;
  }

  /** Claves revisables de una sección concreta (sus campos + el plan si la sección lo incluye). */
  private camposDeSeccion(sec: SeccionDef): string[] {
    const keys = sec.campos.map((c) => c.k);
    if (sec.wPlan) keys.push('plan');
    return keys;
  }

  /** ¿Todos los ítems revisables de la sección están CONFORME? */
  bloqueConforme(sec: SeccionDef): boolean {
    const keys = this.camposDeSeccion(sec);
    return keys.length > 0 && keys.every((k) => this.rev(k)?.estado === 'CONFORME');
  }

  /** La carta solo se habilita cuando TODO ítem revisable del proyecto está CONFORME. */
  protected todosConformes = computed(() => {
    const e = this.p();
    if (!e || !e.listoRevision) return false;
    return this.itemsRevisables().every((k) => this.rev(k)?.estado === 'CONFORME');
  });

  /** Da conformidad a todo el bloque actual (una sola petición) y salta al siguiente. */
  conformeBloque(): void {
    const sec = this.secActual();
    if (!sec || this.guardandoBloque()) return;
    // Solo los ítems confirmables: se omiten las observaciones vivas (quedan pendientes de corrección).
    const aConfirmar = this.camposDeSeccion(sec).filter((k) => {
      const s = this.rev(k)?.estado;
      return s !== 'CONFORME' && s !== 'OBSERVADO' && s !== 'EN_CORRECCION';
    });
    if (!aConfirmar.length) {
      this._toast.error('No hay ítems por confirmar en este bloque (los observados esperan corrección del estudiante).');
      return;
    }
    const esUltimo = this.paso() >= this.totalPasos() - 1;
    this.guardandoBloque.set(true);
    // Actualización optimista: se refleja de inmediato y se avanza sin recargar todo el proyecto.
    this.marcarConformeLocal(aConfirmar);
    if (esUltimo) {
      if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      this.siguiente();
    }
    this._svc.conformeSeccion$(this.tesisId, aConfirmar).subscribe({
      next: () => { this._toast.success('Bloque con conformidad'); this.guardandoBloque.set(false); },
      error: (e) => {
        this.guardandoBloque.set(false);
        this._toast.error(e?.error?.message ?? 'No se pudo dar conformidad al bloque');
        this.cargar(); // revierte el optimismo con el estado real del servidor
      },
    });
  }

  /** Marca localmente los campos como CONFORME (sin recargar), para respuesta instantánea. */
  private marcarConformeLocal(campos: string[]): void {
    const e = this.p();
    if (!e) return;
    const revs = [...(e.revisiones ?? [])];
    for (const k of campos) {
      const i = revs.findIndex((r) => r.campo === k);
      if (i >= 0) revs[i] = { ...revs[i], estado: 'CONFORME' };
      else revs.push({ campo: k, estado: 'CONFORME', eventos: [] });
    }
    this.p.set({ ...e, revisiones: revs });
  }

  emitirCarta(): void {
    this._confirm.confirmSave({ title: 'Carta de opinión favorable', message: 'Se emitirá tu carta de opinión favorable y el estudiante podrá continuar con Turnitin y la solicitud de aprobación.' })
      .then(() => {
        this._svc.cartaOpinion$(this.tesisId).subscribe({
          next: () => { this._toast.success('Carta de opinión favorable emitida'); this.cargar(); },
          error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo emitir la carta'),
        });
      }).catch(() => {});
  }

  volver(): void { this._router.navigate(['/admin/revision-proyecto']); }
}
