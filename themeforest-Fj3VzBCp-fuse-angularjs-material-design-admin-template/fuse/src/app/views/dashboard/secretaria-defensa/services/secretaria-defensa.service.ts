import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class SecretariaDefensaService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.secretaria.defensa;

  bandeja$(buscar?: string, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(this.base, { params });
  }

  recibir$(tesisId: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/recibir`, {});
  }

  /** Proyectos con revisores designados que requieren (o ya tienen) la rúbrica oficial. */
  bandejaRubricas$(buscar?: string): Observable<any> {
    let params = new HttpParams();
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(`${this.base}/rubricas`, { params });
  }

  /** Enciende/suspende la evaluación con la rúbrica oficial del sistema (no sube nada). */
  habilitarRubrica$(tesisId: string, habilitar: boolean): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/rubrica/habilitar?habilitar=${habilitar}`, {});
  }

  /** Word de la rúbrica oficial vigente del enfoque (para la vista previa). */
  rubricaOficialRaw$(enfoque: string): Observable<Blob> {
    return this._http.get(`${environment.url}api/secretaria/rubricas-oficiales/vigente/${enfoque}/documento`,
      { responseType: 'blob' });
  }

  /** Legado: adjunta un Word propio del expediente (caso excepcional). */
  subirRubrica$(tesisId: string, archivo: File): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', archivo);
    return this._http.post<any>(`${this.base}/${tesisId}/rubrica`, fd);
  }

  /** Vista previa (texto) de la rúbrica subida, para confirmar que es el documento correcto. */
  previewRubrica$(tesisId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${tesisId}/rubrica/preview`);
  }

  /** El Word (.docx) crudo de la rúbrica, para renderizarlo en el navegador (vista previa fiel). */
  descargarRubricaRaw$(tesisId: string): Observable<Blob> {
    return this._http.get(`${this.base}/${tesisId}/rubrica/raw`, { responseType: 'blob' });
  }

  // ── Programación de la defensa (la realiza la Secretaría) ──
  defensa$(tesisId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${tesisId}/defensa`);
  }
  programarDefensa$(tesisId: string, body: any): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/defensa`, body);
  }
}
