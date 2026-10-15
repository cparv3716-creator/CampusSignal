package edu.campussignal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.campussignal.dto.EmailActionStatus;
import edu.campussignal.entity.EmailAction;

public interface EmailActionRepository extends JpaRepository<EmailAction, Long> {
    List<EmailAction> findAllByOrderByCreatedAtDescIdDesc();
    List<EmailAction> findAllByEmailIdOrderByCreatedAtDescIdDesc(long emailId);

    @EntityGraph(attributePaths = "email")
    List<EmailAction> findAllByStatus(EmailActionStatus status);
}
