/** Abre un Blob recibido del backend en una pestaña nueva (preview/descarga). */
export function abrirBlob(blob: Blob, _nombre?: string): void {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank');
  // Liberar la URL tras dar tiempo a que la pestaña la consuma.
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}