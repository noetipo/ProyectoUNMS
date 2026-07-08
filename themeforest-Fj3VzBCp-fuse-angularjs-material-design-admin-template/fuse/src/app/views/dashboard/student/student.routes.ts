import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: 'programas',
    loadChildren: () =>
      import('./components/programa-doctorado/programa-doctorado-routers'),
  },
  {
    path: 'estudiantes',
    loadChildren: () =>
      import('./components/estudiante-list/estudiante-routers'),
  },
  {
    path: 'cargos',
    loadChildren: () =>
      import('./components/cargo/cargo-routers'),
  },
  {
    path: 'centros-laborales',
    loadChildren: () =>
      import('./components/centro-laboral/centro-laboral-routers'),
  },
  { path: '', redirectTo: 'programas', pathMatch: 'full' },
];

export default routes;
