package unmsm.edu.pe.security.domain.repositories;

import unmsm.edu.pe.security.domain.entities.RefreshToken;
import unmsm.edu.pe.security.domain.entities.User;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository {
    List<RefreshToken> getAllRefreshTokens();
    Optional<RefreshToken> getRefreshTokenById(Long id);
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUser(User user);
    RefreshToken saveRefreshToken(RefreshToken refreshToken);
    void removeRefreshTokenById(Long id);
    void revokeAllByUser(User user);
}
