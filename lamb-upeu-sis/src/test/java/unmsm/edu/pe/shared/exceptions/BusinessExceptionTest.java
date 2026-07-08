package unmsm.edu.pe.shared.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructor_withMessage_setsMessage() {
        BusinessException ex = new BusinessException("duplicate username");
        assertEquals("duplicate username", ex.getMessage());
    }

    @Test
    void constructor_withMessageAndCause_setsBoth() {
        Throwable cause = new RuntimeException("root cause");
        BusinessException ex = new BusinessException("wrapped error", cause);
        assertEquals("wrapped error", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void isInstanceOfRuntimeException() {
        assertInstanceOf(RuntimeException.class, new BusinessException("test"));
    }

    @Test
    void canBeThrownAndCaught() {
        assertThrows(BusinessException.class, () -> {
            throw new BusinessException("thrown in test");
        });
    }

    @Test
    void causeIsNullWhenNotProvided() {
        BusinessException ex = new BusinessException("no cause");
        assertNull(ex.getCause());
    }
}