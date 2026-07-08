package unmsm.edu.pe.security.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.security.application.dto.*;
import unmsm.edu.pe.security.domain.entities.RefreshToken;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.security.domain.repositories.RefreshTokenRepository;
import unmsm.edu.pe.security.domain.repositories.UserRepository;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;
import unmsm.edu.pe.security.domain.services.AuthService;
import unmsm.edu.pe.security.infrastructure.utils.JwtTokenGenerator;
import unmsm.edu.pe.security.infrastructure.utils.JwtTokenValidator;
import unmsm.edu.pe.security.infrastructure.utils.PasswordEncoder;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
@Transactional
public class AuthServiceImpl implements AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    UserRoleAssignmentRepository userRoleRepository;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    PasswordEncoder passwordEncoder;

    @Inject
    JwtTokenGenerator jwtTokenGenerator;

    @Inject
    JwtTokenValidator jwtTokenValidator;

    @Override
    public AuthResponseDto login(LoginRequestDto loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .or(() -> userRepository.findByEmail(loginRequest.getUsername()))
                .orElseThrow(() -> new NotFoundException("Invalid username or password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }

        if (user.getStatus() == UserStatus.INACTIVE || user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException("Account is " + user.getStatus().name().toLowerCase());
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.saveUser(user);

        String accessToken = jwtTokenGenerator.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        List<String> roleCodes = userRoleRepository.findRoleCodesByUserId(user.getId());

        AuthResponseDto.UserInfoDto userInfo = new AuthResponseDto.UserInfoDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                roleCodes,
                user.getStatus(),
                user.getLastLogin()
        );

        return new AuthResponseDto(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenGenerator.getDuration(),
                userInfo
        );
    }

    @Override
    public AuthResponseDto register(RegisterRequestDto registerRequest) {
        // Validar unicidad
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BusinessException("Username already exists");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        // Crear usuario (sin roles; se asignan luego vía user_roles)
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhone(registerRequest.getPhone());
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLogin(LocalDateTime.now());

        User savedUser = userRepository.saveUser(user);

        // Generar tokens
        String accessToken = jwtTokenGenerator.generateAccessToken(savedUser);
        String refreshToken = createRefreshToken(savedUser);

        AuthResponseDto.UserInfoDto userInfo = new AuthResponseDto.UserInfoDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                Collections.emptyList(),
                Collections.emptyList(),
                savedUser.getStatus(),
                savedUser.getLastLogin()
        );

        return new AuthResponseDto(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenGenerator.getDuration(),
                userInfo
        );
    }

    @Override
    public TokenResponseDto refreshToken(RefreshTokenRequestDto refreshRequest) {
        if (!jwtTokenValidator.validateToken(refreshRequest.getRefreshToken())) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        String username = jwtTokenValidator.getUsernameFromToken(refreshRequest.getRefreshToken());
        String userId = jwtTokenValidator.getUserIdFromToken(refreshRequest.getRefreshToken());

        if (username == null || userId == null) {
            throw new BusinessException("Invalid refresh token claims");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshRequest.getRefreshToken())
                .orElseThrow(() -> new BusinessException("Refresh token not found"));

        if (refreshToken.getIsRevoked() || refreshToken.isExpired()) {
            throw new BusinessException("Refresh token has been revoked or expired");
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtTokenGenerator.generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user);

        refreshToken.setIsRevoked(true);
        refreshTokenRepository.saveRefreshToken(refreshToken);

        return new TokenResponseDto(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenGenerator.getDuration()
        );
    }

    @Override
    public void logout(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        refreshToken.setIsRevoked(true);
        refreshTokenRepository.saveRefreshToken(refreshToken);
    }

    @Override
    public void logoutAllDevices(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        refreshTokenRepository.revokeAllByUser(user);
    }

    private String createRefreshToken(User user) {
        String tokenValue = jwtTokenGenerator.generateRefreshToken(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenGenerator.getRefreshDuration()));
        refreshToken.setIsRevoked(false);

        refreshTokenRepository.saveRefreshToken(refreshToken);

        return tokenValue;
    }
}