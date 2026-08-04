package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.tesis.application.dto.ArchivoDescargable;
import unmsm.edu.pe.tesis.application.dto.PlantillaRubricaItem;
import unmsm.edu.pe.tesis.application.util.RubricaDefinicion;
import unmsm.edu.pe.tesis.domain.entities.PlantillaRubrica;
import unmsm.edu.pe.tesis.domain.repositories.PlantillaRubricaRepository;
import unmsm.edu.pe.tesis.domain.services.PlantillaRubricaService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PlantillaRubricaServiceImpl implements PlantillaRubricaService {

    static final String WORD_CT = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    /** Los dos enfoques con rúbrica propia (el mixto se evalúa con la cuantitativa). */
    private static final String[] ENFOQUES = {"CUANTITATIVO", "CUALITATIVO"};

    @Inject PlantillaRubricaRepository plantillaRepository;
    @Inject AlmacenamientoArchivos almacenamiento;
    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;

    @Override
    @Transactional
    public List<PlantillaRubricaItem> listar() {
        List<PlantillaRubricaItem> out = new ArrayList<>();
        for (String enfoque : ENFOQUES) {
            var rubrica = RubricaDefinicion.porEnfoque(enfoque);
            var vigente = plantillaRepository.vigente(enfoque).orElse(null);
            var historial = plantillaRepository.historial(enfoque).stream()
                    .filter(p -> vigente == null || !p.getId().equals(vigente.getId()))
                    .map(p -> PlantillaRubricaItem.VersionItem.builder()
                            .id(p.getId()).version(p.getVersion())
                            .nombreOriginal(p.getNombreOriginal()).fechaCarga(p.getFechaCarga()).build())
                    .toList();
            out.add(PlantillaRubricaItem.builder()
                    .enfoque(enfoque)
                    .enfoqueLabel(label(enfoque))
                    .descripcion(descripcion(enfoque))
                    .criterios(rubrica.criterios().size())
                    .puntajeTotal(rubrica.total())
                    .puntajeAprobacion(RubricaDefinicion.APROBADO_MIN)
                    .id(vigente != null ? vigente.getId() : null)
                    .version(vigente != null ? vigente.getVersion() : null)
                    .nombreOriginal(vigente != null ? vigente.getNombreOriginal() : null)
                    .fechaCarga(vigente != null ? vigente.getFechaCarga() : null)
                    .historial(historial)
                    .build());
        }
        return out;
    }

    @Override
    @Transactional
    public PlantillaRubricaItem publicar(String enfoque, String version, byte[] contenido,
                                         String nombreOriginal, String contentType) {
        String e = normalizar(enfoque);
        if (contenido == null || contenido.length == 0) {
            throw new BusinessException("Adjunta la rúbrica en Word (.docx)");
        }
        if (contenido.length > 10 * 1024 * 1024) {
            throw new BusinessException("El archivo supera 10 MB");
        }
        if (!esWord(nombreOriginal, contentType)) {
            throw new BusinessException("La rúbrica debe subirse en Word (.docx)");
        }
        String v = version != null && !version.isBlank()
                ? version.trim()
                : String.valueOf(java.time.LocalDate.now().getYear());

        // La versión anterior NO se borra: los proyectos ya evaluándose conservan la suya.
        plantillaRepository.desmarcarVigentes(e);
        plantillaRepository.save(PlantillaRubrica.builder()
                .enfoque(e).version(v).vigente(true)
                .nombreOriginal(nombreOriginal).contenido(contenido).contentType(WORD_CT)
                .tamanioBytes((long) contenido.length)
                .fechaCarga(LocalDateTime.now())
                .subidoPor(personaActualId())
                .build());
        return listar().stream().filter(i -> i.getEnfoque().equals(e)).findFirst().orElseThrow();
    }

    @Override
    @Transactional
    public ArchivoDescargable documento(UUID plantillaId) {
        PlantillaRubrica p = plantillaRepository.buscarPorId(plantillaId)
                .orElseThrow(() -> new NotFoundException("Esa versión de la rúbrica no existe"));
        return archivo(p);
    }

    @Override
    @Transactional
    public ArchivoDescargable documentoVigente(String enfoque) {
        PlantillaRubrica p = plantillaRepository.vigente(normalizar(enfoque))
                .orElseThrow(() -> new BusinessException(
                        "Aún no se ha publicado la rúbrica oficial de este enfoque"));
        return archivo(p);
    }

    private ArchivoDescargable archivo(PlantillaRubrica p) {
        // El Word vive en la base; las primeras versiones quedaron en el almacenamiento de archivos.
        byte[] bytes = p.getContenido() != null && p.getContenido().length > 0
                ? p.getContenido()
                : almacenamiento.obtener(p.getStorageKey());
        String nombre = p.getNombreOriginal() != null ? p.getNombreOriginal()
                : "rubrica-" + p.getEnfoque().toLowerCase() + "-" + p.getVersion() + ".docx";
        return new ArchivoDescargable(bytes, WORD_CT, nombre);
    }

    /** El enfoque mixto comparte rúbrica con el cuantitativo. */
    private String normalizar(String enfoque) {
        return "CUALITATIVO".equalsIgnoreCase(enfoque != null ? enfoque.trim() : "")
                ? "CUALITATIVO" : "CUANTITATIVO";
    }

    private String label(String enfoque) {
        return "CUALITATIVO".equals(enfoque) ? "Cualitativa" : "Cuantitativa / mixta";
    }

    private String descripcion(String enfoque) {
        return "CUALITATIVO".equals(enfoque)
                ? "Se aplica a los proyectos con enfoque cualitativo."
                : "Se aplica a los proyectos con enfoque cuantitativo o mixto.";
    }

    private boolean esWord(String nombre, String contentType) {
        boolean porNombre = nombre != null && nombre.toLowerCase().endsWith(".docx");
        boolean porTipo = contentType != null && contentType.contains("wordprocessingml");
        return porNombre || porTipo;
    }

    private UUID personaActualId() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) return null;
        return personaRepository.findByUserId(userId).map(p -> p.getId()).orElse(null);
    }
}
