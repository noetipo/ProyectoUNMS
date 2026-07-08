package unmsm.edu.pe.security.domain.services;

import unmsm.edu.pe.security.application.dto.*;

public interface AuthService {
    AuthResponseDto login(LoginRequestDto loginRequest);
    AuthResponseDto register(RegisterRequestDto registerRequest);
    TokenResponseDto refreshToken(RefreshTokenRequestDto refreshRequest);
    void logout(String refreshToken);
    void logoutAllDevices(String username);
}