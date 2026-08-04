import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { ChartComponent, ApexChart, ApexAxisChartSeries, ApexNonAxisChartSeries } from 'ng-apexcharts';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { descargarBlob } from '@/app/views/dashboard/reportes/download.util';
import { PanelService } from '../services/panel.service';

/**
 * Panel de inicio · "lo importante primero".
 *
 * <p>Arriba, lo que le toca a quien entra —el doctorando ve su etapa; el asesor, sus asesorados;
 * la Secretaría, su cola de trámite—, con la lista de pendientes ya priorizada. Debajo, las
 * cifras del programa, iguales para todos y <b>solo conteos</b>: no aparece ningún nombre, así
 * que puede verlas cualquier usuario sin exponer a nadie.</p>
 *
 * <p>Reemplaza al panel del template, cuyos números estaban escritos a mano.</p>
 */
@Component({
  selector: 'app-panel',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, ChartComponent],
  template: `
    <div class="page">
      <div class="page-content">
        @if (cargando()) {
          <p class="text-sm text-slate-400 p-6">Cargando tu panel…</p>
        } @else if (p(); as d) {

          <!-- ── Cabecera: saludo + rol + reporte ── -->
          <header class="relative overflow-hidden px-6 pt-7 pb-20 bg-gradient-to-br from-[#8C1D2E] via-[#a02236] to-[#5f1420]">
            <div class="absolute -right-16 -top-24 size-64 rounded-full bg-white/10"></div>
            <div class="absolute -right-4 top-24 size-40 rounded-full bg-white/5"></div>
            <div class="relative mx-auto max-w-[1440px] flex flex-wrap items-start justify-between gap-4">
              <div class="min-w-0">
                <p class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-white/15 text-white/90 text-[11px] font-semibold">
                  <mat-icon svgIcon="graduation-cap" class="size-3.5" /> {{ d.rolLabel }}
                </p>
                <h1 class="text-[26px] font-extrabold text-white leading-tight mt-1.5">{{ d.saludo }}</h1>
                <p class="text-[13px] text-white/70">{{ d.subtitulo }}</p>
              </div>
              <button mat-flat-button class="!bg-white !text-[#8C1D2E] !h-9 !text-[12px] !font-bold shrink-0"
                      [disabled]="descargando()" (click)="descargarReporte()">
                <mat-icon svgIcon="download" class="size-4 mr-1" /> Descargar reporte
              </button>
            </div>
          </header>

          <div class="mx-auto max-w-[1440px] px-6 -mt-14 pb-8 space-y-4">

            <!-- ── Métricas del rol ── -->
            @if (d.metricas?.length) {
              <section class="grid gap-3" [ngClass]="d.metricas.length >= 3 ? 'md:grid-cols-3' : 'md:grid-cols-2'">
                @for (m of d.metricas; track m.etiqueta) {
                  <button type="button" class="text-left rounded-2xl bg-white border border-slate-200 shadow-sm p-4 transition hover:shadow-md hover:-translate-y-0.5"
                          [class.cursor-default]="!m.link" (click)="ir(m.link)">
                    <div class="flex items-start gap-3">
                      <span class="grid place-items-center size-10 rounded-xl shrink-0" [class]="fondo(m.tono)">
                        <mat-icon [svgIcon]="m.icono" class="size-5" [class]="texto(m.tono)" />
                      </span>
                      <div class="min-w-0 flex-1">
                        <p class="text-[10.5px] font-bold uppercase tracking-wide text-slate-400">{{ m.etiqueta }}</p>
                        <p class="text-[26px] font-extrabold leading-tight" [class]="texto(m.tono)">{{ m.valor }}</p>
                        <p class="text-[11.5px] text-slate-500 leading-snug">{{ m.detalle }}</p>
                      </div>
                      @if (m.link) { <mat-icon svgIcon="arrow-right" class="size-4 text-slate-300 shrink-0 mt-1" /> }
                    </div>
                  </button>
                }
              </section>
            }

            <div class="grid gap-4 lg:grid-cols-[1.05fr_1fr] items-start">

              <!-- ── Pendientes: qué hago hoy ── -->
              <section class="rounded-2xl bg-white border border-slate-200 shadow-sm p-4">
                <div class="flex items-baseline gap-2 mb-3">
                  <h2 class="text-[13.5px] font-bold text-slate-800">Qué te toca ahora</h2>
                  <span class="text-[11px] text-slate-400">· ordenado por urgencia</span>
                </div>
                @if (d.pendientes?.length) {
                  <div class="space-y-2">
                    @for (t of d.pendientes; track t.titulo) {
                      <button type="button" class="w-full text-left flex items-start gap-2.5 rounded-xl border px-3 py-2.5 transition hover:bg-slate-50"
                              [class]="borde(t.prioridad)" [class.cursor-default]="!t.link" (click)="ir(t.link)">
                        <span class="grid place-items-center size-7 rounded-lg shrink-0" [class]="fondoPrioridad(t.prioridad)">
                          <mat-icon [svgIcon]="t.icono" class="size-3.5" [class]="textoPrioridad(t.prioridad)" />
                        </span>
                        <div class="min-w-0 flex-1">
                          <p class="text-[12.5px] font-semibold text-slate-800 leading-snug">{{ t.titulo }}</p>
                          <p class="text-[11px] text-slate-500">{{ t.detalle }}</p>
                        </div>
                        <span class="shrink-0 text-[9px] font-extrabold px-1.5 py-0.5 rounded" [class]="chip(t.prioridad)">
                          {{ etiquetaPrioridad(t.prioridad) }}
                        </span>
                      </button>
                    }
                  </div>
                } @else {
                  <div class="flex flex-col items-center gap-2 py-8 text-center">
                    <mat-icon svgIcon="badge-check" class="size-10 text-emerald-200" />
                    <p class="text-[13px] font-semibold text-slate-600">Nada pendiente por ahora</p>
                    <p class="text-[11.5px] text-slate-400">Te avisaremos por la campanita cuando algo requiera tu atención.</p>
                  </div>
                }

                <!-- Mi avance (solo doctorando) -->
                @if (d.miEtapa) {
                  <div class="mt-4 pt-3 border-t border-slate-100">
                    <div class="flex items-baseline justify-between mb-1.5">
                      <span class="text-[11.5px] font-semibold text-slate-600">
                        Etapa {{ d.miEtapa }} de 8 · {{ d.miEtapaTitulo }}
                      </span>
                      <span class="text-[13px] font-extrabold text-[#8C1D2E]">{{ d.miAvancePct }}%</span>
                    </div>
                    <div class="h-2 rounded-full bg-slate-100 overflow-hidden">
                      <div class="h-full rounded-full bg-gradient-to-r from-[#8C1D2E] to-[#c23a52] transition-all"
                           [style.width.%]="d.miAvancePct"></div>
                    </div>
                  </div>
                }
              </section>

              <!-- ── Embudo por etapa ── -->
              <section class="rounded-2xl bg-white border border-slate-200 shadow-sm p-4">
                <div class="flex items-baseline gap-2 mb-1">
                  <h2 class="text-[13.5px] font-bold text-slate-800">Doctorandos por etapa</h2>
                  <span class="text-[11px] text-slate-400">· {{ d.agregados.doctorandosActivos }} activos</span>
                </div>
                <apx-chart [series]="serieEtapas()" [chart]="chartBarras" [plotOptions]="plotBarras"
                           [dataLabels]="dataLabels" [xaxis]="ejeEtapas()" [colors]="['#8C1D2E']"
                           [grid]="grid" [tooltip]="tooltip" />
              </section>
            </div>

            <!-- ── Cifras del programa ── -->
            <section class="rounded-2xl bg-white border border-slate-200 shadow-sm p-4">
              <div class="flex items-baseline gap-2 mb-3">
                <h2 class="text-[13.5px] font-bold text-slate-800">El programa en números</h2>
                <span class="text-[11px] text-slate-400">· sin datos personales, visible para todos</span>
              </div>

              <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
                @for (k of kpis(); track k.k) {
                  <div class="rounded-xl border border-slate-100 p-3.5">
                    <div class="flex items-center gap-2">
                      <mat-icon [svgIcon]="k.icono" class="size-4" [class]="texto(k.tono)" />
                      <p class="text-[24px] font-extrabold leading-none" [class]="texto(k.tono)">{{ k.v }}</p>
                    </div>
                    <p class="text-[11.5px] text-slate-500 mt-1">{{ k.k }}</p>
                  </div>
                }
              </div>

              @if (d.agregados.porLinea?.length) {
                <div class="grid gap-4 md:grid-cols-2 mt-4 items-center">
                  <div>
                    <p class="text-[12px] font-semibold text-slate-600 mb-1.5">Por línea de investigación</p>
                    <div class="space-y-1.5">
                      @for (l of d.agregados.porLinea; track l.etiqueta) {
                        <div class="flex items-center gap-2">
                          <span class="text-[11.5px] text-slate-600 w-[150px] truncate" [title]="l.etiqueta">{{ l.etiqueta }}</span>
                          <span class="flex-1 h-2 rounded-full bg-slate-100 overflow-hidden">
                            <span class="block h-full rounded-full bg-sky-500" [style.width.%]="pct(l.valor)"></span>
                          </span>
                          <span class="text-[11.5px] font-bold text-slate-700 w-5 text-right">{{ l.valor }}</span>
                        </div>
                      }
                    </div>
                  </div>
                  <apx-chart [series]="serieLineas()" [chart]="chartDona" [labels]="labelsLineas()"
                             [legend]="legend" [colors]="paleta" [dataLabels]="dataLabelsOff" [tooltip]="tooltip" />
                </div>
              }

              <p class="flex items-start gap-2 text-[11px] text-slate-400 mt-4 pt-3 border-t border-slate-100">
                <mat-icon svgIcon="lock" class="size-3.5 shrink-0 mt-px" />
                Estas cifras son solo conteos: no incluyen nombres, códigos ni notas. El detalle de cada
                doctorando vive en Seguimiento y en su expediente, con acceso por rol.
              </p>
            </section>
          </div>
        }
      </div>
    </div>
  `,
})
export class PanelComponent implements OnInit {
  private _svc = inject(PanelService);
  private _router = inject(Router);
  private _toast = inject(NotificationService);

  protected p = signal<any | null>(null);
  protected cargando = signal(true);
  protected descargando = signal(false);

  protected readonly paleta = ['#8C1D2E', '#c23a52', '#0369a1', '#0891b2', '#059669', '#b45309'];

  // ── Configuración de los gráficos (ApexCharts, ya presente en el proyecto) ──
  protected chartBarras: ApexChart = {
    type: 'bar', height: 260, toolbar: { show: false }, fontFamily: 'inherit', animations: { enabled: true },
  };
  protected plotBarras = { bar: { horizontal: true, borderRadius: 6, barHeight: '62%', distributed: false } };
  protected dataLabels = { enabled: true, style: { fontSize: '11px', fontWeight: 700 } };
  protected dataLabelsOff = { enabled: false };
  protected grid = { borderColor: '#f1f5f9', strokeDashArray: 3 };
  protected tooltip = { theme: 'light' };
  protected legend = { position: 'bottom' as const, fontSize: '11px' };
  protected chartDona: ApexChart = {
    type: 'donut', height: 240, fontFamily: 'inherit', toolbar: { show: false },
  };

  protected serieEtapas = computed<ApexAxisChartSeries>(() => [{
    name: 'Doctorandos',
    data: (this.p()?.agregados?.porEtapa ?? []).map((c: any) => c.valor),
  }]);
  protected ejeEtapas = computed(() => ({
    categories: (this.p()?.agregados?.porEtapa ?? []).map((c: any) => `${c.orden}. ${c.etiqueta}`),
    labels: { style: { fontSize: '10.5px' } },
  }));
  protected serieLineas = computed<ApexNonAxisChartSeries>(() =>
    (this.p()?.agregados?.porLinea ?? []).map((c: any) => c.valor));
  protected labelsLineas = computed(() =>
    (this.p()?.agregados?.porLinea ?? []).map((c: any) => c.etiqueta));

  /** Las cuatro cifras grandes del bloque institucional. */
  protected kpis = computed(() => {
    const a = this.p()?.agregados;
    if (!a) return [];
    return [
      { k: 'Doctorandos activos', v: a.doctorandosActivos, icono: 'users-round', tono: 'granate' },
      { k: 'En proyecto o defensa', v: a.enRevision, icono: 'file-search', tono: 'cielo' },
      { k: 'Sin movimiento +30 días', v: a.detenidos, icono: 'triangle-alert', tono: 'ambar' },
      { k: 'Procesos finalizados', v: a.sustentados, icono: 'badge-check', tono: 'esmeralda' },
    ];
  });

  ngOnInit(): void {
    this._svc.panel$().subscribe({
      next: (res: any) => { this.p.set(res?.data ?? res ?? null); this.cargando.set(false); },
      error: () => { this.cargando.set(false); this._toast.error('No se pudo cargar el panel'); },
    });
  }

  ir(link?: string | null): void {
    if (!link) return;
    const [path, query] = link.split('?');
    const params: Record<string, string> = {};
    (query ?? '').split('&').filter(Boolean).forEach((q) => {
      const [k, v] = q.split('=');
      if (k) params[k] = v ?? '';
    });
    this._router.navigate([path], Object.keys(params).length ? { queryParams: params } : {});
  }

  descargarReporte(): void {
    this.descargando.set(true);
    this._svc.reporte$().subscribe({
      next: (blob) => { descargarBlob(blob, 'panel-titulacion.xlsx'); this.descargando.set(false); },
      error: () => { this.descargando.set(false); this._toast.error('No se pudo generar el reporte'); },
    });
  }

  /** Ancho relativo de la barra de una línea de investigación. */
  pct(v: number): number {
    const max = Math.max(1, ...(this.p()?.agregados?.porLinea ?? []).map((c: any) => c.valor));
    return Math.round((v / max) * 100);
  }

  // ── Paleta por tono (misma semántica que el resto del sistema) ──
  fondo(t?: string): string {
    switch (t) {
      case 'granate': return 'bg-[#FDF6F7]';
      case 'ambar': return 'bg-amber-50';
      case 'esmeralda': return 'bg-emerald-50';
      case 'cielo': return 'bg-sky-50';
      default: return 'bg-slate-100';
    }
  }
  texto(t?: string): string {
    switch (t) {
      case 'granate': return 'text-[#8C1D2E]';
      case 'ambar': return 'text-amber-600';
      case 'esmeralda': return 'text-emerald-600';
      case 'cielo': return 'text-sky-600';
      default: return 'text-slate-500';
    }
  }
  borde(p?: string): string {
    return p === 'ALTA' ? 'border-[#8C1D2E]/30 bg-[#FDF6F7]/50'
      : p === 'MEDIA' ? 'border-amber-200 bg-amber-50/40' : 'border-slate-100';
  }
  fondoPrioridad(p?: string): string {
    return p === 'ALTA' ? 'bg-[#8C1D2E]/10' : p === 'MEDIA' ? 'bg-amber-100' : 'bg-slate-100';
  }
  textoPrioridad(p?: string): string {
    return p === 'ALTA' ? 'text-[#8C1D2E]' : p === 'MEDIA' ? 'text-amber-600' : 'text-slate-400';
  }
  chip(p?: string): string {
    return p === 'ALTA' ? 'bg-[#8C1D2E] text-white'
      : p === 'MEDIA' ? 'bg-amber-100 text-amber-700' : 'bg-slate-100 text-slate-500';
  }
  etiquetaPrioridad(p?: string): string {
    return p === 'ALTA' ? 'AHORA' : p === 'MEDIA' ? 'PRONTO' : 'INFO';
  }
}
