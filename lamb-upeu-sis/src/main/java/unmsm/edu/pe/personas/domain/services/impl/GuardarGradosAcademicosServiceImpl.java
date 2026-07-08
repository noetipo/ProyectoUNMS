package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import unmsm.edu.pe.personas.application.dto.GradoAcademicoRequest;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.PersonaGradoAcademico;
import unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository;
import unmsm.edu.pe.personas.domain.services.GuardarGradosAcademicosService;
import unmsm.edu.pe.shared.exceptions.ValidationException;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class GuardarGradosAcademicosServiceImpl implements GuardarGradosAcademicosService {

    @Inject PersonaGradoAcademicoRepository personaGradoRepository;

    @Override
    public void aplicar(Persona persona, List<GradoAcademicoRequest> grados) {
        if (grados == null) {
            return; // no provisto: no se modifican los grados existentes
        }

        long principales = grados.stream().filter(g -> Boolean.TRUE.equals(g.getPrincipal())).count();
        if (principales > 1) {
            throw new ValidationException("Solo puede haber un grado académico marcado como principal");
        }

        // Reemplazo total idempotente.
        personaGradoRepository.deleteByPersonaId(persona.getId());

        List<PersonaGradoAcademico> nuevos = grados.stream()
                .map(g -> PersonaGradoAcademico.builder()
                        .persona(persona)
                        .grado(g.getGrado())
                        .anio(g.getAnio())
                        .universidad(trimToNull(g.getUniversidad()))
                        .principal(Boolean.TRUE.equals(g.getPrincipal()))
                        .build())
                .collect(Collectors.toList());

        if (!nuevos.isEmpty()) {
            personaGradoRepository.saveAll(nuevos);
        }
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
