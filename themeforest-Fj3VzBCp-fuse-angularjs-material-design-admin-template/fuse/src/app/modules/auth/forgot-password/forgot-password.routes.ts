import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./forgot-password.component').then((m) => m.ForgotPasswordComponent),
  },
];

export default routes;
