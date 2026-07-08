// ─── Enums ───────────────────────────────────────────────────────────────────

export enum Sexo {
  HOMBRE = 'HOMBRE',
  MUJER  = 'MUJER',
}

export enum EstadoCivil {
  SOLTERO     = 'SOLTERO',
  CASADO      = 'CASADO',
  DIVORCIADO  = 'DIVORCIADO',
  VIUDO       = 'VIUDO',
  CONVIVIENTE = 'CONVIVIENTE',
}

export enum Financiamiento {
  BECA_COMPLETA  = 'BECA_COMPLETA',
  BECA_PARCIAL   = 'BECA_PARCIAL',
  AUTOFINANCIADO = 'AUTOFINANCIADO',
}

export enum Condicion {
  REGULAR    = 'REGULAR',
  SANCIONADO = 'SANCIONADO',
  EGRESADO   = 'EGRESADO',
  RETIRADO   = 'RETIRADO',
}

export enum Procedencia {
  NACIONAL   = 'NACIONAL',
  EXTRANJERO = 'EXTRANJERO',
}

export enum TipoDocumento {
  // Identificación personal
  DNI                = 'DNI',
  CARNET_EXTRANJERIA = 'CARNET_EXTRANJERIA',
  PASAPORTE          = 'PASAPORTE',
  PTP                = 'PTP',
  CARNET_DIPLOMATICO = 'CARNET_DIPLOMATICO',
  // Tipos de archivos subidos
  DNI_CE             = 'DNI_CE',
  PARTIDA_NACIMIENTO = 'PARTIDA_NACIMIENTO',
}

// ─── Paginación ───────────────────────────────────────────────────────────────

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

// ─── Programa de Doctorado ────────────────────────────────────────────────────

export interface ProgramaDoctorado {
  id: string;
  codigoSistema: string;
  nombre: string;
  descripcion: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

// ─── Centro Laboral ───────────────────────────────────────────────────────────

export interface CentroLaboral {
  id: string;
  codigoSistema: string;
  nombre: string;
  descripcion: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CentroLaboralRequest { nombre: string; descripcion?: string; }
export type CentroLaboralUpdate = Partial<CentroLaboralRequest>;

// ─── Cargo ────────────────────────────────────────────────────────────────────

export interface Cargo {
  id: string;
  codigoSistema: string;
  nombre: string;
  descripcion: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CargoRequest { nombre: string; descripcion?: string; }
export type CargoUpdate = Partial<CargoRequest>;

export interface ProgramaDoctoradoRequest {
  nombre: string;
  descripcion?: string;
}

export interface ProgramaDoctoradoUpdate {
  nombre?: string;
  descripcion?: string;
  active?: boolean;
}

// ─── Estudiante ───────────────────────────────────────────────────────────────

export interface Estudiante {
  id: string;
  codigoSistema: string;
  // Datos personales
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  fechaNacimiento: string | null;   // 'YYYY-MM-DD'
  tipoDocumento: TipoDocumento | null;
  numeroDocumento: string | null;
  sexo: Sexo | null;
  estadoCivil: EstadoCivil | null;
  nacionalidad: string | null;
  discapacidad: boolean;
  // Contacto
  celular: string | null;
  emailPersonal: string | null;
  emailInstitucional: string | null;
  // Académico
  programaDoctorado: ProgramaDoctorado | null;
  codMatricula: string | null;
  anioIngreso: number | null;
  financiamiento: Financiamiento | null;
  condicion: Condicion | null;
  // Laboral
  centroLaboral: CentroLaboral | null;
  cargoActual: Cargo | null;
  procedencia: Procedencia | null;
  orcid: string | null;
  // Otros
  observaciones: string | null;
  // Auditoría
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface EstudianteRequest {
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno?: string;
  fechaNacimiento?: string;         // 'YYYY-MM-DD'
  tipoDocumento?: TipoDocumento;
  numeroDocumento?: string;
  sexo?: Sexo;
  estadoCivil?: EstadoCivil;
  nacionalidad?: string;
  discapacidad?: boolean;
  celular?: string;
  emailPersonal?: string;
  emailInstitucional?: string;
  programaDoctoradoId?: string;
  codMatricula?: string;
  anioIngreso?: number;
  financiamiento?: Financiamiento;
  condicion?: Condicion;
  centroLaboralId?: string;
  cargoActualId?: string;
  procedencia?: Procedencia;
  orcid?: string;
  observaciones?: string;
}

export type EstudianteUpdate = Partial<EstudianteRequest>;

// ─── Documento de Estudiante ──────────────────────────────────────────────────

export interface DocumentoEstudiante {
  id: string;
  estudianteId: string;
  tipoDocumento: TipoDocumento;
  nombreOriginal: string;
  contentType: string;
  tamanioBytes: number;
  storageKey: string;
  hashSha256: string;
  fechaCarga: string;
  active: boolean;
  createdAt: string;
}
