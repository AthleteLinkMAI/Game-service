package team.anonyms.athletelink.gameservice.ranks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void nextRankHasAnExactThreshold() {
        assertEquals(RankTiers.Name.BRONZE, RankTiers.findByScore(1000).name());
        assertEquals(1500, RankTiers.nextRankScore(1000));
        assertEquals(1500, RankTiers.nextRankScore(1499));
        assertEquals(2000, RankTiers.nextRankScore(1500));
        assertNull(RankTiers.nextRankScore(4000));
    }
}
