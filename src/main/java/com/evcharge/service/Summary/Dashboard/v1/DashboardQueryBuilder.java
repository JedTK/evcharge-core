package com.evcharge.service.Summary.Dashboard.v1;

import com.evcharge.entity.admin.AdminToOrganizeEntity;
import com.evcharge.entity.sys.SysStreetEntity;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

/**
 * 数据仪表板组织器
 */
public class DashboardQueryBuilder {
    /**
     * 组织代码，多个值用英文逗号分隔
     */
    public String organize_code;
    /**
     * 平台代码，多个值用英文逗号分隔
     */
    public String platform_code;
    /**
     * 运营模式代码：直投直营、代理、直管（代管理），多个值用英文逗号分隔
     */
    public String op_mode_code;
    /**
     * 合作角色：物业、街道、商务渠道、投资人，多个值用英文逗号分隔
     */
    public String partner_type_code;
    /**
     * 省代码，多个值用英文逗号分隔
     */
    public String province_code;
    /**
     * 市代码，多个值用英文逗号分隔
     */
    public String city_code;
    /**
     * 行政区域代码，多个值用英文逗号分隔
     */
    public String district_code;
    /**
     * 街道代码，多个值用英文逗号分隔
     */
    public String street_code;
    /**
     * 查询的开始时间戳
     */
    public long start_time = 0;
    /**
     * 查询的结束时间戳
     */
    public long end_time = 0;

    /**
     * 自动分配组织代码：如果前端传递auto值过来，会自动获取管理员的组织代码
     *
     * @param adminId 管理员id
     */
    public void autoAssignOrganizeCode(long adminId) {
        if ("auto".equalsIgnoreCase(organize_code)) {
            this.organize_code = AdminToOrganizeEntity.getInstance().getOrganizeCode(adminId);
        }
    }

    /**
     * 执行函数前检查
     *
     * @return 是否通过
     */
    public SyncResult check() {
        // 检查开始时间和结束时间是否设置
        if (this.start_time == 0) return new SyncResult(2, "请选择开始日期");
        if (this.end_time == 0) return new SyncResult(2, "请选择结束日期");

        // 将开始时间设定为当天凌晨，结束时间设定为当天结束
        this.start_time = TimeUtil.toDayBegin00(this.start_time);
        this.end_time = TimeUtil.toDayEnd24(this.end_time);

        // 确保结束时间不能早于开始时间
        if (this.end_time <= this.start_time) return new SyncResult(2, "结束时间不能小于开始时间");
        return new SyncResult(0, "");
    }

    /**
     * 通过省市区街道等代码来获取街道代码集合
     *
     * @return 街道代码列表
     */
    public String[] getStreetCodeList() {
        // 如果街道代码不为空，则查询范围为街道
        if (!StringUtil.isEmpty(this.street_code)) {
            return this.street_code.split(",");
        }

        // 如果街道代码为空，行政区域代码不为空，则查询范围为行政区域
        if (!StringUtil.isEmpty(this.district_code)) {
            return SysStreetEntity.getInstance().getListWithAreaCodes(this.district_code);
        }

        // 通过市代码查询街道代码
        if (!StringUtil.isEmpty(this.city_code)) {
            return SysStreetEntity.getInstance().getListWithCityCodes(this.city_code);
        }

        // 通过省代码查询街道代码
        if (!StringUtil.isEmpty(this.province_code)) {
            return SysStreetEntity.getInstance().getListWithProvinceCodes(this.province_code);
        }
        return null;
    }

    /**
     * 生成唯一的key
     *
     * @return 缓存键字符串
     */
    public String getUniqueKey() {
        // 使用 StringBuilder 拼接各字段，形成唯一标识
        String keyBuilder = "DashboardV1:" +
                "org:" + (StringUtil.isEmpty(this.organize_code) ? "ALL" : this.organize_code) + ":" +
                "plat:" + (StringUtil.isEmpty(this.platform_code) ? "ALL" : this.platform_code) + ":" +
                "op:" + (StringUtil.isEmpty(this.op_mode_code) ? "ALL" : this.op_mode_code) + ":" +
                "role:" + (StringUtil.isEmpty(this.partner_type_code) ? "ALL" : this.partner_type_code) + ":" +
                "prov:" + (StringUtil.isEmpty(this.province_code) ? "ALL" : this.province_code) + ":" +
                "city:" + (StringUtil.isEmpty(this.city_code) ? "ALL" : this.city_code) + ":" +
                "dist:" + (StringUtil.isEmpty(this.district_code) ? "ALL" : this.district_code) + ":" +
                "street:" + (StringUtil.isEmpty(this.street_code) ? "ALL" : this.street_code) + ":" +
                "start:" + this.start_time + ":" +
                "end:" + this.end_time;
        return common.md5(keyBuilder);
    }
}
