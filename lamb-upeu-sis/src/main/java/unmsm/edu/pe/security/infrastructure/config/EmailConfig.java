package unmsm.edu.pe.security.infrastructure.config;

import io.quarkus.runtime.configuration.ConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class EmailConfig {

    private static final Logger LOG = Logger.getLogger(EmailConfig.class);

    @ConfigProperty(name = "quarkus.mailer.mock", defaultValue = "false")
    boolean mockMailer;

    @ConfigProperty(name = "quarkus.mailer.host")
    String mailHost;

    @ConfigProperty(name = "quarkus.mailer.from")
    String fromEmail;

    void onStart(@Observes StartupEvent event) {
        LOG.info("📧 Email Configuration:");
        LOG.info("   - Host: " + mailHost);
        LOG.info("   - From: " + fromEmail);
        LOG.info("   - Mock Mode: " + mockMailer);

        if (!mockMailer && (mailHost == null || mailHost.isEmpty())) {
            throw new ConfigurationException("Email host is not configured!");
        }
    }
}