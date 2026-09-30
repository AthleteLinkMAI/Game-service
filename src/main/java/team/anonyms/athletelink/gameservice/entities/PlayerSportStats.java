package team.anonyms.athletelink.gameservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "player_sport_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_player_sport_stats_profile_sport",
                columnNames = {"player_profile_id", "sport_id"}
        )
)
public class PlayerSportStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerProfile playerProfile;

    @NotNull(message = "Sport ID is required")
    private Long sportId;

    @NotNull
    @Builder.Default
    private Integer wins = 0;

    @NotNull
    @Builder.Default
    private Integer losses = 0;

    @NotNull
    @Builder.Default
    private Integer draws = 0;

    @NotNull
    @Builder.Default
    private Integer score = 0;
}
