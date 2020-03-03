package org.meds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.data.domain.Spell;
import org.meds.database.repository.SpellRepository;
import org.meds.enums.*;
import org.meds.map.Location;
import org.meds.map.MapManager;
import org.meds.net.Session;
import org.meds.net.message.server.ChatMessage;
import org.meds.spell.Aura;
import org.meds.spell.Aura.States;
import org.meds.spell.SpellFactory;
import org.meds.util.KeyValuePair;
import org.meds.util.Random;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public abstract class Unit {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    protected SpellRepository spellRepository;
    @Autowired
    protected SpellFactory spellFactory;
    @Autowired
    protected MapManager mapManager;
    @Autowired
    protected World world;

    public interface TargetDiedListener {

        void unitTargetDied(Unit unit);
    }

    public static class DamageEvent extends EventObject {

        private Damage damage;
        private Unit victim;

        public DamageEvent(Unit source, Damage damage, Unit victim) {
            super(source);
            this.damage = damage;
            this.victim = victim;
        }

        @Override
        public Unit getSource() {
            return (Unit)super.getSource();
        }

        public Damage getDamage() {
            return this.damage;
        }

        public Unit getVictim() {
            return this.victim;
        }
    }

    public interface KillingBlowListener extends EventListener {
        void handleEvent(DamageEvent e);
    }

    public static class PositionEvent extends EventObject {

        private Location prevLocation;
        private Location newLocation;

        public PositionEvent(Unit source, Location prevLocation, Location nextLocation) {
            super(source);
            this.prevLocation = prevLocation;
            this.newLocation = nextLocation;
        }

        @Override
        public Unit getSource() {
            return (Unit) super.getSource();
        }

        public Location getPrevLocation() {
            return prevLocation;
        }

        public Location getNewLocation() {
            return newLocation;
        }
    }

    public interface PositionChangedListener extends EventListener {
        void handleEvent(PositionEvent event);
    }

    protected int id;
    protected Races race;

    protected UnitTypes unitType;

    protected int clanId;
    protected ClanMemberStatuses clanMemberStatus;

    protected UnitParameters parameters;

    protected int health;
    protected int mana;
    protected int healthManaRegenTact;

    protected KeyValuePair<Spell, Unit> combatSpell;
    protected KeyValuePair<Spell, Unit> effectSpell;

    protected Map<Integer, Aura> auras;

    protected DeathStates deathState;

    protected Map<Integer, Integer> skills;
    protected Map<Integer, Integer> spells;

    protected Unit target;

    protected Religions religion;

    /**
     * Current battle of the Unit. Do not set this value manually! Use setter.
     */
    protected Battle battle;

    protected Location position;

    private Set<TargetDiedListener> targetDiedListeners;

    private Set<KillingBlowListener> killingBlowListeners;

    private Set<PositionChangedListener> positionChangedListeners;

    private Map<Damage.ReductionTypes, Set<Damage.AffectionHandler>> damageReductions = new HashMap<>(Damage.ReductionTypes.values().length);

    public Unit() {
        this.id = 0;
        this.unitType = UnitTypes.Unit;
        this.race = Races.Generic;
        this.parameters = new UnitParameters(this);
        this.deathState = DeathStates.Alive;
        this.clanId = 0;
        this.clanMemberStatus = ClanMemberStatuses.Private;
        this.combatSpell = null;
        this.effectSpell = null;
        this.auras = new HashMap<>();
        this.skills = new HashMap<>();
        this.spells = new HashMap<>();
        this.religion = Religions.None;
        this.targetDiedListeners = new HashSet<>();
        this.killingBlowListeners = new HashSet<>();
        this.positionChangedListeners = new HashSet<>();
    }

    public void addTargetDiedListener(TargetDiedListener listener) {
        this.targetDiedListeners.add(listener);
    }

    public void removeTargetDiedListener(TargetDiedListener listener) {
        this.targetDiedListeners.remove(listener);
    }

    public void addKillingBlowListener(KillingBlowListener listener) {
        this.killingBlowListeners.add(listener);
    }

    public void removeKillingBlowListener(KillingBlowListener listener) {
        this.killingBlowListeners.remove(listener);
    }

    public void addPositionChangedListener(PositionChangedListener listener) {
        this.positionChangedListeners.add(listener);
    }

    public void removePositionChangedListener(PositionChangedListener listener) {
        this.positionChangedListeners.remove(listener);
    }

    public void addDamageReduction(Damage.ReductionTypes type, Damage.AffectionHandler handler) {
        Set<Damage.AffectionHandler> handlers = this.damageReductions.get(type);
        if (handlers == null) {
            handlers = new HashSet<>();
            this.damageReductions.put(type, handlers);
        }
        handlers.add(handler);
    }

    public void removeDamageReduction(Damage.ReductionTypes type, Damage.AffectionHandler handler) {
        Set<Damage.AffectionHandler> handlers = this.damageReductions.get(type);
        if (handlers == null) {
            return;
        }
        handlers.remove(handler);
    }

    public int getId() {
        return this.id;
    }

    public abstract int getAutoSpell();

    public abstract String getName();

    public abstract int getAvatar();

    public abstract int getSpellLevel(int spellId);

    public abstract int getSkillLevel(int skillId);

    public UnitTypes getUnitType() {
        return this.unitType;
    }

    public Races getRace() {
        return this.race;
    }

    public Religions getReligion() {
        return this.religion;
    }

    public DeathStates getDeathState() {
        return this.deathState;
    }

    public Location getPosition() {
        return this.position;
    }

    public void setPosition(Location location) {
        // Remove Relax statement
        removeAura(1000);
        Location prevLocation = this.position;
        this.position = location;

        if (prevLocation != null) {
            prevLocation.unitLeft(this);
        }
        if (location != null) {
            location.unitEntered(this);
        } else {
            logger.debug("{} has new NULL position", toString());
        }

        if (this.positionChangedListeners.size() > 0) {
            PositionEvent event = new PositionEvent(this, prevLocation, location);
            for (PositionChangedListener listener : this.positionChangedListeners) {
                listener.handleEvent(event);
            }
        }
    }

    public UnitParameters getParameters() {
        return this.parameters;
    }

    public int getHealth() {
        return this.health;
    }

    public void setHealth(int health) {
        this.health = health;
        onVisualChanged();
    }

    public void changeHealth(int diff) {
        this.setHealth(this.health + diff);
    }

    public int getMana() {
        return this.mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
        onVisualChanged();
    }

    public void changeMana(int diff) {
        this.setMana(this.mana + diff);
    }

    public void setHealthMana(int health, int mana) {
        this.health = health;
        this.mana = mana;
        onVisualChanged();
    }

    public Unit getTarget() {
        return this.target;
    }

    public void setTarget(Unit target) {
        // Target can be at unit location only
        if (target != null && target.position != this.position) {
            target = null;
        }

        // Set Target
        if (target != null) {
            // Unit is not in battle currently
            if (this.battle == null) {
                // Target is not in battle
                // Create new battle
                if (target.battle == null) {
                    this.setBattle(world.createBattle());
                }
                // Assign target's battle
                else {
                    this.setBattle(target.battle);
                }
            }
        }
        // Remove target == leave battle
        else {
            this.setBattle(null);
        }

        this.target = target;
    }

    public Battle getBattle() {
        return this.battle;
    }

    public void setBattle(Battle battle) {
        if (battle == this.battle)
            return;
        if (this.battle != null) {
            this.battle.leaveBattle(this);
        }
        if (battle != null) {
            // Remove Relax Aura
            this.removeAura(1000);
            battle.enterBattle(this);
        }

        this.battle = battle;
    }

    public int getClanId() {
        return this.clanId;
    }

    public ClanMemberStatuses getClanMemberStatus() {
        return this.clanMemberStatus;
    }

    public int create() {
        return this.id;
    }

    public abstract int getLevel();

    public abstract int getReligLevel();

    public boolean isPlayer() {
        return this.unitType == UnitTypes.Player;
    }

    public boolean isAlive() {
        return this.deathState == DeathStates.Alive;
    }

    public boolean isInCombat() {
        return this.battle != null;
    }

    public boolean canMove() {
        // TODO: Stan, overloading and other checks
        return this.battle == null;
    }

    public void runAway() {
        if (this.battle == null) {
            return;
        }

        // Send message "You panic and try to run away"
        Session session;
        if (this.isPlayer() && (session = ((Player)this).getSession()) != null) {
            session.send(new ChatMessage(254));
        }
        this.battle.runAway(this);
    }

    public void doBattleAttack() {
        if (this.target == null) {
            return;
        }

         // Target starts battle with this unit
        if (this.target.getTarget() == null && !this.target.isInCombat()) {
            this.target.setTarget(this);
        }

        /* Magic */
        if (this.getAutoSpell() != 0) {
            // This spell killed a target
            if (this.castSpell(this.getAutoSpell(), this.target) &&
                    (this.target == null || !this.target.isAlive())) {
                return;
            }
        }

        /* Physical Hit */
        int minDamage = this.parameters.value(Parameters.Damage);
        int maxDamage = this.parameters.value(Parameters.MaxDamage);
        logger.debug("{}: evaluating random damage from {} up to {}",
                this.toString(), minDamage, maxDamage);
        int initialDamage = Random.nextInt(minDamage, maxDamage);

        Damage damage = new Damage(initialDamage, this.target);
        damage.IsAutoAttack = true;
        this.dealDamage(damage);
    }

    public void calculateFinalDamage(Damage damage) {
        Unit target = damage.getTarget();
        if (target == null)
            return;

        /*
            Apply damage reduction sources
         */
        // Protection
        damage.FinalDamage -= target.parameters.value(Parameters.Protection);
        if (damage.FinalDamage <= 0)
            return;

        // Handle all the reductions with the specified order in Enum
        Set<Damage.AffectionHandler> affections;
        for (Damage.ReductionTypes affectionType : Damage.ReductionTypes.values()) {
            affections = target.damageReductions.get(affectionType);
            if (affections != null) {
                for (Damage.AffectionHandler affection : affections) {
                    if (affection.handle(damage)) {
                        return;
                    }
                }
            }
        }
    }

    public void dealDamage(Damage damage) {
        // No target - no damage
        if (damage.getTarget() == null) {
            return;
        }

        Unit victim = damage.getTarget();

        // Auto-attack
        // Is hit or miss
        if (damage.IsAutoAttack) {
            // Chance to Hit
            double hitChance = this.parameters.value(Parameters.ChanceToHit) * 3d / (this.parameters.value(Parameters.ChanceToHit) * 3 + this.target.parameters.value(Parameters.Armour));
            boolean isHit = Random.nextDouble() <= hitChance;

            if (!isHit) {
                this.addServerMessage(damage.MessageDealerMiss, victim.getName());
                victim.addServerMessage(damage.MessageVictimMiss, getName());

                this.position.send(this, victim,
                        new ChatMessage(damage.MessagePositionMiss, this.getName(), victim.getName()));

                return;
            }
        }

        this.calculateFinalDamage(damage);

        logger.debug("{} deals {} damages to {}. Victim's health: {}",
                this, damage.FinalDamage, victim, victim.getHealth());

        // No damage
        if (damage.FinalDamage <= 0) {
            addServerMessage(damage.MessageDealerNoDamage, victim.getName());
            victim.addServerMessage(damage.MessageVictimNoDamage, getName());

            this.position.send(this, victim,
                    new ChatMessage(damage.MessagePositionNoDamage, this.getName(), victim.getName()));

            return;
        }

        // Killing blow
        if (victim.getHealth() <= damage.getRealDamage()) {
            damage.IsFatal = true;
            addServerMessage(damage.MessageDealerKillingBlow, Integer.toString(damage.FinalDamage), victim.getName());
            victim.addServerMessage(damage.MessageVictimKillingBlow, this.getName(), Integer.toString(damage.FinalDamage));

            this.position.send(this, victim,
                    new ChatMessage(damage.MessagePositionKillingBlow, this.getName(), damage.FinalDamage, victim.getName()));

            Corpse corpse = victim.die();

            this.setTarget(null);

            DamageEvent damageEvent = new DamageEvent(this, damage, victim);
            if (this.killingBlowListeners.size() != 0) {
                for (KillingBlowListener listener : this.killingBlowListeners) {
                    listener.handleEvent(damageEvent);
                }
            }

            // The killer is a Player
            if (this.unitType == UnitTypes.Player) {
                Player player = (Player)this;

                // AutoLoot
                if (corpse != null && player.getSettings().has(PlayerSettings.AutoLoot))
                    player.lootCorpse(corpse);
            }
        }
        // Ordinary hit.
        else {
            victim.changeHealth(-damage.getRealDamage());

            addServerMessage(damage.MessageDealerDamage, victim.getName(), Integer.toString(damage.FinalDamage));
            victim.addServerMessage(damage.MessageVictimDamage, getName(), Integer.toString(damage.FinalDamage));
            this.position.send(this, victim,
                    new ChatMessage(damage.MessagePositionDamage, this.getName(), victim.getName(), damage.FinalDamage));
        }
    }

    public abstract Corpse die();

    public void useMagic(int spellId, int targetId) {
        Spell entry = spellRepository.get(spellId);
        if (entry == null) {
            return;
        }

        Unit target = world.getUnit(targetId);
        if (target == null) {
            target = this;
        }

        if (entry.getType() == SpellTypes.Combat) {
            this.combatSpell = new KeyValuePair<>(entry, target);
        } else {
            this.effectSpell = new KeyValuePair<>(entry, target);
        }
    }

    /**
     * Adds the specified aura to the unit aura list.
     * @param aura Aura instance to add (or update)
     * @return A result whether the aura has been added/updated or not.
     */
    public boolean addAura(Aura aura) {
        // An aura with the same spell is already applied
        Aura currentAura = getAura(aura.getSpellEntity().getId());
        if (currentAura != null) {
            // Identical levels
            if (currentAura.getLevel() == aura.getLevel()) {
                // Nothing to do with isPermanent auras
                // Timed auras should be updated (Current implementation: forceRemove => apply again)
                if (currentAura.isPermanent()) {
                    return false;
                } else {
                    removeAura(currentAura.getSpellEntity().getId(), true);
                }
            // Current level is higher then the new level => Send 'no effect' message
            } else if (currentAura.getLevel() > aura.getLevel()) {
                if (this.isPlayer() && ((Player)this).getSession() != null)
                    ((Player)this).getSession().send(new ChatMessage(301));
                return false;
            // Otherwise, just update the aur
            } else {
                removeAura(currentAura.getSpellEntity().getId(), true);
            }
        }

        this.auras.put(aura.getSpellEntity().getId(), aura);
        aura.setState(Aura.States.Active);

        // Send aura data;
        if (this.isPlayer() && ((Player)this).getSession() != null)
            ((Player)this).getSession().send(aura.getBonusMessages());
        return true;
    }

    /**
     * Marks an aura with AuraStates.Removed. The aura will be removed from an aura list at the next Unit.Update
     * @param spellId ID of a Spell that is the source of the aura.
     */
    public void removeAura(int spellId) {
        removeAura(spellId, false);
    }

    /**
     * Remove an aura with the specified Spell Id and by the specified method
     * @param spellId ID of the Spell that is the source of the aura.
     * @param force Whether the Aura removes usual (dispelled or expired) or just cancel effect of the Aura
     *              without any messaging and triggering.
     */
    public void removeAura(int spellId, boolean force) {
        Aura aura = this.auras.get(spellId);
        if (aura == null)
            return;

        if (force) {
            aura.forceRemove();
            this.auras.remove(aura);
        } else {
            aura.setState(Aura.States.Removed);
        }
    }

    public Aura getAura(int spellId) {
        return this.auras.get(spellId);
    }

    public boolean hasAura(int spellId) {
        return this.auras.containsKey(spellId);
    }

    public boolean castSpell(int spellId) {
        return this.castSpell(spellId, null);
    }

    public boolean castSpell(int spellId, Unit target) {
        int level = this.getSpellLevel(spellId);
        if (level == 0) {
            return false;
        }
        return spellFactory.create(spellId, this, level, target).cast();
    }

    protected void onVisualChanged() {
        if (this.position != null)
            this.position.unitVisualChanged(this);
    }

    public void addServerMessage(int messageId, String... args) { }

    public void update(int time) {
         // Handle unit's auras
        synchronized (this.auras) {
            if (this.auras.size() > 0) {
                List<Aura> removedAuras = new ArrayList<>(this.auras.size());
                // Update auras
                for (Aura aura : this.auras.values()) {
                    aura.update(time);
                    if (aura.getState() == States.Removed)
                        removedAuras.add(aura);
                }

                if (removedAuras.size() > 0) {
                    for (Aura aura : removedAuras) {
                        this.auras.remove(aura.getSpellEntity().getId());
                    }
                }
            }
        }

        // Next action might be with alive units only
        if (this.deathState == DeathStates.Dead)
            return;

        if (this.effectSpell != null && this.effectSpell.getKey() != null) {
            this.castSpell(this.effectSpell.getKey().getId(), this.effectSpell.getValue());

            this.effectSpell = new KeyValuePair<>(null, null);
        }

        // HealthMana regeneration
        if (this.healthManaRegenTact >= 4) {
            if (getHealth() < this.parameters.value(Parameters.Health) || getMana() < this.parameters.value(Parameters.Mana)) {
                int healthRegen = isInCombat() ? this.parameters.value(Parameters.HealthRegeneration) / 2 : this.parameters.value(Parameters.HealthRegeneration);
                int manaRegen = isInCombat() ? this.parameters.value(Parameters.ManaRegeneration) / 2 : this.parameters.value(Parameters.ManaRegeneration);
                // Triple bonus at star
                if (this.position.getSpecialLocationType() == SpecialLocationTypes.Star) {
                    healthRegen *= 3;
                    manaRegen *= 3;
                }

                int newHealth = this.getHealth() + healthRegen;
                int newMana = this.getMana() + manaRegen;

                if (newHealth > this.parameters.value(Parameters.Health)) {
                    newHealth = this.parameters.value(Parameters.Health);
                }

                if (this.getMana() + manaRegen > this.parameters.value(Parameters.Mana)) {
                    newMana = this.parameters.value(Parameters.Mana);
                }

                this.setHealthMana(newHealth, newMana);
            }
            healthManaRegenTact = 0;
        } else {
            ++healthManaRegenTact;
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

        Unit unit = (Unit) o;

        return this.id == unit.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public String toString() {
        return String.format("%s \"%s\" (ID: %d)", this.unitType.toString(), this.getName(), this.getId());
    }
}
