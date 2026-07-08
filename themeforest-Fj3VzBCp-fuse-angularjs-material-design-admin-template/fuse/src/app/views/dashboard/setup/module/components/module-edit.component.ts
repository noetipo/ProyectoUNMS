import { Component, inject, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { Module } from '../models/module';
import { ParentModule } from '../../parentModule/models/parent-module';

export interface ModuleEditData {
  module: Module;
  parentModules: ParentModule[];
}

@Component({
  selector: 'app-module-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatIconModule,
  ],
  template: `
    <div class="form-dialog w-[560px]" [class.anim-shake]="shaking()">

      <div class="form-dialog__header">
        <div>
          <h2 class="form-dialog__title">Editar módulo</h2>
          <p class="form-dialog__subtitle">
            Modificando: <span class="font-medium text-slate-600">{{ data.module.title }}</span>
          </p>
        </div>
        <button mat-icon-button class="!w-8 !h-8 !text-slate-400 hover:!text-slate-600" (click)="dialogRef.close()">
          <mat-icon svgIcon="x" class="size-4" />
        </button>
      </div>

      <form [formGroup]="form" (ngSubmit)="save()" class="flex flex-col flex-1 overflow-hidden">
        <div class="form-dialog__body">

          <div class="form-section" style="animation-delay: 40ms">
            <label class="form-label">Módulo padre <span class="form-required">*</span></label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <mat-select formControlName="parentModuleId" placeholder="Seleccionar módulo padre">
                @for (pm of data.parentModules; track pm.id) {
                  <mat-option [value]="pm.id">{{ pm.title }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
          </div>

          <div class="form-section grid grid-cols-2 gap-3" style="animation-delay: 80ms">
            <div>
              <label class="form-label">Título <span class="form-required">*</span></label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <input matInput formControlName="title" placeholder="ej. Usuarios" />
              </mat-form-field>
            </div>
            <div>
              <label class="form-label">Subtítulo</label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <input matInput formControlName="subtitle" placeholder="Descripción breve" />
              </mat-form-field>
            </div>
          </div>

          <div class="form-section grid grid-cols-2 gap-3" style="animation-delay: 120ms">
            <div>
              <label class="form-label">Ícono</label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <input matInput formControlName="icon" placeholder="ej. settings, users" />
              </mat-form-field>
            </div>
            <div>
              <label class="form-label">Tipo</label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <mat-select formControlName="type">
                  <mat-option value="item">item</mat-option>
                  <mat-option value="group">group</mat-option>
                  <mat-option value="collapsable">collapsable</mat-option>
                </mat-select>
              </mat-form-field>
            </div>
          </div>

          <div class="form-section" style="animation-delay: 160ms">
            <label class="form-label">Ruta / Link</label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <mat-icon matPrefix svgIcon="link" class="!text-slate-400 size-4 mr-1" />
              <input matInput formControlName="link" placeholder="admin/setup/module" />
            </mat-form-field>
          </div>

          <div class="form-section grid grid-cols-2 gap-3 items-end" style="animation-delay: 200ms">
            <div>
              <label class="form-label">Orden</label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <input matInput type="number" formControlName="moduleOrder" placeholder="0" min="0" />
              </mat-form-field>
            </div>
            <div class="pb-0.5">
              <label class="form-label">Estado</label>
              <mat-slide-toggle formControlName="status" color="primary" class="!text-sm !text-slate-600">
                {{ form.get('status')?.value ? 'Activo' : 'Inactivo' }}
              </mat-slide-toggle>
            </div>
          </div>

        </div>

        <div class="form-dialog__footer justify-between">
          <span class="text-xs text-slate-400">ID: <code class="font-mono">{{ data.module.id }}</code></span>
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
export class ModuleEditComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  dialogRef = inject(MatDialogRef<ModuleEditComponent>);
  form!: UntypedFormGroup;

  protected shaking = signal(false);

  constructor(@Inject(MAT_DIALOG_DATA) public data: ModuleEditData) {}

  ngOnInit(): void {
    const m = this.data.module;
    this.form = this._fb.group({
      parentModuleId: [m.parentModuleId ?? m.parentModule?.id ?? '', Validators.required],
      title:          [m.title  ?? '',    Validators.required],
      subtitle:       [m.subtitle ?? ''],
      icon:           [m.icon    ?? ''],
      link:           [m.link    ?? ''],
      type:           [m.type    ?? 'item'],
      moduleOrder:    [m.moduleOrder ?? 0],
      status:         [m.status ?? true],
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this._shake();
      return;
    }
    this.dialogRef.close({ ...this.form.value, id: this.data.module.id });
  }

  private _shake(): void {
    this.shaking.set(false);
    requestAnimationFrame(() => requestAnimationFrame(() => {
      this.shaking.set(true);
      setTimeout(() => this.shaking.set(false), 500);
    }));
  }
}
