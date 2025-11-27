package com.evcharge.entity.inspect.log;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

import java.io.Serializable;

/**
 * 巡检工单;
 *
 * @author : Jay
 * @date : 2024-10-24
 */
@TargetDB("inspect")
public class InspectWorkOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 工单号
     */
    public String order_sn;
    /**
     * 巡检时间
     */
    public long inspect_time ;
    /**
     * 巡检工作人员
     */
    public long inspect_admin_id ;
    /**
     * 异常模块 设备损坏/雨棚损坏之类的
     */
    public long error_module;
    /**
     * 异常等级
     */
    public String error_level;
    /**
     * 异常信息
     */
    public String error_msg;
    /**
     * 异常描述  如果有log_id ,关联logid的data_value
     */
    public String error_description;
    /**
     * 日志id 不一定有
     */
    public long log_detail_id;
    /**
     * 站点uuid
     */
    public String cs_uuid;
    /**
     * 处理异常工作人员
     */
    public long deal_admin_id;
    /**
     * 处理时间
     */
    public long deal_time;
    /**
     * 备注
     */
    public String remark;
    /**
     * 处理信息内容
     */
    public String deal_msg ;
    /**
     * 审核时间
     */
    public long review_time ;
    /**
     * 回退原因
     */
    public String back_reason ;
    /**
     *  1=待处理 2=审核中 3=审核通过，已处理 4=驳回
     */
    public int status;
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
     *
     * @return
     */
    public static InspectWorkOrderEntity getInstance() {
        return new InspectWorkOrderEntity();
    }

    /**
     * 创建工单
     * @return
     */
    public static String createOrderSn() {
        return String.format("OR%s%sSN", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                , common.randomStr(4));
    }


}