package meds.spell;

import meds.Damage;
import meds.Damage.AffectionHandler;

public class AuraShield extends Aura
{
	private class AbsorptionHandler implements AffectionHandler
    {
        @Override
        public boolean handle(Damage damage)
        {
            int cleanDamage = damage.FinalDamage - damage.Absorbed;
            // The shield expires
            if (cleanDamage  >= level)
            {
                damage.Absorbed += level;
                // Force remove this aura
                owner.removeAura(spellEntry.getId());
                return false;
            }
            else
            {
                setLevel(level - cleanDamage);
                damage.Absorbed = damage.FinalDamage;
                return true;
            }
        }
    }

    private AffectionHandler absorptionHandler;

    @Override
    protected void applyAura()
    {
        super.applyAura();
        // Server message
        if (this.ownerPlayer != null && this.ownerPlayer.getSession() != null && this.spellEntry.getId() == 1141)
            this.ownerPlayer.getSession().addServerMessage(436);
        this.absorptionHandler = new AbsorptionHandler();
        this.owner.addDamageReduction(Damage.ReductionTypes.Absorption, this.absorptionHandler);
    }

    @Override
    protected void removeAura()
    {
        super.removeAura();
        this.owner.removeDamageReduction(Damage.ReductionTypes.Absorption, this.absorptionHandler);
        // Server message
        if (this.ownerPlayer != null && this.ownerPlayer.getSession() != null && this.spellEntry.getId() == 1141)
            this.ownerPlayer.getSession().addServerMessage(477);
    }
}
