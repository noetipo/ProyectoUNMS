import { Component, inject, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { UserRoleService } from '@/app/providers/services/setup/user-role.service';

interface RoleOption {
  id: string;
  name: string;
  description?: string;
  selected?: boolean;
}

@Component({
  selector: 'app-user-roles-assign',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
  ],
  template: `
    <div class="form-dialog w-[480px]">

      <div class="form-dialog__header">
        <div>
          <h2 class="form-dialog__title">Asignar roles al usuario</h2>
          <p class="form-dialog__subtitle">Marca los roles que tendrá este usuario</p>
        </div>
        <button mat-icon-button class="!w-8 !h-8 !text-slate-400 hover:!text-slate-600"
          [disabled]="saving()" (click)="dialogRef.close()">
          <mat-icon svgIcon="x" class="size-4" />
        </button>
      </div>

      <div class="form-dialog__body">
        @if (loading()) {
          <div class="space-y-3">
            @for (_ of [1,2,3,4]; track $index) {
              <div class="flex items-center gap-3">
                <div class="anim-shimmer h-4 w-4 rounded shrink-0"></div>
                <div class="flex-1 space-y-1">
                  <div class="anim-shimmer h-3 rounded" [style.width.%]="50 + $index * 8"></div>
                  <div class="anim-shimmer h-2 rounded w-3/4"></div>
                </div>
              </div>
            }
          </div>
        } @else {
          <div class="space-y-1">
            @for (role of roles(); track role.id) {
              <div class="flex items-start gap-2 py-2 border-b border-slate-50 last:border-0">
                <mat-checkbox
                  color="primary"
                  [checked]="isSelected(role.id)"
                  (change)="toggle(role.id, $event.checked)"
                  class="!text-sm !text-slate-700 mt-0.5"
                >
                  <div>
                    <span class="font-medium">{{ role.name }}</span>
                    @if (role.description) {
                      <span class="block text-xs text-slate-400 mt-0.5">{{ role.description }}</span>
                    }
                  </div>
                </mat-checkbox>
              </div>
            }
            @if (roles().length === 0) {
              <div class="flex flex-col items-center gap-2 py-10">
                <mat-icon svgIcon="shield" class="size-10 text-slate-200" />
                <p class="text-sm text-slate-400">No hay roles disponibles</p>
              </div>
            }
          </div>
        }
      </div>

      <div class="form-dialog__footer justify-between">
        <span class="text-xs text-slate-400">{{ selectedIds.size }} rol(es) seleccionado(s)</span>
        <div class="flex items-center gap-2">
          <button mat-button class="!text-slate-500 !text-sm"
            [disabled]="saving()" (click)="dialogRef.close()">
            Cancelar
          </button>
          <button mat-flat-button color="primary"
            class="!rounded-lg !px-5 !h-9 !text-sm !font-medium"
            [disabled]="saving() || loading()"
            (click)="save()">
            {{ saving() ? 'Guardando...' : 'Guardar roles' }}
          </button>
        </div>
      </div>

    </div>
  `,
})
export class UserRolesAssignComponent implements OnInit {
  private _service = inject(UserRoleService);
  dialogRef = inject(MatDialogRef<UserRolesAssignComponent>);

  protected roles   = signal<RoleOption[]>([]);
  protected loading = signal(true);
  protected saving  = signal(false);

  selectedIds = new Set<string>();

  constructor(@Inject(MAT_DIALOG_DATA) public userId: string) {}

  ngOnInit(): void {
    this._service.getAllRolesSelectedByUserId$(this.userId).subscribe({
      next: (res) => {
        const list: RoleOption[] = Array.isArray(res) ? res : (res?.data ?? []);
        list.filter(r => r.selected).forEach(r => this.selectedIds.add(r.id));
        this.roles.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  isSelected(id: string): boolean {
    return this.selectedIds.has(id);
  }

  toggle(id: string, checked: boolean): void {
    checked ? this.selectedIds.add(id) : this.selectedIds.delete(id);
  }

  save(): void {
    this.saving.set(true);
    const roleIds = Array.from(this.selectedIds);
    this._service.assignRolesToUser$(this.userId, roleIds).subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogRef.close(true);
      },
      error: () => this.saving.set(false),
    });
  }
}
