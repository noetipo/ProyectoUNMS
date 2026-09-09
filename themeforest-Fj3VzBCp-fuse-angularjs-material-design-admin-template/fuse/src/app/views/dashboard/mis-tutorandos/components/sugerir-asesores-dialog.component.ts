import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { toSignal } from '@angular/core/rxjs-interop';
import { SugerenciaAsesorService } from '../services/sugerencia-asesor.service';
import { DocentePerfilDialogComponent } from './docente-perfil-dialog.component';

interface DialogData {
  estudianteId: string;
  estudianteNombre: string;
  lineaNombre?: string;
}

@Component({
  selector: 'app-sugerir-asesores-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatMenuModule],
  template: `
    <div class="p-5 w-[540px] max-w-full">
      <div class="flex items-start justify-between mb-3">
        <div>
          <h2 class="text-lg font-semibold text-slate-800">Sugerir asesores</h2>
          <p class="text-xs text-slate-400 mt-0.5">Tutorando: {{ data.estudianteNombre }}</p>
          @if (lineaNombre()) {
            <p class="text-xs text-slate-500 mt-0.5">
              Línea del tema: <span class="font-medium text-slate-600">{{ lineaNombre() }}</span>
            </p>
          }
        </div>
        <button mat-icon-button (click)="ref.close(changed)" class="!w-8 !h-8">
          <mat-icon svgIcon="x" class="size-4 text-slate-400" />
        </button>
      </div>

      <!-- Sugerencias actuales -->
      <div class="mb-4">
        <p class="text-xs font-medium text-slate-500 mb-1">Sugeridos actuales</p>
        @if (sugeridos().length) {
          <div class="space-y-1">
            @for (s of sugeridos(); track s.sugerenciaId) {
              <div class="flex items-center justify-between gap-2 rounded-lg border border-slate-100 px-3 py-1.5">
                <span class="text-sm text-slate-700 min-w-0">
                  <span class="px-1.5 py-0.5 rounded text-[10px] font-bold mr-1.5"
                        [ngClass]="s.tipo === 'COASESOR' ? 'bg-sky-100 text-sky-700' : 'bg-[#FDF6F7] text-[#8C1D2E]'">
                    {{ s.tipo === 'COASESOR' ? 'CO-ASESOR' : 'ASESOR' }}
                  </span>
                  {{ s.apellidos }}, {{ s.nombres }}
                  @if (s.gradoAcademico) { <span class="text-xs text-slate-400"> · {{ s.gradoAcademico }}</span> }
                </span>
                <button mat-icon-button class="!w-7 !h-7 shrink-0" (click)="quitar(s.sugerenciaId)" title="Quitar">
                  <mat-icon svgIcon="x" class="text-rose-400 size-3.5" />
                </button>
              </div>
            }
          </div>
        } @else {
          <p class="text-sm text-slate-400">Aún no hay asesores sugeridos.</p>
        }
      </div>

      <!-- Los dos puestos de la tesis: mientras no haya designación, se pueden sugerir varios
           candidatos por puesto para que el doctorando elija a quién solicitar. -->
      <div class="grid grid-cols-2 gap-2 mb-3">
        <div class="rounded-lg border px-2.5 py-1.5"
             [ngClass]="asesorTomado() ? 'border-[#8C1D2E]/20 bg-[#FDF6F7]' : 'border-dashed border-slate-200'">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wide">Asesor</p>
          @if (asesorActual()) {
            <p class="text-[11.5px] leading-tight text-[#8C1D2E] font-semibold">{{ asesorActual() }}</p>
          } @else if (sugeridosAsesor().length) {
            <p class="text-[11.5px] leading-tight text-slate-500">
              {{ sugeridosAsesor().length }} candidato{{ sugeridosAsesor().length === 1 ? '' : 's' }} sugerido{{ sugeridosAsesor().length === 1 ? '' : 's' }}
            </p>
          } @else {
            <p class="text-[11.5px] leading-tight text-slate-400">Puesto libre</p>
          }
        </div>
        <div class="rounded-lg border px-2.5 py-1.5"
             [ngClass]="coasesorTomado() ? 'border-sky-200 bg-sky-50' : 'border-dashed border-slate-200'">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wide">Co-asesor <span class="font-normal normal-case">(opcional)</span></p>
          @if (coasesorActual()) {
            <p class="text-[11.5px] leading-tight text-sky-700 font-semibold">{{ coasesorActual() }}</p>
          } @else if (sugeridosCoasesor().length) {
            <p class="text-[11.5px] leading-tight text-slate-500">
              {{ sugeridosCoasesor().length }} candidato{{ sugeridosCoasesor().length === 1 ? '' : 's' }} sugerido{{ sugeridosCoasesor().length === 1 ? '' : 's' }}
            </p>
          } @else {
            <p class="text-[11.5px] leading-tight text-slate-400">Puesto libre</p>
          }
        </div>
      </div>

      <p class="text-xs font-medium text-slate-500 mb-1">
        Docentes de la línea del tema
        @if (!cupoLleno()) {
          <span class="text-slate-400 font-normal">— puedes sugerir varios por puesto</span>
        } @else {
          <span class="text-slate-400 font-normal">— ambos puestos ya están designados</span>
        }
      </p>

      @if (cargando()) {
        <p class="text-xs text-slate-400 mt-2">Cargando candidatos…</p>
      } @else if (conLinea() === false) {
        <div class="rounded-lg bg-amber-50 border border-amber-200 px-3 py-2 text-sm text-amber-700 mt-1">
          El tutorando aún no tiene un tema con línea de investigación registrada. El coordinador debe registrar el tema y su línea.
        </div>
      } @else {
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="w-full">
          <mat-icon matPrefix svgIcon="search" class="size-4 text-slate-400" />
          <input matInput [formControl]="buscar" placeholder="Filtrar por nombre o apellido…" />
        </mat-form-field>

        @if (resultados().length) {
          <div class="mt-2 max-h-48 overflow-y-auto space-y-1">
            @for (d of resultados(); track d.id) {
              <!-- Fila compacta: lo justo para decidir. El detalle (cargo, estudios, líneas)
                   vive en el modal de "Ver más" para no competir con el botón de agregar. -->
              <div class="flex items-start justify-between gap-3 rounded-lg hover:bg-slate-50 px-3 py-2">
                <div class="min-w-0">
                  <p class="text-sm text-slate-700 leading-snug">
                    {{ d.apellidos }}, {{ d.nombres }}
                    @if (d.gradoAcademico) { <span class="text-xs text-slate-400"> · {{ d.gradoAcademico }}</span> }
                  </p>
                  <p class="text-[11px] leading-[1.35]">
                    <span [ngClass]="d.asesoriasActivas ? 'text-amber-600' : 'text-emerald-600'">
                      {{ d.asesoriasActivas || 'Sin' }} asesoría{{ d.asesoriasActivas === 1 ? '' : 's' }} activa{{ d.asesoriasActivas === 1 ? '' : 's' }}
                    </span>
                    <button type="button" class="text-slate-400 hover:text-[#8C1D2E] underline underline-offset-2 ml-1.5"
                            (click)="verMas(d)">Ver más</button>
                  </p>
                </div>

                <!-- La elección del puesto vive en el propio botón: un clic, dos opciones claras. -->
                <button mat-stroked-button class="!h-7 !text-xs !min-w-0 !px-2.5 shrink-0"
                        [disabled]="yaSugerido(d.id) || saving() || cupoLleno()"
                        [matMenuTriggerFor]="rolMenu" [matMenuTriggerData]="{ d: d }">
                  {{ yaSugerido(d.id) ? 'Agregado' : 'Agregar' }}
                  @if (!yaSugerido(d.id)) { <mat-icon svgIcon="chevron-down" class="!size-3 ml-0.5" /> }
                </button>
              </div>
            }
          </div>
        } @else if (candidatos().length) {
          <p class="text-sm text-slate-400 mt-2">Ningún docente coincide con el filtro.</p>
        } @else {
          <p class="text-sm text-slate-400 mt-2">No hay docentes registrados en esta línea de investigación.</p>
        }
      }

      @if (err()) { <p class="text-xs text-rose-500 mt-2">{{ err() }}</p> }

      <div class="flex justify-end mt-4">
        <button mat-stroked-button (click)="ref.close(changed)">Cerrar</button>
      </div>
    </div>

    <!-- Menú de puesto, compartido por la lista y por la ficha "Ver más". Se desactiva la
         opción cuyo puesto ya está cubierto por una designación vigente. -->
    <mat-menu #rolMenu="matMenu">
      <ng-template matMenuContent let-d="d">
        <button mat-menu-item [disabled]="asesorTomado()" (click)="agregar(d, 'ASESOR')">
          <mat-icon svgIcon="handshake" class="size-4 text-[#8C1D2E]" />
          <span>Como <b>asesor</b>@if (asesorTomado()) { <span class="text-[10.5px] text-slate-400">— ya designado</span> }</span>
        </button>
        <button mat-menu-item [disabled]="coasesorTomado()" (click)="agregar(d, 'COASESOR')">
          <mat-icon svgIcon="user-round-plus" class="size-4 text-sky-600" />
          <span>Como <b>co-asesor</b>@if (coasesorTomado()) { <span class="text-[10.5px] text-slate-400">— ya designado</span> }</span>
        </button>
      </ng-template>
    </mat-menu>
  `,
})
export class SugerirAsesoresDialogComponent implements OnInit {
  private _svc = inject(SugerenciaAsesorService);
  private _dialog = inject(MatDialog);
  protected ref = inject(MatDialogRef<SugerirAsesoresDialogComponent>);
  protected data = inject<DialogData>(MAT_DIALOG_DATA);

  protected buscar = new FormControl('');
  private _filtro = toSignal(this.buscar.valueChanges, { initialValue: '' });

  protected sugeridos = signal<any[]>([]);
  protected candidatos = signal<any[]>([]);
  protected conLinea = signal<boolean | null>(null);
  protected lineaNombre = signal<string | null>(null);
  protected cargando = signal(true);
  protected saving = signal(false);
  protected err = signal<string | null>(null);
  protected changed = false;

  /** Designación vigente del tutorando (viene con los candidatos). */
  protected asesorActual = signal<string | null>(null);
  protected coasesorActual = signal<string | null>(null);

  /**
   * Un puesto está tomado cuando ya tiene una designación vigente: hasta entonces se pueden
   * seguir sugiriendo candidatos, para que el doctorando elija a quién solicitar.
   */
  protected asesorTomado = computed(() => !!this.asesorActual());
  protected coasesorTomado = computed(() => !!this.coasesorActual());

  /** Sin puestos libres no hay nada más que sugerir. */
  protected cupoLleno = computed(() => this.asesorTomado() && this.coasesorTomado());

  /** Candidatos sugeridos para cada puesto (puede haber varios: el doctorando elige). */
  protected sugeridosAsesor = computed(() => this.sugeridos().filter((s: any) => (s.tipo ?? 'ASESOR') === 'ASESOR'));
  protected sugeridosCoasesor = computed(() => this.sugeridos().filter((s: any) => s.tipo === 'COASESOR'));

  /** Ficha completa del docente; desde ahí se sugiere eligiendo el puesto igual que en la lista. */
  protected verMas(d: any): void {
    this._dialog.open(DocentePerfilDialogComponent, {
      data: {
        ...d,
        yaSugerido: this.yaSugerido(d.id) || this.cupoLleno(),
        asesorTomado: this.asesorTomado(),
        coasesorTomado: this.coasesorTomado(),
      },
      autoFocus: false,
    }).afterClosed().subscribe((accion) => {
      if (accion === 'ASESOR' || accion === 'COASESOR') { this.agregar(d, accion); }
    });
  }

  /** Filtro cliente sobre los candidatos de la línea. */
  protected resultados = computed(() => {
    const q = (this._filtro() ?? '').trim().toLowerCase();
    const list = this.candidatos();
    if (!q) { return list; }
    return list.filter((d) =>
      `${d.apellidos ?? ''} ${d.nombres ?? ''}`.toLowerCase().includes(q));
  });

  ngOnInit(): void {
    this.cargar();
    this.cargarCandidatos();
  }

  cargar(): void {
    this._svc.listar$(this.data.estudianteId).subscribe({
      next: (res: any) => this.sugeridos.set(res?.data ?? res ?? []),
      error: () => this.sugeridos.set([]),
    });
  }

  cargarCandidatos(): void {
    this.cargando.set(true);
    this._svc.candidatos$(this.data.estudianteId).subscribe({
      next: (res: any) => {
        const d = res?.data ?? res ?? {};
        this.conLinea.set(!!d.conLinea);
        this.lineaNombre.set(d.lineaNombre ?? this.data.lineaNombre ?? null);
        this.candidatos.set(d.docentes ?? []);
        this.asesorActual.set(d.asesorActual ?? null);
        this.coasesorActual.set(d.coasesorActual ?? null);
        this.cargando.set(false);
      },
      error: () => {
        this.conLinea.set(null);
        this.candidatos.set([]);
        this.cargando.set(false);
        this.err.set('No se pudieron cargar los candidatos.');
      },
    });
  }

  yaSugerido(docenteId: string): boolean {
    return this.sugeridos().some((s) => s.asesorDocenteId === docenteId);
  }

  /** @param tipo puesto elegido en el menú del botón "Agregar" (o en la ficha "Ver más"). */
  agregar(d: any, tipo: 'ASESOR' | 'COASESOR' = 'ASESOR'): void {
    if (this.yaSugerido(d.id)) return;
    this.saving.set(true); this.err.set(null);
    this._svc.sugerir$(this.data.estudianteId, d.id, tipo).subscribe({
      next: () => { this.saving.set(false); this.changed = true; this.cargar(); },
      error: (e) => { this.saving.set(false); this.err.set(e?.error?.message ?? e?.error?.error ?? 'No se pudo sugerir'); },
    });
  }

  quitar(sugerenciaId: string): void {
    this._svc.quitar$(sugerenciaId).subscribe({
      next: () => { this.changed = true; this.cargar(); },
      error: (e) => this.err.set(e?.error?.message ?? 'No se pudo quitar'),
    });
  }
}
