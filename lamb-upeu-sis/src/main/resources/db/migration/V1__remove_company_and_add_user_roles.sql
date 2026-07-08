-- =============================================================================
-- Migración: eliminar el dominio "company" y reanclar el RBAC a "users".
--
-- Contexto: el esquema se autogenera con Hibernate (quarkus.hibernate-orm.
-- database.generation = update). Hibernate NUNCA elimina tablas ni columnas
-- existentes, por eso este script realiza la limpieza destructiva a mano.
-- La tabla nueva user_roles también la crea Hibernate al arrancar; aquí se
-- incluye con IF NOT EXISTS para poder aplicar el script sobre una base limpia
-- sin depender del arranque de la app.
--
-- Aplicar manualmente (orden de FKs respetado) sobre PostgreSQL:
--   psql -h localhost -p 5433 -U postgres -d quarkus_db \
--        -f src/main/resources/db/migration/V1__remove_company_and_add_user_roles.sql
--
-- Idempotente: puede ejecutarse varias veces sin error.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1) Eliminar tablas del dominio company (CASCADE también elimina sus FKs).
--    Orden: dependientes primero, luego companies.
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS company_user_admins CASCADE;
DROP TABLE IF EXISTS company_user_roles  CASCADE;
DROP TABLE IF EXISTS company_users       CASCADE;
DROP TABLE IF EXISTS companies           CASCADE;

-- -----------------------------------------------------------------------------
-- 2) Quitar la columna company_id de roles y role_modules.
--    (En PostgreSQL, DROP COLUMN elimina también la FK asociada.)
-- -----------------------------------------------------------------------------
ALTER TABLE roles        DROP COLUMN IF EXISTS company_id;
ALTER TABLE role_modules DROP COLUMN IF EXISTS company_id;

-- -----------------------------------------------------------------------------
-- 3) Quitar el enum "role" de users (los roles ahora viven en user_roles).
-- -----------------------------------------------------------------------------
ALTER TABLE users DROP COLUMN IF EXISTS role;

-- -----------------------------------------------------------------------------
-- 4) Crear la tabla user_roles (users ↔ roles) con auditoría y unicidad.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_roles (
    id         uuid         NOT NULL,
    assigned   boolean      NOT NULL DEFAULT true,
    user_id    uuid         NOT NULL,
    role_id    uuid         NOT NULL,
    active     boolean      NOT NULL DEFAULT true,
    created_at timestamp    NOT NULL,
    updated_at timestamp,
    created_by varchar(100),
    updated_by varchar(100),
    deletedat  timestamp,
    CONSTRAINT pk_user_roles PRIMARY KEY (id),
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

COMMIT;