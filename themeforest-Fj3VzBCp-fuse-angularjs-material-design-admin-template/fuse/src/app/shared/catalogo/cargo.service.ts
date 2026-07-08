import { Injectable } from '@angular/core';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { CatalogoBaseService } from './catalogo-base.service';

@Injectable({ providedIn: 'root' })
export class CargoService extends CatalogoBaseService {
  protected get base(): string {
    return environment.url + END_POINTS.catalogos.cargos;
  }
}