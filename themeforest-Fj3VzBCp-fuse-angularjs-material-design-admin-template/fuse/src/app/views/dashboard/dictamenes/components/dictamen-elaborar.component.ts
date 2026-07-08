import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router } from '@angular/router';
import { abrirBlob } from '@/app/shared/perfil-completo/download.util';
import { descargarBlob } from '@/app/views/dashboard/reportes/download.util';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { DictamenService } from '../services/dictamen.service';

@Component({
  selector: 'app-dictamen-elaborar',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <button mat-button class="!text-slate-500 !text-xs !px-2 mb-1" (click)="volver()">
            <mat-icon svgIcon="chevron-left" class="size-3.5" /> Volver a la bandeja
          </button>
          <h1 class="page-title">Elaborar dictamen de designación</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <div class="mx-auto max-w-[900px] space-y-4">
          @if (d(); as det) {
            @if (msg()) { <div class="rounded-lg bg-emerald-50 border border-emerald-200 px-3 py-2 text-sm text-emerald-700">{{ msg() }}</div> }
            @if (err()) { <div class="rounded-lg bg-rose-50 border border-rose-200 px-3 py-2 text-sm text-rose-700">{{ err() }}</div> }

            <!-- Estudiante / tesis -->
            <section class="form-card">
              <header class="form-card__head"><h2 class="form-card__title">Estudiante y tesis</h2></header>
              <div class="form-grid">
                <div><span class="form-label">Estudiante</span><p class="text-sm text-slate-700">{{ det.estudianteNombre }}</p></div>
                <div><span class="form-label">Código</span><p class="text-sm text-slate-700">{{ det.codigoSistema ?? '—' }}</p></div>
                <div><span class="form-label">Programa</span><p class="text-sm text-slate-700">{{ det.programaNombre ?? '—' }}</p></div>
                <div class="sm:col-span-2 lg:col-span-3"><span class="form-label">Título de tesis</span><p class="text-sm text-slate-700">{{ det.tituloTesis }}</p></div>
                <div><span class="form-label">Asesor</span><p class="text-sm text-slate-700">{{ det.asesorGrado }} {{ det.asesorNombre ?? '—' }}</p></div>
                @if (det.coasesorNombre) { <div><span class="form-label">Co-asesor</span><p class="text-sm text-slate-700">{{ det.coasesorGrado }} {{ det.coasesorNombre }}</p></div> }
              </div>
            </section>

            <!-- Documentos del estudiante -->
            <section class="form-card">
              <header class="form-card__head"><h2 class="form-card__title">Documentos firmados del estudiante</h2></header>
              <div class="flex flex-wrap gap-2">
                <button mat-stroked-button class="!h-8 !text-xs" [disabled]="!det.solicitudFirmadaDisponible" (click)="verFirmado('solicitud')">
                  <mat-icon svgIcon="file-text" class="size-3.5 mr-1" /> Solicitud firmada
                </button>
                <button mat-stroked-button class="!h-8 !text-xs" [disabled]="!det.cartaFirmadaDisponible" (click)="verFirmado('carta')">
                  <mat-icon svgIcon="file-text" class="size-3.5 mr-1" /> Carta firmada
                </button>
              </div>
            </section>

            <!-- Datos del dictamen -->
            <section class="form-card">
              <header class="form-card__head"><h2 class="form-card__title">Datos del dictamen</h2></header>
              <form [formGroup]="form" class="form-grid">
                <div>
                  <label class="form-label">N° de dictamen</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput [value]="det.numero ?? '(se asigna al generar)'" disabled />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">N° expediente digital <span class="form-required">*</span></label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput formControlName="expediente" placeholder="Ej. EXP-2026-000123" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Fecha de solicitud <span class="form-required">*</span></label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput type="date" formControlName="fechaSolicitud" />
                  </mat-form-field>
                </div>
              </form>
              <div class="flex flex-wrap gap-2 mt-2">
                <button mat-flat-button color="primary" class="!h-9 !text-sm" [disabled]="saving()" (click)="generar('pdf')">
                  <mat-icon svgIcon="file-text" class="size-3.5 mr-1" /> Generar y descargar (PDF)
                </button>
                <button mat-stroked-button class="!h-9 !text-sm" [disabled]="saving()" (click)="generar('docx')">
                  <mat-icon svgIcon="download" class="size-3.5 mr-1" /> Word
                </button>
                <button mat-button class="!h-9 !text-sm !text-rose-500" [disabled]="saving()" (click)="observar()">Observar documentos</button>
              </div>
            </section>

            <!-- Subir dictamen firmado -->
            <section class="form-card">
              <header class="form-card__head"><h2 class="form-card__title">Dictamen firmado por el Director</h2></header>
              @if (det.dictamenFirmadoSubido) {
                <div class="rounded-lg bg-emerald-50 border border-emerald-200 px-3 py-2 text-sm text-emerald-700 mb-2">
                  Dictamen firmado subido. El estudiante ya puede verlo.
                </div>
              }
              <button mat-flat-button color="primary" class="!h-9 !text-sm" [disabled]="subiendo() || det.estado === 'POR_ELABORAR' || det.estado === 'OBSERVADO' || !det.estado" (click)="fileDic.click()">
                <mat-icon svgIcon="upload" class="size-3.5 mr-1" /> {{ det.dictamenFirmadoSubido ? 'Reemplazar' : 'Subir dictamen firmado' }}
              </button>
              @if (det.estado === 'POR_ELABORAR' || !det.estado) { <p class="text-[11px] text-slate-400 mt-1">Primero genera el dictamen.</p> }
              <input #fileDic type="file" hidden accept=".pdf,.docx" (change)="onFile($event)" />
            </section>
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

  protected d = signal<any | null>(null);
  protected saving = signal(false);
  protected subiendo = signal(false);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);
  private tesisId = '';

  protected form: FormGroup = this._fb.group({
    expediente: ['', Validators.required],
    fechaSolicitud: ['', Validators.required],
  });

  ngOnInit(): void {
    this.tesisId = this._route.snapshot.paramMap.get('tesisId') ?? '';
    this.cargar();
  }

  cargar(): void {
    this._svc.detalle$(this.tesisId).subscribe({
      next: (res) => {
        const det = res?.data ?? res;
        this.d.set(det);
        if (det?.expediente) this.form.patchValue({ expediente: det.expediente });
        if (det?.fechaSolicitud) this.form.patchValue({ fechaSolicitud: det.fechaSolicitud });
      },
      error: () => this.err.set('No se pudo cargar el detalle'),
    });
  }

  verFirmado(tipo: 'solicitud' | 'carta'): void {
    this._svc.descargarFirmado$(this.tesisId, tipo).subscribe({
      next: (blob) => abrirBlob(blob, `${tipo}_firmada.pdf`),
      error: () => this.err.set('No se pudo abrir el documento'),
    });
  }

  generar(formato: 'pdf' | 'docx'): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); this.err.set('Completa expediente y fecha de solicitud.'); return; }
    this.saving.set(true); this.err.set(null);
    const v = this.form.value;
    this._svc.elaborar$(this.tesisId, v.expediente.trim(), v.fechaSolicitud).subscribe({
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
    this.subiendo.set(true); this.err.set(null);
    this._svc.subirFirmado$(this.tesisId, file).subscribe({
      next: () => { this.subiendo.set(false); this.msg.set('Dictamen firmado subido.'); this.cargar(); },
      error: (e) => { this.subiendo.set(false); this.err.set(e?.error?.message ?? 'No se pudo subir el archivo'); },
    });
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
}
