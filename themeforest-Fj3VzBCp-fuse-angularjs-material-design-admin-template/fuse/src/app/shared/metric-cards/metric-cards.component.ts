import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

export interface MetricCard {
  label: string;
  value: number | string;
  accent?: string; // clase tailwind para el número, p.ej. 'text-emerald-600'
}

@Component({
  selector: 'app-metric-cards',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      @for (c of cards; track c.label) {
        <div class="rounded-xl border border-slate-100 dark:border-slate-700 bg-white dark:bg-slate-800 p-4">
          <div class="text-xs text-slate-400">{{ c.label }}</div>
          <div class="text-2xl font-semibold" [class]="c.accent || 'text-slate-800 dark:text-slate-100'">
            {{ c.value }}
          </div>
        </div>
      }
    </div>
  `,
})
export class MetricCardsComponent {
  @Input() cards: MetricCard[] = [];
}