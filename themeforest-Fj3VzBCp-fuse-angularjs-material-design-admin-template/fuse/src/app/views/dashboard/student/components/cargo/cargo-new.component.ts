import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-cargo-new',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
  ],
  template: `
    <div class="form-dialog w-[480px]" [class.anim-shake]="shaking()">
      <div class="form-dialog__header">
        <div>
          <h2 class="form-dialog__title">Nuevo cargo</h2>
          <p class="form-dialog__subtitle">Completa la información del cargo laboral</p>
        </div>
        <button mat-icon-button class="!w-8 !h-8 !text-slate-400" (click)="dialogRef.close()">
          <mat-icon svgIcon="x" class="size-4" />
        </button>
      </div>

      <form [formGroup]="form" (ngSubmit)="save()" class="flex flex-col flex-1 overflow-hidden">
        <div class="form-dialog__body">

          <div class="form-section" style="animation-delay:40ms">
            <label class="form-label">Nombre <span class="form-required">*</span></label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <input matInput formControlName="nombre" placeholder="MEDICO ASISTENTE" />
            </mat-form-field>
            <p class="text-[11px] text-slate-400 mt-1">Se normalizará a mayúsculas</p>
          </div>

          <div class="form-section" style="animation-delay:80ms">
            <label class="form-label">Descripción</label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <textarea matInput formControlName="descripcion" rows="3" placeholder="Descripción del cargo..."></textarea>
            </mat-form-field>
          </div>

        </div>
        <div class="form-dialog__footer justify-end">
          <button type="button" mat-button class="!text-slate-500 !text-sm" (click)="dialogRef.close()">Cancelar</button>
          <button type="submit" mat-flat-button color="primary" class="!rounded-lg !px-5 !h-9 !text-sm !font-medium" [disabled]="form.invalid">
            Crear cargo
          </button>
        </div>
      </form>
    </div>
  `,
})
export class CargoNewComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  dialogRef   = inject(MatDialogRef<CargoNewComponent>);
  protected shaking = signal(false);
  form!: UntypedFormGroup;

  ngOnInit(): void {
    this.form = this._fb.group({
      nombre:      ['', [Validators.required, Validators.minLength(2), Validators.maxLength(200)]],
      descripcion: ['', Validators.maxLength(500)],
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); this._shake(); return; }
    const { nombre, descripcion } = this.form.value;
    this.dialogRef.close({ nombre: nombre.trim(), descripcion: descripcion || undefined });
  }

  private _shake(): void {
    this.shaking.set(false);
    requestAnimationFrame(() => requestAnimationFrame(() => {
      this.shaking.set(true);
      setTimeout(() => this.shaking.set(false), 500);
    }));
  }
}