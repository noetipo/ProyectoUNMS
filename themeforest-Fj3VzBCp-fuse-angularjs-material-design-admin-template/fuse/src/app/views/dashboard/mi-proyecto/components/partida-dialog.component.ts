import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { RUBROS } from '../models/proyecto.model';

/** Modal para agregar una partida al presupuesto. */
@Component({
  selector: 'app-partida-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <div class="w-[440px] p-2">
      <h2 class="text-base font-bold text-slate-800 mb-4">Agregar partida al presupuesto</h2>

      <label class="block text-[12px] font-semibold text-slate-500 mb-1.5">Rubro</label>
      <select class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E] mb-3" [(ngModel)]="rubro">
        @for (r of rubros; track r) { <option [value]="r">{{ r }}</option> }
      </select>

      <label class="block text-[12px] font-semibold text-slate-500 mb-1.5">Descripción</label>
      <input class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E] mb-3"
             [(ngModel)]="descripcion" cdkFocusInitial placeholder="Ej.: Reactivos de laboratorio" />

      <label class="block text-[12px] font-semibold text-slate-500 mb-1.5">Monto (S/)</label>
      <input type="number" min="0" class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E]"
             [(ngModel)]="monto" placeholder="0.00" />

      <div class="flex items-center justify-end gap-2 mt-4">
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="cancelar()">Cancelar</button>
        <button mat-flat-button
                class="!h-9 !text-sm !rounded-lg !px-4 !font-medium !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                (click)="agregar()">
          Agregar partida
        </button>
      </div>
    </div>
  `,
})
export class PartidaDialogComponent {
  private dialogRef = inject(MatDialogRef<PartidaDialogComponent>);
  protected readonly rubros = RUBROS;
  protected rubro = RUBROS[0];
  protected descripcion = '';
  protected monto: number | null = null;

  agregar(): void {
    this.dialogRef.close({
      rubro: this.rubro,
      descripcion: this.descripcion.trim(),
      monto: Number(this.monto) || 0,
    });
  }
  cancelar(): void { this.dialogRef.close(null); }
}
