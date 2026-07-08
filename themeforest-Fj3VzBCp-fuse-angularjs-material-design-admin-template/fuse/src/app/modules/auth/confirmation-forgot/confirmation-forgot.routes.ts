import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./confirmation-forgot.component').then(
        (m) => m.ConfirmationForgotComponent
      ),
  },
];

export default routes;
