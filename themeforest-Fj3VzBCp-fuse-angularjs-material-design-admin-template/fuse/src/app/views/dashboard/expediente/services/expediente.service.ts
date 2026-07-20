import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class ExpedienteService {
  private _http = inject(HttpClient);
  private readonly mio = environment.url + END_POINTS.expediente.mio;
  private readonly base = environment.url + END_POINTS.expediente.base;

  /** Expediente del estudiante autenticado. */
  miExpediente$(): Observable<any> {
    return this._http.get<any>(this.mio);
  }

  /** Expediente de una tesis (secretaría / admin / coordinador). */
  porTesis$(tesisId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/${tesisId}`);
  }
}
