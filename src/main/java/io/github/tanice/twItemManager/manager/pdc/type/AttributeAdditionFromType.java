package io.github.tanice.twItemManager.manager.pdc.type;

import java.io.Serializable;

/**
 * 决定最后伤害的计算方式和顺序
 */
public enum AttributeAdditionFromType implements Serializable {
    ITEM,  /* 物品自带属性 物品词条属性 物品宝石属性.  此时的damage只有一个值（白值）,否则damage会有2个值(第第一个表示白值, 第二个表示比例) */
    ENTITY, /* 实体PDC */
    QUALITY,
    GEM,
    LEVEL,
    ACCESSORY,
    BUFF
    /* 只有 ACCESSORY 和 ITEM 看槽位 */
}
