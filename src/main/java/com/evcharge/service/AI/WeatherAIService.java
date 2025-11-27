package com.evcharge.service.AI;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.ai.AIAssistantEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.sys.SysStreetEntity;
import com.evcharge.libsdk.openai.IOpenAI;
import com.evcharge.libsdk.qweather.QweatherAPI;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherAIService {
    private final static String TAG = "AI天气助手";

    private volatile static WeatherAIService instance;

    public static WeatherAIService getInstance() {
        if (instance == null) {
            synchronized (WeatherAIService.class) {
                if (instance == null) instance = new WeatherAIService();
            }
        }
        return instance;
    }

    private volatile static QweatherAPI qweatherAPI;

    /**
     * 获取和风天气API
     */
    private QweatherAPI getQweatherAPI() {
        if (qweatherAPI == null) {
            synchronized (WeatherAIService.class) {
                if (qweatherAPI == null) {
                    String apiHost = SysGlobalConfigEntity.getString("Qweather:apiHost");
                    String publicID = SysGlobalConfigEntity.getString("Qweather:publicID");
                    String privateKey = SysGlobalConfigEntity.getString("Qweather:privateKey");

                    qweatherAPI = new QweatherAPI().setPublicID(publicID).setPrivateKey(privateKey).setApiHost(apiHost);

                }
            }
        }
        return qweatherAPI;
    }

    /**
     * 获取预警或天气消息
     *
     * @param streetCode 街道代码
     * @return 预警文本消息
     */
    public String getWarnTextByStreetCode(String streetCode, boolean inCache) {
        String warnText;
        if (inCache) {
            warnText = DataService.getMainCache().getString(String.format("WeatherAI:WarnText:%s", streetCode));
            if (!StringUtil.isEmpty(warnText)) return warnText;
        }
        // 获取街道实体信息
        SysStreetEntity streetEntity = SysStreetEntity.getInstance().getWithCode(streetCode);

        // 获取AI助手的代码（默认为WeatherAI）
        String aiCode = SysGlobalConfigEntity.getString("Weather:AIAssistant:Code", "WeatherAI");

        // 获取AI助手实体
        AIAssistantEntity assistantEntity = AIAssistantService.getInstance().getAIAssistant(aiCode);
        if (assistantEntity == null) {
            LogsUtil.warn(TAG, "[%s] - AI助手不存在或已关闭", aiCode);
            return "";
        }

        // 获取OpenAI配置
        IOpenAI openAI = AIAssistantService.getInstance().getOpenAI(aiCode);
        if (openAI == null) {
            LogsUtil.warn(TAG, "[%s] - OpenAI 配置不正确", aiCode);
            return "";
        }

        // 创建消息内容
        JSONArray message = new JSONArray();
        // 添加AI助手角色内容
        message.add(new JSONObject() {{
            put("role", assistantEntity.role_name);   // AI助手角色名
            put("content", assistantEntity.role_content);   // AI助手角色内容
        }});

        // 创建用户角色的消息内容
        JSONObject userRoleContent = new JSONObject();
        userRoleContent.put("role", "user");  // 用户角色

        // 获取和风天气API实例
        QweatherAPI weatherAPI = getQweatherAPI();

        String lon = streetEntity.lng;
        String lat = streetEntity.lat;
        // 存储天气信息
        JSONObject weatherJson;
        // 获取当前天气预警信息
        JSONObject warningJson = weatherAPI.nowWarning(lon, lat);
        JSONArray warningArray = JsonUtil.getJSONArray(warningJson, "warning");
        if (warningJson != null && warningArray != null && !warningArray.isEmpty()) {
            // 如果有预警信息，加入预警内容
            userRoleContent.put("content", warningArray.toJSONString());
        } else {
            // 如果没有预警信息，则获取当前天气
            weatherJson = weatherAPI.hourWeather(lon, lat, "m", 24);
            userRoleContent.put("content", JsonUtil.getJSONArray(weatherJson, "hourly").toJSONString());  // 当前天气信息
        }

        // 将用户角色内容添加到消息中
        message.add(userRoleContent);

        // 设置参数
        JSONObject params = new JSONObject();
        params.put("model", assistantEntity.model_name);  // 使用AI助手的模型名称

        // 调用OpenAI接口获取生成的预警消息
        String responseText = openAI.completions(message, params);
        if (StringUtil.isEmpty(responseText)) return "";

        warnText = JsonUtil.getString(responseText, "$.choices[0].message.content");
        warnText = warnText.trim().replaceAll("\n", "").replaceAll("\r", "");
        DataService.getMainCache().set(String.format("WeatherAI:WarnText:%s", streetCode), warnText, ECacheTime.HOUR);
        return warnText;
    }


    /**
     * 根据经纬度获取天气情况
     *
     * @param lon     String
     * @param lat     String
     * @param inCache boolean
     * @return SyncResult
     */
    public SyncResult getWarnTextByLonLat(String lon, String lat, boolean inCache) {
        String cacheKey = String.format("Dashboard:V2:Weather:WarnText:%s,%s", lon, lat);
        JSONObject cache = DataService.getMainCache().getJSONObject(cacheKey);
        if (cache != null) {
            return new SyncResult(0, "success", cache);
        }
        String warnText;
        // 创建用户角色的消息内容
        JSONObject userRoleContent = new JSONObject();
        userRoleContent.put("role", "user");  // 用户角色
        // 获取OpenAI配置
        String aiCode = SysGlobalConfigEntity.getString("Weather:AIAssistant:Code", "WeatherAIV2");

        if (inCache) {
            warnText = DataService.getMainCache().getString(String.format("WeatherAI:WarnText:%s,%s", lon, lat));
            if (!StringUtil.isEmpty(warnText)) return new SyncResult(0, "success", warnText);
        }
        // 获取和风天气API实例
        QweatherAPI weatherAPI = getQweatherAPI();
        // 获取AI助手实体
        AIAssistantEntity assistantEntity = AIAssistantService.getInstance().getAIAssistant(aiCode);
        if (assistantEntity == null) {
            LogsUtil.warn(TAG, "[%s] - AI助手不存在或已关闭", aiCode);
            return new SyncResult(1, "");
        }

        // 获取OpenAI配置
        IOpenAI openAI = AIAssistantService.getInstance().getOpenAI(aiCode);
        if (openAI == null) {
            LogsUtil.warn(TAG, "[%s] - OpenAI 配置不正确", aiCode);
            return new SyncResult(1, "配置不正确");
        }
        // 创建消息内容
        JSONArray message = new JSONArray();
        // 添加AI助手角色内容
        message.add(new JSONObject() {{
            put("role", assistantEntity.role_name);   // AI助手角色名
            put("content", assistantEntity.role_content);   // AI助手角色内容
        }});
        JSONObject params = new JSONObject();
        params.put("model", assistantEntity.model_name);  // 使用AI助手的模型名称

        // 存储天气信息
        JSONObject weatherJson;
        // 获取当前天气预警信息
        JSONObject warningJson = weatherAPI.nowWarning(lon, lat);
        JSONArray warningArray = JsonUtil.getJSONArray(warningJson, "warning");
        if (warningJson != null && warningArray != null && !warningArray.isEmpty()) {
            // 如果有预警信息，加入预警内容
            userRoleContent.put("content", warningArray.toJSONString());
        } else {
            // 如果没有预警信息，则获取当前天气
            weatherJson = weatherAPI.hourWeather(lon, lat, "m", 24);
            userRoleContent.put("content", JsonUtil.getJSONArray(weatherJson, "hourly").toJSONString());  // 当前天气信息
        }

        message.add(userRoleContent);
        // 调用OpenAI接口获取生成的预警消息
        String responseText = openAI.completions(message, params);
        if (StringUtil.isEmpty(responseText)) return new SyncResult(1, "");
        System.out.println("responseText=" + responseText);
        warnText = JsonUtil.getString(responseText, "$.choices[0].message.content");
        warnText = warnText.trim().replaceAll("\n", "").replaceAll("\r", "");
//        DataService.getMainCache().set(String.format("WeatherAI:WarnText:%s,%s", lon, lat), warnText, ECacheTime.HOUR);
        JSONObject jsonObject = extractJsonFromText(warnText);
        if (jsonObject == null) return new SyncResult(1, "获取天气信息失败");
        DataService.getMainCache().setJSONObject(cacheKey, jsonObject);
        return new SyncResult(0, "success", jsonObject);

    }

    private JSONObject extractJsonFromText(String text) {

        Pattern pattern = Pattern.compile("\\{.*?\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String jsonStr = matcher.group(0);

            // 替换转义字符（可选，如果输入已是未转义的 JSON 可跳过）
            jsonStr = jsonStr.replaceAll("\\\\\"", "\"");

            // 使用 Fastjson2 解析
            JSONObject jsonObject = JSONObject.parseObject(jsonStr);

            System.out.println("Level: " + jsonObject.getString("level"));
            System.out.println("Level Text: " + jsonObject.getString("level_text"));
            System.out.println("Warning Text: " + jsonObject.getString("warning_text"));

            return jsonObject;
        } else {
            System.out.println("未找到 JSON 内容");
            return null;
        }


    }
}
