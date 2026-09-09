import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { NotificationService } from '@/app/shared/notification/notification.service';
import { DocumentoPreviewDialogComponent } from '@/app/shared/perfil-completo/documento-preview-dialog.component';
import { SustentacionService } from '../services/sustentacion.service';

/**
 * Secretaría · Etapa 8, la última: recepción, dictamen de designación del Jurado, programación
 * del acto y Acta de sustentación. Al archivar el Acta, la tesis queda concluida.
 */
@Component({
  selector: 'app-sustentacion-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <p class="breadcrumb">Proceso de Tesis · Etapa 8 · Sustentación</p>
          <h1 class="page-title">{{ d()?.estudianteNombre || 'Sustentación de tesis' }}</h1>
        </div>
        <button mat-stroked-button class="!h-9 !text-sm !rounded-lg !border-slate-200 !text-slate-600" (click)="volver()">
          <mat-icon svgIcon="arrow-left" class="size-4 mr-1" /> Volver
        </button>
      </div>

      <div class="page-content p-6">
        @if (cargando()) {
          <p class="text-sm text-slate-400 py-10 text-center">Cargando…</p>
        } @else if (d(); as x) {

          <div class="franja">
            <div class="min-w-0">
              <p class="text-[11px] uppercase tracking-wide text-white/60">Proyecto</p>
              <p class="text-[13px] text-white truncate">{{ x.tituloTesis }}</p>
            </div>
            <div class="dato"><span>Jurado</span><b>{{ x.jurado?.length || 0 }} / 3</b></div>
            <div class="dato"><span>Sustentación</span><b>{{ x.sustentacionProgramada ? (x.fechaSustentacion | date:'dd/MM/yyyy') : 'Sin programar' }}</b></div>
            <div class="dato"><span>Estado</span><b>{{ x.pendiente }}</b></div>
          </div>

          @if (x.concluida) {
            <div class="aviso ok">
              <mat-icon svgIcon="badge-check" class="size-4 shrink-0" />
              <span>Tesis sustentada y concluida el {{ x.fechaConclusion | date:'dd/MM/yyyy' }} — {{ x.resultadoLabel }}.</span>
            </div>
          } @else if (x.actaSubida && x.resultado === 'DESAPROBADO') {
            <div class="aviso desaprobado">
              <mat-icon svgIcon="circle-alert" class="size-4 shrink-0" />
              <span>Sustentación desaprobada el {{ x.fechaActa | date:'dd/MM/yyyy' }}@if (x.observacionActa) { — {{ x.observacionActa }} }. Coordina un nuevo acto en el paso 3.</span>
            </div>
          }

          <div class="pasos">
            <div [class]="clasePaso(1)"><i>1</i> Recepción</div>
            <div [class]="clasePaso(2)"><i>2</i> Dictamen del Jurado</div>
            <div [class]="clasePaso(3)"><i>3</i> Programar el acto</div>
            <div [class]="clasePaso(4)"><i>4</i> Acta y cierre</div>
          </div>

          <!-- Paso 1 · Recepción -->
          <section class="tarjeta">
            <header>
              <h2>Recepción del expediente</h2>
              <p>El estudiante solicitó su Jurado de Sustentación. Recepciónalo para que el Coordinador pueda designarlo.</p>
            </header>
            @if (x.expedienteRecibido) {
              <div class="aviso ok"><mat-icon svgIcon="badge-check" class="size-4 shrink-0" /> Recepcionado el {{ x.fechaRecepcion | date:'dd/MM/yyyy' }}.</div>
            } @else {
              <button mat-flat-button class="btn-granate" [disabled]="guardando()" (click)="recepcionar()">Recepcionar expediente</button>
            }
          </section>

          <!-- Paso 2 · Dictamen del Jurado -->
          <section class="tarjeta" [class.apagada]="!paso2Habilitado()">
            <header>
              <h2>Dictamen de designación del Jurado</h2>
              <p>
                @if (!paso2Habilitado()) {
                  Se habilita cuando el Coordinador designe a los 3 miembros del Jurado de Sustentación.
                } @else {
                  Numeración de la Unidad de Posgrado.
                }
              </p>
            </header>
            @if (paso2Habilitado()) {
              @if (x.jurado?.length) {
                <ul class="space-y-1 mb-3">
                  @for (j of x.jurado; track j.docenteId) {
                    <li class="text-[13px] text-slate-600">{{ j.docenteNombre }} @if (j.rol === 'PRESIDENTE') { <b class="text-[#8C1D2E]">· Presidente</b> }</li>
                  }
                </ul>
              }
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="lbl">N° de dictamen</label>
                  <input class="inp" [ngModel]="numero()" (ngModelChange)="numero.set($event)" placeholder="001985-2026-UPG-VDIP-FM/UNMSM" />
                </div>
                <div>
                  <label class="lbl">N° de expediente</label>
                  <input class="inp" [ngModel]="expediente()" (ngModelChange)="expediente.set($event)" placeholder="EXP-2026-001985" />
                </div>
              </div>
              <div class="flex items-center gap-3 mt-3">
                <button mat-flat-button class="btn-granate" [disabled]="guardando()" (click)="elaborar()">
                  {{ x.dictamenElaborado ? 'Actualizar dictamen' : 'Elaborar dictamen' }}
                </button>
                @if (x.dictamenElaborado) {
                  <button mat-stroked-button class="!h-9 !text-[13px] !rounded-lg !border-slate-200 !text-slate-600" (click)="verDictamen()">
                    <mat-icon svgIcon="eye" class="size-4 mr-1" /> Vista previa
                  </button>
                }
              </div>
              @if (x.dictamenElaborado) {
                <div class="linea"></div>
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <p class="text-[13px] font-semibold text-slate-700">Dictamen firmado</p>
                    @if (x.dictamenFirmadoSubido) {
                      <p class="text-[12px] text-emerald-600">Registrado el {{ x.dictamenFechaEmision | date:'dd/MM/yyyy' }}</p>
                    } @else {
                      <p class="text-[12px] text-slate-500">Sube el PDF o Word firmado.</p>
                    }
                  </div>
                  <button mat-stroked-button class="!h-9 !text-[13px] !rounded-lg !border-[#8C1D2E]/30 !text-[#8C1D2E]"
                          [disabled]="subiendo()" (click)="pedirArchivo('DICTAMEN')">
                    <mat-icon svgIcon="upload" class="size-4 mr-1" /> {{ x.dictamenFirmadoSubido ? 'Reemplazar' : 'Subir firmado' }}
                  </button>
                </div>
              }
            }
          </section>

          <!-- Paso 3 · Programar el acto -->
          <section class="tarjeta" [class.apagada]="!paso3Habilitado()">
            <header>
              <h2>Programar el acto de sustentación</h2>
              <p>
                @if (!paso3Habilitado()) {
                  Se habilita cuando el dictamen del Jurado esté firmado.
                } @else if (x.sustentacionProgramada) {
                  Ya coordinada.
                } @else if (x.actaSubida && x.resultado === 'DESAPROBADO') {
                  El acto anterior fue desaprobado. Coordina modalidad, lugar (o enlace) y fecha del nuevo acto.
                } @else {
                  Coordina modalidad, lugar (o enlace) y fecha del acto.
                }
              </p>
            </header>
            @if (paso3Habilitado()) {
              @if (x.sustentacionProgramada) {
                <p class="text-[13px] text-slate-700">
                  {{ x.fechaSustentacion | date:'EEEE d \\'de\\' MMMM \\'de\\' y' }}@if (x.horaSustentacion) { · {{ x.horaSustentacion }} }
                  · {{ x.modalidadLabel }}
                  @if (x.lugarSustentacion) { · {{ x.lugarSustentacion }} }
                </p>
                @if (x.enlaceSustentacion) { <p class="text-[11.5px] text-slate-500 mt-0.5">Enlace: {{ x.enlaceSustentacion }}</p> }
              } @else {
                <div>
                  <label class="lbl">Modalidad</label>
                  <div class="seg">
                    @for (m of modalidades; track m.valor) {
                      <button type="button" [class.on]="modalidad() === m.valor" (click)="modalidad.set(m.valor)">{{ m.label }}</button>
                    }
                  </div>
                </div>
                <div class="grid grid-cols-[1fr_120px] gap-3 mt-3">
                  <div><label class="lbl">Fecha</label><input type="date" class="inp" [ngModel]="fecha()" (ngModelChange)="fecha.set($event)" /></div>
                  <div><label class="lbl">Hora</label><input type="time" class="inp" [ngModel]="hora()" (ngModelChange)="hora.set($event)" /></div>
                </div>
                @if (requiereLugar()) {
                  <div class="mt-3"><label class="lbl">Aula o ambiente</label><input class="inp" [ngModel]="lugar()" (ngModelChange)="lugar.set($event)" placeholder="Ej.: Auditorio de Posgrado" /></div>
                }
                @if (requiereEnlace()) {
                  <div class="mt-3"><label class="lbl">Enlace de la sesión</label><input class="inp" [ngModel]="enlace()" (ngModelChange)="enlace.set($event)" placeholder="Ej.: https://meet.unmsm.edu.pe/sustentacion-…" /></div>
                }
                <button mat-flat-button class="btn-granate mt-3" [disabled]="!programarCompleto() || guardando()" (click)="programar()">Programar sustentación</button>
              }
            }
          </section>

          <!-- Paso 4 · Acta y cierre -->
          <section class="tarjeta" [class.apagada]="!paso4Habilitado()">
            <header>
              <h2>Acta de sustentación</h2>
              <p>
                @if (!paso4Habilitado()) {
                  Se habilita cuando la sustentación esté programada.
                } @else if (x.concluida) {
                  La tesis ya quedó concluida.
                } @else {
                  Registra el resultado del acto y sube el Acta firmada. Con esto se concluye la tesis.
                }
              </p>
            </header>
            @if (paso4Habilitado() && !x.concluida) {
              <div class="grid grid-cols-[220px_1fr] gap-3">
                <div>
                  <label class="lbl">Resultado</label>
                  <div class="seg">
                    @for (r of resultados; track r.valor) {
                      <button type="button" [class.on]="resultado() === r.valor" (click)="resultado.set(r.valor)">{{ r.label }}</button>
                    }
                  </div>
                </div>
                <div>
                  <label class="lbl">Observaciones @if (resultado() !== 'APROBADO') { <span class="text-[#8C1D2E]">· obligatorio</span> }</label>
                  <input class="inp" [ngModel]="observacion()" (ngModelChange)="observacion.set($event)" placeholder="Acuerdos del acto u motivo de la desaprobación" />
                </div>
              </div>
              <button mat-flat-button class="btn-granate mt-3" [disabled]="!actaCompleto() || subiendo()" (click)="pedirArchivo('ACTA')">
                <mat-icon svgIcon="upload" class="size-4 mr-1" /> Subir Acta y concluir
              </button>
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

    .pasos { display:flex; gap:10px; margin-bottom:16px; flex-wrap:wrap; }
    .pasos > div { flex:1; min-width:150px; display:flex; align-items:center; gap:8px; font-size:12.5px; padding:8px 12px; border-radius:10px;
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

    .lbl { display:block; font-size:11.5px; font-weight:600; color:#64748b; margin-bottom:4px; }
    .inp { width:100%; border:1px solid #e2e8f0; border-radius:8px; padding:8px 12px; font-size:13.5px; outline:none; background:#fff; }
    .inp:focus { border-color:#8C1D2E; }

    .seg { display:flex; border:1px solid #e2e8f0; border-radius:8px; overflow:hidden; }
    .seg button { flex:1; padding:7px 6px; font-size:12.5px; color:#475569; background:#fff; border-right:1px solid #eef2f7; white-space:nowrap; }
    .seg button:last-child { border-right:0; }
    .seg button.on { background:#8C1D2E; color:#fff; font-weight:600; }

    .aviso { display:flex; align-items:center; gap:9px; border-radius:10px; padding:10px 14px; font-size:12.5px; margin-bottom:16px; }
    .aviso.ok { background:#ECFDF5; color:#047857; border:1px solid #A7F3D0; }
    .aviso.desaprobado { background:#FEF2F2; color:#B91C1C; border:1px solid #FECACA; }

    .btn-granate { height:36px; font-size:13px; border-radius:8px; padding:0 16px; font-weight:500;
                   background:#8C1D2E !important; color:#fff !important; }
    .btn-granate[disabled] { opacity:.5; }
  `],
})
export class SustentacionDetalleComponent implements OnInit {
  private _svc = inject(SustentacionService);
  private _toast = inject(NotificationService);
  private _route = inject(ActivatedRoute);
  private _router = inject(Router);
  private _dialog = inject(MatDialog);
  private _confirm = inject(ConfirmDialogService);

  protected d = signal<any | null>(null);
  protected cargando = signal(true);
  protected guardando = signal(false);
  protected subiendo = signal(false);

  protected numero = signal('');
  protected expediente = signal('');

  protected modalidad = signal('PRESENCIAL');
  protected fecha = signal('');
  protected hora = signal('');
  protected lugar = signal('');
  protected enlace = signal('');

  protected resultado = signal('APROBADO');
  protected observacion = signal('');

  protected readonly modalidades = [
    { valor: 'PRESENCIAL', label: 'Presencial' },
    { valor: 'VIRTUAL', label: 'Virtual' },
    { valor: 'HIBRIDA', label: 'Híbrida' },
  ];
  protected readonly resultados = [
    { valor: 'APROBADO', label: 'Aprobado' },
    { valor: 'APROBADO_CON_OBSERVACIONES', label: 'Con observaciones' },
    { valor: 'DESAPROBADO', label: 'Desaprobado' },
  ];

  private destino: 'DICTAMEN' | 'ACTA' | null = null;
  private tesisId = '';

  @ViewChild('picker') private picker?: ElementRef<HTMLInputElement>;

  protected paso2Habilitado = () => (this.d()?.jurado?.length ?? 0) >= 3;
  protected paso3Habilitado = () => !!this.d()?.dictamenFirmadoSubido;
  protected paso4Habilitado = () => !!this.d()?.sustentacionProgramada;

  protected requiereLugar(): boolean { return this.modalidad() !== 'VIRTUAL'; }
  protected requiereEnlace(): boolean { return this.modalidad() !== 'PRESENCIAL'; }

  protected programarCompleto(): boolean {
    if (!this.fecha()) return false;
    if (this.requiereLugar() && !this.lugar().trim()) return false;
    if (this.requiereEnlace() && !this.enlace().trim()) return false;
    return true;
  }

  protected actaCompleto(): boolean {
    return this.resultado() === 'APROBADO' || this.observacion().trim().length > 0;
  }

  ngOnInit(): void {
    this.tesisId = this._route.snapshot.paramMap.get('tesisId') ?? '';
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this._svc.detalle$(this.tesisId).subscribe({
      next: (res) => {
        const x = res?.data ?? res;
        this.d.set(x);
        this.numero.set(x?.numeroSugerido ?? '');
        this.expediente.set(x?.expedienteSugerido ?? '');
        this.cargando.set(false);
      },
      error: () => { this.cargando.set(false); this._toast.error('No se pudo cargar el expediente'); },
    });
  }

  protected clasePaso(n: number): string {
    const x = this.d();
    if (!x) return '';
    const hecho = n === 1 ? x.expedienteRecibido : n === 2 ? x.dictamenFirmadoSubido : n === 3 ? x.sustentacionProgramada : x.concluida;
    if (hecho) return 'hecho';
    const habilitado = n === 1 || (n === 2 && this.paso2Habilitado()) || (n === 3 && this.paso3Habilitado()) || (n === 4 && this.paso4Habilitado());
    return habilitado ? 'actual' : '';
  }

  protected recepcionar(): void {
    this.guardando.set(true);
    this._svc.recepcionar$(this.tesisId).subscribe({
      next: () => { this.guardando.set(false); this._toast.success('Expediente recepcionado'); this.cargar(); },
      error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo recepcionar'); },
    });
  }

  protected elaborar(): void {
    this.guardando.set(true);
    this._svc.elaborarDictamen$(this.tesisId, { numero: this.numero().trim() || null, expediente: this.expediente().trim() || null }).subscribe({
      next: () => { this.guardando.set(false); this._toast.success('Dictamen elaborado'); this.cargar(); },
      error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo elaborar'); },
    });
  }

  protected verDictamen(): void {
    this._svc.documentoDictamen$(this.tesisId, 'docx').subscribe({
      next: (blob) => this.abrirPreview(blob, 'dictamen-sustentacion.docx'),
      error: () => this._toast.error('No se pudo generar el documento'),
    });
  }

  private abrirPreview(blob: Blob, nombre: string): void {
    const url = URL.createObjectURL(blob);
    this._dialog.open(DocumentoPreviewDialogComponent, {
      data: { nombre, url, esPdf: blob.type.includes('pdf'), mime: blob.type, blob },
      maxWidth: '96vw', maxHeight: '92vh',
    });
  }

  protected programar(): void {
    if (!this.programarCompleto() || this.guardando()) return;
    const label = this.modalidades.find((m) => m.valor === this.modalidad())?.label ?? '';
    this._confirm.confirmSave({
      title: 'Programar la sustentación',
      message: `Se citará a ${this.d()?.estudianteNombre} a sustentar su tesis. Al confirmar:`,
      details: [
        `${label}, el ${this.fecha()}${this.hora() ? ' a las ' + this.hora() : ''}`,
        this.requiereLugar() ? `en ${this.lugar().trim()}` : `enlace: ${this.enlace().trim()}`,
        'el doctorando recibe el aviso de inmediato',
      ],
      confirmLabel: 'Programar sustentación',
    }).then(() => {
      this.guardando.set(true);
      this._svc.programar$(this.tesisId, {
        fecha: this.fecha(), hora: this.hora(),
        lugar: this.requiereLugar() ? this.lugar() : '',
        modalidad: this.modalidad(),
        enlace: this.requiereEnlace() ? this.enlace() : '',
      }).subscribe({
        next: () => { this.guardando.set(false); this._toast.success('Sustentación programada'); this.cargar(); },
        error: (e) => { this.guardando.set(false); this._toast.error(e?.error?.message ?? 'No se pudo programar'); },
      });
    }).catch(() => {});
  }

  protected pedirArchivo(destino: 'DICTAMEN' | 'ACTA'): void {
    if (destino === 'ACTA') {
      const favorable = this.resultado() !== 'DESAPROBADO';
      this._confirm.confirmSave({
        title: 'Registrar el Acta de sustentación',
        message: favorable
          ? 'Esto concluye el proceso de titulación del doctorando. Al confirmar:'
          : 'El acto no fue favorable: la tesis NO se concluye. Al confirmar:',
        details: favorable
          ? [
              `Resultado: ${this.resultados.find((r) => r.valor === this.resultado())?.label}`,
              'se archiva el Acta firmada',
              'este paso no se puede deshacer',
            ]
          : [
              `Resultado: ${this.resultados.find((r) => r.valor === this.resultado())?.label}`,
              'se archiva esta Acta como historial',
              'la sustentación queda sin programar hasta que coordines un nuevo acto',
            ],
        acknowledge: favorable ? 'Entiendo que esto concluye la tesis' : undefined,
        confirmLabel: favorable ? 'Subir Acta y concluir' : 'Subir Acta',
      }).then(() => { this.destino = destino; this.picker?.nativeElement.click(); }).catch(() => {});
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
    this.subiendo.set(true);
    const done = (msg: string) => { this.subiendo.set(false); this._toast.success(msg); this.cargar(); };
    const fail = (e: any) => { this.subiendo.set(false); this._toast.error(e?.error?.message ?? 'No se pudo subir el archivo'); };
    if (this.destino === 'DICTAMEN') {
      this._svc.subirDictamenFirmado$(this.tesisId, file).subscribe({ next: () => done('Dictamen firmado registrado'), error: fail });
    } else {
      const favorable = this.resultado() !== 'DESAPROBADO';
      this._svc.registrarActa$(this.tesisId, this.resultado(), this.observacion(), file).subscribe({
        next: () => done(favorable
          ? 'Acta registrada; el proceso de titulación ha concluido'
          : 'Acta registrada. Coordina un nuevo acto de sustentación cuando corresponda.'),
        error: fail,
      });
    }
  }

  protected volver(): void {
    this._router.navigate(['/admin/sustentacion']);
  }
}
