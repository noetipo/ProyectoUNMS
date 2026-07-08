// src/main/java/upeu/edu/pe/catalog/domain/services/impl/CategoryServiceImpl.java
package unmsm.edu.pe.catalog.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.catalog.application.dto.CategoryRequestDto;
import unmsm.edu.pe.catalog.application.dto.CategoryResponseDto;
import unmsm.edu.pe.catalog.application.dto.CategoryUpdateDto;
import unmsm.edu.pe.catalog.application.mapper.CategoryMapper;
import unmsm.edu.pe.catalog.domain.entities.Category;
import unmsm.edu.pe.catalog.domain.repositories.CategoryRepository;
import unmsm.edu.pe.catalog.domain.services.CategoryService;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class CategoryServiceImpl implements CategoryService {

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponseDto> findAll() {
        List<Category> categories = categoryRepository.getAllCategories(); // Cambiado
        return categoryMapper.toResponseDtoList(categories);
    }

    @Override
    public CategoryResponseDto findById(Long id) {
        Category category = categoryRepository.getCategoryById(id) // Cambiado
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
        return categoryMapper.toResponseDto(category);
    }

    @Override
    public CategoryResponseDto create(CategoryRequestDto requestDto) {
        // Validación manual de nombre único
        List<Category> existingCategories = categoryRepository.getAllCategories(); // Cambiado
        boolean nameExists = existingCategories.stream()
                .anyMatch(cat -> cat.getName().equals(requestDto.getName()));

        if (nameExists) {
            throw new BusinessException("Category with name '" + requestDto.getName() + "' already exists");
        }

        Category category = categoryMapper.requestDtoToEntity(requestDto);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponseDto(savedCategory);
    }

    @Override
    public CategoryResponseDto update(Long id, CategoryUpdateDto updateDto) {
        Category existingCategory = categoryRepository.getCategoryById(id) // Cambiado
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        if (updateDto.getName() != null && !updateDto.getName().equals(existingCategory.getName())) {
            // Validación manual de nombre único
            List<Category> allCategories = categoryRepository.getAllCategories(); // Cambiado
            boolean nameExists = allCategories.stream()
                    .anyMatch(cat -> cat.getName().equals(updateDto.getName()) && !cat.getId().equals(id));

            if (nameExists) {
                throw new BusinessException("Category with name '" + updateDto.getName() + "' already exists");
            }
        }

        categoryMapper.updateEntityFromDto(updateDto, existingCategory);

        // Usar el método update de tu interfaz
        Optional<Category> updatedCategory = categoryRepository.update(id, existingCategory);
        return updatedCategory.map(categoryMapper::toResponseDto)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
    }

    @Override
    public void deleteById(Long id) {
        // Validar que existe antes de eliminar
        Optional<Category> category = categoryRepository.getCategoryById(id); // Cambiado
        if (category.isEmpty()) {
            throw new NotFoundException("Category not found with id: " + id);
        }
        categoryRepository.removeById(id); // Cambiado
    }
}