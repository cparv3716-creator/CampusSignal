package edu.campussignal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.campussignal.entity.Email;

public interface EmailRepository extends JpaRepository<Email, Long> {
    Optional<Email> findByGmailMessageId(String gmailMessageId);
}
