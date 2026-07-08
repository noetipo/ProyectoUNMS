import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EstudianteService } from '../../services/estudiante.service';
import { Estudiante } from '../../models/student.models';
import HelveticaAfm from 'pdfkit/js/data/Helvetica.afm';
import HelveticaBoldAfm from 'pdfkit/js/data/Helvetica-Bold.afm';
import HelveticaObliqueAfm from 'pdfkit/js/data/Helvetica-Oblique.afm';
import HelveticaBoldObliqueAfm from 'pdfkit/js/data/Helvetica-BoldOblique.afm';

@Component({
  selector: 'app-estudiante-constancia',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule],
  template: `
    <!-- ── Toolbar ── -->
    <div class="fixed top-0 inset-x-0 z-10 bg-white border-b border-slate-200 px-6 py-3
                flex items-center justify-between shadow-sm">
      <div class="flex items-center gap-3">
        <a [routerLink]="['/admin/student/estudiantes', id]" mat-button class="!text-slate-500 !text-sm">
          <mat-icon svgIcon="chevron-left" class="size-4 mr-1" /> Volver al detalle
        </a>
        <span class="text-xs text-slate-300">|</span>
        <span class="text-sm font-medium text-slate-700">Constancia de Matrícula</span>
      </div>
      <button mat-flat-button color="primary"
        class="!rounded-lg !h-8 !px-4 !text-sm !font-medium"
        [disabled]="loading() || generando()"
        (click)="generarPdf()">
        <mat-icon svgIcon="file-text" class="size-3.5 mr-1.5" />
        {{ generando() ? 'Generando…' : 'Descargar PDF' }}
      </button>
    </div>

    <!-- ── Fondo ── -->
    <div class="min-h-screen bg-slate-200 pt-20 pb-12 px-4">

      @if (loading()) {
        <div class="flex justify-center items-center h-64">
          <p class="text-sm text-slate-400">Cargando datos del estudiante…</p>
        </div>
      }

      @if (errorMsg()) {
        <div class="max-w-xl mx-auto mt-10 rounded-lg bg-rose-50 border border-rose-200 px-4 py-3 text-sm text-rose-700">
          {{ errorMsg() }}
        </div>
      }

      @if (est(); as e) {

        <!-- ── Documento A4 ── -->
        <div class="bg-white mx-auto shadow-2xl"
          style="width:794px; min-height:1122px; padding:60px 72px; position:relative;
                 font-family:'Times New Roman',Times,serif; box-sizing:border-box;">

          <!-- Franja superior bicolor -->
          <div style="border-top:5px solid #1b3a6b; border-bottom:2px solid #4a7c3f;
                      padding:10px 0; margin-bottom:26px; text-align:center;">
            <p style="font-size:13px; font-weight:700; letter-spacing:3px; color:#1b3a6b;
                      margin:0; font-family:Arial,sans-serif; text-transform:uppercase;">
              Universidad Nacional Mayor de San Marcos
            </p>
            <p style="font-size:9.5px; color:#4a7c3f; margin:4px 0 0;
                      font-family:Arial,sans-serif; font-style:italic;">
              Decana de América — Fundada el 12 de mayo de 1551
            </p>
          </div>

          <!-- Sub-header -->
          <div style="text-align:center; margin-bottom:30px;">
            <p style="font-size:10px; font-weight:700; letter-spacing:2px; color:#374151;
                      margin:0; font-family:Arial,sans-serif; text-transform:uppercase;">
              Escuela de Posgrado
            </p>
            <p style="font-size:10.5px; color:#374151; margin:5px 0 0; font-family:Arial,sans-serif;">
              {{ e.programaDoctorado?.nombre ?? 'Programa de Doctorado' }}
            </p>
          </div>

          <!-- Título -->
          <div style="text-align:center; margin-bottom:38px;">
            <p style="font-size:21px; font-weight:700; letter-spacing:5px; color:#1b3a6b;
                      margin:0; text-transform:uppercase; font-family:Arial,sans-serif;">
              CONSTANCIA
            </p>
            <p style="font-size:11px; color:#6b7280; margin:6px 0 0;
                      font-family:Arial,sans-serif; letter-spacing:2px; text-transform:uppercase;">
              de Matrícula y Condición Académica
            </p>
          </div>

          <!-- Cuerpo -->
          <div style="font-size:12.5px; line-height:2; color:#1f2937; text-align:justify;">

            <p style="margin:0 0 18px; text-indent:40px;">
              La Coordinación del
              <strong>{{ e.programaDoctorado?.nombre ?? 'Programa de Doctorado' }}</strong>
              de la Escuela de Posgrado de la
              <strong>Universidad Nacional Mayor de San Marcos</strong>,
              hace constar que:
            </p>

            <!-- Bloque nombre -->
            <div style="border:1px solid #cbd5e1; border-radius:6px; padding:18px 28px;
                        margin:26px 0; background:#f8fafc; text-align:center;">
              <p style="font-size:17px; font-weight:700; color:#1b3a6b; margin:0;
                        text-transform:uppercase; letter-spacing:1px; font-family:Arial,sans-serif;">
                {{ e.apellidoPaterno }} {{ e.apellidoMaterno ?? '' }}, {{ e.nombres }}
              </p>
              @if (e.tipoDocumento && e.numeroDocumento) {
                <p style="font-size:11px; color:#4b5563; margin:8px 0 0; font-family:Arial,sans-serif;">
                  {{ tipoDocLabel[e.tipoDocumento] }}:&nbsp;
                  <strong style="font-size:13px;">{{ e.numeroDocumento }}</strong>
                </p>
              }
            </div>

            <p style="margin:18px 0; text-indent:40px;">
              Se encuentra debidamente <strong>MATRICULADO(A)</strong> en el
              <strong>{{ e.programaDoctorado?.nombre ?? '—' }}</strong>,
              habiendo ingresado en el año académico <strong>{{ e.anioIngreso ?? '—' }}</strong>,
              con código de matrícula <strong>{{ e.codMatricula ?? '—' }}</strong>,
              bajo la modalidad de financiamiento
              <strong>{{ e.financiamiento ? financLabel[e.financiamiento] : '—' }}</strong>
              y con condición académica actual de
              <strong>{{ e.condicion ? condLabel[e.condicion] : '—' }}</strong>.
            </p>

            @if (e.emailInstitucional) {
              <p style="margin:18px 0; text-indent:40px;">
                El(la) estudiante tiene asignado el correo institucional
                <strong>{{ e.emailInstitucional }}</strong>.
              </p>
            }

            <p style="margin:18px 0; text-indent:40px;">
              La presente constancia se expide a solicitud del(la) interesado(a),
              para los fines que estime conveniente.
            </p>

          </div>

          <!-- Fecha -->
          <p style="text-align:right; font-size:12px; color:#374151; margin-top:44px;
                    margin-bottom:60px; font-family:Arial,sans-serif;">
            Lima, {{ fechaHoy() }}
          </p>

          <!-- Firmas -->
          <div style="display:flex; justify-content:space-around; margin-top:30px;">
            <div style="text-align:center; width:210px;">
              <div style="border-top:1px solid #6b7280; padding-top:8px;">
                <p style="font-size:11px; font-weight:700; color:#1f2937; margin:0;
                          font-family:Arial,sans-serif;">Director(a) del Programa</p>
                <p style="font-size:10px; color:#6b7280; margin:3px 0 0;
                          font-family:Arial,sans-serif;">Escuela de Posgrado — UNMSM</p>
              </div>
            </div>
            <div style="text-align:center; width:210px;">
              <div style="border-top:1px solid #6b7280; padding-top:8px;">
                <p style="font-size:11px; font-weight:700; color:#1f2937; margin:0;
                          font-family:Arial,sans-serif;">Coordinador(a) Académico(a)</p>
                <p style="font-size:10px; color:#6b7280; margin:3px 0 0;
                          font-family:Arial,sans-serif;">Programa de Doctorado — UNMSM</p>
              </div>
            </div>
          </div>

          <!-- Pie de verificación -->
          <div style="position:absolute; bottom:36px; left:72px; right:72px;
                      border-top:1px solid #e5e7eb; padding-top:8px; text-align:center;">
            <p style="font-size:8.5px; color:#9ca3af; margin:0; font-family:Arial,sans-serif;">
              Emitido por el Sistema de Gestión Académica · UNMSM
              &nbsp;·&nbsp;
              Código: <strong>{{ e.codigoSistema }}-{{ verCode() }}</strong>
              &nbsp;·&nbsp;
              Fecha de emisión: {{ fechaHoy() }}
            </p>
          </div>

        </div>
      }
    </div>
  `,
})
export class EstudianteConstanciaComponent implements OnInit {
  private _route   = inject(ActivatedRoute);
  private _service = inject(EstudianteService);

  est      = signal<Estudiante | null>(null);
  loading  = signal(true);
  generando = signal(false);
  errorMsg = signal<string | null>(null);
  id!: string;

  readonly tipoDocLabel: Record<string, string> = {
    DNI:                'DNI',
    CARNET_EXTRANJERIA: 'Carnet de Extranjería',
    PASAPORTE:          'Pasaporte',
    PTP:                'PTP',
    CARNET_DIPLOMATICO: 'Carnet Diplomático',
  };
  readonly financLabel: Record<string, string> = {
    BECA_COMPLETA:  'Beca Completa',
    BECA_PARCIAL:   'Beca Parcial',
    AUTOFINANCIADO: 'Autofinanciado',
  };
  readonly condLabel: Record<string, string> = {
    REGULAR:    'Regular',
    SANCIONADO: 'Sancionado',
    EGRESADO:   'Egresado',
    RETIRADO:   'Retirado',
  };

  ngOnInit(): void {
    this.id = this._route.snapshot.paramMap.get('id')!;
    this._service.getById(this.id).subscribe({
      next: e  => { this.est.set(e); this.loading.set(false); },
      error: e => { this.errorMsg.set(e.message); this.loading.set(false); },
    });
  }

  fechaHoy(): string {
    const d = new Date();
    const meses = ['enero','febrero','marzo','abril','mayo','junio',
                   'julio','agosto','septiembre','octubre','noviembre','diciembre'];
    return `${d.getDate()} de ${meses[d.getMonth()]} de ${d.getFullYear()}`;
  }

  verCode(): string {
    return new Date().toISOString().slice(0, 10).replace(/-/g, '');
  }

  async generarPdf(): Promise<void> {
    const e = this.est();
    if (!e) return;

    this.generando.set(true);

    // Dynamic import handles esbuild CJS→ESM interop (.default fallback)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const mod = await import('pdfmake/build/pdfmake') as any;
    const pdfMake = mod.default ?? mod;

    // Load Helvetica AFM metrics into pdfmake's VirtualFileSystem
    pdfMake.addVirtualFileSystem({
      'data/Helvetica.afm':           { data: HelveticaAfm,           encoding: 'utf8' },
      'data/Helvetica-Bold.afm':      { data: HelveticaBoldAfm,       encoding: 'utf8' },
      'data/Helvetica-Oblique.afm':   { data: HelveticaObliqueAfm,    encoding: 'utf8' },
      'data/Helvetica-BoldOblique.afm': { data: HelveticaBoldObliqueAfm, encoding: 'utf8' },
    });
    pdfMake.addFonts({
      Helvetica: {
        normal: 'Helvetica', bold: 'Helvetica-Bold',
        italics: 'Helvetica-Oblique', bolditalics: 'Helvetica-BoldOblique',
      },
    });

    const fecha   = this.fechaHoy();
    const ver     = this.verCode();
    const programa = e.programaDoctorado?.nombre ?? 'Programa de Doctorado';
    const nombre  = `${e.apellidoPaterno} ${e.apellidoMaterno ?? ''}, ${e.nombres}`.toUpperCase().trim();
    const docId   = (e.tipoDocumento && e.numeroDocumento)
      ? `${this.tipoDocLabel[e.tipoDocumento] ?? e.tipoDocumento}: ${e.numeroDocumento}`
      : null;
    const financi = this.financLabel[e.financiamiento!] ?? e.financiamiento ?? '—';
    const condic  = this.condLabel[e.condicion!] ?? e.condicion ?? '—';
    const W = 451; // usable width (595 - 72*2)

    const docDef = {
      defaultStyle:  { font: 'Helvetica', fontSize: 11, color: '#1f2937' },
      pageSize:      'A4',
      pageMargins:   [72, 60, 72, 72] as [number, number, number, number],

      footer: () => ({
        text: `Emitido por el Sistema de Gestión Académica · UNMSM  ·  Código: ${e.codigoSistema ?? this.id}-${ver}  ·  Fecha: ${fecha}`,
        fontSize: 7,
        color: '#9ca3af',
        alignment: 'center',
        margin: [72, 8, 72, 0],
        font: 'Helvetica',
      }),

      content: [
        // ── Header band ──────────────────────────────────────
        { canvas: [{ type: 'rect', x: 0, y: 0, w: W, h: 5, color: '#1b3a6b' }] },
        { canvas: [{ type: 'rect', x: 0, y: 0, w: W, h: 2, color: '#4a7c3f' }], margin: [0, 3, 0, 0] },

        { text: 'UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS',
          bold: true, fontSize: 13, color: '#1b3a6b',
          alignment: 'center', characterSpacing: 2, margin: [0, 10, 0, 0] },
        { text: 'Decana de América — Fundada el 12 de mayo de 1551',
          italics: true, fontSize: 9, color: '#4a7c3f',
          alignment: 'center', margin: [0, 4, 0, 0] },

        // ── Sub-header ────────────────────────────────────────
        { canvas: [{ type: 'line', x1: 0, y1: 0, x2: W, y2: 0,
            lineWidth: 0.5, lineColor: '#e5e7eb' }], margin: [0, 14, 0, 14] },

        { text: 'ESCUELA DE POSGRADO',
          bold: true, fontSize: 10, color: '#374151',
          alignment: 'center', characterSpacing: 2 },
        { text: programa,
          fontSize: 10, color: '#374151',
          alignment: 'center', margin: [0, 5, 0, 0] },

        // ── Title ─────────────────────────────────────────────
        { text: 'CONSTANCIA',
          bold: true, fontSize: 22, color: '#1b3a6b',
          alignment: 'center', characterSpacing: 5, margin: [0, 28, 0, 0] },
        { text: 'DE MATRÍCULA Y CONDICIÓN ACADÉMICA',
          fontSize: 9.5, color: '#6b7280',
          alignment: 'center', characterSpacing: 2, margin: [0, 6, 0, 32] },

        // ── Body § 1 ──────────────────────────────────────────
        { text: [
            'La Coordinación del ',
            { text: programa, bold: true },
            ' de la Escuela de Posgrado de la ',
            { text: 'Universidad Nacional Mayor de San Marcos', bold: true },
            ', hace constar que:',
          ],
          fontSize: 12, alignment: 'justify', lineHeight: 1.8,
          margin: [20, 0, 0, 0] },

        // ── Student name box ──────────────────────────────────
        {
          table: {
            widths: ['*'],
            body: [[{
              stack: [
                { text: nombre, bold: true, fontSize: 16, color: '#1b3a6b',
                  alignment: 'center', font: 'Helvetica' },
                ...(docId ? [{ text: docId, fontSize: 10, color: '#4b5563',
                  alignment: 'center', margin: [0, 6, 0, 0] }] : []),
              ],
              fillColor: '#f8fafc',
              margin: [28, 16, 28, 16],
            }]],
          },
          layout: {
            hLineWidth: () => 1,
            vLineWidth: () => 1,
            hLineColor: () => '#cbd5e1',
            vLineColor: () => '#cbd5e1',
          },
          margin: [0, 20, 0, 0],
        },

        // ── Body § 2 ──────────────────────────────────────────
        { text: [
            'Se encuentra debidamente ',
            { text: 'MATRICULADO(A)', bold: true },
            ' en el ',
            { text: programa, bold: true },
            ', habiendo ingresado en el año académico ',
            { text: String(e.anioIngreso ?? '—'), bold: true },
            ', con código de matrícula ',
            { text: e.codMatricula ?? '—', bold: true },
            ', bajo la modalidad de financiamiento ',
            { text: financi, bold: true },
            ' y con condición académica actual de ',
            { text: condic, bold: true },
            '.',
          ],
          fontSize: 12, alignment: 'justify', lineHeight: 1.8,
          margin: [20, 20, 0, 0] },

        // ── Body § 3 (email, conditional) ────────────────────
        ...(e.emailInstitucional ? [{
          text: [
            'El(la) estudiante tiene asignado el correo institucional ',
            { text: e.emailInstitucional, bold: true },
            '.',
          ],
          fontSize: 12, alignment: 'justify', lineHeight: 1.8,
          margin: [20, 20, 0, 0],
        }] : []),

        // ── Body § 4 ──────────────────────────────────────────
        { text: 'La presente constancia se expide a solicitud del(la) interesado(a), para los fines que estime conveniente.',
          fontSize: 12, alignment: 'justify', lineHeight: 1.8,
          margin: [20, 20, 0, 0] },

        // ── Date ──────────────────────────────────────────────
        { text: `Lima, ${fecha}`,
          fontSize: 11, color: '#374151', alignment: 'right',
          margin: [0, 44, 0, 0] },

        // ── Signatures ────────────────────────────────────────
        {
          columns: [
            {
              width: 200,
              stack: [
                { canvas: [{ type: 'line', x1: 0, y1: 0, x2: 200, y2: 0,
                    lineWidth: 1, lineColor: '#6b7280' }] },
                { text: 'Director(a) del Programa', bold: true, fontSize: 10,
                  color: '#1f2937', alignment: 'center', margin: [0, 6, 0, 0] },
                { text: 'Escuela de Posgrado — UNMSM', fontSize: 9,
                  color: '#6b7280', alignment: 'center', margin: [0, 3, 0, 0] },
              ],
            },
            { width: '*', text: '' },
            {
              width: 200,
              stack: [
                { canvas: [{ type: 'line', x1: 0, y1: 0, x2: 200, y2: 0,
                    lineWidth: 1, lineColor: '#6b7280' }] },
                { text: 'Coordinador(a) Académico(a)', bold: true, fontSize: 10,
                  color: '#1f2937', alignment: 'center', margin: [0, 6, 0, 0] },
                { text: 'Programa de Doctorado — UNMSM', fontSize: 9,
                  color: '#6b7280', alignment: 'center', margin: [0, 3, 0, 0] },
              ],
            },
          ],
          margin: [0, 60, 0, 0],
        },
      ],
    };

    try {
      await pdfMake.createPdf(docDef).download(`constancia-${e.codigoSistema ?? this.id}.pdf`);
    } catch (err) {
      console.error('pdfmake error', err);
    } finally {
      this.generando.set(false);
    }
  }
}