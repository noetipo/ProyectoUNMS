package unmsm.edu.pe.tesis.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenSustentacionRequest;
import unmsm.edu.pe.tesis.application.dto.ProgramarSustentacionRequest;
import unmsm.edu.pe.tesis.application.dto.RegistrarActaSustentacionRequest;
import unmsm.edu.pe.tesis.domain.entities.DictamenSustentacion;
import unmsm.edu.pe.tesis.domain.entities.JuradoSustentacion;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoDictamen;
import unmsm.edu.pe.tesis.domain.enums.EstadoTesis;
import unmsm.edu.pe.tesis.domain.enums.ResultadoDefensa;
import unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.DictamenSustentacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.JuradoSustentacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.ResolverDatosPlantillaService;
import unmsm.edu.pe.tesis.infrastructure.export.DocumentoAsesoriaRenderer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Trámite de Sustentación (Etapa 8, la última): recepción, dictamen de designación del Jurado,
 * programación del acto y Acta de sustentación. Registrar el Acta concluye el proceso de titulación.
 */
@ExtendWith(MockitoExtension.class)
class SustentacionServiceImplTest {

    @Mock SecurityUtils securityUtils;
    @Mock ProyectoTesisRepository proyectoRepository;
    @Mock JuradoSustentacionRepository juradoRepository;
    @Mock DictamenSustentacionRepository dictamenRepository;
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

    @InjectMocks SustentacionServiceImpl service;

    private final UUID TESIS = UUID.randomUUID();
    private final UUID PROY = UUID.randomUUID();
    private final UUID DOC_A = UUID.randomUUID();
    private final UUID DOC_B = UUID.randomUUID();
    private final UUID DOC_C = UUID.randomUUID();

    private ProyectoTesis proyectoSolicitado;

    @BeforeEach
    void setUp() {
        proyectoSolicitado = ProyectoTesis.builder().id(PROY).tesisId(TESIS)
                .sustentacionSolicitada(true).build();
        lenient().when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(proyectoSolicitado));
        lenient().when(tesisAutorRepository.estudianteDeTesis(TESIS)).thenReturn(null);
        lenient().when(tesisRepository.buscarPorId(TESIS)).thenReturn(Optional.of(new Tesis()));
    }

    private byte[] doc() { return "contenido".getBytes(); }

    private RegistrarActaSustentacionRequest actaReq(String resultado, String observacion) {
        RegistrarActaSustentacionRequest r = new RegistrarActaSustentacionRequest();
        r.setResultado(resultado);
        r.setObservacion(observacion);
        return r;
    }

    // ── recepcionar ──

    @Test
    void recepcionar_solicitado_marcaRecibido() {
        service.recepcionar(TESIS);

        assertTrue(Boolean.TRUE.equals(proyectoSolicitado.getExpedienteSustentacionRecibido()));
        assertNotNull(proyectoSolicitado.getFechaRecepcionSustentacion());
        verify(proyectoRepository).save(proyectoSolicitado);
    }

    @Test
    void recepcionar_noSolicitado_falla() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));
        assertThrows(BusinessException.class, () -> service.recepcionar(TESIS));
    }

    @Test
    void recepcionar_yaRecibido_falla() {
        proyectoSolicitado.setExpedienteSustentacionRecibido(true);
        assertThrows(BusinessException.class, () -> service.recepcionar(TESIS));
    }

    // ── elaborarDictamen ──

    private void conExpedienteRecibidoYJuradoDesignado() {
        proyectoSolicitado.setExpedienteSustentacionRecibido(true);
        lenient().when(juradoRepository.listarPorProyecto(PROY)).thenReturn(List.of(
                JuradoSustentacion.builder().docenteId(DOC_A).orden(1).presidente(true).build(),
                JuradoSustentacion.builder().docenteId(DOC_B).orden(2).build(),
                JuradoSustentacion.builder().docenteId(DOC_C).orden(3).build()));
        lenient().when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        lenient().when(correlativoRepository.siguienteCorrelativo(anyInt())).thenReturn(1);
        lenient().when(resolverPlantilla.resolverDictamenSustentacion(any(), any(), any(), any(), any()))
                .thenReturn(Map.of("NOMBRE", "x"));
    }

    @Test
    void elaborarDictamen_recibidoYJuradoCompleto_generaYGuarda() {
        conExpedienteRecibidoYJuradoDesignado();

        service.elaborarDictamen(TESIS, new ElaborarDictamenSustentacionRequest());

        verify(dictamenRepository).save(argThat(d ->
                d.getEstado() == EstadoDictamen.ELABORADO && d.getDatosDictamen() != null));
    }

    @Test
    void elaborarDictamen_sinRecepcionar_falla() {
        assertThrows(BusinessException.class,
                () -> service.elaborarDictamen(TESIS, new ElaborarDictamenSustentacionRequest()));
        verify(dictamenRepository, never()).save(any());
    }

    @Test
    void elaborarDictamen_juradoIncompleto_falla() {
        proyectoSolicitado.setExpedienteSustentacionRecibido(true);
        when(juradoRepository.listarPorProyecto(PROY)).thenReturn(List.of(
                JuradoSustentacion.builder().docenteId(DOC_A).orden(1).build()));

        assertThrows(BusinessException.class,
                () -> service.elaborarDictamen(TESIS, new ElaborarDictamenSustentacionRequest()));
    }

    @Test
    void elaborarDictamen_yaFirmado_falla() {
        proyectoSolicitado.setExpedienteSustentacionRecibido(true);
        when(juradoRepository.listarPorProyecto(PROY)).thenReturn(List.of(
                JuradoSustentacion.builder().docenteId(DOC_A).orden(1).build(),
                JuradoSustentacion.builder().docenteId(DOC_B).orden(2).build(),
                JuradoSustentacion.builder().docenteId(DOC_C).orden(3).build()));
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(
                DictamenSustentacion.builder().tesisId(TESIS).estado(EstadoDictamen.FIRMADO).build()));

        assertThrows(BusinessException.class,
                () -> service.elaborarDictamen(TESIS, new ElaborarDictamenSustentacionRequest()));
    }

    // ── subirDictamenFirmado ──

    @Test
    void subirDictamenFirmado_elaborado_marcaFirmado() {
        DictamenSustentacion dic = DictamenSustentacion.builder().tesisId(TESIS).estado(EstadoDictamen.ELABORADO).build();
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(dic));
        when(documentoTesisRepository.buscarPorTesisYTipo(any(), any())).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), any(), any())).thenReturn("key-1");

        service.subirDictamenFirmado(TESIS, doc(), "d.pdf", "application/pdf");

        assertEquals(EstadoDictamen.FIRMADO, dic.getEstado());
        verify(dictamenRepository).save(dic);
    }

    @Test
    void subirDictamenFirmado_porElaborar_falla() {
        DictamenSustentacion dic = DictamenSustentacion.builder().tesisId(TESIS).estado(EstadoDictamen.POR_ELABORAR).build();
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(dic));
        assertThrows(BusinessException.class,
                () -> service.subirDictamenFirmado(TESIS, doc(), "d.pdf", "application/pdf"));
    }

    // ── programar ──

    @Test
    void programar_conDictamenFirmado_presencialConLugar_programa() {
        when(documentoTesisRepository.existePorTesisYTipo(TESIS, SustentacionServiceImpl.T_DICTAMEN_SUSTENTACION)).thenReturn(true);
        ProgramarSustentacionRequest req = new ProgramarSustentacionRequest();
        req.setFecha(LocalDate.of(2026, 9, 20));
        req.setModalidad("PRESENCIAL");
        req.setLugar("Auditorio 3");

        service.programar(TESIS, req);

        assertTrue(Boolean.TRUE.equals(proyectoSolicitado.getSustentacionProgramada()));
        assertEquals(LocalDate.of(2026, 9, 20), proyectoSolicitado.getFechaSustentacion());
        verify(proyectoRepository).save(proyectoSolicitado);
    }

    @Test
    void programar_sinDictamenFirmado_falla() {
        when(documentoTesisRepository.existePorTesisYTipo(TESIS, SustentacionServiceImpl.T_DICTAMEN_SUSTENTACION)).thenReturn(false);
        ProgramarSustentacionRequest req = new ProgramarSustentacionRequest();
        req.setFecha(LocalDate.of(2026, 9, 20));
        req.setModalidad("PRESENCIAL");
        req.setLugar("Auditorio 3");

        assertThrows(BusinessException.class, () -> service.programar(TESIS, req));
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    void programar_virtualSinEnlace_falla() {
        when(documentoTesisRepository.existePorTesisYTipo(TESIS, SustentacionServiceImpl.T_DICTAMEN_SUSTENTACION)).thenReturn(true);
        ProgramarSustentacionRequest req = new ProgramarSustentacionRequest();
        req.setFecha(LocalDate.of(2026, 9, 20));
        req.setModalidad("VIRTUAL");

        assertThrows(ValidationException.class, () -> service.programar(TESIS, req));
    }

    @Test
    void programar_yaProgramada_falla() {
        when(documentoTesisRepository.existePorTesisYTipo(TESIS, SustentacionServiceImpl.T_DICTAMEN_SUSTENTACION)).thenReturn(true);
        proyectoSolicitado.setSustentacionProgramada(true);
        ProgramarSustentacionRequest req = new ProgramarSustentacionRequest();
        req.setFecha(LocalDate.of(2026, 9, 20));
        req.setModalidad("PRESENCIAL");
        req.setLugar("Auditorio 3");

        assertThrows(BusinessException.class, () -> service.programar(TESIS, req));
    }

    // ── registrarActa ──

    @Test
    void registrarActa_aprobado_concluyeTesis() {
        proyectoSolicitado.setSustentacionProgramada(true);
        when(documentoTesisRepository.buscarPorTesisYTipo(any(), any())).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), any(), any())).thenReturn("key-acta");
        when(tesisRepository.buscarPorId(TESIS)).thenReturn(Optional.of(new Tesis()));

        service.registrarActa(TESIS, actaReq("APROBADO", null), doc(), "acta.pdf", "application/pdf");

        assertTrue(Boolean.TRUE.equals(proyectoSolicitado.getActaSustentacionSubida()));
        assertTrue(Boolean.TRUE.equals(proyectoSolicitado.getTesisConcluida()));
        assertEquals(ResultadoDefensa.APROBADO, proyectoSolicitado.getResultadoSustentacion());
        assertNotNull(proyectoSolicitado.getFechaConclusionTesis());
        verify(tesisRepository).save(argThat(t -> t.getEstado() == EstadoTesis.SUSTENTADO));
    }

    @Test
    void registrarActa_desaprobadoSinObservacion_falla() {
        proyectoSolicitado.setSustentacionProgramada(true);
        assertThrows(ValidationException.class,
                () -> service.registrarActa(TESIS, actaReq("DESAPROBADO", null), doc(), "acta.pdf", "application/pdf"));
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    void registrarActa_desaprobado_noConcluyeYReabreProgramacion() {
        proyectoSolicitado.setSustentacionProgramada(true);
        proyectoSolicitado.setFechaSustentacion(java.time.LocalDate.of(2026, 9, 1));
        proyectoSolicitado.setLugarSustentacion("Auditorio 3");
        when(documentoTesisRepository.buscarPorTesisYTipo(any(), any())).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), any(), any())).thenReturn("key-acta2");

        service.registrarActa(TESIS, actaReq("DESAPROBADO", "No sustentó adecuadamente"), doc(), "acta.pdf", "application/pdf");

        assertEquals(ResultadoDefensa.DESAPROBADO, proyectoSolicitado.getResultadoSustentacion());
        assertTrue(Boolean.TRUE.equals(proyectoSolicitado.getActaSustentacionSubida()));
        assertFalse(Boolean.TRUE.equals(proyectoSolicitado.getTesisConcluida()));
        assertFalse(Boolean.TRUE.equals(proyectoSolicitado.getSustentacionProgramada()));
        assertNull(proyectoSolicitado.getFechaSustentacion());
        assertNull(proyectoSolicitado.getLugarSustentacion());
        verify(tesisRepository, never()).save(any());
    }

    @Test
    void registrarActa_sinProgramar_falla() {
        assertThrows(BusinessException.class,
                () -> service.registrarActa(TESIS, actaReq("APROBADO", null), doc(), "acta.pdf", "application/pdf"));
    }

    @Test
    void registrarActa_yaConcluida_falla() {
        proyectoSolicitado.setSustentacionProgramada(true);
        proyectoSolicitado.setTesisConcluida(true);
        assertThrows(BusinessException.class,
                () -> service.registrarActa(TESIS, actaReq("APROBADO", null), doc(), "acta.pdf", "application/pdf"));
    }

    @Test
    void registrarActa_resultadoInvalido_falla() {
        proyectoSolicitado.setSustentacionProgramada(true);
        assertThrows(ValidationException.class,
                () -> service.registrarActa(TESIS, actaReq("EXCELENTE", null), doc(), "acta.pdf", "application/pdf"));
    }

    // ── detalle: proyecto inexistente ──

    @Test
    void detalle_proyectoInexistente_falla() {
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.detalle(TESIS));
    }
}
