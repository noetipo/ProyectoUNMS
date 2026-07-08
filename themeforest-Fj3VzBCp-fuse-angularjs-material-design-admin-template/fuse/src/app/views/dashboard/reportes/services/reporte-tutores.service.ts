import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class ReporteTutoresService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.reportes.tutores;
  private readonly sinTutorUrl = environment.url + END_POINTS.reportes.estudiantesSinTutor;

  resumen$(facultadId?: string, programaId?: string): Observable<any> {
    let params = new HttpParams();
    if (facultadId) params = params.set('facultadId', facultadId);
    if (programaId) params = params.set('programaId', programaId);
    return this._http.get<any>(`${this.base}/resumen`, { params });
  }

  tutores$(buscar?: string, facultadId?: string, programaId?: string, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (buscar) params = params.set('buscar', buscar);
    if (facultadId) params = params.set('facultadId', facultadId);
    if (programaId) params = params.set('programaId', programaId);
    return this._http.get<any>(this.base, { params });
  }

  /** Lazy: estudiantes de un tutor (se pide al expandir). */
  estudiantesDeTutor$(id: string, page = 0, size = 100): Observable<any> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this._http.get<any>(`${this.base}/${id}/estudiantes`, { params });
  }

  sinTutor$(facultadId?: string, programaId?: string, buscar?: string, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (facultadId) params = params.set('facultadId', facultadId);
    if (programaId) params = params.set('programaId', programaId);
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(this.sinTutorUrl, { params });
  }

  /** Export server-side (todos los datos del filtro). Devuelve el archivo como Blob. */
  export$(formato: 'pdf' | 'xlsx', buscar?: string, facultadId?: string, programaId?: string): Observable<Blob> {
    let params = new HttpParams().set('formato', formato);
    if (buscar) params = params.set('buscar', buscar);
    if (facultadId) params = params.set('facultadId', facultadId);
    if (programaId) params = params.set('programaId', programaId);
    return this._http.get(`${this.base}/export`, { params, responseType: 'blob' });
  }
}
