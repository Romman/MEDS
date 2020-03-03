package org.meds.spell;

import org.meds.*;
import org.meds.enums.Parameters;
import org.meds.item.Item;
import org.meds.item.ItemBonusParameters;
import org.meds.item.ItemClasses;
import org.meds.item.ItemSubClassWeapon;
import org.meds.net.message.server.ChatMessage;
import org.meds.util.Random;

public class Spell {

    private org.meds.data.domain.Spell entry;
    private Unit caster;
    private Unit target;
    private int level;
    private Item item;

    public Spell(org.meds.data.domain.Spell entry, Unit caster, int level, Unit target, Item item) {
        this.entry = entry;
        this.caster = caster;
        this.level = level;
        this.target = target;
        this.item = item;
    }

    public boolean cast() {
        if (this.entry == null) {
            return false;
        }

        switch (this.entry.getId()) {
            // Frost, Fire and Lightning magic spells
            case 1: // Fire Ball
            case 2: // Ice Arrow
            case 3: // Electroshock
                handleSpellBattleMagic();
                break;
            case 14: // Tigers Strength
            case 16: // Steel Body
            case 17: // Bears Blood
            case 18: // Feline Grace
            case 37: // Wisdom of the Owl
                handleSpellBuff();
                break;
            case 36:  // Layered Defense
            case 1141: // Heroic Shield
                handleSpellShield();
                break;
            // Examine
            case 38:
                if (this.caster.getUnitType() == UnitTypes.Player) {
                    ((Player) this.caster).examine(this.target);
                }
                break;
            // First Aid
            case 54:
                handleSpellFirstAid();
                break;
            // Relax
            case 60:
                if (this.caster.hasAura(1000)) {
                    this.caster.removeAura(1000);
                } else {
                    // TODO: Spring fix build
//                    this.caster.addAura(Aura.createAura(DataStorage.SpellRepository.get(1000), this.caster, -1, -10));
                }
                break;
            default:
                return false;
        }

        return true;
    }

    private void handleSpellFirstAid() {
        int recoveredHealth = this.caster.getParameters().value(Parameters.Health) / 2;

        // Caster is at full HP
        if (this.caster.getHealth() >= this.caster.getParameters().value(Parameters.Health)) {
            recoveredHealth = 0;
        } else {
            if (recoveredHealth > 12 * this.caster.getLevel())
                recoveredHealth = 12 * this.caster.getLevel();

            if (recoveredHealth + this.caster.getHealth() > this.caster.getParameters().value(Parameters.Health))
                recoveredHealth = this.caster.getParameters().value(Parameters.Health) - this.caster.getHealth();

            if (this.caster.getMana() < recoveredHealth / 6)
                recoveredHealth = this.caster.getMana() * 6;
        }

        if (this.caster.isPlayer() && ((Player) this.caster).getSession() != null) {
            ((Player) this.caster).getSession().send(new ChatMessage(501, Integer.toString(recoveredHealth)));
        }

        this.caster.changeHealth(recoveredHealth);
        this.caster.changeMana(-recoveredHealth / 6);
    }

    private void handleSpellBuff() {
        // Missing target - target is a caster
        if (this.target == null)
            this.target = caster;

        // After the previous condition the target is NULL
        // Spell doesn't have caster and target
        if (this.target == null)
            return;

        // TODO: research allowed targeting for auras
        // Temporary disabled player-to-mob casting
        if (this.caster != null && this.caster.isPlayer() && this.target.getUnitType() == UnitTypes.Creature)
            return;

        int duration = 600000; // 10 minutes
        // Aura from elixirs lasts 15 minutes
        if (item != null)
            duration = 900000;
            // Self-buffing is 20 minutes long
        else if (caster == target)
            duration = 1200000;

        // TODO: mana cost
        Aura aura = Aura.createAura(entry, this.target, level, duration);
        if (!this.target.addAura(aura))
            return;

        //
        // Send messages
        //

        // Message by item
        if (item != null) {
            // TODO: Spring fix build
//            SendMessage(this.target, 4, item.getTitle());
            return;
        }

        // Either caster or target or both should be a player
        if ((this.caster == null || (this.caster != null && !this.caster.isPlayer())) && !this.target.isPlayer())
            return;

        // ID messages
        int selfMessage = -1;
        int casterMessage = -1;
        int targetMessage = -1;
        int positionMessage = -1;

        switch (entry.getId()) {
            case 14: // Tiger Strength
                selfMessage = 513;
                targetMessage = 514;
                positionMessage = 515;
                casterMessage = 566;
                break;
            case 16: // Steel Body
                selfMessage = 516;
                targetMessage = 517;
                positionMessage = 518;
                casterMessage = 567;
                break;
            case 17: // Bears Blood
                selfMessage = 528;
                casterMessage = 571;
                targetMessage = 529;
                positionMessage = 530;
                break;
            case 18: // Feline Grace
                selfMessage = 519;
                casterMessage = 568;
                targetMessage = 520;
                positionMessage = 521;
                break;
            case 37: // Wisdom of the Owl
                selfMessage = 594;
                targetMessage = 595;
                casterMessage = 596;
                positionMessage = 597;
                break;
        }

        if (selfMessage != -1 && this.caster == target)
            SendMessage(this.caster, selfMessage);
        else if (caster != null) {
            if (casterMessage != -1)
                SendMessage(this.caster, casterMessage, target.getName());
            if (targetMessage != -1)
                SendMessage(target, targetMessage, this.caster.getName());
        }
        if (this.caster != null)
            target.getPosition().send(this.caster, this.target,
                    new ChatMessage(positionMessage, this.caster.getName(), this.target.getName()));
    }

    private void handleSpellBattleMagic() {
        int minDamage = 0;
        int maxDamage = 0;

        // Staff damage
        if (this.caster.isPlayer()) {
            Player pCaster = (Player) this.caster;
            Item rightHandItem = pCaster.getInventory().get(Inventory.Slots.RightHand);
            if (rightHandItem != null
                    && rightHandItem.getTemplate().getItemClass() == ItemClasses.Weapon
                    && rightHandItem.getTemplate().getSubClass() == ItemSubClassWeapon.Staff.getValue()) {
                minDamage = rightHandItem.getBonusValue(ItemBonusParameters.BaseMinDamage);
                maxDamage = rightHandItem.getBonusValue(ItemBonusParameters.BaseMaxDamage);
            }
        }

        // Magic Damage
        minDamage += this.caster.getParameters().value(Parameters.MagicDamage);
        maxDamage += this.caster.getParameters().value(Parameters.MagicDamage);

        // Effect percentage
        int effect = 100;


        if (this.entry.getId() == 1 // Fire Ball
                || this.entry.getId() == 2 // Ice Arrow
                || this.entry.getId() == 3) { // Electroshock
            switch (this.level) {
                case 1:
                    effect = 43;
                    break;
                case 2:
                    effect = 45;
                    break;
                case 3:
                    effect = 49;
                    break;
                case 4:
                    effect = 52;
                    break;
                case 5:
                    effect = 55;
                    break;
                case 6:
                    effect = 58;
                    break;
                case 7:
                    effect = 61;
                    break;
                case 8:
                    effect = 64;
                    break;
                case 9:
                    effect = 67;
                    break;
                case 10:
                    effect = 70;
                    break;
                case 11:
                    effect = 73;
                    break;
                case 12:
                    effect = 76;
                    break;
                case 13:
                    effect = 79;
                    break;
                case 14:
                    effect = 82;
                    break;
                case 15:
                    effect = 85;
                    break;
                case 16:
                    effect = 88;
                    break;
                case 17:
                    effect = 91;
                    break;
                case 18:
                    effect = 94;
                    break;
                case 19:
                    effect = 97;
                    break;
                case 20:
                    effect = 100;
                    break;
                default:
                    effect = 100;
                    break;
            }
        }

        minDamage = minDamage * effect / 100;
        maxDamage = maxDamage * effect / 100;

        // Random damage between MIN and MAX
        int initialDamage = Random.nextInt(minDamage, maxDamage);

        /*
        * Dispersion
        * */
        double dispersion;
        switch (this.entry.getId()) {
            // Fire
            case 1:
                dispersion = 0.1d;
                break;
            // Frost
            case 2:
                dispersion = 0.2d;
                break;
            // Lightning
            case 3:
                dispersion = 0.3d;
                break;
            default:
                dispersion = 0d;
                break;
        }
        initialDamage = (int) (initialDamage * (Random.nextDouble() * (dispersion * 2) - dispersion + 1));

        Damage damage = new Damage(initialDamage, this.target);
        damage.Spell = this;

        /*
        * Messages
        * */
        switch (this.entry.getId()) {
            // Fire Ball
            case 1:
                damage.MessageDealerDamage = 101;
                damage.MessageDealerKillingBlow = 107;

                //damage.MessageVictimMiss;
                //damage.MessageVictimNoDamage;
                damage.MessageVictimDamage = 102;
                damage.MessageVictimKillingBlow = 108;

                //damage.MessagePositionMiss;
                //damage.MessagePositionNoDamage;
                damage.MessagePositionDamage = 103;
                damage.MessagePositionKillingBlow = 109;
                break;
            // Ice Arrow
            case 2:
                damage.MessageDealerDamage = 113;
                damage.MessageDealerKillingBlow = 119;

                //damage.MessageVictimMiss;
                //damage.MessageVictimNoDamage;
                damage.MessageVictimDamage = 114;
                damage.MessageVictimKillingBlow = 120;

                //damage.MessagePositionMiss;
                //damage.MessagePositionNoDamage;
                damage.MessagePositionDamage = 115;
                damage.MessagePositionKillingBlow = 121;
                break;
            // Electroshock
            case 3:
                //damage.MessageDealerMiss;
                //damage.MessageDealerNoDamage;
                damage.MessageDealerDamage = 125;
                damage.MessageDealerKillingBlow = 131;

                //damage.MessageVictimMiss;
                //damage.MessageVictimNoDamage;
                damage.MessageVictimDamage = 126;
                damage.MessageVictimKillingBlow = 132;

                //damage.MessagePositionMiss;
                //damage.MessagePositionNoDamage;
                damage.MessagePositionDamage = 127;
                damage.MessagePositionKillingBlow = 133;
                break;
        }

        this.caster.dealDamage(damage);
    }

    private void handleSpellShield() {
        int level = 0;
        int duration = 1200000; // 20 minutes by default
        switch (this.entry.getId()) {
            case 36:  // Layered Defense
                level = this.caster.getParameters().value(Parameters.Health);
                break;
            case 1141: // Heroic Shield
                // TODO: determine caster level dependence
                // 120 + Level * 12 (Low level)
                // 96 + Level * 12 (High Level)
                level = 120 + this.caster.getLevel() * 12;
                duration = -1;
                break;
        }
        if (level == 0)
            return;
        this.caster.addAura(Aura.createAura(this.entry, this.caster, level, duration));
    }

    private void SendMessage(Unit unit, int messageId, String... data) {
        if (unit == null || !unit.isPlayer())
            return;
        Player player = (Player) unit;
        if (player.getSession() == null)
            return;
        player.getSession().send(new ChatMessage(messageId, data));
    }
}
