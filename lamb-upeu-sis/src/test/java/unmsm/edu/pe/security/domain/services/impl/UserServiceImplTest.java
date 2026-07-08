package unmsm.edu.pe.security.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.security.application.dto.PasswordChangeDto;
import unmsm.edu.pe.security.application.dto.UserRequestDto;
import unmsm.edu.pe.security.application.dto.UserResponseDto;
import unmsm.edu.pe.security.application.dto.UserUpdateDto;
import unmsm.edu.pe.security.application.mapper.UserMapper;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.security.domain.repositories.UserRepository;
import unmsm.edu.pe.security.infrastructure.utils.PasswordEncoder;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserServiceImpl userService;

    private User user;
    private UserResponseDto userResponseDto;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPass");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setStatus(UserStatus.ACTIVE);

        userResponseDto = new UserResponseDto();
        userResponseDto.setId(userId);
        userResponseDto.setUsername("testuser");
        userResponseDto.setEmail("test@example.com");
        userResponseDto.setStatus(UserStatus.ACTIVE);
    }

    // --- findAll() ---

    @Test
    void findAll_returnsMappedUserList() {
        when(userRepository.getAllUsers()).thenReturn(List.of(user));
        when(userMapper.toResponseDtoList(List.of(user))).thenReturn(List.of(userResponseDto));

        List<UserResponseDto> result = userService.findAll();

        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    void findAll_withNoUsers_returnsEmptyList() {
        when(userRepository.getAllUsers()).thenReturn(Collections.emptyList());
        when(userMapper.toResponseDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        assertTrue(userService.findAll().isEmpty());
    }

    // --- findAllByStatus() ---

    @Test
    void findAllByStatus_returnsUsersWithMatchingStatus() {
        when(userRepository.findAllByStatus(UserStatus.ACTIVE)).thenReturn(List.of(user));
        when(userMapper.toResponseDtoList(List.of(user))).thenReturn(List.of(userResponseDto));

        List<UserResponseDto> result = userService.findAllByStatus(UserStatus.ACTIVE);

        assertEquals(1, result.size());
    }

    @Test
    void findAllByStatus_withNoMatches_returnsEmptyList() {
        when(userRepository.findAllByStatus(UserStatus.INACTIVE)).thenReturn(Collections.emptyList());
        when(userMapper.toResponseDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        assertTrue(userService.findAllByStatus(UserStatus.INACTIVE).isEmpty());
    }

    // --- findById() ---

    @Test
    void findById_withExistingId_returnsUser() {
        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

        UserResponseDto result = userService.findById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void findById_withUnknownId_throwsNotFoundException() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.getUserById(unknown)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.findById(unknown));
        assertTrue(ex.getMessage().contains(unknown.toString()));
    }

    // --- findByUsername() ---

    @Test
    void findByUsername_withExistingUsername_returnsUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

        UserResponseDto result = userService.findByUsername("testuser");

        assertEquals("testuser", result.getUsername());
    }

    @Test
    void findByUsername_withUnknownUsername_throwsNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> userService.findByUsername("ghost"));
        assertTrue(ex.getMessage().contains("ghost"));
    }

    // --- create() ---

    @Test
    void create_withUniqueCredentials_encodesPasswordAndSavesUser() {
        UserRequestDto request = new UserRequestDto(
                "newuser", "new@example.com", "pass1234",
                "Jane", "Smith", null, UserStatus.ACTIVE
        );
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("pass1234")).thenReturn("encodedPass");
        when(userRepository.saveUser(user)).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

        UserResponseDto result = userService.create(request);

        assertNotNull(result);
        verify(passwordEncoder).encode("pass1234");
        verify(userRepository).saveUser(user);
    }

    @Test
    void create_withDuplicateUsername_throwsBusinessException() {
        UserRequestDto request = new UserRequestDto(
                "testuser", "other@example.com", "pass1234",
                "Jane", "Smith", null, UserStatus.ACTIVE
        );
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.create(request));
        assertTrue(ex.getMessage().contains("testuser"));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    void create_withDuplicateEmail_throwsBusinessException() {
        UserRequestDto request = new UserRequestDto(
                "newuser", "test@example.com", "pass1234",
                "Jane", "Smith", null, UserStatus.ACTIVE
        );
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.create(request));
        assertTrue(ex.getMessage().contains("test@example.com"));
        verify(userRepository, never()).saveUser(any());
    }

    // --- update() ---

    @Test
    void update_withNewUniqueEmail_updatesUser() {
        UserUpdateDto updateDto = new UserUpdateDto(
                "new@example.com", "Jane", "Smith", null, null, null
        );
        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.saveUser(user)).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

        assertDoesNotThrow(() -> userService.update(userId, updateDto));
        verify(userMapper).updateEntityFromDto(updateDto, user);
        verify(userRepository).saveUser(user);
    }

    @Test
    void update_withSameEmail_skipsUniquenessCheck() {
        UserUpdateDto updateDto = new UserUpdateDto(
                "test@example.com", null, null, null, null, null
        );
        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
        when(userRepository.saveUser(user)).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

        assertDoesNotThrow(() -> userService.update(userId, updateDto));
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void update_withDuplicateEmail_throwsBusinessException() {
        UserUpdateDto updateDto = new UserUpdateDto(
                "taken@example.com", null, null, null, null, null
        );
        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.update(userId, updateDto));
        assertTrue(ex.getMessage().contains("taken@example.com"));
    }

    @Test
    void update_withUnknownId_throwsNotFoundException() {
        UUID unknown = UUID.randomUUID();
        UserUpdateDto updateDto = new UserUpdateDto(null, "Jane", null, null, null, null);
        when(userRepository.getUserById(unknown)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.update(unknown, updateDto));
    }

    // --- deleteById() ---

    @Test
    void deleteById_withExistingId_deletesUser() {
        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.deleteById(userId));
        verify(userRepository).removeUserById(userId);
    }

    @Test
    void deleteById_withUnknownId_throwsNotFoundException() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.getUserById(unknown)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.deleteById(unknown));
        verify(userRepository, never()).removeUserById(any());
    }

    // --- changePassword() ---

    @Test
    void changePassword_withCorrectCurrentPassword_updatesPassword() {
        PasswordChangeDto dto = new PasswordChangeDto("oldPass", "newPass1234");
        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "encodedPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass1234")).thenReturn("newEncoded");

        assertDoesNotThrow(() -> userService.changePassword(userId, dto));
        assertEquals("newEncoded", user.getPassword());
        verify(userRepository).saveUser(user);
    }

    @Test
    void changePassword_withWrongCurrentPassword_throwsBusinessException() {
        PasswordChangeDto dto = new PasswordChangeDto("wrongPass", "newPass1234");
        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "encodedPass")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.changePassword(userId, dto));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    void changePassword_withUnknownId_throwsNotFoundException() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.getUserById(unknown)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.changePassword(unknown, new PasswordChangeDto("a", "b")));
    }

    // --- updateLastLogin() ---

    @Test
    void updateLastLogin_withExistingId_setsLastLogin() {
        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.updateLastLogin(userId));
        assertNotNull(user.getLastLogin());
        verify(userRepository).saveUser(user);
    }

    @Test
    void updateLastLogin_withUnknownId_throwsNotFoundException() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.getUserById(unknown)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.updateLastLogin(unknown));
    }

    // --- countByStatus() ---

    @Test
    void countByStatus_delegatesToRepository() {
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(10L);
        assertEquals(10L, userService.countByStatus(UserStatus.ACTIVE));
    }
}