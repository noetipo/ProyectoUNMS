import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { SolicitudAsesoriaService } from '../services/solicitud-asesoria.service';
import { ESTADOS_SOLICITUD, SolicitudBandeja } from '../models/asesoria.model';
import { ResponderDialogComponent } from './responder-dialog.component';
import { SolicitudDetalleDialogComponent } from './solicitud-detalle-dialog.component';

@Component({
  selector: 'app-bandeja-asesoria',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatSelectModule,
    MatButtonModule, MatIconModule, PaginationControlsComponent,
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Proceso de Tesis</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Solicitudes de asesoría</span>
          </div>
          <h1 class="page-title">Bandeja de solicitudes</h1>
        </div>
      </div>

      <div class="page-toolbar">
        <form [formGroup]="filterForm">
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="w-48">
            <mat-select formControlName="estado">
              @for (e of estados; track e) { <mat-option [value]="e">{{ e }}</mat-option> }
            </mat-select>
          </mat-form-field>
        </form>
        @if (!loading()) {
          <span class="text-[11px] text-slate-400">{{ total() }} solicitud(es)</span>
        }
      </div>

      <div class="page-content">
        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>Estudiante</th>
                <th class="w-28">Código</th>
                <th>Programa</th>
                <th>Línea</th>
                <th>Título tentativo</th>
                <th>Fecha</th>
                <th class="w-28 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (s of rows(); track s.id) {
                <tr>
                  <td class="font-medium text-slate-700">{{ s.estudianteApellidos }}, {{ s.estudianteNombres }}</td>
                  <td class="font-mono text-[11px] text-slate-400">{{ s.codigoSistema ?? '—' }}</td>
                  <td class="text-slate-500 text-sm max-w-[180px] truncate">{{ s.programaNombre ?? '—' }}</td>
                  <td class="text-slate-500 text-sm">{{ s.lineaNombre ?? '—' }}</td>
                  <td class="text-slate-500 text-sm max-w-[220px] truncate" [title]="s.mensaje ?? ''">{{ s.tituloTentativo ?? '—' }}</td>
                  <td class="text-slate-400 text-sm">{{ s.fechaSolicitud | date:'dd/MM/yyyy' }}</td>
                  <td>
                    <div class="row-actions">
                      <button mat-icon-button class="!w-7 !h-7" (click)="verDetalle(s)" title="Revisar el tema y decidir">
                        <mat-icon svgIcon="file-search" class="text-[#8C1D2E] size-3.5" />
                      </button>
                      @if (s.estado === 'PENDIENTE') {
                        <button mat-icon-button class="!w-7 !h-7" (click)="aceptar(s)" title="Aceptar">
                          <mat-icon svgIcon="check" class="text-emerald-500 size-3.5" />
                        </button>
                        <button mat-icon-button class="!w-7 !h-7" (click)="rechazar(s)" title="Rechazar">
                          <mat-icon svgIcon="x" class="text-rose-400 size-3.5" />
                        </button>
                      } @else {
                        <span class="text-[11px] font-medium px-2 py-0.5 rounded-full" [class]="estadoClass(s.estado)">
                          {{ s.estado }}
                        </span>
                      }
                    </div>
                  </td>
                </tr>
              }
              @empty {
                <tr>
                  <td colspan="7" class="text-center">
                    <div class="table-empty">
                      <mat-icon svgIcon="inbox" class="size-10 text-slate-200" />
                      <p class="table-empty__text">Sin solicitudes</p>
                      <p class="table-empty__subtext">No hay solicitudes en este estado</p>
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
export class BandejaAsesoriaComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _svc = inject(SolicitudAsesoriaService);
  private _confirm = inject(ConfirmDialogService);
  private _dialog = inject(MatDialog);

  protected readonly estados = ESTADOS_SOLICITUD;
  protected rows = signal<SolicitudBandeja[]>([]);
  protected total = signal(0);
  protected page = signal(0);
  protected size = signal(20);
  protected loading = signal(false);

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ estado: ['PENDIENTE'] });
    this.filterForm.valueChanges.subscribe(() => { this.page.set(0); this.load(); });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this._svc.bandeja$(this.filterForm.value.estado || 'PENDIENTE', this.page(), this.size()).subscribe({
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

  /** Abre el tema del doctorando; desde ahí se puede aceptar o rechazar sin volver a la tabla. */
  verDetalle(s: SolicitudBandeja): void {
    this._dialog.open(SolicitudDetalleDialogComponent, {
      data: s, autoFocus: false, panelClass: ['dialog-rounded', 'dialog-anim'],
    }).afterClosed().subscribe((accion) => {
      if (accion === 'aceptar') { this.aceptar(s); }
      if (accion === 'rechazar') { this.rechazar(s); }
    });
  }

  aceptar(s: SolicitudBandeja): void {
    this._confirm.confirmSave({
      title: 'Aceptar solicitud',
      message: `¿Aceptar la solicitud de ${s.estudianteApellidos}, ${s.estudianteNombres}?`,
    })
      .then(() => this._svc.responder$(s.id, 'ACEPTAR').subscribe({ next: () => this.load() }))
      .catch(() => {});
  }

  rechazar(s: SolicitudBandeja): void {
    const ref = this._dialog.open(ResponderDialogComponent, {
      width: '480px', panelClass: ['dialog-rounded', 'dialog-anim'],
      data: { estudiante: `${s.estudianteApellidos}, ${s.estudianteNombres}`, titulo: s.tituloTentativo },
    });
    ref.afterClosed().subscribe((result) => {
      if (result?.motivo) {
        this._svc.responder$(s.id, 'RECHAZAR', result.motivo).subscribe({ next: () => this.load() });
      }
    });
  }

  estadoClass(estado: string): string {
    switch (estado) {
      case 'ACEPTADA': return 'bg-emerald-50 text-emerald-600';
      case 'RECHAZADA': return 'bg-rose-50 text-rose-600';
      case 'CANCELADA': return 'bg-slate-100 text-slate-500';
      default: return 'bg-amber-50 text-amber-600';
    }
  }
}
