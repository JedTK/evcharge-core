package com.evcharge.entity.user.disabled;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 残疾等级;
 * @author : Jay
 * @date : 2024-1-11
 */
public class DisabledLevelEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 等级标题
     */
    public String title ;
    /**
     * 充电折扣比率
     */
    public int charge_discount_rate ;
    /**
     * 备注
     */
    public String memo ;
    /**
     * 状态
     */
    public int status ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static DisabledLevelEntity getInstance() {
        return new DisabledLevelEntity();
    }

    /**
     *
     * @param levelId
     * @return
     */
    public DisabledLevelEntity getInfo(long levelId){
        return this.where("id",levelId)
                .cache(String.format("User:%s:DisableLevel",levelId),86400*1000)
                .findModel();
    }

}