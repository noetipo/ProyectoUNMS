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
        path: 'registro-tema',
        canActivate: [roleGuard('COORDINADOR', 'COORD_PROG', 'COORD_SEC', 'ADMIN', 'SECRETARIA')],
        loadComponent: () =>
          import('@/app/views/dashboard/registro-tema/components/registro-tema-report.component').then((m) => m.RegistroTemaReportComponent),
      },
      {
        path: 'mi-asesoria',
        canActivate: [roleGuard('ESTUDIANTE')],
        loadComponent: () =>
          import('@/app/views/dashboard/mi-asesoria/components/mi-asesoria.component').then((m) => m.MiAsesoriaComponent),
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
