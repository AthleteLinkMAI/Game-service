package team.anonyms.athletelink.gameservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.gameservice.entities.RoundResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundResultRepository extends JpaRepository<RoundResult, UUID> {

    Optional<RoundResult> findByActivityRequestIdAndRoundNumber(UUID activityRequestId, int roundNumber);

    List<RoundResult> findAllByActivityRequestIdOrderByRoundNumberAsc(UUID activityRequestId);
}
