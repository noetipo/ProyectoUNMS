package unmsm.edu.pe.security.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

@ApplicationScoped
public class SecurityConfig {

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "https://unmsm.edu.pe")
    String issuer;

    private static KeyPair keyPair;

    static {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(2048);
            keyPair = keyGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating RSA key pair", e);
        }
    }

    @Produces
    @Dependent
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    @Produces
    @Dependent
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
}