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
    private SoulBindAndConsumableListener soulBindAndConsumableListener;
    private WorkbenchListener workbenchListener;
    private PlayerDataListener playerDataListener;
    private GenericParticleListener particleListener;
    private DamageListener damageListener;
    private TwItemUpdateListener updateListener;
    private SkillListener skillListener;
    private BuffListener buffListener;
    private EntityAttributeListener entityAttributeListener;

    /** 指令 */
    private MainCommand mainCommand;

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

        buffManager = new BuffManager(this);
        skillManager = new SkillManager(this);
        itemManager = new ItemManager(this); /* 顺序不可变 */
        entityAttributeManager = new EntityAttributeManager();

        soulBindAndConsumableListener = new SoulBindAndConsumableListener(this);
        workbenchListener = new WorkbenchListener(this);
        playerDataListener = new PlayerDataListener(this);
        particleListener = new GenericParticleListener(this);
        PacketEvents.getAPI().getEventManager().registerListener(particleListener, PacketListenerPriority.NORMAL);
        damageListener = new DamageListener(this);
        updateListener = new TwItemUpdateListener(this);
        buffListener = new BuffListener(this);
        skillListener = new SkillListener(this);
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
