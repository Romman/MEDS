package org.meds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.data.domain.CreatureLoot;
import org.meds.data.domain.CreatureTemplate;
import org.meds.database.BiRepository;
import org.meds.database.Repository;
import org.meds.enums.*;
import org.meds.item.Item;
import org.meds.item.ItemFactory;
import org.meds.map.Location;
import org.meds.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
@Scope("prototype")
public class Creature extends Unit {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    private Locale locale;
    @Autowired
    private BiRepository<CreatureLoot> creatureLootRepository;
    @Autowired
    private Repository<CreatureTemplate> creatureTemplateRepository;
    @Autowired
    private ItemFactory itemFactory;

    /**
     * Creature Respawn time after death. In milliseconds.
     */
    public final static int RespawnTime = 60000; // TODO: Determine the exact values of respawn timer

    private Set<Item> loot;
    private int cashGold;

    private int respawnTimer;

    private Location spawnLocation;

    private org.meds.data.domain.Creature entry;

    private CreatureTemplate template;
    private int autoSpell;

    private String fullName;

    private CreatureBossTypes bossType;

    private int maxGoldValue;
    private int minGoldValue;

    private Map<Integer, Integer> spells;
    private Map<Integer, Integer> skills;

    public Creature(org.meds.data.domain.Creature entry) {
        super();
        this.id = entry.getId();
        this.entry = entry;

        this.unitType = UnitTypes.Creature;
        this.spells = new HashMap<>();
        this.skills = new HashMap<>();
        this.loot = new HashSet<>();
        this.bossType = CreatureBossTypes.Normal;
    }

    @PostConstruct
    private void init() {
        this.spawnLocation = mapManager.getLocation(this.entry.getLocationId());
    }

    public CreatureTemplate getTemplate() {
        return this.template;
    }

    public int getTemplateId() {
        return this.entry.getTemplateId();
    }

    @Override
    public String getName() {
        return this.fullName;
    }

    @Override
    public int getLevel() {
        return this.template.getLevel();
    }

    @Override
    public int getReligLevel() {
        return 0;
    }

    @Override
    public int getAvatar() {
        return this.template.getAvatarId();
    }

    @Override
    public int getAutoSpell() {
        return this.autoSpell;
    }

    @Override
    public int getSkillLevel(int skillId) {
        Integer level = this.skills.get(skillId);
        if (level == null)
            return 0;
        return level;
    }

    @Override
    public int getSpellLevel(int spellId) {
        Integer level = this.spells.get(spellId);
        if (level == null)
            return 0;
        return level;
    }

    public CreatureBossTypes getBossType() {
        return bossType;
    }

    public int getCashGold() {
        return this.cashGold;
    }

    public int getMaxGoldValue() {
        return this.maxGoldValue;
    }

    public int getMinGoldValue() {
        return this.minGoldValue;
    }

    public boolean locationBound() {
        return this.spawnLocation != null;
    }

    public CreatureTypes getCreatureType() {
        return world.getCreatureType(this.getTemplateId());
    }

    @Override
    public int create() {
        this.template = creatureTemplateRepository.get(this.getTemplateId());

        if (this.template == null)
            return 0;

        calculateParameters();
        calculateCashRange();

        this.health = this.parameters.value(Parameters.Health);
        this.mana = this.parameters.value(Parameters.Mana);

        // TODO: additional parameters, spells and skills according to the creature current type

        this.constructName();

        world.unitCreated(this);

        return this.id;
    }

    private void calculateParameters() {

         /*
         * CREATURE STATS:
         *
         * Base stats (Constitution, Strength, Dexterity, Intelligence):
         * 8 + Level / 6 => Floor
         *
         * Health:
         * Level * Level / 4 => Floor (Under 70 Level)
         * Level * Level / 3 => Floor (Above 70 Level)
         *
         * Mana: ???
         * Level * Level * Level * 0.000105 + Level * Level * 0.025 + Level * 0.3 + 40
         *
         * Health Regeneration: ???
         * Level * Level *0.012 + Level * 0.1 + 4 => Floor (Below level 70)
         * Level * Level *0.016 + Level * 0.1 + 4 => Floor (Above level 70)
         *
         * Mana Regeneration ????
         * Level * Level * Level * 0.000013 + Level * Level * 0.002 + Level * 0.1 + 7
         *
         * MinDamage:
         * Level * Level * 0.015  + Level - 12 => Floor
         *
         * MaxDamage:
         * Level * Level * 0.03 + Level * 12 - 12 => Floor
         *
         * Magic Damage:
         * Level * Level * 0.0226 + Level + 1 => Floor
         *
         * Protection:
         * Level * Level * 0.001 + Level / 22 - 11
         *
         * Armour:
         *   Level * Level * 0.03 + 3 * Level => Floor
         * - Religious (Sun, Moon, Order, Chaos)
         *   x 1
         * - Monk, Mauler, Barbarian, Swordsman, Vindicator, Assassin, Duelist
         *   x 2
         * - Elemental (Cold, Fire, Lightning)
         *   x 0.5
         *
         * Chance To Hit:
         *   Level * Level * 0.0075 + Level * 0.75 => Ceiling
         *
         * Chance To Cast:
         *   Level * Level * 0.0075 + Level * 0.75 => Ceiling
         * - Swordsman, Mauler, Barbarian
         *   x 0.5
         *
         * Resistance
         *   Level * Level * 0.03 + Level * 3
         * - Elemental
         *   x 2
         * - Vindicator, Monk, Assassin, Duelist
         *   x 0.5
         *
         * */

        int level = getLevel();
        this.parameters = new UnitParameters(this);

        this.parameters.base().value(Parameters.Constitution, 8 + level / 6);
        this.parameters.base().value(Parameters.Strength, 8 + level / 6);
        this.parameters.base().value(Parameters.Dexterity, 8 + level / 6);
        this.parameters.base().value(Parameters.Intelligence, 8 + level / 6);

        int bossMultiplier = 1;
        if (this.template.hasFlag(CreatureFlags.Unique)) {
            bossMultiplier = 11;
        }
        switch (this.bossType) {
            case DeathAura:
            case Freeze:
            case AbsorbAura:
            case Vampire:
                bossMultiplier = 2;
                break;
            case Champion:
                bossMultiplier = 4;
                break;
            case Possessed:
                bossMultiplier = 11;
                break;
        }
        int bonusHp;
        if (level < 71) {
            bonusHp = level * level / 4;
        } else {
            bonusHp = level * level / 3;
        }
        if (bossMultiplier > 1) {
            bonusHp = (bonusHp + this.parameters.base().value(Parameters.Health)) * bossMultiplier;
        }
        this.parameters.guild().value(Parameters.Health, bonusHp);

        this.parameters.guild().value(Parameters.Mana, (int) (level * level * level * 0.000105 + level * level * 0.025 + level * 0.3 + 40));
        this.parameters.guild().value(Parameters.HealthRegeneration, (int) (level * level * 0.012 + level * 0.1 + 4));
        this.parameters.guild().value(Parameters.ManaRegeneration, (int) (level * level * level * 0.000013 + level * level * 0.002 + level * 0.1 + 7));
        this.parameters.guild().value(Parameters.MinDamage, level * level / 67 + level - 12);
        this.parameters.guild().value(Parameters.MaxDamage, level * level / 34 + 2 * level - 12);
        this.parameters.guild().value(Parameters.MagicDamage, (int) (level * level * 0.0226 + level + 1));
        this.parameters.guild().value(Parameters.Armour, (int) (level * level * 0.03 + level * 3)); // Fighters *2, Elemental *0.5);
        this.parameters.guild().value(Parameters.Protection, (int) (level * level * 0.001 + level / 22d) - 11);
        this.parameters.guild().value(Parameters.ChanceToHit, (int) (level * level * 0.0075 + level * 0.75));
        this.parameters.guild().value(Parameters.ChanceToCast, (int) (level * level * 0.0075 + level * 0.75));
        this.parameters.guild().value(Parameters.AllResistance, (int) (level * level * 0.03 + level * 3));
        // TODO: estimate and add the other parameters coeffs
    }

    private void calculateCashRange() {

        double bossMultiplier = 1d;
        if (this.template.hasFlag(CreatureFlags.Unique)) {
            bossMultiplier = 10d;
        }
        switch (this.bossType) {
            case Champion:
            case Vampire:
            case AbsorbAura:
            case Freeze:
                bossMultiplier = 1.2d;
                break;
            case Spectral:
            case DeathAura:
                bossMultiplier = 1.3d;
                // TODO: Determine other multipliers
        }

        // TODO: Chaos Zone and something else affect the gold amount
        this.minGoldValue = (int) (bossMultiplier * this.getLevel() / 2); // 50% of the creature level * multiplier
        this.maxGoldValue = (int) (bossMultiplier * (this.getLevel() + 1) * 3 / 2); // 150% of the creature level * multiplier
    }

    private void generateBossType() {

        // Creatures above level 50 (Not sure)
        if (this.getLevel() < 50)
            return;

        // Except Unique Creatures
        if (this.template.hasFlag(CreatureFlags.Unique))
            return;

        // 5% chance to be a boss
        if (Random.nextInt(100) >= 5) {
            this.bossType = CreatureBossTypes.Normal;
            return;
        }

        // According to its type
        List<CreatureBossTypes> possibleTypes = new ArrayList<>(CreatureBossTypes.values().length);

        // Can be anyone
        possibleTypes.add(CreatureBossTypes.DeathAura);
        possibleTypes.add(CreatureBossTypes.Possessed);
        possibleTypes.add(CreatureBossTypes.Spectral);
        possibleTypes.add(CreatureBossTypes.AbsorbAura);
        possibleTypes.add(CreatureBossTypes.Freeze);

        // Religion Cultists
        CreatureTypes creatureType = this.getCreatureType();
        if (creatureType == CreatureTypes.MoonCultist ||
                creatureType == CreatureTypes.SunCultist ||
                creatureType == CreatureTypes.OrderAdept ||
                creatureType == CreatureTypes.ChaosAdept) {
            possibleTypes.add(CreatureBossTypes.Fanatic);
        }

        // Mages
        if (creatureType == CreatureTypes.FireAdept ||
                creatureType == CreatureTypes.ColdAdept ||
                creatureType == CreatureTypes.LightningAdept) {
            possibleTypes.add(CreatureBossTypes.Elementalist);
        }
        // Not mages
        else {
            possibleTypes.add(CreatureBossTypes.Champion);
        }

        this.bossType = possibleTypes.get(Random.nextInt(possibleTypes.size()));
    }

    /**
     * Spawn the creature at its location(bound or random) with random loot and money account.
     */
    public void spawn() {
        this.deathState = DeathStates.Alive;

        CreatureBossTypes oldType = this.bossType;
        generateBossType();
        // New Boss Type
        if (oldType != this.bossType) {
            calculateParameters();
            calculateCashRange();
            constructName();
        }

        Location location;
        if (this.locationBound()) {
            location = this.spawnLocation;
        } else {
            // Find random location at creature's region
            location = mapManager.getRegion(this.template.getRegionId()).getRandomLocation(false);
        }

        if (location == null) {
            logger.error("{} (Entry={}) was not spawned. Location not found or not specified.",
                    toString(), this.getTemplateId());
            return;
        }

        this.setPosition(location);
        // Add Loot
        this.loot.clear();
        Collection<CreatureLoot> loot = creatureLootRepository.get(this.getTemplateId());
        for (CreatureLoot creatureLoot : loot) {
            if (Random.nextDouble() * 100 > creatureLoot.getChance())
                continue;
            Item item = itemFactory.create(creatureLoot.getItemTemplateId(), creatureLoot.getCount());
            item.getModification().generateTotemicType();
            this.loot.add(item);
        }

        if (!this.template.hasFlag(CreatureFlags.Beast))
            this.cashGold = Random.nextInt(this.minGoldValue, this.maxGoldValue);

        this.health = this.parameters.value(Parameters.Health);
        this.mana = this.parameters.value(Parameters.Mana);
    }

    @Override
    public Corpse die() {
        Corpse corpse;
        this.deathState = DeathStates.Dead;
        this.respawnTimer = RespawnTime;
        if (this.cashGold == 0 && this.loot.size() == 0)
            corpse = null;
        else {
            corpse = new Corpse(mapManager.getNextCorpseId(), this);
            corpse.fillWithLoot(this.loot, this.cashGold);
        }

        this.setPosition(null);
        return corpse;
    }

    private void constructName() {
        StringBuilder nameBuilder = new StringBuilder(this.template.getName());
        CreatureTypes type = getCreatureType();
        if (type != CreatureTypes.Normal)
            nameBuilder.append("(").append(locale.getString(type.getTitleStringId())).append(")");
        if (this.template.hasFlag(CreatureFlags.Unique)) {
            nameBuilder.append("(").append(locale.getString(33)).append(")");
        }
        if (this.bossType != CreatureBossTypes.Normal) {
            nameBuilder.append("(").append(locale.getString(this.bossType.getTitleStringId())).append(")");
        }
        this.fullName = nameBuilder.toString();
    }

    public Iterator<Item> getLootIterator() {
        return this.loot.iterator();
    }

    @Override
    public void update(int time) {
        super.update(time);

        switch (this.deathState) {
            case Alive:
                if (this.template.hasFlag(CreatureFlags.LocationBind))
                    break;
                if (this.isInCombat())
                    break;
                // 10% chance for motion
                if (Random.nextDouble() < 0.05) {
                    MovementDirections direction = this.position.getRandomDirection(false, true);
                    if (direction != MovementDirections.None) {
                        mapManager.registerMovement(this, direction);
                    }
                }
                break;
            case Dead:
                if (this.respawnTimer < 0) {
                    spawn();
                } else {
                    this.respawnTimer -= time;
                }
                break;
        }
    }
}
