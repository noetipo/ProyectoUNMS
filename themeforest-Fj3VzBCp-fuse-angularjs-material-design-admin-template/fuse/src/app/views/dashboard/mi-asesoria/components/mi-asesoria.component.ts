import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { abrirBlob } from '@/app/shared/perfil-completo/download.util';
import { descargarBlob } from '@/app/views/dashboard/reportes/download.util';
import { MiAsesoriaService } from '../services/mi-asesoria.service';
import { AsesorSugerido, etiquetaEstadoDerivado, MiAsesoria } from '../models/mi-asesoria.model';

@Component({
  selector: 'app-mi-asesoria',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatExpansionModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Proceso de Tesis</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Mi asesoría</span>
          </div>
          <h1 class="page-title">Mi asesoría de tesis</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <div class="max-w-3xl mx-auto space-y-5">

          @if (loading()) {
            <p class="text-sm text-slate-400">Cargando…</p>
          } @else if (data(); as d) {

            @if (msg()) {
              <div class="rounded-lg bg-emerald-50 border border-emerald-200 px-3 py-2 text-sm text-emerald-700">{{ msg() }}</div>
            }
            @if (err()) {
              <div class="rounded-lg bg-rose-50 border border-rose-200 px-3 py-2 text-sm text-rose-700">{{ err() }}</div>
            }

            <!-- Progreso -->
            <div class="flex items-center gap-2 text-xs">
              @for (s of pasos(); track s.label; let i = $index) {
                <div class="flex items-center gap-2">
                  <span class="flex items-center gap-1.5 px-2.5 py-1 rounded-full"
                        [class]="s.estado === 'done' ? 'bg-emerald-50 text-emerald-700' : (s.estado === 'active' ? 'bg-sky-50 text-sky-700' : 'bg-slate-100 text-slate-400')">
                    <span class="w-1.5 h-1.5 rounded-full"
                          [class]="s.estado === 'done' ? 'bg-emerald-500' : (s.estado === 'active' ? 'bg-sky-500' : 'bg-slate-300')"></span>
                    {{ s.label }}
                  </span>
                  @if (i < pasos().length - 1) { <mat-icon svgIcon="chevron-right" class="size-3 text-slate-300" /> }
                </div>
              }
            </div>

            <!-- Tema -->
            <section class="rounded-xl border border-slate-100 p-4">
              <div class="flex items-center justify-between mb-1">
                <h2 class="text-sm font-semibold text-slate-800">Tema de investigación</h2>
                <span class="text-[11px] font-medium px-2 py-0.5 rounded-full"
                      [class]="d.estadoDerivado === 'SIN_TEMA' ? 'bg-rose-50 text-rose-600' : (d.estadoDerivado === 'SIN_ASESOR' ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600')">
                  {{ estadoLabel(d.estadoDerivado) }}
                </span>
              </div>
              @if (d.conTema) {
                <p class="text-sm font-medium text-slate-700">{{ d.temaTitulo }}</p>
                <div class="grid grid-cols-2 gap-2 text-xs text-slate-500 mt-2">
                  <div>Línea: {{ d.lineaNombre ?? '—' }}</div>
                  <div>Nivel: {{ d.nivel ?? '—' }}</div>
                </div>
              } @else {
                <p class="text-sm text-slate-400">Aún no tienes un tema registrado. El coordinador lo registrará por ti.</p>
              }
            </section>

            <!-- Asesores sugeridos -->
            <section class="rounded-xl border border-slate-100 p-4">
              <h2 class="text-sm font-semibold text-slate-800 mb-2">Asesores sugeridos por tu tutor</h2>

              @if (d.solicitudEstado === 'ACEPTADA') {
                <div class="rounded-lg bg-emerald-50 border border-emerald-200 px-3 py-2.5 text-sm text-emerald-700 mb-2">
                  Asesoría aceptada por <b>{{ d.docenteSolicitadoNombre }}</b>. Descarga tus documentos en la sección <b>Documentos</b>, más abajo.
                </div>
              } @else if (d.solicitudEstado === 'PENDIENTE') {
                <div class="rounded-lg bg-amber-50 border border-amber-200 px-3 py-2 text-sm text-amber-700 mb-2 flex items-center justify-between">
                  <span>Solicitud <b>pendiente</b> enviada a {{ d.docenteSolicitadoNombre }}.</span>
                  @if (d.solicitudId) {
                    <button mat-stroked-button class="!h-7 !text-xs !min-w-0 !px-3" (click)="cancelar(d.solicitudId!)">Cancelar</button>
                  }
                </div>
              } @else if (d.solicitudEstado === 'RECHAZADA') {
                <div class="rounded-lg bg-rose-50 border border-rose-200 px-3 py-2 text-sm text-rose-700 mb-2">
                  {{ d.docenteSolicitadoNombre }} rechazó la solicitud@if (d.motivoRespuesta) { : {{ d.motivoRespuesta }} }. Puedes solicitar a otro.
                </div>
              }

              @if (d.sugeridos.length) {
                <mat-accordion class="block space-y-2">
                  @for (a of d.sugeridos; track a.sugerenciaId) {
                    <mat-expansion-panel class="!shadow-none !border !border-slate-100 !rounded-lg">
                      <mat-expansion-panel-header>
                        <mat-panel-title>
                          <span class="text-sm font-medium text-slate-700">{{ a.apellidos }}, {{ a.nombres }}</span>
                        </mat-panel-title>
                        <mat-panel-description>
                          <span class="text-xs text-slate-400">{{ a.gradoAcademico ?? '' }} · {{ a.asesoriasActivas }} asesoría(s)</span>
                        </mat-panel-description>
                      </mat-expansion-panel-header>
                      <div class="text-xs text-slate-500 space-y-1">
                        <div>Email: {{ a.emailInstitucional ?? '—' }}</div>
                        <div>Líneas: {{ (a.lineas?.length ? a.lineas!.join(', ') : '—') }}</div>
                        <div>Carga actual: {{ a.asesoriasActivas }} asesoría(s) activa(s)</div>
                        @if (a.nota) { <div class="italic">Nota del tutor: {{ a.nota }}</div> }
                      </div>
                      <div class="flex justify-end mt-3">
                        <button class="btn-dark !h-7 !text-xs !px-3"
                                [disabled]="!puedeSolicitar(a)"
                                (click)="solicitar(a)">
                          Solicitar asesoría
                        </button>
                      </div>
                      @if (!a.lineaIds?.length) {
                        <p class="text-[11px] text-rose-400 text-right mt-1">Este asesor no tiene líneas registradas.</p>
                      }
                    </mat-expansion-panel>
                  }
                </mat-accordion>
              } @else {
                <p class="text-sm text-slate-400">Tu tutor aún no te ha sugerido asesores.</p>
              }
            </section>

            <!-- Documentos -->
            <section class="rounded-xl border border-slate-100 p-4">
              <h2 class="text-sm font-semibold text-slate-800 mb-2">Documentos</h2>
              <div class="rounded-lg bg-sky-50 border border-sky-200 px-3 py-2 text-[12px] text-sky-700 mb-3">
                Descarga cada documento, fírmalo físicamente y súbelo firmado. Con ambos, la secretaría elaborará tu dictamen.
              </div>
              <div class="space-y-3">
                <!-- Solicitud -->
                <div class="rounded-lg border border-slate-100 p-3">
                  <div class="flex items-center justify-between gap-3">
                    <div>
                      <p class="text-sm text-slate-700 font-medium">Solicitud de asesoría</p>
                      <p class="text-[11px]" [class.text-emerald-600]="d.solicitudFirmadaSubida" [class.text-amber-600]="!d.solicitudFirmadaSubida">
                        {{ d.solicitudFirmadaSubida ? 'Firmado subido' : 'Pendiente de firma' }}
                      </p>
                    </div>
                    <div class="flex items-center gap-1.5 shrink-0">
                      <button mat-stroked-button class="!h-8 !text-xs !min-w-0 !px-3" [disabled]="!d.solicitudPdfDisponible || descargando()" (click)="descargar('SOLICITUD_ASESORIA', 'pdf')">
                        <mat-icon svgIcon="file-text" class="size-3.5 mr-1" /> PDF
                      </button>
                      <button mat-stroked-button class="!h-8 !text-xs !min-w-0 !px-3" [disabled]="!d.solicitudPdfDisponible || descargando()" (click)="descargar('SOLICITUD_ASESORIA', 'docx')">
                        <mat-icon svgIcon="download" class="size-3.5 mr-1" /> Word
                      </button>
                      <button mat-flat-button color="primary" class="!h-8 !text-xs !min-w-0 !px-3" [disabled]="subiendo()" (click)="fileSol.click()">
                        <mat-icon svgIcon="upload" class="size-3.5 mr-1" /> {{ d.solicitudFirmadaSubida ? 'Reemplazar' : 'Subir firmado' }}
                      </button>
                      <input #fileSol type="file" hidden accept=".pdf,.docx" (change)="onFile($event, 'solicitud')" />
                    </div>
                  </div>
                </div>
                <!-- Carta -->
                <div class="rounded-lg border border-slate-100 p-3">
                  <div class="flex items-center justify-between gap-3">
                    <div>
                      <p class="text-sm text-slate-700 font-medium">Carta de aceptación del asesor</p>
                      <p class="text-[11px]" [class.text-emerald-600]="d.cartaFirmadaSubida" [class.text-amber-600]="!d.cartaFirmadaSubida">
                        {{ d.cartaFirmadaSubida ? 'Firmado subido' : (d.cartaPdfDisponible ? 'Pendiente de firma' : 'Disponible cuando el asesor acepte') }}
                      </p>
                    </div>
                    <div class="flex items-center gap-1.5 shrink-0">
                      <button mat-stroked-button class="!h-8 !text-xs !min-w-0 !px-3" [disabled]="!d.cartaPdfDisponible || descargando()" (click)="descargar('CARTA_ACEPTACION', 'pdf')">
                        <mat-icon svgIcon="file-text" class="size-3.5 mr-1" /> PDF
                      </button>
                      <button mat-stroked-button class="!h-8 !text-xs !min-w-0 !px-3" [disabled]="!d.cartaPdfDisponible || descargando()" (click)="descargar('CARTA_ACEPTACION', 'docx')">
                        <mat-icon svgIcon="download" class="size-3.5 mr-1" /> Word
                      </button>
                      <button mat-flat-button color="primary" class="!h-8 !text-xs !min-w-0 !px-3" [disabled]="subiendo() || !d.cartaPdfDisponible" (click)="fileCarta.click()">
                        <mat-icon svgIcon="upload" class="size-3.5 mr-1" /> {{ d.cartaFirmadaSubida ? 'Reemplazar' : 'Subir firmado' }}
                      </button>
                      <input #fileCarta type="file" hidden accept=".pdf,.docx" (change)="onFile($event, 'carta')" />
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- Dictamen -->
            @if (d.dictamenEmitido) {
              <section class="rounded-xl border border-emerald-200 bg-emerald-50 p-4">
                <div class="flex items-start gap-3">
                  <mat-icon svgIcon="badge-check" class="size-6 text-emerald-600 shrink-0" />
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-semibold text-emerald-800">Dictamen de designación de asesor</p>
                    @if (d.dictamenNumero) { <p class="text-xs text-emerald-700">N° {{ d.dictamenNumero }}</p> }
                    <p class="text-[11px] text-emerald-600">Firmado por el Director de la Unidad de Posgrado@if (d.dictamenFechaEmision) { · {{ d.dictamenFechaEmision | date:'dd/MM/yyyy' }} }</p>
                  </div>
                  <button mat-flat-button color="primary" class="!h-8 !text-xs shrink-0" [disabled]="descargando()" (click)="verDictamen()">
                    <mat-icon svgIcon="download" class="size-3.5 mr-1" /> Descargar
                  </button>
                </div>
              </section>
            } @else {
              <section class="rounded-xl border border-slate-100 p-4">
                <div class="flex items-center gap-3">
                  <mat-icon svgIcon="clock" class="size-5 text-slate-300" />
                  <div>
                    <p class="text-sm font-medium text-slate-600">Dictamen de designación de asesor</p>
                    <p class="text-[11px] text-slate-400">
                      {{ (d.solicitudFirmadaSubida && d.cartaFirmadaSubida) ? 'La secretaría está elaborando tu dictamen.' : 'Sube los dos documentos firmados para que la secretaría elabore tu dictamen.' }}
                    </p>
                    @if (d.dictamenMotivoObservacion) {
                      <p class="text-[11px] text-rose-500 mt-0.5">Observado: {{ d.dictamenMotivoObservacion }} — vuelve a subir los documentos.</p>
                    }
                  </div>
                </div>
              </section>
            }
          }
        </div>
      </div>
    </div>
  `,
})
export class MiAsesoriaComponent implements OnInit {
  private _svc = inject(MiAsesoriaService);
  private _confirm = inject(ConfirmDialogService);

  protected data = signal<MiAsesoria | null>(null);
  protected loading = signal(true);
  protected descargando = signal(false);
  protected subiendo = signal(false);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);

  protected pasos = computed(() => {
    const d = this.data();
    const conTema = !!d?.conTema;
    const aceptada = d?.solicitudEstado === 'ACEPTADA';
    return [
      { label: 'Tema registrado', estado: conTema ? 'done' : 'active' },
      { label: 'Asesor aceptado', estado: aceptada ? 'done' : (conTema ? 'active' : 'idle') },
      { label: 'Documentos', estado: aceptada ? 'active' : 'idle' },
    ];
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this._svc.bandeja$().subscribe({
      next: (res: any) => { this.data.set(res?.data ?? res ?? null); this.loading.set(false); },
      error: () => { this.data.set(null); this.loading.set(false); },
    });
  }

  estadoLabel = etiquetaEstadoDerivado;

  puedeSolicitar(a: AsesorSugerido): boolean {
    const d = this.data();
    return !!d && d.solicitudEstado !== 'PENDIENTE' && d.solicitudEstado !== 'ACEPTADA' && !!a.lineaIds?.length;
  }

  solicitar(a: AsesorSugerido): void {
    const linea = a.lineaIds?.[0];
    if (!linea) return;
    this.msg.set(null); this.err.set(null);
    this._svc.solicitar$(a.asesorDocenteId, linea, this.data()?.temaTitulo).subscribe({
      next: () => { this.msg.set('Solicitud enviada. Ya puedes descargar la Solicitud en PDF.'); this.cargar(); },
      error: (e) => this.err.set(e?.error?.message ?? e?.error?.error ?? 'No se pudo enviar la solicitud'),
    });
  }

  cancelar(solicitudId: string): void {
    this._confirm.confirmDelete({ title: 'Cancelar solicitud', message: '¿Cancelar tu solicitud de asesoría?' })
      .then(() => this._svc.cancelar$(solicitudId).subscribe({ next: () => { this.msg.set('Solicitud cancelada.'); this.cargar(); } }))
      .catch(() => {});
  }

  descargar(tipo: 'SOLICITUD_ASESORIA' | 'CARTA_ACEPTACION', formato: 'pdf' | 'docx' = 'pdf'): void {
    this.descargando.set(true);
    this.err.set(null);
    this._svc.descargar$(tipo, formato).subscribe({
      next: (blob) => {
        const nombre = `${tipo.toLowerCase()}.${formato}`;
        // El PDF se previsualiza en pestaña; el Word (.docx) se descarga (el navegador no lo renderiza).
        if (formato === 'docx') {
          descargarBlob(blob, nombre);
        } else {
          abrirBlob(blob, nombre);
        }
        this.descargando.set(false);
      },
      error: () => { this.err.set('No se pudo descargar el documento'); this.descargando.set(false); },
    });
  }

  onFile(event: Event, tipo: 'solicitud' | 'carta'): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.subiendo.set(true);
    this.err.set(null);
    this._svc.subirFirmado$(tipo, file).subscribe({
      next: () => { this.subiendo.set(false); this.msg.set('Documento firmado subido correctamente.'); this.cargar(); },
      error: (e) => { this.subiendo.set(false); this.err.set(e?.error?.message ?? 'No se pudo subir el archivo (usa PDF o Word).'); },
    });
  }

  verDictamen(): void {
    this.descargando.set(true);
    this.err.set(null);
    this._svc.descargarDictamen$().subscribe({
      next: (blob) => { abrirBlob(blob, 'dictamen.pdf'); this.descargando.set(false); },
      error: () => { this.err.set('No se pudo descargar el dictamen'); this.descargando.set(false); },
    });
  }
}
