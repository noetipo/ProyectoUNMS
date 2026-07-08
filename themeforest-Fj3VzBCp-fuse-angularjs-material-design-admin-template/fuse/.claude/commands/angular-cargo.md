# angular-cargo

Implementa el módulo **Cargos** desde cero siguiendo el patrón de `setup/module`.

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
| GET | `/api/v1/cargos?page=0&size=20&search=` | `PaginatedResponse<Cargo>` directo |
| GET | `/api/v1/cargos/{id}` | `ApiResponse<Cargo>` |
| GET | `/api/v1/cargos/codigo/{codigoSistema}` | `ApiResponse<Cargo>` |
| POST | `/api/v1/cargos` | `ApiResponse<Cargo>` 201 |
| PUT | `/api/v1/cargos/{id}` | `ApiResponse<Cargo>` |
| DELETE | `/api/v1/cargos/{id}` | `{ message, id }` |

### Interfaces TypeScript

```typescript
export interface Cargo {
  id: string;
  codigoSistema: string;
  nombre: string;
  descripcion: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
export interface CargoRequest { nombre: string; descripcion?: string; }
export type CargoUpdate = Partial<CargoRequest>;
```

---

## PASO 1 — Agregar interfaces a `models/student.models.ts`

Añade las interfaces `Cargo`, `CargoRequest`, `CargoUpdate` y `PaginatedResponse<T>` (si no existe).

---

## PASO 2 — Crear `services/cargo.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class CargoService extends ApiService {
  private readonly url = `${this.base}/api/v1/cargos`;

  // Paginado: devuelve PaginatedResponse DIRECTO (sin ApiResponse wrapper) — NO uses unwrap()
  // El array de items está en res.content, igual que module-container usa res.content
  getWithQuery$(params: any): Observable<PaginatedResponse<Cargo>> {
    return this.http.get<PaginatedResponse<Cargo>>(this.url, { params });
  }

  // Individual: devuelve ApiResponse — SÍ usa unwrap()
  getById(id: string): Observable<Cargo> {
    return this.unwrap(this.http.get<ApiResponse<Cargo>>(`${this.url}/${id}`));
  }
  create(dto: CargoRequest): Observable<Cargo> {
    return this.unwrap(this.http.post<ApiResponse<Cargo>>(this.url, dto));
  }
  update(id: string, dto: CargoUpdate): Observable<Cargo> {
    return this.unwrap(this.http.put<ApiResponse<Cargo>>(`${this.url}/${id}`, dto));
  }
  delete(id: string): Observable<void> {
    return this.unwrap(this.http.delete<ApiResponse<void>>(`${this.url}/${id}`));
  }
}
```

---

## PASO 3 — Crear `components/cargo/cargo-container.component.ts`

Copia **exactamente** la estructura de `module-container.component.ts`. Adaptaciones:

- `paginatedResponse = signal<PaginatedResponse<Cargo>>({ content: [] })`
- `visibleItems = signal<Cargo[]>([])`
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
      this._revealRows(res.content ?? []);   // res.content es el array de Cargo[]
    },
    error: () => this.loading.set(false),
  });
}
```

Template:
- `page-toolbar`: input con `formControlName="search"`, contador `paginatedResponse().totalElements`
- `page-content`: `<app-cargo-list [items]="visibleItems()" [highlightedId]="highlightedId()" (eventEdit)="onEdit($event)" (eventDelete)="onDelete($event)" />`
- `page-footer`: `<pagination-controls [totalItems]="paginatedResponse().totalElements ?? 0" [itemsPerPage]="pageSize()" [currentPage]="currentPage()" (paginationChange)="onPageChange($event)" />`

Breadcrumb: `Posgrado > Cargos` | Título: `Cargos Laborales` | Botón: `Nuevo cargo`

---

## PASO 4 — Crear `components/cargo/cargo-list.component.ts`

Copia la estructura de `module-list.component.ts`. Adaptaciones:

```typescript
@Input() items: Cargo[] = [];
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

Empty state: `Sin cargos registrados` / `Crea el primero con "Nuevo cargo"` / ícono `briefcase`

Mobile cards: muestra `codigoSistema` (mono), `nombre`, botones editar/eliminar.

---

## PASO 5 — Crear `components/cargo/cargo-new.component.ts`

Copia la estructura de `module-new.component.ts`. Adaptaciones:

- `constructor(@Inject(MAT_DIALOG_DATA) public data: void) {}`  (no recibe datos)
- Título: `Nuevo cargo` | Subtítulo: `Completa la información del cargo laboral`
- Campos:
  - `nombre` — required, min 2, max 200, placeholder `MEDICO ASISTENTE`; nota debajo: "Se normalizará a mayúsculas"
  - `descripcion` — opcional, max 500, `<textarea rows="3">`
- Botón submit: `Crear cargo`
- `save()`: `this.dialogRef.close(this.form.value)`
- `[class.anim-shake]="shaking()"` + `_shake()` con doble RAF

---

## PASO 6 — Crear `components/cargo/cargo-edit.component.ts`

Copia la estructura de `module-edit.component.ts`. Adaptaciones:

- `constructor(@Inject(MAT_DIALOG_DATA) public data: Cargo) {}`
- Pre-carga: `nombre: [data.nombre ?? '']`, `descripcion: [data.descripcion ?? '']`
- Título: `Editar cargo` | Subtítulo: `Modificando: <span>{{ data.nombre }}</span>`
- Footer izquierda: `ID: <code>{{ data.id }}</code>`
- `save()`: `this.dialogRef.close({ ...this.form.value, id: this.data.id })`

---

## PASO 7 — Crear `components/cargo/cargo-routers.ts`

```typescript
import { Routes } from '@angular/router';
const routes: Routes = [{
  path: '',
  loadComponent: () => import('./cargo-container.component').then(m => m.CargoContainerComponent),
}];
export default routes;
```

---

## PASO 8 — Actualizar `student.routes.ts`

Añade:

```typescript
{
  path: 'cargos',
  loadChildren: () => import('./components/cargo/cargo-routers'),
},
```

---

## Resultado esperado

- `/admin/student/cargos` → lista paginada con búsqueda reactiva (`filterForm.valueChanges`)
- `PaginationControlsComponent` en el footer
- Tabla en `CargoListComponent` separado con `@Input`/`@Output`
- Crear/Editar → diálogos con `anim-shake`
- Eliminar → `ConfirmDialogService.confirmDelete()`
- Columna `codigoSistema` visible (ej. `CAR-A1B2C3D4`)