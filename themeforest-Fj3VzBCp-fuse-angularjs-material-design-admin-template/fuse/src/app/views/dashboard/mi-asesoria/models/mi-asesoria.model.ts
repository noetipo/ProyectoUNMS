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
}

/** Bandeja de asesoría del estudiante. */
export interface MiAsesoria {
  conTema: boolean;
  tesisId?: string;
  temaTitulo?: string;
  lineaNombre?: string;
  nivel?: string;
  estadoDerivado?: string;

  sugeridos: AsesorSugerido[];

  solicitudId?: string;
  solicitudEstado: string; // SIN_SOLICITUD | PENDIENTE | ACEPTADA | RECHAZADA | CANCELADA
  docenteSolicitadoId?: string;
  docenteSolicitadoNombre?: string;
  fechaSolicitud?: string;
  fechaRespuesta?: string;
  motivoRespuesta?: string;

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
