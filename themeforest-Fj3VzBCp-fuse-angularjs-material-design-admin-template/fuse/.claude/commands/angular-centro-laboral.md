# angular-centro-laboral

Implementa el módulo **Centros Laborales** desde cero siguiendo el patrón de `setup/module`.

## Rutas absolutas

- **Frontend:** `/Users/noe/Documents/ProyectoUNMS/themeforest-Fj3VzBCp-fuse-angularjs-material-design-admin-template/fuse`
- **Destino:** `src/app/views/dashboard/student/`

## Archivos de referencia — LÉELOS TODOS PRIMERO

### Patrón de arquitectura (setup/module):
- `src/app/views/dashboard/setup/module/components/module-container.component.ts`
- `src/app/views/dashboard/setup/module/components/module-list.component.ts`
- `src/app/views/dashboard/setup/module/components/module-new.component.ts`
- `src/app/views/dashboard/setup/module/components/module-edit.component.ts`
- `src/app/views/dashboard/setup/module/models/module.ts`

### Archivos existentes a leer para contexto:
- `src/app/views/dashboard/student/models/student.models.ts`
- `src/app/views/dashboard/student/services/programa-doctorado.service.ts`
- `src/app/views/dashboard/student/student.routes.ts`

---

## Backend — Endpoints disponibles

| Método | URL | Respuesta |
|--------|-----|-----------|
| GET | `/api/v1/centros-laborales?page=0&size=20&search=` | `PaginatedResponse<CentroLaboral>` directo |
| GET | `/api/v1/centros-laborales/{id}` | `ApiResponse<CentroLaboral>` |
| GET | `/api/v1/centros-laborales/codigo/{codigoSistema}` | `ApiResponse<CentroLaboral>` |
| POST | `/api/v1/centros-laborales` | `ApiResponse<CentroLaboral>` 201 |
| PUT | `/api/v1/centros-laborales/{id}` | `ApiResponse<CentroLaboral>` |
| DELETE | `/api/v1/centros-laborales/{id}` | `{ message, id }` |

### Interfaces TypeScript

```typescript
export interface CentroLaboral {
  id: string;
  codigoSistema: string;
  nombre: string;
  descripcion: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
export interface CentroLaboralRequest { nombre: string; descripcion?: string; }
export type CentroLaboralUpdate = Partial<CentroLaboralRequest>;
```

---

## PASO 1 — Agregar interfaces a `models/student.models.ts`

Añade `CentroLaboral`, `CentroLaboralRequest`, `CentroLaboralUpdate` y `PaginatedResponse<T>` (si no existe).

---

## PASO 2 — Crear `services/centro-laboral.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class CentroLaboralService extends ApiService {
  private readonly url = `${this.base}/api/v1/centros-laborales`;

  // Paginado: devuelve PaginatedResponse DIRECTO (sin ApiResponse wrapper) — NO uses unwrap()
  // El array de items está en res.content, igual que module-container usa res.content
  getWithQuery$(params: any): Observable<PaginatedResponse<CentroLaboral>> {
    return this.http.get<PaginatedResponse<CentroLaboral>>(this.url, { params });
  }

  // Individual: devuelve ApiResponse — SÍ usa unwrap()
  getById(id: string): Observable<CentroLaboral> {
    return this.unwrap(this.http.get<ApiResponse<CentroLaboral>>(`${this.url}/${id}`));
  }
  create(dto: CentroLaboralRequest): Observable<CentroLaboral> {
    return this.unwrap(this.http.post<ApiResponse<CentroLaboral>>(this.url, dto));
  }
  update(id: string, dto: CentroLaboralUpdate): Observable<CentroLaboral> {
    return this.unwrap(this.http.put<ApiResponse<CentroLaboral>>(`${this.url}/${id}`, dto));
  }
  delete(id: string): Observable<void> {
    return this.unwrap(this.http.delete<ApiResponse<void>>(`${this.url}/${id}`));
  }
}
```

---

## PASO 3 — Crear `components/centro-laboral/centro-laboral-container.component.ts`

Copia **exactamente** la estructura de `module-container.component.ts`. Adaptaciones:

- `paginatedResponse = signal<PaginatedResponse<CentroLaboral>>({ content: [] })`
- `visibleItems = signal<CentroLaboral[]>([])`
- `filterForm = _fb.group({ search: [''] })` + `filterForm.valueChanges.subscribe(() => { currentPage.set(0); load(); })`

### Método `load()` — idéntico al patrón de module-container

```typescript
load(): void {
  this.loading.set(true);
  const params = { page: this.currentPage(), size: this.pageSize(), ...this.filterForm.value };
  this._svc.getWithQuery$(params).subscribe({
    next: (res) => {
      this.paginatedResponse.set(res);       // contiene totalElements, totalPages, etc.
      this.loading.set(false);
      this._revealRows(res.content ?? []);   // res.content es el array de CentroLaboral[]
    },
    error: () => this.loading.set(false),
  });
}
```

Template:
- `page-toolbar`: input con `formControlName="search"`, contador `paginatedResponse().totalElements`
- `page-content`: `<app-centro-laboral-list [items]="visibleItems()" [highlightedId]="highlightedId()" (eventEdit)="onEdit($event)" (eventDelete)="onDelete($event)" />`
- `page-footer`: `<pagination-controls [totalItems]="paginatedResponse().totalElements ?? 0" [itemsPerPage]="pageSize()" [currentPage]="currentPage()" (paginationChange)="onPageChange($event)" />`

Breadcrumb: `Posgrado > Centros Laborales` | Título: `Centros Laborales` | Botón: `Nuevo centro`

---

## PASO 4 — Crear `components/centro-laboral/centro-laboral-list.component.ts`

Copia la estructura de `module-list.component.ts`. Adaptaciones:

```typescript
@Input() items: CentroLaboral[] = [];
@Input() highlightedId: string | null = null;
@Output() eventEdit = new EventEmitter<string>();
@Output() eventDelete = new EventEmitter<string>();
```

Columnas desktop: `#` | `Código` | `Nombre` | `Descripción` | `Estado` | `Acciones`

```html
<td class="font-mono text-[11px] text-slate-400">{{ item.codigoSistema }}</td>
<td class="font-medium text-slate-700">{{ item.nombre }}</td>
<td class="text-slate-500 text-sm max-w-[250px] truncate">{{ item.descripcion ?? '—' }}</td>
```

Empty state: `Sin centros laborales registrados` / `Crea el primero con "Nuevo centro"` / ícono `building-office`

---

## PASO 5 — Crear `components/centro-laboral/centro-laboral-new.component.ts`

Copia la estructura de `module-new.component.ts`. Adaptaciones:

- `constructor(@Inject(MAT_DIALOG_DATA) public data: void) {}`
- Título: `Nuevo centro laboral` | Subtítulo: `Completa la información del centro laboral`
- Campos:
  - `nombre` — required, min 2, max 200, placeholder `HOSPITAL NACIONAL DOS DE MAYO`; nota: "Se normalizará a mayúsculas"
  - `descripcion` — opcional, max 500, `<textarea rows="3">`
- Botón submit: `Crear centro`
- `save()`: `this.dialogRef.close(this.form.value)`
- `[class.anim-shake]="shaking()"` + `_shake()` con doble RAF

---

## PASO 6 — Crear `components/centro-laboral/centro-laboral-edit.component.ts`

Copia la estructura de `module-edit.component.ts`. Adaptaciones:

- `constructor(@Inject(MAT_DIALOG_DATA) public data: CentroLaboral) {}`
- Pre-carga: `nombre: [data.nombre ?? '']`, `descripcion: [data.descripcion ?? '']`
- Título: `Editar centro laboral` | Subtítulo: `Modificando: <span>{{ data.nombre }}</span>`
- Footer izquierda: `ID: <code>{{ data.id }}</code>`
- `save()`: `this.dialogRef.close({ ...this.form.value, id: this.data.id })`

---

## PASO 7 — Crear `components/centro-laboral/centro-laboral-routers.ts`

```typescript
import { Routes } from '@angular/router';
const routes: Routes = [{
  path: '',
  loadComponent: () => import('./centro-laboral-container.component').then(m => m.CentroLaboralContainerComponent),
}];
export default routes;
```

---

## PASO 8 — Actualizar `student.routes.ts`

Añade:

```typescript
{
  path: 'centros-laborales',
  loadChildren: () => import('./components/centro-laboral/centro-laboral-routers'),
},
```

---

## Resultado esperado

- `/admin/student/centros-laborales` → lista paginada con búsqueda reactiva (`filterForm.valueChanges`)
- `PaginationControlsComponent` en el footer
- Tabla en `CentroLaboralListComponent` separado con `@Input`/`@Output`
- Crear/Editar → diálogos con `anim-shake`
- Eliminar → `ConfirmDialogService.confirmDelete()`
- Columna `codigoSistema` visible (ej. `CL-A1B2C3D4`)