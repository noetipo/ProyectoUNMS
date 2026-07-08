import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class TutoriaService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.tutorias.base;

  /** Autocomplete de tutores (docentes PROF_TUTOR) con cupo. */
  buscarTutores$(buscar?: string, page = 0, size = 10): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(`${this.base}/tutores`, { params });
  }

  /** Estudiantes para la asignación en bloque (programa + estado tutor + texto). */
  estudiantes$(facultadId?: string, programaId?: string, conTutor?: string, buscar?: string, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (facultadId) params = params.set('facultadId', facultadId);
    if (programaId) params = params.set('programaId', programaId);
    if (conTutor) params = params.set('conTutor', conTutor);
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(`${this.base}/estudiantes`, { params });
  }

  tutorVigente$(estudianteId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/estudiante/${estudianteId}/vigente`);
  }

  historial$(estudianteId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/estudiante/${estudianteId}/historial`);
  }

  asignar$(estudianteId: string, tutorId: string, motivoCambio?: string): Observable<any> {
    return this._http.post<any>(`${this.base}/asignar`, { estudianteId, tutorId, motivoCambio });
  }

  asignarEnBloque$(tutorId: string, estudianteIds: string[], motivoCambio?: string): Observable<any> {
    return this._http.post<any>(`${this.base}/asignar-en-bloque`, { tutorId, estudianteIds, motivoCambio });
  }

  /** Quitar un estudiante de un tutor (finaliza su tutoría vigente). */
  finalizar$(tutorId: string, estudianteId: string, motivo?: string): Observable<any> {
    let params = new HttpParams();
    if (motivo) params = params.set('motivo', motivo);
    return this._http.delete<any>(`${this.base}/${tutorId}/estudiantes/${estudianteId}`, { params });
  }
}
