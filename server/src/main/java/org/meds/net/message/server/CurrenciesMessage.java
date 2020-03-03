package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class CurrenciesMessage implements ServerMessage {

    private final Collection<CurrencyInfo> infos;

    public CurrenciesMessage(Collection<CurrencyInfo> infos) {
        this.infos = infos;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.Currencies;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        for (CurrencyInfo info : this.infos) {
            stream.writeInt(info.id);
            stream.writeInt(info.unk2);
            stream.writeString(info.title);
            stream.writeString(info.description);
            stream.writeInt(info.unk5);
            stream.writeBoolean(info.disabled);
            stream.writeInt(info.amount);
        }
    }

    public static class CurrencyInfo {

        private final int id;
        private final int unk2;
        private final String title;
        private final String description;
        private final int unk5;
        private final boolean disabled;
        private final int amount;

        public CurrencyInfo(int id, int unk2, String title, String description, int unk5,
                            boolean disabled, int amount) {
            this.id = id;
            this.unk2 = unk2;
            this.title = title;
            this.description = description;
            this.unk5 = unk5;
            this.disabled = disabled;
            this.amount = amount;
        }
    }
}
