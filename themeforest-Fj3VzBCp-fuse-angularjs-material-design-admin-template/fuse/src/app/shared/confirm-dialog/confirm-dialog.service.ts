import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { ConfirmDialogComponent, ConfirmDialogData } from './confirm-dialog.component';

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private _dialog = inject(MatDialog);

  async confirmDelete(config?: ConfirmDialogData): Promise<void> {
    return this._open({ type: 'delete', ...config });
  }

  async confirmEdit(config?: ConfirmDialogData): Promise<void> {
    return this._open({ type: 'edit', ...config });
  }

  async confirmSave(config?: ConfirmDialogData): Promise<void> {
    return this._open({ type: 'save', ...config });
  }

  private async _open(data: ConfirmDialogData): Promise<void> {
    const ref = this._dialog.open(ConfirmDialogComponent, {
      width:                  '360px',
      panelClass:             ['dialog-anim'],
      disableClose:           true,
      enterAnimationDuration: '0ms',
      exitAnimationDuration:  '220ms',
      data,
    });
    const result = await firstValueFrom(ref.afterClosed());
    if (!result) throw new Error('Cancelled');
  }
}
