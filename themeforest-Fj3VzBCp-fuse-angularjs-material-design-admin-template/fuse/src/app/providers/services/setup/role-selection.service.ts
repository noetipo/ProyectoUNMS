import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface SelectedRole {
  id: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class RoleSelectionService {
  private _role$ = new BehaviorSubject<SelectedRole | null>(null);

  /** Observable que emite el rol activo cada vez que cambia. */
  readonly role$ = this._role$.asObservable();

  get current(): SelectedRole | null {
    return this._role$.value;
  }

  select(role: SelectedRole): void {
    this._role$.next(role);
  }

  clear(): void {
    this._role$.next(null);
  }
}