import { Component, Inject, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { CoordinadorProyectoService } from '../services/coordinador-proyecto.service';

export interface DesignarRevisoresData {
  tesisId: string;
  estudiante: string;
  titulo: string;
}

/** Modal para que el Coordinador designe los 2 revisores del proyecto (Etapa 5). */
@Component({
  selector: 'app-designar-revisores-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="w-[520px] max-w-[92vw] flex flex-col max-h-[85vh]">
      <div class="px-4 pt-4 pb-2 shrink-0">
        <h2 class="text-base font-bold text-slate-800">Designar revisores del proyecto</h2>
        <p class="text-[12px] text-slate-500 mt-0.5">{{ data.estudiante }} · <span class="text-slate-400">{{ data.titulo }}</span></p>
      </div>

      <div class="px-4 flex-1 min-h-0 overflow-y-auto">
        <p class="text-[12px] text-slate-500 mb-2">Selecciona <b>exactamente 2 docentes</b> como revisores (Jurado Informante del proyecto). No puede ser el asesor.</p>

        <input class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm mb-2 focus:outline-none focus:border-[#8C1D2E]"
               [(ngModel)]="filtro" placeholder="Buscar docente…" />

        <div class="rounded-lg border border-slate-100 divide-y divide-slate-100">
          @for (d of filtrados(); track d.id) {
            <button type="button" class="w-full flex items-center gap-2 px-3 py-2 text-left hover:bg-slate-50"
                    [ngClass]="{ 'bg-[#FDF6F7]': sel().includes(d.id) }" (click)="toggle(d.id)">
              <span class="size-4 rounded border flex items-center justify-center shrink-0"
                    [ngClass]="sel().includes(d.id) ? 'bg-[#8C1D2E] border-[#8C1D2E]' : 'border-slate-300'">
                @if (sel().includes(d.id)) { <mat-icon svgIcon="check" class="size-3 text-white" /> }
              </span>
              <span class="flex-1 text-[13px] text-slate-700">{{ d.nombre }}</span>
              @if (d.categoria) { <span class="text-[10px] text-slate-400">{{ d.categoria }}</span> }
            </button>
          }
          @if (!filtrados().length) { <p class="px-3 py-3 text-[12px] text-slate-400">Sin resultados.</p> }
        </div>
      </div>

      <div class="flex items-center justify-between gap-2 px-4 py-3 border-t border-slate-100 shrink-0">
        <span class="text-[12px]" [ngClass]="sel().length === 2 ? 'text-emerald-600 font-semibold' : 'text-slate-400'">
          {{ sel().length }} / 2 seleccionados
        </span>
        <div class="flex gap-2">
          <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="cancelar()">Cancelar</button>
          <button mat-flat-button class="!h-9 !text-sm !rounded-lg !px-4 !font-medium !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                  [disabled]="sel().length !== 2 || guardando()" (click)="designar()">
            @if (guardando()) { <mat-icon svgIcon="loader-circle" class="size-4 mr-1 animate-spin" /> } Designar revisores
          </button>
        </div>
      </div>
    </div>
  `,
})
export class DesignarRevisoresDialogComponent {
  private _svc = inject(CoordinadorProyectoService);
  private _toast = inject(NotificationService);

  protected docentes = signal<any[]>([]);
  protected sel = signal<string[]>([]);
  protected filtro = '';
  protected guardando = signal(false);

  protected filtrados = computed(() => {
    const q = this.filtro.trim().toLowerCase();
    const list = this.docentes();
    return q ? list.filter((d) => (d.nombre ?? '').toLowerCase().includes(q)) : list;
  });

  constructor(
    public dialogRef: MatDialogRef<DesignarRevisoresDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DesignarRevisoresData,
  ) {
    this._svc.docentes$().subscribe({
      next: (res) => this.docentes.set((res?.data ?? res) ?? []),
      error: () => this._toast.error('No se pudieron cargar los docentes'),
    });
  }

  toggle(id: string): void {
    const cur = this.sel();
    if (cur.includes(id)) { this.sel.set(cur.filter((x) => x !== id)); return; }
    if (cur.length >= 2) { this._toast.error('Solo puedes seleccionar 2 revisores'); return; }
    this.sel.set([...cur, id]);
  }

  designar(): void {
    if (this.sel().length !== 2 || this.guardando()) return;
    this.guardando.set(true);
    this._svc.designar$(this.data.tesisId, this.sel()).subscribe({
      next: () => { this._toast.success('Revisores designados'); this.dialogRef.close(true); },
      error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo designar'); },
    });
  }
  cancelar(): void { this.dialogRef.close(false); }
}
