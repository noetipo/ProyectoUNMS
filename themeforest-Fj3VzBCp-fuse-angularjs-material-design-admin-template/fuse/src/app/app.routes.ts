import { Route } from '@angular/router';

export const routes: Route[] = [
  // -----------------------------------------------------------------------
  // Auth nuevas páginas standalone (sin layout, sin sidebar)
  // -----------------------------------------------------------------------
  {
    path: 'sign-in',
    loadChildren: () => import('./modules/auth/sign-in/sign-in.routes'),
  },
  {
    path: 'sign-up',
    loadChildren: () => import('./modules/auth/sign-up/sign-up.routes'),
  },
  {
    path: 'forgot-password',
    loadChildren: () =>
      import('./modules/auth/forgot-password/forgot-password.routes'),
  },
  {
    path: 'recuperar',
    loadChildren: () =>
      import('./modules/auth/reset-password/reset-password.routes'),
  },
  {
    path: 'confirm-rest-password',
    loadChildren: () =>
      import('./modules/auth/confirm-reset-pasword/confirm-reset-password.routes'),
  },
  {
    path: 'confirmation-forgot',
    loadChildren: () =>
      import('./modules/auth/confirmation-forgot/confirmation-forgot.routes'),
  },
  {
    path: 'validate-email',
    loadChildren: () =>
      import('./modules/auth/validate-email/validate-email.routes'),
  },
  {
    path: 'confirmation-required',
    loadChildren: () =>
      import('./modules/auth/confirmation-required/confirmation-required.routes'),
  },

  // -----------------------------------------------------------------------
  // Website routes (Fuse original)
  // -----------------------------------------------------------------------
  {
    path: 'home',
    loadChildren: () => import('./domains/website/routes'),
  },

  // -----------------------------------------------------------------------
  // Auth (Fuse original — mantener compatibilidad)
  // -----------------------------------------------------------------------
  {
    path: 'auth',
    loadChildren: () => import('./domains/auth/routes'),
  },

  // -----------------------------------------------------------------------
  // Admin con sidebar Fuse (incluye /dashboard/setup bajo el mismo layout)
  // -----------------------------------------------------------------------
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'sign-in',
  },
  {
    path: 'admin',
    loadChildren: () => import('./domains/admin/routes'),
  },

  // -----------------------------------------------------------------------
  // Coming soon
  // -----------------------------------------------------------------------
  {
    path: 'coming-soon',
    loadChildren: () => import('./domains/coming-soon/routes'),
  },
];
