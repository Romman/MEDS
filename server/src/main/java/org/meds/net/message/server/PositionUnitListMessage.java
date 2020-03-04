package org.meds.net.message.server;

import java.util.Collection;
import java.util.Collections;
import org.meds.enums.CreatureBossTypes;
import org.meds.enums.Religions;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class PositionUnitListMessage implements ServerMessage {

    private final Collection<UnitInfo> units;

    public PositionUnitListMessage(Collection<UnitInfo> units) {
        this.units = units;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.PositionUnitList;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.units.size());
        for (UnitInfo unit : this.units) {
            unit.serialize(stream);
        }
    }

    public static class UnitInfo {

        private final String name;
        private final int id;
        private final int avatar;
        private final int health;
        private final int level;
        private final Religions religion;
        private final int religionLevel;
        private final boolean isGroupLeader;
        private final int groupLeaderId;
        private final int targetId;
        /**
         * Avatar time?
         */
        private final int unk11 = 1212413397;
        private final int mana;

        /**
         * TODO: Implement the following fields
         */
        private final int clanId = 0;
        private final int clanStatus = 0;
        private final boolean isHidden = false;
        private final boolean outOfLaw = false;
        /**
         * Looks like not a gender
         * Mostly 0
         * Found a player with 101
         */
        private final int gender = 1;
        private final int title = 0;
        private final boolean isPet = false;
        private final CreatureBossTypes bossType;

        private final int unk21 = 0;
        private final String unk22 = "";
        private final int unk23 = 0;

        public UnitInfo(String name, int id, int avatar, int health, int level, Religions religion, int religionLevel,
                        boolean isGroupLeader, int groupLeaderId, int targetId, int mana, CreatureBossTypes bossType) {
            this.name = name;
            this.id = id;
            this.avatar = avatar;
            this.health = health;
            this.level = level;
            this.religion = religion;
            this.religionLevel = religionLevel;
            this.isGroupLeader = isGroupLeader;
            this.groupLeaderId = groupLeaderId;
            this.targetId = targetId;
            this.mana = mana;
            this.bossType = bossType;
        }

        private void serialize(MessageWriteStream stream) {
            stream.writeString(this.name);
            stream.writeInt(this.id);
            stream.writeInt(this.avatar);
            stream.writeInt(this.health);
            stream.writeInt(this.level);
            stream.writeInt(this.religion.getValue());
            stream.writeInt(this.religionLevel);
            stream.writeBoolean(this.isGroupLeader);
            stream.writeInt(this.groupLeaderId);
            stream.writeInt(this.targetId);
            stream.writeInt(this.unk11);
            stream.writeInt(this.mana);
            stream.writeInt(this.clanId);
            stream.writeInt(this.clanStatus);
            stream.writeBoolean(this.isHidden);
            stream.writeBoolean(this.outOfLaw);
            stream.writeInt(this.gender);
            stream.writeInt(this.title);
            stream.writeBoolean(this.isPet);
            stream.writeInt(this.bossType.getValue());
            stream.writeInt(this.unk21);
            stream.writeString(this.unk22);
            stream.writeInt(this.unk23);
        }
    }
}
