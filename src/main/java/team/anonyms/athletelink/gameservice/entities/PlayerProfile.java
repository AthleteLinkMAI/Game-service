package team.anonyms.athletelink.gameservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "player_profiles")
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "User ID is required")
    @Column(unique = true)
    private UUID userId;

    @NotNull
    @Builder.Default
    private Integer totalPoints = 0;

    @NotNull
    @Builder.Default
    private Integer rating = 1000;

    @NotNull
    @Builder.Default
    private Integer gamesPlayed = 0;

    @NotNull
    @Builder.Default
    private Integer wins = 0;

    @NotNull
    @Builder.Default
    private Integer losses = 0;

    @NotNull
    @Builder.Default
    private Integer draws = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
