import { Component, Inject, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { timeout } from 'rxjs';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { MiProyectoService } from '../services/mi-proyecto.service';
import { ReferenciaItem, TIPOS_REFERENCIA } from '../models/proyecto.model';

/** Modal para agregar/editar una referencia bibliográfica estructurada. */
@Component({
  selector: 'app-referencia-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="w-[520px] max-w-[92vw] flex flex-col max-h-[85vh]">
      <h2 class="text-base font-bold text-slate-800 px-3 pt-3 pb-2 shrink-0">{{ r.id ? 'Editar' : 'Agregar' }} referencia</h2>

      <div class="overflow-y-auto px-3 flex-1 min-h-0">
      <!-- Autocompletar por DOI (CrossRef) -->
      <div class="rounded-xl border border-[#8C1D2E]/25 bg-[#FDF6F7] p-3 mb-4">
        <p class="text-[12px] font-bold text-[#8C1D2E] mb-2">🔎 Autocompletar por DOI</p>
        <div class="flex gap-2">
          <input class="inp" [(ngModel)]="r.doi" (keyup.enter)="autocompletar()" placeholder="Pega el DOI, ej. 10.1038/nature12373" />
          <button mat-flat-button class="!h-9 !text-[12px] !rounded-lg !px-3 !font-medium !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                  [disabled]="cargandoDoi() || !r.doi?.trim()" (click)="autocompletar()">
            <span class="inline-flex items-center gap-1.5 leading-none">
              @if (cargandoDoi()) {
                <mat-icon svgIcon="loader-circle" class="size-4 animate-spin" /> Consultando…
              } @else {
                <mat-icon svgIcon="sparkles" class="size-4" /> Rellenar
              }
            </span>
          </button>
        </div>
        <p class="text-[11px] text-slate-500 mt-1.5">Trae autores, título, año, revista, volumen y páginas automáticamente.</p>

        <div class="border-t border-[#8C1D2E]/15 mt-2.5 pt-2.5">
          <p class="text-[11px] font-semibold text-slate-500 mb-1.5">…o busca por título (sin DOI)</p>
          <div class="flex gap-2">
            <input class="inp" [(ngModel)]="queryTitulo" (keyup.enter)="buscarTitulo()" placeholder="Título del libro / tesis / artículo" />
            <button mat-stroked-button class="!h-9 !text-[12px] !rounded-lg !px-3 !text-[#8C1D2E]" [disabled]="buscando() || !queryTitulo.trim()" (click)="buscarTitulo()">
              <span class="inline-flex items-center gap-1 leading-none">
                @if (buscando()) { <mat-icon svgIcon="loader-circle" class="size-4 animate-spin" /> Buscando… } @else { <mat-icon svgIcon="search" class="size-4" /> Buscar }
              </span>
            </button>
          </div>
          @if (resultados().length) {
            <div class="mt-2 max-h-44 overflow-y-auto rounded-lg border border-slate-100 divide-y divide-slate-100">
              @for (res of resultados(); track $index) {
                <button type="button" class="w-full text-left px-2.5 py-1.5 hover:bg-slate-50" (click)="usarResultado(res)">
                  <p class="text-[12px] text-slate-700 leading-snug">{{ res.titulo }}</p>
                  <p class="text-[10.5px] text-slate-400">{{ res.autores || '—' }}<span *ngIf="res.anio"> · {{ res.anio }}</span><span *ngIf="res.fuente"> · {{ res.fuente }}</span></p>
                </button>
              }
            </div>
          } @else if (busco()) {
            <p class="text-[11px] text-slate-400 mt-1.5">Sin resultados. Si es una tesis local no indexada, pega su <b>BibTeX</b> abajo o llena los campos a mano (tipo “Tesis”).</p>
          }
        </div>

        <div class="border-t border-[#8C1D2E]/15 mt-2.5 pt-2.5">
          @if (!mostrarBibtex()) {
            <button type="button" class="text-[11px] font-semibold text-[#8C1D2E] hover:underline" (click)="mostrarBibtex.set(true)">
              …o pega el BibTeX <span class="font-normal text-slate-400">(ideal para tesis) ▾</span>
            </button>
          } @else {
            <p class="text-[11px] font-semibold text-slate-500 mb-1.5">Pega el BibTeX <span class="font-normal text-slate-400">(Google Scholar → Citar → BibTeX)</span></p>
            <textarea class="inp !text-[12px] font-mono" rows="3" [(ngModel)]="bibtex" placeholder="@mastersthesis&#123;… title=&#123;…&#125;, author=&#123;…&#125;, year=&#123;…&#125; &#125;"></textarea>
            <button mat-stroked-button class="!h-8 !text-[12px] !rounded-lg !px-3 !text-[#8C1D2E] mt-1.5" [disabled]="parseando() || !bibtex.trim()" (click)="rellenarBibtex()">
              <span class="inline-flex items-center gap-1 leading-none">
                @if (parseando()) { <mat-icon svgIcon="loader-circle" class="size-4 animate-spin" /> Leyendo… } @else { <mat-icon svgIcon="wand-sparkles" class="size-4" /> Rellenar del BibTeX }
              </span>
            </button>
          }
        </div>
      </div>

      <label class="lbl">Tipo de fuente</label>
      <select class="inp mb-3" [(ngModel)]="r.tipo">
        @for (t of tipos; track t.v) { <option [value]="t.v">{{ t.l }}</option> }
      </select>

      <label class="lbl">Autores <span class="text-slate-400 font-normal">· formato "Apellido, Iniciales; Apellido, Iniciales"</span></label>
      <input class="inp mb-3" [(ngModel)]="r.autores" placeholder="Ej.: García, JM; Pérez, M" />

      <div class="grid grid-cols-[1fr_100px] gap-3 mb-3">
        <div><label class="lbl">Título</label><input class="inp" [(ngModel)]="r.titulo" placeholder="Título del trabajo" /></div>
        <div><label class="lbl">Año</label><input class="inp" [(ngModel)]="r.anio" placeholder="2024" /></div>
      </div>

      @if (r.tipo === 'ARTICULO' || r.tipo === 'CAPITULO_LIBRO') {
        <label class="lbl">{{ r.tipo === 'ARTICULO' ? 'Revista' : 'Libro (título del libro)' }}</label>
        <input class="inp mb-3" [(ngModel)]="r.fuente" placeholder="{{ r.tipo === 'ARTICULO' ? 'Nombre de la revista' : 'Título del libro' }}" />
        <div class="grid grid-cols-3 gap-3 mb-3">
          <div><label class="lbl">Volumen</label><input class="inp" [(ngModel)]="r.volumen" /></div>
          <div><label class="lbl">Número</label><input class="inp" [(ngModel)]="r.numero" /></div>
          <div><label class="lbl">Páginas</label><input class="inp" [(ngModel)]="r.paginas" placeholder="123-130" /></div>
        </div>
        <label class="lbl">DOI <span class="text-slate-400 font-normal">(opcional)</span></label>
        <input class="inp mb-1" [(ngModel)]="r.doi" placeholder="10.1234/xxxx" />
      }

      @if (r.tipo === 'LIBRO' || r.tipo === 'CAPITULO_LIBRO' || r.tipo === 'TESIS' || r.tipo === 'INFORME') {
        <div class="grid grid-cols-2 gap-3 mb-3">
          <div><label class="lbl">Editorial / Institución</label><input class="inp" [(ngModel)]="r.editorial" /></div>
          <div><label class="lbl">Ciudad</label><input class="inp" [(ngModel)]="r.ciudad" /></div>
        </div>
      }

      @if (r.tipo === 'PAGINA_WEB') {
        <label class="lbl">Sitio / editor</label>
        <input class="inp mb-3" [(ngModel)]="r.fuente" placeholder="Nombre del sitio" />
        <div class="grid grid-cols-[1fr_130px] gap-3 mb-3">
          <div><label class="lbl">URL</label><input class="inp" [(ngModel)]="r.url" placeholder="https://…" /></div>
          <div><label class="lbl">Fecha de acceso</label><input class="inp" [(ngModel)]="r.fechaAcceso" placeholder="12 jul 2026" /></div>
        </div>
      }

      </div>

      <div class="flex items-center justify-end gap-2 px-3 py-2.5 border-t border-slate-100 shrink-0">
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="cancelar()">Cancelar</button>
        <button mat-flat-button
                class="!h-9 !text-sm !rounded-lg !px-4 !font-medium !bg-[#8C1D2E] !text-white hover:!bg-[#731725]"
                [disabled]="!r.titulo?.trim()" (click)="guardar()">
          {{ r.id ? 'Guardar' : 'Agregar referencia' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .lbl { display:block; font-size:12px; font-weight:600; color:#64748b; margin-bottom:5px; }
    .inp { width:100%; border:1px solid #e2e8f0; border-radius:8px; padding:8px 12px; font-size:14px; outline:none; }
    .inp:focus { border-color:#8C1D2E; }
  `],
})
export class ReferenciaDialogComponent {
  private _svc = inject(MiProyectoService);
  private _toast = inject(NotificationService);
  protected readonly tipos = TIPOS_REFERENCIA;
  protected r: ReferenciaItem;
  protected cargandoDoi = signal(false);
  protected queryTitulo = '';
  protected buscando = signal(false);
  protected busco = signal(false);
  protected resultados = signal<any[]>([]);
  protected bibtex = '';
  protected parseando = signal(false);
  protected mostrarBibtex = signal(false);

  /** Al usar un método de autocompletado, limpia los inputs de los otros métodos. */
  private limpiarBusquedas(): void {
    this.queryTitulo = '';
    this.resultados.set([]);
    this.busco.set(false);
    this.bibtex = '';
    this.mostrarBibtex.set(false);
  }

  constructor(
    public dialogRef: MatDialogRef<ReferenciaDialogComponent>,
    @Inject(MAT_DIALOG_DATA) data: ReferenciaItem | null,
  ) {
    this.r = data ? { ...data } : { tipo: 'ARTICULO' };
  }

  autocompletar(): void {
    const doi = (this.r.doi || '').trim();
    if (!doi || this.cargandoDoi()) return;
    this.cargandoDoi.set(true);
    // timeout de seguridad: si CrossRef no responde en 20 s, no se queda cargando para siempre.
    this._svc.buscarDoi$(doi).pipe(timeout(20000)).subscribe({
      next: (res) => {
        const d = res?.data ?? res;
        // Combina lo recibido conservando el id (edición) y el DOI ingresado.
        this.r = { ...this.r, ...d, id: this.r.id };
        this.cargandoDoi.set(false);
        this.limpiarBusquedas();
        this._toast.success('Datos traídos de CrossRef');
      },
      error: (e) => {
        this.cargandoDoi.set(false);
        const msg = e?.name === 'TimeoutError'
          ? 'La consulta tardó demasiado; revisa tu conexión e intenta de nuevo.'
          : (e?.error?.message ?? 'No se pudo consultar el DOI');
        this._toast.error(msg);
      },
    });
  }

  buscarTitulo(): void {
    const q = this.queryTitulo.trim();
    if (!q || this.buscando()) return;
    this.buscando.set(true);
    this._svc.buscarTitulo$(q).pipe(timeout(20000)).subscribe({
      next: (res) => {
        this.resultados.set((res?.data ?? res) ?? []);
        this.busco.set(true);
        this.buscando.set(false);
      },
      error: (e) => {
        this.resultados.set([]);
        this.busco.set(true);
        this.buscando.set(false);
        this._toast.error(e?.name === 'TimeoutError' ? 'La búsqueda tardó demasiado.' : (e?.error?.message ?? 'No se pudo buscar'));
      },
    });
  }
  usarResultado(res: any): void {
    this.r = { ...this.r, ...res, id: this.r.id };
    this.limpiarBusquedas();
    this._toast.success('Datos cargados de la búsqueda');
  }
  rellenarBibtex(): void {
    const bib = this.bibtex.trim();
    if (!bib || this.parseando()) return;
    this.parseando.set(true);
    this._svc.parsearBibtex$(bib).pipe(timeout(15000)).subscribe({
      next: (res) => {
        this.r = { ...this.r, ...(res?.data ?? res), id: this.r.id };
        this.parseando.set(false);
        this.limpiarBusquedas();
        this._toast.success('Referencia leída del BibTeX');
      },
      error: (e) => {
        this.parseando.set(false);
        this._toast.error(e?.error?.message ?? 'No se pudo leer el BibTeX');
      },
    });
  }

  guardar(): void {
    if (!this.r.titulo?.trim()) return;
    this.dialogRef.close(this.r);
  }
  cancelar(): void { this.dialogRef.close(null); }
}
