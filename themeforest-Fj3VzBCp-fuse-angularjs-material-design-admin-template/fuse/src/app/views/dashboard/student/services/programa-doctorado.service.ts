import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService, ApiResponse } from '@/app/core/services/api.service';
import {
  ProgramaDoctorado,
  ProgramaDoctoradoRequest,
  ProgramaDoctoradoUpdate,
  PaginatedResponse,
} from '../models/student.models';

@Injectable({ providedIn: 'root' })
export class ProgramaDoctoradoService extends ApiService {
  private readonly url = `${this.base}/api/v1/programas-doctorado`;

  // El endpoint paginado devuelve PaginatedResponse DIRECTO — NO usa unwrap()
  // Los items están en res.content
  getWithQuery$(params: any): Observable<PaginatedResponse<ProgramaDoctorado>> {
    return this.http.get<PaginatedResponse<ProgramaDoctorado>>(this.url, { params });
  }

  getById(id: string): Observable<ProgramaDoctorado> {
    return this.unwrap(
      this.http.get<ApiResponse<ProgramaDoctorado>>(`${this.url}/${id}`)
    );
  }

  create(dto: ProgramaDoctoradoRequest): Observable<ProgramaDoctorado> {
    return this.unwrap(
      this.http.post<ApiResponse<ProgramaDoctorado>>(this.url, dto)
    );
  }

  update(id: string, dto: ProgramaDoctoradoUpdate): Observable<ProgramaDoctorado> {
    return this.unwrap(
      this.http.put<ApiResponse<ProgramaDoctorado>>(`${this.url}/${id}`, dto)
    );
  }

  delete(id: string): Observable<void> {
    return this.unwrap(
      this.http.delete<ApiResponse<void>>(`${this.url}/${id}`)
    );
  }
}
