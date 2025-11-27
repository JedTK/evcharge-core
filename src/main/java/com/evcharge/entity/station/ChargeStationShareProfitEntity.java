package com.evcharge.entity.station;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.admin.AdminWalletEntity;
import com.evcharge.entity.chargecard.UserChargeCardOrderEntity;
import com.evcharge.enumdata.EBalanceType;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.ThreadUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 充电桩分润配置
 *
 * @author : JED
 * @date : 2022-12-21
 */
public class ChargeStationShareProfitEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电站ID
     */
    public long CSId;
    /**
     * 管理员ID
     */
    public long adminId;
    /**
     * 组织id，可能为0
     */
    public long organize_id;
    /**
     * 状态：0=删除，1=正常
     */
    public int status;
    /**
     * 分润方式：1=月结电位分润、2=手动电量分润、3=实时电量分润、4=实时消费金额(计次充电、内购卡金额)百分比分润
     * 1、月结电位分润（固定数结算：插座单价*插座数）
     * 2、手动电量分润（人工不定期抄表后对账结算：电量单价*抄表电表电量）
     * 3、实时电量分润（电量单价固定分润：电量单价*系统充电电量）
     * 4、实时消费金额(计次充电、内购卡金额)百分比分润（按每笔消费分润：消费金额*百分比）（买断电桩）
     */
    public int typeId;
    /**
     * 收益单价
     */
    public double incomePrice;
    /**
     * 收益比率，按百分比收益时用
     */
    public double incomeRatio;
    /**
     * 开始生效时间
     */
    public long startTime;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStationShareProfitEntity getInstance() {
        return new ChargeStationShareProfitEntity();
    }

    /**
     * 根据充电订单进行分润（多线程）
     *
     * @param chargeOrderEntity 充电订单
     * @return
     */
    public void threadShareProfitCheckWithChargeOrder(ChargeOrderEntity chargeOrderEntity) {
        ThreadUtil.getInstance().execute("根据充电订单进行分润", () -> {
            ThreadUtil.sleep(3000);
            List<ChargeStationShareProfitEntity> list = ChargeStationShareProfitEntity.getInstance()
                    .where("CSId", chargeOrderEntity.CSId)
                    .where("status", 1)
                    .whereIn("typeId", "3,4")
                    .selectList();
            for (ChargeStationShareProfitEntity csspEntity : list) {
                SyncResult r = new SyncResult(1, "");
                if (csspEntity.typeId == 3) {
                    r = csspEntity.incomeWithChargeOrderPowerConsumption(chargeOrderEntity);
                } else if (csspEntity.typeId == 4) {
                    r = csspEntity.incomeWithChargeOrder(chargeOrderEntity);
                }

                if (r.code != 0 && r.code != 1) {
                    LogsUtil.info("", "[根据充电订单进行分润（多线程）] %s", r.msg);
                }
            }
        });
    }

    /**
     * 根据充电卡订单进行分润（多线程）
     *
     * @param chargeCardOrderSN 充电卡订单号
     */
    public void threadShareProfitCheckWithChargeCardOrderSN(String chargeCardOrderSN) {
        ThreadUtil.getInstance().execute("根据充电订单进行分润", () -> {
            UserChargeCardOrderEntity userChargeCardOrderEntity = UserChargeCardOrderEntity.getInstance()
                    .where("OrderSN", chargeCardOrderSN)
                    .findModel();
            if (userChargeCardOrderEntity == null || userChargeCardOrderEntity.id == 0) return;

            List<ChargeStationShareProfitEntity> list = ChargeStationShareProfitEntity.getInstance()
                    .where("CSId", userChargeCardOrderEntity.CSId)
                    .where("status", 1)
                    .where("typeId", 4)
                    .selectList();
            Iterator it = list.iterator();
            while (it.hasNext()) {
                ChargeStationShareProfitEntity csspEntity = (ChargeStationShareProfitEntity) it.next();
                if (csspEntity.typeId != 4) continue;
                SyncResult r = csspEntity.incomeWithChargeCardOrder(userChargeCardOrderEntity);
                if (r.code != 0 && r.code != 1) {
                    LogsUtil.info("", "[根据充电卡订单进行分润（多线程）] %s", r.msg);
                }
            }
        });
    }

    //region 分润详情

    /**
     * typeId=1 月结电位分润（固定数结算：插座单价*插座数）
     * <p>
     * 1、月结电位分润（固定数结算：插座单价*插座数）
     * 2、手动电量分润（人工不定期抄表后对账结算：电量单价*抄表电表电量）
     * 3、实时电量分润（电量单价固定分润：电量单价*系统充电电量）
     * 4、实时消费金额(计次充电、内购卡金额)百分比分润（按每笔消费分润：消费金额*百分比）（买断电桩）
     *
     * @param month_timestamp 月时间戳
     * @return
     */
    public SyncResult incomeWithMonthlySocket(long month_timestamp) {
        String month = TimeUtil.toTimeString(month_timestamp, "yyyy-MM");
        if (this.status != 1) return new SyncResult(19, "分润已停止");
        if (this.startTime > month_timestamp) return new SyncResult(20, "分润还没生效");

        ChargeStationMonthlySummaryEntity summaryEntity = ChargeStationMonthlySummaryEntity.getInstance()
                .where("CSId", this.CSId)
                .where("date", month)
                .findModel();
        if (summaryEntity == null || summaryEntity.id == 0) return new SyncResult(11, "充电桩月汇总数据不存在");
        if (this.typeId != 1) return new SyncResult(12, "错误的分润方式函数执行");

        /**
         * 分润方式：
         * 1、月结电位分润（固定数结算：插座单价*插座数）
         * 2、手动电量分润（人工不定期抄表后对账结算：电量单价*抄表电表电量）
         * 3、实时电量分润（电量单价固定分润：电量单价*系统充电电量）
         * 4、实时消费金额(计次充电、内购卡金额)百分比分润（按每笔消费分润：消费金额*百分比）（买断电桩）
         */
        //收益金额 = 充电桩总插座数 * 收益单价（每个人的收益单价都不同的，这个需要配置人自己调整）
        BigDecimal incomeAmount = new BigDecimal(summaryEntity.totalSocket)
                .multiply(new BigDecimal(this.incomePrice))
                .setScale(2, RoundingMode.HALF_UP);
        if (incomeAmount.compareTo(BigDecimal.ZERO) == 0) return new SyncResult(0, "无收益");

        ShareProfitIncomeLogEntity logEntity = new ShareProfitIncomeLogEntity();
        logEntity.CSId = this.CSId;
        logEntity.startTime = TimeUtil.toMonthBegin00(month_timestamp);
        logEntity.endTime = TimeUtil.toMonthEnd24(month_timestamp);
        logEntity.adminId = this.adminId;
        logEntity.organize_id = organize_id;
        logEntity.incomeAmount = incomeAmount;
        logEntity.CSSPId = this.id;

        return logEntity.beginTransaction(connection -> {
            //检查是否存在收益
            Map<String, Object> logData = logEntity.field("id")
                    .where("CSId", logEntity.CSId)
                    .where("adminId", logEntity.adminId)
                    .where("organize_id", logEntity.organize_id)
                    .where("CSSPId", logEntity.CSSPId)
                    .where("startTime", logEntity.startTime)
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
            if (logData.size() > 0) return new SyncResult(22, "收益已存在");

            //添加收益日志
            SyncResult r = logEntity.addTransaction(connection);
            if (r.code != 0) return r;

            //插入钱包记录
            r = AdminWalletEntity.getInstance().addTransaction(connection
                    , logEntity.adminId
                    , logEntity.organize_id
                    , "充电桩分润收益"
                    , EBalanceType.INCOME
                    , incomeAmount
                    , String.format("%s 月结电位分润", month));

            LogsUtil.customer("分润系统", "系统自动化 月结电位分润 月份：%s 收益日志：%s", month, JSONObject.toJSONString(logData));
            return r;
        });
    }

    /**
     * typeId=2 手动电量分润（人工不定期抄表后对账结算：电量单价*抄表电表电量）
     * <p>
     * 1、月结电位分润（固定数结算：插座单价*插座数）
     * 2、手动电量分润（人工不定期抄表后对账结算：电量单价*抄表电表电量）
     * 3、实时电量分润（电量单价固定分润：电量单价*系统充电电量）
     * 4、实时消费金额(计次充电、内购卡金额)百分比分润（按每笔消费分润：消费金额*百分比）（买断电桩）
     *
     * @param electricityBillEntity 电费账单
     * @return
     */
    public SyncResult incomeWithMeterReading(ElectricityBillEntity electricityBillEntity) {
        if (this.typeId != 2) return new SyncResult(12, "错误的分润方式函数执行");
        //状态：1=未支付，2=已支付，3=异常
        if (electricityBillEntity.status != 2) return new SyncResult(13, "电费单还没缴费");
        if (this.status != 1) return new SyncResult(19, "分润已停止");
        if (this.startTime > electricityBillEntity.startTime) return new SyncResult(20, "分润还没生效");

        /**
         * 分润方式：
         * 1、月结电位分润（固定数结算：插座单价*插座数）
         * 2、手动电量分润（人工不定期抄表后对账结算：电量单价*抄表电表电量）
         * 3、实时电量分润（电量单价固定分润：电量单价*系统充电电量）
         * 4、实时消费金额(计次充电、内购卡金额)百分比分润（按每笔消费分润：消费金额*百分比）（买断电桩）
         */
        //收益金额 = 电量单价 * 抄表电表电量
        BigDecimal incomeAmount = electricityBillEntity.settle_kwh.multiply(new BigDecimal(this.incomePrice)).setScale(2, RoundingMode.HALF_UP);
        if (incomeAmount.compareTo(BigDecimal.ZERO) == 0) return new SyncResult(0, "无收益");

        ShareProfitIncomeLogEntity logEntity = new ShareProfitIncomeLogEntity();
        logEntity.CSId = this.CSId;
        logEntity.extraData = electricityBillEntity.meterNo;
        logEntity.startTime = electricityBillEntity.startTime;
        logEntity.endTime = electricityBillEntity.endTime;
        logEntity.adminId = this.adminId;
        logEntity.organize_id = this.organize_id;
        logEntity.incomeAmount = incomeAmount;
        logEntity.CSSPId = this.id;

        return logEntity.beginTransaction(connection -> {
            //检查是否存在收益
            Map<String, Object> logData = logEntity.field("id")
                    .where("CSId", logEntity.CSId)
                    .where("adminId", logEntity.adminId)
                    .where("organize_id", logEntity.organize_id)
                    .where("CSSPId", logEntity.CSSPId)
                    .where("extraData", logEntity.extraData)
                    .where("startTime", logEntity.startTime)
                    .where("endTime", logEntity.endTime)
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
            if (logData.size() > 0) return new SyncResult(22, "收益已存在");

            //添加收益日志
            SyncResult r = logEntity.addTransaction(connection);
            if (r.code != 0) return r;

            //插入钱包记录
            r = AdminWalletEntity.getInstance().addTransaction(connection
                    , logEntity.adminId
                    , logEntity.organize_id
                    , "充电桩分润收益"
                    , EBalanceType.INCOME
                    , incomeAmount
                    , "手动电量分润");

            LogsUtil.customer("分润系统", "人工抄表电量分润 电表编号：%s 收益日志：%s", electricityBillEntity.meterNo, JSONObject.toJSONString(logData));
            return r;
        });
    }

    /**
     * typeId=3 实时电量分润（电量单价固定分润：电量单价*系统充电电量）
     * TODO 手动电量分润 未完成，未测试
     * <p>
     * 1、月结电位分润（固定数结算：插座单价*插座数）
     * 2、手动电量分润（人工不定期抄表后对账结算：电量单价*抄表电表电量）
     * 3、实时电量分润（电量单价固定分润：电量单价*系统充电电量）
     * 4、实时消费金额(计次充电、内购卡金额)百分比分润（按每笔消费分润：消费金额*百分比）（买断电桩）
     *
     * @param chargeOrderEntity 充电订单
     * @return
     */
    public SyncResult incomeWithChargeOrderPowerConsumption(ChargeOrderEntity chargeOrderEntity) {
        if (this.typeId != 3) return new SyncResult(12, "错误的分润方式函数执行");
        if (chargeOrderEntity.status != 2) return new SyncResult(13, "请先结算充电订单再进行分润计算");
        if (this.status != 1) return new SyncResult(19, "分润已停止");
        if (this.startTime > chargeOrderEntity.create_time) return new SyncResult(20, "分润还没生效");

        //收益金额 = 电量单价*系统充电电量
        BigDecimal incomeAmount = new BigDecimal(chargeOrderEntity.powerConsumption)
                .multiply(new BigDecimal(this.incomePrice))
                .setScale(2, RoundingMode.HALF_UP);
        if (incomeAmount.compareTo(BigDecimal.ZERO) == 0) return new SyncResult(0, "无收益");

        ShareProfitIncomeLogEntity logEntity = new ShareProfitIncomeLogEntity();
        logEntity.orderSN = chargeOrderEntity.OrderSN;
        logEntity.extraData = "";
        logEntity.CSId = this.CSId;
        logEntity.startTime = chargeOrderEntity.startTime;
        logEntity.endTime = chargeOrderEntity.stopTime;
        logEntity.adminId = this.adminId;
        logEntity.organize_id = this.organize_id;
        logEntity.incomeAmount = incomeAmount;
        logEntity.CSSPId = this.id;

        return logEntity.beginTransaction(connection -> {
            //检查是否存在收益
            Map<String, Object> logData = logEntity.field("id")
                    .where("CSId", logEntity.CSId)
                    .where("adminId", logEntity.adminId)
                    .where("organize_id", logEntity.organize_id)
                    .where("CSSPId", logEntity.CSSPId)
                    .where("orderSN", chargeOrderEntity.OrderSN)
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
            if (logData.size() > 0) return new SyncResult(22, "收益已存在");

            //添加收益日志
            SyncResult r = logEntity.addTransaction(connection);
            if (r.code != 0) return r;

            //插入钱包记录
            r = AdminWalletEntity.getInstance().addTransaction(connection
                    , logEntity.adminId
                    , logEntity.organize_id
                    , "充电桩分润收益"
                    , EBalanceType.INCOME
                    , incomeAmount
                    , "实时电量分润"
                    , ChargeOrderEntity.class
                    , chargeOrderEntity.OrderSN
                    , logEntity.extraData
            );

            LogsUtil.customer("分润系统", "实时电量分润 订单号：%s 收益日志：%s", logEntity.orderSN, JSONObject.toJSONString(logData));
            return r;
        });
    }

    /**
     * typeId=4 实时消费金额(计次充电、内购卡金额)百分比分润（按每笔消费分润：消费金额*百分比）（买断电桩）
     * TODO 手动电量分润 未完成，未测试
     * <p>
     * 1、月结电位分润（固定数结算：插座单价*插座数）
     * 2、手动电量分润（人工不定期抄表后对账结算：电量单价*抄表电表电量）
     * 3、实时电量分润（电量单价固定分润：电量单价*系统充电电量）
     * 4、实时消费金额(计次充电、充电卡金额)百分比分润（按每笔消费分润：消费金额*百分比）（买断电桩）
     *
     * @param chargeOrderEntity 充电订单
     * @return
     */
    public SyncResult incomeWithChargeOrder(ChargeOrderEntity chargeOrderEntity) {
        if (this.typeId != 4) return new SyncResult(12, "错误的分润方式函数执行");
        if (chargeOrderEntity.status != 2) return new SyncResult(13, "请先结算充电订单再进行分润计算");
        //只有计次的充电订单才进行分润
        if (chargeOrderEntity.paymentTypeId != 1) return new SyncResult(0, "");
        if (this.status != 1) return new SyncResult(19, "分润已停止");
        if (this.startTime > chargeOrderEntity.create_time) return new SyncResult(20, "分润还没生效");

        //收益金额 = 消费金额*百分比
        BigDecimal incomeAmount = new BigDecimal(chargeOrderEntity.totalAmount)
                .multiply(new BigDecimal(this.incomeRatio))
                .setScale(2, RoundingMode.HALF_UP);
        if (incomeAmount.compareTo(BigDecimal.ZERO) == 0) return new SyncResult(0, "无收益");

        ShareProfitIncomeLogEntity logEntity = new ShareProfitIncomeLogEntity();
        logEntity.orderSN = chargeOrderEntity.OrderSN;
        logEntity.extraData = "";
        logEntity.CSId = this.CSId;
        logEntity.startTime = chargeOrderEntity.startTime;
        logEntity.endTime = chargeOrderEntity.stopTime;
        logEntity.adminId = this.adminId;
        logEntity.organize_id = this.organize_id;
        logEntity.incomeAmount = incomeAmount;
        logEntity.CSSPId = this.id;

        return logEntity.beginTransaction(connection -> {
            //检查是否存在收益
            Map<String, Object> logData = logEntity.field("id")
                    .where("CSId", logEntity.CSId)
                    .where("adminId", logEntity.adminId)
                    .where("organize_id", logEntity.organize_id)
                    .where("CSSPId", logEntity.CSSPId)
                    .where("orderSN", chargeOrderEntity.OrderSN)
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
            if (logData.size() > 0) return new SyncResult(22, "收益已存在");

            //添加收益日志
            SyncResult r = logEntity.addTransaction(connection);
            if (r.code != 0) return r;

            //插入钱包记录
            r = AdminWalletEntity.getInstance().addTransaction(connection
                    , logEntity.adminId
                    , logEntity.organize_id
                    , "充电桩分润收益"
                    , EBalanceType.INCOME
                    , incomeAmount
                    , "实时消费金额百分比分润"
                    , ChargeOrderEntity.class
                    , chargeOrderEntity.OrderSN
                    , logEntity.extraData
            );

            LogsUtil.customer("分润系统", "实时消费金额百分比分润 充电订单号：%s 收益日志：%s", logEntity.orderSN, JSONObject.toJSONString(logData));
            return r;
        });
    }

    /**
     * typeId=4 实时消费金额(计次充电、内购卡金额)百分比分润（按每笔消费分润：消费金额*百分比）（买断电桩）
     * TODO 手动电量分润 未完成，未测试
     * <p>
     * 1、月结电位分润（固定数结算：插座单价*插座数）
     * 2、手动电量分润（人工不定期抄表后对账结算：电量单价*抄表电表电量）
     * 3、实时电量分润（电量单价固定分润：电量单价*系统充电电量）
     * 4、实时消费金额(计次充电、充电卡金额)百分比分润（按每笔消费分润：消费金额*百分比）（买断电桩）
     *
     * @param userChargeCardOrderEntity 充电卡订单
     * @return
     */
    public SyncResult incomeWithChargeCardOrder(UserChargeCardOrderEntity userChargeCardOrderEntity) {
        if (this.typeId != 4) return new SyncResult(12, "错误的分润方式函数执行");
        if (userChargeCardOrderEntity.status != 1) return new SyncResult(13, "请先结算充电订单再进行分润计算");
        if (this.status != 1) return new SyncResult(19, "分润已停止");
        if (this.startTime > userChargeCardOrderEntity.create_time) return new SyncResult(20, "分润还没生效");

        //收益金额 = 消费金额*百分比
        BigDecimal incomeAmount = new BigDecimal(userChargeCardOrderEntity.totalAmount)
                .multiply(new BigDecimal(this.incomeRatio))
                .setScale(2, RoundingMode.HALF_UP);
        if (incomeAmount.compareTo(BigDecimal.ZERO) == 0) return new SyncResult(0, "无收益");

        ShareProfitIncomeLogEntity logEntity = new ShareProfitIncomeLogEntity();
        logEntity.orderSN = userChargeCardOrderEntity.OrderSN;
        logEntity.extraData = "";
        logEntity.CSId = this.CSId;
        logEntity.startTime = userChargeCardOrderEntity.create_time;
        logEntity.endTime = userChargeCardOrderEntity.create_time;
        logEntity.adminId = this.adminId;
        logEntity.organize_id = this.organize_id;
        logEntity.incomeAmount = incomeAmount;
        logEntity.CSSPId = this.id;

        return logEntity.beginTransaction(connection -> {
            //检查是否存在收益
            Map<String, Object> logData = logEntity.field("id")
                    .where("CSId", logEntity.CSId)
                    .where("adminId", logEntity.adminId)
                    .where("organize_id", logEntity.organize_id)
                    .where("CSSPId", logEntity.CSSPId)
                    .where("orderSN", userChargeCardOrderEntity.OrderSN)
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
            if (logData.size() > 0) return new SyncResult(22, "收益已存在");

            //添加收益日志
            SyncResult r = logEntity.addTransaction(connection);
            if (r.code != 0) return r;

            //插入钱包记录
            r = AdminWalletEntity.getInstance().addTransaction(connection
                    , logEntity.adminId
                    , logEntity.organize_id
                    , "充电桩分润收益"
                    , EBalanceType.INCOME
                    , incomeAmount
                    , "实时消费金额百分比分润"
                    , UserChargeCardOrderEntity.class
                    , userChargeCardOrderEntity.OrderSN
                    , logEntity.extraData
            );

            LogsUtil.customer("[分润系统] 实时消费金额百分比分润 充电卡订单号：%s 收益日志：%s", logEntity.orderSN, JSONObject.toJSONString(logData));
            return r;
        });
    }
    //endregion
}
