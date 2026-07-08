import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { TutoriaService } from '../services/tutoria.service';
import { TutorCombo } from '../models/tutoria.model';

/**
 * Autocomplete reutilizable de tutores (docentes PROF_TUTOR): busca por texto con
 * debounce, muestra el cupo por opción y bloquea a los llenos. Emite el tutor elegido.
 */
@Component({
  selector: 'app-tutor-autocomplete',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule,
    MatAutocompleteModule, MatIconModule, MatButtonModule,
  ],
  template: `
    <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
      <input matInput [formControl]="ctrl" [matAutocomplete]="auto" [placeholder]="placeholder"
        (focus)="onFocus()" />
      @if (ctrl.value) {
        <button matSuffix mat-icon-button type="button" (click)="clear()" aria-label="Quitar">
          <mat-icon svgIcon="x" class="!size-4 text-slate-400" />
        </button>
      }
      <mat-autocomplete #auto [displayWith]="display" (optionSelected)="onSelect($event.option.value)">
        @if (loading()) {
          <mat-option disabled>Buscando…</mat-option>
        } @else if (!options().length) {
          <mat-option disabled>Sin resultados</mat-option>
        }
        @for (t of options(); track t.id) {
          <mat-option [value]="t" [disabled]="!t.disponible">
            <span class="font-medium">{{ t.apellidos }}, {{ t.nombres }}</span>
            @if (t.gradoAcademico) { <span class="text-xs text-slate-400"> · {{ t.gradoAcademico }}</span> }
            <span class="text-xs"
              [class.text-emerald-600]="t.disponible" [class.text-rose-500]="!t.disponible">
              · {{ t.estudiantesActuales }}/{{ t.cupoMaximo }}{{ t.disponible ? '' : ' · No disponible' }}
            </span>
            @if (t.esTutor === false) { <span class="text-xs text-amber-600"> · tutor nuevo</span> }
          </mat-option>
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
})
export class TutorAutocompleteComponent implements OnInit {
  private _svc = inject(TutoriaService);

  @Input() placeholder = 'Buscar tutor por nombre…';
  /** Valor inicial (edición): objeto con al menos { id, apellidos, nombres }. */
  @Input() set preset(v: Partial<TutorCombo> | null | undefined) {
    if (v && v.id) this.ctrl.setValue(v as TutorCombo, { emitEvent: false });
  }
  @Output() selected = new EventEmitter<TutorCombo | null>();

  protected ctrl = new FormControl<string | TutorCombo>('');
  protected options = signal<TutorCombo[]>([]);
  protected loading = signal(false);

  ngOnInit(): void {
    this.ctrl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((a, b) => this._key(a) === this._key(b)),
      switchMap((v) => {
        if (typeof v !== 'string') return of<any>(null); // se seleccionó un objeto
        this.loading.set(true);
        // catchError DENTRO del switchMap: un error no mata el flujo (seguiría "sin buscar").
        return this._svc.buscarTutores$(v.trim(), 0, 10).pipe(
          catchError(() => of<any>({ data: { content: [] } })),
        );
      }),
    ).subscribe((res: any) => {
      this.loading.set(false);
      if (res === null) return; // opción seleccionada: conserva la selección
      this.options.set(res?.data?.content ?? []);
    });
  }

  /** Al enfocar el campo vacío, carga los primeros tutores para que el panel no aparezca vacío. */
  protected onFocus(): void {
    if (typeof this.ctrl.value === 'string' && !this.options().length && !this.loading()) {
      this.loading.set(true);
      this._svc.buscarTutores$((this.ctrl.value ?? '').trim(), 0, 10).subscribe({
        next: (res: any) => { this.loading.set(false); this.options.set(res?.data?.content ?? []); },
        error: () => { this.loading.set(false); this.options.set([]); },
      });
    }
  }

  private _key(v: string | TutorCombo | null): string {
    return v == null ? '' : (typeof v === 'string' ? v : v.id);
  }

  protected display = (t: string | TutorCombo): string =>
    t && typeof t === 'object' ? `${t.apellidos}, ${t.nombres}` : (t as string) ?? '';

  protected onSelect(t: TutorCombo): void {
    this.selected.emit(t);
  }

  protected clear(): void {
    this.ctrl.setValue('');
    this.options.set([]);
    this.selected.emit(null);
  }
}
