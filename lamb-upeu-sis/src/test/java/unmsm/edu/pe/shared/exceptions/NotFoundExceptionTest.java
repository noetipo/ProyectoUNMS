package unmsm.edu.pe.shared.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotFoundExceptionTest {

    @Test
    void constructor_withMessage_setsMessage() {
        NotFoundException ex = new NotFoundException("entity not found");
        assertEquals("entity not found", ex.getMessage());
    }

    @Test
    void constructor_withMessageAndCause_setsBoth() {
        Throwable cause = new RuntimeException("database error");
        NotFoundException ex = new NotFoundException("record missing", cause);
        assertEquals("record missing", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void isInstanceOfRuntimeException() {
        assertInstanceOf(RuntimeException.class, new NotFoundException("test"));
    }

    @Test
    void canBeThrownAndCaught() {
        assertThrows(NotFoundException.class, () -> {
            throw new NotFoundException("not found in test");
        });
    }

    @Test
    void causeIsNullWhenNotProvided() {
        NotFoundException ex = new NotFoundException("no cause");
        assertNull(ex.getCause());
    }
}