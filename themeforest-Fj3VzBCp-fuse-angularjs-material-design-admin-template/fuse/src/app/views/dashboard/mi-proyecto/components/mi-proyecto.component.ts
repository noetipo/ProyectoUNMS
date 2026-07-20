import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { Router } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { DocumentoPreviewDialogComponent } from '@/app/shared/perfil-completo/documento-preview-dialog.component';
import { environment } from '@/environments/environment';
import { MiProyectoService } from '../services/mi-proyecto.service';
import { ActividadDialogComponent } from './actividad-dialog.component';
import { PartidaDialogComponent } from './partida-dialog.component';
import { CorreccionDialogComponent } from './correccion-dialog.component';
import { ReferenciaDialogComponent } from './referencia-dialog.component';
import { CitaInsertarDialogComponent } from './cita-insertar-dialog.component';
import {
  ActividadItem, ESTADOS_ACTIVIDAD, FASES, FASE_ENUM, FASE_LABEL,
  FINANCIAMIENTOS, PROY_DEF, PartidaItem, ProyectoEditor, RUBROS, RevisionItem,
  SeccionDef, chipRevision, tipoDot, tipoVerbo, ganttMeses, ganttColFecha, rangoFechas,
  ReferenciaItem, ESTILOS_CITA, RevisorEval,
} from '../models/proyecto.model';

/** Editor "Proyecto de tesis en línea" del estudiante (Etapa 4), por pasos (wizard). */
@Component({
  selector: 'app-mi-proyecto',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule, MatMenuModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <button mat-button class="!text-slate-500 !text-xs !px-2 mb-1" (click)="volver()">
            <mat-icon svgIcon="chevron-left" class="size-3.5" /> Volver al expediente
          </button>
          <h1 class="page-title">Proyecto de tesis en línea</h1>
        </div>
        <div class="ml-auto flex items-center gap-2">
          <button mat-flat-button class="!h-8 !text-[11px] !rounded-lg !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" (click)="generarDoc('pdf')">
            <mat-icon svgIcon="file-down" class="size-3.5 mr-1" /> PDF
          </button>
          <button mat-stroked-button class="!h-8 !text-[11px] !rounded-lg !text-[#8C1D2E]" (click)="generarDoc('docx')">
            <mat-icon svgIcon="file-text" class="size-3.5 mr-1" /> Word
          </button>
          @if (esDev) {
            <span class="w-px h-5 bg-slate-200 mx-1"></span>
            <span class="text-[10px] font-bold tracking-wider text-amber-500">DEMO</span>
            <button mat-stroked-button class="!h-8 !text-[11px] !rounded-lg" (click)="seedDemo()">
              <mat-icon svgIcon="wand-sparkles" class="size-3.5 mr-1" /> Rellenar prueba
            </button>
            <button mat-stroked-button class="!h-8 !text-[11px] !rounded-lg !text-rose-600" (click)="resetDemo()">
              <mat-icon svgIcon="rotate-ccw" class="size-3.5 mr-1" /> Reiniciar flujo
            </button>
          }
        </div>
      </div>

      <!-- Menú de citas: inserta la cita en el texto tomada de las referencias -->
      <mat-menu #citaMenu="matMenu">
        <!-- Elegir formato antes de citar (con pista de para qué sirve cada uno) -->
        <div class="px-3 pt-2 pb-1.5" (click)="$event.stopPropagation()">
          <div class="text-[10px] font-bold tracking-wider text-slate-400 mb-1.5">
            FORMATO DE CITA
            @if (p()?.estiloCitaBloqueado) { <span class="inline-flex items-center gap-0.5 text-slate-400"><mat-icon svgIcon="lock" class="size-3" /> fijado</span> }
            <span class="font-normal normal-case">· pasa el mouse para ver para qué sirve</span>
          </div>
          <div class="flex flex-wrap gap-1 max-w-[320px]">
            @for (s of estilos; track s.v) {
              <button type="button" [title]="s.hint"
                      class="px-2 py-0.5 rounded text-[11px] font-semibold border transition"
                      [ngClass]="p()?.estiloCita === s.v ? 'bg-[#8C1D2E] text-white border-[#8C1D2E]' : (p()?.estiloCitaBloqueado ? 'text-slate-300 border-slate-100' : 'text-slate-600 border-slate-200 hover:bg-slate-50')"
                      (click)="setEstilo(s.v); $event.stopPropagation()">{{ s.l }}</button>
            }
          </div>
        </div>
        <div class="border-t border-slate-100 my-1"></div>
        <div class="px-3 py-0.5 text-[10px] font-bold tracking-wider text-slate-400">INSERTAR CITA</div>
        @for (ref of p()?.referencias ?? []; track ref.id ?? $index) {
          <button mat-menu-item class="!text-[12.5px]" [title]="ref.formateada" (click)="insertarCita(ref)">
            <span class="font-bold text-[#8C1D2E] mr-1.5">{{ ref.citaTexto }}</span>
            <span class="text-slate-500">{{ ref.autores || ref.titulo }}<span class="text-slate-400"> · {{ ref.anio }}</span></span>
          </button>
        }
        <button mat-menu-item class="!text-[12.5px] !text-[#8C1D2E] !font-semibold" (click)="nuevaCitaReferencia()">
          <mat-icon svgIcon="plus" class="size-4 mr-1" /> Agregar nueva referencia y citar…
        </button>
        @if (!(p()?.referencias?.length)) {
          <div class="px-3 py-1 text-[11px] text-slate-400" style="max-width:280px">Aún no tienes referencias. Agrégalas al citar y se listarán solas en la bibliografía.</div>
        }
      </mat-menu>

      <div class="page-content p-6">
        @if (p(); as e) {
          <div class="mx-auto max-w-[1080px] space-y-4">
            <!-- Cabecera: enfoque (radio) + avance + acciones -->
            <section class="form-card">
              <p class="text-[11px] text-slate-400 mb-3">Autoguardado por campo en la base de datos (tabla proyecto_campos) · estructura según enfoque</p>
              <div class="flex flex-wrap items-center gap-3">
                <div class="inline-flex rounded-xl border-2 border-slate-200 p-1">
                  <button type="button" class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-bold transition disabled:opacity-40 disabled:cursor-not-allowed"
                          [ngClass]="e.enfoque === 'CUANTITATIVO' ? 'bg-[#FDF6F7] text-[#8C1D2E]' : 'text-slate-500'"
                          [disabled]="e.enfoqueBloqueado && e.enfoque !== 'CUANTITATIVO'"
                          (click)="setEnfoque('CUANTITATIVO')">
                    <span class="size-4 rounded-full border-2 flex items-center justify-center"
                          [ngClass]="e.enfoque === 'CUANTITATIVO' ? 'border-[#8C1D2E]' : 'border-slate-300'">
                      @if (e.enfoque === 'CUANTITATIVO') { <span class="size-2 rounded-full bg-[#8C1D2E]"></span> }
                    </span>
                    Cuantitativo / mixto
                  </button>
                  <button type="button" class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-bold transition disabled:opacity-40 disabled:cursor-not-allowed"
                          [ngClass]="e.enfoque === 'CUALITATIVO' ? 'bg-[#FDF6F7] text-[#8C1D2E]' : 'text-slate-500'"
                          [disabled]="e.enfoqueBloqueado && e.enfoque !== 'CUALITATIVO'"
                          (click)="setEnfoque('CUALITATIVO')">
                    <span class="size-4 rounded-full border-2 flex items-center justify-center"
                          [ngClass]="e.enfoque === 'CUALITATIVO' ? 'border-[#8C1D2E]' : 'border-slate-300'">
                      @if (e.enfoque === 'CUALITATIVO') { <span class="size-2 rounded-full bg-[#8C1D2E]"></span> }
                    </span>
                    Cualitativo
                  </button>
                </div>
                @if (e.enfoqueBloqueado) {
                  <button mat-button class="!h-8 !text-[11px] !text-slate-500 !px-2" (click)="cambiarEnfoque()">
                    <mat-icon svgIcon="lock" class="size-3.5 mr-1" /> Cambiar enfoque
                  </button>
                }
                <div class="flex items-center gap-2 flex-1 min-w-[160px]">
                  <div class="h-1.5 flex-1 rounded bg-slate-100 overflow-hidden">
                    <div class="h-full rounded" [ngClass]="e.avancePct >= 100 ? 'bg-emerald-500' : 'bg-[#8C1D2E]'" [style.width.%]="e.avancePct"></div>
                  </div>
                  <span class="text-lg font-extrabold" [ngClass]="e.avancePct >= 100 ? 'text-emerald-600' : 'text-[#8C1D2E]'">{{ e.avancePct }}%</span>
                </div>
                @if (conformes() > 0) {
                  <span class="px-2 py-1 rounded-md text-[11px] font-bold bg-emerald-100 text-emerald-700">{{ conformes() }} conformes</span>
                }
                @if (e.estado === 'OBSERVADO') {
                  <button mat-flat-button color="primary" class="!h-9 !text-sm"
                          [disabled]="observacionesPendientes() > 0"
                          [title]="observacionesPendientes() > 0 ? 'Corrige todas las observaciones antes de reenviar' : ''"
                          (click)="reenviar()">
                    <mat-icon svgIcon="send" class="size-3.5 mr-1" /> Reenviar a revisión
                    @if (observacionesPendientes() > 0) { <span class="ml-1 font-normal opacity-90">({{ observacionesPendientes() }} sin corregir)</span> }
                  </button>
                } @else {
                  <button mat-flat-button color="primary" class="!h-9 !text-sm" [disabled]="!e.puedeMarcarListo || e.listoRevision" (click)="marcarListo()">
                    {{ e.listoRevision ? '✓ Enviado a revisión' : 'Marcar listo para revisión' }}
                  </button>
                }
              </div>
            </section>

            <!-- Aviso: dónde faltan corregir observaciones -->
            @if (observacionesPendientes() > 0) {
              <section class="rounded-xl border border-amber-200 bg-amber-50 p-3.5">
                <p class="text-[12px] font-bold text-amber-700 mb-2">
                  <mat-icon svgIcon="flag" class="size-3.5 mr-1 align-text-bottom" /> Tienes {{ observacionesPendientes() }} observación(es) sin corregir. Ve a cada ítem, corrígelo y pulsa "Marcar como corregido":
                </p>
                <div class="flex flex-wrap gap-2">
                  @for (it of itemsPendientes(); track it.campo) {
                    <button type="button" class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-semibold bg-white border border-amber-200 text-amber-700 hover:bg-amber-100"
                            (click)="irAItem(it)">
                      <span class="text-amber-400">{{ it.seccion }} ·</span> {{ it.label }}
                      <mat-icon svgIcon="arrow-right" class="size-3" />
                    </button>
                  }
                </div>
              </section>
            }

            <!-- Etapa 5: defensa programada -->
            @if (e.defensaProgramada) {
              <section class="rounded-xl border border-[#8C1D2E]/25 bg-[#FDF6F7] p-4">
                <div class="flex items-center gap-2 mb-1.5">
                  <mat-icon svgIcon="calendar-check" class="size-4 text-[#8C1D2E]" />
                  <p class="text-[13px] font-bold text-[#8C1D2E]">Defensa del proyecto programada</p>
                </div>
                <p class="text-[13px] text-slate-700">
                  {{ e.fechaDefensa | date:'EEEE d \\'de\\' MMMM \\'de\\' y' }}<span *ngIf="e.horaDefensa"> · {{ e.horaDefensa }}</span><span *ngIf="e.lugarDefensa"> · {{ e.lugarDefensa }}</span>
                </p>
                @if (e.dictamenNumero) { <p class="text-[11px] text-slate-400 mt-0.5 font-mono">{{ e.dictamenNumero }}</p> }
              </section>
            }

            <!-- Etapa 6: ejecución de la tesis (informe final + avances) -->
            @if (e.defensaProgramada) {
              <section class="rounded-xl border border-slate-200 bg-white p-4">
                <div class="flex items-center gap-2 mb-2">
                  <mat-icon svgIcon="rocket" class="size-4 text-[#8C1D2E]" />
                  <p class="text-[13px] font-bold text-slate-700">Ejecución de la tesis · Informe final</p>
                </div>
                @if (e.informeFinalAprobado) {
                  <div class="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-[12.5px] text-emerald-700 flex items-center gap-1.5">
                    <mat-icon svgIcon="badge-check" class="size-4 shrink-0" /> Tu asesor aprobó el informe final.
                  </div>
                  <!-- Etapa 7: solicitar Jurado Informante -->
                  <div class="mt-2">
                    @if (!e.juradoInformanteSolicitado) {
                      <button mat-flat-button class="!h-8 !text-[12px] !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" (click)="solicitarJurado()">
                        <mat-icon svgIcon="send" class="size-3.5 mr-1" /> Solicitar Jurado Informante
                      </button>
                    } @else if (e.informeFinalRevisado) {
                      <div class="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-[12.5px] text-emerald-700 flex items-center gap-1.5">
                        <mat-icon svgIcon="badge-check" class="size-4 shrink-0" /> El Jurado Informante aprobó tu informe final. Puedes continuar con el trámite de expedito.
                      </div>
                    } @else {
                      <span class="text-[11px] text-slate-400">Jurado Informante solicitado — en evaluación.</span>
                    }
                    <!-- Evaluaciones del Jurado Informante -->
                    @if (e.evaluacionesJuradoInforme?.length) {
                      <div class="mt-2 space-y-1.5">
                        @for (jr of e.evaluacionesJuradoInforme; track jr.id) {
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
                } @else {
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="px-2 py-0.5 rounded text-[11px] font-bold" [ngClass]="e.informeFinalSubido ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'">
                      {{ e.informeFinalSubido ? 'INFORME SUBIDO' : 'SIN SUBIR' }}
                    </span>
                    <button mat-stroked-button class="!h-8 !text-[12px] !text-[#8C1D2E]" (click)="informeInput.click()">
                      <mat-icon svgIcon="upload" class="size-3.5 mr-1" /> {{ e.informeFinalSubido ? 'Reemplazar informe final' : 'Subir informe final (PDF)' }}
                    </button>
                    <input #informeInput type="file" accept="application/pdf" class="hidden" (change)="subirInforme($event)" />
                  </div>
                }
                @if (e.avances?.length) {
                  <div class="mt-3">
                    <p class="text-[11px] font-bold text-slate-400 mb-1.5">EVALUACIONES DE AVANCE DEL ASESOR</p>
                    <div class="space-y-1.5">
                      @for (av of e.avances; track av.id) {
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

            <!-- Etapa 5: revisores designados y estado de la evaluación -->
            @if (e.evaluacionesRevisores?.length) {
              <section class="rounded-xl border border-slate-200 bg-white p-4">
                <p class="text-[13px] font-bold text-slate-700">Revisores designados y estado de la evaluación</p>
                <p class="text-[11.5px] text-slate-400 mb-3">Cada revisor evalúa con la rúbrica oficial (en línea o escaneada) · plazo 15 días útiles.</p>
                <div class="space-y-2.5">
                  @for (rv of e.evaluacionesRevisores; track rv.revisorId) {
                    <div class="rounded-xl border border-slate-100 p-3">
                      <div class="flex items-start gap-3">
                        <span class="size-9 shrink-0 rounded-full bg-slate-100 text-slate-500 text-[12px] font-bold flex items-center justify-center">{{ iniciales(rv.docenteNombre) }}</span>
                        <div class="flex-1 min-w-0">
                          <p class="text-[13px] font-bold text-slate-800 leading-tight">{{ rv.docenteNombre || ('Revisor ' + rv.orden) }}</p>
                          <p class="text-[11.5px] text-slate-400 leading-tight">
                            {{ rv.docenteCategoria || '—' }}<span *ngIf="rv.docenteLinea"> · {{ rv.docenteLinea }}</span>
                          </p>
                        </div>
                        @if (rv.puntajeTotal != null) { <span class="text-[10.5px] text-slate-400 shrink-0">{{ rv.puntajeTotal }}/20</span> }
                      </div>
                      <div class="mt-2">
                        <span class="px-2 py-0.5 rounded-md text-[10.5px] font-bold" [ngClass]="revEstadoBadge(rv.estado)">{{ revEstadoTexto(rv.estado) }}</span>
                      </div>
                      @if (rv.comentario) {
                        <p class="text-[12px] text-slate-600 mt-2"><span class="font-semibold text-rose-500">Observación:</span> {{ rv.comentario }}</p>
                      }
                      @if (rv.respuesta) {
                        <p class="text-[12px] text-slate-700 mt-1 rounded-md bg-sky-50 border border-sky-100 px-2 py-1.5"><span class="font-semibold text-sky-600">Tu respuesta:</span> {{ rv.respuesta }}</p>
                      }
                      @if (rv.estado === 'OBSERVADO' && !rv.respuesta) {
                        <button mat-flat-button class="!h-7 !text-[11px] mt-2 !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" (click)="responderRevisor(rv)">
                          <mat-icon svgIcon="reply" class="size-3.5 mr-1" /> Levantar observación
                        </button>
                      }
                    </div>
                  }
                </div>
                @if (e.revisoresConformes) {
                  <div class="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-[12.5px] text-emerald-700 flex items-center gap-1.5">
                    <mat-icon svgIcon="badge-check" class="size-4 shrink-0" /> Proyecto aprobado por los revisores — listo para la defensa.
                  </div>
                }
              </section>
            }

            <!-- Índice lateral (Estructura del proyecto) + contenido -->
            <div class="grid grid-cols-1 lg:grid-cols-[214px_1fr] gap-4 items-start">
              <aside class="lg:sticky lg:top-4 self-start rounded-xl border border-slate-200 bg-white p-2">
                <div class="text-[10px] font-extrabold tracking-wider text-slate-400 px-2.5 pt-1.5 pb-2">ESTRUCTURA DEL PROYECTO</div>
                @for (sec of secciones(); track sec.id; let i = $index) {
                  <button type="button" class="w-full flex items-center gap-2 px-2.5 py-2 rounded-lg text-left transition"
                          [ngClass]="paso() === i ? 'bg-[#8C1D2E] text-white' : 'text-slate-600 hover:bg-slate-50'"
                          (click)="irA(i)">
                    <span class="size-[7px] rounded-full shrink-0" [style.background]="paso() === i ? '#ffffff' : dotColor(sec)"></span>
                    <span class="flex-1 text-[11.5px] font-semibold truncate">{{ sec.titulo }}</span>
                    <span class="text-[10px] font-extrabold font-mono" [ngClass]="paso() === i ? 'text-white/85' : (stat(sec).full ? 'text-emerald-600' : 'text-slate-400')">{{ stat(sec).fil }}/{{ stat(sec).tot }}</span>
                  </button>
                }
                <button type="button" class="w-full flex items-center gap-2 px-2.5 py-2 rounded-lg text-left transition"
                        [ngClass]="esCierre() ? 'bg-[#8C1D2E] text-white' : 'text-slate-600 hover:bg-slate-50'"
                        (click)="irA(secciones().length)">
                  <mat-icon svgIcon="flag" class="size-3.5 shrink-0" [ngClass]="esCierre() ? 'text-white' : 'text-slate-400'" />
                  <span class="flex-1 text-[11.5px] font-semibold">Cierre y envío</span>
                </button>
                <div class="mx-1.5 mt-2 mb-1 px-3 py-2 rounded-lg bg-slate-50 text-[10.5px] text-slate-500 leading-relaxed">
                  Cada campo genera un <b>UPDATE</b> individual al dejar de escribir — nada se pierde al salir.
                </div>
              </aside>

              <div class="space-y-4 min-w-0">

            <!-- Contenido del paso actual -->
            @if (secActual(); as sec) {
              <section class="form-card">
                <header class="form-card__head">
                  <h2 class="form-card__title !text-[#8C1D2E]">{{ sec.titulo }}</h2>
                  @if (sec.sub) { <p class="text-[11px] text-slate-400">{{ sec.sub }}</p> }
                </header>

                <div class="space-y-4">
                  @for (c of sec.campos; track c.k) {
                    <div>
                      <div class="flex items-center gap-2 mb-1">
                        <label class="form-label !mb-0">{{ c.l }}</label>
                        @if (!c.input && c.k !== 'hipotesis' && c.k !== 'referencias') {
                          <button type="button" [matMenuTriggerFor]="citaMenu" (click)="citaField.set(c.k)"
                                  class="ml-auto flex items-center gap-1 text-[11px] font-semibold text-[#8C1D2E] hover:underline">
                            <mat-icon svgIcon="quote" class="size-3.5" /> Insertar cita
                          </button>
                          <span class="px-1.5 py-0.5 rounded text-[10px] font-bold" [ngClass]="chip(c.k).cls">{{ chip(c.k).label }}</span>
                        } @else {
                          <span class="ml-auto px-1.5 py-0.5 rounded text-[10px] font-bold" [ngClass]="chip(c.k).cls">{{ chip(c.k).label }}</span>
                        }
                      </div>
                      @if (c.k === 'hipotesis') {
                        <!-- Hipótesis como lista repetible (H1, H2, …), igual que objetivos -->
                        <div class="space-y-2">
                          @for (h of hipotesis(); track $index; let i = $index) {
                            <div class="flex items-center gap-2">
                              <span class="text-[11px] text-slate-400 w-9 shrink-0">H{{ i + 1 }}</span>
                              <input class="flex-1 rounded-lg border border-slate-200 px-3 py-1.5 text-sm focus:outline-none focus:border-[#8C1D2E]"
                                     [value]="h" (blur)="saveHip(i, $event)" placeholder="Hipótesis de la investigación" />
                              <button mat-icon-button class="!size-7" (click)="delHip(i)"><mat-icon svgIcon="trash" class="size-4 text-rose-400" /></button>
                            </div>
                          }
                          @if (!hipotesis().length) { <p class="text-[12px] text-slate-400">Aún no has agregado hipótesis.</p> }
                          <button mat-button class="!h-7 !text-[11px] !text-[#8C1D2E]" (click)="addHip()"><mat-icon svgIcon="plus" class="size-3.5 mr-1" /> Agregar hipótesis</button>
                        </div>
                      } @else if (c.k === 'referencias') {
                        <!-- Gestor de referencias estructuradas (auto-formato APA/Vancouver/IEEE) -->
                        <div class="space-y-3">
                          <div class="flex items-center gap-2 flex-wrap">
                            <span class="text-[11px] text-slate-400">Estilo de cita:</span>
                            @for (s of estilos; track s.v) {
                              <button type="button" [title]="s.hint" class="px-2.5 py-1 rounded-lg text-[11px] font-bold border transition"
                                      [ngClass]="e.estiloCita === s.v ? 'bg-[#FDF6F7] text-[#8C1D2E] border-[#8C1D2E]/30' : (e.estiloCitaBloqueado ? 'text-slate-300 border-slate-100' : 'text-slate-500 border-slate-200 hover:bg-slate-50')"
                                      (click)="setEstilo(s.v)">{{ s.l }}</button>
                            }
                            @if (e.estiloCitaBloqueado) {
                              <span class="inline-flex items-center gap-1 text-[10px] text-slate-400" title="Un documento usa un solo estilo. Para cambiarlo, haz clic en otro y confirma el desbloqueo.">
                                <mat-icon svgIcon="lock" class="size-3" /> fijado
                              </span>
                            }
                          </div>
                          <div class="space-y-2">
                            @for (ref of e.referencias ?? []; track ref.id ?? $index; let i = $index) {
                              <div class="flex items-start gap-2 rounded-lg border border-slate-100 bg-slate-50 px-3 py-2">
                                <span class="text-[11px] font-mono text-slate-400 shrink-0 mt-0.5">{{ (e.estiloCita === 'VANCOUVER' || e.estiloCita === 'IEEE') ? '[' + (i + 1) + ']' : '•' }}</span>
                                <p class="flex-1 text-[12.5px] text-slate-700 leading-snug">{{ ref.formateada || '(referencia incompleta)' }}</p>
                                <button mat-icon-button class="!size-7 shrink-0" (click)="editReferencia(ref)"><mat-icon svgIcon="pencil" class="size-3.5 text-slate-400" /></button>
                                <button mat-icon-button class="!size-7 shrink-0" (click)="delReferencia(ref)"><mat-icon svgIcon="trash" class="size-4 text-rose-400" /></button>
                              </div>
                            }
                            @if (!(e.referencias?.length)) { <p class="text-[12px] text-slate-400">Aún no has agregado referencias.</p> }
                          </div>
                          <button mat-button class="!h-7 !text-[11px] !text-[#8C1D2E]" (click)="addReferencia()"><mat-icon svgIcon="plus" class="size-3.5 mr-1" /> Agregar referencia</button>
                        </div>
                      } @else if (c.input) {
                        <input class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E]"
                               [value]="val(c.k)" [placeholder]="c.ph ?? ''" (blur)="guardar(c.k, $event)" />
                      } @else {
                        <textarea class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E]"
                                  [rows]="c.rows ?? 3" [value]="val(c.k)" [placeholder]="c.ph ?? ''"
                                  (focus)="onCitaFocus(c.k, $event)" (blur)="guardar(c.k, $event)"></textarea>
                      }
                      @if (rev(c.k)?.eventos?.length) {
                        <div class="mt-1.5">
                          <div class="flex items-center gap-3 flex-wrap">
                            <button type="button" class="flex items-center gap-1 text-[11px] font-semibold text-[#8C1D2E] hover:underline" (click)="toggleHist(c.k)">
                              <mat-icon [svgIcon]="histOpen(c.k) ? 'chevron-down' : 'chevron-right'" class="size-3.5" /> {{ histOpen(c.k) ? 'Ocultar' : 'Ver' }} historial ({{ rev(c.k)!.eventos.length }})
                            </button>
                            @if (rev(c.k)?.estado === 'OBSERVADO' || rev(c.k)?.estado === 'EN_CORRECCION') {
                              <button type="button" class="flex items-center gap-1 text-[11px] font-semibold text-emerald-600 hover:underline" (click)="corregir(c.k, c.l)">
                                <mat-icon svgIcon="check" class="size-3.5" /> Marcar como corregido
                              </button>
                            }
                          </div>
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
                    </div>
                  }
                </div>

                @if (sec.wObj) {
                  <div class="mt-4 pt-4 border-t border-slate-100">
                    <div class="flex items-center justify-between mb-2">
                      <label class="form-label !mb-0">Objetivos específicos</label>
                      <button mat-button class="!h-7 !text-[11px] !text-[#8C1D2E]" (click)="addObjetivo()"><mat-icon svgIcon="plus" class="size-3.5 mr-1" /> Agregar objetivo</button>
                    </div>
                    <div class="space-y-2">
                      @for (o of e.objetivos; track o.id ?? $index; let i = $index) {
                        <div class="flex items-center gap-2">
                          <span class="text-[11px] text-slate-400 w-9 shrink-0">OE{{ i + 1 }}</span>
                          <input class="flex-1 rounded-lg border border-slate-200 px-3 py-1.5 text-sm focus:outline-none focus:border-[#8C1D2E]"
                                 [value]="o.texto ?? ''" (blur)="saveObjetivo(o, $event)" placeholder="Objetivo específico" />
                          <button mat-icon-button class="!size-7" (click)="delObjetivo(o)"><mat-icon svgIcon="trash" class="size-4 text-rose-400" /></button>
                        </div>
                      }
                      @if (!e.objetivos.length) { <p class="text-[12px] text-slate-400">Aún no has agregado objetivos específicos.</p> }
                    </div>
                  </div>
                }

                @if (sec.wPlan) {
                  <div class="mt-4 pt-4 border-t border-slate-100 space-y-5">

                    <!-- Revisión del plan de actividades (mini-historial) -->
                    @if (rev('plan')?.eventos?.length) {
                      <div class="rounded-lg border border-slate-100 p-3">
                        <div class="flex items-center gap-2 flex-wrap mb-1">
                          <span class="text-[11px] font-bold text-slate-500">Revisión del plan de actividades</span>
                          <span class="px-1.5 py-0.5 rounded text-[10px] font-bold" [ngClass]="chip('plan').cls">{{ chip('plan').label }}</span>
                        </div>
                        <div class="flex items-center gap-3 flex-wrap">
                          <button type="button" class="flex items-center gap-1 text-[11px] font-semibold text-[#8C1D2E] hover:underline" (click)="toggleHist('plan')">
                            <mat-icon [svgIcon]="histOpen('plan') ? 'chevron-down' : 'chevron-right'" class="size-3.5" /> {{ histOpen('plan') ? 'Ocultar' : 'Ver' }} historial ({{ rev('plan')!.eventos.length }})
                          </button>
                          @if (rev('plan')?.estado === 'OBSERVADO' || rev('plan')?.estado === 'EN_CORRECCION') {
                            <button type="button" class="flex items-center gap-1 text-[11px] font-semibold text-emerald-600 hover:underline" (click)="corregir('plan', 'Plan de actividades')">
                              <mat-icon svgIcon="check" class="size-3.5" /> Marcar como corregido
                            </button>
                          }
                        </div>
                        @if (histOpen('plan')) {
                          <ol class="mt-2 ml-1.5 pl-4 border-l border-slate-200 space-y-3">
                            @for (ev of eventos('plan'); track $index) {
                              <li class="relative">
                                <span class="absolute -left-[21px] top-[3px] size-2.5 rounded-full ring-2 ring-white" [ngClass]="tipoDot(ev.tipo)"></span>
                                <div class="flex items-baseline gap-1.5 flex-wrap leading-tight">
                                  <b class="text-[11.5px] text-slate-700">{{ ev.autor }}</b>
                                  <span class="text-[11px] text-slate-500">{{ tipoVerbo(ev.tipo) }}</span>
                                  <span class="text-[10px] text-slate-400">· {{ ev.fecha }}</span>
                                </div>
                                @if (ev.texto) { <p class="text-[12px] mt-1 whitespace-pre-wrap" [ngClass]="ev.tipo === 'CORRECCIÓN' ? 'text-slate-700 rounded-md bg-sky-50 border border-sky-100 px-2 py-1.5' : 'text-slate-600'">{{ ev.texto }}</p> }
                              </li>
                            }
                          </ol>
                        }
                      </div>
                    }

                    <!-- Cronograma de actividades -->
                    <div class="rounded-xl border border-slate-200 p-4">
                      <div class="flex flex-wrap items-center gap-x-3 gap-y-1 mb-2">
                        <h3 class="text-sm font-bold text-slate-700">Cronograma de actividades <span class="font-normal text-slate-400">· {{ meses().length || 0 }} meses</span></h3>
                        <span class="ml-auto text-[11px] text-slate-500">{{ e.actividades.length }} actividades · {{ actHechas() }} hechas · {{ actEnCurso() }} en curso</span>
                        <span class="text-[10px] text-slate-300 font-mono">proyecto_actividades</span>
                      </div>
                      <div class="flex items-center gap-2 mb-2">
                        @if (!e.planPublicado) {
                          <button mat-stroked-button class="!h-8 !text-xs !text-[#8C1D2E]" (click)="abrirActividad()">
                            <mat-icon svgIcon="plus" class="size-3.5 mr-1" /> Agregar actividad
                          </button>
                        }
                        <button mat-button class="!h-8 !text-[11px] !text-[#8C1D2E] ml-auto" [disabled]="e.planPublicado" (click)="publicarPlan()">
                          {{ e.planPublicado ? '✓ Plan publicado' : 'Publicar plan' }}
                        </button>
                      </div>
                      @if (e.planPublicado) {
                        <p class="text-[11px] text-slate-600 bg-slate-50 border border-slate-200 rounded-lg px-3 py-2 mb-3 flex items-center gap-1.5">
                          <mat-icon svgIcon="lock" class="size-3.5 text-slate-400 shrink-0" /> <span>Plan publicado — el cronograma quedó <b>congelado</b>. Solo puedes actualizar el <b>estado</b> de cada actividad (haz clic en él) para el seguimiento durante la ejecución.</span>
                        </p>
                      } @else {
                        <p class="text-[11px] text-amber-700 bg-amber-50 rounded-lg px-3 py-2 mb-3">
                          💡 Estas actividades se convierten en el <b>plan de actividades</b> que el asesor monitorea (%) y el tutor supervisa durante la ejecución (Etapa 6). Al <b>publicar el plan</b> el cronograma queda fijo y solo podrás actualizar el estado de cada actividad.
                        </p>
                      }

                      <div class="overflow-x-auto">
                        <div class="min-w-[640px]">
                          <div class="grid grid-cols-[minmax(150px,32%)_1fr_88px_24px] items-end gap-2 pb-1 border-b border-slate-100">
                            <span class="text-[10px] font-bold tracking-wide text-slate-400">ACTIVIDAD · FASE</span>
                            <div class="grid" [style.grid-template-columns]="'repeat(' + (meses().length || 1) + ', 1fr)'">
                              @for (m of meses(); track m.idx) { <span class="text-[8px] text-center text-slate-300 font-mono">{{ m.label }}</span> }
                            </div>
                            <span class="text-[10px] font-bold tracking-wide text-slate-400 text-center">ESTADO</span>
                            <span></span>
                          </div>
                          @for (a of e.actividades; track a.id ?? $index) {
                            <div class="grid grid-cols-[minmax(150px,32%)_1fr_88px_24px] items-center gap-2 py-2 border-b border-slate-50">
                              <div>
                                <p class="text-[12px] text-slate-700 leading-tight">{{ a.nombre }}</p>
                                <div class="flex items-center gap-2 mt-1">
                                  <span class="inline-block px-1.5 py-0.5 rounded text-[9px] font-bold" [style.background]="faseBg(a.fase)" [style.color]="faseFg(a.fase)">{{ faseLabel(a.fase) }}</span>
                                  @if (rangoFechas(a); as r) { <span class="text-[10px] text-slate-400">{{ r }}</span> }
                                </div>
                              </div>
                              <div class="grid h-2.5 rounded bg-slate-100" [style.grid-template-columns]="'repeat(' + (meses().length || 1) + ', 1fr)'">
                                @if (ganttCol(a); as gc) {
                                  <div class="h-full rounded" [style.grid-column]="gc" [style.background]="faseFg(a.fase)"></div>
                                }
                              </div>
                              <button type="button" class="px-2 py-1 rounded-md text-[10px] font-bold text-center hover:opacity-80" [ngClass]="estadoCls(a.estado)" (click)="cicloEstado(a)">{{ estadoLabel(a.estado) }}</button>
                              @if (!e.planPublicado) {
                                <button mat-icon-button class="!size-6" (click)="delActividad(a)"><mat-icon svgIcon="x" class="size-4 text-slate-400" /></button>
                              } @else { <span></span> }
                            </div>
                          }
                          @if (!e.actividades.length) { <p class="text-[12px] text-slate-400 py-3">Aún no hay actividades en el cronograma.</p> }
                        </div>
                      </div>
                    </div>

                    <!-- Presupuesto por partidas -->
                    <div class="rounded-xl border border-slate-200 p-4">
                      <div class="flex flex-wrap items-center gap-x-3 gap-y-1 mb-3">
                        <h3 class="text-sm font-bold text-slate-700">Presupuesto por partidas</h3>
                        <span class="text-[10px] text-slate-300 font-mono ml-auto">proyecto_presupuesto</span>
                        <button mat-stroked-button class="!h-8 !text-xs !text-[#8C1D2E]" (click)="abrirPartida()">
                          <mat-icon svgIcon="plus" class="size-3.5 mr-1" /> Agregar partida
                        </button>
                      </div>
                      <div class="flex flex-wrap items-center gap-2 mb-3">
                        <span class="text-[11px] text-slate-400">Financiamiento:</span>
                        <select class="rounded border border-slate-200 px-2 py-1 text-xs" [value]="e.financiamiento" (change)="changeFinanc($event)">
                          @for (f of financiamientos; track f) { <option [value]="f">{{ f }}</option> }
                        </select>
                        <span class="ml-auto text-sm font-bold text-slate-600">TOTAL <span class="text-[#8C1D2E]">S/ {{ e.presupuestoTotal | number:'1.2-2' }}</span></span>
                      </div>

                      <div class="overflow-x-auto">
                        <table class="w-full text-[12.5px]">
                          <thead><tr class="text-slate-400 text-left"><th class="py-1 font-medium">Rubro</th><th class="py-1 font-medium">Descripción</th><th class="py-1 font-medium text-right">Monto (S/)</th><th></th></tr></thead>
                          <tbody>
                            @for (pa of e.partidas; track pa.id ?? $index) {
                              <tr class="border-t border-slate-100">
                                <td class="py-1 pr-2 text-slate-600">{{ pa.rubro }}</td>
                                <td class="py-1 pr-2 text-slate-500">{{ pa.descripcion }}</td>
                                <td class="py-1 pr-2 text-right text-slate-700">{{ pa.monto | number:'1.2-2' }}</td>
                                <td class="py-1 text-right"><button mat-icon-button class="!size-6" (click)="delPartida(pa)"><mat-icon svgIcon="x" class="size-4 text-slate-400" /></button></td>
                              </tr>
                            }
                            @if (!e.partidas.length) { <tr><td colspan="4" class="py-3 text-[12px] text-slate-400">Aún no hay partidas.</td></tr> }
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
                }

                @if (sec.wMatriz) {
                  <div class="mt-4 pt-4 border-t border-slate-100">
                    <div class="rounded-xl border border-slate-100 bg-slate-50/40 p-3.5">
                      <div class="flex items-center gap-2 mb-2.5">
                        <span class="text-[12px] font-bold text-slate-700 flex-1">Matriz de consistencia <span class="font-normal text-slate-400">· se genera automáticamente con lo que ya escribiste</span></span>
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
                  </div>
                }
              </section>
            } @else {
              <!-- Paso final: Cierre y envío (Turnitin + solicitud) -->
              <section class="form-card">
                <header class="form-card__head"><h2 class="form-card__title">Turnitin — informe de similitud</h2>
                  <p class="text-[11px] text-slate-400">Índice máximo referencial: 20% (excluye citas, referencias y cadenas &lt; 8 palabras).</p></header>
                <div class="flex flex-wrap items-center gap-3">
                  <input type="number" min="0" max="100" class="w-24 rounded border border-slate-200 px-2 py-1.5 text-sm" placeholder="% similitud" [(ngModel)]="turnPct" />
                  <button mat-flat-button color="primary" class="!h-9 !text-sm" (click)="fileTurn.click()">
                    <mat-icon svgIcon="upload" class="size-3.5 mr-1" /> {{ e.turnitinSubido ? 'Reemplazar informe' : 'Subir informe (PDF)' }}
                  </button>
                  <input #fileTurn type="file" hidden accept=".pdf,.docx" (change)="onTurnitin($event)" />
                  @if (e.turnitinSubido) {
                    <span class="text-[12px] px-2 py-1 rounded" [ngClass]="(e.porcentajeSimilitud ?? 0) <= 20 ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'">
                      Similitud: {{ e.porcentajeSimilitud ?? 0 }}%
                    </span>
                    <button mat-icon-button class="!size-8" title="Ver informe enviado" (click)="verDoc('turnitin')"><mat-icon svgIcon="eye" class="size-4 text-slate-500" /></button>
                  }
                </div>
              </section>

              <section class="form-card">
                <header class="form-card__head"><h2 class="form-card__title">Expediente y solicitud de aprobación</h2></header>
                <!-- Proyecto versión final (PDF) -->
                <div class="flex flex-wrap items-center gap-3 mb-3">
                  <button mat-stroked-button class="!h-9 !text-sm !text-[#8C1D2E]" (click)="fileFinal.click()">
                    <mat-icon svgIcon="upload" class="size-3.5 mr-1" /> {{ e.proyectoFinalSubido ? 'Reemplazar proyecto final' : 'Subir proyecto versión final (PDF)' }}
                  </button>
                  <input #fileFinal type="file" hidden accept=".pdf,.docx" (change)="onProyectoFinal($event)" />
                  @if (e.proyectoFinalSubido) {
                    <span class="text-[12px] px-2 py-1 rounded bg-emerald-50 text-emerald-700">Proyecto final cargado ✓</span>
                    <button mat-icon-button class="!size-8" title="Ver proyecto final enviado" (click)="verDoc('proyecto-final')"><mat-icon svgIcon="eye" class="size-4 text-slate-500" /></button>
                  }
                </div>

                <ul class="space-y-1.5 text-[13px] mb-3">
                  <li>{{ check(e.listoRevision) }} Proyecto marcado listo para revisión</li>
                  <li>{{ check(e.cartaAsesor) }} Carta de opinión favorable del asesor</li>
                  <li>{{ check(e.turnitinSubido) }} Informe de Turnitin</li>
                  <li>{{ check(e.proyectoFinalSubido) }} Proyecto versión final</li>
                  <li>{{ check(e.expedienteSubido) }} Solicitud de aprobación generada</li>
                </ul>
                <button mat-flat-button color="primary" class="!h-9 !text-sm"
                        [disabled]="!e.cartaAsesor || !e.turnitinSubido || !e.proyectoFinalSubido || e.expedienteSubido" (click)="solicitar()">
                  <mat-icon svgIcon="send" class="size-3.5 mr-1" /> {{ e.expedienteSubido ? '✓ Solicitud enviada' : 'Solicitud de aprobación' }}
                </button>
                @if (!e.cartaAsesor) { <p class="text-[11px] text-slate-400 mt-1">Disponible cuando el asesor emita su carta de opinión favorable.</p> }
                @if (e.expedienteSubido) {
                  <div class="mt-2 rounded-lg px-3 py-2 text-[12px]" [ngClass]="e.expedienteRecibido ? 'bg-emerald-50 border border-emerald-200 text-emerald-700' : 'bg-amber-50 border border-amber-200 text-amber-700'">
                    @if (e.expedienteRecibido) {
                      ✓ <b>Recibido por la Secretaría.</b> Tu proyecto pasó a la <b>Etapa 5 · Defensa del proyecto</b>; el Coordinador designará los revisores.
                    } @else {
                      ⏳ <b>Solicitud enviada.</b> Esperando que la Secretaría recepcione tu expediente.
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
              @if (!esCierre()) {
                <button mat-flat-button color="primary" class="!h-9 !text-sm !rounded-lg" (click)="siguiente()">
                  Siguiente <mat-icon svgIcon="chevron-right" class="size-4" />
                </button>
              } @else {
                <span class="w-[92px]"></span>
              }
            </div>
              </div>
            </div>

            <!-- Historial de cambios y correcciones (colapsable, discreto) -->
            @if (historial().length) {
              <details class="rounded-xl border border-slate-200 bg-white">
                <summary class="cursor-pointer select-none px-4 py-2.5 text-[12px] font-semibold text-slate-500 hover:text-slate-700 flex items-center gap-2">
                  <mat-icon svgIcon="history" class="size-3.5" /> Historial de cambios y correcciones ({{ historial().length }})
                </summary>
                <div class="px-4 pb-3 pt-1 border-t border-slate-100 space-y-1.5 max-h-72 overflow-y-auto">
                  @for (h of historial(); track $index) {
                    <div class="text-[11px] leading-relaxed">
                      <span class="text-slate-400 font-mono">{{ h.fecha }}</span>
                      <span class="font-bold" [ngClass]="tipoCls(h.tipo)"> · {{ h.tipo }}</span>
                      <span class="text-slate-500"> · {{ h.label }} · {{ h.autor }} ({{ h.rol }})</span>
                      @if (h.texto) { <span class="text-slate-600"> — {{ h.texto }}</span> }
                    </div>
                  }
                </div>
              </details>
            }
          </div>
        } @else if (err()) {
          <p class="text-sm text-rose-500">{{ err() }}</p>
        } @else {
          <p class="text-sm text-slate-400">Cargando editor…</p>
        }
      </div>
    </div>
  `,
})
export class MiProyectoComponent implements OnInit {
  private _svc = inject(MiProyectoService);
  private _router = inject(Router);
  private _toast = inject(NotificationService);
  private _confirm = inject(ConfirmDialogService);
  private _dialog = inject(MatDialog);

  /** Botones demo (Rellenar prueba / Reiniciar flujo) solo en desarrollo, nunca en producción. */
  protected readonly esDev = !environment.production;

  protected p = signal<ProyectoEditor | null>(null);
  protected err = signal<string | null>(null);
  protected paso = signal(0);

  protected readonly fases = FASES;
  protected readonly estadosAct = ESTADOS_ACTIVIDAD;
  protected readonly rubros = RUBROS;
  protected readonly financiamientos = FINANCIAMIENTOS;
  protected faseLabel = (v?: string) => (v ? FASE_LABEL[v] ?? v : '—');

  // Drafts de alta
  protected actNombre = ''; protected actFase = FASES[0]; protected actIni: number | null = null; protected actFin: number | null = null;
  protected parRubro = RUBROS[0]; protected parDesc = ''; protected parMonto: number | null = null;
  protected turnPct: number | null = null;
  protected mostrarActForm = signal(false);
  protected mostrarParForm = signal(false);
  protected rangoFechas = rangoFechas;
  /** Columnas del cronograma derivadas de las fechas de las actividades. */
  protected meses = computed(() => ganttMeses(this.p()?.actividades ?? []));
  private minMesIdx = computed(() => this.meses()[0]?.idx ?? 0);
  private readonly FASE_COLOR: Record<string, { fg: string; bg: string }> = {
    PLANIFICACION: { fg: '#475569', bg: '#F1F5F9' },
    TRABAJO_CAMPO: { fg: '#1D4ED8', bg: '#DBEAFE' },
    ANALISIS: { fg: '#B45309', bg: '#FEF3C7' },
    REDACCION: { fg: '#15803D', bg: '#DCFCE7' },
  };

  protected secciones = computed<SeccionDef[]>(() => {
    const e = this.p();
    if (!e) return [];
    const cual = e.enfoque === 'CUALITATIVO';
    return PROY_DEF.filter((s) => !(s.cuant && cual) && !(s.cual && !cual));
  });
  protected totalPasos = computed(() => this.secciones().length + 1); // +1 = Cierre y envío
  protected esCierre = computed(() => this.paso() >= this.secciones().length);
  protected secActual = computed<SeccionDef | null>(() => this.secciones()[this.paso()] ?? null);

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this._svc.editor$().subscribe({
      next: (res) => {
        this.p.set(res?.data ?? res);
        this.turnPct = this.p()?.porcentajeSimilitud ?? this.turnPct;
        if (this.paso() >= this.totalPasos()) this.paso.set(this.totalPasos() - 1);
        // Deja visible el historial de los ítems observados/corregidos (para saber qué corregir).
        const conHist = (this.p()?.revisiones ?? []).filter((r) => (r.eventos?.length ?? 0) > 0).map((r) => r.campo);
        this.histSet.set(new Set([...this.histSet(), ...conHist]));
        this.syncHipotesis();
      },
      error: (e) => this.err.set(e?.error?.message ?? 'No se pudo cargar el proyecto'),
    });
  }

  // ── Pasos (wizard) ──
  pasoLabel(sec: SeccionDef): string { return sec.titulo; }
  irA(i: number): void {
    this.paso.set(Math.max(0, Math.min(this.totalPasos() - 1, i)));
    if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  siguiente(): void { this.irA(this.paso() + 1); }
  anterior(): void { this.irA(this.paso() - 1); }

  stat(sec: SeccionDef): { fil: number; tot: number; full: boolean } {
    const e = this.p();
    // Aspectos administrativos no tiene campos de texto: se mide por actividades + presupuesto.
    if (sec.wPlan) {
      let fil = 0;
      if ((e?.actividades?.length ?? 0) > 0) fil++;
      if ((e?.partidas?.length ?? 0) > 0) fil++;
      return { fil, tot: 2, full: fil === 2 };
    }
    const tot = sec.campos.length;
    let fil = 0;
    for (const c of sec.campos) { const v = e?.campos?.[c.k]; if (v && v.trim()) fil++; }
    return { fil, tot, full: tot > 0 && fil === tot };
  }
  conformes(): number { return this.p()?.revisiones?.filter((r) => r.estado === 'CONFORME').length ?? 0; }
  /** Color del punto de la sección en el índice lateral (verde=completa, ámbar=parcial, gris=vacía). */
  dotColor(sec: SeccionDef): string {
    const s = this.stat(sec);
    return s.full ? '#16A34A' : s.fil > 0 ? '#D97706' : '#E2E8F0';
  }
  /** Ítems observados aún sin corregir (bloquean el reenvío a revisión). */
  observacionesPendientes(): number {
    return this.itemsPendientes().length;
  }
  /** Detalle de los ítems por corregir: campo, etiqueta, sección y su paso, para navegar. */
  itemsPendientes(): { campo: string; label: string; seccion: string; paso: number }[] {
    const secs = this.secciones();
    const revs = (this.p()?.revisiones ?? []).filter((r) => r.estado === 'OBSERVADO' || r.estado === 'EN_CORRECCION');
    return revs.map((r) => {
      let paso = -1, label = r.campo, seccion = '';
      secs.forEach((s, i) => {
        const c = s.campos.find((cc) => cc.k === r.campo);
        if (c) { paso = i; label = c.l; seccion = s.titulo; }
        else if (r.campo === 'plan' && s.wPlan) { paso = i; label = 'Plan de actividades'; seccion = s.titulo; }
      });
      return { campo: r.campo, label, seccion, paso };
    });
  }
  irAItem(it: { paso: number }): void { if (it.paso >= 0) this.irA(it.paso); }


  // ── Historial de cambios y correcciones ──
  labelDe(campo: string): string {
    if (campo === 'plan') return 'Plan de actividades';
    for (const s of PROY_DEF) { const c = s.campos.find((cc) => cc.k === campo); if (c) return c.l; }
    return campo;
  }
  private parseFecha(f?: string): number {
    const m = f?.match(/(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})/);
    return m ? new Date(+m[3], +m[2] - 1, +m[1], +m[4], +m[5]).getTime() : 0;
  }
  historial() {
    const out: { fecha: string; ts: number; tipo: string; autor: string; rol: string; label: string; texto: string }[] = [];
    for (const r of this.p()?.revisiones ?? []) {
      const label = this.labelDe(r.campo);
      for (const ev of r.eventos ?? []) {
        out.push({ fecha: ev.fecha ?? '', ts: this.parseFecha(ev.fecha), tipo: ev.tipo, autor: ev.autor ?? '', rol: ev.rol ?? '', label, texto: ev.texto ?? '' });
      }
    }
    return out.sort((a, b) => b.ts - a.ts);
  }
  tipoCls(tipo?: string): string {
    switch (tipo) {
      case 'OBSERVACIÓN': return 'text-rose-600';
      case 'CONFORMIDAD': return 'text-emerald-600';
      default: return 'text-slate-500';
    }
  }

  // ── Cronograma (Gantt de 18 meses) ──
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
  cicloEstado(a: ActividadItem): void {
    if (!a.id) return;
    const orden = ['PENDIENTE', 'EN_CURSO', 'HECHA'];
    const next = orden[(orden.indexOf(a.estado ?? 'PENDIENTE') + 1) % 3];
    // Solo el estado (funciona incluso con el plan publicado/congelado).
    this._svc.actualizarActividad$(a.id, { estado: next } as any).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }

  // ── Campos ──
  val(k: string): string { return this.p()?.campos?.[k] ?? ''; }
  rev(k: string): RevisionItem | undefined { return this.p()?.revisiones?.find((r) => r.campo === k); }
  chip(k: string) { return chipRevision(this.rev(k)?.estado); }

  // ── Mini-historial por campo (colapsable) ──
  protected tipoDot = tipoDot;
  protected tipoVerbo = tipoVerbo;
  private histSet = signal<Set<string>>(new Set());
  toggleHist(k: string): void { const s = new Set(this.histSet()); s.has(k) ? s.delete(k) : s.add(k); this.histSet.set(s); }
  histOpen(k: string): boolean { return this.histSet().has(k); }
  /** Eventos en orden cronológico (del más antiguo al más reciente), como línea de tiempo. */
  eventos(k: string) { return [...(this.rev(k)?.eventos ?? [])]; }
  obs(k: string): { texto?: string } | null {
    const r = this.rev(k);
    if (!r || r.estado === 'SIN_REVISION' || r.estado === 'CONFORME') return null;
    const last = [...r.eventos].reverse().find((ev) => ev.tipo === 'OBSERVACIÓN');
    return last ? { texto: last.texto } : null;
  }

  guardar(k: string, ev: Event): void {
    const valor = (ev.target as HTMLInputElement | HTMLTextAreaElement).value;
    if (valor === this.val(k)) return;
    this._svc.guardarCampo$(k, valor).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }

  corregir(k: string, label: string): void {
    this._dialog.open(CorreccionDialogComponent, { width: '480px', autoFocus: true, data: { titulo: label } })
      .afterClosed().subscribe((texto: string | null) => {
        if (!texto) return;
        this._svc.corregir$(k, texto).subscribe({
          next: () => { this._toast.success('Corrección enviada'); this.cargar(); },
          error: (e) => this.fail(e),
        });
      });
  }

  // ── Etapa 7: Jurado Informante ──
  solicitarJurado(): void {
    this._confirm.confirmSave({
      title: 'Solicitar Jurado Informante',
      message: 'Se enviará tu expediente (informe final aprobado + Turnitin) para que el Coordinador designe el Jurado Informante. ¿Continuar?',
    }).then(() => {
      this._svc.solicitarJuradoInformante$().subscribe({
        next: () => { this._toast.success('Jurado Informante solicitado'); this.cargar(); },
        error: (e) => this.fail(e),
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
        error: (e) => this.fail(e),
      });
    });
  }

  // ── Etapa 6: subir informe final ──
  subirInforme(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this._svc.subirInformeFinal$(file).subscribe({
      next: () => { this._toast.success('Informe final subido'); this.cargar(); },
      error: (e) => this.fail(e),
    });
    input.value = '';
  }

  // ── Etapa 5: responder a un revisor ──
  iniciales(nombre?: string): string {
    if (!nombre) return '—';
    const p = nombre.replace(/^(Dr\.|Dra\.|Mg\.|Lic\.)\s*/i, '').trim().split(/\s+/).filter(Boolean);
    return ((p[0]?.[0] ?? '') + (p[1]?.[0] ?? '')).toUpperCase() || '—';
  }
  revEstadoTexto(e?: string): string {
    return e === 'CONFORME' ? 'CONFORME' : e === 'OBSERVADO' ? 'CON OBSERVACIONES' : 'PENDIENTE DE EVALUAR';
  }
  revEstadoBadge(e?: string): string {
    return e === 'CONFORME' ? 'bg-emerald-100 text-emerald-700'
      : e === 'OBSERVADO' ? 'bg-rose-100 text-rose-700'
      : 'bg-slate-100 text-slate-500';
  }
  revEstadoLabel(e?: string): string {
    return e === 'CONFORME' ? '✓ CONFORME' : e === 'OBSERVADO' ? '⚑ OBSERVADO' : 'POR EVALUAR';
  }
  revEstadoCls(e?: string): string {
    return e === 'CONFORME' ? 'bg-emerald-100 text-emerald-700'
      : e === 'OBSERVADO' ? 'bg-rose-100 text-rose-700'
      : 'bg-slate-100 text-slate-500';
  }
  responderRevisor(rv: RevisorEval): void {
    if (!rv.revisorId) return;
    this._dialog.open(CorreccionDialogComponent, {
      width: '480px', autoFocus: true,
      data: { titulo: 'Levantar observación · Revisor ' + rv.orden },
    }).afterClosed().subscribe((texto: string | null) => {
      if (!texto) return;
      this._svc.responderRevisor$(rv.revisorId!, texto).subscribe({
        next: () => { this._toast.success('Respuesta enviada al revisor'); this.cargar(); },
        error: (e) => this.fail(e),
      });
    });
  }

  setEnfoque(v: string): void {
    const e = this.p();
    if (!e || e.enfoque === v || e.enfoqueBloqueado) return;
    this._svc.setEnfoque$(v).subscribe({ next: () => this.cargar(), error: (err) => this.fail(err) });
  }

  cambiarEnfoque(): void {
    this._confirm.confirmSave({
      title: 'Cambiar enfoque',
      message: 'Cambiar el enfoque reestructura el formulario (la sección III cambia entre "Hipótesis y variables" y "Categorías del estudio"). ¿Deseas desbloquearlo para elegir otro?',
    }).then(() => {
      this._svc.desbloquearEnfoque$().subscribe({
        next: () => { this._toast.success('Enfoque desbloqueado — ya puedes elegir el otro'); this.cargar(); },
        error: (e) => this.fail(e),
      });
    }).catch(() => {});
  }

  changeFinanc(ev: Event): void {
    this._svc.setFinanciamiento$((ev.target as HTMLSelectElement).value).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }

  // ── Objetivos ──
  addObjetivo(): void {
    this._svc.agregarObjetivo$({ texto: '' }).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }
  saveObjetivo(o: any, ev: Event): void {
    const texto = (ev.target as HTMLInputElement).value;
    if (!o.id || texto === (o.texto ?? '')) return;
    this._svc.actualizarObjetivo$(o.id, { texto }).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }
  delObjetivo(o: any): void {
    if (!o.id) return;
    this._svc.eliminarObjetivo$(o.id).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }

  // ── Hipótesis (lista repetible respaldada por el campo 'hipotesis', separadas por salto de línea) ──
  protected hipotesis = signal<string[]>([]);
  private syncHipotesis(): void {
    const raw = this.p()?.campos?.['hipotesis'] ?? '';
    const guardadas = raw.split('\n').map((x) => x.trim()).filter((x) => x.length > 0);
    // conserva las filas vacías que el usuario esté agregando (aún sin escribir)
    const vaciasEnEdicion = this.hipotesis().filter((x) => !x.trim()).length;
    this.hipotesis.set([...guardadas, ...Array(vaciasEnEdicion).fill('')]);
  }
  private saveHipotesis(): void {
    const joined = this.hipotesis().map((x) => x.trim()).filter((x) => x.length > 0).join('\n');
    this._svc.guardarCampo$('hipotesis', joined).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }
  addHip(): void { this.hipotesis.set([...this.hipotesis(), '']); }
  saveHip(i: number, ev: Event): void {
    const v = (ev.target as HTMLInputElement).value;
    const arr = [...this.hipotesis()];
    if (arr[i] === v) return;
    arr[i] = v;
    this.hipotesis.set(arr);
    this.saveHipotesis();
  }
  delHip(i: number): void {
    this.hipotesis.set(this.hipotesis().filter((_, idx) => idx !== i));
    this.saveHipotesis();
  }

  // ── Referencias bibliográficas ──
  protected readonly estilos = ESTILOS_CITA;
  addReferencia(): void {
    this._dialog.open(ReferenciaDialogComponent, { width: '560px', maxWidth: '95vw', maxHeight: '90vh', autoFocus: true, data: null })
      .afterClosed().subscribe((data) => {
        if (!data) return;
        this._svc.agregarReferencia$(data).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
      });
  }
  editReferencia(ref: ReferenciaItem): void {
    this._dialog.open(ReferenciaDialogComponent, { width: '560px', maxWidth: '95vw', maxHeight: '90vh', autoFocus: true, data: ref })
      .afterClosed().subscribe((data) => {
        if (!data || !ref.id) return;
        this._svc.actualizarReferencia$(ref.id, data).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
      });
  }
  delReferencia(ref: ReferenciaItem): void {
    if (!ref.id) return;
    this._svc.eliminarReferencia$(ref.id).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }
  private estiloLabel(v: string): string { return this.estilos.find((s) => s.v === v)?.l ?? v; }

  setEstilo(v: string): void {
    const e = this.p();
    if (!e || e.estiloCita === v) return;
    const lbl = this.estiloLabel(v);
    if (e.estiloCitaBloqueado) {
      // Bloqueado: advertir antes de desbloquear y cambiar.
      this._confirm.confirmDelete({
        title: 'El estilo de cita está fijado',
        message: 'Un documento usa un solo estilo. Si cambias a ' + lbl + ', la bibliografía se reformatea, '
          + 'pero las citas que YA escribiste en el texto (p. ej. [1] o (Autor, año)) NO se convierten solas y '
          + 'tendrás que revisarlas manualmente. ¿Deseas desbloquear y cambiar?',
      }).then(() => {
        this._svc.desbloquearEstiloCita$().subscribe({
          next: () => this._svc.setEstiloCita$(v).subscribe({
            next: () => { this._toast.success('Estilo cambiado a ' + lbl); this.cargar(); },
            error: (err) => this.fail(err),
          }),
          error: (err) => this.fail(err),
        });
      }).catch(() => {});
      return;
    }
    // Primera vez: fijar con aviso.
    this._confirm.confirmSave({
      title: 'Fijar estilo de cita: ' + lbl,
      message: 'Se usará ' + lbl + ' en TODO el documento (citas y bibliografía). Una vez fijado quedará '
        + 'bloqueado; para cambiarlo después deberás desbloquearlo. ¿Continuar?',
    }).then(() => {
      this._svc.setEstiloCita$(v).subscribe({
        next: () => { this._toast.success('Estilo fijado: ' + lbl); this.cargar(); },
        error: (err) => this.fail(err),
      });
    }).catch(() => {});
  }

  // ── Insertar cita en el texto (desde las referencias) ──
  protected citaField = signal<string>('');
  private citaCtx: { campo: string; el: HTMLTextAreaElement } | null = null;
  onCitaFocus(campo: string, ev: Event): void {
    this.citaCtx = { campo, el: ev.target as HTMLTextAreaElement };
  }
  /** Agrega una referencia nueva (por DOI o manual) y cita inmediatamente en el texto. */
  nuevaCitaReferencia(): void {
    const campo = this.citaField();
    this._dialog.open(ReferenciaDialogComponent, { width: '560px', maxWidth: '95vw', maxHeight: '90vh', autoFocus: true, data: null })
      .afterClosed().subscribe((data) => {
        if (!data) return;
        this._svc.agregarReferencia$(data).subscribe({
          next: (res) => {
            const nueva = (res?.data ?? res) as ReferenciaItem; // trae su citaTexto
            this.citaField.set(campo);
            this.insertarCita(nueva);
            this._toast.success('Referencia agregada y citada');
          },
          error: (e) => this.fail(e),
        });
      });
  }
  insertarCita(ref: ReferenciaItem): void {
    const campo = this.citaField();
    if (!campo) return;
    this._dialog.open(CitaInsertarDialogComponent, {
      width: '480px', maxWidth: '95vw', autoFocus: false,
      data: { ref, estilo: this.p()?.estiloCita ?? 'APA' },
    }).afterClosed().subscribe((cita: string | null) => {
      if (!cita) return;
      this.citaField.set(campo);
      this.insertarTextoCita(campo, cita);
    });
  }
  private insertarTextoCita(campo: string, cita: string): void {
    const el = this.citaCtx && this.citaCtx.campo === campo ? this.citaCtx.el : null;
    const valor = el ? el.value : this.val(campo);
    const pos = el ? (el.selectionStart ?? valor.length) : valor.length;
    // agrega un espacio antes de la cita si el texto previo no termina en espacio
    const pre = pos > 0 && !/\s$/.test(valor.slice(0, pos)) ? ' ' : '';
    const nuevo = valor.slice(0, pos) + pre + cita + valor.slice(pos);
    this.guardarCampoValor(campo, nuevo);
  }
  private guardarCampoValor(campo: string, valor: string): void {
    const e = this.p();
    if (e) { e.campos = { ...e.campos, [campo]: valor }; this.p.set({ ...e }); }
    this._svc.guardarCampo$(campo, valor).subscribe({ next: () => this.cargar(), error: (err) => this.fail(err) });
  }
  generarDoc(formato: 'pdf' | 'docx'): void {
    this._svc.generarDocumento$(formato).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'Proyecto de tesis.' + formato;
        a.click();
        URL.revokeObjectURL(url);
        this._toast.success('Documento ' + formato.toUpperCase() + ' generado');
      },
      error: (e) => this.fail(e),
    });
  }

  // ── Actividades ──
  /** El cronograma se congela al publicar el plan. */
  protected planBloqueado(): boolean { return !!this.p()?.planPublicado; }

  abrirActividad(): void {
    if (this.planBloqueado()) return;
    this._dialog.open(ActividadDialogComponent, { width: '480px', autoFocus: true })
      .afterClosed().subscribe((data) => {
        if (!data) return;
        this._svc.agregarActividad$(data).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
      });
  }

  addActividad(): void {
    if (!this.actNombre.trim()) { this._toast.error('Escribe el nombre de la actividad'); return; }
    const a: ActividadItem = {
      nombre: this.actNombre.trim(), fase: FASE_ENUM[this.actFase],
      mesInicio: this.actIni ?? undefined, mesFin: this.actFin ?? undefined, estado: 'PENDIENTE',
    };
    this._svc.agregarActividad$(a).subscribe({
      next: () => { this.actNombre = ''; this.actIni = null; this.actFin = null; this.mostrarActForm.set(false); this.cargar(); },
      error: (e) => this.fail(e),
    });
  }
  changeEstado(a: ActividadItem, ev: Event): void {
    if (!a.id) return;
    this._svc.actualizarActividad$(a.id, { ...a, estado: (ev.target as HTMLSelectElement).value }).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }
  delActividad(a: ActividadItem): void {
    if (!a.id || this.planBloqueado()) return;
    this._svc.eliminarActividad$(a.id).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }

  // ── Presupuesto ──
  abrirPartida(): void {
    this._dialog.open(PartidaDialogComponent, { width: '480px', autoFocus: true })
      .afterClosed().subscribe((data) => {
        if (!data) return;
        this._svc.agregarPartida$(data).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
      });
  }

  addPartida(): void {
    const p: PartidaItem = { rubro: this.parRubro, descripcion: this.parDesc.trim(), monto: this.parMonto ?? 0 };
    this._svc.agregarPartida$(p).subscribe({
      next: () => { this.parDesc = ''; this.parMonto = null; this.mostrarParForm.set(false); this.cargar(); },
      error: (e) => this.fail(e),
    });
  }
  delPartida(p: PartidaItem): void {
    if (!p.id) return;
    this._svc.eliminarPartida$(p.id).subscribe({ next: () => this.cargar(), error: (e) => this.fail(e) });
  }

  // ── Turnitin ──
  onTurnitin(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this._svc.subirTurnitin$(file, this.turnPct ?? 0).subscribe({
      next: () => { this._toast.success('Informe de Turnitin subido'); this.cargar(); },
      error: (e) => this.fail(e),
    });
  }

  verDoc(tipo: 'turnitin' | 'proyecto-final'): void {
    this._svc.descargarDocumento$(tipo).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const nombre = tipo === 'turnitin' ? 'Informe de Turnitin' : 'Proyecto versión final';
        this._dialog.open(DocumentoPreviewDialogComponent, {
          data: { nombre, url, esPdf: (blob.type || 'application/pdf').includes('pdf') },
          maxWidth: '92vw',
        }).afterClosed().subscribe(() => setTimeout(() => URL.revokeObjectURL(url), 1000));
      },
      error: (e) => this.fail(e),
    });
  }

  onProyectoFinal(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this._svc.subirProyectoFinal$(file).subscribe({
      next: () => { this._toast.success('Proyecto versión final subido'); this.cargar(); },
      error: (e) => this.fail(e),
    });
  }

  // ── Hitos ──
  marcarListo(): void {
    this._svc.marcarListo$().subscribe({ next: () => { this._toast.success('Proyecto enviado a revisión del asesor'); this.cargar(); }, error: (e) => this.fail(e) });
  }
  reenviar(): void {
    this._svc.reenviarRevision$().subscribe({ next: () => { this._toast.success('Proyecto reenviado a revisión del asesor'); this.cargar(); }, error: (e) => this.fail(e) });
  }
  publicarPlan(): void {
    this._confirm.confirmSave({
      title: 'Publicar plan de actividades',
      message: 'Al publicar, el cronograma queda fijo: ya no podrás agregar, eliminar ni cambiar fechas de las actividades. Solo podrás actualizar el estado de cada una durante la ejecución. ¿Deseas publicarlo?',
    }).then(() => {
      this._svc.publicarPlan$().subscribe({ next: () => { this._toast.success('Plan de actividades publicado'); this.cargar(); }, error: (e) => this.fail(e) });
    }).catch(() => {});
  }
  solicitar(): void {
    this._svc.solicitarAprobacion$().subscribe({ next: () => { this._toast.success('Solicitud de aprobación registrada'); this.cargar(); }, error: (e) => this.fail(e) });
  }

  // ── DEMO / pruebas ──
  seedDemo(): void {
    this._confirm.confirmSave({
      title: 'Rellenar datos de prueba',
      message: 'Se completará el proyecto con datos de ejemplo (enfoque cuantitativo) y se enviará a revisión del asesor. Útil para probar el flujo rápido.',
    }).then(() => {
      this._svc.seedDemo$().subscribe({
        next: () => { this._toast.success('Datos de prueba cargados y enviados a revisión'); this.cargar(); },
        error: (e) => this.fail(e),
      });
    }).catch(() => {});
  }
  resetDemo(): void {
    this._confirm.confirmDelete({
      title: 'Reiniciar flujo',
      message: 'Se borrarán las observaciones y correcciones, y el proyecto volverá a "aún no enviado" (el contenido de los campos se conserva). ¿Continuar?',
    }).then(() => {
      this._svc.resetDemo$().subscribe({
        next: () => { this._toast.success('Flujo reiniciado'); this.cargar(); },
        error: (e) => this.fail(e),
      });
    }).catch(() => {});
  }

  // ── Matriz de consistencia (derivada, igual que el prototipo) ──
  matriz(): { problema: string; objetivos: string; hipotesis: string; variables: string } {
    const e = this.p();
    const v = (k: string) => e?.campos?.[k]?.trim();
    const nObj = (e?.objetivos ?? []).filter((o) => o.texto && o.texto.trim()).length;
    return {
      problema: v('formulacion') || '— completa la formulación del problema (I)',
      objetivos: (v('objGeneral') || '— completa el objetivo general (I)') + (nObj ? ' · +' + nObj + ' específicos' : ''),
      hipotesis: e?.enfoque === 'CUALITATIVO' ? 'No aplica (enfoque cualitativo)' : (v('hipotesis') || '— completa la hipótesis (III)'),
      variables: v('variables') || v('diseno') || '— completa variables (III) o diseño (IV)',
    };
  }
  matrizConsistente(): boolean {
    const e = this.p();
    return !!(e?.campos?.['formulacion']?.trim() && e?.campos?.['objGeneral']?.trim());
  }

  check(ok: boolean): string { return ok ? '✅' : '⬜'; }
  volver(): void { this._router.navigate(['/admin/expediente']); }
  private fail(e: any): void { this._toast.error(e?.error?.message ?? 'No se pudo completar la acción'); }
}
