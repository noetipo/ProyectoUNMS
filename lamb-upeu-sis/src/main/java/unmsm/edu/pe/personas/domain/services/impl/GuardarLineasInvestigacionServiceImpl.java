package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import unmsm.edu.pe.personas.application.dto.DocenteLineaRequest;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.DocenteLineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.services.GuardarLineasInvestigacionService;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.ValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GuardarLineasInvestigacionServiceImpl implements GuardarLineasInvestigacionService {

    @Inject LineaInvestigacionRepository lineaInvestigacionRepository;
    @Inject DocenteLineaInvestigacionRepository docenteLineaRepository;

    @Override
    public void aplicar(Docente docente, List<DocenteLineaRequest> lineas) {
        if (lineas == null) {
            return; // no provisto: no se modifican las líneas existentes
        }

        // Regla "una sola principal"
        long principales = lineas.stream().filter(l -> Boolean.TRUE.equals(l.getEsPrincipal())).count();
        if (principales > 1) {
            throw new ValidationException("Solo puede haber una línea de investigación marcada como principal");
        }

        // Sin duplicados de la misma línea
        Set<UUID> vistos = new HashSet<>();
        for (DocenteLineaRequest l : lineas) {
            if (l.getLineaInvestigacionId() == null) {
                throw new ValidationException("La línea de investigación es obligatoria");
            }
            if (!vistos.add(l.getLineaInvestigacionId())) {
                throw new ValidationException("Línea de investigación duplicada: " + l.getLineaInvestigacionId());
            }
        }

        UUID docenteId = docente.getPersonaId();

        // Reemplazo total (idempotente)
        docenteLineaRepository.deleteByDocenteId(docenteId);

        List<DocenteLineaInvestigacion> nuevas = lineas.stream().map(l -> {
            LineaInvestigacion linea = lineaInvestigacionRepository.buscarPorId(l.getLineaInvestigacionId())
                    .filter(li -> Boolean.TRUE.equals(li.getActive()))
                    .orElseThrow(() -> new BusinessException(
                            "Línea de investigación no encontrada o inactiva: " + l.getLineaInvestigacionId()));
            return DocenteLineaInvestigacion.builder()
                    .docente(docente)
                    .lineaInvestigacion(linea)
                    .esPrincipal(Boolean.TRUE.equals(l.getEsPrincipal()))
                    .build();
        }).collect(Collectors.toList());

        if (!nuevas.isEmpty()) {
            docenteLineaRepository.saveAll(nuevas);
        }
    }
}
