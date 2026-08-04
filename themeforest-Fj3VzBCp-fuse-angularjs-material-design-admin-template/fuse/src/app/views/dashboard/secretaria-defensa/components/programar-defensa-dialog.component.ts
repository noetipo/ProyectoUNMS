import { Component, Inject, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { SecretariaDefensaService } from '../services/secretaria-defensa.service';

export interface ProgramarDefensaData {
  tesisId: string;
  estudiante: string;
  titulo: string;
}

/** Modal de la Secretaría para programar la defensa: Jurado Examinador + fecha/hora/lugar. */
@Component({
  selector: 'app-programar-defensa-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="w-[540px] max-w-[92vw] flex flex-col max-h-[85vh]">
      <div class="px-4 pt-4 pb-2 shrink-0">
        <h2 class="text-base font-bold text-slate-800">Programar defensa del proyecto</h2>
        <p class="text-[12px] text-slate-500 mt-0.5">{{ data.estudiante }} · <span class="text-slate-400">{{ data.titulo }}</span></p>
      </div>

      <div class="px-4 flex-1 min-h-0 overflow-y-auto space-y-3">
        <p class="text-[12px] text-slate-500">Designa el <b>Jurado Examinador</b> (Presidente + 2 miembros de la línea de investigación). La asesora se agrega automáticamente.</p>

        <div>
          <label class="lbl">Presidente del jurado</label>
          <select class="inp" [(ngModel)]="presidenteId">
            <option value="">— seleccionar —</option>
            @for (d of docentes(); track d.id) { <option [value]="d.id">{{ d.nombre }}</option> }
          </select>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="lbl">Miembro 1</label>
            <select class="inp" [(ngModel)]="miembro1">
              <option value="">— seleccionar —</option>
              @for (d of docentes(); track d.id) { <option [value]="d.id">{{ d.nombre }}</option> }
            </select>
          </div>
          <div>
            <label class="lbl">Miembro 2</label>
            <select class="inp" [(ngModel)]="miembro2">
              <option value="">— seleccionar —</option>
              @for (d of docentes(); track d.id) { <option [value]="d.id">{{ d.nombre }}</option> }
            </select>
          </div>
        </div>
        @if (!docentes().length) { <p class="text-[11px] text-amber-600">No hay docentes de la línea de investigación de la tesis disponibles para el jurado.</p> }
        @if (repetidos()) { <p class="text-[11px] text-rose-500">El presidente y los 2 miembros deben ser docentes distintos.</p> }

        <div class="grid grid-cols-[1fr_120px] gap-3">
          <div><label class="lbl">Fecha de la defensa</label><input type="date" class="inp" [(ngModel)]="fecha" /></div>
          <div><label class="lbl">Hora</label><input type="time" class="inp" [(ngModel)]="hora" /></div>
        </div>
        <div><label class="lbl">Lugar</label><input class="inp" [(ngModel)]="lugar" placeholder="Ej.: Auditorio de Posgrado" /></div>
      </div>

      <div class="flex items-center justify-end gap-2 px-4 py-3 border-t border-slate-100 shrink-0">
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="cancelar()">Cancelar</button>
        <button mat-flat-button class="!h-9 !text-sm !rounded-lg !px-4 !font-medium !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                [disabled]="!valido() || guardando()" (click)="programar()">
          @if (guardando()) { <mat-icon svgIcon="loader-circle" class="size-4 mr-1 animate-spin" /> } Programar defensa
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
export class ProgramarDefensaDialogComponent {
  private _svc = inject(SecretariaDefensaService);
  private _toast = inject(NotificationService);

  protected docentes = signal<any[]>([]);
  protected presidenteId = '';
  protected miembro1 = '';
  protected miembro2 = '';
  protected fecha = '';
  protected hora = '';
  protected lugar = '';
  protected guardando = signal(false);

  protected repetidos = computed(() => {
    const ids = [this.presidenteId, this.miembro1, this.miembro2].filter((x) => !!x);
    return new Set(ids).size !== ids.length;
  });
  protected valido = computed(() =>
    !!this.presidenteId && !!this.miembro1 && !!this.miembro2 && !this.repetidos() && !!this.fecha);

  constructor(
    public dialogRef: MatDialogRef<ProgramarDefensaDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProgramarDefensaData,
  ) {
    this._svc.docentesDefensa$(this.data.tesisId).subscribe({
      next: (res) => this.docentes.set((res?.data ?? res) ?? []),
      error: () => this._toast.error('No se pudieron cargar los docentes'),
    });
  }

  programar(): void {
    if (!this.valido() || this.guardando()) return;
    this.guardando.set(true);
    this._svc.programarDefensa$(this.data.tesisId, {
      presidenteId: this.presidenteId,
      miembroIds: [this.miembro1, this.miembro2],
      fecha: this.fecha, hora: this.hora, lugar: this.lugar,
    }).subscribe({
      next: () => { this._toast.success('Defensa programada'); this.dialogRef.close(true); },
      error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo programar'); },
    });
  }
  cancelar(): void { this.dialogRef.close(false); }
}
