import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FASES, FASE_ENUM } from '../models/proyecto.model';

/** Modal para agregar una actividad al cronograma (plan de actividades). */
@Component({
  selector: 'app-actividad-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <div class="w-[440px] p-2">
      <h2 class="text-base font-bold text-slate-800 mb-4">Agregar actividad al cronograma</h2>

      <label class="block text-[12px] font-semibold text-slate-500 mb-1.5">Actividad</label>
      <input class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E] mb-3"
             [(ngModel)]="nombre" cdkFocusInitial placeholder="Ej.: Trabajo de campo: encuestas" />

      <label class="block text-[12px] font-semibold text-slate-500 mb-1.5">Fase</label>
      <select class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E] mb-3" [(ngModel)]="fase">
        @for (f of fases; track f) { <option [value]="f">{{ f }}</option> }
      </select>

      <div class="grid grid-cols-2 gap-3 mb-1">
        <div>
          <label class="block text-[12px] font-semibold text-slate-500 mb-1.5">Fecha de inicio</label>
          <input type="date" class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E]" [(ngModel)]="fechaInicio" />
        </div>
        <div>
          <label class="block text-[12px] font-semibold text-slate-500 mb-1.5">Fecha de fin</label>
          <input type="date" class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:border-[#8C1D2E]" [(ngModel)]="fechaFin" [min]="fechaInicio" />
        </div>
      </div>
      @if (fechaInvalida()) { <p class="text-[11px] text-rose-500 mb-1">La fecha de fin no puede ser anterior a la de inicio.</p> }

      <div class="flex items-center justify-end gap-2 mt-4">
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="cancelar()">Cancelar</button>
        <button mat-flat-button
                class="!h-9 !text-sm !rounded-lg !px-4 !font-medium !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                [disabled]="!nombre.trim() || !fechaInicio || fechaInvalida()" (click)="agregar()">
          Agregar actividad
        </button>
      </div>
    </div>
  `,
})
export class ActividadDialogComponent {
  private dialogRef = inject(MatDialogRef<ActividadDialogComponent>);
  protected readonly fases = FASES;
  protected nombre = '';
  protected fase = FASES[0];
  protected fechaInicio = '';
  protected fechaFin = '';

  fechaInvalida(): boolean { return !!this.fechaInicio && !!this.fechaFin && this.fechaFin < this.fechaInicio; }

  agregar(): void {
    if (!this.nombre.trim() || !this.fechaInicio || this.fechaInvalida()) return;
    this.dialogRef.close({
      nombre: this.nombre.trim(),
      fase: FASE_ENUM[this.fase],
      fechaInicio: this.fechaInicio,
      fechaFin: this.fechaFin || this.fechaInicio,
      estado: 'PENDIENTE',
    });
  }
  cancelar(): void { this.dialogRef.close(null); }
}
