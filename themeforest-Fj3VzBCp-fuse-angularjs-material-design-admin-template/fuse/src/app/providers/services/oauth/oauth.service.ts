import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { AuthUser, LoginResponse } from '@/app/modules/auth/sign-in/models/login.model';
import { LocalStorage } from '@/app/core/local-storage/local-storage';

@Injectable({ providedIn: 'root' })
export class OauthService {
  private _httpClient = inject(HttpClient);
  private _storage = inject(LocalStorage);
  private _authenticated = false;

  readonly currentUser = signal<AuthUser | null>(this._loadUserFromStorage());

  // ── Token storage ──────────────────────────────────────────────────────────

  setAccessToken(token: string): void {
    this._storage.setItem('accessToken', token);
  }

  setRefreshToken(token: string): void {
    this._storage.setItem('refreshToken', token);
  }

  getAccessToken(): string | null {
    return this._storage.getItem('accessToken');
  }

  getRefreshToken(): string | null {
    return this._storage.getItem('refreshToken');
  }

  // ── Auth flow ──────────────────────────────────────────────────────────────

  authenticate(credentials: { username: string; password: string }): Observable<LoginResponse> {
    if (this._authenticated) {
      return throwError(() => new Error('User is already authenticated.'));
    }

    return this._httpClient
      .post<LoginResponse>(`${environment.url}${END_POINTS.oauth.login}`, credentials)
      .pipe(
        tap((response) => this.setSession(response)),
        shareReplay()
      );
  }

  setSession(response: LoginResponse): void {
    const data = response?.data;
    if (!data) return;

    const accessToken = data.accessToken ?? data.refreshToken;
    this.setAccessToken(accessToken);
    this.setRefreshToken(data.refreshToken);

    if (data.user) {
      this._storage.setItem('currentUser', JSON.stringify(data.user));
      this.currentUser.set(data.user);
    }

    this._authenticated = true;
  }

  signOut(): void {
    const refreshToken = this.getRefreshToken();

    if (refreshToken) {
      this._httpClient
        .post(`${environment.url}${END_POINTS.oauth.logout}`, { refreshToken })
        .subscribe({ error: () => {} });
    }

    this._storage.removeItem('accessToken');
    this._storage.removeItem('refreshToken');
    this._storage.removeItem('currentUser');
    this.currentUser.set(null);
    this._authenticated = false;
  }

  check(): Observable<boolean> {
    if (this._authenticated) {
      return of(true);
    }

    const accessToken = this.getAccessToken();
    if (!accessToken) {
      return of(false);
    }

    this._authenticated = true;
    return of(true);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private _loadUserFromStorage(): AuthUser | null {
    try {
      const raw = this._storage.getItem('currentUser');
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  }
}
