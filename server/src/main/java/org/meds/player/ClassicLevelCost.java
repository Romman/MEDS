package org.meds.player;

import org.springframework.stereotype.Component;

/**
 * Level Cost calculator for versions 2.1+
 * TODO: Use the component with teh active "Classic" profile only
 */
@Component
public class ClassicLevelCost implements LevelCost {

    /**
     * Level number that has no cost to learn a guild lesson
     */
    private static final int FIRST_FREE_LEVELS = 4;

    @Override
    public int getLevelExp(int nextLevel) {
        if (nextLevel < 1) {
            return 0;
        }

        // According to the official formula
        int lvl = nextLevel - 1;
        int exp;
        if (lvl < 200) {
            exp = (int) ((5 + lvl) * (lvl + 1) * (lvl * 0.0025) + 5);
        } else {
            exp = (int) ((5 + lvl) * (lvl + 1) * (1 + (lvl - 299) * 0.25));
        }

        if (lvl > 380) {
            double power = (lvl - 380) * 0.025 + 1;
            exp = (int) Math.pow(exp, power);
        }

        return exp;
    }

    @Override
    public int getLevelGold(int nextLevel) {
        // According to the official formula
        int level = nextLevel - 1;
        if (level <= 4) {
            return 0;
        }

        int gold;
        if (level < 300) {
            gold = (int) ((((5 + level) * (level + 1) + 5) * 10) * (level * 0.0025));
        } else if (level <= 360) {
            gold = (int) ((5 + level) * (level + 1) * (5 + (level - 299) * (level / 85d)));
        } else { // 361-380
            gold = (int) ((5 + level) * (level + 1) * (5 + (level - 299) * 0.5 * 10));
        }

        if (level > 380) {
            double power = (level - 380) * 0.005 + 1;
            gold = (int) Math.pow(gold, power);
        }

        return gold;
    }

    @Override
    public int getTotalGold(int level) {
        // Try to figure out a formula. Not a loop statement.
        int gold = 0;
        for (int i = FIRST_FREE_LEVELS + 1; i <= level; i++) {
            gold += getLevelGold(i);
        }

        return gold;
    }

    @Override
    public int getTotalGold(int fromLevel, int toLevel) {
        int gold = 0;
        for (int i = fromLevel + 1; i <= toLevel; i++) {
            gold += getLevelGold(i);
        }

        return gold;
    }
}
