import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { DocumentoPreviewDialogComponent } from '@/app/shared/perfil-completo/documento-preview-dialog.component';
import { CierreProyectoService } from '../services/cierre-proyecto.service';

/**
 * Secretaría · cierre de la Etapa 5 en tres pasos: resultado de la defensa (con las rúbricas
 * de los revisores), dictamen de aprobación y archivo del proyecto final. Cada paso se habilita
 * cuando el anterior está completo, de modo que el orden del trámite se respeta solo.
 */
@Component({
  selector: 'app-cierre-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 5 · Cierre del proyecto</p>
          <h1 class="page-title">{{ d()?.estudianteNombre || 'Cierre del proyecto' }}</h1>
        </div>
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="volver()">
          <mat-icon svgIcon="arrow-left" class="size-4 mr-1" /> Volver
        </button>
      </div>

      <div class="page-content p-6">
        @if (cargando()) {
          <p class="text-sm text-slate-400 py-10 text-center">Cargando…</p>
        } @else if (d(); as x) {

          <!-- Franja del expediente -->
          <div class="franja">
            <div class="min-w-0">
              <p class="text-[11px] uppercase tracking-wide text-white/60">Proyecto</p>
              <p class="text-[13px] text-white truncate">{{ x.tituloTesis }}</p>
            </div>
            <div class="dato">
              <span>Defensa</span>
              <b>{{ x.fechaDefensa | date:'dd/MM/yyyy' }}@if (x.horaDefensa) { · {{ x.horaDefensa }} }</b>
            </div>
            <div class="dato">
              <span>Modalidad</span>
              <b>{{ x.modalidadLabel || '—' }}</b>
            </div>
            <div class="dato">
              <span>{{ x.enlaceDefensa ? 'Enlace' : 'Ambiente' }}</span>
              <b class="truncate max-w-[210px]">{{ x.enlaceDefensa || x.lugarDefensa || '—' }}</b>
            </div>
          </div>

          <!-- Pasos -->
          <div class="pasos">
            <div [class]="clasePaso(1)"><i>1</i> Resultado de la defensa</div>
            <div [class]="clasePaso(2)"><i>2</i> Dictamen de aprobación</div>
            <div [class]="clasePaso(3)"><i>3</i> Archivo del expediente</div>
          </div>

          @if (x.cerrado) {
            <div class="aviso ok">
              <mat-icon svgIcon="badge-check" class="size-4 shrink-0" />
              <span>Proyecto aprobado y archivado el {{ x.fechaCierre | date:'dd/MM/yyyy' }}. El doctorando ya puede iniciar la ejecución de la tesis.</span>
            </div>
          } @else if (x.resultado === 'DESAPROBADO') {
            <div class="aviso malo">
              <mat-icon svgIcon="circle-alert" class="size-4 shrink-0" />
              <span>Proyecto desaprobado el {{ x.fechaResultado | date:'dd/MM/yyyy' }}. Volvió a los revisores para reevaluar el proyecto corregido; podrás registrar el resultado de la nueva defensa cuando se reprograme.</span>
            </div>
          }

          <!-- ── Paso 1 · rúbricas y resultado ── -->
          <section class="tarjeta">
            <header>
              <h2>Rúbricas de la defensa</h2>
              <p>Recepciona la rúbrica firmada de cada revisor con la nota que consignó.</p>
            </header>

            <table class="w-full text-sm">
              <thead><tr class="text-slate-400 text-left">
                <th class="py-2 px-3 font-medium">Revisor</th>
                <th class="py-2 px-3 font-medium w-[110px]">Nota</th>
                <th class="py-2 px-3 font-medium">Rúbrica firmada</th>
                <th class="w-[110px]"></th>
              </tr></thead>
              <tbody>
                @for (r of x.rubricas; track r.docenteId) {
                  <tr class="border-t border-slate-100">
                    <td class="py-2 px-3 text-slate-700">{{ r.docenteNombre }}</td>
                    <td class="py-2 px-3">
                      @if (r.recibida) {
                        <span class="text-slate-700 font-semibold">{{ r.puntaje ?? '—' }}<span class="text-slate-400 font-normal"> / 100</span></span>
                      } @else {
                        <input class="inp !py-1 !text-[13px]" type="number" min="0" max="100"
                               [ngModel]="notas()[r.docenteId]" (ngModelChange)="setNota(r.docenteId, $event)" placeholder="0-100" />
                      }
                    </td>
                    <td class="py-2 px-3">
                      @if (r.recibida) {
                        <span [class]="chip('ok')">Recibida</span>
                        <span class="text-[11px] text-slate-400 ml-2">{{ r.nombreArchivo }} · {{ r.fechaCarga | date:'dd/MM/yyyy' }}</span>
                      } @else {
                        <span [class]="chip('pend')">Por recibir</span>
                      }
                    </td>
                    <td class="py-2 px-3 text-right">
                      <div class="row-actions">
                        @if (r.recibida) {
                          <button mat-icon-button class="!w-7 !h-7" title="Ver la rúbrica recepcionada" (click)="verRubrica(r)">
                            <mat-icon svgIcon="eye" class="size-4 text-slate-400" />
                          </button>
                        }
                        @if (!x.cerrado) {
                          <button mat-icon-button class="!w-7 !h-7" [title]="r.recibida ? 'Reemplazar la rúbrica' : 'Subir la rúbrica firmada'"
                                  [disabled]="subiendo()" (click)="pedirArchivo(r.docenteId)">
                            <mat-icon svgIcon="upload" class="size-4 text-[#8C1D2E]" />
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>

            <div class="linea"></div>

            <header>
              <h2>Resultado del acto</h2>
              <p>Lo que acordaron los revisores al término de la defensa.</p>
            </header>

            @if (x.defensaRealizada) {
              <div class="resumen">
                <span [class]="chip(x.resultado === 'DESAPROBADO' ? 'malo' : 'ok')">{{ x.resultadoLabel }}</span>
                <span class="text-[12px] text-slate-500">registrado el {{ x.fechaResultado | date:'dd/MM/yyyy' }}</span>
                @if (x.observacionDefensa) {
                  <p class="text-[12.5px] text-slate-600 basis-full mt-1">{{ x.observacionDefensa }}</p>
                }
              </div>
            } @else {
              <div class="space-y-3">
                <div class="seg">
                  @for (r of resultados; track r.valor) {
                    <button type="button" [class.on]="resultado() === r.valor" (click)="resultado.set(r.valor)">{{ r.label }}</button>
                  }
                </div>
                <div class="grid grid-cols-[180px_1fr] gap-3">
                  <div>
                    <label class="lbl">Fecha del acto</label>
                    <input type="date" class="inp" [ngModel]="fechaActo()" (ngModelChange)="fechaActo.set($event)" />
                  </div>
                  <div>
                    <label class="lbl">Acuerdos del acto @if (resultado() !== 'APROBADO') { <span class="text-[#8C1D2E]">·  obligatorio</span> }</label>
                    <input class="inp" [ngModel]="observacion()" (ngModelChange)="observacion.set($event)"
                           placeholder="Observaciones a subsanar o motivo de la desaprobación" />
                  </div>
                </div>
                @if (rubricasPendientes()) {
                  <p class="text-[12px] text-amber-600">Aún faltan rúbricas por recepcionar; puedes registrar el resultado, pero el dictamen las exige todas.</p>
                }
                <div class="text-right">
                  <button mat-flat-button class="btn-granate" [disabled]="!resultadoValido() || guardando()" (click)="registrarResultado()">
                    Registrar resultado
                  </button>
                </div>
              </div>
            }
          </section>

          <!-- ── Paso 2 · dictamen ── -->
          <section class="tarjeta" [class.apagada]="!paso2Habilitado()">
            <header>
              <h2>Dictamen de aprobación</h2>
              <p>
                @if (!paso2Habilitado()) {
                  Se habilita al registrar un resultado favorable con todas las rúbricas recepcionadas.
                } @else {
                  Numeración de la Unidad de Posgrado; la vigencia es de 4 años desde la defensa.
                }
              </p>
            </header>

            @if (paso2Habilitado()) {
              <div class="grid grid-cols-3 gap-3">
                <div>
                  <label class="lbl">N° de dictamen</label>
                  <input class="inp" [ngModel]="numero()" (ngModelChange)="numero.set($event)" placeholder="001985-2026-UPG-VDIP-FM/UNMSM" />
                </div>
                <div>
                  <label class="lbl">N° de expediente</label>
                  <input class="inp" [ngModel]="expediente()" (ngModelChange)="expediente.set($event)" placeholder="EXP-2026-001985" />
                </div>
                <div>
                  <label class="lbl">Vigencia hasta</label>
                  <input type="date" class="inp" [ngModel]="vigencia()" (ngModelChange)="vigencia.set($event)" />
                </div>
              </div>

              <div class="flex items-center gap-3 mt-3">
                <button mat-flat-button class="btn-granate" [disabled]="guardando()" (click)="elaborar()">
                  {{ x.dictamenElaborado ? 'Actualizar dictamen' : 'Elaborar dictamen' }}
                </button>

                @if (x.dictamenElaborado) {
                  <span class="text-[12px] text-slate-500">Descargar en</span>
                  <div class="seg !w-[150px]">
                    <button type="button" [class.on]="formato() === 'docx'" (click)="formato.set('docx')">Word</button>
                    <button type="button" [class.on]="formato() === 'pdf'" (click)="formato.set('pdf')">PDF</button>
                  </div>
                  <button mat-stroked-button class="!h-9 !text-[13px] !rounded-lg !border-slate-200 !text-slate-600" (click)="verDictamen()">
                    <mat-icon svgIcon="eye" class="size-4 mr-1" /> Vista previa
                  </button>
                  <button mat-stroked-button class="!h-9 !text-[13px] !rounded-lg !border-slate-200 !text-slate-600" (click)="descargarDictamen()">
                    <mat-icon svgIcon="download" class="size-4 mr-1" /> Descargar
                  </button>
                }
              </div>

              @if (x.dictamenElaborado) {
                <div class="linea"></div>
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <p class="text-[13px] font-semibold text-slate-700">Dictamen firmado por el Director</p>
                    @if (x.dictamenFirmadoSubido) {
                      <p class="text-[12px] text-emerald-600">Registrado el {{ x.dictamenFechaEmision | date:'dd/MM/yyyy' }} · vigencia hasta {{ x.dictamenVigenciaHasta | date:'dd/MM/yyyy' }}</p>
                    } @else {
                      <p class="text-[12px] text-slate-500">Sube el PDF o Word firmado para dar por emitido el dictamen.</p>
                    }
                  </div>
                  @if (!x.cerrado) {
                    <button mat-stroked-button class="!h-9 !text-[13px] !rounded-lg !border-[#8C1D2E]/30 !text-[#8C1D2E]"
                            [disabled]="subiendo()" (click)="pedirArchivo('DICTAMEN')">
                      <mat-icon svgIcon="upload" class="size-4 mr-1" /> {{ x.dictamenFirmadoSubido ? 'Reemplazar' : 'Subir firmado' }}
                    </button>
                  }
                </div>
              }
            }
          </section>

          <!-- ── Paso 3 · archivo ── -->
          <section class="tarjeta" [class.apagada]="!paso3Habilitado()">
            <header>
              <h2>Archivo del expediente</h2>
              <p>
                @if (!paso3Habilitado()) {
                  Se habilita cuando el dictamen firmado está registrado.
                } @else {
                  Archiva el proyecto final aprobado. Con esto la Etapa 5 queda cerrada y el doctorando pasa a la ejecución.
                }
              </p>
            </header>

            @if (paso3Habilitado()) {
              @if (x.proyectoFinalArchivado) {
                <div class="aviso ok">
                  <mat-icon svgIcon="archive" class="size-4 shrink-0" />
                  <span>Proyecto archivado: {{ x.proyectoFinalNombre }}</span>
                </div>
              } @else {
                <div class="flex items-center gap-3">
                  @if (x.proyectoFinalDelEstudiante) {
                    <button mat-flat-button class="btn-granate" [disabled]="guardando()" (click)="archivarDelEstudiante()">
                      Archivar el que subió el doctorando
                    </button>
                    <span class="text-[12px] text-slate-400">o</span>
                  }
                  <button mat-stroked-button class="!h-9 !text-[13px] !rounded-lg !border-slate-200 !text-slate-600"
                          [disabled]="subiendo()" (click)="pedirArchivo('PROYECTO')">
                    <mat-icon svgIcon="upload" class="size-4 mr-1" /> Subir otro archivo
                  </button>
                </div>
              }
            }
          </section>
        }
      </div>
    </div>

    <input #picker type="file" class="hidden" accept=".pdf,.docx,.doc" (change)="archivoElegido($event)" />
  `,
  styles: [`
    .franja { display:flex; align-items:center; gap:26px; background:#8C1D2E; border-radius:12px; padding:12px 18px; margin-bottom:16px; }
    .franja .dato { display:flex; flex-direction:column; }
    .franja .dato span { font-size:10.5px; text-transform:uppercase; letter-spacing:.04em; color:rgba(255,255,255,.6); }
    .franja .dato b { font-size:13px; color:#fff; font-weight:600; }

    .pasos { display:flex; gap:10px; margin-bottom:16px; }
    .pasos > div { flex:1; display:flex; align-items:center; gap:8px; font-size:12.5px; padding:8px 12px; border-radius:10px;
                   background:#f3f4f6; color:#9ca3af; border:1px solid transparent; }
    .pasos > div i { width:20px; height:20px; border-radius:6px; background:#e5e7eb; color:#6b7280; display:grid; place-items:center;
                     font-style:normal; font-size:11px; font-weight:700; }
    .pasos > div.actual { background:#FDF2F4; color:#8C1D2E; border-color:rgba(140,29,46,.2); font-weight:600; }
    .pasos > div.actual i { background:#8C1D2E; color:#fff; }
    .pasos > div.hecho { background:#ECFDF5; color:#047857; }
    .pasos > div.hecho i { background:#047857; color:#fff; }

    .tarjeta { background:#fff; border:1px solid #e9edf2; border-radius:12px; padding:18px 20px; margin-bottom:16px; }
    .tarjeta.apagada { opacity:.55; }
    .tarjeta header { margin-bottom:12px; }
    .tarjeta header h2 { font-size:14.5px; font-weight:700; color:#334155; }
    .tarjeta header p { font-size:12px; color:#64748b; margin-top:2px; }
    .linea { height:1px; background:#eef2f7; margin:18px 0; }
    .resumen { display:flex; flex-wrap:wrap; align-items:center; gap:10px; }

    .lbl { display:block; font-size:11.5px; font-weight:600; color:#64748b; margin-bottom:4px; }
    .inp { width:100%; border:1px solid #e2e8f0; border-radius:8px; padding:8px 12px; font-size:13.5px; outline:none; background:#fff; }
    .inp:focus { border-color:#8C1D2E; }

    .seg { display:flex; border:1px solid #e2e8f0; border-radius:8px; overflow:hidden; }
    .seg button { flex:1; padding:7px 10px; font-size:12.5px; color:#475569; background:#fff; border-right:1px solid #eef2f7; white-space:nowrap; }
    .seg button:last-child { border-right:0; }
    .seg button.on { background:#8C1D2E; color:#fff; font-weight:600; }

    .aviso { display:flex; align-items:center; gap:9px; border-radius:10px; padding:10px 14px; font-size:12.5px; margin-bottom:16px; }
    .aviso.ok { background:#ECFDF5; color:#047857; border:1px solid #A7F3D0; }
    .aviso.malo { background:#FEF2F2; color:#B91C1C; border:1px solid #FECACA; }

    .btn-granate { height:36px; font-size:13px; border-radius:8px; padding:0 16px; font-weight:500;
                   background:#8C1D2E !important; color:#fff !important; }
    .btn-granate[disabled] { opacity:.5; }
  `],
})
export class CierreDetalleComponent implements OnInit {
  private _svc = inject(CierreProyectoService);
  private _toast = inject(NotificationService);
  private _route = inject(ActivatedRoute);
  private _router = inject(Router);
  private _dialog = inject(MatDialog);
  private _confirm = inject(ConfirmDialogService);

  protected d = signal<any | null>(null);
  protected cargando = signal(true);
  protected guardando = signal(false);
  protected subiendo = signal(false);

  /** Notas escritas por revisor mientras aún no se suben (docenteId → nota). */
  protected notas = signal<Record<string, number | null>>({});

  protected resultado = signal('APROBADO');
  protected fechaActo = signal('');
  protected observacion = signal('');

  protected numero = signal('');
  protected expediente = signal('');
  protected vigencia = signal('');
  protected formato = signal<'pdf' | 'docx'>('docx');

  protected readonly resultados = [
    { valor: 'APROBADO', label: 'Aprobado' },
    { valor: 'APROBADO_CON_OBSERVACIONES', label: 'Aprobado con observaciones' },
    { valor: 'DESAPROBADO', label: 'Desaprobado' },
  ];

  /** Destino del selector de archivo abierto: id del revisor, 'DICTAMEN' o 'PROYECTO'. */
  private destino: string | null = null;
  private tesisId = '';

  @ViewChild('picker') private picker?: ElementRef<HTMLInputElement>;

  protected rubricasPendientes = computed(() =>
    (this.d()?.rubricas ?? []).some((r: any) => !r.recibida));

  protected resultadoValido = computed(() =>
    this.resultado() === 'APROBADO' || this.observacion().trim().length > 0);

  protected paso2Habilitado = computed(() => {
    const x = this.d();
    return !!x && x.defensaRealizada && x.resultado !== 'DESAPROBADO' && !this.rubricasPendientes();
  });

  protected paso3Habilitado = computed(() => {
    const x = this.d();
    return !!x && x.dictamenFirmadoSubido;
  });

  ngOnInit(): void {
    this.tesisId = this._route.snapshot.paramMap.get('tesisId') ?? '';
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this._svc.estado$(this.tesisId).subscribe({
      next: (res) => {
        const x = res?.data ?? res;
        this.d.set(x);
        this.numero.set(x?.dictamenNumero ?? '');
        this.expediente.set(x?.dictamenExpediente ?? x?.expedienteSugerido ?? '');
        this.vigencia.set(this.iso(x?.dictamenVigenciaHasta));
        this.fechaActo.set(this.iso(x?.fechaResultado) || this.iso(x?.fechaDefensa));
        this.cargando.set(false);
      },
      error: () => { this.cargando.set(false); this._toast.error('No se pudo cargar el expediente'); },
    });
  }

  // ── Paso 1 ──

  protected setNota(docenteId: string, valor: any): void {
    this.notas.update((n) => ({ ...n, [docenteId]: valor === '' || valor == null ? null : Number(valor) }));
  }

  protected pedirArchivo(destino: string): void {
    // Subir el proyecto final también cierra la etapa: se confirma antes de elegir el archivo.
    if (destino === 'PROYECTO') {
      this.confirmarCierre()
        .then(() => { this.destino = destino; this.picker?.nativeElement.click(); })
        .catch(() => {});
      return;
    }
    this.destino = destino;
    this.picker?.nativeElement.click();
  }

  protected archivoElegido(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || !this.destino) return;

    const destino = this.destino;
    this.destino = null;
    this.subiendo.set(true);

    const listo = (msg: string) => { this.subiendo.set(false); this._toast.success(msg); this.cargar(); };
    const falla = (e: any) => { this.subiendo.set(false); this._toast.error(e?.error?.message ?? 'No se pudo subir el archivo'); };

    if (destino === 'DICTAMEN') {
      this._svc.subirDictamenFirmado$(this.tesisId, file).subscribe({ next: () => listo('Dictamen firmado registrado'), error: falla });
    } else if (destino === 'PROYECTO') {
      this._svc.archivarProyecto$(this.tesisId, file).subscribe({ next: () => listo('Proyecto archivado'), error: falla });
    } else {
      const nota = this.notas()[destino];
      this._svc.recepcionarRubrica$(this.tesisId, destino, file, nota).subscribe({ next: () => listo('Rúbrica recepcionada'), error: falla });
    }
  }

  protected verRubrica(r: any): void {
    this._svc.rubricaRaw$(this.tesisId, r.docenteId).subscribe({
      next: (blob) => this.abrirPreview(blob, r.nombreArchivo ?? 'rubrica-defensa'),
      error: () => this._toast.error('No se pudo abrir la rúbrica'),
    });
  }

  protected registrarResultado(): void {
    if (!this.resultadoValido() || this.guardando()) return;
    const favorable = this.resultado() !== 'DESAPROBADO';
    const etiqueta = this.resultados.find((r) => r.valor === this.resultado())?.label ?? '';
    this._confirm.confirmSave({
      title: 'Registrar el resultado de la defensa',
      message: `Se registrará el acto como «${etiqueta}». Al confirmar:`,
      details: favorable
        ? ['queda habilitado el dictamen de aprobación',
           'el doctorando recibe el aviso del resultado',
           'el resultado no se puede cambiar después']
        : ['no corresponde emitir dictamen de aprobación',
           'el doctorando recibe el aviso del resultado',
           'el resultado no se puede cambiar después'],
      confirmLabel: 'Registrar resultado',
    }).then(() => {
      this.guardando.set(true);
      this._svc.registrarResultado$(this.tesisId, {
        resultado: this.resultado(),
        fecha: this.fechaActo() || null,
        observacion: this.observacion().trim() || null,
      }).subscribe({
        next: () => { this.guardando.set(false); this._toast.success('Resultado registrado'); this.cargar(); },
        error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo registrar'); },
      });
    }).catch(() => {});
  }

  // ── Paso 2 ──

  protected elaborar(): void {
    if (this.guardando()) return;
    this.guardando.set(true);
    this._svc.elaborarDictamen$(this.tesisId, {
      numero: this.numero().trim() || null,
      expediente: this.expediente().trim() || null,
      vigenciaHasta: this.vigencia() || null,
    }).subscribe({
      next: () => { this.guardando.set(false); this._toast.success('Dictamen elaborado'); this.cargar(); },
      error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo elaborar'); },
    });
  }

  protected verDictamen(): void {
    this._svc.documentoDictamen$(this.tesisId, this.formato()).subscribe({
      next: (blob) => this.abrirPreview(blob, `dictamen-aprobacion.${this.formato()}`),
      error: () => this._toast.error('No se pudo generar el documento'),
    });
  }

  protected descargarDictamen(): void {
    this._svc.documentoDictamen$(this.tesisId, this.formato()).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `dictamen-aprobacion.${this.formato()}`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this._toast.error('No se pudo generar el documento'),
    });
  }

  // ── Paso 3 ──

  protected archivarDelEstudiante(): void {
    if (this.guardando()) return;
    this.confirmarCierre().then(() => {
      this.guardando.set(true);
      this._svc.archivarDelEstudiante$(this.tesisId).subscribe({
        next: () => { this.guardando.set(false); this._toast.success('Proyecto archivado'); this.cargar(); },
        error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo archivar'); },
      });
    }).catch(() => {});
  }

  /**
   * Archivar cierra la Etapa 5 y no tiene vuelta atrás: es el único paso del sistema con
   * casilla de confirmación además del aviso.
   */
  private confirmarCierre(): Promise<void> {
    const x = this.d();
    const vigencia = x?.dictamenVigenciaHasta
      ? new Date(x.dictamenVigenciaHasta).toLocaleDateString('es-PE')
      : null;
    return this._confirm.confirmSave({
      title: `Cerrar el proyecto de ${x?.estudianteNombre ?? 'el doctorando'}`,
      message: 'Se archivará el proyecto final y la Etapa 5 quedará cerrada. Al confirmar:',
      details: [
        'el doctorando pasa a la ejecución de la tesis',
        ...(vigencia ? [`el dictamen queda vigente hasta el ${vigencia}`] : []),
        'este paso no se puede deshacer',
      ],
      acknowledge: 'Entiendo que el cierre es definitivo',
      confirmLabel: 'Cerrar el proyecto',
    });
  }

  // ── Utilidades ──

  protected volver(): void {
    this._router.navigate(['/admin/cierre-proyecto']);
  }

  protected clasePaso(n: number): string {
    const x = this.d();
    if (!x) return '';
    const hecho = n === 1 ? x.defensaRealizada : n === 2 ? x.dictamenFirmadoSubido : x.proyectoFinalArchivado;
    if (hecho) return 'hecho';
    const habilitado = n === 1 || (n === 2 && this.paso2Habilitado()) || (n === 3 && this.paso3Habilitado());
    return habilitado ? 'actual' : '';
  }

  protected chip(tipo: 'ok' | 'curso' | 'pend' | 'malo'): string {
    const base = 'inline-block px-2 py-0.5 rounded text-[11px] font-bold whitespace-nowrap ';
    switch (tipo) {
      case 'ok': return base + 'bg-emerald-100 text-emerald-700';
      case 'curso': return base + 'bg-amber-100 text-amber-700';
      case 'malo': return base + 'bg-rose-100 text-rose-600';
      default: return base + 'bg-[#8C1D2E]/10 text-[#8C1D2E]';
    }
  }

  private abrirPreview(blob: Blob, nombre: string): void {
    const url = URL.createObjectURL(blob);
    this._dialog.open(DocumentoPreviewDialogComponent, {
      data: { nombre, url, esPdf: blob.type.includes('pdf'), mime: blob.type, blob },
      panelClass: 'documento-preview-panel',
      maxWidth: '96vw',
      maxHeight: '92vh',
    });
  }

  private iso(fecha: any): string {
    return fecha ? String(fecha).substring(0, 10) : '';
  }
}
