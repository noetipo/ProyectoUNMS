import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { ActividadItem, ObjetivoItem, PartidaItem } from '../models/proyecto.model';

@Injectable({ providedIn: 'root' })
export class MiProyectoService {
  private _http = inject(HttpClient);
  private readonly base = environment.url + END_POINTS.proyecto.miProyecto;

  editor$(): Observable<any> {
    return this._http.get<any>(this.base);
  }

  guardarCampo$(campo: string, valor: string): Observable<any> {
    return this._http.put<any>(`${this.base}/campos/${campo}`, { valor });
  }

  setEnfoque$(enfoque: string): Observable<any> {
    return this._http.put<any>(`${this.base}/enfoque`, { enfoque });
  }

  desbloquearEnfoque$(): Observable<any> {
    return this._http.post<any>(`${this.base}/enfoque/desbloquear`, {});
  }

  setFinanciamiento$(financiamiento: string): Observable<any> {
    return this._http.put<any>(`${this.base}/financiamiento`, { financiamiento });
  }

  // ── Objetivos ──
  agregarObjetivo$(o: ObjetivoItem): Observable<any> {
    return this._http.post<any>(`${this.base}/objetivos`, o);
  }
  actualizarObjetivo$(id: string, o: ObjetivoItem): Observable<any> {
    return this._http.put<any>(`${this.base}/objetivos/${id}`, o);
  }
  eliminarObjetivo$(id: string): Observable<any> {
    return this._http.delete<any>(`${this.base}/objetivos/${id}`);
  }

  // ── Actividades ──
  agregarActividad$(a: ActividadItem): Observable<any> {
    return this._http.post<any>(`${this.base}/actividades`, a);
  }
  actualizarActividad$(id: string, a: ActividadItem): Observable<any> {
    return this._http.put<any>(`${this.base}/actividades/${id}`, a);
  }
  eliminarActividad$(id: string): Observable<any> {
    return this._http.delete<any>(`${this.base}/actividades/${id}`);
  }

  // ── Presupuesto ──
  agregarPartida$(p: PartidaItem): Observable<any> {
    return this._http.post<any>(`${this.base}/partidas`, p);
  }
  actualizarPartida$(id: string, p: PartidaItem): Observable<any> {
    return this._http.put<any>(`${this.base}/partidas/${id}`, p);
  }
  eliminarPartida$(id: string): Observable<any> {
    return this._http.delete<any>(`${this.base}/partidas/${id}`);
  }

  // ── Hitos ──
  marcarListo$(): Observable<any> {
    return this._http.post<any>(`${this.base}/listo-revision`, {});
  }
  reenviarRevision$(): Observable<any> {
    return this._http.post<any>(`${this.base}/reenviar-revision`, {});
  }
  publicarPlan$(): Observable<any> {
    return this._http.post<any>(`${this.base}/plan/publicar`, {});
  }
  corregir$(campo: string, respuesta: string): Observable<any> {
    return this._http.post<any>(`${this.base}/observaciones/${campo}/corregir`, { respuesta });
  }
  responderRevisor$(revisorId: string, respuesta: string): Observable<any> {
    return this._http.post<any>(`${this.base}/revisores/${revisorId}/responder`, { respuesta });
  }
  solicitarJuradoInformante$(): Observable<any> {
    return this._http.post<any>(`${this.base}/jurado-informante/solicitar`, {});
  }
  responderJuradoInforme$(revisorId: string, respuesta: string): Observable<any> {
    return this._http.post<any>(`${this.base}/jurado-informante/${revisorId}/responder`, { respuesta });
  }
  subirTurnitin$(file: File, porcentaje: number): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', file, file.name);
    fd.append('porcentaje', String(porcentaje));
    return this._http.post<any>(`${this.base}/turnitin`, fd);
  }
  subirProyectoFinal$(file: File): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', file, file.name);
    return this._http.post<any>(`${this.base}/proyecto-final`, fd);
  }
  subirInformeFinal$(file: File): Observable<any> {
    const fd = new FormData();
    fd.append('archivo', file, file.name);
    return this._http.post<any>(`${this.base}/informe-final`, fd);
  }
  /** Ver/descargar un documento propio subido (turnitin | proyecto-final). */
  descargarDocumento$(tipo: 'turnitin' | 'proyecto-final'): Observable<Blob> {
    return this._http.get(`${this.base}/documentos/${tipo}`, { responseType: 'blob' });
  }
  solicitarAprobacion$(): Observable<any> {
    return this._http.post<any>(`${this.base}/expediente/solicitar-aprobacion`, {});
  }

  // ── Referencias ──
  /** Autocompletar una referencia desde su DOI (CrossRef). */
  buscarDoi$(doi: string): Observable<any> {
    return this._http.get<any>(`${this.base}/referencias/por-doi`, { params: { doi } });
  }
  /** Buscar candidatos de referencia por título/autor (sin DOI). */
  buscarTitulo$(q: string): Observable<any> {
    return this._http.get<any>(`${this.base}/referencias/buscar`, { params: { q } });
  }
  /** Rellenar una referencia pegando su BibTeX. */
  parsearBibtex$(bibtex: string): Observable<any> {
    return this._http.post<any>(`${this.base}/referencias/bibtex`, bibtex, { headers: { 'Content-Type': 'text/plain' } });
  }
  agregarReferencia$(r: any): Observable<any> {
    return this._http.post<any>(`${this.base}/referencias`, r);
  }
  actualizarReferencia$(id: string, r: any): Observable<any> {
    return this._http.put<any>(`${this.base}/referencias/${id}`, r);
  }
  eliminarReferencia$(id: string): Observable<any> {
    return this._http.delete<any>(`${this.base}/referencias/${id}`);
  }
  setEstiloCita$(estilo: string): Observable<any> {
    return this._http.put<any>(`${this.base}/estilo-cita/${estilo}`, {});
  }
  desbloquearEstiloCita$(): Observable<any> {
    return this._http.post<any>(`${this.base}/estilo-cita/desbloquear`, {});
  }
  /** Genera el documento del proyecto (formato = pdf | docx) y lo devuelve como Blob. */
  generarDocumento$(formato: 'pdf' | 'docx'): Observable<Blob> {
    return this._http.get(`${this.base}/documento/${formato}`, { responseType: 'blob' });
  }

  // ── DEMO / pruebas ──
  seedDemo$(): Observable<any> {
    return this._http.post<any>(`${this.base}/demo/seed`, {});
  }
  resetDemo$(): Observable<any> {
    return this._http.post<any>(`${this.base}/demo/reset`, {});
  }
}
