package edu.campussignal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.campussignal.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByEmail(String email);
}
