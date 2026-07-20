import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class SecretariaDefensaService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.secretaria.defensa;

  bandeja$(buscar?: string, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (buscar) params = params.set('buscar', buscar);
    return this._http.get<any>(this.base, { params });
  }

  recibir$(tesisId: string): Observable<any> {
    return this._http.post<any>(`${this.base}/${tesisId}/recibir`, {});
  }
}
