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
  /** Etapa en curso (1..8; 9 = proceso finalizado). */
  etapaEnCurso?: number;
  /** true desde la Etapa 4: hay dictamen de designación y ya se puede redactar el proyecto. */
  proyectoHabilitado?: boolean;
  /** true cuando el asesor emitió su carta: toca el cierre del expediente (Turnitin, final, envío). */
  cierreHabilitado?: boolean;
  /** true si aún hay una observación (asesor, revisor o Jurado Informante) por corregir en el proyecto. */
  proyectoPendienteCorreccion?: boolean;
  /** true cuando el Dictamen de Expedito ya está firmado: se puede solicitar el Jurado de Sustentación. */
  sustentacionHabilitada?: boolean;
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
