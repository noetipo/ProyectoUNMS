import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, ApiResponse } from '@/app/core/services/api.service';
import { Cargo, CargoRequest, CargoUpdate, PaginatedResponse } from '../models/student.models';

@Injectable({ providedIn: 'root' })
export class CargoService extends ApiService {
  private readonly url = `${this.base}/api/v1/cargos`;

  getWithQuery$(params: any): Observable<PaginatedResponse<Cargo>> {
    return this.http.get<PaginatedResponse<Cargo>>(this.url, { params });
  }

  getById(id: string): Observable<Cargo> {
    return this.unwrap(this.http.get<ApiResponse<Cargo>>(`${this.url}/${id}`));
  }

  create(dto: CargoRequest): Observable<Cargo> {
    return this.unwrap(this.http.post<ApiResponse<Cargo>>(this.url, dto));
  }

  update(id: string, dto: CargoUpdate): Observable<Cargo> {
    return this.unwrap(this.http.put<ApiResponse<Cargo>>(`${this.url}/${id}`, dto));
  }

  delete(id: string): Observable<void> {
    return this.unwrap(this.http.delete<ApiResponse<void>>(`${this.url}/${id}`));
  }
}