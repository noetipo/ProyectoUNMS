export interface ReporteResumen {
  tutores: number;
  estudiantesConTutor: number;
  estudiantesSinTutor: number;
  promedioPorTutor: number;
}

export interface ReporteTutor {
  id: string;
  nombres: string;
  apellidos: string;
  gradoAcademico?: string;
  estudiantes: number;
  cupoMaximo: number;
  cupoLleno: boolean;
  programas: number;
}

export interface TutorEstudiante {
  estudianteId: string;
  nombres: string;
  apellidos: string;
  codigoSistema?: string;
  codMatricula?: string;
  programaNombre?: string;
  fechaInicio?: string;
  anioIngreso?: number;
}

export interface EstudianteSinTutor {
  estudianteId: string;
  nombres: string;
  apellidos: string;
  codigoSistema?: string;
  codMatricula?: string;
  programaNombre?: string;
  anioIngreso?: number;
}
