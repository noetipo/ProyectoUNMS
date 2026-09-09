import { Routes } from '@angular/router';
import { AdminLayout } from './layout/layout';
import { roleGuard } from '@/app/core/guards/role.guard';
import { END_POINTS } from '@/app/providers/utils/end-points';

const routes: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [
      // Redirect empty path to '/sign-in'
      { path: '', pathMatch: 'full', redirectTo: '/sign-in' },

      // -----------------------------------------------------------------------
      // Dashboards
      // -----------------------------------------------------------------------
      {
        path: 'dashboards',
        loadChildren: () => import('./modules/dashboards/routes'),
      },

      // -----------------------------------------------------------------------
      // Setup (Configuration modules)
      // -----------------------------------------------------------------------
      {
        path: 'setup',
        loadChildren: () =>
          import('@/app/views/dashboard/setup/setup.routes'),
      },

      // -----------------------------------------------------------------------
      // Personas (reporte + CRUD) — ADMIN / SECRETARIA
      // -----------------------------------------------------------------------
      {
        path: 'personas',
        canActivate: [roleGuard('ADMIN', 'SECRETARIA')],
        loadChildren: () =>
          import('@/app/views/dashboard/personas/personas.routes'),
      },

      // -----------------------------------------------------------------------
      // Estudiantes (reporte) — ADMIN / SECRETARIA / COORDINADOR
      // -----------------------------------------------------------------------
      {
        path: 'estudiantes',
        canActivate: [roleGuard('ADMIN', 'SECRETARIA', 'COORDINADOR')],
        loadChildren: () =>
          import('@/app/views/dashboard/estudiantes/estudiantes.routes'),
      },

      // -----------------------------------------------------------------------
      // Docentes (reporte) — ADMIN / SECRETARIA / COORDINADOR
      // -----------------------------------------------------------------------
      {
        path: 'docentes',
        canActivate: [roleGuard('ADMIN', 'SECRETARIA', 'COORDINADOR')],
        loadChildren: () =>
          import('@/app/views/dashboard/docentes/docentes.routes'),
      },

      // -----------------------------------------------------------------------
      // Mantenimientos de catálogos — ADMIN / SECRETARIA
      // -----------------------------------------------------------------------
      {
        path: 'cargos',
        canActivate: [roleGuard('ADMIN', 'SECRETARIA')],
        data: { titulo: 'Cargos', singular: 'cargo', base: END_POINTS.catalogos.cargos, icon: 'briefcase', ejemplo: 'MEDICO ASISTENTE' },
        loadComponent: () =>
          import('@/app/shared/catalogo/catalogo-crud.component').then((m) => m.CatalogoCrudComponent),
      },
      {
        path: 'centros-laborales',
        canActivate: [roleGuard('ADMIN', 'SECRETARIA')],
        data: { titulo: 'Centros laborales', singular: 'centro laboral', base: END_POINTS.catalogos.centrosLaborales, icon: 'building-office', ejemplo: 'HOSPITAL NACIONAL DOS DE MAYO' },
        loadComponent: () =>
          import('@/app/shared/catalogo/catalogo-crud.component').then((m) => m.CatalogoCrudComponent),
      },
      {
        path: 'lineas-investigacion',
        canActivate: [roleGuard('ADMIN', 'SECRETARIA')],
        data: { titulo: 'Líneas de investigación', singular: 'línea de investigación', base: END_POINTS.catalogos.lineasInvestigacion, icon: 'light-bulb', ejemplo: 'ONCOLOGÍA Y CÁNCER' },
        loadComponent: () =>
          import('@/app/shared/catalogo/catalogo-crud.component').then((m) => m.CatalogoCrudComponent),
      },
      {
        path: 'facultades',
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'SECRETARIA')],
        loadComponent: () =>
          import('@/app/views/dashboard/configuracion/facultades.component').then((m) => m.FacultadesComponent),
      },
      {
        path: 'lemas-anuales',
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'SECRETARIA')],
        loadComponent: () =>
          import('@/app/views/dashboard/configuracion/lemas-anuales.component').then((m) => m.LemasAnualesComponent),
      },
      {
        path: 'parametros-sistema',
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'SECRETARIA')],
        loadComponent: () =>
          import('@/app/views/dashboard/configuracion/parametros-sistema.component').then((m) => m.ParametrosSistemaComponent),
      },

      // -----------------------------------------------------------------------
      // Tutorías · Asignación de tutor en bloque — ADMIN / SECRETARIA / COORDINADOR
      // -----------------------------------------------------------------------
      {
        path: 'asignar-tutor',
        canActivate: [roleGuard('ADMIN', 'SECRETARIA', 'COORDINADOR')],
        loadComponent: () =>
          import('@/app/views/dashboard/tutorias/components/asignar-tutor-bloque.component').then((m) => m.AsignarTutorBloqueComponent),
      },
      {
        path: 'reporte-tutores',
        canActivate: [roleGuard('ADMIN', 'SECRETARIA', 'COORDINADOR', 'COORD_PROG', 'PERS_ADMIN',
          'DECANO', 'VICEDECANO', 'JEFE_UPG', 'COORD_SEC')],
        loadComponent: () =>
          import('@/app/views/dashboard/reportes/components/reporte-tutores.component').then((m) => m.ReporteTutoresComponent),
      },
      {
        path: 'mis-tutorandos',
        canActivate: [roleGuard('PROF_TUTOR')],
        loadComponent: () =>
          import('@/app/views/dashboard/mis-tutorandos/components/mis-tutorandos.component').then((m) => m.MisTutorandosComponent),
      },
      {
        path: 'rubricas-oficiales',
        canActivate: [roleGuard('SECRETARIA', 'ADMIN', 'COORDINADOR', 'COORD_PROG')],
        loadComponent: () =>
          import('@/app/views/dashboard/rubricas-oficiales/components/rubricas-oficiales.component').then((m) => m.RubricasOficialesComponent),
      },
      {
        path: 'registro-tema',
        canActivate: [roleGuard('COORDINADOR', 'COORD_PROG', 'COORD_SEC', 'ADMIN', 'SECRETARIA')],
        loadComponent: () =>
          import('@/app/views/dashboard/registro-tema/components/registro-tema-report.component').then((m) => m.RegistroTemaReportComponent),
      },
      // -----------------------------------------------------------------------
      // Estudiante · "Mi tesis": una sola entrada de menú con pestañas
      // (avance del proceso · editor del proyecto · asesoría).
      // -----------------------------------------------------------------------
      {
        path: 'mi-tesis',
        canActivate: [roleGuard('ESTUDIANTE')],
        loadComponent: () =>
          import('@/app/views/dashboard/mi-tesis/components/mi-tesis.component').then((m) => m.MiTesisComponent),
        children: [
          { path: '', redirectTo: 'avance', pathMatch: 'full' },
          {
            path: 'avance',
            loadComponent: () =>
              import('@/app/views/dashboard/expediente/components/expediente-tesis.component').then((m) => m.ExpedienteTesisComponent),
          },
          {
            path: 'proyecto',
            loadComponent: () =>
              import('@/app/views/dashboard/mi-proyecto/components/mi-proyecto.component').then((m) => m.MiProyectoComponent),
          },
          {
            path: 'cierre',
            loadComponent: () =>
              import('@/app/views/dashboard/mi-proyecto/components/cierre-envio.component').then((m) => m.CierreEnvioComponent),
          },
          {
            path: 'ejecucion',
            loadComponent: () =>
              import('@/app/views/dashboard/mi-proyecto/components/ejecucion-tesis.component').then((m) => m.EjecucionTesisComponent),
          },
          {
            path: 'sustentacion',
            loadComponent: () =>
              import('@/app/views/dashboard/mi-proyecto/components/sustentacion-tesis.component').then((m) => m.SustentacionTesisComponent),
          },
          {
            path: 'asesoria',
            loadComponent: () =>
              import('@/app/views/dashboard/mi-asesoria/components/mi-asesoria.component').then((m) => m.MiAsesoriaComponent),
          },
        ],
      },
      // Rutas antiguas (enlaces de las notificaciones y marcadores) → pestaña equivalente.
      { path: 'mi-asesoria', redirectTo: 'mi-tesis/asesoria', pathMatch: 'full' },
      { path: 'expediente', redirectTo: 'mi-tesis/avance', pathMatch: 'full' },
      { path: 'mi-proyecto', redirectTo: 'mi-tesis/proyecto', pathMatch: 'full' },
      {
        path: 'revision-proyecto',
        canActivate: [roleGuard('ASESOR')],
        loadComponent: () =>
          import('@/app/views/dashboard/revision-proyecto/components/revision-bandeja.component').then((m) => m.RevisionBandejaComponent),
      },
      {
        path: 'revision-proyecto/:tesisId',
        canActivate: [roleGuard('ASESOR')],
        loadComponent: () =>
          import('@/app/views/dashboard/revision-proyecto/components/revision-proyecto.component').then((m) => m.RevisionProyectoComponent),
      },
      {
        path: 'supervision-proyecto',
        canActivate: [roleGuard('PROF_TUTOR')],
        loadComponent: () =>
          import('@/app/views/dashboard/supervision-proyecto/components/supervision-bandeja.component').then((m) => m.SupervisionBandejaComponent),
      },
      {
        path: 'supervision-proyecto/:tesisId',
        canActivate: [roleGuard('PROF_TUTOR')],
        loadComponent: () =>
          import('@/app/views/dashboard/supervision-proyecto/components/supervision-proyecto.component').then((m) => m.SupervisionProyectoComponent),
      },
      {
        path: 'seguimiento-alumnos',
        canActivate: [roleGuard('SECRETARIA', 'COORDINADOR', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/seguimiento-alumnos/components/seguimiento-alumnos.component').then((m) => m.SeguimientoAlumnosComponent),
      },
      {
        // Expediente de UN doctorando (consulta de secretaría/coordinación) — misma línea de
        // tiempo que ve el alumno, en modo lectura.
        path: 'expedientes/:tesisId',
        canActivate: [roleGuard('SECRETARIA', 'COORDINADOR', 'COORD_PROG', 'COORD_SEC', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/expediente/components/expediente-tesis.component').then((m) => m.ExpedienteTesisComponent),
      },
      {
        path: 'secretaria-defensa',
        canActivate: [roleGuard('SECRETARIA', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/secretaria-defensa/components/secretaria-defensa.component').then((m) => m.SecretariaDefensaComponent),
      },
      {
        // Tramo final de la Etapa 5: rúbricas de la defensa, dictamen de aprobación y archivo.
        path: 'cierre-proyecto',
        canActivate: [roleGuard('SECRETARIA', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/cierre-proyecto/components/cierre-bandeja.component').then((m) => m.CierreBandejaComponent),
      },
      {
        path: 'cierre-proyecto/:tesisId',
        canActivate: [roleGuard('SECRETARIA', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/cierre-proyecto/components/cierre-detalle.component').then((m) => m.CierreDetalleComponent),
      },
      {
        path: 'coordinador-proyecto',
        canActivate: [roleGuard('COORDINADOR', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/coordinador-proyecto/components/coordinador-proyecto.component').then((m) => m.CoordinadorProyectoComponent),
      },
      {
        path: 'revisor-proyecto',
        canActivate: [roleGuard('DOCENTE', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/revisor-proyecto/components/revisor-bandeja.component').then((m) => m.RevisorBandejaComponent),
      },
      {
        path: 'revisor-proyecto/:tesisId',
        canActivate: [roleGuard('DOCENTE', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/revisor-proyecto/components/revisor-evaluar.component').then((m) => m.RevisorEvaluarComponent),
      },
      {
        path: 'ejecucion-tesis',
        canActivate: [roleGuard('ASESOR', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/ejecucion-tesis/components/ejecucion-bandeja.component').then((m) => m.EjecucionBandejaComponent),
      },
      {
        path: 'ejecucion-tesis/:tesisId',
        canActivate: [roleGuard('ASESOR', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/ejecucion-tesis/components/ejecucion-detalle.component').then((m) => m.EjecucionDetalleComponent),
      },
      {
        path: 'jurado-informe',
        canActivate: [roleGuard('DOCENTE', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/jurado-informe/components/jurado-informe-bandeja.component').then((m) => m.JuradoInformeBandejaComponent),
      },
      {
        path: 'jurado-informe/:tesisId',
        canActivate: [roleGuard('DOCENTE', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/jurado-informe/components/jurado-informe-evaluar.component').then((m) => m.JuradoInformeEvaluarComponent),
      },
      {
        path: 'jurado-informante',
        canActivate: [roleGuard('SECRETARIA', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/jurado-informante/components/jurado-informante-bandeja.component').then((m) => m.JuradoInformanteBandejaComponent),
      },
      {
        path: 'jurado-informante/:tesisId',
        canActivate: [roleGuard('SECRETARIA', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/jurado-informante/components/jurado-informante-detalle.component').then((m) => m.JuradoInformanteDetalleComponent),
      },
      {
        path: 'sustentacion',
        canActivate: [roleGuard('SECRETARIA', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/sustentacion/components/sustentacion-bandeja.component').then((m) => m.SustentacionBandejaComponent),
      },
      {
        path: 'sustentacion/:tesisId',
        canActivate: [roleGuard('SECRETARIA', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/sustentacion/components/sustentacion-detalle.component').then((m) => m.SustentacionDetalleComponent),
      },

      // -----------------------------------------------------------------------
      // Proceso de tesis · Bandeja del asesor (docente)
      // -----------------------------------------------------------------------
      {
        path: 'dictamenes',
        canActivate: [roleGuard('SECRETARIA', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/dictamenes/components/dictamenes-report.component').then((m) => m.DictamenesReportComponent),
      },
      {
        path: 'dictamenes/:tesisId',
        canActivate: [roleGuard('SECRETARIA', 'ADMIN')],
        loadComponent: () =>
          import('@/app/views/dashboard/dictamenes/components/dictamen-elaborar.component').then((m) => m.DictamenElaborarComponent),
      },
      {
        path: 'solicitudes-asesoria',
        canActivate: [roleGuard('DOCENTE')],
        loadComponent: () =>
          import('@/app/views/dashboard/asesorias/components/bandeja-asesoria.component').then((m) => m.BandejaAsesoriaComponent),
      },

      // -----------------------------------------------------------------------
      // Mi perfil — cualquier usuario autenticado con persona
      // -----------------------------------------------------------------------
      {
        path: 'mi-perfil',
        loadComponent: () =>
          import('@/app/views/dashboard/mi-perfil/components/mi-perfil.component').then((m) => m.MiPerfilComponent),
      },

      // -----------------------------------------------------------------------
      // Student (Posgrado — Doctorado)
      // -----------------------------------------------------------------------
      {
        path: 'student',
        loadChildren: () =>
          import('@/app/views/dashboard/student/student.routes'),
      },

      // -----------------------------------------------------------------------
      // General Apps
      // -----------------------------------------------------------------------
      {
        path: 'academy',
        loadChildren: () => import('./modules/apps/academy/routes'),
      },
      {
        path: 'contacts',
        loadChildren: () => import('./modules/apps/contacts/routes'),
      },
      {
        path: 'file-manager',
        loadChildren: () => import('./modules/apps/file-manager/routes'),
      },
      {
        path: 'help-center',
        loadChildren: () => import('./modules/apps/help-center/routes'),
      },
      {
        path: 'notes',
        loadChildren: () => import('./modules/apps/notes/routes'),
      },
      {
        path: 'tasks',
        loadChildren: () => import('./modules/apps/tasks/routes'),
      },

      // -----------------------------------------------------------------------
      // Extras
      // -----------------------------------------------------------------------
      {
        path: 'settings',
        loadChildren: () => import('./modules/extras/settings/routes'),
      },
      {
        path: 'notifications',
        loadChildren: () => import('./modules/extras/notifications/routes'),
      },
      {
        path: 'error',
        loadChildren: () => import('./modules/extras/error/routes'),
      },

      // -----------------------------------------------------------------------
      // Documentation
      // -----------------------------------------------------------------------
      {
        path: 'documentation',
        loadChildren: () => import('./modules/documentation/routes'),
      },

      // 404
      {
        path: '404',
        pathMatch: 'full',
        loadComponent: () =>
          import('./modules/extras/error/features/error-404'),
      },

      // Catch all
      { path: '**', redirectTo: '404' },
    ],
  },
];

export default routes;
