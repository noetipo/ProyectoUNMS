import { HttpClient, HttpParams } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface CatalogoItemResponse {
  id: string;
  codigoSistema?: string;
  nombre: string;
  descripcion?: string;
  activo?: boolean;
}

export interface CatalogoRequest {
  nombre: string;
  descripcion?: string;
}

/**
 * CRUD genérico para catálogos simples (cargos / centros laborales): listado
 * paginado con búsqueda, alta, edición y borrado lógico. Las subclases solo
 * aportan la URL base.
 */
export abstract class CatalogoBaseService {
  protected _http = inject(HttpClient);
  protected abstract get base(): string;

  listar$(search: string | undefined, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (search) params = params.set('search', search);
    return this._http.get<any>(this.base, { params });
  }

  /** Lista completa (para selects del formulario de perfil). */
  listarTodos$(): Observable<any> {
    const params = new HttpParams().set('page', '0').set('size', '1000');
    return this._http.get<any>(this.base, { params });
  }

  obtener$(id: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${id}`);
  }

  crear$(body: CatalogoRequest): Observable<any> {
    return this._http.post<any>(this.base, body);
  }

  actualizar$(id: string, body: CatalogoRequest): Observable<any> {
    return this._http.put<any>(`${this.base}/${id}`, body);
  }

  eliminar$(id: string): Observable<any> {
    return this._http.delete<any>(`${this.base}/${id}`);
  }
}