import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./centro-laboral-container.component').then(m => m.CentroLaboralContainerComponent),
  },
];

export default routes;