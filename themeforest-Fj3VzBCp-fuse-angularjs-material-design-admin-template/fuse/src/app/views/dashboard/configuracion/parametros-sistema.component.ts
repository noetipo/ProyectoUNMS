import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';

const ROW_STAGGER_MS = 55;
const DIALOG_BASE = { enterAnimationDuration: '0ms', exitAnimationDuration: '220ms' };
const DIALOG_PANEL = ['dialog-rounded', 'dialog-anim'];

interface ParamDialogData { mode: 'new' | 'edit'; item?: any; }

@Component({
  selector: 'app-parametro-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <div class="p-5 w-[500px] max-w-full">
      <h2 class="text-base font-semibold text-slate-800 mb-3">
        {{ data.mode === 'new' ? 'Nuevo parámetro' : 'Editar parámetro' }}
      </h2>
      <form [formGroup]="form" (ngSubmit)="save()" class="flex flex-col gap-1">
        <div>
          <label class="form-label" for="p-clave">Clave <span class="form-required">*</span></label>
          <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
            <input matInput id="p-clave" formControlName="clave" maxlength="60" placeholder="CIUDAD_EMISION" />
          </mat-form-field>
          @if (data.mode === 'edit') {
            <p class="text-[11px] text-slate-400 -mt-1 mb-1">La clave no puede cambiarse.</p>
          }
        </div>
        <div>
          <label class="form-label" for="p-valor">Valor <span class="form-required">*</span></label>
          <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
            <textarea matInput id="p-valor" formControlName="valor" rows="2" maxlength="500"></textarea>
          </mat-form-field>
        </div>
        <div>
          <label class="form-label" for="p-desc">Descripción</label>
          <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
            <input matInput id="p-desc" formControlName="descripcion" maxlength="255" />
          </mat-form-field>
        </div>
        <div class="flex justify-end gap-2 mt-3">
          <button type="button" mat-button (click)="ref.close()">Cancelar</button>
          <button type="submit" mat-flat-button color="primary" class="!rounded-lg !px-5 !h-9 !text-sm">Guardar</button>
        </div>
      </form>
    </div>
  `,
})
export class ParametroFormDialogComponent {
  private _fb = inject(FormBuilder);
  protected ref = inject(MatDialogRef<ParametroFormDialogComponent>);
  protected data = inject<ParamDialogData>(MAT_DIALOG_DATA);

  protected form: FormGroup = this._fb.group({
    clave: [{ value: this.data.item?.clave ?? '', disabled: this.data.mode === 'edit' }, [Validators.required, Validators.maxLength(60)]],
    valor: [this.data.item?.valor ?? '', [Validators.required, Validators.maxLength(500)]],
    descripcion: [this.data.item?.descripcion ?? '', Validators.maxLength(255)],
  });

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.ref.close({ clave: (v.clave ?? '').trim(), valor: (v.valor ?? '').trim(), descripcion: (v.descripcion ?? '').trim() || undefined });
  }
}

@Component({
  selector: 'app-parametros-sistema',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule, PaginationControlsComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Configuración</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Parámetros del sistema</span>
          </div>
          <h1 class="page-title">Parámetros del sistema</h1>
        </div>
        <button type="button" class="btn-dark" (click)="onNew()">
          <mat-icon svgIcon="plus" class="size-3.5" /> Nuevo parámetro
        </button>
      </div>

      <div class="page-toolbar">
        <form [formGroup]="filterForm">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="search" placeholder="Buscar por clave..." />
          </div>
        </form>
        @if (!loading()) { <span class="text-[11px] text-slate-400 anim-fade-in">{{ total() }} registro(s)</span> }
      </div>

      <div class="page-content">
        @if (loading()) {
          <div class="divide-y divide-slate-50">
            @for (_ of skeletonRows; track $index; let i = $index) {
              <div class="skeleton-row" [style.opacity]="1 - i * 0.1">
                <div class="anim-shimmer h-2 w-4 rounded shrink-0"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="120 + (i % 3) * 20"></div>
                <div class="anim-shimmer h-2 rounded flex-1"></div>
                <div class="anim-shimmer h-3.5 w-10 rounded shrink-0"></div>
              </div>
            }
          </div>
        } @else {
          <div class="hidden md:block overflow-x-auto">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="w-8">#</th>
                  <th class="w-56">Clave</th>
                  <th>Valor</th>
                  <th>Descripción</th>
                  <th class="w-20">Estado</th>
                  <th class="w-20 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (item of visibleItems(); track item.id; let i = $index) {
                  <tr [class.anim-flash]="item.id === highlightedId()">
                    <td class="text-slate-400 tabular-nums">{{ (page() * size()) + i + 1 }}</td>
                    <td class="font-mono text-[12px] text-slate-600 font-medium">{{ item.clave }}</td>
                    <td class="text-slate-700 text-sm max-w-[240px] truncate">{{ item.valor }}</td>
                    <td class="text-slate-500 text-sm max-w-[220px] truncate">{{ item.descripcion ?? '—' }}</td>
                    <td>
                      <span class="status-badge" [class.status-badge--active]="item.activo" [class.status-badge--inactive]="!item.activo">
                        <span class="status-dot" [class.status-dot--active]="item.activo" [class.status-dot--inactive]="!item.activo"></span>
                        {{ item.activo ? 'Activo' : 'Inactivo' }}
                      </span>
                    </td>
                    <td>
                      <div class="row-actions">
                        <button mat-icon-button class="!w-7 !h-7" (click)="onEdit(item)">
                          <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                        </button>
                        @if (item.activo) {
                          <button mat-icon-button class="!w-7 !h-7" (click)="onDelete(item)">
                            <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
                @empty {
                  <tr><td colspan="6" class="text-center">
                    <div class="table-empty">
                      <mat-icon svgIcon="heroicons_outline:adjustments-horizontal" class="size-10 text-slate-200" />
                      <p class="table-empty__text">Sin parámetros</p>
                      <p class="table-empty__subtext">Crea el primero con el botón "Nuevo parámetro"</p>
                    </div>
                  </td></tr>
                }
              </tbody>
            </table>
          </div>

          <div class="md:hidden divide-y divide-slate-100">
            @for (item of visibleItems(); track item.id) {
              <div class="px-5 py-3 anim-row-in" [class.anim-flash]="item.id === highlightedId()">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex-1 min-w-0">
                    <p class="font-mono text-[12px] text-slate-600 font-medium truncate">{{ item.clave }}</p>
                    <p class="text-sm text-slate-700 truncate">{{ item.valor }}</p>
                    @if (item.descripcion) { <p class="text-xs text-slate-400 truncate mt-0.5">{{ item.descripcion }}</p> }
                  </div>
                  <div class="flex items-center gap-0.5 shrink-0">
                    <button mat-icon-button class="!w-7 !h-7" (click)="onEdit(item)">
                      <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                    </button>
                    @if (item.activo) {
                      <button mat-icon-button class="!w-7 !h-7" (click)="onDelete(item)">
                        <mat-icon svgIcon="trash" class="text-rose-400 size-3.5" />
                      </button>
                    }
                  </div>
                </div>
              </div>
            }
            @empty { <div class="flex flex-col items-center gap-2 py-16"><p class="text-sm text-slate-400">Sin parámetros</p></div> }
          </div>
        }
      </div>

      @if (!loading()) {
        <div class="page-footer">
          <pagination-controls [totalItems]="total()" [itemsPerPage]="size()" [currentPage]="page()" (paginationChange)="onPage($event)" />
        </div>
      }
    </div>
  `,
})
export class ParametrosSistemaComponent implements OnInit, OnDestroy {
  private _http = inject(HttpClient);
  private _fb = inject(UntypedFormBuilder);
  private _dialog = inject(MatDialog);
  private _confirm = inject(ConfirmDialogService);
  private readonly base = environment.url + END_POINTS.configuracion.parametrosSistema;

  protected rows = signal<any[]>([]);
  protected visibleItems = signal<any[]>([]);
  protected total = signal(0);
  protected page = signal(0);
  protected size = signal(20);
  protected loading = signal(false);
  protected highlightedId = signal<string | null>(null);
  protected readonly skeletonRows = Array(8).fill(null);
  private _timers: ReturnType<typeof setTimeout>[] = [];
  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ search: [''] });
    this.filterForm.valueChanges.subscribe(() => { this.page.set(0); this.load(); });
    this.load();
  }

  ngOnDestroy(): void { this._clear(); }

  load(): void {
    this.loading.set(true);
    let params = new HttpParams().set('page', String(this.page())).set('size', String(this.size()));
    const s = this.filterForm.value.search;
    if (s) params = params.set('search', s);
    this._http.get<any>(this.base, { params }).subscribe({
      next: (res) => {
        const d = res?.data ?? res;
        this.rows.set(d?.content ?? []);
        this.total.set(d?.total ?? d?.totalElements ?? 0);
        this.loading.set(false);
        this._reveal(d?.content ?? []);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PaginationEvent): void { this.page.set(e.page); this.size.set(e.size); this.load(); }

  onNew(): void {
    const ref = this._dialog.open(ParametroFormDialogComponent, { ...DIALOG_BASE, width: '520px', panelClass: DIALOG_PANEL, data: { mode: 'new' } });
    ref.afterClosed().subscribe((dto) => {
      if (!dto) return;
      this._http.post<any>(this.base, dto).subscribe({ next: (res) => { this.load(); this._flash((res?.data ?? res)?.id); } });
    });
  }

  onEdit(item: any): void {
    const ref = this._dialog.open(ParametroFormDialogComponent, { ...DIALOG_BASE, width: '520px', panelClass: DIALOG_PANEL, data: { mode: 'edit', item } });
    ref.afterClosed().subscribe((dto) => {
      if (!dto) return;
      this._http.put<any>(`${this.base}/${item.id}`, dto).subscribe({ next: () => { this.load(); this._flash(item.id); } });
    });
  }

  onDelete(item: any): void {
    this._confirm.confirmDelete({ title: 'Eliminar parámetro', message: `¿Desactivar "${item.clave}"? (borrado lógico)` })
      .then(() => this._http.delete<any>(`${this.base}/${item.id}`).subscribe({ next: () => this.load() }))
      .catch(() => {});
  }

  private _reveal(items: any[]): void {
    this._clear();
    this.visibleItems.set([]);
    items.forEach((it, i) => this._timers.push(setTimeout(() => this.visibleItems.update(p => [...p, it]), i * ROW_STAGGER_MS)));
  }
  private _flash(id?: string): void { if (!id) return; this.highlightedId.set(id); setTimeout(() => this.highlightedId.set(null), 1400); }
  private _clear(): void { this._timers.forEach(t => clearTimeout(t)); this._timers = []; }
}
