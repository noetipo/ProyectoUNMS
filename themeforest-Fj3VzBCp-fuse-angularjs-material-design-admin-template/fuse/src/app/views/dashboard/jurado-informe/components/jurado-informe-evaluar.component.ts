import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { JuradoInformeService } from '../services/jurado-informe.service';

/** Jurado Informante · Etapa 7 — evaluación del informe final. */
@Component({
  selector: 'app-jurado-informe-evaluar',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <button mat-button class="!text-slate-500 !text-xs !px-2 mb-1" (click)="volver()">
            <mat-icon svgIcon="chevron-left" class="size-3.5" /> Volver a la bandeja
          </button>
          <h1 class="page-title">Evaluación del informe final</h1>
        </div>
      </div>

      <div class="page-content p-6">
        @if (d(); as data) {
          <div class="mx-auto max-w-[720px] space-y-4">
            <section class="form-card">
              <p class="text-[13px] font-semibold text-slate-800">{{ data.estudianteNombre }} <span class="text-slate-400 font-normal">· {{ data.codigoSistema }}</span></p>
              <p class="text-[12.5px] text-slate-500">{{ data.programaNombre }}</p>
              <p class="text-[13px] font-bold text-[#8C1D2E] mt-1">{{ data.titulo }}</p>
              <div class="flex items-center gap-2 mt-2">
                @if (data.presidente) { <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-[#FDF6F7] text-[#8C1D2E]">Eres el Presidente del Jurado</span> }
                <span class="px-2 py-0.5 rounded text-[11px] font-bold" [ngClass]="data.informeFinalSubido ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'">
                  {{ data.informeFinalSubido ? 'Informe final subido' : 'Informe no subido' }}
                </span>
              </div>
              @if (data.respuestaEstudiante) {
                <div class="mt-3 rounded-lg bg-sky-50 border border-sky-100 px-3 py-2">
                  <p class="text-[11px] font-bold text-sky-700 mb-0.5">El estudiante respondió tus observaciones:</p>
                  <p class="text-[12px] text-slate-700 whitespace-pre-wrap">{{ data.respuestaEstudiante }}</p>
                </div>
              }
              @if (data.cerrada) {
                <div class="mt-2 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-[12px] text-emerald-700 flex items-center gap-1.5">
                  <mat-icon svgIcon="badge-check" class="size-4 shrink-0" /> Ya diste conformidad (puntaje {{ data.miPuntaje }}/{{ data.puntajeMaximo }}). Evaluación cerrada.
                </div>
              }
            </section>

            @if (!data.cerrada) {
              <section class="form-card">
                <header class="form-card__head"><h2 class="form-card__title">Rúbrica del informe final</h2></header>
                <label class="form-label">Puntaje global (0 – {{ data.puntajeMaximo }})</label>
                <input type="number" min="0" [max]="data.puntajeMaximo" class="w-40 rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E]" [(ngModel)]="puntaje" />
                <label class="form-label !mt-3">Observaciones / comentario</label>
                <textarea class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E]"
                          rows="4" [(ngModel)]="comentario" placeholder="Qué debe corregir el estudiante (obligatorio para observar)…"></textarea>
                <div class="flex gap-2 mt-3">
                  <button mat-stroked-button class="!h-9 !text-[12px] !flex-1 !text-rose-600" [disabled]="!valido() || guardando()" (click)="evaluar(false)">
                    <mat-icon svgIcon="flag" class="size-3.5 mr-1" /> Observar
                  </button>
                  <button mat-flat-button class="!h-9 !text-[12px] !flex-1 !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" [disabled]="!valido() || guardando()" (click)="evaluar(true)">
                    <mat-icon svgIcon="check" class="size-3.5 mr-1" /> Dar conformidad
                  </button>
                </div>
              </section>
            }
          </div>
        } @else {
          <p class="text-sm text-slate-400">Cargando…</p>
        }
      </div>
    </div>
  `,
})
export class JuradoInformeEvaluarComponent implements OnInit {
  private _route = inject(ActivatedRoute);
  private _router = inject(Router);
  private _svc = inject(JuradoInformeService);
  private _toast = inject(NotificationService);
  private _confirm = inject(ConfirmDialogService);

  protected d = signal<any | null>(null);
  protected puntaje: number | null = null;
  protected comentario = '';
  protected guardando = signal(false);
  private tesisId = '';

  ngOnInit(): void {
    this.tesisId = this._route.snapshot.paramMap.get('tesisId') ?? '';
    this.cargar();
  }

  cargar(): void {
    this._svc.detalle$(this.tesisId).subscribe({
      next: (res) => { const data = res?.data ?? res; this.d.set(data); this.puntaje = data?.miPuntaje ?? null; this.comentario = data?.miComentario ?? ''; },
      error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo cargar'),
    });
  }

  valido(): boolean {
    const max = this.d()?.puntajeMaximo ?? 20;
    return this.puntaje != null && this.puntaje >= 0 && this.puntaje <= max;
  }

  evaluar(conforme: boolean): void {
    if (!this.valido() || this.guardando()) return;
    if (!conforme && !this.comentario.trim()) { this._toast.error('Escribe las observaciones para el estudiante'); return; }
    this._confirm.confirmSave({
      title: conforme ? 'Dar conformidad al informe final' : 'Observar el informe final',
      message: conforme ? 'Registrarás tu conformidad. Una vez dada, tu evaluación queda cerrada. ¿Continuar?' : 'Registrarás tus observaciones para que el estudiante las corrija. ¿Continuar?',
    }).then(() => {
      this.guardando.set(true);
      this._svc.evaluar$(this.tesisId, this.puntaje!, this.comentario.trim(), conforme).subscribe({
        next: () => { this._toast.success(conforme ? 'Conformidad registrada' : 'Observaciones registradas'); this.guardando.set(false); this.cargar(); },
        error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo registrar'); },
      });
    }).catch(() => {});
  }

  volver(): void { this._router.navigate(['/admin/jurado-informe']); }
}
