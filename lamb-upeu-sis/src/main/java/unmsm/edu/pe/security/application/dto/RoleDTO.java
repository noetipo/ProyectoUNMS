package unmsm.edu.pe.security.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class RoleDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private UUID userId;
        private List<UUID> roleIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String name;
        private String code;
        private String description;
        private Boolean status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private Boolean selected;
    }
}