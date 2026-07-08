import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { forkJoin } from 'rxjs';
import { PerfilService } from '../services/perfil.service';
import { CargoService } from '@/app/shared/catalogo/cargo.service';
import { CentroLaboralService } from '@/app/shared/catalogo/centro-laboral.service';
import { PerfilCompletoFormComponent } from '@/app/shared/perfil-completo/perfil-completo-form.component';
import { CatalogoItem, DocumentoItem, PerfilCompleto } from '@/app/shared/perfil-completo/perfil-completo.model';
import { abrirBlob } from '@/app/shared/perfil-completo/download.util';
import { GRADOS_ACADEMICOS_LABELS } from '@/app/views/dashboard/personas/models/persona.model';
import { RegistroTemaService } from '@/app/views/dashboard/registro-tema/services/registro-tema.service';
import { etiquetaEstadoDerivado, Tema } from '@/app/views/dashboard/registro-tema/models/registro-tema.model';

@Component({
  selector: 'app-mi-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, PerfilCompletoFormComponent],
  template: `
    <div class="page">

      <!-- ── Header ── -->
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Cuenta</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Mi perfil</span>
          </div>
          <h1 class="page-title">Mi perfil</h1>
        </div>
      </div>

      <!-- ── Content ── -->
      <div class="page-content">
        <div class="mx-auto max-w-[1360px] px-6 py-6 space-y-5">

      @if (loading()) {
        <p class="text-sm text-slate-400">Cargando...</p>
      } @else if (perfil(); as p) {

        @if (okMsg()) {
          <div class="flex items-start gap-2 rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-sm text-emerald-700">
            <mat-icon svgIcon="circle-check" class="size-4 mt-0.5 shrink-0" />
            <span>{{ okMsg() }}</span>
          </div>
        }
        @if (errorMsg()) {
          <div role="alert" class="flex items-start gap-2 rounded-xl bg-rose-50 border border-rose-200 px-4 py-3 text-sm text-rose-700">
            <mat-icon svgIcon="circle-alert" class="size-4 mt-0.5 shrink-0" />
            <span>{{ errorMsg() }}</span>
          </div>
        }

        <!-- Datos de solo lectura -->
        <section class="form-card">
          <header class="form-card__head">
            <h2 class="form-card__title">Datos (solo lectura)</h2>
          </header>
          <div class="form-grid">
            <div>
              <span class="form-label">Nombres</span>
              <p class="text-sm text-slate-700">{{ p.nombres || '—' }}</p>
            </div>
            <div>
              <span class="form-label">Apellidos</span>
              <p class="text-sm text-slate-700">{{ p.apellidos || '—' }}</p>
            </div>
            <div>
              <span class="form-label">Documento</span>
              <p class="text-sm text-slate-700">{{ p.numeroDocumento || '—' }}</p>
            </div>
            <div>
              <span class="form-label">Usuario</span>
              <p class="text-sm text-slate-700">{{ p.username || '—' }}</p>
            </div>
            <div>
              <span class="form-label">Perfiles</span>
              <p class="text-sm text-slate-700">{{ (p.perfiles || []).join(', ') || '—' }}</p>
            </div>
            <div>
              <span class="form-label">Roles</span>
              <p class="text-sm text-slate-700">{{ (p.roles || []).join(', ') || '—' }}</p>
            </div>
          </div>
        </section>

        <!-- Grados académicos (solo lectura) -->
        <section class="form-card">
          <header class="form-card__head">
            <h2 class="form-card__title">Grados académicos</h2>
          </header>
          @if ((p.gradosAcademicos ?? []).length) {
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="text-left text-slate-400 border-b border-slate-100">
                    <th class="py-1.5 pr-3 font-medium">Grado</th>
                    <th class="py-1.5 pr-3 font-medium">Año</th>
                    <th class="py-1.5 pr-3 font-medium">Universidad</th>
                    <th class="py-1.5 font-medium text-center">Principal</th>
                  </tr>
                </thead>
                <tbody>
                  @for (g of p.gradosAcademicos; track g.id ?? g.grado) {
                    <tr class="border-b border-slate-50">
                      <td class="py-1.5 pr-3 text-slate-700">{{ gradoLabel(g.grado) }}</td>
                      <td class="py-1.5 pr-3 text-slate-500 tabular-nums">{{ g.anio ?? '—' }}</td>
                      <td class="py-1.5 pr-3 text-slate-500">{{ g.universidad ?? '—' }}</td>
                      <td class="py-1.5 text-center">
                        @if (g.principal) {
                          <span class="inline-block rounded-full bg-emerald-50 text-emerald-600 text-xs px-2 py-0.5">Principal</span>
                        } @else { <span class="text-slate-300">—</span> }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <p class="text-xs text-slate-400">No tienes grados académicos registrados.</p>
          }
        </section>

        <!-- Tema de investigación (solo lectura) -->
        @if (tema(); as t) {
          <section class="form-card">
            <header class="form-card__head">
              <h2 class="form-card__title">Tema de investigación</h2>
              <span class="text-[11px] font-medium px-2 py-0.5 rounded-full"
                    [class]="t.estadoDerivado === 'SIN_ASESOR' ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600'">
                {{ estadoTema(t.estadoDerivado) }}
              </span>
            </header>
            <p class="text-sm font-medium text-slate-700">{{ t.titulo }}</p>
            @if (t.resumen) { <p class="text-xs text-slate-500 mt-1">{{ t.resumen }}</p> }
            <div class="form-grid mt-3">
              <div>
                <span class="form-label">Línea</span>
                <p class="text-sm text-slate-700">{{ t.lineaNombre ?? '—' }}</p>
              </div>
              <div>
                <span class="form-label">Nivel</span>
                <p class="text-sm text-slate-700">{{ t.nivel ?? '—' }}</p>
              </div>
              <div>
                <span class="form-label">Registrado</span>
                <p class="text-sm text-slate-700">{{ t.fechaRegistro ? (t.fechaRegistro | date:'dd/MM/yyyy') : '—' }}</p>
              </div>
            </div>
          </section>
        } @else if (temaCargado()) {
          <section class="form-card">
            <header class="form-card__head">
              <h2 class="form-card__title">Tema de investigación</h2>
            </header>
            <p class="text-sm text-slate-400">Aún no tienes un tema registrado. El coordinador lo registrará por ti.</p>
          </section>
        }

        <!-- Edición de campos permitidos -->
        <section class="form-card">
          <header class="form-card__head">
            <h2 class="form-card__title">Editar mis datos de contacto</h2>
          </header>
          <form [formGroup]="form" (ngSubmit)="guardar()">
            <div class="form-grid">
              <div>
                <label class="form-label" for="mp-email">Email personal</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput id="mp-email" formControlName="emailPersonal" />
                </mat-form-field>
              </div>
              <div>
                <label class="form-label" for="mp-cel">Celular</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput id="mp-cel" formControlName="celular" />
                </mat-form-field>
              </div>
              <div>
                <label class="form-label" for="mp-orcid">ORCID</label>
                <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
                  <input matInput id="mp-orcid" formControlName="orcid" />
                </mat-form-field>
              </div>
            </div>
            <div class="flex justify-end mt-3">
              <button type="submit" mat-flat-button color="primary"
                      class="!rounded-lg !px-5 !h-9 !text-sm !font-medium" [disabled]="saving()">
                {{ saving() ? 'Guardando...' : 'Guardar cambios' }}
              </button>
            </div>
          </form>
        </section>

        <!-- Perfil completo: cargos, centros laborales y documentos -->
        <section class="form-card">
          <header class="form-card__head">
            <h2 class="form-card__title">Mis cargos, centros laborales y documentos</h2>
          </header>

          @if (perfilOk()) {
            <div class="mb-3 flex items-start gap-2 rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-sm text-emerald-700">
              <mat-icon svgIcon="circle-check" class="size-4 mt-0.5 shrink-0" />
              <span>{{ perfilOk() }}</span>
            </div>
          }
          @if (perfilError()) {
            <div class="mb-3 flex items-start gap-2 rounded-xl bg-rose-50 border border-rose-200 px-4 py-3 text-sm text-rose-700">
              <mat-icon svgIcon="circle-alert" class="size-4 mt-0.5 shrink-0" />
              <span>{{ perfilError() }}</span>
            </div>
          }

          <app-perfil-completo-form
            [cargos]="cargos()" [centros]="centros()" [value]="perfilCompleto()" [saving]="savingPerfil()"
            (save)="guardarPerfil($event)" (download)="descargar($event)" />
        </section>

      } @else {
        <p class="text-sm text-red-600">No se encontró tu perfil. Tu usuario no tiene una persona asociada.</p>
      }
        </div>
      </div>
    </div>
  `,
})
export class MiPerfilComponent implements OnInit {
  protected gradoLabel(grado: string): string {
    return GRADOS_ACADEMICOS_LABELS[grado] ?? grado;
  }

  private _service = inject(PerfilService);
  private _cargoService = inject(CargoService);
  private _centroService = inject(CentroLaboralService);
  private _temaService = inject(RegistroTemaService);
  private _fb = inject(FormBuilder);

  protected perfil = signal<any | null>(null);
  protected tema = signal<Tema | null>(null);
  protected temaCargado = signal(false);
  protected loading = signal(true);
  protected saving = signal(false);
  protected okMsg = signal<string | null>(null);
  protected errorMsg = signal<string | null>(null);

  // Perfil completo (historiales + documentos)
  protected cargos = signal<CatalogoItem[]>([]);
  protected centros = signal<CatalogoItem[]>([]);
  protected perfilCompleto = signal<PerfilCompleto | null>(null);
  protected savingPerfil = signal(false);
  protected perfilOk = signal<string | null>(null);
  protected perfilError = signal<string | null>(null);

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this._fb.group({
      emailPersonal: ['', Validators.email],
      celular: [''],
      orcid: [''],
    });
    this.cargar();
    this.cargarTema();
  }

  private cargarTema(): void {
    this._temaService.miTema$().subscribe({
      next: (res: any) => { this.tema.set(res?.data ?? null); this.temaCargado.set(true); },
      error: () => { this.tema.set(null); this.temaCargado.set(true); },
    });
  }

  protected estadoTema = etiquetaEstadoDerivado;

  cargar(): void {
    this.loading.set(true);
    this._service.obtener$().subscribe({
      next: (res) => {
        const p = res?.data ?? res ?? null;
        this.perfil.set(p);
        if (p) {
          this.form.patchValue({
            emailPersonal: p.emailPersonal ?? '',
            celular: p.celular ?? '',
            orcid: p.orcid ?? '',
          });
          this.cargarCatalogos();
          this.cargarPerfilCompleto();
        }
        this.loading.set(false);
      },
      error: () => { this.perfil.set(null); this.loading.set(false); },
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

  private cargarPerfilCompleto(): void {
    this._service.obtenerCompleto$().subscribe({
      next: (res) => this.perfilCompleto.set(res?.data ?? res ?? null),
      error: () => this.perfilCompleto.set(null),
    });
  }

  guardarPerfil(formData: FormData): void {
    this.perfilOk.set(null);
    this.perfilError.set(null);
    this.savingPerfil.set(true);
    this._service.guardarCompleto$(formData).subscribe({
      next: (res) => {
        this.savingPerfil.set(false);
        this.perfilOk.set('Perfil completo actualizado');
        this.perfilCompleto.set(res?.data ?? res ?? this.perfilCompleto());
      },
      error: (err) => {
        this.savingPerfil.set(false);
        this.perfilError.set(err?.error?.message || err?.error?.error || 'No se pudo guardar el perfil completo');
      },
    });
  }

  descargar(doc: DocumentoItem): void {
    this._service.descargarDocumento$(doc.id).subscribe({
      next: (blob) => abrirBlob(blob, doc.nombreOriginal),
      error: () => this.perfilError.set('No se pudo descargar el documento'),
    });
  }

  guardar(): void {
    this.okMsg.set(null);
    this.errorMsg.set(null);
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this._service.actualizar$(this.form.value).subscribe({
      next: (res) => {
        this.saving.set(false);
        this.okMsg.set('Perfil actualizado');
        this.perfil.set(res?.data ?? res ?? this.perfil());
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMsg.set(err?.error?.message || err?.error?.error || 'No se pudo actualizar');
      },
    });
  }
}