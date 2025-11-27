package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * 充电桩一般数据;
 *
 * @author : JED
 * @date : 2024-1-16
 */
public class ChargeStationGeneralDataEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电桩编码
     */
    public String CSCode;
    /**
     * 充电桩类型：0-直流桩，1-交流桩
     */
    public int typeId;
    /**
     * 插座数量/充电枪数量
     */
    public Integer socketCount;
    /**
     * IMEI编码
     */
    public String IMEI;
    /**
     * SIM卡号
     */
    public String simcode;
    /**
     * 通信协议版本
     */
    public String protocolVersion;
    /**
     * 程序版本
     */
    public String softwareVersion;
    /**
     * 网络链接类型
     */
    public String netWorkType;
    /**
     * 运营商
     */
    public String carrier;
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
    public static ChargeStationGeneralDataEntity getInstance() {
        return new ChargeStationGeneralDataEntity();
    }

    /**
     * 新增或更新数据
     *
     * @param CSCode 充电桩编码
     * @param data   数据
     * @return
     */
    public boolean updateData(String CSCode, Map<String, Object> data) {
        if (!StringUtils.hasLength(CSCode)) return false;
        data.put("update_time", TimeUtil.getTimestamp());
        int noquery;
        if (!this.where("CSCode", CSCode).exist()) {
            data.put("CSCode", CSCode);
            noquery = this.insert(data);
        } else {
            noquery = this.where("CSCode", CSCode).update(data);
        }
        return noquery > 0;
    }
}
