package team.anonyms.athletelink.gameservice.ranks;

import java.util.List;
import java.util.UUID;

public final class RankTiers {

    public enum Name {
        BRONZE,
        SILVER,
        GOLD,
        DIAMOND,
        MYTHIC,
        LEGEND,
        MASTER
    }

    public record Tier(
            UUID id,
            Name name,
            int tier,
            int scoreMin,
            int scoreMax
    ) {
        public Tier {
            if (tier < 1 || tier > 3) {
                throw new IllegalArgumentException("Rank tier must be between 1 and 3");
            }
            if (scoreMin > scoreMax) {
                throw new IllegalArgumentException("Rank score minimum cannot exceed maximum");
            }
        }
    }

    public static final int INITIAL_SCORE = 0;

    public static final List<Tier> VALUES = List.of(
            new Tier(UUID.fromString("00000000-0000-0000-0000-000000000001"), Name.BRONZE, 1, 0, 999),
            new Tier(UUID.fromString("00000000-0000-0000-0000-000000000002"), Name.SILVER, 1, 1000, 1199),
            new Tier(UUID.fromString("00000000-0000-0000-0000-000000000003"), Name.GOLD, 1, 1200, 1399),
            new Tier(UUID.fromString("00000000-0000-0000-0000-000000000004"), Name.DIAMOND, 1, 1400, 1599),
            new Tier(UUID.fromString("00000000-0000-0000-0000-000000000005"), Name.MYTHIC, 1, 1600, 1799),
            new Tier(UUID.fromString("00000000-0000-0000-0000-000000000006"), Name.LEGEND, 1, 1800, 1999),
            new Tier(UUID.fromString("00000000-0000-0000-0000-000000000007"), Name.MASTER, 1, 2000, Integer.MAX_VALUE)
    );

    private RankTiers() {
    }

    public static Tier findByScore(int score) {
        return VALUES.stream()
                .filter(tier -> score >= tier.scoreMin() && score <= tier.scoreMax())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No rank tier configured for score " + score));
    }
}
