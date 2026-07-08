import { CommonModule } from '@angular/common';
import { Component, Inject, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { TutoriaService } from '../services/tutoria.service';
import { TutoriaHistorial } from '../models/tutoria.model';

export interface TutoriaHistorialDialogData {
  estudianteId: string;
  nombre?: string;
}

/** Diálogo: historial de tutorías de un estudiante (tutor, rango de fechas, vigente). */
@Component({
  selector: 'app-tutoria-historial-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="form-dialog w-[520px]">
      <div class="form-dialog__header">
        <h2 class="form-dialog__title">Historial de tutorías</h2>
        <button mat-icon-button (click)="dialogRef.close()"><mat-icon svgIcon="x" class="!size-4" /></button>
      </div>
      <div class="form-dialog__body">
        @if (data.nombre) { <p class="text-sm text-slate-500 mb-3">{{ data.nombre }}</p> }
        @if (loading()) {
          <p class="text-sm text-slate-400">Cargando…</p>
        } @else if (!items().length) {
          <p class="text-sm text-slate-400">Sin registros de tutoría.</p>
        } @else {
          <ul class="space-y-2">
            @for (h of items(); track h.id) {
              <li class="rounded-lg border border-slate-100 px-3 py-2">
                <div class="flex items-center justify-between">
                  <span class="font-medium text-slate-700 text-sm">{{ h.docenteNombre }}
                    @if (h.gradoAcademico) { <span class="text-slate-400">· {{ h.gradoAcademico }}</span> }
                  </span>
                  @if (h.actual) {
                    <span class="text-[10px] font-semibold uppercase tracking-wide bg-emerald-100 text-emerald-700 px-1.5 py-0.5 rounded">Vigente</span>
                  } @else {
                    <span class="text-[10px] font-semibold uppercase tracking-wide bg-slate-100 text-slate-500 px-1.5 py-0.5 rounded">Cerrada</span>
                  }
                </div>
                <p class="text-xs text-slate-400 mt-0.5">
                  {{ h.fechaInicio | date:'dd/MM/yyyy' }} — {{ h.fechaFin ? (h.fechaFin | date:'dd/MM/yyyy') : 'actualidad' }}
                </p>
                @if (h.motivoCambio) { <p class="text-xs text-slate-500 mt-0.5 italic">{{ h.motivoCambio }}</p> }
              </li>
            }
          </ul>
        }
      </div>
      <div class="form-dialog__footer justify-end">
        <button mat-button class="!text-slate-500 !text-sm" (click)="dialogRef.close()">Cerrar</button>
      </div>
    </div>
  `,
})
export class TutoriaHistorialDialogComponent implements OnInit {
  private _svc = inject(TutoriaService);
  dialogRef = inject(MatDialogRef<TutoriaHistorialDialogComponent>);

  protected items = signal<TutoriaHistorial[]>([]);
  protected loading = signal(true);

  constructor(@Inject(MAT_DIALOG_DATA) public data: TutoriaHistorialDialogData) {}

  ngOnInit(): void {
    this._svc.historial$(this.data.estudianteId).subscribe({
      next: (res: any) => { this.items.set(res?.data ?? []); this.loading.set(false); },
      error: () => { this.items.set([]); this.loading.set(false); },
    });
  }
}
