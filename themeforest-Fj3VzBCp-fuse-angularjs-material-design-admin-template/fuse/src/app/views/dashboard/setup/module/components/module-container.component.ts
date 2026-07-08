import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ModuleService } from '@/app/providers/services/setup/module.service';
import { ParentModuleService } from '@/app/providers/services/setup/parent-module.service';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { PaginatedResponse } from '../models/module';
import { ParentModule } from '../../parentModule/models/parent-module';
import { ModuleListComponent } from './module-list.component';
import { ModuleNewComponent } from './module-new.component';
import { ModuleEditComponent } from './module-edit.component';

const DIALOG_BASE  = { enterAnimationDuration: '0ms', exitAnimationDuration: '220ms' };
const DIALOG_PANEL = ['dialog-rounded', 'dialog-anim'];
const ROW_STAGGER_MS = 55;

@Component({
  selector: 'app-module-container',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    ModuleListComponent,
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
            <span class="breadcrumb__current">Módulos</span>
          </div>
          <h1 class="page-title">Módulos del sistema</h1>
        </div>
        <button type="button" class="btn-dark" (click)="onNew()">
          <mat-icon svgIcon="plus" class="size-3.5" />
          Nuevo módulo
        </button>
      </div>

      <!-- ── Toolbar ── -->
      <div class="page-toolbar">
        <form [formGroup]="filterForm">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="name" placeholder="Buscar módulo..." />
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
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="60 + (i % 3) * 25"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="90 + (i % 4) * 20"></div>
                <div class="anim-shimmer h-2 w-12 rounded shrink-0"></div>
                <div class="anim-shimmer h-2 rounded flex-1"></div>
                <div class="anim-shimmer h-2 w-5 rounded shrink-0"></div>
                <div class="anim-shimmer h-3.5 w-12 rounded-full shrink-0"></div>
                <div class="anim-shimmer h-3.5 w-10 rounded shrink-0"></div>
              </div>
            }
          </div>
        } @else {
          <app-module-list
            [modules]="visibleModules()"
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
export class ModuleContainerComponent implements OnInit, OnDestroy {
  private _moduleService      = inject(ModuleService);
  private _parentModuleService = inject(ParentModuleService);
  private _dialog             = inject(MatDialog);
  private _confirmDialog      = inject(ConfirmDialogService);
  private _fb                 = inject(UntypedFormBuilder);

  protected paginatedResponse = signal<PaginatedResponse>({ content: [] });
  protected visibleModules    = signal<any[]>([]);
  protected parentModules     = signal<ParentModule[]>([]);
  protected currentPage       = signal(0);
  protected pageSize          = signal(20);
  protected loading           = signal(false);
  protected highlightedId     = signal<string | null>(null);

  protected readonly skeletonRows = Array(8).fill(null);

  private _revealTimers: ReturnType<typeof setTimeout>[] = [];

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ name: [''] });
    this.filterForm.valueChanges.subscribe(() => {
      this.currentPage.set(0);
      this.load();
    });
    this.loadParentModules();
    this.load();
  }

  ngOnDestroy(): void {
    this._clearTimers();
  }

  load(): void {
    this.loading.set(true);
    const params = { page: this.currentPage(), size: this.pageSize(), ...this.filterForm.value };
    this._moduleService.getWithQuery$(params).subscribe({
      next: (res) => {
        this.paginatedResponse.set(res);
        this.loading.set(false);
        this._revealRows(res.content ?? []);
      },
      error: () => this.loading.set(false),
    });
  }

  loadParentModules(): void {
    this._parentModuleService.getAllNotPaginate().subscribe({
      next: (res) => this.parentModules.set(res),
    });
  }

  onPageChange(event: PaginationEvent): void {
    this.currentPage.set(event.page);
    this.pageSize.set(event.size);
    this.load();
  }

  onNew(): void {
    const ref = this._dialog.open(ModuleNewComponent, {
      ...DIALOG_BASE,
      width:      '560px',
      panelClass: DIALOG_PANEL,
      data:       this.parentModules(),
    });
    ref.afterClosed().subscribe((r) => {
      if (r) {
        this._moduleService.add$(r).subscribe((res: any) => {
          this.load();
          if (res?.id) this._flashRow(res.id);
        });
      }
    });
  }

  onEdit(id: string): void {
    this._moduleService.getById$(id).subscribe((m) => {
      const ref = this._dialog.open(ModuleEditComponent, {
        ...DIALOG_BASE,
        width:      '560px',
        panelClass: DIALOG_PANEL,
        data:       { module: m, parentModules: this.parentModules() },
      });
      ref.afterClosed().subscribe((r) => {
        if (r) {
          this._moduleService.update$(id, r).subscribe(() => {
            this.load();
            this._flashRow(id);
          });
        }
      });
    });
  }

  onDelete(id: string): void {
    this._confirmDialog.confirmDelete()
      .then(() => this._moduleService.delete$(id).subscribe(() => this.load()))
      .catch(() => {});
  }

  private _revealRows(items: any[]): void {
    this._clearTimers();
    this.visibleModules.set([]);
    items.forEach((item, i) => {
      const t = setTimeout(() => {
        this.visibleModules.update(prev => [...prev, item]);
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
