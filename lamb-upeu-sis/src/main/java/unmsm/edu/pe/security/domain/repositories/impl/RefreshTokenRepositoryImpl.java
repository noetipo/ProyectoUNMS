package unmsm.edu.pe.security.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.security.domain.entities.RefreshToken;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.repositories.RefreshTokenRepository;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository, PanacheRepositoryBase<RefreshToken, Long> {

    @Override
    public List<RefreshToken> getAllRefreshTokens() {
        return listAll();
    }

    @Override
    public Optional<RefreshToken> getRefreshTokenById(Long id) {
        return findByIdOptional(id);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    @Override
    public List<RefreshToken> findByUser(User user) {
        return list("user", user);
    }

    @Override
    public RefreshToken saveRefreshToken(RefreshToken refreshToken) {
        persist(refreshToken);
        return refreshToken;
    }

    @Override
    public void removeRefreshTokenById(Long id) {
        delete("id", id);
    }

    @Override
    public void revokeAllByUser(User user) {
        update("isRevoked = true WHERE user = ?1 AND isRevoked = false", user);
    }
}