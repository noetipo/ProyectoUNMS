import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService, ApiResponse } from '@/app/core/services/api.service';
import {
  Estudiante,
  EstudianteRequest,
  EstudianteUpdate,
  PaginatedResponse,
} from '../models/student.models';

@Injectable({ providedIn: 'root' })
export class EstudianteService extends ApiService {
  private readonly url = `${this.base}/api/v1/estudiantes`;

  // El endpoint paginado devuelve PaginatedResponse DIRECTO — NO usa unwrap()
  // Los items están en res.content
  getWithQuery$(params: any): Observable<PaginatedResponse<Estudiante>> {
    return this.http.get<PaginatedResponse<Estudiante>>(this.url, { params });
  }

  getById(id: string): Observable<Estudiante> {
    return this.unwrap(
      this.http.get<ApiResponse<Estudiante>>(`${this.url}/${id}`)
    );
  }

  getByDni(dni: string): Observable<Estudiante> {
    return this.unwrap(
      this.http.get<ApiResponse<Estudiante>>(`${this.url}/dni/${dni}`)
    );
  }

  getByCodMatricula(cod: string): Observable<Estudiante> {
    return this.unwrap(
      this.http.get<ApiResponse<Estudiante>>(`${this.url}/matricula/${cod}`)
    );
  }

  /**
   * POST /api/v1/estudiantes — multipart/form-data
   * El campo "estudiante" debe enviarse como Blob JSON, no como string plano.
   */
  create(
    dto: EstudianteRequest,
    dniFile?: File | null,
    partidaFile?: File | null
  ): Observable<Estudiante> {
    const form = new FormData();
    form.append(
      'estudiante',
      new Blob([JSON.stringify(dto)], { type: 'application/json' }),
      ''
    );
    if (dniFile)     form.append('dniFile',     dniFile,     dniFile.name);
    if (partidaFile) form.append('partidaFile', partidaFile, partidaFile.name);
    return this.unwrap(
      this.http.post<ApiResponse<Estudiante>>(this.url, form)
    );
  }

  update(id: string, dto: EstudianteUpdate): Observable<Estudiante> {
    return this.unwrap(
      this.http.put<ApiResponse<Estudiante>>(`${this.url}/${id}`, dto)
    );
  }

  delete(id: string): Observable<void> {
    return this.unwrap(
      this.http.delete<ApiResponse<void>>(`${this.url}/${id}`)
    );
  }

  exportarExcel(params: Record<string, string>): Observable<Blob> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([k, v]) => { if (v) httpParams = httpParams.set(k, v); });
    return this.http.get(`${this.url}/exportar-excel`, {
      params: httpParams,
      responseType: 'blob',
    });
  }
}
