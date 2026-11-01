package team.anonyms.athletelink.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Planned Game-owned result of one recorded and scored round. This DTO is part of a contract-only endpoint.")
public record RequestRoundResultDto(
        @Schema(description = "Game-generated round result UUID", example = "66666666-6666-6666-6666-666666666666")
        UUID id,
        @Schema(description = "UUID of the started request", example = "22222222-2222-2222-2222-222222222222")
        UUID requestId,
        @Schema(description = "One-based round number", example = "1", minimum = "1")
        int roundNumber,
        @Schema(description = "Sport name used as its ID in the start event", example = "Футбол")
        String sportId,
        @Schema(description = "Time Game recorded the result", example = "2030-06-15T18:30:00Z")
        Instant recordedAt,
        @Schema(description = "Game-owned request status after this round. COMPLETED means the final round was recorded; Request Service receives that status asynchronously.",
                allowableValues = {"ACTIVE", "COMPLETED"}, example = "ACTIVE")
        String requestStatus,
        @Schema(description = "Per-player round outcome and score change")
        List<PlayerResult> players
) {
    @Schema(description = "One accepted player's result in this round")
    public record PlayerResult(
            @Schema(description = "User UUID", example = "33333333-3333-3333-3333-333333333333")
            UUID userId,
            @Schema(description = "WIN or LOSS for a winners-versus-losers submission", example = "WIN")
            GameOutcome outcome,
            @Schema(description = "Signed rating change computed by Game", example = "12")
            int ratingChange
    ) {}
}
