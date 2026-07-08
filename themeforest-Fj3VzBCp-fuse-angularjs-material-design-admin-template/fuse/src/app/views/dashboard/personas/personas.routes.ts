import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/persona-report.component').then((m) => m.PersonaReportComponent),
  },
  {
    path: 'nueva',
    loadComponent: () =>
      import('./components/persona-form.component').then((m) => m.PersonaFormComponent),
  },
  {
    // Editar reutiliza EXACTAMENTE el mismo formulario que crear (modo edición por :id).
    path: ':id',
    loadComponent: () =>
      import('./components/persona-form.component').then((m) => m.PersonaFormComponent),
  },
];

export default routes;