package unmsm.edu.pe.catalog.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.catalog.application.dto.CategoryRequestDto;
import unmsm.edu.pe.catalog.application.dto.CategoryResponseDto;
import unmsm.edu.pe.catalog.application.dto.CategoryUpdateDto;
import unmsm.edu.pe.catalog.application.mapper.CategoryMapper;
import unmsm.edu.pe.catalog.domain.entities.Category;
import unmsm.edu.pe.catalog.domain.repositories.CategoryRepository;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    CategoryMapper categoryMapper;

    @InjectMocks
    CategoryServiceImpl categoryService;

    private Category category;
    private CategoryResponseDto categoryResponseDto;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("ELECTRONICS");
        category.setDescription("Electronics category");

        categoryResponseDto = new CategoryResponseDto();
        categoryResponseDto.setId(1L);
        categoryResponseDto.setName("ELECTRONICS");
        categoryResponseDto.setDescription("Electronics category");
    }

    // --- findAll() ---

    @Test
    void findAll_returnsMappedCategoryList() {
        when(categoryRepository.getAllCategories()).thenReturn(List.of(category));
        when(categoryMapper.toResponseDtoList(List.of(category))).thenReturn(List.of(categoryResponseDto));

        List<CategoryResponseDto> result = categoryService.findAll();

        assertEquals(1, result.size());
        assertEquals("ELECTRONICS", result.get(0).getName());
        verify(categoryRepository).getAllCategories();
        verify(categoryMapper).toResponseDtoList(List.of(category));
    }

    @Test
    void findAll_withNoCategories_returnsEmptyList() {
        when(categoryRepository.getAllCategories()).thenReturn(Collections.emptyList());
        when(categoryMapper.toResponseDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        assertTrue(categoryService.findAll().isEmpty());
    }

    // --- findById() ---

    @Test
    void findById_withExistingId_returnsCategory() {
        when(categoryRepository.getCategoryById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponseDto(category)).thenReturn(categoryResponseDto);

        CategoryResponseDto result = categoryService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ELECTRONICS", result.getName());
    }

    @Test
    void findById_withUnknownId_throwsNotFoundException() {
        when(categoryRepository.getCategoryById(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> categoryService.findById(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    // --- create() ---

    @Test
    void create_withUniqueName_savesAndReturnsCategory() {
        CategoryRequestDto request = new CategoryRequestDto("BOOKS", "Books description", true);
        Category newEntity = new Category();
        newEntity.setName("BOOKS");
        CategoryResponseDto newResponse = new CategoryResponseDto();
        newResponse.setId(2L);
        newResponse.setName("BOOKS");

        when(categoryRepository.getAllCategories()).thenReturn(Collections.emptyList());
        when(categoryMapper.requestDtoToEntity(request)).thenReturn(newEntity);
        when(categoryRepository.save(newEntity)).thenReturn(newEntity);
        when(categoryMapper.toResponseDto(newEntity)).thenReturn(newResponse);

        CategoryResponseDto result = categoryService.create(request);

        assertEquals("BOOKS", result.getName());
        verify(categoryRepository).save(newEntity);
    }

    @Test
    void create_whenListIsEmpty_savesCategory() {
        CategoryRequestDto request = new CategoryRequestDto("FOOD", "Food items", true);
        Category foodEntity = new Category();
        foodEntity.setName("FOOD");
        CategoryResponseDto foodResponse = new CategoryResponseDto();
        foodResponse.setId(3L);
        foodResponse.setName("FOOD");

        when(categoryRepository.getAllCategories()).thenReturn(Collections.emptyList());
        when(categoryMapper.requestDtoToEntity(request)).thenReturn(foodEntity);
        when(categoryRepository.save(foodEntity)).thenReturn(foodEntity);
        when(categoryMapper.toResponseDto(foodEntity)).thenReturn(foodResponse);

        assertNotNull(categoryService.create(request));
    }

    @Test
    void create_withDuplicateName_throwsBusinessException() {
        CategoryRequestDto request = new CategoryRequestDto("ELECTRONICS", "Duplicate", true);
        when(categoryRepository.getAllCategories()).thenReturn(List.of(category));

        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.create(request));
        assertTrue(ex.getMessage().contains("ELECTRONICS"));
        verify(categoryRepository, never()).save(any());
    }

    // --- update() ---

    @Test
    void update_withExistingIdAndNewUniqueName_updatesSuccessfully() {
        CategoryUpdateDto updateDto = new CategoryUpdateDto("TECH", "Tech desc", true);
        Category updatedEntity = new Category();
        updatedEntity.setId(1L);
        updatedEntity.setName("TECH");
        CategoryResponseDto updatedResponse = new CategoryResponseDto();
        updatedResponse.setId(1L);
        updatedResponse.setName("TECH");

        when(categoryRepository.getCategoryById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.getAllCategories()).thenReturn(Collections.emptyList());
        when(categoryRepository.update(1L, category)).thenReturn(Optional.of(updatedEntity));
        when(categoryMapper.toResponseDto(updatedEntity)).thenReturn(updatedResponse);

        CategoryResponseDto result = categoryService.update(1L, updateDto);

        assertNotNull(result);
        verify(categoryMapper).updateEntityFromDto(updateDto, category);
        verify(categoryRepository).update(1L, category);
    }

    @Test
    void update_withSameName_skipsUniquenessCheck() {
        CategoryUpdateDto updateDto = new CategoryUpdateDto("ELECTRONICS", null, null);

        when(categoryRepository.getCategoryById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.update(1L, category)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponseDto(category)).thenReturn(categoryResponseDto);

        assertDoesNotThrow(() -> categoryService.update(1L, updateDto));
        verify(categoryRepository, never()).getAllCategories();
    }

    @Test
    void update_withUnknownId_throwsNotFoundException() {
        CategoryUpdateDto updateDto = new CategoryUpdateDto("NEW", null, null);
        when(categoryRepository.getCategoryById(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> categoryService.update(99L, updateDto));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void update_withNameConflictInOtherCategory_throwsBusinessException() {
        Category otherCategory = new Category();
        otherCategory.setId(2L);
        otherCategory.setName("BOOKS");

        CategoryUpdateDto updateDto = new CategoryUpdateDto("BOOKS", null, null);
        when(categoryRepository.getCategoryById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.getAllCategories()).thenReturn(List.of(category, otherCategory));

        assertThrows(BusinessException.class, () -> categoryService.update(1L, updateDto));
    }

    // --- deleteById() ---

    @Test
    void deleteById_withExistingId_removesCategory() {
        when(categoryRepository.getCategoryById(1L)).thenReturn(Optional.of(category));

        assertDoesNotThrow(() -> categoryService.deleteById(1L));
        verify(categoryRepository).removeById(1L);
    }

    @Test
    void deleteById_withUnknownId_throwsNotFoundException() {
        when(categoryRepository.getCategoryById(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> categoryService.deleteById(99L));
        assertTrue(ex.getMessage().contains("99"));
        verify(categoryRepository, never()).removeById(any());
    }
}