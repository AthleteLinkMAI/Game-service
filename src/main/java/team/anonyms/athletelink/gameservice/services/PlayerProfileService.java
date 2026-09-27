package team.anonyms.athletelink.gameservice.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.gameservice.dto.PlayerSportDetailsDto;
import team.anonyms.athletelink.gameservice.dto.PlayerSummaryDto;
import team.anonyms.athletelink.gameservice.dto.RankDto;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.entities.Sport;
import team.anonyms.athletelink.gameservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.gameservice.ranks.RankTiers;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerTotalsProjection;
import team.anonyms.athletelink.gameservice.repositories.SportRepository;

import java.util.UUID;

@Service
public class PlayerProfileService {

    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerSportStatsRepository playerSportStatsRepository;
    private final SportRepository sportRepository;

    public PlayerProfileService(
            PlayerProfileRepository playerProfileRepository,
            PlayerSportStatsRepository playerSportStatsRepository,
            SportRepository sportRepository
    ) {
        this.playerProfileRepository = playerProfileRepository;
        this.playerSportStatsRepository = playerSportStatsRepository;
        this.sportRepository = sportRepository;
    }

    @Transactional(readOnly = true)
    public PlayerSummaryDto getSummary(UUID userId) {
        PlayerProfile profile = playerProfileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Player profile not found"));

        PlayerTotalsProjection totals =
                playerSportStatsRepository.findTotalsByProfileId(profile.getId());

        int wins = Math.toIntExact(totals.getWins());
        int losses = Math.toIntExact(totals.getLosses());
        int draws = Math.toIntExact(totals.getDraws());

        int gamesPlayed = wins + losses + draws;

        return new PlayerSummaryDto(
                userId,
                new PlayerSummaryDto.Summary(
                        totals.getSportsPlayed(),
                        gamesPlayed,
                        wins,
                        losses,
                        draws
                )
        );
    }


    @Transactional(readOnly = true)
    public PlayerSportDetailsDto getSportDetails(
            UUID userId,
            Long sportId
    ) {
        PlayerProfile profile =
                playerProfileRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Player profile not found"
                                ));

        Sport sport =
                sportRepository.findById(sportId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Sport not found"
                                ));

        PlayerSportStats stats =
                playerSportStatsRepository
                        .findByPlayerProfile_IdAndSport_Id(
                                profile.getId(),
                                sportId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Player has no statistics for this sport"
                                ));

        int gamesPlayed =
                stats.getWins()
                        + stats.getLosses()
                        + stats.getDraws();

        long leaderboardPlace =
                playerSportStatsRepository.countPlayersAhead(
                        sportId,
                        stats.getScore()
                ) + 1;

        RankTiers.Tier tier =
                RankTiers.findByScore(stats.getScore());

        RankDto rank = new RankDto(
                tier.name().name(),
                tier.tier(),
                tier.scoreMin(),
                tier.scoreMax()
        );

        return new PlayerSportDetailsDto(
                profile.getUserId(),
                sport.getId(),
                sport.getName(),
                gamesPlayed,
                stats.getWins(),
                stats.getLosses(),
                stats.getDraws(),
                stats.getScore(),
                leaderboardPlace,
                rank
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

    // TODO: connect through Kafka
    public PlayerProfile getOrCreateProfile(UUID userId) {
        return playerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PlayerProfile newProfile = PlayerProfile.builder()
                            .userId(userId)
                            .build();
                    return playerProfileRepository.save(newProfile);
        });
    }

    // TODO: connect through Kafka
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

    // TODO: connect through Kafka
    public PlayerSportStats saveSportStats(PlayerSportStats stats) {
        return playerSportStatsRepository.save(stats);
    }

    /**
     * Resolves the external user ID to the local player profile ID.
     * Match-history queries use the local ID stored in player_game_results.
     */
    @Transactional(readOnly = true)
    public UUID getProfileId(UUID userId) {
        return playerProfileRepository.findByUserId(userId)
                .map(PlayerProfile::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Player profile not found"));
    }

}
