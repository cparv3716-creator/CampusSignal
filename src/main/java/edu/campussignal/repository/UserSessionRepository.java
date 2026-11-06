package edu.campussignal.repository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.campussignal.entity.UserSession;
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByTokenHash(String tokenHash);
    void deleteByExpiresAtBefore(Instant instant);
}
