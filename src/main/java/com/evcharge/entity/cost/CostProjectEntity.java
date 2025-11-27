package com.evcharge.entity.cost;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 项目成本;
 *
 * @author : JED
 * @date : 2025-1-7
 */
public class CostProjectEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 其他关联的项目编码，如立项的项目编码
     */
    public String project_code;
    /**
     * 项目名称
     */
    public String project_name;
    /**
     * 充电桩唯一编号
     */
    public String cs_id;
    /**
     * 状态:0-删除，1-正常，2-拆除
     */
    public int status;
    /**
     * 创建者id(管理员id)
     */
    public long creator_id;
    /**
     * 备注
     */
    public String remark;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 平台代码
     */
    public String platform_code;
    /**
     * 合计成本总金额
     */
    public BigDecimal cost_total_amount;
    /**
     * 是否为模板，其他项目可以负责模板的内容项
     */
    public int is_template;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static CostProjectEntity getInstance() {
        return new CostProjectEntity();
    }
}
