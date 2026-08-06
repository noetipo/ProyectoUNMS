import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { ChartComponent, ApexChart } from 'ng-apexcharts';
import { NotificationService } from '@/app/shared/notification/notification.service';
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

          <!-- ── Cabecera compacta: una franja, no un mural ── -->
          <header class="relative overflow-hidden px-6 py-3 bg-gradient-to-r from-[#8C1D2E] to-[#a8283d]">
            <div class="absolute -right-10 -top-10 size-28 rounded-full bg-white/10"></div>
            <div class="relative mx-auto max-w-[1440px] flex flex-wrap items-center gap-3">
              <div class="min-w-0">
                <h1 class="text-[16px] font-extrabold text-white leading-tight">{{ d.saludo }}</h1>
                <p class="text-[11.5px] text-white/70 leading-tight">{{ d.subtitulo }}</p>
              </div>
              <span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-white/15 text-white/90 text-[10.5px] font-semibold">
                <mat-icon svgIcon="graduation-cap" class="size-3" /> {{ d.rolLabel }}
              </span>
            </div>
          </header>

          <div class="mx-auto max-w-[1440px] px-6 py-4 space-y-4">

            <!-- ── Franja de cifras del rol: una tira fina, no tarjetones ── -->
            @if (d.metricas?.length) {
              <section class="grid gap-2.5 grid-cols-1 sm:grid-cols-2"
                       [ngClass]="d.metricas.length >= 4 ? 'lg:grid-cols-4' : 'lg:grid-cols-3'">
                @for (m of d.metricas; track m.etiqueta) {
                  <button type="button"
                          class="flex items-center gap-2.5 text-left rounded-xl bg-white border border-slate-200 px-3 py-2.5 transition hover:border-slate-300"
                          [class.cursor-default]="!m.link" [title]="m.detalle" (click)="ir(m.link)">
                    <span class="grid place-items-center size-8 rounded-lg shrink-0" [class]="fondo(m.tono)">
                      <mat-icon [svgIcon]="m.icono" class="size-4" [class]="texto(m.tono)" />
                    </span>
                    <span class="min-w-0 flex-1">
                      <span class="block text-[17px] font-extrabold leading-none truncate" [class]="texto(m.tono)">{{ m.valor }}</span>
                      <span class="block text-[10.5px] text-slate-500 leading-tight truncate">{{ m.etiqueta }}</span>
                    </span>
                    @if (m.link) { <mat-icon svgIcon="arrow-right" class="size-3.5 text-slate-300 shrink-0" /> }
                  </button>
                }
              </section>
            }

            <div class="grid gap-3 lg:grid-cols-[1.6fr_1fr] items-start">

              <!-- ── Primer gráfico del rol: el más importante, a la izquierda y ancho ── -->
              @if (grafico(0); as g) {
                <section class="rounded-xl bg-white border border-slate-200 p-3.5 min-w-0 order-2 lg:order-1">
                  <div class="flex items-baseline gap-2 mb-1">
                    <h2 class="text-[12.5px] font-bold text-slate-800">{{ g.titulo }}</h2>
                    <span class="text-[10.5px] text-slate-400 truncate">· {{ g.subtitulo }}</span>
                  </div>
                  <apx-chart [series]="serie(g)" [chart]="conf(g)" [plotOptions]="plot(g)"
                             [dataLabels]="etiquetas(g)" [xaxis]="eje(g)" [labels]="g.categorias"
                             [colors]="colores(g)" [grid]="grid" [tooltip]="tooltip" [legend]="legend" />
                </section>
              }

              <!-- ── Pendientes: qué hago hoy ── -->
              <section class="rounded-xl bg-white border border-slate-200 p-3.5 min-w-0 order-1 lg:order-2">
                <div class="flex items-baseline gap-2 mb-2.5">
                  <h2 class="text-[12.5px] font-bold text-slate-800">Qué te toca ahora</h2>
                  <span class="text-[10.5px] text-slate-400">· por urgencia</span>
                </div>
                @if (d.pendientes?.length) {
                  <div class="space-y-1.5">
                    @for (t of d.pendientes; track t.titulo) {
                      <button type="button" class="w-full text-left flex items-center gap-2 rounded-lg border px-2.5 py-2 transition hover:bg-slate-50"
                              [class]="borde(t.prioridad)" [class.cursor-default]="!t.link" (click)="ir(t.link)">
                        <span class="shrink-0 text-[8.5px] font-extrabold px-1.5 py-0.5 rounded" [class]="chip(t.prioridad)">
                          {{ etiquetaPrioridad(t.prioridad) }}
                        </span>
                        <span class="min-w-0 flex-1">
                          <span class="block text-[11.5px] font-semibold text-slate-800 leading-snug">{{ t.titulo }}</span>
                          <span class="block text-[10px] text-slate-500 truncate">{{ t.detalle }}</span>
                        </span>
                        @if (t.link) { <mat-icon svgIcon="arrow-right" class="size-3.5 text-slate-300 shrink-0" /> }
                      </button>
                    }
                  </div>
                } @else {
                  <div class="flex flex-col items-center gap-1.5 py-6 text-center">
                    <mat-icon svgIcon="badge-check" class="size-8 text-emerald-200" />
                    <p class="text-[12px] font-semibold text-slate-600">Nada pendiente por ahora</p>
                    <p class="text-[10.5px] text-slate-400">Te avisaremos por la campanita.</p>
                  </div>
                }

                <!-- Mi avance (solo doctorando) -->
                @if (d.miEtapa) {
                  <div class="mt-3 pt-2.5 border-t border-slate-100">
                    <div class="flex items-baseline justify-between mb-1">
                      <span class="text-[11px] font-semibold text-slate-600 truncate">
                        Etapa {{ d.miEtapa }} de 8 · {{ d.miEtapaTitulo }}
                      </span>
                      <span class="text-[12px] font-extrabold text-[#8C1D2E] shrink-0">{{ d.miAvancePct }}%</span>
                    </div>
                    <div class="h-1.5 rounded-full bg-slate-100 overflow-hidden">
                      <div class="h-full rounded-full bg-gradient-to-r from-[#8C1D2E] to-[#c23a52]"
                           [style.width.%]="d.miAvancePct"></div>
                    </div>
                  </div>
                }
              </section>
            </div>

            <!-- ── Resto de gráficos: rejilla de 3 (2 en tablet, 1 en móvil) ── -->
            @if (restoGraficos().length) {
              <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                @for (g of restoGraficos(); track g.id) {
                  <section class="rounded-xl bg-white border border-slate-200 p-3.5 min-w-0"
                           [class.sm:col-span-2]="g.ancho" [class.xl:col-span-2]="g.ancho">
                    <div class="flex items-baseline gap-2 mb-1">
                      <h2 class="text-[12.5px] font-bold text-slate-800">{{ g.titulo }}</h2>
                      <span class="text-[10.5px] text-slate-400 truncate">· {{ g.subtitulo }}</span>
                    </div>
                    <apx-chart [series]="serie(g)" [chart]="conf(g)" [plotOptions]="plot(g)"
                               [dataLabels]="etiquetas(g)" [xaxis]="eje(g)" [labels]="g.categorias"
                               [colors]="colores(g)" [grid]="grid" [tooltip]="tooltip" [legend]="legend" />
                  </section>
                }
              </div>
            }

            <!-- ── Cifras del programa · solo llegan a roles de gestión/docencia ── -->
            @if (d.agregados) {
            <section class="rounded-xl bg-white border border-slate-200 p-3.5">
              <div class="flex items-baseline gap-2 mb-2.5">
                <h2 class="text-[12.5px] font-bold text-slate-800">El programa en números</h2>
                <span class="text-[10.5px] text-slate-400">· solo conteos, sin datos personales</span>
              </div>

              <div class="grid gap-2.5 grid-cols-2 lg:grid-cols-4">
                @for (k of kpis(); track k.k) {
                  <div class="flex items-center gap-2.5 rounded-xl border border-slate-100 px-3 py-2.5">
                    <span class="grid place-items-center size-8 rounded-lg shrink-0" [class]="fondo(k.tono)">
                      <mat-icon [svgIcon]="k.icono" class="size-4" [class]="texto(k.tono)" />
                    </span>
                    <span class="min-w-0">
                      <span class="block text-[17px] font-extrabold leading-none" [class]="texto(k.tono)">{{ k.v }}</span>
                      <span class="block text-[10.5px] text-slate-500 leading-tight">{{ k.k }}</span>
                    </span>
                  </div>
                }
              </div>

              @if (d.agregados.porLinea?.length) {
                <div class="mt-3 pt-2.5 border-t border-slate-100">
                  <p class="text-[11px] font-semibold text-slate-600 mb-1.5">Por línea de investigación</p>
                  <div class="grid gap-x-5 gap-y-1 sm:grid-cols-2">
                    @for (l of d.agregados.porLinea; track l.etiqueta) {
                      <div class="flex items-center gap-2">
                        <span class="text-[11px] text-slate-600 w-[130px] truncate" [title]="l.etiqueta">{{ l.etiqueta }}</span>
                        <span class="flex-1 h-1.5 rounded-full bg-slate-100 overflow-hidden">
                          <span class="block h-full rounded-full bg-sky-500" [style.width.%]="pct(l.valor)"></span>
                        </span>
                        <span class="text-[11px] font-bold text-slate-700 w-4 text-right">{{ l.valor }}</span>
                      </div>
                    }
                  </div>
                </div>
              }

              <p class="flex items-start gap-1.5 text-[10px] text-slate-400 mt-2.5 pt-2 border-t border-slate-100">
                <mat-icon svgIcon="lock" class="size-3 shrink-0 mt-px" />
                No incluyen nombres, códigos ni notas: el detalle vive en Seguimiento y en cada expediente, con acceso por rol.
              </p>
            </section>
            }
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

  protected readonly paleta = ['#8C1D2E', '#c23a52', '#0369a1', '#0891b2', '#059669', '#b45309'];

  // ── Configuración común de los gráficos (ApexCharts, ya presente en el proyecto) ──
  protected grid = { borderColor: '#f1f5f9', strokeDashArray: 3 };
  protected tooltip = { theme: 'light' };
  protected legend = { position: 'bottom' as const, fontSize: '10.5px', itemMargin: { horizontal: 6 } };

  // ── Gráficos genéricos: el backend decide CUÁLES según el rol; aquí solo se pintan ──
  protected graficos = computed<any[]>(() => this.p()?.graficos ?? []);
  protected grafico(i: number): any | null { return this.graficos()[i] ?? null; }
  protected restoGraficos = computed<any[]>(() => this.graficos().slice(1));

  /** Serie: los de dona/radial van sin nombre (array plano); los de ejes, con nombre. */
  serie(g: any): any {
    const datos = g?.series?.[0]?.datos ?? [];
    return g?.tipo === 'dona' || g?.tipo === 'radial'
      ? datos
      : [{ name: g?.series?.[0]?.nombre ?? '', data: datos }];
  }

  conf(g: any): ApexChart {
    const alto = g?.tipo === 'barrasH' ? Math.max(220, (g?.categorias?.length ?? 4) * 30) : 260;
    const tipo = g?.tipo === 'dona' ? 'donut'
      : g?.tipo === 'radial' ? 'radialBar'
      : g?.tipo === 'area' ? 'area' : 'bar';
    return { type: tipo as any, height: alto, toolbar: { show: false }, fontFamily: 'inherit' };
  }

  plot(g: any): any {
    if (g?.tipo === 'barrasH') return { bar: { horizontal: true, borderRadius: 6, barHeight: '62%' } };
    if (g?.tipo === 'barras') return { bar: { horizontal: false, borderRadius: 6, columnWidth: '46%' } };
    if (g?.tipo === 'radial') {
      return {
        radialBar: {
          hollow: { size: '62%' },
          dataLabels: {
            name: { fontSize: '11px', color: '#94a3b8', offsetY: 22 },
            value: { fontSize: '30px', fontWeight: 800, color: '#8C1D2E', offsetY: -14 },
          },
        },
      };
    }
    return {};
  }

  etiquetas(g: any): any {
    return g?.tipo === 'dona' || g?.tipo === 'radial'
      ? { enabled: false }
      : { enabled: true, style: { fontSize: '11px', fontWeight: 700 } };
  }

  eje(g: any): any {
    if (g?.tipo === 'dona' || g?.tipo === 'radial') return { categories: [] };
    return { categories: g?.categorias ?? [], labels: { style: { fontSize: '10.5px' } } };
  }

  colores(g: any): string[] {
    return g?.colores?.length ? g.colores : this.paleta;
  }

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
