package org.meds.spell;

import org.meds.Damage;
import org.meds.Damage.AffectionHandler;
import org.meds.net.message.server.ChatMessage;

public class AuraShield extends Aura {

    private class AbsorptionHandler implements AffectionHandler {

        @Override
        public boolean handle(Damage damage) {
            int cleanDamage = damage.FinalDamage - damage.Absorbed;
            // The shield expires
            if (cleanDamage >= level) {
                damage.Absorbed += level;
                // Force remove this aura
                owner.removeAura(spellEntry.getId());
                return false;
            } else {
                setLevel(level - cleanDamage);
                damage.Absorbed = damage.FinalDamage;
                return true;
            }
        }
    }

    private AffectionHandler absorptionHandler;

    @Override
    protected void applyAura() {
        super.applyAura();
        // Server message
        if (this.ownerPlayer != null && this.ownerPlayer.getSession() != null && this.spellEntry.getId() == 1141)
            this.ownerPlayer.getSession().send(new ChatMessage(476));
        this.absorptionHandler = new AbsorptionHandler();
        this.owner.addDamageReduction(Damage.ReductionTypes.Absorption, this.absorptionHandler);
    }

    @Override
    public void forceRemove() {
        super.forceRemove();
        this.owner.removeDamageReduction(Damage.ReductionTypes.Absorption, this.absorptionHandler);
    }

    @Override
    protected void removeAura() {
        super.removeAura();
        this.owner.removeDamageReduction(Damage.ReductionTypes.Absorption, this.absorptionHandler);
        // Server message
        if (this.ownerPlayer != null && this.ownerPlayer.getSession() != null && this.spellEntry.getId() == 1141)
            this.ownerPlayer.getSession().send(new ChatMessage(477));
    }
}
