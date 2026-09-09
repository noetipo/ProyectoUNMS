package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.CorreoNotificacionEnviado;
import unmsm.edu.pe.tesis.domain.repositories.CorreoNotificacionEnviadoRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class CorreoNotificacionEnviadoRepositoryImpl
        implements CorreoNotificacionEnviadoRepository, PanacheRepositoryBase<CorreoNotificacionEnviado, UUID> {

    @Override
    public boolean existe(UUID personaId, String huella) {
        return count("personaId = ?1 and huella = ?2", personaId, huella) > 0;
    }

    @Override
    public void registrar(UUID personaId, String huella) {
        persist(CorreoNotificacionEnviado.builder()
                .personaId(personaId).huella(huella).fechaEnvio(LocalDateTime.now()).build());
    }
}
