package unmsm.edu.pe.tesis.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenAprobacionRequest;
import unmsm.edu.pe.tesis.application.dto.RegistrarResultadoDefensaRequest;
import unmsm.edu.pe.tesis.domain.entities.DictamenAprobacion;
import unmsm.edu.pe.tesis.domain.entities.DocumentoTesis;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.RubricaDefensa;
import unmsm.edu.pe.tesis.domain.enums.EstadoDictamen;
import unmsm.edu.pe.tesis.domain.repositories.DictamenAprobacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRubricaPuntajeRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.RubricaDefensaRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.ResolverDatosPlantillaService;
import unmsm.edu.pe.tesis.infrastructure.export.DocumentoAsesoriaRenderer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cola del proceso (Etapa 5, tras la defensa): recepcionar rúbricas → registrar resultado →
 * elaborar dictamen de aprobación → subir firmado → archivar. Cada paso exige que el anterior
 * ya esté hecho (ver el diagrama del proceso guardado por la usuaria).
 */
@ExtendWith(MockitoExtension.class)
class CierreProyectoServiceImplTest {

    @Mock SecurityUtils securityUtils;
    @Mock ProyectoTesisRepository proyectoRepository;
    @Mock ProyectoRevisorRepository revisorRepository;
    @Mock ProyectoRubricaPuntajeRepository puntajeRepository;
    @Mock RubricaDefensaRepository rubricaRepository;
    @Mock DictamenAprobacionRepository dictamenRepository;
    @Mock DictamenDesignacionRepository correlativoRepository;
    @Mock DocumentoTesisRepository documentoTesisRepository;
    @Mock TesisRepository tesisRepository;
    @Mock TesisAutorRepository tesisAutorRepository;
    @Mock EstudianteRepository estudianteRepository;
    @Mock PersonaRepository personaRepository;
    @Mock AlmacenamientoArchivos almacenamiento;
    @Mock ResolverDatosPlantillaService resolverPlantilla;
    @Mock DocumentoAsesoriaRenderer renderer;
    @Spy com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @InjectMocks CierreProyectoServiceImpl service;

    private final UUID TESIS = UUID.randomUUID();
    private final UUID PROY = UUID.randomUUID();
    private final UUID DOC_A = UUID.randomUUID();
    private final UUID DOC_B = UUID.randomUUID();

    private ProyectoTesis proyectoConDefensaProgramada;

    @BeforeEach
    void setUp() {
        proyectoConDefensaProgramada = ProyectoTesis.builder().id(PROY).tesisId(TESIS)
                .defensaProgramada(true).build();
        lenient().when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(proyectoConDefensaProgramada));
        lenient().when(revisorRepository.buscarPorProyectoYDocente(PROY, DOC_A))
                .thenReturn(Optional.of(ProyectoRevisor.builder().docenteId(DOC_A).orden(1).build()));
    }

    private byte[] pdf() { return "contenido".getBytes(); }

    // ── recepcionarRubrica ──

    @Test
    void recepcionarRubrica_valida_guarda() {
        when(almacenamiento.guardar(any(), any(), any())).thenReturn("key-1");

        service.recepcionarRubrica(TESIS, DOC_A, 90, pdf(), "rubrica.pdf", "application/pdf");

        verify(rubricaRepository).save(argThat(r ->
                r.getTesisId().equals(TESIS) && r.getDocenteId().equals(DOC_A) && r.getPuntaje() == 90));
    }

    @Test
    void recepcionarRubrica_defensaNoProgramada_falla() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));

        assertThrows(BusinessException.class,
                () -> service.recepcionarRubrica(TESIS, DOC_A, 90, pdf(), "r.pdf", "application/pdf"));
        verify(rubricaRepository, never()).save(any());
    }

    @Test
    void recepcionarRubrica_docenteNoEsRevisor_falla() {
        when(revisorRepository.buscarPorProyectoYDocente(PROY, DOC_B)).thenReturn(Optional.empty());
        assertThrows(ValidationException.class,
                () -> service.recepcionarRubrica(TESIS, DOC_B, 90, pdf(), "r.pdf", "application/pdf"));
    }

    @Test
    void recepcionarRubrica_puntajeFueraDeRango_falla() {
        assertThrows(ValidationException.class,
                () -> service.recepcionarRubrica(TESIS, DOC_A, 101, pdf(), "r.pdf", "application/pdf"));
        assertThrows(ValidationException.class,
                () -> service.recepcionarRubrica(TESIS, DOC_A, -1, pdf(), "r.pdf", "application/pdf"));
    }

    @Test
    void recepcionarRubrica_archivoVacio_falla() {
        assertThrows(ValidationException.class,
                () -> service.recepcionarRubrica(TESIS, DOC_A, 90, new byte[0], "r.pdf", "application/pdf"));
    }

    @Test
    void recepcionarRubrica_tipoNoPermitido_falla() {
        assertThrows(ValidationException.class,
                () -> service.recepcionarRubrica(TESIS, DOC_A, 90, pdf(), "r.exe", "application/x-msdownload"));
    }

    // ── registrarResultado ──

    private RegistrarResultadoDefensaRequest resultadoReq(String resultado, String obs) {
        RegistrarResultadoDefensaRequest r = new RegistrarResultadoDefensaRequest();
        r.setResultado(resultado);
        r.setObservacion(obs);
        return r;
    }

    @Test
    void registrarResultado_aprobado_guardaSinExigirObservacion() {
        service.registrarResultado(TESIS, resultadoReq("APROBADO", null));

        assertTrue(Boolean.TRUE.equals(proyectoConDefensaProgramada.getDefensaRealizada()));
        assertNotNull(proyectoConDefensaProgramada.getFechaResultadoDefensa());
        verify(proyectoRepository).save(proyectoConDefensaProgramada);
    }

    @Test
    void registrarResultado_desaprobadoSinObservacion_falla() {
        assertThrows(ValidationException.class,
                () -> service.registrarResultado(TESIS, resultadoReq("DESAPROBADO", null)));
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    void registrarResultado_desaprobadoConObservacion_guarda() {
        service.registrarResultado(TESIS, resultadoReq("DESAPROBADO", "No sustenta el diseño"));
        verify(proyectoRepository).save(any());
    }

    @Test
    void registrarResultado_desaprobado_reabreRevisionDeLosRevisores() {
        proyectoConDefensaProgramada.setFechaDefensa(java.time.LocalDate.of(2026, 9, 1));
        proyectoConDefensaProgramada.setLugarDefensa("Auditorio 3");
        proyectoConDefensaProgramada.setRevisoresConformes(true);
        ProyectoRevisor r1 = ProyectoRevisor.builder().id(UUID.randomUUID()).docenteId(DOC_A).orden(1)
                .estado(unmsm.edu.pe.tesis.domain.enums.EstadoRevisor.CONFORME)
                .comentario("Todo conforme").puntajeTotal(95).build();
        ProyectoRevisor r2 = ProyectoRevisor.builder().id(UUID.randomUUID()).docenteId(DOC_B).orden(2)
                .estado(unmsm.edu.pe.tesis.domain.enums.EstadoRevisor.CONFORME)
                .comentario("Ok").puntajeTotal(90).build();
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(r1, r2));

        service.registrarResultado(TESIS, resultadoReq("DESAPROBADO", "No sustenta el diseño"));

        assertFalse(Boolean.TRUE.equals(proyectoConDefensaProgramada.getDefensaProgramada()));
        assertFalse(Boolean.TRUE.equals(proyectoConDefensaProgramada.getRevisoresConformes()));
        assertNull(proyectoConDefensaProgramada.getFechaDefensa());
        assertNull(proyectoConDefensaProgramada.getLugarDefensa());
        // El resultado desaprobado queda como historial hasta que se registre el de la próxima defensa.
        assertEquals(unmsm.edu.pe.tesis.domain.enums.ResultadoDefensa.DESAPROBADO,
                proyectoConDefensaProgramada.getResultadoDefensa());
        assertEquals(unmsm.edu.pe.tesis.domain.enums.EstadoRevisor.DESIGNADO, r1.getEstado());
        assertEquals(unmsm.edu.pe.tesis.domain.enums.EstadoRevisor.DESIGNADO, r2.getEstado());
        assertNull(r1.getComentario());
        assertNull(r1.getPuntajeTotal());
        verify(revisorRepository).save(r1);
        verify(revisorRepository).save(r2);
        verify(puntajeRepository).eliminarPorRevisor(r1.getId());
        verify(puntajeRepository).eliminarPorRevisor(r2.getId());
    }

    @Test
    void registrarResultado_aprobado_noTocaALosRevisores() {
        service.registrarResultado(TESIS, resultadoReq("APROBADO", null));
        verify(revisorRepository, never()).listarPorProyecto(any());
        verify(puntajeRepository, never()).eliminarPorRevisor(any());
    }

    @Test
    void registrarResultado_resultadoInvalido_falla() {
        assertThrows(ValidationException.class,
                () -> service.registrarResultado(TESIS, resultadoReq("EXCELENTE", null)));
    }

    @Test
    void registrarResultado_defensaNoProgramada_falla() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));
        assertThrows(BusinessException.class,
                () -> service.registrarResultado(TESIS, resultadoReq("APROBADO", null)));
    }

    @Test
    void registrarResultado_proyectoYaAprobado_falla() {
        proyectoConDefensaProgramada.setProyectoAprobado(true);
        assertThrows(BusinessException.class,
                () -> service.registrarResultado(TESIS, resultadoReq("APROBADO", null)));
    }

    // ── elaborarDictamen ──

    private void conResultadoFavorableYRubricasCompletas() {
        proyectoConDefensaProgramada.setDefensaRealizada(true);
        proyectoConDefensaProgramada.setResultadoDefensa(unmsm.edu.pe.tesis.domain.enums.ResultadoDefensa.APROBADO);
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(
                ProyectoRevisor.builder().docenteId(DOC_A).orden(1).build(),
                ProyectoRevisor.builder().docenteId(DOC_B).orden(2).build()));
        when(rubricaRepository.buscarPorTesisYDocente(TESIS, DOC_A))
                .thenReturn(Optional.of(RubricaDefensa.builder().docenteId(DOC_A).puntaje(90).build()));
        when(rubricaRepository.buscarPorTesisYDocente(TESIS, DOC_B))
                .thenReturn(Optional.of(RubricaDefensa.builder().docenteId(DOC_B).puntaje(85).build()));
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        lenient().when(correlativoRepository.siguienteCorrelativo(anyInt())).thenReturn(1);
        lenient().when(tesisRepository.buscarPorId(TESIS)).thenReturn(Optional.of(new unmsm.edu.pe.tesis.domain.entities.Tesis()));
        lenient().when(tesisAutorRepository.estudianteDeTesis(TESIS)).thenReturn(null);
        lenient().when(resolverPlantilla.resolverDictamenAprobacion(any(), any(), any(), any(), any(), any()))
                .thenReturn(Map.of("NOMBRE", "x"));
    }

    @Test
    void elaborarDictamen_favorableYCompleto_generaYGuarda() {
        conResultadoFavorableYRubricasCompletas();

        service.elaborarDictamen(TESIS, new ElaborarDictamenAprobacionRequest());

        verify(dictamenRepository).save(argThat(d ->
                d.getEstado() == EstadoDictamen.ELABORADO && d.getDatosDictamen() != null));
    }

    @Test
    void elaborarDictamen_sinResultadoRegistrado_falla() {
        assertThrows(BusinessException.class,
                () -> service.elaborarDictamen(TESIS, new ElaborarDictamenAprobacionRequest()));
    }

    @Test
    void elaborarDictamen_desaprobado_falla() {
        proyectoConDefensaProgramada.setDefensaRealizada(true);
        proyectoConDefensaProgramada.setResultadoDefensa(unmsm.edu.pe.tesis.domain.enums.ResultadoDefensa.DESAPROBADO);
        assertThrows(BusinessException.class,
                () -> service.elaborarDictamen(TESIS, new ElaborarDictamenAprobacionRequest()));
    }

    @Test
    void elaborarDictamen_faltaRubricaPorRecepcionar_falla() {
        proyectoConDefensaProgramada.setDefensaRealizada(true);
        proyectoConDefensaProgramada.setResultadoDefensa(unmsm.edu.pe.tesis.domain.enums.ResultadoDefensa.APROBADO);
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(
                ProyectoRevisor.builder().docenteId(DOC_A).orden(1).build(),
                ProyectoRevisor.builder().docenteId(DOC_B).orden(2).build()));
        when(rubricaRepository.buscarPorTesisYDocente(TESIS, DOC_A))
                .thenReturn(Optional.of(RubricaDefensa.builder().docenteId(DOC_A).puntaje(90).build()));
        when(rubricaRepository.buscarPorTesisYDocente(TESIS, DOC_B)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> service.elaborarDictamen(TESIS, new ElaborarDictamenAprobacionRequest()));
    }

    @Test
    void elaborarDictamen_yaFirmado_falla() {
        conResultadoFavorableYRubricasCompletas();
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(
                DictamenAprobacion.builder().tesisId(TESIS).estado(EstadoDictamen.FIRMADO).build()));

        assertThrows(BusinessException.class,
                () -> service.elaborarDictamen(TESIS, new ElaborarDictamenAprobacionRequest()));
    }

    // ── subirDictamenFirmado ──

    @Test
    void subirDictamenFirmado_elaborado_marcaFirmado() {
        DictamenAprobacion dic = DictamenAprobacion.builder().tesisId(TESIS).estado(EstadoDictamen.ELABORADO).build();
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(dic));
        when(documentoTesisRepository.buscarPorTesisYTipo(any(), any())).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), any(), any())).thenReturn("key-2");

        service.subirDictamenFirmado(TESIS, pdf(), "dictamen.pdf", "application/pdf");

        assertEquals(EstadoDictamen.FIRMADO, dic.getEstado());
        verify(dictamenRepository).save(dic);
    }

    @Test
    void subirDictamenFirmado_sinElaborar_falla() {
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> service.subirDictamenFirmado(TESIS, pdf(), "d.pdf", "application/pdf"));
    }

    @Test
    void subirDictamenFirmado_porElaborar_falla() {
        DictamenAprobacion dic = DictamenAprobacion.builder().tesisId(TESIS).estado(EstadoDictamen.POR_ELABORAR).build();
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(dic));
        assertThrows(BusinessException.class,
                () -> service.subirDictamenFirmado(TESIS, pdf(), "d.pdf", "application/pdf"));
    }

    // ── archivarProyectoFinal / archivarProyectoDelEstudiante ──

    @Test
    void archivarProyectoFinal_conDictamenFirmado_aprueba() {
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(
                DictamenAprobacion.builder().tesisId(TESIS).estado(EstadoDictamen.FIRMADO).build()));
        when(documentoTesisRepository.buscarPorTesisYTipo(any(), any())).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), any(), any())).thenReturn("key-3");
        when(tesisRepository.buscarPorId(TESIS)).thenReturn(Optional.empty());

        service.archivarProyectoFinal(TESIS, pdf(), "final.pdf", "application/pdf");

        assertTrue(Boolean.TRUE.equals(proyectoConDefensaProgramada.getProyectoAprobado()));
        assertNotNull(proyectoConDefensaProgramada.getFechaAprobacionProyecto());
    }

    @Test
    void archivarProyectoFinal_sinDictamen_falla() {
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> service.archivarProyectoFinal(TESIS, pdf(), "final.pdf", "application/pdf"));
    }

    @Test
    void archivarProyectoFinal_dictamenNoFirmado_falla() {
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(
                DictamenAprobacion.builder().tesisId(TESIS).estado(EstadoDictamen.ELABORADO).build()));
        assertThrows(BusinessException.class,
                () -> service.archivarProyectoFinal(TESIS, pdf(), "final.pdf", "application/pdf"));
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    void archivarProyectoDelEstudiante_sinProyectoFinalSubido_falla() {
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(
                DictamenAprobacion.builder().tesisId(TESIS).estado(EstadoDictamen.FIRMADO).build()));
        when(documentoTesisRepository.buscarPorTesisYTipo(TESIS, "PROYECTO_VERSION_FINAL")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.archivarProyectoDelEstudiante(TESIS));
    }

    @Test
    void archivarProyectoDelEstudiante_conProyectoFinalSubido_archivaYAprueba() {
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(
                DictamenAprobacion.builder().tesisId(TESIS).estado(EstadoDictamen.FIRMADO).build()));
        DocumentoTesis original = DocumentoTesis.builder().tesisId(TESIS).tipo("PROYECTO_VERSION_FINAL")
                .storageKey("key-orig").nombreOriginal("final.pdf").contentType("application/pdf").build();
        when(documentoTesisRepository.buscarPorTesisYTipo(TESIS, "PROYECTO_VERSION_FINAL"))
                .thenReturn(Optional.of(original));
        when(documentoTesisRepository.buscarPorTesisYTipo(TESIS, "PROYECTO_FINAL_APROBADO")).thenReturn(Optional.empty());
        when(almacenamiento.obtener("key-orig")).thenReturn(pdf());
        when(almacenamiento.guardar(any(), any(), any())).thenReturn("key-4");
        when(tesisRepository.buscarPorId(TESIS)).thenReturn(Optional.empty());

        service.archivarProyectoDelEstudiante(TESIS);

        assertTrue(Boolean.TRUE.equals(proyectoConDefensaProgramada.getProyectoAprobado()));
        verify(documentoTesisRepository).save(argThat(d -> "PROYECTO_FINAL_APROBADO".equals(d.getTipo())));
    }
}
