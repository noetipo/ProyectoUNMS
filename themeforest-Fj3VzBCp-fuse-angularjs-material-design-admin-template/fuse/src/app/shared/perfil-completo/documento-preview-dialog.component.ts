import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

export type DocumentoPreviewData = {
  /** Nombre visible del archivo. */
  nombre: string;
  /** Object URL crudo (para la imagen, descargas y "abrir en pestaña nueva"). */
  url: string;
  /** true entonces se muestra en un iframe; false en una imagen. */
  esPdf: boolean;
  /** MIME del archivo (opcional). Permite distinguir Word/otros de imágenes para no dejar el visor en blanco. */
  mime?: string;
  /** El Blob original: necesario para renderizar el Word con docx-preview. */
  blob?: Blob;
};

/**
 * Diálogo de previsualización de un documento.
 *
 * <p>PDF → iframe saneado con DomSanitizer · imagen → etiqueta img · <b>Word → se renderiza a
 * HTML con docx-preview</b> (misma librería que ya usa la Secretaría para ver las rúbricas), de
 * modo que el .docx también se pueda revisar sin bajarlo ni abrir Office. Si el render falla se
 * cae al mensaje con los botones de abrir/descargar.</p>
 *
 * <p>Cierra con X, Esc o clic fuera (comportamiento por defecto de MatDialog), que además
 * devuelve el foco al elemento que lo abrió.</p>
 */
@Component({
  selector: 'app-documento-preview-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="flex flex-col h-[92vh] w-[min(96vw,1200px)]">

      <div class="flex items-center justify-between gap-3 px-5 py-3 border-b border-slate-100">
        <h2 class="text-sm font-semibold text-slate-800 truncate" [title]="data.nombre">
          {{ data.nombre }}
        </h2>
        <div class="flex items-center gap-1 shrink-0">
          <a mat-icon-button [href]="data.url" target="_blank" rel="noopener"
             aria-label="Abrir en una pestaña nueva" matTooltip="Abrir en pestaña nueva">
            <mat-icon svgIcon="external-link" class="size-4 text-slate-500" />
          </a>
          <button mat-icon-button type="button" (click)="close()" aria-label="Cerrar previsualización">
            <mat-icon svgIcon="x" class="size-4 text-slate-500" />
          </button>
        </div>
      </div>

      <div class="flex-1 overflow-auto bg-slate-50 flex items-center justify-center p-4">
        @if (data.esPdf) {
          <iframe [src]="safeUrl" class="w-full h-full rounded-lg border border-slate-200 bg-white"
                  [title]="data.nombre"></iframe>
        } @else if (esImagen) {
          <img [src]="data.url" [alt]="data.nombre"
               class="max-w-full max-h-full object-contain rounded-lg shadow-sm" />
        } @else if (esWord && !errorDocx()) {
          <!-- Word: se renderiza a HTML en el propio modal. -->
          <div class="w-full h-full overflow-auto rounded-lg border border-slate-200 bg-white">
            @if (cargandoDocx()) {
              <div class="flex flex-col items-center justify-center h-full gap-2 text-slate-400">
                <mat-icon svgIcon="loader" class="!size-8 animate-spin" />
                <p class="text-[12.5px]">Preparando la vista previa…</p>
              </div>
            }
            <div #docxHost class="docx-host px-2 py-3" [class.hidden]="cargandoDocx()"></div>
          </div>
        } @else {
          <!-- Formato que el navegador no puede mostrar (o el render falló): abrir/descargar. -->
          <div class="text-center px-6 py-10 max-w-sm">
            <mat-icon svgIcon="file-text" class="!size-14 text-slate-300 mb-3" />
            <p class="text-sm font-semibold text-slate-700 mb-1 break-words">{{ data.nombre }}</p>
            <p class="text-[12.5px] text-slate-500 mb-5">
              {{ errorDocx() ?? 'Este documento no puede previsualizarse en el navegador. Ábrelo o descárgalo para verlo.' }}
            </p>
            <div class="flex items-center justify-center gap-2">
              <a mat-flat-button color="primary" [href]="data.url" [download]="data.nombre" class="!h-9 !text-sm">
                <mat-icon svgIcon="download" class="size-4 mr-1" /> Descargar
              </a>
              <a mat-stroked-button [href]="data.url" target="_blank" rel="noopener" class="!h-9 !text-sm">
                <mat-icon svgIcon="external-link" class="size-4 mr-1" /> Abrir
              </a>
            </div>
          </div>
        }
      </div>

      <div class="flex items-center justify-between gap-3 px-5 py-3 border-t border-slate-100">
        <a class="text-xs text-slate-500 hover:underline" [href]="data.url" [download]="data.nombre">
          Descargar una copia
        </a>
        <a class="text-xs text-primary-600 hover:underline" [href]="data.url" target="_blank" rel="noopener">
          Abrir en pestaña nueva
        </a>
      </div>
    </div>
  `,
  styles: [`
    /* docx-preview inyecta su propio HTML: hay que atravesar la encapsulación para acotarlo
       al ancho del modal (si no, saca la hoja A4 a tamaño real y aparece scroll horizontal). */
    .docx-host ::ng-deep .docx-wrapper { background: transparent; padding: 0; }
    .docx-host ::ng-deep .docx { width: 100% !important; min-height: 0 !important;
                                 padding: 12px 16px !important; box-shadow: none !important; }
  `],
})
export class DocumentoPreviewDialogComponent implements AfterViewInit {
  private _dialogRef = inject<MatDialogRef<DocumentoPreviewDialogComponent>>(MatDialogRef);
  protected data = inject<DocumentoPreviewData>(MAT_DIALOG_DATA);

  @ViewChild('docxHost') private docxHost?: ElementRef<HTMLElement>;

  // Solo el PDF necesita saltarse el sanitizador (se inyecta en un iframe).
  protected readonly safeUrl: SafeResourceUrl =
    inject(DomSanitizer).bypassSecurityTrustResourceUrl(this.data.url);

  /** No-PDF que es imagen. Si no viene el MIME se asume imagen (retrocompatibilidad). */
  protected readonly esImagen: boolean =
    !this.data.esPdf && (this.data.mime ? this.data.mime.startsWith('image/') : true);

  /** Word (.docx/.doc): se puede renderizar si tenemos el Blob original. */
  protected readonly esWord: boolean =
    !this.data.esPdf && !this.esImagen && !!this.data.blob
    && /word|officedocument\.wordprocessingml/i.test(this.data.mime ?? '');

  protected cargandoDocx = signal(true);
  protected errorDocx = signal<string | null>(null);

  ngAfterViewInit(): void {
    if (this.esWord) void this.renderDocx();
  }

  /** Renderiza el .docx a HTML dentro del modal (carga perezosa de la librería). */
  private async renderDocx(): Promise<void> {
    const limite = <T,>(p: Promise<T>, ms: number) =>
      Promise.race([p, new Promise<T>((_, rej) => setTimeout(() => rej(new Error('timeout')), ms))]);
    try {
      const host = this.docxHost?.nativeElement;
      if (!host || !this.data.blob) throw new Error('sin contenedor');
      host.innerHTML = '';
      const mod = await limite(import('docx-preview'), 20000);
      await limite(mod.renderAsync(this.data.blob, host, undefined, {
        className: 'docx', inWrapper: true, breakPages: true,
        ignoreWidth: true, ignoreHeight: true, useBase64URL: true, experimental: true,
      }), 20000);
      this.cargandoDocx.set(false);
    } catch {
      this.cargandoDocx.set(false);
      this.errorDocx.set('No se pudo mostrar el Word aquí. Ábrelo o descárgalo para verlo con Office.');
    }
  }

  close(): void { this._dialogRef.close(); }
}
