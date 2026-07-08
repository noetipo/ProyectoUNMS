// src/main/java/upeu/edu/pe/security/infrastructure/utils/PasswordEncoder.java
package unmsm.edu.pe.security.infrastructure.utils;

import jakarta.enterprise.context.ApplicationScoped;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class PasswordEncoder {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    public String encode(String rawPassword) {
        try {
            // Generate salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Hash password with salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(rawPassword.getBytes());

            // Combine salt and hash
            byte[] saltAndHash = new byte[SALT_LENGTH + hashedPassword.length];
            System.arraycopy(salt, 0, saltAndHash, 0, SALT_LENGTH);
            System.arraycopy(hashedPassword, 0, saltAndHash, SALT_LENGTH, hashedPassword.length);

            // Encode to Base64
            return Base64.getEncoder().encodeToString(saltAndHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error encoding password", e);
        }
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        try {
            // Decode the stored password
            byte[] saltAndHash = Base64.getDecoder().decode(encodedPassword);

            // Extract salt
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(saltAndHash, 0, salt, 0, SALT_LENGTH);

            // Extract hash
            byte[] storedHash = new byte[saltAndHash.length - SALT_LENGTH];
            System.arraycopy(saltAndHash, SALT_LENGTH, storedHash, 0, storedHash.length);

            // Hash the raw password with the extracted salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] computedHash = md.digest(rawPassword.getBytes());

            // Compare hashes
            return MessageDigest.isEqual(storedHash, computedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error matching password", e);
        }
    }
}