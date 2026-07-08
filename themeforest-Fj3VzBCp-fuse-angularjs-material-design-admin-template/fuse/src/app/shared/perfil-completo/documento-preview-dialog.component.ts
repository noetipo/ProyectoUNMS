import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
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
};

/**
 * Diálogo de previsualización de un documento recién adjuntado.
 * Las imágenes se muestran en una etiqueta img; el PDF en un iframe saneado
 * con DomSanitizer. Cierra con X, Esc o clic fuera (comportamiento por defecto
 * de MatDialog), que además devuelve el foco al elemento que lo abrió.
 */
@Component({
  selector: 'app-documento-preview-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="flex flex-col max-h-[85vh] w-[min(90vw,860px)]">

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
          <iframe [src]="safeUrl" class="w-full h-[70vh] rounded-lg border border-slate-200 bg-white"
                  [title]="data.nombre"></iframe>
        } @else {
          <img [src]="data.url" [alt]="data.nombre"
               class="max-w-full max-h-[70vh] object-contain rounded-lg shadow-sm" />
        }
      </div>

      <div class="flex items-center justify-end px-5 py-3 border-t border-slate-100">
        <a class="text-xs text-primary-600 hover:underline" [href]="data.url" target="_blank" rel="noopener">
          Abrir en pestaña nueva
        </a>
      </div>
    </div>
  `,
})
export class DocumentoPreviewDialogComponent {
  private _dialogRef = inject<MatDialogRef<DocumentoPreviewDialogComponent>>(MatDialogRef);
  protected data = inject<DocumentoPreviewData>(MAT_DIALOG_DATA);

  // Solo el PDF necesita saltarse el sanitizador (se inyecta en un iframe).
  protected readonly safeUrl: SafeResourceUrl =
    inject(DomSanitizer).bypassSecurityTrustResourceUrl(this.data.url);

  close(): void { this._dialogRef.close(); }
}
