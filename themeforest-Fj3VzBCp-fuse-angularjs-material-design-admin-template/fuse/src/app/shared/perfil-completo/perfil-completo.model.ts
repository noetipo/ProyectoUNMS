// ── Tipos de documento soportados por el backend (documentos_persona) ─────────
export interface TipoDocumentoDef {
  tipo: string;
  label: string;
  requerido: boolean;
}

/** DNI y Partida de nacimiento son obligatorios al crear el perfil. */
export const TIPOS_DOCUMENTO_PERFIL: TipoDocumentoDef[] = [
  { tipo: 'DNI', label: 'DNI', requerido: true },
  { tipo: 'PARTIDA_NACIMIENTO', label: 'Partida de nacimiento', requerido: true },
  { tipo: 'CARNET_EXTRANJERIA', label: 'Carnet de extranjería', requerido: false },
  { tipo: 'PASAPORTE', label: 'Pasaporte', requerido: false },
  { tipo: 'PTP', label: 'PTP', requerido: false },
  { tipo: 'CARNET_DIPLOMATICO', label: 'Carnet diplomático', requerido: false },
  { tipo: 'DNI_CE', label: 'DNI / CE', requerido: false },
];

export interface CatalogoItem {
  id: string;
  nombre: string;
}

// ── Datos del perfil completo (lo que devuelve el backend) ───────────────────
export interface CargoHistorialItem {
  id?: string;
  cargoId: string;
  cargoNombre?: string;
  fechaInicio?: string | null;
  fechaFin?: string | null;
  actual?: boolean;
}

export interface CentroHistorialItem {
  id?: string;
  centroLaboralId: string;
  centroNombre?: string;
  fechaInicio?: string | null;
  fechaFin?: string | null;
  actual?: boolean;
}

export interface DocumentoItem {
  id: string;
  tipoDocumento: string;
  nombreOriginal?: string;
  contentType?: string;
  tamanioBytes?: number;
  fechaCarga?: string;
}

export interface PerfilCompleto {
  persona?: any;
  cargos: CargoHistorialItem[];
  centrosLaborales: CentroHistorialItem[];
  documentos: DocumentoItem[];
}

// ── Parte JSON (`datos`) que viaja en el multipart ───────────────────────────
export interface PerfilCompletoData {
  cargos: {
    cargoId: string;
    fechaInicio?: string | null;
    fechaFin?: string | null;
    actual: boolean;
  }[];
  centrosLaborales: {
    centroLaboralId: string;
    fechaInicio?: string | null;
    fechaFin?: string | null;
    actual: boolean;
  }[];
}

/**
 * Construye el `FormData` del perfil completo: una parte JSON `datos` con los
 * arrays + una parte de archivo por cada tipo de documento (nombre = tipo).
 */
export function buildPerfilFormData(
  datos: PerfilCompletoData,
  archivos: Record<string, File>,
): FormData {
  const fd = new FormData();
  fd.append('datos', JSON.stringify(datos));
  Object.entries(archivos).forEach(([tipo, file]) => {
    if (file) {
      fd.append(tipo, file, file.name);
    }
  });
  return fd;
}

export function formatTamanio(bytes?: number): string {
  if (!bytes && bytes !== 0) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}