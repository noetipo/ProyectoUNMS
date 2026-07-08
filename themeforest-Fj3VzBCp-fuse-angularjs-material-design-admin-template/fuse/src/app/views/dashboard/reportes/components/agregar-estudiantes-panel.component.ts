import { CommonModule } from '@angular/common';
import { Component, computed, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { debounceTime, distinctUntilChanged, startWith, switchMap } from 'rxjs';
import { TutoriaService } from '@/app/views/dashboard/tutorias/services/tutoria.service';
import { EstudianteAsignable } from '@/app/views/dashboard/tutorias/models/tutoria.model';

/**
 * Panel embebido para AGREGAR estudiantes a un tutor ya fijo (la fila del reporte).
 * Reutiliza el buscador de estudiantes (conTutor=false) y la asignación en bloque.
 */
@Component({
  selector: 'app-agregar-estudiantes-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <div class="rounded-lg border border-slate-200 bg-slate-50/60 p-3 mt-2">
      <div class="flex items-center justify-between mb-2">
        <p class="text-sm font-medium text-slate-700">Agregar estudiantes a {{ tutorNombre }}</p>
        <span class="text-xs" [class.text-emerald-600]="disponibles() > 0" [class.text-rose-600]="disponibles() <= 0">
          {{ disponibles() }} cupo(s) disponible(s)
        </span>
      </div>

      @if (msg()) {
        <div class="mb-2 rounded px-3 py-1.5 text-xs"
          [class.bg-emerald-50]="!error()" [class.text-emerald-700]="!error()"
          [class.bg-red-50]="error()" [class.text-red-700]="error()">{{ msg() }}</div>
      }

      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
        <mat-icon matPrefix svgIcon="search" class="!size-4 text-slate-400 ml-2" />
        <input matInput [formControl]="buscar" placeholder="Buscar estudiante sin tutor…" />
      </mat-form-field>

      <div class="max-h-52 overflow-y-auto mt-1 divide-y divide-slate-100">
        @if (loading()) {
          <p class="text-xs text-slate-400 py-2">Buscando…</p>
        } @else if (!resultados().length) {
          <p class="text-xs text-slate-400 py-2">Sin estudiantes para agregar.</p>
        } @else {
          @for (e of resultados(); track e.estudianteId) {
            <div class="flex items-center justify-between py-1.5">
              <div class="min-w-0">
                <p class="text-sm text-slate-700 truncate">{{ e.apellidos }}, {{ e.nombres }}</p>
                <p class="text-[11px] text-slate-400">{{ e.codigoSistema ?? '—' }} · {{ e.programaNombre ?? '—' }}</p>
              </div>
              @if (seleccion().has(e.estudianteId)) {
                <button type="button" mat-button class="!text-xs !text-emerald-600" (click)="quitar(e)">✓ Agregado</button>
              } @else {
                <button type="button" mat-stroked-button class="!h-7 !text-xs !min-w-0 !px-2"
                  [disabled]="disponibles() <= 0" (click)="agregar(e)">+ Agregar</button>
              }
            </div>
          }
        }
      </div>

      <div class="flex items-center justify-end gap-2 mt-2 pt-2 border-t border-slate-100">
        <button type="button" mat-button class="!text-slate-500 !text-xs" (click)="cerrar.emit()">Cancelar</button>
        <button type="button" mat-flat-button color="primary" class="!h-8 !text-xs !px-4"
          [disabled]="!seleccion().size || saving() || excede()" (click)="asignar()">
          {{ saving() ? 'Asignando…' : 'Asignar ' + seleccion().size + ' a ' + tutorNombre }}
        </button>
      </div>
    </div>
  `,
})
export class AgregarEstudiantesPanelComponent implements OnInit {
  private _svc = inject(TutoriaService);

  @Input() tutorId!: string;
  @Input() tutorNombre = '';
  @Input() cupoMaximo = 0;
  @Input() estudiantesActuales = 0;

  @Output() asignado = new EventEmitter<void>();
  @Output() cerrar = new EventEmitter<void>();

  protected buscar = new FormControl('');
  protected resultados = signal<EstudianteAsignable[]>([]);
  protected loading = signal(false);
  protected saving = signal(false);
  protected seleccion = signal<Map<string, EstudianteAsignable>>(new Map());
  protected msg = signal<string | null>(null);
  protected error = signal(false);

  protected disponibles = computed(() => Math.max(0, this.cupoMaximo - this.estudiantesActuales - this.seleccion().size));
  protected excede = computed(() => this.estudiantesActuales + this.seleccion().size > this.cupoMaximo);

  ngOnInit(): void {
    this.buscar.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((q) => {
        this.loading.set(true);
        return this._svc.estudiantes$(undefined, undefined, 'false', q ?? '', 0, 10);
      }),
    ).subscribe({
      next: (res: any) => {
        this.loading.set(false);
        const sel = this.seleccion();
        this.resultados.set((res?.data?.content ?? []).filter((e: EstudianteAsignable) => !sel.has(e.estudianteId)));
      },
      error: () => { this.loading.set(false); this.resultados.set([]); },
    });
  }

  agregar(e: EstudianteAsignable): void {
    if (this.disponibles() <= 0) return;
    const m = new Map(this.seleccion());
    m.set(e.estudianteId, e);
    this.seleccion.set(m);
    this.resultados.set(this.resultados().filter((r) => r.estudianteId !== e.estudianteId));
  }

  quitar(e: EstudianteAsignable): void {
    const m = new Map(this.seleccion());
    m.delete(e.estudianteId);
    this.seleccion.set(m);
    this.resultados.set([e, ...this.resultados()]);
  }

  asignar(): void {
    if (!this.seleccion().size) return;
    this.saving.set(true);
    this.msg.set(null);
    this._svc.asignarEnBloque$(this.tutorId, [...this.seleccion().keys()]).subscribe({
      next: () => { this.saving.set(false); this.asignado.emit(); },
      error: (err) => {
        this.saving.set(false);
        this.error.set(true);
        this.msg.set(err?.error?.message || err?.error?.error || 'No se pudo asignar');
      },
    });
  }
}
