package org.meds.item;

import org.meds.Player;
import org.meds.enums.Parameters;
import org.meds.net.message.server.ChatMessage;
import org.meds.spell.Spell;
import org.meds.spell.SpellFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Romman
 */
@Component
public class ItemUsageService {

    public static final int POTION_HEALING_MESSAGE = 500;

    private SpellFactory spellFactory;

    @Autowired
    public void setSpellFactory(SpellFactory spellFactory) {
        this.spellFactory = spellFactory;
    }

    public boolean processUsage(Player user, Item item) {
        Spell spell = null;
        switch (item.getTemplate().getId()) {
            case 64: // Small Health Potion
                int hpToHeal = user.getParameters().value(Parameters.Health) - user.getHealth();
                // HP is full
                if (hpToHeal < 1) {
                    return false;
                }
                // TODO: level checking
                user.setHealth(user.getParameters().value(Parameters.Health));
                if (user.getSession() != null) {
                    user.getSession().send(new ChatMessage(
                            POTION_HEALING_MESSAGE, item.getTemplate().getTitle(),Integer.toString(hpToHeal)
                    ));
                }
                break;
            case 3581: // Small Mana Potion
                // TODO: Implement
                break;
            case 35196: // Steel Body
                spell = spellFactory.create(16, user, 3, null, item);
                break;
            case 35197: // Bears Blood
                // Level from Item level???
                spell = spellFactory.create(17, user, 3, null, item);
                break;
            case 35198: // Tigers Strength
                spell = spellFactory.create(14, user, 3, null, item);
                break;
            case 35199: // Feline Grace
                spell = spellFactory.create(18, user, 3, null, item);
                break;
            case 35200: // Wisdom of the Owl
                spell = spellFactory.create(37, user, 3, null, item);
                break;
        }

        if (spell != null) {
            return spell.cast();
        }
        return true;
    }
}
