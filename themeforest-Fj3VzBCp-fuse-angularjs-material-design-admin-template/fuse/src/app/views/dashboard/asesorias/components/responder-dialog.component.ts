import { Component, Inject, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

export interface ResponderDialogData {
  estudiante: string;
  titulo?: string;
}

/** Diálogo para rechazar una solicitud: exige un motivo. */
@Component({
  selector: 'app-responder-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule,
  ],
  template: `
    <div class="form-dialog w-[480px]">
      <div class="form-dialog__header">
        <h2 class="form-dialog__title">Rechazar solicitud</h2>
        <button mat-icon-button (click)="dialogRef.close()">
          <mat-icon svgIcon="x" class="size-4" />
        </button>
      </div>
      <form [formGroup]="form" (ngSubmit)="confirmar()">
        <div class="form-dialog__body">
          <p class="text-sm text-slate-500 mb-3">
            {{ data.estudiante }}@if (data.titulo) { — <span class="italic">{{ data.titulo }}</span> }
          </p>
          <label class="form-label">Motivo del rechazo <span class="form-required">*</span></label>
          <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
            <textarea matInput formControlName="motivo" rows="4"
              placeholder="Explica brevemente el motivo..."></textarea>
          </mat-form-field>
          @if (form.get('motivo')!.touched && form.get('motivo')!.invalid) {
            <p class="text-xs text-rose-500 mt-1">El motivo es obligatorio (mínimo 5 caracteres).</p>
          }
        </div>
        <div class="form-dialog__footer justify-end">
          <button type="button" mat-button class="!text-slate-500 !text-sm" (click)="dialogRef.close()">Cancelar</button>
          <button type="submit" mat-flat-button color="warn"
            class="!rounded-lg !px-5 !h-9 !text-sm !font-medium">Rechazar</button>
        </div>
      </form>
    </div>
  `,
})
export class ResponderDialogComponent implements OnInit {
  private _fb = inject(FormBuilder);
  dialogRef = inject(MatDialogRef<ResponderDialogComponent>);

  form!: FormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ResponderDialogData) {}

  ngOnInit(): void {
    this.form = this._fb.group({
      motivo: ['', [Validators.required, Validators.minLength(5)]],
    });
  }

  confirmar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close({ motivo: this.form.value.motivo.trim() });
  }
}
