import { CommonModule } from '@angular/common';
import { afterNextRender, Component, computed, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import {
  FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PersonaService } from '../services/persona.service';
import { DocumentoPreviewDialogComponent } from '@/app/shared/perfil-completo/documento-preview-dialog.component';
import {
  CATEGORIAS_DOCENTE, CONDICIONES_DOCENTE, CONDICIONES_ESTUDIANTE, CrearPersonaRequest,
  ESTADOS_CIVILES, FacultadItem, FINANCIAMIENTOS, GRADOS_ACADEMICOS, GRADOS_ACADEMICOS_LABELS, ProgramaPosgrado,
  PROCEDENCIAS, SEXOS, TIPOS_DOCUMENTO,
} from '../models/persona.model';
import { CargoService } from '@/app/shared/catalogo/cargo.service';
import { CentroLaboralService } from '@/app/shared/catalogo/centro-laboral.service';
import { LineaInvestigacionService } from '@/app/shared/catalogo/linea-investigacion.service';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { TutorAutocompleteComponent } from '@/app/views/dashboard/tutorias/components/tutor-autocomplete.component';
import { TutorCombo } from '@/app/views/dashboard/tutorias/models/tutoria.model';
import { PerfilCompletoFormComponent } from '@/app/shared/perfil-completo/perfil-completo-form.component';
import { buildPerfilFormData, CatalogoItem, DocumentoItem, PerfilCompleto } from '@/app/shared/perfil-completo/perfil-completo.model';

@Component({
  selector: 'app-persona-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule, MatButtonModule, MatIconModule,
    MatCheckboxModule, MatProgressSpinnerModule, PerfilCompletoFormComponent, TutorAutocompleteComponent,
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
            <span class="breadcrumb__current">{{ editMode() ? 'Editar' : 'Nueva' }}</span>
          </div>
          <h1 class="page-title">{{ editMode() ? 'Editar persona' : 'Registro de persona' }}</h1>
        </div>
        @if (isDev()) {
          <button type="button" class="btn-ghost" (click)="fillSample()">
            <mat-icon svgIcon="wand-sparkles" class="size-3.5" />
            Llenar datos
          </button>
        }
      </div>

      <!-- ── Content ── -->
      <div class="page-content">
        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="mx-auto max-w-[1360px] px-6 py-6 space-y-5">

            @if (errorMsg()) {
              <div role="alert"
                   class="flex items-start gap-2 rounded-xl bg-rose-50 border border-rose-200 px-4 py-3 text-sm text-rose-700">
                <mat-icon svgIcon="circle-alert" class="size-4 mt-0.5 shrink-0" />
                <span>{{ errorMsg() }}</span>
              </div>
            }

            <!-- ── Datos personales ── -->
            <section class="form-card" formGroupName="persona">
              <header class="form-card__head">
                <h2 class="form-card__title">Datos personales</h2>
              </header>
              <div class="form-grid">
                <div>
                  <label class="form-label" for="f-tipoDoc">Tipo de documento <span class="form-required">*</span></label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select id="f-tipoDoc" formControlName="tipoDocumento">
                      @for (t of tiposDocumento; track t) { <mat-option [value]="t">{{ t }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-numDoc">Número de documento <span class="form-required">*</span></label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-numDoc" formControlName="numeroDocumento" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-apePat">Apellido paterno <span class="form-required">*</span></label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-apePat" formControlName="apellidoPaterno" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-apeMat">Apellido materno</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-apeMat" formControlName="apellidoMaterno" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-nombres">Nombres <span class="form-required">*</span></label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-nombres" formControlName="nombres" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-sexo">Sexo</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select id="f-sexo" formControlName="sexo">
                      @for (s of sexos; track s) { <mat-option [value]="s">{{ s }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-fnac">Fecha de nacimiento</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-fnac" type="date" formControlName="fechaNacimiento" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-ecivil">Estado civil</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select id="f-ecivil" formControlName="estadoCivil">
                      @for (e of estadosCiviles; track e) { <mat-option [value]="e">{{ e }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-proc">Procedencia</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <mat-select id="f-proc" formControlName="procedencia">
                      @for (p of procedencias; track p) { <mat-option [value]="p">{{ p }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-emailp">Email personal</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-emailp" formControlName="emailPersonal" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-cel">Celular</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-cel" formControlName="celular" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-orcid">ORCID</label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-orcid" formControlName="orcid" />
                  </mat-form-field>
                </div>
              </div>
            </section>

            <!-- ── Cuenta de acceso (solo al crear) ── -->
            @if (!editMode()) {
            <section class="form-card" formGroupName="cuenta">
              <header class="form-card__head">
                <h2 class="form-card__title">Cuenta de acceso</h2>
              </header>
              <div class="form-grid">
                <div>
                  <label class="form-label" for="f-user">Usuario <span class="form-required">*</span></label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-user" formControlName="username" autocomplete="off" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-email">Email <span class="form-required">*</span></label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-email" formControlName="email" />
                  </mat-form-field>
                </div>
                <div>
                  <label class="form-label" for="f-pass">Contraseña <span class="form-required">*</span></label>
                  <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                    <input matInput id="f-pass" [type]="showPassword() ? 'text' : 'password'" formControlName="password" autocomplete="new-password" />
                    <button mat-icon-button matSuffix type="button" tabindex="-1"
                            [attr.aria-label]="showPassword() ? 'Ocultar contraseña' : 'Ver contraseña'"
                            (click)="showPassword.set(!showPassword())">
                      <mat-icon [svgIcon]="showPassword() ? 'eye-off' : 'eye'" class="size-4 text-slate-400" />
                    </button>
                  </mat-form-field>
                </div>
              </div>
            </section>
            }

            <!-- ── Perfil estudiante ── -->
            <section class="form-card" [class.form-card--off]="!perfilEstudiante()">
              <header class="form-card__head">
                <h2 class="form-card__title">Perfil estudiante</h2>
                <mat-slide-toggle [checked]="perfilEstudiante()" (change)="toggleEstudiante($event.checked)"
                                  [disabled]="editMode()" aria-label="Activar perfil estudiante" />
              </header>
              <div class="section-collapse" [class.section-collapse--open]="perfilEstudiante()">
                <div class="section-collapse__inner">
                  <div class="form-grid" formGroupName="estudiante">
                    <div>
                      <label class="form-label" for="f-facultad">Facultad <span class="form-required">*</span></label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <mat-select id="f-facultad" formControlName="facultadId" (selectionChange)="onFacultadChange()">
                          @for (f of facultades(); track f.id) {
                            <mat-option [value]="f.id">{{ f.nombre }}</mat-option>
                          }
                        </mat-select>
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-programa">Programa de posgrado <span class="form-required">*</span></label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <mat-select id="f-programa" formControlName="programaId"
                                    [disabled]="!form.get('estudiante.facultadId')?.value">
                          @for (p of programas(); track p.id) {
                            <mat-option [value]="p.id">{{ p.nombre }} ({{ p.nivel }})</mat-option>
                          }
                        </mat-select>
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-codsis-e">Código de sistema</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <input matInput id="f-codsis-e" formControlName="codigoSistema" />
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-codmat">Código de matrícula</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <input matInput id="f-codmat" formControlName="codMatricula" />
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-emaili-e">Email institucional</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <input matInput id="f-emaili-e" formControlName="emailInstitucional" />
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-anio">Año de ingreso</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <input matInput id="f-anio" type="number" formControlName="anioIngreso" />
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-cond-e">Condición</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <mat-select id="f-cond-e" formControlName="condicion">
                          @for (c of condicionesEstudiante; track c) { <mat-option [value]="c">{{ c }}</mat-option> }
                        </mat-select>
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-fin">Financiamiento</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <mat-select id="f-fin" formControlName="financiamiento">
                          @for (f of financiamientos; track f) { <mat-option [value]="f">{{ f }}</mat-option> }
                        </mat-select>
                      </mat-form-field>
                    </div>
                    <div class="sm:col-span-2 xl:col-span-5 border-t border-slate-100 pt-3 mt-1">
                      <span class="form-label">Tutor académico</span>
                      <app-tutor-autocomplete placeholder="Buscar y asignar tutor…" [preset]="tutorPreset()" (selected)="onTutorSelected($event)" />
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- ── Grados académicos (nivel persona) ── -->
            <section class="form-card">
              <header class="form-card__head">
                <h2 class="form-card__title">Grados académicos</h2>
                <button type="button" class="btn-ghost text-xs" (click)="addGrado()">+ Agregar grado</button>
              </header>
              <div class="section-collapse section-collapse--open">
                <div class="section-collapse__inner">
                  <div formArrayName="gradosAcademicos" class="flex flex-col gap-3">
                    @if (!gradosFA.length) {
                      <p class="text-xs text-slate-400">Sin grados registrados. Use “Agregar grado”.</p>
                    }
                    @for (g of gradosFA.controls; track g; let i = $index) {
                      <div [formGroupName]="i" class="grado-row grid grid-cols-1 sm:grid-cols-12 gap-2 items-end border border-slate-100 rounded-lg p-3">
                        <div class="sm:col-span-3">
                          <label class="form-label">Grado <span class="form-required">*</span></label>
                          <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                            <mat-select formControlName="grado">
                              @for (op of gradosAcademicos; track op) {
                                <mat-option [value]="op">{{ gradoLabel(op) }}</mat-option>
                              }
                            </mat-select>
                          </mat-form-field>
                        </div>
                        <div class="sm:col-span-2">
                          <label class="form-label">Año</label>
                          <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                            <input matInput type="number" formControlName="anio" min="1950" max="2100" />
                          </mat-form-field>
                        </div>
                        <div class="sm:col-span-4">
                          <label class="form-label">Universidad</label>
                          <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                            <input matInput formControlName="universidad" maxlength="200" />
                          </mat-form-field>
                        </div>
                        <div class="sm:col-span-2">
                          <mat-checkbox formControlName="principal" (change)="onGradoPrincipalChange(i)">Principal</mat-checkbox>
                        </div>
                        <div class="sm:col-span-1 text-right">
                          <button type="button" class="btn-ghost text-rose-600" (click)="removeGrado(i)" aria-label="Quitar grado">✕</button>
                        </div>
                      </div>
                    }
                  </div>
                </div>
              </div>
            </section>

            <!-- ── Perfil docente ── -->
            <section class="form-card" [class.form-card--off]="!perfilDocente()">
              <header class="form-card__head">
                <h2 class="form-card__title">Perfil docente</h2>
                <mat-slide-toggle [checked]="perfilDocente()" (change)="toggleDocente($event.checked)"
                                  [disabled]="editMode()" aria-label="Activar perfil docente" />
              </header>
              <div class="section-collapse" [class.section-collapse--open]="perfilDocente()">
                <div class="section-collapse__inner">
                  <div class="form-grid" formGroupName="docente">
                    <div>
                      <label class="form-label" for="f-cat">Categoría</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <mat-select id="f-cat" formControlName="categoria">
                          @for (c of categoriasDocente; track c) { <mat-option [value]="c">{{ c }}</mat-option> }
                        </mat-select>
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-cond-d">Condición</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <mat-select id="f-cond-d" formControlName="condicion">
                          @for (c of condicionesDocente; track c) { <mat-option [value]="c">{{ c }}</mat-option> }
                        </mat-select>
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-codsis-d">Código de sistema</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <input matInput id="f-codsis-d" formControlName="codigoSistema" />
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-emaili-d">Email institucional</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <input matInput id="f-emaili-d" formControlName="emailInstitucional" />
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-lineas">Líneas de investigación</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <mat-select id="f-lineas" formControlName="lineasInvestigacionIds" multiple
                          (selectionChange)="onLineasChange()">
                          @for (l of lineasInvestigacion(); track l.id) {
                            <mat-option [value]="l.id">{{ l.nombre }}</mat-option>
                          }
                        </mat-select>
                      </mat-form-field>
                    </div>
                    <div>
                      <label class="form-label" for="f-lprin">Línea principal</label>
                      <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                        <mat-select id="f-lprin" formControlName="lineaPrincipalId" [disabled]="!selectedLineaIds().length">
                          <mat-option [value]="''">— Ninguna —</mat-option>
                          @for (id of selectedLineaIds(); track id) {
                            <mat-option [value]="id">{{ lineaNombre(id) }}</mat-option>
                          }
                        </mat-select>
                      </mat-form-field>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            @if (!perfilEstudiante() && !perfilDocente()) {
              <p class="text-xs text-amber-600">Debe activar al menos un perfil (estudiante o docente).</p>
            }

            <!-- ── Cargos, centros laborales y documentos ── -->
            <section class="form-card">
              <header class="form-card__head">
                <div>
                  <h2 class="form-card__title">Cargos, centros laborales y documentos</h2>
                  <p class="text-xs text-slate-400 mt-0.5">El DNI y la Partida de nacimiento son obligatorios para registrar a la persona.</p>
                </div>
              </header>
              <app-perfil-completo-form #perfilForm [embedded]="true" [cargos]="cargos()" [centros]="centros()"
                [value]="perfilCompleto()"
                (previewExisting)="previsualizarExistente($event)" (download)="descargarExistente($event)" />
            </section>

          </div>

          <!-- ── Barra de acciones fija ── -->
          <div class="action-bar">
            <div class="mx-auto max-w-[1360px] px-6 py-3 flex items-center justify-between gap-4">
              <p class="text-xs" [class]="statusClass()">{{ statusText() }}</p>
              <div class="flex items-center gap-2">
                <button type="button" mat-button class="!text-slate-500 !text-sm" (click)="cancelar()" [disabled]="saving()">
                  Cancelar
                </button>
                <button type="submit" mat-flat-button color="primary"
                        class="!rounded-lg !px-5 !h-9 !text-sm !font-medium inline-flex items-center gap-2"
                        [disabled]="saving() || !perfilActivo()">
                  @if (saving()) {
                    <mat-progress-spinner class="btn-spinner" diameter="18" mode="indeterminate" />
                  }
                  {{ saving() ? (editMode() ? 'Guardando…' : 'Registrando…') : (editMode() ? 'Guardar cambios' : 'Registrar persona') }}
                </button>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class PersonaFormComponent implements OnInit {
  private _fb = inject(FormBuilder);
  private _service = inject(PersonaService);
  private _cargoService = inject(CargoService);
  private _centroService = inject(CentroLaboralService);
  private _lineaService = inject(LineaInvestigacionService);
  private _router = inject(Router);
  private _route = inject(ActivatedRoute);
  private _dialog = inject(MatDialog);
  private _notify = inject(NotificationService);
  private _confirm = inject(ConfirmDialogService);
  private _host = inject(ElementRef<HTMLElement>);

  // ── Modo edición (misma pantalla que crear) ──
  protected editId: string | null = null;
  protected editMode = signal(false);
  protected perfilCompleto = signal<PerfilCompleto | null>(null);
  protected tutorPreset = signal<Partial<TutorCombo> | null>(null);

  @ViewChild('perfilForm') perfilForm?: PerfilCompletoFormComponent;

  protected readonly tiposDocumento = TIPOS_DOCUMENTO;
  protected readonly sexos = SEXOS;
  protected readonly estadosCiviles = ESTADOS_CIVILES;
  protected readonly procedencias = PROCEDENCIAS;
  protected readonly condicionesEstudiante = CONDICIONES_ESTUDIANTE;
  protected readonly financiamientos = FINANCIAMIENTOS;
  protected readonly gradosAcademicos = GRADOS_ACADEMICOS;
  protected readonly categoriasDocente = CATEGORIAS_DOCENTE;
  protected readonly condicionesDocente = CONDICIONES_DOCENTE;

  /** Solo en desarrollo (localhost) se muestra el botón "Llenar datos". */
  protected readonly isDev = signal(false);

  constructor() {
    afterNextRender(() => {
      const host = window.location.hostname;
      this.isDev.set(host === 'localhost' || host === '127.0.0.1' || host === '[::1]');
    });
  }

  protected facultades = signal<FacultadItem[]>([]);
  protected programas = signal<ProgramaPosgrado[]>([]);
  protected cargos = signal<CatalogoItem[]>([]);
  protected centros = signal<CatalogoItem[]>([]);
  protected lineasInvestigacion = signal<CatalogoItem[]>([]);
  protected perfilEstudiante = signal(false);
  protected perfilDocente = signal(false);
  protected showPassword = signal(false);
  protected saving = signal(false);
  protected errorMsg = signal<string | null>(null);

  /** Al menos un perfil activo (requisito para registrar). */
  protected perfilActivo = computed(() => this.perfilEstudiante() || this.perfilDocente());

  /** Texto del estado en la barra de acciones. */
  protected statusText = computed(() => {
    if (this.saving()) { return 'Registrando persona…'; }
    if (!this.perfilActivo()) { return 'Activa al menos un perfil (estudiante o docente).'; }
    return 'Revisa los datos y registra la persona.';
  });
  protected statusClass = computed(() =>
    !this.saving() && !this.perfilActivo() ? 'text-amber-600' : 'text-slate-400',
  );

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this._fb.group({
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
      }),
      cuenta: this._fb.group({
        username: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['contra123', [Validators.required, Validators.minLength(8)]],
      }),
      estudiante: this._fb.group({
        codigoSistema: [''],
        codMatricula: [''],
        emailInstitucional: ['', Validators.email],
        anioIngreso: [null],
        facultadId: [''],
        programaId: [''],
        condicion: [''],
        financiamiento: [''],
        observaciones: [''],
        tutorId: [''],
      }),
      docente: this._fb.group({
        codigoSistema: [''],
        emailInstitucional: ['', Validators.email],
        categoria: [''],
        condicion: [''],
        lineasInvestigacionIds: [[] as string[]],
        lineaPrincipalId: [''],
      }),
      gradosAcademicos: this._fb.array([] as FormGroup[]),
    });

    // Perfiles desactivados por defecto
    this.form.get('estudiante')!.disable();
    this.form.get('docente')!.disable();

    // ── Modo edición: la cuenta no se edita (se oculta y se desactiva) ──
    this.editId = this._route.snapshot.paramMap.get('id');
    if (this.editId) {
      this.editMode.set(true);
      this.form.get('cuenta')!.disable();
    }

    // Cascada Facultad → Programa: se cargan las facultades; los programas se
    // cargan al elegir una facultad (o al precargar en edición).
    this._service.listFacultades$().subscribe({
      next: (res) => this.facultades.set(res?.data?.content ?? res?.data ?? res ?? []),
      error: () => this.facultades.set([]),
    });

    forkJoin({
      cargos: this._cargoService.listarTodos$(),
      centros: this._centroService.listarTodos$(),
      lineas: this._lineaService.listarTodos$(),
    }).subscribe((res: any) => {
      this.cargos.set(res.cargos?.data?.content ?? res.cargos?.content ?? []);
      this.centros.set(res.centros?.data?.content ?? res.centros?.content ?? []);
      this.lineasInvestigacion.set(res.lineas?.data?.content ?? res.lineas?.content ?? []);
      if (this.editMode()) { this.cargarParaEdicion(); }
    });
  }

  // ── Edición: carga la persona + perfil y rellena el mismo formulario ──
  private cargarParaEdicion(): void {
    if (!this.editId) { return; }
    forkJoin({
      persona: this._service.obtener$(this.editId),
      perfil: this._service.obtenerPerfilCompleto$(this.editId),
    }).subscribe({
      next: ({ persona, perfil }: any) => {
        const p = persona?.data ?? persona ?? {};
        this.perfilCompleto.set(perfil?.data ?? perfil ?? null);
        this.prefillEdicion(p);
      },
      error: () => this.errorMsg.set('No se pudo cargar la persona a editar.'),
    });
  }

  private prefillEdicion(p: any): void {
    this.form.get('persona')!.patchValue({
      tipoDocumento: p.tipoDocumento, numeroDocumento: p.numeroDocumento,
      apellidoPaterno: p.apellidoPaterno, apellidoMaterno: p.apellidoMaterno, nombres: p.nombres,
      sexo: p.sexo, fechaNacimiento: p.fechaNacimiento, estadoCivil: p.estadoCivil,
      nacionalidad: p.nacionalidad, procedencia: p.procedencia,
      emailPersonal: p.emailPersonal, celular: p.celular, orcid: p.orcid,
    });

    if (p.estudiante) {
      this.toggleEstudiante(true);
      const e = p.estudiante;
      this.form.get('estudiante')!.patchValue({
        codigoSistema: e.codigoSistema, codMatricula: e.codMatricula,
        emailInstitucional: e.emailInstitucional, anioIngreso: e.anioIngreso,
        condicion: e.condicion, financiamiento: e.financiamiento,
        observaciones: e.observaciones, tutorId: e.tutorId ?? '',
      });
      // Cascada en edición: facultad (derivada del programa) → programas → programa.
      const facId = e.facultadId ?? '';
      this.form.get('estudiante.facultadId')!.setValue(facId);
      if (facId) {
        this._service.listProgramas$(facId).subscribe({
          next: (res) => {
            this.programas.set(res?.data ?? res ?? []);
            this.form.get('estudiante.programaId')!.setValue(e.programaId ?? '');
          },
          error: () => this.programas.set([]),
        });
      }
      this.form.get('estudiante.codigoSistema')!.disable(); // no editable tras crear
      if (e.tutorId) {
        const [ap, no] = (e.tutorNombre ?? '').split(/,\s*/);
        this.tutorPreset.set({ id: e.tutorId, apellidos: ap ?? e.tutorNombre, nombres: no ?? '' } as Partial<TutorCombo>);
      }
    }

    if (p.docente) {
      this.toggleDocente(true);
      const d = p.docente;
      const lineas = (d.lineasInvestigacion ?? []) as any[];
      this.form.get('docente')!.patchValue({
        codigoSistema: d.codigoSistema, emailInstitucional: d.emailInstitucional,
        categoria: d.categoria, condicion: d.condicion,
        lineasInvestigacionIds: lineas.map((l) => l.lineaInvestigacionId ?? l.id).filter(Boolean),
        lineaPrincipalId: (lineas.find((l) => l.esPrincipal)?.lineaInvestigacionId
          ?? lineas.find((l) => l.esPrincipal)?.id) ?? '',
      });
      this.form.get('docente.codigoSistema')!.disable();
    }

    // Grados académicos (nivel persona)
    this.gradosFA.clear();
    for (const g of p.gradosAcademicos ?? []) {
      this.gradosFA.push(this.buildGradoGroup(g.grado, g.anio ?? null, g.universidad ?? '', !!g.principal));
    }

    // En edición no se cambia QUÉ perfiles tiene la persona (solo sus datos).
    this.form.markAsPristine();
  }

  /** Previsualiza in-app un documento ya cargado (mismo visor que los adjuntos nuevos). */
  previsualizarExistente(doc: DocumentoItem): void {
    if (!this.editId) { return; }
    this._service.descargarDocumento$(this.editId, doc.id).subscribe({
      next: (blob) => {
        const nombre = doc.nombreOriginal || 'Documento';
        const tipo = doc.contentType || blob.type || '';
        const esPdf = tipo.includes('pdf') || nombre.toLowerCase().endsWith('.pdf');
        const url = URL.createObjectURL(new Blob([blob], { type: tipo || 'application/octet-stream' }));
        this._dialog.open(DocumentoPreviewDialogComponent, {
          panelClass: ['dialog-anim'], autoFocus: false, maxWidth: '92vw',
          data: { nombre, url, esPdf, mime: tipo },
        }).afterClosed().subscribe(() => setTimeout(() => URL.revokeObjectURL(url), 1000));
      },
      error: () => this.errorMsg.set('No se pudo previsualizar el documento.'),
    });
  }

  descargarExistente(doc: DocumentoItem): void {
    if (!this.editId) { return; }
    this._service.descargarDocumento$(this.editId, doc.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = doc.nombreOriginal || 'documento';
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: () => this.errorMsg.set('No se pudo descargar el documento.'),
    });
  }

  /** Guarda la edición: PUT datos (persona/estudiante/docente/tutor/líneas) + POST perfil (historiales/documentos). */
  private guardarEdicion(request: CrearPersonaRequest, perfil: { datos: any; archivos: Record<string, File> }): void {
    if (!this.editId) { return; }
    const body: any = { ...request.persona };
    if (request.estudiante) { body.estudiante = request.estudiante; }
    if (request.docente) { body.docente = request.docente; }
    // Siempre se envía la lista actual (upsert idempotente: refleja altas y bajas).
    body.gradosAcademicos = this._collectGrados();

    this.saving.set(true);
    this._service.actualizar$(this.editId, body).subscribe({
      next: () => {
        // Historiales + documentos (nuevos) en el mismo guardado.
        const fd = buildPerfilFormData(perfil.datos, perfil.archivos);
        this._service.guardarPerfilCompleto$(this.editId!, fd).subscribe({
          next: () => this._finEdicion(),
          error: () => this._finEdicion(), // los datos ya se guardaron; el perfil es best-effort
        });
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMsg.set(err?.error?.message || err?.error?.error || 'No se pudieron guardar los cambios.');
        this._scrollToFirstError();
      },
    });
  }

  private _finEdicion(): void {
    this.saving.set(false);
    this.form.markAsPristine();
    this._notify.success('Cambios guardados correctamente');
    this._router.navigate(['/admin/personas'], { state: { registeredId: this.editId } });
  }

  toggleEstudiante(checked: boolean): void {
    this.perfilEstudiante.set(checked);
    const g = this.form.get('estudiante')!;
    if (checked) {
      g.enable();
      g.get('facultadId')!.addValidators(Validators.required);
      g.get('programaId')!.addValidators(Validators.required);
    } else {
      g.get('facultadId')!.clearValidators();
      g.get('programaId')!.clearValidators();
      g.reset();
      g.disable();
    }
    g.get('facultadId')!.updateValueAndValidity();
    g.get('programaId')!.updateValueAndValidity();
  }

  /** Cascada Facultad → Programa: al cambiar la facultad, resetea el programa y recarga. */
  onFacultadChange(): void {
    const facId = this.form.get('estudiante.facultadId')!.value;
    this.form.get('estudiante.programaId')!.setValue('');
    this.programas.set([]);
    if (!facId) { return; }
    this._service.listProgramas$(facId).subscribe({
      next: (res) => this.programas.set(res?.data ?? res ?? []),
      error: () => this.programas.set([]),
    });
  }

  toggleDocente(checked: boolean): void {
    this.perfilDocente.set(checked);
    const g = this.form.get('docente')!;
    if (checked) {
      g.enable();
    } else {
      g.reset();
      g.disable();
    }
  }

  // ── Grados académicos (nivel persona) ──
  get gradosFA(): FormArray {
    return this.form.get('gradosAcademicos') as FormArray;
  }

  gradoLabel(grado: string): string {
    return GRADOS_ACADEMICOS_LABELS[grado] ?? grado;
  }

  private buildGradoGroup(grado = '', anio: number | null = null, universidad = '', principal = false): FormGroup {
    return this._fb.group({
      grado: [grado, Validators.required],
      anio: [anio],
      universidad: [universidad, Validators.maxLength(200)],
      principal: [principal],
    });
  }

  addGrado(): void {
    this.gradosFA.push(this.buildGradoGroup('', null, '', this.gradosFA.length === 0));
    this.form.markAsDirty();
  }

  removeGrado(i: number): void {
    this.gradosFA.removeAt(i);
    this.form.markAsDirty();
  }

  /** Solo un grado puede ser principal: al marcar uno, desmarca los demás. */
  onGradoPrincipalChange(i: number): void {
    const marcado = this.gradosFA.at(i).get('principal')!.value;
    if (marcado) {
      this.gradosFA.controls.forEach((c, idx) => {
        if (idx !== i) { c.get('principal')!.setValue(false, { emitEvent: false }); }
      });
    }
  }

  /** IDs de líneas actualmente seleccionadas en el multiselect docente. */
  selectedLineaIds(): string[] {
    return (this.form?.get('docente.lineasInvestigacionIds')?.value as string[]) ?? [];
  }

  lineaNombre(id: string): string {
    return this.lineasInvestigacion().find((l) => l.id === id)?.nombre ?? id;
  }

  /** Al cambiar la selección, limpia la principal si dejó de estar seleccionada. */
  onLineasChange(): void {
    const g = this.form.get('docente')!;
    const ids = (g.get('lineasInvestigacionIds')!.value as string[]) ?? [];
    const principal = g.get('lineaPrincipalId')!.value;
    if (principal && !ids.includes(principal)) {
      g.get('lineaPrincipalId')!.setValue('');
    }
  }

  /** Rellena el formulario con datos de ejemplo (ayuda para pruebas). */
  fillSample(): void {
    this.errorMsg.set(null);
    const n = Math.floor(Math.random() * 90000) + 10000;
    const dni = String(Math.floor(Math.random() * 90000000) + 10000000);
    const celular = '9' + String(Math.floor(Math.random() * 90000000) + 10000000);

    this.form.get('persona')!.patchValue({
      tipoDocumento: 'DNI',
      numeroDocumento: dni,
      apellidoPaterno: 'PÉREZ',
      apellidoMaterno: 'GARCÍA',
      nombres: 'JUAN CARLOS',
      sexo: this.sexos[0],
      fechaNacimiento: '1995-05-20',
      estadoCivil: this.estadosCiviles[0],
      nacionalidad: 'PERUANA',
      procedencia: this.procedencias[0],
      emailPersonal: `juan.perez${n}@gmail.com`,
      celular,
      orcid: '0000-0002-1825-0097',
    });

    this.form.get('cuenta')!.patchValue({
      username: `jperez${n}`,
      email: `juan.perez${n}@unmsm.edu.pe`,
      password: 'Password123',
    });

    // Activa y rellena el perfil estudiante de ejemplo
    if (!this.perfilEstudiante()) { this.toggleEstudiante(true); }
    this.form.get('estudiante')!.patchValue({
      codigoSistema: `SIS${n}`,
      codMatricula: `MAT${n}`,
      emailInstitucional: `juan.perez${n}@unmsm.edu.pe`,
      anioIngreso: 2024,
      condicion: this.condicionesEstudiante[0],
      financiamiento: this.financiamientos[0],
    });
    // Cascada de ejemplo: primera facultad → sus programas → primer programa.
    const facEjemplo = this.facultades()[0]?.id ?? '';
    this.form.get('estudiante.facultadId')!.setValue(facEjemplo);
    if (facEjemplo) {
      this._service.listProgramas$(facEjemplo).subscribe({
        next: (res) => {
          this.programas.set(res?.data ?? res ?? []);
          this.form.get('estudiante.programaId')!.setValue(this.programas()[0]?.id ?? '');
        },
      });
    }

    // Grados académicos de ejemplo (uno principal).
    this.gradosFA.clear();
    this.gradosFA.push(this.buildGradoGroup('MAGISTER', 2018, 'Universidad Nacional Mayor de San Marcos', false));
    this.gradosFA.push(this.buildGradoGroup('DOCTOR', 2022, 'Universidad Nacional Mayor de San Marcos', true));

    // Rellena la sección embebida: cargos, centros y documentos obligatorios.
    this.perfilForm?.fillSample();
  }

  submit(): void {
    if (this.saving()) { return; } // evita doble envío
    this.errorMsg.set(null);

    if (!this.perfilEstudiante() && !this.perfilDocente()) {
      this.errorMsg.set('Debe activar al menos un perfil (estudiante o docente).');
      this._scrollToFirstError();
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this._scrollToFirstError();
      return;
    }

    // Recoge historiales + documentos (valida DNI/Partida obligatorios, un solo "actual").
    // collect() marca sus propios campos inválidos (incluidos DNI/Partida).
    const perfil = this.perfilForm?.collect();
    if (!perfil) {
      this.errorMsg.set('Revisa la sección de cargos, centros y documentos.');
      this._scrollToFirstError();
      return;
    }

    const raw = this.form.getRawValue();
    const request: CrearPersonaRequest = {
      persona: this._clean(raw.persona),
      cuenta: raw.cuenta,
      perfil: perfil.datos,
    };
    if (this.perfilEstudiante()) {
      request.estudiante = this._clean(raw.estudiante);
    }
    if (this.perfilDocente()) {
      const rawDocente = raw.docente as any;
      const docente: any = this._clean(rawDocente);
      const ids: string[] = rawDocente.lineasInvestigacionIds ?? [];
      const principalId: string = rawDocente.lineaPrincipalId ?? '';
      delete docente.lineasInvestigacionIds;
      delete docente.lineaPrincipalId;
      docente.lineasInvestigacion = ids.map((id) => ({
        lineaInvestigacionId: id,
        esPrincipal: id === principalId,
      }));
      request.docente = docente;
    }

    const grados = this._collectGrados();
    if (grados.length) {
      request.gradosAcademicos = grados;
    }

    // ── Edición: actualiza datos (PUT) + perfil/documentos (POST) y vuelve al reporte ──
    if (this.editMode()) {
      this.guardarEdicion(request, perfil);
      return;
    }

    // Una sola petición multipart: parte 'datos' (JSON) + una parte por documento.
    const fd = new FormData();
    fd.append('datos', JSON.stringify(request));
    Object.entries(perfil.archivos).forEach(([tipo, file]) => fd.append(tipo, file, file.name));

    this.saving.set(true);
    this._service.registrarMultipart$(fd).subscribe({
      next: (res) => {
        this.saving.set(false);
        this.form.markAsPristine();
        this.perfilForm?.form.markAsPristine();
        const id = res?.data?.id ?? res?.id;
        // Redirige al listado de personas (el reporte recarga sus datos en
        // ngOnInit, por lo que la persona nueva aparece). Se resalta su fila.
        this._notify.success('Persona registrada correctamente');
        this._router.navigate(['/admin/personas'], {
          state: id ? { registeredId: id } : undefined,
        });
      },
      error: (err) => {
        // Permanece en el formulario con los datos intactos y rehabilita el botón.
        this.saving.set(false);
        this.errorMsg.set(
          err?.error?.message || err?.error?.error ||
          'No se pudo registrar la persona. Inténtalo de nuevo.',
        );
        this._notify.error('No se pudo registrar la persona.');
        // Error del backend: el formulario es válido, así que llevamos al
        // usuario al banner de error (arriba) en lugar de a un campo.
        this._scrollToBanner();
      },
    });
  }

  /** Desplaza y enfoca el primer campo inválido (o la sección de documentos). */
  private _scrollToFirstError(): void {
    // afterNextRender no aplica aquí: el DOM ya existe; esperamos al siguiente
    // frame para que Angular pinte el estado ng-invalid antes de buscarlo.
    requestAnimationFrame(() => {
      const host = this._host.nativeElement as HTMLElement;
      const invalid = host.querySelector<HTMLElement>(
        'input.ng-invalid, textarea.ng-invalid, mat-select.ng-invalid, [formcontrolname].ng-invalid',
      );
      const target = invalid
        ?? host.querySelector<HTMLElement>('app-perfil-completo-form')
        ?? host.querySelector<HTMLElement>('[role="alert"]');
      target?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      if (typeof invalid?.focus === 'function') { invalid.focus({ preventScroll: true }); }
    });
  }

  /** Lleva la vista al banner de error (parte superior del formulario). */
  private _scrollToBanner(): void {
    requestAnimationFrame(() => {
      const host = this._host.nativeElement as HTMLElement;
      host.querySelector<HTMLElement>('[role="alert"]')
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
  }

  onTutorSelected(t: TutorCombo | null): void {
    this.form.get('estudiante.tutorId')?.setValue(t?.id ?? '');
  }

  cancelar(): void {
    if (this.saving()) { return; }

    const hasChanges = this.form.dirty || !!this.perfilForm?.form.dirty;
    if (!hasChanges) {
      this._router.navigate(['/admin/personas']);
      return;
    }

    this._confirm.confirmSave({
      title: 'Tienes cambios sin guardar',
      message: '¿Deseas salir? Se perderán los datos ingresados en el formulario.',
    })
      .then(() => this._router.navigate(['/admin/personas']))
      .catch(() => { /* el usuario decidió quedarse en el formulario */ });
  }

  /** Arma la lista de grados para el request, garantizando un único principal. */
  private _collectGrados(): { grado: string; anio: number | null; universidad: string | null; principal: boolean }[] {
    const rows = (this.gradosFA.getRawValue() as any[])
      .filter((g) => g && g.grado)
      .map((g) => ({
        grado: g.grado,
        anio: g.anio != null && g.anio !== '' ? Number(g.anio) : null,
        universidad: (g.universidad ?? '').trim() || null,
        principal: !!g.principal,
      }));
    // Un solo principal: si ninguno está marcado, el primero pasa a ser principal.
    if (rows.length && !rows.some((g) => g.principal)) {
      rows[0].principal = true;
    } else {
      let visto = false;
      for (const g of rows) {
        if (g.principal && visto) { g.principal = false; }
        if (g.principal) { visto = true; }
      }
    }
    return rows;
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
