package com.evcharge.entity.RSProfit;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * 分润系统充电桩汇总;
 *
 * @author : JED
 * @date : 2024-7-15
 */
@Getter
@Setter
@TargetDB("evcharge_rsprofit")
public class RSProfitChargeStationSummaryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    private long id;
    /**
     * 收益人联系电话
     */
    private String channel_phone;
    /**
     * 充电桩ID
     */
    private String cs_id;
    /**
     * 总收益
     */
    private BigDecimal total_income;
    /**
     * 创建时间戳
     */
    private long create_time;
    /**
     * 更新时间戳
     */
    private long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static RSProfitChargeStationSummaryEntity getInstance() {
        return new RSProfitChargeStationSummaryEntity();
    }

    private final static String TAG = "分润系统-充电站汇总";

    /**
     * 同步任务作业
     *
     * @param channel_phone 受益人联系手机
     * @param cs_id         充电桩Id
     * @return
     */
    public SyncResult syncTaskJob(String channel_phone, String cs_id) {
        LogsUtil.info(TAG, "[%s] %s 正在统计数据...", channel_phone, cs_id);

        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("channel_phone", channel_phone);
            data.put("cs_id", cs_id);

            // 总收益
            BigDecimal total_income = RSProfitIncomeLogsEntity.getInstance()
                    .where("channel_phone", channel_phone)
                    .where("cs_id", cs_id)
                    .sumGetBigDecimal("amount");
            data.put("total_income", total_income);

            data.put("update_time", TimeUtil.getTimestamp());
            if (getInstance()
                    .where("channel_phone", channel_phone)
                    .where("cs_id", cs_id)
                    .exist()) {
                getInstance()
                        .where("channel_phone", channel_phone)
                        .where("cs_id", cs_id)
                        .update(data);
            } else {
                data.put("create_time", TimeUtil.getTimestamp());
                getInstance().insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "汇总数据发生错误，date=%s", cs_id);
        }

        LogsUtil.info(TAG, "[%s] %s 统计数据 完成！", channel_phone, cs_id);
        return new SyncResult(0, "");
    }

    /**
     * 开始同步
     */
    public void startSync(String channel_phone) {
        List<Map<String, Object>> list = RSProfitConfigEntity.getInstance()
                .field("cs_id")
                .where("channel_phone", channel_phone)
                .group("cs_id")
                .select();
        for (Map<String, Object> data : list) {
            String cs_id = MapUtil.getString(data, "cs_id");

            if (!StringUtil.hasLength(channel_phone)) continue;
            ThreadUtil.getInstance().execute(String.format("[%s-%s] %s", channel_phone, cs_id, TAG), () -> {
                syncTaskJob(channel_phone, cs_id);
            });
        }
    }

    /**
     * 开始同步
     */
    public void startSync() {
        List<Map<String, Object>> list = RSProfitConfigEntity.getInstance()
                .field("channel_phone,cs_id")
                .group("channel_phone,cs_id")
                .select();
        for (Map<String, Object> data : list) {
            String channel_phone = MapUtil.getString(data, "channel_phone");
            String cs_id = MapUtil.getString(data, "cs_id");

            if (!StringUtil.hasLength(channel_phone)) continue;
            ThreadUtil.getInstance().execute(String.format("[%s-%s] %s", channel_phone, cs_id, TAG), () -> {
                syncTaskJob(channel_phone, cs_id);
            });
        }
    }
}
