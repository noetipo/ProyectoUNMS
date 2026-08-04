import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'project',
  },
  {
    // El panel real del sistema: datos vivos y "lo importante primero" según el rol.
    // (El de la plantilla, con cifras escritas a mano, quedó en 'project-demo'.)
    path: 'project',
    loadComponent: () =>
      import('@/app/views/dashboard/panel/components/panel.component').then((m) => m.PanelComponent),
  },
  {
    path: 'project-demo',
    loadComponent: () => import('./features/project/project'),
  },
  {
    path: 'analytics',
    loadComponent: () => import('./features/analytics/analytics'),
  },
  {
    path: 'finance',
    loadComponent: () => import('./features/finance/finance'),
  },
];

export default routes;
