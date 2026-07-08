import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { END_POINTS } from '@/app/providers/utils/end-points';
import { NavigationItem } from '@/app/domains/admin/layout/data/navigation';

export interface MenuItemDto {
  id: string;
  title: string;
  subtitle?: string;
  type?: string;
  icon?: string;
  link?: string;
  moduleOrder?: number;
  children?: MenuItemDto[];
}

@Injectable({ providedIn: 'root' })
export class MenuService {
  private _http = inject(HttpClient);

  /** GET /api/modules/menu — menú del usuario según sus roles */
  getMenu$(): Observable<MenuItemDto[]> {
    return this._http.get<MenuItemDto[]>(
      `${environment.url}${END_POINTS.setup.moduleMenu}`
    );
  }

  /** GET /api/modules/menu/role/{roleId} — menú filtrado por un rol específico */
  getMenuByRole$(roleId: string): Observable<MenuItemDto[]> {
    return this._http.get<MenuItemDto[]>(
      `${environment.url}${END_POINTS.setup.moduleMenu}/role/${roleId}`
    );
  }

  /** Convierte la respuesta del backend al formato NavigationItem del sidebar */
  mapToNavigation(items: MenuItemDto[]): NavigationItem[] {
    return items
      .slice()
      .sort((a, b) => (a.moduleOrder ?? 0) - (b.moduleOrder ?? 0))
      .map((item) => this._mapItem(item));
  }

  private _mapItem(item: MenuItemDto): NavigationItem {
    const navItem: NavigationItem = {
      id: item.id,
      label: item.title,
      description: item.subtitle || undefined,
      icon: this._normalizeIcon(item.icon),
      route: this._normalizeLink(item.link),
      expanded: false,
    };

    if (item.children?.length) {
      navItem.children = item.children
        .slice()
        .sort((a, b) => (a.moduleOrder ?? 0) - (b.moduleOrder ?? 0))
        .map((child) => this._mapItem(child));
    }

    return navItem;
  }

  /** Traduce links del Angular 17 (homeScreen/) al path base del Angular 21 (admin/) */
  private _normalizeLink(link?: string): string | undefined {
    if (!link) return undefined;
    return link.replace(/^\/?homeScreen\//, 'admin/');
  }

  /** Traduce iconos heroicons_outline:/heroicons_solid: a nombres Lucide */
  private _normalizeIcon(icon?: string): string | undefined {
    if (!icon) return undefined;

    // Quitar prefijo heroicons_outline: o heroicons_solid:
    const name = icon.replace(/^heroicons_(outline|solid):/, '');

    // Si el nombre ya no tiene prefijo conocido y no tiene ':', devolver directo (es Lucide ya)
    if (!icon.includes(':')) return icon;

    // Mapeo heroicons → lucide
    const map: Record<string, string> = {
      // Navegación / Layout
      'home': 'house',
      'bars-3': 'menu',
      'bars-4': 'list',
      'bars-2': 'menu',
      'squares-2x2': 'grid-2x2',
      'view-columns': 'columns-2',
      'list-bullet': 'list',
      'numbered-list': 'list-ordered',
      'rectangle-stack': 'layers',
      'table-cells': 'table',
      // Usuarios / Personas
      'user': 'user',
      'user-circle': 'circle-user',
      'user-group': 'users',
      'users': 'users',
      'identification': 'id-card',
      'finger-print': 'fingerprint-pattern',
      // Configuración
      'cog': 'settings',
      'cog-6-tooth': 'settings',
      'cog-8-tooth': 'settings-2',
      'adjustments-horizontal': 'sliders-horizontal',
      'adjustments-vertical': 'sliders-vertical',
      'wrench': 'wrench',
      'wrench-screwdriver': 'wrench',
      // Documentos / Archivos
      'document': 'file',
      'document-text': 'file-text',
      'document-duplicate': 'copy',
      'document-chart-bar': 'file-chart-column',
      'clipboard': 'clipboard',
      'clipboard-document': 'clipboard',
      'clipboard-document-list': 'clipboard-list',
      'clipboard-document-check': 'clipboard-check',
      'clipboard-list': 'clipboard-list',
      'clipboard-check': 'clipboard-check',
      'archive-box': 'archive',
      'folder': 'folder',
      'folder-open': 'folder-open',
      // Alertas / Estado
      'exclamation-circle': 'circle-alert',
      'exclamation-triangle': 'triangle-alert',
      'information-circle': 'info',
      'check-circle': 'circle-check',
      'x-circle': 'circle-x',
      'shield-check': 'shield-check',
      'shield-exclamation': 'shield-alert',
      'bell': 'bell',
      'bell-alert': 'bell-dot',
      // Base de datos / Servidor
      'circle-stack': 'database',
      'server': 'server',
      'cpu-chip': 'cpu',
      'cloud': 'cloud',
      'cloud-arrow-up': 'cloud-upload',
      'cloud-arrow-down': 'cloud-download',
      // Gráficas / Reportes
      'chart-bar': 'chart-bar',
      'chart-pie': 'chart-pie',
      'presentation-chart-bar': 'chart-bar',
      'presentation-chart-line': 'chart-line',
      'arrow-trending-up': 'trending-up',
      'arrow-trending-down': 'trending-down',
      // Empresa / Negocio
      'building-office': 'building',
      'building-office-2': 'building-2',
      'office-building': 'building',
      'briefcase': 'briefcase',
      'academic-cap': 'graduation-cap',
      // Finanzas / Pagos
      'currency-dollar': 'dollar-sign',
      'currency-euro': 'euro',
      'banknotes': 'banknote',
      'credit-card': 'credit-card',
      'receipt-percent': 'receipt',
      'shopping-bag': 'shopping-bag',
      'shopping-cart': 'shopping-cart',
      'truck': 'truck',
      // Comunicación
      'envelope': 'mail',
      'envelope-open': 'mail-open',
      'chat-bubble-left': 'message-square',
      'chat-bubble-left-ellipsis': 'message-square',
      'paper-airplane': 'send',
      'phone': 'phone',
      'phone-arrow-up-right': 'phone-outgoing',
      // Mapa / Ubicación
      'map': 'map',
      'map-pin': 'map-pin',
      'globe-alt': 'globe',
      'globe-americas': 'globe',
      // Acciones
      'pencil': 'pencil',
      'pencil-square': 'square-pen',
      'trash': 'trash',
      'plus': 'plus',
      'plus-circle': 'circle-plus',
      'minus': 'minus',
      'minus-circle': 'circle-minus',
      'x-mark': 'x',
      'check': 'check',
      'eye': 'eye',
      'eye-slash': 'eye-off',
      'magnifying-glass': 'search',
      'magnifying-glass-plus': 'zoom-in',
      'magnifying-glass-minus': 'zoom-out',
      'funnel': 'funnel',
      'arrow-up-tray': 'upload',
      'arrow-down-tray': 'download',
      'share': 'share',
      'link': 'link',
      'arrow-path': 'refresh-ccw',
      'arrow-uturn-left': 'undo',
      'arrow-uturn-right': 'redo',
      // Seguridad
      'lock-closed': 'lock',
      'lock-open': 'lock-open',
      'key': 'key',
      // Misc
      'star': 'star',
      'heart': 'heart',
      'bookmark': 'bookmark',
      'tag': 'tag',
      'tags': 'tags',
      'calendar': 'calendar',
      'calendar-days': 'calendar-days',
      'clock': 'clock',
      'light-bulb': 'lightbulb',
      'fire': 'flame',
      'bolt': 'bolt',
      'power': 'power',
      'qr-code': 'qr-code',
      'newspaper': 'newspaper',
      'book-open': 'book-open',
      'inbox': 'inbox',
      'photo': 'image',
      'film': 'film',
      'music-note': 'music',
      'microphone': 'mic',
      'video-camera': 'video',
      'cube': 'box',
      'beaker': 'beaker',
      'bug-ant': 'bug',
      'rss': 'rss',
      'wifi': 'wifi',
      'signal': 'signal',
    };

    return map[name] ?? name;
  }
}
