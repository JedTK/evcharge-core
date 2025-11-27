package com.evcharge.entity.inspect.station;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


@TargetDB("inspect")
public class ChargeStationInfoTemplateEntity extends BaseEntity implements Serializable {

    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 父级id
     */
    public long parent_id ;
    /**
     * 物料名称
     */
    public String name ;
    /**
     * 物料描述
     */
    public String description ;
    /**
     * 计量单位
     */
    public String unit ;
    /**
     * 值类型 int,string,bool
     */
    public String value_type ;
    /**
     * 可选项
     */
    public String options ;
    /**
     * 状态 0=启用 1=停用
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
    public static ChargeStationInfoTemplateEntity getInstance() {
        return new ChargeStationInfoTemplateEntity();
    }

    public List<Map<String,Object>> getDefaultTemplate(){
        return this.where("status",0).select();
    }


}
