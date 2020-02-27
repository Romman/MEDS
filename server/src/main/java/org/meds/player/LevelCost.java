package org.meds.player;

/**
 * Level cost calculator.
 */
public interface LevelCost {

    /**
     * Gets the amount of experience required to reach the next (specified) level.
     *
     * @param nextLevel
     * @return required amount of experience
     */
    int getLevelExp(int nextLevel);

    /**
     * Gets the amount of gold required to learn a guild lesson of the specified level.
     *
     * @param level
     * @return required amount of gold
     */
    int getLevelGold(int level);

    /**
     * Gets the total amount of gold required to learn all of guild lessons up to specified level
     *
     * @param level
     * @return total amount of gold
     */
    int getTotalGold(int level);

    /**
     * Gets the total amount of gold required to get from one "guild" level to another.
     *
     * @param fromLevel the starting level number a player has
     * @param toLevel   the final level of a player
     * @return total amount of gold
     */
    int getTotalGold(int fromLevel, int toLevel);
}
