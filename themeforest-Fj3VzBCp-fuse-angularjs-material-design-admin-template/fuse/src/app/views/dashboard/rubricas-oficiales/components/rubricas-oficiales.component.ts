import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { previsualizarBlob } from '@/app/shared/perfil-completo/preview.util';
import { RubricaOficialService } from '../services/rubrica-oficial.service';

/**
 * Configuración · Rúbricas oficiales de los revisores.
 *
 * <p>La rúbrica es un documento institucional: no tiene sentido adjuntarla en cada expediente.
 * Aquí vive una por enfoque (cuantitativa/mixta y cualitativa) y se publica una versión nueva
 * cuando la UPG la cambia —normalmente una vez al año—. En la bandeja de la Secretaría solo se
 * <b>habilita</b> la evaluación de cada proyecto.</p>
 *
 * <p>Publicar una versión no toca los proyectos que ya se están evaluando: a cada uno se le
 * congeló la versión que se le aplicó.</p>
 */
@Component({
  selector: 'app-rubricas-oficiales',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Configuración</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Rúbricas oficiales</span>
          </div>
          <h1 class="page-title">Rúbricas oficiales de los revisores</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <div class="mx-auto max-w-[1440px] space-y-3">

          <p class="flex items-start gap-2 text-[12.5px] text-slate-500 rounded-xl border border-slate-100 bg-slate-50/60 px-3.5 py-2.5">
            <mat-icon svgIcon="info" class="size-4 shrink-0 text-slate-400 mt-px" />
            <span>
              El sistema ya trae los criterios y puntajes de cada rúbrica; lo que se publica aquí es el
              <b>Word oficial</b> que consultan los revisores. Al publicar una versión nueva, los proyectos que
              ya se estaban evaluando conservan la suya.
            </span>
          </p>

          @if (cargando()) {
            <p class="text-sm text-slate-400">Cargando…</p>
          } @else {
            <div class="grid gap-3 md:grid-cols-2 items-start">
              @for (p of plantillas(); track p.enfoque) {
                <section class="form-card">
                  <header class="form-card__head">
                    <div class="flex items-start gap-2.5 min-w-0">
                      <mat-icon svgIcon="layers" class="size-[18px] text-[#8C1D2E] shrink-0 mt-0.5" />
                      <div class="min-w-0">
                        <h2 class="form-card__title">Rúbrica {{ p.enfoqueLabel }}</h2>
                        <p class="text-[11px] text-slate-400">{{ p.descripcion }}</p>
                      </div>
                    </div>
                    <span class="shrink-0 text-[10.5px] font-semibold px-2 py-0.5 rounded-full"
                          [class]="p.version ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'">
                      {{ p.version ? 'Vigente v' + p.version : 'Sin publicar' }}
                    </span>
                  </header>

                  <div class="text-[12px] text-slate-500 space-y-1">
                    <p class="flex items-center gap-1.5">
                      <mat-icon svgIcon="list-checks" class="size-3.5 text-slate-300" />
                      {{ p.criterios }} criterios · total {{ p.puntajeTotal }} · aprueba con {{ p.puntajeAprobacion }}
                    </p>
                    @if (p.nombreOriginal) {
                      <p class="flex items-center gap-1.5">
                        <mat-icon svgIcon="file-check" class="size-3.5 text-emerald-600" />
                        <span class="truncate">{{ p.nombreOriginal }}</span>
                        <span class="text-slate-400">· {{ p.fechaCarga | date:'dd/MM/yyyy' }}</span>
                      </p>
                    } @else {
                      <p class="flex items-center gap-1.5 text-amber-600">
                        <mat-icon svgIcon="triangle-alert" class="size-3.5" />
                        Todavía no se publica el Word: la Secretaría no podrá habilitar evaluaciones de este enfoque.
                      </p>
                    }
                  </div>

                  <div class="flex flex-wrap items-center gap-2 mt-3">
                    <input type="text" [(ngModel)]="versiones[p.enfoque]" [placeholder]="anioActual"
                           class="w-24 rounded-lg border border-slate-200 px-2 py-1 text-[11.5px]" />
                    <button mat-flat-button color="primary" class="!h-8 !text-[11.5px]"
                            [disabled]="subiendo()" (click)="elegir(p.enfoque, file)">
                      <mat-icon svgIcon="upload" class="size-3.5 mr-1" />
                      {{ p.version ? 'Publicar versión nueva' : 'Publicar rúbrica' }}
                    </button>
                    @if (p.id) {
                      <div class="row-actions">
                        <button mat-icon-button class="!w-7 !h-7" title="Ver la rúbrica vigente" (click)="ver(p.id, p.enfoqueLabel, p.version)">
                          <mat-icon svgIcon="file-search" class="text-slate-400 size-3.5" />
                        </button>
                      </div>
                    }
                  </div>

                  @if (p.historial?.length) {
                    <div class="mt-3 pt-2.5 border-t border-slate-100">
                      <p class="flex items-center gap-1.5 text-[11px] text-slate-400 mb-1.5">
                        <mat-icon svgIcon="history" class="size-3.5" /> Versiones anteriores
                      </p>
                      <div class="space-y-1">
                        @for (h of p.historial; track h.id) {
                          <div class="flex items-center gap-2 text-[11.5px] text-slate-500">
                            <span class="font-semibold">v{{ h.version }}</span>
                            <span class="truncate flex-1">{{ h.nombreOriginal }}</span>
                            <span class="text-slate-400">{{ h.fechaCarga | date:'dd/MM/yyyy' }}</span>
                            <div class="row-actions">
                              <button mat-icon-button class="!w-7 !h-7" title="Ver esta versión" (click)="ver(h.id, p.enfoqueLabel, h.version)">
                                <mat-icon svgIcon="file-search" class="text-slate-400 size-3.5" />
                              </button>
                            </div>
                          </div>
                        }
                      </div>
                    </div>
                  }
                </section>
              }
            </div>
            <input #file type="file" hidden accept=".docx" (change)="onArchivo($event)" />
          }
        </div>
      </div>
    </div>
  `,
})
export class RubricasOficialesComponent implements OnInit {
  private _svc = inject(RubricaOficialService);
  private _toast = inject(NotificationService);
  private _dialog = inject(MatDialog);

  protected plantillas = signal<any[]>([]);
  protected cargando = signal(true);
  protected subiendo = signal(false);
  protected readonly anioActual = String(new Date().getFullYear());
  /** Versión escrita por enfoque (si se deja vacía, el backend usa el año en curso). */
  protected versiones: Record<string, string> = {};
  private enfoqueDestino = 'CUANTITATIVO';

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando.set(true);
    this._svc.listar$().subscribe({
      next: (res: any) => { this.plantillas.set(res?.data ?? res ?? []); this.cargando.set(false); },
      error: () => { this.cargando.set(false); this._toast.error('No se pudieron cargar las rúbricas'); },
    });
  }

  elegir(enfoque: string, input: HTMLInputElement): void {
    this.enfoqueDestino = enfoque;
    input.value = '';
    input.click();
  }

  onArchivo(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.subiendo.set(true);
    this._svc.publicar$(this.enfoqueDestino, file, this.versiones[this.enfoqueDestino]).subscribe({
      next: () => {
        this.subiendo.set(false);
        this._toast.success('Rúbrica publicada');
        this.versiones[this.enfoqueDestino] = '';
        this.cargar();
      },
      error: (e) => {
        this.subiendo.set(false);
        this._toast.error(e?.error?.message ?? 'No se pudo publicar la rúbrica');
      },
    });
  }

  /** Vista previa del Word en el visor (se renderiza con docx-preview, no se descarga). */
  ver(id: string, label: string, version?: string): void {
    this._svc.documento$(id).subscribe({
      next: (blob) => previsualizarBlob(this._dialog, blob, `Rúbrica ${label}${version ? ' · v' + version : ''}`),
      error: () => this._toast.error('No se pudo abrir el documento'),
    });
  }
}
