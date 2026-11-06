package edu.campussignal.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.campussignal.entity.UserAccount;
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByGoogleSubject(String googleSubject);
}
