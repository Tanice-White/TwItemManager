package io.github.tanice.twItemManager.manager.mmskill;

//import io.lumine.mythic.bukkit.MythicBukkit;
//import io.lumine.mythic.bukkit.BukkitAPIHelper;
//import org.bukkit.ChatColor;
//import org.bukkit.entity.Player;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * 技能管理器，负责处理技能的释放逻辑
// */
//public class SkillManager {
//
//    private final CastSkills plugin;
//    private final ConfigManager configManager;
//    private BukkitAPIHelper mythicMobsAPI;
//
//    // 用于记录技能施放时间，以防止短时间内重复释放同一技能
//    private final Map<UUID, Map<String, Long>> executionTimes = new ConcurrentHashMap<>();
//
//    /**
//     * 构造一个新的技能管理器
//     *
//     * @param plugin        插件实例
//     * @param configManager 配置管理器
//     */
//    public SkillManager(CastSkills plugin, ConfigManager configManager) {
//        this.plugin = plugin;
//        this.configManager = configManager;
//
//        initMythicMobsAPI();
//    }
//
//    /**
//     * 初始化MythicMobs API
//     */
//    private void initMythicMobsAPI() {
//        try {
//            if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
//                mythicMobsAPI = MythicBukkit.inst().getAPIHelper();
//                plugin.getColoredLogger().info("成功初始化MythicMobs API");
//            } else {
//                plugin.getColoredLogger().warning("MythicMobs插件未找到，技能释放功能将不可用");
//            }
//        } catch (Exception e) {
//            plugin.getColoredLogger().warning("初始化MythicMobs API时出错: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 检查MythicMobs中是否存在指定技能
//     *
//     * @param skillName 技能名称
//     * @return 是否存在
//     */
//    public boolean isSkillExist(String skillName) {
//        try {
//            if (MythicBukkit.inst() == null) {
//                plugin.getColoredLogger().debug("MythicMobs实例为空");
//                return false;
//            }
//
//            if (MythicBukkit.inst().getSkillManager() == null) {
//                plugin.getColoredLogger().debug("MythicMobs SkillManager为空");
//                return false;
//            }
//
//            // 使用MythicMobs的SkillManager的getSkill方法检查技能是否存在
//            Optional<?> maybeSkill = MythicBukkit.inst().getSkillManager().getSkill(skillName);
//            boolean exists = maybeSkill.isPresent();
//
//            plugin.getColoredLogger().debug("技能 " + skillName + " 是否存在: " + exists);
//            return exists;
//        } catch (Exception e) {
//            plugin.getColoredLogger().debug("检查技能是否存在时出错: " + e.getMessage());
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * 获取所有可用的技能名称
//     *
//     * @return 所有技能名称
//     */
//    public String getAvailableSkills() {
//        StringBuilder sb = new StringBuilder();
//        try {
//            if (MythicBukkit.inst() == null || MythicBukkit.inst().getSkillManager() == null) {
//                return "无法获取MythicMobs技能列表";
//            }
//
//            // 获取所有技能
//            java.util.Collection<?> skills = MythicBukkit.inst().getSkillManager().getSkills();
//            if (skills != null && !skills.isEmpty()) {
//                for (Object skill : skills) {
//                    sb.append(skill.toString()).append(", ");
//                }
//                if (sb.length() > 2) {
//                    sb.setLength(sb.length() - 2);
//                }
//            } else {
//                sb.append("技能列表为空");
//            }
//        } catch (Exception e) {
//            sb.append("获取技能列表出错: ").append(e.getMessage());
//        }
//        return sb.toString();
//    }
//
//    /**
//     * 施放技能
//     *
//     * @param player    玩家
//     * @param skillName 技能名称
//     * @return 是否成功施放
//     */
//    public boolean castSkill(Player player, String skillName) {
//        if (player == null || skillName == null || skillName.isEmpty()) {
//            plugin.getColoredLogger().debug("castSkill: 参数无效，player=" + (player != null ? player.getName() : "null") + ", skillName=" + skillName);
//            return false;
//        }
//
//        plugin.getColoredLogger().debugWithKey("skill_attempt", "player", player.getName(), "skill", skillName);
//
//        try {
//            // 如果MythicMobs API为空，尝试重新获取
//            if (mythicMobsAPI == null) {
//                plugin.getColoredLogger().debug("MythicMobs API为空，尝试重新获取");
//                try {
//                    if (MythicBukkit.inst() != null) {
//                        mythicMobsAPI = MythicBukkit.inst().getAPIHelper();
//                        plugin.getColoredLogger().debug("成功重新获取MythicMobs API");
//                    } else {
//                        plugin.getColoredLogger().debug("MythicBukkit.inst()为空，无法获取API");
//                        player.sendMessage(ChatColor.RED + "技能释放失败：MythicMobs插件未加载");
//                        return false;
//                    }
//                } catch (Exception e) {
//                    plugin.getColoredLogger().debug("无法获取MythicMobs API: " + e.getMessage());
//                    e.printStackTrace();
//                    player.sendMessage(ChatColor.RED + "技能释放失败，请联系服务器管理员");
//                    return false;
//                }
//            }
//
//            // 检查技能是否存在
//            boolean skillExists = isSkillExist(skillName);
//
//            if (!skillExists) {
//                plugin.getColoredLogger().debug("技能 " + skillName + " 不存在!");
//                player.sendMessage(ChatColor.RED + "技能 " + skillName + " 不存在，请联系服务器管理员");
//
//                // 输出所有可用技能
//                String availableSkills = getAvailableSkills();
//                plugin.getColoredLogger().debug("可用技能列表: " + availableSkills);
//                return false;
//            }
//
//            // 防止重复释放同一技能
//            UUID playerUUID = player.getUniqueId();
//            Map<String, Long> playerExecutionTimes = executionTimes.computeIfAbsent(playerUUID, k -> new HashMap<>());
//            long currentTime = System.currentTimeMillis();
//            long lastExecutionTime = playerExecutionTimes.getOrDefault(skillName, 0L);
//            if (currentTime - lastExecutionTime < 200) return false;
//
//            // 记录此次技能施放时间
//            playerExecutionTimes.put(skillName, currentTime);
//
//            // 优先使用MythicMobs API的标准方法，确保正确设置施法者
//            try {
//                // 确保使用正确的施法者，这样projectile技能的伤害才会归属于玩家
//                // 1. 首先转换玩家为MythicMobs可识别的AbstractEntity
//                Object bukkitAdapter = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter").getDeclaredMethod("adapt", org.bukkit.entity.Entity.class).invoke(null, player);
//
//                // 2. 创建SkillCaster对象（正确设置施法者）
//                Object skillCaster = null;
//                try {
//                    // 尝试创建PlayerSkillCaster
//                    Class<?> playerSkillCasterClass = Class.forName("io.lumine.xikage.mythicmobs.skills.PlayerSkillCaster");
//                    java.lang.reflect.Constructor<?> constructor = playerSkillCasterClass.getConstructor(Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractEntity"));
//                    skillCaster = constructor.newInstance(bukkitAdapter);
//                } catch (Exception e) {
//                    // 如果找不到PlayerSkillCaster，回退到TemporaryCaster
//                    try {
//                        Class<?> temporaryCasterClass = Class.forName("io.lumine.mythic.api.mobs.GenericCaster");
//                        java.lang.reflect.Constructor<?> constructor = temporaryCasterClass.getConstructor(Class.forName("io.lumine.mythic.api.adapters.AbstractEntity"));
//                        skillCaster = constructor.newInstance(bukkitAdapter);
//                    } catch (Exception ex) {
//                        // 最后尝试通过MythicEntity获取
//                        Class<?> mythicEntityClass = Class.forName("io.lumine.mythic.core.adapters.VirtualEntity");
//                        java.lang.reflect.Method getMythicEntityMethod = mythicEntityClass.getMethod("get", org.bukkit.entity.Entity.class);
//                        Object mythicEntity = getMythicEntityMethod.invoke(null, player);
//
//                        if (mythicEntity != null) {
//                            java.lang.reflect.Method getCasterMethod = mythicEntity.getClass().getMethod("getCaster");
//                            skillCaster = getCasterMethod.invoke(mythicEntity);
//                        }
//                    }
//                }
//
//                // 3. 调用MythicMobs API执行技能
//                if (skillCaster != null && mythicMobsAPI != null) {
//                    // 反射获取castSkill方法
//                    for (java.lang.reflect.Method method : mythicMobsAPI.getClass().getMethods()) {
//                        if (method.getName().equals("castSkill")) {
//                            try {
//                                method.invoke(mythicMobsAPI, skillCaster, skillName, player, player.getLocation(), null, 1.0f);
//                                return true;
//                            } catch (Exception e) {
//                                // 尝试不同的参数组合
//                                try {
//                                    method.invoke(mythicMobsAPI, skillCaster, skillName, bukkitAdapter, bukkitAdapter, null, 1.0f);
//                                    return true;
//                                } catch (Exception ex) {
//                                    // 继续尝试其他方法
//                                }
//                            }
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                // 如果精确方法失败，继续尝试其他方法
//            }
//
//            // 如果上面的方法失败，回退到其他方法尝试
//            boolean result = false;
//
//            // 方法2: 尝试使用临时施法者对象释放技能
//            if (!result) {
//                plugin.getColoredLogger().debug("方法2：尝试创建临时施法者释放技能");
//                try {
//                    // 尝试不同的类路径
//                    String[] casterClassPaths = {
//                        "io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitPlayer",
//                        "io.lumine.xikage.mythicmobs.mobs.ActiveMob",
//                        "io.lumine.xikage.mythicmobs.skills.SkillCaster",
//                        "io.lumine.xikage.mythicmobs.skills.SkillMetadata"
//                    };
//
//                    for (String classPath : casterClassPaths) {
//                        try {
//                            Class<?> casterClass = Class.forName(classPath);
//                            plugin.getColoredLogger().debug("找到类: " + classPath);
//
//                            // 查找相关方法
//                            for (java.lang.reflect.Method method : casterClass.getDeclaredMethods()) {
//                                if (method.getName().contains("cast") || method.getName().contains("Cast") ||
//                                    method.getName().contains("skill") || method.getName().contains("Skill")) {
//                                    plugin.getColoredLogger().debug("在类 " + classPath + " 中找到方法: " + method.getName());
//                                }
//                            }
//
//                            // 尝试通过反射获取相关实例
//                            if (classPath.contains("BukkitPlayer")) {
//                                // 尝试获取BukkitPlayer的构造函数
//                                try {
//                                    java.lang.reflect.Constructor<?> constructor = casterClass.getDeclaredConstructor(org.bukkit.entity.Entity.class);
//                                    constructor.setAccessible(true);
//                                    Object caster = constructor.newInstance(player);
//
//                                    // 尝试调用castSkill方法
//                                    java.lang.reflect.Method castMethod = findMethod(casterClass, "castSkill", String.class);
//                                    if (castMethod != null) {
//                                        castMethod.setAccessible(true);
//                                        result = (Boolean) castMethod.invoke(caster, skillName);
//                                        plugin.getColoredLogger().debug("通过BukkitPlayer施放技能结果: " + result);
//                                        if (result) return true;
//                                    }
//                                } catch (Exception e) {
//                                    plugin.getColoredLogger().debug("无法使用BukkitPlayer: " + e.getMessage());
//                                }
//                            }
//                        } catch (ClassNotFoundException e) {
//                            plugin.getColoredLogger().debug("类未找到: " + classPath);
//                        }
//                    }
//
//                    // 使用API的MythicMob类进行尝试
//                    try {
//                        // 尝试获取ActiveMob
//                        if (mythicMobsAPI != null) {
//                            plugin.getColoredLogger().debug("尝试使用MythicMob对象");
//                            Object mythicMob = mythicMobsAPI.getMythicMobInstance(player);
//                            if (mythicMob != null) {
//                                plugin.getColoredLogger().debug("成功获取MythicMob实例");
//                                // 查找和尝试所有可能的方法
//                                for (java.lang.reflect.Method method : mythicMob.getClass().getMethods()) {
//                                    if ((method.getName().contains("cast") || method.getName().contains("Cast")) &&
//                                        method.getParameterCount() == 1 &&
//                                        method.getParameterTypes()[0] == String.class) {
//                                        try {
//                                            method.setAccessible(true);
//                                            result = (Boolean) method.invoke(mythicMob, skillName);
//                                            plugin.getColoredLogger().debug("通过MythicMob." + method.getName() + "施放技能结果: " + result);
//                                            if (result) return true;
//                                        } catch (Exception e) {
//                                            plugin.getColoredLogger().debug("调用MythicMob." + method.getName() + "失败: " + e.getMessage());
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    } catch (Exception e) {
//                        plugin.getColoredLogger().debug("使用MythicMob对象失败: " + e.getMessage());
//                    }
//                } catch (Exception e) {
//                    plugin.getColoredLogger().debug("使用临时施法者失败: " + e.getMessage());
//                }
//            }
//
//            // 如果上述两种方法失败，尝试使用API的各种方法（作为备选）
//            if (!result) {
//                plugin.getColoredLogger().debug("尝试使用API方法释放技能");
//                result = tryAPIMethodsToExecuteSkill(player, skillName);
//            }
//
//            plugin.getColoredLogger().debug("最终技能施放结果: " + result);
//
//            if (result) {
//                // 技能释放成功日志，根据debug设置决定是否输出详细信息
//                plugin.getColoredLogger().debugWithKey("skill_cast_success", "player", player.getName(), "skill", skillName);
//
//                // 只发送动作条提示，移除聊天框提示
//                String actionBarMessage = configManager.getActionBarSkillTriggeredMessage()
//                        .replace("{skill}", skillName);
//                cn.i7mc.castskills.util.MessageUtil.sendActionBar(player, actionBarMessage);
//            } else {
//                plugin.getColoredLogger().warning(player.getName() + " 尝试释放技能 " + skillName + " 失败");
//
//                if (skillExists) {
//                    // 只发送动作条提示，移除聊天框提示
//                    cn.i7mc.castskills.util.MessageUtil.sendActionBar(player,
//                            configManager.getActionBarSkillCastFailedMessage());
//                } else {
//                    // 只发送动作条提示，移除聊天框提示
//                    cn.i7mc.castskills.util.MessageUtil.sendActionBar(player,
//                            configManager.getActionBarSkillCastFailedMessage());
//                }
//            }
//
//            return result;
//        } catch (Exception e) {
//            plugin.getColoredLogger().warning("释放技能 " + skillName + " 时出错: " + e.getMessage());
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * 尝试使用各种API方法执行技能
//     *
//     * @param player 玩家
//     * @param skillName 技能名称
//     * @return 是否成功
//     */
//    private boolean tryAPIMethodsToExecuteSkill(Player player, String skillName) {
//        try {
//            boolean result = false;
//
//            // 尝试方法1: 使用基本方法
//            try {
//                result = mythicMobsAPI.castSkill(player, skillName);
//                plugin.getColoredLogger().debug("API方法1结果: " + result);
//            } catch (Exception e) {
//                plugin.getColoredLogger().debug("API方法1异常: " + e.getMessage());
//            }
//
//            // 如果方法1失败，尝试方法2: 使用权限值参数
//            if (!result) {
//                try {
//                    result = mythicMobsAPI.castSkill(player, skillName, 1.0f);
//                    plugin.getColoredLogger().debug("API方法2结果: " + result);
//                } catch (Exception e) {
//                    plugin.getColoredLogger().debug("API方法2异常: " + e.getMessage());
//                }
//            }
//
//            // 如果方法2失败，尝试方法3: 使用位置参数
//            if (!result) {
//                try {
//                    result = mythicMobsAPI.castSkill(player, skillName, player.getLocation());
//                    plugin.getColoredLogger().debug("API方法3结果: " + result);
//                } catch (Exception e) {
//                    plugin.getColoredLogger().debug("API方法3异常: " + e.getMessage());
//                }
//            }
//
//            // 如果方法3失败，尝试方法4: 使用位置和权限值参数
//            if (!result) {
//                try {
//                    result = mythicMobsAPI.castSkill(player, skillName, player.getLocation(), 1.0f);
//                    plugin.getColoredLogger().debug("API方法4结果: " + result);
//                } catch (Exception e) {
//                    plugin.getColoredLogger().debug("API方法4异常: " + e.getMessage());
//                }
//            }
//
//            // 如果API方法都失败，尝试方法5: 直接使用MythicMobs内部执行器
//            if (!result) {
//                try {
//                    plugin.getColoredLogger().debug("尝试使用MythicMobs内部执行器");
//                    // 获取MythicMobs主实例
//                    Object mmInstance = MythicBukkit.inst();
//
//                    // 尝试获取getSkillManager方法
//                    java.lang.reflect.Method getSkillManagerMethod = mmInstance.getClass().getMethod("getSkillManager");
//                    Object skillManager = getSkillManagerMethod.invoke(mmInstance);
//
//                    // 尝试获取getSkill方法
//                    java.lang.reflect.Method getSkillMethod = skillManager.getClass().getMethod("getSkill", String.class);
//                    Optional<?> skillOptional = (Optional<?>) getSkillMethod.invoke(skillManager, skillName);
//
//                    if (skillOptional.isPresent()) {
//                        Object skill = skillOptional.get();
//                        plugin.getColoredLogger().debug("成功获取技能对象: " + skill.getClass().getName());
//
//                        // 尝试创建TemporaryCaster和SkillMetadata
//                        result = executeWithSkillMetadata(player, skillName, skill);
//                        if (result) {
//                            plugin.getColoredLogger().debug("通过创建临时SkillMetadata执行技能成功!");
//                            return true;
//                        }
//
//                        // 如果上面的方法失败，尝试直接执行skill对象
//                        // 尝试不同的方法名和参数组合
//                        String[] methodNames = {"execute", "cast", "apply", "run"};
//
//                        for (String methodName : methodNames) {
//                            // 尝试查找具有不同参数的方法
//                            for (java.lang.reflect.Method method : skill.getClass().getMethods()) {
//                                if (method.getName().equals(methodName)) {
//                                    plugin.getColoredLogger().debug("找到方法: " + method);
//
//                                    try {
//                                        // 检查参数数量
//                                        int paramCount = method.getParameterCount();
//                                        Object[] params = new Object[paramCount];
//
//                                        // 填充参数数组
//                                        for (int i = 0; i < paramCount; i++) {
//                                            Class<?> paramType = method.getParameterTypes()[i];
//
//                                            // 尝试根据参数类型填充合适的值
//                                            if (paramType.isAssignableFrom(player.getClass())) {
//                                                params[i] = player;
//                                            } else if (paramType.getName().contains("Caster") ||
//                                                    paramType.getName().contains("Entity")) {
//                                                // 尝试创建临时Caster
//                                                try {
//                                                    // 尝试找到合适的Adapter或转换方法
//                                                    Class<?> adapterClass = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
//                                                    for (java.lang.reflect.Method adapterMethod : adapterClass.getMethods()) {
//                                                        if (adapterMethod.getReturnType().getName().contains(paramType.getSimpleName()) &&
//                                                                adapterMethod.getParameterCount() == 1 &&
//                                                                adapterMethod.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
//                                                            plugin.getColoredLogger().debug("使用转换方法: " + adapterMethod);
//                                                            params[i] = adapterMethod.invoke(null, player);
//                                                            break;
//                                                        }
//                                                    }
//                                                } catch (Exception e) {
//                                                    plugin.getColoredLogger().debug("创建Caster参数失败: " + e.getMessage());
//                                                    // 如果创建失败则设为null
//                                                    params[i] = null;
//                                                }
//                                            } else if (paramType == Float.TYPE || paramType == Float.class) {
//                                                params[i] = 1.0f; // 技能强度
//                                            } else if (paramType == Integer.TYPE || paramType == Integer.class) {
//                                                params[i] = 1; // 技能等级
//                                            } else if (paramType == Boolean.TYPE || paramType == Boolean.class) {
//                                                params[i] = true;
//                                            } else if (paramType == String.class) {
//                                                params[i] = skillName;
//                                            } else {
//                                                // 其他类型参数设为null
//                                                params[i] = null;
//                                            }
//                                        }
//
//                                        // 执行方法
//                                        method.setAccessible(true);
//                                        Object methodResult = method.invoke(skill, params);
//                                        plugin.getColoredLogger().debug("执行结果: " + methodResult);
//
//                                        if (methodResult instanceof Boolean) {
//                                            result = (Boolean) methodResult;
//                                            if (result) {
//                                                plugin.getColoredLogger().debug("成功使用方法 " + method + " 执行技能");
//                                                return true;
//                                            }
//                                        } else if (methodResult != null) {
//                                            // 如果返回非布尔值但不为null，认为成功
//                                            plugin.getColoredLogger().debug("方法执行返回非布尔值: " + methodResult);
//                                            result = true;
//                                            return true;
//                                        }
//                                    } catch (Exception e) {
//                                        plugin.getColoredLogger().debug("执行方法 " + method + " 失败: " + e.getMessage());
//                                    }
//                                }
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    plugin.getColoredLogger().debug("使用内部执行器失败: " + e.getMessage());
//                }
//            }
//
//            // 如果还是失败，尝试方法6: 使用事件触发
//            if (!result) {
//                try {
//                    plugin.getColoredLogger().debug("尝试通过事件触发技能");
//
//                    // 通过反射获取相关类和方法
//                    Class<?> skillTriggerEventClass = Class.forName("io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent");
//                    if (skillTriggerEventClass != null) {
//                        // 尝试创建事件
//                        java.lang.reflect.Constructor<?> constructor = null;
//                        for (java.lang.reflect.Constructor<?> c : skillTriggerEventClass.getConstructors()) {
//                            plugin.getColoredLogger().debug("找到构造函数: " + c);
//                            constructor = c;
//                            if (c.getParameterCount() == 0 ||
//                                (c.getParameterCount() == 1 && c.getParameterTypes()[0] == String.class)) {
//                                constructor = c;
//                                break;
//                            }
//                        }
//
//                        if (constructor != null) {
//                            constructor.setAccessible(true);
//                            Object event;
//                            if (constructor.getParameterCount() == 1) {
//                                event = constructor.newInstance(skillName);
//                            } else {
//                                event = constructor.newInstance();
//                            }
//
//                            // 查找设置技能名称的方法
//                            for (java.lang.reflect.Method method : event.getClass().getMethods()) {
//                                if (method.getName().startsWith("set") &&
//                                    method.getParameterCount() == 1 &&
//                                    method.getParameterTypes()[0] == String.class) {
//                                    method.invoke(event, skillName);
//                                }
//                            }
//
//                            // 触发事件
//                            plugin.getServer().getPluginManager().callEvent((org.bukkit.event.Event)event);
//                            result = true;
//                        }
//                    }
//                } catch (Exception e) {
//                    plugin.getLogger().info("[调试-SkillManager] 通过事件触发失败: " + e.getMessage());
//                }
//            }
//
//            return result;
//        } catch (Exception e) {
//            plugin.getLogger().warning("[调试-SkillManager] API调用出错: " + e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * 使用正确设置的SkillMetadata执行技能
//     *
//     * @param player 玩家
//     * @param skillName 技能名称
//     * @param skillObject Skill对象
//     * @return 是否成功执行
//     */
//    private boolean executeWithSkillMetadata(Player player, String skillName, Object skillObject) {
//        try {
//            // 1. 创建SkillTrigger
//            Class<?> skillTriggerClass = Class.forName("io.lumine.mythic.core.skills.SkillTriggers");
//            Object trigger = null;
//            for (Object enumConstant : skillTriggerClass.getEnumConstants()) {
//                if (enumConstant.toString().equals("API") || enumConstant.toString().equals("CAST")) {
//                    trigger = enumConstant;
//                    break;
//                }
//            }
//
//            if (trigger == null) {
//                plugin.getColoredLogger().debug("无法找到合适的SkillTrigger");
//                return false;
//            }
//
//            // 2. 创建AbstractEntity
//            Class<?> adapterClass = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
//            java.lang.reflect.Method adaptEntityMethod = adapterClass.getMethod("adapt", org.bukkit.entity.Entity.class);
//            Object abstractEntity = adaptEntityMethod.invoke(null, player);
//
//            // 3. 创建AbstractLocation
//            java.lang.reflect.Method adaptLocationMethod = adapterClass.getMethod("adapt", org.bukkit.Location.class);
//            Object abstractLocation = adaptLocationMethod.invoke(null, player.getLocation());
//
//            // 4. 创建TemporaryCaster
//            Object skillCaster = null;
//            try {
//                // 尝试创建TemporaryCaster (4.4版本)
//                Class<?> temporaryCasterClass = Class.forName("io.lumine.mythic.core.adapters.VirtualEntity");
//                java.lang.reflect.Method getMythicEntityMethod = temporaryCasterClass.getMethod("get", org.bukkit.entity.Entity.class);
//                Object mythicEntity = getMythicEntityMethod.invoke(null, player);
//
//                if (mythicEntity != null) {
//                    java.lang.reflect.Method getCasterMethod = mythicEntity.getClass().getMethod("getCaster");
//                    skillCaster = getCasterMethod.invoke(mythicEntity);
//                }
//            } catch (Exception e) {
//                plugin.getColoredLogger().debug("创建MythicEntity失败，尝试其他方式: " + e.getMessage());
//
//                // 回退方法1: 尝试自定义实现SkillCaster接口
//                try {
//                    Class<?> skillCasterClass = Class.forName("io.lumine.mythic.api.skills.SkillCaster");
//                    skillCaster = java.lang.reflect.Proxy.newProxyInstance(
//                        this.getClass().getClassLoader(),
//                        new Class<?>[] { skillCasterClass },
//                        new java.lang.reflect.InvocationHandler() {
//                            @Override
//                            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
//                                String methodName = method.getName();
//                                if (methodName.equals("getEntity")) {
//                                    return abstractEntity;
//                                } else if (methodName.equals("getLocation")) {
//                                    return abstractLocation;
//                                } else if (methodName.equals("getPower")) {
//                                    return 1.0f;
//                                } else if (methodName.equals("getLevel")) {
//                                    return 1;
//                                } else if (methodName.equals("setUsingDamageSkill")) {
//                                    return null;
//                                } else if (methodName.equals("isUsingDamageSkill")) {
//                                    return false;
//                                }
//                                return null;
//                            }
//                        });
//                } catch (Exception ex) {
//                    plugin.getColoredLogger().debug("创建代理SkillCaster失败: " + ex.getMessage());
//                }
//            }
//
//            if (skillCaster == null) {
//                plugin.getColoredLogger().debug("无法创建SkillCaster对象");
//                return false;
//            }
//
//            // 5. 创建HashSet
//            java.util.HashSet<Object> entityTargets = new java.util.HashSet<>();
//            entityTargets.add(abstractEntity);
//
//            java.util.HashSet<Object> locationTargets = new java.util.HashSet<>();
//            locationTargets.add(abstractLocation);
//
//            // 6. 创建SkillMetadata
//            Class<?> skillMetadataClass = Class.forName("io.lumine.mythic.core.skills.SkillMetadataImpl");
//            java.lang.reflect.Constructor<?> constructor = skillMetadataClass.getConstructor(
//                skillTriggerClass,
//                Class.forName("io.lumine.mythic.api.skills.SkillCaster"),
//                Class.forName("io.lumine.mythic.api.adapters.AbstractEntity"),
//                Class.forName("io.lumine.mythic.api.adapters.AbstractLocation"),
//                java.util.HashSet.class,
//                java.util.HashSet.class,
//                float.class
//            );
//
//            Object skillMetadata = constructor.newInstance(
//                trigger,
//                skillCaster,
//                abstractEntity,
//                abstractLocation,
//                entityTargets,
//                locationTargets,
//                1.0f
//            );
//
//            // 7. 调用execute方法
//            java.lang.reflect.Method executeMethod = skillObject.getClass().getMethod("execute", skillMetadataClass);
//            executeMethod.invoke(skillObject, skillMetadata);
//
//            // 如果没有抛出异常，认为执行成功
//            return true;
//        } catch (Exception e) {
//            plugin.getLogger().info("[调试-SkillManager] executeWithSkillMetadata失败: " + e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * 清理过期的技能施放记录
//     */
//    public void cleanupExecutionTimes() {
//        long currentTime = System.currentTimeMillis();
//
//        for (Map.Entry<UUID, Map<String, Long>> entry : executionTimes.entrySet()) {
//            Map<String, Long> playerTimes = entry.getValue();
//            playerTimes.entrySet().removeIf(time -> currentTime - time.getValue() > 60000); // 1分钟过期
//
//            // 如果玩家没有执行记录，移除该玩家的记录
//            if (playerTimes.isEmpty()) {
//                executionTimes.remove(entry.getKey());
//            }
//        }
//    }
//
//    /**
//     * 检查MythicMobs API是否可用
//     *
//     * @return 是否可用
//     */
//    public boolean isMythicMobsAPIAvailable() {
//        return mythicMobsAPI != null;
//    }
//
//    /**
//     * 辅助方法：查找指定名称和参数的方法
//     */
//    private java.lang.reflect.Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
//        try {
//            return clazz.getDeclaredMethod(methodName, paramTypes);
//        } catch (NoSuchMethodException e) {
//            // 尝试查找公共方法
//            try {
//                return clazz.getMethod(methodName, paramTypes);
//            } catch (NoSuchMethodException e2) {
//                // 尝试查找包含名称的所有方法
//                for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
//                    if (method.getName().equals(methodName) &&
//                        method.getParameterCount() == paramTypes.length) {
//                        boolean matches = true;
//                        Class<?>[] actualTypes = method.getParameterTypes();
//                        for (int i = 0; i < paramTypes.length; i++) {
//                            if (!actualTypes[i].isAssignableFrom(paramTypes[i]) &&
//                                !paramTypes[i].isAssignableFrom(actualTypes[i])) {
//                                matches = false;
//                                break;
//                            }
//                        }
//                        if (matches) return method;
//                    }
//                }
//                return null;
//            }
//        }
//    }
//}