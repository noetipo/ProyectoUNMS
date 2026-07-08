import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/module-container.component').then(
        (m) => m.ModuleContainerComponent
      ),
  },
];

export default routes;
