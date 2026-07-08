/** Resumen de cabecera del panel del tutor. */
export interface MisTutorandosResumen {
  tutorNombre: string;
  total: number;
  cupoMaximo: number;
}

/** Un tutorando (estudiante en tutoría vigente) del tutor autenticado. */
export interface Tutorando {
  estudianteId: string;
  nombres: string;
  apellidos: string;
  codigoSistema?: string;
  codMatricula?: string;
  programaNombre?: string;
  fechaInicio?: string;
  anioIngreso?: number;
  // Estado derivado del tema (misma fuente de verdad que coordinador/perfil).
  estadoDerivado?: string;
  tesisTitulo?: string;
  lineaNombre?: string;
}

/** Etiqueta legible del estado derivado. */
export function etiquetaEstado(codigo?: string): string {
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
export function claseEstado(codigo?: string): string {
  switch (codigo) {
    case 'SIN_TEMA': return 'bg-rose-50 text-rose-600';
    case 'SIN_ASESOR': return 'bg-amber-50 text-amber-600';
    default: return 'bg-emerald-50 text-emerald-600';
  }
}
