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
import io.github.tanice.twItemManager.manager.player.EntityAttributeManager;
import io.github.tanice.twItemManager.manager.skill.SkillManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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
    @Getter
    private static SkillManager skillManager;
    @Getter
    private static EntityAttributeManager entityAttributeManager;

    /** 监听器 */
    private static SoulBindAndConsumableListener soulBindAndConsumableListener;
    private static WorkbenchListener workbenchListener;
    private static PlayerDataListener playerDataListener;
    private static GenericParticleListener particleListener;
    private static DamageListener damageListener;
    private static TwItemUpdateListener updateListener;
    private static MMSkillListener mmSkillListener;
    private static BuffListener buffListener;
    private static EntityAttributeListener entityAttributeListener;

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
        skillManager = new SkillManager(this);
        buffListener = new BuffListener(this);
        entityAttributeManager = new EntityAttributeManager();

        soulBindAndConsumableListener = new SoulBindAndConsumableListener(this);
        workbenchListener = new WorkbenchListener(this);
        playerDataListener = new PlayerDataListener(this);
        particleListener = new GenericParticleListener(this);
        PacketEvents.getAPI().getEventManager().registerListener(particleListener, PacketListenerPriority.NORMAL);
        damageListener = new DamageListener(this);
        updateListener = new TwItemUpdateListener(this);

        Plugin mythicMobs = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (mythicMobs != null && mythicMobs.isEnabled()) {
            getLogger().info("已启用MythicMobs, 初始化相关功能...");
            mmSkillListener = new MMSkillListener(this);
        } else {
            getLogger().info("未找到 MythicMobs, 安装 MythicMobs 以使用此插件的全部功能");
        }

        entityAttributeListener = new EntityAttributeListener(this);

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
        if (itemManager != null) itemManager.onDisable();
        if (buffManager != null) buffManager.onDisable();
        if (skillManager != null) skillManager.OnDisable();
        if (entityAttributeManager != null) entityAttributeManager.onDisable();

        if (mainCommand != null) mainCommand.onDisable();
    }

    public void onReload() {
        updateCode = System.currentTimeMillis();
        Config.onReload(this);

        /* reload 需要使用 databaseManager */
        itemManager.onReload();
        /* reload 需要使用 databaseManager */
        buffManager.onReload();
        databaseManager.onReload();  // 在两者之后 reload
        skillManager.onReload();
        entityAttributeManager.onReload();

        /* Listener中需要Config中的值初始化才需要reload */
        particleListener.onReload();
        damageListener.onReload();

        mainCommand.onReload();
    }
}
