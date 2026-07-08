import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { RegistrarTemaBody } from '../models/registro-tema.model';

@Injectable({ providedIn: 'root' })
export class RegistroTemaService {
  private _http = inject(HttpClient);
  private readonly reporteUrl = environment.url + END_POINTS.coordinador.estudiantesTema;
  private readonly temaBase = environment.url + END_POINTS.coordinador.tema;
  private readonly miTemaUrl = environment.url + END_POINTS.coordinador.miTema;

  resumen$(facultadId?: string, programaId?: string): Observable<any> {
    let params = new HttpParams();
    if (facultadId) params = params.set('facultadId', facultadId);
    if (programaId) params = params.set('programaId', programaId);
    return this._http.get<any>(`${this.reporteUrl}/resumen`, { params });
  }

  estudiantes$(facultadId?: string, programaId?: string, conTema?: boolean | null, buscar?: string, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (facultadId) params = params.set('facultadId', facultadId);
    if (programaId) params = params.set('programaId', programaId);
    if (conTema !== null && conTema !== undefined) params = params.set('conTema', String(conTema));
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(this.reporteUrl, { params });
  }

  registrar$(estudianteId: string, body: RegistrarTemaBody): Observable<any> {
    return this._http.post<any>(`${this.temaBase}/${estudianteId}/tema`, body);
  }

  editar$(estudianteId: string, body: RegistrarTemaBody): Observable<any> {
    return this._http.put<any>(`${this.temaBase}/${estudianteId}/tema`, body);
  }

  /** Tema del estudiante autenticado (para la sección "Tema de investigación" del perfil). */
  miTema$(): Observable<any> {
    return this._http.get<any>(this.miTemaUrl);
  }
}
