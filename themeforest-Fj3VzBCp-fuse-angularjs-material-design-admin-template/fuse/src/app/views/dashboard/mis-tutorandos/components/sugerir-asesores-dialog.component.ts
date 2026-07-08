import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { toSignal } from '@angular/core/rxjs-interop';
import { SugerenciaAsesorService } from '../services/sugerencia-asesor.service';

interface DialogData {
  estudianteId: string;
  estudianteNombre: string;
  lineaNombre?: string;
}

@Component({
  selector: 'app-sugerir-asesores-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <div class="p-5 w-[540px] max-w-full">
      <div class="flex items-start justify-between mb-3">
        <div>
          <h2 class="text-lg font-semibold text-slate-800">Sugerir asesores</h2>
          <p class="text-xs text-slate-400 mt-0.5">Tutorando: {{ data.estudianteNombre }}</p>
          @if (lineaNombre()) {
            <p class="text-xs text-slate-500 mt-0.5">
              Línea del tema: <span class="font-medium text-slate-600">{{ lineaNombre() }}</span>
            </p>
          }
        </div>
        <button mat-icon-button (click)="ref.close(changed)" class="!w-8 !h-8">
          <mat-icon svgIcon="x" class="size-4 text-slate-400" />
        </button>
      </div>

      <!-- Sugerencias actuales -->
      <div class="mb-4">
        <p class="text-xs font-medium text-slate-500 mb-1">Sugeridos actuales</p>
        @if (sugeridos().length) {
          <div class="space-y-1">
            @for (s of sugeridos(); track s.sugerenciaId) {
              <div class="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-1.5">
                <span class="text-sm text-slate-700">
                  {{ s.apellidos }}, {{ s.nombres }}
                  @if (s.gradoAcademico) { <span class="text-xs text-slate-400"> · {{ s.gradoAcademico }}</span> }
                </span>
                <button mat-icon-button class="!w-7 !h-7" (click)="quitar(s.sugerenciaId)" title="Quitar">
                  <mat-icon svgIcon="x" class="text-rose-400 size-3.5" />
                </button>
              </div>
            }
          </div>
        } @else {
          <p class="text-sm text-slate-400">Aún no hay asesores sugeridos.</p>
        }
      </div>

      <!-- Candidatos por línea del tema -->
      <p class="text-xs font-medium text-slate-500 mb-1">Agregar asesor (docentes de la línea del tema)</p>

      @if (cargando()) {
        <p class="text-xs text-slate-400 mt-2">Cargando candidatos…</p>
      } @else if (conLinea() === false) {
        <div class="rounded-lg bg-amber-50 border border-amber-200 px-3 py-2 text-sm text-amber-700 mt-1">
          El tutorando aún no tiene un tema con línea de investigación registrada. El coordinador debe registrar el tema y su línea.
        </div>
      } @else {
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="w-full">
          <mat-icon matPrefix svgIcon="search" class="size-4 text-slate-400" />
          <input matInput [formControl]="buscar" placeholder="Filtrar por nombre o apellido…" />
        </mat-form-field>

        @if (resultados().length) {
          <div class="mt-2 max-h-48 overflow-y-auto space-y-1">
            @for (d of resultados(); track d.id) {
              <div class="flex items-center justify-between rounded-lg hover:bg-slate-50 px-3 py-1.5">
                <span class="text-sm text-slate-700">
                  {{ d.apellidos }}, {{ d.nombres }}
                  @if (d.gradoAcademico) { <span class="text-xs text-slate-400"> · {{ d.gradoAcademico }}</span> }
                </span>
                <button mat-stroked-button class="!h-7 !text-xs !min-w-0 !px-3"
                        [disabled]="yaSugerido(d.id) || saving()" (click)="agregar(d)">
                  {{ yaSugerido(d.id) ? 'Agregado' : 'Agregar' }}
                </button>
              </div>
            }
          </div>
        } @else if (candidatos().length) {
          <p class="text-sm text-slate-400 mt-2">Ningún docente coincide con el filtro.</p>
        } @else {
          <p class="text-sm text-slate-400 mt-2">No hay docentes registrados en esta línea de investigación.</p>
        }
      }

      @if (err()) { <p class="text-xs text-rose-500 mt-2">{{ err() }}</p> }

      <div class="flex justify-end mt-4">
        <button mat-stroked-button (click)="ref.close(changed)">Cerrar</button>
      </div>
    </div>
  `,
})
export class SugerirAsesoresDialogComponent implements OnInit {
  private _svc = inject(SugerenciaAsesorService);
  protected ref = inject(MatDialogRef<SugerirAsesoresDialogComponent>);
  protected data = inject<DialogData>(MAT_DIALOG_DATA);

  protected buscar = new FormControl('');
  private _filtro = toSignal(this.buscar.valueChanges, { initialValue: '' });

  protected sugeridos = signal<any[]>([]);
  protected candidatos = signal<any[]>([]);
  protected conLinea = signal<boolean | null>(null);
  protected lineaNombre = signal<string | null>(null);
  protected cargando = signal(true);
  protected saving = signal(false);
  protected err = signal<string | null>(null);
  protected changed = false;

  /** Filtro cliente sobre los candidatos de la línea. */
  protected resultados = computed(() => {
    const q = (this._filtro() ?? '').trim().toLowerCase();
    const list = this.candidatos();
    if (!q) { return list; }
    return list.filter((d) =>
      `${d.apellidos ?? ''} ${d.nombres ?? ''}`.toLowerCase().includes(q));
  });

  ngOnInit(): void {
    this.cargar();
    this.cargarCandidatos();
  }

  cargar(): void {
    this._svc.listar$(this.data.estudianteId).subscribe({
      next: (res: any) => this.sugeridos.set(res?.data ?? res ?? []),
      error: () => this.sugeridos.set([]),
    });
  }

  cargarCandidatos(): void {
    this.cargando.set(true);
    this._svc.candidatos$(this.data.estudianteId).subscribe({
      next: (res: any) => {
        const d = res?.data ?? res ?? {};
        this.conLinea.set(!!d.conLinea);
        this.lineaNombre.set(d.lineaNombre ?? this.data.lineaNombre ?? null);
        this.candidatos.set(d.docentes ?? []);
        this.cargando.set(false);
      },
      error: () => {
        this.conLinea.set(null);
        this.candidatos.set([]);
        this.cargando.set(false);
        this.err.set('No se pudieron cargar los candidatos.');
      },
    });
  }

  yaSugerido(docenteId: string): boolean {
    return this.sugeridos().some((s) => s.asesorDocenteId === docenteId);
  }

  agregar(d: any): void {
    if (this.yaSugerido(d.id)) return;
    this.saving.set(true); this.err.set(null);
    this._svc.sugerir$(this.data.estudianteId, d.id).subscribe({
      next: () => { this.saving.set(false); this.changed = true; this.cargar(); },
      error: (e) => { this.saving.set(false); this.err.set(e?.error?.message ?? e?.error?.error ?? 'No se pudo sugerir'); },
    });
  }

  quitar(sugerenciaId: string): void {
    this._svc.quitar$(sugerenciaId).subscribe({
      next: () => { this.changed = true; this.cargar(); },
      error: (e) => this.err.set(e?.error?.message ?? 'No se pudo quitar'),
    });
  }
}
