package org.meds.net.message;

public enum ServerMessageIdentity implements MessageIdentity {

    Aura("a"),
    AchievementList("acl"),
    AchievementUpdate("acu"),
    BuyBackList("bbl"), // Appeared in 2.0.0.0
    TradeUpdate("change"),
    TradeResult("ch_res"),
    CultInfo("ci"),
    BonusMagicParameter("cms"),
    UpdateQuest("cq"),
    _cs("cs"),
    DeleteAura("d"),
    DayTime("dn"),
    EquipmentInfo("eq"),
    Experience("exp"),
    _fex("fex"),
    _fpi("fpi"),
    GroupCreated("gcr"),
    GetTrade("get_ch"),
    GuildInfo("gi"),
    @Deprecated
    GuildLessonInfo("ginf"), // Deprecated after 2.0.0.0
    GroupSettings("glvl"),
    GuildLessonsInfo("gni"), // Appeared in 2.0.0.0 after new guild system.
    GetCorpse("gtr"),
    _hoi("hoi"),
    Health("hp"),
    _hs("hs"),
    ItemInfo("ii"),
    Inn("inn"),
    InventoryUpdate("inu"),
    InventoryInfo("inv"),
    _invt("invt"),
    ClanInfo("kinf"),
    Currencies("l$"), // Appeared in 1.2.7.6
    PlayersListAdd("la"),
    PlayersListDelete("ld"),
    OnlineList("lf"),
    LocationInfo("li"),
    _lh0("lh0"),
    LearnGuildInfo("lni"), // Appeared in 2.0.0.0 after new guild system.
    Location("loc"),
    LoginResult("login_result"),
    QuestListInfo("lq"),
    PlayersListUpdate("lu"),
    GuildLevels("lvl"),
    AutoSpell("mb"),
    Notepad("mem"),
    ChatMessage("mes"),
    MagicInfo("mi"),
    SocialChatMessage("msg"),
    MessageList("msl"),
    NoGo("ng"),
    _omg("omg"),
    PlayerLocation("pl"), // Appeared in 2.0.0.0
    PlayersLocation("pll"), // Appeared in 2.0.0.0
    Professions("prof"),
    _prot1("prot1"),
    _prot2("prot2"),
    PositionUnitList("pss"),
    MoneyInfo("$"), // No longer exists after 1.2.7.6 (See l$ command instead)
    QuestInfo("q"),
    QuestFinalText("qft"),
    NpcQuestList("qli"),
    QuestListRegion("qlr"),
    RegionLocations("r"),
    RelaxOff("r0"),
    RelaxOn("r1"),
    StarInfo("rcity"),
    _s0("s0"),
    SkillInfo("si"),
    ShopInfo("sinf"),
    Sound("snd"),
    QuestInfoForAccept("sq"),
    PlayerInfo("sti"),
    _swf("swf"),
    ServerTime("t"),
    _tc("tc"),
    CorpseList("tl"),
    TeamLoot("tlt"),
    Version("version"),
    Currency("w$"), // Appeared in 1.2.7.6
    BattleState("war"),
    _wg("wg"),
    _zzz("zzz");

    private final String identity;

    ServerMessageIdentity(String identity) {
        this.identity = identity;
    }

    @Override
    public String identity() {
        return this.identity;
    }

    /**
     * TODO: Remove this method prior {@link #identity()} usages.
     *  This method should have standard Enum implementation and must be used for debugging purposes only.
     */
    @Override
    public String toString() {
        return this.identity;
    }
}
