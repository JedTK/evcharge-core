package com.evcharge.entity.station.bill;


import com.evcharge.entity.platform.PlatformMonthSummaryV2Entity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.station.ChargeStationMonthSummaryV2Entity;
import com.evcharge.entity.station.ElectricityMeterEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * null;
 *
 * @author : Jay
 * @date : 2024-2-20
 */
public class ElectricityPowerSupplyBillEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 电表id
     */
    public long meter_id;
    /**
     * 户号
     */
    public String account_number;
    /**
     * 账单日期
     */
    public String bill_date;
    /**
     * 账单日期时间戳
     */
    public long bill_date_time;
    /**
     * 计费周期
     */
    public String billing_cycle;
    /**
     * 所属供电所
     */
    public String supply_station;
    /**
     * 用电类别 商业用电 民营用电 物业用电
     */
    public String power_category;
    /**
     * 电费
     */
    public double electricity_fee;
    /**
     * 服务费
     */
    public double service_fee ;
    /**
     * 用电量
     */
    public double power_consumption;
    /**
     * 电价
     */
    public double electricity_price;
    /**
     * 电费类别
     */
    public String fee_category;
    /**
     * 上次电表数
     */
    public double last_meter_reading;
    /**
     * 本次电表数
     */
    public double current_meter_reading;
    /**
     * 抄见电量
     */
    public double recorded_power;
    /**
     * 换表电量
     */
    public double meter_change_power;
    /**
     * 退补电量
     */
    public double refund_power;
    /**
     * 变损电量
     */
    public double transform_loss_power;
    /**
     * 公摊电量
     */
    public double shared_power;
    /**
     * 免费电量
     */
    public double free_power;
    /**
     * 分表电量
     */
    public double submeter_power;
    /**
     * 合计电量
     */
    public double total_power;

    /**
     * 发票
     */
    public String invoice;
    /**
     * 电费账单,;
     */
    public Double electricity_fee_total;

    /**
     * 平台代码
     */
    public String platform_code;
    /**
     * 组织代码
     */
    public String organize_code;
    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ElectricityPowerSupplyBillEntity getInstance() {
        return new ElectricityPowerSupplyBillEntity();
    }


    public void syncPlatformTaskJob() {
        long monthTimestamp = TimeUtil.getMonthBegin00(-4);
//        syncPlatformMonthTaskJob(2,monthTimestamp);
        List<ElectricityMeterEntity> list = ElectricityMeterEntity.getInstance().selectList();
        for (ElectricityMeterEntity electricityMeterEntity : list) {
            syncStationMonthTaskJob(electricityMeterEntity.id, monthTimestamp);
        }

    }

    /**
     * 统计平台每月总电费
     *
     * @param monthTimestamp
     */
    public void syncPlatformMonthTaskJob(long monthTimestamp) {
        String date = TimeUtil.toTimeString(monthTimestamp, "yyyy-MM");
        BigDecimal electricityFee = ElectricityPowerSupplyBillEntity.getInstance()
                .where("bill_date_time", monthTimestamp)
                .sumGetBigDecimal("electricity_fee");

        String organizeCode = SysGlobalConfigEntity.getString("System:Organize:Code");

        PlatformMonthSummaryV2Entity platformMonthSummaryV2Entity = PlatformMonthSummaryV2Entity.getInstance()
                .where("organize_code", organizeCode)
                .where("date_time", monthTimestamp)
                .findEntity();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("electricity_fee", electricityFee);

        if (platformMonthSummaryV2Entity != null) {
            PlatformMonthSummaryV2Entity.getInstance().where("id", platformMonthSummaryV2Entity.getId()).update(data);
            LogsUtil.info(this.getClass().getName(), String.format("更新台账单成功，更新日期=%s", date));

        } else {
            data.put("date_time", monthTimestamp);
            data.put("date", date);
            data.put("organize_code", organizeCode);
            data.put("create_time", TimeUtil.getTimestamp());
            ChargeStationMonthSummaryV2Entity.getInstance().insertGetId(data);
            LogsUtil.info(this.getClass().getName(), String.format("添加平台账单成功，更新日期=%s", date));
        }


    }

    public void syncStationMonthTaskJob(long meterId, long monthTimestamp) {
        /*
         * 查找当月电费单
         * 查找绑定充电桩
         * 写入电费
         */
//        List<ElectricityPowerSupplyBillEntity> billList = this.where("bill_date", monthTimestamp).selectList();
//        if (billList.isEmpty()) {
//            return;
//        }
        String date = TimeUtil.toTimeString(monthTimestamp, "yyyy-MM");

        ElectricityPowerSupplyBillEntity electricityPowerSupplyBillEntity = ElectricityPowerSupplyBillEntity.getInstance()
                .where("meter_id", meterId)
                .where("bill_date_time", monthTimestamp)
                .findEntity();

        if (electricityPowerSupplyBillEntity == null) {
            LogsUtil.info(this.getClass().getName(), "更新电费账单失败,账单不存在，电表id=" + meterId);
            return;
        }

        int totalSocket = EMeterToCStationEntity.getInstance().alias("etc").join(ChargeStationEntity.getInstance().theTableName(), "cs", "cs.CSId=etc.cs_id").where("meter_id", meterId).sum("cs.totalSocket");
        String[] csIds = EMeterToCStationEntity.getInstance().where("meter_id", meterId).selectForStringArray("cs_id");

        if (csIds.length == 0) {
            LogsUtil.info(this.getClass().getName(), String.format("更新电费账单失败，该电表没有绑定站点，电表id=%s,更新日期=%s", meterId, date));
            return;
        }

        List<ChargeStationEntity> csList = ChargeStationEntity.getInstance().whereIn("CSId", csIds).selectList();
        //
        if (csList.isEmpty()) {
            LogsUtil.info(this.getClass().getName(), String.format("更新电费账单失败，该电表没有绑定站点，电表id=%s,更新日期=%s", meterId, date));
            return;
        }
        for (ChargeStationEntity cs : csList) {
            BigDecimal numerator = BigDecimal.valueOf(cs.totalSocket);
            BigDecimal denominator = BigDecimal.valueOf(totalSocket);
            BigDecimal rate = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

            ChargeStationMonthSummaryV2Entity chargeStationMonthSummaryV2Entity =
                    ChargeStationMonthSummaryV2Entity.getInstance()
                            .where("CSId", cs.CSId)
                            .where("date_time", monthTimestamp)
                            .findEntity();

            //当月充电桩信息
            BigDecimal fee = rate.multiply(BigDecimal.valueOf(electricityPowerSupplyBillEntity.electricity_fee));
            if (rate.compareTo(BigDecimal.ZERO) == 0) {
                LogsUtil.info(this.getClass().getName(), String.format("更新电费账单失败，电费费用为0，站点id=%s,电表id=%s,更新日期=%s", cs.CSId, meterId, date));
                continue;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("electricity_fee", fee);

            if (chargeStationMonthSummaryV2Entity != null) {
                chargeStationMonthSummaryV2Entity.where("id", chargeStationMonthSummaryV2Entity.getId()).update(data);
                LogsUtil.info(this.getClass().getName(), String.format("更新电费账单成功，站点id=%s,电表id=%s,更新日期=%s", cs.CSId, meterId, date));
            } else {
                data.put("CSId", cs.CSId);
                data.put("date_time", monthTimestamp);
                data.put("date", date);
                data.put("organize_code", cs.organize_code);
                data.put("create_time", TimeUtil.getTimestamp());
                ChargeStationMonthSummaryV2Entity.getInstance().insertGetId(data);
                LogsUtil.info(this.getClass().getName(), String.format("添加电费账单成功，站点id=%s,电表id=%s,更新日期=%s", cs.CSId, meterId, date));
            }
        }

    }

    public void syncAllPlatformMonthTaskJob() {


    }

    /**
     * 通过站点信息查询电费单
     *
     * @param CSId           充电桩ID
     * @param bill_date_time 账单日期时间戳(毫秒)
     */
    public ElectricityPowerSupplyBillEntity getWithCSId(String CSId, long bill_date_time) {
        return this.alias("epsb")
                .join(EMeterToCStationEntity.getInstance().theTableName(), "emc", "epsb.meter_id = emc.meter_id")
                .where("emc.cs_id", CSId)
                .where("epsb.bill_date_time", bill_date_time)
                .findEntity();
    }
}