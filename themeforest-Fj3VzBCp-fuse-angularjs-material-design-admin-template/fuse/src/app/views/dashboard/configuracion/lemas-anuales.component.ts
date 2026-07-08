import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';

const ROW_STAGGER_MS = 55;
const DIALOG_BASE = { enterAnimationDuration: '0ms', exitAnimationDuration: '220ms' };
const DIALOG_PANEL = ['dialog-rounded', 'dialog-anim'];

interface LemaDialogData { mode: 'new' | 'edit'; item?: any; }

@Component({
  selector: 'app-lema-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <div class="p-5 w-[500px] max-w-full">
      <h2 class="text-base font-semibold text-slate-800 mb-3">
        {{ data.mode === 'new' ? 'Nuevo lema anual' : 'Editar lema anual' }}
      </h2>
      <form [formGroup]="form" (ngSubmit)="save()" class="flex flex-col gap-1">
        <div>
          <label class="form-label" for="l-anio">Año <span class="form-required">*</span></label>
          <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
            <input matInput id="l-anio" type="number" formControlName="anio" min="1900" max="2100" placeholder="2026" />
          </mat-form-field>
        </div>
        <div>
          <label class="form-label" for="l-texto">Texto (nombre del año) <span class="form-required">*</span></label>
          <mat-form-field appearance="outline" class="w-full" subscriptSizing="dynamic">
            <textarea matInput id="l-texto" formControlName="texto" rows="2" maxlength="300"
                      placeholder="Año de la Esperanza y el Fortalecimiento de la Democracia"></textarea>
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
export class LemaFormDialogComponent {
  private _fb = inject(FormBuilder);
  protected ref = inject(MatDialogRef<LemaFormDialogComponent>);
  protected data = inject<LemaDialogData>(MAT_DIALOG_DATA);

  protected form: FormGroup = this._fb.group({
    anio: [this.data.item?.anio ?? new Date().getFullYear(), [Validators.required, Validators.min(1900), Validators.max(2100)]],
    texto: [this.data.item?.texto ?? '', [Validators.required, Validators.maxLength(300)]],
  });

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    this.ref.close({ anio: Number(v.anio), texto: (v.texto ?? '').trim() });
  }
}

@Component({
  selector: 'app-lemas-anuales',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule, MatSlideToggleModule, MatTooltipModule, PaginationControlsComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Configuración</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Lemas anuales</span>
          </div>
          <h1 class="page-title">Lemas anuales</h1>
        </div>
        <button type="button" class="btn-dark" (click)="onNew()">
          <mat-icon svgIcon="plus" class="size-3.5" /> Nuevo lema
        </button>
      </div>

      <div class="page-toolbar">
        <form [formGroup]="filterForm">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="search" placeholder="Buscar por año o texto..." />
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
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="40"></div>
                <div class="anim-shimmer h-2 rounded flex-1"></div>
                <div class="anim-shimmer h-4 w-10 rounded-full shrink-0"></div>
              </div>
            }
          </div>
        } @else {
          <div class="hidden md:block overflow-x-auto">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="w-8">#</th>
                  <th class="w-20">Año</th>
                  <th>Texto (nombre del año)</th>
                  <th class="w-28">Vigente</th>
                  <th class="w-20 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (item of visibleItems(); track item.id; let i = $index) {
                  <tr [class.anim-flash]="item.id === highlightedId()">
                    <td class="text-slate-400 tabular-nums">{{ (page() * size()) + i + 1 }}</td>
                    <td class="font-semibold text-slate-700 tabular-nums">{{ item.anio }}</td>
                    <td class="text-slate-600 text-sm">{{ item.texto }}</td>
                    <td>
                      <mat-slide-toggle [checked]="item.activo" [disabled]="busy()"
                                        (change)="onToggle(item, $event.checked)"
                                        [matTooltip]="item.activo ? 'Vigente para ' + item.anio : 'Marcar como vigente'">
                        <span class="text-xs" [class.text-emerald-600]="item.activo" [class.text-slate-400]="!item.activo">
                          {{ item.activo ? 'Vigente' : 'Inactivo' }}
                        </span>
                      </mat-slide-toggle>
                    </td>
                    <td>
                      <div class="row-actions">
                        <button mat-icon-button class="!w-7 !h-7" (click)="onEdit(item)">
                          <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                }
                @empty {
                  <tr><td colspan="5" class="text-center">
                    <div class="table-empty">
                      <mat-icon svgIcon="heroicons_outline:megaphone" class="size-10 text-slate-200" />
                      <p class="table-empty__text">Sin lemas</p>
                      <p class="table-empty__subtext">Crea el primero con el botón "Nuevo lema"</p>
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
                    <p class="font-semibold text-slate-700">{{ item.anio }}</p>
                    <p class="text-sm text-slate-600">{{ item.texto }}</p>
                    <div class="mt-1">
                      <mat-slide-toggle [checked]="item.activo" [disabled]="busy()" (change)="onToggle(item, $event.checked)">
                        <span class="text-xs" [class.text-emerald-600]="item.activo" [class.text-slate-400]="!item.activo">
                          {{ item.activo ? 'Vigente' : 'Inactivo' }}
                        </span>
                      </mat-slide-toggle>
                    </div>
                  </div>
                  <button mat-icon-button class="!w-7 !h-7 shrink-0" (click)="onEdit(item)">
                    <mat-icon svgIcon="pencil" class="text-amber-500 size-3.5" />
                  </button>
                </div>
              </div>
            }
            @empty { <div class="flex flex-col items-center gap-2 py-16"><p class="text-sm text-slate-400">Sin lemas</p></div> }
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
export class LemasAnualesComponent implements OnInit, OnDestroy {
  private _http = inject(HttpClient);
  private _fb = inject(UntypedFormBuilder);
  private _dialog = inject(MatDialog);
  private readonly base = environment.url + END_POINTS.configuracion.lemasAnuales;

  protected rows = signal<any[]>([]);
  protected visibleItems = signal<any[]>([]);
  protected total = signal(0);
  protected page = signal(0);
  protected size = signal(20);
  protected loading = signal(false);
  protected busy = signal(false);
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

  onToggle(item: any, checked: boolean): void {
    this.busy.set(true);
    const url = `${this.base}/${item.id}/${checked ? 'activar' : 'desactivar'}`;
    this._http.post<any>(url, {}).subscribe({
      next: () => { this.busy.set(false); this.load(); },  // recarga: refleja el que se desactivó por el mismo año
      error: () => { this.busy.set(false); this.load(); },
    });
  }

  onNew(): void {
    const ref = this._dialog.open(LemaFormDialogComponent, { ...DIALOG_BASE, width: '520px', panelClass: DIALOG_PANEL, data: { mode: 'new' } });
    ref.afterClosed().subscribe((dto) => {
      if (!dto) return;
      this._http.post<any>(this.base, dto).subscribe({ next: (res) => { this.load(); this._flash((res?.data ?? res)?.id); } });
    });
  }

  onEdit(item: any): void {
    const ref = this._dialog.open(LemaFormDialogComponent, { ...DIALOG_BASE, width: '520px', panelClass: DIALOG_PANEL, data: { mode: 'edit', item } });
    ref.afterClosed().subscribe((dto) => {
      if (!dto) return;
      this._http.put<any>(`${this.base}/${item.id}`, dto).subscribe({ next: () => { this.load(); this._flash(item.id); } });
    });
  }

  private _reveal(items: any[]): void {
    this._clear();
    this.visibleItems.set([]);
    items.forEach((it, i) => this._timers.push(setTimeout(() => this.visibleItems.update(p => [...p, it]), i * ROW_STAGGER_MS)));
  }
  private _flash(id?: string): void { if (!id) return; this.highlightedId.set(id); setTimeout(() => this.highlightedId.set(null), 1400); }
  private _clear(): void { this._timers.forEach(t => clearTimeout(t)); this._timers = []; }
}
