import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

/** Panel de inicio: lo importante primero según el rol + cifras del programa. */
@Injectable({ providedIn: 'root' })
export class PanelService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + 'api/panel';

  panel$(): Observable<any> {
    return this._http.get<any>(this.base);
  }

  /** El mismo panel en Excel (por HttpClient: un <a href> no manda el JWT). */
  reporte$(): Observable<Blob> {
    return this._http.get(`${this.base}/reporte`, { responseType: 'blob' });
  }
}
