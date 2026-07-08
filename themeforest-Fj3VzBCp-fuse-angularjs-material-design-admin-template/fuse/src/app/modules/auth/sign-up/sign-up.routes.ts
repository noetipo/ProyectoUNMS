import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./sign-up.component').then((m) => m.SignUpComponent),
  },
];

export default routes;
