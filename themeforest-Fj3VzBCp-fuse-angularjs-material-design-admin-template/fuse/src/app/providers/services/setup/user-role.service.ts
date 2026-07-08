import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EntityDataService } from '@/app/providers/utils/entity-data.service';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { environment } from '@/environments/environment';

@Injectable({ providedIn: 'root' })
export class UserRoleService extends EntityDataService<any> {
  constructor() {
    super(inject(HttpClient), END_POINTS.setup.userRole);
  }

  /**
   * GET /api/user-roles/user/{userId}
   * Retorna todos los roles marcando cuáles tiene asignados el usuario.
   */
  getAllRolesSelectedByUserId$(idUser: string): Observable<any> {
    return this.httpClient.get<any>(
      `${environment.url}${this.endPoint}/user/${idUser}`
    );
  }

  /**
   * POST /api/user-roles
   * Body: { userId, roleIds: string[] }
   */
  assignRolesToUser$(userId: string, roleIds: string[]): Observable<any> {
    return this.httpClient.post<any>(
      `${environment.url}${this.endPoint}`,
      { userId, roleIds }
    );
  }
}