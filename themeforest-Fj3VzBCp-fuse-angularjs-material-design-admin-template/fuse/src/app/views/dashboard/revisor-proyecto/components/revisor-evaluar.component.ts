import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { RevisorProyectoService } from '../services/revisor-proyecto.service';
import { PROY_DEF, tipoDot, tipoVerbo } from '../../mi-proyecto/models/proyecto.model';

/** Revisor · Etapa 5 — evaluación de un proyecto con la rúbrica. */
@Component({
  selector: 'app-revisor-evaluar',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <button mat-button class="!text-slate-500 !text-xs !px-2 mb-1" (click)="volver()">
            <mat-icon svgIcon="chevron-left" class="size-3.5" /> Volver a la bandeja
          </button>
          <h1 class="page-title">Evaluación del proyecto</h1>
        </div>
      </div>

      <div class="page-content p-4">
        @if (data(); as d) {
          <div class="mx-auto max-w-[1720px]">
            <!-- Cabecera (tarjeta) -->
            <section class="form-card mb-3 !py-3">
              <div class="flex items-start gap-x-4 gap-y-1 flex-wrap">
                <div class="min-w-0 flex-1">
                  <p class="text-[12.5px] text-slate-700"><span class="font-semibold text-slate-500">Doctorando:</span> {{ d.proyecto.estudianteNombre }} <span class="text-slate-400">· {{ d.proyecto.codigoSistema }}</span></p>
                  <p class="text-[12px] text-slate-500"><span class="font-semibold">Programa:</span> {{ d.proyecto.programaNombre }}</p>
                  <p class="text-[12.5px] mt-0.5"><span class="font-semibold text-slate-500">Título:</span> <span class="font-bold text-[#8C1D2E]">{{ d.proyecto.titulo }}</span></p>
                </div>
                @if (d.cerrada) {
                  <span class="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-[11.5px] font-semibold text-emerald-700 shrink-0">
                    <mat-icon svgIcon="badge-check" class="size-3.5 shrink-0" /> Evaluación registrada · {{ d.miPuntajeTotal }}/{{ d.puntajeMaximo }}
                  </span>
                } @else if (d.respuestaEstudiante) {
                  <span class="inline-flex items-center gap-1 rounded-lg border border-sky-200 bg-sky-50 px-2.5 py-1 text-[11.5px] font-semibold text-sky-700 shrink-0">
                    <mat-icon svgIcon="rotate-ccw" class="size-3.5 shrink-0" /> El estudiante levantó tus observaciones
                  </span>
                } @else if (bloqueadaCalificacion()) {
                  <span class="inline-flex items-center gap-1 rounded-lg border border-amber-200 bg-amber-50 px-2.5 py-1 text-[11.5px] font-semibold text-amber-700 shrink-0">
                    <mat-icon svgIcon="lock" class="size-3.5 shrink-0" /> Subsanación solicitada · esperando al estudiante
                  </span>
                }
              </div>
            </section>

            <div class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_460px] gap-3 items-start">
              <!-- Proyecto del doctorando (visor PDF) -->
              <div class="min-w-0">
                <div class="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden lg:sticky lg:top-4">
                  <div class="flex items-center gap-2 px-3 py-2 border-b border-slate-100 bg-slate-50">
                    <span class="size-6 rounded bg-[#8C1D2E] text-white grid place-items-center text-[9px] font-bold shrink-0">PDF</span>
                    <span class="text-[12px] font-semibold text-slate-700 truncate">Proyecto — {{ d.proyecto.estudianteNombre }}</span>
                    <div class="ml-auto flex items-center gap-1.5 shrink-0">
                      <button type="button" class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold text-slate-600 bg-white border border-slate-200 hover:bg-slate-100 transition" (click)="imprimirProyecto()">
                        <mat-icon svgIcon="printer" class="size-3.5" /> Imprimir
                      </button>
                      <button type="button" class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold text-white bg-[#8C1D2E] hover:bg-[#731725] transition" (click)="descargarProyecto()">
                        <mat-icon svgIcon="download" class="size-3.5" /> Descargar
                      </button>
                    </div>
                  </div>
                  @if (pdfUrl()) {
                    <iframe [src]="pdfUrl()" class="w-full h-[calc(100vh-150px)] min-h-[520px] border-0 bg-slate-500" title="Proyecto en PDF"></iframe>
                  } @else {
                    <div class="h-[calc(100vh-150px)] min-h-[520px] grid place-items-center text-sm text-slate-400">Generando PDF del proyecto…</div>
                  }
                </div>
              </div>

              <!-- Rúbrica oficial de evaluación (panel de altura completa) -->
              <aside class="form-card !p-0 flex flex-col lg:h-[calc(100vh-150px)] lg:overflow-hidden">
                <!-- Cabecera fija -->
                <div class="shrink-0 px-4 pt-3.5 pb-2.5 border-b border-slate-100">
                  <div class="flex items-center gap-2 flex-wrap">
                    <h2 class="form-card__title">Rúbrica de evaluación</h2>
                    @if (d.rubricaDisponible) {
                      <button type="button" (click)="descargarWord()"
                              class="ml-auto flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 hover:bg-emerald-100 transition">
                        <mat-icon svgIcon="download" class="size-3.5" /> Descargar Word
                      </button>
                    }
                  </div>
                  <p class="text-[11px] text-slate-400 mt-1">{{ d.rubricaTitulo }} · Cumple / Parcial / No cumple · aprueba ≥ {{ d.aprobadoMin }}.</p>
                </div>

                @if (!d.rubricaDisponible) {
                  <div class="flex-1 overflow-y-auto p-4">
                    <div class="rounded-lg border border-amber-200 bg-amber-50 px-3 py-3 text-[12px] text-amber-700 flex items-start gap-2">
                      <mat-icon svgIcon="lock" class="size-4 shrink-0 mt-px" />
                      <span>La evaluación aún <b>no está habilitada</b>. Secretaría debe subir la rúbrica oficial de este proyecto. Te avisaremos por la campanita cuando esté lista.</span>
                    </div>
                  </div>
                } @else {
                  <!-- Puntaje total (fijo) -->
                  <div class="shrink-0 px-4 pt-3">
                    <div class="flex items-center justify-between rounded-lg bg-[#FDF6F7] border border-[#8C1D2E]/15 px-3 py-2">
                      <span class="text-[11px] font-semibold text-slate-500">Puntaje total</span>
                      <div class="flex items-center gap-2">
                        @if (completa()) {
                          <span class="px-2 py-0.5 rounded text-[10px] font-bold"
                                [ngClass]="total() >= d.aprobadoMin ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'">
                            {{ total() >= d.aprobadoMin ? 'Aprobado' : 'Desaprobado' }}
                          </span>
                        }
                        <span class="text-[18px] font-extrabold text-[#8C1D2E] leading-none">{{ total() }}<span class="text-[12px] text-slate-400">/{{ d.puntajeMaximo }}</span></span>
                      </div>
                    </div>
                  </div>

                  <!-- Secciones (scroll) -->
                  <div class="flex-1 lg:overflow-y-auto px-4 pt-3 pb-3 space-y-2.5">
                    @if (d.respuestaEstudiante && criteriosObservados().length && !d.cerrada) {
                      <!-- Correcciones por verificar: solo se activa si hay observación pendiente -->
                      <section class="rounded-xl border border-sky-200 bg-sky-50 p-3">
                        <p class="text-[11px] font-bold text-sky-700 mb-1.5 flex items-center gap-1.5">
                          <mat-icon svgIcon="rotate-ccw" class="size-3.5" /> El estudiante respondió · verifica {{ criteriosObservados().length }} criterio(s) que observaste
                        </p>
                        <p class="text-[12px] text-slate-700 whitespace-pre-wrap rounded-lg bg-white/70 border border-sky-100 px-2.5 py-1.5 mb-2">{{ d.respuestaEstudiante }}</p>
                        <!-- Solo navegación: aceptar la corrección se hace EN el criterio, junto a
                             la observación que se está retirando. -->
                        <div class="flex flex-wrap gap-1.5">
                          @for (c of criteriosObservados(); track c.key) {
                            <button type="button" class="flex items-center gap-1 px-2 py-0.5 rounded-md text-[10.5px] font-semibold bg-white border border-sky-200 text-sky-700 hover:bg-sky-100"
                                    [title]="'Observaste: ' + c.observacion" (click)="irACriterio(c.key)">
                              {{ c.titulo }} <mat-icon svgIcon="arrow-right" class="size-3" />
                            </button>
                          }
                        </div>
                        <p class="text-[10.5px] text-slate-500 mt-2 leading-snug">En cada criterio decides: <b>Aceptar corrección</b> retira tu observación. Cuando no quede ninguna, aparece <b>Dar conformidad</b>.</p>
                      </section>
                    } @else if (d.respuestaEstudiante) {
                      <div class="rounded-lg bg-sky-50 border border-sky-100 px-3 py-2">
                        <p class="text-[11px] font-bold text-sky-700 mb-0.5">El estudiante respondió tus observaciones:</p>
                        <p class="text-[12px] text-slate-700 whitespace-pre-wrap">{{ d.respuestaEstudiante }}</p>
                      </div>
                    }
                    @for (sec of d.secciones; track sec.key) {
                      <div class="rounded-xl border border-slate-200 overflow-hidden">
                        <div class="flex items-center gap-2 px-3 py-2 cursor-pointer bg-[#FDF6F7] select-none" (click)="toggleSec(sec)">
                          <mat-icon [svgIcon]="secAbierta(sec) ? 'chevron-down' : 'chevron-right'" class="size-4 text-[#8C1D2E] shrink-0" />
                          <span class="text-[11px] font-extrabold uppercase tracking-wide text-[#8C1D2E]">{{ sec.titulo }}</span>
                          <span class="ml-auto text-[10.5px] font-bold shrink-0">
                            @if (seccionCompleta(sec)) {
                              <span class="text-emerald-600">✓ {{ subtotalObtenido(sec) }}/{{ sec.subtotalMaximo }}</span>
                            } @else {
                              <span class="text-slate-400">{{ criteriosCalificados(sec) }}/{{ sec.criterios.length }} calificados</span>
                            }
                          </span>
                        </div>
                        @if (secAbierta(sec)) {
                          <div class="p-2 space-y-1.5">
                            @for (cr of sec.criterios; track cr.key) {
                              <div class="rounded-lg border border-slate-100 p-1.5 scroll-mt-2 transition-shadow" [id]="'crit-' + cr.key"
                                   [ngClass]="resaltado() === cr.key ? 'ring-2 ring-[#8C1D2E]' : ''">
                                <!-- Fila 1: criterio a la izquierda, calificación a la derecha.
                                     Fila 2: descripción y el enlace de observación, con los bordes
                                     alineados a los de arriba. -->
                                <div class="flex items-start justify-between gap-3">
                                  <p class="text-[12px] font-semibold text-slate-700 leading-snug min-w-0">{{ cr.titulo }}</p>
                                  <div class="shrink-0 inline-flex rounded-lg border border-slate-200 overflow-hidden divide-x divide-slate-200"
                                       role="group" [attr.aria-label]="'Calificar ' + cr.titulo">
                                    <button type="button" [disabled]="d.cerrada || bloqueadaCalificacion()"
                                            class="w-[74px] px-1 py-1 text-[10px] font-bold uppercase tracking-wide leading-none transition disabled:opacity-50"
                                            [ngClass]="niveles()[cr.key] === 'CUMPLE' ? 'bg-emerald-600 text-white' : 'bg-white text-slate-500 hover:bg-slate-50'"
                                            (click)="setNivel(cr.key, 'CUMPLE')">
                                      Cumple <span class="text-[11px] font-extrabold">{{ cr.cumple }}</span>
                                    </button>
                                    <button type="button" [disabled]="d.cerrada || bloqueadaCalificacion()"
                                            class="w-[74px] px-1 py-1 text-[10px] font-bold uppercase tracking-wide leading-none transition disabled:opacity-50"
                                            [ngClass]="niveles()[cr.key] === 'PARCIAL' ? 'bg-amber-500 text-white' : 'bg-white text-slate-500 hover:bg-slate-50'"
                                            (click)="setNivel(cr.key, 'PARCIAL')">
                                      Parcial <span class="text-[11px] font-extrabold">{{ cr.parcial }}</span>
                                    </button>
                                    <button type="button" [disabled]="d.cerrada || bloqueadaCalificacion()"
                                            class="w-[74px] px-1 py-1 text-[10px] font-bold uppercase tracking-wide leading-none transition disabled:opacity-50"
                                            [ngClass]="niveles()[cr.key] === 'NO_CUMPLE' ? 'bg-rose-600 text-white' : 'bg-white text-slate-500 hover:bg-slate-50'"
                                            (click)="setNivel(cr.key, 'NO_CUMPLE')">
                                      No cump. <span class="text-[11px] font-extrabold">{{ cr.noCumple }}</span>
                                    </button>
                                  </div>
                                </div>

                                <div class="flex items-start justify-between gap-3 mt-0.5">
                                  <p class="text-[10.5px] text-slate-400 leading-[1.3] line-clamp-2 min-w-0" [title]="cr.descripcion">{{ cr.descripcion }}</p>
                                  @if (!d.cerrada) {
                                    <!-- Observar es una acción, no una nota al pie: va en rojo (en gris
                                         se perdía entre el texto de ayuda y nadie la encontraba). -->
                                    <button type="button" class="shrink-0 inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md text-[10.5px] font-bold transition"
                                            [ngClass]="observaciones()[cr.key]
                                              ? 'text-rose-700 bg-rose-50 border border-rose-200'
                                              : 'text-rose-600 hover:bg-rose-50'"
                                            (click)="toggleObs(cr.key)">
                                      <mat-icon svgIcon="flag" class="size-3" /> {{ observaciones()[cr.key] ? 'Observado' : 'Observar' }}
                                    </button>
                                  }
                                </div>
                                @if (obsOpen(cr.key) || observaciones()[cr.key]) {
                                  @if (enviadas().has(cr.key)) {
                                    <!-- Ya está en el servidor (se envió con Solicitar subsanación antes):
                                         sigue editable hasta el próximo envío. -->
                                    <textarea class="mt-1 w-full rounded-lg border border-amber-200 bg-amber-50/60 px-2.5 py-1.5 text-[11.5px] focus:outline-none focus:border-amber-400"
                                              rows="2" [disabled]="d.cerrada" [ngModel]="observaciones()[cr.key] || ''" (ngModelChange)="setObs(cr.key, $event)"
                                              [placeholder]="'Sugerencia de subsanación para «' + cr.titulo + '»…'"></textarea>
                                    @if (!d.cerrada) {
                                      <div class="flex items-center gap-2 mt-1">
                                        <span class="flex-1 text-[10px] text-slate-400 leading-tight">Ya está enviada al estudiante.</span>
                                        <button type="button" class="shrink-0 flex items-center gap-1 px-2 py-0.5 rounded-md text-[10.5px] font-bold text-slate-500 hover:text-rose-600 hover:bg-rose-50 transition"
                                                (click)="quitarObs(cr.key)">
                                          <mat-icon svgIcon="x" class="size-3" /> Quitar observación
                                        </button>
                                      </div>
                                    }
                                  } @else if (confirmadas().has(cr.key) && !obsOpen(cr.key)) {
                                    <!-- Guardada localmente: queda lista, a la espera de "Solicitar subsanación"
                                         (que envía TODAS las observaciones guardadas de una vez). -->
                                    <div class="mt-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1.5">
                                      <p class="text-[10px] font-bold text-emerald-700 flex items-center gap-1">
                                        <mat-icon svgIcon="check" class="size-3" /> Observación guardada
                                      </p>
                                      <p class="text-[11px] text-slate-700 whitespace-pre-wrap">{{ observaciones()[cr.key] }}</p>
                                      @if (!d.cerrada) {
                                        <div class="flex items-center gap-2 mt-1">
                                          <button type="button" class="shrink-0 flex items-center gap-1 px-2 py-0.5 rounded-md text-[10.5px] font-bold text-slate-500 hover:text-slate-700 hover:bg-white transition"
                                                  (click)="toggleObs(cr.key)">
                                            <mat-icon svgIcon="pencil" class="size-3" /> Editar
                                          </button>
                                          <button type="button" class="shrink-0 flex items-center gap-1 px-2 py-0.5 rounded-md text-[10.5px] font-bold text-slate-500 hover:text-rose-600 hover:bg-rose-50 transition"
                                                  (click)="quitarObs(cr.key)">
                                            <mat-icon svgIcon="x" class="size-3" /> Quitar
                                          </button>
                                        </div>
                                      }
                                    </div>
                                  } @else {
                                    <!-- Redactando: todavía no se guardó, así que no es "Observado" de verdad. -->
                                    <textarea class="mt-1 w-full rounded-lg border border-amber-200 bg-amber-50/60 px-2.5 py-1.5 text-[11.5px] focus:outline-none focus:border-amber-400"
                                              rows="2" [disabled]="d.cerrada" [ngModel]="observaciones()[cr.key] || ''" (ngModelChange)="setObs(cr.key, $event)"
                                              [placeholder]="'Sugerencia de subsanación para «' + cr.titulo + '»…'"></textarea>
                                    @if (!d.cerrada) {
                                      <div class="flex items-center gap-2 mt-1">
                                        <span class="flex-1 text-[10px] text-slate-400 leading-tight">Aún no se guardó esta observación.</span>
                                        <button type="button" class="shrink-0 flex items-center gap-1 px-2 py-0.5 rounded-md text-[10.5px] font-bold text-slate-500 hover:text-rose-600 hover:bg-rose-50 transition"
                                                (click)="quitarObs(cr.key)">
                                          <mat-icon svgIcon="x" class="size-3" /> Descartar
                                        </button>
                                        <button type="button" class="shrink-0 flex items-center gap-1 px-2 py-0.5 rounded-md text-[10.5px] font-bold text-white bg-emerald-600 hover:bg-emerald-700 shadow-sm transition disabled:opacity-40 disabled:cursor-not-allowed"
                                                [disabled]="!(observaciones()[cr.key] ?? '').trim()"
                                                (click)="guardarObs(cr.key)">
                                          <mat-icon svgIcon="check" class="size-3" /> Guardar
                                        </button>
                                      </div>
                                    }
                                  }
                                }

                                <!-- Qué corrigió el estudiante EN ESTE criterio + la decisión, juntas.
                                     Solo si la observación ya se envió (existe en el servidor): un
                                     borrador que el revisor recién está redactando no tiene nada que
                                     "aceptar" todavía. El botón se ofrece siempre que el criterio siga
                                     observado: si el revisor ya lo revisó en el PDF, puede aceptarlo sin más. -->
                                @if (enviadas().has(cr.key) && observaciones()[cr.key] && !d.cerrada) {
                                  <div class="mt-1.5 rounded-lg border border-sky-200 bg-sky-50 px-2.5 py-1.5">
                                    @if (cr.correccionEstudiante) {
                                      <p class="text-[10px] font-bold text-sky-700 flex items-center gap-1">
                                        <mat-icon svgIcon="rotate-ccw" class="size-3" /> El estudiante corrigió
                                        @if (cr.correccionFecha) { <span class="font-normal text-sky-500">· {{ cr.correccionFecha }}</span> }
                                      </p>
                                      <p class="text-[11px] text-slate-700 whitespace-pre-wrap line-clamp-4 rounded-md bg-white/70 border border-sky-100 px-2 py-1 mt-0.5"
                                         [title]="cr.correccionEstudiante">{{ cr.correccionEstudiante }}</p>
                                    } @else if (d.respuestaEstudiante) {
                                      <p class="text-[10px] font-bold text-sky-700 flex items-center gap-1">
                                        <mat-icon svgIcon="rotate-ccw" class="size-3" /> El estudiante respondió
                                      </p>
                                      <p class="text-[11px] text-slate-700 whitespace-pre-wrap line-clamp-3" [title]="d.respuestaEstudiante">{{ d.respuestaEstudiante }}</p>
                                    } @else {
                                      <p class="text-[10px] text-sky-700">Aún sin corrección del estudiante en este ítem.</p>
                                    }
                                    <button type="button" class="mt-1.5 flex items-center gap-1 px-2 py-0.5 rounded-md text-[10.5px] font-bold text-white bg-emerald-600 hover:bg-emerald-700 shadow-sm transition"
                                            (click)="aceptarCorreccion(cr.key)">
                                      <mat-icon svgIcon="check" class="size-3" /> Aceptar corrección
                                    </button>
                                  </div>
                                }
                              </div>
                            }
                          </div>
                        }
                      </div>
                    }
                  </div>

                  @if (!d.cerrada) {
                    <div class="shrink-0 px-4 py-2.5 border-t border-slate-100 bg-white">
                      <p class="text-[10.5px] text-slate-500 mb-2 leading-snug">
                        Marca el nivel de cada criterio y agrega una <b class="text-[#8C1D2E]">observación</b> donde deba subsanar. Al completar un rubro, se pliega solo.
                      </p>
                      <!-- Un solo botón, el que corresponde: con observaciones registradas la
                           conformidad es imposible, así que mostrarla solo confunde. -->
                      <div class="flex gap-2">
                        @if (hayObservacion()) {
                          <button mat-stroked-button class="!h-9 !text-[12px] !font-semibold !flex-1 !text-rose-600 !border-rose-300 !bg-rose-50 hover:!bg-rose-100"
                                  [disabled]="!completa() || guardando()" (click)="evaluar(false)">
                            <mat-icon svgIcon="flag" class="size-3.5 mr-1" /> Solicitar subsanación
                          </button>
                        } @else {
                          <button mat-flat-button class="!h-9 !text-[12px] !flex-1 !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                                  [disabled]="!completa() || guardando()" (click)="evaluar(true)">
                            <mat-icon svgIcon="check" class="size-3.5 mr-1" /> Dar conformidad
                          </button>
                        }
                      </div>
                      @if (!completa()) {
                        <p class="text-[11px] text-amber-600 mt-1.5">Califica todos los criterios de la rúbrica para poder evaluar.</p>
                      } @else if (hayObservacion()) {
                        <p class="text-[11px] text-slate-400 mt-1.5">Con observaciones registradas el proyecto vuelve al estudiante para que subsane. Bórralas si deseas dar conformidad.</p>
                      } @else {
                        <p class="text-[11px] text-slate-400 mt-1.5">¿Falta algo? Marca <b class="text-rose-600">Observar</b> en el criterio correspondiente y podrás solicitar la subsanación.</p>
                      }
                    </div>
                  }
                }
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
export class RevisorEvaluarComponent implements OnInit {
  private _route = inject(ActivatedRoute);
  private _router = inject(Router);
  private _svc = inject(RevisorProyectoService);
  private _toast = inject(NotificationService);
  private _confirm = inject(ConfirmDialogService);
  private _san = inject(DomSanitizer);

  protected data = signal<any | null>(null);
  protected niveles = signal<Record<string, string>>({});        // criterio -> CUMPLE | PARCIAL | NO_CUMPLE
  protected observaciones = signal<Record<string, string>>({});  // criterio -> observación (incluye borradores sin enviar)
  /** Criterios cuya observación ya quedó registrada en el servidor (se envió con "Solicitar
   *  subsanación"). Un borrador que el revisor recién está escribiendo no cuenta como enviado:
   *  "Aceptar corrección" solo tiene sentido sobre una observación que el alumno ya puede ver. */
  protected enviadas = signal<Set<string>>(new Set());
  /** Observaciones nuevas que el revisor ya guardó con el botón "Guardar" (pero aún no se
   *  enviaron: eso solo ocurre al pulsar "Solicitar subsanación", que las envía todas juntas). */
  protected confirmadas = signal<Set<string>>(new Set());
  protected guardando = signal(false);
  private tesisId = '';

  // Visor PDF del proyecto.
  protected pdfUrl = signal<SafeResourceUrl | null>(null);
  private pdfBlobUrl: string | null = null;

  // UI de la rúbrica: observaciones abiertas + secciones forzadas (override del auto-plegado).
  private obsSet = signal<Set<string>>(new Set());
  private secForzadas = signal<Record<string, boolean>>({});
  protected tipoDot = tipoDot;
  protected tipoVerbo = tipoVerbo;

  protected secciones = computed(() => {
    const d = this.data();
    if (!d) return [];
    const cual = d.proyecto?.enfoque === 'CUALITATIVO';
    return PROY_DEF.filter((s: any) => !(s.cuant && cual) && !(s.cual && !cual) && s.id !== 'p5');
  });

  /** Todos los criterios de la rúbrica, aplanados. */
  private criterios(): any[] {
    return (this.data()?.secciones ?? []).flatMap((s: any) => s.criterios ?? []);
  }
  private puntajeNivel(cr: any, nivel: string): number {
    if (nivel === 'CUMPLE') return cr.cumple ?? 0;
    if (nivel === 'PARCIAL') return cr.parcial ?? 0;
    if (nivel === 'NO_CUMPLE') return cr.noCumple ?? 0;
    return 0;
  }
  protected total = computed(() => {
    const nv = this.niveles();
    return this.criterios().reduce((t, c: any) => t + (nv[c.key] ? this.puntajeNivel(c, nv[c.key]) : 0), 0);
  });
  protected completa = computed(() => {
    const nv = this.niveles();
    const crs = this.criterios();
    return crs.length > 0 && crs.every((c: any) => !!nv[c.key]);
  });
  protected subtotalObtenido(sec: any): number {
    const nv = this.niveles();
    return (sec.criterios ?? []).reduce((t: number, c: any) => t + (nv[c.key] ? this.puntajeNivel(c, nv[c.key]) : 0), 0);
  }
  descargarWord(): void {
    // El Word se descarga llenado con los puntajes registrados en el sistema (o en blanco si aún no evaluó).
    this._svc.descargarRubricaLlenada$(this.tesisId).subscribe({
      next: (b) => this.saveBlob(b, 'rubrica-evaluada.docx'),
      error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo descargar la rúbrica'),
    });
  }
  private saveBlob(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = name; a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  /** El revisor puede observar/evaluar solo si la rúbrica está subida y su evaluación no está cerrada. */
  protected puedeEvaluar(): boolean {
    const d = this.data();
    return !!d && d.rubricaDisponible && !d.cerrada;
  }

  ngOnInit(): void {
    this.tesisId = this._route.snapshot.paramMap.get('tesisId') ?? '';
    this.cargar();
    this.cargarPdf();
  }

  cargar(): void {
    this._svc.detalle$(this.tesisId).subscribe({
      next: (res) => {
        const d = res?.data ?? res;
        this.data.set(d);
        const nv: Record<string, string> = {};
        const obs: Record<string, string> = {};
        (d?.secciones ?? []).forEach((s: any) => (s.criterios ?? []).forEach((c: any) => {
          if (c.nivel) nv[c.key] = c.nivel;
          if (c.observacion) obs[c.key] = c.observacion;
        }));
        this.niveles.set(nv);
        this.observaciones.set(obs);
        this.enviadas.set(new Set(Object.keys(obs)));
        // Con la rúbrica ya calificada, TODAS las secciones se pliegan solas (están completas) y
        // los criterios observados —donde ahora se acepta la corrección— quedaban escondidos.
        // Si el estudiante respondió, se abren esas secciones para que la acción se vea.
        if (d?.respuestaEstudiante) {
          const abiertas: Record<string, boolean> = { ...this.secForzadas() };
          (d?.secciones ?? []).forEach((s: any) => {
            if ((s.criterios ?? []).some((c: any) => (obs[c.key] ?? '').trim())) abiertas[s.key] = true;
          });
          this.secForzadas.set(abiertas);
        }
      },
      error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo cargar la evaluación'),
    });
  }

  private cargarPdf(): void {
    this._svc.proyectoPdf$(this.tesisId).subscribe({
      next: (blob) => {
        if (this.pdfBlobUrl) URL.revokeObjectURL(this.pdfBlobUrl);
        this.pdfBlobUrl = URL.createObjectURL(blob);
        // navpanes=0 oculta el panel de miniaturas/marcadores; view=FitH ajusta al ancho.
        this.pdfUrl.set(this._san.bypassSecurityTrustResourceUrl(this.pdfBlobUrl + '#toolbar=1&navpanes=0&view=FitH'));
      },
      error: () => this._toast.error('No se pudo generar el PDF del proyecto'),
    });
  }

  imprimirProyecto(): void {
    if (this.pdfBlobUrl) window.open(this.pdfBlobUrl, '_blank');
  }
  descargarProyecto(): void {
    if (this.pdfBlobUrl) { const a = document.createElement('a'); a.href = this.pdfBlobUrl; a.download = 'proyecto.pdf'; a.click(); }
  }

  val(d: any, k: string): string { return d?.proyecto?.campos?.[k] ?? ''; }

  setNivel(key: string, nivel: string): void {
    if (this.data()?.cerrada || this.bloqueadaCalificacion()) return;
    this.niveles.set({ ...this.niveles(), [key]: nivel });
  }

  /**
   * Ya se pidió subsanación y se está esperando al estudiante: la calificación queda congelada
   * hasta que corrija (el backend recién marca `cerrada` con la conformidad final, así que esta
   * espera intermedia necesita su propio candado en pantalla).
   */
  protected bloqueadaCalificacion(): boolean {
    return this.data()?.miEstado === 'OBSERVADO';
  }

  // ── Correcciones por verificar (solo si el estudiante respondió y hay observación pendiente) ──
  protected resaltado = signal<string | null>(null);

  /** Criterios que el revisor observó y siguen con observación pendiente (por verificar). */
  protected criteriosObservados = computed(() => {
    const obs = this.observaciones();
    const out: { key: string; titulo: string; observacion: string }[] = [];
    for (const sec of (this.data()?.secciones ?? [])) {
      for (const c of (sec.criterios ?? [])) {
        const o = (obs[c.key] ?? '').trim();
        if (o) out.push({ key: c.key, titulo: c.titulo, observacion: o });
      }
    }
    return out;
  });

  /** Navega al criterio observado: abre su sección, hace scroll y lo resalta. */
  irACriterio(key: string): void {
    const sec = (this.data()?.secciones ?? []).find((s: any) => (s.criterios ?? []).some((c: any) => c.key === key));
    if (sec) this.secForzadas.set({ ...this.secForzadas(), [sec.key]: true });
    if (typeof document === 'undefined') return;
    setTimeout(() => {
      const el = document.getElementById('crit-' + key);
      el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      this.resaltado.set(key);
      setTimeout(() => { if (this.resaltado() === key) this.resaltado.set(null); }, 2000);
    }, 60);
  }

  /** Acepta la corrección del estudiante en un criterio: retira su observación (queda subsanado). */
  aceptarCorreccion(key: string): void {
    if (this.data()?.cerrada) return;
    this.observaciones.set({ ...this.observaciones(), [key]: '' });
    const s = new Set(this.obsSet()); s.delete(key); this.obsSet.set(s);
    const env = new Set(this.enviadas()); env.delete(key); this.enviadas.set(env);
    const conf = new Set(this.confirmadas()); conf.delete(key); this.confirmadas.set(conf);
  }

  /** Descarta la observación del criterio y cierra su caja de texto. */
  quitarObs(key: string): void {
    this.aceptarCorreccion(key);
  }

  /**
   * Guarda esta observación puntual (requiere texto). Queda lista y colapsada, a la espera de
   * "Solicitar subsanación" — el botón que de verdad la envía, junto con las demás guardadas.
   */
  guardarObs(key: string): void {
    if (this.data()?.cerrada) return;
    if (!(this.observaciones()[key] ?? '').trim()) return;
    const conf = new Set(this.confirmadas()); conf.add(key); this.confirmadas.set(conf);
    const s = new Set(this.obsSet()); s.delete(key); this.obsSet.set(s);
  }

  // ── Observación por criterio + plegado de secciones ──
  setObs(key: string, texto: string): void {
    if (this.data()?.cerrada) return;
    this.observaciones.set({ ...this.observaciones(), [key]: texto });
  }
  obsOpen(k: string): boolean { return this.obsSet().has(k); }
  toggleObs(k: string): void {
    const s = new Set(this.obsSet());
    s.has(k) ? s.delete(k) : s.add(k);
    this.obsSet.set(s);
  }
  /**
   * Solo lo guardado (o ya enviado antes) viaja al servidor: un borrador a medio escribir, sin
   * pasar por "Guardar", no debe colarse en el envío solo porque el revisor tenía la caja abierta.
   */
  private observacionesParaEnviar(): Record<string, string> {
    const obs = this.observaciones();
    const out: Record<string, string> = {};
    Object.keys(obs).forEach((k) => {
      if (this.enviadas().has(k) || this.confirmadas().has(k)) out[k] = obs[k];
    });
    return out;
  }

  protected hayObservacion(): boolean {
    return Object.values(this.observacionesParaEnviar()).some((o) => (o ?? '').trim().length > 0);
  }

  /**
   * Texto escrito que nunca se guardó ni se descartó. Se revisa por el propio texto, no por si
   * la caja sigue abierta: si el revisor la cierra con el botón "Observar" en vez de "Guardar" o
   * "Descartar", el texto queda huérfano igual y no debe colarse (ni perderse en silencio) al enviar.
   */
  protected hayBorradorSinGuardar(): boolean {
    const obs = this.observaciones();
    return Object.keys(obs).some((k) =>
      !this.enviadas().has(k) && !this.confirmadas().has(k) && (obs[k] ?? '').trim().length > 0);
  }
  seccionCompleta(sec: any): boolean {
    const nv = this.niveles();
    return (sec.criterios ?? []).length > 0 && (sec.criterios ?? []).every((c: any) => !!nv[c.key]);
  }
  criteriosCalificados(sec: any): number {
    const nv = this.niveles();
    return (sec.criterios ?? []).filter((c: any) => !!nv[c.key]).length;
  }
  /** Auto-plegado: abierta si aún no está completa; el revisor puede forzar abrir/cerrar. */
  secAbierta(sec: any): boolean {
    const forced = this.secForzadas()[sec.key];
    if (forced !== undefined) return forced;
    return !this.seccionCompleta(sec);
  }
  toggleSec(sec: any): void {
    this.secForzadas.set({ ...this.secForzadas(), [sec.key]: !this.secAbierta(sec) });
  }

  /**
   * Un criterio calificado como Parcial/No cumple, pero sin observación guardada: sin ese texto
   * el ítem del editor no tiene a dónde llevar al estudiante (se queda con el estado que tenía
   * antes, a veces "conforme" de una etapa previa, y parece que no hay nada que corregir).
   */
  protected criteriosSinObservar(): any[] {
    const nv = this.niveles();
    const enviadasOGuardadas = new Set([...this.enviadas(), ...this.confirmadas()]);
    return this.criterios().filter((c: any) => nv[c.key] && nv[c.key] !== 'CUMPLE' && !enviadasOGuardadas.has(c.key));
  }

  evaluar(conforme: boolean): void {
    if (!this.completa() || this.guardando()) return;
    if (this.hayBorradorSinGuardar()) {
      this._toast.error('Guarda o descarta la observación que estás redactando antes de continuar'); return;
    }
    if (!conforme) {
      const faltantes = this.criteriosSinObservar();
      if (faltantes.length) {
        this._toast.error(`Falta escribir la observación de «${faltantes[0].titulo}» (lo calificaste sin Cumple del todo)`);
        this.irACriterio(faltantes[0].key);
        return;
      }
    }
    if (!conforme && !this.hayObservacion()) {
      this._toast.error('Agrega al menos una observación para que el estudiante subsane'); return;
    }
    // Si soy el último revisor pendiente, mi conformidad aprueba el proyecto: hay que decirlo.
    const ultimo = conforme && !!this.data()?.ultimoPendiente;
    this._confirm.confirmSave({
      title: conforme ? 'Dar conformidad al proyecto' : 'Solicitar la subsanación',
      message: conforme
        ? (ultimo
            ? 'Eres el último revisor que falta. Al confirmar:'
            : 'Registrarás tu conformidad con la rúbrica. Al confirmar:')
        : 'Guardarás tu rúbrica y las observaciones. Al confirmar:',
      details: conforme
        ? (ultimo
            ? ['el proyecto queda aprobado por los revisores',
               'pasa a la Secretaría para programar la defensa',
               'tu evaluación se cierra y ya no podrás observar']
            : ['tu evaluación se cierra y ya no podrás observar',
               'el proyecto sigue en espera del otro revisor'])
        : ['el proyecto vuelve al doctorando para que subsane',
           'podrás revisar de nuevo cuando levante las observaciones'],
      confirmLabel: conforme ? 'Sí, dar conformidad' : 'Solicitar subsanación',
    }).then(() => {
      this.guardando.set(true);
      this._svc.evaluar$(this.tesisId, this.niveles(), this.observacionesParaEnviar(), '', conforme).subscribe({
        next: () => { this._toast.success(conforme ? 'Conformidad registrada' : 'Subsanación solicitada'); this.guardando.set(false); this.cargar(); },
        error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo registrar la evaluación'); },
      });
    }).catch(() => {});
  }

  volver(): void { this._router.navigate(['/admin/revisor-proyecto']); }
}
