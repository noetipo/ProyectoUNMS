import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/estudiante-report.component').then((m) => m.EstudianteReportComponent),
  },
];

export default routes;