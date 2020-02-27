package org.meds.spell;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.meds.Player;
import org.meds.data.domain.Spell;
import org.meds.net.ServerCommands;
import org.meds.net.ServerPacket;
import org.meds.Unit;

public class Aura {

    public enum States {
        /**
         * An aura just has been created and is yet not applied to a target.
         */
        Created,
        /**
         * Unit is under effect of an aura
         */
        Active,
        /**
         * Still active but remained 1 minute.
         */
        Ending,
        /**
         * An aura no longer affects a target
         */
        Removed,
    }

    private static Logger logger = LogManager.getLogger();

    private static final Map<Integer, Class<? extends Aura>> customAuraClasses = new HashMap<>();

    static {
        customAuraClasses.put(14, AuraBuff.class); // Tigers Strength
        customAuraClasses.put(16, AuraBuff.class); // Steel Body
        customAuraClasses.put(17, AuraBuff.class); // Feline Grace
        customAuraClasses.put(18, AuraBuff.class); // Bears Blood
        customAuraClasses.put(36, AuraShield.class); // Layered Defense
        customAuraClasses.put(37, AuraBuff.class); // Wisdom of the Owl
        customAuraClasses.put(1000, AuraRelax.class); // Relax
        customAuraClasses.put(1141, AuraShield.class); // Heroic Shield
    }

    protected int level;
    protected int remainingTime;
    protected Unit owner;
    protected Player ownerPlayer;
    protected org.meds.data.domain.Spell spellEntry;
    protected States state;
    protected boolean minuteLeft;
    protected boolean isPermanent;

    /**
     * Creates a new instance of an Aura class.
     *
     * @param entry    Entry of the related spell.
     * @param owner    A unit who is target of the aura
     * @param level    Level of the aura.
     * @param duration Duration in milliseconds. If duration is negative the the aura is isPermanent.
     * @return A new Aura instance for this Spell
     */
    public static Aura createAura(Spell entry, Unit owner, int level, int duration) {
        Aura aura = null;
        Class<? extends Aura> auraClass = Aura.customAuraClasses.get(entry.getId());
        // Custom aura Class

        if (auraClass != null) {
            try {
                aura = auraClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error(
                        new ParameterizedMessage("Can not instantiate the Aura class {} for spell ID {}",
                                auraClass.getName(), entry.getId()), e);
            }
        }
        // Generic (this) aura class
        else {
            aura = new Aura();
        }

        aura.spellEntry = entry;
        aura.owner = owner;
        if (owner.isPlayer())
            aura.ownerPlayer = (Player) owner;
        aura.level = level;
        aura.remainingTime = duration;
        aura.isPermanent = duration < 0;
        aura.state = States.Created;
        aura.minuteLeft = false;

        return aura;
    }

    protected Aura() {

    }

    public Spell getSpellEntity() {
        return this.spellEntry;
    }

    public int getLevel() {
        return this.level;
    }

    protected void setLevel(int level) {
        this.level = level;
        if (this.ownerPlayer != null && this.ownerPlayer.getSession() != null)
            this.ownerPlayer.getSession().send(getPacketData());
    }

    public int getRemainingTime() {
        return this.remainingTime;
    }

    public States getState() {
        return this.state;
    }

    public void setState(States state) {
        switch (state) {
            case Created:
                break;
            case Active:
                applyAura();
                break;
            case Ending:
                if (this.owner.isPlayer()) {
                    Player player = (Player) this.owner;
                    if (player.getSession() != null)
                        player.getSession().sendServerMessage(1529, this.spellEntry.getName());
                }
                break;
            case Removed:
                removeAura();
                break;
        }

        this.state = state;
    }

    public boolean isPermanent() {
        return this.isPermanent;
    }

    protected void applyAura() {

    }

    public void refresh(int level, int time) {
        this.level = level;
        this.remainingTime = time;
        this.minuteLeft = false;
        setState(States.Active);
    }

    protected void removeAura() {
        // If a player - send result
        if (this.ownerPlayer != null && this.ownerPlayer.getSession() != null) {
            this.ownerPlayer.getSession().send(new ServerPacket(ServerCommands.DeleteAura).add(this.spellEntry.getId()));
        }
    }

    public ServerPacket getPacketData() {
        return new ServerPacket(ServerCommands.Aura)
                .add(this.spellEntry.getId())
                .add(this.level)
                .add(this.isPermanent ? "-1" : this.remainingTime / 1000);
    }

    /**
     * Removes the aura effect without any messages or triggering any events. Just cancels it.
     */
    public void forceRemove() {
        // If the owner is a player => send result
        if (this.ownerPlayer != null && this.ownerPlayer.getSession() != null) {
            this.ownerPlayer.getSession().send(new ServerPacket(ServerCommands.DeleteAura).add(this.spellEntry.getId()));
        }
    }

    public void update(int time) {
        if (this.isPermanent)
            return;

        // Only active auras may be updated
        if (this.state == States.Created || this.state == States.Removed)
            return;

        this.remainingTime -= time;

        if (this.remainingTime < 0)
            this.setState(States.Removed);
        else if (this.remainingTime < 60000 && this.state == States.Active) {
            this.setState(States.Ending);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Aura aura = (Aura) o;

        return this.owner.equals(aura.owner)
                && this.spellEntry.equals(aura.spellEntry);
    }

    @Override
    public int hashCode() {
        return this.owner.hashCode() + this.spellEntry.hashCode();
    }
}
