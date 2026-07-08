import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class SolicitudAsesoriaService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.asesorias.solicitudes;

  /** Docente: bandeja por estado (default PENDIENTE). */
  bandeja$(estado = 'PENDIENTE', page = 0, size = 20): Observable<any> {
    const params = new HttpParams()
      .set('estado', estado)
      .set('page', String(page))
      .set('size', String(size));
    return this._http.get<any>(`${this.base}/bandeja`, { params });
  }

  /** Docente: acepta o rechaza. */
  responder$(id: string, decision: 'ACEPTAR' | 'RECHAZAR', motivo?: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${id}/responder`, { decision, motivo });
  }
}
