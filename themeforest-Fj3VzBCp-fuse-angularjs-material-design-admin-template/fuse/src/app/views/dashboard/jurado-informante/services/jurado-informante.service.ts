import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

/** Secretaría · Etapa 7: trámite del Jurado Informante y Dictamen de Expedito. */
@Injectable({ providedIn: 'root' })
export class JuradoInformanteService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.secretaria.juradoInformante;

  bandeja$(buscar?: string): Observable<any> {
    let params = new HttpParams();
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(this.base, { params });
  }

  detalle$(tesisId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${tesisId}`);
  }

  recepcionar$(tesisId: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/recepcionar`, {});
  }

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

  archivar$(tesisId: string, archivo: File): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', archivo);
    return this._http.post<any>(`${this.base}/${tesisId}/archivar`, fd);
  }

  elaborarExpedito$(tesisId: string, body: any): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/expedito`, body);
  }

  documentoExpedito$(tesisId: string, formato: 'pdf' | 'docx'): Observable<Blob> {
    return this._http.get(`${this.base}/${tesisId}/expedito/documento?formato=${formato}`, { responseType: 'blob' });
  }

  subirExpeditoFirmado$(tesisId: string, archivo: File): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', archivo);
    return this._http.post<any>(`${this.base}/${tesisId}/expedito/firmado`, fd);
  }
}
