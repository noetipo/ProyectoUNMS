-- =============================================================================
-- Migración V5: solicitudes_asesoria (Fase 1 · Dictamen asesor).
-- El estudiante solicita asesoría a un docente; el docente acepta/rechaza.
--
-- Aditiva e idempotente. Las entidades JPA también mapean esta tabla (Hibernate
-- `update` la crea); aquí se garantizan además el índice único PARCIAL
-- (una sola PENDIENTE por estudiante) y los CHECK, que Hibernate no genera.
--
-- Nota: `tesis_id` es un uuid sin FK (aún no existe la tabla `tesis` en el
-- esquema activo), igual que `asesorias.tesis_id`.
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS solicitudes_asesoria (
    id                     uuid         NOT NULL DEFAULT gen_random_uuid(),
    estudiante_id          uuid         NOT NULL,
    docente_id             uuid         NOT NULL,
    linea_investigacion_id uuid,
    tesis_id               uuid,
    titulo_tentativo       varchar(500),
    mensaje                varchar(2000),
    tipo                   varchar(20)  NOT NULL DEFAULT 'ASESOR',
    estado                 varchar(20)  NOT NULL DEFAULT 'PENDIENTE',
    fecha_solicitud        timestamp    NOT NULL DEFAULT now(),
    fecha_respuesta        timestamp,
    motivo_respuesta       varchar(500),
    active                 boolean      NOT NULL DEFAULT true,
    created_at             timestamp    NOT NULL DEFAULT now(),
    updated_at             timestamp,
    created_by             varchar(100),
    updated_by             varchar(100),
    CONSTRAINT pk_solicitudes_asesoria PRIMARY KEY (id),
    CONSTRAINT fk_solicitudes_asesoria_estudiante FOREIGN KEY (estudiante_id) REFERENCES estudiantes (persona_id),
    CONSTRAINT fk_solicitudes_asesoria_docente FOREIGN KEY (docente_id) REFERENCES docentes (persona_id),
    CONSTRAINT fk_solicitudes_asesoria_linea FOREIGN KEY (linea_investigacion_id) REFERENCES lineas_investigacion (id),
    CONSTRAINT solicitudes_asesoria_tipo_check CHECK (tipo IN ('ASESOR','COASESOR')),
    CONSTRAINT solicitudes_asesoria_estado_check CHECK (estado IN ('PENDIENTE','ACEPTADA','RECHAZADA','CANCELADA'))
);

CREATE INDEX IF NOT EXISTS idx_solicitudes_asesoria_docente
    ON solicitudes_asesoria (docente_id, estado);
CREATE INDEX IF NOT EXISTS idx_solicitudes_asesoria_estudiante
    ON solicitudes_asesoria (estudiante_id, estado);

-- Una sola solicitud PENDIENTE por estudiante a la vez
CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_pendiente_por_estudiante
    ON solicitudes_asesoria (estudiante_id) WHERE estado = 'PENDIENTE';

COMMIT;
