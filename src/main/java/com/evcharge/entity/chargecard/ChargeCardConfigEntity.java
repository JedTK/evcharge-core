package com.evcharge.entity.chargecard;


import com.xyzs.entity.BaseEntity;

import javax.swing.plaf.PanelUI;
import java.io.Serializable;
import java.util.List;

/**
 * 充电卡配置;
 *
 * @author : JED
 * @date : 2022-10-9
 */
public class ChargeCardConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 产品id
     */
    public long product_id;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     * 卡名
     */
    public String cardName;
    /**
     * 副标题
     */
    public String subtitle;
    /**
     * 封面
     */
    public String coverImage;
    /**
     * 标签列表
     */
    public String tags;
    /**
     * 单价
     */
    public double price;
    /**
     * 每日限制充电时间，单位：秒
     */
    public int dailyChargeTime;
    /**
     * 卡类型：1-数字充电卡，2-NFC-ID实体卡
     */
    public int cardTypeId;
    /**
     * 类型：1=日，2=月，3=年
     */
    public int typeId;
    /**
     * 计数值，1日，1月，1年
     */
    public int countValue;
    /**
     * 优先级别
     */
    public int priority;
    /**
     * 允许叠加
     */
    public int allowSuperposition;
    /**
     * 简短描述
     */
    public String describe;
    /**
     * 收费标准配置关联
     */
    public long chargeStandardConfigId;
    /**
     * 拥有者id
     */
    public long owner_id;
    /**
     * 组织id
     */
    public long organize_id;
    /**
     * 限制省份，0-否，1-是，有限制则检查
     */
    public int limit_provinces;
    /**
     * 限制城市，0-否，1-是，有限制则检查
     */
    public int limit_city;
    /**
     * 限制区域，0-否，1-是，有限制则检查
     */
    public int limit_district;
    /**
     * 限制街道，0-否，1-是，有限制则检查
     */
    public int limit_street;
    /**
     * 限制社区，0-否，1-是，有限制则检查
     */
    public int limit_communities;
    /**
     * 限制所购买的充电桩可用，0-否，1-是，有限制则检查
     */
    public int limit_buy_station;
    /**
     * 排除私有站，0-否，1-是，有限制则检查
     */
    public int is_exclude_private;
    /**
     * 是否关联其他充电桩，limit_buy_cs为1时有效，列如：用户购买卡A，可以在桩A、B、C中使用，检查ChargeStation层级关系
     */
    public int is_related_cs;
    /**
     * 用途类型: user,staff,partners
     */
    public String usageType;

    //endregion

    /**
     * 获得一个实例
     */
    public static ChargeCardConfigEntity getInstance() {
        return new ChargeCardConfigEntity();
    }

    /**
     * 通过配置id查询配置
     *
     * @param cardConfigId 充电卡配置ID
     * @return 充电卡配置信息
     */
    public ChargeCardConfigEntity getConfigWithId(long cardConfigId) {
        return ChargeCardConfigEntity.getInstance()
                .cache(String.format("ChargeCardConfig:%s:Details", cardConfigId))
                .findEntity(cardConfigId);
    }
    /**
     * 通过配置id查询配置
     *
     * @param productId 产品id
     * @return 充电卡配置信息
     */
    public ChargeCardConfigEntity getConfigWithProductId(long productId) {

        return ChargeCardConfigEntity.getInstance()
                .cache(String.format("ChargeCardConfigByProductId:%s:Details", productId))
                .where("product_id",productId)
                .findEntity();
    }
    /**
     * 通过配置id查询配置
     *
     * @param spu_code 充电卡配置唯一编码（慢慢从充电卡配置ID过度使用此值查询）
     * @return 充电卡配置信息
     */
    public ChargeCardConfigEntity getConfigWithCode(String spu_code) {
        return ChargeCardConfigEntity.getInstance()
                .cache(String.format("ChargeCardConfig:%s:Details", spu_code))
                .where("spu_code", spu_code)
                .findEntity();
    }
}
