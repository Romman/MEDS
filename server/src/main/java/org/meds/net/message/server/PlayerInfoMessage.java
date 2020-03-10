package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class PlayerInfoMessage implements ServerMessage {

    private final int id;
    private final String name;
    private final int avatarId;
    /**
     * Most likely the timestamp of the avatar image but not sure
     * TODO: Research and implement
     */
    private final int avatarDate = 1248860848;
    private final int race;
    private final int unk6 = 0; // generally the value is 0
    private final int clanId;
    private final int clanMemberStatus;
    /**
     * Clan bonus??
     */
    private final int unk9 = 0;
    /**
     * TODO: Skulls Count after Religion implementation
     */
    private final int skulls = 0;

    private final int baseInt;
    private final int baseCon;
    private final int baseStr;
    private final int baseDex;
    private final int guildCon;
    private final int guildStr;
    private final int guildDex;
    private final int guildInt;
    private final int guildDamage;
    private final int guildProtection;
    private final int guildChanceToHit;
    private final int guildArmour;
    private final int guildChanceToCast;
    private final int guildMagicDamage;
    private final int guildHealth;
    private final int guildMana;
    private final int guildHealthRegen;
    private final int guildManaRegen;
    private final int guildFireResistance;
    private final int guildFrostResistance;
    private final int guildLightningResistance;

    /**
     * Most likely a Star location of the player.
     * TODO: Research and implement;
     */
    private final int startCell = 9989;
    private final int settings;
    /**
     * TODO: Is it? Research and implement
     */
    private final int startServerTime = 1367478137;
    /**
     * TODO: Server Version? (current server version or what??)
     */
    private final int unk35 = 33620995;
    private final int inventoryCapacity;
    /**
     * ???
     * Always 0
     */
    private final int unk37 = 0;
    /**
     * TODO: Gender
     */
    private final int gender = 1;
    /**
     * TODO: Religious status
     */
    private final int religiousStatus = 0;

    /**
     * 0 or 2
     */
    private final int unk40 = 0;
    private final String unk41 = "";
    private final int unk42 = 0;

    public PlayerInfoMessage(int id, String name, int avatarId, int race, int clanId,
                             int clanMemberStatus,
                             int baseInt, int baseCon, int baseStr, int baseDex,
                             int guildCon, int guildStr, int guildDex, int guildInt, int guildDamage,
                             int guildProtection, int guildChanceToHit, int guildArmour, int guildChanceToCast,
                             int guildMagicDamage, int guildHealth, int guildMana, int guildHealthRegen,
                             int guildManaRegen, int guildFireResistance, int guildFrostResistance,
                             int guildLightningResistance, int settings,
                             int inventoryCapacity) {
        this.id = id;
        this.name = name;
        this.avatarId = avatarId;
        this.race = race;
        this.clanId = clanId;
        this.clanMemberStatus = clanMemberStatus;
        this.baseInt = baseInt;
        this.baseCon = baseCon;
        this.baseStr = baseStr;
        this.baseDex = baseDex;
        this.guildCon = guildCon;
        this.guildStr = guildStr;
        this.guildDex = guildDex;
        this.guildInt = guildInt;
        this.guildDamage = guildDamage;
        this.guildProtection = guildProtection;
        this.guildChanceToHit = guildChanceToHit;
        this.guildArmour = guildArmour;
        this.guildChanceToCast = guildChanceToCast;
        this.guildMagicDamage = guildMagicDamage;
        this.guildHealth = guildHealth;
        this.guildMana = guildMana;
        this.guildHealthRegen = guildHealthRegen;
        this.guildManaRegen = guildManaRegen;
        this.guildFireResistance = guildFireResistance;
        this.guildFrostResistance = guildFrostResistance;
        this.guildLightningResistance = guildLightningResistance;
        this.settings = settings;
        this.inventoryCapacity = inventoryCapacity;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.PlayerInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.id);
        stream.writeString(this.name);
        stream.writeInt(this.avatarId);
        stream.writeInt(this.avatarDate);
        stream.writeInt(this.race);
        stream.writeInt(this.unk6);
        stream.writeInt(this.clanId);
        stream.writeInt(this.clanMemberStatus);
        stream.writeInt(this.unk9);
        stream.writeInt(this.skulls);
        stream.writeInt(this.baseInt);
        stream.writeInt(this.baseCon);
        stream.writeInt(this.baseStr);
        stream.writeInt(this.baseDex);
        stream.writeInt(this.guildCon);
        stream.writeInt(this.guildStr);
        stream.writeInt(this.guildInt);
        stream.writeInt(this.guildDex);
        stream.writeInt(this.guildDamage);
        stream.writeInt(this.guildProtection);
        stream.writeInt(this.guildChanceToHit);
        stream.writeInt(this.guildArmour);
        stream.writeInt(this.guildChanceToCast);
        stream.writeInt(this.guildMagicDamage);
        stream.writeInt(this.guildHealth);
        stream.writeInt(this.guildMana);
        stream.writeInt(this.guildHealthRegen);
        stream.writeInt(this.guildManaRegen);
        stream.writeInt(this.guildFireResistance);
        stream.writeInt(this.guildFrostResistance);
        stream.writeInt(this.guildLightningResistance);
        stream.writeInt(this.startCell);
        stream.writeInt(this.settings);
        stream.writeInt(this.startServerTime);
        stream.writeInt(this.unk35);
        stream.writeInt(this.inventoryCapacity);
        stream.writeInt(this.unk37);
        stream.writeInt(this.gender);
        stream.writeInt(this.religiousStatus);
        stream.writeInt(this.unk40);
        stream.writeString(this.unk41);
        stream.writeInt(this.unk42);
    }
}
