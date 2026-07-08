import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./reset-password.component').then((m) => m.ResetPasswordComponent),
  },
];

export default routes;
