import { PerfilCompletoData } from '@/app/shared/perfil-completo/perfil-completo.model';

// ── Catálogos (enums del backend) ───────────────────────────────────────────
export const TIPOS_DOCUMENTO = [
  'DNI', 'CARNET_EXTRANJERIA', 'PASAPORTE', 'PTP', 'CARNET_DIPLOMATICO',
  'DNI_CE', 'PARTIDA_NACIMIENTO',
] as const;
export const SEXOS = ['HOMBRE', 'MUJER'] as const;
export const ESTADOS_CIVILES = ['SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO', 'CONVIVIENTE'] as const;
export const PROCEDENCIAS = ['NACIONAL', 'EXTRANJERO'] as const;
export const CONDICIONES_ESTUDIANTE = ['REGULAR', 'SANCIONADO', 'EGRESADO', 'RETIRADO'] as const;
export const FINANCIAMIENTOS = ['BECA_COMPLETA', 'BECA_PARCIAL', 'AUTOFINANCIADO'] as const;
export const GRADOS_ACADEMICOS = [
  'BACHILLER', 'LICENCIADO', 'SEGUNDA_ESPECIALIDAD', 'MAGISTER', 'DOCTOR', 'POST_DOCTORADO',
] as const;
export const GRADOS_ACADEMICOS_LABELS: Record<string, string> = {
  BACHILLER: 'Bachiller',
  LICENCIADO: 'Licenciado',
  SEGUNDA_ESPECIALIDAD: 'Segunda especialidad',
  MAGISTER: 'Magíster',
  DOCTOR: 'Doctor',
  POST_DOCTORADO: 'Post-doctorado',
};
export const CATEGORIAS_DOCENTE = ['PRINCIPAL', 'ASOCIADO', 'AUXILIAR'] as const;
export const CONDICIONES_DOCENTE = ['NOMBRADO', 'CONTRATADO'] as const;

// ── Request: crear persona ───────────────────────────────────────────────────
export interface CrearPersonaRequest {
  persona: {
    tipoDocumento: string;
    numeroDocumento: string;
    apellidoPaterno: string;
    apellidoMaterno?: string;
    nombres: string;
    sexo?: string;
    fechaNacimiento?: string;
    estadoCivil?: string;
    nacionalidad?: string;
    procedencia?: string;
    emailPersonal?: string;
    celular?: string;
    discapacidad?: boolean;
    orcid?: string;
  };
  cuenta: {
    username: string;
    email: string;
    password: string;
  };
  estudiante?: {
    codigoSistema?: string;
    codMatricula?: string;
    emailInstitucional?: string;
    anioIngreso?: number;
    programaId: string;
    condicion?: string;
    financiamiento?: string;
    observaciones?: string;
    tutorId?: string;
  };
  docente?: {
    codigoSistema?: string;
    emailInstitucional?: string;
    categoria?: string;
    condicion?: string;
    /** Líneas de investigación (especialidad) del docente. */
    lineasInvestigacion?: DocenteLineaInput[];
  };
  /** Grados académicos a nivel de persona (uno principal). */
  gradosAcademicos?: GradoAcademicoInput[];
  /** Historiales de cargos y centros laborales (alta multipart). */
  perfil?: PerfilCompletoData;
}

export interface DocenteLineaInput {
  lineaInvestigacionId: string;
  esPrincipal: boolean;
}

export interface GradoAcademicoInput {
  grado: string;
  anio?: number | null;
  universidad?: string | null;
  principal?: boolean;
}

export interface AgregarPerfilDocenteRequest {
  codigoSistema?: string;
  emailInstitucional?: string;
  categoria?: string;
  condicion?: string;
}

// ── Response ─────────────────────────────────────────────────────────────────
export interface PersonaResponse {
  id: string;
  nombres: string;
  apellidos: string;
  tipoDocumento?: string;
  apellidoPaterno?: string;
  apellidoMaterno?: string;
  sexo?: string;
  fechaNacimiento?: string;
  estadoCivil?: string;
  nacionalidad?: string;
  procedencia?: string;
  discapacidad?: boolean;
  numeroDocumento: string;
  emailPersonal?: string;
  celular?: string;
  orcid?: string;
  username: string;
  perfiles: string[];
  roles: string[];
  estudiante?: EstudianteResumen;
  docente?: DocenteResumen;
  gradosAcademicos?: GradoAcademicoItem[];
}

export interface GradoAcademicoItem {
  id?: string;
  grado: string;
  anio?: number | null;
  universidad?: string | null;
  principal?: boolean;
}

export interface EstudianteResumen {
  codigoSistema?: string;
  codMatricula?: string;
  emailInstitucional?: string;
  anioIngreso?: number;
  programaId?: string;
  programaNombre?: string;
  facultadId?: string;
  facultadNombre?: string;
  condicion?: string;
  financiamiento?: string;
  observaciones?: string;
  tutorId?: string;
  tutorNombre?: string;
}

export interface DocenteResumen {
  codigoSistema?: string;
  emailInstitucional?: string;
  gradoAcademico?: string;
  categoria?: string;
  condicion?: string;
  lineasInvestigacion?: LineaInvestigacionItem[];
}

export interface LineaInvestigacionItem {
  id: string;
  nombre: string;
  esPrincipal: boolean;
}

export interface ProgramaPosgrado {
  id: string;
  nombre: string;
  nivel: string;
  facultadId?: string;
  facultadNombre?: string;
}

export interface FacultadItem {
  id: string;
  codigo?: string;
  nombre: string;
}