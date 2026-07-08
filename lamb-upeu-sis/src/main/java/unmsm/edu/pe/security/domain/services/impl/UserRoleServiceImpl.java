package unmsm.edu.pe.security.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;
import unmsm.edu.pe.security.application.dto.RoleDTO;
import unmsm.edu.pe.security.domain.entities.Role;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.entities.UserRoleAssignment;
import unmsm.edu.pe.security.domain.repositories.UserRepository;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;
import unmsm.edu.pe.security.domain.services.UserRoleService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserRoleServiceImpl implements UserRoleService {

    private static final Logger LOG = Logger.getLogger(UserRoleServiceImpl.class);

    @Inject
    UserRoleAssignmentRepository userRoleRepository;

    @Inject
    UserRepository userRepository;

    @Override
    public List<RoleDTO.Response> findAllRolesSelectedByUserId(UUID userId) {
        return userRoleRepository.findAllRolesSelectedByUserId(userId).stream()
                .map(this::mapToRoleResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<RoleDTO.Response> save(RoleDTO.Request request) {
        LOG.info("=== Guardando roles para usuario: " + request.getUserId() + " ===");

        User user = userRepository.getUserById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getUserId()));

        // Marcar como eliminadas las asignaciones existentes
        List<UserRoleAssignment> existing = userRoleRepository.findByUser(user);
        LOG.info("Roles existentes a eliminar: " + existing.size());
        existing.forEach(ur -> {
            ur.markAsDeleted();
            ur.setAssigned(false);
            userRoleRepository.remove(ur);
        });

        // Crear nuevas asignaciones
        List<UserRoleAssignment> nuevos = request.getRoleIds().stream()
                .map(roleId -> UserRoleAssignment.builder()
                        .user(user)
                        .role(Role.builder().id(roleId).build())
                        .assigned(true)
                        .build())
                .collect(Collectors.toList());

        LOG.info("Nuevos roles a asignar: " + nuevos.size());
        userRoleRepository.saveAll(nuevos);

        return findAllRolesSelectedByUserId(request.getUserId());
    }

    private RoleDTO.Response mapToRoleResponseDto(Object[] result) {
        RoleDTO.Response response = new RoleDTO.Response();
        response.setId((UUID) result[0]);
        response.setCreatedAt(toLocalDateTime(result[1]));
        response.setDeletedAt(toLocalDateTime(result[2]));
        response.setDescription((String) result[3]);
        response.setName((String) result[4]);
        response.setStatus((Boolean) result[5]);
        response.setUpdatedAt(toLocalDateTime(result[6]));
        response.setCode((String) result[7]);
        response.setSelected((Boolean) result[8]);
        return response;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime()).toLocalDateTime();
        }
        return null;
    }
}