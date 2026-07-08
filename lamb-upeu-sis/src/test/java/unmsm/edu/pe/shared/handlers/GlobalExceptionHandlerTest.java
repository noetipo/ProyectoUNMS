package unmsm.edu.pe.shared.handlers;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.ApiResponse;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void toResponse_withNotFoundException_returns404WithCorrectBody() {
        NotFoundException ex = new NotFoundException("Category not found with id: 99");

        Response response = handler.toResponse(ex);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        ApiResponse<?> body = (ApiResponse<?>) response.getEntity();
        assertFalse(body.isSuccess());
        assertEquals("Category not found with id: 99", body.getMessage());
        assertEquals("RESOURCE_NOT_FOUND", body.getError());
    }

    @Test
    void toResponse_withBusinessException_returns409WithCorrectBody() {
        BusinessException ex = new BusinessException("Username already exists");

        Response response = handler.toResponse(ex);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        ApiResponse<?> body = (ApiResponse<?>) response.getEntity();
        assertFalse(body.isSuccess());
        assertEquals("Username already exists", body.getMessage());
        assertEquals("BUSINESS_RULE_VIOLATION", body.getError());
    }

    @Test
    void toResponse_withGenericException_returns500WithGenericMessage() {
        RuntimeException ex = new RuntimeException("unexpected NPE");

        Response response = handler.toResponse(ex);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        ApiResponse<?> body = (ApiResponse<?>) response.getEntity();
        assertFalse(body.isSuccess());
        assertEquals("An unexpected error occurred", body.getMessage());
        assertEquals("INTERNAL_SERVER_ERROR", body.getError());
    }

    @Test
    void toResponse_withNullMessage_handlesGracefully() {
        NotFoundException ex = new NotFoundException(null);

        Response response = handler.toResponse(ex);

        assertEquals(404, response.getStatus());
    }
}