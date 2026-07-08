-- =============================================================================
-- Migración V4: catálogo lineas_investigacion y tabla puente
-- docente_lineas_investigacion (especialidad del docente).
--
-- Aditiva: no toca tablas fuera de alcance. Idempotente. Las entidades JPA
-- también mapean estas tablas; aquí se garantizan además el índice único
-- PARCIAL (WHERE es_principal, una sola principal por docente) que Hibernate
-- no genera.
--
-- Aplicar sobre PostgreSQL:
--   psql -h localhost -p 5433 -U postgres -d quarkus_db \
--        -f src/main/resources/db/migration/V4__lineas_investigacion_y_docente_lineas.sql
-- =============================================================================

BEGIN;

-- ── Catálogo: lineas_investigacion ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lineas_investigacion (
    id          uuid         NOT NULL DEFAULT gen_random_uuid(),
    codigo      varchar(20)  UNIQUE,
    nombre      varchar(200) NOT NULL UNIQUE,
    descripcion varchar(500),
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamp    NOT NULL DEFAULT now(),
    updated_at  timestamp,
    created_by  varchar(100),
    updated_by  varchar(100),
    CONSTRAINT pk_lineas_investigacion PRIMARY KEY (id)
);

-- ── Puente: docente_lineas_investigacion ────────────────────────────────────
CREATE TABLE IF NOT EXISTS docente_lineas_investigacion (
    id                     uuid    NOT NULL DEFAULT gen_random_uuid(),
    es_principal           boolean NOT NULL DEFAULT false,
    docente_id             uuid    NOT NULL,
    linea_investigacion_id uuid    NOT NULL,
    active                 boolean NOT NULL DEFAULT true,
    created_at             timestamp NOT NULL DEFAULT now(),
    updated_at             timestamp,
    created_by             varchar(100),
    updated_by             varchar(100),
    CONSTRAINT pk_docente_lineas PRIMARY KEY (id),
    CONSTRAINT uq_docente_linea UNIQUE (docente_id, linea_investigacion_id),
    CONSTRAINT fk_docente_lineas_docente FOREIGN KEY (docente_id) REFERENCES docentes (persona_id),
    CONSTRAINT fk_docente_lineas_linea FOREIGN KEY (linea_investigacion_id) REFERENCES lineas_investigacion (id)
);

CREATE INDEX IF NOT EXISTS idx_docente_lineas_docente
    ON docente_lineas_investigacion (docente_id);
CREATE INDEX IF NOT EXISTS idx_docente_lineas_linea
    ON docente_lineas_investigacion (linea_investigacion_id);

-- Máximo una línea principal por docente
CREATE UNIQUE INDEX IF NOT EXISTS uq_linea_principal_por_docente
    ON docente_lineas_investigacion (docente_id) WHERE es_principal;

COMMIT;
