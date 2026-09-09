import { Component, Inject, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { SecretariaDefensaService } from '../services/secretaria-defensa.service';

export interface ProgramarDefensaData {
  tesisId: string;
  estudiante: string;
  titulo: string;
  /** Nombres de los dos revisores del proyecto: ellos evalúan la defensa, no se eligen aquí. */
  revisores: string[];
}

/**
 * Modal de la Secretaría para programar la defensa: modalidad + fecha/hora/aula o enlace.
 *
 * <p>Quiénes evalúan la defensa NO se eligen aquí: son los mismos dos revisores que el
 * Coordinador ya designó y que evaluaron el proyecto con la rúbrica. El proceso no contempla
 * un jurado aparte para este paso — eso es de la Sustentación final (Etapa 8), una pantalla
 * distinta que aún no existe.</p>
 */
@Component({
  selector: 'app-programar-defensa-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="w-[480px] max-w-[92vw] flex flex-col max-h-[85vh]">
      <div class="px-4 pt-4 pb-2 shrink-0">
        <h2 class="text-base font-bold text-slate-800">Programar defensa del proyecto</h2>
        <p class="text-[12px] text-slate-500 mt-0.5">{{ data.estudiante }} · <span class="text-slate-400">{{ data.titulo }}</span></p>
      </div>

      <div class="px-4 flex-1 min-h-0 overflow-y-auto space-y-3">
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5">
          <p class="text-[10.5px] font-bold text-slate-400 uppercase tracking-wide mb-1">Evalúan la defensa</p>
          @if (data.revisores.length) {
            <div class="flex flex-wrap gap-1.5">
              @for (nombre of data.revisores; track nombre) {
                <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-white border border-slate-200 text-[11.5px] text-slate-700">
                  <mat-icon svgIcon="user-round" class="size-3 text-slate-400" /> {{ nombre }}
                </span>
              }
            </div>
          } @else {
            <p class="text-[12px] text-slate-500">Los dos revisores ya designados para el proyecto.</p>
          }
        </div>

        <div>
          <label class="lbl">Modalidad</label>
          <div class="seg">
            @for (m of modalidades; track m.valor) {
              <button type="button" [class.on]="modalidad === m.valor" (click)="modalidad = m.valor">{{ m.label }}</button>
            }
          </div>
        </div>

        <div class="grid grid-cols-[1fr_120px] gap-3">
          <div><label class="lbl">Fecha de la defensa</label><input type="date" class="inp" [(ngModel)]="fecha" /></div>
          <div><label class="lbl">Hora</label><input type="time" class="inp" [(ngModel)]="hora" /></div>
        </div>
        @if (requiereLugar()) {
          <div><label class="lbl">Aula o ambiente</label><input class="inp" [(ngModel)]="lugar" placeholder="Ej.: Auditorio de Posgrado" /></div>
        }
        @if (requiereEnlace()) {
          <div>
            <label class="lbl">Enlace de la sesión</label>
            <input class="inp" [(ngModel)]="enlace" placeholder="Ej.: https://meet.unmsm.edu.pe/defensa-…" />
            <p class="text-[11px] text-slate-400 mt-1">Se incluye en el aviso que recibe el doctorando.</p>
          </div>
        }
      </div>

      <div class="flex items-center justify-end gap-2 px-4 py-3 border-t border-slate-100 shrink-0">
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="cancelar()">Cancelar</button>
        <button mat-flat-button class="btn-programar !h-9 !text-sm !rounded-lg !px-4 !font-medium"
                [class.lista]="completo() && !guardando()"
                [disabled]="!completo() || guardando()" [title]="!completo() ? 'Completa fecha, hora y aula o enlace' : ''"
                (click)="programar()">
          @if (guardando()) { <mat-icon svgIcon="loader-circle" class="size-4 mr-1 animate-spin" /> } Programar defensa
        </button>
      </div>
    </div>
  `,
  styles: [`
    .lbl { display:block; font-size:12px; font-weight:600; color:#64748b; margin-bottom:5px; }
    .inp { width:100%; border:1px solid #e2e8f0; border-radius:8px; padding:8px 12px; font-size:14px; outline:none; background:#fff; }
    .inp:focus { border-color:#8C1D2E; }
    /* El fondo granate solo se aplica cuando el formulario está completo: si se forzara con
       !important sin condición, un botón deshabilitado se veía igual de "clickeable" que uno
       activo y el clic no hacía nada sin ninguna pista visual de por qué. */
    .btn-programar { background:#cbd5e1 !important; color:#64748b !important; }
    .btn-programar.lista { background:#8C1D2E !important; color:#fff !important; }
    .btn-programar.lista:hover { background:#731725 !important; }
    .seg { display:flex; border:1px solid #e2e8f0; border-radius:8px; overflow:hidden; }
    .seg button { flex:1; padding:7px 4px; font-size:12.5px; color:#475569; background:#fff; border-right:1px solid #eef2f7; }
    .seg button:last-child { border-right:0; }
    .seg button.on { background:#8C1D2E; color:#fff; font-weight:600; }
  `],
})
export class ProgramarDefensaDialogComponent {
  private _svc = inject(SecretariaDefensaService);
  private _toast = inject(NotificationService);
  private _confirm = inject(ConfirmDialogService);

  protected fecha = '';
  protected hora = '';
  protected lugar = '';
  protected enlace = '';
  protected modalidad = 'PRESENCIAL';
  protected guardando = signal(false);

  protected readonly modalidades = [
    { valor: 'PRESENCIAL', label: 'Presencial' },
    { valor: 'VIRTUAL', label: 'Virtual' },
    { valor: 'HIBRIDA', label: 'Híbrida' },
  ];

  /** La modalidad decide qué dato de ubicación se pide: aula, enlace o ambos. */
  protected requiereLugar(): boolean { return this.modalidad !== 'VIRTUAL'; }
  protected requiereEnlace(): boolean { return this.modalidad !== 'PRESENCIAL'; }

  /** Sin aula (o sin enlace, según la modalidad) el backend rechaza la programación. */
  protected completo(): boolean {
    if (!this.fecha) return false;
    if (this.requiereLugar() && !this.lugar.trim()) return false;
    if (this.requiereEnlace() && !this.enlace.trim()) return false;
    return true;
  }

  constructor(
    public dialogRef: MatDialogRef<ProgramarDefensaDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProgramarDefensaData,
  ) {}

  programar(): void {
    if (!this.completo() || this.guardando()) return;
    const modalidad = this.modalidades.find((m) => m.valor === this.modalidad)?.label ?? '';
    this._confirm.confirmSave({
      title: 'Programar la defensa',
      message: `Se citará a ${this.data.estudiante} a la defensa de su proyecto. Al confirmar:`,
      details: [
        `${modalidad}, el ${this.fecha}${this.hora ? ' a las ' + this.hora : ''}`,
        this.requiereLugar() ? `en ${this.lugar.trim()}` : `enlace: ${this.enlace.trim()}`,
        'evalúan los dos revisores ya designados para el proyecto',
        'el doctorando recibe el aviso de inmediato',
      ],
      confirmLabel: 'Programar defensa',
    }).then(() => this.enviar()).catch(() => {});
  }

  private enviar(): void {
    this.guardando.set(true);
    this._svc.programarDefensa$(this.data.tesisId, {
      fecha: this.fecha, hora: this.hora,
      lugar: this.requiereLugar() ? this.lugar : '',
      modalidad: this.modalidad,
      enlace: this.requiereEnlace() ? this.enlace : '',
    }).subscribe({
      next: () => { this._toast.success('Defensa programada'); this.dialogRef.close(true); },
      error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo programar'); },
    });
  }
  cancelar(): void { this.dialogRef.close(false); }
}
