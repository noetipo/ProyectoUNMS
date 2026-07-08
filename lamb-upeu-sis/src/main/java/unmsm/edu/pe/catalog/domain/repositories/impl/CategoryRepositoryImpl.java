package unmsm.edu.pe.catalog.domain.repositories.impl;// src/main/java/upeu/edu/pe/catalog/infrastructure/repositories/CategoryRepositoryImpl.java

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.catalog.domain.entities.Category;
import unmsm.edu.pe.catalog.domain.repositories.CategoryRepository;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CategoryRepositoryImpl implements CategoryRepository, PanacheRepositoryBase<Category, Long> {

    @Override
    public List<Category> getAllCategories() {
        return listAll();
    }

    @Override
    public Optional<Category> update(Long id, Category category) {
        Optional<Category> existing = findByIdOptional(id);
        if (existing.isPresent()) {
            Category existingCategory = existing.get();
            existingCategory.setName(category.getName());
            existingCategory.setDescription(category.getDescription());
            existingCategory.setActive(category.getActive());
            return Optional.of(existingCategory);
        }
        return Optional.empty();
    }

    @Override
    public Category save(Category category) {
        persist(category);
        return category;
    }

    @Override
    public Optional<Category> getCategoryById(Long id) {
        return findByIdOptional(id);
    }

    @Override
    public void removeById(Long id) {  // Cambiado de deleteById a removeById
        delete("id", id);
    }
}