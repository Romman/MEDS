package org.meds.data.hibernate.dao;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.meds.data.dao.WorldDAO;
import org.meds.data.domain.*;

import java.util.List;

@SuppressWarnings("unchecked")
public class HibernateWorldDAO extends HibernateDAO implements WorldDAO {

    @Override
    public List<LocaleString> getLocaleStrings() {
        return openSession().createCriteria(LocaleString.class).list();
    }

    @Override
    public LocaleString getLocaleString(int id) {
        return (LocaleString) openSession().get(LocaleString.class, id);
    }

    @Override
    public List<NewMessage> getNewMessages() {
        return openSession().createCriteria(NewMessage.class).list();
    }

    @Override
    public List<Achievement> getAchievements() {
        return openSession().createCriteria(Achievement.class).list();
    }

    @Override
    public Achievement getAchievement(int id) {
        return (Achievement) openSession().get(Achievement.class, id);
    }

    @Override
    public List<AchievementCriterion> getAchievementCriteria() {
        return openSession().createCriteria(AchievementCriterion.class).list();
    }

    @Override
    public List<CreatureTemplate> getCreatureTemplates() {
        return openSession().createCriteria(CreatureTemplate.class).list();
    }

    @Override
    public CreatureTemplate getCreatureTemplate(int id) {
        return (CreatureTemplate) openSession().get(CreatureTemplate.class, id);
    }

    @Override
    public List<CreatureLoot> getCreatureLoot() {
        return openSession().createCriteria(CreatureLoot.class).list();
    }

    @Override
    public List<CreatureLoot> getLootForCreature(int creatureTemplateId) {
        return openSession().createCriteria(CreatureLoot.class)
                .add(Restrictions.eq("creatureTemplateId", creatureTemplateId)).list();
    }

    @Override
    public List<Creature> getCreatures() {
        return openSession().createCriteria(Creature.class).list();
    }

    @Override
    public int getCreatureCountForTemplate(int templateId) {
        return ((Long)openSession().createCriteria(Creature.class)
                .add(Restrictions.eq("templateId", templateId))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();
    }

    @Override
    public List<Currency> getCurrencies() {
        return openSession().createCriteria(Currency.class).list();
    }

    @Override
    public List<Guild> getGuilds() {
        return openSession().createCriteria(Guild.class).list();
    }

    @Override
    public Guild getGuild(int id) {
        return (Guild) openSession().get(Guild.class, id);
    }

    @Override
    public List<GuildLesson> getGuildLessons() {
        return openSession().createCriteria(GuildLesson.class).list();
    }

    @Override
    public List<GuildLesson> getLessonForGuild(int guildId) {
        return openSession().createCriteria(GuildLesson.class)
                .add(Restrictions.eq("guildId", guildId)).list();
    }

    @Override
    public List<ItemTemplate> getItemTemplates() {
        return openSession().createCriteria(ItemTemplate.class).list();
    }

    @Override
    public ItemTemplate getItemTemplate(int id) {
        return (ItemTemplate) openSession().get(ItemTemplate.class, id);
    }

    @Override
    public List<QuestTemplate> getQuestTemplates() {
        return openSession().createCriteria(QuestTemplate.class).list();
    }

    @Override
    public QuestTemplate getQuestTemplate(int id) {
        return (QuestTemplate) openSession().get(QuestTemplate.class, id);
    }

    @Override
    public List<CreatureQuestRelation> getCreatureQuestRelations() {
        return openSession().createCriteria(CreatureQuestRelation.class).list();
    }

    @Override
    public List<CreatureQuestRelation> getQuestRelationsForCreature(int creatureTemplateId) {
        return openSession().createCriteria(CreatureQuestRelation.class)
                .add(Restrictions.eq("creatureTemplateId", creatureTemplateId)).list();
    }

    @Override
    public List<CreatureQuestRelation> getQuestRelationsForQuest(int questTemplateId) {
        return openSession().createCriteria(CreatureQuestRelation.class)
                .add(Restrictions.eq("questTemplateId", questTemplateId)).list();
    }

    @Override
    public List<Skill> getSkills() {
        return openSession().createCriteria(Skill.class).list();
    }

    @Override
    public Skill getSkill(int id) {
        return (Skill) openSession().get(Skill.class, id);
    }

    @Override
    public List<Spell> getSpells() {
        return openSession().createCriteria(Spell.class).list();
    }

    @Override
    public Spell getSpell(int id) {
        return (Spell) openSession().get(Spell.class, id);
    }
}
