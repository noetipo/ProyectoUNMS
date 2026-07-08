import { inject, Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

/**
 * Notificaciones tipo "toast" reutilizables en toda la app.
 *
 * Envuelve MatSnackBar (ya incluido en Angular Material, sin dependencias
 * nuevas). El overlay vive fuera del router-outlet, por lo que el mensaje
 * sobrevive a la navegación entre rutas (p. ej. mostrar éxito tras redirigir).
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private _snackBar = inject(MatSnackBar);

  private readonly _base: MatSnackBarConfig = {
    horizontalPosition: 'end',
    verticalPosition: 'bottom',
  };

  success(message: string, duration = 4000): void {
    this._snackBar.open(message, 'OK', {
      ...this._base,
      duration,
      panelClass: ['toast', 'toast--success'],
    });
  }

  error(message: string, duration = 6000): void {
    this._snackBar.open(message, 'Cerrar', {
      ...this._base,
      duration,
      panelClass: ['toast', 'toast--error'],
    });
  }
}
