import { Component, inject, Inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { ProgramaDoctorado } from '../../models/student.models';

@Component({
  selector: 'app-programa-doctorado-edit',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatIconModule,
  ],
  template: `
    <div class="form-dialog w-[480px]" [class.anim-shake]="shaking()">

      <div class="form-dialog__header">
        <div>
          <h2 class="form-dialog__title">Editar programa</h2>
          <p class="form-dialog__subtitle">
            Modificando: <span class="font-medium text-slate-600">{{ programa.nombre }}</span>
          </p>
        </div>
        <button mat-icon-button class="!w-8 !h-8 !text-slate-400 hover:!text-slate-600"
          (click)="dialogRef.close()">
          <mat-icon svgIcon="x" class="size-4" />
        </button>
      </div>

      <form [formGroup]="form" (ngSubmit)="save()" class="flex flex-col flex-1 overflow-hidden">
        <div class="form-dialog__body">

          <div class="form-section" style="animation-delay: 40ms">
            <label class="form-label">
              Nombre del programa <span class="form-required">*</span>
            </label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <input matInput formControlName="nombre"
                placeholder="DOCTORADO EN EDUCACION"
                class="!uppercase" />
            </mat-form-field>
            <p class="text-[11px] text-slate-400 mt-1">
              El servidor lo normalizará a mayúsculas automáticamente.
            </p>
          </div>

          <div class="form-section" style="animation-delay: 80ms">
            <label class="form-label">Descripción</label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <textarea matInput formControlName="descripcion" rows="3"
                placeholder="Descripción opcional del programa…"></textarea>
            </mat-form-field>
          </div>

          <div class="form-section" style="animation-delay: 120ms">
            <label class="form-label">Estado</label>
            <mat-slide-toggle formControlName="active" color="primary" class="!text-sm !text-slate-600">
              {{ form.get('active')?.value ? 'Activo' : 'Inactivo' }}
            </mat-slide-toggle>
          </div>

        </div>

        <div class="form-dialog__footer justify-between">
          <span class="text-xs text-slate-400">
            ID: <code class="font-mono">{{ programa.id }}</code>
          </span>
          <div class="flex items-center gap-2">
            <button type="button" mat-button class="!text-slate-500 !text-sm"
              (click)="dialogRef.close()">
              Cancelar
            </button>
            <button type="submit" mat-flat-button color="primary"
              class="!rounded-lg !px-5 !h-9 !text-sm !font-medium"
              [disabled]="form.invalid">
              Guardar cambios
            </button>
          </div>
        </div>
      </form>

    </div>
  `,
})
export class ProgramaDoctoradoEditComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  dialogRef   = inject(MatDialogRef<ProgramaDoctoradoEditComponent>);

  form!: UntypedFormGroup;
  protected shaking = signal(false);

  constructor(@Inject(MAT_DIALOG_DATA) public programa: ProgramaDoctorado) {}

  ngOnInit(): void {
    this.form = this._fb.group({
      nombre:      [this.programa.nombre      ?? '', [Validators.required, Validators.minLength(2), Validators.maxLength(200)]],
      descripcion: [this.programa.descripcion ?? '',  Validators.maxLength(500)],
      active:      [this.programa.active      ?? true],
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this._shake();
      return;
    }
    const dirty = this._getDirtyValues();
    // Si el usuario no cambió nada, cierra sin llamar al backend
    if (Object.keys(dirty).length === 0) {
      this.dialogRef.close();
      return;
    }
    this.dialogRef.close(dirty);
  }

  private _getDirtyValues(): Record<string, unknown> {
    const out: Record<string, unknown> = {};
    Object.entries(this.form.controls).forEach(([key, ctrl]) => {
      if (ctrl.dirty) out[key] = ctrl.value === '' ? null : ctrl.value;
    });
    return out;
  }

  private _shake(): void {
    this.shaking.set(false);
    requestAnimationFrame(() => requestAnimationFrame(() => {
      this.shaking.set(true);
      setTimeout(() => this.shaking.set(false), 500);
    }));
  }
}
