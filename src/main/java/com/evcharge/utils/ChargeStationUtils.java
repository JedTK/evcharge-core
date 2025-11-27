package com.evcharge.utils;

import com.xyzs.utils.StringUtil;

/**
 * 充电桩辅助类，用于兼容或检测
 */
public class ChargeStationUtils {

    /**
     * 判断是否有效的站点编码
     *
     * @param CSId
     * @return
     */
    public static boolean isEmptyCSId(String CSId) {
        if (StringUtil.isEmpty(CSId)) return true;
        return "0".equals(CSId);
    }
}
