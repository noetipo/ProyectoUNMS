import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  error: string | null;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  protected readonly http = inject(HttpClient);
  protected readonly base = environment.url.replace(/\/$/, '');

  protected unwrap<T>(obs: Observable<ApiResponse<T>>): Observable<T> {
    return obs.pipe(
      catchError((err: HttpErrorResponse) => {
        const msg = (err.error as any)?.message ?? err.message ?? 'Error desconocido';
        return throwError(() => new Error(msg));
      }),
      map((r: ApiResponse<T>) => {
        if (!r.success) throw new Error(r.message ?? 'Operación fallida');
        return r.data as T;
      })
    );
  }
}
