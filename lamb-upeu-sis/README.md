# lamb-upeu-sis

Sistema monolítico modular desarrollado con Quarkus que implementa arquitectura DDD (Domain Driven Design) con autenticación JWT personalizada.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/.

## 🏗️ Arquitectura

```
src/main/java/upeu/edu/pe/
├── catalog/                    # Módulo de Catálogo
│   ├── application/           # DTOs y casos de uso
│   ├── domain/               # Entidades y servicios de dominio
│   └── infrastructure/       # Controllers y repositorios
├── security/                  # Módulo de Seguridad
│   ├── application/          # DTOs de autenticación
│   ├── domain/              # Entidades User, RefreshToken
│   └── infrastructure/      # JWT, filtros, controllers
└── shared/                   # Componentes compartidos
    ├── annotations/         # Anotaciones personalizadas (@Normalize)
    ├── entities/           # AuditableEntity base
    ├── context/            # Contexto de auditoría
    └── utils/              # Utilidades generales
```

## 🚀 Configuración del Entorno

### Prerrequisitos

- Java 21 o superior
- PostgreSQL 13+
- OpenSSL (para generar claves RSA)
- Gradle 8+

### 1. Configuración de Base de Datos

```sql
-- Crear base de datos
CREATE DATABASE quarkus_db;
CREATE USER postgres WITH PASSWORD '12345678';
GRANT ALL PRIVILEGES ON DATABASE quarkus_db TO postgres;
```

### 2. Variables de Entorno

Crear archivo `.env` en la raíz del proyecto:

```bash
DB_USERNAME=postgres
DB_PASSWORD=12345678
DB_NAME=quarkus_db
```

### 3. Generar Claves RSA para JWT

El script que proporcionaste se ejecuta así:

#### En Linux/macOS:
```bash
# Hacer el script ejecutable
chmod +x generate-jwt-keys.sh

# Ejecutar el script
./generate-jwt-keys.sh
```

#### En Windows:
```powershell
# Usar Git Bash o WSL
bash generate-jwt-keys.sh
```

#### Manual (si no tienes OpenSSL):
```bash
# Crear directorio
mkdir -p src/main/resources/META-INF

# Generar claves usando keytool (viene con Java)
keytool -genkeypair -alias jwt -keyalg RSA -keysize 2048 \
    -keystore src/main/resources/META-INF/keystore.p12 \
    -storetype PKCS12 -storepass changeit -keypass changeit
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

### Quick Start para Desarrollo

```bash
# 1. Configurar base de datos PostgreSQL
createdb quarkus_db

# 2. Generar claves JWT (opcional para development)
./generate-jwt-keys.sh

# 3. Ejecutar en modo desarrollo
./gradlew quarkusDev

# 4. Acceder a la aplicación
# - API: http://localhost:8080
# - Swagger UI: http://localhost:8080/swagger-ui
# - Health Check: http://localhost:8080/health
# - Dev UI: http://localhost:8080/q/dev/
```

## 🔐 Sistema de Autenticación

### JWT Personalizado

El sistema usa implementación JWT personalizada con:
- Tokens de acceso (1 hora)
- Tokens de refresh (7 días)
- Validación por filtro personalizado
- Auditoría automática de entidades

### Endpoints de Autenticación

```bash
# Registro
POST /api/v1/auth/register
Content-Type: application/json
{
  "username": "usuario123",
  "email": "usuario@ejemplo.com", 
  "password": "password123",
  "firstName": "Juan",
  "lastName": "Pérez",
  "phone": "+51987654321",
  "role": "USER"
}

# Login
POST /api/v1/auth/login
Content-Type: application/json
{
  "username": "usuario123",
  "password": "password123"
}

# Refresh Token
POST /api/v1/auth/refresh
Content-Type: application/json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

## 📝 Uso de la API

### Autenticación en Requests

#### Swagger UI:
1. Hacer login/registro para obtener el token
2. Clic en "Authorize"
3. Pegar el `accessToken` (sin "Bearer ")
4. Todas las peticiones incluirán automáticamente la autorización

#### Postman:
1. Authorization tab → Bearer Token
2. Pegar el `accessToken`

#### cURL:
```bash
curl -X GET "http://localhost:8080/api/v1/categories" \
     -H "Authorization: Bearer <tu-access-token>"
```

### Ejemplo: Gestión de Categorías

```bash
# Listar categorías (requiere autenticación)
GET /api/v1/categories

# Crear categoría
POST /api/v1/categories
Content-Type: application/json
Authorization: Bearer <token>
{
  "name": "  tecnología   ",     # Se normalizará a "TECNOLOGÍA"
  "description": "  Productos  tecnológicos  "  # Se normalizará a "Productos tecnológicos"
}

# Obtener categoría por ID
GET /api/v1/categories/{id}

# Actualizar categoría
PUT /api/v1/categories/{id}

# Eliminar categoría
DELETE /api/v1/categories/{id}
```

## 🔧 Características Técnicas

### Normalización Automática de Texto

Las entidades usan anotaciones `@Normalize` para limpiar automáticamente los textos:

```java
@Column(nullable = false, unique = true, length = 100)
@Normalize(Normalize.NormalizeType.UPPERCASE)
private String name;

@Column(length = 255)
@Normalize(Normalize.NormalizeType.SPACES_ONLY) 
private String description;
```

Tipos disponibles:
- `UPPERCASE`: Mayúsculas + sin espacios dobles
- `LOWERCASE`: Minúsculas + sin espacios dobles
- `TITLE_CASE`: Primera letra mayúscula + sin espacios dobles
- `SPACES_ONLY`: Solo elimina espacios dobles

### Auditoría Automática

Todas las entidades que extiendan `AuditableEntity` tendrán:
- `createdAt`, `updatedAt`: Timestamps automáticos
- `createdBy`, `updatedBy`: Usuario del JWT automático
- `active`: Flag de estado (soft delete)

## 🐞 Debugging

### Logs de Autenticación
Los filtros JWT muestran logs detallados:
```
=== JWT FILTER EXECUTED ===
Full URI: http://localhost:8080/api/v1/categories
Authorization Header: Bearer eyJhbGciOiJIUzI1NiIs...
Token validation result: true
SUCCESS: User authenticated: usuario123
```

### Logs de Normalización
```
AuditListener: Entity about to be persisted: Category
=== CATEGORIES ENDPOINT CALLED ===
```

## 📊 Base de Datos

### Configuración de Hibernate

```yaml
# Desarrollo - mantiene datos
hibernate-orm:
  database:
    generation: update

# Producción - solo validación  
hibernate-orm:
  database:
    generation: validate
```

### Estructura Principal

```sql
-- Usuarios
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    -- Campos de auditoría
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Categorías  
CREATE TABLE categories (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    -- Campos de auditoría heredados
);
```

## 🔨 Desarrollo

### Agregar Nuevo Módulo

1. Crear estructura de packages siguiendo DDD
2. Extender `AuditableEntity` para entidades
3. Usar anotaciones `@Normalize` para campos de texto
4. Implementar controllers con `@SecurityRequirement(name = "bearerAuth")`

### Comandos Útiles

```bash
# Desarrollo con hot reload
./gradlew quarkusDev

# Compilar para producción
./gradlew build -Dquarkus.package.type=uber-jar

# Tests
./gradlew test

# Limpiar proyecto
./gradlew clean
```

## Packaging and running the application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it's not an *über-jar* as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an *über-jar*, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an *über-jar*, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/lamb-upeu-sis-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)