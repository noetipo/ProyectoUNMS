export const END_POINTS = {
  oauth: {
    login: 'api/v1/auth/login',
    register: 'api/v1/auth/register',
    refresh: 'api/v1/auth/refresh',
    logout: 'api/v1/auth/logout',
    logoutAll: 'api/v1/auth/logout-all',
    validate: 'api/v1/auth/validate',
    me: 'api/v1/auth/me',
    // Registro de usuario
    signup: 'api/v1/auth/register',
    // TODO: estos endpoints aún no existen en el backend (vivían en CompanyResource, eliminado)
    forgotPassword: 'api/v1/auth/forgot-password',
    resetPassword: 'api/v1/auth/reset-password',
    verifyEmail: 'api/v1/auth/verify-email',
  },
  setup: {
    parentModule: 'parent-module',
    parentModuleList: 'parent-module/list',
    userRole: 'api/user-roles',
    module: 'api/modules',
    moduleMenu: 'api/modules/menu',
    role: 'api/roles',
    roleModule: 'api/roles/module',
    users: 'api/v1/users',
    categories: 'api/v1/categories',
  },
  personas: {
    base: 'api/personas',
    estudiantes: 'api/estudiantes',
    docentes: 'api/docentes',
    miPerfil: 'api/mi-perfil',
    programasPosgrado: 'api/programas-posgrado',
  },
  catalogos: {
    cargos: 'api/cargos',
    centrosLaborales: 'api/centros-laborales',
    lineasInvestigacion: 'api/lineas-investigacion',
  },
  configuracion: {
    facultades: 'api/facultades',
    lemasAnuales: 'api/lemas-anuales',
    parametrosSistema: 'api/parametros-sistema',
  },
  asesorias: {
    solicitudes: 'api/solicitudes-asesoria',
    miAsesoria: 'api/mi-asesoria', // + /documentos/{tipo}
  },
  secretaria: {
    dictamenes: 'api/secretaria/dictamenes',
    defensa: 'api/secretaria/defensa',   // Etapa 5 · recepción de expedientes
  },
  proyecto: {
    miProyecto: 'api/mi-proyecto',        // Etapa 4 · editor del estudiante
    asesorProyectos: 'api/asesor/proyectos', // Etapa 4 · revisión del asesor
    tutorProyectos: 'api/tutor/proyectos',   // Etapa 4 · supervisión del tutor
  },
  expediente: {
    mio: 'api/mi-expediente',             // expediente del estudiante autenticado
    base: 'api/expedientes',              // + /{tesisId} (secretaría/admin/coordinador)
  },
  notificaciones: {
    base: 'api/notificaciones',           // campanita (por rol)
  },
  tutorias: {
    base: 'api/tutorias',
  },
  tutorSugerencias: {
    base: 'api/tutor', // /estudiantes/{id}/sugerencias, /sugerencias/{id}
  },
  reportes: {
    tutores: 'api/reportes/tutores',
    estudiantesSinTutor: 'api/reportes/estudiantes-sin-tutor',
  },
  tutor: {
    misTutorandos: 'api/mis-tutorandos',
  },
  coordinador: {
    estudiantesTema: 'api/coordinador/estudiantes-tema',
    tema: 'api/coordinador/estudiantes', // + /{estudianteId}/tema
    miTema: 'api/mi-perfil/tema',
    proyectos: 'api/coordinador/proyectos', // Etapa 5 · designación de revisores
  },
  revisor: {
    proyectos: 'api/revisor/proyectos', // Etapa 5 · evaluación con rúbrica
  },
  ejecucion: {
    asesor: 'api/asesor/ejecucion', // Etapa 6 · ejecución de la tesis
  },
  juradoInforme: {
    base: 'api/jurado-informe', // Etapa 7 · Jurado Informante del informe final
  },
  catalog: { base: 'api/catalog' },
  client: { base: 'api/client' },
  reports: { base: 'api/reports' },
  accounting: { base: 'api/accounting' },
  buys: { base: 'api/buys' },
  payments: { base: 'api/payments' },
  sales: { base: 'api/sales' },
  warehouseMovement: { base: 'api/warehouse-movement' },
};