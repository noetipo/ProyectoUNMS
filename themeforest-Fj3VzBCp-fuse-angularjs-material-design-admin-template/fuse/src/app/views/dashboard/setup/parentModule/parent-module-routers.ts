import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/parent-module-container.component').then(
        (m) => m.ParentModuleContainerComponent
      ),
  },
];

export default routes;
