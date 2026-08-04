import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { SeguimientoService } from '../services/seguimiento.service';
import {
  EtapaConteo,
  SeguimientoAlumno,
  SeguimientoResumen,
  claseAvance,
  claseResponsable,
  claseVigencia,
  colorMovimiento,
  etiquetaResponsable,
  etiquetaVigencia,
  iconoMovimiento,
  textoMovimiento,
} from '../models/seguimiento.model';

/**
 * Secretaría · Seguimiento de doctorandos. Responde tres preguntas de un vistazo: en qué etapa
 * va cada alumno, hace cuánto no se mueve, y qué acción lo destraba — con el botón que lleva
 * justo a la pantalla donde se resuelve. Los filtros son de cliente (el universo es pequeño);
 * el Excel se pide al backend con esos mismos filtros.
 */
@Component({
  selector: 'app-seguimiento-alumnos',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule, RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Seguimiento</p>
          <h1 class="page-title">Seguimiento de doctorandos</h1>
        </div>
        <div class="flex items-center gap-2">
          <button mat-stroked-button class="!h-9 !text-xs !text-slate-600" [disabled]="exportando()" (click)="exportar()">
            <mat-icon svgIcon="download" class="size-3.5 mr-1" /> {{ exportando() ? 'Generando…' : 'Exportar Excel' }}
          </button>
          <button mat-stroked-button class="!h-9 !text-xs !text-slate-600" [disabled]="loading()" (click)="cargar()">
            <mat-icon svgIcon="refresh-cw" class="size-3.5 mr-1" /> Actualizar
          </button>
        </div>
      </div>

      <div class="page-content p-6">
        <!-- Tarjetas de una sola línea: icono + rótulo + cifra a la derecha. El detalle que
             antes iba debajo pasó al tooltip; el estado "filtrando" se ve por el borde. -->
        <div class="grid grid-cols-2 lg:grid-cols-5 gap-2.5 mb-4">
          <div class="rounded-xl border border-slate-100 bg-white px-3.5 py-2 flex items-center gap-2"
               [title]="'Sin tema registrado: ' + (resumen()?.sinTema ?? 0)">
            <mat-icon svgIcon="users" class="size-4 shrink-0 text-slate-400" />
            <span class="text-[11.5px] font-medium text-slate-400 truncate">Doctorandos</span>
            <span class="ml-auto text-[22px] font-bold text-slate-700 leading-none shrink-0">{{ resumen()?.total ?? 0 }}</span>
          </div>

          <button type="button" class="rounded-xl border px-3.5 py-2 transition flex items-center gap-2"
                  [ngClass]="soloPendientes() ? 'border-[#8C1D2E] bg-[#FDF6F7]' : 'border-slate-100 bg-white hover:border-[#8C1D2E]/40'"
                  [title]="soloPendientes() ? 'Filtrando: solo pendientes de Secretaría' : 'Clic para ver solo los pendientes de Secretaría'"
                  (click)="soloPendientes.set(!soloPendientes())">
            <mat-icon svgIcon="clock" class="size-4 shrink-0 text-[#8C1D2E]" />
            <span class="text-[11.5px] font-medium text-[#8C1D2E] truncate">Esperan a Secretaría</span>
            <span class="ml-auto text-[22px] font-bold text-[#8C1D2E] leading-none shrink-0">{{ resumen()?.pendientesSecretaria ?? 0 }}</span>
          </button>

          <button type="button" class="rounded-xl border px-3.5 py-2 transition flex items-center gap-2"
                  [ngClass]="soloDetenidos() ? 'border-amber-500 bg-amber-50' : 'border-slate-100 bg-white hover:border-amber-300'"
                  [title]="soloDetenidos() ? 'Filtrando: solo detenidos' : 'Clic para ver solo los detenidos más de 30 días'"
                  (click)="soloDetenidos.set(!soloDetenidos())">
            <mat-icon svgIcon="triangle-alert" class="size-4 shrink-0 text-amber-600" />
            <span class="text-[11.5px] font-medium text-amber-600 truncate">Sin movimiento +30d</span>
            <span class="ml-auto text-[22px] font-bold text-amber-600 leading-none shrink-0">{{ resumen()?.detenidos ?? 0 }}</span>
          </button>

          <button type="button" class="rounded-xl border px-3.5 py-2 transition flex items-center gap-2"
                  [ngClass]="soloPorVencer() ? 'border-rose-500 bg-rose-50' : 'border-slate-100 bg-white hover:border-rose-300'"
                  title="Dictamen del proyecto con vigencia de 4 años: vence en 6 meses o menos"
                  (click)="soloPorVencer.set(!soloPorVencer())">
            <mat-icon svgIcon="calendar-x" class="size-4 shrink-0 text-rose-600" />
            <span class="text-[11.5px] font-medium text-rose-600 truncate">Dictamen por vencer</span>
            <span class="ml-auto text-[22px] font-bold text-rose-600 leading-none shrink-0">{{ resumen()?.dictamenesPorVencer ?? 0 }}</span>
          </button>

          <div class="rounded-xl border border-slate-100 bg-white px-3.5 py-2 flex items-center gap-2"
               [title]="'Sustentados de ' + (resumen()?.total ?? 0) + ' doctorandos'">
            <mat-icon svgIcon="circle-check" class="size-4 shrink-0 text-slate-400" />
            <span class="text-[11.5px] font-medium text-slate-400 truncate">Sustentados</span>
            <span class="ml-auto text-[22px] font-bold text-emerald-600 leading-none shrink-0">{{ resumen()?.finalizados ?? 0 }}</span>
          </div>
        </div>

        <!-- Distribución por etapa: una tira de píldoras (clic = filtro). Las etapas vacías
             quedan atenuadas para que la vista salte a donde sí hay alumnos. -->
        <div class="flex flex-wrap items-center gap-1.5 mb-4">
          <span class="text-[11px] font-bold text-slate-400 uppercase tracking-wide mr-1">Por etapa</span>
          @for (e of resumen()?.porEtapa ?? []; track e.numero) {
            <button type="button" class="inline-flex items-center gap-1.5 pl-1.5 pr-2 py-1 rounded-lg border text-[11px] transition"
                    [ngClass]="etapaFiltro() === e.numero
                      ? 'border-[#8C1D2E] bg-[#FDF6F7]'
                      : (e.cantidad ? 'border-slate-200 bg-white hover:border-slate-300' : 'border-slate-100 bg-white opacity-60 hover:opacity-100')"
                    [title]="'Etapa ' + e.numero + ' · ' + e.titulo"
                    (click)="filtrarEtapa(e)">
              <span class="px-1 rounded bg-slate-100 text-slate-500 font-bold text-[10px]">{{ e.numero }}</span>
              <span class="text-slate-600">{{ e.titulo }}</span>
              <span class="font-bold" [ngClass]="e.cantidad ? 'text-[#8C1D2E]' : 'text-slate-300'">{{ e.cantidad }}</span>
            </button>
          }
          @if (hayFiltros()) {
            <button mat-button class="!h-7 !text-[11px] !text-slate-500 ml-auto" (click)="limpiarFiltros()">
              <mat-icon svgIcon="x" class="size-3.5 mr-1" /> Limpiar filtros
            </button>
          }
        </div>

        <!-- Filtros -->
        <div class="flex flex-wrap items-center gap-2 mb-3">
          <div class="relative flex-1 min-w-[240px] max-w-[340px]">
            <mat-icon svgIcon="search" class="size-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input type="text" [ngModel]="buscar()" (ngModelChange)="buscar.set($event)"
                   placeholder="Buscar por nombre, código o título…"
                   class="w-full h-9 pl-9 pr-3 rounded-lg border border-slate-200 text-[12.5px] outline-none focus:border-[#8C1D2E]" />
          </div>

          <label class="inline-flex items-center gap-1.5 h-9 pl-3 pr-1 rounded-lg border border-slate-200 bg-white max-w-[300px]">
            <span class="text-[11.5px] text-slate-400 whitespace-nowrap">Programa</span>
            <select [ngModel]="programa()" (ngModelChange)="programa.set($event)"
                    class="h-full min-w-0 bg-transparent pr-1 text-[12.5px] text-slate-600 outline-none cursor-pointer">
              <option value="">Todos</option>
              @for (p of programas(); track p) { <option [value]="p">{{ p }}</option> }
            </select>
          </label>

          <!-- "¿A quién hay que apurar?" — la etiqueta va visible para que se entienda sin
               explicación: se lee "A cargo de: Coordinador". -->
          <label class="inline-flex items-center gap-1.5 h-9 pl-3 pr-1 rounded-lg border border-slate-200 bg-white">
            <span class="text-[11.5px] text-slate-400 whitespace-nowrap">A cargo de</span>
            <select [ngModel]="responsable() ?? ''" (ngModelChange)="responsable.set($event || null)"
                    class="h-full bg-transparent pr-1 text-[12.5px] text-slate-600 outline-none cursor-pointer">
              <option value="">Cualquiera</option>
              @for (r of responsables; track r) { <option [value]="r">{{ etiquetaResponsable(r) }}</option> }
            </select>
          </label>

          <button type="button" class="ml-auto inline-flex items-center gap-1 text-[11.5px] text-slate-500 hover:text-slate-700"
                  (click)="ordenDetenido.set(!ordenDetenido())">
            <mat-icon [svgIcon]="ordenDetenido() ? 'arrow-down-wide-narrow' : 'arrow-down-up'" class="size-3.5" />
            {{ ordenDetenido() ? 'Más detenidos primero' : 'Orden alfabético' }}
          </button>
          <span class="text-[11.5px] text-slate-400">{{ filtradas().length }} de {{ alumnos().length }}</span>
        </div>

        <!-- Tabla -->
        <div class="data-table">
          <table class="w-full text-sm">
            <thead><tr class="text-slate-400 text-left">
              <th class="py-2 px-3 font-medium">Doctorando</th>
              <th class="py-2 px-3 font-medium w-[250px]">Etapa y avance</th>
              <th class="py-2 px-3 font-medium">¿Qué falta?</th>
              <th class="py-2 px-3 font-medium text-right w-[210px]">Acción</th>
            </tr></thead>
            <tbody>
              @for (a of filtradas(); track a.estudianteId) {
                <tr class="border-t border-slate-100" [ngClass]="a.pendienteSecretaria ? 'bg-[#FDF6F7]/60' : ''">
                  <td class="py-1.5 px-3 text-slate-700">
                    <span class="block font-semibold text-slate-800 text-[13px] leading-[1.35]">{{ a.apellidos }}, {{ a.nombres }}</span>
                    <span class="block text-[10.5px] text-slate-400 leading-[1.35] truncate max-w-[280px]"
                          [title]="(a.tituloTesis ?? 'Sin tema') + (a.asesorNombre ? ' · Asesor: ' + a.asesorNombre : '')">
                      {{ a.codigoSistema ?? '—' }} · {{ a.programaNombre ?? '—' }}
                    </span>
                  </td>
                  <td class="py-1.5 px-3">
                    <div class="flex items-center gap-1.5">
                      <span class="px-1 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-500 shrink-0">
                        {{ a.etapaNumero > 8 ? 'FIN' : a.etapaNumero }}
                      </span>
                      <span class="text-[12px] text-slate-600 truncate">{{ a.etapaTitulo }}</span>
                      <span class="ml-auto text-[10px] text-slate-400 shrink-0">{{ a.avancePct }}%</span>
                    </div>
                    <div class="mt-1 h-1 rounded-full bg-slate-100 overflow-hidden">
                      <div class="h-full rounded-full" [ngClass]="claseAvance(a.avancePct)" [style.width.%]="a.avancePct"></div>
                    </div>
                    <!-- Hace cuánto que el expediente no registra un hito. Va aquí, pegado a la
                         etapa (es de ella de quien habla), y no en una columna "Inactivo" que se
                         leía al revés. El reloj pasa a alerta cuando supera los 30 días. -->
                    <span class="mt-1 flex items-center gap-1 text-[10.5px]" [ngClass]="colorMovimiento(a.diasEnEtapa)">
                      <mat-icon [svgIcon]="iconoMovimiento(a.diasEnEtapa)" class="size-3 shrink-0" />
                      {{ textoMovimiento(a.diasEnEtapa) }}
                    </span>
                  </td>
                  <td class="py-1.5 px-3">
                    <!-- Solo lo que le toca a la Secretaría lleva chip de color; los demás
                         roles se leen como texto suave para no competir con lo suyo. -->
                    <div class="flex items-center gap-1.5">
                      @if (a.pendienteSecretaria) {
                        <span class="px-1.5 py-0.5 rounded text-[10px] font-bold shrink-0" [ngClass]="claseResponsable(a.responsable)">
                          {{ etiquetaResponsable(a.responsable) }}
                        </span>
                        <span class="text-[12px] text-[#8C1D2E] font-semibold">{{ a.pendiente }}</span>
                      } @else {
                        <span class="text-[12px] text-slate-600">
                          @if (a.responsable !== 'NADIE') {
                            <span class="text-slate-400">{{ etiquetaResponsable(a.responsable) }} · </span>
                          }{{ a.pendiente }}
                        </span>
                      }
                    </div>
                    @if (a.vigenciaMeses !== null && a.vigenciaMeses !== undefined) {
                      <span class="inline-block mt-0.5 px-1.5 py-0.5 rounded text-[10px] font-bold" [ngClass]="claseVigencia(a.vigenciaMeses)">
                        {{ etiquetaVigencia(a.vigenciaMeses) }}
                      </span>
                    }
                  </td>
                  <td class="py-1.5 px-3">
                    <div class="flex items-center justify-end gap-1">
                      @if (a.accionLink) {
                        <button type="button" class="inline-flex items-center gap-1 h-8 px-2.5 rounded-lg text-xs font-medium border transition"
                                [ngClass]="a.pendienteSecretaria
                                  ? 'bg-[#8C1D2E] text-white border-[#8C1D2E] hover:bg-[#73172581]'
                                  : 'bg-white text-slate-500 border-slate-200 hover:border-slate-300'"
                                [title]="a.pendienteSecretaria ? a.pendiente : 'Le corresponde a ' + etiquetaResponsable(a.responsable)"
                                (click)="irAccion(a)">
                          {{ a.accionLabel }} <mat-icon svgIcon="arrow-right" class="size-3.5" />
                        </button>
                      }
                      <!-- Secundarias con la convención del sistema: .row-actions + iconos 7×7.
                           route = expediente/línea de tiempo · id-card = ficha de la persona
                           (file-search queda reservado para "revisar un documento"). -->
                      <div class="row-actions">
                        @if (a.tesisId) {
                          <a mat-icon-button class="!w-7 !h-7" title="Expediente y línea de tiempo"
                             [routerLink]="['/admin/expedientes', a.tesisId]">
                            <mat-icon svgIcon="route" class="text-slate-400 size-3.5" />
                          </a>
                        }
                        <a mat-icon-button class="!w-7 !h-7" title="Ficha de la persona"
                           [routerLink]="['/admin/personas', a.estudianteId]">
                          <mat-icon svgIcon="id-card" class="text-slate-400 size-3.5" />
                        </a>
                      </div>
                    </div>
                  </td>
                </tr>
              }
              @if (!filtradas().length && !loading()) {
                <tr><td colspan="4" class="py-6 text-center text-sm text-slate-400">
                  {{ alumnos().length ? 'Ningún alumno coincide con el filtro.' : 'No hay doctorandos registrados.' }}
                </td></tr>
              }
              @if (loading()) {
                <tr><td colspan="4" class="py-6 text-center text-sm text-slate-400">Cargando seguimiento…</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class SeguimientoAlumnosComponent implements OnInit {
  private _svc = inject(SeguimientoService);
  private _router = inject(Router);
  private _toast = inject(NotificationService);

  protected alumnos = signal<SeguimientoAlumno[]>([]);
  protected resumen = signal<SeguimientoResumen | null>(null);
  protected loading = signal(true);
  protected exportando = signal(false);

  protected buscar = signal('');
  protected programa = signal('');
  protected etapaFiltro = signal<number | null>(null);
  protected responsable = signal<string | null>(null);
  protected soloPendientes = signal(false);
  protected soloDetenidos = signal(false);
  protected soloPorVencer = signal(false);
  protected ordenDetenido = signal(false);

  protected readonly responsables = ['SECRETARIA', 'COORDINADOR', 'ASESOR', 'REVISOR', 'JURADO', 'ESTUDIANTE'];

  /** Programas presentes en los datos (evita pedir el catálogo completo solo para filtrar). */
  protected programas = computed(() =>
    [...new Set(this.alumnos().map((a) => a.programaNombre).filter((p): p is string => !!p))].sort()
  );

  protected filtradas = computed(() => {
    const etapa = this.etapaFiltro();
    const resp = this.responsable();
    const prog = this.programa();
    const soloPend = this.soloPendientes();
    const soloDet = this.soloDetenidos();
    const soloVen = this.soloPorVencer();
    const q = this.buscar().trim().toLowerCase();

    const rows = this.alumnos().filter((a) => {
      if (etapa !== null && a.etapaNumero !== etapa) return false;
      if (resp && a.responsable !== resp) return false;
      if (prog && a.programaNombre !== prog) return false;
      if (soloPend && !a.pendienteSecretaria) return false;
      if (soloDet && !(a.diasEnEtapa != null && a.diasEnEtapa > 30)) return false;
      if (soloVen && !(a.vigenciaMeses != null && a.vigenciaMeses <= 6)) return false;
      if (!q) return true;
      return [a.apellidos, a.nombres, a.codigoSistema, a.tituloTesis, a.programaNombre, a.asesorNombre]
        .some((v) => (v ?? '').toLowerCase().includes(q));
    });

    return this.ordenDetenido()
      ? [...rows].sort((x, y) => (y.diasEnEtapa ?? -1) - (x.diasEnEtapa ?? -1))
      : rows;
  });

  protected hayFiltros = computed(() =>
    this.etapaFiltro() !== null || !!this.responsable() || !!this.programa() ||
    this.soloPendientes() || this.soloDetenidos() || this.soloPorVencer() || !!this.buscar()
  );

  protected claseAvance = claseAvance;
  protected claseResponsable = claseResponsable;
  protected etiquetaResponsable = etiquetaResponsable;
  protected textoMovimiento = textoMovimiento;
  protected colorMovimiento = colorMovimiento;
  protected iconoMovimiento = iconoMovimiento;
  protected claseVigencia = claseVigencia;
  protected etiquetaVigencia = etiquetaVigencia;

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.loading.set(true);
    this._svc.tablero$().subscribe({
      next: (res) => {
        const d = res?.data ?? res;
        this.alumnos.set(d?.alumnos ?? []);
        this.resumen.set(d?.resumen ?? null);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /** El link viene del backend como "/admin/pantalla?tesis={id}": lo parto para navegar. */
  irAccion(a: SeguimientoAlumno): void {
    if (!a.accionLink) return;
    const [path, query] = a.accionLink.split('?');
    const tesis = new URLSearchParams(query ?? '').get('tesis');
    this._router.navigate([path], tesis ? { queryParams: { tesis } } : {});
  }

  filtrarEtapa(e: EtapaConteo): void {
    this.etapaFiltro.set(this.etapaFiltro() === e.numero ? null : e.numero);
  }

  limpiarFiltros(): void {
    this.etapaFiltro.set(null);
    this.responsable.set(null);
    this.programa.set('');
    this.buscar.set('');
    this.soloPendientes.set(false);
    this.soloDetenidos.set(false);
    this.soloPorVencer.set(false);
  }

  exportar(): void {
    this.exportando.set(true);
    this._svc.excel$({
      buscar: this.buscar(),
      etapa: this.etapaFiltro(),
      responsable: this.responsable(),
      programa: this.programa(),
    }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'seguimiento-doctorandos.xlsx';
        a.click();
        URL.revokeObjectURL(url);
        this.exportando.set(false);
      },
      error: () => {
        this._toast.error('No se pudo generar el Excel');
        this.exportando.set(false);
      },
    });
  }
}
