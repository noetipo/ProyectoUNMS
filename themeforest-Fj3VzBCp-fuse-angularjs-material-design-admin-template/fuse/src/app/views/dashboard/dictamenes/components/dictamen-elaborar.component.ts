import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { previsualizarBlob } from '@/app/shared/perfil-completo/preview.util';
// El dictamen recién generado sí se abre en pestaña: hay que imprimirlo para la firma.
import { abrirBlob } from '@/app/shared/perfil-completo/download.util';
import { descargarBlob } from '@/app/views/dashboard/reportes/download.util';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { DictamenService } from '../services/dictamen.service';

@Component({
  selector: 'app-dictamen-elaborar',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatDatepickerModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <button mat-button class="!text-slate-500 !text-xs !px-2 mb-1" (click)="volver()">
            <mat-icon svgIcon="chevron-left" class="size-3.5" /> Volver a la bandeja
          </button>
          <h1 class="page-title">Elaborar dictamen de designación</h1>
        </div>
        @if (d(); as det) {
          <span class="text-[11px] font-semibold px-2.5 py-1 rounded-full" [ngClass]="claseEstado(det.estado)">
            {{ etiquetaEstado(det.estado) }}
          </span>
        }
      </div>

      <div class="page-content p-6">
        <div class="mx-auto max-w-[1440px] space-y-3">
          @if (d(); as det) {
            @if (msg()) {
              <div class="flex items-center gap-2 rounded-lg bg-emerald-50 border border-emerald-200 px-3 py-2 text-[12.5px] text-emerald-700">
                <mat-icon svgIcon="circle-check" class="size-4 shrink-0" /> {{ msg() }}
              </div>
            }
            @if (err()) {
              <div class="flex items-center gap-2 rounded-lg bg-rose-50 border border-rose-200 px-3 py-2 text-[12.5px] text-rose-700">
                <mat-icon svgIcon="circle-x" class="size-4 shrink-0" /> {{ err() }}
              </div>
            }

            <!-- Franja del expediente: de quién es el dictamen, en una sola pieza horizontal -->
            <section class="rounded-xl border border-slate-200 overflow-hidden">
              <div class="flex items-start gap-2.5 px-3.5 py-3">
                <mat-icon svgIcon="book-open" class="size-[18px] text-slate-400 shrink-0 mt-0.5" />
                <div class="flex-1 min-w-0">
                  <p class="text-[13px] font-semibold text-slate-800">{{ det.tituloTesis }}</p>
                  <p class="text-[11px] text-slate-400 truncate">{{ det.programaNombre ?? 'Programa no registrado' }}</p>
                </div>
              </div>
              <div class="grid sm:grid-cols-3 border-t border-slate-100 bg-slate-50/60
                          divide-y sm:divide-y-0 sm:divide-x divide-slate-100">
                <div class="flex items-center gap-2 px-3.5 py-2.5 min-w-0">
                  <mat-icon svgIcon="id-card" class="size-4 text-slate-400 shrink-0" />
                  <div class="min-w-0">
                    <p class="text-[10px] uppercase tracking-wide text-slate-400">Doctorando</p>
                    <p class="text-[12.5px] font-semibold text-slate-700 truncate">
                      {{ det.estudianteNombre }} <span class="font-normal text-slate-400">· {{ det.codigoSistema ?? '—' }}</span>
                    </p>
                  </div>
                </div>
                <div class="flex items-center gap-2 px-3.5 py-2.5 min-w-0">
                  <mat-icon svgIcon="handshake" class="size-4 text-[#8C1D2E] shrink-0" />
                  <div class="min-w-0">
                    <p class="text-[10px] uppercase tracking-wide text-slate-400">Asesor</p>
                    <p class="text-[12.5px] font-semibold text-slate-700 truncate">{{ det.asesorGrado }} {{ det.asesorNombre ?? '—' }}</p>
                  </div>
                </div>
                <div class="flex items-center gap-2 px-3.5 py-2.5 min-w-0">
                  <mat-icon svgIcon="users-round" class="size-4 shrink-0" [class]="det.coasesorNombre ? 'text-sky-500' : 'text-slate-300'" />
                  <div class="min-w-0">
                    <p class="text-[10px] uppercase tracking-wide text-slate-400">Co-asesor</p>
                    <p class="text-[12.5px] truncate" [class]="det.coasesorNombre ? 'font-semibold text-slate-700' : 'text-slate-400 italic'">
                      {{ det.coasesorNombre ? det.coasesorGrado + ' ' + det.coasesorNombre : 'sin designar (opcional)' }}
                    </p>
                  </div>
                </div>
              </div>
            </section>

            <!-- Izquierda: lo que la Secretaría revisa · Derecha: lo que emite -->
            <div class="grid gap-3 md:grid-cols-2 items-start">

              <section class="form-card">
                <header class="form-card__head">
                  <h2 class="form-card__title">Documentos firmados del estudiante</h2>
                  <span class="text-[11px] text-slate-400">revísalos antes de emitir</span>
                </header>
                <div class="space-y-1.5">
                  @for (doc of documentos(); track doc.key) {
                    <div class="flex items-center gap-2.5 rounded-lg border border-slate-100 px-2.5 py-2">
                      <mat-icon [svgIcon]="doc.disponible ? 'file-check' : 'file-text'" class="size-4 shrink-0"
                                [class]="doc.disponible ? 'text-emerald-600' : 'text-slate-300'" />
                      <div class="min-w-0 flex-1">
                        <p class="text-[12px] font-medium text-slate-700 truncate">{{ doc.label }}</p>
                        <p class="text-[10.5px]" [class]="doc.disponible ? 'text-emerald-600' : 'text-slate-400'">
                          {{ doc.disponible ? 'Subido por el estudiante' : 'Aún no lo sube' }}
                        </p>
                      </div>
                      <div class="row-actions shrink-0">
                        <button mat-icon-button class="!w-7 !h-7" title="Revisar el documento firmado"
                                [disabled]="!doc.disponible" (click)="verFirmado(doc.key)">
                          <mat-icon svgIcon="file-search" class="text-slate-400 size-3.5" />
                        </button>
                      </div>
                    </div>
                  }
                </div>
                @if (det.motivoObservacion) {
                  <p class="mt-2 text-[11px] text-rose-500">Observados: {{ det.motivoObservacion }}</p>
                }
                <div class="flex justify-end mt-2.5">
                  <button mat-stroked-button class="!h-8 !text-[11.5px] !text-rose-600" [disabled]="saving()" (click)="observar()">
                    <mat-icon svgIcon="triangle-alert" class="size-3.5 mr-1" /> Observar documentos
                  </button>
                </div>
              </section>

              <!-- Emisión: un solo camino de tres pasos (antes "descargar" y "subir" vivían en
                   tarjetas distintas y no se entendía cuál iba primero). -->
              <section class="form-card">
                <header class="form-card__head">
                  <h2 class="form-card__title">Emisión del dictamen</h2>
                  <span class="text-[11px] text-slate-400">redactar → firmar → archivar</span>
                </header>

                <!-- Paso 1 -->
                <div class="flex gap-3">
                  <div class="flex flex-col items-center">
                    <span class="grid place-items-center size-6 rounded-full text-[11px] font-bold shrink-0"
                          [ngClass]="pasoUnoHecho(det) ? 'bg-emerald-50 text-emerald-600 border border-emerald-200' : 'bg-[#8C1D2E] text-white'">1</span>
                    <span class="w-px flex-1 bg-slate-200 my-1"></span>
                  </div>
                  <div class="flex-1 min-w-0 pb-4">
                    <p class="text-[12.5px] font-semibold text-slate-800">Redacta el dictamen</p>
                    <p class="text-[11px] text-slate-400 mb-2">Escribe la numeración oficial de la UPG y la fecha de la solicitud.</p>
                    <form [formGroup]="form" class="grid grid-cols-1 sm:grid-cols-3 gap-x-3 gap-y-2">
                      <div>
                        <label class="form-label">N° de dictamen <span class="form-required">*</span></label>
                        <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                          <input matInput formControlName="numero" placeholder="000003-2026-UPG-VDIF" />
                        </mat-form-field>
                      </div>
                      <div>
                        <label class="form-label">N° expediente digital <span class="form-required">*</span></label>
                        <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                          <input matInput formControlName="expediente" placeholder="EXP-2026-000003" />
                        </mat-form-field>
                      </div>
                      <div>
                        <label class="form-label">Fecha de solicitud <span class="form-required">*</span></label>
                        <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                          <input matInput [matDatepicker]="fechaPicker" formControlName="fechaSolicitud" placeholder="dd/mm/aaaa" readonly (click)="fechaPicker.open()" />
                          <mat-datepicker-toggle matIconSuffix [for]="fechaPicker" />
                          <mat-datepicker #fechaPicker />
                        </mat-form-field>
                      </div>
                    </form>
                    <!-- Un solo botón: el formato es una preferencia, no dos acciones distintas
                         (dos botones se leían como "regenerar" vs "descargar"). -->
                    <div class="flex flex-wrap items-center gap-2 mt-3">
                      <span class="text-[11px] text-slate-400">Se descargará en</span>
                      <div class="inline-flex rounded-lg border border-slate-200 p-0.5">
                        @for (f of formatos; track f.valor) {
                          <button type="button" (click)="formato.set(f.valor)"
                                  class="flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11.5px] font-bold transition"
                                  [ngClass]="formato() === f.valor ? 'bg-[#FDF6F7] text-[#8C1D2E]' : 'text-slate-500 hover:text-slate-700'">
                            <mat-icon [svgIcon]="f.icon" class="size-3.5" /> {{ f.label }}
                          </button>
                        }
                      </div>
                      <button mat-flat-button color="primary" class="!h-8 !text-[11.5px]" [disabled]="saving()" (click)="generar(formato())">
                        <mat-icon svgIcon="stamp" class="size-3.5 mr-1" />
                        {{ pasoUnoHecho(det) ? 'Regenerar y descargar' : 'Generar y descargar' }}
                      </button>
                    </div>
                  </div>
                </div>

                <!-- Paso 2 -->
                <div class="flex gap-3">
                  <div class="flex flex-col items-center">
                    <span class="grid place-items-center size-6 rounded-full text-[11px] font-bold shrink-0"
                          [ngClass]="det.dictamenFirmadoSubido ? 'bg-emerald-50 text-emerald-600 border border-emerald-200'
                                   : pasoUnoHecho(det) ? 'bg-[#8C1D2E] text-white' : 'bg-slate-100 text-slate-400'">2</span>
                    <span class="w-px flex-1 bg-slate-200 my-1"></span>
                  </div>
                  <div class="flex-1 min-w-0 pb-4">
                    <p class="text-[12.5px] font-semibold" [class]="pasoUnoHecho(det) ? 'text-slate-800' : 'text-slate-400'">
                      Recaba la firma del Director
                    </p>
                    <p class="text-[11px] text-slate-400">Imprime el dictamen descargado y hazlo firmar por el Director de la UPG.</p>
                  </div>
                </div>

                <!-- Paso 3 -->
                <div class="flex gap-3">
                  <span class="grid place-items-center size-6 rounded-full text-[11px] font-bold shrink-0 self-start"
                        [ngClass]="det.dictamenFirmadoSubido ? 'bg-emerald-50 text-emerald-600 border border-emerald-200'
                                 : pasoUnoHecho(det) ? 'bg-[#8C1D2E] text-white' : 'bg-slate-100 text-slate-400'">3</span>
                  <div class="flex-1 min-w-0">
                    <p class="text-[12.5px] font-semibold" [class]="puedeSubir(det) ? 'text-slate-800' : 'text-slate-400'">
                      Sube el dictamen firmado
                    </p>
                    <p class="text-[11px] text-slate-400 mb-2">
                      {{ det.dictamenFirmadoSubido ? 'Ya está archivado: el estudiante puede descargarlo desde Mi asesoría.'
                         : (puedeSubir(det) ? 'Escanéalo en PDF y súbelo para cerrar el trámite.' : 'Se habilita cuando generes el dictamen.') }}
                    </p>
                    <div class="flex flex-wrap items-center gap-2">
                      <button mat-flat-button color="primary" class="!h-8 !text-[11.5px]"
                              [disabled]="subiendo() || !puedeSubir(det)" (click)="fileDic.click()">
                        <mat-icon svgIcon="upload" class="size-3.5 mr-1" />
                        {{ det.dictamenFirmadoSubido ? 'Reemplazar el firmado' : 'Subir el firmado' }}
                      </button>
                      @if (det.dictamenFirmadoSubido) {
                        <span class="inline-flex items-center gap-1 text-[11px] text-emerald-600">
                          <mat-icon svgIcon="badge-check" class="size-3.5" /> Trámite cerrado
                        </span>
                      }
                    </div>
                    <input #fileDic type="file" hidden accept=".pdf,.docx" (change)="onFile($event)" />
                  </div>
                </div>
              </section>
            </div>
          } @else {
            <p class="text-sm text-slate-400">Cargando…</p>
          }
        </div>
      </div>
    </div>
  `,
})
export class DictamenElaborarComponent implements OnInit {
  private _route = inject(ActivatedRoute);
  private _router = inject(Router);
  private _svc = inject(DictamenService);
  private _fb = inject(FormBuilder);
  private _confirm = inject(ConfirmDialogService);
  private _dialog = inject(MatDialog);

  protected d = signal<any | null>(null);
  protected saving = signal(false);
  protected subiendo = signal(false);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);
  private tesisId = '';

  /** La numeración es manual: la UPG lleva sus propios correlativos (no se autogeneran). */
  protected form: FormGroup = this._fb.group({
    numero: ['', Validators.required],
    expediente: ['', Validators.required],
    fechaSolicitud: [null as Date | null, Validators.required],
  });

  /** Formato elegido para la descarga (el PDF es el que se firma; el Word, por si hay que editar). */
  protected formato = signal<'pdf' | 'docx'>('pdf');
  protected readonly formatos = [
    { valor: 'pdf' as const, label: 'PDF', icon: 'file-text' },
    { valor: 'docx' as const, label: 'Word', icon: 'file-type' },
  ];

  /** ¿Ya se redactó el dictamen? (paso 1 del flujo de emisión) */
  protected pasoUnoHecho(det: any): boolean {
    return !!det?.numero && det?.estado !== 'POR_ELABORAR' && det?.estado !== 'OBSERVADO';
  }

  /** Los dos firmados que la Secretaría revisa antes de emitir. */
  protected documentos = computed(() => {
    const det = this.d();
    return [
      { key: 'solicitud' as const, label: 'Solicitud de asesoría', disponible: !!det?.solicitudFirmadaDisponible },
      { key: 'carta' as const, label: 'Carta de aceptación del asesor', disponible: !!det?.cartaFirmadaDisponible },
    ];
  });

  /** El firmado se sube recién cuando el dictamen ya fue elaborado. */
  protected puedeSubir(det: any): boolean {
    return !!det?.estado && det.estado !== 'POR_ELABORAR' && det.estado !== 'OBSERVADO';
  }

  protected etiquetaEstado(estado?: string): string {
    switch (estado) {
      case 'ELABORADO': return 'Falta la firma';
      case 'FIRMADO': return 'Firmado y entregado';
      case 'OBSERVADO': return 'Documentos observados';
      default: return 'Sin redactar';
    }
  }

  /** Misma convención que la bandeja: granate = te toca · ámbar = esperando · esmeralda = listo. */
  protected claseEstado(estado?: string): string {
    switch (estado) {
      case 'ELABORADO': return 'bg-amber-50 text-amber-700';
      case 'FIRMADO': return 'bg-emerald-50 text-emerald-700';
      case 'OBSERVADO': return 'bg-rose-50 text-rose-600';
      default: return 'bg-[#FDF6F7] text-[#8C1D2E]';
    }
  }

  ngOnInit(): void {
    this.tesisId = this._route.snapshot.paramMap.get('tesisId') ?? '';
    this.cargar();
  }

  cargar(): void {
    this._svc.detalle$(this.tesisId).subscribe({
      next: (res) => {
        const det = res?.data ?? res;
        this.d.set(det);
        const fecha = this.parseFecha(det?.fechaSolicitud);
        // La numeración ya registrada se trae para poder corregirla; si no hay, el campo queda
        // vacío con el formato de ejemplo en el placeholder.
        this.form.patchValue({
          numero: det?.numero ?? '',
          expediente: det?.expediente ?? '',
          ...(fecha ? { fechaSolicitud: fecha } : {}),
        });
      },
      error: () => this.err.set('No se pudo cargar el detalle'),
    });
  }

  /** Vista previa en modal: la Secretaría revisa el firmado sin llenarse el disco de descargas. */
  verFirmado(tipo: 'solicitud' | 'carta'): void {
    this._svc.descargarFirmado$(this.tesisId, tipo).subscribe({
      next: (blob) => previsualizarBlob(this._dialog, blob,
        tipo === 'solicitud' ? 'Solicitud de asesoría firmada' : 'Carta de aceptación firmada'),
      error: () => this.err.set('No se pudo abrir el documento'),
    });
  }

  generar(formato: 'pdf' | 'docx'): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.err.set('Completa el N° de dictamen, el N° de expediente y la fecha de solicitud.');
      return;
    }
    this.saving.set(true); this.err.set(null);
    const { numero, expediente, fechaSolicitud } = this.form.value;
    this._svc.elaborar$(this.tesisId, this.fmtFecha(fechaSolicitud), numero?.trim(), expediente?.trim()).subscribe({
      next: () => {
        this._svc.documento$(this.tesisId, formato).subscribe({
          next: (blob) => {
            if (formato === 'docx') descargarBlob(blob, 'dictamen_designacion.docx'); else abrirBlob(blob, 'dictamen_designacion.pdf');
            this.saving.set(false); this.msg.set('Dictamen generado.'); this.cargar();
          },
          error: () => { this.saving.set(false); this.err.set('Se elaboró pero no se pudo descargar'); this.cargar(); },
        });
      },
      error: (e) => { this.saving.set(false); this.err.set(e?.error?.message ?? 'No se pudo elaborar el dictamen'); },
    });
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    const yaSubido = !!this.d()?.dictamenFirmadoSubido;
    // Subirlo cierra el trámite del estudiante: se confirma en vez de subirlo apenas se elige.
    this._confirm.confirmSave({
      title: yaSubido ? 'Reemplazar el dictamen firmado' : 'Subir el dictamen firmado',
      message: `Se subirá «${file.name}». Al confirmar:`,
      details: yaSubido
        ? ['reemplaza el archivo que habías subido antes', 'el estudiante lo verá firmado de inmediato']
        : ['el dictamen queda emitido y archivado', 'el estudiante recibe el aviso y puede descargarlo desde Mi asesoría'],
      confirmLabel: yaSubido ? 'Reemplazar' : 'Subir dictamen',
    }).then(() => {
      this.subiendo.set(true); this.err.set(null);
      this._svc.subirFirmado$(this.tesisId, file).subscribe({
        next: () => { this.subiendo.set(false); this.msg.set('Dictamen firmado subido.'); this.cargar(); },
        error: (e) => { this.subiendo.set(false); this.err.set(e?.error?.message ?? 'No se pudo subir el archivo'); },
      });
    }).catch(() => {});
  }

  observar(): void {
    this._confirm.confirmSave({ title: 'Observar documentos', message: 'Se marcará el dictamen como OBSERVADO y el estudiante deberá re-subir sus documentos. Indica el motivo en el siguiente paso.' })
      .then(() => {
        const motivo = window.prompt('Motivo de la observación:');
        if (!motivo) return;
        this._svc.observar$(this.tesisId, motivo).subscribe({
          next: () => { this.msg.set('Documentos observados.'); this._router.navigate(['/admin/dictamenes']); },
          error: (e) => this.err.set(e?.error?.message ?? 'No se pudo observar'),
        });
      }).catch(() => {});
  }

  volver(): void { this._router.navigate(['/admin/dictamenes']); }

  /** ISO "yyyy-MM-dd" del backend → Date local (evita el corrimiento de un día por UTC). */
  private parseFecha(s?: string | null): Date | null {
    if (!s) return null;
    const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(s);
    return m ? new Date(+m[1], +m[2] - 1, +m[3]) : new Date(s);
  }

  /** Date del datepicker → "yyyy-MM-dd" para el backend (LocalDate). */
  private fmtFecha(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
