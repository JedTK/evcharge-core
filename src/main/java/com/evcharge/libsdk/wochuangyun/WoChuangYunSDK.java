package com.evcharge.libsdk.wochuangyun;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class WoChuangYunSDK {

    public String API_Key;

    public static WoChuangYunSDK getInstance() {
        return new WoChuangYunSDK();
    }

    public WoChuangYunSDK() {
        API_Key = ConfigManager.getString("WoChuangYunSDK.API.Key");
    }

    /**
     * @return
     */
    public SyncResult call(String callerNbr, String calleeNbr, String user_ip) {
        long timestamp = TimeUtil.getTimestamp() / 1000;
        String Authorization = createAuthorization(timestamp);

        if (!StringUtils.hasLength(user_ip)) user_ip = HttpRequestUtil.getIP();

        JSONObject json = new JSONObject();
        json.put("api_key", API_Key);
        json.put("timetemp", timestamp);
        json.put("callerNbr", callerNbr);
        json.put("calleeNbr", calleeNbr);
        json.put("user_ip", user_ip);

        String params = json.toJSONString();
        String text = HttpUtil.sendPost("https://ykhapi.gzzyrj.com/api/open/call/dial", params, new LinkedHashMap<>() {{
            put("Authorization", Authorization);
        }}, "application/json; charset=UTF-8");
        if (!StringUtils.hasLength(text)) return new SyncResult(1, "请求失败");

        //{"code":1,"msg":"请求成功!","data":{"call_id":"d70b3df9-96b6-0fe7-69b3-7878e4548d34","bindNbr":"09717301946","callerNbr":"15015846068","calleeNbr":"15812401589"}}
        JSONObject jsonObject = JSONObject.parseObject(text);
//        int code = JsonUtil.getInt(jsonObject, "code", 0);
        int code = jsonObject.getIntValue("code");
//        String msg = JsonUtil.getString(jsonObject, "msg");
        String msg = JsonUtil.getString(jsonObject, "msg");
        if (code != 1) return new SyncResult(10 + code, msg);

        Map<Object, Object> data = JsonUtil.getMap(jsonObject, "data");
        return new SyncResult(0, msg, data);
    }

    /**
     * 创建授权码
     *
     * @param timestamp 秒级时间戳
     * @return
     */
    public String createAuthorization(long timestamp) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:");
        Date date = new Date(timestamp * 1000);

        String date_time = simpleDateFormat.format(date);
        String sha1 = common.sha1(date_time);
        String baseString = String.format("%s%s%s", API_Key, sha1, timestamp);
        return common.md5(baseString);
    }
}
