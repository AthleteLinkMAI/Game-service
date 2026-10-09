package team.anonyms.athletelink.gameservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import team.anonyms.athletelink.gameservice.enums.GameResultStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "round_results")
public class RoundResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    private UUID activityRequestId;

    @NotNull
    @Min(1)
    private Integer roundNumber;

    @NotNull
    @Min(1)
    private Long sportId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GameResultStatus status;

    @NotNull
    private UUID recordedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
