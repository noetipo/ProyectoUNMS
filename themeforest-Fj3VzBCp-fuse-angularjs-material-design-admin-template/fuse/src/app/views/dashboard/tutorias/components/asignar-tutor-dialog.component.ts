import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TutoriaService } from '../services/tutoria.service';
import { TutorCombo } from '../models/tutoria.model';
import { TutorAutocompleteComponent } from './tutor-autocomplete.component';

/** Datos que recibe el diálogo: a quién se le asigna y si ya tenía tutor (cambio). */
export interface AsignarTutorData {
  estudianteId: string;
  estudianteNombre: string;
  tutorActual?: string | null;
  /** true cuando el diálogo se abre encadenado justo después de registrar el tema. */
  recienRegistrado?: boolean;
}

/**
 * Asigna el tutor de UN estudiante, sin salir de la pantalla donde se registró su tema
 * (el registro del tema y la designación del tutor son pasos consecutivos del proceso).
 * Para cargas masivas sigue existiendo "Asignar tutor en bloque".
 */
@Component({
  selector: 'app-asignar-tutor-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, TutorAutocompleteComponent],
  template: `
    <div class="form-dialog">
      <div class="form-dialog__header">
        <div>
          <p class="text-[11px] text-slate-400">Etapa 2 · Designación del tutor</p>
          <h2 class="text-sm font-semibold text-slate-800">
            {{ data.tutorActual ? 'Cambiar tutor' : 'Asignar tutor' }}
          </h2>
        </div>
        <button mat-icon-button mat-dialog-close class="!size-8"><mat-icon svgIcon="x" class="size-4" /></button>
      </div>

      <div class="form-dialog__body space-y-3">
        @if (data.recienRegistrado) {
          <div class="flex items-start gap-2 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2">
            <mat-icon svgIcon="circle-check" class="size-4 text-emerald-600 shrink-0 mt-0.5" />
            <p class="text-[12px] text-emerald-800">
              <b>Tema registrado.</b> El siguiente paso del proceso es designarle un tutor.
              Puedes hacerlo ahora o más tarde desde la columna <b>Tutor</b> del listado.
            </p>
          </div>
        }

        <p class="text-[12.5px] text-slate-500">
          Estudiante: <b class="text-slate-700">{{ data.estudianteNombre }}</b>
        </p>

        @if (data.tutorActual) {
          <p class="text-[11.5px] text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
            Tutor actual: <b>{{ data.tutorActual }}</b>. Al guardar se cierra esa tutoría y se registra la nueva.
          </p>
        }

        <app-tutor-autocomplete (selected)="tutor.set($event)" />

        @if (tutor(); as t) {
          <div class="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-sm">
            <span class="font-medium text-slate-700">{{ t.apellidos }}, {{ t.nombres }}</span>
            <span class="text-xs" [ngClass]="t.estudiantesActuales >= t.cupoMaximo ? 'text-rose-600' : 'text-emerald-600'">
              Cupo: {{ t.estudiantesActuales }}/{{ t.cupoMaximo }}
            </span>
          </div>
        }

        @if (err()) { <p class="text-[12px] text-rose-600">{{ err() }}</p> }
      </div>

      <div class="form-dialog__footer">
        <button mat-stroked-button mat-dialog-close class="!h-8 !text-xs">Cancelar</button>
        <button class="btn-dark !h-8 !text-xs !px-4" [disabled]="!tutor() || guardando()" (click)="guardar()">
          {{ guardando() ? 'Guardando…' : (data.tutorActual ? 'Cambiar tutor' : 'Asignar tutor') }}
        </button>
      </div>
    </div>
  `,
})
export class AsignarTutorDialogComponent {
  private _svc = inject(TutoriaService);
  private _ref = inject(MatDialogRef<AsignarTutorDialogComponent>);
  protected data = inject<AsignarTutorData>(MAT_DIALOG_DATA);

  protected tutor = signal<TutorCombo | null>(null);
  protected guardando = signal(false);
  protected err = signal<string | null>(null);

  guardar(): void {
    const t = this.tutor();
    if (!t) return;
    this.guardando.set(true);
    this.err.set(null);
    this._svc.asignar$(this.data.estudianteId, t.id).subscribe({
      next: () => this._ref.close(true),
      error: (e) => {
        this.err.set(e?.error?.message ?? 'No se pudo asignar el tutor');
        this.guardando.set(false);
      },
    });
  }
}
