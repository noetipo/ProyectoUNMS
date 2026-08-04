import { MatDialog } from '@angular/material/dialog';
import { DocumentoPreviewDialogComponent } from './documento-preview-dialog.component';

/**
 * Muestra un Blob del backend en el diálogo de previsualización, en vez de descargarlo.
 *
 * <p>Ver un adjunto casi siempre es "comprobar que subí el archivo correcto", y para eso bajarlo
 * al disco sobra: se abre el visor, se mira y se cierra. El diálogo ya ofrece "abrir en pestaña
 * nueva" y descarga para quien sí las necesite, y sabe explicar el caso de Word (el navegador no
 * lo renderiza). El object URL se libera al cerrar.</p>
 */
export function previsualizarBlob(dialog: MatDialog, blob: Blob, nombre: string): void {
  const url = URL.createObjectURL(blob);
  const mime = blob.type || 'application/pdf';
  dialog.open(DocumentoPreviewDialogComponent, {
    // El Blob viaja además del object URL: el visor de Word lo necesita para renderizarlo.
    data: { nombre, url, esPdf: mime.includes('pdf'), mime, blob },
    maxWidth: '92vw',
  }).afterClosed().subscribe(() => setTimeout(() => URL.revokeObjectURL(url), 1000));
}
