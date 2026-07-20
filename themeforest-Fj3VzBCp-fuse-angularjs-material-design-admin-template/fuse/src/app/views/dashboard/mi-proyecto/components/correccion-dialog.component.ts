import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface CorreccionDialogData {
  /** Nombre del ítem/campo que se corrige (p. ej. "Bases teóricas"). */
  titulo: string;
}

/** Modal para que el estudiante describa qué corrigió antes de levantar la observación. */
@Component({
  selector: 'app-correccion-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <div class="w-[440px] p-2">
      <h2 class="text-base font-bold text-slate-800 mb-4">Enviar corrección: {{ data.titulo }}</h2>

      <label class="block text-[12px] font-semibold text-slate-500 mb-1.5">Descripción de la corrección realizada</label>
      <textarea
        class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E]"
        rows="4" [(ngModel)]="texto" cdkFocusInitial
        placeholder="Qué cambiaste en el ítem para levantar la observación…"></textarea>

      <div class="flex items-center justify-end gap-2 mt-4">
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="cancelar()">
          Cancelar
        </button>
        <button mat-flat-button
                class="!h-9 !text-sm !rounded-lg !px-4 !font-medium !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                [disabled]="!texto.trim()" (click)="confirmar()">
          Enviar corrección
        </button>
      </div>
    </div>
  `,
})
export class CorreccionDialogComponent {
  protected texto = '';

  constructor(
    public dialogRef: MatDialogRef<CorreccionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CorreccionDialogData,
  ) {}

  confirmar(): void { if (this.texto.trim()) this.dialogRef.close(this.texto.trim()); }
  cancelar(): void { this.dialogRef.close(null); }
}
