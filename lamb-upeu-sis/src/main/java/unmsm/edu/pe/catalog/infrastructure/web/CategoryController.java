// src/main/java/upeu/edu/pe/catalog/infrastructure/web/CategoryController.java
package unmsm.edu.pe.catalog.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.catalog.application.dto.CategoryRequestDto;
import unmsm.edu.pe.catalog.application.dto.CategoryResponseDto;
import unmsm.edu.pe.catalog.application.dto.CategoryUpdateDto;
import unmsm.edu.pe.catalog.domain.services.CategoryService;
import unmsm.edu.pe.shared.response.ApiResponse;

import java.util.List;

@Path("/api/v1/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
//@Secured  // Agregar esta anotación

@Tag(name = "Categories", description = "Category management operations")
public class CategoryController {

    @Inject
    CategoryService categoryService;
    @GET
    @Path("/ping")
    public Response ping() {
        System.out.println("=== PING ENDPOINT CALLED ===");
        return Response.ok("pong").build();
    }
    @GET
    @Operation(summary = "Get all categories", description = "Retrieve all categories")
    @APIResponse(responseCode = "200", description = "Categories retrieved successfully")
    public Response getAllCategories(@QueryParam("active") @DefaultValue("false") boolean activeOnly) {
        System.out.println("=== CATEGORIES ENDPOINT CALLED ===");

        List<CategoryResponseDto> categories = activeOnly ?
                categoryService.findAll() : categoryService.findAll();

        return Response.ok(ApiResponse.success("Categories retrieved successfully", categories)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieve a specific category by its ID")
    @APIResponse(responseCode = "200", description = "Category found")
    @APIResponse(responseCode = "404", description = "Category not found")
    public Response getCategoryById(@Parameter(description = "Category ID") @PathParam("id") Long id) {
        CategoryResponseDto category = categoryService.findById(id);
        return Response.ok(ApiResponse.success("Category retrieved successfully", category)).build();
    }

    @POST
    @Operation(summary = "Create category", description = "Create a new category")
    @APIResponse(responseCode = "201", description = "Category created successfully")
    @APIResponse(responseCode = "400", description = "Invalid input data")
    @APIResponse(responseCode = "409", description = "Category already exists")
    public Response createCategory(@Valid CategoryRequestDto requestDto) {
        CategoryResponseDto category = categoryService.create(requestDto);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Category created successfully", category))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update category", description = "Update an existing category")
    @APIResponse(responseCode = "200", description = "Category updated successfully")
    @APIResponse(responseCode = "404", description = "Category not found")
    @APIResponse(responseCode = "409", description = "Category name already exists")
    public Response updateCategory(
            @Parameter(description = "Category ID") @PathParam("id") Long id,
            @Valid CategoryUpdateDto updateDto) {
        CategoryResponseDto category = categoryService.update(id, updateDto);
        return Response.ok(ApiResponse.success("Category updated successfully", category)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete category", description = "Delete a category by its ID")
    @APIResponse(responseCode = "200", description = "Category deleted successfully")
    @APIResponse(responseCode = "404", description = "Category not found")
    public Response deleteCategory(@Parameter(description = "Category ID") @PathParam("id") Long id) {
        categoryService.deleteById(id);
        return Response.ok(ApiResponse.success("Category deleted successfully", null)).build();
    }
}