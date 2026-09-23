package team.anonyms.athletelink.gameservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerSportStatsRepository extends JpaRepository<PlayerSportStats, UUID> {

    Optional<PlayerSportStats> findByPlayerProfile_IdAndSport_Id(UUID playerProfileId, Long sportId);

    List<PlayerSportStats> findAllByPlayerProfile_IdOrderBySport_Id(UUID playerProfileId);
}
