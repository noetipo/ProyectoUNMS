import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { previsualizarBlob } from '@/app/shared/perfil-completo/preview.util';
import { MiProyectoService } from '../services/mi-proyecto.service';

/**
 * "Cierre y envío" — el trámite que sigue a la redacción del proyecto: informe de Turnitin,
 * proyecto en versión final y solicitud de aprobación a la UPG.
 *
 * <p>Vivía como último paso <i>dentro</i> del editor (2026-08-03: "no debería ser parte del
 * proyecto en sí"), donde estorbaba mientras se redactaba y quedaba escondido justo cuando
 * empieza a importar. Ahora es su propia pestaña, que aparece únicamente cuando el asesor emite
 * su carta de opinión favorable; hasta entonces el editor solo señala hacia aquí.</p>
 */
@Component({
  selector: 'app-cierre-envio',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Proceso de Tesis</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Cierre y envío</span>
          </div>
          <h1 class="page-title">Cierre y envío del proyecto</h1>
        </div>
        @if (e(); as p) {
          <span class="text-[11px] font-semibold px-2.5 py-1 rounded-full"
                [class]="p.expedienteRecibido ? 'bg-emerald-50 text-emerald-700'
                       : p.expedienteSubido ? 'bg-amber-50 text-amber-700' : 'bg-[#FDF6F7] text-[#8C1D2E]'">
            {{ p.expedienteRecibido ? 'Recibido por Secretaría' : p.expedienteSubido ? 'Esperando recepción' : 'Pendiente de envío' }}
          </span>
        }
      </div>

      <div class="page-content p-6">
        <div class="mx-auto max-w-[1440px] space-y-3">
          @if (e(); as p) {

            @if (!p.cartaAsesor) {
              <!-- Sin la carta del asesor no hay nada que cerrar todavía. -->
              <div class="rounded-xl bg-amber-50 border border-amber-200 px-4 py-3 text-[12.5px] text-amber-700 flex items-center gap-2.5">
                <mat-icon svgIcon="lock" class="size-5 shrink-0" />
                <span class="flex-1">
                  Tu asesor aún no emite la <b>carta de opinión favorable</b>. Cuando lo haga podrás subir el
                  Turnitin y el proyecto en versión final.
                </span>
                <button mat-stroked-button class="!h-8 !text-[11.5px] shrink-0" (click)="irAlEditor()">
                  <mat-icon svgIcon="square-pen" class="size-3.5 mr-1" /> Volver al proyecto
                </button>
              </div>
            }

            <div class="grid gap-3 md:grid-cols-2 items-start">

              <!-- Lo que hay que adjuntar -->
              <section class="form-card">
                <header class="form-card__head">
                  <h2 class="form-card__title">Documentos del expediente</h2>
                  <span class="text-[11px] text-slate-400">PDF · máx. 10 MB</span>
                </header>

                <div class="space-y-1.5">
                  <!-- Turnitin -->
                  <div class="flex items-center gap-2.5 rounded-lg border border-slate-100 px-2.5 py-2">
                    <mat-icon [svgIcon]="p.turnitinSubido ? 'file-check' : 'file-text'" class="size-4 shrink-0"
                              [class]="p.turnitinSubido ? 'text-emerald-600' : p.cartaAsesor ? 'text-amber-500' : 'text-slate-300'" />
                    <div class="min-w-0 flex-1">
                      <p class="text-[12px] font-medium text-slate-700 truncate">Informe de similitud (Turnitin)</p>
                      <p class="text-[10.5px]" [class]="p.turnitinSubido ? 'text-emerald-600' : 'text-slate-400'">
                        {{ p.turnitinSubido ? 'Subido · similitud ' + (p.porcentajeSimilitud ?? 0) + '%'
                           : 'Índice máximo referencial: 20%' }}
                      </p>
                    </div>
                    @if (!p.turnitinSubido) {
                      <input type="number" min="0" max="100" [(ngModel)]="turnPct" [disabled]="!p.cartaAsesor"
                             class="w-20 rounded-lg border border-slate-200 px-2 py-1 text-[11.5px] disabled:bg-slate-50 disabled:text-slate-400"
                             placeholder="% simil." />
                    } @else {
                      <span class="text-[11px] px-2 py-0.5 rounded-full shrink-0"
                            [ngClass]="(p.porcentajeSimilitud ?? 0) <= 20 ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'">
                        {{ p.porcentajeSimilitud ?? 0 }}%
                      </span>
                    }
                    <div class="row-actions shrink-0">
                      @if (p.turnitinSubido) {
                        <button mat-icon-button class="!w-7 !h-7" title="Ver el informe enviado" (click)="verDoc('turnitin')">
                          <mat-icon svgIcon="file-search" class="text-slate-400 size-3.5" />
                        </button>
                      }
                      <button mat-icon-button class="!w-7 !h-7" [disabled]="!p.cartaAsesor"
                              [title]="p.turnitinSubido ? 'Reemplazar el informe' : 'Subir el informe de Turnitin'"
                              (click)="fileTurn.click()">
                        <mat-icon svgIcon="upload" class="size-3.5" [class]="p.turnitinSubido ? 'text-emerald-600' : 'text-[#8C1D2E]'" />
                      </button>
                    </div>
                    <input #fileTurn type="file" hidden accept=".pdf,.docx" (change)="onTurnitin($event)" />
                  </div>

                  <!-- Proyecto versión final -->
                  <div class="flex items-center gap-2.5 rounded-lg border border-slate-100 px-2.5 py-2">
                    <mat-icon [svgIcon]="p.proyectoFinalSubido ? 'file-check' : 'file-text'" class="size-4 shrink-0"
                              [class]="p.proyectoFinalSubido ? 'text-emerald-600' : p.cartaAsesor ? 'text-amber-500' : 'text-slate-300'" />
                    <div class="min-w-0 flex-1">
                      <p class="text-[12px] font-medium text-slate-700 truncate">Proyecto en versión final</p>
                      <p class="text-[10.5px]" [class]="p.proyectoFinalSubido ? 'text-emerald-600' : 'text-slate-400'">
                        {{ p.proyectoFinalSubido ? 'Subido' : 'El documento con las observaciones ya levantadas' }}
                      </p>
                    </div>
                    <div class="row-actions shrink-0">
                      @if (p.proyectoFinalSubido) {
                        <button mat-icon-button class="!w-7 !h-7" title="Ver el proyecto enviado" (click)="verDoc('proyecto-final')">
                          <mat-icon svgIcon="file-search" class="text-slate-400 size-3.5" />
                        </button>
                      }
                      <button mat-icon-button class="!w-7 !h-7" [disabled]="!p.cartaAsesor"
                              [title]="p.proyectoFinalSubido ? 'Reemplazar el proyecto final' : 'Subir el proyecto en versión final'"
                              (click)="fileFinal.click()">
                        <mat-icon svgIcon="upload" class="size-3.5" [class]="p.proyectoFinalSubido ? 'text-emerald-600' : 'text-[#8C1D2E]'" />
                      </button>
                    </div>
                    <input #fileFinal type="file" hidden accept=".pdf,.docx" (change)="onProyectoFinal($event)" />
                  </div>
                </div>
              </section>

              <!-- Requisitos + envío -->
              <section class="form-card">
                <header class="form-card__head">
                  <h2 class="form-card__title">Solicitud de aprobación</h2>
                  <span class="text-[11px] text-slate-400">{{ listos() }} de {{ requisitos().length }}</span>
                </header>

                <ul class="space-y-1.5 mb-3">
                  @for (r of requisitos(); track r.label) {
                    <li class="flex items-center gap-2 text-[12px]">
                      <mat-icon [svgIcon]="r.ok ? 'circle-check' : 'circle-dashed'" class="size-4 shrink-0"
                                [class]="r.ok ? 'text-emerald-600' : 'text-slate-300'" />
                      <span [class]="r.ok ? 'text-slate-600' : 'text-slate-400'">{{ r.label }}</span>
                    </li>
                  }
                </ul>

                <button mat-flat-button color="primary" class="!h-8 !text-[11.5px] w-full"
                        [disabled]="!puedeSolicitar()" (click)="solicitar()">
                  <mat-icon svgIcon="send" class="size-3.5 mr-1" />
                  {{ p.expedienteSubido ? 'Solicitud enviada' : 'Enviar solicitud de aprobación' }}
                </button>

                @if (p.expedienteSubido) {
                  <div class="mt-2.5 flex items-start gap-2 rounded-lg px-3 py-2 text-[12px]"
                       [ngClass]="p.expedienteRecibido ? 'bg-emerald-50 border border-emerald-200 text-emerald-700'
                                                       : 'bg-amber-50 border border-amber-200 text-amber-700'">
                    <mat-icon [svgIcon]="p.expedienteRecibido ? 'badge-check' : 'clock'" class="size-4 shrink-0 mt-px" />
                    @if (p.expedienteRecibido) {
                      <span>Secretaría recibió tu expediente. Pasas a la <b>Etapa 5 · Defensa del proyecto</b>: el Coordinador designará a tus revisores.</span>
                    } @else {
                      <span>Solicitud enviada. Esperando que Secretaría recepcione tu expediente.</span>
                    }
                  </div>
                } @else if (!puedeSolicitar()) {
                  <p class="text-[11px] text-slate-400 mt-1.5">Completa los requisitos de arriba para poder enviarla.</p>
                }
              </section>
            </div>
          } @else {
            <p class="text-sm text-slate-400">Cargando…</p>
          }
        </div>
      </div>
    </div>
  `,
})
export class CierreEnvioComponent implements OnInit {
  private _svc = inject(MiProyectoService);
  private _router = inject(Router);
  private _toast = inject(NotificationService);
  private _confirm = inject(ConfirmDialogService);
  private _dialog = inject(MatDialog);

  protected e = signal<any | null>(null);
  protected turnPct: number | null = null;

  /** Checklist del expediente, en el orden en que se cumple. */
  protected requisitos = computed(() => {
    const p = this.e();
    return [
      { label: 'Proyecto enviado a revisión', ok: !!p?.listoRevision },
      { label: 'Carta de opinión favorable del asesor', ok: !!p?.cartaAsesor },
      { label: 'Informe de Turnitin', ok: !!p?.turnitinSubido },
      { label: 'Proyecto en versión final', ok: !!p?.proyectoFinalSubido },
      { label: 'Solicitud de aprobación enviada', ok: !!p?.expedienteSubido },
    ];
  });

  protected listos = computed(() => this.requisitos().filter((r) => r.ok).length);

  protected puedeSolicitar = computed(() => {
    const p = this.e();
    return !!p?.cartaAsesor && !!p?.turnitinSubido && !!p?.proyectoFinalSubido && !p?.expedienteSubido;
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this._svc.editor$().subscribe({
      next: (res: any) => this.e.set(res?.data ?? res ?? null),
      error: () => this._toast.error('No se pudo cargar tu proyecto'),
    });
  }

  irAlEditor(): void {
    this._router.navigate(['/admin/mi-tesis/proyecto']);
  }

  onTurnitin(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this._svc.subirTurnitin$(file, this.turnPct ?? 0).subscribe({
      next: () => { this._toast.success('Informe de Turnitin subido'); this.cargar(); },
      error: (err) => this._toast.error(err?.error?.message ?? 'No se pudo subir el informe'),
    });
  }

  onProyectoFinal(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this._svc.subirProyectoFinal$(file).subscribe({
      next: () => { this._toast.success('Proyecto versión final subido'); this.cargar(); },
      error: (err) => this._toast.error(err?.error?.message ?? 'No se pudo subir el proyecto'),
    });
  }

  /** Vista previa en modal: sirve para comprobar que el archivo subido es el correcto. */
  verDoc(tipo: 'turnitin' | 'proyecto-final'): void {
    this._svc.descargarDocumento$(tipo).subscribe({
      next: (blob) => previsualizarBlob(this._dialog, blob,
        tipo === 'turnitin' ? 'Informe de Turnitin' : 'Proyecto en versión final'),
      error: () => this._toast.error('No se pudo abrir el documento'),
    });
  }

  solicitar(): void {
    this._confirm.confirmSave({
      title: 'Enviar solicitud de aprobación',
      message: 'Se enviará tu expediente a la UPG para la aprobación del proyecto. ¿Continuar?',
    }).then(() => {
      this._svc.solicitarAprobacion$().subscribe({
        next: () => { this._toast.success('Solicitud de aprobación enviada'); this.cargar(); },
        error: (err) => this._toast.error(err?.error?.message ?? 'No se pudo enviar la solicitud'),
      });
    }).catch(() => {});
  }
}
