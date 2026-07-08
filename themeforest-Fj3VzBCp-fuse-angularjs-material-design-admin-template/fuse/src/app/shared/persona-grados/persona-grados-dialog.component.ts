import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { GRADOS_ACADEMICOS_LABELS } from '@/app/views/dashboard/personas/models/persona.model';
import { PersonaService } from '@/app/views/dashboard/personas/services/persona.service';

/** Datos que recibe el diálogo: id de persona y su nombre para el título. */
export interface PersonaGradosDialogData {
  id: string;
  nombre?: string;
}

/**
 * Diálogo de solo lectura que lista los grados académicos de una persona
 * (grado, año, universidad y cuál es el principal). Se usa desde los reportes
 * de personas y docentes para ver el detalle sin abrir el formulario de edición.
 */
@Component({
  selector: 'app-persona-grados-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title class="!text-base">Grados académicos{{ data.nombre ? ' — ' + data.nombre : '' }}</h2>
    <mat-dialog-content class="!pt-1">
      @if (loading()) {
        <div class="flex justify-center py-6"><mat-spinner diameter="28" /></div>
      } @else if (grados().length) {
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-left text-slate-400 border-b border-slate-100">
                <th class="py-1.5 pr-3 font-medium">Grado</th>
                <th class="py-1.5 pr-3 font-medium">Año</th>
                <th class="py-1.5 pr-3 font-medium">Universidad</th>
                <th class="py-1.5 font-medium text-center">Principal</th>
              </tr>
            </thead>
            <tbody>
              @for (g of grados(); track g.id ?? g.grado) {
                <tr class="border-b border-slate-50">
                  <td class="py-1.5 pr-3 text-slate-700">{{ label(g.grado) }}</td>
                  <td class="py-1.5 pr-3 text-slate-500 tabular-nums">{{ g.anio ?? '—' }}</td>
                  <td class="py-1.5 pr-3 text-slate-500">{{ g.universidad ?? '—' }}</td>
                  <td class="py-1.5 text-center">
                    @if (g.principal) {
                      <span class="inline-block rounded-full bg-emerald-50 text-emerald-600 text-xs px-2 py-0.5">Principal</span>
                    } @else { <span class="text-slate-300">—</span> }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      } @else {
        <p class="text-sm text-slate-400 py-4">Esta persona no tiene grados académicos registrados.</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cerrar</button>
    </mat-dialog-actions>
  `,
})
export class PersonaGradosDialogComponent implements OnInit {
  protected readonly data = inject<PersonaGradosDialogData>(MAT_DIALOG_DATA);
  private readonly _service = inject(PersonaService);
  protected readonly _ref = inject(MatDialogRef<PersonaGradosDialogComponent>);

  protected loading = signal(true);
  protected grados = signal<any[]>([]);

  ngOnInit(): void {
    this._service.obtener$(this.data.id).subscribe({
      next: (res) => {
        const p = res?.data ?? res;
        this.grados.set(p?.gradosAcademicos ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  label(grado: string): string {
    return GRADOS_ACADEMICOS_LABELS[grado] ?? grado;
  }
}
