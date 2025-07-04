package io.github.tanice.twItemManager;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.tanice.twItemManager.command.MainCommand;
import io.github.tanice.twItemManager.command.impl.*;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.helper.asm.ASMHelper;
import io.github.tanice.twItemManager.listener.*;
import io.github.tanice.twItemManager.manager.buff.BuffManager;
import io.github.tanice.twItemManager.manager.database.DatabaseManager;
import io.github.tanice.twItemManager.manager.item.ItemManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import static io.github.tanice.twItemManager.util.Logger.logWarning;

public final class TwItemManager extends JavaPlugin {
    @Getter
    public static TwItemManager instance;
    @Getter
    private static long updateCode;

    /** 管理器 */
    @Getter
    private static DatabaseManager databaseManager;
    @Getter
    private static ItemManager itemManager;
    @Getter
    private static BuffManager buffManager;

    /** 监听器 */
    private static TwItemListener twItemListener;
    private static WorkbenchListener workbenchListener;
    private static PlayerDataListener playerDataListener;
    private static GenericParticleListener particleListener;
    private static DamageEventListener damageEventListener;
    private static TwItemUpdateListener updateListener;
    private static BuffListener buffListener;

    /** 指令 */
    private static MainCommand mainCommand;

    /* 更改finalDamage方法 */
    static {
        ASMHelper.applyModification();
    }

    @Override
    public void onEnable() {
        updateCode = System.currentTimeMillis();
        instance = this;

        /* Config 必须最先初始化 */
        Config.save(this);   // 根据情况生成默认配置文件
        Config.onEnable(this);  // 读取全局配置文件
        databaseManager = new DatabaseManager();

        itemManager = new ItemManager(this);
        buffManager = new BuffManager(this);

        twItemListener = new TwItemListener(this);
        workbenchListener = new WorkbenchListener(this);
        playerDataListener = new PlayerDataListener(this);
        particleListener = new GenericParticleListener(this);
        PacketEvents.getAPI().getEventManager().registerListener(particleListener, PacketListenerPriority.NORMAL);
        damageEventListener = new DamageEventListener(this);
        updateListener = new TwItemUpdateListener(this);
        buffListener = new BuffListener(this);

        mainCommand = new MainCommand(this);
        mainCommand.register(new GetCommand());
        mainCommand.register(new ReloadCommand());
        mainCommand.register(new RecastCommand());
        mainCommand.register(new LevelCommand());
        mainCommand.register(new CheckCommand());
        mainCommand.register(new RemoveGemCommand());
        mainCommand.onEnable();

        this.getLogger().info("TwItems Plugin Enabled");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.onDisable();
        if (mainCommand != null) mainCommand.onDisable();
        if (buffManager != null) buffManager.onDisable();
    }

    public void onReload() {
        updateCode = System.currentTimeMillis();
        Config.onReload(this);

        /* reload 需要使用 databaseManager */
        itemManager.onReload();
        /* reload 需要使用 databaseManager */
        buffManager.onReload();
        databaseManager.onReload();

        twItemListener.onReload();
        workbenchListener.onReload();
        playerDataListener.onReload();
        particleListener.onReload();
        damageEventListener.onReload();
        updateListener.onReload();
        buffListener.onReload();

        mainCommand.onReload();
    }
}
