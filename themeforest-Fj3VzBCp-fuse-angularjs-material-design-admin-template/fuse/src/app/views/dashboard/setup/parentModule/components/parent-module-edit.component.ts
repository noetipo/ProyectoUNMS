import { Component, inject, Inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { ParentModule } from '../models/parent-module';

@Component({
  selector: 'app-parent-module-edit',
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
          <h2 class="form-dialog__title">Editar módulo padre</h2>
          <p class="form-dialog__subtitle">
            Modificando: <span class="font-medium text-slate-600">{{ parentModule.title }}</span>
          </p>
        </div>
        <button mat-icon-button class="!w-8 !h-8 !text-slate-400 hover:!text-slate-600" (click)="dialogRef.close()">
          <mat-icon svgIcon="x" class="size-4" />
        </button>
      </div>

      <form [formGroup]="form" (ngSubmit)="save()" class="flex flex-col flex-1 overflow-hidden">
        <div class="form-dialog__body">

          <div class="form-section" style="animation-delay: 40ms">
            <label class="form-label">Título <span class="form-required">*</span></label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <input matInput formControlName="title" placeholder="ej. Configuración" />
            </mat-form-field>
          </div>

          <div class="form-section grid grid-cols-2 gap-3" style="animation-delay: 80ms">
            <div>
              <label class="form-label">Ícono</label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <input matInput formControlName="icon" placeholder="ej. settings" />
              </mat-form-field>
            </div>
            <div>
              <label class="form-label">Orden</label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <input matInput type="number" formControlName="moduleOrder" placeholder="0" min="0" />
              </mat-form-field>
            </div>
          </div>

          <div class="form-section" style="animation-delay: 120ms">
            <label class="form-label">Ruta / Link</label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <mat-icon matPrefix svgIcon="link" class="!text-slate-400 size-4 mr-1" />
              <input matInput formControlName="link" placeholder="admin/setup" />
            </mat-form-field>
          </div>

          <div class="form-section" style="animation-delay: 160ms">
            <label class="form-label">Estado</label>
            <mat-slide-toggle formControlName="status" color="primary" class="!text-sm !text-slate-600">
              {{ form.get('status')?.value ? 'Activo' : 'Inactivo' }}
            </mat-slide-toggle>
          </div>

        </div>

        <div class="form-dialog__footer justify-between">
          <span class="text-xs text-slate-400">ID: <code class="font-mono">{{ parentModule.id }}</code></span>
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
export class ParentModuleEditComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  dialogRef = inject(MatDialogRef<ParentModuleEditComponent>);
  form!: UntypedFormGroup;
  protected shaking = signal(false);

  constructor(@Inject(MAT_DIALOG_DATA) public parentModule: ParentModule) {}

  ngOnInit(): void {
    this.form = this._fb.group({
      title:       [this.parentModule.title       ?? '', Validators.required],
      icon:        [this.parentModule.icon        ?? ''],
      link:        [this.parentModule.link        ?? ''],
      moduleOrder: [this.parentModule.moduleOrder ?? 0],
      status:      [this.parentModule.status      ?? true],
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this._shake();
      return;
    }
    this.dialogRef.close({ ...this.form.value, id: this.parentModule.id });
  }

  private _shake(): void {
    this.shaking.set(false);
    requestAnimationFrame(() => requestAnimationFrame(() => {
      this.shaking.set(true);
      setTimeout(() => this.shaking.set(false), 500);
    }));
  }
}
