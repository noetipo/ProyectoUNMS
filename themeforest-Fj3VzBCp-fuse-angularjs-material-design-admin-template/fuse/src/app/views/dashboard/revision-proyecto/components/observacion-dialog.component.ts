import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface ObservacionDialogData {
  /** Nombre del ítem/campo que se observa (p. ej. "Título del proyecto"). */
  titulo: string;
}

/** Modal para que el asesor escriba la observación de un ítem del proyecto. */
@Component({
  selector: 'app-observacion-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <div class="w-[440px] p-2">
      <h2 class="text-base font-bold text-slate-800 mb-4">Observar ítem: {{ data.titulo }}</h2>

      <label class="block text-[12px] font-semibold text-slate-500 mb-1.5">Observación del asesor</label>
      <textarea
        class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E]"
        rows="4" [(ngModel)]="texto" cdkFocusInitial
        placeholder="Qué debe corregir el doctorando en este ítem y por qué…"></textarea>

      <div class="flex items-center justify-end gap-2 mt-4">
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="cancelar()">
          Cancelar
        </button>
        <button mat-flat-button
                class="!h-9 !text-sm !rounded-lg !px-4 !font-medium !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                [disabled]="!texto.trim()" (click)="confirmar()">
          Registrar observación
        </button>
      </div>
    </div>
  `,
})
export class ObservacionDialogComponent {
  protected texto = '';

  constructor(
    public dialogRef: MatDialogRef<ObservacionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ObservacionDialogData,
  ) {}

  confirmar(): void { if (this.texto.trim()) this.dialogRef.close(this.texto.trim()); }
  cancelar(): void { this.dialogRef.close(null); }
}
