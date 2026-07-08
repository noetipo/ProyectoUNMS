import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./confirmation-required.component').then(
        (m) => m.ConfirmationRequiredComponent
      ),
  },
];

export default routes;
