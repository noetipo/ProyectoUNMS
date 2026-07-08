import { Component, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { EstudianteService } from '../../services/estudiante.service';
import { ProgramaDoctoradoService } from '../../services/programa-doctorado.service';
import { CentroLaboralService } from '../../services/centro-laboral.service';
import { CargoService } from '../../services/cargo.service';
import {
  Estudiante, ProgramaDoctorado, CentroLaboral, Cargo,
  Sexo, EstadoCivil, Financiamiento, Condicion, Procedencia, TipoDocumento,
} from '../../models/student.models';

@Component({
  selector: 'app-estudiante-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './estudiante-edit.component.html',
})
export class EstudianteEditComponent implements OnInit {
  private _fb        = inject(FormBuilder);
  private _route     = inject(ActivatedRoute);
  private _router    = inject(Router);
  private _service   = inject(EstudianteService);
  private _progSvc   = inject(ProgramaDoctoradoService);
  private _centroSvc = inject(CentroLaboralService);
  private _cargoSvc  = inject(CargoService);

  readonly tipoDocumentoValues  = [TipoDocumento.DNI, TipoDocumento.CARNET_EXTRANJERIA, TipoDocumento.PASAPORTE, TipoDocumento.PTP, TipoDocumento.CARNET_DIPLOMATICO];
  readonly sexoValues           = Object.values(Sexo);
  readonly estadoCivilValues    = Object.values(EstadoCivil);
  readonly financiamientoValues = Object.values(Financiamiento);
  readonly condicionValues      = Object.values(Condicion);
  readonly procedenciaValues    = Object.values(Procedencia);

  readonly tipoDocumentoLabel: Record<string, string> = {
    DNI:                'DNI — Documento Nacional de Identidad',
    CARNET_EXTRANJERIA: 'CE — Carnet de Extranjería',
    PASAPORTE:          'Pasaporte',
    PTP:                'PTP — Permiso Temporal de Permanencia',
    CARNET_DIPLOMATICO: 'Carnet Diplomático',
  };
  readonly sexoLabel: Record<string, string> = {
    HOMBRE: 'Hombre', MUJER: 'Mujer',
  };
  readonly estadoCivilLabel: Record<string, string> = {
    SOLTERO: 'Soltero', CASADO: 'Casado', DIVORCIADO: 'Divorciado',
    VIUDO: 'Viudo', CONVIVIENTE: 'Conviviente',
  };
  readonly financiamientoLabel: Record<string, string> = {
    BECA_COMPLETA: 'Beca Completa', BECA_PARCIAL: 'Beca Parcial', AUTOFINANCIADO: 'Autofinanciado',
  };
  readonly condicionLabel: Record<string, string> = {
    REGULAR: 'Regular', SANCIONADO: 'Sancionado', EGRESADO: 'Egresado', RETIRADO: 'Retirado',
  };
  readonly procedenciaLabel: Record<string, string> = {
    NACIONAL: 'Nacional', EXTRANJERO: 'Extranjero',
  };

  programas   = signal<ProgramaDoctorado[]>([]);
  centros     = signal<CentroLaboral[]>([]);
  cargos      = signal<Cargo[]>([]);
  loading     = signal(false);
  saving      = signal(false);
  errorMsg    = signal<string | null>(null);
  estudiante  = signal<Estudiante | null>(null);

  form!: FormGroup;
  private id!: string;

  ngOnInit(): void {
    this.id = this._route.snapshot.paramMap.get('id')!;

    this.form = this._fb.group({
      nombres:             ['', [Validators.required, Validators.minLength(2)]],
      apellidoPaterno:     ['', [Validators.required, Validators.minLength(2)]],
      apellidoMaterno:     [''],
      fechaNacimiento:     [null],
      tipoDocumento:       [null],
      numeroDocumento:     [''],
      sexo:                [null],
      estadoCivil:         [null],
      nacionalidad:        [''],
      discapacidad:        [false],
      celular:             [''],
      emailPersonal:       ['', Validators.email],
      emailInstitucional:  ['', Validators.email],
      programaDoctoradoId: [null],
      codMatricula:        [''],
      anioIngreso:         [null, [Validators.min(1900), Validators.max(2100)]],
      financiamiento:      [null],
      condicion:           [null],
      centroLaboralId:     [null],
      cargoActualId:       [null],
      procedencia:         [null],
      orcid:               [''],
      observaciones:       [''],
    });

    this._progSvc.getWithQuery$({ page: 0, size: 100 }).subscribe({ next: res => this.programas.set(res.content) });
    this._centroSvc.getWithQuery$({ page: 0, size: 200 }).subscribe({ next: res => this.centros.set(res.content) });
    this._cargoSvc.getWithQuery$({ page: 0, size: 200 }).subscribe({ next: res => this.cargos.set(res.content) });

    this.loading.set(true);
    this._service.getById(this.id).subscribe({
      next: est => {
        this.estudiante.set(est);
        this.loading.set(false);
        this.form.patchValue({
          ...est,
          programaDoctoradoId: est.programaDoctorado?.id ?? null,
          centroLaboralId:     est.centroLaboral?.id     ?? null,
          cargoActualId:       est.cargoActual?.id       ?? null,
        });
      },
      error: e => {
        this.loading.set(false);
        this.errorMsg.set(e.message);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.errorMsg.set(null);
    const dirty = this._getDirtyValues();
    this._service.update(this.id, dirty).subscribe({
      next: () => this._router.navigate(['/admin/student/estudiantes', this.id]),
      error: e  => { this.saving.set(false); this.errorMsg.set(e.message); },
    });
  }

  fillTestData(): void {
    this.form.patchValue({
      nombres:            'María Isabel',
      apellidoPaterno:    'Quispe',
      apellidoMaterno:    'Torres',
      fechaNacimiento:    '1980-07-22',
      tipoDocumento:      'DNI',
      numeroDocumento:    '87654321',
      sexo:               'MUJER',
      estadoCivil:        'SOLTERO',
      nacionalidad:       'Peruana',
      discapacidad:       false,
      celular:            '+51 987 654 321',
      emailPersonal:      'mquispe@ejemplo.com',
      emailInstitucional: 'm.quispe@unms.edu.pe',
      programaDoctoradoId: this.programas()[0]?.id ?? null,
      codMatricula:       '2023-DOC-042',
      anioIngreso:        2023,
      financiamiento:     'AUTOFINANCIADO',
      condicion:          'REGULAR',
      centroLaboralId:    this.centros()[0]?.id ?? null,
      cargoActualId:      this.cargos()[0]?.id  ?? null,
      procedencia:        'NACIONAL',
      orcid:              '0000-0002-9876-5432',
      observaciones:      'Investigadora con publicaciones en revistas indexadas.',
    });
    this.form.markAsDirty();
  }

  private _getDirtyValues(): Record<string, unknown> {
    const dirty: Record<string, unknown> = {};
    Object.entries(this.form.controls).forEach(([key, ctrl]) => {
      if (ctrl.dirty) dirty[key] = ctrl.value === '' ? null : ctrl.value;
    });
    return dirty;
  }
}
