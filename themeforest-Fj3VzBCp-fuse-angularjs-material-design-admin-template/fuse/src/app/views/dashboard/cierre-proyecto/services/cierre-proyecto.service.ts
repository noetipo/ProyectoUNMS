import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

/** Secretaría · Etapa 5, tramo final: rúbricas de la defensa, dictamen de aprobación y archivo. */
@Injectable({ providedIn: 'root' })
export class CierreProyectoService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.secretaria.cierre;

  bandeja$(buscar?: string): Observable<any> {
    let params = new HttpParams();
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(this.base, { params });
  }

  estado$(tesisId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${tesisId}`);
  }

  // ── Paso 1 · rúbricas y resultado ──
  recepcionarRubrica$(tesisId: string, docenteId: string, archivo: File, puntaje?: number | null): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', archivo);
    if (puntaje !== null && puntaje !== undefined) fd.append('puntaje', String(puntaje));
    return this._http.post<any>(`${this.base}/${tesisId}/rubrica/${docenteId}`, fd);
  }

  rubricaRaw$(tesisId: string, docenteId: string): Observable<Blob> {
    return this._http.get(`${this.base}/${tesisId}/rubrica/${docenteId}/raw`, { responseType: 'blob' });
  }

  registrarResultado$(tesisId: string, body: any): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/resultado`, body);
  }

  // ── Paso 2 · dictamen de aprobación ──
  elaborarDictamen$(tesisId: string, body: any): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/dictamen`, body);
  }

  documentoDictamen$(tesisId: string, formato: 'pdf' | 'docx'): Observable<Blob> {
    return this._http.get(`${this.base}/${tesisId}/dictamen/documento?formato=${formato}`, { responseType: 'blob' });
  }

  subirDictamenFirmado$(tesisId: string, archivo: File): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', archivo);
    return this._http.post<any>(`${this.base}/${tesisId}/dictamen/firmado`, fd);
  }

  // ── Paso 3 · archivo del expediente ──
  archivarProyecto$(tesisId: string, archivo: File): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', archivo);
    return this._http.post<any>(`${this.base}/${tesisId}/proyecto-final`, fd);
  }

  archivarDelEstudiante$(tesisId: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/proyecto-final/del-estudiante`, {});
  }
}
