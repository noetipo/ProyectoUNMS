-- =============================================================================
-- Migración V6: tutorías (tutor académico por estudiante, con historial) +
-- cupo máximo de tutoría por docente.
--
-- Aditiva e idempotente. Las entidades JPA también mapean estas columnas
-- (Hibernate `update` las crea); aquí se garantizan además el índice único
-- PARCIAL (un solo tutor vigente por estudiante) que Hibernate no genera.
-- Mismo estilo historial que persona_cargos (actual/fecha_inicio/fecha_fin).
-- =============================================================================

BEGIN;

ALTER TABLE docentes ADD COLUMN IF NOT EXISTS cupo_maximo_tutoria integer;

CREATE TABLE IF NOT EXISTS tutorias (
    id            uuid      NOT NULL DEFAULT gen_random_uuid(),
    estudiante_id uuid      NOT NULL,
    docente_id    uuid      NOT NULL,
    fecha_inicio  date      NOT NULL DEFAULT current_date,
    fecha_fin     date,
    actual        boolean   NOT NULL DEFAULT true,
    motivo_cambio varchar(500),
    active        boolean   NOT NULL DEFAULT true,
    created_at    timestamp NOT NULL DEFAULT now(),
    updated_at    timestamp,
    created_by    varchar(100),
    updated_by    varchar(100),
    CONSTRAINT pk_tutorias PRIMARY KEY (id),
    CONSTRAINT fk_tutorias_estudiante FOREIGN KEY (estudiante_id) REFERENCES estudiantes (persona_id),
    CONSTRAINT fk_tutorias_docente FOREIGN KEY (docente_id) REFERENCES docentes (persona_id)
);

CREATE INDEX IF NOT EXISTS idx_tutorias_estudiante ON tutorias (estudiante_id);
CREATE INDEX IF NOT EXISTS idx_tutorias_docente_actual ON tutorias (docente_id, actual);

-- Un solo tutor vigente por estudiante
CREATE UNIQUE INDEX IF NOT EXISTS uq_tutoria_actual_por_estudiante
    ON tutorias (estudiante_id) WHERE actual;

COMMIT;
