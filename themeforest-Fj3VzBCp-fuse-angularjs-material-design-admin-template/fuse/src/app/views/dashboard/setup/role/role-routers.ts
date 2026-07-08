import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/role-container.component').then(
        (m) => m.RoleContainerComponent
      ),
  },
];

export default routes;
