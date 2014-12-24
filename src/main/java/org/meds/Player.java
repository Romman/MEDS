package org.meds;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.meds.database.DBStorage;
import org.meds.database.Hibernate;
import org.meds.database.entity.*;
import org.meds.enums.*;
import org.meds.logging.Logging;
import org.meds.map.Location;
import org.meds.net.ServerOpcodes;
import org.meds.net.ServerPacket;
import org.meds.spell.Aura;
import org.meds.util.EnumFlags;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class Player extends Unit
{
    private class SessionDisconnect implements org.meds.net.Session.DisconnectListener
    {
        @Override
        public void disconnect(org.meds.net.Session session)
        {
            // TODO; Implement timer to logout the hanging player
            Player.this.session = null;
        }
    }

    protected static final int SaveTime = 60000;
    protected static final int SyncTime = 20000;

    protected org.meds.net.Session session;

    private EnumFlags<PlayerSettings> settings;

    private EnumFlags<PlayerStatuses> statuses;

    private int saverTimer;
    private int syncTimer;

    private Inventory inventory;
    private Inn inn;

    private CharacterInfo info;

    private Map<Integer, Quest> quests;

    private int guildLevel;

    private SessionDisconnect disconnector;

    private AchievementManager achievementManager;

    private Group group;

    public Player(int guid)
    {
        super();
        this.guid = guid;
        this.unitType = UnitTypes.Player;

        this.statuses = new EnumFlags<>();
        this.settings = new EnumFlags<>();

        this.inn = new Inn(this);
        this.inventory = new Inventory(this);

        this.saverTimer = SaveTime;
        this.syncTimer = SyncTime;

        this.disconnector = new SessionDisconnect();
    }

    @Override
    public int getAutoSpell()
    {
        Integer autoSpell = this.info.getAutoSpellId();
        if (autoSpell == null)
            return 0;
        return autoSpell;
    }

    @Override
    public int getSkillLevel(int skillId)
    {
        CharacterSkill skill = this.info.getSkills().get(skillId);
        if (skill == null)
            return 0;
        return skill.getLevel();
    }

    @Override
    public int getSpellLevel(int spellId)
    {
        CharacterSpell spell = this.info.getSpells().get(spellId);
        if (spell == null)
            return 0;
        return spell.getLevel();
    }

    public void setAutoSpell(int spellId)
    {
        if (this.getSpellLevel(spellId) != 0)
        {
            spellId = 0;
        }

        this.info.setAutoSpellId(spellId);

        if (this.session != null)
            this.session.send(new ServerPacket(ServerOpcodes.AutoSpell).add(spellId));
    }

    public boolean isRelax()
    {
        return this.hasAura(1000);
    }

    public Location getHome()
    {
        return org.meds.map.Map.getInstance().getLocation(this.info.getHomeId());
    }

    public void setHome()
    {
        this.setHome(this.position);
    }

    public void setHome(Location home)
    {
        if (home == null)
            return;
        if (home.getSpecialLocationType() != SpecialLocationTypes.Star)
            return;

        this.info.setHomeId(home.getId());
        if (this.session != null)
            this.session.sendServerMessage(17, home.getRegion().getName());
    }

    public org.meds.net.Session getSession()
    {
        return this.session;
    }

    @Override
    public String getName()
    {
        return this.info.getName();
    }

    @Override
    public int getAvatar()
    {
        return this.info.getAvatarId();
    }

    public Inventory getInventory()
    {
        return this.inventory;
    }

    public Inn getInn()
    {
        return this.inn;
    }

    public EnumFlags<PlayerSettings> getSettings()
    {
        return this.settings;
    }

    public EnumFlags<PlayerStatuses> getStatuses()
    {
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

    public int getGuildLevel()
    {
        return this.guildLevel;
    }

    @Override
    public int getHealth()
    {
        return this.info.getHealth();
    }

    @Override
    public int getMana()
    {
        return this.info.getMana();
    }

    @Override
    public void setHealth(int health)
    {
        super.setHealth(health);
        this.info.setHealth(health);
        if (this.session != null)
            this.session.send(this.getHealthManaData());
    }

    @Override
    public void setMana(int mana)
    {
        super.setMana(mana);
        this.info.setMana(mana);
        if (this.session != null)
            this.session.send(this.getHealthManaData());
    }

    @Override
    public void setHealthMana(int health, int mana)
    {
        super.setHealthMana(health, mana);
        this.info.setHealth(health);
        this.info.setMana(mana);
        if (this.session != null)
            this.session.send(this.getHealthManaData());
    }

    @Override
    public int getLevel()
    {
        return this.info.getLevel();
    }

    @Override
    public int getReligLevel()
    {
        return this.info.getReligLevel();
    }

    public int getReligExp()
    {
        return this.info.getReligExp();
    }

    public int getExp()
    {
        return this.info.getExp();
    }

    private void setExp(int value)
    {
        this.info.setExp(value);

        int nextLvlExp = LevelCost.getExp(this.getLevel() + 1);

        // Is Level Up
        if (nextLvlExp <= this.getExp())
        {
            this.info.setExp(this.info.getExp() - nextLvlExp);
            this.info.setLevel(this.info.getLevel() + 1);
            // Send messages
            onVisualChanged();
            onDisplayChanged();
            if (this.session != null)
                this.session.sendServerMessage(497). // You gain a new level
                        sendServerMessage(492). // You can learn new lesson in a guild
                        send(this.getLevelData()).send(getParametersData());
        }
        else
        {
            if (this.session != null)
                this.session.send(this.getLevelData(true));
        }

    }

    public void setLevel(int level)
    {
        if (this.info.getLevel() == level)
            return;
        this.info.setLevel(level);

        onVisualChanged();
        onDisplayChanged();

        if (this.session != null)
            this.session.send(this.getLevelData());
    }

    public ServerPacket getLevelData()
    {
        return this.getLevelData(false);
    }

    public ServerPacket getLevelData(boolean experienceOnly)
    {
        ServerPacket packet = new ServerPacket(ServerOpcodes.Experience);
        packet.add(this.getExp());
        packet.add(this.getReligExp());

        if(!experienceOnly)
        {
            packet.add(this.getLevel());
            packet.add("0");
            packet.add(this.getReligLevel());
        }

        return packet;
    }

    protected void addExp(int value)
    {
        this.setExp(this.getExp() + value);
    }

    public int getCurrencyAmount(Currency currency)
    {
        return this.getCurrencyAmount(currency.getId());
    }

    public int getCurrencyAmount(Currencies currency)
    {
        return this.getCurrencyAmount(currency.getValue());
    }

    public int getCurrencyAmount(int currencyId)
    {
        CharacterCurrency currency = this.info.getCurrencies().get(currencyId);
        if (currency == null)
            return 0;
        return currency.getAmount();
    }

    public boolean changeCurrency(Currencies currency, int difference)
    {
        return this.changeCurrency(currency.getValue(), difference);
    }

    public boolean changeCurrency(int currencyId, int difference)
    {
        CharacterCurrency currency = this.info.getCurrencies().get(currencyId);
        // Create new record
        if (currency == null)
        {
            currency = new CharacterCurrency(this.guid, currencyId);
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
    public void depositMoney(int amount)
    {
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
    public void withdrawMoney(int amount)
    {
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
            this.session.send(new ServerPacket(ServerOpcodes.GroupCreated).add("1") // Created as leader
                    .add(this.getGuid()) // Leader's GUID
            )
                    .send(this.group.getSettingsData())
                    .send(this.group.getTeamLootData())
                    .sendServerMessage(270) // Group has been created
                    .sendServerMessage(this.group.getTeamLootMode().getModeMessage());
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
                this.session.sendServerMessage(group.getTeamLootMode().getModeMessage())
                        // "You join the group of {LEADER_NAME}
                        .sendServerMessage(273, group.getLeader().getName());
            }
            // Say to everyone that the player changes its Leader's GUID
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
            // Say to everyone that the player changes its Leader's GUID
            onVisualChanged();
            return true;
        }

        return false;
    }

    @Override
    public int create()
    {
        if (!load())
            return 0;

        this.health = this.parameters.value(Parameters.Health);
        this.mana = this.parameters.value(Parameters.Mana);

        return super.create();
    }

    public void logIn(org.meds.net.Session session)
    {
        if (this.session != null)
        {
            Logging.Warn.log ("Player %d assigns a new session with existing one.");
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

    private boolean load()
    {
        Session session = Hibernate.getSessionFactory().openSession();

        this.info = DBStorage.getCharacterInfo(session, this.guid);

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
        for (CharacterGuild guild : this.info.getGuilds().values())
        {
            this.guildLevel += guild.getLevel();
        }

        // Accept all the quests
        this.quests = new HashMap<>();
        for (CharacterQuest charQuest : this.info.getQuests().values()) {
            Quest quest = new Quest(this, DBStorage.QuestTemplateStore.get(charQuest.getQuestTemplateId()), charQuest);
            quest.accept();
            this.quests.put(quest.getQuestTemplate().getId(), quest);
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
        this.position = org.meds.map.Map.getInstance().getLocation(this.info.getLocationId());

        // Home
        // Directly from info

        // AutoSpell
        // TODO: here or from info

        this.settings = new EnumFlags<>(this.info.getSettings());
        this.statuses = new EnumFlags<>(this.info.getStatuses());

        this.inventory.load(this.info.getInventoryItems());
        this.inn.load(this.info.getInnItems());

        session.close();

        this.health = this.parameters.value(Parameters.Health);
        this.mana = this.parameters.value(Parameters.Mana);

        this.achievementManager = new AchievementManager(this);

        return true;
    }

    public void save()
    {
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

        Session session = Hibernate.getSessionFactory().openSession();
        Transaction t = session.beginTransaction();
        session.update(this.info);
        t.commit();
        //session.flush();
        session.close();
    }

    public ServerPacket getParametersData()
    {
        return new ServerPacket(ServerOpcodes.PlayerInfo)
            .add(this.guid)
            .add(this.getName())
            .add(this.getAvatar())
            .add("1248860848") // Seems like a date. but what the date???
            .add(this.race.toString())
            .add(0)
            .add(this.clanId)
            .add(this.clanMemberStatus)
            .add(0) // Clan bonus???
            .add(0) // TODO: SkullsCount
            .add(this.parameters.base().value(Parameters.Intelligence))
            .add(this.parameters.base().value(Parameters.Constitution))
            .add(this.parameters.base().value(Parameters.Strength))
            .add(this.parameters.base().value(Parameters.Dexterity))
            .add(this.parameters.guild().value(Parameters.Constitution))
            .add(this.parameters.guild().value(Parameters.Strength))
            .add(this.parameters.guild().value(Parameters.Dexterity))
            .add(this.parameters.guild().value(Parameters.Intelligence))
            .add(this.parameters.guild().value(Parameters.Damage))
            .add(this.parameters.guild().value(Parameters.Protection))
            .add(this.parameters.guild().value(Parameters.ChanceToHit))
            .add(this.parameters.guild().value(Parameters.Armour))
            .add(this.parameters.guild().value(Parameters.ChanceToCast))
            .add(this.parameters.guild().value(Parameters.MagicDamage))
            .add(this.parameters.guild().value(Parameters.Health))
            .add(this.parameters.guild().value(Parameters.Mana))
            .add(this.parameters.guild().value(Parameters.HealthRegeneration))
            .add(this.parameters.guild().value(Parameters.ManaRegeneration))
            .add(this.parameters.guild().value(Parameters.FireResistance))
            .add(this.parameters.guild().value(Parameters.FrostResistance))
            .add(this.parameters.guild().value(Parameters.LightningResistance))
            .add("9989") // StartCell ?
            .add(this.settings.toString())
            .add("1367478137") // Start Server Time
            .add("16909320") // TODO: Version (or Version of what???)
            .add(this.inventory.getCapacity())
            .add("0")
            .add("1") // TODO: Gender
            .add("0"); // TODO: Religious status
    }

    public ServerPacket getGuildData()
    {
        ServerPacket packet = new ServerPacket(ServerOpcodes.GuildInfo);
        packet.add(DBStorage.GuildStore.size());
        for (Map.Entry<Integer, Guild> entry : DBStorage.GuildStore.entrySet())
        {
            packet.add(entry.getValue().getId())
                .add(entry.getValue().getName())
                .add(entry.getValue().getPrevId());
            CharacterGuild characterGuild = this.info.getGuilds().get(entry.getValue().getId());
            if (characterGuild == null)
                packet.add("0");
            else
                packet.add(characterGuild.getLevel());
        }
        return packet;
    }

    public ServerPacket getMagicData()
    {
        ServerPacket packet = new ServerPacket(ServerOpcodes.MagicInfo);
        packet.add(DBStorage.SpellStore.size());

        for (Map.Entry<Integer, Spell> entry : DBStorage.SpellStore.entrySet())
        {
            packet.add(entry.getValue().getId())
                .add(entry.getValue().getType().toString())
                .add(entry.getValue().getName());
            CharacterSpell characterSpell = this.info.getSpells().get(entry.getValue().getId());
            if (characterSpell == null)
                packet.add("0");
            else
                packet.add(characterSpell.getLevel());
        }
        return packet;
    }

    public ServerPacket getSkillData()
    {
        ServerPacket packet = new ServerPacket(ServerOpcodes.SkillInfo);
        packet.add(DBStorage.SkillStore.size());

        for (Map.Entry<Integer, Skill> entry : DBStorage.SkillStore.entrySet())
        {
            packet.add(entry.getValue().getId())
                .add(entry.getValue().getName());
            CharacterSkill characterSkill = this.info.getSkills().get(entry.getValue().getId());
            if (characterSkill == null)
                packet.add("0");
            else
                packet.add(characterSkill.getLevel());
        }
        return packet;
    }

    public ServerPacket getGuildLevelData()
    {
        ServerPacket packet = new ServerPacket(ServerOpcodes.GuildLevels);
        packet.add(this.guildLevel);
        packet.add(this.info.getGuilds().size());
        Guild guildEntry;
        for (CharacterGuild guild : this.info.getGuilds().values()) {
            guildEntry = DBStorage.GuildStore.get(guild.getGuildId());
            packet.add(guildEntry.getName());
            packet.add(guild.getLevel());
        }
        return packet;
    }

    public void learnGuildLesson(Guild guild)
    {
        if (guild == null)
            return;

        int guildId = guild.getId();

        CharacterGuild charGuild = this.info.getGuilds().get(guildId);
        if (charGuild == null)
        {
            // This is the first lesson in this guild
            // Check the previous(required) guild to be learned;
            if (guild.getPrevId() != 0)
            {
                CharacterGuild prevCharGuild = this.info.getGuilds().get(guild.getPrevId());
                if (prevCharGuild == null || prevCharGuild.getLevel() != 15)
                    return;
            }

            charGuild = new CharacterGuild(this.guid, guildId);
            this.info.getGuilds().put(guildId, charGuild);
        }

        // This guild is already learned
        if (charGuild.getLevel() == 15)
            return;

        GuildLesson lesson = DBStorage.GuildLessonStore.get(guildId).get(charGuild.getLevel() + 1);
        if (!this.changeCurrency(Currencies.Gold.getValue(), -LevelCost.getGold(this.guildLevel + 1)))
            return;

        this.applyGuildImprovement(lesson.getImprovementType1(), lesson.getId1(), lesson.getCount1());
        this.applyGuildImprovement(lesson.getImprovementType2(), lesson.getId2(), lesson.getCount2());

        charGuild.setLevel(charGuild.getLevel() + 1);
        ++this.guildLevel;

        if (this.session != null) {
            session.sendServerMessage(498);
            // TODO: Implement sound sending (Sound 31 here)
            session.send(new ServerPacket().add(this.getMagicData()).add(this.getParametersData()).add(this.getGuildLevelData()));
        }
    }

    private void applyGuildImprovement(GuildLesson.ImprovementTypes type, int id, int count)
    {
        switch(type)
        {
            case Parameter:
                this.parameters.guild().change(Parameters.parse(id), count);
                break;
            case Skill:
                CharacterSkill characterSkill = this.info.getSkills().get(id);
                if (characterSkill == null)
                {
                    characterSkill = new CharacterSkill(id, 0);
                    this.info.getSkills().put(id, characterSkill);
                }
                characterSkill.setLevel(characterSkill.getLevel() + count);
                Logging.Debug.log(this + "has learnt skill id " + id + " and the current level is " +
                        characterSkill.getLevel());
                break;
            case Spell:
                CharacterSpell characterSpell = this.info.getSpells().get(id);
                if (characterSpell == null) {
                    characterSpell = new CharacterSpell(this.guid, id, 0);
                    this.info.getSpells().put(id, characterSpell);
                }
                characterSpell.setLevel(characterSpell.getLevel() + count);
                Logging.Debug.log(this + "has learnt spell id " + id + " and the current level is " +
                        characterSpell.getLevel());
                // TODO: Message about new level with spell
                // TODO: Set AutoSpell if not set
                break;
            default: break;
        }
    }

    public ServerPacket getAchievementData() {
        ServerPacket packet = new ServerPacket(ServerOpcodes.AchievementList);
        packet.add(0); // List of all achievements

        for (Achievement achievement : DBStorage.AchievementStore.values()) {
            packet.add(achievement.getId());
            packet.add(achievement.getTitle());
            packet.add(achievement.getDescription());

            CharacterAchievement charAchieve;
            if ((charAchieve = getAchievement(achievement.getId())) != null) {
                packet.add(charAchieve.getProgress());
                packet.add(achievement.getCount());
                packet.add(charAchieve.getCompleteDate());
            } else {
                packet.add(0);
                packet.add(achievement.getCount());
                packet.add(0);
            }
            packet.add(achievement.getCategoryId());
            packet.add(achievement.getPoints());
        }

        return packet;
    }

    public ServerPacket getCurrencyData()
    {
        ServerPacket packet = new ServerPacket(ServerOpcodes.Currencies);
        for (Currency currency : DBStorage.CurrencyStore.values())
        {
            packet.add(currency.getId())
                .add(currency.getUnk2())
                .add(currency.getTitle())
                .add(currency.getDescription())
                .add(currency.getUnk5())
                .add(currency.isDisabled() ? "1" : "0")
                .add(getCurrencyAmount(currency.getId()));
        }
        return packet;
    }

    @Override
    public ServerPacket getHealthManaData()
    {
        return new ServerPacket(ServerOpcodes.Health).
                add(this.health).
                add(this.mana).
                add("0"); // Unknown. Always is 0.
    }

    private void onCurrencyChanged(int currencyId, int difference)
    {
        // HACK: Bank deposit sends all the data

        // Partial Data change
        if (this.session != null)
            this.session.send(new ServerPacket(ServerOpcodes.Currency).add(currencyId).add(getCurrencyAmount(currencyId)));
    }

    @Override
    public Corpse die()
    {
        // TODO: Remove auras

        this.deathState = DeathStates.Dead;

        if (this.session != null)
        {
            // TODO: why do we send relax state?
            //player.Session.AddData("r0");
            ServerPacket packet = new ServerPacket(ServerOpcodes._lh0).add("");
            packet.add(ServerOpcodes.NoGo).add(this.getHome().getId());
            this.session.send(packet);
        }
        this.setPosition(this.getHome());
        this.deathState = DeathStates.Alive;
        this.setHealth(1);

        return null;
    }

    public void lootCorpse(Corpse corpse)
    {
        // Money
        if (corpse.getGold() > 0)
        {
            if (this.session != null)
                // TODO: Remove locale string form here and below
                this.session.sendServerMessage(998, corpse.getOwner().getName(), Integer.toString(corpse.getGold()), " золота");
            this.position.send(this,
                    new ServerPacket(ServerOpcodes.ServerMessage)
                            .add("999").add(this.getName())
                            .add(corpse.getOwner().getName())
                            .add(corpse.getGold())
                            .add(" золота"));
            this.changeCurrency(Currencies.Gold, corpse.getGold());
        }

        Set<Item> items = corpse.getItems();
        if (items.size() > 0)
        {
            for (Item item : items)
            {
                if (this.session != null)
                this.session.sendServerMessage(998, corpse.getOwner().getName(), item.getCount() > 1 ? item.getCount() + " " : "", item.Template.getTitle());
                this.position.send(this,
                        new ServerPacket(ServerOpcodes.ServerMessage)
                                .add("999")
                                .add(this.getName())
                                .add(corpse.getOwner().getName())
                                .add(item.getCount() > 1 ? item.getCount() + " " : "")
                                .add(item.Template.getTitle()));
                this.inventory.tryStoreItem(item);
                // TODO: leave corpse if a player didn't take all the loot
            }
        }

        this.position.removeCorpse(corpse);
    }

    public void interact(Unit unit) {
        Logging.Debug.log("Player " + this.getName() + " interacts with " + unit.getName());

        if (unit.getUnitType() == UnitTypes.Creature) {
            Creature creature = (Creature)unit;

            // Quests
            if (creature.getTemplate().hasFlag(CreatureFlags.QuestGiver)) {

                // Does the creature have any quests
                Map<Integer, CreatureQuestRelation> creatureQuests = DBStorage.CreatureQuestRelationStore.get(creature.getTemplateId());
                if (creatureQuests == null)
                    return;

                Quest quest;

                int nextQuestId = 0;

                // Look through all the quests to define whether the player has not completed that quests
                for (Integer questTemplateId :  creatureQuests.keySet()) {
                    quest = getQuest(questTemplateId);
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

                // Prepare quest List data to send to the player. Then the player will choose the one.
                ServerPacket packet = new ServerPacket(ServerOpcodes.NpcQuestList);
                int count = 0;
                // Search through quest relations of the NPC
                for(CreatureQuestRelation creatureQuest : creatureQuests.values()) {
                    QuestTemplate template = DBStorage.QuestTemplateStore.get(creatureQuest.getQuestTemplateId());
                    // NPC can give a quest
                    // The quest is valid
                    // The player level is enough
                    if (creatureQuest.canGiveQuest()
                            && template != null
                            && template.getLevel() <= this.getLevel()) {
                        // Player has not taken the quest before
                        quest = getQuest(creatureQuest.getQuestTemplateId());
                        if (quest != null)
                            continue;

                        packet.add(template.getId());
                        packet.add(template.getTitle());
                        ++count;
                    }
                }

                if (count == 0)
                    return;

                if (this.session != null) {
                    this.session.send(packet);
                }

                return;
            }
        }

        // Set target and enter the target's battle
        this.setTarget(unit);
    }

    public boolean tryAcceptQuest(int questId) {
        QuestTemplate template = DBStorage.QuestTemplateStore.get(questId);
        if (template == null) {
            Logging.Warn.log("{Player %s (%d) tries to accept not existing quest template %d", getName(), getGuid(), questId);
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
            charQuest.setCharacterId(this.getGuid());
            charQuest.setQuestTemplateId(template.getId());
            this.info.getQuests().put(questId, charQuest);
            quest = new Quest(this, template, charQuest);
            this.quests.put(questId, quest);
        }

        if (this.session != null) {
            this.session.send(quest.getQuestTemplate().getQuestInfoData(true));
        }
        return true;
    }

    @Override
    public void addServerMessage(int messageId, String... args)
    {
        if (this.session != null)
            this.session.sendServerMessage(messageId, args);
    }

    /**
     * Occurs when at least one of player parameters that are displayed in online list changes.
     */
    protected void onDisplayChanged()
    {
        World.getInstance().playerUpdated(this);
    }

    @Override
    public void update(int time)
    {
        super.update(time);

        // Client-Server Aura Synchronization
        // TODO: also recalculate aura bonus parameters
        if (this.syncTimer < 0)
        {
            // Update Heroic Shield effect
            if (!this.isInCombat())
                new org.meds.spell.Spell(1141, this, 1).cast();

            if (this.session != null && this.auras.size() != 0)
            {
                synchronized (this.auras)
                {
                    for(Aura aura : this.auras.values())
                    {
                        // Synchronize timed auras only!
                        if (aura.isPermanent())
                            continue;

                        this.session.send(aura.getPacketData());
                    }
                }
            }
            this.syncTimer = Player.SyncTime;
        }
        else
            this.syncTimer -= time;

        // Auto-Save
        if (this.saverTimer < 0)
        {
            Logging.Debug.log("Player save timer executing");
            save();
            this.saverTimer = SaveTime;
        }
        else
            this.saverTimer -= time;
    }
}
