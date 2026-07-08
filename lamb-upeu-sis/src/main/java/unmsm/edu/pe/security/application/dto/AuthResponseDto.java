package unmsm.edu.pe.security.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.security.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserInfoDto user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDto {
        private UUID id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private List<String> roles;
        private List<String> roleCodes;
        private UserStatus status;
        private LocalDateTime lastLogin;
    }
}