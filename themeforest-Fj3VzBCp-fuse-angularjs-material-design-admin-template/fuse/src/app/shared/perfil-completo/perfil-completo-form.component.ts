import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, Output, inject } from '@angular/core';
import {
  FormArray, FormBuilder, FormGroup, ReactiveFormsModule,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DocumentoPreviewDialogComponent } from './documento-preview-dialog.component';
import {
  buildPerfilFormData, CatalogoItem, DocumentoItem, PerfilCompleto,
  PerfilCompletoData, TIPOS_DOCUMENTO_PERFIL, formatTamanio,
} from './perfil-completo.model';

const MAX_FILE_BYTES = 10 * 1024 * 1024;
const TIPOS_PERMITIDOS = ['application/pdf', 'image/jpeg', 'image/png'];

/** Fila cruda de un historial (cargos o centros) obtenida de getRawValue(). */
type FilaHistorial = {
  cargoId?: string;
  centroLaboralId?: string;
  fechaInicio: string | null;
  fechaFin: string | null;
  actual: boolean;
};

/**
 * Formulario reutilizable del perfil completo: historiales dinámicos de cargos y
 * centros laborales (arrays) + carga de documentos con previsualización.
 * Construye un `FormData` multipart y lo emite por (save). No conoce las URLs:
 * el contenedor (ficha de persona, mi-perfil o registro) decide el endpoint.
 *
 * API pública estable (la usan persona-form, persona-ficha y mi-perfil):
 *   Inputs: cargos / centros / saving / embedded / value
 *   Outputs: save / download
 *   collect() · submit() · fillSample()
 */
@Component({
  selector: 'app-perfil-completo-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatCheckboxModule, MatTooltipModule,
  ],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-6">

      @if (localError()) {
        <div role="alert" class="rounded-lg bg-rose-50 border border-rose-200 px-3 py-2 text-sm text-rose-700">
          {{ localError() }}
        </div>
      }

      <!-- ── Historial de cargos ── -->
      <section class="rounded-2xl border border-slate-200/70 dark:border-slate-700 p-4">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-sm font-semibold text-slate-800 dark:text-slate-200">Historial de cargos</h3>
          <button type="button" class="btn-add" (click)="addCargo()">
            <mat-icon svgIcon="plus" class="size-3.5" /> Agregar cargo
          </button>
        </div>
        <div formArrayName="cargos" class="space-y-2">
          @for (g of cargosArray.controls; track $index; let i = $index) {
            <div [formGroupName]="i" class="grid grid-cols-12 gap-2 items-center">
              <select formControlName="cargoId" [attr.aria-label]="'Cargo fila ' + (i + 1)" class="hist-select col-span-4">
                <option value="">— Cargo —</option>
                @for (c of cargos; track c.id) { <option [value]="c.id">{{ c.nombre }}</option> }
              </select>
              <input type="date" formControlName="fechaInicio" aria-label="Fecha de inicio" class="hist-input col-span-3" />
              <input type="date" formControlName="fechaFin" aria-label="Fecha de fin" class="hist-input col-span-3" />
              <label class="col-span-1 flex items-center justify-center text-xs gap-1 text-slate-600 dark:text-slate-300">
                <input type="checkbox" formControlName="actual" (change)="syncActual(cargosArray, i)" /> actual
              </label>
              <button type="button" class="col-span-1 text-slate-400 hover:text-rose-600 justify-self-end"
                      aria-label="Quitar cargo" (click)="removeCargo(i)">
                <mat-icon svgIcon="trash-2" class="size-4" />
              </button>
            </div>
          }
          @if (!cargosArray.length) { <p class="text-xs text-slate-400">Sin cargos registrados.</p> }
        </div>
      </section>

      <!-- ── Historial de centros laborales ── -->
      <section class="rounded-2xl border border-slate-200/70 dark:border-slate-700 p-4">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-sm font-semibold text-slate-800 dark:text-slate-200">Historial de centros laborales</h3>
          <button type="button" class="btn-add" (click)="addCentro()">
            <mat-icon svgIcon="plus" class="size-3.5" /> Agregar centro
          </button>
        </div>
        <div formArrayName="centros" class="space-y-2">
          @for (g of centrosArray.controls; track $index; let i = $index) {
            <div [formGroupName]="i" class="grid grid-cols-12 gap-2 items-center">
              <select formControlName="centroLaboralId" [attr.aria-label]="'Centro laboral fila ' + (i + 1)" class="hist-select col-span-4">
                <option value="">— Centro laboral —</option>
                @for (c of centros; track c.id) { <option [value]="c.id">{{ c.nombre }}</option> }
              </select>
              <input type="date" formControlName="fechaInicio" aria-label="Fecha de inicio" class="hist-input col-span-3" />
              <input type="date" formControlName="fechaFin" aria-label="Fecha de fin" class="hist-input col-span-3" />
              <label class="col-span-1 flex items-center justify-center text-xs gap-1 text-slate-600 dark:text-slate-300">
                <input type="checkbox" formControlName="actual" (change)="syncActual(centrosArray, i)" /> actual
              </label>
              <button type="button" class="col-span-1 text-slate-400 hover:text-rose-600 justify-self-end"
                      aria-label="Quitar centro laboral" (click)="removeCentro(i)">
                <mat-icon svgIcon="trash-2" class="size-4" />
              </button>
            </div>
          }
          @if (!centrosArray.length) { <p class="text-xs text-slate-400">Sin centros registrados.</p> }
        </div>
      </section>

      <!-- ── Documentos ── -->
      <section class="rounded-2xl border border-slate-200/70 dark:border-slate-700 p-4">
        <div class="flex items-center justify-between mb-1">
          <h3 class="text-sm font-semibold text-slate-800 dark:text-slate-200">Documentos</h3>
        </div>
        <p class="text-[11px] text-slate-400 mb-3">Formatos: PDF, JPG, PNG · máx. 10 MB · (*) obligatorio.</p>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
          @for (d of tiposDocumento; track d.tipo) {
            <div class="doc-card" [class.doc-card--filled]="hasFile(d.tipo)"
                 [class.doc-card--empty]="!hasFile(d.tipo)">

              <div class="flex items-center justify-between gap-2 mb-2">
                <span class="text-xs font-medium text-slate-600 dark:text-slate-300 truncate">
                  {{ d.label }}@if (d.requerido) { <span class="text-primary-600">&nbsp;*</span> }
                </span>
                @if (hasFile(d.tipo)) {
                  <div class="flex items-center gap-0.5 shrink-0">
                    <button mat-icon-button type="button" class="!w-6 !h-6" (click)="preview(d.tipo)"
                            [attr.aria-label]="'Ver ' + d.label" matTooltip="Ver">
                      <mat-icon svgIcon="eye" class="size-3.5 text-slate-500" />
                    </button>
                    <button mat-icon-button type="button" class="!w-6 !h-6" (click)="removeFile(d.tipo, fileInput)"
                            [attr.aria-label]="'Quitar ' + d.label" matTooltip="Quitar">
                      <mat-icon svgIcon="x" class="size-3.5 text-rose-400" />
                    </button>
                  </div>
                } @else if (docExistente(d.tipo); as ex) {
                  <div class="flex items-center gap-0.5 shrink-0">
                    <button mat-icon-button type="button" class="!w-6 !h-6" (click)="previewExisting.emit(ex)"
                            [attr.aria-label]="'Previsualizar ' + d.label" matTooltip="Previsualizar">
                      <mat-icon svgIcon="eye" class="size-3.5 text-slate-500" />
                    </button>
                    <button mat-icon-button type="button" class="!w-6 !h-6" (click)="download.emit(ex)"
                            [attr.aria-label]="'Descargar ' + d.label" matTooltip="Descargar">
                      <mat-icon svgIcon="download" class="size-3.5 text-emerald-500" />
                    </button>
                  </div>
                }
              </div>

              @if (archivos[d.tipo]; as f) {
                <!-- Estado: archivo nuevo adjunto -->
                <button type="button" class="doc-card__body" (click)="preview(d.tipo)"
                        [attr.aria-label]="'Previsualizar ' + f.name">
                  @if (isImage(d.tipo)) {
                    <img [src]="previewUrls[d.tipo]" [alt]="f.name" class="doc-thumb" />
                  } @else {
                    <span class="doc-thumb doc-thumb--file">
                      <mat-icon svgIcon="file-text" class="size-7 text-slate-400" />
                    </span>
                  }
                  <span class="doc-card__name" [title]="f.name">{{ f.name }}</span>
                  <span class="doc-card__meta">{{ tamanioArchivo(f) }}</span>
                </button>
              } @else if (docExistente(d.tipo); as ex) {
                <!-- Estado: documento ya cargado en el backend (clic = previsualizar) -->
                <button type="button" class="doc-card__body" (click)="previewExisting.emit(ex)"
                        [attr.aria-label]="'Previsualizar documento cargado ' + d.label">
                  <span class="doc-thumb doc-thumb--file">
                    <mat-icon svgIcon="circle-check" class="size-6 text-emerald-500" />
                  </span>
                  <span class="doc-card__name" [title]="ex.nombreOriginal">{{ ex.nombreOriginal || 'Documento cargado' }}</span>
                  <span class="doc-card__meta">{{ formatBytes(ex.tamanioBytes) }} · cargado</span>
                </button>
              } @else {
                <!-- Estado: vacío -->
                <div class="doc-card__body doc-card__body--drop" role="button" tabindex="0"
                     [attr.aria-label]="'Subir ' + d.label"
                     (click)="fileInput.click()"
                     (keydown.enter)="fileInput.click()" (keydown.space)="$event.preventDefault(); fileInput.click()">
                  <mat-icon svgIcon="upload" class="size-6 text-slate-300 mb-1" />
                  <span class="text-[11px] text-slate-400">Subir archivo</span>
                  <span class="mt-1 text-[10px] font-medium px-1.5 py-0.5 rounded-full"
                        [class]="d.requerido ? 'bg-amber-50 text-amber-600' : 'bg-slate-100 text-slate-400'">
                    {{ d.requerido ? 'Requerido' : 'Opcional' }}
                  </span>
                </div>
              }

              @if (docError[d.tipo]) {
                <p class="mt-1.5 text-[11px] text-rose-600">{{ docError[d.tipo] }}</p>
              }

              <input #fileInput type="file" accept=".pdf,.jpg,.jpeg,.png" class="hidden"
                     (change)="onFile(d.tipo, $event)" />
            </div>
          }
        </div>
      </section>

      @if (!embedded) {
        <div class="flex justify-end">
          <button type="submit" mat-flat-button color="primary" class="!rounded-lg !px-5 !h-9 !text-sm !font-medium" [disabled]="saving">
            {{ saving ? 'Guardando...' : 'Guardar perfil completo' }}
          </button>
        </div>
      }
    </form>
  `,
})
export class PerfilCompletoFormComponent implements OnDestroy {
  private _fb = inject(FormBuilder);
  private _dialog = inject(MatDialog);

  @Input() cargos: CatalogoItem[] = [];
  @Input() centros: CatalogoItem[] = [];
  @Input() saving = false;
  /** En modo embebido se oculta el botón propio; el padre llama a collect(). */
  @Input() embedded = false;

  /** Prefill con el perfil actual (historiales + documentos ya cargados). */
  @Input() set value(v: PerfilCompleto | null) {
    this._documentos = v?.documentos ?? [];
    this.rebuild(v);
  }

  /** Emite el FormData multipart listo para enviar. */
  @Output() save = new EventEmitter<FormData>();
  /** Emite el documento a descargar/previsualizar. */
  @Output() download = new EventEmitter<DocumentoItem>();
  /** Previsualizar (in-app) un documento ya cargado en el backend. */
  @Output() previewExisting = new EventEmitter<DocumentoItem>();

  protected readonly tiposDocumento = TIPOS_DOCUMENTO_PERFIL;
  protected readonly formatBytes = formatTamanio;

  protected archivos: Record<string, File> = {};
  /** Object URLs de los archivos nuevos (para miniatura/previsualización). */
  protected previewUrls: Record<string, string> = {};
  /** Mensaje de error por tarjeta (tipo/tamaño). */
  protected docError: Record<string, string> = {};

  private _documentos: DocumentoItem[] = [];
  private _localError: string | null = null;

  form: FormGroup = this._fb.group({
    cargos: this._fb.array([]),
    centros: this._fb.array([]),
  });

  get cargosArray(): FormArray { return this.form.get('cargos') as FormArray; }
  get centrosArray(): FormArray { return this.form.get('centros') as FormArray; }

  localError(): string | null { return this._localError; }

  ngOnDestroy(): void {
    Object.values(this.previewUrls).forEach((url) => this._revoke(url));
  }

  // ── Historiales ────────────────────────────────────────────────────────────

  private rebuild(v: PerfilCompleto | null): void {
    this.cargosArray.clear();
    this.centrosArray.clear();
    (v?.cargos ?? []).forEach((c) => this.cargosArray.push(this._cargoGroup(
      c.cargoId ?? '', c.fechaInicio ?? '', c.fechaFin ?? '', !!c.actual,
    )));
    (v?.centrosLaborales ?? []).forEach((c) => this.centrosArray.push(this._centroGroup(
      c.centroLaboralId ?? '', c.fechaInicio ?? '', c.fechaFin ?? '', !!c.actual,
    )));
  }

  private _cargoGroup(cargoId: string, ini: string, fin: string, actual: boolean): FormGroup {
    const g = this._fb.group({
      cargoId: [cargoId], fechaInicio: [ini], fechaFin: [{ value: fin, disabled: actual }], actual: [actual],
    });
    return g;
  }
  private _centroGroup(centroId: string, ini: string, fin: string, actual: boolean): FormGroup {
    return this._fb.group({
      centroLaboralId: [centroId], fechaInicio: [ini], fechaFin: [{ value: fin, disabled: actual }], actual: [actual],
    });
  }

  addCargo(): void { this.cargosArray.push(this._cargoGroup('', '', '', false)); }
  removeCargo(i: number): void { this.cargosArray.removeAt(i); }

  addCentro(): void { this.centrosArray.push(this._centroGroup('', '', '', false)); }
  removeCentro(i: number): void { this.centrosArray.removeAt(i); }

  /**
   * Marca "actual" en la fila `i`: sólo una fila puede ser actual, y la fila
   * actual deshabilita + limpia su fecha de fin (via disable() del control).
   */
  syncActual(array: FormArray, i: number): void {
    const chosen = !!array.at(i).get('actual')?.value;
    array.controls.forEach((c, idx) => {
      if (chosen && idx !== i) { c.get('actual')?.setValue(false, { emitEvent: false }); }
      const isActual = !!c.get('actual')?.value;
      const fin = c.get('fechaFin');
      if (isActual) {
        fin?.reset('', { emitEvent: false });
        fin?.disable({ emitEvent: false });
      } else {
        fin?.enable({ emitEvent: false });
      }
    });
  }

  // ── Documentos ─────────────────────────────────────────────────────────────

  docExistente(tipo: string): DocumentoItem | undefined {
    return this._documentos.find((d) => d.tipoDocumento === tipo);
  }

  hasFile(tipo: string): boolean { return !!this.archivos[tipo]; }
  isImage(tipo: string): boolean { return this.archivos[tipo]?.type?.startsWith('image/') ?? false; }
  tamanioArchivo(f: File): string { return formatTamanio(f.size); }

  onFile(tipo: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) { return; }

    if (file.type && !TIPOS_PERMITIDOS.includes(file.type)) {
      this.docError[tipo] = 'Tipo no permitido (solo PDF, JPG, PNG).';
      input.value = '';
      return;
    }
    if (file.size > MAX_FILE_BYTES) {
      this.docError[tipo] = 'Supera el máximo de 10 MB.';
      input.value = '';
      return;
    }

    delete this.docError[tipo];
    this._revoke(this.previewUrls[tipo]);
    this.archivos[tipo] = file;
    this.previewUrls[tipo] = this._objectUrl(file);
    this._localError = null;
  }

  removeFile(tipo: string, input?: HTMLInputElement): void {
    this._revoke(this.previewUrls[tipo]);
    delete this.previewUrls[tipo];
    delete this.archivos[tipo];
    delete this.docError[tipo];
    if (input) { input.value = ''; }
  }

  preview(tipo: string): void {
    const file = this.archivos[tipo];
    const url = this.previewUrls[tipo];
    if (!file || !url) { return; }
    this._dialog.open(DocumentoPreviewDialogComponent, {
      panelClass: ['dialog-anim'],
      autoFocus: false,
      maxWidth: '92vw',
      data: { nombre: file.name, url, esPdf: file.type === 'application/pdf', mime: file.type },
    });
  }

  private _objectUrl(file: File): string {
    return typeof URL !== 'undefined' && URL.createObjectURL ? URL.createObjectURL(file) : '';
  }
  private _revoke(url?: string): void {
    if (url && typeof URL !== 'undefined' && URL.revokeObjectURL) { URL.revokeObjectURL(url); }
  }

  // ── Recolección / envío ──────────────────────────────────────────────────────

  /**
   * Valida y devuelve los datos + archivos del perfil, o null si hay error
   * (queda visible en localError). Lo usa el padre en modo embebido para
   * combinarlos con otros datos antes de enviar.
   */
  collect(): { datos: PerfilCompletoData; archivos: Record<string, File> } | null {
    this._localError = null;

    // Documentos obligatorios: satisfechos por archivo nuevo o documento ya cargado.
    for (const d of this.tiposDocumento.filter((t) => t.requerido)) {
      if (!this.archivos[d.tipo] && !this.docExistente(d.tipo)) {
        this._localError = `Falta el documento obligatorio: ${d.label}`;
        return null;
      }
    }

    // Validación de selección de catálogo en cada fila.
    if (this.cargosArray.controls.some((c) => !c.get('cargoId')?.value)) {
      this._localError = 'Hay filas de cargo sin seleccionar un cargo del catálogo';
      return null;
    }
    if (this.centrosArray.controls.some((c) => !c.get('centroLaboralId')?.value)) {
      this._localError = 'Hay filas de centro laboral sin seleccionar un centro del catálogo';
      return null;
    }

    // getRawValue() incluye fechaFin aunque esté deshabilitada (fila "actual" → null).
    const cargos = this.cargosArray.getRawValue() as FilaHistorial[];
    const centros = this.centrosArray.getRawValue() as FilaHistorial[];
    const datos: PerfilCompletoData = {
      cargos: cargos.map((c) => ({
        cargoId: c.cargoId ?? '',
        fechaInicio: c.fechaInicio || null,
        fechaFin: c.actual ? null : (c.fechaFin || null),
        actual: !!c.actual,
      })),
      centrosLaborales: centros.map((c) => ({
        centroLaboralId: c.centroLaboralId ?? '',
        fechaInicio: c.fechaInicio || null,
        fechaFin: c.actual ? null : (c.fechaFin || null),
        actual: !!c.actual,
      })),
    };

    return { datos, archivos: this.archivos };
  }

  submit(): void {
    const payload = this.collect();
    if (!payload) { return; }
    this.save.emit(buildPerfilFormData(payload.datos, payload.archivos));
  }

  /**
   * Rellena historiales y documentos obligatorios con datos simulados (pruebas).
   * Genera archivos PDF dummy para DNI y Partida de nacimiento y una fila de
   * cargo/centro "actual" usando el primer ítem de cada catálogo.
   */
  fillSample(): void {
    this._localError = null;

    if (this.cargos.length) {
      this.cargosArray.clear();
      this.cargosArray.push(this._cargoGroup(this.cargos[0].id, '2020-03-01', '', true));
    }
    if (this.centros.length) {
      this.centrosArray.clear();
      this.centrosArray.push(this._centroGroup(this.centros[0].id, '2020-03-01', '', true));
    }

    const pdf = (name: string) =>
      new File(['%PDF-1.4\n% Documento simulado para pruebas\n'], name, { type: 'application/pdf' });
    (['DNI', 'PARTIDA_NACIMIENTO'] as const).forEach((tipo) => {
      const file = pdf(`${tipo.toLowerCase()}-simulado.pdf`);
      delete this.docError[tipo];
      this._revoke(this.previewUrls[tipo]);
      this.archivos[tipo] = file;
      this.previewUrls[tipo] = this._objectUrl(file);
    });
  }
}
