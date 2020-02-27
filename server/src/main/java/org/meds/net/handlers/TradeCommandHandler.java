package org.meds.net.handlers;

import java.text.MessageFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.Player;
import org.meds.Trade;
import org.meds.World;
import org.meds.enums.Currencies;
import org.meds.item.Item;
import org.meds.item.ItemFactory;
import org.meds.item.ItemFlags;
import org.meds.item.ItemPrototype;
import org.meds.net.ClientCommandData;
import org.meds.net.SessionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman.
 */
public abstract class TradeCommandHandler extends CommonClientCommandHandler {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    private SessionContext sessionContext;
    @Autowired
    private World world;
    @Autowired
    private ItemFactory itemFactory;

    public abstract boolean isApply();

    @Override
    public void handle(ClientCommandData data) {
        Trade trade = sessionContext.getPlayer().getTrade();
        if (trade == null) {
            return;
        }
        Player player = sessionContext.getPlayer();
        Player trader = world.getPlayer(data.getInt(0));
        // The real trader and the new supply trader do not match
        if (trader != trade.getOtherSide().getPlayer()) {
            return;
        }

        Trade.Supply supply = trade.new Supply();
        int counter = 1;
        for (int i = 0; i < 3; ++i) {
            ItemPrototype prototype = new ItemPrototype(
                    data.getInt(counter++),
                    data.getInt(counter++),
                    data.getInt(counter++));
            Item item = itemFactory.create(prototype, data.getInt(counter++));

            // An item has not been constructed right
            if (item == null || item.getCount() == 0) {
                continue;
            }
            if (item.hasFlag(ItemFlags.IsPersonal)) {
                continue;
            }

            if (!player.getInventory().hasItem(item)) {
                logger.warn("Trade: {} places an item {} but he does not have the item in the inventory",
                        player.toString(), item.getPrototype().toString());
                continue;
            }
            supply.setItem(i, item);
        }

        int gold = data.getInt(counter++);
        int platinum = data.getInt(counter++);
        if (gold <= 0) {
            gold = 0;
        } else if (gold > player.getCurrencyAmount(Currencies.Gold)) {
            gold = player.getCurrencyAmount(Currencies.Gold);
        }
        if (platinum <= 0) {
            platinum = 0;
        } else if (platinum > player.getCurrencyAmount(Currencies.Platinum)) {
            platinum = player.getCurrencyAmount(Currencies.Platinum);
        }
        supply.setGold(gold);
        supply.setPlatinum(platinum);

        if (isApply()) {
            if (!trade.getCurrentSupply().equals(supply)) {
                logger.warn("Trade: {} agreed to trade, but his own supply does not match. The current supply is updated", player.toString());
                trade.setCurrentSupply(supply);
            }

            Trade.Supply demand = trade.new Supply();
            for (int i = 0; i < 3; ++i) {
                ItemPrototype prototype = new ItemPrototype(
                        data.getInt(counter++),
                        data.getInt(counter++),
                        data.getInt(counter++));
                Item item = itemFactory.create(prototype, data.getInt(counter++));

                if (item == null || item.getCount() == 0) {
                    continue;
                }
                demand.setItem(i, item);
            }

            gold = data.getInt(counter++);
            platinum = data.getInt(counter++);

            demand.setGold(gold);
            demand.setPlatinum(platinum);

            trade.agree(demand);
        }
        // Trade update
        else {
            trade.setCurrentSupply(supply);
        }
    }
}
