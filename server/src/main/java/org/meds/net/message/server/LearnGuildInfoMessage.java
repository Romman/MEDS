package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class LearnGuildInfoMessage implements ServerMessage {

    /**
     * Always 0
     */
    private final int unk1 = 0;
    private final int available;
    /**
     * Positive - free available lessons count
     * Negative - total learned lessons
     */
    private final int guildLevel;
    /**
     * ???
     * The same as {@link #available}?
     */
    private final int availableLessons;
    private final int nextLessonGold;
    private final int allLessonsGold;
    private final String resetCost;
    /**
     * ???
     * Maybe next reset cost
     */
    private final String unk8 = "100500 gold";

    public LearnGuildInfoMessage(int available, int guildLevel, int availableLessons, int nextLessonGold,
                                 int allLessonsGold, String resetCost) {
        this.available = available;
        this.guildLevel = guildLevel;
        this.availableLessons = availableLessons;
        this.nextLessonGold = nextLessonGold;
        this.allLessonsGold = allLessonsGold;
        this.resetCost = resetCost;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.LearnGuildInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.unk1);
        stream.writeInt(this.available);
        stream.writeInt(this.guildLevel);
        stream.writeInt(this.availableLessons);
        stream.writeInt(this.nextLessonGold);
        stream.writeInt(this.allLessonsGold);
        stream.writeString(this.resetCost);
        stream.writeString(this.unk8);
    }
}
