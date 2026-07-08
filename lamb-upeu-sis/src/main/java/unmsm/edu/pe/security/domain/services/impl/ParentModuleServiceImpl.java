package unmsm.edu.pe.security.domain.services.impl;

import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.security.application.dto.ParentModuleDTO;
import unmsm.edu.pe.security.application.dto.ParentModuleRequestDto;
import unmsm.edu.pe.security.application.mapper.ParentModuleMapper;
import unmsm.edu.pe.security.domain.entities.ParentModule;
import unmsm.edu.pe.security.domain.repositories.ParentModuleRepository;
import unmsm.edu.pe.security.domain.services.ParentModuleService;
import unmsm.edu.pe.shared.exceptions.NotFoundException;

import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.shared.exceptions.NotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class ParentModuleServiceImpl implements ParentModuleService {

    @Inject
    ParentModuleRepository parentModuleRepository;

    //@Inject
    //ModuleRepository moduleRepository;

    @Inject
    ParentModuleMapper parentModuleMapper;

    //@Inject
    //ModuleMapper moduleMapper;

    @Override
    public Map<String, Object> listPaginate(Integer page, Integer size, String nameFilter) {
        Page panachePage = Page.of(page, size);

        List<ParentModule> parentModules = parentModuleRepository.findWithFilters(nameFilter, panachePage);
        long totalElements = parentModuleRepository.countWithFilters(nameFilter);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        Map<String, Object> response = new HashMap<>();
        response.put("content", parentModuleMapper.toDTOList(parentModules));
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);

        return response;
    }

    @Override
    public List<ParentModuleDTO> list(String nameFilter) {
        List<ParentModule> parentModules = parentModuleRepository.findAllActiveWithFilters(nameFilter);
        return parentModuleMapper.toDTOList(parentModules);
    }

    /*@Override
    public List<ParentModuleDTO> listDetailModuleList() {
        List<ParentModule> parentModules = parentModuleRepository.getAllParentModules();

        return parentModules.stream()
                .map(parentModule -> {
                    ParentModuleDTO parentModuleDTO = parentModuleMapper.toDTO(parentModule);

                    List<Module> modules = moduleRepository.findByParentModule(parentModule);
                    List<ModuleDTO> moduleDTOs = modules.stream()
                            .map(moduleMapper::toDto)
                            .collect(Collectors.toList());

                    parentModuleDTO.setModuleDTOS(moduleDTOs);
                    return parentModuleDTO;
                })
                .collect(Collectors.toList());
    }*/

    @Override
    public ParentModuleDTO save(ParentModuleRequestDto requestDto) {
        ParentModule parentModule = parentModuleMapper.requestDtoToEntity(requestDto);
        ParentModule saved = parentModuleRepository.save(parentModule);
        return parentModuleMapper.toDTO(saved);
    }

    @Override
    public ParentModuleDTO findById(UUID uuid) {
        ParentModule parentModule = parentModuleRepository.getParentModuleById(uuid)
                .orElseThrow(() -> new NotFoundException("ParentModule not found with ID: " + uuid));
        return parentModuleMapper.toDTO(parentModule);
    }

    @Override
    public ParentModuleDTO findByCode(String code) {
        ParentModule parentModule = parentModuleRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("ParentModule not found with code: " + code));
        return parentModuleMapper.toDTO(parentModule);
    }

    @Override
    public ParentModuleDTO update(UUID uuid, ParentModuleRequestDto requestDto) {
        ParentModule parentModule = parentModuleMapper.requestDtoToEntity(requestDto);
        ParentModule updated = parentModuleRepository.update(uuid, parentModule)
                .orElseThrow(() -> new NotFoundException("ParentModule not found with ID: " + uuid));
        return parentModuleMapper.toDTO(updated);
    }

    @Override
    public void delete(UUID uuid) {
        ParentModule parentModule = parentModuleRepository.getParentModuleById(uuid)
                .orElseThrow(() -> new NotFoundException("ParentModule not found with ID: " + uuid));

        parentModule.markAsDeleted();
        parentModuleRepository.remove(parentModule);
    }
}