package com.evcharge.entity.shop;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.user.UserCouponV1Entity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 商城订单表;
 *
 * @author : Jay
 * @date : 2022-12-16
 */
@TargetDB("evcharge_shop")
public class ShopOrderEntity extends BaseEntity implements Serializable {
    //1-待付款，2-待发货，3-待收货，4-已完成，5-退货申请，6-同意退货，7-退货中，8-商家收货，9-待退款，10-退款成功
    public static final int OrderStatusCancel = -1;
    public static final int OrderStatusUnPay = 1;
    public static final int OrderStatusUnSend = 2;
    public static final int OrderStatusUnFinish = 3;
    public static final int OrderStatusFinish = 4;
    public static final int OrderStatusApplyReturn = 5;
    public static final int OrderStatusAgreeReturn = 6;
    public static final int OrderStatusReturning = 7;
    public static final int OrderStatusShopGetGoods = 8;
    public static final int OrderStatusUnRefund = 9;
    public static final int OrderStatusRefundFinish = 10;

    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 订单号
     */
    public String ordersn;
    /**
     * 用户id
     */
    public long uid;
    /**
     * 支付订单编号
     */
    public String payordersn;
    /**
     * 实付款
     */
    public BigDecimal payprice;
    /**
     * 支付时间
     */
    public long paytime;
    /**
     * 支付方式
     */
    public long paytype_id;
    /**
     * 订单总价
     */
    public BigDecimal total_price;
    /**
     * 商品总价
     */
    public BigDecimal goods_price;
    /**
     * 优惠总价
     */
    public BigDecimal discount_price;
    /**
     * 运费总价
     */
    public BigDecimal fare_price;
    /**
     * 商品总数量
     */
    public int goods_amount;
    /**
     * 状态：1-待付款，2-待发货，3-待收货，4-已完成，5-退货申请，6-同意退货，7-退货中，8-商家收货，9-待退款，10-退款成功
     */
    public int status;
    /**
     * 是否测试
     */
    public int istest;
    /**
     * 分享码
     */
    public String sharecode;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;
    /**
     * 发货时间
     */
    public long send_time;
    /**
     * 取消时间
     */
    public long cancel_time;
    /**
     * 完成时间
     */
    public long finish_time;
    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ShopOrderEntity getInstance() {
        return new ShopOrderEntity();
    }

    /**
     * 创建订单编号
     *
     * @return
     */
    public String createOrderSn() {

        String orderSn = String.format("Shop%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"), common.randomStr(4));
//                return "Charge"+common.toTimeString(TimeUtil.getTimestamp(),"yyyyMMddHHmm")+common.randomStr(4);
        return orderSn;

    }

    /**
     * 根据id获取订单信息
     *
     * @param id
     * @return
     */
    public ShopOrderEntity getOrderById(long id) {
        return this
                .field("id,ordersn,uid,payordersn,paytime,total_price,goods_price,discount_price,goods_amount,status,create_time,update_time,cancel_time,send_time,finish_time")
                .cache(String.format("Shop:OrderInfo:%s", id), 86400 * 1000)
                .where("id", id)
                .order("create_time DESC").findModel();
    }

    /**
     * 删除缓存
     *
     * @param id
     */
    public void delOrderCacheById(long id) {
        DataService.getMainCache().del(String.format("Shop:OrderInfo:%s", id));
    }

    /**
     * 河马支付回调
     *
     * @param orderSn
     * @param sandData
     * @return
     */
    public String hmCallback(String orderSn, JSONObject sandData) {

//        {
//               "bank_order_no": "HMP1809266450549890110215171", //银行订单号，平台送给渠道的商户订单号
//                "bank_trx_no": "4200000186201809268150013529", //银行流水号，渠道返回给平台的流水号，订单成功情况下存在
//                "buyer_id": "oUpF8uMv_S50DhHpWF8yaUSteOVo", //买家ID，付款方唯一标识
//                "is_refund": "true", //是否有退款 true/false
//                "out_order_no": "1537930941182", //商户订单号，商户下当天唯一
//                "plat_trx_no": "6661809266450549890101826563", //平台交易流水号，平台唯一，在订单非异常情况下必填
//                "refund_success_amount": 0.01, //成功退款金额，单位元
//                "sub_code": "SUCCESS",
//                "sub_msg": "交易成功",
//                "success_time": "20180808111211", //支付成功时间，格式yyyyMMddHHmmss
//                "buyer_pay_amount": "0.01" //买家付款金额，买家实际付款的金额，订单金额-优惠金额
//        }
        ShopOrderEntity shopOrderEntity = this.findOrderByOrderSn(orderSn);
        if (shopOrderEntity == null) {
            LogsUtil.trace("", String.format("[商城订单回调]订单编号:%s，订单信息不存在", orderSn));
            return "error";
        }

        if (shopOrderEntity.status != OrderStatusUnPay) {
            LogsUtil.trace("", String.format("[商城订单回调]订单编号:%s，订单状态不正确，当前订单状态为%s", orderSn, shopOrderEntity.status));
            return "error";
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", OrderStatusUnSend); //支付成功
        data.put("payordersn", sandData.get("bank_order_no")); //更新订单
        data.put("update_time", TimeUtil.getTimestamp());
        data.put("paytype_id", 1);
        data.put("paytime", TimeUtil.getTimestamp());
        data.put("payprice", sandData.getBigDecimal("total_amount"));

        SyncResult r = this.beginTransaction(connection -> {
            if (this.where("id", shopOrderEntity.id).update(data) == 0) {
                return new SyncResult(1, "订单数据更新失败");
            }
            //更新优惠券信息
            UserCouponV1Entity userCouponV1Entity = UserCouponV1Entity.getInstance()
                    .where("order_id", shopOrderEntity.id)
                    .findModel();

            if (userCouponV1Entity != null) {
                SyncResult res = userCouponV1Entity.consume(userCouponV1Entity.id);
                if (res.code != 0) {
                    return new SyncResult(1, "优惠券数据更新失败");
                }
            }

            return new SyncResult(0, "success");
        });

        if (r.code != 0) {
            LogsUtil.trace("", String.format("[商城订单回调]订单编号:%s，回调失败，失败原因=%s", r.msg));
            return "ERROR";
        }
        DataService.getMainCache().del(String.format("Shop:OrderInfo:%s", orderSn));
        DataService.getMainCache().del(String.format("Shop:OrderInfo:%s", shopOrderEntity.id));
        return "SUCCESS";
    }

    /**
     * 免费支付回调
     *
     * @param orderSn
     * @return
     */
    public SyncResult freeCallback(String orderSn) {
        ShopOrderEntity shopOrderEntity = this.findOrderByOrderSn(orderSn);
        if (shopOrderEntity == null) {
            LogsUtil.trace("", String.format("[商城订单回调]订单编号:%s，订单信息不存在", orderSn));
            return new SyncResult(1, "订单不存在");
        }

        if (shopOrderEntity.status != OrderStatusUnPay) {
            LogsUtil.trace("", String.format("[商城订单回调]订单编号:%s，订单状态不正确，当前订单状态为%s", orderSn, shopOrderEntity.status));
            return new SyncResult(1, "订单状态不正确");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", shopOrderEntity.OrderStatusUnSend); //支付成功
        data.put("payprice", 0);
        data.put("paytime", TimeUtil.getTimestamp());
        data.put("update_time", TimeUtil.getTimestamp());
        data.put("paytype_id", 0);

        SyncResult r = this.beginTransaction(connection -> {
            if (this.where("id", shopOrderEntity.id).update(data) == 0) {
                return new SyncResult(1, "订单数据更新失败");
            }
            //更新优惠券信息
            UserCouponV1Entity userCouponV1Entity = UserCouponV1Entity.getInstance()
                    .where("order_id", shopOrderEntity.id)
                    .findModel();

            if (userCouponV1Entity != null) {
                SyncResult res = userCouponV1Entity.consume(userCouponV1Entity.id);
                if (res.code != 0) {
                    return new SyncResult(1, "优惠券数据更新失败");
                }
            }

            return new SyncResult(0, "success");
        });

        if (r.code != 0) {
            LogsUtil.trace("", String.format("[商城订单回调]订单编号:%s，回调失败，失败原因=%s", r.msg));
            return new SyncResult(1, r.msg);
        }
        DataService.getMainCache().del(String.format("Shop:OrderInfo:%s", orderSn));
        DataService.getMainCache().del(String.format("Shop:OrderInfo:%s", shopOrderEntity.id));
        return new SyncResult(0, "success");

    }

    /**
     * 根据订单编号查找订单
     *
     * @param orderSn
     * @return
     */
    public ShopOrderEntity findOrderByOrderSn(String orderSn) {
        return this.cache(String.format("Shop:OrderInfo:%s", orderSn), 86400 * 1000)
                .where("ordersn", orderSn)
                .findModel();
    }

    /**
     * 机器人uid
     *
     * @param robotId
     * @param goodsId
     */
    public SyncResult robotCreateOrder(long robotId, long goodsId) {
        ShopOrderEntity shopOrderEntity = new ShopOrderEntity();
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("uid", robotId);
        order.put("ordersn", ShopOrderEntity.getInstance().createOrderSn());
        order.put("sharecode", "");
        order.put("istest",1);//测试订单
        order.put("status", shopOrderEntity.OrderStatusFinish);//待支付
        order.put("create_time", TimeUtil.getTimestamp());

        ShopGoodsEntity shopGoodsEntity = ShopGoodsEntity.getInstance().getGoodsInfoById(goodsId);

        if (shopGoodsEntity.id == 0) return new SyncResult(1, "商品不存在");
        if (shopGoodsEntity.status == 0) return new SyncResult(1, "商品已下架");
        long r = shopOrderEntity.insertGetId(order);
        if (r == 0) {
            return new SyncResult(1, "创建订单失败，请稍后再试");
        }
        shopOrderEntity.id = r;

        double totalPrice = 0;
        double goodsPrice = 0;
        double discountPrice = 0;
        Map<String, Object> orderGoods = new LinkedHashMap<>();
        orderGoods.put("order_id", shopOrderEntity.id);
        orderGoods.put("goods_id", goodsId);
        orderGoods.put("goods_title", shopGoodsEntity.title);
        orderGoods.put("main_image", shopGoodsEntity.main_image);
        orderGoods.put("price", shopGoodsEntity.sale_price);
        orderGoods.put("amount", common.randomInt(1, 20));
        orderGoods.put("unit", shopGoodsEntity.unit);
        orderGoods.put("weight", shopGoodsEntity.weight);
        orderGoods.put("space_x", shopGoodsEntity.space_x);
        orderGoods.put("space_y", shopGoodsEntity.space_y);
        orderGoods.put("space_z", shopGoodsEntity.space_z);
        orderGoods.put("status", 1);
        orderGoods.put("create_time", TimeUtil.getTimestamp());
        //写入order_goods表
        ShopOrderGoodsEntity.getInstance().insert(orderGoods);

        return new SyncResult(0,"success");
    }


}