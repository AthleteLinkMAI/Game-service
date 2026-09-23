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
    @JoinColumn(name = "player_profile_id", nullable = false)
    private PlayerProfile playerProfile;

    @NotNull(message = "Sport is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Integer wins = 0;

    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Integer losses = 0;

    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Integer draws = 0;

    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Integer score = 0;
}
