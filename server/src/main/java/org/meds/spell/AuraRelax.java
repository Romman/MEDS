package org.meds.spell;

import org.meds.enums.Parameters;
import org.meds.net.message.server.ChatMessage;
import org.meds.net.message.server.RelaxOffMessage;
import org.meds.net.message.server.RelaxOnMessage;

public class AuraRelax extends Aura {

    private int healthRegenBonus = 0;
    private int manaRegenBonus = 0;

    @Override
    protected void applyAura() {
        if (this.ownerPlayer != null && this.ownerPlayer.getSession() != null) {
            this.ownerPlayer.getSession().send(new ChatMessage(1051));
            this.ownerPlayer.getSession().send(new RelaxOnMessage());
        }
        this.healthRegenBonus = this.owner.getParameters().base().value(Parameters.HealthRegeneration);
        this.manaRegenBonus = this.owner.getParameters().base().value(Parameters.ManaRegeneration);
        this.owner.getParameters().magic().change(Parameters.HealthRegeneration, this.healthRegenBonus);
        this.owner.getParameters().magic().change(Parameters.ManaRegeneration, this.manaRegenBonus);
        super.applyAura();
    }

    @Override
    protected void removeAura() {
        if (this.ownerPlayer != null && this.ownerPlayer.getSession() != null) {
            this.ownerPlayer.getSession().send(new ChatMessage(303));
            this.ownerPlayer.getSession().send(new RelaxOffMessage());
        }
        this.owner.getParameters().magic().change(Parameters.HealthRegeneration, -this.healthRegenBonus);
        this.owner.getParameters().magic().change(Parameters.ManaRegeneration, -this.manaRegenBonus);
        super.removeAura();
    }
}
