package io.github.tanice.twItemManager.manager.buff;

import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// buff 记录
@Getter
public class BuffRecord {
    final UUID uuid;
    BuffPDC buff;
    final String buffInnerName;
    int cooldownCounter;
    int durationCounter;

    public BuffRecord(@NotNull LivingEntity entity, @NotNull BuffPDC buff) {
        this.uuid = entity.getUniqueId();
        this.buff = buff;
        this.buffInnerName = buff.getInnerName();
        this.cooldownCounter = buff.getCd();
        this.durationCounter = buff.getDuration();
    }

    public BuffRecord(@NotNull String uuid, @NotNull String buffInnerName, int cooldownCounter, int durationCounter) {
        this.uuid = UUID.fromString(uuid);
        this.buffInnerName = buffInnerName;
        this.cooldownCounter = cooldownCounter;
        this.durationCounter = durationCounter;
    }
}
