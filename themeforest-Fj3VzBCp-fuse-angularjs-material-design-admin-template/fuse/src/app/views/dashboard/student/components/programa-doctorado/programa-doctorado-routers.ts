import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./programa-doctorado-container.component').then(
        (m) => m.ProgramaDoctoradoContainerComponent
      ),
  },
];

export default routes;
