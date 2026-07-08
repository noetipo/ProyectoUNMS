import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { OauthService } from '@/app/providers/services/oauth/oauth.service';

/**
 * Guard por rol: permite el acceso si el usuario autenticado tiene al menos
 * uno de los códigos de rol indicados. Si no, redirige a "mi perfil".
 */
export function roleGuard(...codes: string[]): CanActivateFn {
  return () => {
    const oauth = inject(OauthService);
    const router = inject(Router);

    const user = oauth.currentUser();
    if (!user) {
      return router.createUrlTree(['/sign-in']);
    }
    const roleCodes = user.roleCodes ?? [];
    const allowed = roleCodes.some((c) => codes.includes(c));
    return allowed ? true : router.createUrlTree(['/admin/mi-perfil']);
  };
}