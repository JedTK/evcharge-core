package com.evcharge.service.meter;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.enumdata.ENotifyType;
import com.evcharge.libsdk.tq.TQDianBiaoSDK;
import com.evcharge.mqtt.XMQTT3AsyncClient;
import com.evcharge.service.GeneralDevice.GeneralDeviceService;
import com.evcharge.service.GeneralDevice.Meter.SmartMeterRecordService;
import com.evcharge.service.notify.NotifyService;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 拓强电表服务类
 */
public class TQ4GMeterService {
    private final static String TAG = "拓强4G电表";
    private static final String brandCode = "CHTQDQ"; // 品牌代码
    private static final String typeCode = "4GEM"; // 类型代码
    /**
     * 抄表失败后重新抄表次数
     */
    private static final int MAX_RETRY_TIMES = 2;
    private volatile static TQ4GMeterService instance;

    public static TQ4GMeterService getInstance() {
        if (instance == null) {
            synchronized (TQ4GMeterService.class) {
                if (instance == null) {
                    instance = new TQ4GMeterService();
                }
            }
        }
        return instance;
    }

    // region remark - 抄表操作

    /**
     * 批量执行抄表任务
     *
     * @param use_mq
     */
    public void readTask(boolean use_mq) {
        int page = 1;
        int limit = 10; // 建议不要太多，10个比较好，因为数据太多怕对方处理不到，同时如果发送到MQTT的话也可能有数据大小的限制
        long pages;
        long total_count = GeneralDeviceEntity.getInstance()
                .where("brandCode", brandCode) // 品牌：拓强4G电表
                .where("typeCode", typeCode)
                .where("status", 1)
                .page(page, limit)
                .countGetLong("1");
        if (total_count == 0) {
            LogsUtil.warn(TAG, "%s ~ %s 无4G电表可以进行抄表");
            return;
        }
        pages = Convert.toInt(Math.ceil(total_count * 1.0 / limit));

        while (page <= pages) {
            List<Map<String, Object>> list;
            try {
                list = GeneralDeviceEntity.getInstance()
                        .field("serialNumber")
                        .where("brandCode", brandCode) // 品牌：拓强4G电表
                        .where("typeCode", typeCode)
                        .where("status", 1)
                        .page(page, limit)
                        .select();
                if (list == null || list.isEmpty()) break;
            } catch (Exception e) {
                LogsUtil.error(TAG, "查询设备列表异常：%s", e.getMessage());
                break;
            }

            page++;

            if (use_mq) {
                //region 利用MOTT下发命令
                JSONObject json = new JSONObject();
                json.put("device_list", list);
                /*
                 *  关于内部程序通信的主题定义：
                 *  订阅（平台-->推送-->中转站）：{应用通道}/{设备编号}/command/业务逻辑函数名
                 *  推送（中转站-->推送-->平台）：{平台代码}/{应用通道}/{设备编号}/业务逻辑函数名
                 */
                XMQTT3AsyncClient.getInstance().publish("GeneralDevice/4GEM/command/meter_batch_read", json, 1);
            } else {
                // 单机直接执行抄表
                readTaskJobWithList(list);
            }
        }
    }

    /**
     * 执行抄表任务具体请求
     *
     * @param meterNoList 电表设备列表
     */
    public ISyncResult readTaskJobWithList(List<Map<String, Object>> meterNoList) {
        if (meterNoList == null || meterNoList.isEmpty()) return new SyncResult(1, "操作失败");

        List<String> list = meterNoList.stream()
                .map(m -> (String) m.get("serialNumber"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return readTaskJob(list);
    }

    /**
     * 执行抄表任务具体请求
     *
     * @param meterNoList 电表设备列表
     */
    public ISyncResult readTaskJob(List<String> meterNoList) {
        if (meterNoList == null || meterNoList.isEmpty()) return new SyncResult(1, "操作失败");

        String notifyUrl = SysGlobalConfigEntity.getString("TQMeter.Read.NotifyUrl");
        boolean isSuccess = false;
        int retry = 0;

        while (retry <= MAX_RETRY_TIMES) {
            try {
                ISyncResult r = TQDianBiaoSDK.getInstance().meterRead(meterNoList, notifyUrl);
                isSuccess = r.isSuccess();
                if (r.isSuccess()) {
                    LogsUtil.info(TAG, "抄表任务发送成功（尝试%d次），设备数量：%d", retry + 1, meterNoList.size());
                    break;
                }
            } catch (Exception e) {
                LogsUtil.error(TAG, "抄表发送异常（第%d次）：%s", retry + 1, e.getMessage());
            }

            retry++;
            if (retry <= MAX_RETRY_TIMES) LogsUtil.warn(TAG, "抄表发送失败，准备重试（第%d次）...", retry);
        }

        if (!isSuccess) LogsUtil.error(TAG, "抄表任务最终失败，已重试%d次", MAX_RETRY_TIMES);
        return new SyncResult(0, "");
    }

    /**
     * 电表抄表回调处理接口
     * <p>
     * 此接口由第三方平台在抄表操作完成后异步回调，提供抄表结果数据。
     * 回调结构应包含：response_content、timestamp、sign 字段。
     *
     * @return 平台识别的处理状态，"SUCCESS" 或 "FAIL"
     */
    public String readCallback() {
        // 1. 获取回调参数
        String responseContent = HttpRequestUtil.getString("response_content");
        long timestamp = HttpRequestUtil.getLong("timestamp");
        String sign = HttpRequestUtil.getString("sign");

        LogsUtil.info(TAG, "4G电表抄表回调：%s", responseContent);

        // 2. 初步校验：确保数据格式及状态正常
        String status = JsonUtil.getString(responseContent, "$[0].status");
        if (!"SUCCESS".equalsIgnoreCase(status)) return "FAIL";


        // 3. 验证签名
        ISyncResult r = TQDianBiaoSDK.getInstance().verifyCallbackSignature(new LinkedHashMap<>() {{
            put("response_content", responseContent);
            put("timestamp", timestamp);
            put("sign", sign);
        }});
        if (!r.isSuccess()) {
            LogsUtil.warn(TAG, "电表抄表回调签名校验失败: sign=%s", sign);
            return "FAIL";
        }

        // 4. 提取回调数据
        String oprId = JsonUtil.getString(responseContent, "$[0].opr_id");
        String resolveTimeStr = JsonUtil.getString(responseContent, "$[0].resolve_time");
        int type = JsonUtil.getInt(responseContent, "$[0].data[0].type");

        /*
         * - 3   ：正向有功总电能
         * - 27  ：A相电流
         * - 30  ：A相电压
         * - 33  ：瞬时有功功率
         */
        if (type != 3) return "SUCCESS";

        BigDecimal totalActiveEnergy = JsonUtil.getBigDecimal(responseContent, "$[0].data[0].value[0]");
        String remark = JsonUtil.getString(responseContent, "$[0].data[0].dsp");

        // 5. 还原上下文
        Map<String, Object> context = TQDianBiaoSDK.getOperationContext(oprId);
        if (context == null) {
            LogsUtil.error(TAG, "无法通过 opr_id [%s] 找到上下文信息，请检查是否跨Redis实例或缓存已过期", oprId);
            return "FAIL";
        }

        String meterNo = MapUtil.getString(context, "cid");
        long resolveTimestamp = TimeUtil.toTimestamp(resolveTimeStr, TimeUtil.getTimestamp());

        // 6. 写入抄表记录
        ISyncResult result = SmartMeterRecordService.getInstance().add(meterNo,
                resolveTimestamp,
                totalActiveEnergy,
                remark,
                null);

        if (result.isSuccess()) return "SUCCESS";
        return "FAIL";
    }

    // endregion

    // region remark - 电表闸门操作

    /**
     *
     * @param serialNumber
     * @param status
     * @param keep_alive
     * @param appChannelCode
     */
    public void sendMQTTGateSwitch(String serialNumber, int status, int keep_alive, String appChannelCode) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("serialNumber", serialNumber);
        requestBody.put("status", status); // 0=断电，1=通电
        requestBody.put("keep_alive", keep_alive); // 0=解除保电，1=保电

        // 主题格式：{appChannelCode}/{deviceCode}/command/gateOp
        XMQTT3AsyncClient.getInstance().publish(
                String.format("%s/%s/command/gate_switch", appChannelCode, serialNumber)
                , requestBody
                , 1
        );
    }

    /**
     * 电表闸门操作 - 一般用于MQTT处理
     *
     * @param serialNumber 序列号
     * @param mqttMessage  mqtt消息 {"serialNumber":"862538063484766","status":1,"keep_alive":1}
     * @return
     */
    public void gateSwitch(String serialNumber, JSONObject mqttMessage) {
        String notify_url = SysGlobalConfigEntity.getString("TQMeter.GateSwitch.NotifyUrl");
        int status = JsonUtil.getInt(mqttMessage, "status", 0);
        int keep_alive = JsonUtil.getInt(mqttMessage, "keep_alive", -1); // 是否保电

        String commandText = status == 0 ? "下发[断电]命令" : "下发[通电]命令";

        ISyncResult r = TQDianBiaoSDK.getInstance().meterGateSwitch(serialNumber, status == 1, notify_url);
        if (!r.isSuccess()) {
            NotifyService.getInstance().asyncPush(serialNumber
                    , "WRM.ALARM"
                    , ENotifyType.WECHATCORPBOT
                    , new JSONObject() {{
                        put("title", String.format("%s %s失败", serialNumber, commandText));
                        put("content", String.format("消息：%s", r.getMsg()));
                    }}
                    , GeneralDeviceService.iNotifyServiceTransDataBuilder
            );
            LogsUtil.warn(TAG, "[%s] - 下发通/断点指令发生错误: %s - %s", serialNumber, r.getMsg(), mqttMessage.toJSONString());
            return;
        }

        JSONArray response_content = (JSONArray) r.getData();
        if (response_content != null) {
            for (int i = 0; i < response_content.size(); i++) {
                JSONObject resJsonObj = response_content.getJSONObject(i);
                String opr_id = JsonUtil.getString(resJsonObj, "opr_id");
                resJsonObj.put("serialNumber", serialNumber);
                resJsonObj.put("status", status);
                DataService.getMainCache().setJSONObject(String.format("TQ4GMeter:GateSwitch:%s", opr_id), resJsonObj);
            }
        }

        if (keep_alive != -1) keepAlive(serialNumber, mqttMessage);
    }

    /**
     * 电表闸门操作 - 一般用于MQTT处理
     *
     * @param serialNumber 序列号
     * @param mqttMessage  mqtt消息 {"serialNumber":"862538063484766","status":1,"keep_alive":1}
     * @return
     */
    public void keepAlive(String serialNumber, JSONObject mqttMessage) {
        String notify_url = SysGlobalConfigEntity.getString("TQMeter.GateSwitch.NotifyUrl");
        int keep_alive = JsonUtil.getInt(mqttMessage, "keep_alive", -1); // 是否保电
        if (keep_alive == -1) return;

        String commandText = keep_alive == 0 ? "下发[解除保电]命令" : "下发[保电]命令";

        ISyncResult r = TQDianBiaoSDK.getInstance().meterKeepAlive(serialNumber, keep_alive == 1, notify_url);
        if (!r.isSuccess()) {
            NotifyService.getInstance().asyncPush(serialNumber
                    , "WRM.ALARM"
                    , ENotifyType.WECHATCORPBOT
                    , new JSONObject() {{
                        put("title", String.format("%s %s失败", serialNumber, commandText));
                        put("content", String.format("消息：%s", r.getMsg()));
                    }}
                    , GeneralDeviceService.iNotifyServiceTransDataBuilder
            );
            LogsUtil.warn(TAG, "[%s] - 下发保电/解除保指令发生错误: %s - %s", serialNumber, r.getMsg(), mqttMessage.toJSONString());
            return;
        }

        JSONObject response_content = (JSONObject) r.getData();
        if (response_content != null) {
            String opr_id = JsonUtil.getString(response_content, "opr_id");
            response_content.put("serialNumber", serialNumber);
            response_content.put("keep_alive", keep_alive);
            DataService.getMainCache().setJSONObject(String.format("TQ4GMeter:GateSwitch:%s", opr_id), response_content);
        }
    }

    /**
     * 电表通/断电操作回调
     *
     * @return
     */
    public void gateSwitchCallback() {
        // 1. 获取回调参数
        String responseContent = HttpRequestUtil.getString("response_content");
        long timestamp = HttpRequestUtil.getLong("timestamp");
        String sign = HttpRequestUtil.getString("sign");

        LogsUtil.info(TAG, "电表闸门操作回调：%s", responseContent);

        // 2. 初步校验：确保数据格式及状态正常
        String status = JsonUtil.getString(responseContent, "$[0].status");
        if (!"SUCCESS".equalsIgnoreCase(status)) return;

        // 3. 验证签名
        ISyncResult r = TQDianBiaoSDK.getInstance().verifyCallbackSignature(new LinkedHashMap<>() {{
            put("response_content", responseContent);
            put("timestamp", timestamp);
            put("sign", sign);
        }});
        if (!r.isSuccess()) {
            LogsUtil.warn(TAG, "电表闸门回调签名校验失败: sign=%s", sign);
            return;
        }

        // 4. 提取回调数据
        JSONArray array = JSONArray.parseArray(responseContent);
        if (array == null) return;
        for (int i = 0; i < array.size(); i++) {
            JSONObject object = array.getJSONObject(i);
            String oprId = JsonUtil.getString(object, "opr_id");
            String opStatus = JsonUtil.getString(object, "status");

            // 从缓存中获取操作记录
            JSONObject opJson = DataService.getMainCache().getJSONObject(String.format("TQ4GMeter:GateSwitch:%s", oprId));
            if (opJson == null) continue;

            String serialNumber = JsonUtil.getString(opJson, "serialNumber");
            if (StringUtil.isEmpty(serialNumber)) continue;

            int op_status = JsonUtil.getInt(opJson, "status", -1);
            int op_keep_alive = JsonUtil.getInt(opJson, "keep_alive", -1); // 是否保电

            JSONObject notifyTransData = new JSONObject();
            String gateStatusRemark = "";
            if (op_status != -1) {
                notifyTransData.put("action", "分/合闸");
                if (op_status == 1) {
                    gateStatusRemark = "通电" + ("SUCCESS".equalsIgnoreCase(opStatus) ? "成功" : "失败");
                } else gateStatusRemark = "断电" + ("SUCCESS".equalsIgnoreCase(opStatus) ? "成功" : "失败");
            }

            if (op_keep_alive != -1) {
                notifyTransData.put("action", "保电操作");
                if (op_status == 1) {
                    gateStatusRemark = "保电" + ("SUCCESS".equalsIgnoreCase(opStatus) ? "成功" : "失败");
                } else gateStatusRemark = "解除保电" + ("SUCCESS".equalsIgnoreCase(opStatus) ? "成功" : "失败");
            }

            notifyTransData.put("command", gateStatusRemark);
            notifyTransData.put("color", "red");
            NotifyService.getInstance().asyncPush(serialNumber
                    , "SYSTEM.SCBP.RECEIVE"
                    , ENotifyType.WECHATCORPBOT
                    , notifyTransData
                    , GeneralDeviceService.iNotifyServiceTransDataBuilder);
        }
    }

    // endregion

    // region remark - 离线检测任务

    /**
     * 离线检测任务
     */
    public void offlineTaskJob() {
        ISyncResult r = TQDianBiaoSDK.getInstance().getCollector();
        if (!r.isSuccess()) {
            LogsUtil.error(TAG, r.getMsg());
            return;
        }

        GeneralDeviceEntity entity = GeneralDeviceEntity.getInstance();
        List<Map<String, Object>> list = (List<Map<String, Object>>) r.getData();
        for (Map<String, Object> map : list) {
            String collectorid = MapUtil.getString(map, "collectorid"); // 采集器号,4G、NB设备采集器号与表号一致
            int csq = MapUtil.getInt(map, "csq"); // 信号值 1-31 。 20以上算信号稳定
            boolean online = MapUtil.getBool(map, "online"); // 是否在线
            String imei = MapUtil.getString(map, "imei");
            String iccid = MapUtil.getString(map, "iccid");
            String disconnect_time = MapUtil.getString(map, "disconnect_time"); // 上次掉线时间 online = false 时存在
            String connect_time = MapUtil.getString(map, "connect_time"); // 上次上线时间 online = true 时存在
            String description = MapUtil.getString(map, "description"); // 备注


            String csqText = "弱";
            if (csq >= 20) csqText = "稳定";

            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("online_status", online ? 1 : 0); // 状态：0-离线，1-在线 2=休眠 3=升级中
            set_data.put("simCode", iccid);

            JSONObject dynamic_info = new JSONObject();
            dynamic_info.put("备注", description);
            dynamic_info.put("信号", csqText);
            dynamic_info.put("上次上线时间", connect_time);
            dynamic_info.put("上次掉线时间", disconnect_time);
            dynamic_info.put("IMEI", imei);
            dynamic_info.put("csq", csq);

            set_data.put("dynamic_info", dynamic_info.toJSONString());
            entity.where("serialNumber", collectorid).update(set_data);
        }
    }

    // endregion
}
