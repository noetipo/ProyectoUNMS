import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./validate-email.component').then((m) => m.ValidateEmailComponent),
  },
];

export default routes;
