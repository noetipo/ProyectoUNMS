# angular-programas-doctorado

Actualiza el módulo **Programas de Doctorado** para usar el endpoint paginado, añadir `codigoSistema`, y migrar a la arquitectura container + list separado usando `PaginationControlsComponent` y `filterForm`.

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
- `src/app/views/dashboard/setup/module/module-routers.ts`

### Archivos existentes a modificar:
- `src/app/views/dashboard/student/models/student.models.ts`
- `src/app/views/dashboard/student/services/programa-doctorado.service.ts`
- `src/app/views/dashboard/student/components/programa-doctorado/programa-doctorado-container.component.ts`
- `src/app/views/dashboard/student/components/programa-doctorado/programa-doctorado-new.component.ts`
- `src/app/views/dashboard/student/components/programa-doctorado/programa-doctorado-edit.component.ts`
- `src/app/views/dashboard/student/student.routes.ts`

---

## Backend — Endpoints disponibles

| Método | URL | Respuesta |
|--------|-----|-----------|
| GET | `/api/v1/programas-doctorado?page=0&size=20&search=` | `PaginatedResponse<ProgramaDoctorado>` directo |
| GET | `/api/v1/programas-doctorado/{id}` | `ApiResponse<ProgramaDoctorado>` |
| GET | `/api/v1/programas-doctorado/codigo/{codigoSistema}` | `ApiResponse<ProgramaDoctorado>` |
| POST | `/api/v1/programas-doctorado` | `ApiResponse<ProgramaDoctorado>` 201 |
| PUT | `/api/v1/programas-doctorado/{id}` | `ApiResponse<ProgramaDoctorado>` |
| DELETE | `/api/v1/programas-doctorado/{id}` | `{ message, id }` |

---

## PASO 1 — Actualizar `models/student.models.ts`

Añade `codigoSistema: string` a la interfaz `ProgramaDoctorado`.
Añade `PaginatedResponse<T>` si no existe:

```typescript
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}
```

---

## PASO 2 — Actualizar `services/programa-doctorado.service.ts`

Reemplaza `getAll()` por `getWithQuery$(params)` siguiendo el mismo patrón de `ModuleService`:

```typescript
// El endpoint paginado devuelve PaginatedResponse DIRECTO (sin ApiResponse wrapper)
// NO uses this.unwrap() aquí
getWithQuery$(params: any): Observable<PaginatedResponse<ProgramaDoctorado>> {
  return this.http.get<PaginatedResponse<ProgramaDoctorado>>(this.url, { params });
}
```

> El `content` del `PaginatedResponse` contiene el array de items. Los otros métodos (getById, create, update, delete) siguen usando `this.unwrap()` ya que esos endpoints sí retornan `ApiResponse<T>`.

---

## PASO 3 — Reescribir `programa-doctorado-container.component.ts`

Sigue **exactamente** el patrón de `module-container.component.ts`. Diferencias clave:

- `paginatedResponse = signal<PaginatedResponse<ProgramaDoctorado>>({ content: [] })`
- `visibleItems = signal<ProgramaDoctorado[]>([])` — los items se revelan con stagger desde `content`
- `filterForm = _fb.group({ search: [''] })` + `filterForm.valueChanges.subscribe(() => { currentPage.set(0); load(); })`

### Método `load()` — igual que module-container

```typescript
load(): void {
  this.loading.set(true);
  const params = { page: this.currentPage(), size: this.pageSize(), ...this.filterForm.value };
  this._svc.getWithQuery$(params).subscribe({
    next: (res) => {
      this.paginatedResponse.set(res);          // guarda totalElements, totalPages, etc.
      this.loading.set(false);
      this._revealRows(res.content ?? []);      // res.content es el array de items
    },
    error: () => this.loading.set(false),
  });
}
```

> `res.content` contiene el array de `ProgramaDoctorado[]`. El template usa `visibleItems()` (llenado por stagger) y `paginatedResponse().totalElements` para el contador y la paginación.

### Template

- `page-toolbar`: `<input formControlName="search" placeholder="Buscar programa…" />`
- `page-content`: `<app-programa-doctorado-list [items]="visibleItems()" [highlightedId]="highlightedId()" (eventEdit)="onEdit($event)" (eventDelete)="onDelete($event)" />`
- `page-footer`: `<pagination-controls [totalItems]="paginatedResponse().totalElements ?? 0" [itemsPerPage]="pageSize()" [currentPage]="currentPage()" (paginationChange)="onPageChange($event)" />`

Breadcrumb: `Posgrado > Programas de Doctorado` | Título: `Programas de Doctorado` | Botón: `Nuevo programa`

---

## PASO 4 — Crear `programa-doctorado-list.component.ts`

Sigue **exactamente** el patrón de `module-list.component.ts`. Diferencias:

```typescript
@Input() items: ProgramaDoctorado[] = [];
@Input() highlightedId: string | null = null;
@Output() eventEdit = new EventEmitter<string>();
@Output() eventDelete = new EventEmitter<string>();
```

Columnas: `#` | `Código` | `Nombre` | `Descripción` | `Estado` | `Acciones`

```html
<td class="font-mono text-[11px] text-slate-400">{{ item.codigoSistema }}</td>
<td class="font-medium text-slate-700">{{ item.nombre }}</td>
<td class="text-slate-500 text-sm max-w-[250px] truncate">{{ item.descripcion ?? '—' }}</td>
```

Empty state: `Sin programas registrados` / `Crea el primero con "Nuevo programa"` / ícono `academic-cap`

---

## PASO 5 — Actualizar `programa-doctorado-new.component.ts`

Sigue el patrón de `module-new.component.ts`. Mantiene la misma lógica de formulario:
- `form = _fb.group({ nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(200)]], descripcion: ['', Validators.maxLength(500)] })`
- `[class.anim-shake]="shaking()"` en el div raíz
- `_shake()` con doble `requestAnimationFrame`
- Campos: `nombre` (required, placeholder `DOCTORADO EN MEDICINA`) + `descripcion` (textarea rows=3)
- Nota bajo nombre: "Se normalizará a mayúsculas"

---

## PASO 6 — Actualizar `programa-doctorado-edit.component.ts`

Sigue el patrón de `module-edit.component.ts`:
- `constructor(@Inject(MAT_DIALOG_DATA) public data: ProgramaDoctorado) {}`
- Pre-carga: `nombre: [data.nombre ?? '']`, `descripcion: [data.descripcion ?? '']`
- Footer: muestra `ID: <code>{{ data.id }}</code>` a la izquierda
- `save()`: cierra con `{ ...this.form.value, id: this.data.id }`

---

## PASO 7 — Verificar `programa-doctorado-routers.ts`

El archivo ya existe. Verifica que cargue `ProgramaDoctoradoContainerComponent`. Si usa el nombre antiguo, actualízalo.

---

## Resultado esperado

- `/admin/student/programas` → lista con búsqueda reactiva y `PaginationControlsComponent`
- Columna `codigoSistema` en tabla
- Tabla delegada a `ProgramaDoctoradoListComponent`
- Search: `filterForm.valueChanges.subscribe()` (no evento `(input)`)
- Paginación: `<pagination-controls ... (paginationChange)="onPageChange($event)" />`