package com.evcharge.entity.RSProfit;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 分润配置;
 *
 * @author : JED
 * @date : 2024-7-1
 */
@TargetDB("evcharge_rsprofit")
public class RSProfitConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电桩ID
     */
    public String cs_id;
    /**
     * 收益人联系电话
     */
    public String channel_phone;
    /**
     * 收益人姓名
     */
    public String channel_name;
    /**
     * 收益人角色：1-场地方，2-居间人，3-商务，4-物业，5-合作伙伴
     */
    public int channel_role;
    /**
     * 分润模式代码
     */
    public String mode_code;
    /**
     * 单价，按单价收益时用，比如：电费单价、充电端口单价
     */
    public BigDecimal price;
    /**
     * 比率，按百分比收益时用，比如：分成比例
     */
    public BigDecimal ratio;
    /**
     * 状态：-1-停止，0-待确认，1-待启动，2-启动中
     */
    public int status;
    /**
     * 收益开始时间
     */
    public long start_time;
    /**
     * 收益结束时间
     */
    public long end_time;
    /**
     * 详情，程序自动生成详情，用于快速查询说明此条配置说明
     */
    public String detail;
    /**
     * 备注
     */
    public String remark;
    /**
     * 组织代码（可选）
     */
    public String organize_code;
    /**
     * 合同编号
     */
    public String contract_no;
    /**
     * 最后日志消息
     */
    public String last_log_message;
    /**
     * 立项的项目ID
     */
    public String project_id;
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
    public static RSProfitConfigEntity getInstance() {
        return new RSProfitConfigEntity();
    }
}
