package org.meds.database;

import org.meds.data.dao.DAOFactory;
import org.meds.data.dao.WorldDAO;
import org.meds.data.domain.*;
import org.meds.database.repository.*;
import org.meds.logging.Logging;
import org.meds.net.ServerCommands;
import org.meds.net.ServerPacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataStorage {

    @Autowired
    private DAOFactory daoFactory;

    @Autowired
    private AchievementRepository achievementRepository;
    @Autowired
    private CreatureLootRepository creatureLootRepository;
    @Autowired
    private CreatureQuestRelationRepository creatureQuestRelationRepository;
    @Autowired
    private CreatureTemplateRepository creatureTemplateRepository;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private GuildLessonRepository guildLessonRepository;
    @Autowired
    private GuildRepository guildRepository;
    @Autowired
    private ItemTemplateRepository itemTemplateRepository;
    @Autowired
    private NewMessageRepository newMessageRepository;
    @Autowired
    private QuestTemplateRepository questTemplateRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private SpellRepository spellRepository;

    private Map<Integer, ServerPacket> guildLessonsInfo;

    public void loadRepositories() {
        WorldDAO worldDAO = daoFactory.getWorldDAO();

        // Achievements
        List<Achievement> achievements = worldDAO.getAchievements();
        List<AchievementCriterion> criteria = worldDAO.getAchievementCriteria();
        this.achievementRepository.setData(achievements, criteria);
        Logging.Info.log("Loaded " + achievements.size() + " achievements");
        Logging.Info.log("Loaded " + criteria.size() + " achievement criteria");

        // Creature Loot
        List<CreatureLoot> creatureLootItems = worldDAO.getCreatureLoot();
        creatureLootRepository.setData(creatureLootItems, CreatureLoot::getCreatureTemplateId, CreatureLoot::getItemTemplateId);
        Logging.Info.log("Loaded %d creature loot (of %d creature templates)", creatureLootRepository.size(), creatureLootRepository.sizeFirst());

        // Creature Quest Relations
        List<CreatureQuestRelation> relations = worldDAO.getCreatureQuestRelations();
        creatureQuestRelationRepository.setData(relations, CreatureQuestRelation::getCreatureTemplateId, CreatureQuestRelation::getQuestTemplateId);
        Logging.Info.log("Loaded %d creature quest relations (of %d creature templates)",
                creatureQuestRelationRepository.size(), creatureQuestRelationRepository.sizeFirst());

        // Creature Templates
        List<CreatureTemplate> creatureTemplates = worldDAO.getCreatureTemplates();
        creatureTemplateRepository.setData(creatureTemplates, CreatureTemplate::getTemplateId);
        Logging.Info.log("Loaded " + creatureTemplateRepository.size() + " creature templates");

        // Currency
        List<Currency> currencies = worldDAO.getCurrencies();
        currencyRepository.setData(currencies, Currency::getId);
        Logging.Info.log("Loaded " + currencies.size() + " currencies");

        // Guild
        List<Guild> guilds = worldDAO.getGuilds();
        guildRepository.setData(guilds, Guild::getId);
        Logging.Info.log("Loaded %d guilds", guildRepository.size());

        // GuildLesson
        List<GuildLesson> guildLessons = worldDAO.getGuildLessons();
        guildLessonRepository.setData(guildLessons, GuildLesson::getGuildId, GuildLesson::getLevel);
        Logging.Info.log("Loaded %d guild lessons (of %d guilds)",
                guildLessonRepository.size(), guildLessonRepository.sizeFirst());

        // Cache GuildLessonInfo packets
        this.guildLessonsInfo = new HashMap<>(guildLessonRepository.size());

        // ItemTemplate
        List<ItemTemplate> items = worldDAO.getItemTemplates();
        itemTemplateRepository.setData(items, ItemTemplate::getId);
        Logging.Info.log("Loaded " +itemTemplateRepository.size() + " items");

        // New Messages
        List<NewMessage> messages = worldDAO.getNewMessages();
        newMessageRepository.setData(messages, NewMessage::getId);
        Logging.Info.log("Loaded " + newMessageRepository.size() + " new messages");

        // Quest Templates
        List<QuestTemplate> questTemplates = worldDAO.getQuestTemplates();
        questTemplateRepository.setData(questTemplates, QuestTemplate::getId);
        Logging.Info.log("Loaded " + questTemplateRepository.size() + " quests");

        // Skills
        List<Skill> skills = worldDAO.getSkills();
        skillRepository.setData(skills, Skill::getId);
        Logging.Info.log("Loaded " + skillRepository.size() + " skills");

        // Spells
        List<Spell> spells = worldDAO.getSpells();
        spellRepository.setData(spells, Spell::getId);
        Logging.Info.log("Loaded " + spellRepository.size() + " spells");
    }

    // TODO: maybe put this method into a more appropriate class
    public ServerPacket getGuildLessonInfo(int guildId) {
        ServerPacket lessonsData = this.guildLessonsInfo.get(guildId);
        if (lessonsData == null) {
            Guild guild = guildRepository.get(guildId);
            if (guild == null) {
                return null;
            }
            lessonsData = new ServerPacket(ServerCommands.GuildLessonsInfo)
                    .add(guildId)
                    .add(guild.getName());
            List<GuildLesson> guildLessons = new ArrayList<>(guildLessonRepository.get(guildId));
            guildLessons.sort((o1, o2) -> o1.getLevel() - o2.getLevel());
            for (GuildLesson guildLesson : guildLessons) {
                lessonsData.add(guildLesson.getDescription());
            }

            this.guildLessonsInfo.put(guildId, lessonsData);
        }

        return lessonsData;
    }
}
