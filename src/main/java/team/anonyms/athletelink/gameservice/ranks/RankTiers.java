package team.anonyms.athletelink.gameservice.ranks;

import java.util.List;

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

    public static final int INITIAL_SCORE = 1000;

    public static final List<Tier> VALUES = List.of(
            new Tier(Name.BRONZE, 1, 1000, 1499),
            new Tier(Name.SILVER, 1, 1500, 1999),
            new Tier(Name.GOLD, 1, 2000, 2499),
            new Tier(Name.DIAMOND, 1, 2500, 2999),
            new Tier(Name.MYTHIC, 1, 3000, 3499),
            new Tier(Name.LEGEND, 1, 3500, 3999),
            new Tier(Name.MASTER, 1, 4000, Integer.MAX_VALUE)
    );

    private RankTiers() {
    }

    public static Tier findByScore(int score) {
        return VALUES.stream()
                .filter(tier -> score >= tier.scoreMin() && score <= tier.scoreMax())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No rank tier configured for score " + score));
    }

    public static Integer nextRankScore(int score) {
        Tier current = findByScore(score);
        int nextIndex = VALUES.indexOf(current) + 1;
        return nextIndex < VALUES.size() ? VALUES.get(nextIndex).scoreMin() : null;
    }
}
