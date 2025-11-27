package com.evcharge.service.ChargeStation;

import com.xyzs.utils.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 站点控制筛选条件
 */
public class SiteControlFilter {
    /**
     * 省份
     */
    public String province;
    /**
     * 城市
     */
    public String city;
    /**
     * 行政区域
     */
    public String district;
    /**
     * 街道
     */
    public String street;
    /**
     * 社区
     */
    public String communities;
    /**
     * 结构：0-无，1-棚，2-架，3-墙 ，4-柱，99-其他
     */
    public int[] arch;
    /**
     * 精准选择的站点编码列表
     */
    public String[] cs_ids;

    /**
     * 转区域文本
     * @return
     */
    public String toAreaText(){
        String area_text = "";
        if (StringUtil.hasText(this.province)) area_text += this.province;
        if (StringUtil.hasText(this.city)) area_text += this.city;
        if (StringUtil.hasText(this.district)) area_text += this.district;
        if (StringUtil.hasText(this.street)) area_text += this.street;
        if (StringUtil.hasText(this.communities)) area_text += this.communities;
        return area_text;
    }

    /**
     * 转结构文本
     *
     * @return
     */
    public String toArchText() {
        if (this.arch == null || this.arch.length == 0) return "--";
        StringBuilder sb = new StringBuilder();
        for (int arch : this.arch) {
            switch (arch) {
                case 1:
                    sb.append("、棚");
                    break;
                case 2:
                    sb.append("、架");
                    break;
                case 3:
                    sb.append("、墙");
                    break;
                case 4:
                    sb.append("、柱");
                    break;
                default:
                    sb.append("、其他");
            }
        }
        return sb.length() > 0 ? sb.substring(1) : "--";
    }

    /**
     * 缩写站点文本
     * @return
     */
    public String toCSIdsText() {
        if (this.cs_ids == null || this.cs_ids.length == 0) {
            return "";
        }
        return Arrays.stream(this.cs_ids)
                .limit(3)
                .collect(Collectors.joining(",")) + "...";
    }
}
