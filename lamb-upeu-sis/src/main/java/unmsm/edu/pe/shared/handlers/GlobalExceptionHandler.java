// src/main/java/upeu/edu/pe/shared/handlers/GlobalExceptionHandler.java
package unmsm.edu.pe.shared.handlers;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.shared.response.ApiResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof NotFoundException) {
            return handleNotFoundException((NotFoundException) exception);
        } else if (exception instanceof BusinessException) {
            return handleBusinessException((BusinessException) exception);
        } else if (exception instanceof ValidationException) {
            return handleValidationException((ValidationException) exception);
        } else if (exception instanceof ConstraintViolationException) {
            return handleConstraintViolationException((ConstraintViolationException) exception);
        } else if (exception instanceof WebApplicationException) {
            return handleWebApplicationException((WebApplicationException) exception);
        } else {
            return handleGenericException(exception);
        }
    }

    /**
     * Preserva el código HTTP de las excepciones JAX-RS (p. ej. ForbiddenException → 403,
     * NotAuthorizedException → 401) en vez de degradarlas a 500.
     */
    private Response handleWebApplicationException(WebApplicationException ex) {
        int status = ex.getResponse() != null ? ex.getResponse().getStatus() : 500;
        String code;
        switch (status) {
            case 401: code = "UNAUTHORIZED"; break;
            case 403: code = "FORBIDDEN"; break;
            case 404: code = "RESOURCE_NOT_FOUND"; break;
            default:  code = status >= 500 ? "INTERNAL_SERVER_ERROR" : "REQUEST_ERROR";
        }
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), code);
        return Response.status(status).entity(response).build();
    }

    private Response handleNotFoundException(NotFoundException ex) {
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), "RESOURCE_NOT_FOUND");
        return Response.status(Response.Status.NOT_FOUND).entity(response).build();
    }

    private Response handleBusinessException(BusinessException ex) {
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), "BUSINESS_RULE_VIOLATION");
        return Response.status(Response.Status.CONFLICT).entity(response).build();
    }

    private Response handleValidationException(ValidationException ex) {
        ApiResponse<Map<String, String>> response = ApiResponse.error(ex.getMessage(), "VALIDATION_ERROR");
        response.setData(ex.getErrors());
        return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }

    private Response handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> getPropertyPath(violation),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        ApiResponse<Map<String, String>> response = ApiResponse.error("Validation failed", "VALIDATION_ERROR");
        response.setData(errors);
        return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }

    private Response handleGenericException(Exception ex) {
        ApiResponse<Object> response = ApiResponse.error("An unexpected error occurred", "INTERNAL_SERVER_ERROR");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
    }

    private String getPropertyPath(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        return propertyPath.isEmpty() ? "field" : propertyPath;
    }
}