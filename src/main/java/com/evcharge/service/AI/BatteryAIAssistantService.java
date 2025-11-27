package com.evcharge.service.AI;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.ai.AIAssistantEntity;
import com.evcharge.entity.ai.OpenAIConfigEntity;
import com.evcharge.entity.ebike.UserEBikeBatteryReportEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.libsdk.openai.IOpenAI;
import com.evcharge.libsdk.openai.OpenAI;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电池健康AI助手服务
 */
public class BatteryAIAssistantService {
    private final static String TAG = "电池健康AI助手服务";
    private volatile IOpenAI OPEN_AI = null;

    public static BatteryAIAssistantService getInstance() {
        return new BatteryAIAssistantService();
    }

    /**
     * 获取实例化的AI助手
     */
    public IOpenAI getAIAssistant() {
        if (OPEN_AI == null) {
            synchronized (BatteryAIAssistantService.class) {
                if (OPEN_AI == null) {
                    String AssistantCode = ConfigManager.getString("BatteryAI.Assistant.Code", SysGlobalConfigEntity.getString("BatteryAI.Assistant.Code"));
                    OpenAIConfigEntity configEntity = AIAssistantService.getInstance().getDefaultOpenAIConfigByAssistantCode(AssistantCode);
                    if (configEntity == null) return null;

                    OPEN_AI = new OpenAI(configEntity.base_url, configEntity.api_key)
                            .setConnectTimeout(configEntity.connect_timeout)
                            .setReadTimeout(configEntity.read_timeout)
                            .setWriteTimeout(configEntity.write_timeout);
                    LogsUtil.info(TAG, "初始化[%s]AI助手服务", AssistantCode);
                }
            }
        }
        return this.OPEN_AI;
    }

    private final static int wait_ms = 10000;

    /**
     * 获取电池健康数据
     *
     * @param brand     品牌
     * @param model     型号
     * @param buyDate   购买日期
     * @param e_bike_id 可选，车辆ID
     */
    public ISyncResult reportByOpenAI(long uid
            , String brand
            , String model
            , String buyDate
            , long e_bike_id) {
        try {
            // 查询充电订单
            List<Map<String, Object>> list = ChargeOrderEntity.getInstance()
                    .field("startTime, endTime, chargeTime, maxPower, powerConsumption")
                    .cache(String.format("User:%s:EBike:%s:ChargeLogs", uid, e_bike_id), ECacheTime.MINUTE)
                    .where("uid", uid)
                    .where("status", 2)
                    .whereIn("stopReasonCode", new long[]{1, 2, 3})
                    .where("chargeTime", ">", 1800)
                    .where("endTime", "<=", TimeUtil.getTimestamp())
                    .select();
            if (list == null || list.isEmpty()) return new SyncResult(2, "暂无充电记录，无法生成报告");

            // 查询是否已经存在记录了
            Map<String, Object> data = UserEBikeBatteryReportEntity.getInstance()
                    .field("content,status,create_time")
                    .where("uid", uid)
//                    .where("e_bike_id", e_bike_id)
                    .where("create_time", ">=", TimeUtil.getTime00(-30))
                    .order("create_time DESC")
                    .find();
            if (data != null && !data.isEmpty()) {
                int status = MapUtil.getInt(data, "status");
                String content = MapUtil.getString(data, "content");

                if (status == 1) {
                    Map<String, Object> cb_data = new LinkedHashMap<>();
                    cb_data.put("wait_ms", wait_ms);
                    return new SyncResult(10, "", cb_data);
                }

                if (status == 2 && !StringUtil.isEmpty(content)) {
                    Map<String, Object> cb_data = new LinkedHashMap<>();
                    cb_data.put("content", MapUtil.getString(data, "content"));
                    cb_data.put("report_time", TimeUtil.getTimestamp());
                    return new SyncResult(0, "", cb_data);
                }
            }

            // 进行多线程请求，主要因为API接口返回需要1~2分钟，客户端可能不会等待这么久断了线程后就无法更新结果
            ThreadPoolManager.getInstance().execute(() -> {
                String AssistantCode = ConfigManager.getString("BatteryAI.Assistant.Code", SysGlobalConfigEntity.getString("BatteryAI.Assistant.Code"));
                AIAssistantEntity assistantEntity = AIAssistantService.getInstance().getAIAssistant(AssistantCode);
                if (assistantEntity == null) {
                    LogsUtil.warn(TAG, "[%s] - AI助手不存在或已关闭", AssistantCode);
//                    return new SyncResult(1, "服务已关闭");
                    return;
                }

                IOpenAI openAI = getAIAssistant();
                if (openAI == null) {
                    LogsUtil.warn(TAG, "AI助手服务还没初始化");
//                    return new SyncResult(1, "服务暂停使用，请稍后再试");
                    return;
                }

                JSONArray message = new JSONArray();
                message.add(new JSONObject() {{
                    put("role", assistantEntity.role_name);
                    put("content", assistantEntity.role_content);
                }});

                StringBuilder input_text = new StringBuilder();
                input_text.append(String.format("品牌：%s\r\n", brand));
                input_text.append(String.format("型号：%s\r\n", model));
                input_text.append(String.format("购买日期：%s\r\n", buyDate));
                input_text.append(String.format("当前日期：%s\r\n", TimeUtil.toTimeString()));

                long last_charging_date = 0;
                input_text.append("充电记录表头：开始充电时间,结束充电时间,实际充电时间,充电最大功率,充电电量(度)\r\n");
                for (Map<String, Object> map : list) {
                    last_charging_date = MapUtil.getLong(map, "endTime");

                    String startTime = TimeUtil.toTimeString(MapUtil.getLong(map, "startTime"));
                    String endTime = TimeUtil.toTimeString(last_charging_date);

                    input_text.append(String.format("%s,%s,%s秒,%sW,%s度\r\n"
                            , startTime
                            , endTime
                            , MapUtil.getInt(map, "chargeTime")
                            , MapUtil.getBigDecimal(map, "maxPower", 2, RoundingMode.HALF_UP).doubleValue()
                            , MapUtil.getBigDecimal(map, "powerConsumption", 2, RoundingMode.HALF_UP).doubleValue()
                    ));
                }

                message.add(new JSONObject() {{
                    put("content", input_text.toString());
                    put("role", "user");
                }});

                JSONObject params = new JSONObject();
                params.put("model", assistantEntity.model_name);


                // 报告记录到数据库中
                UserEBikeBatteryReportEntity reportEntity = new UserEBikeBatteryReportEntity();
                reportEntity.uid = uid;
                reportEntity.input_text = input_text.toString();
                reportEntity.content = "";
                reportEntity.e_bike_id = e_bike_id;
                reportEntity.last_charging_date = last_charging_date;
                reportEntity.status = 1;
                reportEntity.create_time = TimeUtil.getTimestamp();
                reportEntity.id = reportEntity.insertGetId();
                LogsUtil.info(TAG, "/completions request... %s", message.toJSONString());

                String result_text = openAI.completions(message, params);
                if (StringUtil.isEmpty(result_text)) new SyncResult(1, "生成报告失败");
                String report_content = JsonUtil.getString(result_text, "$.choices[0].message.content");
                //移除特殊字符
                report_content = report_content
                        .replace("\\n\\n```", "")
                        .replace("\\n```", "");

//                LogsUtil.info(TAG, "/completions request... %s", report_content);

                // 更新数据库数据
                Map<String, Object> set_data = new LinkedHashMap<>();
                set_data.put("status", 2);
                set_data.put("content", report_content);
                reportEntity.update(reportEntity.id, set_data);
            });

            Map<String, Object> cb_data = new LinkedHashMap<>();
            cb_data.put("wait_ms", wait_ms);
            return new SyncResult(10, "", cb_data);
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "请求发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 获取电池健康数据
     *
     * @param brand     品牌
     * @param model     型号
     * @param buyDate   购买日期
     * @param e_bike_id 可选，车辆ID
     */
    public ISyncResult reportByOpenAI1(long uid
            , String brand
            , String model
            , String buyDate
            , long e_bike_id) {
        String AssistantCode = ConfigManager.getString("BatteryAI.Assistant.Code", SysGlobalConfigEntity.getString("BatteryAI.Assistant.Code"));
        AIAssistantEntity assistantEntity = AIAssistantService.getInstance().getAIAssistant(AssistantCode);
        if (assistantEntity == null) {
            LogsUtil.warn(TAG, "[%s] - AI助手不存在或已关闭", AssistantCode);
            return new SyncResult(1, "服务已关闭");
        }

        IOpenAI openAI = getAIAssistant();
        if (openAI == null) {
            LogsUtil.warn(TAG, "AI助手服务还没初始化");
            return new SyncResult(1, "服务暂停使用，请稍后再试");
        }

        try {
            // 查询充电订单
            List<Map<String, Object>> list = ChargeOrderEntity.getInstance()
                    .field("startTime, endTime, chargeTime, maxPower, powerConsumption")
                    .cache(String.format("User:%s:EBike:%s:ChargeLogs", uid, e_bike_id), ECacheTime.MINUTE)
                    .where("uid", uid)
                    .where("status", 2)
                    .whereIn("stopReasonCode", new long[]{1, 2, 3})
                    .where("chargeTime", ">", 1800)
                    .where("endTime", "<=", TimeUtil.getTimestamp())
                    .select();
            if (list == null || list.isEmpty()) return new SyncResult(2, "暂无充电记录，无法生成报告");

            JSONArray message = new JSONArray();
            message.add(new JSONObject() {{
                put("role", assistantEntity.role_name);
                put("content", assistantEntity.role_content);
            }});

            StringBuilder input_text = new StringBuilder();
            input_text.append(String.format("品牌：%s\r\n", brand));
            input_text.append(String.format("型号：%s\r\n", model));
            input_text.append(String.format("购买日期：%s\r\n", buyDate));
            input_text.append(String.format("当前日期：%s\r\n", TimeUtil.toTimeString()));

            long last_charging_date = 0;
            input_text.append("充电记录表头：开始充电时间,结束充电时间,实际充电时间,充电最大功率,充电电量(度)\r\n");
            for (Map<String, Object> map : list) {
                last_charging_date = MapUtil.getLong(map, "endTime");

                String startTime = TimeUtil.toTimeString(MapUtil.getLong(map, "startTime"));
                String endTime = TimeUtil.toTimeString(last_charging_date);

                input_text.append(String.format("%s,%s,%s秒,%sW,%s度\r\n"
                        , startTime
                        , endTime
                        , MapUtil.getInt(map, "chargeTime")
                        , MapUtil.getBigDecimal(map, "maxPower", 2, RoundingMode.HALF_UP).doubleValue()
                        , MapUtil.getBigDecimal(map, "powerConsumption", 2, RoundingMode.HALF_UP).doubleValue()
                ));
            }

            message.add(new JSONObject() {{
                put("content", input_text.toString());
                put("role", "user");
            }});

            JSONObject params = new JSONObject();
            params.put("model", assistantEntity.model_name);

//            LogsUtil.info(TAG, "/completions request... %s", message.toJSONString());

            String result_text = openAI.completions(message, params);
            if (StringUtil.isEmpty(result_text)) new SyncResult(1, "生成报告失败");

            String report_content = JsonUtil.getString(result_text, "$.choices[0].message.content");
            //移除特殊字符
            report_content = report_content
                    .replace("\\n\\n```", "")
                    .replace("\\n```", "");

//            LogsUtil.info(TAG, "/completions request... %s", report_content);

            // 报告记录到数据库中
            UserEBikeBatteryReportEntity reportEntity = new UserEBikeBatteryReportEntity();
            reportEntity.uid = uid;
            reportEntity.input_text = input_text.toString();
            reportEntity.content = report_content;
            reportEntity.e_bike_id = e_bike_id;
            reportEntity.last_charging_date = last_charging_date;
            reportEntity.status = 2;
            reportEntity.create_time = TimeUtil.getTimestamp();
            reportEntity.id = reportEntity.insertGetId();

//            Map<String, Object> set_data = new LinkedHashMap<>();
//            set_data.put("status", 2);
//            set_data.put("content", report_content);
//            reportEntity.update(reportEntity.id, set_data);

            Map<String, Object> cb_data = new LinkedHashMap<>();
            cb_data.put("content", report_content);
            cb_data.put("report_time", TimeUtil.getTimestamp());
            return new SyncResult(0, "", cb_data);
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "请求发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 获取最近的查询的报告日期
     *
     * @param uid       用户id
     * @param e_bike_id 用户车辆id
     */
    public ISyncResult lastReportDate(long uid, long e_bike_id) {
        Map<String, Object> data = UserEBikeBatteryReportEntity.getInstance()
                .field("create_time")
                .cache(String.format("User:%s:EBike:%s:LastBatteryReportDate", uid, e_bike_id))
                .where("uid", uid)
                .where("status", 2)
                .order("create_time DESC")
                .find();
        if (data == null || data.isEmpty()) return new SyncResult(1, "");
        return new SyncResult(0, "", data);
    }

    /**
     * 获取最近的查询的报告
     *
     * @param uid       用户id
     * @param e_bike_id 用户车辆id
     * @param day       多少天前
     */
    public ISyncResult lastReport(long uid, long e_bike_id, int day) {
        Map<String, Object> data = UserEBikeBatteryReportEntity.getInstance()
                .field("content,create_time")
                .cache(String.format("User:%s:EBike:%s:BatteryReport:%s", uid, e_bike_id, day), ECacheTime.MINUTE * 10)
                .where("uid", uid)
                .where("e_bike_id", e_bike_id)
                .where("status", 2)
                .where("create_time", ">=", TimeUtil.getTime00(day))
                .order("create_time DESC")
                .find();
        if (data == null || data.isEmpty()) return new SyncResult(1, "");
        data.put("report_time", MapUtil.getLong(data, "create_time"));
        return new SyncResult(0, "", data);
    }
}
