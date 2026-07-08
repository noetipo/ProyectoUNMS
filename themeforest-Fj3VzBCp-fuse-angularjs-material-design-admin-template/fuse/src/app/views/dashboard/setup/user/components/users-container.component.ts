import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime } from 'rxjs/operators';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { UsersService } from '@/app/providers/services/setup/users.service';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { PaginationControlsComponent, PaginationEvent } from '@/app/shared/pagination-controls/pagination-controls.component';
import { User } from '../models/user';
import { UserListComponent } from './user-list.component';
import { UserNewComponent } from './user-new.component';
import { UserRolesAssignComponent } from './user-roles-assign.component';

const DIALOG_BASE  = { enterAnimationDuration: '0ms', exitAnimationDuration: '220ms' };
const DIALOG_PANEL = ['dialog-rounded', 'dialog-anim'];
const ROW_STAGGER_MS = 55;

interface PaginatedUsers {
  content: User[];
  totalElements?: number;
}

@Component({
  selector: 'app-users-container',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    UserListComponent,
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
            <span class="breadcrumb__current">Usuarios</span>
          </div>
          <h1 class="page-title">Usuarios del sistema</h1>
        </div>
        <button type="button" class="btn-dark" (click)="onNew()">
          <mat-icon svgIcon="user-plus" class="size-3.5" />
          Nuevo usuario
        </button>
      </div>

      <!-- ── Toolbar ── -->
      <div class="page-toolbar">
        <form [formGroup]="filterForm" class="flex flex-wrap items-center gap-3 w-full">
          <div class="toolbar-search">
            <mat-icon svgIcon="search" class="toolbar-search__icon" />
            <input formControlName="search" placeholder="Buscar por nombre, DNI, usuario o email..." />
          </div>
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="w-56">
            <mat-select formControlName="roles" multiple placeholder="Rol">
              @for (r of roleOptions(); track r.code) { <mat-option [value]="r.code">{{ r.name }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="w-48">
            <mat-select formControlName="estados" multiple placeholder="Estado">
              @for (e of estadoOptions; track e.value) { <mat-option [value]="e.value">{{ e.label }}</mat-option> }
            </mat-select>
          </mat-form-field>
          @if (filtrosActivos()) {
            <button type="button" mat-button (click)="limpiar()" class="!text-xs">Limpiar</button>
          }
        </form>
        @if (!loading()) {
          <span class="text-[11px] text-slate-400 anim-fade-in shrink-0">
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
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="70 + (i % 3) * 25"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="120 + (i % 4) * 20"></div>
                <div class="anim-shimmer h-2 rounded shrink-0" [style.width.px]="140 + (i % 3) * 15"></div>
                <div class="anim-shimmer h-3.5 w-16 rounded-full shrink-0"></div>
                <div class="anim-shimmer h-4 w-8 rounded-full shrink-0"></div>
                <div class="anim-shimmer h-3.5 w-10 rounded shrink-0"></div>
              </div>
            }
          </div>
        } @else {
          <app-user-list
            [users]="visibleUsers()"
            [highlightedId]="highlightedId()"
            (eventAssign)="onAssign($event)"
            (eventChangeState)="onChangeState($event)"
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
export class UsersContainerComponent implements OnInit, OnDestroy {
  private _usersService  = inject(UsersService);
  private _dialog        = inject(MatDialog);
  private _confirmDialog = inject(ConfirmDialogService);
  private _fb            = inject(UntypedFormBuilder);
  private _http          = inject(HttpClient);

  protected paginatedResponse = signal<PaginatedUsers>({ content: [], totalElements: 0 });
  protected visibleUsers      = signal<User[]>([]);
  protected currentPage       = signal(0);
  protected pageSize          = signal(20);
  protected loading           = signal(false);
  protected highlightedId     = signal<string | null>(null);
  protected roleOptions       = signal<{ code: string; name: string }[]>([]);

  protected readonly estadoOptions = [
    { value: 'ACTIVE', label: 'Activo' },
    { value: 'INACTIVE', label: 'Inactivo' },
    { value: 'SUSPENDED', label: 'Suspendido' },
    { value: 'PENDING_VERIFICATION', label: 'Pend. verificación' },
  ];

  protected readonly skeletonRows = Array(8).fill(null);
  private _revealTimers: ReturnType<typeof setTimeout>[] = [];

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ search: [''], roles: [[]], estados: [[]] });
    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => {
      this.currentPage.set(0);
      this.load();
    });
    this._cargarRoles();
    this.load();
  }

  ngOnDestroy(): void {
    this._clearTimers();
  }

  private _cargarRoles(): void {
    this._http.get<any>(`${environment.url}${END_POINTS.setup.role}?size=100`).subscribe({
      next: (res: any) => {
        const list = res?.content ?? res?.data?.content ?? res?.data ?? [];
        this.roleOptions.set(list.map((r: any) => ({ code: r.code, name: r.name ?? r.code })));
      },
      error: () => this.roleOptions.set([]),
    });
  }

  protected filtrosActivos(): boolean {
    const v = this.filterForm?.value ?? {};
    return !!(v.search || (v.roles?.length) || (v.estados?.length));
  }

  limpiar(): void {
    this.filterForm.setValue({ search: '', roles: [], estados: [] });
  }

  load(): void {
    this.loading.set(true);
    const v = this.filterForm.value;
    const params: any = { page: this.currentPage(), size: this.pageSize() };
    if (v.search) params.search = v.search;
    if (v.roles?.length) params.roles = v.roles;
    if (v.estados?.length) params.estados = v.estados;
    this._usersService.getWithQuery$(params).subscribe({
      next: (res: any) => {
        // Envoltorio ApiResponse → data = PageResponse { content, total, page, size }
        const d = res?.data ?? res;
        const content: User[] = d?.content ?? (Array.isArray(d) ? d : []);
        const totalElements: number = d?.total ?? d?.totalElements ?? content.length;
        this.paginatedResponse.set({ content, totalElements });
        this.loading.set(false);
        this._revealRows(content);
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
    const ref = this._dialog.open(UserNewComponent, {
      ...DIALOG_BASE,
      width: '500px',
      panelClass: DIALOG_PANEL,
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) this.load();
    });
  }

  onAssign(userId: string): void {
    this._dialog.open(UserRolesAssignComponent, {
      ...DIALOG_BASE,
      width: '480px',
      panelClass: DIALOG_PANEL,
      data: userId,
    });
  }

  onChangeState(id: string): void {
    this._usersService.updateStateUserId$(id).subscribe({
      next: (res: any) => {
        const updated: User = res?.data ?? res;
        this.visibleUsers.update(list =>
          list.map(u => u.id === id ? { ...u, ...updated } : u)
        );
      },
    });
  }

  private _revealRows(items: User[]): void {
    this._clearTimers();
    this.visibleUsers.set([]);
    items.forEach((item, i) => {
      const t = setTimeout(() => {
        this.visibleUsers.update(prev => [...prev, item]);
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
