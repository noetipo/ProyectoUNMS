import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EntityDataService } from '@/app/providers/utils/entity-data.service';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { environment } from '@/environments/environment';

@Injectable({ providedIn: 'root' })
export class RoleService extends EntityDataService<any> {
  constructor() {
    super(inject(HttpClient), END_POINTS.setup.role);
  }

  /**
   * POST /api/roles/module
   * Body: { roleId, parentModuleId, modules: [{ id, selected, ... }] }
   */
  postAssigmentModulesToRole$(data: any): Observable<any> {
    return this.httpClient.post<any>(
      `${environment.url}${END_POINTS.setup.roleModule}`,
      data
    );
  }
}
