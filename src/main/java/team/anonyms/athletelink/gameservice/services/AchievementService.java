package team.anonyms.athletelink.gameservice.services;

import org.springframework.stereotype.Service;
import team.anonyms.athletelink.gameservice.dto.AchievementDto;
import team.anonyms.athletelink.gameservice.dto.PlayerAchievementDto;
import team.anonyms.athletelink.gameservice.entities.Achievement;
import team.anonyms.athletelink.gameservice.entities.PlayerAchievement;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.repositories.AchievementRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerAchievementRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service class for managing achievements: retrieving the achievement catalog,
 * checking and granting achievements to players based on their statistics.
 */
@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;

    public AchievementService(
        AchievementRepository achievementRepository,
        PlayerAchievementRepository playerAchievementRepository
    ) {
        this.achievementRepository = achievementRepository;
        this.playerAchievementRepository = playerAchievementRepository;
    }

    /**
     * Gets a list of all available achievements in the system.
     *
     * @return A list of all achievements.
     */
    public List<AchievementDto> getAllAchievements() {
        return achievementRepository.findAll().stream()
                .map(a -> new AchievementDto(
                        a.getId(),
                        a.getCode(),
                        a.getName(),
                        a.getDescription(),
                        a.getIconUrl()
                ))
                .toList();
    }

    /**
     * Gets all achievements earned by a specific player.
     *
     * @param playerProfileId ID of the player profile
     * @return A list of earned achievements with timestamps.
     */
    public List<PlayerAchievementDto> getPlayerAchievements(UUID playerProfileId) {
        return playerAchievementRepository.findAllByPlayerProfileId(playerProfileId).stream()
                .map(pa -> new PlayerAchievementDto(
                        pa.getAchievement().getCode(),
                        pa.getAchievement().getName(),
                        pa.getAchievement().getDescription(),
                        pa.getAchievement().getIconUrl(),
                        pa.getEarnedAt()
                ))
                .toList();
    }

    /**
     * Checks and grants achievements to a player based on their current statistics.
     * This method is called after each game result is processed.
     *
     * Currently supported achievements:
     * - FIRST_WIN: Player has at least 1 win
     * - TEN_GAMES: Player has played at least 10 games
     * - FIFTY_GAMES: Player has played at least 50 games
     * - HUNDRED_GAMES: Player has played at least 100 games
     * - MASTER: Player's rating has reached 1500 or higher
     *
     * Note: FIVE_WINS_STREAK requires match history analysis and is not yet implemented.
     *
     * @param profile The player profile to check achievements for
     */
    public void checkAndGrantAchievements(PlayerProfile profile) {
        tryGrant(profile, "FIRST_WIN", profile.getWins() >= 1);
        tryGrant(profile, "TEN_GAMES", profile.getGamesPlayed() >= 10);
        tryGrant(profile, "FIFTY_GAMES", profile.getGamesPlayed() >= 50);
        tryGrant(profile, "HUNDRED_GAMES", profile.getGamesPlayed() >= 100);
        tryGrant(profile, "MASTER", profile.getRating() >= 1500);
    }

    /**
     * Attempts to grant an achievement to a player if the condition is met
     * and the player hasn't already earned it.
     *
     * @param profile The player profile
     * @param achievementCode The unique code of the achievement
     * @param conditionMet Whether the condition for granting the achievement is met
     */
    private void tryGrant(PlayerProfile profile, String achievementCode, boolean conditionMet) {
        if (!conditionMet) {
            return;
        }

        Achievement achievement = achievementRepository.findByCode(achievementCode)
                .orElse(null);

        if (achievement == null) {
            return;
        }

        boolean alreadyEarned = playerAchievementRepository
                .existsByPlayerProfileIdAndAchievementId(profile.getId(), achievement.getId());

        if (alreadyEarned) {
            return;
        }

        PlayerAchievement playerAchievement = PlayerAchievement.builder()
                .playerProfile(profile)
                .achievement(achievement)
                .build();

        playerAchievementRepository.save(playerAchievement);
    }
}
