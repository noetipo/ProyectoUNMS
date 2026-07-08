import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, ApiResponse } from '@/app/core/services/api.service';
import { CentroLaboral, CentroLaboralRequest, CentroLaboralUpdate, PaginatedResponse } from '../models/student.models';

@Injectable({ providedIn: 'root' })
export class CentroLaboralService extends ApiService {
  private readonly url = `${this.base}/api/v1/centros-laborales`;

  getWithQuery$(params: any): Observable<PaginatedResponse<CentroLaboral>> {
    return this.http.get<PaginatedResponse<CentroLaboral>>(this.url, { params });
  }

  getById(id: string): Observable<CentroLaboral> {
    return this.unwrap(this.http.get<ApiResponse<CentroLaboral>>(`${this.url}/${id}`));
  }

  create(dto: CentroLaboralRequest): Observable<CentroLaboral> {
    return this.unwrap(this.http.post<ApiResponse<CentroLaboral>>(this.url, dto));
  }

  update(id: string, dto: CentroLaboralUpdate): Observable<CentroLaboral> {
    return this.unwrap(this.http.put<ApiResponse<CentroLaboral>>(`${this.url}/${id}`, dto));
  }

  delete(id: string): Observable<void> {
    return this.unwrap(this.http.delete<ApiResponse<void>>(`${this.url}/${id}`));
  }
}