/** Conteo de alumnos en una etapa del proceso. */
export interface EtapaConteo {
  numero: number;
  titulo: string;
  cantidad: number;
}

/** Tarjetas de estadística del tablero. */
export interface SeguimientoResumen {
  total: number;
  finalizados: number;
  pendientesSecretaria: number;
  sinTema: number;
  detenidos: number;
  dictamenesPorVencer: number;
  porEtapa: EtapaConteo[];
}

/** Fila del tablero: un doctorando con su etapa y qué lo tiene detenido. */
export interface SeguimientoAlumno {
  estudianteId: string;
  tesisId?: string;
  apellidos: string;
  nombres: string;
  codigoSistema?: string;
  programaNombre?: string;
  tituloTesis?: string;
  lineaNombre?: string;
  tutorNombre?: string;
  asesorNombre?: string;
  etapaNumero: number;
  etapaTitulo: string;
  avancePct: number;
  estadoDerivado?: string;
  estadoLabel?: string;
  pendiente: string;
  responsable: string;
  pendienteSecretaria: boolean;
  /** Pantalla donde se resuelve el pendiente (ya trae ?tesis={id}); null si no hay una. */
  accionLink?: string | null;
  accionLabel?: string | null;
  fechaUltimoHito?: string;
  diasEnEtapa?: number | null;
  fechaDefensa?: string;
  vigenciaVenceEl?: string;
  vigenciaMeses?: number | null;
  vigenciaVencida: boolean;
}

export interface SeguimientoTablero {
  resumen: SeguimientoResumen;
  alumnos: SeguimientoAlumno[];
}

/** Color del chip del responsable de la acción pendiente. */
export function claseResponsable(responsable: string): string {
  switch (responsable) {
    case 'SECRETARIA': return 'bg-[#FDF6F7] text-[#8C1D2E] border border-[#8C1D2E]/20';
    case 'COORDINADOR': return 'bg-indigo-50 text-indigo-700 border border-indigo-100';
    case 'ASESOR': return 'bg-sky-50 text-sky-700 border border-sky-100';
    case 'REVISOR': return 'bg-violet-50 text-violet-700 border border-violet-100';
    case 'JURADO': return 'bg-amber-50 text-amber-700 border border-amber-100';
    case 'ESTUDIANTE': return 'bg-slate-100 text-slate-600 border border-slate-200';
    default: return 'bg-emerald-50 text-emerald-700 border border-emerald-100';
  }
}

/** Etiqueta legible del responsable. */
export function etiquetaResponsable(responsable: string): string {
  switch (responsable) {
    case 'SECRETARIA': return 'Secretaría';
    case 'COORDINADOR': return 'Coordinador';
    case 'ASESOR': return 'Asesor';
    case 'REVISOR': return 'Revisores';
    case 'JURADO': return 'Jurado';
    case 'ESTUDIANTE': return 'Estudiante';
    default: return 'Completado';
  }
}

/** Semáforo de inactividad: verde al día, ámbar >30 días, rojo >60. */
export function claseDias(dias?: number | null): string {
  if (dias === null || dias === undefined) return 'bg-slate-100 text-slate-400';
  if (dias > 60) return 'bg-rose-100 text-rose-700';
  if (dias > 30) return 'bg-amber-100 text-amber-700';
  return 'bg-emerald-50 text-emerald-600';
}

/** Texto del tiempo sin movimiento. */
export function etiquetaDias(dias?: number | null): string {
  if (dias === null || dias === undefined) return '—';
  if (dias === 0) return 'hoy';
  if (dias === 1) return '1 día';
  return `${dias} días`;
}

/* ── Movimiento del expediente (bajo la barra de avance) ─────────────────────
   Es el dato que antes vivía en una columna llamada "Inactivo", que se leía al
   revés ("Inactivo: hoy"). Ahora se redacta como frase y acompaña a la etapa,
   que es de lo que habla: hace cuánto que ese expediente no registra un hito. */

/** Frase del último movimiento del expediente. */
export function textoMovimiento(dias?: number | null): string {
  if (dias === null || dias === undefined) return 'sin hitos registrados';
  if (dias === 0) return 'se movió hoy';
  if (dias === 1) return 'sin cambios desde ayer';
  return `sin cambios hace ${dias} días`;
}

/** Color: gris sin datos · slate al día · ámbar >30 días · rosa >60 (igual que los chips). */
export function colorMovimiento(dias?: number | null): string {
  if (dias === null || dias === undefined) return 'text-slate-300';
  if (dias > 60) return 'text-rose-500';
  if (dias > 30) return 'text-amber-600';
  return 'text-slate-400';
}

/** Icono lucide: reloj mientras va al día, alerta cuando el expediente se estancó. */
export function iconoMovimiento(dias?: number | null): string {
  if (dias === null || dias === undefined) return 'circle-dashed';
  return dias > 30 ? 'triangle-alert' : 'clock';
}

/** Chip de vigencia del dictamen (4 años): rojo vencido, ámbar si faltan ≤6 meses. */
export function claseVigencia(meses?: number | null): string {
  if (meses === null || meses === undefined) return '';
  if (meses < 0) return 'bg-rose-100 text-rose-700';
  if (meses <= 6) return 'bg-amber-100 text-amber-700';
  return 'bg-slate-100 text-slate-500';
}

export function etiquetaVigencia(meses?: number | null): string {
  if (meses === null || meses === undefined) return '';
  if (meses < 0) return `Dictamen vencido hace ${Math.abs(meses)} m`;
  if (meses === 0) return 'Dictamen vence este mes';
  return `Dictamen vence en ${meses} m`;
}

/** Color de la barra de avance según qué tan lejos llegó el alumno. */
export function claseAvance(pct: number): string {
  if (pct >= 88) return 'bg-emerald-500';
  if (pct >= 50) return 'bg-[#8C1D2E]';
  if (pct >= 25) return 'bg-amber-500';
  return 'bg-slate-400';
}
