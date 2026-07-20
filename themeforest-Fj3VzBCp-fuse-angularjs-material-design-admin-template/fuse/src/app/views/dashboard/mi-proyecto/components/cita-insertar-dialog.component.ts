import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ReferenciaItem } from '../models/proyecto.model';

export interface CitaInsertarData { ref: ReferenciaItem; estilo: string; }

/** Elige cómo insertar la cita: parentética / narrativa, y página opcional (cita textual). */
@Component({
  selector: 'app-cita-insertar-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <div class="w-[440px] max-w-full p-2">
      <h2 class="text-base font-bold text-slate-800 mb-1">Insertar cita</h2>
      <p class="text-[12px] text-slate-500 mb-3 leading-snug">{{ data.ref.formateada }}</p>

      <label class="lbl">¿Cómo quieres citar?</label>
      <div class="grid grid-cols-2 gap-2 mb-3">
        <button type="button" class="opt" [class.on]="modo === 'parentetica'" (click)="modo = 'parentetica'">
          <span class="t">Parentética</span>
          <span class="ej">{{ data.ref.citaTexto }}</span>
          <span class="d">Autor y año entre paréntesis, al final de la idea.</span>
        </button>
        <button type="button" class="opt" [class.on]="modo === 'narrativa'" (click)="modo = 'narrativa'">
          <span class="t">Narrativa</span>
          <span class="ej">{{ data.ref.citaNarrativa }}</span>
          <span class="d">El autor forma parte de la oración.</span>
        </button>
      </div>

      @if (!numerico()) {
        <label class="lbl">Página <span class="text-slate-400 font-normal">· opcional, solo para cita textual (copiaste las palabras exactas)</span></label>
        <input class="inp mb-2" [(ngModel)]="pagina" placeholder="Ej.: 45   ó   45-47" />
      }

      <div class="rounded-lg bg-[#FDF6F7] border border-[#8C1D2E]/20 px-3 py-2 mb-4">
        <span class="text-[11px] text-slate-500">Se insertará:</span>
        <span class="text-[13px] font-bold text-[#8C1D2E] ml-1">{{ preview() }}</span>
      </div>

      <div class="flex items-center justify-end gap-2">
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="cancelar()">Cancelar</button>
        <button mat-flat-button class="!h-9 !text-sm !rounded-lg !px-4 !font-medium !bg-[#8C1D2E] !text-white hover:!bg-[#731725]" (click)="insertar()">Insertar cita</button>
      </div>
    </div>
  `,
  styles: [`
    .lbl { display:block; font-size:12px; font-weight:600; color:#64748b; margin-bottom:5px; }
    .inp { width:100%; border:1px solid #e2e8f0; border-radius:8px; padding:8px 12px; font-size:14px; outline:none; }
    .inp:focus { border-color:#8C1D2E; }
    .opt { display:flex; flex-direction:column; gap:2px; text-align:left; border:1.5px solid #e2e8f0; border-radius:10px; padding:9px 11px; cursor:pointer; background:#fff; transition:.15s; }
    .opt:hover { border-color:#cbd5e1; }
    .opt.on { border-color:#8C1D2E; background:#FDF6F7; }
    .opt .t { font-size:12.5px; font-weight:700; color:#334155; }
    .opt.on .t { color:#8C1D2E; }
    .opt .ej { font-size:12px; color:#8C1D2E; font-weight:600; }
    .opt .d { font-size:10.5px; color:#94a3b8; line-height:1.25; }
  `],
})
export class CitaInsertarDialogComponent {
  protected modo: 'parentetica' | 'narrativa' = 'parentetica';
  protected pagina = '';

  constructor(
    public dialogRef: MatDialogRef<CitaInsertarDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CitaInsertarData,
  ) {}

  numerico(): boolean { return this.data.estilo === 'VANCOUVER' || this.data.estilo === 'IEEE'; }

  preview(): string {
    const base = this.modo === 'narrativa' ? (this.data.ref.citaNarrativa ?? '') : (this.data.ref.citaTexto ?? '');
    return this.conPagina(base);
  }

  private conPagina(cita: string): string {
    const pg = (this.pagina || '').trim();
    if (!pg || this.numerico()) return cita;
    const suf = this.data.estilo === 'MLA' ? ' ' + pg : ', p. ' + pg;
    const i = cita.lastIndexOf(')');
    return i >= 0 ? cita.slice(0, i) + suf + cita.slice(i) : cita + ' (' + pg + ')';
  }

  insertar(): void { this.dialogRef.close(this.preview()); }
  cancelar(): void { this.dialogRef.close(null); }
}
