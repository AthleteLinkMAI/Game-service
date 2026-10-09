package team.anonyms.athletelink.gameservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;

import java.util.List;
import java.util.UUID;

/** Payload sent by Core for one completed round. */
public record SubmitRoundResultDto(
        @NotNull UUID activityRequestId,
        @NotNull @Min(1) Long sportId,
        @JsonAlias("round_number") @Min(1) int roundNumber,
        @NotEmpty List<@NotNull @Valid PlayerResultEntry> players
) {
    public record PlayerResultEntry(
            @NotNull UUID userId,
            @NotNull GameOutcome outcome
    ) {}
}
