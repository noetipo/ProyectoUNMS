import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/users-container.component').then(
        (m) => m.UsersContainerComponent
      ),
  },
];

export default routes;
