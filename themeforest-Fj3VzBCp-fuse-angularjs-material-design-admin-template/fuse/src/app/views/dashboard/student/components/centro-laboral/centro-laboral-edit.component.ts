import { Component, inject, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { CentroLaboral } from '../../models/student.models';

@Component({
  selector: 'app-centro-laboral-edit',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
  ],
  template: `
    <div class="form-dialog w-[480px]" [class.anim-shake]="shaking()">
      <div class="form-dialog__header">
        <div>
          <h2 class="form-dialog__title">Editar centro laboral</h2>
          <p class="form-dialog__subtitle">Modificando: <span class="font-medium text-slate-600">{{ data.nombre }}</span></p>
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
              <input matInput formControlName="nombre" />
            </mat-form-field>
          </div>

          <div class="form-section" style="animation-delay:80ms">
            <label class="form-label">Descripción</label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <textarea matInput formControlName="descripcion" rows="3"></textarea>
            </mat-form-field>
          </div>

        </div>
        <div class="form-dialog__footer justify-between">
          <span class="text-xs text-slate-400">ID: <code class="font-mono">{{ data.id }}</code></span>
          <div class="flex items-center gap-2">
            <button type="button" mat-button class="!text-slate-500 !text-sm" (click)="dialogRef.close()">Cancelar</button>
            <button type="submit" mat-flat-button color="primary" class="!rounded-lg !px-5 !h-9 !text-sm !font-medium" [disabled]="form.invalid">
              Guardar cambios
            </button>
          </div>
        </div>
      </form>
    </div>
  `,
})
export class CentroLaboralEditComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  dialogRef   = inject(MatDialogRef<CentroLaboralEditComponent>);
  protected shaking = signal(false);
  form!: UntypedFormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: CentroLaboral) {}

  ngOnInit(): void {
    this.form = this._fb.group({
      nombre:      [this.data.nombre ?? '', [Validators.required, Validators.minLength(2), Validators.maxLength(200)]],
      descripcion: [this.data.descripcion ?? '', Validators.maxLength(500)],
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); this._shake(); return; }
    this.dialogRef.close({ ...this.form.value, id: this.data.id });
  }

  private _shake(): void {
    this.shaking.set(false);
    requestAnimationFrame(() => requestAnimationFrame(() => {
      this.shaking.set(true);
      setTimeout(() => this.shaking.set(false), 500);
    }));
  }
}