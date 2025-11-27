package com.evcharge.entity.RSProfit;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 分润模式;
 * 1、消费金额*分成比例
 * 2、纯利润(消费金额-电费-其他费用)*分成比例
 * 3、电费单价*度数
 * 4、电费单价*度数+纯利润(消费金额-电费-其他费用)*分成比例
 * 5、充电端口单价*数量
 *
 * @author : JED
 * @date : 2024-7-1
 */
@Getter
@Setter
@TargetDB("evcharge_rsprofit")
public class RSProfitModeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    private long id;
    /**
     * 分润模式代码
     */
    private String mode_code;
    /**
     * 标题
     */
    private String title;
    /**
     * 详情
     */
    private String detail;
    /**
     * 优先级，数字越大越优先
     */
    private int priority;
    /**
     * 状态：0-删除，1-正常
     */
    private int status;
    /**
     * 限制单个配置单价，-1不限制，设置的单价金额不能超过设定值
     */
    private BigDecimal limitPrice;
    /**
     * 限制单个配置比率，-1不限制，设置的分成比例不能超过设定值
     */
    private BigDecimal limitRatio;
    /**
     * 限制总单价，-1不限制，总分成金额不能超过设定值
     */
    private BigDecimal limitTotalPrice;
    /**
     * 限制总比率，-1不限制，总分成比例不能超过设定值
     */
    private BigDecimal limitTotalRatio;
    /**
     * 创建时间
     */
    private long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static RSProfitModeEntity getInstance() {
        return new RSProfitModeEntity();
    }

    /**
     * 获取 分润模型信息
     *
     * @param mode_code 分润模型代码
     * @return 分润配置
     */
    public RSProfitModeEntity getWithModeCode(String mode_code) {
        return getWithModeCode(mode_code, true);
    }

    /**
     * 获取 分润模型信息
     *
     * @param mode_code 分润模型代码
     * @param inCache   是否优先从缓存中获取数据
     * @return 分润配置
     */
    public RSProfitModeEntity getWithModeCode(String mode_code, boolean inCache) {
        if (inCache) this.cache(String.format("BaseData:RSProfitMode:%s", mode_code));
        return this.where("mode_code", mode_code).findEntity();
    }
}
