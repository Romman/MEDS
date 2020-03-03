package org.meds;

import java.util.ArrayList;
import java.util.List;
import org.meds.data.domain.CreatureTemplate;
import org.meds.data.domain.ItemTemplate;
import org.meds.data.domain.QuestTemplate;
import org.meds.database.Repository;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.server.QuestInfoMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Romman
 */
@Component
public class QuestInfoPacketFactory {

    @Autowired
    private Repository<CreatureTemplate> creatureTemplateRepository;
    @Autowired
    private Repository<ItemTemplate> itemTemplateRepository;
    @Autowired
    private Locale locale;

    public ServerMessage create(QuestTemplate template) {
        return create(template, false);
    }

    public ServerMessage create(QuestTemplate template, boolean isForAccept) {
        int requiredCount = 0;
        String requiredName = null;

        switch (template.getType()) {
            case Kill:
                CreatureTemplate creatureTemplate = creatureTemplateRepository.get(template.getRequiredCreatureId());
                if (creatureTemplate != null) {
                    requiredCount = template.getRequiredCount();
                    requiredName = creatureTemplate.getName();
                }
                break;
            default:
                break;
        }
        List<String> rewards = new ArrayList<>(3);

        if (template.getRewardExp() != 0) {
            rewards.add(template.getRewardExp() + locale.getString(1));
        }
        if (template.getRewardGold() != 0) {
            rewards.add(template.getRewardGold() + locale.getString(2));
        }

        ItemTemplate itemTemplate;
        if (template.getRewardItem1Count() != 0 && template.getRewardItem1Id() != 0) {
            itemTemplate = itemTemplateRepository.get(template.getRewardItem1Id());
            if (itemTemplate != null) {
                rewards.add(template.getRewardItem1Count() + " x " + itemTemplate.getTitle());
            }
        }
        if (template.getRewardItem2Count() != 0 && template.getRewardItem2Id() != 0) {
            itemTemplate = itemTemplateRepository.get(template.getRewardItem2Id());
            if (itemTemplate != null) {
                rewards.add(template.getRewardItem2Count() + " x " + itemTemplate.getTitle());
            }
        }

        return new QuestInfoMessage(
                isForAccept,
                template.getId(),
                template.getTitle(),
                template.getType(),
                template.getDescription(),
                requiredCount,
                requiredName,
                rewards
        );
    }

}
