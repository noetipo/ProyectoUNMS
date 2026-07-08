package unmsm.edu.pe.shared.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void success_withData_setsAllFieldsCorrectly() {
        ApiResponse<String> response = ApiResponse.success("Operation successful", "result data");
        assertTrue(response.isSuccess());
        assertEquals("Operation successful", response.getMessage());
        assertEquals("result data", response.getData());
        assertNull(response.getError());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void success_withoutData_hasNullDataField() {
        ApiResponse<Void> response = ApiResponse.success("Done");
        assertTrue(response.isSuccess());
        assertEquals("Done", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getError());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void error_withErrorCode_setsAllFieldsCorrectly() {
        ApiResponse<Object> response = ApiResponse.error("Something failed", "ERR_001");
        assertFalse(response.isSuccess());
        assertEquals("Something failed", response.getMessage());
        assertEquals("ERR_001", response.getError());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void error_withoutErrorCode_hasNullErrorField() {
        ApiResponse<Object> response = ApiResponse.error("Something failed");
        assertFalse(response.isSuccess());
        assertEquals("Something failed", response.getMessage());
        assertNull(response.getError());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void setData_updatesDataAfterCreation() {
        ApiResponse<String> response = ApiResponse.error("Error", "ERR");
        response.setData("additional info");
        assertEquals("additional info", response.getData());
    }

    @Test
    void success_withNullData_hasNullDataField() {
        ApiResponse<String> response = ApiResponse.success("OK", null);
        assertTrue(response.isSuccess());
        assertNull(response.getData());
    }

    @Test
    void success_timestampIsSetOnCreation() {
        ApiResponse<String> r1 = ApiResponse.success("first");
        ApiResponse<String> r2 = ApiResponse.success("second");
        assertNotNull(r1.getTimestamp());
        assertNotNull(r2.getTimestamp());
    }
}