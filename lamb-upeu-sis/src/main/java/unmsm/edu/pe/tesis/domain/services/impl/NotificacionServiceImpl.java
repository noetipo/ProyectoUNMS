package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.tesis.application.dto.NotificacionItem;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.services.NotificacionService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class NotificacionServiceImpl implements NotificacionService {

    @Inject SecurityUtils securityUtils;
    @Inject ProyectoTesisRepository proyectoRepository;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Transactional
    public List<NotificacionItem> paraUsuarioActual() {
        List<NotificacionItem> out = new ArrayList<>();
        List<String> roles = Arrays.asList(securityUtils.getCurrentUserRoles());

        // Secretaría: expedientes con solicitud enviada aún sin recepcionar.
        if (roles.contains("SECRETARIA") || roles.contains("ADMIN")) {
            for (Object[] r : proyectoRepository.bandejaExpedientes(null, 0, 50)) {
                if (Boolean.TRUE.equals(r[8])) {
                    continue; // ya recibido
                }
                UUID tesisId = (UUID) r[0];
                String estudiante = ((asStr(r[1]) + " " + asStr(r[2])).trim() + ", " + asStr(r[3])).trim();
                out.add(NotificacionItem.builder()
                        .id("exp-" + tesisId)
                        .title("Expediente por recepcionar")
                        .description("Recepciona el expediente de " + estudiante + " y comunícalo al Coordinador.")
                        .link("/admin/secretaria-defensa")
                        .icon("inbox")
                        .fecha(fecha(r[7]))
                        .build());
            }
        }
        return out;
    }

    private String fecha(Object o) {
        LocalDate d = null;
        if (o instanceof LocalDate ld) d = ld;
        else if (o instanceof java.sql.Date sd) d = sd.toLocalDate();
        return d != null ? d.format(FECHA) : null;
    }

    private String asStr(Object o) { return o != null ? o.toString() : null; }
}
