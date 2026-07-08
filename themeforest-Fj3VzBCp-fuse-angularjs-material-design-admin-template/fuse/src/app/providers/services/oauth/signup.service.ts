import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class SignupService {
  private _httpClient = inject(HttpClient);

  /** Registro de usuario → POST /api/v1/auth/register */
  signUp(data: any): Observable<any> {
    return this._httpClient.post<any>(
      `${environment.url}${END_POINTS.oauth.signup}`,
      data
    );
  }

  /** Recuperar contraseña → POST forgot-password?email=... (TODO: endpoint pendiente en backend) */
  forgotPassword(email: string): Observable<any> {
    const params = new HttpParams().set('email', email);
    return this._httpClient.post(
      `${environment.url}${END_POINTS.oauth.forgotPassword}`,
      null,
      { params, responseType: 'text' }
    );
  }

  /** Restablecer contraseña → POST reset-password?token=...&newPassword=... (TODO: endpoint pendiente en backend) */
  resetPassword(token: string, newPassword: string): Observable<any> {
    const params = new HttpParams().set('token', token).set('newPassword', newPassword);
    return this._httpClient.post(
      `${environment.url}${END_POINTS.oauth.resetPassword}`,
      null,
      { params, responseType: 'text' }
    );
  }

  /** Verificar email → POST verify-email/{token} (TODO: endpoint pendiente en backend) */
  verifyEmail(token: string): Observable<any> {
    return this._httpClient.post(
      `${environment.url}${END_POINTS.oauth.verifyEmail}/${token}`,
      null,
      { responseType: 'text' }
    );
  }
}
