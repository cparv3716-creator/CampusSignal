package edu.campussignal.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.campussignal.entity.GoogleCredential;
public interface GoogleCredentialRepository extends JpaRepository<GoogleCredential, Long> {
    Optional<GoogleCredential> findByUserId(Long userId);
}
