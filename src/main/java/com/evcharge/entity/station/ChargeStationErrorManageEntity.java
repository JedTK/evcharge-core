package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 充电站异常管理;
 *
 * @author : Jay
 * @date : 2024-3-11
 */
public class ChargeStationErrorManageEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电站id
     */
    public long cs_id;
    /**
     * 异常模块 1-站点 2-设备 3-摄像头 4-烟感 5-灭火器
     */
    public int error_module;
    /**
     * 异常类型
     */
    public int error_type;
    /**
     * 内容
     */
    public String content;
    /**
     * 用户描述
     */
    public String user_desc;
    /**
     * 设备物理号
     */
    public String device_numbers;
    /**
     * 技术后台检测情况
     */
    public String tech_check_status;
    /**
     * 图片
     */
    public String error_img;
    /**
     * 异常等级 1=普通 2=紧急
     */
    public int level;
    /**
     * 备注
     */
    public String remark;
    /**
     * 状态 0-待处理 1-已处理 -1=其他
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStationErrorManageEntity getInstance() {
        return new ChargeStationErrorManageEntity();
    }

    /**
     * 创建异常处理
     *
     * @param module              异常模块
     * @param chargeStationEntity 站点实体类
     * @param deviceNumber        设备信息
     * @param content             内容
     */
    public void createMsg(int module, ChargeStationEntity chargeStationEntity, String deviceNumber, String content) {
        createMsg(module, chargeStationEntity, deviceNumber, content, 1, 0);
    }

    /**
     * 创建异常处理
     *
     * @param module              异常模块
     * @param chargeStationEntity 站点实体类
     * @param deviceNumber        设备信息
     * @param content             内容
     */
    public void createMsg(int module, ChargeStationEntity chargeStationEntity, String deviceNumber, String content, int level, int status) {
//        ChargeStationErrorManageEntity chargeStationErrorManageEntity = new ChargeStationErrorManageEntity();
//        chargeStationErrorManageEntity.error_module = module;
//        chargeStationErrorManageEntity.cs_id = Long.parseLong(chargeStationEntity.CSId);
//        chargeStationErrorManageEntity.device_numbers = deviceNumber;
//        chargeStationErrorManageEntity.content = content;
//        chargeStationErrorManageEntity.level=1;
//        chargeStationErrorManageEntity.status=status;
//        chargeStationErrorManageEntity.create_time = TimeUtil.getTimestamp();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("error_module", module);
        data.put("cs_id", Long.parseLong(chargeStationEntity.CSId));
        data.put("device_numbers", deviceNumber);
        data.put("content", content);
        data.put("level", level);
        data.put("status", status);
        data.put("create_time", TimeUtil.getTimestamp());

        ChargeStationErrorManageEntity.getInstance().insert(data);

    }


}