package team.anonyms.athletelink.gameservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "player_game_results")
public class PlayerGameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private GameResult gameResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerProfile playerProfile;

    @NotNull(message = "Game outcome is required")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GameOutcome outcome;

    @NotNull
    @Builder.Default
    private Integer ratingChange = 0;

}
