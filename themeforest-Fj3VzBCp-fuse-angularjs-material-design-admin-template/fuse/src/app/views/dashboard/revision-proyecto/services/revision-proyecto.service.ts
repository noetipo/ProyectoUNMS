import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class RevisionProyectoService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.proyecto.asesorProyectos;

  bandeja$(estado?: string, buscar?: string, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (estado) params = params.set('estado', estado);
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(this.base, { params });
  }

  detalle$(tesisId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${tesisId}`);
  }

  observar$(tesisId: string, campo: string, texto: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/observar`, { campo, texto });
  }

  conforme$(tesisId: string, campo: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/campos/${campo}/conforme`, {});
  }

  cartaOpinion$(tesisId: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/carta-opinion`, {});
  }
}
