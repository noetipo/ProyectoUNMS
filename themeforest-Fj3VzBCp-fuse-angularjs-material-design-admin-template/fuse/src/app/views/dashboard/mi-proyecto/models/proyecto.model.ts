// ─────────────────────────────────────────────────────────────────────────────
// Modelos del editor "Proyecto de tesis en línea" (Etapa 4).
// La estructura de secciones/campos refleja `proyDef` del prototipo.
// ─────────────────────────────────────────────────────────────────────────────

export interface CampoDef {
  k: string;
  l: string;
  input?: boolean;   // true = una línea; si no, textarea
  rows?: number;
  ph?: string;
}

export interface SeccionDef {
  id: string;
  titulo: string;
  sub?: string;
  campos: CampoDef[];
  cuant?: boolean;   // visible solo con enfoque CUANTITATIVO
  cual?: boolean;    // visible solo con enfoque CUALITATIVO
  wObj?: boolean;    // incluye objetivos específicos
  wPlan?: boolean;   // incluye cronograma + presupuesto
  wMatriz?: boolean; // incluye la matriz de consistencia (solo lectura)
}

/** Definición del editor (equivalente a proyDef del prototipo). */
export const PROY_DEF: SeccionDef[] = [
  { id: 'g', titulo: 'Datos generales', sub: 'Identificación del proyecto', campos: [
    { k: 'titulo', l: 'Título del proyecto', input: true, ph: 'Título tentativo de la tesis' },
    { k: 'resumen', l: 'Resumen', rows: 4, ph: 'Máx. 2000 caracteres — problema, objetivo, método y resultados esperados' },
    { k: 'palabras', l: 'Palabras clave (3 a 5, separadas por punto y coma)', input: true, ph: 'Ej.: diabetes mellitus tipo 2; altitud; prevalencia' },
  ] },
  { id: 'p1', titulo: 'I. Planteamiento del problema', sub: 'Importancia, novedad, interés y viabilidad del tema', wObj: true, campos: [
    { k: 'situacion', l: 'Situación problemática', rows: 5, ph: 'Qué se ha investigado, hasta dónde se ha llegado y qué falta por hacer' },
    { k: 'formulacion', l: 'Formulación del problema', rows: 2, ph: 'Pregunta específica derivada de la situación problemática' },
    { k: 'justificacion', l: 'Justificación de la investigación', rows: 3, ph: 'Conveniencia teórica, práctica, metodológica y social' },
    { k: 'objGeneral', l: 'Objetivo general', rows: 2 },
  ] },
  { id: 'p2', titulo: 'II. Marco teórico', sub: 'Antecedentes vinculados al problema', campos: [
    { k: 'antecedentes', l: 'Antecedentes del problema', rows: 4 },
    { k: 'bases', l: 'Bases teóricas', rows: 4 },
    { k: 'glosario', l: 'Definición conceptual de términos / glosario', rows: 3 },
  ] },
  { id: 'p3', titulo: 'III. Hipótesis y variables', sub: 'Solo enfoque cuantitativo', cuant: true, campos: [
    { k: 'hipotesis', l: 'Hipótesis de la investigación', rows: 2 },
    { k: 'variables', l: 'Identificación de las variables', rows: 3 },
    { k: 'operacional', l: 'Operacionalización de variables', rows: 3, ph: 'Variable · definición operacional · dimensiones · indicadores · escala' },
  ] },
  { id: 'p3q', titulo: 'III. Categorías del estudio', sub: 'Solo enfoque cualitativo', cual: true, campos: [
    { k: 'categorias', l: 'Categorías apriorísticas', rows: 3 },
    { k: 'supuestos', l: 'Supuestos de la investigación', rows: 2 },
  ] },
  { id: 'p4', titulo: 'IV. Metodología', sub: 'Con fuerte énfasis de los revisores en el método', campos: [
    { k: 'diseno', l: 'Tipo y diseño de investigación', rows: 2 },
    { k: 'poblacion', l: 'Población y muestra / participantes', rows: 3 },
    { k: 'tecnicas', l: 'Técnicas e instrumentos de recolección de datos', rows: 3 },
    { k: 'analisis', l: 'Plan de análisis de datos', rows: 3 },
    { k: 'eticos', l: 'Aspectos éticos', rows: 2, ph: 'Consentimiento informado, comité(s) de ética, confidencialidad' },
  ] },
  { id: 'p5', titulo: 'V. Aspectos administrativos', sub: 'Cronograma y presupuesto por partidas', campos: [], wPlan: true },
  { id: 'p6', titulo: 'VI. Referencias y anexos', sub: 'Estilo Vancouver', wMatriz: true, campos: [
    { k: 'referencias', l: 'Referencias bibliográficas', rows: 4 },
    { k: 'anexos', l: 'Anexos (instrumentos, consentimiento informado)', rows: 2 },
  ] },
];

export const FASES = ['Planificación', 'Trabajo de campo', 'Análisis', 'Redacción'];
export const FASE_ENUM: Record<string, string> = {
  'Planificación': 'PLANIFICACION', 'Trabajo de campo': 'TRABAJO_CAMPO',
  'Análisis': 'ANALISIS', 'Redacción': 'REDACCION',
};
export const FASE_LABEL: Record<string, string> = {
  PLANIFICACION: 'Planificación', TRABAJO_CAMPO: 'Trabajo de campo',
  ANALISIS: 'Análisis', REDACCION: 'Redacción',
};
export const ESTADOS_ACTIVIDAD = ['PENDIENTE', 'EN_CURSO', 'HECHA'];
export const RUBROS = ['Personal de apoyo', 'Insumos de laboratorio', 'Equipos', 'Servicios', 'Movilidad', 'Publicación', 'Otros'];
export const FINANCIAMIENTOS = ['Autofinanciado', 'Beca / PROCIENCIA', 'Fondo concursable VRIP'];

export interface RevisionEvento {
  tipo: string; autor?: string; rol?: string; texto?: string; fecha?: string;
}
export interface RevisionItem {
  campo: string; estado: string; eventos: RevisionEvento[];
}
export interface ObjetivoItem { id?: string; orden?: number; texto?: string; }
export interface ActividadItem {
  id?: string; nombre: string; fase?: string;
  mesInicio?: number; mesFin?: number;
  fechaInicio?: string; fechaFin?: string;   // ISO 'yyyy-MM-dd'
  estado?: string; orden?: number;
}

const _MESES_ABREV = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'set', 'oct', 'nov', 'dic'];

/** Índice de mes absoluto (año*12 + mes-1) de una fecha ISO 'yyyy-MM-dd'. */
function _mesIdx(iso?: string): number | null {
  if (!iso) return null;
  const [y, m] = iso.split('-').map(Number);
  if (!y || !m) return null;
  return y * 12 + (m - 1);
}

/** Meses del cronograma (columnas del Gantt) derivados de las fechas de las actividades. */
export function ganttMeses(acts: ActividadItem[]): { idx: number; label: string }[] {
  const ini = acts.map((a) => _mesIdx(a.fechaInicio)).filter((x): x is number => x != null);
  if (!ini.length) return [];
  const fin = acts.map((a) => _mesIdx(a.fechaFin) ?? _mesIdx(a.fechaInicio)).filter((x): x is number => x != null);
  const min = Math.min(...ini);
  const max = Math.max(...fin, min);
  const out: { idx: number; label: string }[] = [];
  for (let i = min; i <= max; i++) {
    out.push({ idx: i, label: `${_MESES_ABREV[i % 12]} ${String(Math.floor(i / 12)).slice(2)}` });
  }
  return out;
}

/** Columna CSS grid (start / end) de la barra de una actividad, dado el mes-índice inicial del cronograma. */
export function ganttColFecha(a: ActividadItem, minIdx: number): string | null {
  const s = _mesIdx(a.fechaInicio);
  if (s == null) return null;
  const e = _mesIdx(a.fechaFin) ?? s;
  const start = s - minIdx;
  const end = Math.max(start, e - minIdx);
  return `${start + 1} / ${end + 2}`;
}

/** Rango de fechas legible de una actividad (ej. "ene 2026 – mar 2026"). */
export function rangoFechas(a: ActividadItem): string {
  const fmt = (iso?: string) => {
    const i = _mesIdx(iso);
    return i == null ? '' : `${_MESES_ABREV[i % 12]} ${Math.floor(i / 12)}`;
  };
  const a1 = fmt(a.fechaInicio), a2 = fmt(a.fechaFin);
  if (!a1) return '';
  return a2 && a2 !== a1 ? `${a1} – ${a2}` : a1;
}
export interface PartidaItem { id?: string; rubro?: string; descripcion?: string; monto?: number; orden?: number; }

export interface RevisorEval {
  revisorId?: string; orden?: number; estado?: string;
  docenteNombre?: string; docenteCategoria?: string; docenteLinea?: string;
  comentario?: string; respuesta?: string;
  puntajeTotal?: number; puntajeMaximo?: number; aprobado?: boolean;
}

export interface AvanceEval {
  id?: string; fechaEvaluacion?: string; puntajeTotal?: number;
  porcentajePlan?: number; comentario?: string;
}

export interface JuradoInformeEval {
  id?: string; orden?: number; presidente?: boolean; estado?: string;
  puntaje?: number; comentario?: string; respuesta?: string;
}

export interface ReferenciaItem {
  id?: string; orden?: number; tipo?: string;
  autores?: string; anio?: string; titulo?: string; fuente?: string;
  volumen?: string; numero?: string; paginas?: string;
  editorial?: string; ciudad?: string; doi?: string; url?: string; fechaAcceso?: string;
  formateada?: string; citaTexto?: string; citaNarrativa?: string;
}

/** Tipos de fuente para una referencia. */
export const TIPOS_REFERENCIA: { v: string; l: string }[] = [
  { v: 'ARTICULO', l: 'Artículo de revista' },
  { v: 'LIBRO', l: 'Libro' },
  { v: 'CAPITULO_LIBRO', l: 'Capítulo de libro' },
  { v: 'TESIS', l: 'Tesis' },
  { v: 'PAGINA_WEB', l: 'Página web' },
  { v: 'INFORME', l: 'Informe' },
];
export const ESTILOS_CITA: { v: string; l: string; hint: string }[] = [
  { v: 'APA', l: 'APA 7', hint: 'Psicología, educación, salud y ciencias sociales. Cita con autor y año: (García, 2023).' },
  { v: 'VANCOUVER', l: 'Vancouver', hint: 'Medicina y ciencias de la salud. Cita numérica: [1].' },
  { v: 'IEEE', l: 'IEEE', hint: 'Ingeniería y tecnología. Cita numérica: [1].' },
  { v: 'HARVARD', l: 'Harvard', hint: 'Economía, negocios y ciencias sociales. Cita con autor y año: (García and Pérez, 2023).' },
  { v: 'MLA', l: 'MLA 9', hint: 'Humanidades, letras y lingüística. Cita con autor: (García).' },
  { v: 'CHICAGO', l: 'Chicago', hint: 'Historia, artes y humanidades. Cita con autor y año: (García 2023).' },
];

export interface ProyectoEditor {
  tesisId: string; proyectoId: string;
  estudianteNombre?: string; codigoSistema?: string; programaNombre?: string;
  asesorNombre?: string; lineaNombre?: string; nivel?: string;
  titulo?: string; resumen?: string;
  enfoque: string; enfoqueBloqueado: boolean; estado: string; financiamiento?: string;
  listoRevision: boolean; planPublicado: boolean; cartaAsesor: boolean;
  /** true cuando quien consulta es el co-asesor: ve el proyecto pero no puede actuar. */
  soloLectura?: boolean;
  turnitinSubido: boolean; proyectoFinalSubido: boolean; expedienteSubido: boolean; expedienteRecibido: boolean; porcentajeSimilitud?: number;
  avanceCompletados: number; avanceTotal: number; avancePct: number;
  campos: Record<string, string>;
  objetivos: ObjetivoItem[]; actividades: ActividadItem[]; partidas: PartidaItem[];
  presupuestoTotal?: number; revisiones: RevisionItem[];
  referencias?: ReferenciaItem[]; estiloCita?: string; estiloCitaBloqueado?: boolean;
  evaluacionesRevisores?: RevisorEval[]; revisoresConformes?: boolean;
  defensaProgramada?: boolean; fechaDefensa?: string; horaDefensa?: string;
  lugarDefensa?: string; dictamenNumero?: string;
  informeFinalSubido?: boolean; informeFinalAprobado?: boolean; avances?: AvanceEval[];
  juradoInformanteSolicitado?: boolean; informeFinalRevisado?: boolean; evaluacionesJuradoInforme?: JuradoInformeEval[];
  puedeMarcarListo: boolean; todosConformes: boolean;
}

/** Chip de estado de revisión por ítem. */
export function chipRevision(estado?: string): { label: string; cls: string } {
  switch (estado) {
    case 'OBSERVADO': return { label: '⚑ OBSERVADO', cls: 'bg-rose-100 text-rose-700' };
    case 'EN_CORRECCION': return { label: '✎ EN CORRECCIÓN', cls: 'bg-amber-100 text-amber-700' };
    case 'CORREGIDO': return { label: '↺ CORREGIDO · POR VERIFICAR', cls: 'bg-sky-100 text-sky-700' };
    case 'CONFORME': return { label: '✓ CONFORME', cls: 'bg-emerald-100 text-emerald-700' };
    default: return { label: 'SIN REVISIÓN', cls: 'bg-slate-100 text-slate-500' };
  }
}

/** Color del badge de un evento del historial de revisión. */
export function tipoBadge(tipo?: string): string {
  switch (tipo) {
    case 'OBSERVACIÓN': return 'bg-rose-100 text-rose-700';
    case 'EDICIÓN': return 'bg-slate-200 text-slate-600';
    case 'CORRECCIÓN': return 'bg-sky-100 text-sky-700';
    case 'CONFORMIDAD': return 'bg-emerald-100 text-emerald-700';
    default: return 'bg-slate-100 text-slate-500';
  }
}

/** Color del punto del evento en la línea de tiempo del historial. */
export function tipoDot(tipo?: string): string {
  switch (tipo) {
    case 'OBSERVACIÓN': return 'bg-rose-500';
    case 'EDICIÓN': return 'bg-slate-400';
    case 'CORRECCIÓN': return 'bg-sky-500';
    case 'CONFORMIDAD': return 'bg-emerald-500';
    default: return 'bg-slate-300';
  }
}

/**
 * Etiqueta y color del AUTOR de un evento, para que el estudiante distinga de un vistazo
 * quién intervino: el asesor (lila) o un revisor (verde azulado). Sus propias correcciones en gris.
 */
export function rolBadge(rol?: string): { label: string; cls: string } | null {
  switch (rol) {
    case 'ASESOR': return { label: 'Asesor', cls: 'bg-violet-100 text-violet-700 ring-1 ring-violet-200' };
    case 'REVISOR': return { label: 'Revisor', cls: 'bg-teal-100 text-teal-700 ring-1 ring-teal-200' };
    case 'ESTUDIANTE': return { label: 'Tú', cls: 'bg-slate-100 text-slate-500 ring-1 ring-slate-200' };
    default: return null;
  }
}

/** Verbo legible del evento, para encabezar cada paso del historial. */
export function tipoVerbo(tipo?: string): string {
  switch (tipo) {
    case 'OBSERVACIÓN': return 'observó';
    case 'EDICIÓN': return 'editó';
    case 'CORRECCIÓN': return 'corrigió';
    case 'CONFORMIDAD': return 'dio conformidad';
    default: return '';
  }
}
