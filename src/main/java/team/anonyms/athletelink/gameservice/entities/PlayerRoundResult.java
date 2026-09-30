package team.anonyms.athletelink.gameservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import team.anonyms.athletelink.gameservice.enums.GameOutcome;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "player_round_results")
public class PlayerRoundResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RoundResult roundResult;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerProfile playerProfile;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GameOutcome outcome;

    @NotNull
    @Builder.Default
    private Integer ratingChange = 0;
}
