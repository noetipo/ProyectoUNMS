import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class JuradoInformeService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.juradoInforme.base;

  bandeja$(): Observable<any> {
    return this._http.get<any>(this.base);
  }

  detalle$(tesisId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${tesisId}`);
  }

  evaluar$(tesisId: string, puntaje: number, comentario: string, conforme: boolean): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/evaluar`, { puntaje, comentario, conforme });
  }
}
