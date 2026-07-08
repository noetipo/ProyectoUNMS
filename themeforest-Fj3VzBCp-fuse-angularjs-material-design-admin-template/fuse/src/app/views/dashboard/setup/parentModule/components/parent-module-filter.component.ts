import { Component, EventEmitter, inject, OnInit, Output } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ParentModuleFilter } from '../models/parent-module';

@Component({
  selector: 'app-parent-module-filter',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  styles: [`
    :host ::ng-deep .mat-mdc-form-field-subscript-wrapper { display: none; }
  `],
  template: `
    <div class="flex items-center justify-between px-4 py-4 bg-white rounded-t-lg border-b border-gray-200">
      <div class="flex items-center gap-3">
        <h2 class="text-xl font-semibold text-gray-800">Módulos Padres</h2>
        <form [formGroup]="filterForm">
          <mat-form-field appearance="outline" class="w-64" subscriptSizing="dynamic">
            <mat-icon matPrefix class="text-gray-400">search</mat-icon>
            <mat-label>Buscar por nombre de módulo</mat-label>
            <input matInput formControlName="title" />
          </mat-form-field>
        </form>
      </div>
      <button mat-flat-button color="primary" class="rounded-full" (click)="eventNew.emit(true)">
        <mat-icon>add</mat-icon>
        Nuevo
      </button>
    </div>
  `,
})
export class ParentModuleFilterComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);

  @Output() eventFilter = new EventEmitter<ParentModuleFilter>();
  @Output() eventNew = new EventEmitter<boolean>();

  filterForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.filterForm = this._fb.group({ title: [''] });
    this.filterForm.valueChanges.subscribe((value) => {
      this.eventFilter.emit({ name: value.title });
    });
  }
}
