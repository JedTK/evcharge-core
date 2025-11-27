package com.evcharge.libsdk.qweather;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.utils.Http2Util;
import com.xyzs.utils.LogsUtil;
import org.springframework.util.StringUtils;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 和风天气API
 * 控制台：<a href="https://console.qweather.com/#/apps">...</a>
 * 开发文档：<a href="https://dev.qweather.com/docs/">...</a>
 */
public class QweatherAPI {

    //region 属性
    /**
     * 公共ID
     */
    private String publicID;
    /**
     * 私有key
     */
    private String privateKey;
    /**
     * API接口Host
     */
    private String apiHost = "https://api.qweather.com";
    /**
     * 接口版本
     */
    private String version = "v7";
    /**
     * 语言代码
     */
    private String lang = "zh";

    /**
     * 设置公共ID
     */
    public QweatherAPI setPublicID(String publicID) {
        this.publicID = publicID;
        return this;
    }

    /**
     * 设置私有key
     */
    public QweatherAPI setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    /**
     * API接口Host 一般为：<a href="https://api.qweather.com">...</a>
     */
    public QweatherAPI setApiHost(String apiHost) {
        this.apiHost = apiHost;
        return this;
    }

    /**
     * 设置接口版本号
     */
    public QweatherAPI setVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * 设置语言
     */
    public QweatherAPI setLang(String lang) {
        this.lang = lang;
        return this;
    }
    //endregion

    /**
     * 实时天气
     *
     * @param lon 经度
     * @param lat 纬度
     */
    public JSONObject nowWeather(String lon, String lat) {
        return nowWeather(lon, lat, "");
    }

    /**
     * 实时天气
     *
     * @param lon  经度
     * @param lat  纬度
     * @param unit 数据单位设置，可选值包括unit=m（公制单位，默认）和unit=i（英制单位）
     */
    public JSONObject nowWeather(String lon, String lat, String unit) {
        //https://api.qweather.com/v7/weather/now?location=101010100&key=YOUR_KEY
        String location = String.format("%s,%s", lon, lat);
        if (!StringUtils.hasLength(unit)) unit = "m";//数据单位设置，可选值包括unit=m（公制单位，默认）和unit=i（英制单位）

        if (!StringUtils.hasLength(this.privateKey)) {
            LogsUtil.error(this.getClass().getSimpleName(), "正在调用和风天气API，但是缺少KEY，请登入和风天气控制台创建项目获取KEY");
            return null;
        }

        String API = String.format("%s/%s/weather/now?location=%s&key=%s&lang=%s&unit=%s"
                , this.apiHost
                , this.version
                , location
                , this.privateKey
                , this.lang
                , unit);
        String responseText = Http2Util.get(API);
        if (!StringUtils.hasLength(responseText)) {
            LogsUtil.error(this.getClass().getSimpleName(), "获取实时天气API请求发生错误，无任何内容返回");
            return null;
        }
        JSONObject json = JSONObject.parseObject(responseText);
        if (json.getIntValue("code", 0) != 200) {
            LogsUtil.error(this.getClass().getSimpleName(), "获取实时天气API请求发生错误：%s", responseText);
            return null;
        }
        return json;
    }

    /**
     * 未来逐小时天气情况
     *
     * @param lon  经度
     * @param lat  纬度
     * @param unit 数据单位设置，可选值包括unit=m（公制单位，默认）和unit=i（英制单位）
     * @param hour 小时，取值范围：24/72/168
     */
    public JSONObject hourWeather(String lon, String lat, String unit, int hour) {
        String location = String.format("%s,%s", lon, lat);
        if (!StringUtils.hasLength(unit)) unit = "m";//数据单位设置，可选值包括unit=m（公制单位，默认）和unit=i（英制单位）

        if (!StringUtils.hasLength(this.privateKey)) {
            LogsUtil.error(this.getClass().getSimpleName(), "正在调用和风天气API，但是缺少KEY，请登入和风天气控制台创建项目获取KEY");
            return null;
        }

        String timeIntervalStr = Map.of(
                24, "24h",
                72, "72h",
                168, "168h"
        ).getOrDefault(hour, "24h");

        String API = String.format("%s/%s/weather/%s?location=%s&key=%s&lang=%s&unit=%s"
                , this.apiHost
                , this.version
                , timeIntervalStr
                , location
                , this.privateKey
                , this.lang
                , unit);
        String responseText = Http2Util.get(API);
        if (!StringUtils.hasLength(responseText)) {
            LogsUtil.error(this.getClass().getSimpleName(), "获取实时天气API请求发生错误，无任何内容返回");
            return null;
        }
        JSONObject json = JSONObject.parseObject(responseText);
        if (json.getIntValue("code", 0) != 200) {
            LogsUtil.error(this.getClass().getSimpleName(), "获取实时天气API请求发生错误：%s", responseText);
            return null;
        }
        return json;
    }

    /**
     * 每日天气预报
     *
     * @param lon  经度
     * @param lat  纬度
     * @param unit 数据单位设置，可选值包括unit=m（公制单位，默认）和unit=i（英制单位）
     * @param day  小时，取值范围：24/72/168
     */
    public JSONObject dayWeather(String lon, String lat, String unit, int day) {
        String location = String.format("%s,%s", lon, lat);
        if (!StringUtils.hasLength(unit)) unit = "m";//数据单位设置，可选值包括unit=m（公制单位，默认）和unit=i（英制单位）

        if (!StringUtils.hasLength(this.privateKey)) {
            LogsUtil.error(this.getClass().getSimpleName(), "正在调用和风天气API，但是缺少KEY，请登入和风天气控制台创建项目获取KEY");
            return null;
        }

        String timeIntervalStr = Map.of(
                3, "3d",
                7, "7d",
                10, "10d",
                15, "15d",
                30, "30d"
        ).getOrDefault(day, "3d");

        String API = String.format("%s/%s/weather/%s?location=%s&key=%s&lang=%s&unit=%s"
                , this.apiHost
                , this.version
                , timeIntervalStr
                , location
                , this.privateKey
                , this.lang
                , unit);
        String responseText = Http2Util.get(API);
        if (!StringUtils.hasLength(responseText)) {
            LogsUtil.error(this.getClass().getSimpleName(), "获取实时天气API请求发生错误，无任何内容返回");
            return null;
        }
        JSONObject json = JSONObject.parseObject(responseText);
        if (json.getIntValue("code", 0) != 200) {
            LogsUtil.error(this.getClass().getSimpleName(), "获取实时天气API请求发生错误：%s", responseText);
            return null;
        }
        return json;
    }

    /**
     * 天气灾害预警
     *
     * @param lon 经度
     * @param lat 纬度
     */
    public JSONObject nowWarning(String lon, String lat) {
        //https://api.qweather.com/v7/warning/now?location=101010100&key=YOUR_KEY
        String location = String.format("%s,%s", lon, lat);

        if (!StringUtils.hasLength(this.privateKey)) {
            LogsUtil.error(this.getClass().getSimpleName(), "正在调用和风天气API，但是缺少KEY，请登入和风天气控制台创建项目获取KEY");
            return null;
        }

        String API = String.format("%s/%s/warning/now?location=%s&key=%s&lang=%s"
                , this.apiHost
                , this.version
                , location
                , this.privateKey
                , this.lang);
        String responseText = Http2Util.get(API);
        if (!StringUtils.hasLength(responseText)) {
            LogsUtil.error(this.getClass().getSimpleName(), "获取实时天气API请求发生错误，无任何内容返回");
            return null;
        }
        JSONObject json = JSONObject.parseObject(responseText);
        if (json.getIntValue("code", 0) != 200) {
            LogsUtil.error(this.getClass().getSimpleName(), "获取实时天气API请求发生错误：%s", responseText);
            return null;
        }
        return json;
    }

    /**
     * 和风天气签名生成算法-JAVA版本
     *
     * @param params 请求参数集，所有参数必须已转换为字符串类型
     * @param secret 签名密钥（用户的认证key）
     * @return 签名
     */
    public static String getSignature(HashMap<String, String> params, String secret) throws Exception {
        // 先将参数以其参数名的字典序升序进行排序
        Map<String, String> sortedParams = new TreeMap<>(params);
        Set<Map.Entry<String, String>> entrys = sortedParams.entrySet();

        // 遍历排序后的字典，将所有参数按"key=value"格式拼接在一起
        StringBuilder baseString = new StringBuilder();
        for (Map.Entry<String, String> param : entrys) {
            //sign参数 和 空值参数 不加入算法
            if (param.getValue() != null && !"".equals(param.getKey().trim()) && !"sign".equals(param.getKey().trim()) && !"key".equals(param.getKey().trim()) && !"".equals(param.getValue().trim())) {
                baseString.append(param.getKey().trim()).append("=").append(param.getValue().trim()).append("&");
            }
        }
        if (baseString.length() > 0) {
            baseString.deleteCharAt(baseString.length() - 1).append(secret);
        }
        // 使用MD5对待签名串求签
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(baseString.toString().getBytes("UTF-8"));
            return new String(encodeHex(bytes));
        } catch (GeneralSecurityException ex) {
            throw ex;
        }
    }

    public static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        int i = 0;
        char[] toDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        for (int var5 = 0; i < l; ++i) {
            out[var5++] = toDigits[(240 & data[i]) >>> 4];
            out[var5++] = toDigits[15 & data[i]];
        }
        return out;
    }
}
