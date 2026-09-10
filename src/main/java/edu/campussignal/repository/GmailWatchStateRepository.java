package edu.campussignal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.campussignal.entity.GmailWatchState;

public interface GmailWatchStateRepository extends JpaRepository<GmailWatchState, Integer> {
}
