package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;
import unmsm.edu.pe.tesis.application.dto.*;
import unmsm.edu.pe.tesis.application.util.CitationFormatter;
import unmsm.edu.pe.tesis.application.util.ProyectoDefinicion;
import unmsm.edu.pe.tesis.domain.entities.*;
import unmsm.edu.pe.tesis.domain.enums.EstadoItemRevision;
import unmsm.edu.pe.tesis.domain.enums.EstiloCita;
import unmsm.edu.pe.tesis.domain.repositories.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Arma el {@link ProyectoEditorResponse} completo a partir del proyecto y su contexto. */
@ApplicationScoped
public class ProyectoEditorAssembler {

    @Inject ProyectoCampoRepository campoRepository;
    @Inject ProyectoObjetivoRepository objetivoRepository;
    @Inject ProyectoActividadRepository actividadRepository;
    @Inject ProyectoPartidaRepository partidaRepository;
    @Inject ProyectoRevisionRepository revisionRepository;
    @Inject ProyectoRevisionEventoRepository eventoRepository;
    @Inject ProyectoReferenciaRepository referenciaRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository documentoTesisRepository;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Set<EstadoItemRevision> PENDIENTES = Set.of(
            EstadoItemRevision.OBSERVADO, EstadoItemRevision.EN_CORRECCION, EstadoItemRevision.CORREGIDO);

    public ProyectoEditorResponse armar(ProyectoTesis p, Tesis tesis, Estudiante est, String asesorNombre) {
        // ── Campos (titulo/resumen viven en tesis) ──
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("titulo", tesis != null ? nz(tesis.getTitulo()) : "");
        campos.put("resumen", tesis != null ? nz(tesis.getResumen()) : "");
        for (ProyectoCampo c : campoRepository.listarPorProyecto(p.getId())) {
            campos.put(c.getClave(), nz(c.getValor()));
        }

        // ── Objetivos / actividades / partidas ──
        List<ObjetivoItem> objetivos = objetivoRepository.listarPorProyecto(p.getId()).stream()
                .map(o -> ObjetivoItem.builder().id(o.getId()).orden(o.getOrden()).texto(o.getTexto()).build())
                .toList();
        List<ActividadItem> actividades = actividadRepository.listarPorProyecto(p.getId()).stream()
                .map(a -> ActividadItem.builder().id(a.getId()).nombre(a.getNombre())
                        .fase(a.getFase() != null ? a.getFase().name() : null)
                        .mesInicio(a.getMesInicio()).mesFin(a.getMesFin())
                        .fechaInicio(a.getFechaInicio()).fechaFin(a.getFechaFin())
                        .estado(a.getEstado() != null ? a.getEstado().name() : null)
                        .orden(a.getOrden()).build())
                .toList();
        List<ProyectoPartida> partidasEnt = partidaRepository.listarPorProyecto(p.getId());
        List<PartidaItem> partidas = partidasEnt.stream()
                .map(pa -> PartidaItem.builder().id(pa.getId()).rubro(pa.getRubro())
                        .descripcion(pa.getDescripcion()).monto(pa.getMonto()).orden(pa.getOrden()).build())
                .toList();
        BigDecimal total = partidasEnt.stream()
                .map(pa -> pa.getMonto() != null ? pa.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Referencias (estructuradas + formateadas al estilo del proyecto) ──
        EstiloCita estilo = p.getEstiloCita() != null ? p.getEstiloCita() : EstiloCita.APA;
        List<ProyectoReferencia> refsEnt = referenciaRepository.listarPorProyecto(p.getId());
        List<ReferenciaItem> referencias = new ArrayList<>();
        for (int i = 0; i < refsEnt.size(); i++) {
            ProyectoReferencia r = refsEnt.get(i);
            referencias.add(ReferenciaItem.builder()
                    .id(r.getId()).orden(r.getOrden())
                    .tipo(r.getTipo() != null ? r.getTipo().name() : null)
                    .autores(r.getAutores()).anio(r.getAnio()).titulo(r.getTitulo()).fuente(r.getFuente())
                    .volumen(r.getVolumen()).numero(r.getNumero()).paginas(r.getPaginas())
                    .editorial(r.getEditorial()).ciudad(r.getCiudad()).doi(r.getDoi()).url(r.getUrl())
                    .fechaAcceso(r.getFechaAcceso())
                    .formateada(sinMarcas(CitationFormatter.formatear(r, estilo)))
                    .citaTexto(CitationFormatter.citaEnTexto(r, estilo, i + 1))
                    .citaNarrativa(CitationFormatter.citaNarrativa(r, estilo, i + 1))
                    .build());
        }

        // ── Revisiones + eventos ──
        List<ProyectoRevision> revEnt = revisionRepository.listarPorProyecto(p.getId());
        List<UUID> revIds = revEnt.stream().map(ProyectoRevision::getId).toList();
        Map<UUID, List<RevisionEventoItem>> eventosPorRev = new HashMap<>();
        for (ProyectoRevisionEvento ev : eventoRepository.listarPorRevisiones(revIds)) {
            eventosPorRev.computeIfAbsent(ev.getRevisionId(), k -> new ArrayList<>()).add(
                    RevisionEventoItem.builder().tipo(ev.getTipo()).autor(ev.getAutor()).rol(ev.getRol())
                            .texto(ev.getTexto())
                            .fecha(ev.getFechaEvento() != null ? ev.getFechaEvento().format(FECHA) : null)
                            .build());
        }
        List<RevisionItem> revisiones = revEnt.stream()
                .map(r -> RevisionItem.builder().campo(r.getCampo())
                        .estado(r.getEstado() != null ? r.getEstado().name() : null)
                        .eventos(eventosPorRev.getOrDefault(r.getId(), List.of())).build())
                .toList();
        boolean hayPendientes = revEnt.stream().anyMatch(r -> PENDIENTES.contains(r.getEstado()));

        // ── Avance (igual que proyStats) ──
        int tot = 0, fil = 0;
        for (ProyectoDefinicion.Seccion sec : ProyectoDefinicion.seccionesActivas(p.getEnfoque())) {
            for (String k : sec.campos()) {
                tot++;
                String v = campos.get(k);
                if (v != null && !v.trim().isEmpty()) fil++;
            }
        }
        tot += 3; // objetivos específicos + actividades + presupuesto
        if (objetivos.stream().anyMatch(o -> o.getTexto() != null && !o.getTexto().trim().isEmpty())) fil++;
        if (!actividades.isEmpty()) fil++;
        if (!partidas.isEmpty()) fil++;
        int pct = tot > 0 ? Math.round((fil * 100f) / tot) : 0;

        // ── Contexto ──
        Persona pe = est != null ? est.getPersona() : null;
        ProgramaPosgrado prog = est != null ? est.getPrograma() : null;

        return ProyectoEditorResponse.builder()
                .tesisId(p.getTesisId())
                .proyectoId(p.getId())
                .estudianteNombre(nombre(pe))
                .codigoSistema(est != null ? est.getCodigoSistema() : null)
                .programaNombre(prog != null ? prog.getNombre() : null)
                .asesorNombre(asesorNombre)
                .lineaNombre(tesis != null && tesis.getLineaInvestigacion() != null ? tesis.getLineaInvestigacion().getNombre() : null)
                .nivel(tesis != null && tesis.getNivel() != null ? tesis.getNivel().name() : null)
                .titulo(tesis != null ? tesis.getTitulo() : null)
                .resumen(tesis != null ? tesis.getResumen() : null)
                .enfoque(p.getEnfoque() != null ? p.getEnfoque().name() : null)
                .enfoqueBloqueado(Boolean.TRUE.equals(p.getEnfoqueBloqueado()))
                .estado(p.getEstado() != null ? p.getEstado().name() : null)
                .financiamiento(p.getFinanciamiento())
                .listoRevision(Boolean.TRUE.equals(p.getListoRevision()))
                .planPublicado(Boolean.TRUE.equals(p.getPlanPublicado()))
                .cartaAsesor(Boolean.TRUE.equals(p.getCartaAsesor()))
                .turnitinSubido(Boolean.TRUE.equals(p.getTurnitinSubido()))
                .proyectoFinalSubido(documentoTesisRepository.existePorTesisYTipo(p.getTesisId(), "PROYECTO_VERSION_FINAL"))
                .expedienteSubido(Boolean.TRUE.equals(p.getExpedienteSubido()))
                .expedienteRecibido(Boolean.TRUE.equals(p.getExpedienteRecibido()))
                .porcentajeSimilitud(p.getPorcentajeSimilitud())
                .avanceCompletados(fil).avanceTotal(tot).avancePct(pct)
                .campos(campos)
                .objetivos(objetivos).actividades(actividades).partidas(partidas)
                .presupuestoTotal(total).revisiones(revisiones)
                .referencias(referencias).estiloCita(estilo.name())
                .estiloCitaBloqueado(Boolean.TRUE.equals(p.getEstiloCitaBloqueado()))
                .puedeMarcarListo(pct >= 100)
                .todosConformes(Boolean.TRUE.equals(p.getListoRevision()) && !hayPendientes)
                .build();
    }

    private String nombre(Persona p) {
        if (p == null) return null;
        return (nz(p.getNombres()) + " " + nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno()))
                .trim().replaceAll("\\s+", " ");
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    /** Quita las marcas de cursiva «i»…«/i» para la vista previa en texto plano. */
    private String sinMarcas(String s) {
        return s == null ? null : s.replace("«i»", "").replace("«/i»", "");
    }
}
