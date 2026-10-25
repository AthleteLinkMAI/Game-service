package team.anonyms.athletelink.gameservice.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.gameservice.dto.BestSportDto;
import team.anonyms.athletelink.gameservice.dto.OtherSportDto;
import team.anonyms.athletelink.gameservice.dto.PlayerSummaryDto;
import team.anonyms.athletelink.gameservice.dto.PlayerSportDetailsDto;
import team.anonyms.athletelink.gameservice.dto.PlayerTotalsRow;
import team.anonyms.athletelink.gameservice.dto.RankDto;
import team.anonyms.athletelink.gameservice.entities.PlayerProfile;
import team.anonyms.athletelink.gameservice.entities.PlayerSportStats;
import team.anonyms.athletelink.gameservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.gameservice.ranks.RankTiers;
import team.anonyms.athletelink.gameservice.repositories.PlayerProfileRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerRoundResultRepository;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PlayerProfileService {

    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerSportStatsRepository playerSportStatsRepository;
    private final PlayerRoundResultRepository playerRoundResultRepository;

    public PlayerProfileService(
            PlayerProfileRepository playerProfileRepository,
            PlayerSportStatsRepository playerSportStatsRepository,
            PlayerRoundResultRepository playerRoundResultRepository
    ) {
        this.playerProfileRepository = playerProfileRepository;
        this.playerSportStatsRepository = playerSportStatsRepository;
        this.playerRoundResultRepository = playerRoundResultRepository;
    }

    @Transactional(readOnly = true)
    public PlayerSummaryDto getSummary(UUID userId) {
        PlayerProfile profile = playerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile not found"));
        PlayerTotalsRow totals = playerSportStatsRepository.findTotalsByProfileId(profile.getId());
        int wins = Math.toIntExact(totals.wins());
        int losses = Math.toIntExact(totals.losses());
        int draws = Math.toIntExact(totals.draws());

        return new PlayerSummaryDto(userId, new PlayerSummaryDto.Summary(
                totals.sportsPlayed(), wins + losses + draws, wins, losses, draws
        ));
    }

    @Transactional(readOnly = true)
    public PlayerSportDetailsDto getSportDetails(UUID userId, Long sportId) {
        PlayerProfile profile = playerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile not found"));
        PlayerSportStats stats = playerSportStatsRepository
                .findByPlayerProfile_IdAndSportId(profile.getId(), sportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Player has no statistics for this sport"));

        long leaderboardPlace = playerSportStatsRepository.countPlayersAhead(sportId, stats.getScore()) + 1;
        RankTiers.Tier tier = RankTiers.findByScore(stats.getScore());
        RankDto rank = new RankDto(tier.name().name(), tier.tier(), tier.scoreMin(), tier.scoreMax());

        return new PlayerSportDetailsDto(
                profile.getUserId(), sportId,
                stats.getWins() + stats.getLosses() + stats.getDraws(),
                stats.getWins(), stats.getLosses(), stats.getDraws(),
                stats.getScore(), leaderboardPlace, rank
        );
    }

    @Transactional(readOnly = true)
    public Optional<BestSportDto> getBestSport(UUID userId) {
        List<PlayerSportStats> sports = getOrderedSportStats(userId);
        if (sports.isEmpty()) {
            return Optional.empty();
        }

        PlayerSportStats best = sports.getFirst();
        int roundsPlayed = best.getWins() + best.getLosses() + best.getDraws();
        double winRatePercent = roundsPlayed == 0
                ? 0.0
                : Math.round(best.getWins() * 1000.0 / roundsPlayed) / 10.0;
        int score = best.getScore();
        RankTiers.Tier tier = RankTiers.findByScore(score);

        return Optional.of(new BestSportDto(
                best.getSportId(), score, RankTiers.nextRankScore(score),
                playerSportStatsRepository.countPlayersAhead(best.getSportId(), score) + 1,
                best.getWins(), winRatePercent,
                new RankDto(tier.name().name(), tier.tier(), tier.scoreMin(), tier.scoreMax())
        ));
    }

    @Transactional(readOnly = true)
    public List<OtherSportDto> getOtherSports(UUID userId) {
        return getOrderedSportStats(userId).stream()
                .skip(1)
                .map(stats -> {
                    int score = stats.getScore();
                    RankTiers.Tier tier = RankTiers.findByScore(score);
                    return new OtherSportDto(
                            stats.getSportId(), score, RankTiers.nextRankScore(score),
                            new RankDto(tier.name().name(), tier.tier(), tier.scoreMin(), tier.scoreMax())
                    );
                })
                .toList();
    }

    private List<PlayerSportStats> getOrderedSportStats(UUID userId) {
        PlayerProfile profile = playerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile not found"));
        return playerSportStatsRepository
                .findAllByPlayerProfile_IdOrderByScoreDescWinsDescSportIdAsc(profile.getId());
    }

    /** Auth CREATE events are safe to process again after a Kafka redelivery. */
    @Transactional
    public void createProfile(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (playerProfileRepository.findByUserId(userId).isEmpty()) {
            playerProfileRepository.save(PlayerProfile.builder().userId(userId).build());
        }
    }

    /** Remove only this user's data; keep shared round records and other players' results. */
    @Transactional
    public void deleteProfile(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        playerProfileRepository.findByUserId(userId).ifPresent(profile -> {
            playerRoundResultRepository.deleteByProfileId(profile.getId());
            playerSportStatsRepository.deleteByProfileId(profile.getId());
            playerProfileRepository.delete(profile);
        });
    }

    /** Core result events must reference a profile previously created from Auth. */
    public PlayerProfile requireProfileForUpdate(UUID userId) {
        return playerProfileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Player profile not found for user: " + userId));
    }

    public PlayerSportStats getOrCreateSportStats(PlayerProfile profile, Long sportId) {
        return playerSportStatsRepository.findByPlayerProfile_IdAndSportId(profile.getId(), sportId)
                .orElseGet(() -> playerSportStatsRepository.save(
                        PlayerSportStats.builder()
                                .playerProfile(profile)
                                .sportId(sportId)
                                .score(RankTiers.INITIAL_SCORE)
                                .build()
                ));
    }

    public PlayerSportStats saveSportStats(PlayerSportStats stats) {
        return playerSportStatsRepository.save(stats);
    }

    @Transactional(readOnly = true)
    public UUID getProfileId(UUID userId) {
        return playerProfileRepository.findByUserId(userId)
                .map(PlayerProfile::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile not found"));
    }
}
