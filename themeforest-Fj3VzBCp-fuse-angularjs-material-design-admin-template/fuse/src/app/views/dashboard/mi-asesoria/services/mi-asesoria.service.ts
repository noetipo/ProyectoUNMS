import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';

@Injectable({ providedIn: 'root' })
export class MiAsesoriaService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.asesorias.miAsesoria;
  private readonly solicitudesUrl = environment.url + END_POINTS.asesorias.solicitudes;

  bandeja$(): Observable<any> {
    return this._http.get<any>(this.base);
  }

  /** Descarga un documento (SOLICITUD_ASESORIA | CARTA_ACEPTACION) en formato pdf o docx, como Blob. */
  descargar$(tipo: 'SOLICITUD_ASESORIA' | 'CARTA_ACEPTACION', formato: 'pdf' | 'docx' = 'pdf'): Observable<Blob> {
    return this._http.get(`${this.base}/documentos/${tipo}?formato=${formato}`, { responseType: 'blob' });
  }

  /** Reutiliza el POST de solicitudes: el estudiante solicita a un asesor sugerido. */
  solicitar$(docenteId: string, lineaInvestigacionId: string, tituloTentativo?: string): Observable<any> {
    return this._http.post<any>(this.solicitudesUrl, {
      docenteId,
      lineaInvestigacionId,
      tituloTentativo,
      tipo: 'ASESOR',
    });
  }

  cancelar$(solicitudId: string): Observable<any> {
    return this._http.post<any>(`${this.solicitudesUrl}/${solicitudId}/cancelar`, {});
  }

  /** El estudiante sube su documento firmado (tipo = solicitud | carta). */
  subirFirmado$(tipo: 'solicitud' | 'carta', file: File): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', file, file.name);
    return this._http.post<any>(`${this.base}/documentos/${tipo}/firmado`, fd);
  }

  /** Descarga el dictamen firmado emitido por la secretaría. */
  descargarDictamen$(): Observable<Blob> {
    return this._http.get(`${this.base}/dictamen`, { responseType: 'blob' });
  }
}
