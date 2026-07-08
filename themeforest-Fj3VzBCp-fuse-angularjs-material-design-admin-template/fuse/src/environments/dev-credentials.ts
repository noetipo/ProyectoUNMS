/**
 * Credenciales de usuarios de prueba — SOLO DESARROLLO.
 *
 * Este archivo se importa ÚNICAMENTE desde `environment.ts` (perfil dev). En el
 * build de producción, `environment.ts` se reemplaza por `environment.prod.ts`
 * (ver `fileReplacements` en angular.json), por lo que esta lista NO queda en el
 * bundle de producción.
 *
 * Debe mantenerse en sincronía con el seeder del backend (`DatabaseSeeder.seedUsuariosPrueba`):
 *  - 2 usuarios de gestión (aparte) + 10 estudiantes + 10 docentes. Usuario = <apellido>.<nombre>.
 *  - Contraseña de prueba: `contra123` (el admin usa `admin123`, es el bootstrap del sistema).
 *  - Docentes: los 3 primeros también son tutores (PROF_TUTOR); los 3 siguientes, asesores (ASESOR).
 *
 * `destacado: true` = usuario clave para pruebas (se pinta en verde en el login).
 */
export interface DevCredential {
  rol: string;
  grupo: 'Gestión' | 'Docentes' | 'Estudiantes';
  username: string;
  password: string;
  destacado?: boolean;
}

export const DEV_CREDENTIALS: DevCredential[] = [
  // ── Gestión (aparte de los 20) ──
  { rol: 'Administrador',            grupo: 'Gestión', username: 'admin',        password: 'admin123',  destacado: true },
  { rol: 'Coordinador del Programa', grupo: 'Gestión', username: 'coordinador',  password: 'contra123', destacado: true },
  { rol: 'Secretaría',               grupo: 'Gestión', username: 'secretaria',   password: 'contra123', destacado: true },

  // ── Docentes (10) — los 3 primeros son tutores; los 3 siguientes, asesores ──
  { rol: 'Docente · Tutor',   grupo: 'Docentes', username: 'salazar.roberto',  password: 'contra123', destacado: true },
  { rol: 'Docente · Tutor',   grupo: 'Docentes', username: 'nunez.patricia',   password: 'contra123' },
  { rol: 'Docente · Tutor',   grupo: 'Docentes', username: 'paredes.carlos',   password: 'contra123' },
  { rol: 'Docente · Asesor',  grupo: 'Docentes', username: 'caceres.silvia',   password: 'contra123', destacado: true },
  { rol: 'Docente · Asesor',  grupo: 'Docentes', username: 'rios.fernando',    password: 'contra123' },
  { rol: 'Docente · Asesor',  grupo: 'Docentes', username: 'vega.marta',       password: 'contra123' },
  { rol: 'Docente',           grupo: 'Docentes', username: 'ledesma.andres',   password: 'contra123' },
  { rol: 'Docente',           grupo: 'Docentes', username: 'bravo.teresa',     password: 'contra123' },
  { rol: 'Docente',           grupo: 'Docentes', username: 'cabrera.julio',    password: 'contra123' },
  { rol: 'Docente',           grupo: 'Docentes', username: 'ponce.diana',      password: 'contra123' },

  // ── Estudiantes (10) ──
  { rol: 'Estudiante', grupo: 'Estudiantes', username: 'quispe.ana',     password: 'contra123', destacado: true },
  { rol: 'Estudiante', grupo: 'Estudiantes', username: 'mamani.luis',    password: 'contra123' },
  { rol: 'Estudiante', grupo: 'Estudiantes', username: 'flores.carmen',  password: 'contra123' },
  { rol: 'Estudiante', grupo: 'Estudiantes', username: 'huaman.jose',    password: 'contra123' },
  { rol: 'Estudiante', grupo: 'Estudiantes', username: 'condori.rosa',   password: 'contra123' },
  { rol: 'Estudiante', grupo: 'Estudiantes', username: 'vargas.pedro',   password: 'contra123' },
  { rol: 'Estudiante', grupo: 'Estudiantes', username: 'rojas.lucia',    password: 'contra123' },
  { rol: 'Estudiante', grupo: 'Estudiantes', username: 'chavez.miguel',  password: 'contra123' },
  { rol: 'Estudiante', grupo: 'Estudiantes', username: 'ramos.elena',    password: 'contra123' },
  { rol: 'Estudiante', grupo: 'Estudiantes', username: 'torres.jorge',   password: 'contra123' },
];
