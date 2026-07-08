package unmsm.edu.pe.personas.infrastructure.web;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import unmsm.edu.pe.personas.application.dto.ArchivoSubido;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.shared.exceptions.BusinessException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Map;

/**
 * Form multipart del perfil completo: una parte JSON ({@code datos}) y una parte
 * de archivo por cada tipo de documento (nombre del campo = tipo).
 */
public class PerfilMultipartForm {

    @RestForm("datos")
    public String datos;

    @RestForm("DNI")
    public FileUpload dni;

    @RestForm("CARNET_EXTRANJERIA")
    public FileUpload carnetExtranjeria;

    @RestForm("PASAPORTE")
    public FileUpload pasaporte;

    @RestForm("PTP")
    public FileUpload ptp;

    @RestForm("CARNET_DIPLOMATICO")
    public FileUpload carnetDiplomatico;

    @RestForm("DNI_CE")
    public FileUpload dniCe;

    @RestForm("PARTIDA_NACIMIENTO")
    public FileUpload partidaNacimiento;

    /** Archivos presentes mapeados por tipo de documento. */
    public Map<TipoDocumento, FileUpload> archivos() {
        Map<TipoDocumento, FileUpload> m = new EnumMap<>(TipoDocumento.class);
        put(m, TipoDocumento.DNI, dni);
        put(m, TipoDocumento.CARNET_EXTRANJERIA, carnetExtranjeria);
        put(m, TipoDocumento.PASAPORTE, pasaporte);
        put(m, TipoDocumento.PTP, ptp);
        put(m, TipoDocumento.CARNET_DIPLOMATICO, carnetDiplomatico);
        put(m, TipoDocumento.DNI_CE, dniCe);
        put(m, TipoDocumento.PARTIDA_NACIMIENTO, partidaNacimiento);
        return m;
    }

    private static void put(Map<TipoDocumento, FileUpload> m, TipoDocumento tipo, FileUpload f) {
        if (f != null) {
            m.put(tipo, f);
        }
    }

    /** Lee los archivos presentes a memoria, mapeados por tipo de documento. */
    public Map<TipoDocumento, ArchivoSubido> leerArchivos() {
        Map<TipoDocumento, ArchivoSubido> out = new EnumMap<>(TipoDocumento.class);
        for (Map.Entry<TipoDocumento, FileUpload> e : archivos().entrySet()) {
            FileUpload fu = e.getValue();
            try {
                byte[] bytes = Files.readAllBytes(fu.uploadedFile());
                out.put(e.getKey(), new ArchivoSubido(bytes, fu.fileName(), fu.contentType(), fu.size()));
            } catch (IOException ex) {
                throw new BusinessException("No se pudo leer el archivo " + e.getKey().name() + ": " + ex.getMessage(), ex);
            }
        }
        return out;
    }
}

