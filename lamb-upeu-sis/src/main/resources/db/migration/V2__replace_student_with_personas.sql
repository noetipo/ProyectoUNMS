-- =============================================================================
-- Migración: reemplazar el contexto `student` (modelo SaaS heredado) por el
-- modelo persona-céntrico (`persona` + perfiles `estudiantes`/`docentes` con
-- PK compartida + `programas_posgrado`).
--
-- El esquema lo gestiona Hibernate (generation=update): NO elimina tablas ni
-- columnas obsoletas, por eso aquí se dropean a mano. Las tablas nuevas
-- (persona, estudiantes, docentes, programas_posgrado) las crea Hibernate al
-- arrancar a partir de las entidades.
--
-- Importante: la nueva tabla `estudiantes` tiene un esquema INCOMPATIBLE con la
-- antigua (antes id propio + datos personales; ahora PK = persona_id). Por eso
-- se DROPea la antigua para que Hibernate cree la nueva limpia.
--
-- Aplicar sobre PostgreSQL:
--   psql -h localhost -p 5433 -U postgres -d quarkus_db \
--        -f src/main/resources/db/migration/V2__replace_student_with_personas.sql
--
-- Idempotente.
-- =============================================================================

BEGIN;

-- Tablas del contexto `student` heredado (orden: dependientes primero).
DROP TABLE IF EXISTS documentos_estudiante CASCADE;
DROP TABLE IF EXISTS estudiantes          CASCADE;
DROP TABLE IF EXISTS programas_doctorado  CASCADE;
DROP TABLE IF EXISTS centros_laborales    CASCADE;
DROP TABLE IF EXISTS cargos               CASCADE;

COMMIT;

-- Nota: persona, estudiantes (nuevo), docentes y programas_posgrado se crean
-- automáticamente vía Hibernate update al iniciar la aplicación.
