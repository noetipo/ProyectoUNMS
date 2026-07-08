package unmsm.edu.pe.security.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.security.application.dto.PasswordChangeDto;
import unmsm.edu.pe.security.application.dto.UserReportItem;
import unmsm.edu.pe.security.application.dto.UserRequestDto;
import unmsm.edu.pe.security.application.dto.UserResponseDto;
import unmsm.edu.pe.security.application.dto.UserUpdateDto;
import unmsm.edu.pe.security.application.mapper.UserMapper;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.security.domain.repositories.UserRepository;
import unmsm.edu.pe.security.domain.services.UserService;
import unmsm.edu.pe.security.infrastructure.utils.PasswordEncoder;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class UserServiceImpl implements UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    UserMapper userMapper;

    @Inject
    PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponseDto> findAll() {
        List<User> users = userRepository.getAllUsers();
        return userMapper.toResponseDtoList(users);
    }

    @Override
    public List<UserResponseDto> findAllByStatus(UserStatus status) {
        List<User> users = userRepository.findAllByStatus(status);
        return userMapper.toResponseDtoList(users);
    }

    @Override
    public PageResponse<UserReportItem> reporte(String search, List<String> roleCodes,
                                                List<UserStatus> statuses, int page, int size) {
        List<Object[]> rows = userRepository.listarReporte(search, roleCodes, statuses, page, size);
        List<UUID> ids = rows.stream().map(r -> (UUID) r[0]).collect(Collectors.toList());

        Map<UUID, List<String>> rolesByUser = new HashMap<>();
        for (Object[] rr : userRepository.rolesDeUsuarios(ids)) {
            rolesByUser.computeIfAbsent((UUID) rr[0], k -> new ArrayList<>()).add((String) rr[1]);
        }

        List<UserReportItem> content = rows.stream().map(r -> toReportItem(r, rolesByUser))
                .collect(Collectors.toList());
        long total = userRepository.contarReporte(search, roleCodes, statuses);
        return PageResponse.of(content, total, page, size);
    }

    private UserReportItem toReportItem(Object[] r, Map<UUID, List<String>> rolesByUser) {
        UUID id = (UUID) r[0];
        UUID personaId = (UUID) r[7];
        String apPat = (String) r[8];
        String apMat = (String) r[9];
        String nombres = (String) r[10];
        String personaNombre = personaId == null ? null
                : (((apPat != null ? apPat : "") + " " + (apMat != null ? apMat : "")).trim()
                        + ", " + (nombres != null ? nombres : "")).trim();
        return UserReportItem.builder()
                .id(id)
                .username((String) r[1])
                .email((String) r[2])
                .firstName((String) r[3])
                .lastName((String) r[4])
                .status(r[5] != null ? r[5].toString() : null)
                .active(Boolean.TRUE.equals(r[6]))
                .personaId(personaId)
                .personaNombre(personaNombre)
                .numeroDocumento((String) r[11])
                .roles(rolesByUser.getOrDefault(id, List.of()))
                .build();
    }

    @Override
    public UserResponseDto findById( UUID id) {
        User user = userRepository.getUserById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        return userMapper.toResponseDto(user);
    }

    @Override
    public UserResponseDto findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        return userMapper.toResponseDto(user);
    }

    @Override
    public UserResponseDto create(UserRequestDto requestDto) {
        // Validate unique constraints
        if (userRepository.existsByUsername(requestDto.getUsername())) {
            throw new BusinessException("Username '" + requestDto.getUsername() + "' already exists");
        }

        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new BusinessException("Email '" + requestDto.getEmail() + "' already exists");
        }

        User user = userMapper.toEntity(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));

        User savedUser = userRepository.saveUser(user);
        return userMapper.toResponseDto(savedUser);
    }

    @Override
    public UserResponseDto update(UUID id, UserUpdateDto updateDto) {
        User existingUser = userRepository.getUserById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        // Validate email uniqueness if being updated
        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(updateDto.getEmail())) {
                throw new BusinessException("Email '" + updateDto.getEmail() + "' already exists");
            }
        }

        userMapper.updateEntityFromDto(updateDto, existingUser);
        User updatedUser = userRepository.saveUser(existingUser);
        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    public void deleteById(UUID id) {
        if (!userRepository.getUserById(id).isPresent()) {
            throw new NotFoundException("User not found with id: " + id);
        }
        userRepository.removeUserById(id);
    }

    @Override
    public void changePassword(UUID id, PasswordChangeDto passwordChangeDto) {
        User user = userRepository.getUserById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPassword()));
        userRepository.saveUser(user);
    }

    @Override
    public void updateLastLogin(UUID id) {
        User user = userRepository.getUserById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        user.setLastLogin(LocalDateTime.now());
        userRepository.saveUser(user);
    }

    @Override
    public long countByStatus(UserStatus status) {
        return userRepository.countByStatus(status);
    }
}