import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./estudiante-list.component').then(
        (m) => m.EstudianteListComponent
      ),
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('../estudiante-create/estudiante-create.component').then(
        (m) => m.EstudianteCreateComponent
      ),
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('../estudiante-edit/estudiante-edit.component').then(
        (m) => m.EstudianteEditComponent
      ),
  },
  {
    path: ':id/constancia',
    loadComponent: () =>
      import('../estudiante-constancia/estudiante-constancia.component').then(
        (m) => m.EstudianteConstanciaComponent
      ),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('../estudiante-detail/estudiante-detail.component').then(
        (m) => m.EstudianteDetailComponent
      ),
  },
];

export default routes;
