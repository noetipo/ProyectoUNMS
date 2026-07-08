import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EntityDataService } from '@/app/providers/utils/entity-data.service';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { environment } from '@/environments/environment';

@Injectable({ providedIn: 'root' })
export class UsersService extends EntityDataService<any> {
  constructor() {
    super(inject(HttpClient), END_POINTS.setup.users);
  }

  /**
   * Actualizar estado del usuario vía PUT /api/v1/users/{id}
   */
  updateStateUserId$(idUser: string): Observable<any> {
    return this.httpClient.put<any>(
      `${environment.url}${END_POINTS.setup.users}/${idUser}`,
      { active: true }
    );
  }
}