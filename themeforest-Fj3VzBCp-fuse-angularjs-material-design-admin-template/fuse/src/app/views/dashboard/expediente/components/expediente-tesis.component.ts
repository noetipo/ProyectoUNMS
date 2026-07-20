import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { ExpedienteService } from '../services/expediente.service';
import { Expediente, claseEtapa, etiquetaEtapa } from '../models/expediente.model';

/**
 * Expediente de tesis — línea de tiempo de las 8 etapas del proceso de titulación.
 * Es la pantalla del "Proceso de Tesis" del estudiante; la Etapa 4 (Elaboración del
 * proyecto) enlaza al editor "Proyecto de tesis en línea".
 */
@Component({
  selector: 'app-expediente-tesis',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · {{ exp()?.programaNombre ?? 'Posgrado' }}</p>
          <h1 class="page-title">Expediente de tesis</h1>
        </div>
      </div>

      <div class="page-content p-6">
        @if (exp(); as e) {
          <div class="mx-auto max-w-[1000px] space-y-4">
            <!-- Cabecera -->
            <section class="form-card">
              <div class="flex flex-wrap items-start justify-between gap-4">
                <div class="min-w-0">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="px-2 py-0.5 rounded-md text-[11px] font-bold bg-amber-100 text-amber-700 uppercase tracking-wide">
                      {{ e.estadoLabel ?? 'En proceso' }}
                    </span>
                    <span class="text-[11px] text-slate-400 font-mono">{{ e.codigo ?? '—' }}</span>
                  </div>
                  <h2 class="text-lg font-bold text-slate-800 leading-snug">{{ e.titulo ?? 'Proyecto sin título' }}</h2>
                  <div class="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-1 text-[13px]">
                    <p><span class="text-slate-400">Doctorando:</span> <span class="text-slate-700 font-medium">{{ e.doctorandoNombre ?? '—' }}</span></p>
                    <p><span class="text-slate-400">Programa:</span> <span class="text-slate-700">{{ e.programaNombre ?? '—' }}</span></p>
                    <p><span class="text-slate-400">Asesor(a):</span> <span class="text-slate-700">{{ e.asesorNombre ?? '—' }}</span></p>
                    <p><span class="text-slate-400">Tutor(a):</span> <span class="text-slate-700">{{ e.tutorNombre ?? '—' }}</span></p>
                    <p class="sm:col-span-2"><span class="text-slate-400">Línea:</span> <span class="text-slate-700">{{ e.lineaNombre ?? '—' }}</span></p>
                  </div>
                </div>
                <div class="text-right shrink-0">
                  <p class="text-[11px] text-slate-400 uppercase tracking-wide">Avance del proceso</p>
                  <p class="text-3xl font-extrabold text-[#8C1D2E] leading-none">{{ e.avancePct }}%</p>
                  <div class="mt-2 h-1.5 w-40 rounded bg-slate-100 overflow-hidden ml-auto">
                    <div class="h-full rounded bg-[#8C1D2E]" [style.width.%]="e.avancePct"></div>
                  </div>
                </div>
              </div>
            </section>

            <!-- Línea de tiempo -->
            <section class="form-card">
              <header class="form-card__head"><h2 class="form-card__title">Línea de tiempo del proceso de titulación</h2></header>
              <ol class="relative">
                @for (et of e.etapas; track et.numero; let last = $last) {
                  <li class="flex gap-3 pb-5" [class.opacity-60]="et.estado === 'PENDIENTE'">
                    <!-- Riel + círculo -->
                    <div class="flex flex-col items-center">
                      <div class="flex items-center justify-center size-7 rounded-full text-xs font-bold shrink-0"
                           [ngClass]="et.estado === 'COMPLETADO' ? 'bg-emerald-500 text-white'
                                    : et.estado === 'EN_CURSO' ? 'bg-[#8C1D2E] text-white' : 'bg-slate-200 text-slate-500'">
                        @if (et.estado === 'COMPLETADO') { <mat-icon svgIcon="check" class="size-4" /> } @else { {{ et.numero }} }
                      </div>
                      @if (!last) { <div class="w-px flex-1 bg-slate-200 mt-1"></div> }
                    </div>
                    <!-- Contenido -->
                    <div class="flex-1 min-w-0 pt-0.5">
                      <div class="flex flex-wrap items-center gap-2">
                        <h3 class="text-sm font-semibold text-slate-800">{{ et.numero }}. {{ et.titulo }}</h3>
                        <span class="px-2 py-0.5 rounded text-[10px] font-bold" [ngClass]="claseEtapa(et.estado)">{{ etiquetaEtapa(et.estado) }}</span>
                        @if (et.fecha) { <span class="ml-auto text-[11px] text-slate-400">{{ et.fecha }}</span> }
                      </div>
                      <p class="text-[12.5px] text-slate-500 mt-0.5">{{ et.descripcion }}</p>
                      @if (et.tieneDictamen) {
                        <span class="inline-flex items-center gap-1 mt-1.5 px-2 py-1 rounded-md border border-rose-200 text-[11px] text-rose-600">
                          <mat-icon svgIcon="file-text" class="size-3.5" /> {{ et.dictamenLabel }}
                        </span>
                      }
                      @if (et.numero === 4 && et.estado === 'EN_CURSO') {
                        <div class="mt-2">
                          <button mat-flat-button color="primary" class="!h-8 !text-xs" (click)="abrirEditor()">
                            <mat-icon svgIcon="square-pen" class="size-3.5 mr-1" /> Abrir editor del proyecto
                          </button>
                        </div>
                      }
                    </div>
                  </li>
                }
              </ol>
            </section>
          </div>
        } @else if (err()) {
          <p class="text-sm text-rose-500">{{ err() }}</p>
        } @else {
          <p class="text-sm text-slate-400">Cargando expediente…</p>
        }
      </div>
    </div>
  `,
})
export class ExpedienteTesisComponent implements OnInit {
  private _svc = inject(ExpedienteService);
  private _router = inject(Router);

  protected exp = signal<Expediente | null>(null);
  protected err = signal<string | null>(null);
  protected claseEtapa = claseEtapa;
  protected etiquetaEtapa = etiquetaEtapa;

  ngOnInit(): void {
    this._svc.miExpediente$().subscribe({
      next: (res) => this.exp.set(res?.data ?? res),
      error: (e) => this.err.set(e?.error?.message ?? 'No se pudo cargar el expediente'),
    });
  }

  abrirEditor(): void {
    this._router.navigate(['/admin/mi-proyecto']);
  }
}
