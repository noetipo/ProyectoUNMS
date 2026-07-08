import { Component, EventEmitter, inject, OnInit, Output } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RoleFilter } from '../models/role';

@Component({
  selector: 'app-role-filter',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <div class="flex items-center justify-between px-4 py-4 bg-white rounded-t-lg border-b border-gray-200">
      <div class="flex items-center gap-3">
        <h2 class="text-xl font-semibold text-gray-800">Roles</h2>
        <form [formGroup]="filterForm" class="flex items-center">
          <mat-form-field appearance="outline" class="w-64" subscriptSizing="dynamic">
            <mat-icon matPrefix class="text-gray-400">search</mat-icon>
            <mat-label>Buscar por nombre</mat-label>
            <input matInput formControlName="name" />
          </mat-form-field>
        </form>
      </div>
      <button mat-flat-button color="primary" class="rounded-full" (click)="eventNew.emit(true)">
        <mat-icon>add</mat-icon>
        Nuevo Rol
      </button>
    </div>
  `,
})
export class RoleFilterComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);

  @Output() eventFilter = new EventEmitter<RoleFilter>();
  @Output() eventNew = new EventEmitter<boolean>();

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ name: [''] });
    this.filterForm.valueChanges.subscribe((value) => {
      this.eventFilter.emit(value);
    });
  }
}
