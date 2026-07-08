// src/main/java/upeu/edu/pe/catalog/domain/services/CategoryService.java
package unmsm.edu.pe.catalog.domain.services;

import unmsm.edu.pe.catalog.application.dto.CategoryRequestDto;
import unmsm.edu.pe.catalog.application.dto.CategoryResponseDto;
import unmsm.edu.pe.catalog.application.dto.CategoryUpdateDto;
import java.util.List;

public interface CategoryService {
    List<CategoryResponseDto> findAll();
    CategoryResponseDto findById(Long id);
    CategoryResponseDto create(CategoryRequestDto requestDto);
    CategoryResponseDto update(Long id, CategoryUpdateDto updateDto);
    void deleteById(Long id);
}