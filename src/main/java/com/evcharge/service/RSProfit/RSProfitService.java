package com.evcharge.service.RSProfit;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.CommandMapping;
import com.evcharge.entity.RSProfit.*;
import com.evcharge.entity.chargestatsionproject.WFChargeStationProjectEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.station.*;
import com.evcharge.entity.station.bill.ElectricityPowerSupplyBillEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RSProfitService {
    /**
     * 获得一个实例
     */
    public static RSProfitService getInstance() {
        return new RSProfitService();
    }

    /**
     * 新增 分润配置
     *
     * @param price 单价
     * @param ratio 比例%,内部会自动除以100
     * @return 添加配置的同步结果
     */
    public SyncResult addConfig(RSProfitConfigEntity entity, BigDecimal price, BigDecimal ratio, long start_time, int month) {
        if (!StringUtil.hasLength(entity.cs_id) && !StringUtil.hasLength(entity.project_id)) {
            return new SyncResult(2, "请选择充电桩或者立项项目ID");
        }
        if (!StringUtil.hasLength(entity.channel_phone)) return new SyncResult(2, "请选择输入收益人手机号码");
        if (!StringUtil.hasLength(entity.channel_name)) return new SyncResult(2, "请选择输入收益人姓名");
        if (ratio.compareTo(BigDecimal.ZERO) > 0) ratio = ratio.divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);

        // region CSId 和 project_id 相互兼容：一般情况下以cs_id为准，如果实在找不到cs_id以project_id为准
        // 兼容模式：如果cs_id不存在值，通过project_id查询对应cs_id，实在查询不到就以project_id为准
        if (!StringUtil.hasLength(entity.cs_id) && StringUtil.hasLength(entity.project_id)) {
            entity.cs_id = WFChargeStationProjectEntity.getInstance().getCSIdWithProjectId(entity.project_id);
            if (!StringUtil.hasLength(entity.cs_id)) entity.cs_id = entity.project_id;
        }
        if (StringUtil.hasLength(entity.cs_id) && !StringUtil.hasLength(entity.project_id)) {
            entity.project_id = entity.cs_id;
        }
        // endregion

        long end_time = TimeUtil.toMonthEnd24(start_time + com.evcharge.enumdata.ECacheTime.MONTH * month);

        try {
            // 查询分润模式
            RSProfitModeEntity modeEntity = RSProfitModeEntity.getInstance().getWithModeCode(entity.mode_code);
            if (modeEntity == null) return new SyncResult(3, "无效的分润模型代码");

            // 检查收益单价是否超过个人限制
            BigDecimal limitPrice = modeEntity.getLimitPrice();
            if (limitPrice.compareTo(BigDecimal.ZERO) >= 0 && price.compareTo(limitPrice) > 0) {
                return new SyncResult(4, String.format("每个收益人设置收益单价不能超过%s元", limitPrice));
            }

            // 检查收益比例是否超过个人限制
            BigDecimal limitRatio = modeEntity.getLimitRatio().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            if (limitRatio.compareTo(BigDecimal.ZERO) >= 0 && ratio.compareTo(limitRatio) > 0) {
                BigDecimal limitRatioPercentage = limitRatio.multiply(new BigDecimal(100)).setScale(4, RoundingMode.HALF_UP);
                return new SyncResult(5, String.format("每个收益人设置收益比例不能超过%s%%", limitRatioPercentage));
            }

            // 查询历史分润配置信息
            List<RSProfitConfigEntity> configList = RSProfitConfigEntity.getInstance()
//                    .where("cs_id", entity.cs_id)
                    .where("(", "cs_id", "=", entity.cs_id, "")
                    .whereOr("", "project_id", "=", entity.project_id, ")")
                    .where("mode_code", entity.mode_code)
                    .where("status", ">=", 0)// 状态：-1-停止，0-待确认，1-待启动，2-启动中
                    .selectList();
            if (configList != null && !configList.isEmpty()) {
                BigDecimal totalPrice = BigDecimal.ZERO;
                BigDecimal totalRatio = BigDecimal.ZERO;

                for (RSProfitConfigEntity config : configList) {
                    if (config.price.compareTo(BigDecimal.ZERO) >= 0) {
                        totalPrice = totalPrice.add(config.price);
                    }
                    if (config.ratio.compareTo(BigDecimal.ZERO) >= 0) {
                        totalRatio = totalRatio.add(config.ratio);
                    }
                }

                // 检查收益总单价是否超过限制
                BigDecimal limitTotalPrice = modeEntity.getLimitTotalPrice();
                if (limitTotalPrice.compareTo(BigDecimal.ZERO) >= 0 && totalPrice.add(price).compareTo(limitTotalPrice) > 0) {

                    return new SyncResult(6, String.format("收益总单价不能超过%s元，当前：¥%s"
                            , limitTotalPrice
                            , totalPrice
                    ));
                }

                // 检查收益总比例是否超过限制
                BigDecimal limitTotalRatio = modeEntity.getLimitTotalRatio().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
                if (limitTotalRatio.compareTo(BigDecimal.ZERO) >= 0 && totalRatio.add(ratio).compareTo(limitTotalRatio) > 0) {
                    BigDecimal limitRatioPercentage = limitTotalRatio.multiply(new BigDecimal(100)).setScale(4, RoundingMode.HALF_UP);
                    return new SyncResult(7, String.format("收益总比例不能超过%s%%，当前：%s%%"
                            , limitRatioPercentage
                            , totalRatio.multiply(new BigDecimal(100)).setScale(4, RoundingMode.HALF_UP)));
                }
            }

            // 检查收益人是否存在相同规则
            if (entity
                    .where("(", "cs_id", "=", entity.cs_id, "")
                    .whereOr("", "project_id", "=", entity.project_id, ")")
                    .where("channel_phone", entity.channel_phone)
                    .where("mode_code", entity.mode_code)
                    .where("status", 1)
                    .exist()) {
                return new SyncResult(8, String.format("收益人已存在[%s]的分润规则，可以查找记录进行修改", modeEntity.getTitle()));
            }

            // region 新增配置数据
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("cs_id", entity.cs_id);
            data.put("project_id", entity.project_id);
            data.put("channel_phone", entity.channel_phone); // 收益人联系电话
            data.put("channel_name", entity.channel_name);  // 收益人姓名
            data.put("channel_role", entity.channel_role); // 收益人角色：1-场地方，2-居间人，3-商务
            data.put("mode_code", entity.mode_code);
            data.put("price", price);
            data.put("ratio", ratio);
            data.put("status", 0);// 状态：-1-停止，0-待确认，1-待启动，2-启动中
            data.put("detail", "");
            data.put("remark", entity.remark);
            data.put("start_time", start_time);
            data.put("end_time", end_time);
            data.put("contract_no", entity.contract_no);
            data.put("last_log_message", "");
            data.put("create_time", TimeUtil.getTimestamp());
            data.put("update_time", TimeUtil.getTimestamp());
            // endregion

            int noquery = entity.insert(data);
            if (noquery > 0) return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, entity.getClass().getSimpleName(), "新增分润配置发生错误");
            return new SyncResult(1, "发生异常，无法添加分润配置");
        }

        return new SyncResult(1, "无法添加分润配置");
    }

    /**
     * 更新 分润配置
     *
     * @param price 单价
     * @param ratio 比例%,内部会自动除以100
     * @return 添加配置的同步结果
     */
    public SyncResult updateConfig(RSProfitConfigEntity entity, BigDecimal price, BigDecimal ratio) {
        if (!StringUtil.hasLength(entity.cs_id) && !StringUtil.hasLength(entity.project_id)) {
            return new SyncResult(2, "请选择充电桩或者立项项目ID");
        }
        if (!StringUtil.hasLength(entity.channel_phone)) return new SyncResult(2, "请选择输入收益人手机号码");
        if (!StringUtil.hasLength(entity.channel_name)) return new SyncResult(2, "请选择输入收益人姓名");

        if (ratio.compareTo(BigDecimal.ZERO) > 0) ratio = ratio.divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);

        Map<String, Object> set_data = new LinkedHashMap<>();

        // region CSId 和 project_id 相互兼容：一般情况下以cs_id为准，如果实在找不到cs_id以project_id为准
        // 兼容模式：如果cs_id不存在值，通过project_id查询对应cs_id，实在查询不到就以project_id为准
        if (!StringUtil.hasLength(entity.cs_id) && StringUtil.hasLength(entity.project_id)) {
            entity.cs_id = WFChargeStationProjectEntity.getInstance().getCSIdWithProjectId(entity.project_id);
            if (!StringUtil.hasLength(entity.cs_id)) {
                entity.cs_id = entity.project_id;
            } else set_data.put("cs_id", entity.cs_id);
        }
        if (StringUtil.hasLength(entity.cs_id) && !StringUtil.hasLength(entity.project_id)) {
            entity.project_id = entity.cs_id;
        }
        // endregion

        try {
            // 查询分润模式
            RSProfitModeEntity modeEntity = RSProfitModeEntity.getInstance().getWithModeCode(entity.mode_code);
            if (modeEntity == null) return new SyncResult(3, "无效的分润模型代码");

            // 检查收益单价是否超过个人限制
            BigDecimal limitPrice = modeEntity.getLimitPrice();
            if (limitPrice.compareTo(BigDecimal.ZERO) >= 0 && price.compareTo(limitPrice) > 0) {
                return new SyncResult(4, String.format("每个收益人设置收益单价不能超过%s元", limitPrice));
            }

            // 检查收益比例是否超过个人限制
            BigDecimal limitRatio = modeEntity.getLimitRatio().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            if (limitRatio.compareTo(BigDecimal.ZERO) >= 0 && ratio.compareTo(limitRatio) > 0) {
                BigDecimal limitRatioPercentage = limitRatio.multiply(new BigDecimal(100)).setScale(4, RoundingMode.HALF_UP);
                return new SyncResult(5, String.format("每个收益人设置收益比例不能超过%s%%", limitRatioPercentage));
            }

            // 查询历史分润配置信息
            List<RSProfitConfigEntity> configList = RSProfitConfigEntity.getInstance()
                    .where("(", "cs_id", "=", entity.cs_id, "")
                    .whereOr("", "project_id", "=", entity.project_id, ")")
                    .where("mode_code", entity.mode_code)
                    .where("status", ">=", 0)// 状态：-1-停止，0-待确认，1-待启动，2-启动中
                    .selectList();
            if (configList != null && !configList.isEmpty()) {
                BigDecimal totalPrice = BigDecimal.ZERO;
                BigDecimal totalRatio = BigDecimal.ZERO;

                BigDecimal oldPrice = BigDecimal.ZERO;
                BigDecimal oldRatio = BigDecimal.ZERO;

                for (RSProfitConfigEntity config : configList) {
                    if (entity.channel_phone.equalsIgnoreCase(config.channel_phone)) {
                        oldPrice = config.price;
                        oldRatio = config.ratio;
                    }
                    if (config.price.compareTo(BigDecimal.ZERO) >= 0) {
                        totalPrice = totalPrice.add(config.price);
                    }
                    if (config.ratio.compareTo(BigDecimal.ZERO) >= 0) {
                        totalRatio = totalRatio.add(config.ratio);
                    }
                }

                // 检查收益总单价是否超过限制
                BigDecimal limitTotalPrice = modeEntity.getLimitTotalPrice();
                if (limitTotalPrice.compareTo(BigDecimal.ZERO) >= 0 && totalPrice.subtract(oldPrice).add(price).compareTo(limitTotalPrice) > 0) {
                    return new SyncResult(6, String.format("收益总单价不能超过%s元，当前：¥%s"
                            , limitTotalPrice
                            , totalPrice
                    ));
                }

                // 检查收益总比例是否超过限制
                BigDecimal limitTotalRatio = modeEntity.getLimitTotalRatio().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
                if (limitTotalRatio.compareTo(BigDecimal.ZERO) >= 0 && totalRatio.subtract(oldRatio).add(ratio).compareTo(limitTotalRatio) > 0) {
                    BigDecimal limitRatioPercentage = limitTotalRatio.multiply(new BigDecimal(100)).setScale(4, RoundingMode.HALF_UP);
                    return new SyncResult(7, String.format("收益总比例不能超过%s%%，当前：%s%%"
                            , limitRatioPercentage
                            , totalRatio.multiply(new BigDecimal(100)).setScale(4, RoundingMode.HALF_UP)));
                }
            }

            // 检查收益人是否存在相同规则
            RSProfitConfigEntity configEntity = RSProfitConfigEntity.getInstance()
//                    .where("cs_id", entity.cs_id)
                    .where("(", "cs_id", "=", entity.cs_id, "")
                    .whereOr("", "project_id", "=", entity.project_id, ")")
                    .where("channel_phone", entity.channel_phone)
                    .where("mode_code", entity.mode_code)
                    .findEntity();
            if (configEntity == null) {
                return new SyncResult(10, String.format("收益人不存在[%s]分润规则", modeEntity.getTitle()));
            }
            // 状态：-1-停止，0-待确认，1-待启动，2-启动中
            if (configEntity.status == 2) {
                return new SyncResult(11, String.format("当前收益人[%s]分润已经启动收益中，无法更改规则", modeEntity.getTitle()));
            }


            if (StringUtil.hasLength(entity.channel_name)) set_data.put("channel_name", entity.channel_name);  // 收益人姓名
            if (entity.channel_role != 0)
                set_data.put("channel_role", entity.channel_role); // 收益人角色：1-场地方，2-居间人，3-商务，4-物业
            set_data.put("price", price);
            set_data.put("ratio", ratio);
            set_data.put("status", 0);// 状态：-1-停止，0-待确认，1-待启动，2-启动中
            if (StringUtil.hasLength(entity.detail)) set_data.put("detail", entity.detail);
            if (StringUtil.hasLength(entity.remark)) set_data.put("remark", entity.remark);
            if (entity.start_time != 0) set_data.put("start_time", entity.start_time);
            if (entity.end_time != 0) set_data.put("end_time", entity.end_time);
            if (StringUtil.hasLength(entity.contract_no)) set_data.put("contract_no", entity.contract_no);
            set_data.put("update_time", TimeUtil.getTimestamp());

            int noquery = entity
//                    .where("cs_id", entity.cs_id)
                    .where("(", "cs_id", "=", entity.cs_id, "")
                    .whereOr("", "project_id", "=", entity.project_id, ")")
                    .where("channel_phone", entity.channel_phone)
                    .where("mode_code", entity.mode_code)
                    .update(set_data);
            if (noquery > 0) return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, entity.getClass().getSimpleName(), "新增分润配置发生错误");
            return new SyncResult(1, "发生异常，无法添加分润配置");
        }

        return new SyncResult(1, "无法添加分润配置");
    }

    /**
     * 删除 分润配置
     *
     * @param cs_id         充电桩ID
     * @param project_id    立项ID
     * @param channel_phone 收益人手机号码
     * @param mode_code     分润模式
     * @return 删除结果
     */
    public SyncResult delConfig(String cs_id, String project_id, String channel_phone, String mode_code) {
        if (!StringUtil.hasLength(cs_id) && !StringUtil.hasLength(project_id)) {
            return new SyncResult(2, "请选择充电桩或者立项项目ID");
        }
        if (!StringUtil.hasLength(channel_phone)) return new SyncResult(2, "请选择输入收益人手机号码");
        if (!StringUtil.hasLength(mode_code)) return new SyncResult(2, "请选择选择分润模式");

        // region CSId 和 project_id 相互兼容：一般情况下以cs_id为准，如果实在找不到cs_id以project_id为准
        // 兼容模式：如果cs_id不存在值，通过project_id查询对应cs_id，实在查询不到就以project_id为准
        if (!StringUtil.hasLength(cs_id) && StringUtil.hasLength(project_id)) {
            cs_id = WFChargeStationProjectEntity.getInstance().getCSIdWithProjectId(project_id);
            if (!StringUtil.hasLength(cs_id)) cs_id = project_id;
        }
        if (StringUtil.hasLength(cs_id) && !StringUtil.hasLength(project_id)) {
            project_id = cs_id;
        }
        // endregion

        // 查询配置信息
        RSProfitConfigEntity configEntity = RSProfitConfigEntity.getInstance()
//                .where("cs_id", entity.cs_id)
                .where("(", "cs_id", "=", cs_id, "")
                .whereOr("", "project_id", "=", project_id, ")")
                .where("channel_phone", channel_phone)
                .where("mode_code", mode_code)
                .findEntity();
        if (configEntity == null) return new SyncResult(0, "");
        // 状态：-1-停止，0-待确认，1-待启动，2-启动中
        if (configEntity.status == 2) {
            return new SyncResult(1, "分润已启动，无法删除配置");
        }
        if (configEntity.status == -1) {
            return new SyncResult(1, "分润停止了，为存根数据不建议删除数据");
        }

        int noquery = configEntity.del(configEntity.id);
        if (noquery > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }

    /**
     * 使用 RocketMQ启动任务 （立项通过后需要确认分润配置）
     *
     * @param cs_id      充电桩id（二选一）
     * @param project_id 立项项目id（二选一）
     * @return 同步结果并不是启动结果
     */
    public SyncResult confirm(String cs_id, String project_id) {
        Map<String, Object> set_data = new HashMap<>();

        // region CSId 和 project_id 相互兼容：一般情况下以cs_id为准，如果实在找不到cs_id以project_id为准
        // 兼容模式：如果cs_id不存在值，通过project_id查询对应cs_id，实在查询不到就以project_id为准
        if (!StringUtil.hasLength(cs_id) && StringUtil.hasLength(project_id)) {
            cs_id = WFChargeStationProjectEntity.getInstance().getCSIdWithProjectId(project_id);
            if (!StringUtil.hasLength(cs_id)) cs_id = project_id;
            else set_data.put("cs_id", cs_id);
        }
        if (StringUtil.hasLength(cs_id) && !StringUtil.hasLength(project_id)) {
            project_id = cs_id;
        }
        // endregion

        set_data.put("status", 1);

        int noquery = RSProfitConfigEntity.getInstance()
                .where("(", "cs_id", "=", cs_id, "")
                .whereOr("", "project_id", "=", project_id, ")")
                .where("status", 0)// 状态：-1-停止，0-待确认，1-待启动，2-启动中
                .update(set_data);
        if (noquery > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }

    /**
     * 使用 RocketMQ启动任务 （一般当充电桩正式上线后启动任务）
     *
     * @param cs_id      充电桩id（二选一）
     * @param project_id 立项项目id（二选一）
     * @return 同步结果并不是启动结果
     */
    public SyncResult sendTaskWithMQ(String cs_id, String project_id) {
        // region CSId 和 project_id 相互兼容：一般情况下以cs_id为准，如果实在找不到cs_id以project_id为准
        // 兼容模式：如果cs_id不存在值，通过project_id查询对应cs_id，实在查询不到就以project_id为准
        if (!StringUtil.hasLength(cs_id) && StringUtil.hasLength(project_id)) {
            cs_id = WFChargeStationProjectEntity.getInstance().getCSIdWithProjectId(project_id);
            if (!StringUtil.hasLength(cs_id)) cs_id = project_id;
        }
        if (StringUtil.hasLength(cs_id) && !StringUtil.hasLength(project_id)) {
            project_id = cs_id;
        }
        // endregion

        XRocketMQ xRocketMQ = XRocketMQ.getGlobal();
        if (xRocketMQ == null) return new SyncResult(2, "RocketMQ服务没有启动，请先启动服务");

        // 查询历史分润配置信息
        List<RSProfitConfigEntity> configList = RSProfitConfigEntity.getInstance()
//                .where("cs_id", cs_id)
                .where("(", "cs_id", "=", cs_id, "")
                .whereOr("", "project_id", "=", project_id, ")")
                .where("status", 1)// 状态：-1-停止，0-待确认，1-待启动，2-启动中
                .selectList();
        if (configList == null || configList.isEmpty()) return new SyncResult(2, "没有待启动的分润配置");
        for (RSProfitConfigEntity config : configList) {
            //region 发送通知
            JSONObject rocket_data = new JSONObject();
            rocket_data.put("config_id", config.id);
            xRocketMQ.pushAsync("{ENV}_QuartzTask", "RSProfitTaskAdd", rocket_data, null, 10000);
            //endregion
        }
        return new SyncResult(0, "");
    }

    // region 任务执行

    /**
     * 开始异步任务启动
     *
     * @param cs_id          充电桩id
     * @param date_timestamp 统计时间戳（这里泛指按月统计）
     */
    public void asyncTaskStart(String cs_id, long date_timestamp) {
        ThreadUtil.getInstance().execute("", () -> {
            try {
                long time = date_timestamp;
                List<Map<String, Object>> list = RSProfitConfigEntity.getInstance()
                        .alias("rc")
                        .field("rc.id,rm.priority")
                        .join(RSProfitModeEntity.getInstance().theTableName(), "rm", "rc.mode_code = rm.mode_code")
                        .where("rc.cs_id", cs_id)
                        .where("rc.status", 2)//状态：-1-停止，0-待确认，1-待启动，2-启动中
//                            .where("start_time", ">=", time)
                        .order("rm.priority")
                        .page(1, 1000)
                        .select();
                if (list == null || list.isEmpty()) return;
                do {
                    for (Map<String, Object> data : list) {
                        long config_id = MapUtil.getLong(data, "id");
                        runWithConfig(config_id, time);
                    }

                    time = TimeUtil.getAddMonthTimestamp(time, 1);
                } while (time <= TimeUtil.getMonthBegin());
            } catch (Exception e) {
                LogsUtil.warn(TAG, "同步数据发生错误");
            }
        });
    }

    /**
     * 同步数据
     *
     * @param date_timestamp 统计时间戳（这里泛指按月统计）
     * @return 同步结果
     */
    public SyncResult syncData(String cs_id, long date_timestamp) {
        try {
            List<Map<String, Object>> list = RSProfitConfigEntity.getInstance()
                    .alias("rc")
                    .field("rc.id,rm.priority")
                    .join(RSProfitModeEntity.getInstance().theTableName(), "rm", "rc.mode_code = rm.mode_code")
                    .where("rc.cs_id", cs_id)
                    .where("rc.status", 2)//状态：-1-停止，0-待确认，1-待启动，2-启动中
                    .order("rm.priority")
                    .page(1, 1000)
                    .select();
            for (Map<String, Object> data : list) {
                long config_id = MapUtil.getLong(data, "id");
                runWithConfig(config_id, date_timestamp);
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.warn(TAG, "同步数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 产生分润收益记录 任务作业
     *
     * @param config_id 分润配置ID
     * @return 同步结果
     */
    public SyncResult runWithConfig(long config_id, long date_timestamp) {
        if (config_id == 0) return new SyncResult(2, "缺少分润配置ID");
        RSProfitConfigEntity config = RSProfitConfigService.getInstance().getConfigWithId(config_id);
        if (config == null) {
            LogsUtil.warn(TAG, "[%s] 无效配置", config_id);
            return new SyncResult(2, "无效配置");
        }

        if (date_timestamp == 0) date_timestamp = TimeUtil.getTimestamp();
        String date = TimeUtil.toTimeString(date_timestamp, "yyyy-MM");

        try {
            // 当月凌晨时间戳
            final long startTime = TimeUtil.toMonthBegin00(date_timestamp);
            // 当月结束时间戳
            final long endTime = TimeUtil.toMonthEnd24(date_timestamp);
            // 检查配置收益状态
            if (config.status == 0) return new SyncResult(910, "暂停收益移除任务");

            // region 检查收益生效时间
            if (config.start_time > startTime) {
                String messageHead = String.format("[%s][%s] - ", config.channel_phone, config.cs_id);
                String message = String.format("%s - %s 无法产生收益，还没到达收益时间：%s", date, config.mode_code, TimeUtil.toTimeString(config.start_time));
                LogsUtil.warn(TAG, messageHead + message);
                config.where("id", config.id).update(new LinkedHashMap<>() {{
                    put("last_log_message", message);
                }});
                return new SyncResult(1, message);
            }
            if (config.end_time < endTime) {
                String messageHead = String.format("[%s][%s] - ", config.channel_phone, config.cs_id);
                String message = String.format("%s - %s 暂停收益，已达到收益结束时间：%s", date, config.mode_code, TimeUtil.toTimeString(config.end_time));
                LogsUtil.warn(TAG, messageHead + message);
                config.where("id", config.id).update(new LinkedHashMap<>() {{
                    put("status", 0);// 状态：0-停止，1-启动
                    put("last_log_message", message);
                }});
                return new SyncResult(910, message);
            }
            // endregion

            if (config.status == 1) {
                // 将待启动的任务状态更新为启动中
                RSProfitConfigEntity.getInstance().update(config.id, new LinkedHashMap<>() {{
                    put("status", 2);
                }});
            }

            LogsUtil.info(TAG, "[%s][%s] - %s - %s 计算收益..."
                    , config.channel_phone
                    , config.cs_id
                    , date
                    , config.mode_code);

            SyncResult r = new SyncResult(1, "");
            BigDecimal incomeAmount;
            Map<String, Object> raw_data;

            Method mc = null;
            for (Method method : getClass().getDeclaredMethods()) {
                if (!method.isAnnotationPresent(CommandMapping.class)) continue;
                if (!config.mode_code.equalsIgnoreCase(method.getAnnotation(CommandMapping.class).value()))
                    continue;
                mc = method;
                break;
            }
            try {
                if (mc == null) {
                    LogsUtil.warn(TAG, "无效分润方式：%s", config.mode_code);
                    return new SyncResult(1, "");
                }
                r = (SyncResult) mc.invoke(this, config, startTime, endTime);
            } catch (InvocationTargetException | IllegalAccessException e) {
                LogsUtil.warn(TAG, "无效分润方式：%s", e.getMessage());
            }

            if (r.code != 0) {
                LogsUtil.warn(TAG, String.format("[%s][%s] - %s - %s %s"
                        , config.channel_phone
                        , config.cs_id
                        , date
                        , config.mode_code
                        , r.msg));

                Map<String, Object> set_data = new HashMap<>();
                set_data.put("last_log_message", String.format("%s - %s", TimeUtil.toTimeString(), r.msg));
                config.where("id", config.id).update(set_data);
                return r;
            }
            raw_data = (Map<String, Object>) r.getData();
            incomeAmount = MapUtil.getBigDecimal(raw_data, "incomeAmount");
            raw_data.remove("incomeAmount");
            // 调整收益，当出现负数时，收益为0
            if (incomeAmount.compareTo(BigDecimal.ZERO) < 0) incomeAmount = BigDecimal.ZERO;

            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("date", date);
            set_data.put("date_time", startTime);
            set_data.put("amount", incomeAmount);
            set_data.put("raw_data", JSONObject.toJSONString(raw_data));
            set_data.put("update_time", TimeUtil.getTimestamp());

            // 检查是否有了收益
            if (RSProfitIncomeLogsEntity.getInstance()
                    .where("date_time", startTime)
                    .where("config_id", config.id)
                    .exist()) {
                // 更新收益记录
                RSProfitIncomeLogsEntity.getInstance()
                        .where("date_time", startTime)
                        .where("config_id", config.id)
                        .update(set_data);

                LogsUtil.info(TAG, "\u001B[34m[%s][%s] - %s - 更新收益 %s ￥%s\u001B[0m"
                        , config.channel_phone
                        , config.cs_id
                        , date
                        , config.mode_code
                        , incomeAmount.setScale(2, RoundingMode.HALF_UP)
                );

                Map<String, Object> last_log_message = new HashMap<>();
                last_log_message.put("last_log_message", String.format("%s - 更新收益 %s +￥%s", TimeUtil.toTimeString(), config.mode_code, incomeAmount.setScale(2, RoundingMode.HALF_UP)));
                config.where("id", config.id).update(last_log_message);
                return new SyncResult(0, "");
            }

            // 新增收益记录
            set_data.put("cs_id", config.cs_id);
            set_data.put("channel_phone", config.channel_phone);
            set_data.put("channel_role", config.channel_role);
            set_data.put("mode_code", config.mode_code);
            set_data.put("config_id", config.id);
            set_data.put("create_time", TimeUtil.getTimestamp());
            RSProfitIncomeLogsEntity.getInstance().insert(set_data);

            LogsUtil.info(TAG, "\u001B[34m[%s][%s] - %s - 产生收益 %s +￥%s\u001B[0m"
                    , config.channel_phone
                    , config.cs_id
                    , date
                    , config.mode_code
                    , incomeAmount.setScale(2, RoundingMode.HALF_UP)
            );

            Map<String, Object> last_log_message = new HashMap<>();
            last_log_message.put("last_log_message", String.format("%s - 产生收益 %s +￥%s", TimeUtil.toTimeString(), config.mode_code, incomeAmount.setScale(2, RoundingMode.HALF_UP)));
            config.where("id", config.id).update(last_log_message);

            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "发生错误");
        }
        return new SyncResult(1, "");
    }

    // region 根据 分润模式计算收益金额，如需要扩展分润模式计算收益可从下面代码新增逻辑
    private final static String TAG = "分润收益";

    /**
     * 所有纯利润收益,收益金额=电净利润
     *
     * @param config 分润配置
     * @return 同步结果
     */
    @CommandMapping("ALL_NET_PROFIT")
    private SyncResult mode_0(RSProfitConfigEntity config, long startTime, long endTime) {
        try {
            // 查询当月统计信息
            ChargeStationMonthSummaryV2Entity summaryV2Entity = ChargeStationMonthSummaryV2Entity.getInstance()
                    .where("CSId", config.cs_id)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .findEntity();
            if (summaryV2Entity == null || summaryV2Entity.getId() == 0) {
                return new SyncResult(2, String.format("%s 无汇总数据，无法计算收益", TimeUtil.toTimeString(startTime, "yyyy-MM")));
            }

            // 消费金额 = 计次充电消费金额 + 充电卡消费金额 - 计次充电消费调整金额 - 充电卡消费调整金额
            BigDecimal chargeAmount = BigDecimal.ZERO
                    .add(summaryV2Entity.getPay_per_charge_amount())
                    .add(summaryV2Entity.getCard_charge_amount())
                    .subtract(summaryV2Entity.getPay_per_adjustment_charge_amount())
                    .subtract(summaryV2Entity.getCard_adjustment_charge_amount())
                    .setScale(4, RoundingMode.HALF_UP);

            // region 电费计费(包含分摊电费逻辑)

            // 根据充电桩和计费日期读取电费账单
            ElectricityPowerSupplyBillEntity ePowerSupplyBillEntity = ElectricityPowerSupplyBillEntity.getInstance()
                    .getWithCSId(config.cs_id, startTime);
            if (ePowerSupplyBillEntity == null) {
                //表示没有录入电费成本账单
                return new SyncResult(3, "缺少电费账单数据，无法计算收益");
            }
            // 电费成本，动态读取充电桩当月电费成本
            BigDecimal electricityCostAmount = new BigDecimal(ePowerSupplyBillEntity.electricity_fee).setScale(4, RoundingMode.HALF_UP);

            // 检查账单电表是否有多个充电桩，如果存在多个充电桩则按充电端口数比值分配电费或电量度数
            // 分摊电费 = 电费 * (此充电桩端口数 / 此电表绑定充电桩的总端口数)
            String[] meterBindCSId = ElectricityMeterEntity.getInstance().getBindCSIdList(ePowerSupplyBillEntity.meter_id);
            if (meterBindCSId.length > 1) {
                // 存在一个电表绑定多个充电桩
                int totalSocket = ChargeStationMonthSummaryV2Entity.getInstance()
                        .whereIn("CSId", meterBindCSId)
                        .where("date_time", ">=", startTime)
                        .where("date_time", "<=", endTime)
                        .sum("total_socket");
                if (totalSocket == 0) {
                    return new SyncResult(3, "缺少总充电端口数据，无法计算分摊电费");
                }
                // 分摊比例
                BigDecimal ratio = new BigDecimal(summaryV2Entity.getTotal_socket())
                        .divide(new BigDecimal(totalSocket), 4, RoundingMode.HALF_UP);
                // 分摊电费 = 电费 * (此充电桩端口数 / 此电表绑定充电桩的总端口数)
                electricityCostAmount = electricityCostAmount.multiply(ratio).setScale(4, RoundingMode.HALF_UP);
            }

            // endregion

            // region 其他费用：其他分润金额
            BigDecimal otherAmount = new BigDecimal(0);
            // 其他收益者金额
            BigDecimal otherRsProfitIncomeAmount = RSProfitIncomeLogsEntity.getInstance()
                    .where("date_time", startTime)
                    .where("cs_id", config.cs_id)
                    .where("config_id", "!=", config.id)
                    .sumGetBigDecimal("amount", 4, RoundingMode.HALF_UP);

            otherAmount = otherAmount.add(otherRsProfitIncomeAmount).setScale(4, RoundingMode.HALF_UP);
            // endregion

            // 收益金额 = 消费金额 - 电费成本 - 其他费用
            BigDecimal incomeAmount = BigDecimal.ZERO
                    .add(chargeAmount)
                    .subtract(electricityCostAmount)
                    .subtract(otherAmount)
                    .setScale(4, RoundingMode.HALF_UP);

            Map<String, Object> raw_data = new LinkedHashMap<>();
            raw_data.put("消费金额", chargeAmount);
            raw_data.put("电费成本", electricityCostAmount);
            raw_data.put("其他成本", otherAmount);

            raw_data.put("计次充电消费金额", summaryV2Entity.getPay_per_charge_amount());
            raw_data.put("充电卡消费金额", summaryV2Entity.getCard_charge_amount());
            raw_data.put("计次充电消费调整金额", summaryV2Entity.getPay_per_adjustment_charge_amount());
            raw_data.put("充电卡消费调整金额", summaryV2Entity.getCard_adjustment_charge_amount());

            raw_data.put("单价", config.price);
            raw_data.put("比例", config.ratio);

            raw_data.put("incomeAmount", incomeAmount);
            return new SyncResult(0, "", raw_data);
        } catch (Exception e) {
            return new SyncResult(1, String.format("收益计算发生错误: %s", e.getMessage()));
        }
    }

    /**
     * 消费金额比例,收益金额=消费金额*分成比例
     *
     * @param config 分润配置
     * @return 同步结果
     */
    @CommandMapping("CHARGE_AMOUNT_RATIO")
    private SyncResult mode_1(RSProfitConfigEntity config, long startTime, long endTime) {
        try {
            // 查询当月统计信息
            ChargeStationMonthSummaryV2Entity summaryV2Entity = ChargeStationMonthSummaryV2Entity.getInstance()
                    .where("CSId", config.cs_id)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .findEntity();
            if (summaryV2Entity == null || summaryV2Entity.getId() == 0) {
                return new SyncResult(2, String.format("%s 无汇总数据，无法计算收益", TimeUtil.toTimeString(startTime, "yyyy-MM")));
            }

            // 消费金额 = 计次充电消费金额 + 充电卡消费金额 - 计次充电消费调整金额 - 充电卡消费调整金额
            BigDecimal chargeAmount = BigDecimal.ZERO
                    .add(summaryV2Entity.getPay_per_charge_amount())
                    .add(summaryV2Entity.getCard_charge_amount())
                    .subtract(summaryV2Entity.getPay_per_adjustment_charge_amount())
                    .subtract(summaryV2Entity.getCard_adjustment_charge_amount())
                    .setScale(4, RoundingMode.HALF_UP);

            // 计算收益： 收益金额 = 消费金额 * 分成比例
            BigDecimal incomeAmount = chargeAmount.multiply(config.ratio).setScale(4, RoundingMode.HALF_UP);

            Map<String, Object> raw_data = new LinkedHashMap<>();
            raw_data.put("消费金额", chargeAmount);
            raw_data.put("计次充电消费金额", summaryV2Entity.getPay_per_charge_amount());
            raw_data.put("充电卡消费金额", summaryV2Entity.getCard_charge_amount());
            raw_data.put("计次充电消费调整金额", summaryV2Entity.getPay_per_adjustment_charge_amount());
            raw_data.put("充电卡消费调整金额", summaryV2Entity.getCard_adjustment_charge_amount());

            raw_data.put("比例", config.ratio);
            raw_data.put("incomeAmount", incomeAmount);
            return new SyncResult(0, "", raw_data);
        } catch (Exception e) {
            return new SyncResult(1, String.format("收益计算发生错误: %s", e.getMessage()));
        }
    }

    /**
     * 净利润金额比例,收益金额=净利润金额*分成比例
     *
     * @param config 分润配置
     * @return 同步结果
     */
    @CommandMapping("NET_PROFIT_RATIO")
    private SyncResult mode_2(RSProfitConfigEntity config, long startTime, long endTime) {
        try {
            // 查询当月统计信息
            ChargeStationMonthSummaryV2Entity summaryV2Entity = ChargeStationMonthSummaryV2Entity.getInstance()
                    .where("CSId", config.cs_id)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .findEntity();
            if (summaryV2Entity == null || summaryV2Entity.getId() == 0) {
                return new SyncResult(2, String.format("%s 无汇总数据，无法计算收益", TimeUtil.toTimeString(startTime, "yyyy-MM")));
            }

            // 消费金额 = 计次充电消费金额 + 充电卡消费金额 - 计次充电消费调整金额 - 充电卡消费调整金额
            BigDecimal chargeAmount = BigDecimal.ZERO
                    .add(summaryV2Entity.getPay_per_charge_amount())
                    .add(summaryV2Entity.getCard_charge_amount())
                    .subtract(summaryV2Entity.getPay_per_adjustment_charge_amount())
                    .subtract(summaryV2Entity.getCard_adjustment_charge_amount())
                    .setScale(4, RoundingMode.HALF_UP);

            // region 电费计费(包含分摊电费逻辑)

            // 根据充电桩和计费日期读取电费账单
            ElectricityPowerSupplyBillEntity ePowerSupplyBillEntity = ElectricityPowerSupplyBillEntity.getInstance()
                    .getWithCSId(config.cs_id, startTime);
            if (ePowerSupplyBillEntity == null) {
                //表示没有录入电费成本账单
                return new SyncResult(3, "缺少电费账单数据，无法计算收益");
            }
            // 总电费
            BigDecimal electricityCostAmount = new BigDecimal(ePowerSupplyBillEntity.electricity_fee).setScale(4, RoundingMode.HALF_UP);

            // 检查账单电表是否有多个充电桩，如果存在多个充电桩则按充电端口数比值分配电费或电量度数
            // 分摊电费 = 电费 * (此充电桩端口数 / 此电表绑定充电桩的总端口数)
            String[] meterBindCSId = ElectricityMeterEntity.getInstance().getBindCSIdList(ePowerSupplyBillEntity.meter_id);
            if (meterBindCSId.length > 1) {
                // 存在一个电表绑定多个充电桩
                int totalSocket = ChargeStationMonthSummaryV2Entity.getInstance()
                        .whereIn("CSId", meterBindCSId)
                        .where("date_time", ">=", startTime)
                        .where("date_time", "<=", endTime)
                        .sum("total_socket");
                if (totalSocket == 0) {
                    return new SyncResult(3, "缺少总充电端口数据，无法计算分摊电费");
                }
                // 分摊比例
                BigDecimal ratio = new BigDecimal(summaryV2Entity.getTotal_socket())
                        .divide(new BigDecimal(totalSocket), 4, RoundingMode.HALF_UP);
                // 分摊电费 = 电费 * (此充电桩端口数 / 此电表绑定充电桩的总端口数)
                electricityCostAmount = electricityCostAmount.multiply(ratio)
                        .setScale(4, RoundingMode.HALF_UP);
            }

            // endregion

            // TODO 其他费用，动态读取其他费用
            BigDecimal otherAmount = new BigDecimal(0);

            // 净利润= 消费金额 - 电费成本 - 其他费用
            BigDecimal netProfitAmount = BigDecimal.ZERO
                    .add(chargeAmount)
                    .subtract(electricityCostAmount)
                    .subtract(otherAmount)
                    .setScale(4, RoundingMode.HALF_UP);

            // 计算收益 ：收益金额= 净利润 * 分成比例
            BigDecimal incomeAmount = BigDecimal.ZERO
                    .add(netProfitAmount)
                    .multiply(config.ratio)
                    .setScale(4, RoundingMode.HALF_UP);

            Map<String, Object> raw_data = new LinkedHashMap<>();
            raw_data.put("净利润", netProfitAmount);
            raw_data.put("消费金额", chargeAmount);
            raw_data.put("电费成本", electricityCostAmount);
            raw_data.put("其他成本", otherAmount);

            raw_data.put("计次充电消费金额", summaryV2Entity.getPay_per_charge_amount());
            raw_data.put("充电卡消费金额", summaryV2Entity.getCard_charge_amount());
            raw_data.put("计次充电消费调整金额", summaryV2Entity.getPay_per_adjustment_charge_amount());
            raw_data.put("充电卡消费调整金额", summaryV2Entity.getCard_adjustment_charge_amount());

            raw_data.put("比例", config.ratio);
            raw_data.put("incomeAmount", incomeAmount);
            return new SyncResult(0, "", raw_data);
        } catch (Exception e) {
            return new SyncResult(1, String.format("收益计算发生错误: %s", e.getMessage()));
        }
    }

    /**
     * 充电端口数,收益金额=充电端口单价*端口数量
     *
     * @param config 分润配置
     * @return 同步结果
     */
    @CommandMapping("SOCKET_COUNT")
    private SyncResult mode_3(RSProfitConfigEntity config, long startTime, long endTime) {
        try {
            // 实时查询充电桩端口数据
            int total_socket = DeviceSocketEntity.getInstance()
                    .alias("ds")
                    .join(DeviceEntity.getInstance().theTableName(), "d", "ds.deviceId = d.id")
                    .where("d.CSId", config.cs_id)
                    .count("1");

            // 计算收益 ：收益金额= 充电端口单价 * 端口数量
            BigDecimal incomeAmount = new BigDecimal(total_socket)
                    .multiply(config.price)
                    .setScale(4, RoundingMode.HALF_UP);

            Map<String, Object> raw_data = new LinkedHashMap<>();
            raw_data.put("端口数", total_socket);
            raw_data.put("单价", config.price);

            raw_data.put("incomeAmount", incomeAmount);
            return new SyncResult(0, "", raw_data);
        } catch (Exception e) {
            return new SyncResult(1, String.format("收益计算发生错误: %s", e.getMessage()));
        }
    }

    /**
     * 电费,收益金额=电费单价*度数
     *
     * @param config 分润配置
     * @return 同步结果
     */
    @CommandMapping("ELECTRICITY_FEE")
    private SyncResult mode_4(RSProfitConfigEntity config, long startTime, long endTime) {
        try {
            // 查询当月统计信息
            ChargeStationMonthSummaryV2Entity summaryV2Entity = ChargeStationMonthSummaryV2Entity.getInstance()
                    .where("CSId", config.cs_id)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .findEntity();
            if (summaryV2Entity == null || summaryV2Entity.getId() == 0) {
                return new SyncResult(2, String.format("%s 无汇总数据，无法计算收益", TimeUtil.toTimeString(startTime, "yyyy-MM")));
            }

            // region 电量计费（包含分摊电量逻辑）

            // 根据充电桩和计费日期读取电费账单
            ElectricityPowerSupplyBillEntity ePowerSupplyBillEntity = ElectricityPowerSupplyBillEntity.getInstance()
                    .getWithCSId(config.cs_id, startTime);
            if (ePowerSupplyBillEntity == null) {
                //表示没有录入电费成本账单
                return new SyncResult(3, "缺少电费账单数据，无法计算收益");
            }
            // 总度数
            BigDecimal electricityMeterValue = new BigDecimal(ePowerSupplyBillEntity.power_consumption).setScale(4, RoundingMode.HALF_UP);

            // 检查账单电表是否有多个充电桩，如果存在多个充电桩则按充电端口数比值分配电费或电量度数
            // 分摊电量 = 电量 * (此充电桩端口数 / 此电表绑定充电桩的总端口数)
            String[] meterBindCSId = ElectricityMeterEntity.getInstance().getBindCSIdList(ePowerSupplyBillEntity.meter_id);
            if (meterBindCSId.length > 1) {
                // 存在一个电表绑定多个充电桩
                int totalSocket = ChargeStationMonthSummaryV2Entity.getInstance()
                        .whereIn("CSId", meterBindCSId)
                        .where("date_time", ">=", startTime)
                        .where("date_time", "<=", endTime)
                        .sum("total_socket");
                if (totalSocket == 0) {
                    return new SyncResult(3, String.format("[%s] %s %s 缺少总充电端口数据，无法计算分摊电量"
                            , config.channel_phone
                            , TimeUtil.toTimeString(startTime)
                            , config.cs_id));
                }
                // 分摊比例
                BigDecimal ratio = new BigDecimal(summaryV2Entity.getTotal_socket())
                        .divide(new BigDecimal(totalSocket), 4, RoundingMode.HALF_UP);
                // 分摊电量 = 电量 * (此充电桩端口数 / 此电表绑定充电桩的总端口数)
                electricityMeterValue = electricityMeterValue.multiply(ratio).setScale(4, RoundingMode.HALF_UP);
            }

            // endregion

            // TODO 其他费用，动态读取其他费用
            BigDecimal otherAmount = new BigDecimal(0);

            // 计算收益 ：收益金额 = 电费单价 * 度数 - 其他费用
            BigDecimal incomeAmount = electricityMeterValue
                    .multiply(config.price)
                    .subtract(otherAmount)
                    .setScale(4, RoundingMode.HALF_UP);

            Map<String, Object> raw_data = new LinkedHashMap<>();
            raw_data.put("电量(度)", electricityMeterValue);
            raw_data.put("单价", config.price);
            raw_data.put("其他成本", otherAmount);

            raw_data.put("incomeAmount", incomeAmount);
            return new SyncResult(0, "", raw_data);
        } catch (Exception e) {
            return new SyncResult(1, String.format("产生收益发生错误: %s", e.getMessage()));
        }
    }

    /**
     * 电费+净利润比例,收益金额=电费单价*度数+净利润*分成比例
     *
     * @param config 分润配置
     * @return 同步结果
     */
    @CommandMapping("ELECTRICITY_AND_NET_PROFIT_RATIO")
    private SyncResult mode_5(RSProfitConfigEntity config, long startTime, long endTime) {
        try {
            // 查询当月统计信息
            ChargeStationMonthSummaryV2Entity summaryV2Entity = ChargeStationMonthSummaryV2Entity.getInstance()
                    .where("CSId", config.cs_id)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .findEntity();
            if (summaryV2Entity == null || summaryV2Entity.getId() == 0) {
                return new SyncResult(2, String.format("%s 无汇总数据，无法计算收益", TimeUtil.toTimeString(startTime, "yyyy-MM")));
            }

            // 消费金额 = 计次充电消费金额 + 充电卡消费金额 - 计次充电消费调整金额 - 充电卡消费调整金额
            BigDecimal chargeAmount = BigDecimal.ZERO
                    .add(summaryV2Entity.getPay_per_charge_amount())
                    .add(summaryV2Entity.getCard_charge_amount())
                    .subtract(summaryV2Entity.getPay_per_adjustment_charge_amount())
                    .subtract(summaryV2Entity.getCard_adjustment_charge_amount())
                    .setScale(4, RoundingMode.HALF_UP);

            // region 电费、电量计费(包含分摊电费、电量逻辑)

            // 根据充电桩和计费日期读取电费账单
            ElectricityPowerSupplyBillEntity ePowerSupplyBillEntity = ElectricityPowerSupplyBillEntity.getInstance()
                    .getWithCSId(config.cs_id, startTime);
            if (ePowerSupplyBillEntity == null) {
                //表示没有录入电费成本账单
                return new SyncResult(3, "缺少电费账单数据，无法计算收益");
            }
            // 电费成本，动态读取充电桩当月电费成本
            BigDecimal electricityCostAmount = new BigDecimal(ePowerSupplyBillEntity.electricity_fee).setScale(4, RoundingMode.HALF_UP);
            // 电量度数，动态读取充电桩当月电量度数
            BigDecimal electricityMeterValue = new BigDecimal(ePowerSupplyBillEntity.power_consumption).setScale(4, RoundingMode.HALF_UP);

            // 检查账单电表是否有多个充电桩，如果存在多个充电桩则按充电端口数比值分配电费或电量度数
            // 分摊电费 = 电费 * (此充电桩端口数 / 此电表绑定充电桩的总端口数)
            String[] meterBindCSId = ElectricityMeterEntity.getInstance().getBindCSIdList(ePowerSupplyBillEntity.meter_id);
            if (meterBindCSId.length > 1) {
                // 存在一个电表绑定多个充电桩
                int totalSocket = ChargeStationMonthSummaryV2Entity.getInstance()
                        .whereIn("CSId", meterBindCSId)
                        .where("date_time", ">=", startTime)
                        .where("date_time", "<=", endTime)
                        .sum("total_socket");
                if (totalSocket == 0) {
                    return new SyncResult(3, "缺少总充电端口数据，无法计算分摊电费");
                }
                // 分摊比例
                BigDecimal ratio = new BigDecimal(summaryV2Entity.getTotal_socket())
                        .divide(new BigDecimal(totalSocket), 4, RoundingMode.HALF_UP);
                // 分摊电费 = 电费 * (此充电桩端口数 / 此电表绑定充电桩的总端口数)
                electricityCostAmount = electricityCostAmount.multiply(ratio).setScale(4, RoundingMode.HALF_UP);
                // 分摊电量 = 电量 * (此充电桩端口数 / 此电表绑定充电桩的总端口数)
                electricityMeterValue = electricityMeterValue.multiply(ratio).setScale(4, RoundingMode.HALF_UP);
            }

            // endregion


            // TODO 其他费用，动态读取其他费用
            BigDecimal otherAmount = new BigDecimal(0);

            // 净利润= 消费金额 - 电费成本 - 其他费用
            BigDecimal netProfitAmount = BigDecimal.ZERO
                    .add(chargeAmount)
                    .subtract(electricityCostAmount)
                    .subtract(otherAmount)
                    .setScale(4, RoundingMode.HALF_UP);

            // 计算收益 ：收益金额= 电费单价 * 度数 + 净利润 * 分成比例
            BigDecimal incomeAmount = BigDecimal.ZERO
                    .add(electricityMeterValue
                            .multiply(config.price))
                    .add(netProfitAmount
                            .multiply(config.ratio))
                    .setScale(4, RoundingMode.HALF_UP);

            Map<String, Object> raw_data = new LinkedHashMap<>();
            raw_data.put("净利润", netProfitAmount);
            raw_data.put("电量(度)", electricityMeterValue);

            raw_data.put("消费金额", chargeAmount);
            raw_data.put("电费成本", electricityCostAmount);
            raw_data.put("其他成本", otherAmount);

            raw_data.put("计次充电消费金额", summaryV2Entity.getPay_per_charge_amount());
            raw_data.put("充电卡消费金额", summaryV2Entity.getCard_charge_amount());
            raw_data.put("计次充电消费调整金额", summaryV2Entity.getPay_per_adjustment_charge_amount());
            raw_data.put("充电卡消费调整金额", summaryV2Entity.getCard_adjustment_charge_amount());

            raw_data.put("单价", config.price);
            raw_data.put("比例", config.ratio);

            raw_data.put("incomeAmount", incomeAmount);
            return new SyncResult(0, "", raw_data);
        } catch (Exception e) {
            return new SyncResult(1, String.format("产生收益发生错误: %s", e.getMessage()));
        }
    }
    //endregion

    // endregion

    // region Controller交互

    /**
     * 上月收益数据 / 当月预估收益数
     *
     * @return
     */
    public ISyncResult getSummaryIncome(String channel_phone) {
        String cacheKey = String.format("RSProfit:%s:SummaryIncome", channel_phone);
        Map<String, Object> data = DataService.getMainCache().getMap(cacheKey);
        if (data != null && !data.isEmpty()) return new SyncResult(0, "", data);

        data = new LinkedHashMap<>();

        // region 上个月收益数据
        long lastStartTime = TimeUtil.getMonthBegin00(-1);
        long lastEndTime = TimeUtil.getMonthEnd24(-1);
        String lastDate = TimeUtil.toTimeString(lastStartTime, "yyyy-MM");
        BigDecimal lastIncome = RSProfitIncomeLogsEntity.getInstance()
//                .cache(String.format("RSProfit:Income:%s:last_income", channel_phone))
                .where("channel_phone", channel_phone)
                .where("date_time", ">=", lastStartTime)
                .where("date_time", "<=", lastEndTime)
                .sumGetBigDecimal("amount", 2, RoundingMode.HALF_UP);
        data.put("last_income", new JSONObject() {{
            put("date", lastDate);
            put("income", lastIncome);
        }});
        // endregion

        // region 当月预估收益数据
        long currentStartTime = TimeUtil.getMonthBegin00();
        long currentEndTime = TimeUtil.getMonthEnd24();
        String currentDate = TimeUtil.toTimeString(currentStartTime, "yyyy-MM");
        BigDecimal currentIncome = RSProfitIncomeLogsEntity.getInstance()
//                .cache(String.format("RSProfit:Income:%s:current_income", channel_phone))
                .where("channel_phone", channel_phone)
                .where("date_time", ">=", currentStartTime)
                .where("date_time", "<=", currentEndTime)
                .sumGetBigDecimal("amount", 2, RoundingMode.HALF_UP);

        data.put("current_income", new JSONObject() {{
            put("date", currentDate);
            put("income", currentIncome);
        }});
        // endregion

        DataService.getMainCache().setMap(cacheKey, data);

        return new SyncResult(0, "", data);
    }

    /**
     * 合作站点数量 / 端口数量
     *
     * @return
     */
    public ISyncResult getSummaryCount(String channel_phone) {
        String cacheKey = String.format("RSProfit:%s:SummaryCount", channel_phone);
        Map<String, Object> data = DataService.getMainCache().getMap(cacheKey);
        if (data != null && !data.isEmpty()) return new SyncResult(0, "", data);

        int cs_count = 0;
        int socket_count = 0;

        // 读取站点列表
        String[] csIdList = RSProfitConfigService.getInstance().getCSIdList(channel_phone, true);
        if (csIdList != null && csIdList.length > 0) {
            // 充电桩数量
            cs_count = csIdList.length;

            // 端口数量
            socket_count = ChargeStationSummaryV2Entity.getInstance()
                    .whereIn("CSId", csIdList)
                    .sum("total_socket");
        }

        //合作充电桩信息
        data = new LinkedHashMap<>();
        data.put("cs_count", cs_count);
        data.put("socket_count", socket_count);

        DataService.getMainCache().setMap(cacheKey, data);
        return new SyncResult(0, "", data);
    }

    /**
     * 获取渠道商充电桩汇总分页数据
     *
     * @param channel_phone 渠道联系手机号
     * @param page          第几页
     * @param limit         每页限制
     * @param orderby       排序字段
     * @return
     */
    public ISyncResult getChargeStationSummaryList(String channel_phone, int page, int limit, String orderby) {
        String countCacheKey = String.format("RSProfit:%s:ChargeStationSummary:TotalCount", channel_phone);
        String listCacheKey = String.format("RSProfit:%s:ChargeStationSummary:%s_%s_%s", channel_phone, page, limit, common.md5(orderby));

        int count = DataService.getMainCache().getInt(countCacheKey, -1);
        if (count != -1) {
            List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
            if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
        }

        count = RSProfitChargeStationSummaryEntity.getInstance()
                .where("channel_phone", channel_phone)
                .count();
        if (count == 0) return new SyncResult(1, "");

        List<Map<String, Object>> list = RSProfitChargeStationSummaryEntity.getInstance()
                .field("cs_id AS CSId,total_income")
                .where("channel_phone", channel_phone)
                .order(orderby)
                .page(page, limit)
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(1, "");

        for (Map<String, Object> data : list) {
            String CSId = MapUtil.getString(data, "CSId");
            Map<String, Object> detail = ChargeStationEntity.getInstance()
                    .field("name,arch,total_socket,charge_time_use_rate" +
                            ",province,city,district,street,communities,roads,address" +
                            ",online_time,lat,lon,status")
                    .alias("cs")
                    .join(ChargeStationSummaryV2Entity.getInstance().theTableName(), "css", "cs.CSId = css.CSId")
                    .where("cs.CSId", CSId)
                    .limit(1)
                    .find();

            data.put("station_name", MapUtil.getString(detail, "name"));
            data.put("arch", MapUtil.getInt(detail, "arch"));

            data.put("total_socket", MapUtil.getInt(detail, "total_socket"));
            data.put("charge_time_use_rate", MapUtil.getDouble(detail, "charge_time_use_rate"));

            data.put("province", MapUtil.getString(detail, "province"));
            data.put("city", MapUtil.getString(detail, "city"));
            data.put("district", MapUtil.getString(detail, "district"));
            data.put("street", MapUtil.getString(detail, "street"));
            data.put("communities", MapUtil.getString(detail, "communities"));
            data.put("roads", MapUtil.getString(detail, "roads"));
            data.put("address", MapUtil.getString(detail, "address"));
            data.put("lat", MapUtil.getDouble(detail, "lat"));
            data.put("lon", MapUtil.getDouble(detail, "lon"));

            data.put("online_time", MapUtil.getLong(detail, "online_time"));
            data.put("status", MapUtil.getInt(detail, "status"));
        }

        DataService.getMainCache().set(countCacheKey, count);
        DataService.getMainCache().setList(listCacheKey, list);

        return new SyncResult(0, "", list);
    }

    /**
     * 获取 站点详情
     *
     * @param channel_phone 渠道手机号码
     * @param CSId          站点id
     * @return
     */
    public ISyncResult getChargeStationDetail(String channel_phone, String CSId) {
        String cacheKey = String.format("RSProfit:%s:%s:Detail", channel_phone, CSId);
        Map<String, Object> data = DataService.getMainCache().getMap(cacheKey);
        if (data != null && !data.isEmpty()) return new SyncResult(0, "", data);

        //检查是否有权限查询站点信息
        if (!RSProfitConfigEntity.getInstance()
                .where("channel_phone", channel_phone)
                .where("cs_id", CSId)
                .exist()) {
            return new SyncResult(98, "无权查看数据");
        }

        data = ChargeStationEntity.getInstance()
                .field("name,arch,total_socket,charge_time_use_rate" +
                        ",province,city,district,street,street_code,communities,roads,address,lat,lon" +
                        ",online_time,status")
                .alias("cs")
                .join(ChargeStationSummaryV2Entity.getInstance().theTableName(), "css", "cs.CSId = css.CSId")
                .where("cs.CSId", CSId)
                .find();
        if (data == null || data.isEmpty()) return new SyncResult(1, "");

        //累计收益
        Map<String, Object> incomeData = RSProfitChargeStationSummaryEntity.getInstance()
                .field("total_income")
                .where("channel_phone", channel_phone)
                .where("cs_id", CSId)
                .find();

        data.put("total_income", MapUtil.getBigDecimal(incomeData, "total_income", 2, RoundingMode.HALF_UP));

        DataService.getMainCache().setMap(cacheKey, data);

        return new SyncResult(0, "", data);
    }

    /**
     * 获取 充电桩 月收益数据
     *
     * @param channel_phone 渠道手机号码
     * @param CSId          站点id
     * @param page          第几页
     * @param limit         每页显示
     * @param orderby       排序字段
     * @return
     */
    public ISyncResult getChargeStationMonthIncomeList(String channel_phone, String CSId, int page, int limit, String orderby) {
        String countCacheKey = String.format("RSProfit:%s:%s:ChargeStationMonthIncome:TotalCount", channel_phone, CSId);
        String listCacheKey = String.format("RSProfit:%s:%s:ChargeStationMonthIncome:%s_%s_%s", channel_phone, CSId, page, limit, common.md5(orderby));

        int count = DataService.getMainCache().getInt(countCacheKey, -1);
        if (count != -1) {
            List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
            if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
        }

        count = RSProfitChargeStationMonthSummaryEntity.getInstance()
                .where("channel_phone", channel_phone)
                .where("cs_id", CSId)
                .count();
        if (count == 0) return new SyncResult(1, "");

        List<Map<String, Object>> list = RSProfitChargeStationMonthSummaryEntity.getInstance()
                .field("date,total_income")
                .where("channel_phone", channel_phone)
                .where("cs_id", CSId)
                .page(page, limit)
                .order(orderby)
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(1, "");

        DataService.getMainCache().set(countCacheKey, count);
        DataService.getMainCache().setList(listCacheKey, list);

        return new SyncListResult(count, page, limit, list);
    }

    /**
     * 查询渠道月收入
     *
     * @param channel_phone 渠道联系手机号
     * @param page          第几页
     * @param limit         每页限制
     * @param orderby       排序字段
     * @return
     */
    public ISyncResult getMonthSummaryIncomeList(String channel_phone, int page, int limit, String orderby) {
        String countCacheKey = String.format("RSProfit:%s:MonthIncome:TotalCount", channel_phone);
        String listCacheKey = String.format("RSProfit:%s:MonthIncome:%s_%s_%s", channel_phone, page, limit, common.md5(orderby));

        int count = DataService.getMainCache().getInt(countCacheKey, -1);
        if (count != -1) {
            List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
            if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
        }

        count = RSProfitMonthSummaryEntity.getInstance()
                .where("channel_phone", channel_phone)
                .count();
        if (count == 0) return new SyncResult(1, "");

        List<Map<String, Object>> list = RSProfitMonthSummaryEntity.getInstance()
                .field("date,total_income")
                .where("channel_phone", channel_phone)
                .page(page, limit)
                .order(orderby)
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(1, "");

        DataService.getMainCache().set(countCacheKey, count);
        DataService.getMainCache().setList(listCacheKey, list);

        return new SyncListResult(count, page, limit, list);
    }

    /**
     * 每日充电数据汇总：每日消费数、每日充电次数
     *
     * @param channel_phone 渠道手机号
     * @return
     */
    public ISyncResult getChargeDaySummary(String channel_phone) {
        String cacheKey = String.format("RSProfit:%s:ChargeDaySummary", channel_phone);
        Map<String, Object> data = DataService.getMainCache().getMap(cacheKey);
        if (data != null && !data.isEmpty()) return new SyncResult(0, "", data);

        // 读取站点列表
        String[] csIdList = RSProfitConfigService.getInstance().getCSIdList(channel_phone, true);
        if (csIdList.length == 0) return new SyncResult(1, "");

        data = ChargeStationDaySummaryV2Entity.getInstance()
                .field("IFNULL(SUM(pay_per_charge_amount - pay_per_adjustment_charge_amount + card_charge_amount - card_adjustment_charge_amount),0) AS charge_amount" +
                        ",IFNULL(SUM(total_use_count),0) AS total_use_count")
                .whereIn("CSId", csIdList)
                .where("date_time", ">=", TimeUtil.getTime00())
                .find();
        if (data == null || data.isEmpty()) return new SyncResult(1, "");

        DataService.getMainCache().setMap(cacheKey, data);

        return new SyncResult(0, "", data);
    }

    /**
     * 渠道站点 - 每日充电数据汇总列表：每日消费数、每日充电次数
     *
     * @param channel_phone 渠道手机号
     * @param page          第几页
     * @param limit         每页限制
     * @param orderby       排序
     * @return
     */
    public ISyncResult getChargeDataDayList(String channel_phone, int page, int limit, String orderby) {
        String countCacheKey = String.format("RSProfit:%s:ChargeData:DayList:TotalCount", channel_phone);
        String listCacheKey = String.format("RSProfit:%s:ChargeData:DayList:%s_%s_%s", channel_phone, page, limit, common.md5(orderby));

        int count = DataService.getMainCache().getInt(countCacheKey, -1);
        if (count != -1) {
            List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
            if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
        }

        String[] csIdList = RSProfitConfigService.getInstance().getCSIdList(channel_phone, true);

        String cs_table_name = ChargeStationEntity.getInstance().theTableName();

        count = ChargeStationDaySummaryV2Entity.getInstance()
                .alias("css")
                .join(cs_table_name, "cs", "cs.CSId = css.CSId")
                .whereIn("css.CSId", csIdList)
                .count();
        if (count == 0) return new SyncResult(1, "");

        List<Map<String, Object>> list = ChargeStationDaySummaryV2Entity.getInstance()
                .alias("css")
                .field("css.CSId,name"
                        + ",date,total_use_count"
                        + ",(pay_per_charge_amount - pay_per_adjustment_charge_amount + card_charge_amount - card_adjustment_charge_amount) AS charge_amount"
                        + ",province,city,district,street,communities,roads,address")
                .leftJoin(cs_table_name, "cs", "cs.CSId = css.CSId")
                .whereIn("css.CSId", csIdList)
                .where("date_time", ">=", TimeUtil.getTime00())
                .order(orderby)
                .page(page, limit)
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(1, "");

        DataService.getMainCache().set(countCacheKey, count);
        DataService.getMainCache().setList(listCacheKey, list);

        return new SyncResult(0, "", list);
    }

    /**
     * 渠道站点 - 对应站点 - 7天充电数据汇总列表：消费金额、充电次数
     *
     * @param channel_phone 渠道手机号
     * @param CSId          站点id
     * @return
     */
    public ISyncResult getChargeStation7DayChargeList(String channel_phone, String CSId) {
        String listCacheKey = String.format("RSProfit:%s:%s:ChargeData:7DayList", channel_phone, CSId);

        List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
        if (list != null && !list.isEmpty()) return new SyncResult(0, "", list);

        //检查是否有权限查询站点信息
        if (!RSProfitConfigEntity.getInstance()
                .where("channel_phone", channel_phone)
                .where("cs_id", CSId)
                .exist()) {
            return new SyncResult(98, "无权查看数据");
        }

        long _7DayAgo = TimeUtil.getTime00(-7);
        list = ChargeStationDaySummaryV2Entity.getInstance()
                .field("id,CSId,date,total_use_count"
                        + ",(pay_per_charge_amount - pay_per_adjustment_charge_amount + card_charge_amount - card_adjustment_charge_amount) AS charge_amount"
                )
                .whereIn("CSId", CSId)
                .where("date_time", ">", _7DayAgo)
                .order("date_time DESC")
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(1, "");

        DataService.getMainCache().setList(listCacheKey, list);

        return new SyncResult(0, "", list);
    }

    // endregion
}
