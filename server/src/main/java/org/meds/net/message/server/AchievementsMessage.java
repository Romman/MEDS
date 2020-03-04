package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class AchievementsMessage implements ServerMessage {

    private final Collection<AchievementInfo> achievements;

    public AchievementsMessage(Collection<AchievementInfo> achievements) {
        this.achievements = achievements;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.AchievementList;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        // 0?
        stream.writeInt(0);
        for (AchievementInfo achievement : this.achievements) {
            stream.writeInt(achievement.id);
            stream.writeString(achievement.title);
            stream.writeString(achievement.description);
            stream.writeInt(achievement.progress);
            stream.writeInt(achievement.required);
            stream.writeInt(achievement.completeDate);
            stream.writeInt(achievement.categoryId);
            stream.writeInt(achievement.points);
            stream.writeString(achievement.unk9);
            stream.writeString(achievement.unk10);
        }
        // Empty value at the end
        // Or is it just an artifact?
        stream.writeString("");
    }

    public static class AchievementInfo {

        private final int id;
        private final String title;
        private final String description;

        private final int progress;
        private final int required;
        private final int completeDate;

        private final int categoryId;
        private final int points;

        private final String unk9 = "";
        private final String unk10 = "000000";

        public AchievementInfo(int id, String title, String description, int progress, int required,
                               int completeDate, int categoryId, int points) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.progress = progress;
            this.required = required;
            this.completeDate = completeDate;
            this.categoryId = categoryId;
            this.points = points;
        }
    }
}
