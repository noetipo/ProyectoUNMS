import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

/** Rúbricas oficiales de los revisores: una por enfoque, versionadas por año. */
@Injectable({ providedIn: 'root' })
export class RubricaOficialService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + 'api/secretaria/rubricas-oficiales';

  listar$(): Observable<any> {
    return this._http.get<any>(this.base);
  }

  /** Publica una versión nueva; la anterior pasa al historial. */
  publicar$(enfoque: string, archivo: File, version?: string): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', archivo);
    if (version) fd.append('version', version);
    return this._http.post<any>(`${this.base}/${enfoque}`, fd);
  }

  /** Word de una versión concreta (vista previa). */
  documento$(id: string): Observable<Blob> {
    return this._http.get(`${this.base}/${id}/documento`, { responseType: 'blob' });
  }
}
