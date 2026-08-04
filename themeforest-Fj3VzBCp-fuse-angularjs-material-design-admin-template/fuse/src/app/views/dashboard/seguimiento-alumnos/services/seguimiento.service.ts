import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

/** Tablero de seguimiento de doctorandos (secretaría/coordinación). Solo lectura. */
@Injectable({ providedIn: 'root' })
export class SeguimientoService {
  private _http = inject(HttpClient);
  private readonly baseUrl = environment.url + END_POINTS.seguimiento.base;

  tablero$(buscar?: string, programaId?: string): Observable<any> {
    let params = new HttpParams();
    if (buscar) params = params.set('buscar', buscar);
    if (programaId) params = params.set('programaId', programaId);
    return this._http.get<any>(this.baseUrl, { params });
  }

  /**
   * Excel con los filtros de pantalla. Se pide como blob por HttpClient (un `<a href>` no
   * lleva el JWT y devolvería 401).
   */
  excel$(filtros: {
    buscar?: string; etapa?: number | null; responsable?: string | null; programa?: string;
  }): Observable<Blob> {
    let params = new HttpParams();
    if (filtros.buscar) params = params.set('buscar', filtros.buscar);
    if (filtros.etapa !== null && filtros.etapa !== undefined) params = params.set('etapa', String(filtros.etapa));
    if (filtros.responsable) params = params.set('responsable', filtros.responsable);
    if (filtros.programa) params = params.set('programa', filtros.programa);
    return this._http.get(`${this.baseUrl}/excel`, { params, responseType: 'blob' });
  }
}
