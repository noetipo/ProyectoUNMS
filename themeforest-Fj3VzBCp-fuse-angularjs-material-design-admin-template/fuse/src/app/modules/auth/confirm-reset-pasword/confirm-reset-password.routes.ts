import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./confirm-reset-password.component').then(
        (m) => m.ConfirmResetPasswordComponent
      ),
  },
];

export default routes;
