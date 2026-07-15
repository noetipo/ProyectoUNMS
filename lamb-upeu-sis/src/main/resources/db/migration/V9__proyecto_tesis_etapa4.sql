-- =============================================================================
-- Migración V9: Etapa 4 · Elaboración del proyecto de tesis en línea.
--
-- Aditiva e idempotente. Las entidades JPA también mapean estas tablas
-- (Hibernate `update` las crea junto con sus índices únicos); aquí se
-- garantizan además los CHECK de enums y los índices, que Hibernate no genera.
--
-- Nota: `proyectos_tesis.tesis_id` es un uuid SIN FK (mismo criterio que
-- `solicitudes_asesoria.tesis_id` en V5). Las tablas hijas sí referencian a
-- `proyectos_tesis(id)`.
-- =============================================================================

BEGIN;

-- Cabecera del proyecto (1 por tesis) -----------------------------------------
CREATE TABLE IF NOT EXISTS proyectos_tesis (
    id                          uuid        NOT NULL DEFAULT gen_random_uuid(),
    tesis_id                    uuid        NOT NULL,
    asesor_id                   uuid,
    enfoque                     varchar(20) NOT NULL DEFAULT 'CUANTITATIVO',
    estado                      varchar(20) NOT NULL DEFAULT 'EN_ELABORACION',
    financiamiento              varchar(60) DEFAULT 'Autofinanciado',
    listo_revision              boolean     NOT NULL DEFAULT false,
    fecha_listo_revision        date,
    plan_publicado              boolean     NOT NULL DEFAULT false,
    carta_asesor                boolean     NOT NULL DEFAULT false,
    fecha_carta_asesor          date,
    turnitin_subido             boolean     NOT NULL DEFAULT false,
    porcentaje_similitud        integer,
    expediente_subido           boolean     NOT NULL DEFAULT false,
    fecha_solicitud_aprobacion  date,
    active                      boolean     NOT NULL DEFAULT true,
    created_at                  timestamp   NOT NULL DEFAULT now(),
    updated_at                  timestamp,
    created_by                  varchar(100),
    updated_by                  varchar(100),
    CONSTRAINT pk_proyectos_tesis PRIMARY KEY (id),
    CONSTRAINT proyectos_tesis_enfoque_check CHECK (enfoque IN ('CUANTITATIVO','CUALITATIVO')),
    CONSTRAINT proyectos_tesis_estado_check CHECK (estado IN ('EN_ELABORACION','EN_REVISION','OBSERVADO','CONFORME','APROBADO'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_proyectos_tesis_tesis ON proyectos_tesis (tesis_id);

-- Campos de texto del editor (clave-valor) ------------------------------------
CREATE TABLE IF NOT EXISTS proyecto_campos (
    id           uuid        NOT NULL DEFAULT gen_random_uuid(),
    proyecto_id  uuid        NOT NULL,
    clave        varchar(40) NOT NULL,
    valor        text,
    active       boolean     NOT NULL DEFAULT true,
    created_at   timestamp   NOT NULL DEFAULT now(),
    updated_at   timestamp,
    created_by   varchar(100),
    updated_by   varchar(100),
    CONSTRAINT pk_proyecto_campos PRIMARY KEY (id),
    CONSTRAINT fk_proyecto_campos_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos_tesis (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_proyecto_campo ON proyecto_campos (proyecto_id, clave);

-- Objetivos específicos -------------------------------------------------------
CREATE TABLE IF NOT EXISTS proyecto_objetivos (
    id           uuid         NOT NULL DEFAULT gen_random_uuid(),
    proyecto_id  uuid         NOT NULL,
    orden        integer      NOT NULL DEFAULT 0,
    texto        varchar(1000),
    active       boolean      NOT NULL DEFAULT true,
    created_at   timestamp    NOT NULL DEFAULT now(),
    updated_at   timestamp,
    created_by   varchar(100),
    updated_by   varchar(100),
    CONSTRAINT pk_proyecto_objetivos PRIMARY KEY (id),
    CONSTRAINT fk_proyecto_objetivos_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos_tesis (id)
);
CREATE INDEX IF NOT EXISTS idx_proyecto_objetivos_proyecto ON proyecto_objetivos (proyecto_id);

-- Cronograma / plan de actividades --------------------------------------------
CREATE TABLE IF NOT EXISTS proyecto_actividades (
    id           uuid         NOT NULL DEFAULT gen_random_uuid(),
    proyecto_id  uuid         NOT NULL,
    nombre       varchar(300) NOT NULL,
    fase         varchar(20),
    mes_inicio   integer,
    mes_fin      integer,
    estado       varchar(20)  NOT NULL DEFAULT 'PENDIENTE',
    orden        integer      NOT NULL DEFAULT 0,
    active       boolean      NOT NULL DEFAULT true,
    created_at   timestamp    NOT NULL DEFAULT now(),
    updated_at   timestamp,
    created_by   varchar(100),
    updated_by   varchar(100),
    CONSTRAINT pk_proyecto_actividades PRIMARY KEY (id),
    CONSTRAINT fk_proyecto_actividades_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos_tesis (id),
    CONSTRAINT proyecto_actividades_fase_check CHECK (fase IS NULL OR fase IN ('PLANIFICACION','TRABAJO_CAMPO','ANALISIS','REDACCION')),
    CONSTRAINT proyecto_actividades_estado_check CHECK (estado IN ('PENDIENTE','EN_CURSO','HECHA'))
);
CREATE INDEX IF NOT EXISTS idx_proyecto_actividades_proyecto ON proyecto_actividades (proyecto_id);

-- Presupuesto por partidas ----------------------------------------------------
CREATE TABLE IF NOT EXISTS proyecto_presupuesto (
    id           uuid          NOT NULL DEFAULT gen_random_uuid(),
    proyecto_id  uuid          NOT NULL,
    rubro        varchar(60),
    descripcion  varchar(300),
    monto        numeric(12,2) NOT NULL DEFAULT 0,
    orden        integer       NOT NULL DEFAULT 0,
    active       boolean       NOT NULL DEFAULT true,
    created_at   timestamp     NOT NULL DEFAULT now(),
    updated_at   timestamp,
    created_by   varchar(100),
    updated_by   varchar(100),
    CONSTRAINT pk_proyecto_presupuesto PRIMARY KEY (id),
    CONSTRAINT fk_proyecto_presupuesto_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos_tesis (id)
);
CREATE INDEX IF NOT EXISTS idx_proyecto_presupuesto_proyecto ON proyecto_presupuesto (proyecto_id);

-- Revisión por ítem (una por campo) -------------------------------------------
CREATE TABLE IF NOT EXISTS proyecto_revisiones (
    id           uuid        NOT NULL DEFAULT gen_random_uuid(),
    proyecto_id  uuid        NOT NULL,
    campo        varchar(40) NOT NULL,
    estado       varchar(20) NOT NULL DEFAULT 'SIN_REVISION',
    active       boolean     NOT NULL DEFAULT true,
    created_at   timestamp   NOT NULL DEFAULT now(),
    updated_at   timestamp,
    created_by   varchar(100),
    updated_by   varchar(100),
    CONSTRAINT pk_proyecto_revisiones PRIMARY KEY (id),
    CONSTRAINT fk_proyecto_revisiones_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos_tesis (id),
    CONSTRAINT proyecto_revisiones_estado_check CHECK (estado IN ('SIN_REVISION','OBSERVADO','EN_CORRECCION','CORREGIDO','CONFORME'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_proyecto_revision_campo ON proyecto_revisiones (proyecto_id, campo);

-- Hilo de eventos de cada revisión --------------------------------------------
CREATE TABLE IF NOT EXISTS proyecto_revision_eventos (
    id           uuid        NOT NULL DEFAULT gen_random_uuid(),
    revision_id  uuid        NOT NULL,
    tipo         varchar(20) NOT NULL,
    autor        varchar(200),
    rol          varchar(20),
    texto        text,
    fecha_evento timestamp,
    active       boolean     NOT NULL DEFAULT true,
    created_at   timestamp   NOT NULL DEFAULT now(),
    updated_at   timestamp,
    created_by   varchar(100),
    updated_by   varchar(100),
    CONSTRAINT pk_proyecto_revision_eventos PRIMARY KEY (id),
    CONSTRAINT fk_proyecto_revision_eventos_revision FOREIGN KEY (revision_id) REFERENCES proyecto_revisiones (id)
);
CREATE INDEX IF NOT EXISTS idx_proyecto_revision_eventos_revision ON proyecto_revision_eventos (revision_id);

COMMIT;
