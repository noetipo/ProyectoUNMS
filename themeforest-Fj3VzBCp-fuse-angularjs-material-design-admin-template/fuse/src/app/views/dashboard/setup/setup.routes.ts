import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: 'role',
    loadChildren: () => import('./role/role-routers'),
  },
  {
    path: 'users',
    loadChildren: () => import('./user/users-routers'),
  },
  {
    path: 'user',
    loadChildren: () => import('./user/users-routers'),
  },
  {
    path: 'parent-module',
    loadChildren: () => import('./parentModule/parent-module-routers'),
  },
  {
    path: 'module',
    loadChildren: () => import('./module/module-routers'),
  },
];

export default routes;