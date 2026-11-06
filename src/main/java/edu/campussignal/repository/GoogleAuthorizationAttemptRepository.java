package edu.campussignal.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.campussignal.entity.GoogleAuthorizationAttempt;
public interface GoogleAuthorizationAttemptRepository extends JpaRepository<GoogleAuthorizationAttempt, Long> {
    Optional<GoogleAuthorizationAttempt> findByStateHash(String stateHash);
}
