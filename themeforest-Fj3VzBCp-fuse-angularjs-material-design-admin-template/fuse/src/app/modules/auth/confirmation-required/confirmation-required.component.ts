import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'auth-confirmation-required',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div class="w-full max-w-md bg-white rounded-xl shadow-md p-8 text-center">
        <div class="flex items-center justify-center w-20 h-20 rounded-full bg-yellow-100 mx-auto mb-6">
          <mat-icon class="text-yellow-600 text-4xl scale-150">pending</mat-icon>
        </div>
        <h2 class="text-2xl font-bold text-gray-900">¡Registro exitoso!</h2>
        <p class="mt-3 text-gray-500">
          Tu cuenta ha sido creada. Por favor revisa tu correo electrónico para
          confirmar tu cuenta antes de iniciar sesión.
        </p>
        <a routerLink="/sign-in" mat-stroked-button class="mt-8 inline-block w-full">
          Ir al inicio de sesión
        </a>
      </div>
    </div>
  `,
})
export class ConfirmationRequiredComponent {}
