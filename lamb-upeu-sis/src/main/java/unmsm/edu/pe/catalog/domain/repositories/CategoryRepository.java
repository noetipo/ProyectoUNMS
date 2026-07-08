// src/main/java/upeu/edu/pe/catalog/domain/repositories/CategoryRepository.java
package unmsm.edu.pe.catalog.domain.repositories;

import unmsm.edu.pe.catalog.domain.entities.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    List<Category> getAllCategories();
    Optional<Category> update(Long id, Category category);
    Category save(Category category);
    Optional<Category> getCategoryById(Long id);
    void removeById(Long id);  // Cambiado de deleteById a removeById
}