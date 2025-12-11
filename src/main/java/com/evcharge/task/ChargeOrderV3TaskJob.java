package com.evcharge.task;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.order.ChargeOrderV3Entity;
import com.evcharge.entity.order.settlement.v3.SettlementForElectricityV3;
import com.evcharge.entity.station.ChargeOrderHeartbeatEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.EBillingType;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.enumdata.EChargePaymentType;
import com.evcharge.enumdata.EPaymentOrderType;
import com.evcharge.mqtt.XMQTTFactory;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

/**
 * 充电订单监控任务
 */
public class ChargeOrderV3TaskJob implements Job {
    protected final static String mMonitorText = "充电订单v3监控任务";
    private final String mGroupName = this.getClass().getSimpleName();
    private final static String mServerIPv4 = common.getLocalIPv4();

    private static ChargeOrderV3TaskJob _this;

    public static ChargeOrderV3TaskJob getInstance() {
        if (_this == null) _this = new ChargeOrderV3TaskJob();
        return _this;
    }

    /**
     * 添加一个监控任务
     */
    public SyncResult add(String OrderSN) {
        try {
            if (!StringUtils.hasLength(OrderSN)) return new SyncResult(1, "");

            Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(TimeUtil.toDate(TimeUtil.getTimestamp() + 5000))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(20)
                            .withRepeatCount(30)
                            .withMisfireHandlingInstructionFireNow()
                    )
                    .build();

            //参数
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("OrderSN", OrderSN);

            //添加任务到调度器
            SyncResult r = QuartzSchedulerManager.getInstance().add(OrderSN
                    , mGroupName
                    , trigger
                    , this.getClass()
                    , jobDataMap
                    , mMonitorText
            );
            if (r.code == 0) {
                DataService.getMainCache().setMap(String.format("Task:%s:%s:%s", mServerIPv4, mGroupName, OrderSN), jobDataMap, true);
                LogsUtil.info("", "\033[1;94m [%s] - 开始监控：OrderSN=%s 下次执行：%s\033[0m", mMonitorText, OrderSN, TimeUtil.toTimeString(trigger.getNextFireTime()));
            } else {
                LogsUtil.info("", "\033[1;94m [%s] - 开始监控发生错误：OrderSN=%s 原因=%s \033[0m", mMonitorText, OrderSN, r.msg);
            }
            return r;
        } catch (Exception e) {
            LogsUtil.error(e, mMonitorText, "添加任务发生错误");
        }
        return new SyncResult(1, "添加失败");
    }

    /**
     * 移除监控任务
     *
     * @param OrderSN   充电订单号
     * @param delReason 停止原因
     */
    public void remove(String OrderSN, String delReason) {
        try {
            QuartzSchedulerManager.getInstance().del(OrderSN, mGroupName);
            DataService.getMainCache().del(String.format("Task:%s:%s:%s", mServerIPv4, mGroupName, OrderSN));
            LogsUtil.info(mMonitorText, "\033[1;91m 删除任务：OrderSN=%s 原因=%s \033[0m", OrderSN, delReason);
        } catch (Exception e) {
            LogsUtil.error(e, mMonitorText, "删除任务发生错误");
        }
    }

    /**
     * 手动触发一个任务
     *
     * @param OrderSN 订单号
     */
    public SyncResult trigger(String OrderSN) {
        try {
            //参数
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("OrderSN", OrderSN);

            QuartzSchedulerManager.getInstance().triggerJob(OrderSN, mGroupName, jobDataMap);

            LogsUtil.info(mMonitorText, "\033[1;91m 触发任务 - OrderSN = %s \033[0m", OrderSN);
            return new SyncResult(0, "操作成功");
        } catch (Exception e) {
            LogsUtil.error(e, mMonitorText, "手动触发任务生错误 - CSId = %s", OrderSN);
        }
        return new SyncResult(1, "操作失败");
    }

    /**
     * 恢复监控任务
     */
    public void resume() {
        ThreadUtil.getInstance().execute(String.format("[%s]恢复", mMonitorText), () -> {
            LogsUtil.info(mMonitorText, "\033[1;94m 恢复任务 \033[0m");

            Set<String> keyset = DataService.getMainCache().keys(String.format("Task:%s:%s:*", mServerIPv4, mGroupName));
            if (keyset.isEmpty()) {
                LogsUtil.info(mMonitorText, "\033[1;94m 恢复任务 %s - 找不到充电中的订单缓存数据 \033[0m", mServerIPv4);
                return;
            }

            for (String key : keyset) {
                Map<String, Object> data = DataService.getMainCache().getMap(key);
                String OrderSN = MapUtil.getString(data, "OrderSN");
                add(OrderSN);
            }
        });
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();
        String OrderSN = MapUtil.getString(data, "OrderSN");
        LogsUtil.info(mMonitorText, "\033[1;94m 执行任务：OrderSN =%s \033[0m", OrderSN);

        if (!StringUtils.hasLength(OrderSN)) {
            remove(OrderSN, String.format("JobData中订单号为空 [%s.%s]", mGroupName, OrderSN));
            return;
        }

        SyncResult r = run(OrderSN);
        if (r.code != 0) {
            int errorCount = DataService.getMainCache().getInt(String.format("Task:%s:ErrorCount:%s:%s", common.getLocalIPv4(), mGroupName, OrderSN), 0);
            if (errorCount > 4) {
                remove(OrderSN, "任务执行期间发生错误次数过多");
            } else {
                errorCount++;
                DataService.getMainCache().set(String.format("Task:%s:ErrorCount:%s:%s", common.getLocalIPv4(), mGroupName, OrderSN), errorCount, ECacheTime.DAY);
            }
            return;
        }
    }

    /**
     * 更新日志：2024-02-18
     * 检查订单是否超过预设订单，如果超过则强制结束
     *
     * @param OrderSN 订单号
     * @return 同步结果
     */
    private SyncResult run(String OrderSN) {
        try {
            // 查询充电订单。使用订单号OrderSN作为查询条件。
            ChargeOrderV3Entity orderEntity = ChargeOrderV3Entity.getInstance()
                    .where("OrderSN", OrderSN)
                    .findEntity();
            // 如果没有找到订单或订单ID为0，则返回错误信息。
            if (orderEntity == null || orderEntity.id == 0) return new SyncResult(10, "查询不到充电订单");

            //判断计费方式是不是以电量计费，如果是使用另外一种订单监控
            if (orderEntity.billingType == EBillingType.Electricity.index) return runForElectricity(orderEntity);

            // 检查充电订单的状态。
            // 如果订单状态为2，表示充电已完成，进行结算并移除监控。
            if (orderEntity.status == 2) {
                remove(OrderSN, "充电完成，已经进行了结算");
                return new SyncResult(0, "");
            }

            // 如果订单状态为-1，表示订单发生错误。
            if (orderEntity.status == -1) {
                // 如果从开始充电起已超过10分钟且状态仍为-1，或者有明确的停止原因，移除监控。
                if (TimeUtil.getTimestamp() > orderEntity.startTime + 10 * ECacheTime.MINUTE || orderEntity.stopReasonCode != 0) {
                    remove(OrderSN, "充电订单发生错误，自动停止监控，原因：" + orderEntity.stopReasonText);
                }
                return new SyncResult(3, "充电订单发生错误，原因：" + orderEntity.stopReasonText);
            }

            // 如果订单状态为1，表示充电中。
            if (orderEntity.status == 1) {
                // 充电开始10分钟内不进行任何操作，认为充电正常进行。
                if (TimeUtil.getTimestamp() < orderEntity.startTime + 10 * ECacheTime.MINUTE) {
                    return new SyncResult(0, "充电继续");
                }

                // 如果当前时间未到达预设的结束时间加30分钟，认为充电仍在继续。
                if (TimeUtil.getTimestamp() < orderEntity.endTime + 30 * ECacheTime.MINUTE) {
                    return new SyncResult(0, "充电继续");
                }

                long stopTime = orderEntity.endTime; // 默认以订单预设的停止时间进行结算。
                // 检查最后的心跳包数据，如果存在，则以心跳包最后的时间为准进行结算。
                ChargeOrderHeartbeatEntity heartbeatEntity = ChargeOrderHeartbeatEntity.getInstance()
                        .where("order_id", orderEntity.id)
                        .order("create_time DESC")
                        .findEntity();
                if (heartbeatEntity != null && heartbeatEntity.id > 0) {
                    stopTime = heartbeatEntity.create_time;
                }

                // 尝试下发停止充电指令。这里的操作可能因设备离线或断电而失败。
                DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
                if (deviceEntity == null) return new SyncResult(4, "无法找到设备信息");

                // 检查设备在线状态
                int status = DataService.getMainCache().getInt(String.format("Device:%s:status", orderEntity.deviceCode), -1);
                if (status == -1) status = deviceEntity.online_status;

                // 设备在线，下发停止指令
                if (status == 1) {
                    JSONObject json = new JSONObject();
                    json.put("deviceCode", orderEntity.deviceCode);
                    json.put("ChargeMode", orderEntity.chargeMode);
                    json.put("port", orderEntity.port); // 端口
                    json.put("OrderSN", OrderSN);
                    XMQTTFactory.getInstance().publish(String.format("EvCharge/%s/%s/command/stopCharge", deviceEntity.brandCode, orderEntity.deviceCode), json.toJSONString());
                }
                // 设备不在线，进行软结算
                else if (status == 0) {
                    SyncResult r = orderEntity.chargeFinish(OrderSN, stopTime, -1, "监控自停");
                    if (r.code == 0) remove(OrderSN, "监控到订单充电超过预设时间，进行软结算");
                    else return r;
                }
            }
            return new SyncResult(0, "充电继续");
        } catch (Exception e) {
            // 记录执行过程中发生的异常。
            LogsUtil.error(e, mMonitorText, "执行期间发生错误 订单：%s", OrderSN);
        }
        // 如果执行过程中捕获到异常，则返回执行错误的结果。
        return new SyncResult(1, "执行错误");
    }

    /**
     * 以电量计费的订单监控任务
     *
     * @param orderV3Entity
     * @return
     */
    private SyncResult runForElectricity(ChargeOrderV3Entity orderV3Entity) {
        // 检查充电订单的状态。
        // 如果订单状态为2，表示充电已完成，进行结算并移除监控。
        if (orderV3Entity.status == 2) {
            remove(orderV3Entity.OrderSN, "充电完成，已经进行了结算");
            return new SyncResult(0, "");
        }

        //判断支付订单类型：1=先付后充，2=先充后付
        if (orderV3Entity.paymentOrderType != EPaymentOrderType.UseBeforePayment.index) {
            return new SyncResult(0, "");
        }

        //如果是先充后付的订单类型才执行下去

        //如果支付方式是余额，则应该检查用户是否足够余额
        if (orderV3Entity.paymentType == EChargePaymentType.Balance.index) {
            //检查用户金额
            //电费金额 = 电量 * 价格
            BigDecimal electricityFeeAmount = SettlementForElectricityV3.billingElecAmountTEMP(orderV3Entity.totalElectricity, orderV3Entity.chargeStandardConfigId);
            //服务费金额 = 电量 * 价格
            BigDecimal serviceFeeAmount = SettlementForElectricityV3.billingServiceAmountTEMP(orderV3Entity.totalElectricity, orderV3Entity.chargeStandardConfigId);
            //安全充电保险费用
            BigDecimal safeChargeFee = new BigDecimal(orderV3Entity.safeChargeFee)
                    .setScale(4, RoundingMode.HALF_UP);
            //停车费 = 未知
            BigDecimal parkingFeeAmount = new BigDecimal(0);

            //优惠金额
            BigDecimal discountAmount = orderV3Entity.discountAmount;
            if (discountAmount == null) discountAmount = new BigDecimal(0);

            //应扣费金额 = (电费+服务费+安全充电保险费+停车费)
            BigDecimal receivableAmount = new BigDecimal(0);
            receivableAmount = receivableAmount
                    .add(electricityFeeAmount)
                    .add(serviceFeeAmount)
                    .add(safeChargeFee)
                    .add(parkingFeeAmount)
                    .setScale(6, RoundingMode.HALF_UP);
            //实际扣费金额 = 应扣费金额 - 优惠金额
            BigDecimal totalAmount = receivableAmount
                    .subtract(discountAmount)
                    .setScale(6, RoundingMode.HALF_UP);

            BigDecimal balance = UserSummaryEntity.getInstance().getBalanceWithUid(orderV3Entity.uid);

            //余额减去实际扣费金额
            balance = balance.subtract(totalAmount);
            //检测余额是否小于1，如果是则停止充电
            if (balance.compareTo(new BigDecimal(1)) <= 0) {
                JSONObject mqtt_data = new JSONObject();
                mqtt_data.put("deviceCode", orderV3Entity.deviceCode);
                mqtt_data.put("ChargeMode", orderV3Entity.chargeMode);
                mqtt_data.put("port", orderV3Entity.port);//端口
                mqtt_data.put("OrderSN", orderV3Entity.OrderSN);

                DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderV3Entity.deviceCode);
                if (deviceEntity == null) {
                    LogsUtil.warn(this.getClass().getSimpleName(), "[%s] - 发生验证错误，无法查询充电设备数据 - %s"
                            , orderV3Entity.OrderSN
                            , orderV3Entity.deviceCode);
                    return new SyncResult(0, "");
                }

                //尝试下发指令结束订单，这里不一定会成功，如果主机断网或者断电了，就无法使用了
                /*
                 *  关于内部程序通信的主题定义：
                 *  订阅（平台-->推送-->中转站）：{应用通道}/{设备编号}/command/业务逻辑函数名
                 *  送（中转站-->推送-->平台）：{平台代码}/{应用通道}/{设备编号}/业务逻辑函数名
                 */
                XMQTTFactory.getInstance().publish(String.format("%s/%s/command/stopCharge"
                                , deviceEntity.appChannelCode
                                , deviceEntity.deviceCode)
                        , mqtt_data.toJSONString());
            }
        }
        //如果支付方式是积分，则应该检查用户是否足够积分
        else if (orderV3Entity.paymentType == EChargePaymentType.Integral.index) {

        }
        return new SyncResult(0, "");
    }
}
