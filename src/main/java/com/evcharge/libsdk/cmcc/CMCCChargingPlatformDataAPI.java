package com.evcharge.libsdk.cmcc;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 中国移动 - 电动自行车智能充电运营平台数据
 */
public class CMCCChargingPlatformDataAPI {
    public static final String TAG = "ChargePlatformAPI";
    public static final String BASE_URL = "https://integrapower.cmiotcd.com";
    public String appId;
    final String secretKey;


    public CMCCChargingPlatformDataAPI(String appId, String appKey) {
        this.appId = appId;
        this.secretKey = appKey;
    }

    /**
     * 组建头部
     *
     * @param body 请求参数的JSON数据
     * @return 包含签名和必要字段的请求头Map
     */
    private Map<String, Object> build_header(String body) {
        Map<String, Object> headers = new LinkedHashMap<>();

        // 生成长度为10的随机字符串NONCE-STR
        String nonceStr = common.randomStr(10);

        // 生成当前时间戳TIMESTAMP（单位：秒）
        long timestamp = TimeUtil.getTimestamp();

        // 设置请求头字段（键名与文档一致，全大写）
        headers.put("APPID", this.appId);
        headers.put("NONCE-STR", nonceStr);
        headers.put("TIMESTAMP", timestamp);

        // 拼接签名字符串：APPID + SecretKey + NONCE-STR + TIMESTAMP + BODY
        String strToSign = this.appId + this.secretKey + nonceStr + timestamp + body;

        // 使用MD5算法生成签名
        String signature = common.md5(strToSign);

        // 将签名设置到请求头
        headers.put("SIGNATURE", signature);

        return headers;
    }

    /**
     * 同步请求
     *
     * @param path    路径
     * @param headers 额外的头部信息
     * @param body    请求主体
     */
    private static String request(@NotNull final String path
            , final Map<String, Object> headers
            , final String body
    ) {
        String url = String.format("%s%s", BASE_URL, path);
        try {
            //第一步创建OKHttpClient
            OkHttpClient client = new OkHttpClient.Builder().build();

            //第二步创建RequestBody（Form表达或JSON）
            String contentType = "application/json";
            RequestBody requestBody = RequestBody.create(body, MediaType.parse(contentType));

            //第三步创建Request
            Request.Builder request = new Request.Builder();
            request.header("Content-Type", contentType);
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                    request.addHeader(entry.getKey(), entry.getValue().toString());
                }
            }
            request.url(url).method("POST", requestBody);

            //第四步创建call回调对象
            final Call call = client.newCall(request.build());
            Response response = call.execute();
            if (response.body() != null) {
                String response_text = response.body().string();
                if (StringUtil.isEmpty(response_text)) {
                    LogsUtil.warn(TAG, "请求无响应 - %s - %s", url, body);
                    return "";
                }
                return response_text;
            }
        } catch (IOException e) {
            LogsUtil.fatal(e, "HttpUtil", "请求执行错误");
        }
        return "";
    }

    /**
     * 单电站推送
     * 推送单个电站数据。若电站已推送过，则更新电站数据。若电站没有推送过，则新增电站数据。
     *
     * @param openId     站点ID
     * @param name       站点名
     * @param createTime 上线时间
     * @param address    站点地址
     * @param lng        经度
     * @param lat        纬度
     * @return 同步结果
     */
    public SyncResult stations_push(String openId, String name, long createTime, String address, String lng, String lat) {
        try {
            String path = "/station.data/v1/api/stations/push/";

            JSONObject body = new JSONObject();
            body.put("openId", openId);
            body.put("name", name);
            body.put("linkPhone", "18088888899");
            body.put("operatorId", "");
            body.put("operator", "");
            body.put("operatorMobile", "");

            body.put("createTime", createTime);
            body.put("address", address);
            body.put("lng", lng);
            body.put("lat", lat);
            /*
             * 地图类型
             * 1 GPS坐标
             * 2 sogou经纬度
             * 3 baidu经纬度
             * 4 mapbar经纬度
             * 5 腾讯、google、高德坐标
             * 6 sogou墨卡托
             */
            body.put("mapType", 5);

            Map<String, Object> headers = build_header(body.toJSONString());
            String response_text = request(path, headers, body.toJSONString());
            if (JsonUtil.getInt(response_text, "code", 0) != 1) {
                LogsUtil.warn(TAG, "站点推送失败 - %s - %s - %s", path, body.toJSONString(), response_text);
                return new SyncResult(2, "请求失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "站点推送失败");
        }
        return new SyncResult(1, "");
    }

    /**
     * 多电站推送
     * 推送单个电站数据。若电站已推送过，则更新电站数据。若电站没有推送过，则新增电站数据。
     *
     * @return 同步结果
     */
    public SyncResult stations_multi_push(List<Map<String, Object>> dataList) {
        try {
            String path = "/station.data/v1/api/stations/multi-push/";
            /*
             * 地图类型
             * 1 GPS坐标
             * 2 sogou经纬度
             * 3 baidu经纬度
             * 4 mapbar经纬度
             * 5 腾讯、google、高德坐标
             * 6 sogou墨卡托
             */
            for (Map<String, Object> data : dataList) {
                data.put("linkPhone", "18088888899");
                data.put("operatorId", "");
                data.put("operator", "");
                data.put("operatorMobile", "");
                data.put("mapType", 5);
            }

            JSONObject body = new JSONObject();
            body.put("stations", dataList);
            body.put("size", dataList.size());

            Map<String, Object> headers = build_header(body.toJSONString());
            String response_text = request(path, headers, body.toJSONString());
            if (JsonUtil.getInt(response_text, "code", 0) != 1) {
                LogsUtil.warn(TAG, "多电站推送失败 - %s - %s - %s", path, body, response_text);
                return new SyncResult(2, "请求失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "多电站推送失败");
        }
        return new SyncResult(1, "");
    }

    /**
     * 单电站删除
     *
     * @param openId 站点ID
     * @return 同步结果
     */
    public SyncResult stations_remove(String openId) {
        try {
            String path = "/station.data/v1/api/stations/remove/";

            JSONObject body = new JSONObject();
            body.put("openId", openId);

            Map<String, Object> headers = build_header(body.toJSONString());
            String response_text = request(path, headers, body.toJSONString());
            if (JsonUtil.getInt(response_text, "code", 0) != 1) {
                LogsUtil.warn(TAG, "单电站删除失败 - %s - %s - %s", path, body.toJSONString(), response_text);
                return new SyncResult(2, "请求失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "单电站删除失败");
        }
        return new SyncResult(1, "");
    }

    /**
     * 多电站删除
     *
     * @param openIds 站点ID
     * @return 同步结果
     */
    public SyncResult stations_multi_remove(String[] openIds) {
        try {
            String path = "/station.data/v1/api/stations/multi-remove/";
            JSONObject body = new JSONObject();
            body.put("openIds", openIds);

            Map<String, Object> headers = build_header(body.toJSONString());
            String response_text = request(path, headers, body.toJSONString());
            if (JsonUtil.getInt(response_text, "code", 0) != 1) {
                LogsUtil.warn(TAG, "多电站删除失败 - %s - %s - %s", path, body.toJSONString(), response_text);
                return new SyncResult(2, "请求失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "多电站删除失败");
        }
        return new SyncResult(1, "");
    }

    /**
     * 单电站删除
     *
     * @param openId 站点ID
     * @return 同步结果
     */
    public SyncResult stations_check(String openId) {
        try {
            String path = "/station.data/v1/api/stations/contain/";
            JSONObject body = new JSONObject();
            body.put("openId", openId);

            Map<String, Object> headers = build_header(body.toJSONString());
            String response_text = request(path, headers, body.toJSONString());
            if (JsonUtil.getInt(response_text, "code", 0) != 1) {
                LogsUtil.warn(TAG, "单电站删除失败 - %s - %s - %s", path, body.toJSONString(), response_text);
                return new SyncResult(2, "请求失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "单电站删除失败");
        }
        return new SyncResult(1, "");
    }

    /**
     * 单设备推送
     *
     * @param stationOpenId 站点ID
     * @param deviceNum     设备编码
     * @param status        设备在线状态
     * @param port_status   端口状态 MAP : 设备状态为在线时,此字段必填,用于标识设备的插口的状态* 其中,key为插口编号,value为插口状态:* ONLINE:"在线(空闲)",CHARGING: "充电中",BREAKDOWN: "故障"
     * @param deviceType    设备类型: 设备类型包含以下值: CENTRA:"集中式充电桩";DISTRIBUTED:"分布式充电桩";UNIQUE:"独立式充电桩";OTHER:"其他";
     * @param activeDate    激活时间, 激活时间（Unix时间戳，毫秒）
     * @param socketsNum    端口数
     * @param model         设备型号
     * @return 同步结果
     */
    public SyncResult devices_push(String stationOpenId
            , String deviceNum
            , String status
            , Map<String, Object> port_status
            , String deviceType
            , long activeDate
            , int socketsNum
            , String model
    ) {
        try {
            String path = "/station.data/v1/api/devices/push/";

            JSONObject body = new JSONObject();
            body.put("stationOpenId", stationOpenId);
            body.put("operatorId", "");
            body.put("deviceNum", deviceNum);
            body.put("socketStatus", port_status);
            body.put("deviceType", deviceType);
            body.put("status", status);
            body.put("socketsNum", socketsNum);
            body.put("activeDate", activeDate);
            body.put("model", model);

            Map<String, Object> headers = build_header(body.toJSONString());
            String response_text = request(path, headers, body.toJSONString());
            if (JsonUtil.getInt(response_text, "code", 0) != 1) {
                LogsUtil.warn(TAG, "单设备推送 - %s - %s - %s", path, body.toJSONString(), response_text);
                return new SyncResult(2, "请求失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "站点推送失败");
        }
        return new SyncResult(1, "");
    }

    /**
     * 充电订单推送
     *
     * @param orderNum           订单号
     * @param userMobile         充电用户手机号
     * @param deviceNum          充电设备号
     * @param socketNum          充电插口号，标识用户在设备上的哪个插口充电
     * @param chargingEndMessage 充电结束原因：充电结束的具体原因，列如：电量充满，用户停止之类
     * @param orderStatus        订单状态：已完成，未完成
     * @param chargingStartTime  充电开始时间（Unix时间戳，毫秒）
     * @param chargingEndTime    充电结束时间（Unix时间戳，毫秒）
     * @param chargingTime       充电时长：单位（分钟）
     * @param stationName        电站的名称
     * @param stationOpenId      电站ID
     * @param electricQuantity   充电电量
     * @return 同步结果
     */
    public SyncResult order_push(String orderNum
            , String userMobile
            , String deviceNum
            , String socketNum
            , String chargingEndMessage
            , String orderStatus
            , long chargingStartTime
            , long chargingEndTime
            , long chargingTime
            , String stationName
            , String stationOpenId
            , double electricQuantity
    ) {
        try {
            String path = "/interests.data/v1/api/consumeBill/pushConsumeBill/";

            JSONObject body = new JSONObject();
            body.put("orderNum", orderNum);
            body.put("operatorId", "");
            body.put("operatorMobile", "");
            body.put("userMobile", userMobile);
            body.put("deviceNum", deviceNum);
            body.put("socketNum", socketNum);
            body.put("chargingEndMessage", chargingEndMessage);
            body.put("orderStatus", orderStatus);
            body.put("chargingStartTime", chargingStartTime);
            body.put("chargingEndTime", chargingEndTime);
            body.put("chargingTime", chargingTime);
            body.put("stationName", stationName);
            body.put("stationOpenId", stationOpenId);
            body.put("electricQuantity", electricQuantity);

            Map<String, Object> headers = build_header(body.toJSONString());
            String response_text = request(path, headers, body.toJSONString());
            if (JsonUtil.getInt(response_text, "code", 0) != 1) {
                LogsUtil.warn(TAG, "单设备推送 - %s - %s - %s", path, body.toJSONString(), response_text);
                return new SyncResult(2, "请求失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "站点推送失败");
        }
        return new SyncResult(1, "");
    }

    public SyncResult alarm_push(String orderNum
            , String userMobile
            , String deviceNum
            , String socketNum
            , long alarmTime
            , String alarmType
            , String alarmDescribe
            , String stationName
            , String stationOpenId
    ) {
        try {
            String path = "/station.data/v1/api/alarm/push/\n";

            JSONObject body = new JSONObject();
            body.put("orderNum", orderNum);
            body.put("userMobile", userMobile);
            body.put("deviceNum", deviceNum);
            body.put("socketNum", socketNum);
            body.put("alarmTime", alarmTime);
            body.put("alarmType", alarmType);
            body.put("alarmDescribe", alarmDescribe);
            body.put("stationName", stationName);
            body.put("stationOpenId", stationOpenId);
            body.put("operatorId", "");
            body.put("operatorMobile", "");

            Map<String, Object> headers = build_header(body.toJSONString());
            String response_text = request(path, headers, body.toJSONString());
            if (JsonUtil.getInt(response_text, "code", 0) != 1) {
                LogsUtil.warn(TAG, "单设备推送 - %s - %s - %s", path, body.toJSONString(), response_text);
                return new SyncResult(2, "请求失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "站点推送失败");
        }
        return new SyncResult(1, "");
    }
}
