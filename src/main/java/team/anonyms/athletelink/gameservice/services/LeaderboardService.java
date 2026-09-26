package team.anonyms.athletelink.gameservice.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.gameservice.dto.CursorPageDto;
import team.anonyms.athletelink.gameservice.dto.LeaderboardEntryDto;
import team.anonyms.athletelink.gameservice.dto.RankDto;
import team.anonyms.athletelink.gameservice.exceptions.InvalidCursorException;
import team.anonyms.athletelink.gameservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.gameservice.pagination.CursorService;
import team.anonyms.athletelink.gameservice.pagination.LeaderboardCursor;
import team.anonyms.athletelink.gameservice.ranks.RankTiers;
import team.anonyms.athletelink.gameservice.repositories.LeaderboardRowProjection;
import team.anonyms.athletelink.gameservice.repositories.PlayerSportStatsRepository;
import team.anonyms.athletelink.gameservice.repositories.SportRepository;

import java.util.List;

/**
 * Reads the leaderboard for one sport using cursor pagination.
 */
@Service
public class LeaderboardService {

    private static final String CURSOR_TYPE = "leaderboard";
    private static final int MAX_PAGE_SIZE = 100;

    private final PlayerSportStatsRepository playerSportStatsRepository;
    private final SportRepository sportRepository;
    private final CursorService cursorService;

    public LeaderboardService(
            PlayerSportStatsRepository playerSportStatsRepository,
            SportRepository sportRepository,
            CursorService cursorService
    ) {
        this.playerSportStatsRepository = playerSportStatsRepository;
        this.sportRepository = sportRepository;
        this.cursorService = cursorService;
    }

    /**
     * Returns one page of the leaderboard for a sport.
     *
     * <p>The repository receives {@code limit + 1} rows. The extra row is not
     * returned to the client; it only tells us that another page exists.</p>
     *
     * @param sportId sport whose leaderboard should be read
     * @param cursor opaque cursor from the previous response, or {@code null}
     * @param limit requested page size
     */
    @Transactional(readOnly = true)
    public CursorPageDto<LeaderboardEntryDto> getLeaderboard(
            Long sportId,
            String cursor,
            int limit
    ) {
        validateLimit(limit);
        sportRepository.findById(sportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sport not found: " + sportId));

        LeaderboardCursor decodedCursor = decodeCursor(cursor, sportId);

        List<LeaderboardRowProjection> rows = playerSportStatsRepository.findLeaderboard(
                sportId,
                decodedCursor == null ? null : decodedCursor.score(),
                decodedCursor == null ? null : decodedCursor.playerProfileId(),
                limit + 1
        );

        boolean hasNext = rows.size() > limit;
        List<LeaderboardRowProjection> pageRows = hasNext
                ? rows.subList(0, limit)
                : rows;

        List<LeaderboardEntryDto> items = pageRows.stream()
                .map(this::toDto)
                .toList();

        String nextCursor = hasNext
                ? encodeNextCursor(pageRows.get(pageRows.size() - 1), sportId)
                : null;

        return new CursorPageDto<>(items, nextCursor, hasNext);
    }

    private LeaderboardCursor decodeCursor(String cursor, Long sportId) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        LeaderboardCursor decoded = cursorService.decode(cursor, LeaderboardCursor.class);
        if (!CURSOR_TYPE.equals(decoded.type())
                || !sportId.equals(decoded.sportId())
                || decoded.playerProfileId() == null) {
            throw new InvalidCursorException("Cursor does not belong to the leaderboard");
        }
        return decoded;
    }

    private String encodeNextCursor(LeaderboardRowProjection lastRow, Long sportId) {
        return cursorService.encode(new LeaderboardCursor(
                CURSOR_TYPE,
                sportId,
                lastRow.getScore(),
                lastRow.getPlayerProfileId()
        ));
    }

    private LeaderboardEntryDto toDto(LeaderboardRowProjection row) {
        RankTiers.Tier tier = RankTiers.findByScore(row.getScore());
        RankDto rank = new RankDto(
                tier.name().name(),
                tier.tier(),
                tier.scoreMin(),
                tier.scoreMax()
        );

        return new LeaderboardEntryDto(
                row.getPlace(),
                row.getUserId(),
                row.getScore(),
                row.getWins(),
                row.getLosses(),
                row.getDraws(),
                rank
        );
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
