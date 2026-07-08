package unmsm.edu.pe.security.domain.repositories;


import unmsm.edu.pe.security.domain.entities.VerificationToken;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository {
    Optional<VerificationToken> findByToken(String token);
    VerificationToken save(VerificationToken token);
    void delete(VerificationToken token);
}