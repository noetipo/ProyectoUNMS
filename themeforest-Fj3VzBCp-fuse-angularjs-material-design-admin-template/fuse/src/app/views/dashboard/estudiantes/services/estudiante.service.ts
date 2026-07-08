import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

export interface EstudianteFiltros {
  search?: string;
  facultadId?: string;
  programaId?: string;
  condicion?: string;
  nivel?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class EstudianteService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.personas.estudiantes;

  list$(f: EstudianteFiltros): Observable<any> {
    let params = new HttpParams()
      .set('page', String(f.page ?? 0))
      .set('size', String(f.size ?? 20));
    if (f.search) params = params.set('search', f.search);
    if (f.facultadId) params = params.set('facultadId', f.facultadId);
    if (f.programaId) params = params.set('programaId', f.programaId);
    if (f.condicion) params = params.set('condicion', f.condicion);
    if (f.nivel) params = params.set('nivel', f.nivel);
    return this._http.get<any>(this.base, { params });
  }

  resumen$(): Observable<any> {
    return this._http.get<any>(`${this.base}/resumen`);
  }
}