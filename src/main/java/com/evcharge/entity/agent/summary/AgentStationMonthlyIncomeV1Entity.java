package com.evcharge.entity.agent.summary;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.agent.agent.AgentToStationEntity;
import com.evcharge.entity.agent.config.AgentSplitModeEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.rocketmq.consumer.v2.AgentXRocketMQConsumerV2;
import com.xyzs.annotation.TargetDB;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 代理站点月度收益表;
 *
 * @author : Jay
 * @date : 2025-2-17
 */
@TargetDB("evcharge_agent")
public class AgentStationMonthlyIncomeV1Entity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 站点id
     */
    public String CSId;
    /**
     * 月份，格式：yyyy-MM
     */
    public String month;
    /**
     * 月份时间戳
     */
    public long month_time;
    /**
     * 站点总消费
     */
    public BigDecimal total_consume;
    /**
     * 站点余额充电消费
     */
    public BigDecimal charge_consume;
    /**
     * 站点充值卡充电消费
     */
    public BigDecimal card_charge_consume;
    /**
     * 分账金额
     */
    public BigDecimal split_amount;
    /**
     * 当天使用的分账比例
     */
    public BigDecimal split_rate;
    /**
     * 服务包费用
     */
    public BigDecimal service_fee;
    /**
     * 分润
     */
    public BigDecimal sharing_fee;
    /**
     * 净收益(分账金额-服务费-分润)
     */
    public BigDecimal net_income;
    /**
     * 结算状态
     */
    public int settlement_status;
    /**
     * 备注
     */
    public String remark;
    /**
     * 状态
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
    public static AgentStationMonthlyIncomeV1Entity getInstance() {
        return new AgentStationMonthlyIncomeV1Entity();
    }

    private final static String TAG = "元气充代理-充电桩数据-月汇总v1";

    /**
     * 同步数据（迭代调用）
     */
    public void syncData(String organizeCode, String CSId, long date_timestamp, long end_time) {
        long startTime = TimeUtil.toTimestamp00(date_timestamp);
        // 使用循环代替递归
        while (startTime <= end_time) {
            syncTaskJob(organizeCode, CSId, startTime);
            startTime += ECacheTime.DAY;
        }
    }

    /**
     * 开始同步任务（支持普通线程与RocketMQ两种方式）
     *
     * @param starTime    任务开始时间戳
     * @param endTime     任务结束时间戳
     * @param useRocketMQ 是否使用RocketMQ批量启动任务
     */
    public void startSyncTask(long starTime, long endTime, boolean useRocketMQ) {
        int page = 1;
        int limit = 100;

        // 标准化时间戳
        starTime = TimeUtil.toMonthBegin00(starTime);
        endTime = TimeUtil.toMonthEnd24(endTime);

        while (true) {
            // 分页查询充电站列表
//            List<Map<String, Object>> list = ChargeStationEntity.getInstance()
//                    .field("id,CSId,name,online_time")
//                    .where("status", 1)
//                    .where("online_time", ">", 0)
//                    .where("isTest", 0) // 根据需求加入条件
//                    .page(page, limit)
//                    .select();
//
            List<AgentToStationEntity> list = AgentToStationEntity.getInstance()
                    .field("id,organize_code,CSId,status")
                    .where("status", 0)
                    .page(page, limit)
                    .selectList();

            if (list == null || list.isEmpty()) break;

            page++;

            for (AgentToStationEntity nd : list) {
//                String CSId = MapUtil.getString(nd, "CSId");
//                String name = StringUtil.removeLineBreaksAndSpaces(MapUtil.getString(nd, "name"));
//                long online_time = MapUtil.getLong(nd, "online_time");
//                long taskStartTime = Math.max(online_time, starTime);
                if (useRocketMQ) {
                    // RocketMQ批量任务
                    JSONObject rocketMQData = new JSONObject();
                    rocketMQData.put("CSId", nd.CSId);
                    rocketMQData.put("organize_code", nd.organize_code);
                    rocketMQData.put("start_time", starTime);
                    rocketMQData.put("end_time", endTime);
                    XRocketMQ.getGlobal().pushOneway(AgentXRocketMQConsumerV2.TOPIC, "MonthSummaryTaskV1", rocketMQData);
                } else {
                    // 普通线程任务
                    long finalEndTime = endTime;
                    long finalStarTime = starTime;
                    ThreadUtil.getInstance().execute(
                            String.format("[%s-%s] %s", nd.organize_code, nd.CSId, TAG),
                            () -> syncData(nd.organize_code, nd.CSId, finalStarTime, finalEndTime)
                    );
                }
            }
        }
    }

    /**
     * 站点同步数据
     * @param organizeCode String
     * @param CSId  String
     * @param dateTimestamp long
     * @return
     */
    public SyncResult syncTaskJob(String organizeCode, String CSId, long dateTimestamp) {
        if (!StringUtil.hasLength(CSId)) return new SyncResult(2, "缺少充电站ID");

        AgentSplitModeEntity agentSplitModeEntity = AgentSplitModeEntity.getInstance().getSplitModeByOrganizeCode(organizeCode);

        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().where("CSId", CSId).findEntity();

        if (chargeStationEntity == null || chargeStationEntity.id == 0) return new SyncResult(1, "站点不存在");

        String date = TimeUtil.toTimeString(dateTimestamp, "yyyy-MM");
        LogsUtil.info(TAG, "[%s-%s] 正在统计 %s 数据...", CSId, StringUtil.removeLineBreaksAndSpaces(chargeStationEntity.name), date);

        try {
            //当天凌晨时间戳
            final long startTime = TimeUtil.toMonthBegin00(dateTimestamp);
            //当天结束时间戳
            final long endTime = TimeUtil.toMonthEnd24(dateTimestamp);

            //totalAmount 余额充电的消费金额
            //chargeCardConsumeAmount 充电卡消费金额

            Map<String, Object> chargeConsumeData = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(totalAmount),0) AS pay_per_adjustment_charge_amount")
                    .alias("co")
                    .where("co.CSId", CSId)
                    .where("co.status", 2)
                    .where("co.paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("co.isTest", 0)
                    .where("co.create_time", ">=", startTime)
                    .where("co.create_time", "<=", endTime)
                    .find();

            BigDecimal chargeConsume = MapUtil.getBigDecimal(chargeConsumeData
                    , "pay_per_adjustment_charge_amount"
                    , 4
                    , RoundingMode.HALF_UP
                    , new BigDecimal(0));

            Map<String, Object> cardChargeData = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(chargeCardConsumeAmount),0) AS card_charge_amount")
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();

            BigDecimal cardChargeConsume = MapUtil.getBigDecimal(cardChargeData, "card_charge_amount", 4, RoundingMode.HALF_UP);
            BigDecimal totalConsume = chargeConsume.add(cardChargeConsume);
            BigDecimal splitAmount = totalConsume.multiply(agentSplitModeEntity.split_rate).divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);

            Map<String, Object> data = new LinkedHashMap<>();

            data.put("organize_code", organizeCode);
            data.put("month", date);
            data.put("month_time", startTime);

            data.put("CSId", CSId);
            data.put("charge_consume", chargeConsume);
            data.put("card_charge_consume", cardChargeConsume);
            data.put("total_consume", totalConsume);
            data.put("split_amount", splitAmount);
            //计算分账金额
            data.put("update_time", TimeUtil.getTimestamp());
            if (getInstance().where("CSId", CSId).where("month_time", startTime).exist()) {
                getInstance().where("CSId", CSId).where("month_time", startTime).update(data);
            } else {
                data.put("create_time", TimeUtil.getTimestamp());
                getInstance().insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "汇总数据发生错误");
        }

        LogsUtil.info(TAG, "[%s-%s] 统计数据 %s 完成！", CSId, StringUtil.removeLineBreaksAndSpaces(chargeStationEntity.name), date);
        return new SyncResult(0, "");
    }
}