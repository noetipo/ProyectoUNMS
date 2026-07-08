import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { LineaInvestigacionService } from '@/app/shared/catalogo/linea-investigacion.service';
import { RegistroTemaService } from '../services/registro-tema.service';
import { EstudianteTema } from '../models/registro-tema.model';

interface DialogData {
  estudiante: EstudianteTema;
  editar: boolean;
}

@Component({
  selector: 'app-registro-tema-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
  ],
  template: `
    <div class="p-5 w-[520px] max-w-full">
      <div class="flex items-start justify-between mb-4">
        <div>
          <h2 class="text-lg font-semibold text-slate-800">
            {{ data.editar ? 'Editar tema' : 'Registrar tema' }}
          </h2>
          <p class="text-xs text-slate-400 mt-0.5">Proceso de tesis · Registro de tema</p>
        </div>
        <button mat-icon-button (click)="ref.close()" class="!w-8 !h-8">
          <mat-icon svgIcon="x" class="size-4 text-slate-400" />
        </button>
      </div>

      <!-- Estudiante fijado -->
      <div class="rounded-lg bg-slate-50 border border-slate-100 px-4 py-3 mb-4">
        <p class="font-medium text-slate-700">{{ data.estudiante.apellidos }}, {{ data.estudiante.nombres }}</p>
        <p class="text-xs text-slate-400 font-mono">
          {{ data.estudiante.codigoSistema ?? data.estudiante.codMatricula ?? '—' }}
          @if (data.estudiante.programaNombre) { · {{ data.estudiante.programaNombre }} }
        </p>
      </div>

      <form [formGroup]="form" (ngSubmit)="guardar()" class="flex flex-col gap-3">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Línea de investigación</mat-label>
          <mat-select formControlName="lineaInvestigacionId">
            @for (l of lineas(); track l.id) { <mat-option [value]="l.id">{{ l.nombre }}</mat-option> }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Título del tema</mat-label>
          <input matInput formControlName="titulo" maxlength="500" placeholder="Título tentativo de la tesis" />
        </mat-form-field>

        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Resumen (opcional)</mat-label>
          <textarea matInput formControlName="resumen" rows="4" maxlength="2000"
                    placeholder="Breve resumen del tema"></textarea>
        </mat-form-field>

        <p class="text-[11px] text-slate-400">
          El nivel se deriva del programa del estudiante y la fecha la registra el sistema.
        </p>

        @if (error()) { <p class="text-xs text-rose-500">{{ error() }}</p> }

        <div class="flex justify-end gap-2 mt-2">
          <button type="button" mat-stroked-button (click)="ref.close()" [disabled]="saving()">Cancelar</button>
          <button type="submit" class="btn-dark" [disabled]="form.invalid || saving()">
            {{ saving() ? 'Guardando…' : (data.editar ? 'Guardar cambios' : 'Registrar tema') }}
          </button>
        </div>
      </form>
    </div>
  `,
})
export class RegistroTemaDialogComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _lineaSvc = inject(LineaInvestigacionService);
  private _svc = inject(RegistroTemaService);
  protected ref = inject(MatDialogRef<RegistroTemaDialogComponent>);
  protected data = inject<DialogData>(MAT_DIALOG_DATA);

  protected lineas = signal<{ id: string; nombre: string }[]>([]);
  protected saving = signal(false);
  protected error = signal<string | null>(null);

  form!: UntypedFormGroup;

  ngOnInit(): void {
    this.form = this._fb.group({
      lineaInvestigacionId: ['', Validators.required],
      titulo: ['', [Validators.required, Validators.maxLength(500)]],
      resumen: [''],
    });

    // Precarga en modo edición (título existente; la línea se resuelve por nombre al cargar).
    if (this.data.editar) {
      this.form.patchValue({ titulo: this.data.estudiante.titulo ?? '' });
    }

    this._lineaSvc.listarTodos$().subscribe({
      next: (res: any) => {
        const items = res?.data?.content ?? res?.content ?? res?.data ?? [];
        this.lineas.set(items);
        if (this.data.editar && this.data.estudiante.lineaNombre) {
          const match = items.find((l: any) => l.nombre === this.data.estudiante.lineaNombre);
          if (match) this.form.patchValue({ lineaInvestigacionId: match.id });
        }
      },
      error: () => this.lineas.set([]),
    });
  }

  guardar(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set(null);
    const body = {
      lineaInvestigacionId: this.form.value.lineaInvestigacionId,
      titulo: (this.form.value.titulo ?? '').trim(),
      resumen: (this.form.value.resumen ?? '').trim() || undefined,
    };
    const req$ = this.data.editar
      ? this._svc.editar$(this.data.estudiante.estudianteId, body)
      : this._svc.registrar$(this.data.estudiante.estudianteId, body);

    req$.subscribe({
      next: (res: any) => { this.saving.set(false); this.ref.close(res?.data ?? res ?? true); },
      error: (err: any) => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? err?.error?.error ?? 'No se pudo guardar el tema');
      },
    });
  }
}
