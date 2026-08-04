package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.shared.utils.EstadoDerivado;
import unmsm.edu.pe.tesis.application.dto.SeguimientoAlumnoItem;
import unmsm.edu.pe.tesis.application.dto.SeguimientoResponse;
import unmsm.edu.pe.tesis.application.dto.SeguimientoResumen;
import unmsm.edu.pe.tesis.application.util.EtapasProceso;
import unmsm.edu.pe.tesis.domain.repositories.SeguimientoRepository;
import unmsm.edu.pe.tesis.domain.services.SeguimientoService;
import unmsm.edu.pe.tesis.infrastructure.export.SeguimientoExcel;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Arma el tablero de seguimiento con UNA sola consulta: por cada doctorando deriva la etapa
 * en curso a partir de los hitos ya persistidos (tutor, dictamen, expediente, revisores,
 * defensa, informe final, jurado informante) y traduce ese estado a "qué falta y quién debe
 * hacerlo". No guarda nada: el seguimiento se calcula en lectura, igual que las notificaciones.
 */
@ApplicationScoped
public class SeguimientoServiceImpl implements SeguimientoService {

    /** Tope de filas del tablero (el universo de doctorandos de posgrado es pequeño). */
    private static final int LIMITE = 500;

    /** Vigencia del dictamen de aprobación del proyecto, en años. */
    private static final int VIGENCIA_DICTAMEN_ANIOS = 4;
    /** A partir de cuántos días sin movimiento se considera "detenido". */
    private static final int DIAS_DETENIDO = 30;
    /** A cuántos meses del vencimiento se empieza a avisar. */
    private static final int MESES_AVISO_VIGENCIA = 6;

    /** Responsables posibles de la acción pendiente. */
    private static final String SECRETARIA = "SECRETARIA";
    private static final String COORDINADOR = "COORDINADOR";
    private static final String ASESOR = "ASESOR";
    private static final String REVISOR = "REVISOR";
    private static final String JURADO = "JURADO";
    private static final String ESTUDIANTE = "ESTUDIANTE";
    private static final String NADIE = "NADIE";

    @Inject SeguimientoRepository repository;

    @Override
    @Transactional
    public SeguimientoResponse tablero(UUID programaId, String buscar) {
        List<SeguimientoAlumnoItem> alumnos = new ArrayList<>();
        for (Object[] f : repository.seguimiento(programaId, buscar, LIMITE)) {
            alumnos.add(mapear(f));
        }
        return SeguimientoResponse.builder()
                .resumen(resumir(alumnos))
                .alumnos(alumnos)
                .build();
    }

    @Override
    @Transactional
    public byte[] excel(String buscar, Integer etapa, String responsable, String programa) {
        List<SeguimientoAlumnoItem> alumnos = tablero(null, buscar).getAlumnos().stream()
                .filter(a -> etapa == null || a.getEtapaNumero() == etapa)
                .filter(a -> vacio(responsable) || responsable.equalsIgnoreCase(a.getResponsable()))
                .filter(a -> vacio(programa) || programa.equalsIgnoreCase(a.getProgramaNombre()))
                .toList();
        return SeguimientoExcel.generar(alumnos);
    }

    private boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    // ── mapeo de la fila ──
    private SeguimientoAlumnoItem mapear(Object[] f) {
        UUID tesisId = uuid(f[6]);
        boolean tieneTesis = tesisId != null;
        String tutor = str(f[10]);
        String asesor = str(f[11]);
        boolean dictamenFirmado = num(f[12]) == 1;
        boolean cartaAsesor = bool(f[14]);
        boolean expedienteSubido = bool(f[15]);
        boolean expedienteRecibido = bool(f[16]);
        boolean revisoresConformes = bool(f[17]);
        boolean defensaProgramada = bool(f[18]);
        boolean informeFinalAprobado = bool(f[19]);
        boolean juradoSolicitado = bool(f[20]);
        boolean informeRevisado = bool(f[21]);
        long revisores = num(f[22]);
        boolean rubricaSubida = num(f[23]) == 1;
        long juradoInforme = num(f[24]);
        String estadoTesis = str(f[9]);
        boolean sustentado = "SUSTENTADO".equals(estadoTesis);

        int etapa = etapaEnCurso(tieneTesis, tutor != null, dictamenFirmado, expedienteSubido,
                defensaProgramada, informeFinalAprobado, informeRevisado, sustentado);

        Pendiente pendiente = pendiente(etapa, tutor != null, asesor != null, dictamenFirmado, cartaAsesor,
                expedienteSubido, expedienteRecibido, revisores, rubricaSubida, revisoresConformes,
                defensaProgramada, informeFinalAprobado, juradoSolicitado, juradoInforme, informeRevisado);

        String estadoDerivado = EstadoDerivado.resolver(tesisId, estadoTesis, asesor != null);

        // Último hito registrado: la fecha más reciente entre todas las que ya se guardaron.
        // fecha_defensa NO entra: es una fecha futura (la defensa aún no ocurre).
        LocalDate ultimoHito = ultimaFecha(f, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36);
        Integer diasEnEtapa = ultimoHito == null || sustentado
                ? null
                : (int) Math.max(0, ChronoUnit.DAYS.between(ultimoHito, LocalDate.now()));

        // Vigencia del dictamen del proyecto: 4 años desde la defensa.
        LocalDate fechaDefensa = fecha(f[25]);
        LocalDate venceEl = defensaProgramada && !sustentado && fechaDefensa != null
                ? fechaDefensa.plusYears(VIGENCIA_DICTAMEN_ANIOS) : null;
        Integer vigenciaMeses = venceEl == null
                ? null : (int) ChronoUnit.MONTHS.between(LocalDate.now(), venceEl);

        return SeguimientoAlumnoItem.builder()
                .estudianteId(uuid(f[0]))
                .tesisId(tesisId)
                .apellidos(nombreCompuesto(str(f[1]), str(f[2])))
                .nombres(str(f[3]))
                .codigoSistema(str(f[4]))
                .programaNombre(str(f[5]))
                .tituloTesis(str(f[7]))
                .lineaNombre(str(f[8]))
                .tutorNombre(tutor)
                .asesorNombre(asesor)
                .etapaNumero(etapa)
                .etapaTitulo(EtapasProceso.corto(etapa))
                .avancePct(EtapasProceso.avancePct(etapa))
                .estadoDerivado(estadoDerivado)
                .estadoLabel(EstadoDerivado.etiqueta(estadoDerivado))
                .pendiente(pendiente.texto())
                .responsable(pendiente.responsable())
                .pendienteSecretaria(SECRETARIA.equals(pendiente.responsable()))
                .accionLink(enlaceConTesis(pendiente.link(), tesisId))
                .accionLabel(pendiente.label())
                .fechaUltimoHito(ultimoHito)
                .diasEnEtapa(diasEnEtapa)
                .fechaDefensa(fechaDefensa)
                .vigenciaVenceEl(venceEl)
                .vigenciaMeses(vigenciaMeses)
                .vigenciaVencida(venceEl != null && venceEl.isBefore(LocalDate.now()))
                .build();
    }

    /** Agrega {@code ?tesis=} para que la bandeja de destino resalte la fila del alumno. */
    private String enlaceConTesis(String link, UUID tesisId) {
        if (link == null) return null;
        return tesisId == null ? link : link + "?tesis=" + tesisId;
    }

    /** La más reciente de las fechas indicadas (ignora nulos). */
    private LocalDate ultimaFecha(Object[] f, int... indices) {
        LocalDate max = null;
        for (int i : indices) {
            LocalDate d = fecha(f[i]);
            if (d != null && (max == null || d.isAfter(max))) {
                max = d;
            }
        }
        return max;
    }

    /**
     * Etapa en curso (1..8; 9 = finalizado). Cada etapa se cierra con el hito que la define:
     * tema → tutor → dictamen de asesor → expediente enviado → defensa programada →
     * informe final aprobado → informe revisado por el jurado → sustentación.
     */
    private int etapaEnCurso(boolean tieneTesis, boolean tieneTutor, boolean dictamenFirmado,
                             boolean expedienteSubido, boolean defensaProgramada,
                             boolean informeFinalAprobado, boolean informeRevisado, boolean sustentado) {
        if (sustentado) return EtapasProceso.TOTAL + 1;
        if (!tieneTesis) return 1;
        if (!tieneTutor) return 2;
        if (!dictamenFirmado) return 3;
        if (!expedienteSubido) return 4;
        if (!defensaProgramada) return 5;
        if (!informeFinalAprobado) return 6;
        if (!informeRevisado) return 7;
        return 8;
    }

    /** Acción concreta que destraba al alumno en su etapa actual, y de quién depende. */
    private Pendiente pendiente(int etapa, boolean tieneTutor, boolean tieneAsesor, boolean dictamenFirmado,
                                boolean cartaAsesor, boolean expedienteSubido, boolean expedienteRecibido,
                                long revisores, boolean rubricaSubida, boolean revisoresConformes,
                                boolean defensaProgramada, boolean informeFinalAprobado, boolean juradoSolicitado,
                                long juradoInforme, boolean informeRevisado) {
        switch (etapa) {
            case 1:
                return new Pendiente("Registrar el tema de tesis", COORDINADOR,
                        "/admin/registro-tema", "Registrar tema");
            case 2:
                return new Pendiente("Designar tutor", COORDINADOR,
                        "/admin/asignar-tutor", "Designar tutor");
            case 3:
                if (!tieneAsesor) {
                    return new Pendiente("El alumno debe solicitar asesor", ESTUDIANTE, null, null);
                }
                return new Pendiente("Elaborar el dictamen de designación de asesor", SECRETARIA,
                        "/admin/dictamenes", "Elaborar dictamen");
            case 4:
                if (!cartaAsesor) {
                    return new Pendiente("El asesor revisa el proyecto (carta de opinión favorable)", ASESOR,
                            "/admin/revision-proyecto", "Ver revisión");
                }
                return new Pendiente("El alumno debe enviar su expediente (Turnitin + proyecto final)",
                        ESTUDIANTE, null, null);
            case 5:
                if (!expedienteRecibido) {
                    return new Pendiente("Recibir el expediente", SECRETARIA,
                            "/admin/secretaria-defensa", "Recibir expediente");
                }
                if (revisores < 2) {
                    return new Pendiente("Designar 2 revisores", COORDINADOR,
                            "/admin/coordinador-proyecto", "Designar revisores");
                }
                if (!rubricaSubida) {
                    return new Pendiente("Subir la rúbrica oficial de revisores", SECRETARIA,
                            "/admin/secretaria-defensa", "Subir rúbrica");
                }
                if (!revisoresConformes) {
                    return new Pendiente("Los revisores están evaluando el proyecto", REVISOR,
                            "/admin/secretaria-defensa", "Ver revisores");
                }
                return new Pendiente("Programar la defensa del proyecto", SECRETARIA,
                        "/admin/secretaria-defensa", "Programar defensa");
            case 6:
                return new Pendiente("El alumno ejecuta la tesis; el asesor aprueba el informe final", ASESOR,
                        "/admin/ejecucion-tesis", "Ver ejecución");
            case 7:
                if (!juradoSolicitado) {
                    return new Pendiente("El alumno debe solicitar el Jurado Informante", ESTUDIANTE, null, null);
                }
                if (juradoInforme < 3) {
                    return new Pendiente("Designar los 3 miembros del Jurado Informante", COORDINADOR,
                            "/admin/coordinador-proyecto", "Designar jurado");
                }
                return new Pendiente("El Jurado Informante está revisando el informe final", JURADO, null, null);
            case 8:
                // La sustentación aún no tiene pantalla propia; se gestiona fuera del sistema.
                return new Pendiente("Programar la sustentación de la tesis", SECRETARIA, null, null);
            default:
                return new Pendiente("Proceso completado", NADIE, null, null);
        }
    }

    // ── resumen ──
    private SeguimientoResumen resumir(List<SeguimientoAlumnoItem> alumnos) {
        long[] porEtapa = new long[EtapasProceso.TOTAL + 2];
        long finalizados = 0;
        long pendientesSecretaria = 0;
        long sinTema = 0;
        long detenidos = 0;
        long porVencer = 0;
        for (SeguimientoAlumnoItem a : alumnos) {
            porEtapa[a.getEtapaNumero()]++;
            if (a.getEtapaNumero() > EtapasProceso.TOTAL) finalizados++;
            if (a.isPendienteSecretaria()) pendientesSecretaria++;
            if (EstadoDerivado.SIN_TEMA.equals(a.getEstadoDerivado())) sinTema++;
            if (a.getDiasEnEtapa() != null && a.getDiasEnEtapa() > DIAS_DETENIDO) detenidos++;
            if (a.getVigenciaMeses() != null && a.getVigenciaMeses() <= MESES_AVISO_VIGENCIA) porVencer++;
        }
        List<SeguimientoResumen.EtapaConteo> conteos = new ArrayList<>();
        for (int n = 1; n <= EtapasProceso.TOTAL; n++) {
            conteos.add(SeguimientoResumen.EtapaConteo.builder()
                    .numero(n).titulo(EtapasProceso.corto(n)).cantidad(porEtapa[n]).build());
        }
        return SeguimientoResumen.builder()
                .total(alumnos.size())
                .finalizados(finalizados)
                .pendientesSecretaria(pendientesSecretaria)
                .sinTema(sinTema)
                .detenidos(detenidos)
                .dictamenesPorVencer(porVencer)
                .porEtapa(conteos)
                .build();
    }

    // ── helpers de lectura de la fila nativa ──
    /** @param link pantalla donde se resuelve (null si no hay una) · @param label texto del botón */
    private record Pendiente(String texto, String responsable, String link, String label) {
    }

    private String nombreCompuesto(String paterno, String materno) {
        String s = (paterno == null ? "" : paterno) + " " + (materno == null ? "" : materno);
        return s.trim().replaceAll("\\s+", " ");
    }

    private UUID uuid(Object o) {
        if (o == null) return null;
        return o instanceof UUID u ? u : UUID.fromString(o.toString());
    }

    private String str(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private boolean bool(Object o) {
        return o instanceof Boolean b ? b : o != null && Boolean.parseBoolean(o.toString());
    }

    private long num(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private LocalDate fecha(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return null;
    }
}
