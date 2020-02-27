package org.meds.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.data.dao.DAOFactory;
import org.meds.data.dao.WorldDAO;
import org.meds.data.domain.*;
import org.meds.database.repository.*;
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

    private static Logger logger = LogManager.getLogger();

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
        logger.info("Loaded {} achievements", achievements.size());
        logger.info("Loaded {} achievement criteria", criteria.size());

        // Creature Loot
        List<CreatureLoot> creatureLootItems = worldDAO.getCreatureLoot();
        creatureLootRepository.setData(creatureLootItems, CreatureLoot::getCreatureTemplateId, CreatureLoot::getItemTemplateId);
        logger.info("Loaded {} entries of creature loot (out of {} creature templates)",
                creatureLootRepository.size(), creatureLootRepository.sizeFirst());

        // Creature Quest Relations
        List<CreatureQuestRelation> relations = worldDAO.getCreatureQuestRelations();
        creatureQuestRelationRepository.setData(relations, CreatureQuestRelation::getCreatureTemplateId, CreatureQuestRelation::getQuestTemplateId);
        logger.info("Loaded {} entries of creature quest relations (out of {} creature templates)",
                creatureQuestRelationRepository.size(), creatureQuestRelationRepository.sizeFirst());

        // Creature Templates
        List<CreatureTemplate> creatureTemplates = worldDAO.getCreatureTemplates();
        creatureTemplateRepository.setData(creatureTemplates, CreatureTemplate::getTemplateId);
        logger.info("Loaded {} creature templates", creatureTemplateRepository.size());

        // Currency
        List<Currency> currencies = worldDAO.getCurrencies();
        currencyRepository.setData(currencies, Currency::getId);
        logger.info("Loaded {} currencies", currencies.size());

        // Guild
        List<Guild> guilds = worldDAO.getGuilds();
        guildRepository.setData(guilds, Guild::getId);
        logger.info("Loaded {} guilds", guildRepository.size());

        // GuildLesson
        List<GuildLesson> guildLessons = worldDAO.getGuildLessons();
        guildLessonRepository.setData(guildLessons, GuildLesson::getGuildId, GuildLesson::getLevel);
        logger.info("Loaded {} guild lessons (of {} guilds)",
                guildLessonRepository.size(), guildLessonRepository.sizeFirst());

        // Cache GuildLessonInfo packets
        this.guildLessonsInfo = new HashMap<>(guildLessonRepository.size());

        // ItemTemplate
        List<ItemTemplate> items = worldDAO.getItemTemplates();
        itemTemplateRepository.setData(items, ItemTemplate::getId);
        logger.info("Loaded {} items", itemTemplateRepository.size());

        // New Messages
        List<NewMessage> messages = worldDAO.getNewMessages();
        newMessageRepository.setData(messages, NewMessage::getId);
        logger.info("Loaded {} new messages", newMessageRepository.size());

        // Quest Templates
        List<QuestTemplate> questTemplates = worldDAO.getQuestTemplates();
        questTemplateRepository.setData(questTemplates, QuestTemplate::getId);
        logger.info("Loaded {} quests", questTemplateRepository.size());

        // Skills
        List<Skill> skills = worldDAO.getSkills();
        skillRepository.setData(skills, Skill::getId);
        logger.info("Loaded {} skills", skillRepository.size());

        // Spells
        List<Spell> spells = worldDAO.getSpells();
        spellRepository.setData(spells, Spell::getId);
        logger.info("Loaded {} spells", spellRepository.size());
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
