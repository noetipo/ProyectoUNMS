import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EstudianteService } from '../../services/estudiante.service';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { DocumentosComponent } from '../documentos/documentos.component';
import { Estudiante } from '../../models/student.models';

@Component({
  selector: 'app-estudiante-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule, DocumentosComponent],
  template: `
    <div class="page">

      <!-- ── Header ── -->
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Posgrado</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <a routerLink="/admin/student/estudiantes" class="hover:text-slate-600 transition-colors">Estudiantes</a>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Detalle</span>
          </div>
          <h1 class="page-title">
            @if (estudiante()) {
              {{ estudiante()!.apellidoPaterno }} {{ estudiante()!.apellidoMaterno }}, {{ estudiante()!.nombres }}
            } @else {
              Cargando…
            }
          </h1>
        </div>
        @if (estudiante()) {
          <div class="flex items-center gap-2">
            <button mat-button class="!text-rose-500 !text-sm" (click)="onDelete()">
              <mat-icon svgIcon="trash" class="size-3.5 mr-1" /> Eliminar
            </button>
            <a [routerLink]="['/admin/student/estudiantes', estudiante()!.id, 'constancia']"
              mat-stroked-button
              class="!text-slate-600 !border-slate-300 !text-xs !h-8 !px-3 !rounded-lg">
              <mat-icon svgIcon="file-text" class="size-3.5 mr-1" /> Constancia
            </a>
            <a [routerLink]="['/admin/student/estudiantes', estudiante()!.id, 'editar']" class="btn-dark">
              <mat-icon svgIcon="pencil" class="size-3.5" /> Editar
            </a>
          </div>
        }
      </div>

      <!-- ── Loading skeleton ── -->
      @if (loading()) {
        <div class="page-content">
          <div class="max-w-5xl mx-auto space-y-5 py-4">
            @for (_ of [1,2,3]; track $index) {
              <div class="rounded-xl border border-slate-100 bg-white p-6">
                <div class="anim-shimmer h-3 w-28 rounded mb-4"></div>
                <div class="grid grid-cols-3 gap-4">
                  @for (__ of [1,2,3,4,5,6]; track $index) {
                    <div class="space-y-1">
                      <div class="anim-shimmer h-2 w-16 rounded"></div>
                      <div class="anim-shimmer h-4 w-full rounded"></div>
                    </div>
                  }
                </div>
              </div>
            }
          </div>
        </div>
      }

      <!-- ── Error ── -->
      @if (errorMsg()) {
        <div class="mx-5 my-4 rounded-lg bg-rose-50 border border-rose-200 px-4 py-3 text-sm text-rose-700">
          {{ errorMsg() }}
        </div>
      }

      <!-- ── Content ── -->
      @if (estudiante(); as est) {
        <div class="page-content overflow-y-auto">
          <div class="max-w-5xl mx-auto space-y-5 py-4 px-1">

            <!-- Info Personal -->
            <div class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
              <h3 class="text-sm font-semibold text-slate-700 mb-5 flex items-center gap-2">
                <mat-icon svgIcon="user" class="size-4 text-slate-400" />
                Datos Personales
              </h3>
              <dl class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-x-4 gap-y-4 text-sm">

                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Nombres</dt>
                  <dd class="font-medium text-slate-700 mt-0.5">{{ est.nombres }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Ap. Paterno</dt>
                  <dd class="font-medium text-slate-700 mt-0.5">{{ est.apellidoPaterno }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Ap. Materno</dt>
                  <dd class="font-medium text-slate-700 mt-0.5">{{ est.apellidoMaterno ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Tipo Documento</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.tipoDocumento ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Nro. Documento</dt>
                  <dd class="font-mono text-slate-600 mt-0.5">{{ est.numeroDocumento ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Fecha Nac.</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.fechaNacimiento ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Sexo</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.sexo ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Estado Civil</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.estadoCivil ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Nacionalidad</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.nacionalidad ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Discapacidad</dt>
                  <dd class="mt-0.5">
                    <span class="status-badge"
                      [class.status-badge--active]="est.discapacidad"
                      [class.status-badge--inactive]="!est.discapacidad">
                      <span class="status-dot"
                        [class.status-dot--active]="est.discapacidad"
                        [class.status-dot--inactive]="!est.discapacidad"></span>
                      {{ est.discapacidad ? 'Sí' : 'No' }}
                    </span>
                  </dd>
                </div>

              </dl>
            </div>

            <!-- Contacto -->
            <div class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
              <h3 class="text-sm font-semibold text-slate-700 mb-4 flex items-center gap-2">
                <mat-icon svgIcon="phone" class="size-4 text-slate-400" />
                Contacto
              </h3>
              <dl class="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Celular</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.celular ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Email Personal</dt>
                  <dd class="font-mono text-slate-600 mt-0.5 truncate">{{ est.emailPersonal ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Email Institucional</dt>
                  <dd class="font-mono text-slate-600 mt-0.5 truncate">{{ est.emailInstitucional ?? '—' }}</dd>
                </div>
              </dl>
            </div>

            <!-- Académico -->
            <div class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
              <h3 class="text-sm font-semibold text-slate-700 mb-4 flex items-center gap-2">
                <mat-icon svgIcon="book-open" class="size-4 text-slate-400" />
                Datos Académicos
              </h3>
              <dl class="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
                <div class="md:col-span-2">
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Programa de Doctorado</dt>
                  <dd class="font-medium text-slate-700 mt-0.5">{{ est.programaDoctorado?.nombre ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Matrícula</dt>
                  <dd class="font-mono text-slate-600 mt-0.5">{{ est.codMatricula ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Año Ingreso</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.anioIngreso ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Financiamiento</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.financiamiento ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Condición</dt>
                  <dd class="mt-0.5">
                    @if (est.condicion) {
                      <span class="status-badge"
                        [class.status-badge--active]="est.condicion === 'REGULAR'"
                        [class.status-badge--inactive]="est.condicion !== 'REGULAR'">
                        <span class="status-dot"
                          [class.status-dot--active]="est.condicion === 'REGULAR'"
                          [class.status-dot--inactive]="est.condicion !== 'REGULAR'"></span>
                        {{ est.condicion }}
                      </span>
                    } @else { <span class="text-slate-400">—</span> }
                  </dd>
                </div>
              </dl>
            </div>

            <!-- Laboral -->
            <div class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
              <h3 class="text-sm font-semibold text-slate-700 mb-4 flex items-center gap-2">
                <mat-icon svgIcon="briefcase" class="size-4 text-slate-400" />
                Datos Laborales
              </h3>
              <dl class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                <div class="md:col-span-2">
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Centro Laboral</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.centroLaboral?.nombre ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Cargo</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.cargoActual?.nombre ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">Procedencia</dt>
                  <dd class="text-slate-600 mt-0.5">{{ est.procedencia ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-[11px] text-slate-400 uppercase tracking-wide">ORCID</dt>
                  <dd class="font-mono text-[11px] text-slate-500 mt-0.5">{{ est.orcid ?? '—' }}</dd>
                </div>
              </dl>
            </div>

            @if (est.observaciones) {
              <div class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
                <h3 class="text-sm font-semibold text-slate-700 mb-2">Observaciones</h3>
                <p class="text-sm text-slate-600">{{ est.observaciones }}</p>
              </div>
            }

            <!-- Documentos -->
            <app-documentos [estudianteId]="est.id" />

          </div>
        </div>
      }

    </div>
  `,
})
export class EstudianteDetailComponent implements OnInit {
  private _service = inject(EstudianteService);
  private _confirm = inject(ConfirmDialogService);
  private _route   = inject(ActivatedRoute);
  private _router  = inject(Router);

  protected estudiante = signal<Estudiante | null>(null);
  protected loading    = signal(true);
  protected errorMsg   = signal<string | null>(null);

  private id!: string;

  ngOnInit(): void {
    this.id = this._route.snapshot.paramMap.get('id')!;
    this._service.getById(this.id).subscribe({
      next: est => { this.estudiante.set(est); this.loading.set(false); },
      error: e  => { this.errorMsg.set(e.message); this.loading.set(false); },
    });
  }

  onDelete(): void {
    this._confirm.confirmDelete()
      .then(() => {
        this._service.delete(this.id).subscribe(() => {
          this._router.navigate(['/admin/student/estudiantes']);
        });
      })
      .catch(() => {});
  }
}
