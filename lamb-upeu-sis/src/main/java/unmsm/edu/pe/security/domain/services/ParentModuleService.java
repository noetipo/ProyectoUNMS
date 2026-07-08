package unmsm.edu.pe.security.domain.services;

import unmsm.edu.pe.security.application.dto.ParentModuleDTO;
import unmsm.edu.pe.security.application.dto.ParentModuleRequestDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ParentModuleService {
    Map<String, Object> listPaginate(Integer page, Integer size, String nameFilter);
    List<ParentModuleDTO> list(String nameFilter);
    ParentModuleDTO save(ParentModuleRequestDto requestDto);
    ParentModuleDTO findById(UUID uuid);
    ParentModuleDTO findByCode(String code);
    ParentModuleDTO update(UUID uuid, ParentModuleRequestDto requestDto);
    void delete(UUID uuid);
}
