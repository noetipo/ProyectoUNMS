import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./cargo-container.component').then(m => m.CargoContainerComponent),
  },
];

export default routes;