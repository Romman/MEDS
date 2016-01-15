package org.meds;

import org.meds.net.ServerCommands;
import org.meds.net.ServerPacket;
import org.meds.util.ReadOnlyIterator;
import org.meds.util.Valued;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Group implements Iterable<Player> {

    public enum ClanAccessModes implements Valued {

        All(0),
        Clan(1),
        Alliance(2);

        private static ClanAccessModes[] values;

        static {
            values = new ClanAccessModes[ClanAccessModes.values().length];
            for (ClanAccessModes mode : ClanAccessModes.values()) {
                values[mode.value] = mode;
            }
        }

        public static ClanAccessModes parse(int value) {
            if (value < 0 || value >= values.length)
                return null;
            return values[value];
        }

        private final int value;

        private ClanAccessModes(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return Integer.toString(this.value);
        }
    }

    public enum TeamLootModes implements Valued {

        Regular(0, 308),
        Random(1, 310),
        Leader(2, 309);

        private static TeamLootModes[] values;

        static {
            values = new TeamLootModes[TeamLootModes.values().length];
            for (TeamLootModes mode : TeamLootModes.values()) {
                values[mode.getValue()] = mode;
            }
        }

        public static TeamLootModes parse(int value) {
            if (value < 0 || value >= TeamLootModes.values.length) {
                return null;
            }
            return values[value];
        }

        private final int value;
        private final int modeMessage;

        TeamLootModes(int value, int modeMessage) {
            this.value = value;
            this.modeMessage = modeMessage;
        }


        @Override
        public int getValue() {
            return this.value;
        }

        public int getModeMessage() {
            return this.modeMessage;
        }

        @Override
        public String toString() {
            return Integer.toString(this.value);
        }
    }

    private Player leader;
    private Set<Player> members;

    // Level access
    private int minLevel;
    private int maxLevel;

    // Religion access
    private boolean noReligionAllowed;
    private boolean sunAllowed;
    private boolean moonAllowed;
    private boolean orderAllowed;
    private boolean chaosAllowed;

    // Clan access
    private ClanAccessModes clanAccessMode;

    private boolean open;

    private TeamLootModes teamLootMode;

    private boolean disbanded;

    public Group(Player leader) {
        this.leader = leader;

        this.members = new HashSet<>();
        this.members.add(leader);

        this.minLevel = leader.getLevel() / 2;
        this.maxLevel = leader.getLevel() * 2;

        this.noReligionAllowed = true;
        this.sunAllowed = true;
        this.moonAllowed = true;
        this.orderAllowed = true;
        this.chaosAllowed = true;

        this.clanAccessMode = ClanAccessModes.All;

        this.open = true;
        this.teamLootMode = TeamLootModes.Regular;
    }

    public Player getLeader() {
        return leader;
    }

    public void setLeader(Player member) {
        if (this.disbanded)
            return;

        // The same leader
        if (member == this.leader)
            return;

        // Member is in this group
        if (!this.members.contains(member))
            return;

        // Change Visual parameters
        member.getPosition().unitVisualChanged(this.leader);
        member.getPosition().unitVisualChanged(member);
        this.leader = member;

        // Message to everyone about leader exchange
        // And current group relation
        ServerPacket message = new ServerPacket(ServerCommands.ServerMessage).add(279).add(member.getName());
        ServerPacket packet;
        for (Player groupMember : this) {
            if (groupMember.getSession() == null)
                continue;
            packet = new ServerPacket(
                    ServerCommands.GroupCreated)
                    .add(groupMember == this.leader ? "1" : "0")
                    .add(this.leader.getGuid());
            // Send group settings to the new leader
            if (groupMember == this.leader) {
                packet.add(this.getSettingsData());
            }
            groupMember.getSession().send(message).send(packet);
        }
    }

    public int getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        // There are no limitations in game
        if (minLevel >= 0 && minLevel <= this.leader.getLevel()) {
            this.minLevel = minLevel;
        }
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        // There are no limitations in game
        if (maxLevel >= this.leader.getLevel() && maxLevel <= 360) {
            this.maxLevel = maxLevel;
        }
    }

    public boolean isNoReligionAllowed() {
        return noReligionAllowed;
    }

    public void setNoReligionAllowed(boolean noReligionAllowed) {
        this.noReligionAllowed = noReligionAllowed;
    }

    public boolean isSunAllowed() {
        return sunAllowed;
    }

    public void setSunAllowed(boolean sunAllowed) {
        this.sunAllowed = sunAllowed;
    }

    public boolean isMoonAllowed() {
        return moonAllowed;
    }

    public void setMoonAllowed(boolean moonAllowed) {
        this.moonAllowed = moonAllowed;
    }

    public boolean isOrderAllowed() {
        return orderAllowed;
    }

    public void setOrderAllowed(boolean orderAllowed) {
        this.orderAllowed = orderAllowed;
    }

    public boolean isChaosAllowed() {
        return chaosAllowed;
    }

    public void setChaosAllowed(boolean chaosAllowed) {
        this.chaosAllowed = chaosAllowed;
    }

    public ClanAccessModes getClanAccessMode() {
        return clanAccessMode;
    }

    public void setClanAccessMode(ClanAccessModes clanAccessMode) {
        this.clanAccessMode = clanAccessMode;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public TeamLootModes getTeamLootMode() {
        return teamLootMode;
    }

    public void setTeamLootMode(TeamLootModes teamLootMode) {
        this.teamLootMode = teamLootMode;
    }

    public boolean join(Player newMember) {

        if (this.disbanded)
            return false;

        // Already in
        if (this.members.contains(newMember))
            return false;

        if (!this.isOpen())
            return false;

        // Level limitation
        if (newMember.getLevel() < this.getMinLevel() || newMember.getLevel() > this.getMaxLevel())
            return false;

        // Religion restriction
        switch (newMember.getReligion()) {
            case None:
                if (!this.isNoReligionAllowed())
                    return false;
                break;
            case Sun:
                if (!this.isSunAllowed())
                    return false;
                break;
            case Moon:
                if (!this.isMoonAllowed())
                    return false;
                break;
            case Order:
                if (!this.isOrderAllowed())
                    return false;
                break;
            case Chaos:
                if (!this.isChaosAllowed())
                    return false;
                break;
            default:
                return false;
        }

        // Clan restriction
        switch (this.getClanAccessMode()) {
            case All:
                // Anyone can join this group
                break;
            case Clan:
                if (newMember.getClanId() != leader.getClanId())
                    return false;
                break;
            case Alliance:
                // TODO: implement after Clan/Alliance implementation
                break;
        }

        // Notify the other members
        ServerPacket packet = new ServerPacket(ServerCommands.ServerMessage).add(268).add(newMember.getName());
        for (Player member : this) {
            if (member.getSession() != null) {
                member.getSession().send(packet);
            }
        }

        this.members.add(newMember);
        return true;
    }

    public boolean leave(Player member) {
        // Dummy
        if (this.disbanded)
            return true;

        if (!this.members.remove(member))
            return false;

        // Send to the left member
        if (member.getSession() != null) {
            // You left the group
            ServerPacket packet = new ServerPacket(ServerCommands.ServerMessage)
                    .add(1028)
                    // No group relation now
                    .add(ServerCommands.GroupCreated)
                    .add(0)
                    .add(0);
            member.getSession().send(packet);
        }

        // Send to remaining members
        ServerPacket packet = new ServerPacket(ServerCommands.ServerMessage)
                .add(269)
                .add(member.getName());
        // TODO: ServerCommands.GroupCreated is sent as well
        // when the player leaves by its own only (not kicked)
        for (Player groupMember : this) {
            if (groupMember.getSession() != null) {
                groupMember.getSession().send(packet);
            }
        }

        return true;
    }

    public void disband() {
        this.disbanded = true;

        ServerPacket packet = new ServerPacket(ServerCommands.ServerMessage).add(280);
        packet.add(ServerCommands.GroupCreated).add("0").add("0");
        for (Player member : this) {
            member.leaveGroup();
            if (member.getSession() != null) {
                member.getSession().send(packet);
            }
        }
    }

    public ServerPacket getSettingsData() {
        ServerPacket packet = new ServerPacket(ServerCommands.GroupSettings);
        packet.add(this.leader.getGuid())
                .add(this.minLevel)
                .add(this.maxLevel)
                .add(this.noReligionAllowed ? "1" : "0")
                .add(this.sunAllowed ? "1" : "0")
                .add(this.moonAllowed ? "1" : "0")
                .add(this.orderAllowed ? "1" : "0")
                .add(this.chaosAllowed ? "1" : "0")
                .add(this.clanAccessMode)
                .add(this.open ? "1" : "0");

        return packet;
    }

    public ServerPacket getTeamLootData() {
        ServerPacket packet = new ServerPacket(ServerCommands.TeamLoot);
        packet.add(this.teamLootMode);
        return packet;
    }

    @Override
    public Iterator<Player> iterator() {
        return new ReadOnlyIterator<>(this.members.iterator());
    }
}
