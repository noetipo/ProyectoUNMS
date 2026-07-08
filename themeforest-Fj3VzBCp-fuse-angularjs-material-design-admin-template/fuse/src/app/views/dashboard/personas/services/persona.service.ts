import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';
import {
  AgregarPerfilDocenteRequest,
  CrearPersonaRequest,
} from '../models/persona.model';

export interface PersonaFiltros {
  search?: string;
  tipoPerfil?: string;
  activo?: boolean;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class PersonaService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.personas.base;
  private readonly programasUrl = environment.url + END_POINTS.personas.programasPosgrado;

  /** POST /api/personas */
  registrar$(request: CrearPersonaRequest): Observable<any> {
    return this._http.post<any>(this.base, request);
  }

  /** POST /api/personas (multipart: datos + historiales + documentos) */
  registrarMultipart$(formData: FormData): Observable<any> {
    return this._http.post<any>(this.base, formData);
  }

  /** GET /api/personas/{id} */
  obtener$(id: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${id}`);
  }

  /** POST /api/personas/{id}/perfiles/docente */
  agregarPerfilDocente$(id: string, request: AgregarPerfilDocenteRequest): Observable<any> {
    return this._http.post<any>(`${this.base}/${id}/perfiles/docente`, request);
  }

  /** GET /api/personas (listado paginado con filtros) */
  listar$(filtros: PersonaFiltros): Observable<any> {
    let params = new HttpParams()
      .set('page', String(filtros.page ?? 0))
      .set('size', String(filtros.size ?? 20));
    if (filtros.search) params = params.set('search', filtros.search);
    if (filtros.tipoPerfil) params = params.set('tipoPerfil', filtros.tipoPerfil);
    if (filtros.activo !== undefined && filtros.activo !== null) {
      params = params.set('activo', String(filtros.activo));
    }
    return this._http.get<any>(this.base, { params });
  }

  /** PUT /api/personas/{id} */
  actualizar$(id: string, body: any): Observable<any> {
    return this._http.put<any>(`${this.base}/${id}`, body);
  }

  /** DELETE /api/personas/{id} (borrado lógico) */
  eliminar$(id: string): Observable<any> {
    return this._http.delete<any>(`${this.base}/${id}`);
  }

  /** GET /api/programas-posgrado (opcional: filtrar por facultad para la cascada). */
  listProgramas$(facultadId?: string): Observable<any> {
    let params = new HttpParams();
    if (facultadId) { params = params.set('facultadId', facultadId); }
    return this._http.get<any>(this.programasUrl, { params });
  }

  /** GET /api/facultades (combo de facultad para la cascada Facultad → Programa). */
  listFacultades$(): Observable<any> {
    const params = new HttpParams().set('page', '0').set('size', '1000');
    return this._http.get<any>(environment.url + END_POINTS.configuracion.facultades, { params });
  }

  /** GET /api/personas/{id}/perfil-completo (historiales + documentos) */
  obtenerPerfilCompleto$(id: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${id}/perfil-completo`);
  }

  /** POST /api/personas/{id}/perfil-completo (multipart) */
  guardarPerfilCompleto$(id: string, formData: FormData): Observable<any> {
    return this._http.post<any>(`${this.base}/${id}/perfil-completo`, formData);
  }

  /** GET /api/personas/{id}/documentos/{documentoId} → archivo (blob) */
  descargarDocumento$(id: string, documentoId: string): Observable<Blob> {
    return this._http.get(`${this.base}/${id}/documentos/${documentoId}`, { responseType: 'blob' });
  }
}