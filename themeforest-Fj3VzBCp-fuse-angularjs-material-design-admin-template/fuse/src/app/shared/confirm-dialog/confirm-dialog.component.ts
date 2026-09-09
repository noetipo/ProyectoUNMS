import { Component, Inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export type ConfirmDialogType = 'delete' | 'edit' | 'save';

export interface ConfirmDialogData {
  type?:    ConfirmDialogType;
  title?:   string;
  message?: string;
  /** Lo que va a ocurrir al confirmar, en viñetas: quien decide sabe qué desencadena. */
  details?: string[];
  /** Casilla que hay que marcar antes de poder confirmar. Solo para pasos sin vuelta atrás. */
  acknowledge?: string;
  /** Texto del botón de confirmación cuando el genérico ("Confirmar") no dice lo suficiente. */
  confirmLabel?: string;
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

          @if (data.details?.length) {
            <ul class="mt-2 list-disc pl-4 text-[12.5px] leading-relaxed text-slate-600">
              @for (d of data.details; track d) { <li>{{ d }}</li> }
            </ul>
          }
        </div>
      </div>

      @if (data.acknowledge) {
        <label class="mb-4 -mt-3 flex cursor-pointer items-center gap-2 text-[12.5px] text-slate-600">
          <input type="checkbox" class="size-3.5 accent-[#8C1D2E]" [checked]="aceptado()" (change)="alternar()" />
          {{ data.acknowledge }}
        </label>
      }

      <div class="flex items-center justify-end gap-2">
        <button mat-button class="!text-slate-500 !text-sm !h-9" (click)="onCancel()">
          Cancelar
        </button>
        <button
          mat-flat-button
          class="!text-sm !h-9 !rounded-lg !px-4 !font-medium transition-colors"
          [class]="cfg.btnClass"
          [disabled]="!!data.acknowledge && !aceptado()"
          (click)="onConfirm()"
        >
          {{ data.confirmLabel ?? cfg.btnLabel }}
        </button>
      </div>

    </div>
  `,
})
export class ConfirmDialogComponent {
  protected readonly cfg: DialogConfig;
  protected readonly defaultTitle:   string;
  protected readonly defaultMessage: string;

  /** Casilla de "entiendo": mientras esté sin marcar, el botón de confirmar queda inhabilitado. */
  protected readonly aceptado = signal(false);

  protected alternar(): void { this.aceptado.update((v) => !v); }

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
