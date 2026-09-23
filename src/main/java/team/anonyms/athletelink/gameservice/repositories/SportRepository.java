package team.anonyms.athletelink.gameservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.gameservice.entities.Sport;

public interface SportRepository extends JpaRepository<Sport, Long> {
}
