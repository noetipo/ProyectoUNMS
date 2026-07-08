import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { UsersService } from '@/app/providers/services/setup/users.service';

@Component({
  selector: 'app-user-new',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <div class="form-dialog w-[500px]" [class.anim-shake]="shaking()">

      <div class="form-dialog__header">
        <div>
          <h2 class="form-dialog__title">Nuevo usuario</h2>
          <p class="form-dialog__subtitle">Completa la información del usuario</p>
        </div>
        <button mat-icon-button class="!w-8 !h-8 !text-slate-400 hover:!text-slate-600"
          [disabled]="saving()" (click)="dialogRef.close()">
          <mat-icon svgIcon="x" class="size-4" />
        </button>
      </div>

      <form [formGroup]="form" (ngSubmit)="save()" class="flex flex-col flex-1 overflow-hidden">
        <div class="form-dialog__body">

          <div class="form-section" style="animation-delay: 40ms">
            <label class="form-label">Nombre de usuario <span class="form-required">*</span></label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <input matInput formControlName="username" placeholder="ej. jperez" autocomplete="off" />
            </mat-form-field>
          </div>

          <div class="form-section" style="animation-delay: 80ms">
            <label class="form-label">Correo electrónico <span class="form-required">*</span></label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <mat-icon matPrefix svgIcon="mail" class="!text-slate-400 size-4 mr-1" />
              <input matInput type="email" formControlName="email" placeholder="usuario@dominio.com" />
            </mat-form-field>
          </div>

          <div class="form-section grid grid-cols-2 gap-3" style="animation-delay: 120ms">
            <div>
              <label class="form-label">Nombres <span class="form-required">*</span></label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <input matInput formControlName="firstName" placeholder="Juan" />
              </mat-form-field>
            </div>
            <div>
              <label class="form-label">Apellidos <span class="form-required">*</span></label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <input matInput formControlName="lastName" placeholder="Pérez" />
              </mat-form-field>
            </div>
          </div>

          <div class="form-section" style="animation-delay: 160ms">
            <label class="form-label">Contraseña <span class="form-required">*</span></label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <mat-icon matPrefix svgIcon="lock" class="!text-slate-400 size-4 mr-1" />
              <input matInput type="password" formControlName="password" placeholder="••••••••" autocomplete="new-password" />
            </mat-form-field>
          </div>

        </div>

        <div class="form-dialog__footer justify-end">
          <button type="button" mat-button class="!text-slate-500 !text-sm"
            [disabled]="saving()" (click)="dialogRef.close()">
            Cancelar
          </button>
          <button type="submit" mat-flat-button color="primary"
            class="!rounded-lg !px-5 !h-9 !text-sm !font-medium"
            [disabled]="form.invalid || saving()">
            {{ saving() ? 'Creando...' : 'Crear usuario' }}
          </button>
        </div>
      </form>

    </div>
  `,
})
export class UserNewComponent implements OnInit {
  private _fb           = inject(UntypedFormBuilder);
  private _usersService = inject(UsersService);
  dialogRef = inject(MatDialogRef<UserNewComponent>);

  form!: UntypedFormGroup;
  protected saving  = signal(false);
  protected shaking = signal(false);

  ngOnInit(): void {
    this.form = this._fb.group({
      username:  ['', Validators.required],
      email:     ['', [Validators.required, Validators.email]],
      firstName: ['', Validators.required],
      lastName:  ['', Validators.required],
      password:  ['', Validators.required],
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this._shake();
      return;
    }
    this.saving.set(true);
    this._usersService.add$(this.form.value).subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogRef.close(true);
      },
      error: () => this.saving.set(false),
    });
  }

  private _shake(): void {
    this.shaking.set(false);
    requestAnimationFrame(() => requestAnimationFrame(() => {
      this.shaking.set(true);
      setTimeout(() => this.shaking.set(false), 500);
    }));
  }
}
