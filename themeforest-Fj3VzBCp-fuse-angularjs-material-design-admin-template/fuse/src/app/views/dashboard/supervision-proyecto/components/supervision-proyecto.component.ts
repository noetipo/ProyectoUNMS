import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { SupervisionProyectoService } from '../services/supervision-proyecto.service';
import {
  ActividadItem, FASE_LABEL, PROY_DEF, ProyectoEditor, RevisionItem, SeccionDef, chipRevision,
} from '../../mi-proyecto/models/proyecto.model';

/** Supervisión del TUTOR sobre el proyecto (solo lectura), centrada en el plan de actividades. */
@Component({
  selector: 'app-supervision-proyecto',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <button mat-button class="!text-slate-500 !text-xs !px-2 mb-1" (click)="volver()">
            <mat-icon svgIcon="chevron-left" class="size-3.5" /> Volver a la bandeja
          </button>
          <h1 class="page-title">Supervisión del proyecto</h1>
        </div>
      </div>

      <div class="page-content p-6">
        @if (p(); as e) {
          <div class="mx-auto max-w-[920px] space-y-4">
            <!-- Cabecera -->
            <section class="form-card">
              <p class="text-[13px] font-semibold text-slate-800">{{ e.estudianteNombre }} <span class="text-slate-400 font-normal">· {{ e.codigoSistema }}</span></p>
              <p class="text-[12.5px] text-slate-500">{{ e.programaNombre }} · Asesor(a): {{ e.asesorNombre ?? '—' }}</p>
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
              <p class="text-[11px] text-slate-400 mt-2">Supervisión en función del plan de actividades. Solo lectura — el tutor no edita ni aprueba.</p>
            </section>

            <!-- Plan de actividades (lo que supervisa el tutor) -->
            <section class="form-card">
              <div class="flex flex-wrap items-center gap-x-3 gap-y-1 mb-2">
                <h3 class="text-sm font-bold text-slate-700">Plan de actividades <span class="font-normal text-slate-400">· cronograma 18 meses</span></h3>
                <span class="ml-auto text-[11px] text-slate-500">{{ e.actividades.length }} actividades · {{ actHechas() }} hechas · {{ actEnCurso() }} en curso</span>
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full" [ngClass]="e.planPublicado ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'">
                  {{ e.planPublicado ? 'PLAN PUBLICADO' : 'PLAN NO PUBLICADO' }}
                </span>
              </div>
              <div class="overflow-x-auto">
                <div class="min-w-[640px]">
                  <div class="grid grid-cols-[minmax(150px,32%)_1fr_88px] items-end gap-2 pb-1 border-b border-slate-100">
                    <span class="text-[10px] font-bold tracking-wide text-slate-400">ACTIVIDAD · FASE</span>
                    <div class="grid" style="grid-template-columns: repeat(18, 1fr)">
                      @for (m of meses; track m) { <span class="text-[8px] text-center text-slate-300 font-mono">{{ m }}</span> }
                    </div>
                    <span class="text-[10px] font-bold tracking-wide text-slate-400 text-center">ESTADO</span>
                  </div>
                  @for (a of e.actividades; track a.id ?? $index) {
                    <div class="grid grid-cols-[minmax(150px,32%)_1fr_88px] items-center gap-2 py-2 border-b border-slate-50">
                      <div>
                        <p class="text-[12px] text-slate-700 leading-tight">{{ a.nombre }}</p>
                        <span class="inline-block mt-1 px-1.5 py-0.5 rounded text-[9px] font-bold" [style.background]="faseBg(a.fase)" [style.color]="faseFg(a.fase)">{{ faseLabel(a.fase) }}</span>
                      </div>
                      <div class="grid h-2.5 rounded bg-slate-100" style="grid-template-columns: repeat(18, 1fr)">
                        @if (ganttCol(a); as gc) {
                          <div class="h-full rounded" [style.grid-column]="gc" [style.background]="faseFg(a.fase)"></div>
                        }
                      </div>
                      <span class="px-2 py-1 rounded-md text-[10px] font-bold text-center" [ngClass]="estadoCls(a.estado)">{{ estadoLabel(a.estado) }}</span>
                    </div>
                  }
                  @if (!e.actividades.length) { <p class="text-[12px] text-slate-400 py-3">El estudiante aún no ha registrado actividades.</p> }
                </div>
              </div>
            </section>

            <!-- Contenido del proyecto por pasos (solo lectura) -->
            <div class="rounded-xl border border-slate-200 bg-white px-2 py-2 overflow-x-auto">
              <div class="flex items-center min-w-max">
                @for (sec of secciones(); track sec.id; let i = $index; let last = $last) {
                  <button type="button" class="flex items-center gap-2 px-3 py-2 rounded-lg transition shrink-0"
                          [ngClass]="paso() === i ? 'bg-[#8C1D2E] text-white' : 'text-slate-500 hover:bg-slate-50'"
                          (click)="irA(i)">
                    <span class="size-6 rounded-full flex items-center justify-center text-[11px] font-bold shrink-0"
                          [ngClass]="paso() === i ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-400'">{{ i + 1 }}</span>
                    <span class="text-[11.5px] font-semibold whitespace-nowrap">{{ sec.titulo }}</span>
                  </button>
                  @if (!last) { <span class="w-5 h-px bg-slate-200 shrink-0"></span> }
                }
              </div>
            </div>

            @if (secActual(); as sec) {
              <section class="form-card">
                <header class="form-card__head"><h2 class="form-card__title !text-[#8C1D2E]">{{ sec.titulo }}</h2></header>
                <div class="space-y-4">
                  @for (c of sec.campos; track c.k) {
                    <div>
                      <div class="flex items-center gap-2 mb-1">
                        <label class="form-label !mb-0">{{ c.l }}</label>
                        <span class="ml-auto px-1.5 py-0.5 rounded text-[10px] font-bold" [ngClass]="chip(c.k).cls">{{ chip(c.k).label }}</span>
                      </div>
                      <div class="rounded-lg bg-slate-50 border border-slate-100 px-3 py-2 text-sm text-slate-700 whitespace-pre-wrap min-h-[38px]">{{ e.campos[c.k] || '—' }}</div>
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
                @if (sec.wMatriz) {
                  <div class="mt-4 pt-3 border-t border-slate-100">
                    <div class="flex items-center gap-2 mb-2.5">
                      <span class="text-[12px] font-bold text-slate-700 flex-1">Matriz de consistencia</span>
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
              </section>

              <div class="flex items-center justify-between">
                <button mat-stroked-button class="!h-9 !text-sm !rounded-lg" [disabled]="paso() === 0" (click)="anterior()">
                  <mat-icon svgIcon="chevron-left" class="size-4" /> Anterior
                </button>
                <span class="text-[12px] text-slate-400">Paso {{ paso() + 1 }} de {{ totalPasos() }}</span>
                @if (paso() < totalPasos() - 1) {
                  <button mat-flat-button color="primary" class="!h-9 !text-sm !rounded-lg" (click)="siguiente()">
                    Siguiente <mat-icon svgIcon="chevron-right" class="size-4" />
                  </button>
                } @else { <span class="w-[92px]"></span> }
              </div>
            }
          </div>
        } @else {
          <p class="text-sm text-slate-400">Cargando…</p>
        }
      </div>
    </div>
  `,
})
export class SupervisionProyectoComponent implements OnInit {
  private _route = inject(ActivatedRoute);
  private _router = inject(Router);
  private _svc = inject(SupervisionProyectoService);
  private _toast = inject(NotificationService);

  protected p = signal<ProyectoEditor | null>(null);
  protected paso = signal(0);
  private tesisId = '';

  protected readonly meses = Array.from({ length: 18 }, (_, i) => i + 1);
  private readonly FASE_COLOR: Record<string, { fg: string; bg: string }> = {
    PLANIFICACION: { fg: '#475569', bg: '#F1F5F9' },
    TRABAJO_CAMPO: { fg: '#1D4ED8', bg: '#DBEAFE' },
    ANALISIS: { fg: '#B45309', bg: '#FEF3C7' },
    REDACCION: { fg: '#15803D', bg: '#DCFCE7' },
  };
  protected faseLabel = (v?: string) => (v ? FASE_LABEL[v] ?? v : '—');

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
    this._svc.detalle$(this.tesisId).subscribe({
      next: (res) => { this.p.set(res?.data ?? res); if (this.paso() >= this.totalPasos()) this.paso.set(this.totalPasos() - 1); },
      error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo cargar el proyecto'),
    });
  }

  irA(i: number): void {
    this.paso.set(Math.max(0, Math.min(this.totalPasos() - 1, i)));
    if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  siguiente(): void { this.irA(this.paso() + 1); }
  anterior(): void { this.irA(this.paso() - 1); }

  rev(k: string): RevisionItem | undefined { return this.p()?.revisiones?.find((r) => r.campo === k); }
  chip(k: string) { return chipRevision(this.rev(k)?.estado); }

  // Gantt
  actHechas(): number { return this.p()?.actividades?.filter((a) => a.estado === 'HECHA').length ?? 0; }
  actEnCurso(): number { return this.p()?.actividades?.filter((a) => a.estado === 'EN_CURSO').length ?? 0; }
  faseFg(fase?: string): string { return (fase && this.FASE_COLOR[fase]?.fg) || '#94A3B8'; }
  faseBg(fase?: string): string { return (fase && this.FASE_COLOR[fase]?.bg) || '#F1F5F9'; }
  ganttCol(a: ActividadItem): string | null {
    if (!a.mesInicio) return null;
    const start = Math.max(1, Math.min(18, a.mesInicio));
    const end = Math.max(start, Math.min(18, a.mesFin ?? a.mesInicio));
    return `${start} / ${end + 1}`;
  }
  estadoLabel(e?: string): string { return (e ?? 'PENDIENTE').replace('_', ' '); }
  estadoCls(e?: string): string {
    switch (e) {
      case 'HECHA': return 'bg-emerald-100 text-emerald-700';
      case 'EN_CURSO': return 'bg-amber-100 text-amber-700';
      default: return 'bg-slate-100 text-slate-500';
    }
  }

  // Matriz
  matriz(): { problema: string; objetivos: string; hipotesis: string; variables: string } {
    const e = this.p();
    const v = (k: string) => e?.campos?.[k]?.trim();
    const nObj = (e?.objetivos ?? []).filter((o) => o.texto && o.texto.trim()).length;
    return {
      problema: v('formulacion') || '— sin formulación (I)',
      objetivos: (v('objGeneral') || '— sin objetivo general (I)') + (nObj ? ' · +' + nObj + ' específicos' : ''),
      hipotesis: e?.enfoque === 'CUALITATIVO' ? 'No aplica (enfoque cualitativo)' : (v('hipotesis') || '— sin hipótesis (III)'),
      variables: v('variables') || v('diseno') || '— sin variables/diseño',
    };
  }
  matrizConsistente(): boolean {
    const e = this.p();
    return !!(e?.campos?.['formulacion']?.trim() && e?.campos?.['objGeneral']?.trim());
  }

  volver(): void { this._router.navigate(['/admin/supervision-proyecto']); }
}
