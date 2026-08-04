import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class SugerenciaAsesorService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.tutorSugerencias.base;

  listar$(estudianteId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/estudiantes/${estudianteId}/sugerencias`);
  }

  /** Docentes candidatos a asesor, filtrados por la línea del tema del tutorando. */
  candidatos$(estudianteId: string): Observable<any> {
    return this._http.get<any>(`${this.base}/estudiantes/${estudianteId}/asesores-candidatos`);
  }

  /** @param tipo puesto para el que se sugiere: ASESOR (principal, uno solo) o COASESOR. */
  sugerir$(estudianteId: string, asesorDocenteId: string,
           tipo: 'ASESOR' | 'COASESOR' = 'ASESOR', nota?: string): Observable<any> {
    return this._http.post<any>(`${this.base}/estudiantes/${estudianteId}/sugerencias`,
      { asesorDocenteId, tipo, nota });
  }

  quitar$(sugerenciaId: string): Observable<any> {
    return this._http.delete<any>(`${this.base}/sugerencias/${sugerenciaId}`);
  }
}
