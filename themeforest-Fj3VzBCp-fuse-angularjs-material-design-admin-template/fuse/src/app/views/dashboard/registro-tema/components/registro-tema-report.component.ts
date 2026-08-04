import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs/operators';
import { OauthService } from '@/app/providers/services/oauth/oauth.service';
import { PersonaService } from '@/app/views/dashboard/personas/services/persona.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { AsignarTutorDialogComponent } from '@/app/views/dashboard/tutorias/components/asignar-tutor-dialog.component';
import { RegistroTemaService } from '../services/registro-tema.service';
import { RegistroTemaDialogComponent } from './registro-tema-dialog.component';
import { claseEstadoDerivado, EstudianteTema, etiquetaEstadoDerivado, TemaResumen } from '../models/registro-tema.model';

@Component({
  selector: 'app-registro-tema-report',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatSelectModule,
    MatInputModule, MatButtonModule, MatIconModule, MatDialogModule, PaginationControlsComponent,
    RouterLink,
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Proceso de Tesis</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Registro de tema</span>
          </div>
          <h1 class="page-title">Registro de tema y línea de investigación</h1>
        </div>
        @if (puedeEditar) {
          <!-- Único acceso a la asignación masiva: salió del menú lateral porque la designación
               individual vive en esta misma pantalla. -->
          <a mat-stroked-button class="!h-9 !text-xs !text-slate-600" routerLink="/admin/asignar-tutor"
             title="Asignar un mismo tutor a varios alumnos a la vez">
            <mat-icon svgIcon="users" class="size-3.5 mr-1" /> Asignar tutor en bloque
          </a>
        }
      </div>

      <!-- Resumen -->
      @if (resumen(); as r) {
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
          <div class="rounded-xl border border-slate-100 p-4">
            <p class="text-2xl font-bold text-slate-800">{{ r.total }}</p>
            <p class="text-xs text-slate-400">Total estudiantes</p>
          </div>
          <div class="rounded-xl border border-slate-100 p-4">
            <p class="text-2xl font-bold text-emerald-600">{{ r.conTema }}</p>
            <p class="text-xs text-slate-400">Con tema</p>
          </div>
          <div class="rounded-xl border border-slate-100 p-4">
            <p class="text-2xl font-bold text-rose-500">{{ r.sinTema }}</p>
            <p class="text-xs text-slate-400">Sin tema</p>
          </div>
          <!-- El paso que sigue al registro del tema: designar tutor. -->
          <div class="rounded-xl border p-4" [ngClass]="r.sinTutor ? 'border-amber-200 bg-amber-50/50' : 'border-slate-100'">
            <p class="text-2xl font-bold" [ngClass]="r.sinTutor ? 'text-amber-600' : 'text-emerald-600'">{{ r.sinTutor ?? 0 }}</p>
            <p class="text-xs text-slate-400">Con tema, sin tutor</p>
          </div>
        </div>

        <!-- Indicación explícita: el tema ya está, falta el tutor. -->
        @if (r.sinTutor) {
          <div class="flex flex-wrap items-center gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-2.5 mb-4">
            <mat-icon svgIcon="user-plus" class="size-4 text-amber-600 shrink-0" />
            <p class="text-[12.5px] text-amber-800 min-w-0">
              <b>{{ r.sinTutor }}</b>
              {{ r.sinTutor === 1 ? 'estudiante con tema registrado espera' : 'estudiantes con tema registrado esperan' }}
              la <b>designación de su tutor</b> (Etapa 2).
            </p>
            @if (puedeEditar) {
              <button mat-stroked-button class="!h-7 !text-xs ml-auto !border-amber-300 !text-amber-800"
                      (click)="verPendientesDeTutor()">
                Ver pendientes
              </button>
            }
          </div>
        }
      }

      <!-- Filtros -->
      <div class="page-toolbar">
        <!-- 4 filtros = 4 columnas en escritorio: caben en una sola fila. -->
        <form [formGroup]="filterForm" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 w-full">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-icon matPrefix svgIcon="search" class="size-4 text-slate-400" />
            <input matInput formControlName="buscar" placeholder="Buscar por nombre o código…" />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-select formControlName="facultadId">
              <mat-option [value]="''">Todas las facultades</mat-option>
              @for (f of facultades(); track f.id) { <mat-option [value]="f.id">{{ f.nombre }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-select formControlName="programaId">
              <mat-option [value]="''">Todos los programas</mat-option>
              @for (p of programas(); track p.id) { <mat-option [value]="p.id">{{ p.nombre }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-select formControlName="conTema">
              <mat-option [value]="'false'">Sin tema (requieren acción)</mat-option>
              <mat-option [value]="'true'">Con tema</mat-option>
              <mat-option [value]="''">Todos</mat-option>
            </mat-select>
          </mat-form-field>
        </form>
      </div>

      <div class="page-content">
        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>Estudiante</th>
                <th>Código</th>
                <th>Programa</th>
                <th>Estado de tema</th>
                <th>Tutor</th>
                @if (puedeEditar) { <th class="w-24 text-right">Acción</th> }
              </tr>
            </thead>
            <tbody>
              @for (e of rows(); track e.estudianteId) {
                <tr>
                  <td class="font-medium text-slate-700">{{ e.apellidos }}, {{ e.nombres }}</td>
                  <td class="text-slate-500 text-sm font-mono">{{ e.codigoSistema ?? e.codMatricula ?? '—' }}</td>
                  <td class="text-slate-500 text-sm">{{ e.programaNombre ?? '—' }}</td>
                  <td>
                    <span class="text-[11px] font-medium px-2 py-0.5 rounded-full" [class]="badge(e)">
                      {{ estadoLabel(e) }}
                    </span>
                    @if (e.conTema && e.titulo) {
                      <p class="text-[11px] text-slate-400 mt-0.5 max-w-[280px] truncate" [title]="e.titulo">{{ e.titulo }}</p>
                    }
                  </td>
                  <td>
                    @if (e.tutorNombre) {
                      <span class="text-[12px] text-slate-600">{{ e.tutorNombre }}</span>
                    } @else if (e.conTema) {
                      <span class="text-[11px] font-medium px-2 py-0.5 rounded-full bg-amber-50 text-amber-600">Falta designar</span>
                    } @else {
                      <span class="text-[11px] text-slate-300">—</span>
                    }
                  </td>
                  @if (puedeEditar) {
                    <td class="text-right">
                      <!-- Misma convención que el resto de tablas del sistema: .row-actions con
                           botones de icono 7×7 y tooltip. El color marca la situación: granate =
                           acción pendiente, ámbar = editar lo ya registrado, gris = cambio opcional. -->
                      <div class="row-actions">
                        @if (e.conTema) {
                          <button mat-icon-button class="!w-7 !h-7" title="Editar tema" (click)="abrir(e, true)">
                            <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                          </button>
                          @if (e.tutorNombre) {
                            <button mat-icon-button class="!w-7 !h-7" title="Cambiar tutor" (click)="asignarTutor(e)">
                              <mat-icon svgIcon="user-round-pen" class="text-slate-400 size-3.5" />
                            </button>
                          } @else {
                            <button mat-icon-button class="!w-7 !h-7" title="Asignar tutor (pendiente)" (click)="asignarTutor(e)">
                              <mat-icon svgIcon="user-round-plus" class="text-[#8C1D2E] size-3.5" />
                            </button>
                          }
                        } @else {
                          <button mat-icon-button class="!w-7 !h-7" title="Registrar tema" (click)="abrir(e, false)">
                            <mat-icon svgIcon="file-plus" class="text-[#8C1D2E] size-3.5" />
                          </button>
                        }
                      </div>
                    </td>
                  }
                </tr>
              }
              @empty {
                <tr>
                  <td [attr.colspan]="puedeEditar ? 6 : 5" class="text-center">
                    <div class="table-empty">
                      <mat-icon svgIcon="inbox" class="size-10 text-slate-200" />
                      <p class="table-empty__text">Sin estudiantes</p>
                      <p class="table-empty__subtext">Ajusta los filtros de búsqueda</p>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>

      @if (!loading()) {
        <div class="page-footer">
          <pagination-controls
            [totalItems]="total()" [itemsPerPage]="size()" [currentPage]="page()"
            (paginationChange)="onPage($event)" />
        </div>
      }
    </div>
  `,
})
export class RegistroTemaReportComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _svc = inject(RegistroTemaService);
  private _personas = inject(PersonaService);
  private _dialog = inject(MatDialog);
  private _oauth = inject(OauthService);

  /** Solo coordinadores con edición registran/editan; el resto (admin, secretaría, monitoreo) ve solo lectura. */
  protected readonly puedeEditar = (this._oauth.currentUser()?.roleCodes ?? [])
    .some((c) => c === 'COORDINADOR' || c === 'COORD_PROG');

  protected facultades = signal<{ id: string; nombre: string }[]>([]);
  protected programas = signal<{ id: string; nombre: string }[]>([]);
  protected resumen = signal<TemaResumen | null>(null);
  protected rows = signal<EstudianteTema[]>([]);
  protected total = signal(0);
  protected page = signal(0);
  protected size = signal(20);
  protected loading = signal(false);

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    // Por defecto "Sin tema": los que requieren acción.
    this.filterForm = this._fb.group({ buscar: [''], facultadId: [''], programaId: [''], conTema: ['false'] });
    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => {
      this.page.set(0);
      this.cargarResumen();
      this.cargar();
    });

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

    this.cargarResumen();
    this.cargar();
  }

  private _cargarProgramas(facultadId?: string): void {
    this._personas.listProgramas$(facultadId || undefined).subscribe({
      next: (res: any) => this.programas.set(res?.data ?? res ?? []),
      error: () => this.programas.set([]),
    });
  }

  private fac(): string { return this.filterForm.value.facultadId || ''; }
  private prog(): string { return this.filterForm.value.programaId || ''; }
  private buscar(): string { return this.filterForm.value.buscar || ''; }
  private conTema(): boolean | null {
    const v = this.filterForm.value.conTema;
    return v === '' ? null : v === 'true';
  }

  cargarResumen(): void {
    this._svc.resumen$(this.fac(), this.prog()).subscribe({
      next: (res: any) => this.resumen.set(res?.data ?? res ?? null),
      error: () => this.resumen.set(null),
    });
  }

  cargar(): void {
    this.loading.set(true);
    this._svc.estudiantes$(this.fac(), this.prog(), this.conTema(), this.buscar(), this.page(), this.size()).subscribe({
      next: (res: any) => {
        const d = res?.data ?? res;
        this.rows.set(d?.content ?? []);
        this.total.set(d?.total ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PaginationEvent): void {
    this.page.set(e.page);
    this.size.set(e.size);
    this.cargar();
  }

  abrir(estudiante: EstudianteTema, editar: boolean): void {
    this._dialog.open(RegistroTemaDialogComponent, { data: { estudiante, editar }, autoFocus: false })
      .afterClosed().subscribe((result) => {
        if (!result) return;
        this.cargarResumen();
        this.cargar();
        // Registrar el tema y designar tutor son pasos consecutivos: si el alumno aún no
        // tiene tutor, se ofrece designarlo de inmediato en vez de mandarlo a otra pantalla.
        if (!editar && !estudiante.tutorNombre) {
          this.asignarTutor(estudiante, true);
        }
      });
  }

  /** Filtra la lista a los que ya tienen tema, donde la columna Tutor marca "Falta designar". */
  verPendientesDeTutor(): void {
    this.filterForm.patchValue({ conTema: 'true' });
  }

  /** @param recienRegistrado true cuando viene encadenado del registro del tema */
  asignarTutor(e: EstudianteTema, recienRegistrado = false): void {
    this._dialog.open(AsignarTutorDialogComponent, {
      data: {
        estudianteId: e.estudianteId,
        estudianteNombre: `${e.apellidos}, ${e.nombres}`,
        tutorActual: e.tutorNombre ?? null,
        recienRegistrado,
      },
      autoFocus: false,
      width: '460px',
    }).afterClosed().subscribe((ok) => {
      if (ok) { this.cargarResumen(); this.cargar(); }
    });
  }

  estadoLabel(e: EstudianteTema): string { return etiquetaEstadoDerivado(e.estadoDerivado ?? (e.conTema ? undefined : 'SIN_TEMA')); }
  badge(e: EstudianteTema): string { return claseEstadoDerivado(e.estadoDerivado ?? (e.conTema ? undefined : 'SIN_TEMA')); }
}
