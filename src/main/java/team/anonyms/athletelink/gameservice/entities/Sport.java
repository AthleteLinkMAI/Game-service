package team.anonyms.athletelink.gameservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "sports")
public class Sport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Sport name is required")
    @Size(max = 100, message = "Sport name must be at most 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Integer minPlayers = 2;

    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Integer maxPlayers = 32;

    @Size(max = 255, message = "Icon URL must be at most 255 characters")
    @Column(length = 255)
    private String iconUrl;
}
