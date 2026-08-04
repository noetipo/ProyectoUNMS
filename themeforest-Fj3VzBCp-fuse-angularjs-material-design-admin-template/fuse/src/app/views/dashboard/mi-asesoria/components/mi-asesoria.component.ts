import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogService } from '@/app/shared/confirm-dialog/confirm-dialog.service';
import { previsualizarBlob } from '@/app/shared/perfil-completo/preview.util';
import { MiAsesoriaService } from '../services/mi-asesoria.service';
import { AsesorSugerido, etiquetaEstadoDerivado, MiAsesoria } from '../models/mi-asesoria.model';

/**
 * "Mi asesoría" — trámite de designación del asesor visto por el doctorando.
 *
 * <p><b>Diseño B (elegido el 2026-08-03):</b> la pantalla venía "muy cargada" —párrafos de
 * explicación, cada dato en su propia tarjeta y tres botones con texto por documento—. Ahora:
 * una <i>franja de expediente</i> arriba (tema + tutor/asesor/co-asesor en horizontal) y debajo
 * dos columnas: <i>qué pasa</i> (estado del trámite) y <i>qué hago</i> (documentos). Los iconos
 * son los lucide de siempre y las acciones de cada documento van en <code>.row-actions</code>
 * con botones de icono 7×7, igual que en las tablas del sistema — granate = acción pendiente,
 * esmeralda = ya resuelto, slate = secundaria.</p>
 */
@Component({
  selector: 'app-mi-asesoria',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="breadcrumb">
            <span>Proceso de Tesis</span>
            <mat-icon svgIcon="chevron-right" class="size-3 opacity-40" />
            <span class="breadcrumb__current">Mi asesoría</span>
          </div>
          <h1 class="page-title">Mi asesoría de tesis</h1>
        </div>
      </div>

      <div class="page-content p-6">
        <div class="mx-auto max-w-[1440px] space-y-3">

          @if (loading()) {
            <p class="text-sm text-slate-400">Cargando…</p>
          } @else if (data(); as d) {

            @if (msg()) {
              <div class="flex items-center gap-2 rounded-lg bg-emerald-50 border border-emerald-200 px-3 py-2 text-[12.5px] text-emerald-700">
                <mat-icon svgIcon="circle-check" class="size-4 shrink-0" /> {{ msg() }}
              </div>
            }
            @if (err()) {
              <div class="flex items-center gap-2 rounded-lg bg-rose-50 border border-rose-200 px-3 py-2 text-[12.5px] text-rose-700">
                <mat-icon svgIcon="circle-x" class="size-4 shrink-0" /> {{ err() }}
              </div>
            }

            <!-- ── Franja de expediente: tema + con quién lo haces ── -->
            <section class="rounded-xl border border-slate-200 overflow-hidden">
              <div class="flex items-start gap-2.5 px-3.5 py-3">
                <mat-icon svgIcon="book-open" class="size-[18px] text-slate-400 shrink-0 mt-0.5" />
                <div class="flex-1 min-w-0">
                  @if (d.conTema) {
                    <p class="text-[13px] font-semibold text-slate-800">{{ d.temaTitulo }}</p>
                    <p class="text-[11px] text-slate-400 truncate">{{ d.lineaNombre ?? 'Sin línea' }} · {{ d.nivel ?? '—' }}</p>
                  } @else {
                    <p class="text-[13px] text-slate-400">El coordinador aún no registra tu tema de tesis.</p>
                  }
                </div>
                <span class="shrink-0 text-[10.5px] font-semibold px-2 py-0.5 rounded-full"
                      [class]="d.estadoDerivado === 'SIN_TEMA' ? 'bg-rose-50 text-rose-600'
                             : d.estadoDerivado === 'SIN_ASESOR' ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600'">
                  {{ estadoLabel(d.estadoDerivado) }}
                </span>
              </div>

              <div class="grid sm:grid-cols-3 border-t border-slate-100 bg-slate-50/60
                          divide-y sm:divide-y-0 sm:divide-x divide-slate-100">
                @for (p of equipo(); track p.rol) {
                  <div class="flex items-center gap-2 px-3.5 py-2.5 min-w-0">
                    <mat-icon [svgIcon]="p.icon" class="size-4 shrink-0" [class]="p.nombre ? p.color : 'text-slate-300'" />
                    <div class="min-w-0">
                      <p class="text-[10px] uppercase tracking-wide text-slate-400">{{ p.rol }}</p>
                      <p class="text-[12.5px] truncate" [class]="p.nombre ? 'font-semibold text-slate-700' : 'text-slate-400 italic'">
                        {{ p.nombre ?? p.vacio }}
                      </p>
                    </div>
                  </div>
                }
              </div>
            </section>

            <!-- ── Dos columnas: qué pasa · qué hago ──
                 Sin documentos todavía, "Estado del trámite" ocupa el ancho completo para que
                 no quede medio lienzo en blanco. -->
            <div class="grid gap-3 md:grid-cols-2 items-start">

              <!-- Estado del trámite (incluye a quién puedes solicitar: es parte del trámite,
                   no una sección aparte que repita lo que ya dice la franja de arriba) -->
              <section class="rounded-xl border border-slate-100 p-3.5"
                       [ngClass]="hayDocumentos(d) ? '' : 'md:col-span-2'">
                <h2 class="text-[12.5px] font-semibold text-slate-800 mb-2.5">Estado del trámite</h2>
                <ul class="space-y-2">
                  @for (e of tramite(); track e.texto) {
                    <li class="flex items-start gap-2">
                      <mat-icon [svgIcon]="e.icon" class="size-4 shrink-0 mt-px" [class]="e.color" />
                      <span class="flex-1 text-[12px] text-slate-600 leading-snug">{{ e.texto }}</span>
                      @if (e.cancelarId) {
                        <button mat-stroked-button class="!h-6 !text-[11px] !min-w-0 !px-2.5 shrink-0" (click)="cancelar(e.cancelarId)">
                          Cancelar
                        </button>
                      }
                      @if (e.descargar) {
                        <div class="row-actions shrink-0">
                          <button mat-icon-button class="!w-7 !h-7" title="Descargar el dictamen firmado"
                                  [disabled]="descargando()" (click)="verDictamen()">
                            <mat-icon svgIcon="download" class="text-emerald-600 size-3.5" />
                          </button>
                        </div>
                      }
                    </li>
                  }
                </ul>

                <!-- Candidatos que aún puedes solicitar. Quien ya ocupa un puesto no aparece:
                     su nombre ya está en la franja de arriba. -->
                @if (sugeridosVisibles().length) {
                  <div class="mt-3 pt-3 border-t border-slate-100">
                    <p class="text-[11px] text-slate-400 mb-2">
                      {{ d.asesorNombre ? 'Tu tutor también sugirió para co-asesoría' : 'Asesores sugeridos por tu tutor' }}
                    </p>
                    <div class="space-y-1.5">
                      @for (a of sugeridosVisibles(); track a.sugerenciaId) {
                        <div class="flex items-center gap-2">
                          <mat-icon [svgIcon]="a.tipo === 'COASESOR' ? 'users-round' : 'handshake'"
                                    class="size-4 shrink-0" [class]="a.tipo === 'COASESOR' ? 'text-sky-500' : 'text-[#8C1D2E]'" />
                          <div class="min-w-0 flex-1" [title]="detalle(a)">
                            <p class="text-[12px] font-medium text-slate-700 truncate">{{ a.apellidos }}, {{ a.nombres }}</p>
                            <p class="text-[10.5px] text-slate-400 truncate">
                              {{ a.gradoAcademico ?? 'Docente' }}@if (a.categoria) { · {{ a.categoria }} } · {{ a.asesoriasActivas }} asesoría(s)
                            </p>
                            <!-- Dónde trabaja y cuántos años lleva: pesa al elegir asesor. -->
                            @if (a.centroLaboral || a.experienciaAnios) {
                              <p class="text-[10.5px] text-slate-400 truncate flex items-center gap-1">
                                @if (a.centroLaboral) {
                                  <mat-icon svgIcon="building-2" class="size-3 text-slate-300 shrink-0" />{{ a.centroLaboral }}
                                }
                                @if (a.experienciaAnios) {
                                  <span class="text-slate-300">·</span> {{ a.experienciaAnios }} años
                                }
                              </p>
                            }
                          </div>
                          <div class="flex flex-col items-center shrink-0">
                            <button class="btn-dark !h-6 !text-[11px] !px-2.5"
                                    [disabled]="!puedeSolicitar(a)" [title]="motivoBloqueo(a) ?? etiquetaSolicitar(a)"
                                    (click)="solicitar(a)">
                              Solicitar
                            </button>
                            <!-- El co-asesor no es obligatorio: se avisa bajo el botón para que
                                 nadie sienta que su trámite depende de pedirlo. -->
                            @if (a.tipo === 'COASESOR') {
                              <span class="text-[9.5px] text-slate-400 mt-1 leading-none"
                                    title="El co-asesor es opcional: tu proceso avanza igual sin él.">opcional</span>
                            }
                          </div>
                        </div>
                      }
                    </div>
                  </div>
                }
              </section>

              <!-- Documentos -->
              @if (hayDocumentos(d)) {
                <section class="rounded-xl border border-slate-100 p-3.5">
                  <div class="flex items-baseline gap-1.5 mb-2.5">
                    <h2 class="text-[12.5px] font-semibold text-slate-800">Documentos</h2>
                    <span class="text-[11px] text-slate-400">· descarga, firma y sube</span>
                  </div>
                  <div class="space-y-1.5">
                    @for (doc of documentos(); track doc.key) {
                      <div class="flex items-center gap-2.5 rounded-lg border border-slate-100 px-2.5 py-2">
                        <mat-icon [svgIcon]="doc.subida ? 'file-check' : 'file-text'" class="size-4 shrink-0"
                                  [class]="doc.subida ? 'text-emerald-600' : doc.disponible ? 'text-amber-500' : 'text-slate-300'" />
                        <div class="min-w-0 flex-1">
                          <p class="text-[12px] font-medium text-slate-700 truncate">{{ doc.label }}</p>
                          <p class="text-[10.5px]" [class]="doc.subida ? 'text-emerald-600' : doc.disponible ? 'text-amber-600' : 'text-slate-400'">
                            {{ doc.subida ? 'Firmado subido' : doc.disponible ? 'Pendiente de firma' : 'Cuando tu asesor acepte' }}
                          </p>
                        </div>
                        <div class="row-actions shrink-0">
                          <button mat-icon-button class="!w-7 !h-7" title="Ver el documento en PDF"
                                  [disabled]="!doc.disponible || descargando()" (click)="descargar(doc.tipo, 'pdf')">
                            <mat-icon svgIcon="file-search" class="text-slate-400 size-3.5" />
                          </button>
                          <button mat-icon-button class="!w-7 !h-7" title="Descargar en Word"
                                  [disabled]="!doc.disponible || descargando()" (click)="descargar(doc.tipo, 'docx')">
                            <mat-icon svgIcon="download" class="text-slate-400 size-3.5" />
                          </button>
                          <button mat-icon-button class="!w-7 !h-7" [title]="doc.subida ? 'Reemplazar el firmado' : 'Subir el documento firmado'"
                                  [disabled]="subiendo() || !doc.disponible" (click)="elegirArchivo(doc.key, file)">
                            <mat-icon svgIcon="upload" class="size-3.5" [class]="doc.subida ? 'text-emerald-600' : 'text-[#8C1D2E]'" />
                          </button>
                        </div>
                      </div>
                    }
                    <input #file type="file" hidden accept=".pdf,.docx" (change)="onFile($event)" />
                  </div>
                </section>
              }
            </div>
          }
        </div>
      </div>
    </div>
  `,
})
export class MiAsesoriaComponent implements OnInit {
  private _svc = inject(MiAsesoriaService);
  private _confirm = inject(ConfirmDialogService);
  private _dialog = inject(MatDialog);

  protected data = signal<MiAsesoria | null>(null);
  protected loading = signal(true);
  protected descargando = signal(false);
  protected subiendo = signal(false);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);

  /** Qué documento se está subiendo (un solo <input file> para las dos filas). */
  private subiendoTipo: 'solicitud' | 'carta' = 'solicitud';

  /** Tutor · asesor · co-asesor, en horizontal. */
  protected equipo = computed(() => {
    const d = this.data();
    return [
      { icon: 'graduation-cap', color: 'text-slate-400', rol: 'Tutor',
        nombre: this.conGrado(d?.tutorGrado, d?.tutorNombre), vacio: 'sin asignar' },
      { icon: 'handshake', color: 'text-[#8C1D2E]', rol: 'Asesor',
        nombre: d?.asesorNombre ?? null, vacio: 'sin designar' },
      { icon: 'users-round', color: 'text-sky-500', rol: 'Co-asesor',
        nombre: d?.coasesorNombre ?? null, vacio: 'opcional' },
    ];
  });

  /**
   * "Qué pasa": el estado del trámite en líneas cortas — la solicitud del asesor, la de
   * co-asesoría (opcional, nunca bloquea) y el dictamen.
   */
  protected tramite = computed(() => {
    const d = this.data();
    if (!d) return [];
    const out: { icon: string; color: string; texto: string; cancelarId?: string; descargar?: boolean }[] = [];

    // 0 · De dónde salen los asesores: el tutor. Solo mientras aún no tienes asesor.
    if (!d.asesorNombre) {
      if (!d.tutorNombre) {
        out.push({ icon: 'graduation-cap', color: 'text-amber-500',
                   texto: 'Aún no tienes tutor. El coordinador te asignará uno y él te sugerirá asesores.' });
      } else if (!d.sugeridos.length) {
        out.push({ icon: 'graduation-cap', color: 'text-slate-300',
                   texto: `${d.tutorNombre} (tu tutor) aún no te ha sugerido asesores.` });
      }
    }

    // 1 · Asesor
    switch (d.solicitudEstado) {
      case 'PENDIENTE':
        out.push({ icon: 'clock', color: 'text-amber-500', cancelarId: d.solicitudId,
                   texto: `Esperando la respuesta de ${d.docenteSolicitadoNombre}.` });
        break;
      case 'ACEPTADA':
        out.push({ icon: 'circle-check', color: 'text-emerald-600',
                   texto: `${d.docenteSolicitadoNombre} aceptó tu asesoría.` });
        break;
      case 'RECHAZADA':
        out.push({ icon: 'circle-x', color: 'text-rose-500',
                   texto: `${d.docenteSolicitadoNombre} rechazó la solicitud${d.motivoRespuesta ? ': ' + d.motivoRespuesta : ''}. Puedes solicitar a otro.` });
        break;
      default:
        out.push({ icon: 'user-round-plus', color: 'text-slate-300',
                   texto: 'Aún no has solicitado asesoría a ningún docente.' });
    }

    // 2 · Co-asesoría (solo si hay algo que contar)
    if (d.coasesorSolicitudEstado === 'PENDIENTE') {
      out.push({ icon: 'clock', color: 'text-amber-500', cancelarId: d.coasesorSolicitudId,
                 texto: `Co-asesoría pendiente con ${d.coasesorSolicitadoNombre}; no detiene tu proceso.` });
    } else if (d.coasesorSolicitudEstado === 'RECHAZADA' && !d.coasesorNombre) {
      out.push({ icon: 'circle-x', color: 'text-rose-500',
                 texto: `${d.coasesorSolicitadoNombre} rechazó la co-asesoría${d.coasesorMotivoRespuesta ? ': ' + d.coasesorMotivoRespuesta : ''}.` });
    }

    // 3 · Dictamen (solo tiene sentido con asesor designado)
    if (d.asesorNombre || d.dictamenEmitido) {
      if (d.dictamenEmitido) {
        out.push({ icon: 'badge-check', color: 'text-emerald-600', descargar: true,
                   texto: `Dictamen de designación emitido${d.dictamenNumero ? ' · N° ' + d.dictamenNumero : ''}.` });
      } else if (d.dictamenMotivoObservacion) {
        out.push({ icon: 'circle-x', color: 'text-rose-500',
                   texto: `Dictamen observado: ${d.dictamenMotivoObservacion} — vuelve a subir los documentos.` });
      } else {
        out.push({ icon: 'stamp', color: 'text-slate-300',
                   texto: (d.solicitudFirmadaSubida && d.cartaFirmadaSubida)
                     ? 'Secretaría está elaborando tu dictamen de designación.'
                     : 'Sube los dos firmados y Secretaría emitirá tu dictamen.' });
      }
    }
    return out;
  });

  /** Los dos documentos del paquete (evita duplicar el bloque en la plantilla). */
  protected documentos = computed(() => {
    const d = this.data();
    return [
      { key: 'solicitud' as const, label: 'Solicitud de asesoría', tipo: 'SOLICITUD_ASESORIA' as const,
        disponible: !!d?.solicitudPdfDisponible, subida: !!d?.solicitudFirmadaSubida },
      { key: 'carta' as const, label: 'Carta de aceptación', tipo: 'CARTA_ACEPTACION' as const,
        disponible: !!d?.cartaPdfDisponible, subida: !!d?.cartaFirmadaSubida },
    ];
  });

  /**
   * Sugeridos que aún se pueden solicitar. Se descarta a quien ya ocupa un puesto (aparecía
   * repetido: en la franja como asesor designado y otra vez aquí con el botón bloqueado) y a
   * toda sugerencia cuyo puesto ya está cubierto. Lo que no es accionable no va en la lista:
   * su información ya está en la franja y en "Estado del trámite".
   */
  protected sugeridosVisibles = computed(() => {
    const d = this.data();
    if (!d) return [];
    return d.sugeridos.filter((a) => {
      if (a.asesorDocenteId === d.asesorDocenteId || a.asesorDocenteId === d.coasesorDocenteId) return false;
      return this.tipoSolicitud(a) === 'COASESOR' ? !d.coasesorNombre : !d.asesorNombre;
    });
  });

  /** Detalle del docente en el tooltip: no necesita ocupar sitio en pantalla. */
  protected detalle(a: AsesorSugerido): string {
    const partes = [
      [a.categoria, a.condicion].filter(Boolean).join(' · ') || null,
      a.cargoActual,
      a.centroLaboral ? `${a.centroLaboral}${a.centroLaboralDetalle ? ' — ' + a.centroLaboralDetalle : ''}` : null,
      a.experienciaAnios ? `${a.experienciaAnios} años de experiencia` : null,
      a.estudios?.length ? a.estudios.join(' · ') : null,
      a.emailInstitucional,
      a.orcid ? `ORCID ${a.orcid}` : null,
      a.lineas?.length ? a.lineas.join(' · ') : null,
      a.nota ? `Nota del tutor: ${a.nota}` : null,
    ].filter(Boolean);
    return partes.join('\n');
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this._svc.bandeja$().subscribe({
      next: (res: any) => { this.data.set(res?.data ?? res ?? null); this.loading.set(false); },
      error: () => { this.data.set(null); this.loading.set(false); },
    });
  }

  estadoLabel = etiquetaEstadoDerivado;

  private conGrado(grado?: string, nombre?: string): string | null {
    if (!nombre) return null;
    return grado ? `${grado} ${nombre}` : nombre;
  }

  /**
   * La sección se muestra SOLO cuando el alumno ya tiene asesor designado. Se mira la
   * designación (`asesorNombre`) y no el estado de la solicitud, por dos motivos: una solicitud
   * aceptada sin tema registrado no llega a materializar la asesoría, y una solicitud posterior
   * rechazada (p. ej. de co-asesoría) no debe esconder los documentos del asesor que ya tiene.
   */
  hayDocumentos(d: MiAsesoria): boolean {
    return !!d.asesorNombre;
  }

  /**
   * Puesto de la solicitud: manda lo que indicó el tutor al sugerirlo; si la sugerencia es
   * antigua y no trae tipo, se deduce del estado (con asesor ya designado solo queda co-asesor).
   */
  tipoSolicitud(a?: AsesorSugerido): 'ASESOR' | 'COASESOR' {
    if (a?.tipo) return a.tipo;
    return this.data()?.asesorNombre ? 'COASESOR' : 'ASESOR';
  }

  etiquetaSolicitar(a?: AsesorSugerido): string {
    return this.tipoSolicitud(a) === 'COASESOR' ? 'Solicitar co-asesoría' : 'Solicitar asesoría';
  }

  /**
   * Por qué no se puede solicitar a este docente, o null si sí se puede. Se muestra junto al
   * botón: un botón deshabilitado sin explicación se lee como "no funciona".
   */
  motivoBloqueo(a: AsesorSugerido): string | null {
    const d = this.data();
    if (!d) return 'Cargando…';
    if (!a.lineaIds?.length) return 'Este docente no tiene líneas de investigación registradas';
    const pendiente = d.solicitudEstado === 'PENDIENTE';           // la del asesor principal
    const coPendiente = d.coasesorSolicitudEstado === 'PENDIENTE'; // la de co-asesoría

    // Co-asesor: el mensaje habla de SU puesto, no de la solicitud del asesor. Que haya una
    // solicitud en curso solo importa aquí porque el asesor principal aún no está confirmado.
    if (this.tipoSolicitud(a) === 'COASESOR') {
      if (!d.asesorNombre) {
        return pendiente
          ? 'Esperando la confirmación de tu asesor principal'
          : 'Disponible cuando tengas asesor principal';
      }
      if (d.coasesorNombre) return `Ya tienes co-asesor: ${d.coasesorNombre}`;
      return coPendiente ? 'Tienes una solicitud de co-asesoría pendiente' : null;
    }

    // Asesor principal: uno solo.
    if (d.asesorNombre) return `Ya tienes asesor designado: ${d.asesorNombre}`;
    if (pendiente) return `Solicitud enviada a ${d.docenteSolicitadoNombre ?? 'un docente'}: esperando respuesta`;
    if (d.solicitudEstado === 'ACEPTADA') return 'Tu asesoría ya fue aceptada';
    return null;
  }

  puedeSolicitar(a: AsesorSugerido): boolean {
    return this.motivoBloqueo(a) === null;
  }

  solicitar(a: AsesorSugerido): void {
    const linea = a.lineaIds?.[0];
    if (!linea) return;
    this.msg.set(null); this.err.set(null);
    const tipo = this.tipoSolicitud(a);
    this._svc.solicitar$(a.asesorDocenteId, linea, this.data()?.temaTitulo, tipo).subscribe({
      next: () => {
        this.msg.set(tipo === 'COASESOR'
          ? 'Solicitud de co-asesoría enviada.'
          : 'Solicitud enviada. Ya puedes descargar la Solicitud en PDF.');
        this.cargar();
      },
      error: (e) => this.err.set(e?.error?.message ?? e?.error?.error ?? 'No se pudo enviar la solicitud'),
    });
  }

  cancelar(solicitudId: string): void {
    this._confirm.confirmDelete({ title: 'Cancelar solicitud', message: '¿Cancelar tu solicitud de asesoría?' })
      .then(() => this._svc.cancelar$(solicitudId).subscribe({ next: () => { this.msg.set('Solicitud cancelada.'); this.cargar(); } }))
      .catch(() => {});
  }

  descargar(tipo: 'SOLICITUD_ASESORIA' | 'CARTA_ACEPTACION', formato: 'pdf' | 'docx' = 'pdf'): void {
    this.descargando.set(true);
    this.err.set(null);
    this._svc.descargar$(tipo, formato).subscribe({
      next: (blob) => {
        // Ambos formatos se ven en el visor (el Word se renderiza con docx-preview); desde ahí
        // se descarga una copia si el alumno la necesita para imprimirla y firmarla.
        const titulo = (tipo === 'SOLICITUD_ASESORIA' ? 'Solicitud de asesoría' : 'Carta de aceptación')
          + (formato === 'docx' ? ' (Word)' : '');
        previsualizarBlob(this._dialog, blob, titulo);
        this.descargando.set(false);
      },
      error: () => { this.err.set('No se pudo descargar el documento'); this.descargando.set(false); },
    });
  }

  /** Recuerda qué fila pidió el archivo y abre el selector (input compartido). */
  elegirArchivo(tipo: 'solicitud' | 'carta', input: HTMLInputElement): void {
    this.subiendoTipo = tipo;
    input.click();
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.subiendo.set(true);
    this.err.set(null);
    this._svc.subirFirmado$(this.subiendoTipo, file).subscribe({
      next: () => { this.subiendo.set(false); this.msg.set('Documento firmado subido correctamente.'); this.cargar(); },
      error: (e) => { this.subiendo.set(false); this.err.set(e?.error?.message ?? 'No se pudo subir el archivo (usa PDF o Word).'); },
    });
  }

  verDictamen(): void {
    this.descargando.set(true);
    this.err.set(null);
    this._svc.descargarDictamen$().subscribe({
      next: (blob) => { previsualizarBlob(this._dialog, blob, 'Dictamen de designación de asesor'); this.descargando.set(false); },
      error: () => { this.err.set('No se pudo descargar el dictamen'); this.descargando.set(false); },
    });
  }
}
