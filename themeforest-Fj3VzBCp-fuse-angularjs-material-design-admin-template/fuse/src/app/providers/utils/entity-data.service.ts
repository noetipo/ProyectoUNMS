import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export class EntityDataService<T> {
  constructor(
    protected httpClient: HttpClient,
    protected endPoint: string
  ) {}

  getAll$(): Observable<T[]> {
    return this.httpClient.get<T[]>(`${environment.url}${this.endPoint}`);
  }

  getById$(id: string): Observable<T> {
    return this.httpClient.get<T>(`${environment.url}${this.endPoint}/${id}`);
  }

  add$(data: any): Observable<T> {
    return this.httpClient.post<T>(`${environment.url}${this.endPoint}`, data);
  }

  update$(id: string, data: any): Observable<T> {
    return this.httpClient.put<T>(
      `${environment.url}${this.endPoint}/${id}`,
      data
    );
  }

  delete$(id: string): Observable<any> {
    return this.httpClient.delete<any>(
      `${environment.url}${this.endPoint}/${id}`
    );
  }

  getWithQuery$(params: any): Observable<any> {
    return this.httpClient.get<any>(`${environment.url}${this.endPoint}`, {
      params,
    });
  }

  getAllNotPaginate(): Observable<T[]> {
    return this.httpClient.get<T[]>(
      `${environment.url}${this.endPoint}/all`
    );
  }
}
