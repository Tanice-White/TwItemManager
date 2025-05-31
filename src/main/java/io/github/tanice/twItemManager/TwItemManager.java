package io.github.tanice.twItemManager;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.tanice.twItemManager.command.MainCommand;
import io.github.tanice.twItemManager.command.impl.*;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.listener.DamageIndicatorListener;
import io.github.tanice.twItemManager.listener.GenericParticleListener;
import io.github.tanice.twItemManager.listener.TwItemListener;
import io.github.tanice.twItemManager.listener.WorkbenchListener;
import io.github.tanice.twItemManager.manager.item.ItemManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class TwItemManager extends JavaPlugin {
    @Getter
    public static TwItemManager instance;
    @Getter
    private static Double updateCode = 0.0;
    @Getter
    private static ItemManager itemManager;
//    @Getter
//    private static GemManager gemManager;
    @Getter
    private static TwItemListener twItemListener;
    @Getter
    private static WorkbenchListener workbenchListener;
    @Getter
    private static GenericParticleListener particleListener;
    @Getter
    private static DamageIndicatorListener damageIndicatorListener;
    @Getter
    private static MainCommand mainCommand;

    @Override
    public void onEnable() {
        instance = this;

        Config.save(this);   // 根据情况生成默认配置文件
        Config.onEnable(this);  // 读取全局配置文件

        itemManager = new ItemManager(this);
//        gemManager = new GemManager(this);

        // 监听器直接从 ConfigUtil 获取变量 不需要显示 reload
        twItemListener = new TwItemListener(this);
        damageIndicatorListener = new DamageIndicatorListener(this);
        workbenchListener = new WorkbenchListener(this);
        particleListener = new GenericParticleListener();
        PacketEvents.getAPI().getEventManager().registerListener(particleListener, PacketListenerPriority.NORMAL);
        // 指令不需要reload
        mainCommand = new MainCommand(this);
        mainCommand.register(new GiveCommand());
        mainCommand.register(new ReloadCommand());
        mainCommand.register(new RecastCommand());
        mainCommand.register(new LevelCommand());
        mainCommand.register(new CheckCommand());
        mainCommand.onEnable();

        this.getLogger().info("TwItems Plugin Enabled");
    }

    @Override
    public void onDisable() {
    }

    public void onReload() {
        updateCode += 1.0;
    }
}
