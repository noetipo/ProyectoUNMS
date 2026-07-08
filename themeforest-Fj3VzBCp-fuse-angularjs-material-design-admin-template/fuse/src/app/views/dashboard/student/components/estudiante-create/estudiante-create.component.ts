import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { EstudianteService } from '../../services/estudiante.service';
import { ProgramaDoctoradoService } from '../../services/programa-doctorado.service';
import { CentroLaboralService } from '../../services/centro-laboral.service';
import { CargoService } from '../../services/cargo.service';
import {
  ProgramaDoctorado, CentroLaboral, Cargo,
  Sexo, EstadoCivil, Financiamiento, Condicion, Procedencia, TipoDocumento,
} from '../../models/student.models';

interface FilePreview {
  url: string;
  safeUrl: SafeResourceUrl;
  isImage: boolean;
}

@Component({
  selector: 'app-estudiante-create',
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
  templateUrl: './estudiante-create.component.html',
})
export class EstudianteCreateComponent implements OnInit, OnDestroy {
  private _fb        = inject(FormBuilder);
  private _service   = inject(EstudianteService);
  private _progSvc   = inject(ProgramaDoctoradoService);
  private _centroSvc = inject(CentroLaboralService);
  private _cargoSvc  = inject(CargoService);
  private _router    = inject(Router);
  private _sanitizer = inject(DomSanitizer);

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
    HOMBRE: 'Hombre',
    MUJER:  'Mujer',
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

  programas      = signal<ProgramaDoctorado[]>([]);
  centros        = signal<CentroLaboral[]>([]);
  cargos         = signal<Cargo[]>([]);
  loading        = signal(false);
  errorMsg       = signal<string | null>(null);
  dniPreview     = signal<FilePreview | null>(null);
  partidaPreview = signal<FilePreview | null>(null);

  dniFile:     File | null = null;
  partidaFile: File | null = null;

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this._fb.group({
      nombres:             ['', [Validators.required, Validators.minLength(2), Validators.maxLength(150)]],
      apellidoPaterno:     ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      apellidoMaterno:     ['', Validators.maxLength(100)],
      fechaNacimiento:     [null],
      tipoDocumento:       [null],
      numeroDocumento:     ['', Validators.maxLength(20)],
      sexo:                [null],
      estadoCivil:         [null],
      nacionalidad:        ['', Validators.maxLength(100)],
      discapacidad:        [false],
      celular:             ['', Validators.maxLength(20)],
      emailPersonal:       ['', [Validators.email, Validators.maxLength(150)]],
      emailInstitucional:  ['', [Validators.email, Validators.maxLength(150)]],
      programaDoctoradoId: [null],
      codMatricula:        ['', Validators.maxLength(20)],
      anioIngreso:         [null, [Validators.min(1900), Validators.max(2100)]],
      financiamiento:      [null],
      condicion:           [null],
      centroLaboralId:     [null],
      cargoActualId:       [null],
      procedencia:         [null],
      orcid:               ['', Validators.maxLength(100)],
      observaciones:       [''],
    });

    this._progSvc.getWithQuery$({ page: 0, size: 100 }).subscribe({ next: res => this.programas.set(res.content) });
    this._centroSvc.getWithQuery$({ page: 0, size: 200 }).subscribe({ next: res => this.centros.set(res.content) });
    this._cargoSvc.getWithQuery$({ page: 0, size: 200 }).subscribe({ next: res => this.cargos.set(res.content) });
  }

  ngOnDestroy(): void {
    const d = this.dniPreview();
    const p = this.partidaPreview();
    if (d) URL.revokeObjectURL(d.url);
    if (p) URL.revokeObjectURL(p.url);
  }

  onDniFileChange(event: Event): void {
    const old = this.dniPreview();
    if (old) URL.revokeObjectURL(old.url);
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.dniFile = file;
    this.dniPreview.set(file ? this._makePreview(file) : null);
  }

  onPartidaFileChange(event: Event): void {
    const old = this.partidaPreview();
    if (old) URL.revokeObjectURL(old.url);
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.partidaFile = file;
    this.partidaPreview.set(file ? this._makePreview(file) : null);
  }

  private _makePreview(file: File): FilePreview {
    const url = URL.createObjectURL(file);
    return {
      url,
      safeUrl: this._sanitizer.bypassSecurityTrustResourceUrl(url),
      isImage: file.type.startsWith('image/'),
    };
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMsg.set(null);
    const dto = this._cleanDto(this.form.value);
    this._service.create(dto, this.dniFile, this.partidaFile).subscribe({
      next: est => this._router.navigate(['/admin/student/estudiantes', est.id]),
      error: e  => { this.loading.set(false); this.errorMsg.set(e.message); },
    });
  }

  fillTestData(): void {
    this.form.patchValue({
      nombres:            'Juan Carlos',
      apellidoPaterno:    'García',
      apellidoMaterno:    'López',
      fechaNacimiento:    '1985-03-15',
      tipoDocumento:      'DNI',
      numeroDocumento:    '12345678',
      sexo:               'HOMBRE',
      estadoCivil:        'CASADO',
      nacionalidad:       'Peruana',
      discapacidad:       false,
      celular:            '+51 999 123 456',
      emailPersonal:      'jgarcia@ejemplo.com',
      emailInstitucional: 'j.garcia@unms.edu.pe',
      programaDoctoradoId: this.programas()[0]?.id ?? null,
      codMatricula:       '2024-DOC-001',
      anioIngreso:        2024,
      financiamiento:     'BECA_COMPLETA',
      condicion:          'REGULAR',
      centroLaboralId:    this.centros()[0]?.id ?? null,
      cargoActualId:      this.cargos()[0]?.id  ?? null,
      procedencia:        'NACIONAL',
      orcid:              '0000-0001-2345-6789',
      observaciones:      'Estudiante destacado con buen rendimiento académico.',
    });
    this.form.markAsDirty();
  }

  private _cleanDto(raw: any): any {
    return Object.fromEntries(
      Object.entries(raw).filter(([, v]) => v !== '' && v !== null && v !== undefined)
    );
  }
}
