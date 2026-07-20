// Modelos del expediente de tesis (línea de tiempo de las 8 etapas del proceso).

export interface EtapaItem {
  numero: number;
  titulo: string;
  descripcion: string;
  estado: 'COMPLETADO' | 'EN_CURSO' | 'PENDIENTE';
  fecha?: string;
  tieneDictamen: boolean;
  dictamenLabel?: string;
}

export interface Expediente {
  tesisId: string;
  codigo?: string;
  estadoDerivado?: string;
  estadoLabel?: string;
  titulo?: string;
  doctorandoNombre?: string;
  programaNombre?: string;
  asesorNombre?: string;
  tutorNombre?: string;
  lineaNombre?: string;
  avancePct: number;
  etapas: EtapaItem[];
}

/** Clases del badge de estado de una etapa. */
export function claseEtapa(estado?: string): string {
  switch (estado) {
    case 'COMPLETADO': return 'bg-emerald-100 text-emerald-700';
    case 'EN_CURSO': return 'bg-rose-100 text-rose-700';
    default: return 'bg-slate-100 text-slate-400';
  }
}

/** Texto del badge de estado de una etapa. */
export function etiquetaEtapa(estado?: string): string {
  switch (estado) {
    case 'COMPLETADO': return 'COMPLETADO';
    case 'EN_CURSO': return 'EN CURSO';
    default: return 'PENDIENTE';
  }
}
