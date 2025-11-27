package com.evcharge.libsdk.ys7;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Http2Util;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 萤石云开放平台
 * 文档地址：<a href="https://open.ys7.com/help">萤石云开放平台</a>
 */
public class YSSDK {
    private final static String HostAPI = "https://open.ys7.com";

    private String appKey;
    private String appSecret;

    public YSSDK setAppKey(String appKey) {
        this.appKey = appKey;
        return this;
    }

    public YSSDK setAppSecret(String appSecret) {
        this.appSecret = appSecret;
        return this;
    }

    public YSSDK() {
    }

    public YSSDK(String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    /**
     * 获取授权码（优先从缓存中获取）
     *
     * @return 授权码
     */
    public String getAccessToken() {
        return getAccessToken(false);
    }

    /**
     * 获取授权码
     * 文档地址：<a href="https://open.ys7.com/help/81">获取授权码</a>
     *
     * @param refresh 是否刷新，不刷新会自动获取缓存授权码
     * @return 授权码
     */
    public String getAccessToken(boolean refresh) {
        String accessToken = "";
        try {
            if (!refresh) {
                accessToken = DataService.getMainCache().getString("libs:YSSDK:AccessToken", accessToken);
                if (StringUtils.hasLength(accessToken)) return accessToken;
            }

            String API = String.format("%s/api/lapp/token/get", HostAPI);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("appKey", this.appKey);
            params.put("appSecret", this.appSecret);

            String responseText = Http2Util.post(API, params, "application/x-www-form-urlencoded");
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn("YSSDK", "获取AccessToken发生错误，服务器没有返回任何东西");
                return "";
            }
            JSONObject json = JSONObject.parseObject(responseText);
            int code = json.getIntValue("code");
            if (code != 200) {
                String msg = json.getString("msg");
                LogsUtil.warn("YSSDK", "获取AccessToken发生错误,%s", msg);
                return "";
            }
            JSONObject data = json.getJSONObject("data");

            accessToken = data.getString("accessToken");
            long expireTime = data.getLongValue("expireTime");
            long expire = expireTime - TimeUtil.getTimestamp() - ECacheTime.HOUR;
            DataService.getMainCache().set("libs:YSSDK:AccessToken", accessToken, expire);
            return accessToken;
        } catch (Exception e) {
            LogsUtil.error(e, "YSSDK", "获取AccessToken发生错误");
        }
        return "";
    }

    /**
     * 获取播放地址
     *
     * @param deviceSerial 设备序列号
     * @param code         ezopen协议地址的设备的视频加密密码
     * @param protocol     流播放协议，1-ezopen、2-hls、3-rtmp、4-flv，默认为1
     * @param expireTime   过期时长，单位秒；针对hls/rtmp/flv设置有效期，相对时间；30秒-720天
     * @param type         地址的类型，1-预览，2-本地录像回放，3-云存储录像回放，非必选，默认为1；回放仅支持rtmp、ezopen、flv协议
     * @param quality      视频清晰度，1-高清（主码流）、2-流畅（子码流）
     * @param channelNo    通道号，非必选，默认为1
     * @param startTime    本地录像/云存储录像回放开始时间,云存储开始结束时间必须在同一天，示例：2019-12-01 00:00:00
     * @param stopTime     本地录像/云存储录像回放结束时间,云存储开始结束时间必须在同一天，示例：2019-12-01 23:59:59
     * @param gbchannel    国标设备的通道编号，视频通道编号ID
     */
    public SyncResult getPlayUrl(String deviceSerial
            , String code
            , int protocol
            , int expireTime
            , int type
            , int quality
            , int channelNo
            , String startTime
            , String stopTime
            , String gbchannel) {
        try {
            if (protocol < 1 || protocol > 4) protocol = 1;
            if (expireTime < 30) expireTime = 30;
            if (expireTime > 62208000) expireTime = 62208000;
            if (type < 1 || type > 3) type = 1;
            if (quality < 1 || quality > 2) quality = 2;
            if (channelNo < 1) channelNo = 1;

            String accessToken = getAccessToken();
            if (!StringUtils.hasLength(accessToken)) return new SyncResult(2, "无效授权码");

            String API = String.format("%s/api/lapp/token/get", HostAPI);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("accessToken", accessToken);
            //设备序列号例如427734222，均采用英文符号，限制最多50个字符
            params.put("deviceSerial", deviceSerial);
            //通道号，非必选，默认为1
            params.put("channelNo", channelNo);
            //流播放协议，1-ezopen、2-hls、3-rtmp、4-flv，默认为1
            params.put("protocol", protocol);
            //ezopen协议地址的设备的视频加密密码
            if (StringUtils.hasLength(code)) params.put("code", code);
            //过期时长，单位秒；针对hls/rtmp/flv设置有效期，相对时间；30秒-720天
            params.put("expireTime", expireTime);
            //地址的类型，1-预览，2-本地录像回放，3-云存储录像回放，非必选，默认为1；回放仅支持rtmp、ezopen、flv协议
            params.put("type", type);
            //视频清晰度，1-高清（主码流）、2-流畅（子码流）
            params.put("quality", quality);
            //本地录像/云存储录像回放开始时间,云存储开始结束时间必须在同一天，示例：2019-12-01 00:00:00
            if (StringUtils.hasLength(startTime)) params.put("startTime", startTime);
            //本地录像/云存储录像回放结束时间,云存储开始结束时间必须在同一天，示例：2019-12-01 23:59:59
            if (StringUtils.hasLength(stopTime)) params.put("stopTime", stopTime);
            //请判断播放端是否要求播放视频为H265编码格式,1表示需要，0表示不要求
            params.put("supportH265", 0);
            /*
             * 回放倍速。倍速为 -1（ 支持的最大倍速）、0.5、1、2、4、8、16；
             * 仅支持protocol为4-flv
             * 且
             * type为2-本地录像回放（ 部分设备可能不支持16倍速） 或者 3-云存储录像回放
             */
            params.put("playbackSpeed", -1);
            //国标设备的通道编号，视频通道编号ID
            if (StringUtils.hasLength(gbchannel)) params.put("gbchannel", gbchannel);

            String responeText = Http2Util.post(API, params, "application/x-www-form-urlencoded");
            if (!StringUtils.hasLength(responeText)) {
                LogsUtil.warn("YSSDK", "获取播放地址发生错误，服务器没有返回任何东西");
                return new SyncResult(404, "服务器无响应");
            }
            JSONObject json = JSONObject.parseObject(responeText);
            int json_code = json.getIntValue("code");
            if (json_code != 200) {
                String msg = json.getString("msg");
                LogsUtil.warn("YSSDK", "获取播放地址发生错误,%s", msg);
                return new SyncResult(10000 + json_code, "获取失败");
            }
            JSONObject data = json.getJSONObject("data");
            String playUrl = data.getString("url");
            return new SyncResult(0, "", playUrl);
        } catch (Exception e) {
            LogsUtil.error(e, "YSSDK", "获取播放地址发生错误");
        }
        return new SyncResult(1, "获取失败");
    }

    /**
     * 获取设备列表
     *
     * @param pageStart 分页起始页，从0开始
     * @param pageSize  分页大小，默认为10，最大为50
     */
    public SyncResult deviceList(int pageStart, int pageSize) {
        try {
            String accessToken = getAccessToken();
            if (!StringUtils.hasLength(accessToken)) return new SyncResult(2, "无效授权码");

            String API = String.format("%s/api/lapp/device/list", HostAPI);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("accessToken", accessToken);
            params.put("pageStart", pageStart);
            params.put("pageSize", pageSize);

            String responseText = Http2Util.post(API, params, "application/x-www-form-urlencoded");
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn("YSSDK", "获取设备列表 - 无任何数据返回");
                return new SyncResult(404, "服务器无响应");
            }

            JSONObject json = JSONObject.parseObject(responseText);
            int json_code = json.getIntValue("code");
            if (json_code != 200) {
                String msg = json.getString("msg");
                LogsUtil.warn("YSSDK", "获取设备列表 - %s", msg);
                return new SyncResult(10000 + json_code, "获取失败");
            }
            JSONObject page = json.getJSONObject("page");
            JSONArray data = json.getJSONArray("data");

            Map<String, Object> cbdata = new LinkedHashMap<>();
            cbdata.put("page", page);
            cbdata.put("data", data);
            return new SyncResult(0, "", cbdata);
        } catch (Exception e) {
            LogsUtil.error(e, "YSSDK", "获取播放地址发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 获取单个设备信息
     *
     * @param deviceSerial 设备序列号，必填
     */
    public SyncResult deviceInfo(String deviceSerial) {
        try {
            String accessToken = getAccessToken();
            if (!StringUtils.hasLength(accessToken)) return new SyncResult(2, "无效授权码");

            String API = String.format("%s/api/lapp/device/info", HostAPI);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("accessToken", accessToken);
            params.put("deviceSerial", deviceSerial.toUpperCase());

            String responseText = Http2Util.post(API, params, "application/x-www-form-urlencoded");
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn("YSSDK", "获取单个设备信息 - 无任何数据返回");
                return new SyncResult(404, "服务器无响应");
            }
            JSONObject json = JSONObject.parseObject(responseText);
            int json_code = json.getIntValue("code");
            if (json_code != 200) {
                String msg = json.getString("msg");
                LogsUtil.warn("YSSDK", "获取单个设备信息 - %s", msg);
                return new SyncResult(10000 + json_code, "获取失败");
            }
            JSONObject data = json.getJSONObject("data");
            return new SyncResult(0, "", data);
        } catch (Exception e) {
            LogsUtil.error(e, "YSSDK", "获取播放地址发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 修改云端设备名称
     *
     * @param deviceSerial 设备序列号,存在英文字母的设备序列号，字母需为大写
     * @param deviceName   设备名称，长度不大于50字节，不能包含特殊字符
     */
    public SyncResult deviceUpdateName(String deviceSerial, String deviceName) {
        try {
            String accessToken = getAccessToken();
            if (!StringUtils.hasLength(accessToken)) return new SyncResult(2, "无效授权码");
            if (!StringUtils.hasLength(deviceName)) return new SyncResult(2, "无效设备名");

            deviceName = deviceName.replace("\r", "").replace("\n", "");

            String API = String.format("%s/api/lapp/device/name/update", HostAPI);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("accessToken", accessToken);
            params.put("deviceSerial", deviceSerial.toUpperCase());
            params.put("deviceName", deviceName);

            String responseText = Http2Util.post(API, params, "application/x-www-form-urlencoded");
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn("YSSDK", "修改云端设备名称 - 无任何数据返回");
                return new SyncResult(404, "服务器无响应");
            }
            JSONObject json = JSONObject.parseObject(responseText);
            int json_code = json.getIntValue("code");
            if (json_code != 200) {
                String msg = json.getString("msg");
                LogsUtil.warn("YSSDK", "修改云端设备名称 - %s", msg);
                return new SyncResult(10000 + json_code, "获取失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "YSSDK", "获取播放地址发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 修改云端设备名称
     *
     * @param deviceSerial 设备序列号,存在英文字母的设备序列号，字母需为大写
     * @param name         通道名称，长度不大于50字节，不能包含特殊字符
     * @param channelNo    非必选参数，不为空表示修改指定通道名称，为空表示修改通道1名称
     */
    public SyncResult deviceUpdateCameraName(String deviceSerial, String name, int channelNo) {
        try {
            String accessToken = getAccessToken();
            if (!StringUtils.hasLength(accessToken)) return new SyncResult(2, "无效授权码");
            if (!StringUtils.hasLength(name)) return new SyncResult(2, "无效设备名");

            name = name.replace("\r", "").replace("\n", "");

            String API = String.format("%s/api/lapp/camera/name/update", HostAPI);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("accessToken", accessToken);
            params.put("deviceSerial", deviceSerial.toUpperCase());
            params.put("name", name);
            params.put("channelNo", channelNo);

            String responseText = Http2Util.post(API, params, "application/x-www-form-urlencoded");
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn("YSSDK", "修改云端设备名称 - 无任何数据返回");
                return new SyncResult(404, "服务器无响应");
            }
            JSONObject json = JSONObject.parseObject(responseText);
            int json_code = json.getIntValue("code");
            if (json_code != 200) {
                String msg = json.getString("msg");
                LogsUtil.warn("YSSDK", "修改云端设备名称 - %s", msg);
                return new SyncResult(10000 + json_code, "获取失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "YSSDK", "获取播放地址发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 设备存储介质状态查询
     *
     * @param deviceSerial 设备序列号
     */
    public SyncResult deviceFormatStatus(String deviceSerial) {
        try {
            String accessToken = getAccessToken();
            if (!StringUtils.hasLength(accessToken)) return new SyncResult(2, "无效授权码");

            String API = String.format("%s/api/v3/device/format/status", HostAPI);
            String responseText = Http2Util.get(API, new LinkedHashMap<>() {{
                put("deviceSerial", deviceSerial.toUpperCase());
            }}, new LinkedHashMap<>() {{
                put("accessToken", accessToken);
            }}, "application/x-www-form-urlencoded");

            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn("YSSDK", "设备存储介质状态查询 - 无任何数据返回");
                return new SyncResult(404, "服务器无响应");
            }

            // {"meta":{"code":200,"message":"操作成功","moreInfo":null},"data":{"storageStatus":[{"index":"1","name":"本地存储","status":"0","formattingRate":"100"}]}}
            int code = JsonUtil.getInt(responseText, "$.meta.code");
            if (code != 200) {
                String msg = JsonUtil.getString(responseText, "$.meta.msg");
                LogsUtil.warn("YSSDK", "设备存储介质状态查询错误 - %s", responseText);
                return new SyncResult(10000 + code, "设备存储介质状态查询 - %s", msg);
            }
            /*
             * data	object	响应数据
             * -storageStatus	array<object>	存储介质信息列表
             * --index	string	存储介质编号
             * --name	string	存储介质名称
             * --status	string	存储介质状态，0正常,1存储介质错,2未格式化,3正在格式化
             * --formattingRate	string	格式化进度
             */
            JSONArray list = JsonUtil.getJSONArray(responseText, "$.data.storageStatus");
            return new SyncResult(0, "", list);
        } catch (Exception e) {
            LogsUtil.error(e, "YSSDK", "获取播放地址发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 设备存储介质 格式化
     *
     * @param deviceSerial 设备序列号
     */
    public SyncResult deviceFormatDisk(String deviceSerial, String diskIndex) {
        try {
            String accessToken = getAccessToken();
            if (!StringUtils.hasLength(accessToken)) return new SyncResult(2, "无效授权码");

            String API = String.format("%s/api/v3/device/format/disk", HostAPI);
            String responseText = Http2Util.get(API, new LinkedHashMap<>() {{
                put("deviceSerial", deviceSerial.toUpperCase());
                put("diskIndex", diskIndex);
            }}, new LinkedHashMap<>() {{
                put("accessToken", accessToken);
            }}, "application/x-www-form-urlencoded");

            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn("YSSDK", "设备存储介质格式化 - 无任何数据返回");
                return new SyncResult(404, "服务器无响应");
            }
            // {"meta":{"code":0,"message":"string","moreInfo":{}}}
            int code = JsonUtil.getInt(responseText, "$.meta.code");
            if (code != 200) {
                String msg = JsonUtil.getString(responseText, "$.meta.msg");
                LogsUtil.warn("YSSDK", "设备存储介质格式化 - %s", responseText);
                return new SyncResult(10000 + code, "设备存储介质格式化 - %s", msg);
            }
            JSONObject moreInfo = JsonUtil.getJSONObject(responseText, "$.meta.moreInfo");
            return new SyncResult(0, "", moreInfo);
        } catch (Exception e) {
            LogsUtil.error(e, "YSSDK", "获取播放地址发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 获取设备SIM卡号
     *
     * @param deviceSerial 序列号
     * @return simCode卡号
     */
    public String getDeviceSimCode(String deviceSerial) {
        try {
            String accessToken = getAccessToken();
            if (!StringUtils.hasLength(accessToken)) return "";

            String API = String.format("%s/api/service/devicekit/simcard/device/nbCardId", HostAPI);
            String responseText = Http2Util.get(API, null, new LinkedHashMap<>() {{
                put("accessToken", accessToken);
                put("deviceSerial", deviceSerial.toUpperCase());
            }}, "application/x-www-form-urlencoded");

            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn("YSSDK", "获取设备SIM卡号 - 无任何数据返回");
                return "";
            }
            // {"meta":{"code":200,"message":"操作成功"},"data":{"nbCardId":"898604F3102380229449"}}
            int code = JsonUtil.getInt(responseText, "$.meta.code");
            if (code != 200) {
                LogsUtil.warn("YSSDK", "获取设备SIM卡号 - %s", responseText);
                return "";
            }
            return JsonUtil.getString(responseText, "$.data.nbCardId");
        } catch (Exception e) {
            LogsUtil.error(e, "YSSDK", "获取播放地址发生错误");
        }
        return "";
    }
}
