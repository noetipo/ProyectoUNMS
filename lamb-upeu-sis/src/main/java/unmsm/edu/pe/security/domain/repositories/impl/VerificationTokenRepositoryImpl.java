package unmsm.edu.pe.security.domain.repositories.impl;


import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.security.domain.entities.VerificationToken;
import unmsm.edu.pe.security.domain.repositories.VerificationTokenRepository;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VerificationTokenRepositoryImpl implements VerificationTokenRepository, PanacheRepositoryBase<VerificationToken, UUID> {

    @Override
    public Optional<VerificationToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    @Override
    public VerificationToken save(VerificationToken token) {
        persist(token);
        return token;
    }

    @Override
    public void delete(VerificationToken token) {
        delete(token);
    }

}
