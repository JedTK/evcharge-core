package com.evcharge.libsdk.Hmpay;

import cn.com.sand.hmpay.vo.request.common.SettleSharingReceiver;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 河马支付分账表;
 *
 * @author : JED
 * @date : 2023-11-28
 */
public class HmPaymentSharingReceiversEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 主商户号
     */
    public String main_merchant_no;
    /**
     * 商户名称
     */
    public String merchant_name;
    /**
     * 小程序appid
     */
    public String wx_app_id;
    /**
     * 分账商户号(河马付系统生成的分账子商户编号15位)
     */
    public String share_merchant_no;
    /**
     * 分账比例
     */
    public double share_rate;
    /**
     * 状态
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
    public static HmPaymentSharingReceiversEntity getInstance() {
        return new HmPaymentSharingReceiversEntity();
    }


    /**
     * 找出分账账号
     *
     * @return
     */
    public SettleSharingReceiver[] findSettleSharingReceivers(String mainMerchantNo, BigDecimal totalAmount) {
        SettleSharingReceiver[] settleSharingReceivers;
        HmPaymentSharingReceiversEntity hmPaymentSharingReceiversEntity = new HmPaymentSharingReceiversEntity();
        List<Map<String, Object>> list = new ArrayList<>();
        list = hmPaymentSharingReceiversEntity.select();

        if (list.size() == 0) return null;
        settleSharingReceivers = new SettleSharingReceiver[list.size()];
        int len=list.size();
        int index = 0;

        for (int i=0;i<len;i++){
            Map<String, Object> nd=list.get(i);
            SettleSharingReceiver settleSharing = new SettleSharingReceiver();
            //写入商户号
            settleSharing.setMerchantNo(MapUtil.getString(nd, "share_merchant_no"));
            //region 计算分账金额
            BigDecimal shareRate = MapUtil.getBigDecimal(nd, "share_rate");
            //分账比例
            BigDecimal divideFactor = new BigDecimal("100.0");
            //先乘以分享比例，再除以100
            BigDecimal shareAmount = totalAmount.multiply(shareRate).divide(divideFactor, 2, RoundingMode.HALF_UP);
            //endregion
            settleSharing.setAmount(shareAmount.doubleValue());
            settleSharing.setDescription("");//分账描述
            settleSharingReceivers[index] = settleSharing;
        }

//        for (Map<String, Object> nd : list) {
//            SettleSharingReceiver settleSharing = new SettleSharingReceiver();
//            //写入商户号
//            settleSharing.setMerchantNo(MapUtil.getString(nd, "share_merchant_no"));
//            //region 计算分账金额
//            BigDecimal shareRate = MapUtil.getBigDecimal(nd, "share_rate");
//            //分账比例
//            BigDecimal divideFactor = new BigDecimal("100.0");
//            //先乘以分享比例，再除以100
//            BigDecimal shareAmount = totalAmount.multiply(shareRate).divide(divideFactor, 2, RoundingMode.HALF_UP);
//            //endregion
//            settleSharing.setAmount(shareAmount.doubleValue());
//            settleSharing.setDescription("");//分账描述
//            settleSharingReceivers[index] = settleSharing;
//            index++;
//        }
        return settleSharingReceivers;
    }


}