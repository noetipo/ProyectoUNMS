import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

export interface DocenteFiltros {
  search?: string;
  grado?: string;
  categoria?: string;
  condicion?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class DocenteService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.personas.docentes;

  list$(f: DocenteFiltros): Observable<any> {
    let params = new HttpParams()
      .set('page', String(f.page ?? 0))
      .set('size', String(f.size ?? 20));
    if (f.search) params = params.set('search', f.search);
    if (f.grado) params = params.set('grado', f.grado);
    if (f.categoria) params = params.set('categoria', f.categoria);
    if (f.condicion) params = params.set('condicion', f.condicion);
    return this._http.get<any>(this.base, { params });
  }

  resumen$(): Observable<any> {
    return this._http.get<any>(`${this.base}/resumen`);
  }
}