package io.github.tanice.twItemManager.manager.global;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.pdc.impl.BuffPDC;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * buff 记录
 */
@Getter
@NotNull
public class BuffRecord {
    private final UUID uuid;
    private BuffPDC buff;
    private final String buffInnerName;

    @Setter
    private boolean isPermanent;
    private final boolean isTimer;

    public int cooldownCounter;
    public int durationCounter;

    public BuffRecord(@NotNull LivingEntity entity, @NotNull BuffPDC buff) {
        this.uuid = entity.getUniqueId();
        this.buff = buff;
        this.buffInnerName = buff.getInnerName();

        this.isPermanent = false;
        this.isTimer = buff.isTimer();

        this.cooldownCounter = buff.getCd();
        this.durationCounter = buff.getDuration();
    }

    public BuffRecord(@NotNull LivingEntity entity, @NotNull BuffPDC buff, boolean isPermanent) {
        this.uuid = entity.getUniqueId();
        this.buff = buff;
        this.buffInnerName = buff.getInnerName();

        this.isPermanent = isPermanent;
        this.isTimer = buff.isTimer();

        this.cooldownCounter = 0;
        this.durationCounter = buff.getDuration();
    }

    public BuffRecord(@NotNull String uuid, @NotNull String buffInnerName, int cooldownCounter, int durationCounter) {
        this.uuid = UUID.fromString(uuid);
        this.buffInnerName = buffInnerName;
        this.buff = TwItemManager.getBuffManager().getBuffPDC(buffInnerName);

        this.isPermanent = false;
        this.isTimer = buff.isTimer();

        this.cooldownCounter = cooldownCounter;
        this.durationCounter = durationCounter;
    }

    /**
     * 自更新 (持续时间 取长合并, cd 更新)
     * 认为传入的 buff 是新的
     */
    public void merge(@NotNull BuffPDC buff) {
        int p = this.buff.getDuration();
        int n = buff.getDuration();

        buff.setDuration(Math.max(p, n));
        int d = p - this.durationCounter;
        this.durationCounter = buff.getDuration() - d;

        this.buff = buff;
    }
}
