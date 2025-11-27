package com.evcharge.entity.device;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 设备检查日志;
 * @author : Jay
 * @date : 2025-7-3
 */
public class GeneralDeviceCheckLogEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 设备名称
     */
    public String device_name ;
    /**
     * 序列号
     */
    public String serial_number ;
    /**
     * 品牌编码
     */
    public String brand_code ;
    /**
     * 类型编码
     */
    public String type_code ;
    /**
     * 检查时间
     */
    public long check_time ;
    /**
     * 检查状态 1=正常 2=异常
     */
    public int check_status ;
    /**
     * 回调信息
     */
    public String callback_content ;
    /**
     * 回调信息
     */
    public String callback_message ;
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
    public static GeneralDeviceCheckLogEntity getInstance() {
        return new GeneralDeviceCheckLogEntity();
    }
}