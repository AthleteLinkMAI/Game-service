package team.anonyms.athletelink.gameservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.gameservice.entities.PlayerGameResult;

import java.util.List;
import java.util.UUID;

public interface PlayerGameResultRepository extends JpaRepository<PlayerGameResult, UUID> {

    List<PlayerGameResult> findAllByGameResultId(UUID gameResultId);

    List<PlayerGameResult> findAllByPlayerProfileIdOrderByGameResult_CreatedAtDesc(UUID playerProfileId);
}
