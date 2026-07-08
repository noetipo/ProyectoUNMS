export const ESTADOS_TUTOR = [
  { value: '', label: 'Todos' },
  { value: 'false', label: 'Sin tutor' },
  { value: 'true', label: 'Con tutor' },
] as const;

export interface TutorCombo {
  id: string;
  nombres: string;
  apellidos: string;
  gradoAcademico?: string;
  codigoSistema?: string;
  estudiantesActuales: number;
  cupoMaximo: number;
  disponible: boolean;
  /** false = aún no es tutor; al asignarle queda habilitado como tutor nuevo. */
  esTutor?: boolean;
}

export interface EstudianteAsignable {
  estudianteId: string;
  nombres: string;
  apellidos: string;
  codigoSistema?: string;
  programaId?: string;
  programaNombre?: string;
  tutorId?: string;
  tutorNombre?: string;
}

export interface TutorVigente {
  tutoriaId: string;
  tutorId: string;
  tutorNombre: string;
  gradoAcademico?: string;
  codigoSistema?: string;
  estudiantesActuales: number;
  cupoMaximo: number;
  fechaInicio?: string;
}

export interface TutoriaHistorial {
  id: string;
  docenteId: string;
  docenteNombre: string;
  gradoAcademico?: string;
  fechaInicio?: string;
  fechaFin?: string;
  actual: boolean;
  motivoCambio?: string;
}

export interface AsignarEnBloqueResult {
  asignados: number;
  sinCambio: number;
  reemplazos: number;
  cupoMaximo: number;
  cupoAntes: number;
  cupoDespues: number;
  mensaje: string;
}
