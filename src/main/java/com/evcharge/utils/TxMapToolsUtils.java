package com.evcharge.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Http2Util;
import com.xyzs.utils.LogsUtil;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TxMapToolsUtils {


    protected static String AppKey = "RSQBZ-2WHCJ-GLMFM-FHIDI-I2EG3-4SFFO";


    /**
     * 获取地址的经纬度
     *
     * @param address 地址
     * @return object 对象 {lat:12312,lng:32131231}
      */
    public static SyncResult getAddressPoi(String address) {
        if (!StringUtils.hasLength(address)) {
            return new SyncResult(1, "地址不能为空");
        }

        String url = "https://apis.map.qq.com/ws/geocoder/v1/";
        url = String.format("%s?key=%s&address=%s", url, AppKey, address);

        System.out.println(url);
        String responseText = Http2Util.get(url);
        if (!StringUtils.hasLength(responseText)) {
            LogsUtil.warn("TxMapToolsUtils", "响应空白结果");
            return new SyncResult(1, "响应空白结果");
        }
        JSONObject jsonObject = JSONObject.parseObject(responseText);
        if (jsonObject == null || jsonObject.getInteger("status") != 0) {
            return new SyncResult(1, "");
        }

        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");

        if(location.isEmpty()){
            return new SyncResult(1,"获取经纬度位置失败");
        }

        return new SyncResult(0,"success",location);




    }


    /**
     * 获取路线图
     *
     * @param from 出发点
     * @param to   终点
     * @return jsonObject
     */
    public static SyncResult getRoutePlanning(String from, String to) {
        //https://apis.map.qq.com/ws/direction/v1/driving/ 驾车
        //https://apis.map.qq.com/ws/direction/v1/walking/ 步行
        //https://apis.map.qq.com/ws/direction/v1/ebicycling/ 电动车
        String url = "https://apis.map.qq.com/ws/direction/v1/ebicycling/";
        url = String.format("%s?from=%s&to=%s&key=%s", url, from, to, AppKey);
        System.out.println(url);
        String responseText = Http2Util.get(url);
        if (!StringUtils.hasLength(responseText)) {
            LogsUtil.warn("TxMapToolsUtils", "响应空白结果");
            return new SyncResult(1, "响应空白结果");
        }
        JSONObject jsonObject = JSONObject.parseObject(responseText);
        if (jsonObject == null || jsonObject.getInteger("status") != 0) {
            return new SyncResult(1, jsonObject.get("message"));
        }

        // 2. 获取第一个路线的polyline数组
        JSONArray routes = jsonObject.getJSONObject("result").getJSONArray("routes");

        if (routes.isEmpty()) {
            return new SyncResult(1, "暂无路线推荐");
        }
        JSONArray polyline = routes.getJSONObject(0).getJSONArray("polyline");
        // 打印polyline数组
        System.out.println("Polyline array:");
        for (int i = 0; i < polyline.size(); i++) {
            System.out.println(polyline.getDouble(i));
        }
        List<Map<String, Object>> routeArr = decompressPolyline(polyline);
        System.out.println(routeArr);
        return new SyncResult(0, "success", routeArr);

    }

    /**
     * 获取用户地址
     *
     * @param lat 经度
     * @param lon 纬度
     * @return object
     */
    public static SyncResult getUserLocation(String lat, String lon) {

        String url = "https://apis.map.qq.com/ws/geocoder/v1/";

        Map<String, Object> param = new LinkedHashMap<>();

        param.put("key", AppKey);
        param.put("location", String.format("%s,%s", lat, lon));


        String text = Http2Util.get(url, param);

        if (StringUtils.hasLength(text)) {
            return new SyncResult(1, "位置信息获取失败");
        }

        JSONObject result = JSONObject.parseObject(text);

        if (result.getInteger("status") != 0) {
            return new SyncResult(1, result.get("message"));
        }

        JSONObject jsonObject = result.getJSONObject("result");

        JSONObject addressComponent = jsonObject.getJSONObject("address_component");
        //addressComponent {nation:'',province:'',city:'',district:'',street:'',street_number:''}
        return new SyncResult(0, "success", addressComponent);
    }

    public static List<Map<String, Object>> decompressPolyline(JSONArray polyline) {
        List<Map<String, Object>> coordinates = new ArrayList<>();

        if (polyline.size() < 2) {
            return coordinates;
        }

        double[] coors = new double[polyline.size()];
        for (int i = 0; i < polyline.size(); i++) {
            coors[i] = polyline.getDouble(i);
        }

        // 解压坐标
        for (int i = 2; i < coors.length; i++) {
//            coors[i] = coors[i-2] + coors[i]/1000000;
            double decompressed = coors[i - 2] + coors[i] / 1000000;
            coors[i] = Math.round(decompressed * 1000000.0) / 1000000.0;
        }

        // 将解压后的坐标转换为所需格式
        for (int i = 0; i < coors.length; i += 2) {
            if (i + 1 < coors.length) {
//                coordinates.add(new double[]{coors[i], coors[i+1]});
                int finalI = i;
                coordinates.add(new LinkedHashMap<>() {{
                    put("latitude", coors[finalI]);
                    put("longitude", coors[finalI + 1]);
                }});
            }
        }
        return coordinates;
    }


}
