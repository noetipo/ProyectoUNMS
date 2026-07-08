import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute } from '@angular/router';
import { environment } from '@/environments/environment';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { CatalogoFormDialogComponent } from './catalogo-form.dialog';

const ROW_STAGGER_MS = 55;
const DIALOG_BASE  = { enterAnimationDuration: '0ms', exitAnimationDuration: '220ms' };
const DIALOG_PANEL = ['dialog-rounded', 'dialog-anim'];

/**
 * Mantenimiento genérico de un catálogo simple (cargos / centros laborales).
 * Se configura por la `data` de la ruta: { titulo, singular, base, icon?, ejemplo? }.
 * Estilo y modales idénticos al módulo de Roles.
 */
@Component({
  selector: 'app-catalogo-crud',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule,
    PaginationControlsComponent,
  ],
  template: `
    <div class="page">

      <!-- ── Header ── -->
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Mantenimientos</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">{{ titulo }}</span>
          </div>
          <h1 class="page-title">{{ titulo }}</h1>
        </div>
        <button type="button" class="btn-dark" (click)="onNew()">
          <mat-icon svgIcon="plus" class="size-3.5" />
          Nuevo {{ singular }}
        </button>
      </div>

      <!-- ── Toolbar ── -->
      <div class="page-toolbar">
        <form [formGroup]="filterForm">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="search" [placeholder]="'Buscar ' + singular + '...'" />
          </div>
        </form>
        @if (!loading()) {
          <span class="text-[11px] text-slate-400 anim-fade-in">
            {{ total() }} registro(s)
          </span>
        }
      </div>

      <!-- ── Content ── -->
      <div class="page-content">
        @if (loading()) {
          <div class="divide-y divide-slate-50">
            @for (_ of skeletonRows; track $index; let i = $index) {
              <div class="skeleton-row" [style.opacity]="1 - i * 0.1">
                <div class="anim-shimmer h-2 w-4 rounded shrink-0"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="80 + (i % 3) * 20"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="180 + (i % 4) * 30"></div>
                <div class="anim-shimmer h-2 rounded flex-1"></div>
                <div class="anim-shimmer h-3.5 w-12 rounded-full shrink-0"></div>
                <div class="anim-shimmer h-3.5 w-10 rounded shrink-0"></div>
              </div>
            }
          </div>
        } @else {

          <!-- Desktop table -->
          <div class="hidden md:block overflow-x-auto">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="w-8">#</th>
                  <th class="w-28">Código</th>
                  <th>Nombre</th>
                  <th>Descripción</th>
                  <th class="w-20">Estado</th>
                  <th class="w-20 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (item of visibleItems(); track item.id; let i = $index) {
                  <tr [class.anim-flash]="item.id === highlightedId()">
                    <td class="text-slate-400 tabular-nums">{{ (page() * size()) + i + 1 }}</td>
                    <td class="font-mono text-[11px] text-slate-400">{{ item.codigoSistema ?? '—' }}</td>
                    <td class="font-medium text-slate-700">{{ item.nombre }}</td>
                    <td class="text-slate-500 text-sm max-w-[260px] truncate">{{ item.descripcion ?? '—' }}</td>
                    <td>
                      <span class="status-badge"
                        [class.status-badge--active]="item.activo"
                        [class.status-badge--inactive]="!item.activo">
                        <span class="status-dot"
                          [class.status-dot--active]="item.activo"
                          [class.status-dot--inactive]="!item.activo"></span>
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
                  <tr>
                    <td colspan="6" class="text-center">
                      <div class="table-empty">
                        <mat-icon [svgIcon]="icon" class="size-10 text-slate-200" />
                        <p class="table-empty__text">Sin registros</p>
                        <p class="table-empty__subtext">Crea el primero con el botón "Nuevo {{ singular }}"</p>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Mobile cards -->
          <div class="md:hidden divide-y divide-slate-100">
            @for (item of visibleItems(); track item.id) {
              <div class="px-5 py-3 anim-row-in" [class.anim-flash]="item.id === highlightedId()">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex-1 min-w-0">
                    <p class="font-mono text-[11px] text-slate-400">{{ item.codigoSistema ?? '—' }}</p>
                    <p class="font-medium text-slate-800 text-sm truncate">{{ item.nombre }}</p>
                    @if (item.descripcion) {
                      <p class="text-xs text-slate-500 truncate mt-0.5">{{ item.descripcion }}</p>
                    }
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
            @empty {
              <div class="flex flex-col items-center gap-2 py-16 anim-fade-in">
                <mat-icon [svgIcon]="icon" class="size-10 text-slate-200" />
                <p class="text-sm text-slate-400">Sin registros</p>
              </div>
            }
          </div>

        }
      </div>

      <!-- ── Pagination ── -->
      @if (!loading()) {
        <div class="page-footer">
          <pagination-controls
            [totalItems]="total()"
            [itemsPerPage]="size()"
            [currentPage]="page()"
            (paginationChange)="onPage($event)"
          />
        </div>
      }

    </div>
  `,
})
export class CatalogoCrudComponent implements OnInit, OnDestroy {
  private _route   = inject(ActivatedRoute);
  private _http    = inject(HttpClient);
  private _fb      = inject(UntypedFormBuilder);
  private _dialog  = inject(MatDialog);
  private _confirm = inject(ConfirmDialogService);

  protected titulo = 'Catálogo';
  protected singular = 'registro';
  protected icon = 'folder';
  private ejemplo = '';
  private base = '';

  protected rows         = signal<any[]>([]);
  protected visibleItems = signal<any[]>([]);
  protected total        = signal(0);
  protected page         = signal(0);
  protected size         = signal(20);
  protected loading      = signal(false);
  protected highlightedId = signal<string | null>(null);

  protected readonly skeletonRows = Array(8).fill(null);
  private _revealTimers: ReturnType<typeof setTimeout>[] = [];

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    const data = this._route.snapshot.data as { titulo?: string; singular?: string; base?: string; icon?: string; ejemplo?: string };
    this.titulo = data.titulo ?? this.titulo;
    this.singular = data.singular ?? this.singular;
    this.icon = data.icon ?? this.icon;
    this.ejemplo = data.ejemplo ?? '';
    this.base = environment.url + (data.base ?? '');

    this.filterForm = this._fb.group({ search: [''] });
    this.filterForm.valueChanges.subscribe(() => { this.page.set(0); this.load(); });
    this.load();
  }

  ngOnDestroy(): void {
    this._clearTimers();
  }

  load(): void {
    this.loading.set(true);
    let params = new HttpParams().set('page', String(this.page())).set('size', String(this.size()));
    const search = this.filterForm.value.search;
    if (search) params = params.set('search', search);
    this._http.get<any>(this.base, { params }).subscribe({
      next: (res) => {
        const d = res?.data ?? res;
        this.rows.set(d?.content ?? []);
        this.total.set(d?.total ?? d?.totalElements ?? 0);
        this.loading.set(false);
        this._revealRows(d?.content ?? []);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PaginationEvent): void {
    this.page.set(e.page);
    this.size.set(e.size);
    this.load();
  }

  onNew(): void {
    const ref = this._dialog.open(CatalogoFormDialogComponent, {
      ...DIALOG_BASE, width: '480px', panelClass: DIALOG_PANEL,
      data: { mode: 'new', singular: this.singular, ejemplo: this.ejemplo },
    });
    ref.afterClosed().subscribe((dto) => {
      if (!dto) return;
      this._http.post<any>(this.base, { nombre: dto.nombre, descripcion: dto.descripcion }).subscribe({
        next: (res) => { this.load(); this._flashRow((res?.data ?? res)?.id); },
        error: () => {},
      });
    });
  }

  onEdit(item: any): void {
    const ref = this._dialog.open(CatalogoFormDialogComponent, {
      ...DIALOG_BASE, width: '480px', panelClass: DIALOG_PANEL,
      data: { mode: 'edit', singular: this.singular, ejemplo: this.ejemplo, item },
    });
    ref.afterClosed().subscribe((dto) => {
      if (!dto) return;
      this._http.put<any>(`${this.base}/${item.id}`, { nombre: dto.nombre, descripcion: dto.descripcion }).subscribe({
        next: () => { this.load(); this._flashRow(item.id); },
        error: () => {},
      });
    });
  }

  onDelete(item: any): void {
    this._confirm.confirmDelete({
      title: `Eliminar ${this.singular}`,
      message: `¿Desactivar "${item.nombre}"? (borrado lógico)`,
    })
      .then(() => this._http.delete<any>(`${this.base}/${item.id}`).subscribe({ next: () => this.load() }))
      .catch(() => {});
  }

  private _revealRows(items: any[]): void {
    this._clearTimers();
    this.visibleItems.set([]);
    items.forEach((item, i) => {
      const t = setTimeout(() => this.visibleItems.update(prev => [...prev, item]), i * ROW_STAGGER_MS);
      this._revealTimers.push(t);
    });
  }

  private _flashRow(id: string | undefined): void {
    if (!id) return;
    this.highlightedId.set(id);
    setTimeout(() => this.highlightedId.set(null), 1400);
  }

  private _clearTimers(): void {
    this._revealTimers.forEach(t => clearTimeout(t));
    this._revealTimers = [];
  }
}
