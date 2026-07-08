import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface FilterField {
  key: string;
  label: string;
  type: 'text' | 'select';
  options?: { value: string; label: string }[];
}

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="flex flex-wrap items-end gap-3">
      @for (f of fields; track f.key) {
        <div class="flex flex-col">
          <label class="text-[11px] text-slate-400 mb-1">{{ f.label }}</label>
          @if (f.type === 'text') {
            <input
              type="text"
              [ngModel]="values[f.key]"
              (ngModelChange)="onText(f.key, $event)"
              [placeholder]="f.label"
              class="h-9 rounded-lg border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-800 px-3 text-sm w-56" />
          } @else {
            <select
              [ngModel]="values[f.key]"
              (ngModelChange)="onSelect(f.key, $event)"
              class="h-9 rounded-lg border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-800 px-2 text-sm w-48">
              <option value="">Todos</option>
              @for (o of f.options; track o.value) {
                <option [value]="o.value">{{ o.label }}</option>
              }
            </select>
          }
        </div>
      }
    </div>
  `,
})
export class FilterBarComponent {
  @Input() fields: FilterField[] = [];
  @Input() values: Record<string, string> = {};
  @Output() filterChange = new EventEmitter<Record<string, string>>();

  private _debounce?: ReturnType<typeof setTimeout>;

  onText(key: string, value: string): void {
    this.values = { ...this.values, [key]: value };
    clearTimeout(this._debounce);
    this._debounce = setTimeout(() => this.filterChange.emit({ ...this.values }), 350);
  }

  onSelect(key: string, value: string): void {
    this.values = { ...this.values, [key]: value };
    this.filterChange.emit({ ...this.values });
  }
}