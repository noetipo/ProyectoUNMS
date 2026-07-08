-- =============================================================================
-- Migración V8: maestros de Configuración — facultades, lemas_anuales,
-- parametros_sistema. Aditiva e idempotente. Las entidades JPA también mapean
-- estas tablas; aquí se garantiza además el índice único PARCIAL de lemas
-- (un solo lema vigente por año) que Hibernate no genera.
--
-- Aplicar sobre PostgreSQL:
--   psql -h localhost -p 5433 -U postgres -d quarkus_db \
--        -f src/main/resources/db/migration/V8__configuracion_facultades_lemas_parametros.sql
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS facultades (
    id         uuid         NOT NULL DEFAULT gen_random_uuid(),
    active     boolean      NOT NULL DEFAULT true,
    created_at timestamp    NOT NULL DEFAULT now(),
    updated_at timestamp,
    created_by varchar(100),
    updated_by varchar(100),
    codigo     varchar(20)  UNIQUE,
    nombre     varchar(200) NOT NULL UNIQUE,
    CONSTRAINT pk_facultades PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS lemas_anuales (
    id         uuid         NOT NULL DEFAULT gen_random_uuid(),
    active     boolean      NOT NULL DEFAULT true,
    created_at timestamp    NOT NULL DEFAULT now(),
    updated_at timestamp,
    created_by varchar(100),
    updated_by varchar(100),
    anio       integer      NOT NULL,
    texto      varchar(300) NOT NULL,
    CONSTRAINT pk_lemas_anuales PRIMARY KEY (id)
);

-- Un solo lema VIGENTE por año.
CREATE UNIQUE INDEX IF NOT EXISTS uq_lema_activo_por_anio
    ON lemas_anuales (anio) WHERE active;

CREATE TABLE IF NOT EXISTS parametros_sistema (
    id          uuid         NOT NULL DEFAULT gen_random_uuid(),
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamp    NOT NULL DEFAULT now(),
    updated_at  timestamp,
    created_by  varchar(100),
    updated_by  varchar(100),
    clave       varchar(60)  NOT NULL UNIQUE,
    valor       varchar(500) NOT NULL,
    descripcion varchar(255),
    CONSTRAINT pk_parametros_sistema PRIMARY KEY (id)
);

COMMIT;
