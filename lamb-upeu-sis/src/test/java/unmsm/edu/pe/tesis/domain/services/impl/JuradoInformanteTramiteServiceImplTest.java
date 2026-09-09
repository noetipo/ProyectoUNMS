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
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenExpeditoRequest;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenJuradoInformeRequest;
import unmsm.edu.pe.tesis.domain.entities.DictamenExpedito;
import unmsm.edu.pe.tesis.domain.entities.DictamenJuradoInforme;
import unmsm.edu.pe.tesis.domain.entities.InformeRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoDictamen;
import unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.DictamenExpeditoRepository;
import unmsm.edu.pe.tesis.domain.repositories.DictamenJuradoInformeRepository;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.InformeRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
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
 * Trámite del Jurado Informante (Etapa 7): recepción, dictamen de designación, archivo del
 * expediente y Dictamen de Expedito — pasos consecutivos del mismo expediente, en un solo servicio.
 */
@ExtendWith(MockitoExtension.class)
class JuradoInformanteTramiteServiceImplTest {

    @Mock SecurityUtils securityUtils;
    @Mock ProyectoTesisRepository proyectoRepository;
    @Mock InformeRevisorRepository informeRevisorRepository;
    @Mock DictamenJuradoInformeRepository dictamenRepository;
    @Mock DictamenExpeditoRepository expeditoRepository;
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

    @InjectMocks JuradoInformanteTramiteServiceImpl service;

    private final UUID TESIS = UUID.randomUUID();
    private final UUID PROY = UUID.randomUUID();
    private final UUID DOC_A = UUID.randomUUID();
    private final UUID DOC_B = UUID.randomUUID();
    private final UUID DOC_C = UUID.randomUUID();

    private ProyectoTesis proyectoSolicitado;

    @BeforeEach
    void setUp() {
        proyectoSolicitado = ProyectoTesis.builder().id(PROY).tesisId(TESIS)
                .juradoInformanteSolicitado(true).build();
        lenient().when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(proyectoSolicitado));
        lenient().when(tesisAutorRepository.estudianteDeTesis(TESIS)).thenReturn(null);
        lenient().when(tesisRepository.buscarPorId(TESIS)).thenReturn(Optional.of(new Tesis()));
    }

    private byte[] doc() { return "contenido".getBytes(); }

    // ── recepcionar ──

    @Test
    void recepcionar_solicitado_marcaRecibido() {
        service.recepcionar(TESIS);

        assertTrue(Boolean.TRUE.equals(proyectoSolicitado.getExpedienteInformeRecibido()));
        assertNotNull(proyectoSolicitado.getFechaRecepcionInforme());
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
        proyectoSolicitado.setExpedienteInformeRecibido(true);
        assertThrows(BusinessException.class, () -> service.recepcionar(TESIS));
    }

    // ── elaborarDictamen (Jurado Informante) ──

    private void conExpedienteRecibidoYJuradoDesignado() {
        proyectoSolicitado.setExpedienteInformeRecibido(true);
        lenient().when(informeRevisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(
                InformeRevisor.builder().docenteId(DOC_A).orden(1).build(),
                InformeRevisor.builder().docenteId(DOC_B).orden(2).build(),
                InformeRevisor.builder().docenteId(DOC_C).orden(3).build()));
        lenient().when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        lenient().when(correlativoRepository.siguienteCorrelativo(anyInt())).thenReturn(1);
        lenient().when(resolverPlantilla.resolverDictamenJuradoInforme(any(), any(), any(), any(), any()))
                .thenReturn(Map.of("NOMBRE", "x"));
    }

    @Test
    void elaborarDictamen_recibidoYJuradoCompleto_generaYGuarda() {
        conExpedienteRecibidoYJuradoDesignado();

        service.elaborarDictamen(TESIS, new ElaborarDictamenJuradoInformeRequest());

        verify(dictamenRepository).save(argThat(d ->
                d.getEstado() == EstadoDictamen.ELABORADO && d.getDatosDictamen() != null));
    }

    @Test
    void elaborarDictamen_sinRecepcionar_falla() {
        assertThrows(BusinessException.class,
                () -> service.elaborarDictamen(TESIS, new ElaborarDictamenJuradoInformeRequest()));
        verify(dictamenRepository, never()).save(any());
    }

    @Test
    void elaborarDictamen_juradoIncompleto_falla() {
        proyectoSolicitado.setExpedienteInformeRecibido(true);
        when(informeRevisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(
                InformeRevisor.builder().docenteId(DOC_A).orden(1).build()));

        assertThrows(BusinessException.class,
                () -> service.elaborarDictamen(TESIS, new ElaborarDictamenJuradoInformeRequest()));
    }

    @Test
    void elaborarDictamen_yaFirmado_falla() {
        proyectoSolicitado.setExpedienteInformeRecibido(true);
        when(informeRevisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(
                InformeRevisor.builder().docenteId(DOC_A).orden(1).build(),
                InformeRevisor.builder().docenteId(DOC_B).orden(2).build(),
                InformeRevisor.builder().docenteId(DOC_C).orden(3).build()));
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(
                DictamenJuradoInforme.builder().tesisId(TESIS).estado(EstadoDictamen.FIRMADO).build()));

        assertThrows(BusinessException.class,
                () -> service.elaborarDictamen(TESIS, new ElaborarDictamenJuradoInformeRequest()));
    }

    // ── subirDictamenFirmado ──

    @Test
    void subirDictamenFirmado_elaborado_marcaFirmado() {
        DictamenJuradoInforme dic = DictamenJuradoInforme.builder().tesisId(TESIS).estado(EstadoDictamen.ELABORADO).build();
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(dic));
        when(documentoTesisRepository.buscarPorTesisYTipo(any(), any())).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), any(), any())).thenReturn("key-1");

        service.subirDictamenFirmado(TESIS, doc(), "d.pdf", "application/pdf");

        assertEquals(EstadoDictamen.FIRMADO, dic.getEstado());
        verify(dictamenRepository).save(dic);
    }

    @Test
    void subirDictamenFirmado_sinElaborar_falla() {
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> service.subirDictamenFirmado(TESIS, doc(), "d.pdf", "application/pdf"));
    }

    @Test
    void subirDictamenFirmado_porElaborar_falla() {
        DictamenJuradoInforme dic = DictamenJuradoInforme.builder().tesisId(TESIS).estado(EstadoDictamen.POR_ELABORAR).build();
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(dic));
        assertThrows(BusinessException.class,
                () -> service.subirDictamenFirmado(TESIS, doc(), "d.pdf", "application/pdf"));
    }

    // ── archivarExpediente ──

    @Test
    void archivarExpediente_conConformidad_marcaArchivado() {
        proyectoSolicitado.setInformeFinalRevisado(true);
        when(documentoTesisRepository.buscarPorTesisYTipo(any(), any())).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), any(), any())).thenReturn("key-2");

        service.archivarExpediente(TESIS, doc(), "expediente.pdf", "application/pdf");

        assertTrue(Boolean.TRUE.equals(proyectoSolicitado.getInformeFinalArchivado()));
        assertNotNull(proyectoSolicitado.getFechaArchivoInforme());
    }

    @Test
    void archivarExpediente_sinConformidad_falla() {
        assertThrows(BusinessException.class,
                () -> service.archivarExpediente(TESIS, doc(), "expediente.pdf", "application/pdf"));
        verify(proyectoRepository, never()).save(any());
    }

    // ── elaborarDictamenExpedito ──

    @Test
    void elaborarExpedito_conExpedienteArchivado_generaYGuarda() {
        proyectoSolicitado.setInformeFinalArchivado(true);
        when(informeRevisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(
                InformeRevisor.builder().docenteId(DOC_A).orden(1).build()));
        when(expeditoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        when(correlativoRepository.siguienteCorrelativo(anyInt())).thenReturn(2);
        when(resolverPlantilla.resolverDictamenExpedito(any(), any(), any(), any(), any()))
                .thenReturn(Map.of("NOMBRE", "x"));

        service.elaborarDictamenExpedito(TESIS, new ElaborarDictamenExpeditoRequest());

        verify(expeditoRepository).save(argThat(d ->
                d.getEstado() == EstadoDictamen.ELABORADO && d.getDatosDictamen() != null));
    }

    @Test
    void elaborarExpedito_sinArchivar_falla() {
        assertThrows(BusinessException.class,
                () -> service.elaborarDictamenExpedito(TESIS, new ElaborarDictamenExpeditoRequest()));
        verify(expeditoRepository, never()).save(any());
    }

    @Test
    void elaborarExpedito_yaFirmado_falla() {
        proyectoSolicitado.setInformeFinalArchivado(true);
        when(expeditoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(
                DictamenExpedito.builder().tesisId(TESIS).estado(EstadoDictamen.FIRMADO).build()));

        assertThrows(BusinessException.class,
                () -> service.elaborarDictamenExpedito(TESIS, new ElaborarDictamenExpeditoRequest()));
    }

    // ── subirDictamenExpeditoFirmado ──

    @Test
    void subirExpeditoFirmado_elaborado_marcaFirmado() {
        DictamenExpedito dic = DictamenExpedito.builder().tesisId(TESIS).estado(EstadoDictamen.ELABORADO).build();
        when(expeditoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(dic));
        when(documentoTesisRepository.buscarPorTesisYTipo(any(), any())).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), any(), any())).thenReturn("key-3");

        service.subirDictamenExpeditoFirmado(TESIS, doc(), "expedito.pdf", "application/pdf");

        assertEquals(EstadoDictamen.FIRMADO, dic.getEstado());
        verify(expeditoRepository).save(dic);
    }

    @Test
    void subirExpeditoFirmado_sinElaborar_falla() {
        when(expeditoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> service.subirDictamenExpeditoFirmado(TESIS, doc(), "e.pdf", "application/pdf"));
    }

    // ── documentoDictamen / documentoExpedito ──

    @Test
    void documentoDictamen_sinElaborar_falla() {
        when(dictamenRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.documentoDictamen(TESIS, "pdf"));
    }

    @Test
    void documentoExpedito_sinElaborar_falla() {
        when(expeditoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.documentoExpedito(TESIS, "pdf"));
    }

    // ── bandeja / detalle: proyecto inexistente ──

    @Test
    void detalle_proyectoInexistente_falla() {
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.detalle(TESIS));
    }
}
