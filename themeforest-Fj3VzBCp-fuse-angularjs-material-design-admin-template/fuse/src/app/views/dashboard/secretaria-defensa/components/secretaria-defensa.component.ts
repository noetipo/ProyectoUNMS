import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { SecretariaDefensaService } from '../services/secretaria-defensa.service';
import { ProgramarDefensaDialogComponent } from './programar-defensa-dialog.component';

/**
 * Secretaría · Etapa 5 (Defensa) — paso 1: bandeja de solicitudes de aprobación recibidas.
 * La Secretaría recibe el expediente y comunica al Coordinador del Programa.
 */
@Component({
  selector: 'app-secretaria-defensa',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 5 · Defensa del proyecto</p>
          <h1 class="page-title">Solicitudes de aprobación recibidas</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <p class="text-[12px] text-slate-500 mb-3">
          Expedientes enviados por el estudiante (solicitud de aprobación). Recíbelos y comunícalos al Coordinador del Programa para que designe los revisores.
        </p>

        <div class="data-table">
          <table class="w-full text-sm">
            <thead><tr class="text-slate-400 text-left">
              <th class="py-2 px-3 font-medium">Estudiante</th>
              <th class="py-2 px-3 font-medium">Programa</th>
              <th class="py-2 px-3 font-medium">Título</th>
              <th class="py-2 px-3 font-medium">Fecha solicitud</th>
              <th class="py-2 px-3 font-medium">Estado</th>
              <th></th>
            </tr></thead>
            <tbody>
              @for (r of rows(); track r.tesisId) {
                <tr class="border-t border-slate-100"
                    [ngClass]="r.tesisId === resaltado() ? 'bg-amber-50 ring-1 ring-inset ring-amber-300' : ''">
                  <td class="py-2 px-3 text-slate-700">{{ r.estudianteApellidos }}, {{ r.estudianteNombres }}<br><span class="text-[11px] text-slate-400">{{ r.codigoSistema }}</span></td>
                  <td class="py-2 px-3 text-slate-500">{{ r.programaNombre }}</td>
                  <td class="py-2 px-3 text-slate-600 max-w-[300px] truncate">{{ r.tituloTesis }}</td>
                  <td class="py-2 px-3 text-slate-500">{{ r.fechaSolicitud | date:'dd/MM/yyyy' }}</td>
                  <td class="py-2 px-3">
                    <span class="status-badge px-2 py-0.5 rounded text-[11px] font-bold"
                          [ngClass]="r.recibido ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'">
                      {{ r.recibido ? 'RECIBIDO' : 'POR RECIBIR' }}
                    </span>
                  </td>
                  <td class="py-2 px-3 text-right">
                    @if (!r.recibido) {
                      <button mat-flat-button color="primary" class="!h-8 !text-xs" (click)="recibir(r)">
                        <mat-icon svgIcon="check" class="size-3.5 mr-1" /> Recibir y comunicar al Coordinador
                      </button>
                    } @else {
                      <span class="text-[11px] text-slate-400">Comunicado al Coordinador</span>
                    }
                  </td>
                </tr>
              }
              @if (!rows().length && !loading()) {
                <tr><td colspan="6" class="py-6 text-center text-sm text-slate-400">No hay solicitudes de aprobación por recibir.</td></tr>
              }
            </tbody>
          </table>
        </div>

        <!-- Rúbricas de los revisores -->
        <div class="mt-8">
          <h2 class="text-[15px] font-bold text-slate-700">Rúbricas de los revisores</h2>
          <p class="text-[12px] text-slate-500 mb-3">
            Proyectos con revisores designados. Sube la rúbrica oficial en <b>Word (.docx)</b> del tipo correspondiente;
            el sistema la <b>convierte a Excel</b> y <b>habilita la evaluación</b> de los revisores.
          </p>
          <div class="data-table">
            <table class="w-full text-sm">
              <thead><tr class="text-slate-400 text-left">
                <th class="py-2 px-3 font-medium">Estudiante</th>
                <th class="py-2 px-3 font-medium">Tipo de rúbrica</th>
                <th class="py-2 px-3 font-medium">Revisores</th>
                <th class="py-2 px-3 font-medium">Estado</th>
                <th></th>
              </tr></thead>
              <tbody>
                @for (r of rubricas(); track r.tesisId) {
                  <tr class="border-t border-slate-100"
                      [ngClass]="r.tesisId === resaltado() ? 'bg-amber-50 ring-1 ring-inset ring-amber-300' : ''">
                    <td class="py-2 px-3 text-slate-700">{{ r.estudianteApellidos }}, {{ r.estudianteNombres }}<br><span class="text-[11px] text-slate-400">{{ r.programaNombre }}</span></td>
                    <td class="py-2 px-3">
                      <span class="px-2 py-0.5 rounded text-[11px] font-bold"
                            [ngClass]="r.enfoque === 'CUALITATIVO' ? 'bg-indigo-100 text-indigo-700' : 'bg-[#FDF6F7] text-[#8C1D2E]'">
                        {{ r.enfoque === 'CUALITATIVO' ? 'Cualitativo' : 'Cuantitativo / Mixto' }}
                      </span>
                    </td>
                    <td class="py-2 px-3 text-slate-600 text-[11.5px]">
                      @if (r.revisores?.length) {
                        @for (rv of r.revisores; track $index) { <div class="whitespace-nowrap">• {{ rv }}</div> }
                      } @else { <span class="text-slate-400">{{ r.numRevisores }} designado(s)</span> }
                    </td>
                    <td class="py-2 px-3">
                      <!-- La rúbrica oficial ya vive en el sistema: aquí solo se enciende la
                           evaluación. El interruptor congela la versión vigente del enfoque. -->
                      <div class="flex items-center gap-2">
                        <button type="button" role="switch" [attr.aria-checked]="habilitada(r)"
                                [disabled]="cambiando() === r.tesisId || (!habilitada(r) && !r.plantillaVersion)"
                                [title]="!r.plantillaVersion && !habilitada(r)
                                  ? 'Aún no se ha publicado la rúbrica oficial de este enfoque'
                                  : (habilitada(r) ? 'Suspender la evaluación' : 'Habilitar la evaluación')"
                                (click)="alternarRubrica(r)"
                                class="relative w-9 h-5 rounded-full transition disabled:opacity-40"
                                [ngClass]="habilitada(r) ? 'bg-[#8C1D2E]' : 'bg-slate-200'">
                          <span class="absolute top-0.5 size-4 rounded-full bg-white shadow transition-all"
                                [ngClass]="habilitada(r) ? 'left-[18px]' : 'left-0.5'"></span>
                        </button>
                        <span class="text-[11px] font-semibold"
                              [ngClass]="habilitada(r) ? 'text-emerald-600' : 'text-slate-400'">
                          {{ habilitada(r) ? 'Habilitada' : 'Deshabilitada' }}
                        </span>
                      </div>
                      <span class="block text-[10.5px] text-slate-400 mt-0.5">
                        @if (r.plantillaVersionAplicada) {
                          {{ r.plantillaLabel }} · v{{ r.plantillaVersionAplicada }}
                        } @else if (r.plantillaVersion) {
                          {{ r.plantillaLabel }} · v{{ r.plantillaVersion }} vigente
                        } @else if (r.rubricaSubida) {
                          Word propio del expediente
                        } @else {
                          <span class="text-amber-600">Falta publicar la rúbrica oficial</span>
                        }
                      </span>
                    </td>
                    <td class="py-2 px-3 text-right">
                      <div class="flex flex-col items-end gap-1">
                        <div class="row-actions">
                          <!-- file-search = revisar un documento (convención del sistema). -->
                          <button mat-icon-button class="!w-7 !h-7" title="Ver la rúbrica que se aplica"
                                  [disabled]="!r.plantillaVersion && !r.rubricaSubida" (click)="preview(r)">
                            <mat-icon svgIcon="file-search" class="text-slate-400 size-3.5" />
                          </button>
                          <button mat-icon-button class="!w-7 !h-7" title="Adjuntar un Word propio para este expediente (excepcional)"
                                  [disabled]="subiendo()" (click)="elegirArchivo(r)">
                            <mat-icon svgIcon="upload" class="text-slate-400 size-3.5" />
                          </button>
                        </div>
                        @if (r.defensaProgramada) {
                          <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10.5px] font-bold bg-emerald-100 text-emerald-700">
                            <mat-icon svgIcon="calendar-check" class="size-3.5" /> Defensa: {{ r.fechaDefensa | date:'dd/MM/yyyy' }}@if (r.horaDefensa) { · {{ r.horaDefensa }} }
                          </span>
                        } @else if (r.revisoresConformes) {
                          <button mat-stroked-button class="!h-8 !text-xs !text-[#8C1D2E] !border-[#8C1D2E]/30" (click)="programarDefensa(r)">
                            <mat-icon svgIcon="calendar-check" class="size-3.5 mr-1" /> Programar defensa
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
                @if (!rubricas().length && !loading()) {
                  <tr><td colspan="5" class="py-6 text-center text-sm text-slate-400">No hay proyectos con revisores designados aún.</td></tr>
                }
              </tbody>
            </table>
          </div>
          <input #fileInput type="file" class="hidden"
                 accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                 (change)="onArchivo($event)" />
        </div>
      </div>

      <!-- Vista previa del documento Word renderizado -->
      @if (previewOpen()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" (click)="cerrarPreview()">
          <div class="bg-white rounded-xl shadow-xl max-w-[920px] w-full max-h-[88vh] flex flex-col" (click)="$event.stopPropagation()">
            <div class="flex items-center gap-2 px-4 py-3 border-b border-slate-100">
              <mat-icon svgIcon="eye" class="size-4 text-[#8C1D2E]" />
              <h3 class="text-[13px] font-bold text-slate-700 truncate">Vista previa · {{ previewNombre() }}</h3>
              <button mat-stroked-button class="!h-8 !text-xs !text-slate-600 ml-auto shrink-0" [disabled]="previewCargando()" (click)="descargarRubrica()">
                <mat-icon svgIcon="download" class="size-3.5 mr-1" /> Descargar Word
              </button>
              <button mat-icon-button class="!size-8 shrink-0" (click)="cerrarPreview()"><mat-icon svgIcon="x" class="size-4" /></button>
            </div>
            <div class="flex-1 overflow-auto bg-slate-100 p-4">
              @if (previewCargando()) { <p class="text-slate-400 text-[12px] text-center py-8">Cargando documento…</p> }
              @if (previewError()) {
                <div class="text-center py-8">
                  <mat-icon svgIcon="file-text" class="!size-12 text-slate-300 mb-2" />
                  <p class="text-[12.5px] text-slate-500">{{ previewError() }}</p>
                </div>
              }
              <div #docxHost class="docx-host"></div>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    :host ::ng-deep .docx-host .docx-wrapper { background: transparent; padding: 0; }
    :host ::ng-deep .docx-host .docx-wrapper > section.docx {
      margin: 0 auto 1rem; box-shadow: 0 1px 8px rgba(15,23,42,.15); background: #fff;
    }
  `],
})
export class SecretariaDefensaComponent implements OnInit {
  private _svc = inject(SecretariaDefensaService);
  private _confirm = inject(ConfirmDialogService);
  private _toast = inject(NotificationService);
  private _dialog = inject(MatDialog);

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('docxHost') docxHost?: ElementRef<HTMLElement>;

  private _route = inject(ActivatedRoute);

  protected rows = signal<any[]>([]);
  protected rubricas = signal<any[]>([]);
  protected loading = signal(true);
  /** ?tesis={id} — llega desde el tablero de seguimiento para señalar de qué alumno se trata. */
  protected resaltado = signal<string | null>(null);
  protected subiendo = signal(false);
  /** tesisId cuyo interruptor está en vuelo (evita dobles clics). */
  protected cambiando = signal<string | null>(null);
  private targetTesis: string | null = null;

  protected previewOpen = signal(false);
  protected previewNombre = signal('');
  protected previewCargando = signal(false);
  protected previewError = signal<string | null>(null);
  private previewBlob: Blob | null = null;

  ngOnInit(): void {
    this.resaltado.set(this._route.snapshot.queryParamMap.get('tesis'));
    this.cargar();
    this.cargarRubricas();
  }

  cargar(): void {
    this.loading.set(true);
    this._svc.bandeja$().subscribe({
      next: (res) => { const d = res?.data ?? res; this.rows.set(d?.content ?? d ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  cargarRubricas(): void {
    this._svc.bandejaRubricas$().subscribe({
      next: (res) => { const d = res?.data ?? res; this.rubricas.set(Array.isArray(d) ? d : (d?.content ?? [])); },
      error: () => {},
    });
  }

  elegirArchivo(r: any): void {
    this.targetTesis = r.tesisId;
    this.fileInput.nativeElement.value = '';
    this.fileInput.nativeElement.click();
  }

  onArchivo(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.targetTesis) return;
    this.subiendo.set(true);
    this._svc.subirRubrica$(this.targetTesis, file).subscribe({
      next: () => { this._toast.success('Rúbrica subida; los revisores ya pueden evaluar'); this.subiendo.set(false); this.cargarRubricas(); },
      error: (e) => { this.subiendo.set(false); this._toast.error(e?.error?.message ?? 'No se pudo subir la rúbrica'); },
    });
  }

  /** Abre el modal para programar la defensa (Jurado Examinador + fecha/hora/lugar). */
  programarDefensa(r: any): void {
    this._dialog.open(ProgramarDefensaDialogComponent, {
      width: '540px', maxWidth: '92vw', autoFocus: false,
      data: { tesisId: r.tesisId, estudiante: (r.estudianteApellidos + ', ' + r.estudianteNombres).trim(), titulo: r.tituloTesis },
    }).afterClosed().subscribe((ok: boolean) => { if (ok) this.cargarRubricas(); });
  }

  /** ¿La evaluación está habilitada? (o el expediente trae su Word propio, modo anterior) */
  habilitada(r: any): boolean {
    return !!r?.rubricaHabilitada || !!r?.rubricaSubida;
  }

  /** Enciende/suspende la evaluación. Al habilitar se congela la versión vigente de la plantilla. */
  alternarRubrica(r: any): void {
    const habilitar = !this.habilitada(r);
    this.cambiando.set(r.tesisId);
    this._svc.habilitarRubrica$(r.tesisId, habilitar).subscribe({
      next: (res) => {
        this.cambiando.set(null);
        this._toast.success(res?.message ?? (habilitar ? 'Evaluación habilitada' : 'Evaluación suspendida'));
        this.cargarRubricas();
      },
      error: (e) => {
        this.cambiando.set(null);
        this._toast.error(e?.error?.message ?? 'No se pudo cambiar el estado de la evaluación');
      },
    });
  }

  /**
   * Abre el modal y renderiza el Word real (docx → HTML) con docx-preview. Muestra la rúbrica
   * oficial vigente del enfoque; si el expediente tiene su propio Word adjunto, ese manda.
   */
  preview(r: any): void {
    this.previewOpen.set(true);
    this.previewNombre.set(r.rubricaSubida
      ? (r.rubricaNombreArchivo || 'Rúbrica del expediente')
      : `Rúbrica ${r.plantillaLabel ?? 'oficial'}${r.plantillaVersion ? ' · v' + r.plantillaVersion : ''}`);
    this.previewError.set(null);
    this.previewBlob = null;
    this.previewCargando.set(true);
    const origen$ = r.rubricaSubida
      ? this._svc.descargarRubricaRaw$(r.tesisId)
      : this._svc.rubricaOficialRaw$(r.enfoque === 'CUALITATIVO' ? 'CUALITATIVO' : 'CUANTITATIVO');
    origen$.subscribe({
      next: (blob) => { this.previewBlob = blob; this.renderDocx(blob); },
      error: (e) => { this.previewCargando.set(false); this.previewError.set(e?.error?.message ?? 'No se pudo cargar el documento'); },
    });
  }

  private async renderDocx(blob: Blob): Promise<void> {
    const limite = <T,>(p: Promise<T>, ms: number) =>
      Promise.race([p, new Promise<T>((_, rej) => setTimeout(() => rej(new Error('timeout')), ms))]);
    try {
      const host = await this.esperarHost();
      host.innerHTML = '';
      const mod = await limite(import('docx-preview'), 20000);
      await limite(mod.renderAsync(blob, host, undefined, {
        className: 'docx', inWrapper: true, breakPages: true,
        ignoreWidth: false, ignoreHeight: false, useBase64URL: true, experimental: true,
      }), 20000);
      this.previewCargando.set(false);
    } catch {
      this.previewCargando.set(false);
      this.previewError.set('No se pudo renderizar el documento. Descárgalo para verlo con Office.');
    }
  }

  /** Espera a que el contenedor #docxHost exista en el DOM (el modal se abre por señal). */
  private esperarHost(): Promise<HTMLElement> {
    return new Promise((resolve, reject) => {
      let n = 0;
      const tick = () => {
        const el = this.docxHost?.nativeElement;
        if (el) { resolve(el); return; }
        if (n++ > 60) { reject(new Error('host no disponible')); return; }
        setTimeout(tick, 20);
      };
      tick();
    });
  }

  cerrarPreview(): void {
    this.previewOpen.set(false);
    this.previewBlob = null;
    if (this.docxHost?.nativeElement) this.docxHost.nativeElement.innerHTML = '';
  }

  descargarRubrica(): void {
    if (!this.previewBlob) return;
    const url = URL.createObjectURL(this.previewBlob);
    const a = document.createElement('a');
    a.href = url;
    a.download = this.previewNombre() || 'rubrica.docx';
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  recibir(r: any): void {
    this._confirm.confirmSave({
      title: 'Recibir expediente',
      message: 'Se marcará el expediente como recibido y se comunicará al Coordinador del Programa para que designe los revisores.',
    }).then(() => {
      this._svc.recibir$(r.tesisId).subscribe({
        next: () => { this._toast.success('Expediente recibido y comunicado al Coordinador'); this.cargar(); },
        error: (e) => this._toast.error(e?.error?.message ?? 'No se pudo recibir el expediente'),
      });
    }).catch(() => {});
  }
}
