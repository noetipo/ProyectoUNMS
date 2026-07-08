import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'auth-confirm-reset-password',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div class="w-full max-w-md bg-white rounded-xl shadow-md p-8 text-center">
        <div class="flex items-center justify-center w-20 h-20 rounded-full bg-green-100 mx-auto mb-6">
          <mat-icon class="text-green-600 text-4xl scale-150">check_circle</mat-icon>
        </div>
        <h2 class="text-2xl font-bold text-gray-900">¡Contraseña restablecida!</h2>
        <p class="mt-3 text-gray-500">
          Tu contraseña ha sido restablecida exitosamente. Ya puedes iniciar sesión con tu nueva contraseña.
        </p>
        <a routerLink="/sign-in" mat-flat-button color="primary" class="mt-8 inline-block w-full">
          Ir al inicio de sesión
        </a>
      </div>
    </div>
  `,
})
export class ConfirmResetPasswordComponent {}
