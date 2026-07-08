# angular-estudiante

Actualiza el módulo **Estudiantes** para usar el endpoint paginado, FK como objetos, `codigoSistema`, y migrar a la arquitectura container + list separado con `PaginationControlsComponent` y `filterForm`.

## Rutas absolutas

- **Frontend:** `/Users/noe/Documents/ProyectoUNMS/themeforest-Fj3VzBCp-fuse-angularjs-material-design-admin-template/fuse`
- **Destino:** `src/app/views/dashboard/student/`

## Archivos de referencia — LÉELOS TODOS PRIMERO

### Patrón de arquitectura (setup/module):
- `src/app/views/dashboard/setup/module/components/module-container.component.ts`
- `src/app/views/dashboard/setup/module/components/module-list.component.ts`
- `src/app/views/dashboard/setup/module/components/module-new.component.ts`
- `src/app/views/dashboard/setup/module/components/module-edit.component.ts`

### Archivos existentes a modificar:
- `src/app/views/dashboard/student/models/student.models.ts`
- `src/app/views/dashboard/student/services/estudiante.service.ts`
- `src/app/views/dashboard/student/services/cargo.service.ts` *(implementar primero con `/angular-cargo`)*
- `src/app/views/dashboard/student/services/centro-laboral.service.ts` *(implementar primero con `/angular-centro-laboral`)*
- `src/app/views/dashboard/student/services/programa-doctorado.service.ts`
- `src/app/views/dashboard/student/components/estudiante/` (todos los archivos existentes)
- `src/app/views/dashboard/student/student.routes.ts`

> **Prerequisito:** Implementar `/angular-cargo` y `/angular-centro-laboral` primero.

---

## CAMBIOS EN EL BACKEND (ya implementados)

### Endpoint GET — ahora paginado con filtros

```
GET /api/v1/estudiantes?page=0&size=20&search=&programaDoctoradoId=&centroLaboralId=&cargoActualId=&condicion=
```

Respuesta: `PaginatedResponse<Estudiante>` directo (sin `ApiResponse` wrapper).

### FK ahora son objetos completos

- `centroLaboral: CentroLaboral | null` (antes era string)
- `cargoActual: Cargo | null` (antes era string)
- Request usa `centroLaboralId: string` y `cargoActualId: string` (UUIDs)

### Campo nuevo: `codigoSistema: string`

---

## PASO 1 — Actualizar `models/student.models.ts`

Actualiza la interfaz `Estudiante`:

```typescript
export interface Estudiante {
  id: string;
  codigoSistema: string;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  fechaNacimiento: string | null;
  dni: string | null;
  pasaporte: string | null;
  sexo: Sexo | null;
  estadoCivil: EstadoCivil | null;
  nacionalidad: string | null;
  discapacidad: boolean;
  celular: string | null;
  emailPersonal: string | null;
  emailInstitucional: string | null;
  programaDoctorado: ProgramaDoctorado | null;
  codMatricula: string | null;
  anioIngreso: number | null;
  financiamiento: Financiamiento | null;
  condicion: Condicion | null;
  centroLaboral: CentroLaboral | null;   // CAMBIO: antes era string
  cargoActual: Cargo | null;             // CAMBIO: antes era string
  procedencia: Procedencia | null;
  orcid: string | null;
  observaciones: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface EstudianteRequest {
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno?: string;
  fechaNacimiento?: string;
  dni?: string;
  pasaporte?: string;
  sexo?: Sexo;
  estadoCivil?: EstadoCivil;
  nacionalidad?: string;
  discapacidad?: boolean;
  celular?: string;
  emailPersonal?: string;
  emailInstitucional?: string;
  programaDoctoradoId?: string;
  codMatricula?: string;
  anioIngreso?: number;
  financiamiento?: Financiamiento;
  condicion?: Condicion;
  centroLaboralId?: string;   // CAMBIO: era centroLaboral: string
  cargoActualId?: string;     // CAMBIO: era cargoActual: string
  procedencia?: Procedencia;
  orcid?: string;
  observaciones?: string;
}

export interface EstudianteFiltros {
  page?: number;
  size?: number;
  search?: string;
  programaDoctoradoId?: string;
  centroLaboralId?: string;
  cargoActualId?: string;
  condicion?: Condicion;
}
```

---

## PASO 2 — Actualizar `services/estudiante.service.ts`

Reemplaza `getAll()` por `getWithQuery$` siguiendo el patrón de `ModuleService`:

```typescript
// Paginado: devuelve PaginatedResponse DIRECTO — NO uses unwrap()
// El container accede a res.content para los items (igual que module-container)
getWithQuery$(params: any): Observable<PaginatedResponse<Estudiante>> {
  return this.http.get<PaginatedResponse<Estudiante>>(this.url, { params });
}
```

Mantén `getById`, `getByCodMatricula`, `getByDni`, `create` (multipart), `update`, `delete` como están (usan `unwrap()` porque esos endpoints sí retornan `ApiResponse<T>`).

---

## PASO 3 — Reescribir `estudiante-container.component.ts`

Sigue **exactamente** el patrón de `module-container.component.ts`. Adaptaciones:

- `paginatedResponse = signal<PaginatedResponse<Estudiante>>({ content: [] })`
- `visibleItems = signal<Estudiante[]>([])`
- Servicios inyectados: `EstudianteService`, `ProgramaDoctoradoService`
- `filterForm = _fb.group({ search: [''], condicion: [''], programaDoctoradoId: [''] })`
- `filterForm.valueChanges.subscribe(() => { currentPage.set(0); load(); })`
- Carga listas de selección en `ngOnInit` (para el toolbar select de programas):
  ```typescript
  this._programaSvc.getWithQuery$({ page: 0, size: 100 }).subscribe(r => this.programas.set(r.content));
  ```

### Método `load()` — idéntico al patrón de module-container

```typescript
load(): void {
  this.loading.set(true);
  const params = { page: this.currentPage(), size: this.pageSize(), ...this.filterForm.value };
  // Limpia params vacíos para no enviar condicion='' al backend
  Object.keys(params).forEach(k => { if (!params[k]) delete params[k]; });
  this._svc.getWithQuery$(params).subscribe({
    next: (res) => {
      this.paginatedResponse.set(res);       // contiene totalElements, totalPages, etc.
      this.loading.set(false);
      this._revealRows(res.content ?? []);   // res.content es el array de Estudiante[]
    },
    error: () => this.loading.set(false),
  });
}
```

Template delegado a `EstudianteListComponent` + `PaginationControlsComponent` en `page-footer`.
Toolbar con 3 controles: búsqueda texto + select condición + select programa.

### Toolbar con filtros múltiples

```html
<div class="page-toolbar">
  <form [formGroup]="filterForm" class="flex flex-wrap gap-3 flex-1">
    <!-- Búsqueda -->
    <div class="toolbar-search">
      <mat-icon svgIcon="search" class="toolbar-search__icon" />
      <input formControlName="search" placeholder="Buscar nombre, DNI, matrícula…" />
    </div>

    <!-- Condición -->
    <mat-form-field appearance="outline" class="w-40" subscriptSizing="dynamic">
      <mat-select formControlName="condicion" placeholder="Condición">
        <mat-option value="">Todas</mat-option>
        <mat-option value="REGULAR">Regular</mat-option>
        <mat-option value="SANCIONADO">Sancionado</mat-option>
        <mat-option value="EGRESADO">Egresado</mat-option>
        <mat-option value="RETIRADO">Retirado</mat-option>
      </mat-select>
    </mat-form-field>

    <!-- Programa -->
    <mat-form-field appearance="outline" class="w-56" subscriptSizing="dynamic">
      <mat-select formControlName="programaDoctoradoId" placeholder="Programa">
        <mat-option value="">Todos</mat-option>
        @for (p of programas(); track p.id) {
          <mat-option [value]="p.id">{{ p.nombre }}</mat-option>
        }
      </mat-select>
    </mat-form-field>
  </form>
  @if (!loading()) {
    <span class="text-[11px] text-slate-400 anim-fade-in">
      {{ paginatedResponse().totalElements ?? 0 }} estudiante(s)
    </span>
  }
</div>
```

Breadcrumb: `Posgrado > Estudiantes` | Título: `Estudiantes` | Botón: `Nuevo estudiante`

---

## PASO 4 — Crear `estudiante-list.component.ts`

Copia la estructura de `module-list.component.ts`. Adaptaciones:

```typescript
@Input() items: Estudiante[] = [];
@Input() highlightedId: string | null = null;
@Output() eventEdit = new EventEmitter<string>();
@Output() eventDelete = new EventEmitter<string>();
```

Columnas desktop: `#` | `Código` | `Nombres` | `DNI` | `Programa` | `Centro Laboral` | `Cargo` | `Condición` | `Acciones`

```html
<td class="font-mono text-[11px] text-slate-400">{{ item.codigoSistema }}</td>
<td class="font-medium text-slate-700">{{ item.nombres }} {{ item.apellidoPaterno }}</td>
<td>{{ item.dni ?? '—' }}</td>
<td class="text-[11px] text-slate-500">{{ item.programaDoctorado?.nombre ?? '—' }}</td>
<td class="text-[11px] text-slate-500">{{ item.centroLaboral?.nombre ?? '—' }}</td>
<td class="text-[11px] text-slate-500">{{ item.cargoActual?.nombre ?? '—' }}</td>
<td>
  <span class="status-badge" [class.status-badge--active]="item.condicion === 'REGULAR'">
    {{ item.condicion ?? '—' }}
  </span>
</td>
```

Empty state: `Sin estudiantes registrados` | ícono `user-circle`

---

## PASO 5 — Actualizar `estudiante-new.component.ts`

Copia la estructura de `module-new.component.ts`. Adaptaciones:

- `constructor(@Inject(MAT_DIALOG_DATA) public data: { programas: ProgramaDoctorado[], centros: CentroLaboral[], cargos: Cargo[] }) {}`
- En `ngOnInit`: inicializa el form completo
- Reemplaza campos de texto libre por `mat-select` para FK:
  ```html
  <!-- Centro laboral -->
  <mat-select formControlName="centroLaboralId" placeholder="Seleccionar centro laboral">
    @for (c of data.centros; track c.id) {
      <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
    }
  </mat-select>

  <!-- Cargo actual -->
  <mat-select formControlName="cargoActualId" placeholder="Seleccionar cargo">
    @for (c of data.cargos; track c.id) {
      <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
    }
  </mat-select>
  ```
- El container pasa las listas al abrir el diálogo:
  ```typescript
  this._dialog.open(EstudianteNewComponent, {
    ...DIALOG_BASE,
    data: { programas: this.programas(), centros: this.centros(), cargos: this.cargos() }
  })
  ```

---

## PASO 6 — Actualizar `estudiante-edit.component.ts`

Similar al new. Pre-carga FK IDs:
```typescript
centroLaboralId: [data.estudiante.centroLaboral?.id ?? null],
cargoActualId:   [data.estudiante.cargoActual?.id   ?? null],
programaDoctoradoId: [data.estudiante.programaDoctorado?.id ?? null],
```

`constructor(@Inject(MAT_DIALOG_DATA) public data: { estudiante: Estudiante, programas: ProgramaDoctorado[], centros: CentroLaboral[], cargos: Cargo[] }) {}`

---

## Resultado esperado

- `/admin/student/estudiantes` → lista paginada con búsqueda + filtro condición + filtro programa
- `filterForm.valueChanges.subscribe()` para todos los filtros
- `PaginationControlsComponent` en el footer
- Tabla en `EstudianteListComponent` separado
- FK `centroLaboral` y `cargoActual` mostrados como nombres (no IDs)
- Crear/Editar: FK seleccionados con `mat-select` (no texto libre)
- Columna `codigoSistema` visible (ej. `EST-A1B2C3`)