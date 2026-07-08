import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EntityDataService } from '@/app/providers/utils/entity-data.service';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { environment } from '@/environments/environment';

@Injectable({ providedIn: 'root' })
export class ModuleService extends EntityDataService<any> {
  constructor() {
    super(inject(HttpClient), END_POINTS.setup.module);
  }

  getAllModulesSelectedByRoleIdAndParentModuleId$(
    roleId: string,
    parentModuleId: string
  ): Observable<any> {
    return this.httpClient.get<any>(
      `${environment.url}${this.endPoint}/modules-selected/roleId/${roleId}/parentModuleId/${parentModuleId}`
    );
  }

  getMenu$(): Observable<any> {
    return this.httpClient.get<any>(
      `${environment.url}${END_POINTS.setup.moduleMenu}`
    );
  }
}
