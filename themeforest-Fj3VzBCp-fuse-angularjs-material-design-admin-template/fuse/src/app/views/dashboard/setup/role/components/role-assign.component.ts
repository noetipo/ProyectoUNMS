import { Component, inject, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin, of } from 'rxjs';
import { ModuleService } from '@/app/providers/services/setup/module.service';
import { RoleService } from '@/app/providers/services/setup/role.service';
import { Module } from '../../module/models/module';
import { ParentModule } from '../../parentModule/models/parent-module';

export interface RoleAssignData {
  roleId: string;
  parentModules: ParentModule[];
}

interface ParentGroup extends ParentModule {
  modules: Module[];
  loading: boolean;
}

@Component({
  selector: 'app-role-assign',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
  ],
  template: `
    <div class="form-dialog w-[520px]">

      <!-- Header -->
      <div class="form-dialog__header">
        <div>
          <h2 class="form-dialog__title">Asignar módulos al rol</h2>
          <p class="form-dialog__subtitle">Marca los módulos que tendrá acceso este rol</p>
        </div>
        <button mat-icon-button class="!w-8 !h-8 !text-slate-400 hover:!text-slate-600"
          [disabled]="saving()" (click)="dialogRef.close(false)">
          <mat-icon svgIcon="x" class="size-4" />
        </button>
      </div>

      <!-- Body -->
      <div class="form-dialog__body">
        @for (group of groups(); track group.id) {
          <div>
            <div class="flex items-center gap-2 mb-2 pb-1.5 border-b border-slate-100">
              <mat-icon svgIcon="layout-grid" class="size-3.5 text-slate-400" />
              <span class="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                {{ group.title }}
              </span>
            </div>

            @if (group.loading) {
              <div class="pl-2 pb-3 space-y-2">
                @for (_ of [1,2,3]; track $index) {
                  <div class="anim-shimmer h-3 rounded" [style.width.px]="80 + $index * 20"></div>
                }
              </div>
            } @else if (group.modules.length === 0) {
              <p class="pl-2 pb-3 text-xs text-slate-400">Sin módulos disponibles</p>
            } @else {
              <div class="pl-2 pb-3 grid grid-cols-2 gap-x-4 gap-y-0.5">
                @for (mod of group.modules; track mod.id) {
                  <mat-checkbox color="primary"
                    [checked]="isSelected(mod.id!)"
                    (change)="toggle(mod.id!, $event.checked)"
                    class="!text-xs !text-slate-600">
                    {{ mod.title }}
                  </mat-checkbox>
                }
              </div>
            }
          </div>
        }

        @if (groups().length === 0) {
          <div class="flex flex-col items-center gap-2 py-10">
            <mat-icon svgIcon="layout-grid" class="size-10 text-slate-200" />
            <p class="text-sm text-slate-400">No hay módulos padres configurados</p>
          </div>
        }
      </div>

      <!-- Footer -->
      <div class="form-dialog__footer justify-between">
        <span class="text-xs text-slate-400">{{ selectedIds.size }} módulo(s) seleccionado(s)</span>
        <div class="flex items-center gap-2">
          <button mat-button class="!text-slate-500 !text-sm"
            [disabled]="saving()" (click)="dialogRef.close(false)">
            Cancelar
          </button>
          <button mat-flat-button color="primary"
            class="!rounded-lg !px-5 !h-9 !text-sm !font-medium"
            [disabled]="saving()"
            (click)="save()">
            {{ saving() ? 'Guardando...' : 'Guardar asignación' }}
          </button>
        </div>
      </div>

    </div>
  `,
})
export class RoleAssignComponent implements OnInit {
  private _moduleService = inject(ModuleService);
  private _roleService   = inject(RoleService);
  dialogRef = inject(MatDialogRef<RoleAssignComponent>);

  protected groups  = signal<ParentGroup[]>([]);
  protected saving  = signal(false);

  selectedIds = new Set<string>();

  constructor(@Inject(MAT_DIALOG_DATA) public data: RoleAssignData) {}

  ngOnInit(): void {
    const parents = this.data.parentModules ?? [];

    this.groups.set(parents.map(pm => ({ ...pm, modules: [], loading: true })));

    // Carga módulos con estado `selected` para el rol actual
    parents.forEach((pm) => {
      this._moduleService
        .getAllModulesSelectedByRoleIdAndParentModuleId$(this.data.roleId, pm.id!)
        .subscribe({
          next: (modules: Module[]) => {
            modules.filter(m => m.selected).forEach(m => this.selectedIds.add(m.id!));
            this._updateGroup(pm.id!, modules, false);
          },
          error: () => this._updateGroup(pm.id!, [], false),
        });
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

    // Construir un POST por cada grupo (parentModule)
    const calls = this.groups().map(group => {
      const body = {
        roleId:         this.data.roleId,
        parentModuleId: group.id,
        modules: group.modules.map(m => ({
          id:       m.id,
          selected: this.selectedIds.has(m.id!),
        })),
      };
      return this._roleService.postAssigmentModulesToRole$(body);
    });

    // Si no hay grupos cargados, cerrar directamente
    forkJoin(calls.length ? calls : [of(null)]).subscribe({
      next:  () => this.dialogRef.close(true),
      error: () => this.saving.set(false),
    });
  }

  private _updateGroup(parentId: string, modules: Module[], loading: boolean): void {
    this.groups.update(list =>
      list.map(g => g.id === parentId ? { ...g, modules, loading } : g)
    );
  }
}
