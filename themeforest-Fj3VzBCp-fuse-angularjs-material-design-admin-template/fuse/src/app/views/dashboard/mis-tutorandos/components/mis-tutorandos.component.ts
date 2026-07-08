import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { MisTutorandosService } from '../services/mis-tutorandos.service';
import { SugerirAsesoresDialogComponent } from './sugerir-asesores-dialog.component';
import { claseEstado, etiquetaEstado, MisTutorandosResumen, Tutorando } from '../models/mis-tutorandos.model';

@Component({
  selector: 'app-mis-tutorandos',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule,
    MatIconModule, MatButtonModule, MatDialogModule, PaginationControlsComponent,
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Proceso de Tesis</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Mis tutorandos</span>
          </div>
          <h1 class="page-title">Mis tutorandos</h1>
          @if (resumen(); as r) {
            <p class="text-sm text-slate-500 mt-1">
              {{ r.tutorNombre }} · <span class="font-medium text-slate-700">{{ r.total }} de {{ r.cupoMaximo }} cupos</span>
            </p>
          }
        </div>
      </div>

      <div class="page-toolbar">
        <form [formGroup]="filterForm">
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="w-72">
            <mat-icon matPrefix svgIcon="search" class="size-4 text-slate-400" />
            <input matInput formControlName="buscar" placeholder="Buscar por nombre o código…" />
          </mat-form-field>
        </form>
        @if (!loading()) {
          <span class="text-[11px] text-slate-400">{{ total() }} tutorando(s)</span>
        }
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
                <th class="w-24 text-center">Año ingreso</th>
                <th class="w-28">Inicio tutoría</th>
                <th class="w-32 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (t of rows(); track t.estudianteId) {
                <tr>
                  <td class="font-medium text-slate-700">{{ t.apellidos }}, {{ t.nombres }}</td>
                  <td class="text-slate-500 text-sm">{{ t.codigoSistema ?? t.codMatricula ?? '—' }}</td>
                  <td class="text-slate-500 text-sm">{{ t.programaNombre ?? '—' }}</td>
                  <td>
                    <span class="text-[11px] font-medium px-2 py-0.5 rounded-full" [class]="claseEstado(t.estadoDerivado)">
                      {{ etiquetaEstado(t.estadoDerivado) }}
                    </span>
                    @if (t.tesisTitulo) {
                      <p class="text-[11px] text-slate-400 mt-0.5 max-w-[240px] truncate" [title]="t.tesisTitulo">{{ t.tesisTitulo }}</p>
                    }
                  </td>
                  <td class="text-slate-500 text-sm text-center">{{ t.anioIngreso ?? '—' }}</td>
                  <td class="text-slate-400 text-sm">{{ t.fechaInicio ? (t.fechaInicio | date:'dd/MM/yyyy') : '—' }}</td>
                  <td class="text-right">
                    <button mat-stroked-button class="!h-7 !text-xs !min-w-0 !px-3" (click)="sugerir(t)">
                      Sugerir asesores
                    </button>
                  </td>
                </tr>
              }
              @empty {
                <tr>
                  <td colspan="7" class="text-center">
                    <div class="table-empty">
                      <mat-icon svgIcon="inbox" class="size-10 text-slate-200" />
                      <p class="table-empty__text">Sin tutorandos</p>
                      <p class="table-empty__subtext">Aún no tienes estudiantes en tutoría vigente</p>
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
export class MisTutorandosComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _svc = inject(MisTutorandosService);
  private _dialog = inject(MatDialog);

  protected resumen = signal<MisTutorandosResumen | null>(null);
  protected rows = signal<Tutorando[]>([]);
  protected total = signal(0);
  protected page = signal(0);
  protected size = signal(20);
  protected loading = signal(false);

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ buscar: [''] });
    this.filterForm.get('buscar')!.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => { this.page.set(0); this.load(); });
    this.loadResumen();
    this.load();
  }

  loadResumen(): void {
    this._svc.resumen$().subscribe({
      next: (res: any) => this.resumen.set(res?.data ?? res ?? null),
    });
  }

  load(): void {
    this.loading.set(true);
    this._svc.tutorandos$(this.filterForm.value.buscar, this.page(), this.size()).subscribe({
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
    this.load();
  }

  sugerir(t: Tutorando): void {
    this._dialog.open(SugerirAsesoresDialogComponent, {
      data: { estudianteId: t.estudianteId, estudianteNombre: `${t.apellidos}, ${t.nombres}`, lineaNombre: t.lineaNombre },
      autoFocus: false,
    });
  }

  protected etiquetaEstado = etiquetaEstado;
  protected claseEstado = claseEstado;
}
