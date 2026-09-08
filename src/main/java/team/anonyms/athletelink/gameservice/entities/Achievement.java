package team.anonyms.athletelink.gameservice.entities;

import jakarta.persistence.*;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "achievements")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 50, message = "Achievement code cannot exceed 50 characters")
    @NotBlank(message = "Achievement code is required")
    @Column(unique = true)
    private String code;

    @Size(max = 100, message = "Achievement name cannot exceed 100 characters")
    @NotBlank(message = "Achievement name is required")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Size(max = 255, message = "Icon URL cannot exceed 255 characters")
    private String iconUrl;
}
