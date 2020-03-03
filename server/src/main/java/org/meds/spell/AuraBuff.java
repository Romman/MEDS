package org.meds.spell;

import java.util.List;
import org.meds.net.message.ServerMessage;
import org.meds.enums.Parameters;
import org.meds.net.message.server.BonusMagicParameterMessage;
import org.meds.net.message.server.ChatMessage;
import org.meds.util.KeyValuePair;

public class AuraBuff extends Aura {

    protected KeyValuePair<Parameters, Integer> bonusParameter1;
    protected KeyValuePair<Parameters, Integer> bonusParameter2;

    @Override
    protected void applyAura() {
        recalculateBonus();
        super.applyAura();
    }

    @Override
    protected void removeAura() {
        super.removeAura();

        removeBonus();

        int selfMessage = -1;
        int positionMessage = -1;

        // Special Messages
        switch (this.spellEntry.getId()) {
            case 14: // Tiger Strength
                selfMessage = 707;
                positionMessage = 725;
                break;
            case 16: // Steel Body
                selfMessage = 709;
                positionMessage = 727;
                break;
            case 17: // Bears Blood
                selfMessage = 710;
                positionMessage = 728;
                break;
            case 18: // Feline Grace
                selfMessage = 711;
                positionMessage = 729;
                break;
            case 37: // Wisdom of the Owl
                selfMessage = 743;
                positionMessage = 744;
                break;
        }

        if (this.ownerPlayer != null && this.ownerPlayer.getSession() != null && selfMessage != -1)
            this.ownerPlayer.getSession().send(new ChatMessage(selfMessage));
        if (positionMessage != -1)
            this.owner.getPosition().send(this.owner,
                    new ChatMessage(positionMessage, this.owner.getName()));
    }

    @Override
    public void forceRemove() {
        super.forceRemove();
        removeBonus();
    }

    protected void recalculateBonus() {
        this.removeBonus(false);

        // Calculate Bonus parameters
        switch (this.spellEntry.getId()) {
            case 14: // Tigers Strength
            case 16: // Steel Body
            case 18: // Feline Grace
            case 37: // Wisdom of the Owl
                Parameters parameter = Parameters.None;
                switch (this.spellEntry.getId()) {
                    case 14: // Tigers Strength
                        parameter = Parameters.Strength;
                        break;
                    case 16: // Steel Body
                        parameter = Parameters.Constitution;
                        break;
                    case 18: // Feline Grace
                        parameter = Parameters.Dexterity;
                        break;
                    case 37: // Wisdom of the Owl
                        parameter = Parameters.Intelligence;
                        break;
                }
                int effect = 0;
                switch (this.level) {
                    case 1:
                        effect = 128;
                        break;
                    case 2:
                        effect = 160;
                        break;
                    case 3:
                        effect = 224;
                        break;
                    case 4:
                        effect = 256;
                        break;
                    case 5:
                        effect = 288;
                        break;
                    case 6:
                        effect = 320;
                        break;
                    case 7:
                        effect = 352;
                        break;
                    case 8:
                        effect = 384;
                        break;
                    case 9:
                        effect = 416;
                        break;
                    case 10:
                        effect = 448;
                        break;
                }
                int limit = (int) ((this.owner.getParameters().base().value(parameter) + this.owner.getParameters().guild().value(parameter)) * 5.6);
                if (effect > limit)
                    effect = limit;
                this.bonusParameter1 = new KeyValuePair<>(parameter, effect);
                break;
            case 17: // Bears Blood
                double effectPercent = 0d;
                switch (this.level) {
                    case 1:
                        effectPercent = 0.15;
                        break;
                    case 2:
                        effectPercent = 0.17;
                        break;
                    case 3:
                        effectPercent = 0.25;
                        break;
                    case 4:
                        effectPercent = 0.26;
                        break;
                    case 5:
                        effectPercent = 0.27;
                        break;
                    case 6:
                        effectPercent = 0.28;
                        break;
                    case 7:
                        effectPercent = 0.29;
                        break;
                    case 8:
                        effectPercent = 0.30;
                        break;
                    case 9:
                        effectPercent = 0.31;
                        break;
                    case 10:
                        effectPercent = 0.33;
                        break;
                }
                this.bonusParameter1 = new KeyValuePair<>(Parameters.Health,
                        (int) (this.owner.getParameters().value(Parameters.Health) * effectPercent + this.owner.getLevel() * this.level * (1 + effectPercent)));
                break;
        }

        // Apply Bonus parameters
        if (this.bonusParameter1 != null) {
            this.owner.getParameters().magic().change(this.bonusParameter1.getKey(), this.bonusParameter1.getValue());
            if (this.bonusParameter2 != null)
                this.owner.getParameters().magic().change(this.bonusParameter2.getKey(), this.bonusParameter2.getValue());
        }
    }

    protected void removeBonus() {
        this.removeBonus(true);
    }

    protected void removeBonus(boolean isSendToPlayer) {
        // Remove bonus parameters
        if (this.bonusParameter1 != null) {
            this.owner.getParameters().magic().change(this.bonusParameter1.getKey(), -this.bonusParameter1.getValue());
            if (isSendToPlayer && this.ownerPlayer != null && this.ownerPlayer.getSession() != null)
                this.ownerPlayer.getSession().send(
                        new BonusMagicParameterMessage(this.bonusParameter1.getKey(), 0)
            );
            if (this.bonusParameter2 != null) {
                this.owner.getParameters().magic().change(this.bonusParameter2.getKey(), -this.bonusParameter2.getValue());
                if (isSendToPlayer && this.ownerPlayer != null && this.ownerPlayer.getSession() != null)
                    this.ownerPlayer.getSession().send(
                            new BonusMagicParameterMessage(this.bonusParameter2.getKey(), 0)
                    );
            }
            this.bonusParameter1 = null;
            this.bonusParameter2 = null;
        }
    }

    @Override
    public void refresh(int level, int time) {
        super.refresh(level, time);
    }

    @Override
    public List<ServerMessage> getBonusMessages() {
        List<ServerMessage> messages = super.getBonusMessages();

        if (this.bonusParameter1 != null) {
            messages.add(new BonusMagicParameterMessage(
                    this.bonusParameter1.getKey(), this.bonusParameter1.getValue()
            ));
            if (this.bonusParameter2 != null) {
                messages.add(new BonusMagicParameterMessage(
                        this.bonusParameter2.getKey(), this.bonusParameter2.getValue()
                ));
            }
        }

        return messages;
    }
}
