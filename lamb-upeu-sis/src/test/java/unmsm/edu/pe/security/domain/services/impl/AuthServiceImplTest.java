package unmsm.edu.pe.security.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.security.application.dto.AuthResponseDto;
import unmsm.edu.pe.security.application.dto.LoginRequestDto;
import unmsm.edu.pe.security.application.dto.RefreshTokenRequestDto;
import unmsm.edu.pe.security.application.dto.TokenResponseDto;
import unmsm.edu.pe.security.domain.entities.RefreshToken;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.security.domain.repositories.RefreshTokenRepository;
import unmsm.edu.pe.security.domain.repositories.UserRepository;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;
import unmsm.edu.pe.security.infrastructure.utils.JwtTokenGenerator;
import unmsm.edu.pe.security.infrastructure.utils.JwtTokenValidator;
import unmsm.edu.pe.security.infrastructure.utils.PasswordEncoder;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserRoleAssignmentRepository userRoleRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenGenerator jwtTokenGenerator;
    @Mock JwtTokenValidator jwtTokenValidator;

    @InjectMocks
    AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setStatus(UserStatus.ACTIVE);

        lenient().when(jwtTokenGenerator.getDuration()).thenReturn(3600L);
        lenient().when(jwtTokenGenerator.getRefreshDuration()).thenReturn(604800L);
    }

    // =========================================================
    // login()
    // =========================================================

    @Test
    void login_withValidUsernameAndPassword_returnsAuthResponse() {
        LoginRequestDto request = new LoginRequestDto("testuser", "password");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtTokenGenerator.generateAccessToken(user)).thenReturn("access.token");
        when(jwtTokenGenerator.generateRefreshToken(user)).thenReturn("refresh.token");

        AuthResponseDto result = authService.login(request);

        assertNotNull(result);
        assertEquals("access.token", result.getAccessToken());
        assertEquals("refresh.token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getUser());
        assertEquals("testuser", result.getUser().getUsername());
    }

    @Test
    void login_withEmailAsIdentifier_findsUserByEmail() {
        LoginRequestDto request = new LoginRequestDto("test@example.com", "password");
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtTokenGenerator.generateAccessToken(user)).thenReturn("access.token");
        when(jwtTokenGenerator.generateRefreshToken(user)).thenReturn("refresh.token");

        AuthResponseDto result = authService.login(request);

        assertNotNull(result);
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void login_withUnknownUser_throwsNotFoundException() {
        LoginRequestDto request = new LoginRequestDto("nobody", "password");
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nobody")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.login(request));
    }

    @Test
    void login_withWrongPassword_throwsBusinessException() {
        LoginRequestDto request = new LoginRequestDto("testuser", "wrongPass");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "encodedPassword")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.login(request));
    }

    @Test
    void login_withInactiveUser_throwsBusinessException() {
        user.setStatus(UserStatus.INACTIVE);
        LoginRequestDto request = new LoginRequestDto("testuser", "password");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    void login_withSuspendedUser_throwsBusinessException() {
        user.setStatus(UserStatus.SUSPENDED);
        LoginRequestDto request = new LoginRequestDto("testuser", "password");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("suspended"));
    }

    @Test
    void login_updatesLastLoginOnSuccess() {
        LoginRequestDto request = new LoginRequestDto("testuser", "password");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtTokenGenerator.generateAccessToken(user)).thenReturn("token");
        when(jwtTokenGenerator.generateRefreshToken(user)).thenReturn("refresh");

        authService.login(request);

        assertNotNull(user.getLastLogin());
        verify(userRepository).saveUser(user);
    }

    @Test
    void login_savesRefreshTokenOnSuccess() {
        LoginRequestDto request = new LoginRequestDto("testuser", "password");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtTokenGenerator.generateAccessToken(user)).thenReturn("token");
        when(jwtTokenGenerator.generateRefreshToken(user)).thenReturn("refresh");

        authService.login(request);

        verify(refreshTokenRepository).saveRefreshToken(any(RefreshToken.class));
    }

    // =========================================================
    // logout()
    // =========================================================

    @Test
    void logout_withValidToken_revokesIt() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setIsRevoked(false);
        when(refreshTokenRepository.findByToken("validToken")).thenReturn(Optional.of(refreshToken));

        authService.logout("validToken");

        assertTrue(refreshToken.getIsRevoked());
        verify(refreshTokenRepository).saveRefreshToken(refreshToken);
    }

    @Test
    void logout_withUnknownToken_throwsBusinessException() {
        when(refreshTokenRepository.findByToken("badToken")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.logout("badToken"));
    }

    // =========================================================
    // logoutAllDevices()
    // =========================================================

    @Test
    void logoutAllDevices_withExistingUser_revokesAll() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        authService.logoutAllDevices("testuser");

        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    void logoutAllDevices_withUnknownUser_throwsNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.logoutAllDevices("ghost"));
        verify(refreshTokenRepository, never()).revokeAllByUser(any());
    }

    // =========================================================
    // refreshToken()
    // =========================================================

    @Test
    void refreshToken_withInvalidJwt_throwsBusinessException() {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto("bad.token");
        when(jwtTokenValidator.validateToken("bad.token")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_withNullClaims_throwsBusinessException() {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto("token.no.claims");
        when(jwtTokenValidator.validateToken("token.no.claims")).thenReturn(true);
        when(jwtTokenValidator.getUsernameFromToken("token.no.claims")).thenReturn(null);
        when(jwtTokenValidator.getUserIdFromToken("token.no.claims")).thenReturn(null);

        assertThrows(BusinessException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_withRevokedToken_throwsBusinessException() {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto("revoked.token");
        RefreshToken revoked = new RefreshToken();
        revoked.setIsRevoked(true);
        revoked.setExpiresAt(LocalDateTime.now().plusDays(1));
        revoked.setUser(user);

        when(jwtTokenValidator.validateToken("revoked.token")).thenReturn(true);
        when(jwtTokenValidator.getUsernameFromToken("revoked.token")).thenReturn("testuser");
        when(jwtTokenValidator.getUserIdFromToken("revoked.token")).thenReturn(user.getId().toString());
        when(refreshTokenRepository.findByToken("revoked.token")).thenReturn(Optional.of(revoked));

        assertThrows(BusinessException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_withExpiredToken_throwsBusinessException() {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto("expired.token");
        RefreshToken expired = new RefreshToken();
        expired.setIsRevoked(false);
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        expired.setUser(user);

        when(jwtTokenValidator.validateToken("expired.token")).thenReturn(true);
        when(jwtTokenValidator.getUsernameFromToken("expired.token")).thenReturn("testuser");
        when(jwtTokenValidator.getUserIdFromToken("expired.token")).thenReturn(user.getId().toString());
        when(refreshTokenRepository.findByToken("expired.token")).thenReturn(Optional.of(expired));

        assertThrows(BusinessException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_withValidToken_returnsNewAccessAndRefreshTokens() {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto("valid.refresh.token");
        RefreshToken valid = new RefreshToken();
        valid.setIsRevoked(false);
        valid.setExpiresAt(LocalDateTime.now().plusDays(7));
        valid.setUser(user);

        when(jwtTokenValidator.validateToken("valid.refresh.token")).thenReturn(true);
        when(jwtTokenValidator.getUsernameFromToken("valid.refresh.token")).thenReturn("testuser");
        when(jwtTokenValidator.getUserIdFromToken("valid.refresh.token")).thenReturn(user.getId().toString());
        when(refreshTokenRepository.findByToken("valid.refresh.token")).thenReturn(Optional.of(valid));
        when(jwtTokenGenerator.generateAccessToken(user)).thenReturn("new.access.token");
        when(jwtTokenGenerator.generateRefreshToken(user)).thenReturn("new.refresh.token");

        TokenResponseDto result = authService.refreshToken(request);

        assertNotNull(result);
        assertEquals("new.access.token", result.getAccessToken());
        assertEquals("new.refresh.token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertTrue(valid.getIsRevoked(), "Old token should be revoked after refresh");
    }

    @Test
    void refreshToken_withNotFoundToken_throwsBusinessException() {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto("missing.token");
        when(jwtTokenValidator.validateToken("missing.token")).thenReturn(true);
        when(jwtTokenValidator.getUsernameFromToken("missing.token")).thenReturn("testuser");
        when(jwtTokenValidator.getUserIdFromToken("missing.token")).thenReturn(user.getId().toString());
        when(refreshTokenRepository.findByToken("missing.token")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.refreshToken(request));
    }
}