package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.ExpedienteBandejaItem;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.services.SecretariaDefensaService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SecretariaDefensaServiceImpl implements SecretariaDefensaService {

    @Inject ProyectoTesisRepository proyectoRepository;

    @Override
    @Transactional
    public PageResponse<ExpedienteBandejaItem> bandeja(String buscar, int page, int size) {
        List<ExpedienteBandejaItem> content = proyectoRepository.bandejaExpedientes(buscar, page, size)
                .stream().map(this::toItem).collect(Collectors.toList());
        long total = proyectoRepository.contarBandejaExpedientes(buscar);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public void recibir(UUID tesisId) {
        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
        if (!Boolean.TRUE.equals(p.getExpedienteSubido())) {
            throw new BusinessException("El estudiante aún no ha enviado la solicitud de aprobación");
        }
        p.setExpedienteRecibido(true);
        p.setFechaRecepcion(LocalDate.now());
        proyectoRepository.save(p);
    }

    private ExpedienteBandejaItem toItem(Object[] r) {
        return ExpedienteBandejaItem.builder()
                .tesisId((UUID) r[0])
                .estudianteApellidos((asStr(r[1]) + " " + asStr(r[2])).trim())
                .estudianteNombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .programaNombre(asStr(r[5]))
                .tituloTesis(asStr(r[6]))
                .fechaSolicitud(toLocalDate(r[7]))
                .recibido(Boolean.TRUE.equals(r[8]))
                .build();
    }

    private LocalDate toLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.sql.Timestamp t) return t.toLocalDateTime().toLocalDate();
        return null;
    }

    private String asStr(Object o) { return o != null ? o.toString() : null; }
}
