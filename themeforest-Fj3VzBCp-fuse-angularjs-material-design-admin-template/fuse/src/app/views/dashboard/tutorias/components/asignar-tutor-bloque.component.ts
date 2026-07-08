import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime } from 'rxjs';
import { PersonaService } from '@/app/views/dashboard/personas/services/persona.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { TutoriaService } from '../services/tutoria.service';
import { EstudianteAsignable, ESTADOS_TUTOR, TutorCombo } from '../models/tutoria.model';
import { TutorAutocompleteComponent } from './tutor-autocomplete.component';

@Component({
  selector: 'app-asignar-tutor-bloque',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCheckboxModule, PaginationControlsComponent,
    TutorAutocompleteComponent,
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Gestión Posgrado</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Asignar tutor en bloque</span>
          </div>
          <h1 class="page-title">Asignar tutor en bloque</h1>
        </div>
      </div>

      <div class="page-content p-6 space-y-4">

        @if (msg()) {
          <div class="rounded-lg px-4 py-2 text-sm"
            [class.bg-emerald-50]="!error()" [class.text-emerald-700]="!error()"
            [class.bg-red-50]="error()" [class.text-red-700]="error()">
            {{ msg() }}
          </div>
        }

        <!-- Tutor -->
        <section class="form-section rounded-xl border border-slate-100 p-4">
          <h2 class="text-sm font-semibold text-slate-800 mb-2">Tutor</h2>
          <app-tutor-autocomplete (selected)="onTutorSelected($event)" />
          @if (tutor(); as t) {
            <div class="mt-2 flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-sm">
              <span class="font-medium text-slate-700">{{ t.apellidos }}, {{ t.nombres }}
                @if (t.gradoAcademico) { <span class="text-slate-400">· {{ t.gradoAcademico }}</span> }
              </span>
              <span class="text-xs" [class.text-emerald-600]="!excedeCupo()" [class.text-rose-600]="excedeCupo()">
                Cupo: {{ t.estudiantesActuales }}/{{ t.cupoMaximo }}
                @if (nuevosSeleccionados() > 0) { → quedará {{ cupoDespues() }}/{{ t.cupoMaximo }} }
                @if (excedeCupo()) { · excede el cupo }
              </span>
            </div>
          }
        </section>

        <!-- Filtros -->
        <section class="form-section rounded-xl border border-slate-100 p-4">
          <form [formGroup]="filterForm" class="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <div>
              <label class="form-label">Facultad</label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <mat-select formControlName="facultadId">
                  <mat-option [value]="''">Todas las facultades</mat-option>
                  @for (f of facultades(); track f.id) { <mat-option [value]="f.id">{{ f.nombre }}</mat-option> }
                </mat-select>
              </mat-form-field>
            </div>
            <div>
              <label class="form-label">Programa</label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <mat-select formControlName="programaId">
                  <mat-option [value]="''">Todos los programas</mat-option>
                  @for (p of programas(); track p.id) { <mat-option [value]="p.id">{{ p.nombre }}</mat-option> }
                </mat-select>
              </mat-form-field>
            </div>
            <div>
              <label class="form-label">Estado de tutor</label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <mat-select formControlName="estado">
                  @for (e of estados; track e.value) { <mat-option [value]="e.value">{{ e.label }}</mat-option> }
                </mat-select>
              </mat-form-field>
            </div>
            <div>
              <label class="form-label">Buscar estudiante</label>
              <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                <input matInput formControlName="buscar" placeholder="Nombre o código…" />
              </mat-form-field>
            </div>
          </form>
          <p class="text-[11px] text-slate-400 mt-1">
            {{ total() }} estudiante(s) · {{ seleccion().size }} seleccionado(s)
          </p>
        </section>

        <!-- Tabla -->
        <div class="overflow-x-auto rounded-xl border border-slate-100">
          <table class="data-table">
            <thead>
              <tr>
                <th class="w-10">
                  <mat-checkbox [checked]="paginaTodaSeleccionada()" [indeterminate]="paginaParcial()"
                    (change)="togglePagina($event.checked)" />
                </th>
                <th>Estudiante</th>
                <th>Programa</th>
                <th>Tutor actual</th>
              </tr>
            </thead>
            <tbody>
              @for (e of rows(); track e.estudianteId) {
                <tr [class.bg-amber-50]="e.tutorId">
                  <td><mat-checkbox [checked]="seleccion().has(e.estudianteId)" (change)="toggle(e, $event.checked)" /></td>
                  <td>
                    <p class="font-medium text-slate-700">{{ e.apellidos }}, {{ e.nombres }}</p>
                    <p class="text-[11px] text-slate-400 font-mono">{{ e.codigoSistema ?? '—' }}</p>
                  </td>
                  <td class="text-slate-500 text-sm">{{ e.programaNombre ?? '—' }}</td>
                  <td class="text-sm">
                    @if (e.tutorId) {
                      <span class="text-amber-700">{{ e.tutorNombre }}</span>
                      <span class="text-[11px] text-slate-400"> · se reemplaza</span>
                    } @else { <span class="text-slate-400">Sin tutor</span> }
                  </td>
                </tr>
              }
              @empty {
                <tr><td colspan="4" class="text-center">
                  <div class="table-empty">
                    <mat-icon svgIcon="users" class="size-10 text-slate-200" />
                    <p class="table-empty__text">Sin estudiantes</p>
                    <p class="table-empty__subtext">Ajusta los filtros</p>
                  </div>
                </td></tr>
              }
            </tbody>
          </table>
        </div>

        @if (!loading()) {
          <pagination-controls [totalItems]="total()" [itemsPerPage]="size()" [currentPage]="page()"
            (paginationChange)="onPage($event)" />
        }

        <div class="flex justify-end pt-2">
          <button mat-flat-button color="primary" class="!rounded-lg !px-5 !h-9 !text-sm !font-medium"
            [disabled]="!tutor() || seleccion().size === 0 || excedeCupo() || saving()"
            (click)="asignar()">
            {{ saving() ? 'Asignando…' : botonLabel() }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class AsignarTutorBloqueComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _svc = inject(TutoriaService);
  private _personas = inject(PersonaService);
  private _confirm = inject(ConfirmDialogService);

  protected readonly estados = ESTADOS_TUTOR;
  protected facultades = signal<{ id: string; nombre: string }[]>([]);
  protected programas = signal<{ id: string; nombre: string }[]>([]);
  protected rows = signal<EstudianteAsignable[]>([]);
  protected total = signal(0);
  protected page = signal(0);
  protected size = signal(20);
  protected loading = signal(false);
  protected saving = signal(false);
  protected tutor = signal<TutorCombo | null>(null);
  protected seleccion = signal<Map<string, EstudianteAsignable>>(new Map());
  protected msg = signal<string | null>(null);
  protected error = signal(false);

  protected nuevosSeleccionados = computed(() => {
    const t = this.tutor();
    if (!t) return 0;
    return [...this.seleccion().values()].filter((e) => e.tutorId !== t.id).length;
  });
  protected cupoDespues = computed(() => {
    const t = this.tutor();
    return t ? t.estudiantesActuales + this.nuevosSeleccionados() : 0;
  });
  protected excedeCupo = computed(() => {
    const t = this.tutor();
    return !!t && this.cupoDespues() > t.cupoMaximo;
  });
  protected botonLabel = computed(() => {
    const t = this.tutor();
    const n = this.seleccion().size;
    return t ? `Asignar ${n} a ${t.apellidos}, ${t.nombres}` : `Asignar ${n}`;
  });

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ facultadId: [''], programaId: [''], estado: [''], buscar: [''] });
    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => { this.page.set(0); this.load(); });

    // Cascada Facultad → Programa.
    this.filterForm.get('facultadId')!.valueChanges.subscribe((facId: string) => {
      this.filterForm.get('programaId')!.setValue('', { emitEvent: false });
      this._cargarProgramas(facId);
    });
    this._personas.listFacultades$().subscribe({
      next: (res: any) => this.facultades.set(res?.data?.content ?? res?.data ?? res ?? []),
      error: () => this.facultades.set([]),
    });
    this._cargarProgramas('');
    this.load();
  }

  private _cargarProgramas(facultadId?: string): void {
    this._personas.listProgramas$(facultadId || undefined).subscribe({
      next: (res: any) => this.programas.set(res?.data ?? res ?? []),
      error: () => this.programas.set([]),
    });
  }

  load(): void {
    this.loading.set(true);
    const f = this.filterForm.value;
    this._svc.estudiantes$(f.facultadId, f.programaId, f.estado, f.buscar, this.page(), this.size()).subscribe({
      next: (res: any) => {
        const d = res?.data ?? res;
        this.rows.set(d?.content ?? []);
        this.total.set(d?.total ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onTutorSelected(t: TutorCombo | null): void {
    this.tutor.set(t);
  }

  onPage(e: PaginationEvent): void {
    this.page.set(e.page);
    this.size.set(e.size);
    this.load();
  }

  toggle(e: EstudianteAsignable, checked: boolean): void {
    const m = new Map(this.seleccion());
    if (checked) m.set(e.estudianteId, e); else m.delete(e.estudianteId);
    this.seleccion.set(m);
  }

  togglePagina(checked: boolean): void {
    const m = new Map(this.seleccion());
    for (const e of this.rows()) {
      if (checked) m.set(e.estudianteId, e); else m.delete(e.estudianteId);
    }
    this.seleccion.set(m);
  }

  paginaTodaSeleccionada(): boolean {
    const r = this.rows();
    return r.length > 0 && r.every((e) => this.seleccion().has(e.estudianteId));
  }

  paginaParcial(): boolean {
    const r = this.rows();
    const sel = r.filter((e) => this.seleccion().has(e.estudianteId)).length;
    return sel > 0 && sel < r.length;
  }

  asignar(): void {
    const t = this.tutor();
    if (!t || this.seleccion().size === 0) return;
    const ids = [...this.seleccion().keys()];
    this._confirm.confirmSave({
      title: 'Asignar tutor',
      message: `¿Asignar ${ids.length} estudiante(s) a ${t.apellidos}, ${t.nombres}?`,
    })
      .then(() => {
        this.saving.set(true);
        this._svc.asignarEnBloque$(t.id, ids).subscribe({
          next: (res: any) => {
            this.saving.set(false);
            this.error.set(false);
            this.msg.set(res?.data?.mensaje ?? 'Asignación aplicada');
            this.seleccion.set(new Map());
            this.tutor.set(null);
            this.load();
          },
          error: (err) => {
            this.saving.set(false);
            this.error.set(true);
            this.msg.set(err?.error?.message || err?.error?.error || 'No se pudo asignar');
          },
        });
      })
      .catch(() => {});
  }
}
