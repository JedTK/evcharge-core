package com.evcharge.entity.inspect.log;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 巡检日志明细表;
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("inspect")
public class InspectLogDetailEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 关联的日志ID
     */
    public long log_id ;
    /**
     * 关联的巡检项目ID
     */
    public long item_id ;
    /**
     * 巡检项目名称
     */
    public String item_name ;
    /**
     * 如果有异常，填写异常信息
     */
    public String data_value ;
    /**
     * 备注
     */
    public String remark ;
    /**
     * 状态 0=巡检中 1=正常 2=异常
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
    public static InspectLogDetailEntity getInstance() {
        return new InspectLogDetailEntity();
    }


    /**
     * 检查日志明细是否存在异常 如果存在异常 返回true 如果没有存在异常 返回false
     * @param logId 日志id
     * @return boolean
     */
    public Boolean checkDetailError(long logId){
        return this.where("log_id",logId).where("status",2).count()>0;
    }


}