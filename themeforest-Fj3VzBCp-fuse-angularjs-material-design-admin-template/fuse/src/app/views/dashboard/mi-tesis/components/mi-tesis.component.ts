import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ExpedienteService } from '../../expediente/services/expediente.service';
import { Expediente } from '../../expediente/models/expediente.model';

/**
 * "Mi tesis" — contenedor único del estudiante. Antes su avance estaba repartido en tres
 * ítems sueltos del menú (expediente, editor del proyecto y asesoría), lo que confundía;
 * ahora es UNA sola entrada con pestañas. Cada pestaña sigue siendo la pantalla de siempre
 * (se cargan como rutas hijas), así que los enlaces de las notificaciones siguen valiendo.
 *
 * <p><b>Las pestañas siguen al proceso.</b> "Mi proyecto" es la Etapa 4 y no existe antes de
 * que la UPG emita el dictamen de designación del asesor: mostrarla siempre hacía creer que
 * el doctorando podía redactar desde el día uno. Mientras no corresponda, en su lugar va un
 * indicador que dice qué falta y lleva a la pantalla donde se resuelve — no una pestaña
 * muerta en gris. Cuando se habilita, entra con una marca "Nuevo" hasta que la abre.</p>
 */
@Component({
  selector: 'app-mi-tesis',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="flex flex-col h-full bg-white">
      <!-- Barra de pestañas -->
      <nav class="flex items-center gap-1 px-6 border-b border-slate-100 bg-slate-50/60">
        <span class="text-[11px] font-bold text-slate-400 uppercase tracking-wide mr-3">Mi tesis</span>

        @for (t of tabs(); track t.path) {
          <a [routerLink]="t.path" routerLinkActive #rla="routerLinkActive" (click)="marcarVisto(t.path)"
             class="tab-aparece relative inline-flex items-center gap-1.5 px-3 py-2.5 text-[12.5px] font-medium border-b-2 -mb-px transition"
             [ngClass]="rla.isActive
               ? 'border-[#8C1D2E] text-[#8C1D2E]'
               : 'border-transparent text-slate-500 hover:text-slate-700'">
            <!-- El número dice en qué orden se recorre; el mapa (avance) no lleva. -->
            @if (t.paso) {
              <span class="grid place-items-center size-4 rounded text-[9px] font-extrabold transition"
                    [ngClass]="rla.isActive ? 'bg-[#8C1D2E] text-white' : 'bg-slate-200 text-slate-500'">{{ t.paso }}</span>
            } @else {
              <mat-icon [svgIcon]="t.icon" class="size-4" />
            }
            {{ t.label }}
            @if (t.path === 'proyecto' && esNuevo()) {
              <span class="ml-1 inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full bg-[#8C1D2E] text-white text-[9px] font-bold tracking-wide">
                <span class="size-1.5 rounded-full bg-white/90 animate-ping"></span> NUEVO
              </span>
            }
          </a>
        }

        <!-- Etapa aún no alcanzada: en vez de una pestaña deshabilitada, un atajo a lo que falta. -->
        @if (!habilitado() && cargado()) {
          <button type="button" (click)="irAloQueFalta()"
                  class="ml-auto my-1.5 inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border border-slate-200 bg-white text-[11.5px] text-slate-400 hover:text-slate-600 hover:border-slate-300 transition"
                  [title]="'Mi proyecto (Etapa 4) se habilita cuando ' + falta().detalle">
            <mat-icon svgIcon="lock" class="size-3.5" />
            <span><b class="font-semibold">Mi proyecto</b> se habilita al {{ falta().corto }}</span>
            <mat-icon svgIcon="arrow-right" class="size-3.5" />
          </button>
        }
      </nav>

      <!-- Pantalla de la pestaña activa -->
      <div class="flex-1 min-h-0 overflow-hidden">
        <router-outlet />
      </div>
    </div>
  `,
  styles: [`
    /* La pestaña que se habilita no debe "aparecer de golpe": entra deslizándose. */
    .tab-aparece { animation: tab-in .35s ease-out both; }
    @keyframes tab-in {
      from { opacity: 0; transform: translateY(-4px); }
      to   { opacity: 1; transform: none; }
    }
    @media (prefers-reduced-motion: reduce) {
      .tab-aparece { animation: none; }
    }
  `],
})
export class MiTesisComponent implements OnInit {
  private _svc = inject(ExpedienteService);
  private _router = inject(Router);
  private readonly navegador = isPlatformBrowser(inject(PLATFORM_ID));

  private static readonly VISTO = 'mi-tesis:proyecto-visto';

  /** Etapa en curso del proceso (1..8); 0 mientras no se sabe. */
  private etapa = signal(0);
  protected cargado = signal(false);
  private visto = signal(true);

  /** La Etapa 4 (elaboración del proyecto) arranca con el dictamen de designación firmado. */
  protected habilitado = computed(() => this.etapa() >= 4);
  /** El cierre del expediente se abre con la carta de opinión favorable del asesor. */
  protected cierre = signal(false);
  protected esNuevo = computed(() => this.habilitado() && !this.visto());

  /**
   * Orden = <b>el orden en que se habilitan</b>, para que la barra se lea como el camino a
   * recorrer: primero consigues asesor, luego redactas, al final cierras y envías. "Avance del
   * proceso" va delante por ser el mapa (siempre disponible, sin número de paso).
   *
   * <p>Antes estaba "Mi asesoría" al final —después de "Cierre y envío"— cuando es lo primero
   * que hace el doctorando: leído de izquierda a derecha, el proceso parecía ir al revés.</p>
   */
  protected tabs = computed(() => [
    { path: 'avance', label: 'Avance del proceso', icon: 'route', paso: 0 },
    { path: 'asesoria', label: 'Mi asesoría', icon: 'handshake', paso: 1 },
    ...(this.habilitado() ? [{ path: 'proyecto', label: 'Mi proyecto', icon: 'file-pen-line', paso: 2 }] : []),
    ...(this.cierre() ? [{ path: 'cierre', label: 'Cierre y envío', icon: 'send', paso: 3 }] : []),
  ]);

  /** Qué falta para llegar a la Etapa 4, en las palabras del proceso. */
  protected falta = computed(() => {
    switch (this.etapa()) {
      case 1: return { corto: 'registrarse tu tema', detalle: 'el Coordinador registre tu tema de tesis', ir: 'avance' };
      case 2: return { corto: 'asignarte un tutor', detalle: 'te asignen tu tutor académico', ir: 'avance' };
      default: return { corto: 'emitirse el dictamen de tu asesor', detalle: 'la UPG emita el dictamen de designación de tu asesor', ir: 'asesoria' };
    }
  });

  ngOnInit(): void {
    this.visto.set(!this.navegador || localStorage.getItem(MiTesisComponent.VISTO) === '1');
    this._svc.miExpediente$().subscribe({
      next: (res) => {
        const e: Expediente | null = res?.data ?? res ?? null;
        this.etapa.set(e?.etapaEnCurso ?? (e?.proyectoHabilitado ? 4 : 1));
        this.cierre.set(!!e?.cierreHabilitado);
        this.cargado.set(true);
        this.protegerRuta();
      },
      // Sin tesis activa todavía: el proceso ni siquiera empezó, así que la pestaña no aplica.
      error: () => { this.etapa.set(1); this.cargado.set(true); this.protegerRuta(); },
    });
  }

  /** Un enlace viejo o un marcador a /mi-tesis/proyecto no debe dejar al alumno en un editor vacío. */
  private protegerRuta(): void {
    if (!this.habilitado() && this._router.url.includes('/mi-tesis/proyecto')) {
      this._router.navigate(['/admin/mi-tesis/avance']);
    }
  }

  /** El atajo lleva a donde el alumno puede hacer algo (o a la línea de tiempo si no depende de él). */
  protected irAloQueFalta(): void {
    this._router.navigate(['/admin/mi-tesis', this.falta().ir]);
  }

  protected marcarVisto(path: string): void {
    if (path !== 'proyecto' || this.visto()) return;
    this.visto.set(true);
    if (this.navegador) localStorage.setItem(MiTesisComponent.VISTO, '1');
  }
}
