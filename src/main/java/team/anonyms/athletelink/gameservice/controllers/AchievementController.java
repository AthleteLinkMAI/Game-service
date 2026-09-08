package team.anonyms.athletelink.gameservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.gameservice.dto.AchievementDto;
import team.anonyms.athletelink.gameservice.services.AchievementService;

import java.util.List;

@RestController
@RequestMapping("/achievements")
@Tag(name = "Achievements catalog", description = "API for retrieving the list of available achievements. Used for rendering the achievements page on the frontend.")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    @Operation(
            summary = "Get all achievements",
            description = "Returns a list of all available achievements from the database. Used for rendering the achievements catalog on the frontend."
    )
    public ResponseEntity<List<AchievementDto>> getAllAchievements() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }
}
