package unmsm.edu.pe.shared.utils;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class);

    @Inject
    Mailer mailer;

    @Inject
    ReactiveMailer reactiveMailer;

    @ConfigProperty(name = "quarkus.mailer.from")
    String defaultFrom;

    /**
     * Envía un correo en formato de texto plano (SINCRÓNICO)
     *
     * @param to      Dirección de correo del destinatario
     * @param subject Asunto del correo
     * @param text    Cuerpo del correo en texto plano
     */
    public void sendEmail(String to, String subject, String text) {
        try {
            mailer.send(
                    Mail.withText(to, subject, text)
            );
            LOG.info("✅ Correo enviado con éxito a " + to);
        } catch (Exception e) {
            LOG.error("❌ Error al enviar el correo a " + to + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Envía un correo en formato HTML (SINCRÓNICO)
     *
     * @param to       Dirección de correo del destinatario
     * @param subject  Asunto del correo
     * @param htmlBody Cuerpo del correo en formato HTML
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            mailer.send(
                    Mail.withHtml(to, subject, htmlBody)
            );
            LOG.info("✅ Correo HTML enviado con éxito a " + to);
        } catch (Exception e) {
            LOG.error("❌ Error al enviar el correo HTML a " + to + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    /**
     * Envía un correo en formato HTML (ASINCRÓNICO - Non-blocking)
     *
     * @param to       Dirección de correo del destinatario
     * @param subject  Asunto del correo
     * @param htmlBody Cuerpo del correo en formato HTML
     * @return Uni<Void> para manejo reactivo
     */
    public Uni<Void> sendHtmlEmailAsync(String to, String subject, String htmlBody) {
        return reactiveMailer.send(
                        Mail.withHtml(to, subject, htmlBody)
                ).onItem().invoke(() -> LOG.info("✅ Correo HTML enviado asincrónicamente a " + to))
                .onFailure().invoke(e -> LOG.error("❌ Error al enviar correo async a " + to, e));
    }

    /**
     * Envía un correo de verificación (SINCRÓNICO)
     *
     * @param to      Dirección de correo del destinatario
     * @param subject Asunto del correo
     * @param name    Nombre del destinatario
     * @param link    Enlace de verificación
     */
    public void sendVerificationEmail(String to, String subject, String name, String link) {
        String htmlContent = generateVerificationEmailTemplate(name, subject, link);
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Envía un correo de verificación (ASINCRÓNICO)
     *
     * @param to      Dirección de correo del destinatario
     * @param subject Asunto del correo
     * @param name    Nombre del destinatario
     * @param link    Enlace de verificación
     * @return Uni<Void> para manejo reactivo
     */
    public Uni<Void> sendVerificationEmailAsync(String to, String subject, String name, String link) {
        String htmlContent = generateVerificationEmailTemplate(name, subject, link);
        return sendHtmlEmailAsync(to, subject, htmlContent);
    }

    /**
     * Envía un correo de recuperación de contraseña
     *
     * @param to   Dirección de correo del destinatario
     * @param name Nombre del destinatario
     * @param link Enlace de recuperación
     */
    public void sendPasswordResetEmail(String to, String name, String link) {
        String subject = "Recuperación de Contraseña";
        String htmlContent = generatePasswordResetEmailTemplate(name, link);
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Genera el template HTML para el correo de verificación
     */
    private String generateVerificationEmailTemplate(String name, String subject, String link) {
        return """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>%s</title>
</head>
<body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
    <div style="max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; 
                overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
        <div style="background-color: #4A90E2; color: #ffffff; text-align: center; padding: 20px;">
            <h1>%s</h1>
        </div>
        <div style="padding: 20px; text-align: center;">
            <h2 style="color: #333333;">Hola %s,</h2>
            <p style="color: #555555;">Gracias por registrarte en nuestra plataforma. 
               Por favor, haz clic en el siguiente enlace para verificar tu correo electrónico:</p>
            <!-- ✅ Enlace Azul Funcional -->
            <p style="margin: 20px 0;">
                <a href="%s" style="display: inline-block; background-color: #4A90E2; color: #ffffff; 
                   padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">
                    Verificar Email
                </a>
            </p>
            <p style="color: #555555;">Si el botón no funciona, copia y pega el siguiente enlace en tu navegador:</p>
            <p style="word-break: break-all;">
                <a href="%s" style="color: #4A90E2; text-decoration: underline;">%s</a>
            </p>
            <p style="color: #999999; font-size: 12px; margin-top: 20px;">
                Este enlace expirará en 24 horas.
            </p>
        </div>
        <div style="background-color: #f4f4f4; padding: 15px; text-align: center; font-size: 12px; color: #888888;">
            <p>Si no realizaste esta acción, por favor ignora este correo.</p>
            <p>© 2025 Tu Empresa. Todos los derechos reservados.</p>
        </div>
    </div>
</body>
</html>
""".formatted(subject, subject, name, link, link, link);
    }

    /**
     * Genera el template HTML para el correo de recuperación de contraseña
     */
    private String generatePasswordResetEmailTemplate(String name, String link) {
        return """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Recuperación de Contraseña</title>
</head>
<body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
    <div style="max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; 
                overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
        <div style="background-color: #FF9800; color: #ffffff; text-align: center; padding: 20px;">
            <h1>🔐 Recuperación de Contraseña</h1>
        </div>
        <div style="padding: 20px; text-align: center;">
            <h2 style="color: #333333;">Hola %s,</h2>
            <p style="color: #555555;">
                Recibimos una solicitud para restablecer tu contraseña. 
                Haz clic en el botón de abajo para continuar:
            </p>
            <p style="margin: 20px 0;">
                <a href="%s" style="display: inline-block; background-color: #FF9800; color: #ffffff; 
                   padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">
                    Restablecer Contraseña
                </a>
            </p>
            <p style="color: #555555;">O copia y pega este enlace en tu navegador:</p>
            <p style="word-break: break-all;">
                <a href="%s" style="color: #FF9800; text-decoration: underline;">%s</a>
            </p>
            <div style="background-color: #fff3cd; padding: 15px; border-left: 4px solid #FF9800; 
                        margin: 20px 0; text-align: left;">
                <strong style="color: #856404;">⚠️ Importante:</strong>
                <p style="color: #856404; margin: 5px 0;">
                    Este enlace expirará en 24 horas por seguridad.
                </p>
            </div>
            <p style="color: #999999; font-size: 12px;">
                Si no solicitaste restablecer tu contraseña, puedes ignorar este mensaje. 
                Tu contraseña permanecerá sin cambios.
            </p>
        </div>
        <div style="background-color: #f4f4f4; padding: 15px; text-align: center; font-size: 12px; color: #888888;">
            <p>Por tu seguridad, nunca compartas este enlace con nadie.</p>
            <p>© 2025 Tu Empresa. Todos los derechos reservados.</p>
        </div>
    </div>
</body>
</html>
""".formatted(name, link, link, link);
    }

    /**
     * Envía un correo con adjuntos
     *
     * @param to          Dirección de correo del destinatario
     * @param subject     Asunto del correo
     * @param htmlBody    Cuerpo del correo en formato HTML
     * @param attachments Lista de archivos adjuntos
     */
    public void sendEmailWithAttachments(String to, String subject, String htmlBody,
                                         java.io.File... attachments) {
        try {
            Mail mail = Mail.withHtml(to, subject, htmlBody);

            // Agregar adjuntos
            for (java.io.File file : attachments) {
                mail.addAttachment(file.getName(), file, "application/octet-stream");
            }

            mailer.send(mail);
            LOG.info("✅ Correo con adjuntos enviado a " + to);
        } catch (Exception e) {
            LOG.error("❌ Error al enviar correo con adjuntos a " + to, e);
            throw new RuntimeException("Failed to send email with attachments", e);
        }
    }

    /**
     * Envía un correo a múltiples destinatarios
     *
     * @param recipients Lista de destinatarios
     * @param subject    Asunto del correo
     * @param htmlBody   Cuerpo del correo en HTML
     */
    public void sendBulkEmail(String[] recipients, String subject, String htmlBody) {
        try {
            Mail mail = Mail.withHtml(null, subject, htmlBody)
                    .setBcc(List.of(recipients));

            mailer.send(mail);
            LOG.info("✅ Correo masivo enviado a " + recipients.length + " destinatarios");
        } catch (Exception e) {
            LOG.error("❌ Error al enviar correo masivo", e);
            throw new RuntimeException("Failed to send bulk email", e);
        }
    }
}
