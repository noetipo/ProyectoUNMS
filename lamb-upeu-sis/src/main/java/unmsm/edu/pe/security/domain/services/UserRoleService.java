package unmsm.edu.pe.security.domain.services;

import unmsm.edu.pe.security.application.dto.RoleDTO;

import java.util.List;
import java.util.UUID;

public interface UserRoleService {

    /** Todos los roles disponibles marcando cuáles están asignados al usuario. */
    List<RoleDTO.Response> findAllRolesSelectedByUserId(UUID userId);

    /** Asigna (reemplaza) el conjunto de roles de un usuario. */
    List<RoleDTO.Response> save(RoleDTO.Request request);
}