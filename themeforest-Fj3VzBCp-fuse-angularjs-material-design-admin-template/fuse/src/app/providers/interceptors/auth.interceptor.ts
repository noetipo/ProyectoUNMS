import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LocalStorage } from '@/app/core/local-storage/local-storage';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const storage = inject(LocalStorage);
  const accessToken = storage.getItem('accessToken');

  if (accessToken) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
    return next(cloned);
  }

  return next(req);
};
