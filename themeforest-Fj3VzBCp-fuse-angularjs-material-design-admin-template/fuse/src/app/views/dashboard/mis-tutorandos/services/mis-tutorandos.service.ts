import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class MisTutorandosService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.tutor.misTutorandos;

  /** Cabecera: nombre del tutor, total y cupo máximo. */
  resumen$(): Observable<any> {
    return this._http.get<any>(`${this.base}/resumen`);
  }

  /** Tutorandos vigentes del tutor autenticado (búsqueda + paginación). */
  tutorandos$(buscar?: string, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(this.base, { params });
  }
}
