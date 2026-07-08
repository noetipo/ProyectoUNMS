import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export type ConfirmDialogType = 'delete' | 'edit' | 'save';

export interface ConfirmDialogData {
  type?:    ConfirmDialogType;
  title?:   string;
  message?: string;
}

interface DialogConfig {
  iconBg:     string;
  iconColor:  string;
  icon:       string;
  btnClass:   string;
  btnLabel:   string;
}

const CONFIGS: Record<ConfirmDialogType, DialogConfig> = {
  delete: {
    iconBg:    'bg-rose-50',
    iconColor: 'text-rose-500',
    icon:      'trash-2',
    btnClass:  '!bg-rose-600 !text-white hover:!bg-rose-700',
    btnLabel:  'Eliminar',
  },
  edit: {
    iconBg:    'bg-amber-50',
    iconColor: 'text-amber-500',
    icon:      'pencil',
    btnClass:  '!bg-amber-500 !text-white hover:!bg-amber-600',
    btnLabel:  'Guardar cambios',
  },
  save: {
    iconBg:    'bg-emerald-50',
    iconColor: 'text-emerald-500',
    icon:      'check',
    btnClass:  '!bg-emerald-600 !text-white hover:!bg-emerald-700',
    btnLabel:  'Confirmar',
  },
};

const DEFAULT_TITLES: Record<ConfirmDialogType, string> = {
  delete: '¿Eliminar registro?',
  edit:   '¿Guardar cambios?',
  save:   '¿Confirmar acción?',
};

const DEFAULT_MESSAGES: Record<ConfirmDialogType, string> = {
  delete: 'Esta acción no se puede deshacer.',
  edit:   'Se guardarán los cambios realizados.',
  save:   'Se ejecutará la acción seleccionada.',
};

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="confirm-dialog w-[360px]">

      <div class="flex items-start gap-4 mb-6">
        <div class="confirm-dialog__icon-wrap" [class]="cfg.iconBg">
          <mat-icon [svgIcon]="cfg.icon" class="size-5" [class]="cfg.iconColor" />
        </div>
        <div class="pt-0.5">
          <h2 class="confirm-dialog__title">{{ data.title ?? defaultTitle }}</h2>
          <p class="confirm-dialog__message">{{ data.message ?? defaultMessage }}</p>
        </div>
      </div>

      <div class="flex items-center justify-end gap-2">
        <button mat-button class="!text-slate-500 !text-sm !h-9" (click)="onCancel()">
          Cancelar
        </button>
        <button
          mat-flat-button
          class="!text-sm !h-9 !rounded-lg !px-4 !font-medium transition-colors"
          [class]="cfg.btnClass"
          (click)="onConfirm()"
        >
          {{ cfg.btnLabel }}
        </button>
      </div>

    </div>
  `,
})
export class ConfirmDialogComponent {
  protected readonly cfg: DialogConfig;
  protected readonly defaultTitle:   string;
  protected readonly defaultMessage: string;

  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData,
  ) {
    const type = data.type ?? 'delete';
    this.cfg            = CONFIGS[type];
    this.defaultTitle   = DEFAULT_TITLES[type];
    this.defaultMessage = DEFAULT_MESSAGES[type];
  }

  onConfirm(): void { this.dialogRef.close(true);  }
  onCancel():  void { this.dialogRef.close(false); }
}
