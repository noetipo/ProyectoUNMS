import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Ficha del docente candidato a asesor. Se abre desde "Ver más" del diálogo de sugerencias:
 * así la lista queda compacta y el detalle no compite con el botón de agregar.
 */
@Component({
  selector: 'app-docente-perfil-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="w-[470px] max-w-full">

      <!-- Cabecera: identidad sobre fondo de marca -->
      <div class="relative px-5 pt-5 pb-4 bg-[#FDF6F7] border-b border-[#8C1D2E]/10">
        <button mat-icon-button mat-dialog-close class="!absolute !top-2 !right-2 !size-8">
          <mat-icon svgIcon="x" class="size-4 text-slate-400" />
        </button>

        <div class="flex items-start gap-3 pr-8">
          <div class="size-12 shrink-0 rounded-full bg-[#8C1D2E] text-white grid place-items-center
                      text-[15px] font-bold tracking-wide shadow-sm">
            {{ iniciales() }}
          </div>
          <div class="min-w-0">
            <h2 class="text-[15px] font-bold text-slate-800 leading-tight">
              {{ d.apellidos }}, {{ d.nombres }}
            </h2>
            <p class="text-[11.5px] text-[#8C1D2E] font-semibold mt-0.5">
              {{ d.gradoAcademico ?? 'Sin grado registrado' }}
            </p>
            @if (situacion(); as s) {
              <p class="text-[11.5px] text-slate-500 mt-0.5">{{ s }}</p>
            }
            @if (d.cargoActual) {
              <p class="inline-flex items-center gap-1 mt-1 px-2 py-0.5 rounded-full bg-white
                        border border-slate-200 text-[11px] text-slate-600">
                <mat-icon svgIcon="briefcase" class="!size-3 text-slate-400" />{{ d.cargoActual }}
              </p>
            }
          </div>
        </div>
      </div>

      <div class="px-5 py-4 space-y-4">

        <!-- Disponibilidad: lo primero que se mira para repartir carga -->
        <div class="flex items-center gap-2 rounded-xl border px-3 py-2.5"
             [ngClass]="d.asesoriasActivas ? 'border-amber-200 bg-amber-50' : 'border-emerald-200 bg-emerald-50'">
          <mat-icon [svgIcon]="d.asesoriasActivas ? 'clock' : 'circle-check'"
                    class="size-4 shrink-0"
                    [ngClass]="d.asesoriasActivas ? 'text-amber-600' : 'text-emerald-600'" />
          <div class="min-w-0">
            <p class="text-[12.5px] font-semibold"
               [ngClass]="d.asesoriasActivas ? 'text-amber-800' : 'text-emerald-800'">
              {{ d.asesoriasActivas ? 'Con carga asignada' : 'Disponible' }}
            </p>
            <p class="text-[11px] text-slate-500">
              {{ d.asesoriasActivas || 'Ninguna' }} asesoría{{ d.asesoriasActivas === 1 ? '' : 's' }} activa{{ d.asesoriasActivas === 1 ? '' : 's' }}
            </p>
          </div>
        </div>

        <!-- Trayectoria: dónde trabaja y desde cuándo. Es lo primero que preguntan al designar. -->
        @if (d.centroLaboral || d.experienciaAnios) {
          <section>
            <p class="flex items-center gap-1.5 text-[10.5px] font-bold text-slate-400 uppercase tracking-wide mb-1.5">
              <mat-icon svgIcon="building-2" class="!size-3.5" /> Centro de trabajo
            </p>
            @if (d.centroLaboral) {
              <p class="text-[12.5px] font-semibold text-slate-700 leading-snug">{{ d.centroLaboral }}</p>
              @if (d.centroLaboralDetalle) {
                <p class="text-[11.5px] text-slate-400 leading-snug">{{ d.centroLaboralDetalle }}</p>
              }
            } @else {
              <p class="text-[12px] text-slate-400 italic">Sin centro laboral registrado</p>
            }
            <div class="flex flex-wrap gap-1.5 mt-1.5">
              @if (d.experienciaAnios) {
                <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[11px]">
                  <mat-icon svgIcon="clock" class="!size-3 text-slate-400" /> {{ d.experienciaAnios }} años de experiencia
                </span>
              }
              @if (d.condicion) {
                <span class="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[11px]">{{ d.condicion }}</span>
              }
            </div>
          </section>
        }

        <!-- Estudios -->
        <section>
          <p class="flex items-center gap-1.5 text-[10.5px] font-bold text-slate-400 uppercase tracking-wide mb-1.5">
            <mat-icon svgIcon="graduation-cap" class="!size-3.5" /> Estudios
          </p>
          @if (d.estudios?.length) {
            <ul class="border-l-2 border-slate-100 pl-3 space-y-1.5">
              @for (e of d.estudios; track e) {
                <li class="relative text-[12.5px] text-slate-700 leading-snug">
                  <span class="absolute -left-[17px] top-1.5 size-1.5 rounded-full bg-[#8C1D2E]/40"></span>
                  {{ e }}
                </li>
              }
            </ul>
          } @else {
            <p class="text-[12px] text-slate-400 italic">Sin grados registrados</p>
          }
        </section>

        <!-- Líneas de investigación -->
        <section>
          <p class="flex items-center gap-1.5 text-[10.5px] font-bold text-slate-400 uppercase tracking-wide mb-1.5">
            <mat-icon svgIcon="lightbulb" class="!size-3.5" /> Líneas de investigación
          </p>
          @if (d.lineas?.length) {
            <div class="flex flex-wrap gap-1">
              @for (l of d.lineas; track l) {
                <span class="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[11px]">{{ l }}</span>
              }
            </div>
          } @else {
            <p class="text-[12px] text-slate-400 italic">Ninguna registrada</p>
          }
        </section>

        @if (d.emailInstitucional || d.orcid) {
          <div class="pt-1 border-t border-slate-100 space-y-1">
            @if (d.emailInstitucional) {
              <p class="flex items-center gap-1.5 text-[11.5px] text-slate-500">
                <mat-icon svgIcon="mail" class="!size-3.5 text-slate-400 shrink-0" />
                <span class="truncate">{{ d.emailInstitucional }}</span>
              </p>
            }
            @if (d.orcid) {
              <p class="flex items-center gap-1.5 text-[11.5px] text-slate-500">
                <mat-icon svgIcon="hash" class="!size-3.5 text-slate-400 shrink-0" />
                <span class="truncate">ORCID {{ d.orcid }}</span>
              </p>
            }
          </div>
        }
      </div>

      <div class="flex items-center justify-end gap-2 px-5 py-3 border-t border-slate-100 bg-slate-50/60">
        <button mat-stroked-button mat-dialog-close class="!h-8 !text-xs">Cerrar</button>
        @if (!d.yaSugerido) {
          <!-- Mismas dos opciones que el menú de la lista; se oculta la del puesto ya cubierto. -->
          @if (!d.coasesorTomado) {
            <button mat-stroked-button class="!h-8 !text-xs !text-sky-700 !border-sky-200"
                    (click)="ref.close('COASESOR')">Como co-asesor</button>
          }
          @if (!d.asesorTomado) {
            <button class="btn-dark !h-8 !text-xs !px-4" (click)="ref.close('ASESOR')">
              <mat-icon svgIcon="handshake" class="size-3.5 mr-1" /> Como asesor
            </button>
          }
        } @else {
          <span class="text-[11.5px] text-emerald-600 font-semibold px-2">Ya sugerido</span>
        }
      </div>
    </div>
  `,
})
export class DocentePerfilDialogComponent {
  protected ref = inject(MatDialogRef<DocentePerfilDialogComponent>);
  /** El docente tal cual viene de /asesores-candidatos, más `yaSugerido`. */
  protected d = inject<any>(MAT_DIALOG_DATA);

  /** Iniciales para el avatar: primera del apellido y primera del nombre. */
  protected iniciales = computed(() => {
    const ap = (this.d?.apellidos ?? '').trim();
    const no = (this.d?.nombres ?? '').trim();
    return ((ap.charAt(0) || '') + (no.charAt(0) || '')).toUpperCase() || '—';
  });

  /** "Docente principal · Nombrado", omitiendo lo que no esté registrado. */
  protected situacion = computed(() => {
    const partes: string[] = [];
    if (this.d?.categoria) { partes.push(`Docente ${String(this.d.categoria).toLowerCase()}`); }
    if (this.d?.condicion) {
      const c = String(this.d.condicion);
      partes.push(c.charAt(0) + c.slice(1).toLowerCase());
    }
    return partes.join(' · ');
  });
}
