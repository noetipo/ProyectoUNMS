package unmsm.edu.pe.tesis.infrastructure.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.shared.utils.EmailService;
import unmsm.edu.pe.tesis.application.dto.NotificacionItem;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.repositories.CorreoNotificacionEnviadoRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.NotificacionService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Envía por correo las notificaciones del alumno, el asesor y el revisor (la campanita del panel
 * de cada uno) apenas aparecen. Como {@code NotificacionServiceImpl} las calcula en lectura a
 * partir del estado de la tesis (no hay tabla ni evento de "esto cambió", ver memoria
 * "notificaciones-derivadas"), este scheduler las recalcula periódicamente para cada persona y usa
 * {@link CorreoNotificacionEnviadoRepository} como el único registro persistente: si la "huella"
 * (id + descripción) de una notificación ya se envió, no se repite; si el texto cambia (avanzó de
 * etapa), se considera nueva.
 */
@ApplicationScoped
public class NotificacionEmailScheduler {

    private static final Logger LOG = Logger.getLogger(NotificacionEmailScheduler.class);

    @Inject TesisRepository tesisRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject PersonaRepository personaRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject NotificacionService notificacionService;
    @Inject CorreoNotificacionEnviadoRepository enviadoRepository;
    @Inject EmailService emailService;

    @ConfigProperty(name = "app.notificaciones.email-habilitado", defaultValue = "false")
    boolean habilitado;

    /**
     * Si está configurado, TODOS los correos se envían aquí en vez de al destinatario real (pruebas).
     * Optional porque SmallRye Config resuelve una propiedad vacía ("") como ausente: un String
     * simple con defaultValue="" falla en el arranque (ver SRCFG00040) apenas alguien la deja en
     * blanco, incluida la del perfil de test.
     */
    @ConfigProperty(name = "app.notificaciones.email-override")
    Optional<String> overrideEmail;

    @ConfigProperty(name = "domain.frontend")
    String frontendUrl;

    @Scheduled(every = "${app.notificaciones.email-intervalo:2m}", delayed = "10s")
    @Transactional
    void enviarCorreosPendientes() {
        if (!habilitado) {
            return;
        }
        int enviados = 0;

        // Alumnos: una persona por tesis activa.
        for (Tesis tesis : tesisRepository.listarTodas()) {
            UUID estudianteId = tesisAutorRepository.estudianteDeTesis(tesis.getId());
            if (estudianteId == null) {
                continue;
            }
            enviados += procesarPersona(estudianteId, notificacionService::notificacionesDeEstudiante,
                    "tesis " + tesis.getId());
        }

        // Docentes: cada uno puede tener notificaciones como asesor y/o como revisor a la vez.
        for (Docente docente : docenteRepository.listarTodosActivos()) {
            enviados += procesarPersona(docente.getPersonaId(), notificacionService::notificacionesDeAsesor,
                    "asesor " + docente.getPersonaId());
            enviados += procesarPersona(docente.getPersonaId(), notificacionService::notificacionesDeRevisor,
                    "revisor " + docente.getPersonaId());
        }

        if (enviados > 0) {
            LOG.info("📧 Correos de notificación enviados en este ciclo: " + enviados);
        }
    }

    private int procesarPersona(UUID personaId, Function<Persona, List<NotificacionItem>> calculo, String contexto) {
        try {
            Persona persona = personaRepository.buscarPorId(personaId).orElse(null);
            if (persona == null) {
                return 0;
            }
            String destino = destinoDe(persona);
            if (destino == null || destino.isBlank()) {
                return 0;
            }
            return enviarNuevas(persona, destino, calculo.apply(persona));
        } catch (Exception e) {
            LOG.error("No se pudo procesar notificaciones por correo de " + contexto, e);
            return 0;
        }
    }

    private int enviarNuevas(Persona persona, String destino, List<NotificacionItem> notificaciones) {
        int enviados = 0;
        for (NotificacionItem n : notificaciones) {
            String huella = n.getId() + "|" + n.getDescription();
            if (enviadoRepository.existe(persona.getId(), huella)) {
                continue;
            }
            try {
                emailService.sendHtmlEmail(destino, n.getTitle(), plantilla(persona, n));
                enviadoRepository.registrar(persona.getId(), huella);
                enviados++;
            } catch (Exception e) {
                LOG.error("No se pudo enviar el correo de notificación '" + n.getId() + "' a " + destino, e);
                // No se registra como enviado: se reintenta en el siguiente ciclo.
            }
        }
        return enviados;
    }

    /** El correo personal si lo tiene, si no el correo del sistema; el override pisa a ambos (solo pruebas). */
    private String destinoDe(Persona persona) {
        if (overrideEmail.filter(s -> !s.isBlank()).isPresent()) {
            return overrideEmail.get();
        }
        if (persona.getEmailPersonal() != null && !persona.getEmailPersonal().isBlank()) {
            return persona.getEmailPersonal();
        }
        return persona.getUser() != null ? persona.getUser().getEmail() : null;
    }

    private String plantilla(Persona persona, NotificacionItem n) {
        String nombre = (nz(persona.getNombres()) + " " + nz(persona.getApellidoPaterno())).trim();
        String url = frontendUrl + (n.getLink() != null ? n.getLink() : "");
        return """
<!DOCTYPE html>
<html lang="es">
<body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
  <div style="max-width: 560px; margin: 20px auto; background: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,.08);">
    <div style="background-color: #8C1D2E; color: #ffffff; padding: 18px 22px;">
      <p style="margin:0; font-size: 11px; text-transform: uppercase; letter-spacing: .04em; opacity:.8;">Proceso de Tesis · UNMSM</p>
      <h1 style="margin: 4px 0 0; font-size: 18px;">%s</h1>
    </div>
    <div style="padding: 22px;">
      <p style="color:#334155; font-size: 14px;">Hola %s,</p>
      <p style="color:#334155; font-size: 14px; line-height: 1.5;">%s</p>
      <p style="margin: 24px 0;">
        <a href="%s" style="display:inline-block; background:#8C1D2E; color:#fff; padding: 11px 22px; border-radius: 6px; text-decoration:none; font-weight:bold; font-size: 13px;">Ver en la plataforma</a>
      </p>
      <p style="color:#94a3b8; font-size: 11.5px;">Este es un aviso automático de tu proceso de titulación; no respondas a este correo.</p>
    </div>
  </div>
</body>
</html>
""".formatted(escapar(n.getTitle()), escapar(nombre.isBlank() ? "doctorando(a)" : nombre), escapar(n.getDescription()), url);
    }

    private String escapar(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String nz(String s) { return s == null ? "" : s; }
}
