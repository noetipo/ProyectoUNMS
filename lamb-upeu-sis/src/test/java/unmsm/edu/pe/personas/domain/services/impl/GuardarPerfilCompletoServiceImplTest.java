package unmsm.edu.pe.personas.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.application.dto.ArchivoSubido;
import unmsm.edu.pe.personas.application.dto.PerfilCompletoData;
import unmsm.edu.pe.personas.domain.entities.*;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.personas.domain.repositories.*;
import unmsm.edu.pe.personas.domain.services.PersonaService;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.shared.exceptions.ValidationException;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardarPerfilCompletoServiceImplTest {

    @Mock PersonaRepository personaRepository;
    @Mock CargoRepository cargoRepository;
    @Mock CentroLaboralRepository centroLaboralRepository;
    @Mock PersonaCargoRepository personaCargoRepository;
    @Mock PersonaCentroLaboralRepository personaCentroLaboralRepository;
    @Mock DocumentoPersonaRepository documentoRepository;
    @Mock AlmacenamientoArchivos almacenamiento;
    @Mock PersonaService personaService;

    @InjectMocks GuardarPerfilCompletoServiceImpl service;

    private Persona persona;
    private UUID personaId;

    @BeforeEach
    void setUp() throws Exception {
        personaId = UUID.randomUUID();
        persona = Persona.builder().id(personaId).nombres("Ana").apellidoPaterno("Perez").build();
        setField("maxSizeBytes", 10_485_760L);
        setField("allowedContentTypes", "application/pdf,image/jpeg,image/png");
    }

    private void setField(String name, Object value) throws Exception {
        Field f = GuardarPerfilCompletoServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    private ArchivoSubido pdf(String nombre) {
        byte[] bytes = "%PDF-1.4 contenido".getBytes();
        return new ArchivoSubido(bytes, nombre, "application/pdf", bytes.length);
    }

    private PerfilCompletoData.CargoHistorial cargo(UUID id, boolean actual) {
        return new PerfilCompletoData.CargoHistorial(id, null, null, actual);
    }

    @Test
    void aplicar_feliz_guardaHistorialesYDocumentos() {
        UUID cargoId = UUID.randomUUID();
        UUID centroId = UUID.randomUUID();
        PerfilCompletoData data = new PerfilCompletoData(
                List.of(cargo(cargoId, true)),
                List.of(new PerfilCompletoData.CentroHistorial(centroId, null, null, true)));

        when(cargoRepository.buscarPorId(cargoId)).thenReturn(Optional.of(Cargo.builder().id(cargoId).nombre("Médico").build()));
        when(centroLaboralRepository.buscarPorId(centroId)).thenReturn(Optional.of(CentroLaboral.builder().id(centroId).nombre("Hospital").build()));
        when(documentoRepository.findByPersonaIdAndTipo(any(), any())).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), anyString(), anyString())).thenReturn("key-generada");

        Map<TipoDocumento, ArchivoSubido> archivos = new EnumMap<>(TipoDocumento.class);
        archivos.put(TipoDocumento.DNI, pdf("dni.pdf"));
        archivos.put(TipoDocumento.PARTIDA_NACIMIENTO, pdf("partida.pdf"));

        service.aplicar(persona, data, archivos, true);

        verify(personaCargoRepository).deleteByPersonaId(personaId);
        verify(personaCargoRepository).saveAll(any());
        verify(personaCentroLaboralRepository).deleteByPersonaId(personaId);
        verify(personaCentroLaboralRepository).saveAll(any());
        verify(almacenamiento, times(2)).guardar(any(), anyString(), anyString());
        verify(documentoRepository, times(2)).save(any(DocumentoPersona.class));
    }

    @Test
    void aplicar_faltaDocumentoObligatorio_lanzaValidation_ySinSubirArchivos() {
        when(documentoRepository.existsByPersonaIdAndTipo(any(), any())).thenReturn(false);
        Map<TipoDocumento, ArchivoSubido> soloDni = new EnumMap<>(TipoDocumento.class);
        soloDni.put(TipoDocumento.DNI, pdf("dni.pdf")); // falta PARTIDA

        assertThrows(ValidationException.class, () -> service.aplicar(persona, null, soloDni, true));
        verify(almacenamiento, never()).guardar(any(), anyString(), anyString());
    }

    @Test
    void aplicar_dobleActual_lanzaValidation() {
        PerfilCompletoData data = new PerfilCompletoData(
                List.of(cargo(UUID.randomUUID(), true), cargo(UUID.randomUUID(), true)), null);

        assertThrows(ValidationException.class, () -> service.aplicar(persona, data, Map.of(), false));
        verify(personaCargoRepository, never()).saveAll(any());
    }

    @Test
    void aplicar_documentoExistente_reemplazaYBorraArchivoAnterior() {
        DocumentoPersona existente = DocumentoPersona.builder()
                .id(UUID.randomUUID()).tipoDocumento(TipoDocumento.DNI)
                .storageKey("key-antigua").persona(persona).build();
        when(documentoRepository.findByPersonaIdAndTipo(personaId, TipoDocumento.DNI)).thenReturn(Optional.of(existente));
        when(almacenamiento.guardar(any(), anyString(), anyString())).thenReturn("key-nueva");

        Map<TipoDocumento, ArchivoSubido> archivos = new EnumMap<>(TipoDocumento.class);
        archivos.put(TipoDocumento.DNI, pdf("dni-nuevo.pdf"));

        service.aplicar(persona, null, archivos, false);

        // se guardó el nuevo archivo y se actualizó la fila con la nueva key
        verify(documentoRepository).save(existente);
        assertEquals("key-nueva", existente.getStorageKey());
        // se borró el archivo anterior (best-effort)
        verify(almacenamiento).eliminar("key-antigua");
    }

    @Test
    void descargarDocumento_propio_devuelveContenido() {
        UUID documentoId = UUID.randomUUID();
        DocumentoPersona doc = DocumentoPersona.builder()
                .id(documentoId).tipoDocumento(TipoDocumento.DNI)
                .storageKey("k").contentType("application/pdf").nombreOriginal("dni.pdf")
                .persona(persona).build();
        when(documentoRepository.buscarPorId(documentoId)).thenReturn(Optional.of(doc));
        when(almacenamiento.obtener("k")).thenReturn("bytes".getBytes());

        var descarga = service.descargarDocumento(personaId, documentoId);

        assertArrayEquals("bytes".getBytes(), descarga.contenido());
        assertEquals("application/pdf", descarga.contentType());
        assertEquals("dni.pdf", descarga.nombreOriginal());
    }

    @Test
    void descargarDocumento_deOtraPersona_lanzaNotFound_ySinLeerStorage() {
        UUID documentoId = UUID.randomUUID();
        DocumentoPersona ajeno = DocumentoPersona.builder()
                .id(documentoId).tipoDocumento(TipoDocumento.DNI).storageKey("k")
                .persona(Persona.builder().id(UUID.randomUUID()).build()).build();
        when(documentoRepository.buscarPorId(documentoId)).thenReturn(Optional.of(ajeno));

        assertThrows(unmsm.edu.pe.shared.exceptions.NotFoundException.class,
                () -> service.descargarDocumento(personaId, documentoId));
        verify(almacenamiento, never()).obtener(anyString());
    }

    @Test
    void aplicar_usaSoloElPuertoDeAlmacenamiento() {
        // El caso de uso no conoce local/S3: solo invoca el puerto (mock).
        when(documentoRepository.findByPersonaIdAndTipo(any(), any())).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), anyString(), anyString())).thenReturn("k");
        Map<TipoDocumento, ArchivoSubido> archivos = new EnumMap<>(TipoDocumento.class);
        archivos.put(TipoDocumento.DNI, pdf("d.pdf"));

        service.aplicar(persona, null, archivos, false);

        verify(almacenamiento).guardar(any(), anyString(), anyString());
        verifyNoMoreInteractions(almacenamiento);
    }
}
