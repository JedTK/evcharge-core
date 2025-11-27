package com.evcharge.entity.active.integral;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.util.Map;

/**
 * 积分模版;
 * @author : Jay
 * @date : 2023-11-30
 */
public class ActiveIntegralTempV1Entity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 规则key，用于缓存或其他用读取用途;''
     */
    public String rule_key ;
    /**
     * 规则名;''
     */
    public String rule_name ;
    /**
     * 规则配置ID;0
     */
    public long setting_id ;
    /**
     * 状态：1-下架，0上架;
     */
    public int status ;
    /**
     * 是否允许使用：0=关闭使用，1=允许使用;0
     */
    public int use_status ;
    /**
     * 发放总量;0
     */
    public int count ;
    /**
     * 积分金额;0
     */
    public int integral ;
    /**
     * 领取限制：每个用户能领取的数量;0
     */
    public int can_get_count ;
    /**
     * 其他限制：固定日期：生效时间;0
     */
    public long start_time ;
    /**
     * 其他限制：固定日期：过期时间;0
     */
    public long end_time ;
    /**
     * 其他限制：N天内有效;0
     */
    public int n_day ;
    /**
     * 备注;''
     */
    public String remark ;
    /**
     * ip;''
     */
    public String ip ;
    /**
     * 创建时间;0
     */
    public long create_time ;
    /**
     * 更新时间;0
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ActiveIntegralTempV1Entity getInstance() {
        return new ActiveIntegralTempV1Entity();
    }

    /**
     * 根据规则key获取优惠券信息
     * @param ruleKey
     * @return
     */
    public ActiveIntegralTempV1Entity getRuleByKey(String ruleKey){
        return this.cache(String.format("Active:Integral:TempV1Info:%s", ruleKey), 86400 * 1000)
                .where("rule_key", ruleKey)
                .findModel();
    }

    public ActiveIntegralTempV1Entity getRuleById(long id){
        return this.cache(String.format("Active:Integral:TempV1Info:%s", id), 86400 * 1000)
                .where("id", id)
                .findModel();
    }

    public String createDesc(long settingId, ActiveIntegralTempV1Entity activeIntegralTempV1Entity){


        return  "";
    }




}