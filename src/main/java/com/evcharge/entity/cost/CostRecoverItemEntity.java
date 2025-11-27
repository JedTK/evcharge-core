package com.evcharge.entity.cost;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 项目成本收回项-充电桩被拆除时回收的记录;
 *
 * @author : JED
 * @date : 2025-1-7
 */
public class CostRecoverItemEntity extends BaseEntity implements Serializable {
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
     * 充电桩唯一编号
     */
    public String cs_id;
    /**
     * 项名
     */
    public String item_name;
    /**
     * 类别编码：如设备、耗材等
     */
    public String category_code;
    /**
     * 商品/物品,spu编码
     */
    public String spu_code;
    /**
     * 规格
     */
    public String spec;
    /**
     * 单位（如个、套）
     */
    public String unit;
    /**
     * 单价
     */
    public BigDecimal unit_price;
    /**
     * 数量
     */
    public int quantity;
    /**
     * 金额
     */
    public BigDecimal total_amount;
    /**
     * 购买渠道
     */
    public String purchase_channel;
    /**
     * 无票、普通发票、增值税专用发票、增值税普通发票、电子普通发票、增值税电子发票
     */
    public String invoice_type;
    /**
     * 开票税率（如6%、13%）
     */
    public BigDecimal invoice_tax_rate;
    /**
     * 备注
     */
    public String remark;
    /**
     * 管理员id,记录是谁录入或更新的
     */
    public long admin_id;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static CostRecoverItemEntity getInstance() {
        return new CostRecoverItemEntity();
    }
}
