export const ESTADOS_SOLICITUD = ['PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'CANCELADA'] as const;
export type EstadoSolicitud = (typeof ESTADOS_SOLICITUD)[number];

/** Fila de la bandeja (vista del docente). */
export interface SolicitudBandeja {
  id: string;
  estudianteId: string;
  estudianteNombres: string;
  estudianteApellidos: string;
  codigoSistema?: string;
  programaNombre?: string;
  lineaNombre?: string;
  tituloTentativo?: string;
  mensaje?: string;
  tipo: string;
  estado: EstadoSolicitud;
  fechaSolicitud?: string;
  fechaRespuesta?: string;
  motivoRespuesta?: string;
  /** Contexto del tema, para decidir la solicitud con información. */
  nivel?: string;
  temaTitulo?: string;
  temaResumen?: string;
  tutorNombre?: string;
}
