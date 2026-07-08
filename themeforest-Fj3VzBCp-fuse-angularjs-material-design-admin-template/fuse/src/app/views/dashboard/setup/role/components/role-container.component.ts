import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RoleService } from '@/app/providers/services/setup/role.service';
import { ParentModuleService } from '@/app/providers/services/setup/parent-module.service';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { PaginatedResponseRole, Role } from '../models/role';
import { ParentModule } from '../../parentModule/models/parent-module';
import { RoleListComponent } from './role-list.component';
import { RoleNewComponent } from './role-new.component';
import { RoleEditComponent } from './role-edit.component';
import { RoleAssignComponent } from './role-assign.component';

const DIALOG_BASE  = { enterAnimationDuration: '0ms', exitAnimationDuration: '220ms' };
const DIALOG_PANEL = ['dialog-rounded', 'dialog-anim'];
const ROW_STAGGER_MS = 55;

@Component({
  selector: 'app-role-container',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    RoleListComponent,
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
            <span class="breadcrumb__current">Roles</span>
          </div>
          <h1 class="page-title">Roles del sistema</h1>
        </div>
        <button type="button" class="btn-dark" (click)="onNew()">
          <mat-icon svgIcon="plus" class="size-3.5" />
          Nuevo rol
        </button>
      </div>

      <!-- ── Toolbar ── -->
      <div class="page-toolbar">
        <form [formGroup]="filterForm">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="name" placeholder="Buscar rol..." />
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
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="120 + (i % 4) * 20"></div>
                <div class="anim-shimmer h-3.5 w-12 rounded-full shrink-0"></div>
                <div class="anim-shimmer h-3.5 w-10 rounded shrink-0"></div>
              </div>
            }
          </div>
        } @else {
          <app-role-list
            [roles]="visibleRoles()"
            [highlightedId]="highlightedId()"
            (eventEdit)="onEdit($event)"
            (eventDelete)="onDelete($event)"
            (eventAsignet)="onAssign($event)"
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
export class RoleContainerComponent implements OnInit, OnDestroy {
  private _roleService         = inject(RoleService);
  private _parentModuleService = inject(ParentModuleService);
  private _dialog              = inject(MatDialog);
  private _confirmDialog       = inject(ConfirmDialogService);
  private _fb                  = inject(UntypedFormBuilder);

  protected paginatedResponse = signal<PaginatedResponseRole>({ content: [] });
  protected visibleRoles      = signal<Role[]>([]);
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
      this.loadRoles();
    });
    this.loadParentModules();
    this.loadRoles();
  }

  ngOnDestroy(): void {
    this._clearTimers();
  }

  loadRoles(): void {
    this.loading.set(true);
    const params = { page: this.currentPage(), size: this.pageSize(), ...this.filterForm.value };
    this._roleService.getWithQuery$(params).subscribe({
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
    this.loadRoles();
  }

  onNew(): void {
    const ref = this._dialog.open(RoleNewComponent, {
      ...DIALOG_BASE,
      width: '480px',
      panelClass: DIALOG_PANEL,
    });
    ref.afterClosed().subscribe((r) => {
      if (r) {
        this._roleService.add$(r).subscribe((res: any) => {
          this.loadRoles();
          if (res?.id) this._flashRow(res.id);
        });
      }
    });
  }

  onEdit(id: string): void {
    this._roleService.getById$(id).subscribe((role) => {
      const ref = this._dialog.open(RoleEditComponent, {
        ...DIALOG_BASE,
        width: '480px',
        panelClass: DIALOG_PANEL,
        data: role,
      });
      ref.afterClosed().subscribe((r) => {
        if (r) {
          this._roleService.update$(id, r).subscribe(() => {
            this.loadRoles();
            this._flashRow(id);
          });
        }
      });
    });
  }

  onDelete(id: string): void {
    this._confirmDialog.confirmDelete()
      .then(() => this._roleService.delete$(id).subscribe(() => this.loadRoles()))
      .catch(() => {});
  }

  onAssign(roleId: string): void {
    const ref = this._dialog.open(RoleAssignComponent, {
      ...DIALOG_BASE,
      width: '520px',
      panelClass: DIALOG_PANEL,
      data: { roleId, parentModules: this.parentModules() },
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) this.loadRoles();
    });
  }

  private _revealRows(items: Role[]): void {
    this._clearTimers();
    this.visibleRoles.set([]);
    items.forEach((item, i) => {
      const t = setTimeout(() => {
        this.visibleRoles.update(prev => [...prev, item]);
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
