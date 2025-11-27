package com.evcharge.entity.station;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.HttpRequestUtil;
import com.xyzs.utils.common;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 充电支付方式配置;
 * 当CSId=0时为全局配置默认开启的充电支付方式，优先以充电桩的配置
 *
 * @author : JED
 * @date : 2023-12-27
 */
public class ChargePaymentTypeConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电桩唯一编号，新增
     */
    public String CSId;
    /**
     * 充电支付方式ID
     */
    public long typeId;
    /**
     * 是否启用：0-不启用，1-启用
     */
    public int isEnabled;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargePaymentTypeConfigEntity getInstance() {
        return new ChargePaymentTypeConfigEntity();
    }

    /**
     * 获取全局充电支付方式全局配置
     */
    public List<ChargePaymentTypeConfigEntity> getGlobalConfig() {
        return getGlobalConfig(true);
    }

    /**
     * 获取全局充电支付方式全局配置
     */
    public List<ChargePaymentTypeConfigEntity> getGlobalConfig(boolean inCache) {
        if (inCache) this.cache("BaseData:ChargePaymentTypeConfig:Global");
        this.where("CSId", "0");
        return this.selectList();
    }

    /**
     * 获取全局充电支付方式全局配置
     */
    public List<ChargePaymentTypeConfigEntity> getConfigWithCSId(String CSId) {
        return getConfigWithCSId(CSId, true);
    }

    /**
     * 获取全局充电支付方式全局配置
     */
    public List<ChargePaymentTypeConfigEntity> getConfigWithCSId(String CSId, boolean inCache) {
        if (!StringUtils.hasLength(CSId) || "0".equalsIgnoreCase(CSId)) return null;
        if (inCache) this.cache(String.format("BaseData:ChargePaymentTypeConfig:%s", CSId));
        this.where("CSId", CSId);
        return this.selectList();
    }

    /**
     * 获取充电扣款方式
     * 1、先查询全局默认开启的充电支付方式
     * 2、再查询对应充电桩的充电支付方式（优先启用）
     *
     * @return
     */
    public SyncResult getConfig(String deviceCode) {
        if (!StringUtils.hasLength(deviceCode)) return new SyncResult(2, "请选择设备");
        deviceCode = deviceCode.toUpperCase();

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);
        if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "设备还未上线，请稍后再试");

        //查询全局充电支付方式配置
        List<ChargePaymentTypeConfigEntity> globalConfig = ChargePaymentTypeConfigEntity.getInstance().getGlobalConfig();
        //根据充电桩ID查询此充电桩的充电支付方式配置
        List<ChargePaymentTypeConfigEntity> chargeStationConfig = new LinkedList<>();
//        if (deviceEntity.CSId > 0) {
        if (!"".equals(deviceEntity.CSId) && !"0".equals(deviceEntity.CSId)) {
            //根据充电桩ID查询此充电桩的充电支付方式配置
            chargeStationConfig = ChargePaymentTypeConfigEntity.getInstance().getConfigWithCSId(String.format("%s", deviceEntity.CSId));
        }

        List<Object> enabled_payment_ids = new LinkedList<>();

        //先确认全局默认开启那种充电支付方式
        for (ChargePaymentTypeConfigEntity nd : globalConfig) {
            if (nd.isEnabled == 1) enabled_payment_ids.add(nd.typeId);
            else enabled_payment_ids.remove(nd.typeId);
        }
        //再确认特定的充电桩是否开启特定的充电支付方式
        for (ChargePaymentTypeConfigEntity nd : chargeStationConfig) {
            if (nd.isEnabled == 1) enabled_payment_ids.add(nd.typeId);
            else enabled_payment_ids.remove(nd.typeId);
        }

        List<Map<String, Object>> list = ChargePaymentTypeEntity.getInstance()
                .field("id,name,icon")
                .whereIn("id", enabled_payment_ids)
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(1, "");
        return new SyncResult(0, "", list);
    }
}
