import { Component, Inject, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { CoordinadorProyectoService } from '../services/coordinador-proyecto.service';

export interface DesignarJuradoInformeData { tesisId: string; estudiante: string; titulo: string; }

/** Modal del Coordinador para designar el Jurado Informante (3 doctores; el 1.º es Presidente). */
@Component({
  selector: 'app-designar-jurado-informe-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="w-[520px] max-w-[92vw] flex flex-col max-h-[85vh]">
      <div class="px-4 pt-4 pb-2 shrink-0">
        <h2 class="text-base font-bold text-slate-800">Designar Jurado Informante</h2>
        <p class="text-[12px] text-slate-500 mt-0.5">{{ data.estudiante }} · <span class="text-slate-400">{{ data.titulo }}</span></p>
      </div>

      <div class="px-4 flex-1 min-h-0 overflow-y-auto space-y-3">
        <p class="text-[12px] text-slate-500">Designa <b>3 docentes</b> con grado de Doctor. El <b>primero</b> actúa como Presidente del Jurado. No puede ser el asesor.</p>
        <div>
          <label class="lbl">Presidente (1.º)</label>
          <select class="inp" [(ngModel)]="m1">
            <option value="">— seleccionar —</option>
            @for (d of docentes(); track d.id) { <option [value]="d.id">{{ d.nombre }}</option> }
          </select>
        </div>
        <div>
          <label class="lbl">Miembro 2</label>
          <select class="inp" [(ngModel)]="m2">
            <option value="">— seleccionar —</option>
            @for (d of docentes(); track d.id) { <option [value]="d.id">{{ d.nombre }}</option> }
          </select>
        </div>
        <div>
          <label class="lbl">Miembro 3</label>
          <select class="inp" [(ngModel)]="m3">
            <option value="">— seleccionar —</option>
            @for (d of docentes(); track d.id) { <option [value]="d.id">{{ d.nombre }}</option> }
          </select>
        </div>
        @if (repetidos()) { <p class="text-[11px] text-rose-500">Los 3 miembros deben ser docentes distintos.</p> }
      </div>

      <div class="flex items-center justify-end gap-2 px-4 py-3 border-t border-slate-100 shrink-0">
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="cancelar()">Cancelar</button>
        <button mat-flat-button class="!h-9 !text-sm !rounded-lg !px-4 !font-medium !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                [disabled]="!valido() || guardando()" (click)="designar()">
          @if (guardando()) { <mat-icon svgIcon="loader-circle" class="size-4 mr-1 animate-spin" /> } Designar jurado
        </button>
      </div>
    </div>
  `,
  styles: [`
    .lbl { display:block; font-size:12px; font-weight:600; color:#64748b; margin-bottom:5px; }
    .inp { width:100%; border:1px solid #e2e8f0; border-radius:8px; padding:8px 12px; font-size:14px; outline:none; background:#fff; }
    .inp:focus { border-color:#8C1D2E; }
  `],
})
export class DesignarJuradoInformeDialogComponent {
  private _svc = inject(CoordinadorProyectoService);
  private _toast = inject(NotificationService);

  protected docentes = signal<any[]>([]);
  protected m1 = '';
  protected m2 = '';
  protected m3 = '';
  protected guardando = signal(false);

  protected repetidos = computed(() => {
    const ids = [this.m1, this.m2, this.m3].filter((x) => !!x);
    return new Set(ids).size !== ids.length;
  });
  protected valido = computed(() => !!this.m1 && !!this.m2 && !!this.m3 && !this.repetidos());

  constructor(
    public dialogRef: MatDialogRef<DesignarJuradoInformeDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DesignarJuradoInformeData,
  ) {
    this._svc.docentes$(this.data.tesisId).subscribe({
      next: (res) => this.docentes.set((res?.data ?? res) ?? []),
      error: () => this._toast.error('No se pudieron cargar los docentes'),
    });
  }

  designar(): void {
    if (!this.valido() || this.guardando()) return;
    this.guardando.set(true);
    this._svc.designarJuradoInforme$(this.data.tesisId, [this.m1, this.m2, this.m3]).subscribe({
      next: () => { this._toast.success('Jurado Informante designado'); this.dialogRef.close(true); },
      error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo designar'); },
    });
  }
  cancelar(): void { this.dialogRef.close(false); }
}
