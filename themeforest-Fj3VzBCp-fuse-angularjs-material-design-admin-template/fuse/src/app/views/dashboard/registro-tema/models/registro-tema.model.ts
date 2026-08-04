/** Tarjetas de resumen del reporte del coordinador. */
export interface TemaResumen {
  total: number;
  conTema: number;
  sinTema: number;
  /** Sin tutoría vigente: el paso que sigue al registro del tema. */
  sinTutor?: number;
}

/** Fila del reporte: estudiante con/sin tema. */
export interface EstudianteTema {
  estudianteId: string;
  nombres: string;
  apellidos: string;
  codigoSistema?: string;
  codMatricula?: string;
  programaNombre?: string;
  nivel?: string;
  conTema: boolean;
  tesisId?: string;
  titulo?: string;
  lineaNombre?: string;
  estado?: string;          // tesis.estado
  estadoDerivado?: string;  // SIN_TEMA | SIN_ASESOR | <estado tesis>
  /** Tutor vigente; null/undefined = aún sin designar. */
  tutorNombre?: string;
}

/** Tema de investigación registrado (respuesta y perfil). */
export interface Tema {
  tesisId: string;
  estudianteId: string;
  titulo: string;
  resumen?: string;
  lineaInvestigacionId?: string;
  lineaNombre?: string;
  nivel?: string;
  estado?: string;
  estadoDerivado?: string;
  fechaRegistro?: string;
}

/** Cuerpo para registrar/editar el tema. */
export interface RegistrarTemaBody {
  lineaInvestigacionId: string;
  titulo: string;
  resumen?: string;
}

/** Etiqueta legible del estado derivado (misma semántica que el backend). */
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

/** Clase de badge por estado derivado. */
export function claseEstadoDerivado(codigo?: string): string {
  switch (codigo) {
    case 'SIN_TEMA': return 'bg-rose-50 text-rose-600';
    case 'SIN_ASESOR': return 'bg-amber-50 text-amber-600';
    default: return 'bg-emerald-50 text-emerald-600';
  }
}
