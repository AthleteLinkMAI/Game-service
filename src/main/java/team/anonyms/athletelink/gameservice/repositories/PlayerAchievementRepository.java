package team.anonyms.athletelink.gameservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.gameservice.entities.PlayerAchievement;

import java.util.List;
import java.util.UUID;

public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, UUID> {

    List<PlayerAchievement> findAllByPlayerProfileId(UUID playerProfileId);

    boolean existsByPlayerProfileIdAndAchievementId(UUID playerProfileId, Long achievementId);
}
