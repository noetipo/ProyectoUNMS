package unmsm.edu.pe.security.domain.services;

import unmsm.edu.pe.security.application.dto.PasswordChangeDto;
import unmsm.edu.pe.security.application.dto.UserReportItem;
import unmsm.edu.pe.security.application.dto.UserRequestDto;
import unmsm.edu.pe.security.application.dto.UserResponseDto;
import unmsm.edu.pe.security.application.dto.UserUpdateDto;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    List<UserResponseDto> findAll();
    List<UserResponseDto> findAllByStatus(UserStatus status);

    /** Reporte paginado con JOIN a persona + filtros por rol/estado y búsqueda (nombre, DNI, usuario, email). */
    PageResponse<UserReportItem> reporte(String search, List<String> roleCodes, List<UserStatus> statuses, int page, int size);

    UserResponseDto findById(UUID id);
    UserResponseDto findByUsername(String username);
    UserResponseDto create(UserRequestDto requestDto);
    UserResponseDto update(UUID id, UserUpdateDto updateDto);
    void deleteById(UUID id);
    void changePassword(UUID id, PasswordChangeDto passwordChangeDto);
    void updateLastLogin(UUID id);
    long countByStatus(UserStatus status);
}