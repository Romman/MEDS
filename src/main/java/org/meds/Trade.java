package org.meds;

import org.meds.enums.Currencies;
import org.meds.net.ServerCommands;
import org.meds.net.ServerPacket;
import org.meds.util.Valued;

public class Trade {

    public enum Results implements Valued {

        Cancel(0),
        Success(1),
        Failed(2);

        private final int value;

        Results(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return Integer.toString(this.value);
        }
    }

    public class Supply {

        public static final int ITEM_SLOTS_COUNT = 3;

        private Item[] items;
        private int gold;
        private int platinum;

        public Supply() {
            this.items = new Item[ITEM_SLOTS_COUNT];
        }

        public Item getItem(int index) {
            return this.items[index];
        }

        public void setItem(int index, Item item) {
            this.items[index] = item;
        }

        public int getGold() {
            return gold;
        }

        public void setGold(int gold) {
            this.gold = gold;
        }

        public int getPlatinum() {
            return platinum;
        }

        public void setPlatinum(int platinum) {
            this.platinum = platinum;
        }

        public ServerPacket getSupplyData() {
            ServerPacket packet = new ServerPacket(ServerCommands.TradeUpdate);
            packet.add(Trade.this.player.getId());
            for (int i = 0; i < ITEM_SLOTS_COUNT; ++i) {
                if (this.items[i] == null) {
                    packet.add("0").add("0").add("0").add("0");
                } else {
                    packet.add(this.items[i].Template.getId());
                    packet.add(this.items[i].getModification());
                    packet.add(this.items[i].getDurability());
                    packet.add(this.items[i].getCount());
                }
            }
            packet.add(this.gold);
            packet.add(this.platinum);
            packet.add(Trade.this.agreedDemand == null ? "0" : "1");
            packet.add("0"); // ???

            return packet;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Supply supply = (Supply) o;

            if (gold != supply.gold) {
                return false;
            }
            if (platinum != supply.platinum) {
                return false;
            }

            for (int i = 0; i < ITEM_SLOTS_COUNT; ++i) {
                if (this.items[i] == null) {
                    if (supply.items[i] != null)
                        return false;
                } else {
                    if (!this.items[i].equals(supply.items[i]))
                        return false;
                }
            }

            return true;
        }
    }

    private Player player;

    private Trade otherSide;

    private Supply currentSupply;

    private Supply agreedDemand;

    private Trade() { }

    public Trade(Player side1, Player side2) {
        this.player = side1;
        this.otherSide = new Trade();
        this.currentSupply = this.new Supply();
        this.otherSide.player = side2;
        this.otherSide.otherSide = this;
        this.otherSide.currentSupply = this.otherSide.new Supply();

        side1.setTrade(this);
        side2.setTrade(this.otherSide);

        if (side2.getSession() != null) {
            side2.getSession().send(new ServerPacket(ServerCommands.GetTrade).add(side1.getId()));
        }
    }

    public Player getPlayer() {
        return this.player;
    }

    public Trade getOtherSide() {
        return this.otherSide;
    }

    public Supply getCurrentSupply() {
        return currentSupply;
    }

    public void setCurrentSupply(Supply currentSupply) {
        this.currentSupply = currentSupply;

        // Notify both sides
        sendTradeData();
        this.otherSide.sendTradeData();
    }

    public boolean isAgreed() {
        return this.agreedDemand != null;
    }

    public void cancel() {
        complete(Results.Cancel);
    }

    private void complete(Results result) {
        this.player.setTrade(null);
        this.otherSide.player.setTrade(null);
        // Send Trade result
        ServerPacket packet = new ServerPacket(ServerCommands.TradeResult).add(result).add("0").add("0");
        if (this.player.getSession() != null) {
            this.player.getSession().send(packet);
        }
        if (this.otherSide.player.getSession() != null) {
            this.otherSide.player.getSession().send(packet);
        }
    }

    public void agree(Supply demand) {
        this.agreedDemand = demand;
        if (this.otherSide.isAgreed()) {
            apply();
        } else {
            this.otherSide.sendTradeData();
        }
    }

    private void apply() {
        if (!this.currentSupply.equals(this.otherSide.agreedDemand)
                || !this.agreedDemand.equals(this.otherSide.currentSupply)) {
            // Both seller and buyer agreed to the different requirements
            declineAgreement();
            return;
        }

        // The players have declared items
        for (Item item : this.currentSupply.items) {
            if (item == null)
                continue;
            if (!this.player.getInventory().hasItem(item)) {
                complete(Results.Failed);
                return;
            }
        }
        for (Item item : this.otherSide.currentSupply.items) {
            if (item == null)
                continue;
            if (!this.otherSide.player.getInventory().hasItem(item)) {
                complete(Results.Failed);
                return;
            }
        }

        // The players can store proposed items
        if (!this.player.getInventory().canStoreItems(this.agreedDemand.items)
                || !this.otherSide.player.getInventory().canStoreItems(this.otherSide.agreedDemand.items)) {
            complete(Results.Failed);
            return;
        }

        // Currencies supply is enough
        if (this.player.getCurrencyAmount(Currencies.Gold) < this.currentSupply.gold
            || this.player.getCurrencyAmount(Currencies.Platinum) < this.currentSupply.platinum
            || this.otherSide.player.getCurrencyAmount(Currencies.Gold) < this.otherSide.currentSupply.gold
            || this.otherSide.player.getCurrencyAmount(Currencies.Platinum) < this.otherSide.currentSupply.platinum) {
            complete(Results.Failed);
            return;
        }

        // All required checks have passed successfully
        // Commit the trade
        for (int i = 0; i < Supply.ITEM_SLOTS_COUNT; ++i) {
            if (this.agreedDemand.items[i] != null) {
                this.player.getInventory().tryStoreItem(
                        this.otherSide.player.getInventory().takeItem(this.agreedDemand.items[i]));
            }
            if (this.currentSupply.items[i] != null) {
                this.otherSide.player.getInventory().tryStoreItem(
                        this.player.getInventory().takeItem(this.currentSupply.items[i]));
            }
        }
        if (this.currentSupply.gold > 0) {
            this.player.changeCurrency(Currencies.Gold, -this.currentSupply.gold);
            this.otherSide.player.changeCurrency(Currencies.Gold, this.currentSupply.gold);
        }
        if (this.currentSupply.platinum > 0) {
            this.player.changeCurrency(Currencies.Platinum, -this.currentSupply.platinum);
            this.otherSide.player.changeCurrency(Currencies.Platinum, this.currentSupply.platinum);
        }
        if (this.otherSide.currentSupply.gold > 0) {
            this.otherSide.player.changeCurrency(Currencies.Gold, -this.currentSupply.gold);
            this.player.changeCurrency(Currencies.Gold, this.currentSupply.gold);
        }
        if (this.otherSide.currentSupply.platinum > 0) {
            this.otherSide.player.changeCurrency(Currencies.Platinum, -this.otherSide.currentSupply.platinum);
            this.player.changeCurrency(Currencies.Platinum, this.otherSide.currentSupply.platinum);
        }
        complete(Results.Success);
    }

    private void declineAgreement() {
        // Just cancel the agreement
        // And send the current supplies
        this.agreedDemand = null;
        this.otherSide.agreedDemand = null;
        this.sendTradeData();
        this.otherSide.sendTradeData();
    }

    public void sendTradeData() {
        if (this.player.getSession() != null) {
            this.player.getSession().send(this.currentSupply.getSupplyData()).send(this.otherSide.currentSupply.getSupplyData());
        }
    }
}
