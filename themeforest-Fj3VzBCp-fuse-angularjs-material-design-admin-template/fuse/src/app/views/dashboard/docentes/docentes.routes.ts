import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/docente-report.component').then((m) => m.DocenteReportComponent),
  },
];

export default routes;