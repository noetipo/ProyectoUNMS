import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface PaginationEvent {
  page: number;
  size: number;
}

@Component({
  selector: 'pagination-controls',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="flex items-center justify-between px-4 py-2.5 bg-white">
      <!-- Selector de tamaño -->
      <div class="flex items-center gap-2 text-xs text-slate-400">
        <span>Filas por página:</span>
        <select
          (change)="onSizeChange($event)"
          class="h-6 px-1.5 text-xs text-slate-600 bg-white border border-slate-200 rounded cursor-pointer focus:outline-none focus:ring-1 focus:ring-slate-300"
        >
          @for (opt of sizeOptions; track opt) {
            <option [value]="opt" [selected]="opt === itemsPerPage">{{ opt }}</option>
          }
        </select>
      </div>

      <!-- Info + navegación -->
      <div class="flex items-center gap-3">
        <span class="text-xs text-slate-400">
          {{ rangeStart }}–{{ rangeEnd }} de {{ totalItems }}
        </span>
        <div class="flex items-center gap-0.5">
          <button
            mat-icon-button
            class="!w-7 !h-7"
            [disabled]="currentPage === 0"
            (click)="onPrevious()"
          >
            <mat-icon svgIcon="chevron-left" class="size-4" />
          </button>
          <button
            mat-icon-button
            class="!w-7 !h-7"
            [disabled]="currentPage >= totalPages - 1"
            (click)="onNext()"
          >
            <mat-icon svgIcon="chevron-right" class="size-4" />
          </button>
        </div>
      </div>
    </div>
  `,
})
export class PaginationControlsComponent implements OnChanges {
  @Input() totalItems: number = 0;
  @Input() itemsPerPage: number = 20;
  @Input() currentPage: number = 0;
  @Output() paginationChange = new EventEmitter<PaginationEvent>();

  readonly sizeOptions = [10, 20, 30, 50, 100];

  totalPages: number = 0;
  rangeStart: number = 0;
  rangeEnd: number = 0;

  ngOnChanges(): void {
    this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage) || 1;
    this.rangeStart = this.totalItems === 0 ? 0 : this.currentPage * this.itemsPerPage + 1;
    this.rangeEnd   = Math.min((this.currentPage + 1) * this.itemsPerPage, this.totalItems);
  }

  onPrevious(): void {
    if (this.currentPage > 0) {
      this.paginationChange.emit({ page: this.currentPage - 1, size: this.itemsPerPage });
    }
  }

  onNext(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.paginationChange.emit({ page: this.currentPage + 1, size: this.itemsPerPage });
    }
  }

  onSizeChange(event: Event): void {
    const size = Number((event.target as HTMLSelectElement).value);
    this.paginationChange.emit({ page: 0, size });
  }
}
