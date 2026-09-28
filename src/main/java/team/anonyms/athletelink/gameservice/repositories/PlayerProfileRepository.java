package team.anonyms.athletelink.gameservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;

import java.util.Optional;
import java.util.UUID;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, UUID> {

    Optional<PlayerProfile> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
