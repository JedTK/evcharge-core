package com.evcharge.entity.agent.summary;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.agent.agent.AgentToOrganizeEntity;
import com.evcharge.entity.agent.config.AgentSplitModeEntity;
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
 * 代理每日收益表;
 *
 * @author : Jay
 * @date : 2025-2-17
 */
@TargetDB("evcharge_agent")
public class AgentDailyIncomeV1Entity extends BaseEntity implements Serializable {
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
     * 提现订单编号
     */
    public String withdraw_order_sn;
    /**
     * 日期，格式：yyyy-MM-dd
     */
    public String date;
    /**
     * 日期时间戳
     */
    public long date_time;
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
     * 提现状态 0=未提现 1=已提现
     */
    public int withdraw_status;
    /**
     * 状态 0=未提现 1=冻结中 2=已提现
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
    public static AgentDailyIncomeV1Entity getInstance() {
        return new AgentDailyIncomeV1Entity();
    }
    /**
     * 获取当天可提现金额
     * @param organizeCode
     * @return
     */



    private final static String TAG = "元气充代理-消费数据-日汇总v1";

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
        starTime = TimeUtil.toDayBegin00(starTime);
        endTime = TimeUtil.toDayEnd24(endTime);

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
            List<AgentToOrganizeEntity> list = AgentToOrganizeEntity.getInstance()
                    .field("id,organize_code,status")
                    .where("status", 0)
                    .page(page, limit)
                    .selectList();

            if (list == null || list.isEmpty()) break;

            page++;

            for (AgentToOrganizeEntity nd : list) {
                if (useRocketMQ) {
                    // RocketMQ批量任务
                    JSONObject rocketMQData = new JSONObject();
                    rocketMQData.put("organize_code", nd.organize_code);
                    rocketMQData.put("start_time", starTime);
                    rocketMQData.put("end_time", endTime);
                    XRocketMQ.getGlobal().pushOneway(AgentXRocketMQConsumerV2.TOPIC, "AgentDailySummaryTaskV1", rocketMQData);
                } else {
                    // 普通线程任务
                    long finalEndTime = endTime;
                    long finalStarTime = starTime;
                    String organizeCode = nd.organize_code;
                    ThreadUtil.getInstance().execute(
                            String.format("[%s] %s", organizeCode, TAG),
                            () -> syncData(organizeCode, finalStarTime, finalEndTime)
                    );
                }
            }
        }
    }

    /**
     * 同步数据（迭代调用）
     */
    public void syncData(String organizeCode, long date_timestamp, long end_time) {
        long startTime = TimeUtil.toTimestamp00(date_timestamp);
        // 使用循环代替递归
        while (startTime <= end_time) {
            syncTaskJob(organizeCode, startTime);
            startTime += ECacheTime.DAY;
        }
    }

    /**
     * 同步数据
     *
     * @param organizeCode  String
     * @param dateTimestamp long
     * @return object
     */
    public SyncResult syncTaskJob(String organizeCode, long dateTimestamp) {
        if (!StringUtil.hasLength(organizeCode)) return new SyncResult(2, "缺少组织id");

        AgentSplitModeEntity agentSplitModeEntity = AgentSplitModeEntity.getInstance().getSplitModeByOrganizeCode(organizeCode);

        String date = TimeUtil.toTimeString(dateTimestamp, "yyyy-MM-dd");
        LogsUtil.info(TAG, "[%s] 正在统计 %s 数据...", organizeCode, date);

        /**
         * 统计代理日收入
         * 获取组织code
         * 统计组织code关联的站点-日数据统计
         * 统计余额消费数据 统计充电卡消费数据 合计总消费数据
         *
         */

        try {

            //当天凌晨时间戳
            final long startTime = TimeUtil.toTimestamp00(dateTimestamp);
            //当天结束时间戳
            final long endTime = TimeUtil.toTimestamp24(dateTimestamp);

            Map<String, Object> chargeConsumeData = AgentStationDailyIncomeV1Entity.getInstance()
                    .field("IFNULL(SUM(charge_consume),0) AS charge_consume")
                    .where("organize_code", organizeCode)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .find();

            BigDecimal chargeConsume = MapUtil.getBigDecimal(chargeConsumeData
                    , "charge_consume"
                    , 4
                    , RoundingMode.HALF_UP
                    , new BigDecimal(0));

            Map<String, Object> cardChargeData = AgentStationDailyIncomeV1Entity.getInstance()
                    .field("IFNULL(SUM(card_charge_consume),0) AS card_charge_consume")
                    .where("organize_code", organizeCode)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .find();

            BigDecimal cardChargeConsume = MapUtil.getBigDecimal(cardChargeData, "card_charge_consume", 4, RoundingMode.HALF_UP);

            Map<String, Object> totalConsumeData = AgentStationDailyIncomeV1Entity.getInstance()
                    .field("IFNULL(SUM(total_consume),0) AS total_consume")
                    .where("organize_code", organizeCode)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .find();

            BigDecimal totalConsume = MapUtil.getBigDecimal(totalConsumeData, "total_consume", 4, RoundingMode.HALF_UP);


            Map<String, Object> splitAmountData = AgentStationDailyIncomeV1Entity.getInstance()
                    .field("IFNULL(SUM(split_amount),0) AS split_amount")
                    .where("organize_code", organizeCode)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .find();

            BigDecimal splitAmount = MapUtil.getBigDecimal(splitAmountData, "split_amount", 4, RoundingMode.HALF_UP);

            Map<String, Object> data = new LinkedHashMap<>();

            data.put("organize_code", organizeCode);
            data.put("date", date);
            data.put("date_time", startTime);
            data.put("charge_consume", chargeConsume);
            data.put("card_charge_consume", cardChargeConsume);
            data.put("total_consume", totalConsume);
            data.put("split_amount", splitAmount);
            //计算分账金额
            data.put("update_time", TimeUtil.getTimestamp());
            if (getInstance().where("organize_code", organizeCode).where("date_time", startTime).exist()) {
                getInstance().where("organize_code", organizeCode).where("date_time", startTime).update(data);
            } else {
                data.put("create_time", TimeUtil.getTimestamp());
                getInstance().insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "汇总数据发生错误");
        }
        return new SyncResult(0, "");

    }


}