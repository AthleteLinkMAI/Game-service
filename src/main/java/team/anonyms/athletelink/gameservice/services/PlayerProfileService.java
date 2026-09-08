package team.anonyms.athletelink.gameservice.services;

import org.springframework.stereotype.Service;
import team.anonyms.athletelink.gameservice.dto.LeaderboardEntryDto;
import team.anonyms.athletelink.gameservice.dto.PlayerProfileDto;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service class for managing player profiles, including retrieving profiles,
 * ensuring they exist (auto-creation), and computing leaderboard data.
 */
@Service
public class PlayerProfileService {

    private final PlayerProfileRepository playerProfileRepository;

    public PlayerProfileService(PlayerProfileRepository playerProfileRepository) {
        this.playerProfileRepository = playerProfileRepository;
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
                profile.getTotalPoints(),
                profile.getRating(),
                profile.getGamesPlayed(),
                profile.getWins(),
                profile.getLosses(),
                profile.getDraws(),
                profile.getCreatedAt()
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

    /**
     * Gets the leaderboard — top 50 players sorted by rating descending.
     *
     * @return A list of leaderboard entries with ranks.
     */
    public List<LeaderboardEntryDto> getLeaderboard() {
        List<PlayerProfile> topPlayers = playerProfileRepository.findTop50ByOrderByRatingDesc();

        AtomicInteger rank = new AtomicInteger(1);
        return topPlayers.stream()
                .map(p -> new LeaderboardEntryDto(
                        rank.getAndIncrement(),
                        p.getUserId(),
                        p.getRating(),
                        p.getGamesPlayed(),
                        p.getWins()
                ))
                .toList();
    }
}
