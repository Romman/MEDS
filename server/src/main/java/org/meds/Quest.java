package org.meds;

import org.meds.data.domain.CharacterQuest;
import org.meds.data.domain.Currency;
import org.meds.data.domain.QuestTemplate;
import org.meds.database.Repository;
import org.meds.database.repository.CurrencyRepository;
import org.meds.enums.Currencies;
import org.meds.enums.QuestStatuses;
import org.meds.enums.QuestTypes;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.server.ChatMessage;
import org.meds.net.message.server.QuestFinalTextMessage;
import org.meds.net.message.server.QuestListInfoMessage;
import org.meds.net.message.server.QuestUpdateMessage;

import java.util.Date;

public class Quest {

    private class KillingBlowHandler implements Unit.KillingBlowListener {

        @Override
        public void handleEvent(Unit.DamageEvent e) {

            Quest quest = Quest.this;

            if (e.getVictim().getUnitType() != UnitTypes.Creature)
                return;

            if (((Creature)e.getVictim()).getTemplate().getTemplateId() != quest.questTemplate.getRequiredCreatureId())
                return;

            quest.setProgress(quest.getProgress() + 1);

            if (quest.player.getSession() != null) {
                quest.player.getSession().send(quest.getUpdateQuestData());
                quest.player.getSession().send(new ChatMessage(
                        337, quest.questTemplate.getTitle(), quest.getProgress(), quest.questTemplate.getRequiredCount()
                ));
            }

            if (quest.getProgress() == quest.questTemplate.getRequiredCount()) {
                quest.goalAchieved();
            }
        }
    }

    private static final long serialVersionUID = 1L;

    private CharacterQuest characterQuest;
    private Player player;
    private QuestTemplate questTemplate;

    private boolean isGoalAchieved;

    private Unit.KillingBlowListener killingBlowHandler;

    // TODO: This is a Spring component and should not be in this class.
    //  Or convert this class to a prototype bean(bad idea)
    private final Repository<Currency> currencyRepository;

    public Quest(Player player, QuestTemplate template, CharacterQuest quest,
                 Repository<Currency> currencyRepository) {
        this.player = player;
        this.questTemplate = template;
        this.characterQuest = quest;
        this.currencyRepository = currencyRepository;
    }

    public QuestTemplate getQuestTemplate() {
        return this.questTemplate;
    }

    public QuestStatuses getStatus(){
        return QuestStatuses.parse(this.characterQuest.getStatusInteger());
    }

    private void setStatus(QuestStatuses status){
        this.characterQuest.setStatusInteger(status.getValue());
    }

    public int getProgress() {
        return this.characterQuest.getProgress();
    }

    private void setProgress(int progress) {
        this.characterQuest.setProgress(progress);
    }

    public int getTimer() {
        return this.characterQuest.getTimer();
    }

    public boolean isTracked() {
        return this.characterQuest.isTracked();
    }

    public int getAcceptDate() {
        return this.characterQuest.getAcceptDate();
    }

    public int getCompleteDate() {
        return this.characterQuest.getCompleteDate();
    }

    @Override
    public int hashCode() {
        return this.player.hashCode() + this.questTemplate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Quest))
            return false;
        Quest cObj = (Quest)obj;

        return this.player.equals(cObj.player) && this.questTemplate.equals(cObj.questTemplate);
    }

    public boolean isAccepted() {
        return this.characterQuest.getAcceptDate() != 0;
    }

    public boolean isGoalAchieved() {
        return this.isGoalAchieved;
    }


    public ServerMessage getQuestData() {
        return new QuestListInfoMessage(
                this.questTemplate.getId(),
                this.questTemplate.getType(),
                this.questTemplate.getTitle(),
                this.getProgress(),
                this.questTemplate.getRequiredCount(),
                this.questTemplate.getTime(),
                this.characterQuest.getStatusInteger(),
                this.isTracked(),
                this.questTemplate.getTracking()
        );
    }

    public ServerMessage getUpdateQuestData() {
        return new QuestUpdateMessage(
                this.questTemplate.getId(),
                this.getProgress(),
                this.questTemplate.getTime(),
                this.characterQuest.getStatusInteger(),
                this.isTracked(),
                this.questTemplate.getTracking()
        );
    }

    public void accept() {
        // May by already accepted
        // For example, an active quest loaded from DB
        if (!isAccepted()) {
            this.characterQuest.setAcceptDate((int)(new Date().getTime() / 1000));
            this.setStatus(QuestStatuses.Taken);
        }

        if (player.getSession() != null)
            player.getSession().send(getUpdateQuestData());

        activateHandlers();
    }

    public void complete() {
        // Send Final Text
        if (this.player.getSession() != null) {
            this.player.getSession().send(new QuestFinalTextMessage(
                    this.questTemplate.getTitle(), this.questTemplate.getEndText()
            ));
        }

        // Change quest status
        this.setStatus(QuestStatuses.Completed);
        this.characterQuest.setCompleteDate((int)(new Date().getTime() / 1000));
        if (this.player.getSession() != null) {
            this.player.getSession().send(getUpdateQuestData());
        }

        // Reward
        if (this.questTemplate.getRewardGold() != 0) {
            if (this.player.getSession() != null) {
                ChatMessage rewardMessage = new ChatMessage(1096,
                        this.questTemplate.getRewardGold(),
                        currencyRepository.get(Currencies.Gold.getValue()).getTitle()
                );
                this.player.getSession().send(rewardMessage);
            }
            this.player.changeCurrency(Currencies.Gold, this.questTemplate.getRewardGold());
        }
    }

    private void goalAchieved() {
        this.isGoalAchieved = true;
        deactivateHandlers();
    }

    private void activateHandlers() {
        if (this.questTemplate.getType() == QuestTypes.Kill) {
            if (this.killingBlowHandler == null) {
                this.killingBlowHandler = new KillingBlowHandler();
            }

            this.player.addKillingBlowListener(this.killingBlowHandler);
        }
    }

    private void deactivateHandlers() {
        this.player.removeKillingBlowListener(this.killingBlowHandler);
    }
}
