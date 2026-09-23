package team.anonyms.athletelink.gameservice.services;

import org.springframework.stereotype.Service;
import team.anonyms.athletelink.gameservice.dto.PlayerProfileDto;
import team.anonyms.athletelink.gameservice.dto.PlayerSportStatsDto;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.entities.Sport;
import team.anonyms.athletelink.gameservice.ranks.RankTiers;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;

import java.util.UUID;

@Service
public class PlayerProfileService {

    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerSportStatsRepository playerSportStatsRepository;

    public PlayerProfileService(
            PlayerProfileRepository playerProfileRepository,
            PlayerSportStatsRepository playerSportStatsRepository
    ) {
        this.playerProfileRepository = playerProfileRepository;
        this.playerSportStatsRepository = playerSportStatsRepository;
    }

    /**
     * Gets the player profile for a given user ID.
     *
     * @param userId ID of the user
     * @throws IllegalArgumentException if the player profile is not found.
     */
    public PlayerProfileDto getProfile(UUID userId) {
        PlayerProfile profile = playerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Player profile not found"));

        return new PlayerProfileDto(
                profile.getUserId(),
                playerSportStatsRepository.findAllByPlayerProfile_IdOrderBySport_Id(profile.getId())
                        .stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    /**
     * Retrieves or creates a player profile for the given user ID.
     * Used internally when processing game results — if the player doesn't have
     * a profile yet, it gets automatically created with default values.
     *
     * @param userId ID of the user
     * @return The existing or newly created player profile entity
     */
    public PlayerProfile getOrCreateProfile(UUID userId) {
        return playerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PlayerProfile newProfile = PlayerProfile.builder()
                            .userId(userId)
                            .build();
                    return playerProfileRepository.save(newProfile);
        });
    }

    public PlayerSportStats getOrCreateSportStats(PlayerProfile profile, Sport sport) {
        return playerSportStatsRepository.findByPlayerProfile_IdAndSport_Id(profile.getId(), sport.getId())
                .orElseGet(() -> playerSportStatsRepository.save(
                        PlayerSportStats.builder()
                                .playerProfile(profile)
                                .sport(sport)
                                .score(RankTiers.INITIAL_SCORE)
                                .build()
                ));
    }

    public PlayerSportStats saveSportStats(PlayerSportStats stats) {
        return playerSportStatsRepository.save(stats);
    }

    private PlayerSportStatsDto toDto(PlayerSportStats stats) {
        RankTiers.Tier rank = RankTiers.findByScore(stats.getScore());
        return new PlayerSportStatsDto(
                stats.getSport().getId(),
                stats.getWins(),
                stats.getLosses(),
                stats.getDraws(),
                stats.getScore(),
                new PlayerSportStatsDto.RankDto(
                        rank.id(),
                        rank.name().name(),
                        rank.tier(),
                        rank.scoreMin(),
                        rank.scoreMax()
                )
        );
    }
}
