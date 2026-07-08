import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ParentModuleService } from '@/app/providers/services/setup/parent-module.service';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { PaginatedResponse, ParentModule } from '../models/parent-module';
import { ParentModuleListComponent } from './parent-module-list.component';
import { ParentModuleNewComponent } from './parent-module-new.component';
import { ParentModuleEditComponent } from './parent-module-edit.component';

const DIALOG_BASE  = { enterAnimationDuration: '0ms', exitAnimationDuration: '220ms' };
const DIALOG_PANEL = ['dialog-rounded', 'dialog-anim'];
const ROW_STAGGER_MS = 55;

@Component({
  selector: 'app-parent-module-container',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    ParentModuleListComponent,
    PaginationControlsComponent,
  ],
  template: `
    <div class="page">

      <!-- ── Header ── -->
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Configuración</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Módulos Padres</span>
          </div>
          <h1 class="page-title">Módulos padres del sistema</h1>
        </div>
        <button type="button" class="btn-dark" (click)="onNew()">
          <mat-icon svgIcon="plus" class="size-3.5" />
          Nuevo módulo padre
        </button>
      </div>

      <!-- ── Toolbar ── -->
      <div class="page-toolbar">
        <form [formGroup]="filterForm">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="title" placeholder="Buscar módulo padre..." />
          </div>
        </form>
        @if (!loading()) {
          <span class="text-[11px] text-slate-400 anim-fade-in">
            {{ paginatedResponse().totalElements ?? 0 }} registro(s)
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
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="80 + (i % 3) * 30"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="60 + (i % 4) * 20"></div>
                <div class="anim-shimmer h-2 w-10 rounded shrink-0"></div>
                <div class="anim-shimmer h-3.5 w-12 rounded-full shrink-0"></div>
                <div class="anim-shimmer h-3.5 w-10 rounded shrink-0"></div>
              </div>
            }
          </div>
        } @else {
          <app-parent-module-list
            [parentModules]="visibleParentModules()"
            [highlightedId]="highlightedId()"
            (eventEdit)="onEdit($event)"
            (eventDelete)="onDelete($event)"
          />
        }
      </div>

      <!-- ── Pagination ── -->
      @if (!loading()) {
        <div class="page-footer">
          <pagination-controls
            [totalItems]="paginatedResponse().totalElements ?? 0"
            [itemsPerPage]="pageSize()"
            [currentPage]="currentPage()"
            (paginationChange)="onPageChange($event)"
          />
        </div>
      }

    </div>
  `,
})
export class ParentModuleContainerComponent implements OnInit, OnDestroy {
  private _service       = inject(ParentModuleService);
  private _dialog        = inject(MatDialog);
  private _confirmDialog = inject(ConfirmDialogService);
  private _fb            = inject(UntypedFormBuilder);

  protected paginatedResponse    = signal<PaginatedResponse>({ content: [] });
  protected visibleParentModules = signal<ParentModule[]>([]);
  protected currentPage          = signal(0);
  protected pageSize             = signal(20);
  protected loading              = signal(false);
  protected highlightedId        = signal<string | null>(null);

  protected readonly skeletonRows = Array(8).fill(null);

  private _revealTimers: ReturnType<typeof setTimeout>[] = [];

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ title: [''] });
    this.filterForm.valueChanges.subscribe(() => {
      this.currentPage.set(0);
      this.load();
    });
    this.load();
  }

  ngOnDestroy(): void {
    this._clearTimers();
  }

  load(): void {
    this.loading.set(true);
    const params = { page: this.currentPage(), size: this.pageSize(), ...this.filterForm.value };
    this._service.getWithQuery$(params).subscribe({
      next: (res) => {
        this.paginatedResponse.set(res);
        this.loading.set(false);
        this._revealRows(res.content ?? []);
      },
      error: () => this.loading.set(false),
    });
  }

  onPageChange(event: PaginationEvent): void {
    this.currentPage.set(event.page);
    this.pageSize.set(event.size);
    this.load();
  }

  onNew(): void {
    const ref = this._dialog.open(ParentModuleNewComponent, {
      ...DIALOG_BASE,
      width: '480px',
      panelClass: DIALOG_PANEL,
    });
    ref.afterClosed().subscribe((r) => {
      if (r) {
        this._service.add$(r).subscribe((res: any) => {
          this.load();
          if (res?.id) this._flashRow(res.id);
        });
      }
    });
  }

  onEdit(id: string): void {
    this._service.getById$(id).subscribe((pm) => {
      const ref = this._dialog.open(ParentModuleEditComponent, {
        ...DIALOG_BASE,
        width: '480px',
        panelClass: DIALOG_PANEL,
        data: pm,
      });
      ref.afterClosed().subscribe((r) => {
        if (r) {
          this._service.update$(id, r).subscribe(() => {
            this.load();
            this._flashRow(id);
          });
        }
      });
    });
  }

  onDelete(id: string): void {
    this._confirmDialog.confirmDelete()
      .then(() => this._service.delete$(id).subscribe(() => this.load()))
      .catch(() => {});
  }

  private _revealRows(items: ParentModule[]): void {
    this._clearTimers();
    this.visibleParentModules.set([]);
    items.forEach((item, i) => {
      const t = setTimeout(() => {
        this.visibleParentModules.update(prev => [...prev, item]);
      }, i * ROW_STAGGER_MS);
      this._revealTimers.push(t);
    });
  }

  private _flashRow(id: string): void {
    this.highlightedId.set(id);
    setTimeout(() => this.highlightedId.set(null), 1400);
  }

  private _clearTimers(): void {
    this._revealTimers.forEach(t => clearTimeout(t));
    this._revealTimers = [];
  }
}
