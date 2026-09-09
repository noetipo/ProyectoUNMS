package unmsm.edu.pe.tesis.domain.repositories;

import java.util.UUID;

public interface CorreoNotificacionEnviadoRepository {
    /** Si ya se envió esta notificación (por huella) a esta persona. */
    boolean existe(UUID personaId, String huella);

    /** Marca la notificación como enviada, para no repetirla en el próximo ciclo. */
    void registrar(UUID personaId, String huella);
}
