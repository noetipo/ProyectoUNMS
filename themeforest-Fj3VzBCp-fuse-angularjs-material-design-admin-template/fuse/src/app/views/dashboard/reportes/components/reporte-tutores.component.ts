import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime } from 'rxjs';
import { PersonaService } from '@/app/views/dashboard/personas/services/persona.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { OauthService } from '@/app/providers/services/oauth/oauth.service';
import { TutoriaService } from '@/app/views/dashboard/tutorias/services/tutoria.service';
import { TutorAutocompleteComponent } from '@/app/views/dashboard/tutorias/components/tutor-autocomplete.component';
import { TutorCombo } from '@/app/views/dashboard/tutorias/models/tutoria.model';
import { ReporteTutoresService } from '../services/reporte-tutores.service';
import { descargarBlob } from '../download.util';
import { EstudianteSinTutor, ReporteResumen, ReporteTutor, TutorEstudiante } from '../models/reporte-tutores.model';
import { AgregarEstudiantesPanelComponent } from './agregar-estudiantes-panel.component';

const ROLES_GESTION = ['ADMIN', 'SECRETARIA', 'COORDINADOR', 'COORD_PROG', 'PERS_ADMIN'];

interface DetalleTutor { loading: boolean; rows: TutorEstudiante[] }

@Component({
  selector: 'app-reporte-tutores',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatExpansionModule, MatCheckboxModule, PaginationControlsComponent,
    AgregarEstudiantesPanelComponent, TutorAutocompleteComponent,
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Gestión Posgrado</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Reporte de tutores</span>
          </div>
          <h1 class="page-title">Reporte de tutores</h1>
        </div>
        <div class="flex gap-2">
          <button type="button" class="btn-ghost" [disabled]="exportando()" (click)="exportar('pdf')">
            <mat-icon svgIcon="file-text" class="size-3.5" /> PDF
          </button>
          <button type="button" class="btn-dark" [disabled]="exportando()" (click)="exportar('xlsx')">
            <mat-icon svgIcon="download" class="size-3.5" /> Excel
          </button>
        </div>
      </div>

      <div class="page-content p-6 space-y-4">

        <!-- Filtros -->
        <form [formGroup]="filterForm" class="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label class="form-label">Buscar tutor</label>
            <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
              <input matInput formControlName="buscar" placeholder="Nombre o apellido…" />
            </mat-form-field>
          </div>
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
        </form>

        <!-- Resumen -->
        @if (resumen(); as r) {
          <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <div class="rounded-xl border border-slate-100 p-4">
              <p class="text-2xl font-bold text-slate-800">{{ r.tutores }}</p>
              <p class="text-xs text-slate-400">Tutores</p>
            </div>
            <div class="rounded-xl border border-slate-100 p-4">
              <p class="text-2xl font-bold text-emerald-600">{{ r.estudiantesConTutor }}</p>
              <p class="text-xs text-slate-400">Estudiantes con tutor</p>
            </div>
            <div class="rounded-xl border border-slate-100 p-4">
              <p class="text-2xl font-bold text-amber-600">{{ r.estudiantesSinTutor }}</p>
              <p class="text-xs text-slate-400">Estudiantes sin tutor</p>
            </div>
            <div class="rounded-xl border border-slate-100 p-4">
              <p class="text-2xl font-bold text-slate-800">{{ r.promedioPorTutor }}</p>
              <p class="text-xs text-slate-400">Promedio por tutor</p>
            </div>
          </div>
        }

        <!-- Tabs -->
        <div class="flex gap-1 border-b border-slate-100">
          <button type="button" class="px-4 py-2 text-sm font-medium"
            [class.text-slate-800]="tab() === 'tutores'" [class.border-b-2]="tab() === 'tutores'"
            [class.border-slate-800]="tab() === 'tutores'" [class.text-slate-400]="tab() !== 'tutores'"
            (click)="setTab('tutores')">Tutores</button>
          <button type="button" class="px-4 py-2 text-sm font-medium"
            [class.text-slate-800]="tab() === 'sin'" [class.border-b-2]="tab() === 'sin'"
            [class.border-slate-800]="tab() === 'sin'" [class.text-slate-400]="tab() !== 'sin'"
            (click)="setTab('sin')">Sin tutor</button>
        </div>

        @if (tab() === 'tutores') {
          <p class="text-[11px] text-slate-400">{{ rangoTexto(total()) }}</p>
          @if (loading()) {
            <p class="text-sm text-slate-400">Cargando tutores…</p>
          } @else if (!tutores().length) {
            <div class="table-empty"><mat-icon svgIcon="users" class="size-10 text-slate-200" /><p class="table-empty__text">Sin resultados</p></div>
          } @else {
            <mat-accordion class="block space-y-2">
              @for (t of tutores(); track t.id) {
                <mat-expansion-panel (opened)="onOpen(t)" class="!rounded-xl !shadow-none !border !border-slate-100">
                  <mat-expansion-panel-header>
                    <mat-panel-title class="!flex !items-center !gap-3">
                      <span class="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-slate-600 text-xs font-semibold">{{ iniciales(t) }}</span>
                      <span class="font-medium text-slate-700">{{ t.apellidos }}, {{ t.nombres }}</span>
                      @if (t.gradoAcademico) { <span class="text-xs text-slate-400">{{ t.gradoAcademico }}</span> }
                    </mat-panel-title>
                    <mat-panel-description class="!flex !items-center !justify-end !gap-2">
                      <span class="text-xs text-slate-400">{{ t.programas }} programa(s)</span>
                      <span class="text-[11px] font-medium px-2 py-0.5 rounded-full"
                        [class.bg-rose-50]="t.cupoLleno" [class.text-rose-600]="t.cupoLleno"
                        [class.bg-slate-100]="!t.cupoLleno" [class.text-slate-600]="!t.cupoLleno">
                        {{ t.estudiantes }}/{{ t.cupoMaximo }}{{ t.cupoLleno ? ' · lleno' : '' }}
                      </span>
                    </mat-panel-description>
                  </mat-expansion-panel-header>

                  @if (puedeGestionar) {
                    <div class="flex items-center justify-end mb-2">
                      <button type="button" mat-stroked-button class="!h-8 !text-xs" (click)="togglePanel(t)">
                        <mat-icon [svgIcon]="panelTutor() === t.id ? 'x' : 'plus'" class="!size-3.5" />
                        {{ panelTutor() === t.id ? 'Cerrar' : 'Agregar estudiantes' }}
                      </button>
                    </div>
                    @if (panelTutor() === t.id) {
                      <app-agregar-estudiantes-panel
                        [tutorId]="t.id" [tutorNombre]="t.apellidos + ', ' + t.nombres"
                        [cupoMaximo]="t.cupoMaximo" [estudiantesActuales]="t.estudiantes"
                        (asignado)="onAsignado(t)" (cerrar)="panelTutor.set(null)" />
                    }
                  }

                  @if (detalle().get(t.id); as det) {
                    @if (det.loading) {
                      <p class="text-sm text-slate-400 py-2">Cargando estudiantes…</p>
                    } @else if (!det.rows.length) {
                      <p class="text-sm text-slate-400 py-2">Sin estudiantes.</p>
                    } @else {
                      <table class="data-table">
                        <thead><tr><th>Estudiante</th><th>Código</th><th>Matrícula</th><th>Programa</th><th>Inicio</th>
                          @if (puedeGestionar) { <th class="w-12 text-right"></th> }
                        </tr></thead>
                        <tbody>
                          @for (e of det.rows; track e.estudianteId) {
                            <tr>
                              <td class="text-slate-700">{{ e.apellidos }}, {{ e.nombres }}</td>
                              <td class="font-mono text-[11px] text-slate-400">{{ e.codigoSistema ?? '—' }}</td>
                              <td class="text-slate-500 text-sm">{{ e.codMatricula ?? '—' }}</td>
                              <td class="text-slate-500 text-sm">{{ e.programaNombre ?? '—' }}</td>
                              <td class="text-slate-400 text-sm">{{ e.fechaInicio | date:'dd/MM/yyyy' }}</td>
                              @if (puedeGestionar) {
                                <td class="text-right">
                                  <button mat-icon-button class="!w-7 !h-7" title="Quitar" (click)="onQuitar(t, e)">
                                    <mat-icon svgIcon="x" class="text-rose-400 size-3.5" />
                                  </button>
                                </td>
                              }
                            </tr>
                          }
                        </tbody>
                      </table>
                    }
                  }
                </mat-expansion-panel>
              }
            </mat-accordion>

            <pagination-controls [totalItems]="total()" [itemsPerPage]="size()" [currentPage]="page()"
              (paginationChange)="onPage($event)" />
          }
        } @else {
          <!-- Sin tutor -->
          <p class="text-[11px] text-slate-400">{{ sinTotal() }} estudiante(s) sin tutor · {{ selSin().size }} seleccionado(s)</p>

          @if (msgSin()) {
            <div class="rounded-lg px-3 py-1.5 text-xs"
              [class.bg-emerald-50]="!errorSin()" [class.text-emerald-700]="!errorSin()"
              [class.bg-red-50]="errorSin()" [class.text-red-700]="errorSin()">{{ msgSin() }}</div>
          }

          @if (puedeGestionar && selSin().size > 0) {
            <div class="rounded-xl border border-slate-200 bg-slate-50/60 p-3 grid grid-cols-1 sm:grid-cols-[1fr_auto] gap-3 items-end">
              <div>
                <label class="form-label">Asignar {{ selSin().size }} estudiante(s) a un docente</label>
                <app-tutor-autocomplete placeholder="Buscar docente (cualquiera)…" (selected)="onTutorSinSelected($event)" />
                @if (tutorSin(); as t) {
                  <p class="text-xs mt-1" [class.text-emerald-600]="!excedeSin()" [class.text-rose-600]="excedeSin()">
                    {{ t.apellidos }}, {{ t.nombres }} · {{ cuposDisponiblesSin() }} cupo(s) disponible(s)
                    @if (t.esTutor === false) { <span class="text-amber-600"> · quedará como tutor nuevo</span> }
                    @if (excedeSin()) { <span> · excede el cupo</span> }
                  </p>
                }
              </div>
              <button type="button" mat-flat-button color="primary" class="!h-9 !text-sm !px-4"
                [disabled]="!tutorSin() || asignandoSin() || excedeSin()" (click)="asignarSinTutor()">
                {{ asignandoSin() ? 'Asignando…' : 'Asignar a ' + (tutorSin() ? tutorSin()!.apellidos : 'tutor') }}
              </button>
            </div>
          }

          <div class="overflow-x-auto rounded-xl border border-slate-100">
            <table class="data-table">
              <thead>
                <tr>
                  @if (puedeGestionar) {
                    <th class="w-10">
                      <mat-checkbox [checked]="sinPaginaTodaSel()" [indeterminate]="sinPaginaParcial()"
                        (change)="toggleSinPagina($event.checked)" />
                    </th>
                  }
                  <th>Estudiante</th><th>Código</th><th>Matrícula</th><th>Programa</th><th>Año ingreso</th>
                </tr>
              </thead>
              <tbody>
                @for (e of sinTutor(); track e.estudianteId) {
                  <tr [class.bg-blue-50]="selSin().has(e.estudianteId)">
                    @if (puedeGestionar) {
                      <td><mat-checkbox [checked]="selSin().has(e.estudianteId)" (change)="toggleSin(e, $event.checked)" /></td>
                    }
                    <td class="text-slate-700">{{ e.apellidos }}, {{ e.nombres }}</td>
                    <td class="font-mono text-[11px] text-slate-400">{{ e.codigoSistema ?? '—' }}</td>
                    <td class="text-slate-500 text-sm">{{ e.codMatricula ?? '—' }}</td>
                    <td class="text-slate-500 text-sm">{{ e.programaNombre ?? '—' }}</td>
                    <td class="text-slate-500 text-sm">{{ e.anioIngreso ?? '—' }}</td>
                  </tr>
                }
                @empty {
                  <tr><td [attr.colspan]="puedeGestionar ? 6 : 5" class="text-center"><div class="table-empty"><p class="table-empty__text">Todos tienen tutor</p></div></td></tr>
                }
              </tbody>
            </table>
          </div>
          @if (puedeGestionar) { <p class="text-[11px] text-slate-400">"Seleccionar todos" aplica a la página visible.</p> }
          <pagination-controls [totalItems]="sinTotal()" [itemsPerPage]="sinSize()" [currentPage]="sinPage()"
            (paginationChange)="onSinPage($event)" />
        }
      </div>
    </div>
  `,
})
export class ReporteTutoresComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _svc = inject(ReporteTutoresService);
  private _personas = inject(PersonaService);
  private _tutoria = inject(TutoriaService);
  private _confirm = inject(ConfirmDialogService);
  private _oauth = inject(OauthService);

  /** Solo roles de gestión ven/usan las acciones (agregar/quitar). Monitoreo = solo lectura. */
  protected readonly puedeGestionar = (this._oauth.currentUser()?.roleCodes ?? [])
    .some((c) => ROLES_GESTION.includes(c));
  protected panelTutor = signal<string | null>(null);
  /** La otra pestaña quedó desactualizada por una acción; se recarga al entrar. */
  private tutoresDirty = false;
  private sinDirty = false;

  protected facultades = signal<{ id: string; nombre: string }[]>([]);
  protected programas = signal<{ id: string; nombre: string }[]>([]);
  protected resumen = signal<ReporteResumen | null>(null);
  protected tutores = signal<ReporteTutor[]>([]);
  protected total = signal(0);
  protected page = signal(0);
  protected size = signal(20);
  protected loading = signal(false);
  protected detalle = signal<Map<string, DetalleTutor>>(new Map());
  protected tab = signal<'tutores' | 'sin'>('tutores');
  protected exportando = signal(false);

  protected sinTutor = signal<EstudianteSinTutor[]>([]);
  protected sinTotal = signal(0);
  protected sinPage = signal(0);
  protected sinSize = signal(20);

  // Selección + asignación en la pestaña "Sin tutor"
  protected selSin = signal<Map<string, EstudianteSinTutor>>(new Map());
  protected tutorSin = signal<TutorCombo | null>(null);
  protected asignandoSin = signal(false);
  protected msgSin = signal<string | null>(null);
  protected errorSin = signal(false);
  protected cuposDisponiblesSin = computed(() => {
    const t = this.tutorSin();
    return t ? Math.max(0, t.cupoMaximo - t.estudiantesActuales) : 0;
  });
  protected excedeSin = computed(() => {
    const t = this.tutorSin();
    return !!t && this.selSin().size > (t.cupoMaximo - t.estudiantesActuales);
  });

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ buscar: [''], facultadId: [''], programaId: [''] });
    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => {
      this.page.set(0);
      this.sinPage.set(0);
      this.detalle.set(new Map()); // invalida caché al cambiar filtro
      this.cargarResumen();
      this.tab() === 'tutores' ? this.cargarTutores() : this.cargarSinTutor();
    });

    // Cascada Facultad → Programa: al cambiar facultad, resetea programa y recarga sus opciones.
    this.filterForm.get('facultadId')!.valueChanges.subscribe((facId: string) => {
      this.filterForm.get('programaId')!.setValue('', { emitEvent: false });
      this.cargarProgramas(facId);
    });

    this._personas.listFacultades$().subscribe({
      next: (res: any) => this.facultades.set(res?.data?.content ?? res?.data ?? res ?? []),
      error: () => this.facultades.set([]),
    });
    this.cargarProgramas('');

    this.cargarResumen();
    this.cargarTutores();
  }

  private cargarProgramas(facultadId?: string): void {
    this._personas.listProgramas$(facultadId || undefined).subscribe({
      next: (res: any) => this.programas.set(res?.data ?? res ?? []),
      error: () => this.programas.set([]),
    });
  }

  private fac(): string { return this.filterForm.value.facultadId || ''; }
  private prog(): string { return this.filterForm.value.programaId || ''; }
  private buscar(): string { return this.filterForm.value.buscar || ''; }

  cargarResumen(): void {
    this._svc.resumen$(this.fac(), this.prog()).subscribe({
      next: (res: any) => this.resumen.set(res?.data ?? res ?? null),
      error: () => this.resumen.set(null),
    });
  }

  cargarTutores(): void {
    this.loading.set(true);
    this._svc.tutores$(this.buscar(), this.fac(), this.prog(), this.page(), this.size()).subscribe({
      next: (res: any) => {
        const d = res?.data ?? res;
        this.tutores.set(d?.content ?? []);
        this.total.set(d?.total ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  cargarSinTutor(): void {
    this.selSin.set(new Map()); // "seleccionar todos" es por página; se limpia al recargar
    this._svc.sinTutor$(this.fac(), this.prog(), this.buscar(), this.sinPage(), this.sinSize()).subscribe({
      next: (res: any) => {
        const d = res?.data ?? res;
        this.sinTutor.set(d?.content ?? []);
        this.sinTotal.set(d?.total ?? 0);
      },
      error: () => { this.sinTutor.set([]); this.sinTotal.set(0); },
    });
  }

  setTab(t: 'tutores' | 'sin'): void {
    this.tab.set(t);
    // Recarga la pestaña destino si quedó pendiente por una acción en la otra.
    if (t === 'tutores' && this.tutoresDirty) {
      this.tutoresDirty = false;
      this.detalle.set(new Map()); // los conteos cambiaron: invalida la caché de estudiantes
      this.cargarTutores();
    }
    if (t === 'sin' && (this.sinDirty || !this.sinTutor().length)) {
      this.sinDirty = false;
      this.cargarSinTutor();
    }
  }

  /** Lazy load + caché por tutor (una sola petición). */
  onOpen(t: ReporteTutor): void {
    if (this.detalle().has(t.id)) return;
    const m = new Map(this.detalle());
    m.set(t.id, { loading: true, rows: [] });
    this.detalle.set(m);
    this._svc.estudiantesDeTutor$(t.id).subscribe({
      next: (res: any) => {
        const d = res?.data ?? res;
        const mm = new Map(this.detalle());
        mm.set(t.id, { loading: false, rows: d?.content ?? [] });
        this.detalle.set(mm);
      },
      error: () => {
        const mm = new Map(this.detalle());
        mm.set(t.id, { loading: false, rows: [] });
        this.detalle.set(mm);
      },
    });
  }

  togglePanel(t: ReporteTutor): void {
    this.panelTutor.set(this.panelTutor() === t.id ? null : t.id);
  }

  onAsignado(t: ReporteTutor): void {
    this.panelTutor.set(null);
    this.recargarFila(t);
  }

  onQuitar(t: ReporteTutor, e: TutorEstudiante): void {
    this._confirm.confirmDelete({
      title: 'Quitar estudiante',
      message: `¿Quitar a ${e.apellidos}, ${e.nombres} del tutor? Quedará sin tutor (se conserva el historial).`,
    })
      .then(() => this._tutoria.finalizar$(t.id, e.estudianteId).subscribe({ next: () => this.recargarFila(t) }))
      .catch(() => {});
  }

  /** Recarga los estudiantes de la fila y actualiza el conteo/cupo del tutor + resumen. */
  private recargarFila(t: ReporteTutor): void {
    this.sinDirty = true; // agregar/quitar cambia quién está "sin tutor"
    this._svc.estudiantesDeTutor$(t.id).subscribe({
      next: (res: any) => {
        const d = res?.data ?? res;
        const rows = d?.content ?? [];
        const mm = new Map(this.detalle());
        mm.set(t.id, { loading: false, rows });
        this.detalle.set(mm);
        const n = d?.total ?? rows.length;
        this.tutores.set(this.tutores().map((x) =>
          x.id === t.id ? { ...x, estudiantes: n, cupoLleno: n >= x.cupoMaximo } : x));
      },
    });
    this.cargarResumen();
  }

  onPage(e: PaginationEvent): void { this.page.set(e.page); this.size.set(e.size); this.cargarTutores(); }
  onSinPage(e: PaginationEvent): void { this.sinPage.set(e.page); this.sinSize.set(e.size); this.cargarSinTutor(); }

  toggleSin(e: EstudianteSinTutor, checked: boolean): void {
    const m = new Map(this.selSin());
    if (checked) m.set(e.estudianteId, e); else m.delete(e.estudianteId);
    this.selSin.set(m);
  }

  toggleSinPagina(checked: boolean): void {
    const m = new Map(this.selSin());
    for (const e of this.sinTutor()) { if (checked) m.set(e.estudianteId, e); else m.delete(e.estudianteId); }
    this.selSin.set(m);
  }

  sinPaginaTodaSel(): boolean {
    const r = this.sinTutor();
    return r.length > 0 && r.every((e) => this.selSin().has(e.estudianteId));
  }

  sinPaginaParcial(): boolean {
    const r = this.sinTutor();
    const n = r.filter((e) => this.selSin().has(e.estudianteId)).length;
    return n > 0 && n < r.length;
  }

  onTutorSinSelected(t: TutorCombo | null): void { this.tutorSin.set(t); }

  asignarSinTutor(): void {
    const t = this.tutorSin();
    if (!t || !this.selSin().size) return;
    this.asignandoSin.set(true);
    this.msgSin.set(null);
    this._tutoria.asignarEnBloque$(t.id, [...this.selSin().keys()]).subscribe({
      next: (res: any) => {
        this.asignandoSin.set(false);
        this.errorSin.set(false);
        this.msgSin.set(res?.data?.mensaje ?? 'Estudiantes asignados');
        this.tutorSin.set(null);
        this.tutoresDirty = true; // se creó/actualizó un tutor: la pestaña Tutores debe recargar
        this.cargarSinTutor();
        this.cargarResumen();
      },
      error: (err) => {
        this.asignandoSin.set(false);
        this.errorSin.set(true);
        this.msgSin.set(err?.error?.message || err?.error?.error || 'No se pudo asignar');
      },
    });
  }

  exportar(formato: 'pdf' | 'xlsx'): void {
    this.exportando.set(true);
    this._svc.export$(formato, this.buscar(), this.fac(), this.prog()).subscribe({
      next: (blob) => { descargarBlob(blob, `reporte-tutores.${formato}`); this.exportando.set(false); },
      error: () => this.exportando.set(false),
    });
  }

  iniciales(t: ReporteTutor): string {
    const a = (t.apellidos || '').trim().charAt(0);
    const n = (t.nombres || '').trim().charAt(0);
    return (a + n).toUpperCase() || '?';
  }

  rangoTexto(total: number): string {
    if (!total) return 'Sin tutores';
    const desde = this.page() * this.size() + 1;
    const hasta = Math.min(total, (this.page() + 1) * this.size());
    return `Mostrando ${desde}–${hasta} de ${total}`;
  }
}
