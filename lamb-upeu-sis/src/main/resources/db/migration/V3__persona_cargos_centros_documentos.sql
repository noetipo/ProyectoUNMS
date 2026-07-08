-- =============================================================================
-- Migración V3: catálogos cargos/centros_laborales, historiales persona_cargos /
-- persona_centros_laborales y documentos_persona.
--
-- Aditiva: no toca tablas fuera de alcance. Idempotente. Las entidades JPA
-- también mapean estas tablas (Hibernate update las reconoce); aquí se garantizan
-- además los índices únicos PARCIALES (WHERE actual) y el CHECK, que Hibernate
-- no genera.
--
-- Aplicar sobre PostgreSQL:
--   psql -h localhost -p 5433 -U postgres -d quarkus_db \
--        -f src/main/resources/db/migration/V3__persona_cargos_centros_documentos.sql
-- =============================================================================

BEGIN;

-- ── Catálogo: cargos ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cargos (
    id             uuid         NOT NULL DEFAULT gen_random_uuid(),
    codigo_sistema varchar(20)  UNIQUE,
    nombre         varchar(150) NOT NULL UNIQUE,
    descripcion    varchar(500),
    active         boolean      NOT NULL DEFAULT true,
    created_at     timestamp    NOT NULL DEFAULT now(),
    updated_at     timestamp,
    created_by     varchar(100),
    updated_by     varchar(100),
    CONSTRAINT pk_cargos PRIMARY KEY (id)
);

-- ── Catálogo: centros_laborales ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS centros_laborales (
    id             uuid         NOT NULL DEFAULT gen_random_uuid(),
    codigo_sistema varchar(20)  UNIQUE,
    nombre         varchar(200) NOT NULL UNIQUE,
    descripcion    varchar(500),
    active         boolean      NOT NULL DEFAULT true,
    created_at     timestamp    NOT NULL DEFAULT now(),
    updated_at     timestamp,
    created_by     varchar(100),
    updated_by     varchar(100),
    CONSTRAINT pk_centros_laborales PRIMARY KEY (id)
);

-- ── Historial: persona_cargos ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS persona_cargos (
    id           uuid    NOT NULL DEFAULT gen_random_uuid(),
    fecha_inicio date,
    fecha_fin    date,
    actual       boolean NOT NULL DEFAULT false,
    persona_id   uuid    NOT NULL,
    cargo_id     uuid    NOT NULL,
    active       boolean NOT NULL DEFAULT true,
    created_at   timestamp NOT NULL DEFAULT now(),
    updated_at   timestamp,
    created_by   varchar(100),
    updated_by   varchar(100),
    CONSTRAINT pk_persona_cargos PRIMARY KEY (id),
    CONSTRAINT fk_persona_cargos_persona FOREIGN KEY (persona_id) REFERENCES persona (id),
    CONSTRAINT fk_persona_cargos_cargo FOREIGN KEY (cargo_id) REFERENCES cargos (id)
);
-- Un solo cargo actual por persona
CREATE UNIQUE INDEX IF NOT EXISTS uq_cargo_actual_por_persona
    ON persona_cargos (persona_id) WHERE actual;

-- ── Historial: persona_centros_laborales ───────────────────────────────────
CREATE TABLE IF NOT EXISTS persona_centros_laborales (
    id                 uuid    NOT NULL DEFAULT gen_random_uuid(),
    fecha_inicio       date,
    fecha_fin          date,
    actual             boolean NOT NULL DEFAULT false,
    persona_id         uuid    NOT NULL,
    centro_laboral_id  uuid    NOT NULL,
    active             boolean NOT NULL DEFAULT true,
    created_at         timestamp NOT NULL DEFAULT now(),
    updated_at         timestamp,
    created_by         varchar(100),
    updated_by         varchar(100),
    CONSTRAINT pk_persona_centros PRIMARY KEY (id),
    CONSTRAINT fk_persona_centros_persona FOREIGN KEY (persona_id) REFERENCES persona (id),
    CONSTRAINT fk_persona_centros_centro FOREIGN KEY (centro_laboral_id) REFERENCES centros_laborales (id)
);
-- Un solo centro laboral actual por persona
CREATE UNIQUE INDEX IF NOT EXISTS uq_centro_actual_por_persona
    ON persona_centros_laborales (persona_id) WHERE actual;

-- ── Documentos de persona ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS documentos_persona (
    id              uuid         NOT NULL DEFAULT gen_random_uuid(),
    tipo_documento  varchar(30)  NOT NULL,
    nombre_original varchar(255),
    storage_key     varchar(500) NOT NULL,
    content_type    varchar(100),
    tamanio_bytes   bigint,
    hash_sha256     varchar(64),
    fecha_carga     timestamp,
    persona_id      uuid         NOT NULL,
    active          boolean      NOT NULL DEFAULT true,
    created_at      timestamp    NOT NULL DEFAULT now(),
    updated_at      timestamp,
    created_by      varchar(100),
    updated_by      varchar(100),
    CONSTRAINT pk_documentos_persona PRIMARY KEY (id),
    CONSTRAINT uq_documento_persona_tipo UNIQUE (persona_id, tipo_documento),
    CONSTRAINT fk_documentos_persona_persona FOREIGN KEY (persona_id) REFERENCES persona (id)
);

-- CHECK del tipo de documento (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_documento_tipo') THEN
        ALTER TABLE documentos_persona ADD CONSTRAINT ck_documento_tipo
            CHECK (tipo_documento IN ('DNI','CARNET_EXTRANJERIA','PASAPORTE','PTP',
                                      'CARNET_DIPLOMATICO','DNI_CE','PARTIDA_NACIMIENTO'));
    END IF;
END $$;

COMMIT;

-- Nota: las entradas de menú (modules + role_modules para cargos/centros, ADMIN y
-- SECRETARIA) se siembran vía DatabaseSeeder al iniciar la app (patrón del proyecto).
