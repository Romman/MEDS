package org.meds;

import org.meds.data.domain.Achievement;
import org.meds.data.domain.AchievementCriterion;
import org.meds.data.domain.CharacterAchievement;
import org.meds.database.Repository;
import org.meds.enums.AchievementCategories;
import org.meds.enums.AchievementCriterionTypes;
import org.meds.enums.Currencies;
import org.meds.logging.Logging;
import org.meds.net.ServerCommands;
import org.meds.net.ServerPacket;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Scope("prototype")
public class AchievementManager {

    private Repository<Achievement> achievementRepository;

    private final Player player;

    private final java.util.Map<AchievementCategories, HashSet<Achievement>> achievements;

    public AchievementManager(Player player) {
        this.player = player;
        this.achievements = new HashMap<>(AchievementCategories.values().length);

        // Create a collection with non-completed achievements
        for (Achievement achievement : achievementRepository) {
            AchievementCategories category = AchievementCategories.parse(achievement.getCategoryId());
            CharacterAchievement charAchieve = this.player.getAchievement(achievement.getId());
            if (charAchieve != null && charAchieve.isCompleted()) {
                continue;
            }
            HashSet<Achievement> categoryAchievements = this.achievements.get(category);
            if (categoryAchievements == null) {
                categoryAchievements = new HashSet<>(achievementRepository.size());
                this.achievements.put(category, categoryAchievements);
            }
            categoryAchievements.add(achievement);
        }

        this.player.addKillingBlowListener(new Unit.KillingBlowListener() {
            @Override
            public void handleEvent(Unit.DamageEvent e) {

                if (e.getVictim().isPlayer())
                    updateProgress(AchievementCategories.PvP, e.getVictim());
                else
                    updateProgress(AchievementCategories.PvM, e.getVictim());
            }
        });
    }

    private void updateProgress(AchievementCategories category, Unit target) {
        if (this.achievements.get(category).size() == 0)
            return;

        // Check all criteria for all achievements
        Set<Integer> completed = new HashSet<>(AchievementCriterionTypes.values().length);
        Set<Integer> failed = new HashSet<>(AchievementCriterionTypes.values().length);

        Iterator<Achievement> iterator = this.achievements.get(category).iterator();
        while (iterator.hasNext()) {
            Achievement achievement = iterator.next();
            Set<AchievementCriterion> criteria = achievement.getCriteria();
            completed.clear();
            failed.clear();
            for (AchievementCriterion criterion : criteria) {
                // Do not need check
                // If an alternative criterion with the same type
                // is already passed checking successfully
                if (completed.contains(criterion.getCriteriaTypeId()))
                    continue;

                boolean isComplete = false;
                switch (AchievementCriterionTypes.parse(criterion.getCriteriaTypeId())) {
                    case CreatureTemplate:
                        if (target.getUnitType() != UnitTypes.Creature)
                            break;
                        if (((Creature)target).getTemplateId() == criterion.getRequirement()) {
                            isComplete = true;
                        }
                        break;
                    case Kingdom:
                        if (this.player.getPosition().getRegion().getKingdom().getEntry().getId() == criterion.getRequirement()) {
                            isComplete = true;
                        }
                        break;
                    case Region:
                        if (this.player.getPosition().getRegion().getId() == criterion.getRequirement()) {
                            isComplete = true;
                        }
                        break;
                    case SpecialCreature:
                        // TODO: Implement special creatures
                        break;
                    case TargetReligion:
                        // TODO: Implement player and creature religion
                        break;
                    case TargetReligiousStatus:
                        // TODO: Implement players religion and statuses
                        break;
                    case TargetRace:
                        if (target.getRace().getValue() == criterion.getRequirement()) {
                            isComplete = true;
                        }
                        break;
                    case UnderSiege:
                        // TODO: Implement location siege and capturing
                        break;
                    case ClanWar:
                        // TODO: Implement Clan mechanics and its PvP
                        break;
                    case AchievementComplete:
                        // TODO: How to pass achievement ID here???
                        break;
                }

                if (isComplete) {
                    completed.add(criterion.getCriteriaTypeId());
                    failed.remove(criterion.getCriteriaTypeId());
                } else {
                    failed.add(criterion.getCriteriaTypeId());
                }
            }

            // At least one failed criterion
            // The achievement does not meet requirements
            if (failed.size() != 0)
                continue;

            // Update progress counter
            CharacterAchievement charAchieve = this.player.getAchievement(achievement.getId());
            if (charAchieve == null) {
                charAchieve = new CharacterAchievement();
                charAchieve.setCharacterId(this.player.getId());
                charAchieve.setAchievementId(achievement.getId());
                this.player.addAchievement(charAchieve);
            }
            charAchieve.setProgress(charAchieve.getProgress() + 1);

            // Complete Achievement
            if (achievement.getCount() == charAchieve.getProgress()) {
                charAchieve.setCompleteDate((int)(new Date().getTime() / 1000));
                // Add achievement points
                this.player.changeCurrency(Currencies.Achievement, achievement.getPoints());
                sendAchievementComplete(achievement, charAchieve);
                iterator.remove();
                Logging.Info.log(String.format("%s completes the achievement %d (%s)",
                        this.player, achievement.getId(), achievement.getTitle()));
            }
            // Update Achievement
            else {
                sendAchievementUpdate(charAchieve);
            }
        }
    }

    private void sendAchievementUpdate(CharacterAchievement charAchieve) {
        if (this.player.getSession() == null)
            return;
        ServerPacket packet = new ServerPacket(ServerCommands.AchievementUpdate);
        packet.add(charAchieve.getAchievementId());
        packet.add(charAchieve.getProgress());
        this.player.getSession().send(packet);
    }

    private void sendAchievementComplete(Achievement achievement, CharacterAchievement charAchieve) {
        if (this.player.getSession() == null)
            return;
        ServerPacket packet = new ServerPacket(ServerCommands.AchievementList);
        packet.add(1); // Achievement Complete list

        packet.add(achievement.getId());
        packet.add(achievement.getTitle());
        packet.add(achievement.getDescription());
        packet.add(charAchieve.getProgress());
        packet.add(achievement.getCount());
        packet.add(charAchieve.getCompleteDate());
        packet.add(achievement.getCategoryId());
        packet.add(achievement.getPoints());

        this.player.getSession().send(packet);
    }
}
