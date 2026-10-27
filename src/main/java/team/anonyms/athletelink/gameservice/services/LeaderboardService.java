package team.anonyms.athletelink.gameservice.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.gameservice.dto.LeaderboardEntryDto;
import team.anonyms.athletelink.gameservice.dto.LeaderboardRow;
import team.anonyms.athletelink.gameservice.ranks.RankTiers;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;

import java.util.List;

/**
 * Reads a numbered page of the leaderboard for one sport.
 */
@Service
public class LeaderboardService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PlayerSportStatsRepository playerSportStatsRepository;

    public LeaderboardService(PlayerSportStatsRepository playerSportStatsRepository) {
        this.playerSportStatsRepository = playerSportStatsRepository;
    }

    /**
     * Returns one page of the leaderboard for a sport.
     *
     * @param sportId sport whose leaderboard should be read
     * @param page one-based page number
     * @param cntPerPage requested page size
     */
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard(
            Long sportId,
            int page,
            int cntPerPage
    ) {
        validatePage(page, cntPerPage);
        if (sportId == null || sportId < 1) {
            throw new IllegalArgumentException("Sport ID must be positive");
        }

        long offset = (long) (page - 1) * cntPerPage;
        return playerSportStatsRepository.findLeaderboard(sportId, cntPerPage, offset).stream()
                .map(this::toDto)
                .toList();
    }

    private LeaderboardEntryDto toDto(LeaderboardRow row) {
        RankTiers.Tier tier = RankTiers.findByScore(row.score());
        LeaderboardEntryDto.Rank rank = new LeaderboardEntryDto.Rank(
                tier.name().name(),
                tier.scoreMin(),
                tier.scoreMax()
        );

        return new LeaderboardEntryDto(
                row.place(),
                row.userId(),
                row.score(),
                row.wins(),
                row.losses(),
                row.draws(),
                rank
        );
    }

    private void validatePage(int page, int cntPerPage) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be at least 1");
        }
        if (cntPerPage < 1 || cntPerPage > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Count per page must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
