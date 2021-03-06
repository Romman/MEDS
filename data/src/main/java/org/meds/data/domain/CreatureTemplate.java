package org.meds.data.domain;

import org.meds.enums.CreatureFlags;
import org.meds.util.EnumFlags;

public class CreatureTemplate {

    private int templateId;
    private String name;
    private int level;
    private int regionId;
    private int avatarId;
    private EnumFlags<CreatureFlags> creatureFlags;

    public int getTemplateId() {
        return templateId;
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public int getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(int avatarId) {
        this.avatarId = avatarId;
    }

    public int getCreatureFlags() {
        return creatureFlags.getValue();
    }

    public void setCreatureFlags(int creatureFlags) {
        this.creatureFlags = new EnumFlags<>(creatureFlags);
    }

    public boolean hasFlag(CreatureFlags flag) {
        return this.creatureFlags.has(flag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CreatureTemplate that = (CreatureTemplate) o;

        return this.templateId == that.templateId;
    }

    @Override
    public int hashCode() {
        return this.templateId;
    }
}
