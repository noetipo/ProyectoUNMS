import { Component, Inject, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { CoordinadorProyectoService } from '../services/coordinador-proyecto.service';

export interface DetalleDesignacionData {
  tesisId: string;
  estudiante: string;
  titulo: string;
  defensaProgramada?: boolean;
  juradoInformeDesignado?: boolean;
}

/** Muestra los revisores del proyecto, el Jurado Examinador y el Jurado Informante designados. */
@Component({
  selector: 'app-detalle-designacion-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="w-[480px] max-w-[92vw] p-2">
      <h2 class="text-base font-bold text-slate-800 px-2 pt-2">Designaciones de la tesis</h2>
      <p class="text-[12px] text-slate-500 px-2 mb-3">{{ data.estudiante }} · <span class="text-slate-400">{{ data.titulo }}</span></p>

      <div class="px-2 space-y-4 max-h-[65vh] overflow-y-auto">
        <!-- Revisores del proyecto -->
        <div>
          <p class="text-[11px] font-bold tracking-wider text-slate-400 mb-1.5">REVISORES DEL PROYECTO (JURADO INFORMANTE DEL PROYECTO)</p>
          @if (revisores().length) {
            <div class="space-y-1.5">
              @for (rv of revisores(); track rv.id) {
                <div class="flex items-center gap-2 rounded-lg border border-slate-100 px-3 py-2">
                  <span class="text-[11px] font-mono text-slate-400 w-8 shrink-0">R{{ rv.orden }}</span>
                  <span class="flex-1 text-[13px] text-slate-700">{{ rv.docenteNombre }}</span>
                  <span class="px-1.5 py-0.5 rounded text-[10px] font-bold" [ngClass]="estadoCls(rv.estado)">{{ estadoLabel(rv.estado) }}</span>
                  @if (rv.comentario != null && rv.comentario) { <mat-icon svgIcon="message-square" class="size-3.5 text-slate-300" [title]="rv.comentario" /> }
                </div>
              }
            </div>
          } @else { <p class="text-[12px] text-slate-400">Aún no se han designado revisores.</p> }
        </div>

        <!-- Jurado Examinador (defensa) -->
        @if (data.defensaProgramada) {
          <div>
            <p class="text-[11px] font-bold tracking-wider text-slate-400 mb-1.5">JURADO EXAMINADOR DE LA DEFENSA</p>
            @if (defensa()?.fecha) {
              <p class="text-[12px] text-slate-500 mb-1.5">
                {{ defensa().fecha | date:'dd/MM/yyyy' }}<span *ngIf="defensa().hora"> · {{ defensa().hora }}</span><span *ngIf="defensa().lugar"> · {{ defensa().lugar }}</span>
              </p>
            }
            <div class="space-y-1.5">
              @for (j of defensa()?.jurado ?? []; track j.docenteId) {
                <div class="flex items-center gap-2 rounded-lg border border-slate-100 px-3 py-2">
                  <span class="flex-1 text-[13px] text-slate-700">{{ j.docenteNombre }}</span>
                  <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600">{{ rolLabel(j.rol) }}</span>
                </div>
              }
            </div>
            @if (defensa()?.dictamenNumero) { <p class="text-[10.5px] text-slate-400 font-mono mt-1.5">{{ defensa().dictamenNumero }}</p> }
          </div>
        }

        <!-- Jurado Informante del informe final -->
        @if (data.juradoInformeDesignado) {
          <div>
            <p class="text-[11px] font-bold tracking-wider text-slate-400 mb-1.5">JURADO INFORMANTE DEL INFORME FINAL</p>
            <div class="space-y-1.5">
              @for (jr of juradoInforme(); track jr.id) {
                <div class="flex items-center gap-2 rounded-lg border border-slate-100 px-3 py-2">
                  <span class="flex-1 text-[13px] text-slate-700">{{ jr.docenteNombre }}</span>
                  @if (jr.presidente) { <span class="text-[10px] text-[#8C1D2E] font-bold">Presidente</span> }
                  <span class="px-1.5 py-0.5 rounded text-[10px] font-bold" [ngClass]="estadoCls(jr.estado)">{{ estadoLabel(jr.estado) }}</span>
                </div>
              }
            </div>
          </div>
        }
      </div>

      <div class="flex justify-end px-2 pt-3">
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="dialogRef.close()">Cerrar</button>
      </div>
    </div>
  `,
})
export class DetalleDesignacionDialogComponent {
  private _svc = inject(CoordinadorProyectoService);

  protected revisores = signal<any[]>([]);
  protected defensa = signal<any | null>(null);
  protected juradoInforme = signal<any[]>([]);

  constructor(
    public dialogRef: MatDialogRef<DetalleDesignacionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DetalleDesignacionData,
  ) {
    this._svc.revisores$(data.tesisId).subscribe({ next: (r) => this.revisores.set((r?.data ?? r) ?? []) });
    if (data.defensaProgramada) {
      this._svc.defensa$(data.tesisId).subscribe({ next: (r) => this.defensa.set(r?.data ?? r) });
    }
    if (data.juradoInformeDesignado) {
      this._svc.juradoInforme$(data.tesisId).subscribe({ next: (r) => this.juradoInforme.set((r?.data ?? r) ?? []) });
    }
  }

  estadoLabel(e?: string): string { return e === 'CONFORME' ? '✓ Conforme' : e === 'OBSERVADO' ? '⚑ Observó' : 'Designado'; }
  estadoCls(e?: string): string {
    return e === 'CONFORME' ? 'bg-emerald-100 text-emerald-700' : e === 'OBSERVADO' ? 'bg-rose-100 text-rose-700' : 'bg-slate-100 text-slate-500';
  }
  rolLabel(r?: string): string { return r === 'PRESIDENTE' ? 'Presidente' : r === 'ASESOR' ? 'Asesor(a)' : 'Miembro'; }
}
