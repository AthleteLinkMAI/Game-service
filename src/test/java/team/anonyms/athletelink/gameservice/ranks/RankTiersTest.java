package team.anonyms.athletelink.gameservice.ranks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankTiersTest {

    @Test
    void scoreBoundariesAreInclusiveAndHaveNoGaps() {
        List<RankTiers.Tier> tiers = RankTiers.VALUES;

        for (int index = 0; index < tiers.size(); index++) {
            RankTiers.Tier tier = tiers.get(index);
            assertEquals(tier, RankTiers.findByScore(tier.scoreMin()));
            assertEquals(tier, RankTiers.findByScore(tier.scoreMax()));

            if (index > 0) {
                RankTiers.Tier previous = tiers.get(index - 1);
                assertEquals((long) previous.scoreMax() + 1L, tier.scoreMin());
            }
        }
    }
}
