package team.anonyms.athletelink.gameservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.gameservice.dto.RankTierDto;
import team.anonyms.athletelink.gameservice.ranks.RankTiers;

import java.util.List;

@RestController
@RequestMapping("/rank-tiers")
@Tag(name = "Rank tiers")
public class RankTierController {

    @GetMapping
    @Operation(summary = "Get configured rank thresholds")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rank thresholds retrieved successfully")
    })
    public ResponseEntity<List<RankTierDto>> getRankTiers() {
        List<RankTierDto> tiers = RankTiers.VALUES.stream()
                .map(tier -> new RankTierDto(
                        tier.name().name(),
                        tier.tier(),
                        tier.scoreMin(),
                        tier.scoreMax()
                ))
                .toList();

        return ResponseEntity.ok(tiers);
    }
}
