import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./sign-in.component').then((m) => m.SignInComponent),
  },
];

export default routes;
