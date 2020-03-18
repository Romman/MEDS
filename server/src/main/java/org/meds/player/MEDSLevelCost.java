package org.meds.player;

import org.springframework.stereotype.Component;

/**
 * First determined formulas and values to calculate level cost.
 *
 * This calculator used to be appropriated for versions about 2.0.3.x
 */
public class MEDSLevelCost implements LevelCost {

    /**
     * TODO: make a global constant(or even configurable value) but not hardcoded here
     */
    public static final int MAX_LEVEL = 360;
    /**
     * Level number that has no cost to learn a guild lesson
     */
    private static final int FIRST_FREE_LEVELS = 7;
    /**
     * Amount of gold that should be subtracted because of {@code FIRST_FREE_LEVELS}.
     */
    public static final int FIRST_LEVELS_OMITTED_GOLD = 205;

    @Override
    public int getLevelExp(int nextLevel) {
        if (nextLevel < 1 || nextLevel > MAX_LEVEL) {
            return 0;
        }

        int exp = nextLevel * nextLevel + 4 * nextLevel;

        if (nextLevel >= 301) {
            exp *= 1.25 + 0.25 * (nextLevel - 301);
        }

        return exp;
    }

    @Override
    public int getLevelGold(int level) {
        if (level < FIRST_FREE_LEVELS || level > MAX_LEVEL) {
            return 0;
        }

        int gold = level * level + 4 * level + 5;
        if (level >= 301) {
            gold *= 1.5 + 0.5 * (level - 302);
            gold += 25 * (299 - level);
        }

        return gold;
    }

    @Override
    public int getTotalGold(int level) {
        if (level < FIRST_FREE_LEVELS)
            return 0;

        if (level > MAX_LEVEL) {
            level = MAX_LEVEL;
        }

        int firstLevels = level;
        int lastLevels = 0;
        if (firstLevels > 300) {
            firstLevels = 300;
            lastLevels = level - 300;
        }

        int gold = firstLevels * (firstLevels + 1) * (2 * firstLevels + 1) / 6
                + 2 * (firstLevels + 1) * firstLevels + 5 * firstLevels - FIRST_LEVELS_OMITTED_GOLD;

        while (lastLevels != 0) {
            gold += getLevelGold(lastLevels-- + 300);
        }

        return gold;
    }

    @Override
    public int getTotalGold(int fromLevel, int toLevel) {
        if (fromLevel > toLevel) {
            return 0;
        }

        return getTotalGold(toLevel) - getTotalGold(fromLevel - 1);
    }
}
