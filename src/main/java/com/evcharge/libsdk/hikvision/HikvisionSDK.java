package com.evcharge.libsdk.hikvision;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import com.xyzs.utils.LogsUtil;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 海康威视 - 消防云平台
 * 文档地址：https://open.hikvision.com/docs/docId?productId=5e942547d916e614c01b72fd&version=%2F11d4118443174c34b53bd541cf77c5b6&tagPath=%E5%AF%B9%E6%8E%A5%E6%8C%87%E5%8D%97
 */
public class HikvisionSDK {

    public HikvisionSDK(String appKey, String appSecret, String host) {
        ArtemisConfig.host = host;
        ArtemisConfig.appKey = appKey;
        ArtemisConfig.appSecret = appSecret;
    }

    private static HikvisionSDK _this;

    /**
     * 获得一个公共实例
     */
    private static HikvisionSDK getInstance(String appKey, String appSecret, String host) {
        if (_this == null) _this = new HikvisionSDK(appKey, appSecret, host);
        return _this;
    }

    public String getCompanys() {
        final String ARTEMIS_PATH = "/artemis";
        final String url = ARTEMIS_PATH + "/api/firepro/v1/getCompany";
        Map<String, String> path = new HashMap() {{
            put("https://", url);//https协议
        }};

        /**
         * STEP5：组装请求参数
         */
        Map<String, String> paramMap = new HashMap();
        paramMap.put("pageNo", "1");
        paramMap.put("companyID", "");
        paramMap.put("pageSize", "20");
        /**
         * STEP6：调用接口
         */
        String result = ArtemisHttpUtil.doPostStringArtemis(path, JSON.toJSONString(paramMap), null, null, "application/json");
        // post请求application/json类型参数
        return result;
    }

    /**
     * 获取监控点播放URL
     *
     * @param cameraID
     * @param playType
     * @return
     */
    public String getPlayURL(String cameraID, String playType) {
        Map<String, String> path = new HashMap() {{
            put("https://", "/artemis/api/firepro/v1/getCameraPlayURL");
        }};
        /**
         * STEP5：组装请求参数
         */
        Map<String, String> paramMap = new HashMap();
        paramMap.put("cameraID", cameraID);
        paramMap.put("playType", playType);

        String responeText = ArtemisHttpUtil.doPostStringArtemis(path, JSON.toJSONString(paramMap), null, null, "application/json");
//        {
//            "code": "0",
//                "data": {
//            "playURL": "https://open.ys7.com/ezopen/h5/iframe_se?url=ezopen://open.ys7.com/203751922/1.live&autoplay=1&accessToken=ra.d8gki4vn1h4uakee92yl0kmt56tnj23k-39jao01xxk-0rmpp1e-bpuukif33"
//        },
//            "msg": "success"
//        }
        if (!StringUtils.hasLength(responeText)) {
            LogsUtil.warn("HikvisionSDK", "获取监控点播放URL发生错误，响应结果为空字符");
            return "";
        }
        JSONObject json = JSONObject.parseObject(responeText);
        int code = json.getIntValue("code", -1);
        if (code != 0) {
            LogsUtil.warn("HikvisionSDK", "获取监控点播放URL发生错误：%s", responeText);
            return "";
        }
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            LogsUtil.warn("HikvisionSDK", "获取监控点播放URL发生错误：%s", responeText);
            return "";
        }
        return data.getString("playURL");
    }
}
