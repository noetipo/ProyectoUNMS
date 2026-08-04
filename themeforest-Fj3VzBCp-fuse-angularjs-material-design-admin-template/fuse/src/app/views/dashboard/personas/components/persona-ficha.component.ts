import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import {
  FormBuilder, FormGroup, ReactiveFormsModule, Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PersonaService } from '../services/persona.service';
import { TutorAutocompleteComponent } from '@/app/views/dashboard/tutorias/components/tutor-autocomplete.component';
import { TutoriaHistorialDialogComponent } from '@/app/views/dashboard/tutorias/components/tutoria-historial-dialog.component';
import { TutorCombo } from '@/app/views/dashboard/tutorias/models/tutoria.model';
import {
  AgregarPerfilDocenteRequest, CATEGORIAS_DOCENTE, CONDICIONES_DOCENTE,
  CONDICIONES_ESTUDIANTE, ESTADOS_CIVILES, FINANCIAMIENTOS, GRADOS_ACADEMICOS,
  PersonaResponse, PROCEDENCIAS, ProgramaPosgrado, SEXOS, TIPOS_DOCUMENTO,
} from '../models/persona.model';
import { CargoService } from '@/app/shared/catalogo/cargo.service';
import { CentroLaboralService } from '@/app/shared/catalogo/centro-laboral.service';
import { PerfilCompletoFormComponent } from '@/app/shared/perfil-completo/perfil-completo-form.component';
import { DocumentoPreviewDialogComponent } from '@/app/shared/perfil-completo/documento-preview-dialog.component';
import { CatalogoItem, DocumentoItem, PerfilCompleto } from '@/app/shared/perfil-completo/perfil-completo.model';
import { abrirBlob } from '@/app/shared/perfil-completo/download.util';

@Component({
  selector: 'app-persona-ficha',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule, MatButtonModule, MatIconModule,
    PerfilCompletoFormComponent, TutorAutocompleteComponent,
  ],
  template: `
    <div class="page">

      <!-- ── Header ── -->
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Académico</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span>Personas</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Editar</span>
          </div>
          <h1 class="page-title">{{ persona()?.nombres }} {{ persona()?.apellidos }}</h1>
        </div>
      </div>

      <!-- ── Content ── -->
      <div class="page-content p-6">
        <div class="max-w-[1360px] mx-auto">
      @if (loading()) {
        <p class="text-sm text-slate-400">Cargando...</p>
      } @else if (persona(); as p) {

        <p class="text-xs text-slate-400 mb-4">Doc: {{ p.numeroDocumento }} · Usuario: {{ p.username }}</p>

        <!-- Perfiles y roles -->
        <div class="flex flex-wrap gap-2 mb-6">
          @for (perfil of p.perfiles; track perfil) {
            <span class="rounded-full bg-blue-50 text-blue-700 text-xs px-3 py-1">{{ perfil }}</span>
          }
          @for (rol of p.roles; track rol) {
            <span class="rounded-full bg-slate-100 text-slate-600 text-xs px-3 py-1">rol: {{ rol }}</span>
          }
        </div>

        @if (editOk()) {
          <div class="mb-4 rounded-lg bg-emerald-50 border border-emerald-200 px-4 py-2 text-sm text-emerald-700">{{ editOk() }}</div>
        }
        @if (editError()) {
          <div class="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-2 text-sm text-red-700">{{ editError() }}</div>
        }

        <!-- ── Edición de datos ── -->
        <form [formGroup]="editForm" (ngSubmit)="guardar()" class="space-y-6">

          <!-- Datos personales -->
          <section class="form-card" formGroupName="persona">
            <h2 class="form-card__title mb-4">Datos personales</h2>
            <div class="form-grid">
              <div>
                <label class="form-label">Tipo de documento <span class="form-required">*</span></label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <mat-select formControlName="tipoDocumento">
                    @for (t of tiposDocumento; track t) { <mat-option [value]="t">{{ t }}</mat-option> }
                  </mat-select>
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Número de documento <span class="form-required">*</span></label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput formControlName="numeroDocumento" />
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Apellido paterno <span class="form-required">*</span></label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput formControlName="apellidoPaterno" />
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Apellido materno</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput formControlName="apellidoMaterno" />
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Nombres <span class="form-required">*</span></label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput formControlName="nombres" />
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Sexo</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <mat-select formControlName="sexo">
                    @for (s of sexos; track s) { <mat-option [value]="s">{{ s }}</mat-option> }
                  </mat-select>
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Fecha de nacimiento</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput type="date" formControlName="fechaNacimiento" />
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Estado civil</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <mat-select formControlName="estadoCivil">
                    @for (e of estadosCiviles; track e) { <mat-option [value]="e">{{ e }}</mat-option> }
                  </mat-select>
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Procedencia</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <mat-select formControlName="procedencia">
                    @for (pr of procedencias; track pr) { <mat-option [value]="pr">{{ pr }}</mat-option> }
                  </mat-select>
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Nacionalidad</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput formControlName="nacionalidad" />
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Email personal</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput formControlName="emailPersonal" />
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">Celular</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput formControlName="celular" />
                </mat-form-field>
              </div>
              <div>
                <label class="form-label">ORCID</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput formControlName="orcid" />
                </mat-form-field>
              </div>
              <div class="flex items-center">
                <label class="flex items-center gap-2 text-sm text-slate-600">
                  <mat-slide-toggle formControlName="discapacidad" /> Discapacidad
                </label>
              </div>
            </div>
          </section>

          <!-- Perfil estudiante (editable si existe) -->
          @if (p.estudiante) {
            <section class="form-card" formGroupName="estudiante">
              <h2 class="form-card__title mb-4">Perfil estudiante</h2>
              <div class="form-grid">
                <div>
                  <label class="form-label">Programa de posgrado</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select formControlName="programaId">
                      @for (pr of programas(); track pr.id) {
                        <mat-option [value]="pr.id">{{ pr.nombre }} ({{ pr.nivel }})</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Código de matrícula</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput formControlName="codMatricula" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Email institucional</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput formControlName="emailInstitucional" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Año de ingreso</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput type="number" formControlName="anioIngreso" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Condición</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select formControlName="condicion">
                      @for (c of condicionesEstudiante; track c) { <mat-option [value]="c">{{ c }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Financiamiento</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select formControlName="financiamiento">
                      @for (f of financiamientos; track f) { <mat-option [value]="f">{{ f }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div class="sm:col-span-2 lg:col-span-5">
                  <label class="form-label">Observaciones</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput formControlName="observaciones" />
                  </mat-form-field>
                </div>

                <!-- Tutor académico -->
                <div class="sm:col-span-2 lg:col-span-5 border-t border-slate-100 pt-3 mt-1">
                  <span class="form-label">Tutor académico</span>
                  @if (p.estudiante.tutorNombre) {
                    <div class="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 mb-2 text-sm">
                      <span class="text-slate-700"><span class="text-slate-400">Actual:</span> {{ p.estudiante.tutorNombre }}</span>
                      <button type="button" mat-button class="!text-xs !text-blue-600" (click)="verHistorial()">Ver historial</button>
                    </div>
                  } @else {
                    <p class="text-xs text-slate-400 mb-1">Sin tutor asignado.</p>
                  }
                  <app-tutor-autocomplete
                    [placeholder]="p.estudiante.tutorNombre ? 'Cambiar tutor…' : 'Asignar tutor…'"
                    (selected)="onTutorSelected($event)" />
                  <p class="text-[11px] text-amber-600 mt-1">
                    Al cambiar el tutor se cierra la tutoría actual y se registra la nueva (queda en el historial).
                  </p>
                </div>
              </div>
            </section>
          }

          <!-- Perfil docente (editable si existe) -->
          @if (p.docente) {
            <section class="form-card" formGroupName="docente">
              <h2 class="form-card__title mb-4">Perfil docente</h2>
              <div class="form-grid">
                <div>
                  <label class="form-label">Grado académico</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select formControlName="gradoAcademico">
                      @for (g of gradosAcademicos; track g) { <mat-option [value]="g">{{ g }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Categoría</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select formControlName="categoria">
                      @for (c of categoriasDocente; track c) { <mat-option [value]="c">{{ c }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Condición</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select formControlName="condicion">
                      @for (c of condicionesDocente; track c) { <mat-option [value]="c">{{ c }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Email institucional</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput formControlName="emailInstitucional" />
                  </mat-form-field>
                </div>
              </div>
            </section>
          }

          <div class="flex justify-end">
            <button type="submit" mat-flat-button color="primary" class="!rounded-lg !px-5 !h-9 !text-sm !font-medium" [disabled]="saving()">
              {{ saving() ? 'Guardando...' : 'Guardar cambios' }}
            </button>
          </div>
        </form>

        <!-- Agregar perfil docente (solo si aún no tiene) -->
        @if (!p.docente) {
          <section class="form-card mt-6 border-dashed">
            <h2 class="form-card__title mb-4">Agregar perfil docente</h2>

            @if (errorMsg()) {
              <div class="mb-3 rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
                {{ errorMsg() }}
              </div>
            }

            <form [formGroup]="form" (ngSubmit)="agregar()">
              <div class="form-grid">
                <div>
                  <label class="form-label">Grado académico <span class="form-required">*</span></label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select formControlName="gradoAcademico">
                      @for (g of gradosAcademicos; track g) { <mat-option [value]="g">{{ g }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Categoría</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select formControlName="categoria">
                      @for (c of categoriasDocente; track c) { <mat-option [value]="c">{{ c }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Condición</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select formControlName="condicion">
                      @for (c of condicionesDocente; track c) { <mat-option [value]="c">{{ c }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Código de sistema</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput formControlName="codigoSistema" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label">Email institucional</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput formControlName="emailInstitucional" />
                  </mat-form-field>
                </div>
              </div>
              <div class="flex justify-end mt-3">
                <button type="submit" mat-flat-button color="primary" class="!rounded-lg !px-5 !h-9 !text-sm !font-medium" [disabled]="savingDocente()">
                  {{ savingDocente() ? 'Guardando...' : 'Agregar perfil docente' }}
                </button>
              </div>
            </form>
          </section>
        }

        <!-- Perfil completo: historiales + documentos -->
        <section class="form-card mt-6">
          <h2 class="form-card__title mb-4">Cargos, centros laborales y documentos</h2>

          @if (perfilOk()) {
            <div class="mb-3 rounded-lg bg-emerald-50 border border-emerald-200 px-3 py-2 text-sm text-emerald-700">{{ perfilOk() }}</div>
          }
          @if (perfilError()) {
            <div class="mb-3 rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">{{ perfilError() }}</div>
          }

          <app-perfil-completo-form
            [cargos]="cargos()" [centros]="centros()" [value]="perfil()" [saving]="savingPerfil()"
            (save)="guardarPerfil($event)" (download)="descargar($event)"
            (previewExisting)="previsualizarExistente($event)" />
        </section>

      } @else {
        <p class="text-sm text-red-600">No se encontró la persona.</p>
      }
        </div>
      </div>
    </div>
  `,
})
export class PersonaFichaComponent implements OnInit {
  private _route = inject(ActivatedRoute);
  private _router = inject(Router);
  private _service = inject(PersonaService);
  private _cargoService = inject(CargoService);
  private _centroService = inject(CentroLaboralService);
  private _fb = inject(FormBuilder);
  private _dialog = inject(MatDialog);

  protected readonly tiposDocumento = TIPOS_DOCUMENTO;
  protected readonly sexos = SEXOS;
  protected readonly estadosCiviles = ESTADOS_CIVILES;
  protected readonly procedencias = PROCEDENCIAS;
  protected readonly condicionesEstudiante = CONDICIONES_ESTUDIANTE;
  protected readonly financiamientos = FINANCIAMIENTOS;
  protected readonly gradosAcademicos = GRADOS_ACADEMICOS;
  protected readonly categoriasDocente = CATEGORIAS_DOCENTE;
  protected readonly condicionesDocente = CONDICIONES_DOCENTE;

  protected persona = signal<PersonaResponse | null>(null);
  protected programas = signal<ProgramaPosgrado[]>([]);
  protected loading = signal(true);
  protected saving = signal(false);          // guardar edición
  protected savingDocente = signal(false);   // agregar perfil docente
  protected editOk = signal<string | null>(null);
  protected editError = signal<string | null>(null);
  protected errorMsg = signal<string | null>(null);

  // Perfil completo (historiales + documentos)
  protected cargos = signal<CatalogoItem[]>([]);
  protected centros = signal<CatalogoItem[]>([]);
  protected perfil = signal<PerfilCompleto | null>(null);
  protected savingPerfil = signal(false);
  protected perfilOk = signal<string | null>(null);
  protected perfilError = signal<string | null>(null);

  private personaId!: string;
  editForm!: FormGroup;   // persona + estudiante + docente
  form!: FormGroup;       // agregar perfil docente

  ngOnInit(): void {
    this.editForm = this._fb.group({
      persona: this._fb.group({
        tipoDocumento: ['DNI', Validators.required],
        numeroDocumento: ['', Validators.required],
        apellidoPaterno: ['', Validators.required],
        apellidoMaterno: [''],
        nombres: ['', Validators.required],
        sexo: [''],
        fechaNacimiento: [''],
        estadoCivil: [''],
        nacionalidad: [''],
        procedencia: [''],
        emailPersonal: ['', Validators.email],
        celular: [''],
        orcid: [''],
        discapacidad: [false],
      }),
      estudiante: this._fb.group({
        codMatricula: [''],
        emailInstitucional: ['', Validators.email],
        anioIngreso: [null],
        programaId: [''],
        condicion: [''],
        financiamiento: [''],
        observaciones: [''],
        tutorId: [''],
      }),
      docente: this._fb.group({
        emailInstitucional: ['', Validators.email],
        gradoAcademico: [''],
        categoria: [''],
        condicion: [''],
      }),
    });

    this.form = this._fb.group({
      codigoSistema: [''],
      emailInstitucional: ['', Validators.email],
      gradoAcademico: ['', Validators.required],
      categoria: [''],
      condicion: [''],
    });

    this.personaId = this._route.snapshot.paramMap.get('id') ?? '';
    this._service.listProgramas$().subscribe({
      next: (res) => this.programas.set(res?.data ?? res ?? []),
      error: () => this.programas.set([]),
    });
    this.cargar();
    this.cargarCatalogos();
    this.cargarPerfil();
  }

  cargar(): void {
    this.loading.set(true);
    this._service.obtener$(this.personaId).subscribe({
      next: (res) => {
        const p: PersonaResponse | null = res?.data ?? res ?? null;
        this.persona.set(p);
        if (p) { this.prefill(p); }
        this.loading.set(false);
      },
      error: () => {
        this.persona.set(null);
        this.loading.set(false);
      },
    });
  }

  private prefill(p: PersonaResponse): void {
    this.editForm.get('persona')!.patchValue({
      tipoDocumento: p.tipoDocumento ?? 'DNI',
      numeroDocumento: p.numeroDocumento ?? '',
      apellidoPaterno: p.apellidoPaterno ?? '',
      apellidoMaterno: p.apellidoMaterno ?? '',
      nombres: p.nombres ?? '',
      sexo: p.sexo ?? '',
      fechaNacimiento: p.fechaNacimiento ?? '',
      estadoCivil: p.estadoCivil ?? '',
      nacionalidad: p.nacionalidad ?? '',
      procedencia: p.procedencia ?? '',
      emailPersonal: p.emailPersonal ?? '',
      celular: p.celular ?? '',
      orcid: p.orcid ?? '',
      discapacidad: p.discapacidad ?? false,
    });
    if (p.estudiante) {
      this.editForm.get('estudiante')!.patchValue({
        codMatricula: p.estudiante.codMatricula ?? '',
        emailInstitucional: p.estudiante.emailInstitucional ?? '',
        anioIngreso: p.estudiante.anioIngreso ?? null,
        programaId: p.estudiante.programaId ?? '',
        condicion: p.estudiante.condicion ?? '',
        financiamiento: p.estudiante.financiamiento ?? '',
        observaciones: p.estudiante.observaciones ?? '',
        tutorId: p.estudiante.tutorId ?? '',
      });
    }
    if (p.docente) {
      this.editForm.get('docente')!.patchValue({
        emailInstitucional: p.docente.emailInstitucional ?? '',
        gradoAcademico: p.docente.gradoAcademico ?? '',
        categoria: p.docente.categoria ?? '',
        condicion: p.docente.condicion ?? '',
      });
    }
  }

  guardar(): void {
    this.editOk.set(null);
    this.editError.set(null);
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      this.editError.set('Revisa los campos obligatorios.');
      return;
    }
    const raw = this.editForm.getRawValue();
    const body: any = this._clean(raw.persona);
    const p = this.persona();
    if (p?.estudiante) { body.estudiante = this._clean(raw.estudiante); }
    if (p?.docente) { body.docente = this._clean(raw.docente); }

    this.saving.set(true);
    this._service.actualizar$(this.personaId, body).subscribe({
      next: () => {
        this.saving.set(false);
        // Vuelve al reporte de personas (ordenado por más reciente) para validar el cambio.
        this._router.navigate(['/admin/personas'], { state: { registeredId: this.personaId } });
      },
      error: (err) => {
        this.saving.set(false);
        this.editError.set(err?.error?.message || err?.error?.error || 'No se pudieron guardar los cambios');
      },
    });
  }

  onTutorSelected(t: TutorCombo | null): void {
    this.editForm.get('estudiante.tutorId')?.setValue(t?.id ?? '');
  }

  verHistorial(): void {
    this._dialog.open(TutoriaHistorialDialogComponent, {
      width: '520px', panelClass: ['dialog-rounded', 'dialog-anim'],
      data: { estudianteId: this.personaId, nombre: `${this.persona()?.nombres ?? ''} ${this.persona()?.apellidos ?? ''}`.trim() },
    });
  }

  agregar(): void {
    this.errorMsg.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const request = this._clean(this.form.value) as AgregarPerfilDocenteRequest;

    this.savingDocente.set(true);
    this._service.agregarPerfilDocente$(this.personaId, request).subscribe({
      next: () => {
        this.savingDocente.set(false);
        this.cargar();
      },
      error: (err) => {
        this.savingDocente.set(false);
        this.errorMsg.set(err?.error?.message || err?.error?.error || 'No se pudo agregar el perfil docente');
      },
    });
  }

  private cargarCatalogos(): void {
    forkJoin({
      cargos: this._cargoService.listarTodos$(),
      centros: this._centroService.listarTodos$(),
    }).subscribe((res: any) => {
      this.cargos.set(res.cargos?.data?.content ?? res.cargos?.content ?? []);
      this.centros.set(res.centros?.data?.content ?? res.centros?.content ?? []);
    });
  }

  private cargarPerfil(): void {
    this._service.obtenerPerfilCompleto$(this.personaId).subscribe({
      next: (res) => this.perfil.set(res?.data ?? res ?? null),
      error: () => this.perfil.set(null),
    });
  }

  guardarPerfil(formData: FormData): void {
    this.perfilOk.set(null);
    this.perfilError.set(null);
    this.savingPerfil.set(true);
    this._service.guardarPerfilCompleto$(this.personaId, formData).subscribe({
      next: () => {
        this.savingPerfil.set(false);
        // Igual que al guardar datos: vuelve al reporte de personas.
        this._router.navigate(['/admin/personas'], { state: { registeredId: this.personaId } });
      },
      error: (err) => {
        this.savingPerfil.set(false);
        this.perfilError.set(err?.error?.message || err?.error?.error || 'No se pudo guardar el perfil completo');
      },
    });
  }

  descargar(doc: DocumentoItem): void {
    this._service.descargarDocumento$(this.personaId, doc.id).subscribe({
      next: (blob) => abrirBlob(blob, doc.nombreOriginal),
      error: () => this.perfilError.set('No se pudo descargar el documento'),
    });
  }

  /** Previsualiza (in-app, sin descargar) un documento ya cargado en el backend. */
  previsualizarExistente(doc: DocumentoItem): void {
    this.perfilError.set(null);
    this._service.descargarDocumento$(this.personaId, doc.id).subscribe({
      next: (blob) => {
        const nombre = doc.nombreOriginal || 'Documento';
        const tipo = doc.contentType || blob.type || '';
        const esPdf = tipo.includes('pdf') || nombre.toLowerCase().endsWith('.pdf');
        // Reconstruye el blob con su content-type real para que el visor lo renderice.
        const url = URL.createObjectURL(new Blob([blob], { type: tipo || 'application/octet-stream' }));
        this._dialog.open(DocumentoPreviewDialogComponent, {
          panelClass: ['dialog-anim'],
          autoFocus: false,
          maxWidth: '92vw',
          data: { nombre, url, esPdf, mime: tipo },
        }).afterClosed().subscribe(() => setTimeout(() => URL.revokeObjectURL(url), 1000));
      },
      error: () => this.perfilError.set('No se pudo previsualizar el documento'),
    });
  }

  /** Elimina strings vacíos para no enviar campos en blanco. */
  private _clean<T extends Record<string, any>>(obj: T): T {
    const out: Record<string, any> = {};
    Object.keys(obj).forEach((k) => {
      const v = obj[k];
      if (v !== '' && v !== null && v !== undefined) {
        out[k] = v;
      }
    });
    return out as T;
  }
}