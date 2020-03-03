package org.meds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.data.dao.DAOFactory;
import org.meds.data.domain.*;
import org.meds.data.domain.Currency;
import org.meds.database.BiRepository;
import org.meds.database.Repository;
import org.meds.enums.*;
import org.meds.item.Item;
import org.meds.item.ItemTitleConstructor;
import org.meds.map.Location;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.server.AchievementsMessage;
import org.meds.net.message.server.AutoSpellMessage;
import org.meds.net.message.server.ChatMessage;
import org.meds.net.message.server.CurrenciesMessage;
import org.meds.net.message.server.CurrencyUpdateMessage;
import org.meds.net.message.server.ExperienceMessage;
import org.meds.net.message.server.GroupCreatedMessage;
import org.meds.net.message.server.GuildInfoMessage;
import org.meds.net.message.server.GuildLevelsMessage;
import org.meds.net.message.server.HealthMessage;
import org.meds.net.message.server.MagicInfoMessage;
import org.meds.net.message.server.NoGoMessage;
import org.meds.net.message.server.NpcQuestsMessage;
import org.meds.net.message.server.PlayerInfoMessage;
import org.meds.net.message.server.ProfessionsMessage;
import org.meds.net.message.server.SkillInfoMessage;
import org.meds.net.message.server._lh0Message;
import org.meds.player.LevelCost;
import org.meds.profession.Profession;
import org.meds.spell.Aura;
import org.meds.util.EnumFlags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
@Scope("prototype")
public class Player extends Unit {

    private class SessionDisconnect implements org.meds.net.Session.DisconnectListener {

        @Override
        public void disconnect(org.meds.net.Session session) {
            // TODO; Implement timer to logout the hanging player
            Player.this.session = null;
        }
    }

    private class KillingBlowHandler implements KillingBlowListener {

        @Override
        public void handleEvent(DamageEvent e) {

            // The victim is a creature
            if (e.getVictim().getUnitType() == UnitTypes.Creature) {

                // Add practise value
                Player.this.info.setPractiseValue(Player.this.info.getPractiseValue() + e.getVictim().getLevel());

                // Reward exp
                if (!Player.this.getSettings().has(PlayerSettings.Asceticism)) {
                    int victimLevel = e.getVictim().getLevel();
                    int killerLevel = Player.this.getLevel();
                    int exp = (victimLevel * victimLevel * victimLevel + 1) / (killerLevel * killerLevel + 1) + 1;

                    // HACK: the limit (or even its existence) is unknown
                    // Cannot get exp more than a half of the next level requirement
                    int nextLevelEpx = levelCost.getLevelExp(killerLevel + 1);
                    if (exp > nextLevelEpx / 2) exp = nextLevelEpx / 2;

                    if (exp > 0) {
                        Player.this.addExp(exp);
                        // You gain experience
                        Player.this.getSession().send(new ChatMessage(1038, Integer.toString(exp)));
                    }
                }
            }
        }
    }

    private static Logger logger = LogManager.getLogger();

    @Autowired
    private DAOFactory daoFactory;
    @Autowired
    private QuestInfoPacketFactory questInfoPacketFactory;
    @Autowired
    private BiRepository<CreatureQuestRelation> creatureQuestRelationRepository;
    @Autowired
    private Repository<QuestTemplate> questTemplateRepository;
    @Autowired
    private Repository<Guild> guildRepository;
    @Autowired
    private BiRepository<GuildLesson> guildLessonRepository;
    @Autowired
    private Repository<Skill> skillRepository;
    @Autowired
    private Repository<Achievement> achievementRepository;
    @Autowired
    private Repository<Currency> currencyRepository;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Inn inn;
    @Autowired
    private Locale locale;
    @Autowired
    private ItemTitleConstructor itemTitleConstructor;
    @Autowired
    private AchievementManager achievementManager;
    @Autowired
    private LevelCost levelCost;

    protected static final int SaveTime = 60000;
    protected static final int SyncTime = 20000;

    protected org.meds.net.Session session;

    private EnumFlags<PlayerSettings> settings;

    private EnumFlags<PlayerStatuses> statuses;

    private int saverTimer;
    private int syncTimer;

    private CharacterInfo info;

    private Map<Integer, Quest> quests;

    private Profession[] professions;

    private int guildLevel;

    private SessionDisconnect disconnector;



    private Group group;

    private Trade trade;

    public Player(int id) {
        super();
        this.id = id;
        this.unitType = UnitTypes.Player;

        this.statuses = new EnumFlags<>();
        this.settings = new EnumFlags<>();

        this.saverTimer = SaveTime;
        this.syncTimer = SyncTime;

        this.disconnector = new SessionDisconnect();
        this.addKillingBlowListener(new KillingBlowHandler());
    }

    @PostConstruct
    private void init() {
        this.inventory.setOwner(this);
        this.inn.setOwner(this);
        this.achievementManager.setPlayer(this);
    }

    @Override
    public int getAutoSpell() {
        Integer autoSpell = this.info.getAutoSpellId();
        if (autoSpell == null)
            return 0;
        return autoSpell;
    }

    @Override
    public int getSkillLevel(int skillId) {
        CharacterSkill skill = this.info.getSkills().get(skillId);
        if (skill == null)
            return 0;
        return skill.getLevel();
    }

    @Override
    public int getSpellLevel(int spellId) {
        CharacterSpell spell = this.info.getSpells().get(spellId);
        if (spell == null)
            return 0;
        return spell.getLevel();
    }

    public void setAutoSpell(int spellId) {
        if (this.getSpellLevel(spellId) == 0) {
            spellId = 0;
        } else {
            this.info.setAutoSpellId(spellId);
        }

        if (this.session != null)
            this.session.send(new AutoSpellMessage(spellId));
    }

    public boolean isRelax() {
        return this.hasAura(1000);
    }

    public Location getHome() {
        return mapManager.getLocation(this.info.getHomeId());
    }

    public void setHome() {
        this.setHome(this.position);
    }

    public void setHome(Location home) {
        if (home == null)
            return;
        if (home.getSpecialLocationType() != SpecialLocationTypes.Star)
            return;

        this.info.setHomeId(home.getId());
        if (this.session != null)
            this.session.send(new ChatMessage(17, home.getRegion().getName()));
    }

    public org.meds.net.Session getSession() {
        return this.session;
    }

    @Override
    public String getName() {
        return this.info.getName();
    }

    @Override
    public int getAvatar() {
        return this.info.getAvatarId();
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public Inn getInn() {
        return this.inn;
    }

    public EnumFlags<PlayerSettings> getSettings() {
        return this.settings;
    }

    public EnumFlags<PlayerStatuses> getStatuses() {
        return this.statuses;
    }

    public CharacterAchievement getAchievement(int achievementId) {
        return this.info.getAchievements().get(achievementId);
    }

    public CharacterAchievement getAchievement(Achievement achievement) {
        return this.getAchievement(achievement.getId());
    }

    public void addAchievement(CharacterAchievement achievement) {
        this.info.getAchievements().put(achievement.getAchievementId(), achievement);
    }

    public Quest getQuest(int questTemplateId) {
        return this.quests.get(questTemplateId);
    }

    public Iterator<Quest> getQuestIterator() {
        return this.quests.values().iterator();
    }

    public AchievementManager getAchievementManager() {
        return this.achievementManager;
    }

    public Profession getProfession(Professions profession) {
        return this.professions[profession.getValue() - 1];
    }

    public int getGuildLevel() {
        return this.guildLevel;
    }

    @Override
    public int getHealth() {
        return this.info.getHealth();
    }

    @Override
    public int getMana() {
        return this.info.getMana();
    }

    @Override
    public void setHealth(int health) {
        super.setHealth(health);
        this.info.setHealth(health);
        if (this.session != null)
            this.session.send(this.getHealthManaData());
    }

    @Override
    public void setMana(int mana) {
        super.setMana(mana);
        this.info.setMana(mana);
        if (this.session != null)
            this.session.send(this.getHealthManaData());
    }

    @Override
    public void setHealthMana(int health, int mana) {
        super.setHealthMana(health, mana);
        this.info.setHealth(health);
        this.info.setMana(mana);
        if (this.session != null)
            this.session.send(this.getHealthManaData());
    }

    @Override
    public int getLevel() {
        return this.info.getLevel();
    }

    @Override
    public int getReligLevel() {
        return this.info.getReligLevel();
    }

    public int getReligExp() {
        return this.info.getReligExp();
    }

    public int getExp() {
        return this.info.getExp();
    }

    private void setExp(int value) {
        this.info.setExp(value);

        int nextLvlExp = this.levelCost.getLevelExp(this.getLevel() + 1);

        // Is Level Up
        if (nextLvlExp <= this.getExp()) {
            this.info.setExp(this.info.getExp() - nextLvlExp);
            this.info.setLevel(this.info.getLevel() + 1);
            // Send messages
            onVisualChanged();
            onDisplayChanged();
            if (this.session != null)
                this.session.send(Arrays.asList(
                        new ChatMessage(497), // You gain a new level
                        new ChatMessage(492), // You can a learn new lesson in a guild
                        this.getLevelData(),
                        this.getParametersData()
                ));
        } else {
            if (this.session != null)
                this.session.send(this.getLevelData(true));
        }

    }

    public void setLevel(int level) {
        if (this.info.getLevel() == level)
            return;
        this.info.setLevel(level);

        onVisualChanged();
        onDisplayChanged();

        if (this.session != null)
            this.session.send(this.getLevelData());
    }

    public String getNotepadNotes() {
        return this.info.getNotepad();
    }

    public void setNotepadNotes(String notes) {
        this.info.setNotepad(notes);
    }

    public ServerMessage getLevelData() {
        return new ExperienceMessage(this.getExp(), this.getReligExp(), this.getLevel(), this.getReligLevel());
    }

    public ServerMessage getLevelData(boolean experienceOnly) {
        return new ExperienceMessage(this.getExp(), this.getReligExp());
    }

    protected void addExp(int value) {
        this.setExp(this.getExp() + value);
    }

    public int getCurrencyAmount(Currency currency) {
        return this.getCurrencyAmount(currency.getId());
    }

    public int getCurrencyAmount(Currencies currency) {
        return this.getCurrencyAmount(currency.getValue());
    }

    public int getCurrencyAmount(int currencyId) {
        CharacterCurrency currency = this.info.getCurrencies().get(currencyId);
        if (currency == null)
            return 0;
        return currency.getAmount();
    }

    public boolean changeCurrency(Currencies currency, int difference) {
        return this.changeCurrency(currency.getValue(), difference);
    }

    public boolean changeCurrency(int currencyId, int difference) {
        CharacterCurrency currency = this.info.getCurrencies().get(currencyId);
        // Create new record
        if (currency == null) {
            currency = new CharacterCurrency(this.id, currencyId);
            this.info.getCurrencies().put(currencyId, currency);
        }

        if (currency.getAmount() + difference < 0)
            return false;

        currency.setAmount(currency.getAmount() + difference);
        this.onCurrencyChanged(currencyId, difference);
        return true;
    }

    /**
     * Converts the specified amount of the gold currency into the bank currency.
     */
    public void depositMoney(int amount) {
        if (amount <= 0)
            return;

        if (amount > getCurrencyAmount(Currencies.Gold.getValue()))
            amount = getCurrencyAmount(Currencies.Gold.getValue());

        this.changeCurrency(Currencies.Gold.getValue(), -amount);
        this.changeCurrency(Currencies.Bank.getValue(), amount);
        // onCurrencyChanged has been called two times inside the previous methods;
    }

    /**
     * Converts the specified amount of the bank currency into gold.
     */
    public void withdrawMoney(int amount) {
        if (amount <= 0)
            return;

        if (amount > getCurrencyAmount(Currencies.Bank.getValue()))
            amount = getCurrencyAmount(Currencies.Bank.getValue());

        this.changeCurrency(Currencies.Bank.getValue(), -amount);
        this.changeCurrency(Currencies.Gold.getValue(), amount);
        // onCurrencyChanged has been called two times inside the previous methods;
    }

    // TODO: Implement BankExchange


    public Group getGroup() {
        return this.group;
    }

    public void createGroup() {
        // Already in group
        if (this.group != null)
            return;

        this.group = new Group(this);
        if (this.session != null) {
            // TODO: Move this to the Group class
            List<ServerMessage> messages = Arrays.asList(
                    new GroupCreatedMessage(true, this.getId()),
                    this.group.getSettingsData(),
                    this.group.getTeamLootData(),
                    new ChatMessage(270), // Group has been created
                    new ChatMessage(this.group.getTeamLootMode().getModeMessage())
            );
            this.session.send(messages);
        }
        // Show 'Group Leader' icon for everyone
        onVisualChanged();
        onDisplayChanged();
    }

    public boolean joinGroup(Player leader) {
        if (leader == null)
            return false;

        Group group = leader.getGroup();
        if (group == null)
            return false;

        if (group.getLeader() != leader)
            return false;

        if (group.join(this)) {
            if (this.session != null) {
                this.group = group;
                // Group Loot message
                this.session.send(new ChatMessage(group.getTeamLootMode().getModeMessage()));
                // "You join the group of {LEADER_NAME}
                this.session.send(new ChatMessage(273, group.getLeader().getName()));
            }
            // Say to everyone that the player changes its Leader's ID
            onVisualChanged();
            return true;
        }

        return false;
    }

    public boolean leaveGroup() {
        if (this.group == null)
            return false;

        if (this.group.leave(this)) {
            this.group = null;
            // Say to everyone that the player changes its Leader's ID
            onVisualChanged();
            return true;
        }

        return false;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    @Override
    public int create() {
        if (!load())
            return 0;

        this.health = this.parameters.value(Parameters.Health);
        this.mana = this.parameters.value(Parameters.Mana);

        return super.create();
    }

    public void logIn(org.meds.net.Session session) {
        if (this.session != null) {
            logger.warn("{} assigns a new session with existing one.", this);
            this.session.removeDisconnectListener(this.disconnector);
        }
        this.session = session;
        this.session.addDisconnectListener(this.disconnector);
        // Reappear to the current location
        this.setPosition(this.position);
        // Adds player into the region he's located as a player container.
        // This provides moving tracking.
        this.position.getRegion().addPlayer(this);
    }

    private boolean load() {

        this.info = daoFactory.getCharacterDAO().getCharacterInfo(this.id);

        // Lazy loading of collections
        this.info.getAchievements().size();
        this.info.getCurrencies().size();
        this.info.getGuilds().size();
        this.info.getInnItems().size();
        this.info.getInventoryItems().size();
        this.info.getQuests().size();
        this.info.getSkills().size();
        this.info.getSpells().size();

        // Guild Level
        for (CharacterGuild guild : this.info.getGuilds().values()) {
            this.guildLevel += guild.getLevel();
        }

        // Accept all the quests
        this.quests = new HashMap<>();
        for (CharacterQuest charQuest : this.info.getQuests().values()) {
            Quest quest = new Quest(this, questTemplateRepository.get(charQuest.getQuestTemplateId()), charQuest);
            quest.accept();
            this.quests.put(quest.getQuestTemplate().getId(), quest);
        }

        // Create profession handlers
        this.professions = new Profession[Professions.values().length];
        for (Professions professions : Professions.values()) {
            CharacterProfession charProf = this.info.getProfessions().get(professions.getValue());
            if (charProf == null) {
                charProf = new CharacterProfession();
                charProf.setCharacterId(this.id);
                charProf.setProfessionId(professions.getValue());
                this.info.getProfessions().put(professions.getValue(), charProf);
            }
            this.professions[professions.getValue() - 1] = Profession.createProfession(professions, charProf, this);
        }


        // Unit fields
        this.race = Races.parse(this.info.getRace());
        this.clanId = this.info.getClanId();
        this.clanMemberStatus = ClanMemberStatuses.parse(this.info.getClanStatus());

        // Parameters
        this.parameters.base().value(Parameters.Constitution, this.info.getBaseCon());
        this.parameters.base().value(Parameters.Strength, this.info.getBaseStr());
        this.parameters.base().value(Parameters.Dexterity, this.info.getBaseDex());
        this.parameters.base().value(Parameters.Intelligence, this.info.getBaseInt());

        this.parameters.guild().value(Parameters.Constitution, this.info.getGuildCon());
        this.parameters.guild().value(Parameters.Strength, this.info.getGuildStr());
        this.parameters.guild().value(Parameters.Dexterity, this.info.getGuildDex());
        this.parameters.guild().value(Parameters.Intelligence, this.info.getGuildInt());
        this.parameters.guild().value(Parameters.Damage, this.info.getGuildDam());
        this.parameters.guild().value(Parameters.Protection, this.info.getGuildAbs());
        this.parameters.guild().value(Parameters.ChanceToHit, this.info.getGuildChth());
        this.parameters.guild().value(Parameters.Armour, this.info.getGuildAc());
        this.parameters.guild().value(Parameters.ChanceToCast, this.info.getGuildChtc());
        this.parameters.guild().value(Parameters.MagicDamage, this.info.getGuildMdam());
        this.parameters.guild().value(Parameters.Health, this.info.getGuildHp());
        this.parameters.guild().value(Parameters.Mana, this.info.getGuildMp());
        this.parameters.guild().value(Parameters.HealthRegeneration, this.info.getGuildHpRegen());
        this.parameters.guild().value(Parameters.ManaRegeneration, this.info.getGuildMpRegen());
        this.parameters.guild().value(Parameters.FireResistance, this.info.getGuildFireResist());
        this.parameters.guild().value(Parameters.FrostResistance, this.info.getGuildFrostResist());
        this.parameters.guild().value(Parameters.LightningResistance, this.info.getGuildShockResist());

        // Location
        this.position = mapManager.getLocation(this.info.getLocationId());

        // Home
        // Directly from info

        // AutoSpell
        // TODO: here or from info

        this.settings = new EnumFlags<>(this.info.getSettings());
        this.statuses = new EnumFlags<>(this.info.getStatuses());

        this.inventory.load(this.info.getInventoryItems());
        this.inn.load(this.info.getInnItems());
        this.achievementManager.load();

        this.health = this.parameters.value(Parameters.Health);
        this.mana = this.parameters.value(Parameters.Mana);

        return true;
    }

    public void save() {
        this.info.setBaseCon(this.parameters.base().value(Parameters.Constitution));
        this.info.setBaseStr(this.parameters.base().value(Parameters.Strength));
        this.info.setBaseDex(this.parameters.base().value(Parameters.Dexterity));
        this.info.setBaseInt(this.parameters.base().value(Parameters.Intelligence));
        this.info.setGuildCon(this.parameters.guild().value(Parameters.Constitution));
        this.info.setGuildStr(this.parameters.guild().value(Parameters.Strength));
        this.info.setGuildDex(this.parameters.guild().value(Parameters.Dexterity));
        this.info.setGuildInt(this.parameters.guild().value(Parameters.Intelligence));
        this.info.setGuildDam(this.parameters.guild().value(Parameters.Damage));
        this.info.setGuildAbs(this.parameters.guild().value(Parameters.Protection));
        this.info.setGuildChth(this.parameters.guild().value(Parameters.ChanceToHit));
        this.info.setGuildAc(this.parameters.guild().value(Parameters.Armour));
        this.info.setGuildChtc(this.parameters.guild().value(Parameters.ChanceToCast));
        this.info.setGuildMdam(this.parameters.guild().value(Parameters.MagicDamage));
        this.info.setGuildHp(this.parameters.guild().value(Parameters.Health));
        this.info.setGuildMp(this.parameters.guild().value(Parameters.Mana));
        this.info.setGuildHpRegen(this.parameters.guild().value(Parameters.HealthRegeneration));
        this.info.setGuildMpRegen(this.parameters.guild().value(Parameters.ManaRegeneration));
        this.info.setGuildFireResist(this.parameters.guild().value(Parameters.FireResistance));
        this.info.setGuildFrostResist(this.parameters.guild().value(Parameters.FrostResistance));
        this.info.setGuildShockResist(this.parameters.guild().value(Parameters.LightningResistance));
        this.info.setLocationId(this.position.getId());
        // Experience and Levels are set
        // Home is set
        // AutoSpell is set
        this.info.setSettings(this.settings.getValue());
        this.info.setStatuses(this.statuses.getValue());

        this.inventory.save();
        this.inn.save();

        daoFactory.getCharacterDAO().update(this.info);
    }

    public ServerMessage getParametersData() {
        return new PlayerInfoMessage(
                this.id,
                this.getName(),
                this.getAvatar(),
                this.race.getValue(),
                this.clanId,
                this.clanMemberStatus.getValue(),
                this.parameters.base().value(Parameters.Intelligence),
                this.parameters.base().value(Parameters.Constitution),
                this.parameters.base().value(Parameters.Strength),
                this.parameters.base().value(Parameters.Dexterity),
                this.parameters.guild().value(Parameters.Constitution),
                this.parameters.guild().value(Parameters.Strength),
                this.parameters.guild().value(Parameters.Dexterity),
                this.parameters.guild().value(Parameters.Intelligence),
                this.parameters.guild().value(Parameters.Damage),
                this.parameters.guild().value(Parameters.Protection),
                this.parameters.guild().value(Parameters.ChanceToHit),
                this.parameters.guild().value(Parameters.Armour),
                this.parameters.guild().value(Parameters.ChanceToCast),
                this.parameters.guild().value(Parameters.MagicDamage),
                this.parameters.guild().value(Parameters.Health),
                this.parameters.guild().value(Parameters.Mana),
                this.parameters.guild().value(Parameters.HealthRegeneration),
                this.parameters.guild().value(Parameters.ManaRegeneration),
                this.parameters.guild().value(Parameters.FireResistance),
                this.parameters.guild().value(Parameters.FrostResistance),
                this.parameters.guild().value(Parameters.LightningResistance),
                this.settings.getValue(),
                this.inventory.getCapacity()
        );
    }

    public ServerMessage getGuildData() {
        List<GuildInfoMessage.Guild> guilds = new ArrayList<>(guildRepository.size());
        for (Guild guild : guildRepository) {
            CharacterGuild characterGuild = this.info.getGuilds().get(guild.getId());
            int level = characterGuild == null ? 0 : characterGuild.getLevel();

            guilds.add(new GuildInfoMessage.Guild(
                    guild.getId(),
                    guild.getName(),
                    guild.getPrevId(),
                    level
            ));
        }
        return new GuildInfoMessage(guilds);
    }

    public ServerMessage getMagicData() {
        List<MagicInfoMessage.SpellInfo> spellInfos = new ArrayList<>(spellRepository.size());
        for (Spell spell : spellRepository) {
            CharacterSpell characterSpell = this.info.getSpells().get(spell.getId());
            int level = characterSpell == null ? 0 : characterSpell.getLevel();
            spellInfos.add(new MagicInfoMessage.SpellInfo(
                    spell.getId(), spell.getType().getValue(), spell.getName(), level
            ));
        }
        return new MagicInfoMessage(spellInfos);
    }

    public ServerMessage getSkillData() {
        List<SkillInfoMessage.SkillInfo> skillInfos = new ArrayList<>(skillRepository.size());
        for (Skill skill : skillRepository) {
            CharacterSkill characterSkill = this.info.getSkills().get(skill.getId());
            int level = characterSkill == null ? 0 : characterSkill.getLevel();

            skillInfos.add(new SkillInfoMessage.SkillInfo(skill.getId(), skill.getName(), level));
        }
        return new SkillInfoMessage(skillInfos);
    }

    public ServerMessage getGuildLevelData() {
        List<GuildLevelsMessage.GuildInfo> infos = new ArrayList<>(this.info.getGuilds().size());
        for (CharacterGuild guild : this.info.getGuilds().values()) {
            Guild guildEntry = guildRepository.get(guild.getGuildId());
            infos.add(new GuildLevelsMessage.GuildInfo(guildEntry.getName(), guild.getLevel()));
        }
        return new GuildLevelsMessage(this.guildLevel, infos);
    }

    public void learnGuildLesson(Guild guild) {
        if (guild == null)
            return;

        int guildId = guild.getId();

        CharacterGuild charGuild = this.info.getGuilds().get(guildId);
        if (charGuild == null) {
            // This is the first lesson in this guild
            // Check the previous(required) guild to be learned;
            if (guild.getPrevId() != 0) {
                CharacterGuild prevCharGuild = this.info.getGuilds().get(guild.getPrevId());
                if (prevCharGuild == null || prevCharGuild.getLevel() != 15)
                    return;
            }

            charGuild = new CharacterGuild(this.id, guildId);
            this.info.getGuilds().put(guildId, charGuild);
        }

        // This guild is already learned
        if (charGuild.getLevel() == 15)
            return;

        // Next guild lesson
        GuildLesson lesson = guildLessonRepository.get(guildId, charGuild.getLevel() + 1);
        if (!this.changeCurrency(Currencies.Gold.getValue(), -levelCost.getLevelGold(this.guildLevel + 1)))
            return;

        this.applyGuildImprovement(lesson.getImprovementType1(), lesson.getId1(), lesson.getCount1());
        this.applyGuildImprovement(lesson.getImprovementType2(), lesson.getId2(), lesson.getCount2());

        charGuild.setLevel(charGuild.getLevel() + 1);
        ++this.guildLevel;

        if (this.session != null) {
            List<ServerMessage> messages = Arrays.asList(
                    new ChatMessage(498),
                    // TODO: Send sound here (Sound 31)
                    getMagicData(),
                    getParametersData(),
                    getGuildLevelData()
            );
            session.send(messages);
        }
    }

    private void applyGuildImprovement(GuildLesson.ImprovementTypes type, int id, int count) {
        switch (type) {
            case Parameter:
                this.parameters.guild().change(Parameters.parse(id), count);
                break;
            case Skill:
                CharacterSkill characterSkill = this.info.getSkills().get(id);
                if (characterSkill == null) {
                    characterSkill = new CharacterSkill(id, 0);
                    this.info.getSkills().put(id, characterSkill);
                }
                characterSkill.setLevel(characterSkill.getLevel() + count);
                logger.debug("{} has learnt a new level({}) of the skill(id={})",
                        this, characterSkill.getLevel(), id);
                break;
            case Spell:
                CharacterSpell characterSpell = this.info.getSpells().get(id);
                if (characterSpell == null) {
                    characterSpell = new CharacterSpell(this.id, id, 0);
                    this.info.getSpells().put(id, characterSpell);
                }
                characterSpell.setLevel(characterSpell.getLevel() + count);
                logger.debug("{} has learnt a new level({}) of the spell(id={})",
                        this, characterSpell.getLevel(), id);
                // TODO: Message about new level with spell
                // TODO: Set AutoSpell if not set
                break;
            default:
                break;
        }
    }

    public void removeGuildLesson(Guild guild) {
        if (guild == null)
            return;

        CharacterGuild charGuild = this.info.getGuilds().get(guild.getId());
        // The player has no levels at this guild
        if (charGuild == null || charGuild.getLevel() == 0)
            return;

        // This guild is fully learned
        if (charGuild.getLevel() == 15) {
            // The next guild should not be started at learning
            CharacterGuild charNextGuild = this.info.getGuilds().get(guild.getNextId());
            if (charNextGuild != null && charNextGuild.getLevel() > 0)
                return;
            // TODO: Should be checked all guilds where 'prevId' is this guild
        }

        GuildLesson lesson = guildLessonRepository.get(guild.getId(), charGuild.getLevel());

        this.cancelGuildImprovement(lesson.getImprovementType1(), lesson.getId1(), lesson.getCount1());
        this.cancelGuildImprovement(lesson.getImprovementType2(), lesson.getId2(), lesson.getCount2());

        charGuild.setLevel(charGuild.getLevel() - 1);
        if (charGuild.getLevel() == 0) {
            this.info.getGuilds().remove(guild.getId());
        }
        --this.guildLevel;

        if (this.session != null) {
            session.send(Arrays.asList(
                    getMagicData(),
                    getParametersData(),
                    getGuildLevelData()
            ));
        }
    }

    private void cancelGuildImprovement(GuildLesson.ImprovementTypes type, int id, int count) {
        switch (type) {
            case Parameter:
                this.parameters.guild().change(Parameters.parse(id), -count);
                break;
            case Skill:
                CharacterSkill characterSkill = this.info.getSkills().get(id);
                if (characterSkill == null)
                    break;
                characterSkill.setLevel(characterSkill.getLevel() - count);
                if (characterSkill.getLevel() < 1) {
                    this.info.getSkills().remove(id);
                }
                logger.debug("{} has downgraded the level to {} of the skill {}",
                        this, characterSkill.getLevel(), id);
                break;
            case Spell:
                CharacterSpell characterSpell = this.info.getSpells().get(id);
                if (characterSpell == null)
                    break;
                characterSpell.setLevel(characterSpell.getLevel() - count);
                if (characterSpell.getLevel() < 1) {
                    this.info.getSpells().remove(id);
                }
                logger.debug("{} has downgraded the level to {} of the skill {}",
                        this, characterSpell.getLevel(), id);
                break;
            default:
                break;
        }
    }

    public ServerMessage getAchievementData() {
        List<AchievementsMessage.AchievementInfo> achievements = new ArrayList<>(this.achievementRepository.size());
        for (Achievement achievement : achievementRepository) {
            int progress = 0;
            int completeDate = 0;
            CharacterAchievement charAchieve;
            if ((charAchieve = getAchievement(achievement.getId())) != null) {
                progress = charAchieve.getProgress();
                completeDate = charAchieve.getCompleteDate();
            }
            achievements.add(new AchievementsMessage.AchievementInfo(
                    achievement.getId(),
                    achievement.getTitle(),
                    achievement.getDescription(),
                    progress,
                    achievement.getCount(),
                    completeDate,
                    achievement.getCategoryId(),
                    achievement.getPoints()
            ));
        }

        return new AchievementsMessage(achievements);
    }

    public ServerMessage getCurrencyData() {
        List<CurrenciesMessage.CurrencyInfo> infos = new ArrayList<>(this.currencyRepository.size());
        for (Currency currency : currencyRepository) {
            infos.add(new CurrenciesMessage.CurrencyInfo(
                    currency.getId(),
                    currency.getUnk2(),
                    currency.getTitle(),
                    currency.getDescription(),
                    currency.getUnk5(),
                    currency.isDisabled(),
                    getCurrencyAmount(currency.getId()))
            );
        }
        return new CurrenciesMessage(infos);
    }

    public ServerMessage getProfessionData() {
        List<ProfessionsMessage.ProfessionInfo> infos = new ArrayList<>(this.professions.length);
        for (Profession profession : this.professions) {
            infos.add(new ProfessionsMessage.ProfessionInfo(
                    locale.getString(profession.getProfession().getTitleId()),
                    profession.getLevel(),
                    profession.getExperience()
            ));
        }
        return new ProfessionsMessage(infos);
    }

    public ServerMessage getHealthManaData() {
        return new HealthMessage(this.health,this.mana);
    }

    private void onCurrencyChanged(int currencyId, int difference) {
        // HACK: Bank deposit sends all the data

        // Partial Data change
        if (this.session != null)
            this.session.send(new CurrencyUpdateMessage(currencyId, getCurrencyAmount(currencyId)));
    }

    @Override
    public Corpse die() {
        // TODO: Remove auras

        this.deathState = DeathStates.Dead;

        if (this.session != null) {
            List<ServerMessage> messages = new ArrayList<>();
            // TODO: why do we send relax state?
            // messages.add(new RelaxOffMessage());
            messages.add(new _lh0Message());
            messages.add(new NoGoMessage(this.getHome().getId()));
            this.session.send(messages);
        }
        this.setPosition(this.getHome());
        this.deathState = DeathStates.Alive;
        this.setHealth(1);

        return null;
    }

    public void lootCorpse(Corpse corpse) {
        // Money
        if (corpse.getGold() > 0) {
            if (this.session != null) {
                this.session.send(new ChatMessage(
                        998, corpse.getOwner().getName(), corpse.getGold(), locale.getString(2)
                ));
            }
            this.position.send(this, new ChatMessage(999, this.getName(),
                    corpse.getOwner().getName(), corpse.getGold(), locale.getString(2)));
            this.changeCurrency(Currencies.Gold, corpse.getGold());
        }

        Set<Item> items = corpse.getItems();
        items.forEach(item -> {
            String itemTitle = itemTitleConstructor.getTitle(item);
            if (this.session != null) {
                this.session.send(new ChatMessage(
                       998, corpse.getOwner().getName(), item.getCount() > 1 ? item.getCount() + " " : "", itemTitle
                ));
            }
            this.position.send(this, new ChatMessage(999, this.getName(),
                    corpse.getOwner().getName(), item.getCount() > 1 ? item.getCount() + " " : "", itemTitle));
            this.inventory.tryStoreItem(item);
            // TODO: leave corpse if a player didn't take all the loot
        });

        this.position.removeCorpse(corpse);
    }

    public void examine(Unit target) {
        // Nothing to output without a session
        if (this.session == null)
            return;

        // Examine self
        if (target == null || target == this) {

            // TODO: Implement
        }
        // Examine another player
        else if (target.getUnitType() == UnitTypes.Player) {

            // TODO: Implement
        }
        // Examine a creature
        else if (target.getUnitType() == UnitTypes.Creature) {
            Creature creature = (Creature) target;
            List<ServerMessage> messages = new ArrayList<>();

            int lineIteration = 1;
            // The first parameter can be seen after 50 creatures of this level;
            double practise = this.info.getPractiseValue() / creature.getLevel() / 50;
            do {
                String[] params;
                switch (lineIteration) {
                    // Health, Mana, Damage, Magic Damage
                    case 1: // 50 pcs
                        params = new String[5];
                        params[0] = creature.getHealth() + "/" + creature.getParameters().value(Parameters.Health);
                        params[1] = Integer.toString(creature.getMana());
                        params[2] = Integer.toString(creature.getParameters().value(Parameters.Mana));
                        // Physical and Magic Damage
                        practise /= 2; // 100 pcs
                        if (practise < 1) {
                            params[3] = "?/?";
                            params[4] = "?";
                        } else {
                            params[3] = creature.getParameters().value(Parameters.Damage) + "/" +
                                    creature.getParameters().value(Parameters.MaxDamage);
                            params[4] = Integer.toString(creature.getParameters().value(Parameters.MagicDamage));
                        }
                        messages.add(new ChatMessage(1265, params));
                        break;
                    // Protection, Health and Mana Regeneration, Chances to Hit and Cast
                    case 2: // 200 pcs
                        params = new String[5];
                        //Protection
                        params[0] = Integer.toString(creature.getParameters().value(Parameters.Protection));

                        // Health and Mana Regeneration
                        practise /= 2; // 400 pcs
                        if (practise < 1d) {
                            params[1] = "?";
                            params[2] = "?";
                        } else {
                            params[1] = Integer.toString(creature.getParameters().value(Parameters.HealthRegeneration));
                            params[2] = Integer.toString(creature.getParameters().value(Parameters.ManaRegeneration));
                        }

                        // Chance to Hit and Chance to Cast
                        practise /= 2; // 800 pcs
                        if (practise < 1d) {
                            params[3] = "?";
                            params[4] = "?";
                        } else {
                            params[3] = Integer.toString(creature.getParameters().value(Parameters.ChanceToHit));
                            params[4] = Integer.toString(creature.getParameters().value(Parameters.ChanceToCast));
                        }
                        messages.add(new ChatMessage(1266, params));
                        break;
                    // Armour, Resists
                    case 3: // 1600 pcs
                        params = new String[4];
                        params[0] = Integer.toString(creature.getParameters().value(Parameters.Armour));

                        // Resists
                        practise /= 2; //3200 pcs
                        if (practise < 1d) {
                            params[1] = "?";
                            params[2] = "?";
                            params[3] = "?";
                        } else {
                            params[1] = Integer.toString(creature.getParameters().value(Parameters.FireResistance));
                            params[2] = Integer.toString(creature.getParameters().value(Parameters.FrostResistance));
                            params[3] = Integer.toString(creature.getParameters().value(Parameters.LightningResistance));
                        }
                        messages.add(new ChatMessage(1267, params));
                        break;
                    // Stats
                    case 4: // 6400 pcs
                        messages.add(new ChatMessage(
                                1271,
                                creature.getParameters().value(Parameters.Strength),
                                creature.getParameters().value(Parameters.Dexterity),
                                creature.getParameters().value(Parameters.Intelligence),
                                creature.getParameters().value(Parameters.Constitution)
                        ));
                        break;
                    // Current loot
                    case 5: // 12800 pcs
                        Iterator<Item> lootIterator = creature.getLootIterator();
                        while (lootIterator.hasNext()) {
                            Item item = lootIterator.next();
                            String itemTitle = itemTitleConstructor.getTitle(item);
                            if (item.getCount() > 1) {
                                itemTitle = item.getCount() + " " + itemTitle;
                            }
                            messages.add(new ChatMessage(1268, creature.getTemplate().getName(), itemTitle));
                        }
                        break;
                    // Creature max gold
                    case 6: // 25600 pcs
                        // Humanoids only
                        if (!creature.getTemplate().hasFlag(CreatureFlags.Beast)) {
                            messages.add(new ChatMessage(
                                    1269,
                                    creature.getTemplate().getName(),
                                    creature.getMaxGoldValue()
                            ));
                        }
                        break;
                    // Creature current gold tip
                    case 7: // 51,200 pcs
                        // Humanoids only
                        if (creature.getTemplate().hasFlag(CreatureFlags.Beast))
                            break;
                        int messageId;
                        double goldLoad = 1d * creature.getCashGold() / creature.getMaxGoldValue();

                        // Almost nothing
                        if (creature.getCashGold() < creature.getMinGoldValue()) {
                            messageId = 1307;
                        }
                        // a little
                        else if (goldLoad <= 0.25) {
                            messageId = 1306;
                        }
                        // medium
                        else if (goldLoad <= 0.75) {
                            messageId = 1305;
                        }
                        // Much (loading over 75%)
                        else {
                            messageId = 1304;
                        }
                        messages.add(new ChatMessage(messageId));
                        break;
                    // Loot chances
                    case 8: // 102,400 pcs
                        /*
                        TODO:
                        1270
                        CreatureName
                        ItemName
                        ChancePercentage
                         */
                    default:
                        break;
                }
                lineIteration++;
                practise /= 2;
            } while (lineIteration < 9 && practise >= 1d);

            String typeTitle;
            if (creature.getTemplate().hasFlag(CreatureFlags.Unique)) {
                typeTitle = locale.getString(33);
            } else {
                typeTitle = locale.getString(34);
            }

            int openingMessageId = messages.isEmpty() ? 1260 : 1261;

            ServerMessage openingMessage = new ChatMessage(
                    openingMessageId,
                    creature.getTemplate().getName(),
                    creature.getTemplate().getName(),
                    typeTitle
            );

            if (messages.isEmpty()) {
                messages.add(openingMessage);
            } else {
                messages.add(0, openingMessage);
            }
            this.session.send(messages);
        }
    }

    public void interact(Unit unit) {
        logger.debug("Player {} interacts with {}", getName(), unit.getName());

        if (unit.getUnitType() == UnitTypes.Creature) {
            Creature creature = (Creature) unit;

            // Quests
            if (creature.getTemplate().hasFlag(CreatureFlags.QuestGiver)) {
                // Does the creature have any quests
                Collection<CreatureQuestRelation> creatureQuests = creatureQuestRelationRepository.get(creature.getTemplateId());
                if (creatureQuests.isEmpty()) {
                    return;
                }

                Quest quest;

                int nextQuestId = 0;

                // Look through all the quests to define whether the player has not completed that quests
                for (CreatureQuestRelation creatureQuest : creatureQuests) {
                    quest = getQuest(creatureQuest.getQuestTemplateId());
                    if (quest == null)
                        continue;

                    // Complete a quest
                    if (quest.getStatus() == QuestStatuses.Taken && quest.isGoalAchieved()) {
                        quest.complete();

                        if (nextQuestId == 0)
                            nextQuestId = quest.getQuestTemplate().getNextQuestId() == null ? 0
                                    : quest.getQuestTemplate().getNextQuestId();
                    }
                }

                if (nextQuestId != 0) {
                    if (tryAcceptQuest(nextQuestId))
                        return;
                }

                if (this.session == null) {
                    return;
                }

                // Prepare quest List data to send to the player. Then the player will choose the one.
                NpcQuestsMessage.Builder questBuilder = new NpcQuestsMessage.Builder();
                // Search through quest relations of the NPC
                for (CreatureQuestRelation creatureQuest : creatureQuests) {
                    QuestTemplate template = questTemplateRepository.get(creatureQuest.getQuestTemplateId());
                    // NPC can give a quest
                    // The quest is valid
                    // The player level is enough
                    if (creatureQuest.canGiveQuest()
                            && template != null
                            && template.getLevel() <= this.getLevel()) {
                        // Player has not taken the quest before
                        quest = getQuest(creatureQuest.getQuestTemplateId());
                        if (quest != null) {
                            continue;
                        }
                        questBuilder.addQuest(template.getId(), template.getTitle());
                    }
                }

                if (questBuilder.size() == 0) {
                    return;
                }
                this.session.send(questBuilder.build());
                return;
            }
        }

        // Set target and enter the target's battle
        this.setTarget(unit);
    }

    public boolean tryAcceptQuest(int questId) {
        QuestTemplate template = questTemplateRepository.get(questId);
        if (template == null) {
            logger.warn("{} tries to accept not existing quest template {}", this, questId);
            return false;
        }

        Quest quest = getQuest(questId);
        if (quest != null) {
            if (quest.isAccepted())
                return false;
        } else {
            // Check required Min level
            if (template.getLevel() > this.getLevel())
                return false;

            CharacterQuest charQuest = new CharacterQuest();
            charQuest.setCharacterId(this.getId());
            charQuest.setQuestTemplateId(template.getId());
            this.info.getQuests().put(questId, charQuest);
            quest = new Quest(this, template, charQuest);
            this.quests.put(questId, quest);
        }

        if (this.session != null) {
            this.session.send(questInfoPacketFactory.create(quest.getQuestTemplate(), true));
        }
        return true;
    }

    @Override
    public void addServerMessage(int messageId, String... args) {
        if (this.session != null)
            this.session.send(new ChatMessage(messageId, args));
    }

    /**
     * Occurs when at least one of player parameters that are displayed in online list changes.
     */
    protected void onDisplayChanged() {
        world.playerUpdated(this);
    }

    @Override
    public void update(int time) {
        super.update(time);

        // Client-Server Aura Synchronization
        // TODO: also recalculate aura bonus parameters
        if (this.syncTimer < 0) {
            // Update Heroic Shield effect
            if (!this.isInCombat()) {
                spellFactory.createSelf(1141, this, 1).cast();
            }

            if (this.session != null && this.auras.size() != 0) {
                synchronized (this.auras) {
                    for (Aura aura : this.auras.values()) {
                        // Synchronize timed auras only!
                        if (aura.isPermanent())
                            continue;

                        this.session.send(aura.getBonusMessages());
                    }
                }
            }
            this.syncTimer = Player.SyncTime;
        } else
            this.syncTimer -= time;

        // Auto-Save
        if (this.saverTimer < 0) {
            logger.debug("Player's {} save timer elapsed.", this);
            save();
            this.saverTimer = SaveTime;
        } else
            this.saverTimer -= time;
    }
}
