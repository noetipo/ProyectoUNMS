package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import unmsm.edu.pe.personas.application.dto.ProgramaPosgradoResponse;
import unmsm.edu.pe.personas.application.mapper.PersonaMapper;
import unmsm.edu.pe.personas.domain.repositories.ProgramaPosgradoRepository;
import unmsm.edu.pe.personas.domain.services.ProgramaPosgradoService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProgramaPosgradoServiceImpl implements ProgramaPosgradoService {

    @Inject ProgramaPosgradoRepository programaRepository;
    @Inject PersonaMapper mapper;

    @Override
    public List<ProgramaPosgradoResponse> list(UUID facultadId) {
        return programaRepository.getByFacultad(facultadId).stream()
                .map(mapper::toProgramaResponse)
                .collect(Collectors.toList());
    }
}
