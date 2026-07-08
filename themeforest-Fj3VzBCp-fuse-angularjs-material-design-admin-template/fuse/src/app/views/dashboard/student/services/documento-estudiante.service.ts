import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, ApiResponse } from '@/app/core/services/api.service';
import { DocumentoEstudiante, TipoDocumento } from '../models/student.models';

@Injectable({ providedIn: 'root' })
export class DocumentoEstudianteService extends ApiService {
  private endpoint(estudianteId: string): string {
    return `${this.base}/api/v1/estudiantes/${estudianteId}/documentos`;
  }

  getByEstudiante(estudianteId: string): Observable<DocumentoEstudiante[]> {
    return this.unwrap(
      this.http.get<ApiResponse<DocumentoEstudiante[]>>(this.endpoint(estudianteId))
    );
  }

  getById(estudianteId: string, id: string): Observable<DocumentoEstudiante> {
    return this.unwrap(
      this.http.get<ApiResponse<DocumentoEstudiante>>(`${this.endpoint(estudianteId)}/${id}`)
    );
  }

  /**
   * POST /api/v1/estudiantes/{estudianteId}/documentos — multipart/form-data
   * Si ya existe un documento del mismo tipo se reemplaza automáticamente.
   */
  upload(estudianteId: string, tipo: TipoDocumento, file: File): Observable<DocumentoEstudiante> {
    const form = new FormData();
    form.append('tipoDocumento', tipo);
    form.append('file', file, file.name);
    return this.unwrap(
      this.http.post<ApiResponse<DocumentoEstudiante>>(this.endpoint(estudianteId), form)
    );
  }

  download(estudianteId: string, id: string): Observable<Blob> {
    return this.http.get(
      `${this.endpoint(estudianteId)}/${id}/download`,
      { responseType: 'blob' }
    );
  }

  delete(estudianteId: string, id: string): Observable<void> {
    return this.unwrap(
      this.http.delete<ApiResponse<void>>(`${this.endpoint(estudianteId)}/${id}`)
    );
  }
}
