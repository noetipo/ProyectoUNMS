package unmsm.edu.pe.shared.utils;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import unmsm.edu.pe.personas.domain.entities.Cargo;
import unmsm.edu.pe.personas.domain.entities.CentroLaboral;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;
import unmsm.edu.pe.personas.domain.enums.CategoriaDocente;
import unmsm.edu.pe.personas.domain.enums.CondicionDocente;
import unmsm.edu.pe.personas.domain.enums.CondicionEstudiante;
import unmsm.edu.pe.personas.domain.enums.EstadoCivil;
import unmsm.edu.pe.personas.domain.enums.Financiamiento;
import unmsm.edu.pe.personas.domain.enums.GradoAcademico;
import unmsm.edu.pe.personas.domain.enums.NivelPrograma;
import unmsm.edu.pe.personas.domain.enums.Procedencia;
import unmsm.edu.pe.personas.domain.enums.Sexo;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.personas.domain.entities.DocenteLineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.PersonaGradoAcademico;
import unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository;
import unmsm.edu.pe.personas.domain.repositories.CargoRepository;
import unmsm.edu.pe.personas.domain.repositories.CentroLaboralRepository;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.repositories.ProgramaPosgradoRepository;
import unmsm.edu.pe.tesis.domain.entities.Asesoria;
import unmsm.edu.pe.tesis.domain.entities.SolicitudAsesoria;
import unmsm.edu.pe.tesis.domain.entities.SugerenciaAsesor;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.entities.TesisAutor;
import unmsm.edu.pe.tesis.domain.enums.EstadoSolicitud;
import unmsm.edu.pe.tesis.domain.enums.EstadoTesis;
import unmsm.edu.pe.tesis.domain.enums.TipoAsesoria;
import unmsm.edu.pe.tesis.domain.repositories.SolicitudAsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.SugerenciaAsesorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;
import unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository;
import unmsm.edu.pe.security.domain.entities.Module;
import unmsm.edu.pe.security.domain.entities.ParentModule;
import unmsm.edu.pe.security.domain.entities.Role;
import unmsm.edu.pe.security.domain.entities.RoleModule;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.entities.UserRoleAssignment;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.security.domain.repositories.ModuleRepository;
import unmsm.edu.pe.security.domain.repositories.ParentModuleRepository;
import unmsm.edu.pe.security.domain.repositories.RoleModuleRepository;
import unmsm.edu.pe.security.domain.repositories.RoleRepository;
import unmsm.edu.pe.security.domain.repositories.UserRepository;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;
import unmsm.edu.pe.security.infrastructure.utils.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🌱 SEEDER UNIFICADO DE BASE DE DATOS
 * Se ejecuta automáticamente al iniciar la aplicación, en una sola transacción.
 */
@ApplicationScoped
public class DatabaseSeeder {

    private static final Logger LOG = Logger.getLogger(DatabaseSeeder.class);

    @Inject UserRepository userRepository;
    @Inject ParentModuleRepository parentModuleRepository;
    @Inject ModuleRepository moduleRepository;
    @Inject RoleRepository roleRepository;
    @Inject RoleModuleRepository roleModuleRepository;
    @Inject UserRoleAssignmentRepository userRoleRepository;
    @Inject ProgramaPosgradoRepository programaPosgradoRepository;
    @Inject CargoRepository cargoRepository;
    @Inject CentroLaboralRepository centroLaboralRepository;
    @Inject LineaInvestigacionRepository lineaInvestigacionRepository;
    @Inject DocenteLineaInvestigacionRepository docenteLineaInvestigacionRepository;
    @Inject PersonaGradoAcademicoRepository personaGradoAcademicoRepository;
    @Inject PersonaRepository personaRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject unmsm.edu.pe.configuracion.domain.repositories.FacultadRepository facultadRepository;
    @Inject unmsm.edu.pe.configuracion.domain.repositories.LemaAnualRepository lemaAnualRepository;
    @Inject unmsm.edu.pe.configuracion.domain.repositories.ParametroSistemaRepository parametroSistemaRepository;
    @Inject PasswordEncoder passwordEncoder;

    /** Solo en desarrollo (%dev): crea usuarios de prueba con contraseña conocida. */
    @ConfigProperty(name = "app.seeders.dev-users", defaultValue = "false")
    boolean devUsersEnabled;

    // ── Admin hardcodeado ──
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_EMAIL = "admin@unmsm.edu.pe";
    private static final String ADMIN_RAW_PASSWORD = "admin123";
    private static final String ADMIN_FIRSTNAME = "Administrador";
    private static final String ADMIN_LASTNAME = "Sistema";
    private static final String ADMIN_PHONE = "+51 999999999";
    private static final String ADMIN_ROLE_CODE = "ADMIN";

    @Transactional
    public void onStart(@Observes StartupEvent event) {
        LOG.info("🌱 ===== INICIANDO SEEDER =====");
        try {
            User adminUser = seedAdminUser();
            List<ParentModule> parentModules = seedParentModules();
            List<Module> modules = seedModules(parentModules);
            List<Role> roles = seedRoles();
            seedRoleModules(roles, modules);
            seedUserRoles(adminUser, roles);
            seedFacultades();          // antes que los programas (FK facultad_id)
            seedProgramasPosgrado();
            seedCargos();
            seedCentrosLaborales();
            seedLineasInvestigacion();
            seedParametrosSistema();
            seedLemasAnuales();
            seedUsuariosPrueba(roles);

            LOG.info("🌱 ✅ SEEDER COMPLETADO");
            LOG.info("🌱 Credenciales admin: " + ADMIN_USERNAME + " / " + ADMIN_RAW_PASSWORD
                    + " · usuarios de prueba: <apellido>.<nombre> / " + PRUEBA_PASSWORD);
        } catch (Exception e) {
            LOG.error("❌ Error ejecutando seeder: " + e.getMessage(), e);
            throw new RuntimeException("Error fatal en seeding", e);
        }
    }

    // ── FASE 1: Admin ──
    private User seedAdminUser() {
        Optional<User> existing = userRepository.findByUsername(ADMIN_USERNAME);
        if (existing.isPresent()) {
            LOG.info("   ⏭️  User admin ya existe");
            return existing.get();
        }
        User user = new User();
        user.setUsername(ADMIN_USERNAME);
        user.setEmail(ADMIN_EMAIL);
        user.setPassword(passwordEncoder.encode(ADMIN_RAW_PASSWORD));
        user.setFirstName(ADMIN_FIRSTNAME);
        user.setLastName(ADMIN_LASTNAME);
        user.setPhone(ADMIN_PHONE);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.saveUser(user);
        LOG.info("   ✅ User admin creado");
        return user;
    }

    // ── FASE 2: Parent Modules ──
    private List<ParentModule> seedParentModules() {
        List<ParentModule> result = new ArrayList<>();
        ParentModuleData[] data = {
                new ParentModuleData("01", "Configuración", "Usuarios, roles y permisos", "collapsable",
                        "heroicons_outline:cog-6-tooth", "/example", 1),
                new ParentModuleData("03", "Gestión Posgrado", "Personas y programas de posgrado", "collapsable",
                        "heroicons_outline:academic-cap", "/example", 3),
                new ParentModuleData("05", "Proceso de Tesis", "Solicitudes de asesoría y tesis", "collapsable",
                        "heroicons_outline:document-text", "/example", 5)
        };
        for (ParentModuleData d : data) {
            Optional<ParentModule> existing = parentModuleRepository.findByCode(d.code);
            if (existing.isPresent()) {
                result.add(existing.get());
            } else {
                ParentModule pm = ParentModule.builder()
                        .code(d.code).title(d.title).subtitle(d.subtitle).type(d.type)
                        .icon(d.icon).link(d.link).moduleOrder(d.moduleOrder).status(true).build();
                parentModuleRepository.save(pm);
                result.add(pm);
            }
        }
        return result;
    }

    // ── FASE 3: Modules ──
    private List<Module> seedModules(List<ParentModule> parentModules) {
        List<Module> result = new ArrayList<>();
        ModuleData[] data = {
                new ModuleData("01", "Usuarios", "basic", "heroicons_outline:user-group",
                        "/admin/setup/user", 1, "01"),
                new ModuleData("03", "Modulos Padres", "basic", "heroicons_outline:clipboard-document",
                        "/admin/setup/parent-module", 3, "01"),
                new ModuleData("04", "Modulos", "basic", "heroicons_outline:clipboard-document-check",
                        "/admin/setup/module", 4, "01"),
                new ModuleData("05", "Roles", "basic", "heroicons_outline:users",
                        "/admin/setup/role", 5, "01"),
                new ModuleData("20", "Personas", "basic", "heroicons_outline:identification",
                        "/admin/personas", 1, "03"),
                new ModuleData("21", "Estudiantes", "basic", "heroicons_outline:academic-cap",
                        "/admin/estudiantes", 2, "03"),
                new ModuleData("22", "Docentes", "basic", "heroicons_outline:user-group",
                        "/admin/docentes", 3, "03"),
                new ModuleData("23", "Mi Perfil", "basic", "heroicons_outline:user-circle",
                        "/admin/mi-perfil", 4, "03"),
                new ModuleData("24", "Cargos", "basic", "heroicons_outline:briefcase",
                        "/admin/cargos", 6, "01"),
                new ModuleData("25", "Centros Laborales", "basic", "heroicons_outline:building-office",
                        "/admin/centros-laborales", 7, "01"),
                new ModuleData("26", "Líneas de Investigación", "basic", "heroicons_outline:light-bulb",
                        "/admin/lineas-investigacion", 8, "01"),
                new ModuleData("35", "Facultades", "basic", "heroicons_outline:building-library",
                        "/admin/facultades", 9, "01"),
                new ModuleData("36", "Lemas Anuales", "basic", "heroicons_outline:megaphone",
                        "/admin/lemas-anuales", 10, "01"),
                new ModuleData("37", "Parámetros del Sistema", "basic", "heroicons_outline:adjustments-horizontal",
                        "/admin/parametros-sistema", 11, "01"),
                new ModuleData("29", "Solicitudes de Asesoría", "basic", "heroicons_outline:inbox",
                        "/admin/solicitudes-asesoria", 3, "05"),
                new ModuleData("30", "Asignar Tutor", "basic", "heroicons_outline:user-plus",
                        "/admin/asignar-tutor", 5, "03"),
                new ModuleData("31", "Reporte de Tutores", "basic", "heroicons_outline:document-chart-bar",
                        "/admin/reporte-tutores", 6, "03"),
                new ModuleData("32", "Mis Tutorandos", "basic", "heroicons_outline:academic-cap",
                        "/admin/mis-tutorandos", 4, "05"),
                new ModuleData("33", "Registro de Tema", "basic", "heroicons_outline:document-text",
                        "/admin/registro-tema", 5, "05"),
                new ModuleData("34", "Mi Asesoría", "basic", "heroicons_outline:clipboard-document-check",
                        "/admin/mi-asesoria", 3, "05"),
                new ModuleData("38", "Dictámenes de Designación", "basic", "heroicons_outline:document-check",
                        "/admin/dictamenes", 7, "05"),
        };
        for (ModuleData d : data) {
            Optional<Module> existing = moduleRepository.findByCode(d.code);
            if (existing.isPresent()) {
                result.add(existing.get());
            } else {
                ParentModule parent = parentModules.stream()
                        .filter(pm -> pm.getCode().equals(d.parentCode))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("ParentModule no encontrado: " + d.parentCode));
                Module module = Module.builder()
                        .code(d.code).title(d.title).subtitle("").type(d.type).icon(d.icon)
                        .link(d.link).moduleOrder(d.moduleOrder).parentModule(parent).status(true).build();
                moduleRepository.save(module);
                result.add(module);
            }
        }
        return result;
    }

    // ── FASE 4: Roles ──
    private List<Role> seedRoles() {
        List<Role> result = new ArrayList<>();
        RoleData[] data = {
                new RoleData("ADMIN", "Administrador", "Acceso completo al sistema"),
                new RoleData("SECRETARIA", "Secretaria", "Gestión administrativa y registro de personas"),
                new RoleData("ESTUDIANTE", "Estudiante", "Estudiante de posgrado"),
                new RoleData("DOCENTE", "Docente", "Docente de posgrado"),
                new RoleData("ASESOR", "Asesor", "Asesor de tesis"),
                new RoleData("COORDINADOR", "Coordinador", "Coordinador académico"),
                new RoleData("REVISOR", "Revisor", "Revisor de trabajos de investigación"),
                new RoleData("JURADO", "Jurado", "Jurado de tesis"),
                // ── Roles de la facultad (grupo en la descripción) ──
                new RoleData("DECANO", "Decano de la Facultad", "Monitoreo"),
                new RoleData("VICEDECANO", "Vicedecano de Investigación", "Monitoreo"),
                new RoleData("JEFE_UPG", "Jefe de la Unidad de Posgrado", "Monitoreo"),
                new RoleData("COORD_SEC", "Coordinador de Sección", "Monitoreo"),
                new RoleData("COORD_PROG", "Coordinador del Programa", "Edición"),
                new RoleData("PROF_RESP", "Profesor responsable", "Edición"),
                new RoleData("PROF_TUTOR", "Profesor tutor", "Edición"),
                new RoleData("PERS_ADMIN", "Personal administrativo", "Edición"),
        };
        for (RoleData d : data) {
            Optional<Role> existing = roleRepository.findByCode(d.code);
            if (existing.isPresent()) {
                result.add(existing.get());
            } else {
                Role role = Role.builder()
                        .code(d.code).name(d.name).description(d.description).status(true).build();
                roleRepository.save(role);
                result.add(role);
                LOG.info("   ✅ Role creado: " + d.code);
            }
        }
        return result;
    }

    // ── FASE 5: Role-Module (menú por rol según matriz de permisos) ──
    private void seedRoleModules(List<Role> roles, List<Module> modules) {
        // roleCode -> códigos de módulo visibles para ese rol
        Map<String, List<String>> matriz = new LinkedHashMap<>();
        matriz.put("ADMIN", List.of("01", "03", "04", "05", "20", "21", "22", "24", "25", "26", "35", "36", "37", "30", "31", "33", "38"));
        matriz.put("SECRETARIA", List.of("20", "21", "22", "24", "25", "26", "35", "36", "37", "30", "31", "33", "38"));
        matriz.put("COORDINADOR", List.of("21", "22", "35", "36", "37", "30", "31", "33"));
        matriz.put("ESTUDIANTE", List.of("23", "34"));
        matriz.put("DOCENTE", List.of("23", "29"));
        matriz.put("JURADO", List.of("23"));
        // Gestión adicional (edición) y monitoreo (solo lectura) ven el reporte de tutores.
        matriz.put("COORD_PROG", List.of("30", "31", "33")); // registro de tema
        matriz.put("PERS_ADMIN", List.of("30", "31"));
        matriz.put("DECANO", List.of("31"));
        matriz.put("VICEDECANO", List.of("31"));
        matriz.put("JEFE_UPG", List.of("31"));
        matriz.put("COORD_SEC", List.of("31"));
        matriz.put("PROF_TUTOR", List.of("32")); // panel del tutor


        int created = 0;
        for (Map.Entry<String, List<String>> entry : matriz.entrySet()) {
            Role role = roles.stream().filter(r -> entry.getKey().equals(r.getCode())).findFirst().orElse(null);
            if (role == null) {
                continue;
            }
            for (String modCode : entry.getValue()) {
                Module module = modules.stream().filter(m -> modCode.equals(m.getCode())).findFirst().orElse(null);
                if (module == null) {
                    continue;
                }
                if (!roleModuleRepository.findByModuleInAndRole(List.of(module), role).isEmpty()) {
                    continue;
                }
                roleModuleRepository.save(RoleModule.builder()
                        .role(role).module(module).assigned(true).build());
                created++;
            }
        }
        LOG.info("   ✅ Role-Module: " + created + " asignaciones");
    }

    // ── FASE 6: User-Role (admin → ADMIN) ──
    private void seedUserRoles(User adminUser, List<Role> roles) {
        Role adminRole = roles.stream()
                .filter(r -> ADMIN_ROLE_CODE.equals(r.getCode()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol admin no encontrado"));

        boolean already = userRoleRepository.findByUser(adminUser).stream()
                .anyMatch(ur -> ur.getRole() != null && adminRole.getId().equals(ur.getRole().getId()));
        if (already) {
            return;
        }
        userRoleRepository.save(UserRoleAssignment.builder()
                .user(adminUser).role(adminRole).assigned(true).build());
        LOG.info("   ✅ User-Role: admin → ADMIN");
    }

    // ── FASE 7: Programas de posgrado ──
    private void seedProgramasPosgrado() {
        // Cada programa pertenece a una facultad (cascada Facultad → Programa).
        ProgramaData[] data = {
                // Facultad de Medicina
                new ProgramaData("FM", "DOCTORADO EN MEDICINA", NivelPrograma.DOCTORADO),
                new ProgramaData("FM", "DOCTORADO EN CIENCIAS DE LA SALUD", NivelPrograma.DOCTORADO),
                new ProgramaData("FM", "DOCTORADO EN SALUD PÚBLICA", NivelPrograma.DOCTORADO),
                new ProgramaData("FM", "MAESTRÍA EN MEDICINA", NivelPrograma.MAESTRIA),
                new ProgramaData("FM", "MAESTRÍA EN SALUD PÚBLICA", NivelPrograma.MAESTRIA),
                new ProgramaData("FM", "MAESTRÍA EN EPIDEMIOLOGÍA", NivelPrograma.MAESTRIA),
                // Facultad de Farmacia y Bioquímica
                new ProgramaData("FFB", "DOCTORADO EN FARMACIA Y BIOQUÍMICA", NivelPrograma.DOCTORADO),
                new ProgramaData("FFB", "MAESTRÍA EN CIENCIAS FARMACÉUTICAS", NivelPrograma.MAESTRIA),
                new ProgramaData("FFB", "MAESTRÍA EN RECURSOS VEGETALES Y TERAPÉUTICOS", NivelPrograma.MAESTRIA),
                // Facultad de Odontología
                new ProgramaData("FO", "DOCTORADO EN ESTOMATOLOGÍA", NivelPrograma.DOCTORADO),
                new ProgramaData("FO", "MAESTRÍA EN ESTOMATOLOGÍA", NivelPrograma.MAESTRIA),
                // Facultad de Ciencias Biológicas
                new ProgramaData("FCB", "DOCTORADO EN CIENCIAS BIOLÓGICAS", NivelPrograma.DOCTORADO),
                new ProgramaData("FCB", "MAESTRÍA EN BIOTECNOLOGÍA", NivelPrograma.MAESTRIA),
                new ProgramaData("FCB", "MAESTRÍA EN MICROBIOLOGÍA", NivelPrograma.MAESTRIA),
                // Facultad de Enfermería
                new ProgramaData("FE", "DOCTORADO EN CIENCIAS DE ENFERMERÍA", NivelPrograma.DOCTORADO),
                new ProgramaData("FE", "MAESTRÍA EN ENFERMERÍA", NivelPrograma.MAESTRIA),
                // Facultad de Psicología
                new ProgramaData("FP", "DOCTORADO EN PSICOLOGÍA", NivelPrograma.DOCTORADO),
                new ProgramaData("FP", "MAESTRÍA EN PSICOLOGÍA CLÍNICA Y DE LA SALUD", NivelPrograma.MAESTRIA),
        };
        int created = 0;
        for (ProgramaData d : data) {
            if (programaPosgradoRepository.existsByNombre(d.nombre)) {
                continue;
            }
            var facultad = facultadRepository.buscarPorCodigo(d.facultadCodigo).orElse(null);
            programaPosgradoRepository.save(ProgramaPosgrado.builder()
                    .nombre(d.nombre).nivel(d.nivel).facultad(facultad).build());
            created++;
        }
        LOG.info("   ✅ Programas posgrado: " + created + " creados");
    }

    // ── FASE 8: Catálogo de cargos (posgrado de medicina UNMSM) ──
    private void seedCargos() {
        CargoData[] data = {
                new CargoData("CG01", "Médico Residente", "Médico en formación dentro del programa de residentado médico"),
                new CargoData("CG02", "Médico Asistente", "Médico asistente de un servicio hospitalario"),
                new CargoData("CG03", "Médico Especialista", "Médico con especialidad reconocida"),
                new CargoData("CG04", "Jefe de Servicio", "Jefe de un servicio clínico o quirúrgico"),
                new CargoData("CG05", "Jefe de Departamento", "Jefe de un departamento asistencial"),
                new CargoData("CG06", "Coordinador de Residentado Médico", "Coordinador del programa de residentado de la sede docente"),
                new CargoData("CG07", "Tutor de Residentes", "Tutor académico de médicos residentes"),
                new CargoData("CG08", "Director de Hospital", "Director general del establecimiento de salud"),
                new CargoData("CG09", "Director de la Unidad de Posgrado", "Director de la Unidad de Posgrado de la Facultad de Medicina"),
                new CargoData("CG10", "Coordinador de Programa de Maestría", "Coordinador académico de un programa de maestría"),
                new CargoData("CG11", "Coordinador de Programa de Doctorado", "Coordinador académico de un programa de doctorado"),
                new CargoData("CG12", "Docente Principal", "Profesor principal de la Facultad de Medicina"),
                new CargoData("CG13", "Docente Asociado", "Profesor asociado de la Facultad de Medicina"),
                new CargoData("CG14", "Docente Auxiliar", "Profesor auxiliar de la Facultad de Medicina"),
                new CargoData("CG15", "Profesor Investigador", "Docente dedicado a la investigación científica"),
        };
        int created = 0;
        for (CargoData d : data) {
            if (cargoRepository.existsByNombre(d.nombre)) {
                continue;
            }
            cargoRepository.save(Cargo.builder()
                    .codigoSistema(d.codigo).nombre(d.nombre).descripcion(d.descripcion).build());
            created++;
        }
        LOG.info("   ✅ Cargos: " + created + " creados");
    }

    // ── Configuración: Facultades ──
    private void seedFacultades() {
        String[][] data = {
                {"FM", "Facultad de Medicina"},
                {"FFB", "Facultad de Farmacia y Bioquímica"},
                {"FO", "Facultad de Odontología"},
                {"FCB", "Facultad de Ciencias Biológicas"},
                {"FE", "Facultad de Enfermería"},
                {"FP", "Facultad de Psicología"},
        };
        int created = 0;
        for (String[] d : data) {
            if (facultadRepository.existsByNombre(d[1])) {
                continue;
            }
            facultadRepository.save(unmsm.edu.pe.configuracion.domain.entities.Facultad.builder()
                    .codigo(d[0]).nombre(d[1]).build());
            created++;
        }
        LOG.info("   ✅ Facultades: " + created + " creadas");
    }

    // ── Configuración: Parámetros del sistema (valores institucionales de plantillas) ──
    private void seedParametrosSistema() {
        String[][] data = {
                {"CIUDAD_EMISION", "Lima", "Ciudad de emisión de documentos oficiales"},
                {"NOMBRE_UNIVERSIDAD", "Universidad Nacional Mayor de San Marcos", "Nombre oficial de la universidad"},
                {"ESCUELA_POSGRADO", "Escuela de Posgrado", "Nombre de la escuela de posgrado"},
                {"FACULTAD", "Facultad de Medicina", "Facultad emisora en el encabezado de documentos"},
                {"TRATAMIENTO_DOCENTE", "Señor(a) docente", "Tratamiento al docente en la solicitud de asesoría"},
                {"NOMBRE_DIRECTOR", "Dr. Roberto Salazar Bravo", "Director de la Unidad de Posgrado (fallback si no hay cargo vigente asignado)"},
                {"CONSIDERANDOS_DICTAMEN",
                        "Que, mediante Resolución Decanal se aprobó el Reglamento de Grados y Títulos de la Facultad, que regula la designación de asesores de tesis de posgrado;\n"
                                + "Que, el estudiante ha presentado la solicitud de designación de asesor adjuntando la carta de aceptación del docente;\n"
                                + "Que, la Unidad de Posgrado ha verificado el cumplimiento de los requisitos establecidos;\n"
                                + "Estando a lo expuesto y en uso de las atribuciones conferidas;",
                        "Cuerpo CONSIDERANDO del dictamen (parametrizable por año/norma)"},
                {"PIE_DICTAMEN", "Documento firmado digitalmente. Copia auténtica imprimible; su autenticidad puede verificarse en el portal institucional.",
                        "Pie de los dictámenes (nota de copia auténtica)"},
        };
        int created = 0;
        for (String[] d : data) {
            if (parametroSistemaRepository.existsByClave(d[0])) {
                continue;
            }
            parametroSistemaRepository.save(unmsm.edu.pe.configuracion.domain.entities.ParametroSistema.builder()
                    .clave(d[0]).valor(d[1]).descripcion(d[2]).build());
            created++;
        }
        LOG.info("   ✅ Parámetros del sistema: " + created + " creados");
    }

    // ── Configuración: Lemas anuales (nombre oficial del año). Agregar años siguientes aquí. ──
    private void seedLemasAnuales() {
        Object[][] data = {
                {2026, "Año de la Esperanza y el Fortalecimiento de la Democracia"},
        };
        int created = 0;
        for (Object[] d : data) {
            int anio = (int) d[0];
            String texto = (String) d[1];
            if (lemaAnualRepository.existeActivoPorAnio(anio)) {
                continue;
            }
            var lema = unmsm.edu.pe.configuracion.domain.entities.LemaAnual.builder()
                    .anio(anio).texto(texto).build();
            lema.setActive(true);
            lemaAnualRepository.save(lema);
            created++;
        }
        LOG.info("   ✅ Lemas anuales: " + created + " creados");
    }

    // ── FASE 9: Catálogo de centros laborales (sedes docentes UNMSM - Medicina) ──
    private void seedCentrosLaborales() {
        CentroData[] data = {
                new CentroData("CL01", "Hospital Nacional Dos de Mayo", "Sede docente - Cercado de Lima"),
                new CentroData("CL02", "Hospital Nacional Arzobispo Loayza", "Sede docente - Cercado de Lima"),
                new CentroData("CL03", "Hospital Nacional Guillermo Almenara Irigoyen", "Sede docente EsSalud - La Victoria"),
                new CentroData("CL04", "Hospital Nacional Edgardo Rebagliati Martins", "Sede docente EsSalud - Jesús María"),
                new CentroData("CL05", "Hospital Nacional Hipólito Unanue", "Sede docente - El Agustino"),
                new CentroData("CL06", "Hospital Nacional Daniel Alcides Carrión", "Sede docente - Callao"),
                new CentroData("CL07", "Hospital Nacional Cayetano Heredia", "Sede docente - San Martín de Porres"),
                new CentroData("CL08", "Hospital Nacional Docente Madre Niño San Bartolomé", "Sede docente - Cercado de Lima"),
                new CentroData("CL09", "Instituto Nacional de Salud del Niño", "Instituto especializado - Breña"),
                new CentroData("CL10", "Instituto Nacional de Enfermedades Neoplásicas", "Instituto especializado en oncología - Surquillo"),
                new CentroData("CL11", "Instituto Nacional Materno Perinatal", "Maternidad de Lima - Cercado de Lima"),
                new CentroData("CL12", "Instituto Nacional de Ciencias Neurológicas", "Instituto especializado en neurología - Cercado de Lima"),
                new CentroData("CL13", "Hospital de Emergencias José Casimiro Ulloa", "Sede docente - Miraflores"),
                new CentroData("CL14", "Instituto Nacional de Salud", "Organismo de investigación del MINSA"),
                new CentroData("CL15", "Hospital Militar Central", "Sede docente - Jesús María"),
                new CentroData("CL16", "Hospital Nacional Luis N. Sáenz PNP", "Hospital de la Policía Nacional - Jesús María"),
                new CentroData("CL17", "Universidad Nacional Mayor de San Marcos", "Facultad de Medicina San Fernando"),
                new CentroData("CL18", "Ministerio de Salud", "Sector salud del Estado peruano"),
        };
        int created = 0;
        for (CentroData d : data) {
            if (centroLaboralRepository.existsByNombre(d.nombre)) {
                continue;
            }
            centroLaboralRepository.save(CentroLaboral.builder()
                    .codigoSistema(d.codigo).nombre(d.nombre).descripcion(d.descripcion).build());
            created++;
        }
        LOG.info("   ✅ Centros laborales: " + created + " creados");
    }

    // ── FASE 9.1: Catálogo de líneas de investigación (posgrado de medicina UNMSM) ──
    private void seedLineasInvestigacion() {
        LineaData[] data = {
                new LineaData("LI01", "Oncología y Cáncer", "Investigación en prevención, diagnóstico y tratamiento del cáncer"),
                new LineaData("LI02", "Enfermedades Infecciosas y Tropicales", "Epidemiología y manejo de enfermedades infecciosas y tropicales"),
                new LineaData("LI03", "Salud Pública y Epidemiología", "Determinantes de la salud, vigilancia epidemiológica y políticas sanitarias"),
                new LineaData("LI04", "Enfermedades Crónicas No Transmisibles", "Diabetes, hipertensión y enfermedades cardiovasculares"),
                new LineaData("LI05", "Salud Materno-Infantil", "Salud de la gestante, el neonato y el niño"),
                new LineaData("LI06", "Neurociencias y Salud Mental", "Trastornos neurológicos y de la salud mental"),
                new LineaData("LI07", "Farmacología y Terapéutica", "Estudios de medicamentos, farmacovigilancia y ensayos clínicos"),
                new LineaData("LI08", "Medicina Genómica y Biología Molecular", "Genética médica, biomarcadores y medicina de precisión"),
                new LineaData("LI09", "Nutrición y Enfermedades Metabólicas", "Nutrición clínica, obesidad y trastornos metabólicos"),
                new LineaData("LI10", "Educación Médica e Innovación Docente", "Formación de recursos humanos en salud e innovación pedagógica"),
        };
        int created = 0;
        for (LineaData d : data) {
            if (lineaInvestigacionRepository.existsByNombre(d.nombre)) {
                continue;
            }
            lineaInvestigacionRepository.save(LineaInvestigacion.builder()
                    .codigo(d.codigo).nombre(d.nombre).descripcion(d.descripcion).build());
            created++;
        }
        LOG.info("   ✅ Líneas de investigación: " + created + " creadas");
    }

    // ── FASE 9.2: Usuarios de prueba (SOLO DESARROLLO, %dev) ──
    // Todas las cuentas de prueba comparten esta contraseña. Usuario = <primerapellido>.<primernombre>.
    private static final String PRUEBA_PASSWORD = "contra123";

    /** Usuarios de gestión (aparte de los 20): {roleCode, username, apPat, apMat, nombres}. */
    private static final String[][] GESTION = {
            {"COORDINADOR", "coordinador", "Coordinador", "Programa", "Ana María"},
            {"SECRETARIA", "secretaria", "Secretaría", "Posgrado", "Rosa Elena"},
    };

    /** 10 estudiantes de prueba: {apPat, apMat, nombres}. */
    private static final String[][] ESTUDIANTES = {
            {"Quispe", "Huamán", "Ana"}, {"Mamani", "Flores", "Luis"}, {"Flores", "Vargas", "Carmen"},
            {"Huamán", "Rojas", "José"}, {"Condori", "Ticona", "Rosa"}, {"Vargas", "Salas", "Pedro"},
            {"Rojas", "Ponce", "Lucía"}, {"Chávez", "Díaz", "Miguel"}, {"Ramos", "Reyes", "Elena"},
            {"Torres", "Soto", "Jorge"},
    };

    /** 10 docentes de prueba: {apPat, apMat, nombres}. Los 3 primeros también son tutores; los 3 siguientes, asesores. */
    private static final String[][] DOCENTES = {
            {"Salazar", "Bravo", "Roberto"}, {"Núñez", "Ledesma", "Patricia"}, {"Paredes", "Cabrera", "Carlos"},
            {"Cáceres", "Campos", "Silvia"}, {"Ríos", "Vega", "Fernando"}, {"Vega", "Salas", "Marta"},
            {"Ledesma", "Tapia", "Andrés"}, {"Bravo", "Reyes", "Teresa"}, {"Cabrera", "Soto", "Julio"},
            {"Ponce", "Díaz", "Diana"},
    };

    /**
     * Crea SOLO los usuarios de prueba: 2 de gestión aparte (coordinador, secretaria) + 10 estudiantes +
     * 10 docentes. Sin relaciones (tutorías/temas/sugerencias): se crean desde la UI. El admin ya existe.
     */
    private void seedUsuariosPrueba(List<Role> roles) {
        if (!devUsersEnabled) {
            LOG.info("   ⏭️  Usuarios de prueba omitidos (app.seeders.dev-users=false)");
            return;
        }
        Role rolEstudiante = rol(roles, "ESTUDIANTE");
        Role rolDocente = rol(roles, "DOCENTE");
        Role rolTutor = rol(roles, "PROF_TUTOR");
        Role rolAsesor = rol(roles, "ASESOR");
        List<ProgramaPosgrado> programas = programaPosgradoRepository.getAll();
        List<LineaInvestigacion> lineas = lineaInvestigacionRepository.listar(null, 0, 50);
        int seq = 1;

        // Usuarios de gestión (aparte). El admin ya se creó en seedAdminUser.
        int gest = 0;
        for (String[] g : GESTION) {
            Role role = rol(roles, g[0]);
            if (role == null) {
                continue;
            }
            Optional<User> ex = userRepository.findByUsername(g[1]);
            User u;
            if (ex.isPresent()) {
                u = ex.get();
                u.setPassword(passwordEncoder.encode(PRUEBA_PASSWORD)); // entidad gestionada → se flushea
            } else {
                u = crearUsuarioDemo(g[1], g[4], g[2], g[3]);
                crearPersonaDemo(dni(seq++), g[2], g[3], g[4], Sexo.MUJER, u);
                gest++;
            }
            asignarRolSiFalta(u, role);
        }

        // 10 estudiantes (rol ESTUDIANTE).
        int est = 0;
        for (int i = 0; i < ESTUDIANTES.length; i++) {
            String[] p = ESTUDIANTES[i];
            String username = username(p[0], p[2]);
            Optional<User> ex = userRepository.findByUsername(username);
            User u;
            if (ex.isPresent()) {
                u = ex.get();
                u.setPassword(passwordEncoder.encode(PRUEBA_PASSWORD));
            } else {
                String d = dni(seq++);
                u = crearUsuarioDemo(username, p[2], p[0], p[1]);
                Persona persona = crearPersonaDemo(d, p[0], p[1], p[2], Sexo.MUJER, u);
                ProgramaPosgrado prog = programas.isEmpty() ? null : programas.get(i % programas.size());
                estudianteRepository.save(Estudiante.builder()
                        .persona(persona).codigoSistema("EST-" + d).codMatricula("M" + d)
                        .emailInstitucional(username + "@unmsm.edu.pe").anioIngreso(2024)
                        .condicion(CondicionEstudiante.REGULAR).financiamiento(Financiamiento.AUTOFINANCIADO)
                        .programa(prog).build());
                est++;
            }
            asignarRolSiFalta(u, rolEstudiante);
        }

        // 10 docentes (rol DOCENTE; 1-3 también PROF_TUTOR, 4-6 también ASESOR). Cada uno con 2 líneas.
        int doc = 0;
        for (int i = 0; i < DOCENTES.length; i++) {
            String[] p = DOCENTES[i];
            String username = username(p[0], p[2]);
            Optional<User> ex = userRepository.findByUsername(username);
            User u;
            if (ex.isPresent()) {
                u = ex.get();
                u.setPassword(passwordEncoder.encode(PRUEBA_PASSWORD));
            } else {
                String d = dni(seq++);
                u = crearUsuarioDemo(username, p[2], p[0], p[1]);
                Persona persona = crearPersonaDemo(d, p[0], p[1], p[2], Sexo.HOMBRE, u);
                Docente docente = docenteRepository.save(Docente.builder()
                        .persona(persona).codigoSistema("DOC-" + d).emailInstitucional(username + "@unmsm.edu.pe")
                        .categoria(CategoriaDocente.PRINCIPAL)
                        .condicion(CondicionDocente.NOMBRADO).cupoMaximoTutoria(20).build());
                if (lineas.size() >= 2) {
                    asignarLineasDocente(docente, lineas.get(i % lineas.size()), true,
                            lineas.get((i + 3) % lineas.size()));
                }
                // Grados académicos (lista a nivel de persona): Bachiller + Magíster + Doctor(principal).
                asignarGradosDemo(persona, i);
                doc++;
            }
            asignarRolSiFalta(u, rolDocente);
            if (i < 3) {
                asignarRolSiFalta(u, rolTutor);
            } else if (i < 6) {
                asignarRolSiFalta(u, rolAsesor);
            }
        }

        LOG.info("   ✅ Usuarios de prueba (SOLO DEV): " + gest + " gestión (coordinador, secretaria) + "
                + est + " estudiantes + " + doc + " docentes. Usuario=<apellido>.<nombre>, clave=" + PRUEBA_PASSWORD);
    }

    private Role rol(List<Role> roles, String code) {
        return roles.stream().filter(r -> code.equals(r.getCode())).findFirst().orElse(null);
    }

    /** Asigna el rol solo si el usuario aún no lo tiene (idempotente; reconcilia usuarios preexistentes). */
    private void asignarRolSiFalta(User user, Role role) {
        if (role == null) {
            return;
        }
        boolean tiene = userRoleRepository.findByUser(user).stream()
                .anyMatch(a -> a.getRole() != null && role.getId() != null && role.getId().equals(a.getRole().getId()));
        if (!tiene) {
            userRoleRepository.save(UserRoleAssignment.builder().user(user).role(role).assigned(true).build());
        }
    }

    private String dni(int seq) {
        return String.format("80%06d", seq);
    }

    private String username(String apPat, String nombres) {
        return normaliza(apPat.split("\\s+")[0]) + "." + normaliza(nombres.split("\\s+")[0]);
    }

    private String normaliza(String s) {
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private User crearUsuarioDemo(String username, String nombres, String apPat, String apMat) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@unmsm.edu.pe");
        user.setPassword(passwordEncoder.encode(PRUEBA_PASSWORD));
        user.setFirstName(nombres);
        user.setLastName((apPat + " " + apMat).trim());
        user.setStatus(UserStatus.ACTIVE);
        userRepository.saveUser(user);
        return user;
    }

    private Persona crearPersonaDemo(String dni, String apPat, String apMat, String nombres, Sexo sexo, User user) {
        Persona persona = Persona.builder()
                .tipoDocumento(TipoDocumento.DNI).numeroDocumento(dni)
                .apellidoPaterno(apPat).apellidoMaterno(apMat).nombres(nombres)
                .sexo(sexo).nacionalidad("Peruana").procedencia(Procedencia.NACIONAL)
                .emailPersonal(user.getUsername() + "@gmail.com").discapacidad(false).user(user).build();
        personaRepository.save(persona);
        return persona;
    }


    private static final String[] UNIVERSIDADES = {
            "Universidad Nacional Mayor de San Marcos", "Pontificia Universidad Católica del Perú",
            "Universidad Peruana Cayetano Heredia", "Universidad Nacional de Ingeniería",
            "Universidad de Barcelona", "Universidad de Buenos Aires",
    };

    /** Grados demo del docente: Bachiller + Magíster (+ Doctor para los primeros); un solo principal. */
    private void asignarGradosDemo(Persona persona, int i) {
        String uni1 = UNIVERSIDADES[i % UNIVERSIDADES.length];
        String uni2 = UNIVERSIDADES[(i + 2) % UNIVERSIDADES.length];
        boolean tieneDoctor = i < 6;
        List<PersonaGradoAcademico> grados = new ArrayList<>();
        grados.add(gradoDemo(persona, GradoAcademico.BACHILLER, 2008 + (i % 5), uni1, false));
        grados.add(gradoDemo(persona, GradoAcademico.MAGISTER, 2013 + (i % 5), uni1, !tieneDoctor));
        if (tieneDoctor) {
            grados.add(gradoDemo(persona, GradoAcademico.DOCTOR, 2018 + (i % 4), uni2, true));
        }
        personaGradoAcademicoRepository.saveAll(grados);
    }

    private PersonaGradoAcademico gradoDemo(Persona persona, GradoAcademico g, int anio, String universidad, boolean principal) {
        return PersonaGradoAcademico.builder()
                .persona(persona).grado(g).anio(anio).universidad(universidad).principal(principal).build();
    }

    private void asignarLineasDocente(Docente docente, LineaInvestigacion linea1, boolean linea1Principal,
                                      LineaInvestigacion linea2) {
        docenteLineaInvestigacionRepository.saveAll(List.of(
                DocenteLineaInvestigacion.builder()
                        .docente(docente).lineaInvestigacion(linea1).esPrincipal(linea1Principal).build(),
                DocenteLineaInvestigacion.builder()
                        .docente(docente).lineaInvestigacion(linea2).esPrincipal(!linea1Principal).build()
        ));
    }


    // ── Data holders ──
    private record ParentModuleData(String code, String title, String subtitle, String type,
                                    String icon, String link, Integer moduleOrder) {}

    private record ModuleData(String code, String title, String type, String icon,
                              String link, Integer moduleOrder, String parentCode) {}

    private record RoleData(String code, String name, String description) {}

    private record ProgramaData(String facultadCodigo, String nombre, NivelPrograma nivel) {}

    private record CargoData(String codigo, String nombre, String descripcion) {}

    private record CentroData(String codigo, String nombre, String descripcion) {}

    private record LineaData(String codigo, String nombre, String descripcion) {}

}
