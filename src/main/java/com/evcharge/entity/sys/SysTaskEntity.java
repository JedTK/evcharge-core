package com.evcharge.entity.sys;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 系统任务;
 * @author : JED
 * @date : 2022-11-9
 */
public class SysTaskEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id ;
    /**
     * 任务中文名
     */
    public String taskText ;
    /**
     * 任务分组，英文，调度器使用
     */
    public String taskGroupName ;
    /**
     * 状态，0=停止，1=待执行，2=执行中
     */
    public int status ;
    /**
     * cron表达式
     */
    public String cron ;
    /**
     * 备注
     */
    public String remark ;
    /**
     * 任务具体执行的类名
     */
    public String JobClass ;
    /**
     * 任务具体执行时传入的参数,json格式
     */
    public String JobData ;
    /**
     * 绑定具体服务器运行
     */
    public String ServerIPv4 ;
    /**
     * 创建时间
     */
    public long create_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static SysTaskEntity getInstance() {
        return new SysTaskEntity();
    }
}
