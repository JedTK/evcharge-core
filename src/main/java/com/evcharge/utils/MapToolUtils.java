package com.evcharge.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapToolUtils {
    private final static double PI = 3.14159265;
    /**
     * 地球半径
     */
    private final static double EARTH_RADIUS = 6378137;

    private final static double RAD = Math.PI / 180.0;

    /**
     * 根据提供的经度和纬度、以及半径，取得此半径内的最大最小经纬度
     *
     * @param lon    经度
     * @param lat    纬度
     * @param radius 半径，单位米
     * @return minLat, minLng, maxLat, maxLng
     */
    public static Map<String, Object> getAround(Double lon, Double lat, int radius) {
        double degree = (24901 * 1609) / 360.0;//度
        double dpmLat = 1 / degree;
        Double radiusLat = dpmLat * (double) radius;
        Double minLat = lat - radiusLat;
        Double maxLat = lat + radiusLat;
        double mpdLng = degree * Math.cos(lat * (PI / 180));
        double dpmLng = 1 / mpdLng;
        Double radiusLng = dpmLng * (double) radius;
        Double minLon = lon - radiusLng;
        Double maxLon = lon + radiusLng;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("minLon", minLon);//最小经度
        data.put("maxLon", maxLon);//最大经度
        data.put("minLat", minLat);//最小纬度
        data.put("maxLat", maxLat);//最大纬度

        return data;
    }

    /**
     * 根据两点间经纬度坐标（double值），计算两点间距离，单位为米 *
     *
     * @param lon1 第一点经度
     * @param lat1 第一点纬度
     * @param lon2 第二点经度
     * @param lat2 第一点纬度
     * @return
     */
    public static Double getDistance(Double lon1, Double lat1, Double lon2, Double lat2) {
        double radLat1 = lat1 * RAD;
        double radLat2 = lat2 * RAD;
        double a = radLat1 - radLat2;
        double b = (lon1 - lon2) * RAD;
        double s = 2 * Math.asin(Math.sqrt(
                Math.pow(Math.sin(a / 2), 2)
                        + Math.cos(radLat1)
                        * Math.cos(radLat2)
                        * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10000.0;
        return s;
    }
}
