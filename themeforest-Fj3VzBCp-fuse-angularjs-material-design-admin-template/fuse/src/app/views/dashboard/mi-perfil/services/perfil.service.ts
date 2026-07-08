import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

export interface MiPerfilUpdate {
  emailPersonal?: string;
  celular?: string;
  orcid?: string;
}

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.personas.miPerfil;

  obtener$(): Observable<any> {
    return this._http.get<any>(this.base);
  }

  actualizar$(body: MiPerfilUpdate): Observable<any> {
    return this._http.put<any>(this.base, body);
  }

  /** GET /api/mi-perfil/completo (historiales + documentos del token) */
  obtenerCompleto$(): Observable<any> {
    return this._http.get<any>(`${this.base}/completo`);
  }

  /** PUT /api/mi-perfil/completo (multipart) */
  guardarCompleto$(formData: FormData): Observable<any> {
    return this._http.put<any>(`${this.base}/completo`, formData);
  }

  /** GET /api/mi-perfil/documentos/{documentoId} → archivo (blob) */
  descargarDocumento$(documentoId: string): Observable<Blob> {
    return this._http.get(`${this.base}/documentos/${documentoId}`, { responseType: 'blob' });
  }
}