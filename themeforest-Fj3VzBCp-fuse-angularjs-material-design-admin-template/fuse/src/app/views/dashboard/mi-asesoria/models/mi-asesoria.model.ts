/** Asesor sugerido por el tutor (con detalle del docente). */
export interface AsesorSugerido {
  sugerenciaId: string;
  asesorDocenteId: string;
  nombres?: string;
  apellidos?: string;
  gradoAcademico?: string;
  emailInstitucional?: string;
  lineas?: string[];
  lineaIds?: string[];
  asesoriasActivas: number;
  nota?: string;
  /** Puesto para el que lo sugirió el tutor: ASESOR (principal) o COASESOR. */
  tipo?: 'ASESOR' | 'COASESOR';

  // Trayectoria del docente (para decidir a quién confiar la tesis)
  categoria?: string;
  condicion?: string;
  cargoActual?: string;
  centroLaboral?: string;
  centroLaboralDetalle?: string;
  experienciaAnios?: number;
  orcid?: string;
  estudios?: string[];
}

/** Bandeja de asesoría del estudiante. */
export interface MiAsesoria {
  conTema: boolean;
  tesisId?: string;
  temaTitulo?: string;
  lineaNombre?: string;
  nivel?: string;
  estadoDerivado?: string;

  // Tutor asignado (tutoría vigente); undefined si aún no le asignaron tutor
  tutorId?: string;
  tutorNombre?: string;
  tutorGrado?: string;

  sugeridos: AsesorSugerido[];

  // Designación vigente: un solo asesor y, opcionalmente, un solo co-asesor
  asesorNombre?: string;
  coasesorNombre?: string;
  /** Ids de los designados: sirven para descartar de "sugeridos" a quien ya tiene el puesto. */
  asesorDocenteId?: string;
  coasesorDocenteId?: string;
  puedeSolicitarCoasesor?: boolean;

  // Solicitud del ASESOR (la principal): es la que marca el avance del proceso y los documentos
  solicitudId?: string;
  solicitudEstado: string; // SIN_SOLICITUD | PENDIENTE | ACEPTADA | RECHAZADA | CANCELADA
  docenteSolicitadoId?: string;
  docenteSolicitadoNombre?: string;
  fechaSolicitud?: string;
  fechaRespuesta?: string;
  motivoRespuesta?: string;

  // Solicitud de CO-ASESORÍA (opcional): va aparte para que no bloquee nada del asesor
  coasesorSolicitudId?: string;
  coasesorSolicitudEstado?: string; // undefined si nunca solicitó co-asesor
  coasesorSolicitadoNombre?: string;
  coasesorMotivoRespuesta?: string;

  solicitudPdfDisponible: boolean;
  cartaPdfDisponible: boolean;

  // Firmados subidos por el estudiante + dictamen
  solicitudFirmadaSubida?: boolean;
  cartaFirmadaSubida?: boolean;
  dictamenEstado?: string | null;   // POR_ELABORAR | ELABORADO | FIRMADO | OBSERVADO
  dictamenEmitido?: boolean;
  dictamenNumero?: string | null;
  dictamenFechaEmision?: string | null;
  dictamenMotivoObservacion?: string | null;
}

export function etiquetaEstadoDerivado(codigo?: string): string {
  switch (codigo) {
    case 'SIN_TEMA': return 'Sin tema';
    case 'SIN_ASESOR': return 'Sin asesor';
    case 'TEMA_REGISTRADO': return 'Tema registrado';
    case 'PROYECTO_PRESENTADO': return 'Proyecto presentado';
    case 'PROYECTO_APROBADO': return 'Proyecto aprobado';
    case 'EN_DESARROLLO': return 'En desarrollo';
    case 'SUSTENTADO': return 'Sustentado';
    default: return codigo ?? '—';
  }
}
