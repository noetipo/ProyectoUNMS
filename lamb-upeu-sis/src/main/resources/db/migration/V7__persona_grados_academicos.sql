-- =============================================================================
-- Migración V7: tabla persona_grados_academicos (grados académicos como lista
-- a nivel de persona). Reemplaza la antigua columna docentes.grado_academico:
-- el grado del docente se deriva del grado marcado como principal.
--
-- Aditiva e idempotente. La entidad JPA PersonaGradoAcademico también mapea esta
-- tabla; aquí se garantizan además el CHECK del enum y el índice único PARCIAL
-- (una sola fila principal por persona) que Hibernate no genera.
--
-- Aplicar sobre PostgreSQL:
--   psql -h localhost -p 5433 -U postgres -d quarkus_db \
--        -f src/main/resources/db/migration/V7__persona_grados_academicos.sql
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS persona_grados_academicos (
    id          uuid         NOT NULL DEFAULT gen_random_uuid(),
    persona_id  uuid         NOT NULL,
    grado       varchar(30)  NOT NULL,
    anio        integer,
    universidad varchar(200),
    principal   boolean      NOT NULL DEFAULT false,
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamp    NOT NULL DEFAULT now(),
    updated_at  timestamp,
    created_by  varchar(100),
    updated_by  varchar(100),
    CONSTRAINT pk_persona_grados PRIMARY KEY (id),
    CONSTRAINT fk_persona_grados_persona FOREIGN KEY (persona_id) REFERENCES personas (id),
    CONSTRAINT ck_persona_grado_tipo CHECK (grado IN (
        'BACHILLER', 'LICENCIADO', 'SEGUNDA_ESPECIALIDAD',
        'MAGISTER', 'DOCTOR', 'POST_DOCTORADO'
    ))
);

CREATE INDEX IF NOT EXISTS idx_persona_grados_persona
    ON persona_grados_academicos (persona_id);

-- Máximo un grado principal por persona (solo filas activas).
CREATE UNIQUE INDEX IF NOT EXISTS uq_grado_principal_por_persona
    ON persona_grados_academicos (persona_id) WHERE principal AND active;

-- La columna antigua queda obsoleta (el grado del docente se deriva del principal).
ALTER TABLE docentes DROP COLUMN IF EXISTS grado_academico;

COMMIT;
