# nuevo-crud

Genera un CRUD completo en el proyecto **lamb-upeu-sis** (Quarkus + Clean Architecture).
Lee primero los archivos existentes para respetar los patrones del proyecto, luego ejecuta los pasos en orden.

## Descripción del modelo

$ARGUMENTS

---

## CONTEXTO DEL PROYECTO (léelo antes de generar)

- **Package base:** `unmsm.edu.pe`
- **Módulo student:** `unmsm.edu.pe.student`
- **Módulo security (DTOs compartidos):** `unmsm.edu.pe.security.application.dto.PaginatedResponseDto`
- **Build:** Gradle + Quarkus, Java 21, Hibernate ORM Panache, MapStruct CDI, Lombok
- **Referencia de entidad:** `src/main/java/unmsm/edu/pe/student/domain/entities/CentroLaboral.java`
- **Referencia de repositorio:** `src/main/java/unmsm/edu/pe/student/domain/repositories/CentroLaboralRepository.java`
- **Referencia de repositorio impl:** `src/main/java/unmsm/edu/pe/student/domain/repositories/impl/CentroLaboralRepositoryImpl.java`
- **Referencia de servicio:** `src/main/java/unmsm/edu/pe/student/domain/services/CentroLaboralService.java`
- **Referencia de servicio impl:** `src/main/java/unmsm/edu/pe/student/domain/services/impl/CentroLaboralServiceImpl.java`
- **Referencia de mapper:** `src/main/java/unmsm/edu/pe/student/application/mapper/CentroLaboralMapper.java`
- **Referencia de controller:** `src/main/java/unmsm/edu/pe/student/infrastructure/web/CentroLaboralController.java`
- **Referencia de test:** `src/test/java/unmsm/edu/pe/student/domain/services/impl/CentroLaboralServiceImplTest.java`
- **Seeder:** `src/main/java/unmsm/edu/pe/shared/utils/DatabaseSeeder.java`

Lee los archivos de referencia antes de generar. No inventes patrones nuevos.

---

## PASO 1 — ENTIDAD

Crea `src/main/java/unmsm/edu/pe/student/domain/entities/{Entidad}.java`

Reglas:
- `@Entity @Table(name = "{tabla_plural}")` 
- Extiende `AuditableEntity` con `@EntityListeners(AuditListener.class)`
- Lombok: `@Data @Builder @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(callSuper = true)`
- `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})`
- PK: `@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;`
- Campo `codigoSistema`: `@Column(name="codigo_sistema", unique=true, nullable=false, updatable=false, length=20) private String codigoSistema;`
- Prefijo del código: usa las iniciales de la entidad en mayúsculas + `-` (ej: `TL-` para TipoLab). Genera 7-8 chars UUID hex en `@PrePersist`.
- Campos propios del modelo indicados en la descripción.
- Relaciones FK con `@ManyToOne(fetch=FetchType.LAZY)` si aplica.
- Normalización de texto con `@Normalize(NormalizationType.UPPERCASE)` en nombre/código o `TITLE_CASE` en nombres propios (importa desde `unmsm.edu.pe.shared.annotations`).

Patrón `@PrePersist`:
```java
@PrePersist
private void generarCodigoSistema() {
    if (this.codigoSistema == null || this.codigoSistema.isBlank()) {
        this.codigoSistema = "XX-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
    }
}
```

---

## PASO 2 — DTOs

Crea en `src/main/java/unmsm/edu/pe/student/application/dto/`:

**`{Entidad}RequestDto.java`**
- `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- Campos con validaciones Jakarta (`@NotBlank`, `@Size`, `@NotNull`, etc.) según el modelo.
- Los campos FK llevan `UUID {campo}Id` (no el objeto completo).
- NO incluye `id` ni `codigoSistema`.

**`{Entidad}ResponseDto.java`**
- `@Data @NoArgsConstructor @AllArgsConstructor`
- Incluye todos los campos + `UUID id` + `String codigoSistema`.
- Los campos FK incluyen el ResponseDto completo del relacionado (no solo el UUID).

**`{Entidad}UpdateDto.java`**
- Igual que RequestDto pero todos los campos son opcionales (`@Size` sin `@NotBlank`).

---

## PASO 3 — MAPPER

Crea `src/main/java/unmsm/edu/pe/student/application/mapper/{Entidad}Mapper.java`

- `@Mapper(componentModel = "cdi")` — sin `uses=` si no hay FK, con `uses={OtroMapper.class}` si hay FK.
- `{Entidad}ResponseDto toResponseDto({Entidad} entity);`
- `List<{Entidad}ResponseDto> toResponseDtoList(List<{Entidad}> list);`
- `toEntity`: ignora `id`, `codigoSistema`, `active`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`. Ignora también los campos de FK objetos (se resuelven en el service).
- `updateEntityFromDto`: igual + `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`.

---

## PASO 4 — REPOSITORIO (interfaz + impl)

**Interfaz** `src/main/java/unmsm/edu/pe/student/domain/repositories/{Entidad}Repository.java`

Métodos estándar:
```java
List<{Entidad}> getAll();
Optional<{Entidad}> getById(UUID id);
Optional<{Entidad}> findByNombre(String nombre);
Optional<{Entidad}> findByCodigoSistema(String codigoSistema);
{Entidad} save({Entidad} entity);
void removeById(UUID id);
boolean existsByNombre(String nombre);
boolean existsByCodigoSistema(String codigoSistema);
PanacheQuery<{Entidad}> findWithFilters(String search);
```
Agrega métodos extra si el modelo los requiere (buscar por campo específico, filtrar por FK, etc.).

**Implementación** `src/main/java/unmsm/edu/pe/student/domain/repositories/impl/{Entidad}RepositoryImpl.java`

- `@ApplicationScoped` implementa la interfaz.
- Extiende `PanacheRepositoryBase<{Entidad}, UUID>`.
- `findWithFilters`: JPQL dinámico con `HashMap<String, Object>` para parámetros. Filtra por `nombre` y `codigoSistema` con `LIKE LOWER(...)`. Si hay FK, permite filtrar por ID del relacionado.
- `save`: llama `persistAndFlush` si `entity.getId()==null`, sino `getEntityManager().merge(entity)`.

---

## PASO 5 — SERVICIO (interfaz + impl)

**Interfaz** `src/main/java/unmsm/edu/pe/student/domain/services/{Entidad}Service.java`

```java
List<{Entidad}ResponseDto> findAll();
PaginatedResponseDto<{Entidad}ResponseDto> findAllPaged(Integer page, Integer size, String search);
{Entidad}ResponseDto findById(UUID id);
{Entidad}ResponseDto findByCodigoSistema(String codigoSistema);
{Entidad}ResponseDto create({Entidad}RequestDto dto);
{Entidad}ResponseDto update(UUID id, {Entidad}UpdateDto dto);
void deleteById(UUID id);
```
Agrega métodos extra si la lógica del modelo lo requiere.

**Implementación** `src/main/java/unmsm/edu/pe/student/domain/services/impl/{Entidad}ServiceImpl.java`

- `@ApplicationScoped @Transactional`
- `findAllPaged`: llama `repository.findWithFilters(search).page(Page.of(page, size))` → construye `PaginatedResponseDto`.
- `create`: valida unicidad (`existsByNombre`) → `NotFoundException` si no existe un FK → `BusinessException` si viola unicidad.
- `update`: busca existente → valida unicidad si cambió → aplica mapper → resuelve FKs → save.
- `deleteById`: verifica existencia → `removeById`.
- Lanza `unmsm.edu.pe.shared.exceptions.NotFoundException` y `BusinessException` según corresponda.
- Si hay FKs, inyecta los repositorios correspondientes con `@Inject`.

---

## PASO 6 — CONTROLLER

Crea `src/main/java/unmsm/edu/pe/student/infrastructure/web/{Entidad}Controller.java`

- `@Path("/api/v1/{ruta-kebab-case}")` `@Produces(APPLICATION_JSON)` `@Consumes(APPLICATION_JSON)`
- `@Tag(name=..., description=...)`
- Logger: `private static final Logger LOG = Logger.getLogger({Entidad}Controller.class);`
- Endpoints:
  - `GET /` — paginado: `@QueryParam("page") @DefaultValue("0")`, `@QueryParam("size") @DefaultValue("10")`, `@QueryParam("search")`. Retorna `PaginatedResponseDto` directo (sin envolver en ApiResponse).
  - `GET /{id}` — por UUID. Envuelve en `ApiResponse.success(...)`.
  - `GET /codigo/{codigoSistema}` — por código.
  - `POST /` — `@Valid`, retorna 201.
  - `PUT /{id}` — `@Valid`.
  - `DELETE /{id}` — retorna `Map.of("message", ..., "id", ...)`.
- Manejo de errores con try-catch y LOG.error en cada endpoint.

---

## PASO 7 — SEEDER DE DATOS

En `src/main/java/unmsm/edu/pe/shared/utils/DatabaseSeeder.java`:

1. Crea un nuevo método privado `seedNombreEntidades()` después de la última fase.
2. Guarda en el `onStart` como `FASE N+1`.
3. El método usa un array de datos representativos (mínimo 5 registros realistas según el modelo).
4. Patrón:
```java
private void seed{Entidades}() {
    int created = 0, existing = 0;
    String[][] data = { {"nombre1","descripcion1"}, ... };
    for (String[] d : data) {
        if ({entidad}Repository.existsByNombre(d[0])) { existing++; continue; }
        {Entidad} e = new {Entidad}();
        e.setNombre(d[0]);
        e.setDescripcion(d[1]);
        {entidad}Repository.save(e);
        created++;
    }
    LOG.info("📊 Resumen: " + created + " creados, " + existing + " existentes");
}
```
5. Inyecta el repositorio en la sección `@Inject` del seeder si no está aún.

---

## PASO 8 — SEEDER DE MENÚ

En `DatabaseSeeder.java`, dentro del array `ModuleData[] data`:

Agrega al final (antes del `};`):
```java
// {NOMBRE SECCIÓN} (03)
new ModuleData("NN", "{Título del módulo}", "", "basic", "heroicons_outline:{icon}",
        "/admin/student/{ruta}", N, "03"),
```

- El código `"NN"` debe ser el siguiente disponible (actualmente el último es `"12"`, así que usa `"13"`, `"14"`, etc.).
- El `"03"` es el parent module "Gestión Doctorado".
- La ruta sigue el patrón `/admin/student/{ruta-kebab}`.
- Elige el ícono HeroIcons más apropiado para el concepto.

---

## PASO 9 — UNIT TESTS

Crea `src/test/java/unmsm/edu/pe/student/domain/services/impl/{Entidad}ServiceImplTest.java`

Sigue exactamente el patrón de `CentroLaboralServiceImplTest.java`. Incluye:

- `@ExtendWith(MockitoExtension.class)`
- `@Mock {Entidad}Repository repository` + `@Mock {Entidad}Mapper mapper`
- `@InjectMocks {Entidad}ServiceImpl service`
- `@BeforeEach setUp()`: crea fixtures reutilizables (entity, requestDto, responseDto, updateDto).
- Tests mínimos obligatorios:
  1. `findAll_listaVacia_retornaListaVacia()`
  2. `findAll_conRegistros_retornaLista()`
  3. `findById_encontrado_retornaDto()`
  4. `findById_noEncontrado_lanzaNotFoundException()`
  5. `findByCodigoSistema_encontrado_retornaDto()`
  6. `findByCodigoSistema_noEncontrado_lanzaNotFoundException()`
  7. `create_nombreUnico_guardaYRetornaDto()`
  8. `create_nombreDuplicado_lanzaBusinessException()`
  9. `update_exitoso_actualizaYRetorna()`
  10. `update_mismoNombre_noVerificaDuplicado()`
  11. `update_nombreDuplicado_lanzaBusinessException()`
  12. `update_noEncontrado_lanzaNotFoundException()`
  13. `delete_encontrado_eliminaCorrectamente()`
  14. `delete_noEncontrado_lanzaNotFoundException()`
- Agrega tests adicionales para la lógica específica indicada en la descripción del modelo.

Ejecuta `./gradlew test` al final y corrige cualquier error de compilación antes de reportar como completado.

---

## PASO 10 — PROMPT ANGULAR

### Ruta de destino

**Directorio Angular:** `/Users/noe/Documents/ProyectoUNMS/themeforest-Fj3VzBCp-fuse-angularjs-material-design-admin-template/fuse/src/app/views/dashboard/student/`

### Patrón de referencia obligatorio

Antes de generar, lee estos archivos del patrón existente en el mismo proyecto Angular:

- `src/app/views/dashboard/setup/module/components/module-container.component.ts` — arquitectura container
- `src/app/views/dashboard/setup/module/components/module-list.component.ts` — componente lista separado
- `src/app/views/dashboard/setup/module/components/module-new.component.ts` — diálogo crear
- `src/app/views/dashboard/setup/module/components/module-edit.component.ts` — diálogo editar
- `src/app/views/dashboard/setup/module/models/module.ts` — modelo e interfaces
- `src/app/views/dashboard/setup/module/module-routers.ts` — rutas lazy
- `src/app/views/dashboard/student/student.routes.ts` — para agregar la nueva ruta

### Arquitectura de componentes (igual que module)

Los archivos a crear siguen esta estructura:

```
student/components/{entidad-kebab}/
  {entidad}-container.component.ts   ← container (lista + toolbar + paginación)
  {entidad}-list.component.ts        ← tabla standalone con @Input/@Output
  {entidad}-new.component.ts         ← diálogo crear
  {entidad}-edit.component.ts        ← diálogo editar
  {entidad}-routers.ts               ← rutas lazy

student/services/{entidad}.service.ts
student/models/student.models.ts     ← agregar interfaces aquí (no crear nuevo archivo)
```

### Reglas de implementación

**Container** (`{entidad}-container.component.ts`):
- Mismo skeleton CSS que `module-container.component.ts`
- Template: `page` → `page-header` + `page-toolbar` + `page-content` + `page-footer`
- `filterForm = _fb.group({ search: [''] })` con `filterForm.valueChanges.subscribe(() => { currentPage.set(0); load(); })`
- `PaginationControlsComponent` importado de `@/app/shared/pagination-controls/pagination-controls.component`
- Paginación: `<pagination-controls [totalItems]="..." [itemsPerPage]="pageSize()" [currentPage]="currentPage()" (paginationChange)="onPageChange($event)" />`
- Stagger: `_revealRows(items)` + `_clearTimers()` + `ngOnDestroy`
- Delega la tabla a `{Entidad}ListComponent` con `[items]="visibleItems()" [highlightedId]="highlightedId()" (eventEdit)="onEdit($event)" (eventDelete)="onDelete($event)"`

**Lista** (`{entidad}-list.component.ts`):
- `@Input() items: {Entidad}[] = []`
- `@Input() highlightedId: string | null = null`
- `@Output() eventEdit = new EventEmitter<string>()`
- `@Output() eventDelete = new EventEmitter<string>()`
- Tabla desktop + cards mobile (mismas clases CSS que module-list)
- `[class.anim-flash]="item.id === highlightedId"`

**Diálogos** (new y edit):
- `[class.anim-shake]="shaking()"` en el div raíz
- `_shake()` con doble `requestAnimationFrame`
- Estructura: `form-dialog__header` + `form-dialog__body` + `form-dialog__footer`
- `form-section` con `style="animation-delay: Xms"` escalonado (40ms, 80ms, 120ms…)

**Servicio** (`student/services/{entidad}.service.ts`):
- `extends ApiService`
- `getWithQuery$(params: any)` — devuelve `PaginatedResponse<{Entidad}>` directo, **NO** usa `unwrap()`. Igual que `ModuleService.getWithQuery$`.
- `getById`, `create`, `update`, `delete` — con `this.unwrap(...)` y `ApiResponse<{Entidad}>` (esos sí llevan wrapper)

**Container `load()` — siempre igual al de module-container:**
```typescript
load(): void {
  this.loading.set(true);
  const params = { page: this.currentPage(), size: this.pageSize(), ...this.filterForm.value };
  this._svc.getWithQuery$(params).subscribe({
    next: (res) => {
      this.paginatedResponse.set(res);        // totalElements, totalPages, etc.
      this.loading.set(false);
      this._revealRows(res.content ?? []);    // res.content = array de items
    },
    error: () => this.loading.set(false),
  });
}
```

### Contenido del prompt a generar

Genera un bloque **`## PROMPT PARA ANGULAR`** con:

1. **Descripción** del módulo y sus campos.
2. **Endpoints** — tabla completa con método, URL, payload/respuesta.
3. **Interfaces TypeScript** para agregar en `models/student.models.ts`.
4. **Pasos de implementación** siguiendo la arquitectura arriba: container → list → new → edit → routers → service → actualizar routes.
5. **Integración con `student.routes.ts`** — la línea exacta a agregar.
6. **Lógica específica** del modelo (FKs, selects, filtros extra, etc.).

---

## ORDEN DE EJECUCIÓN

1. Lee los archivos de referencia.
2. Ejecuta los pasos 1-9 en orden, creando o modificando archivos.
3. Corre `export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew test` y corrige errores.
4. Muestra el **PROMPT ANGULAR** al final como bloque copiable.
5. Reporta un resumen de todos los archivos creados/modificados.