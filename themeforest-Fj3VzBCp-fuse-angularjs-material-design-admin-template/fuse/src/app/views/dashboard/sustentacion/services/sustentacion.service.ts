import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

/** Secretaría · Etapa 8 (la última): dictamen, programación del acto, acta y cierre de la tesis. */
@Injectable({ providedIn: 'root' })
export class SustentacionService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.secretaria.sustentacion;

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

  programar$(tesisId: string, body: { fecha: string; hora?: string; lugar?: string; modalidad: string; enlace?: string }): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/programar`, body);
  }

  registrarActa$(tesisId: string, resultado: string, observacion: string, archivo: File): Observable<any> {
    const fd = new FormData();
    fd.append('resultado', resultado);
    fd.append('observacion', observacion ?? '');
    fd.append('archivo', archivo);
    return this._http.post<any>(`${this.base}/${tesisId}/acta`, fd);
  }
}
