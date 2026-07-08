import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class DictamenService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.secretaria.dictamenes;

  bandeja$(estado?: string, facultadId?: string, programaId?: string, buscar?: string, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (estado) params = params.set('estado', estado);
    if (facultadId) params = params.set('facultadId', facultadId);
    if (programaId) params = params.set('programaId', programaId);
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(this.base, { params });
  }

  resumen$(): Observable<any> {
    return this._http.get<any>(`${this.base}/resumen`);
  }

  detalle$(tesisId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${tesisId}`);
  }

  /** Descarga un firmado del estudiante (tipo = solicitud | carta) como Blob. */
  descargarFirmado$(tesisId: string, tipo: 'solicitud' | 'carta'): Observable<Blob> {
    return this._http.get(`${this.base}/${tesisId}/documentos/${tipo}`, { responseType: 'blob' });
  }

  elaborar$(tesisId: string, expediente: string, fechaSolicitud: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}`, { expediente, fechaSolicitud });
  }

  /** Descarga el dictamen elaborado (pdf | docx). */
  documento$(tesisId: string, formato: 'pdf' | 'docx'): Observable<Blob> {
    return this._http.get(`${this.base}/${tesisId}/documento?formato=${formato}`, { responseType: 'blob' });
  }

  subirFirmado$(tesisId: string, file: File): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', file, file.name);
    return this._http.post<any>(`${this.base}/${tesisId}/firmado`, fd);
  }

  observar$(tesisId: string, motivo: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/observar`, { motivo });
  }
}
