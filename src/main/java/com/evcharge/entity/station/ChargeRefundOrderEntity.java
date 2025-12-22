package com.evcharge.entity.station;


import com.evcharge.entity.recharge.RechargeRefundOrderEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.EBalanceUpdateType;
import com.evcharge.enumdata.ERefundStatus;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * 充电退款订单;
 *
 * @author : JED
 * @date : 2023-11-13
 */
public class ChargeRefundOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 原订单号，用于关联数据
     */
    public String OrderSN;
    /**
     * 状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
     */
    public int refund_status;
    /**
     * 退款金额
     */
    public double refund_amount;
    /**
     * 退款原因
     */
    public String refund_reason;
    /**
     * 是否为测试订单，0=否，1=是
     */
    public int isTest;
    /**
     * 测试ID
     */
    public long testId;
    /**
     * 充电桩ID
     */
    public String CSId;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 设备号
     */
    public String deviceCode;
    /**
     * 设备端口
     */
    public int port;
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
    public static ChargeRefundOrderEntity getInstance() {
        return new ChargeRefundOrderEntity();
    }

    /**
     * 充电退款操作
     *
     * @param OrderSN 充值订单号
     * @param reason  退款理由
     */
    public SyncResult refund(String OrderSN, double refund_amount, String reason) {
        if (!StringUtils.hasLength(OrderSN)) return new SyncResult(2, "请输入订单号");
        if (refund_amount == 0) return new SyncResult(2, "请输入退款金额");
        refund_amount = Math.abs(refund_amount);

        //查询充电订单信息
        ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();
        if (orderEntity == null || orderEntity.id == 0) return new SyncResult(3, "不存在订单");
        //状态,-1=启动错误，0=待启动，1=充电中，2=已完成
        if (orderEntity.status != 2) return new SyncResult(4, "订单还没充电完成，无法操作退款");

        //检查之前的退款金额
        double total_refund_amount = RechargeRefundOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .where("refund_status", ERefundStatus.Completed.index)
                .sum("refund_amount");
        if (Math.abs(refund_amount + total_refund_amount) > orderEntity.totalAmount) {
            return new SyncResult(8, "退款金额不能大于原支付金额");
        }

        String organize_code = orderEntity.organize_code;
        if (!StringUtil.hasLength(organize_code)) {
            organize_code = SysGlobalConfigEntity.getString("System:Organize:Code");
        }

        ChargeRefundOrderEntity refundOrderEntity = ChargeRefundOrderEntity.getInstance();
        refundOrderEntity.uid = orderEntity.uid;
        refundOrderEntity.OrderSN = OrderSN;
        refundOrderEntity.refund_status = ERefundStatus.Completed.index;
        refundOrderEntity.refund_amount = refund_amount;
        refundOrderEntity.refund_reason = reason;
        refundOrderEntity.isTest = orderEntity.isTest;
        refundOrderEntity.testId = orderEntity.testId;
        refundOrderEntity.CSId = orderEntity.CSId;
        refundOrderEntity.organize_code = organize_code;
        refundOrderEntity.deviceCode = orderEntity.deviceCode;
        refundOrderEntity.port = orderEntity.port;
        refundOrderEntity.create_time = TimeUtil.getTimestamp();
        refundOrderEntity.update_time = TimeUtil.getTimestamp();

        double finalRefund_amount = refund_amount;
        return refundOrderEntity.beginTransaction(connection -> {
            refundOrderEntity.id = refundOrderEntity.insertTransaction(connection);
            if (refundOrderEntity.id == 0) return new SyncResult(10, "退款失败");

            return UserSummaryEntity.getInstance().updateBalanceTransaction(connection,
                    orderEntity.uid
                    , finalRefund_amount
                    , EBalanceUpdateType.charge_refund
                    , "充电退款-" + reason
                    , OrderSN);
        });
    }
}
