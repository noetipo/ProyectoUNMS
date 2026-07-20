import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { RevisorProyectoService } from '../services/revisor-proyecto.service';
import { PROY_DEF } from '../../mi-proyecto/models/proyecto.model';

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

      <div class="page-content p-6">
        @if (data(); as d) {
          <div class="mx-auto max-w-[1080px]">
            <!-- Cabecera -->
            <section class="form-card mb-4">
              <p class="text-[13px] font-semibold text-slate-800">{{ d.proyecto.estudianteNombre }} <span class="text-slate-400 font-normal">· {{ d.proyecto.codigoSistema }}</span></p>
              <p class="text-[12.5px] text-slate-500">{{ d.proyecto.programaNombre }}</p>
              <p class="text-[13px] font-bold text-[#8C1D2E] mt-1">{{ d.proyecto.titulo }}</p>
              @if (d.cerrada) {
                <div class="mt-2 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-[12px] text-emerald-700 flex items-center gap-1.5">
                  <mat-icon svgIcon="badge-check" class="size-4 shrink-0" /> Ya diste conformidad (puntaje {{ d.miPuntajeTotal }}/{{ d.puntajeMaximo }}). La evaluación está cerrada.
                </div>
              }
            </section>

            <div class="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-4 items-start">
              <!-- Proyecto (solo lectura) -->
              <div class="space-y-4 min-w-0">
                @for (sec of secciones(); track sec.id) {
                  <section class="form-card">
                    <header class="form-card__head"><h2 class="form-card__title !text-[#8C1D2E]">{{ sec.titulo }}</h2></header>
                    <div class="space-y-3">
                      @for (c of sec.campos; track c.k) {
                        @if (val(d, c.k)) {
                          <div>
                            <label class="form-label !mb-0.5">{{ c.l }}</label>
                            <div class="rounded-lg bg-slate-50 border border-slate-100 px-3 py-2 text-sm text-slate-700 whitespace-pre-wrap">{{ val(d, c.k) }}</div>
                          </div>
                        }
                      }
                      @if (sec.wObj && d.proyecto.objetivos?.length) {
                        <div>
                          <label class="form-label !mb-0.5">Objetivos específicos</label>
                          <ol class="list-decimal ml-5 text-[13px] text-slate-700 space-y-0.5">
                            @for (o of d.proyecto.objetivos; track $index) { <li>{{ o.texto }}</li> }
                          </ol>
                        </div>
                      }
                    </div>
                  </section>
                }
              </div>

              <!-- Rúbrica de evaluación (sticky) -->
              <aside class="lg:sticky lg:top-4 self-start form-card">
                <header class="form-card__head"><h2 class="form-card__title">Rúbrica de evaluación</h2></header>
                @if (d.respuestaEstudiante) {
                  <div class="mb-3 rounded-lg bg-sky-50 border border-sky-100 px-3 py-2">
                    <p class="text-[11px] font-bold text-sky-700 mb-0.5">El estudiante respondió tus observaciones:</p>
                    <p class="text-[12px] text-slate-700 whitespace-pre-wrap">{{ d.respuestaEstudiante }}</p>
                  </div>
                }
                <p class="text-[11px] text-slate-400 mb-3">Escala: Excelente (4) · Bueno (3) · Básico (2) · En proceso (1)</p>

                <div class="space-y-3">
                  @for (cr of d.rubrica; track cr.key) {
                    <div>
                      <p class="text-[12.5px] font-semibold text-slate-700">{{ cr.titulo }}</p>
                      <p class="text-[10.5px] text-slate-400 leading-tight mb-1.5">{{ cr.descripcion }}</p>
                      <div class="flex gap-1">
                        @for (n of [4,3,2,1]; track n) {
                          <button type="button" class="flex-1 h-8 rounded-lg text-[11px] font-bold border transition"
                                  [disabled]="d.cerrada"
                                  [ngClass]="puntajes()[cr.key] === n ? 'bg-[#8C1D2E] text-white border-[#8C1D2E]' : 'text-slate-500 border-slate-200 hover:bg-slate-50 disabled:opacity-50'"
                                  (click)="set(cr.key, n)">{{ n }}</button>
                        }
                      </div>
                    </div>
                  }
                </div>

                <div class="mt-3 flex items-center justify-between text-[12px]">
                  <span class="text-slate-400">Puntaje total</span>
                  <span class="font-extrabold text-[#8C1D2E]">{{ total() }} / {{ d.puntajeMaximo }}</span>
                </div>

                <label class="form-label !mt-3 !mb-1">Observaciones / comentario</label>
                <textarea class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E]"
                          rows="4" [(ngModel)]="comentario" [disabled]="d.cerrada"
                          placeholder="Qué debe corregir el estudiante (obligatorio para observar)…"></textarea>

                @if (!d.cerrada) {
                  <div class="flex gap-2 mt-3">
                    <button mat-stroked-button class="!h-9 !text-[12px] !flex-1 !text-rose-600" [disabled]="!completa() || guardando()" (click)="evaluar(false)">
                      <mat-icon svgIcon="flag" class="size-3.5 mr-1" /> Observar
                    </button>
                    <button mat-flat-button class="!h-9 !text-[12px] !flex-1 !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" [disabled]="!completa() || guardando()" (click)="evaluar(true)">
                      <mat-icon svgIcon="check" class="size-3.5 mr-1" /> Dar conformidad
                    </button>
                  </div>
                  @if (!completa()) { <p class="text-[11px] text-amber-600 mt-1.5">Califica los {{ d.rubrica.length }} criterios para poder evaluar.</p> }
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

  protected data = signal<any | null>(null);
  protected puntajes = signal<Record<string, number>>({});
  protected comentario = '';
  protected guardando = signal(false);
  private tesisId = '';

  protected secciones = computed(() => {
    const d = this.data();
    if (!d) return [];
    const cual = d.proyecto?.enfoque === 'CUALITATIVO';
    return PROY_DEF.filter((s: any) => !(s.cuant && cual) && !(s.cual && !cual) && s.id !== 'p5');
  });

  protected total = computed(() => Object.values(this.puntajes()).reduce((a, b) => a + (b || 0), 0));
  protected completa = computed(() => {
    const d = this.data();
    return !!d && d.rubrica.every((c: any) => this.puntajes()[c.key] > 0);
  });

  ngOnInit(): void {
    this.tesisId = this._route.snapshot.paramMap.get('tesisId') ?? '';
    this.cargar();
  }

  cargar(): void {
    this._svc.detalle$(this.tesisId).subscribe({
      next: (res) => {
        const d = res?.data ?? res;
        this.data.set(d);
        const p: Record<string, number> = {};
        (d?.rubrica ?? []).forEach((c: any) => { if (c.puntaje != null) p[c.key] = c.puntaje; });
        this.puntajes.set(p);
        this.comentario = d?.miComentario ?? '';
      },
      error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo cargar la evaluación'),
    });
  }

  val(d: any, k: string): string { return d?.proyecto?.campos?.[k] ?? ''; }

  set(key: string, n: number): void {
    if (this.data()?.cerrada) return;
    this.puntajes.set({ ...this.puntajes(), [key]: n });
  }

  evaluar(conforme: boolean): void {
    if (!this.completa() || this.guardando()) return;
    if (!conforme && !this.comentario.trim()) { this._toast.error('Escribe las observaciones para el estudiante'); return; }
    this._confirm.confirmSave({
      title: conforme ? 'Dar conformidad al proyecto' : 'Observar el proyecto',
      message: conforme
        ? 'Registrarás tu conformidad con la rúbrica. Una vez dada, tu evaluación queda cerrada. ¿Continuar?'
        : 'Registrarás tus observaciones para que el estudiante las corrija. ¿Continuar?',
    }).then(() => {
      this.guardando.set(true);
      this._svc.evaluar$(this.tesisId, this.puntajes(), this.comentario.trim(), conforme).subscribe({
        next: () => { this._toast.success(conforme ? 'Conformidad registrada' : 'Observaciones registradas'); this.guardando.set(false); this.cargar(); },
        error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo registrar la evaluación'); },
      });
    }).catch(() => {});
  }

  volver(): void { this._router.navigate(['/admin/revisor-proyecto']); }
}
