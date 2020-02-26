package org.meds.data.dao;


import org.meds.data.domain.*;

import java.util.List;

public interface WorldDAO {

    List<LocaleString> getLocaleStrings();

    LocaleString getLocaleString(int id);

    List<NewMessage> getNewMessages();

    List<Achievement> getAchievements();

    Achievement getAchievement(int id);

    List<AchievementCriterion> getAchievementCriteria();

    List<CreatureTemplate> getCreatureTemplates();

    CreatureTemplate getCreatureTemplate(int id);

    List<CreatureLoot> getCreatureLoot();

    List<CreatureLoot> getLootForCreature(int creatureTemplateId);

    List<Creature> getCreatures();

    int getCreatureCountForTemplate(int templateId);

    List<Currency> getCurrencies();

    List<Guild> getGuilds();

    Guild getGuild(int id);

    List<GuildLesson> getGuildLessons();

    List<GuildLesson> getLessonForGuild(int guildId);

    List<ItemTemplate> getItemTemplates();

    ItemTemplate getItemTemplate(int id);

    List<QuestTemplate> getQuestTemplates();

    QuestTemplate getQuestTemplate(int id);

    List<CreatureQuestRelation> getCreatureQuestRelations();

    List<CreatureQuestRelation> getQuestRelationsForCreature(int creatureTemplateId);

    List<CreatureQuestRelation> getQuestRelationsForQuest(int questTemplateId);

    List<Skill> getSkills();

    Skill getSkill(int id);

    List<Spell> getSpells();

    Spell getSpell(int id);
}
