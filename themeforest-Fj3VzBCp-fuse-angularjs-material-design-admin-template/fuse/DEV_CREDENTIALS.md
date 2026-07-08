# Credenciales de desarrollo

> ⚠️ **SOLO PARA DESARROLLO.** Estos usuarios y contraseñas **no existen ni deben usarse en producción**.
> Se crean automáticamente por el seeder del backend únicamente cuando el perfil de Quarkus es `dev`
> (`app.seeders.dev-users: true`), y el panel del login solo se muestra en builds de desarrollo.
> **Rótalos o deshabilítalos antes de cualquier despliegue.**

## Contraseña

Los 20 usuarios (10 estudiantes + 10 docentes) y los de gestión (coordinador, secretaría) usan:

```
contra123
```

El `admin` usa `admin123` (es el usuario bootstrap del sistema). Las contraseñas se guardan **hasheadas**
(SHA-256 + salt vía `PasswordEncoder`). **Usuario = `<primerapellido>.<primernombre>`** (minúsculas, sin tildes).

## Usuarios de gestión (aparte de los 20)

| Rol                      | Usuario        | Contraseña  |
|--------------------------|----------------|-------------|
| Administrador            | `admin`        | `admin123`  |
| Coordinador del Programa | `coordinador`  | `contra123` |
| Secretaría               | `secretaria`   | `contra123` |

## Docentes (10) — clave `contra123`

Los 3 primeros también son **tutores** (PROF_TUTOR); los 3 siguientes, **asesores** (ASESOR).

| Rol              | Usuario           |
|------------------|-------------------|
| Docente · Tutor  | `salazar.roberto` |
| Docente · Tutor  | `nunez.patricia`  |
| Docente · Tutor  | `paredes.carlos`  |
| Docente · Asesor | `caceres.silvia`  |
| Docente · Asesor | `rios.fernando`   |
| Docente · Asesor | `vega.marta`      |
| Docente          | `ledesma.andres`  |
| Docente          | `bravo.teresa`    |
| Docente          | `cabrera.julio`   |
| Docente          | `ponce.diana`     |

## Estudiantes (10) — clave `contra123`

| Usuario         | | Usuario         |
|-----------------|-|-----------------|
| `quispe.ana`    | | `vargas.pedro`  |
| `mamani.luis`   | | `rojas.lucia`   |
| `flores.carmen` | | `chavez.miguel` |
| `huaman.jose`   | | `ramos.elena`   |
| `condori.rosa`  | | `torres.jorge`  |

Email de acceso: `<usuario>@unmsm.edu.pe` · `status = ACTIVE`. **Sin relaciones sembradas**
(tutorías, temas, sugerencias): se crean desde la UI para probar los flujos.

## Cómo levantar el entorno para que se creen

1. **Backend** (`lamb-upeu-sis`), perfil `dev` (usa PostgreSQL en `localhost:5433`):

   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)
   ./gradlew quarkusDev
   ```

   Al iniciar, el seeder concilia los roles y crea (de forma **idempotente**) los usuarios de arriba.
   En el log verás: `✅ Usuarios de prueba (SOLO DEV): 2 gestión + 10 estudiantes + 10 docentes ... clave=contra123`.

2. **Frontend** (`fuse`):

   ```bash
   npm start   # ng serve (configuración development)
   ```

   En la pantalla de login aparece el panel **"Usuarios de prueba · Solo desarrollo"** con un botón
   **Usar** por fila que autocompleta e inicia sesión.

## Fuentes de verdad (mantener sincronizadas)

Si cambias esta lista, actualiza las **tres** copias:

1. Seeder backend: `lamb-upeu-sis/.../shared/utils/DatabaseSeeder.java` → `seedUsuariosPrueba` (`GESTION`, `ESTUDIANTES`, `DOCENTES`, `PRUEBA_PASSWORD`).
2. Frontend (solo dev): `fuse/src/environments/dev-credentials.ts` → `DEV_CREDENTIALS`.
3. Este documento.

## Producción

- El seeder **no** crea estos usuarios si `app.seeders.dev-users` no es `true` (por defecto `false`; el perfil `%prod` no lo activa).
- El build de producción de Angular reemplaza `environment.ts` por `environment.prod.ts`
  (`fileReplacements` en `angular.json`), con `showDevCredentials: false` y `devCredentials: []`,
  de modo que **ni el panel ni las contraseñas** quedan en el bundle de producción.
