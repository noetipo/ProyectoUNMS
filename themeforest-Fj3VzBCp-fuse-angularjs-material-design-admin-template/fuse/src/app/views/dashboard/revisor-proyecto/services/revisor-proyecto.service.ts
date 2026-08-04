import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class RevisorProyectoService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.revisor.proyectos;

  bandeja$(): Observable<any> {
    return this._http.get<any>(this.base);
  }

  detalle$(tesisId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${tesisId}`);
  }

  evaluar$(tesisId: string, niveles: Record<string, string>, observaciones: Record<string, string>, comentario: string, conforme: boolean): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/evaluar`, { niveles, observaciones, comentario, conforme });
  }

  /** PDF del proyecto para el visor (blob con auth). */
  proyectoPdf$(tesisId: string): Observable<Blob> {
    return this._http.get(`${this.base}/${tesisId}/proyecto-pdf`, { responseType: 'blob' });
  }

  /** Descarga la rúbrica oficial (Word) subida por Secretaría — con auth (blob). */
  descargarRubrica$(tesisId: string): Observable<Blob> {
    return this._http.get(`${this.base}/${tesisId}/rubrica`, { responseType: 'blob' });
  }

  /** Descarga la rúbrica ya llenada (Excel) con los puntajes del revisor. */
  descargarRubricaLlenada$(tesisId: string): Observable<Blob> {
    return this._http.get(`${this.base}/${tesisId}/rubrica-llenada`, { responseType: 'blob' });
  }

  observarItem$(tesisId: string, campo: string, texto: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/observar-item`, { campo, texto });
  }

  conformidadItem$(tesisId: string, campo: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/items/${campo}/conformidad`, {});
  }
}
