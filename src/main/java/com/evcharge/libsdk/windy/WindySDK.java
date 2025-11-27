package com.evcharge.libsdk.windy;

import com.xyzs.utils.*;
import org.springframework.util.StringUtils;


/**
 * 广州帝能股份有限公司的设备MQTT转发服务处理
 */
public class WindySDK {

    /**
     * 版本号
     */
    public final static String mVersion = "2";

    private static WindySDK _this;

    /**
     * 获得一个实例
     *
     * @return
     */
    public static WindySDK getInstance() {
        if (_this == null) _this = new WindySDK();
        return _this;
    }

    /**
     * 设备码转换设备号
     *
     * @param deviceCode
     * @return
     */
    public String convertDeviceNumber(String deviceCode) {
        try {
            String dc = deviceCode.substring(0, deviceCode.length() - 2);
            char[] chars = dc.toCharArray();
            String nd = "";
            String number16 = "";
            for (int i = dc.length(); i >= 0; i--) {
                if (i % 2 == 0) {
                    number16 += nd;
                    nd = "";
                }
                if (i - 1 >= 0) nd = String.format("%s%s", chars[i - 1], nd);
            }

            int number = Integer.parseInt(number16, 16);
            return String.format("%s", number);
        } catch (Exception e) {
//            LogsUtil.error(e, "", "设备码转换设备号出错：%s", deviceCode);
        }
        return "";
    }

    /**
     * 设备物理ID转设备码
     *
     * @param deviceNumber
     * @return
     */
    @Deprecated
    public String convertDeviceCode(int deviceNumber, boolean isHost) {
        try {
            String hex = Integer.toHexString(deviceNumber);
            char[] chars = hex.toCharArray();
            String nd = "";
            String number16 = "";
            for (int i = hex.length(); i >= 0; i--) {
                if (i % 2 == 0) {
                    number16 += nd;
                    nd = "";
                }
                if (i - 1 >= 0) nd = String.format("%s%s", chars[i - 1], nd);
            }
            String deviceCode = number16;
            if (isHost) deviceCode += "09";
            else deviceCode += "04";
            return deviceCode.toUpperCase();
        } catch (Exception e) {
            LogsUtil.error(e, "", "设备物理ID转设备码");
        }
        return "";
    }

    /**
     * 通过设备码判断是否为主机
     *
     * @param deviceCode
     * @return
     */
    public boolean isHost(String deviceCode) {
        if (!StringUtils.hasLength(deviceCode)) return false;
        String end = deviceCode.substring(deviceCode.length() - 2);
        if ("09".equals(end)) return true;
        return false;
    }
}
