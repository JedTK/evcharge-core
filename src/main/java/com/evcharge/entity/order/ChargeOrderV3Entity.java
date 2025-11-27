package com.evcharge.entity.order;

import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.order.settlement.v3.SettlementForCabinetV3;
import com.evcharge.entity.order.settlement.v3.SettlementForEBikeV3;
import com.evcharge.entity.order.settlement.v3.IChargingSettlementV3;
import com.evcharge.entity.order.settlement.v3.SettlementForNEVCarV3;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.sys.SysMessageEntity;
import com.evcharge.entity.user.UserBalanceLogEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.EBalanceUpdateType;
import com.evcharge.enumdata.EChargeOrderType;
import com.evcharge.libsdk.wechat.WechatSubscribeTmplSDK;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 充电订单;
 *
 * @author : JED
 * @date : 2024-3-12
 */
public class ChargeOrderV3Entity extends BaseEntity implements Serializable {
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
     * 充电站ID
     */
    public String CSId;
    /**
     * 充电订单号
     */
    public String OrderSN;
    /**
     * 设备码
     */
    public String deviceCode;
    /**
     * 端口
     */
    public int port;
    /**
     * 状态：-1=错误，0=待启动，1=充电中，2=已完成
     */
    public int status;
    /**
     * 状态错误说明
     */
    public String status_msg;
    /**
     * 订单类型：1=电动自行车充电订单，2=充电柜充电订单，3=新能源汽车充电订单
     */
    public int orderType;
    /**
     * 支付方式：1=余额，2=充电卡，3=积分
     */
    public int paymentType;
    /**
     * 支付订单类型：1=先付后充，2=先充后付
     */
    public int paymentOrderType;
    /**
     * 支付状态：0=未支付，1=已支付
     */
    public int paymentStatus;
    /**
     * 充电模式：0=计时，1=包月
     */
    public int chargeMode;
    /**
     * 充电时间：配置ID
     */
    public long chargeTimeItemId;
    /**
     * 收费标准：收费项目ID，峰值计算时用到
     */
    public long chargeStandardItemId;
    /**
     * 收费标准：配置ID
     */
    public long chargeStandardConfigId;
    /**
     * 计费类型：1-峰值功率计费，2-电量计费
     */
    public int billingType;
    /**
     * 开始充电时间戳，单位：毫秒
     */
    public long startTime;
    /**
     * 预计结束充电时间戳，单位：毫秒
     */
    public long endTime;
    /**
     * 停止充电时间戳，单位：毫秒
     */
    public long stopTime;
    /**
     * 充电时长,单位：秒
     */
    public long chargeTime;
    /**
     * 预扣费金额，元，先付后充预计扣费金额
     */
    public BigDecimal estimateAmount;
    /**
     * 优惠金额，元，活动优惠，如：积分优惠
     */
    public BigDecimal discountAmount;
    /**
     * 应扣费金额，元，应该扣费的金额（充电卡消耗金额）
     */
    public BigDecimal receivableAmount;
    /**
     * 实际扣费金额，元，减去优惠等等实际情况扣费的金额
     */
    public BigDecimal totalAmount;
    /**
     * 电费=衍生耗电量 * 电费单价 * 充电小时
     */
    public BigDecimal electricityFeeAmount;
    /**
     * 服务费=服务费电价 * 充电小时
     */
    public BigDecimal serviceFeeAmount;
    /**
     * 总用电量，心跳包更新
     */
    public BigDecimal totalElectricity;
    /**
     * 停车费金额，元
     */
    public BigDecimal parkingFeeAmount;
    /**
     * 充满自停标识，0=否，1=是
     */
    public int chargeAutoStop;
    /**
     * 安全充电保险，0=不启用，1=启用
     */
    public int safeCharge;
    /**
     * 安全充电保险费用
     */
    public double safeChargeFee;
    /**
     * 充电最大功率
     */
    public BigDecimal maxPower;
    /**
     * 限制充电功率
     */
    public long limitChargePower;
    /**
     * 停止原因：状态码，-1=故障，1=充满自停，2=达到最大充电时间，3=达到预设时间，4=达到预设电量，5=用户拔出
     */
    public int stopReasonCode;
    /**
     * 停止原因：文本
     */
    public String stopReasonText;
    /**
     * 充电过载重启，0-否，1-是
     */
    public int overloadRestart;
    /**
     * 父级订单号
     */
    public String parentOrderSN;
    /**
     * 电表ID
     */
    public long meterId;
    /**
     * 版本
     */
    public int version;
    /**
     * 优惠说明
     */
    public String discountRemark;
    /**
     * 更新时间戳
     */
    public long update_time;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeOrderV3Entity getInstance() {
        return new ChargeOrderV3Entity();
    }

    /**
     * 结算充电
     * 订单类型：    1=电动自行车充电订单，2=充电柜充电订单，3=新能源汽车充电订单
     * 支付方式：    1=余额，2=充电卡，3=积分
     * 支付订单类型： 1=先付后充，2=先充后付
     * 充电模式：    0=计时，1=包月
     * 计费类型：    1-峰值功率计费，2-电量计费
     * <p>
     * 流程：
     * 1、订单类型结算 - 计费类型结算
     * 2、实际流程：根据不同类型的订单进行自行结算，而类型订单结束时也可能会根据不同的计费类型来选择结算
     *
     * @return 同步结果
     */
    public SyncResult chargeFinish(String OrderSN
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {

        String logMessage = "";
        logMessage += String.format("\033[1;94m 充电结算v3版本：OrderSN=%s stopReasonCode=%s stopReasonText=%s \033[0m", OrderSN, stopReasonCode, stopReasonText);
        LogsUtil.info(this.getClass().getSimpleName(), "%s", logMessage);

        ChargeOrderV3Entity orderEntity = ChargeOrderV3Entity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();

        //判断是否已经进行了软结算
        if (orderEntity.status == 2) return new SyncResult(0, "已结算");
        if (orderEntity.stopTime != 0) stopTime = orderEntity.stopTime;
        if (stopTime <= 0) stopTime = TimeUtil.getTimestamp();

        //查询设备数据
        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
        if (deviceEntity == null || deviceEntity.id == 0) {
            LogsUtil.error(this.getClass().getSimpleName(), "查询设备数据出错,deviceCode=%s", orderEntity.deviceCode);
            return new SyncResult(1, "查询设备数据出错");
        }

        //根据不同的订单类型进行结算
        IChargingSettlementV3 settlement = null;
        EChargeOrderType eChargeOrderType = EChargeOrderType.valueOf(orderEntity.orderType);
        switch (eChargeOrderType) {
            case EBike:// 电动自行车充电订单
                settlement = new SettlementForEBikeV3();
                break;
            case Cabinet:// 充电柜充电订单
                settlement = new SettlementForCabinetV3();
                break;
            case NEVCar:// 新能源汽车充电订单
                settlement = new SettlementForNEVCarV3();
                break;
        }
        if (settlement == null) return new SyncResult(404, "[%s]无法结算，原因：缺少结算规则", OrderSN);
        //进行结算
        SyncResult r = settlement.chargeFinish(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
        if (r.code != 0) return r;

        Map<String, Object> cbData = (Map<String, Object>) r.data;
        double totalAmount = MapUtil.getDouble(cbData, "totalAmount");

        //已经进行了结算，需要重新查询订单信息来进行判断
        orderEntity = ChargeOrderV3Entity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();

        //region 微信充电完成通知

        try {
            String PortText = "未知";
            DeviceSocketEntity deviceSocketEntity = DeviceSocketEntity.getInstance()
                    .cache(String.format("Device:%s:Port:%s:Socket", deviceEntity.id, orderEntity.port))
                    .where("deviceId", deviceEntity.id)
                    .where("port", orderEntity.port)
                    .findEntity();
            if (deviceSocketEntity != null && deviceSocketEntity.id > 0) {
                PortText = String.valueOf(deviceSocketEntity.index);

                deviceSocketEntity.where("deviceId", deviceEntity.id)
                        .where("port", orderEntity.port)
                        .update(new HashMap<>() {{
                            put("usePower", 0.0);
                        }});
            }

            ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance()
                    .where("id", orderEntity.CSId)
                    .findEntity();
            String ChargeStandardName = "";
            if (chargeStationEntity != null) {
                ChargeStandardName = chargeStationEntity.name;
            }

            String ChargeTimeText = TimeUtil.convertToFormatTime(orderEntity.chargeTime);

            Map<String, Object> noticeData = new HashMap<>();
            noticeData.put("OrderSN", OrderSN);
            noticeData.put("ChargeStandardName", ChargeStandardName);
            noticeData.put("Port", PortText);
            noticeData.put("ChargeTime", ChargeTimeText);
            //2022/11/2 修复充电完成的微信通知，显示充电时间的小数点N位
            noticeData.put("totalAmount", String.format("%.2f", totalAmount));
            //2022/11/2 修改通知模板，添加停止原因
            noticeData.put("stopReasonText", stopReasonText);
            System.out.println(noticeData);
            wechatChargeDoneNotice(orderEntity.uid, noticeData);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "发送充电完成通知发生错误");
        }

        //endregion

        //发送用户站内通知
        SysMessageEntity.getInstance().sendChargeNoticeToUserWithStopReasonCode(stopReasonCode, orderEntity);

        return r;
    }

    /**
     * 微信订阅充电完成通知
     */
    public void wechatChargeDoneNotice(long uid, Map<String, Object> data) {
        String templateId = SysGlobalConfigEntity.getString("Wechat:App:ChargeDoneNotice:TemplateId");

        Map<String, Object> msgObj = new HashMap<>();

        //character_string1={OrderSN}&thing2={ChargeStandardName}&amount6={totalAmount}&thing12={ChargeTime}&thing9={stopReasonText}
        String templateData = SysGlobalConfigEntity.getString("Wechat:App:ChargeDoneNotice:TemplateData");
        try {
            //region 替换值data的值
//            Iterator it = data.keySet().iterator();
//            while (it.hasNext()) {
//                String key = (String) it.next();
//                String value = MapUtil.getString(data, key);
//                templateData = templateData.replace(String.format("{%s}", key), value);
//            }
//            String[] temp = templateData.split("&");
//            for (String str : temp) {
//                String[] s = str.split("=");
//                String key = s[0];
//                Object value = s[1];
//                msgObj.put(key, value);
//            }
            //endregion

            //获得openId
            Map<String, Object> wechatUser = UserEntity.getInstance().findWxUserByID(uid);
            String openId = MapUtil.getString(wechatUser, "open_id");

            //获得跳转路径
            String jumpUrl = SysGlobalConfigEntity.getString("Wechat:App:ChargeDoneNotice:JumpPath");
            jumpUrl = jumpUrl.replace("{OrderSN}", MapUtil.getString(data, "OrderSN"));

            //发送通知
            WechatSubscribeTmplSDK wechatSubscribeTmplSDK = new WechatSubscribeTmplSDK();
            msgObj = wechatSubscribeTmplSDK.createTemplateData(templateData, data);

            SyncResult r = wechatSubscribeTmplSDK.sendSubscribeMessage(openId, jumpUrl, templateId, msgObj);
            if (r.code != 0) {
                LogsUtil.error("微信订阅消息", "充电完成通知，发生错误，原因：%s", r.msg);
            }
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getSimpleName(), "微信订阅充电完成通知 - 发生错误 templateId：%s templateData：%s", templateId, templateData);
        }
    }

    /**
     * 充电预扣费退款，会自动检查此订单是否已经退款了
     *
     * @param uid            用户ID
     * @param estimateAmount 预扣费金额
     * @param OrderSN        订单号
     * @return
     * @throws SQLException
     */
    public SyncResult esChargeRefund(long uid, BigDecimal estimateAmount, String OrderSN) {
        return beginTransaction(connection -> esChargeRefundTransaction(connection, uid, estimateAmount, OrderSN));
    }

    /**
     * 充电预扣费退款，会自动检查此订单是否已经退款了
     *
     * @param connection     事务
     * @param uid            用户ID
     * @param estimateAmount 预扣费金额
     * @param OrderSN        订单号
     * @return
     * @throws SQLException
     */
    public SyncResult esChargeRefundTransaction(Connection connection, long uid, BigDecimal estimateAmount, String OrderSN) throws SQLException {
        return esChargeRefundTransaction(connection, uid, estimateAmount, OrderSN, 0);
    }

    /**
     * 充电预扣费退款，会自动检查此订单是否已经退款了
     *
     * @param connection     事务
     * @param uid            用户ID
     * @param estimateAmount 预扣费金额
     * @param OrderSN        订单号
     * @param testId         测试ID
     * @return
     * @throws SQLException
     */
    public SyncResult esChargeRefundTransaction(Connection connection, long uid, BigDecimal estimateAmount, String OrderSN, long testId) throws SQLException {
        //先检查是否此笔订单是否已预扣费
        if (!UserBalanceLogEntity.getInstance()
                .where("type", EBalanceUpdateType.escharge)
                .where("OrderSN", OrderSN)
                .existTransaction(connection)) {
            return new SyncResult(0, "没有充电预扣费");
        }

        Map<String, Object> data = UserBalanceLogEntity.getInstance()
                .field("id")
                .where("uid", uid)
                .where("type", EBalanceUpdateType.escharge_refund)
                .where("OrderSN", OrderSN)
                .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
        if (data != null && !data.isEmpty()) return new SyncResult(0, "已经退款");

        return UserSummaryEntity.getInstance().updateBalanceTransaction(connection
                , uid
                , estimateAmount
                , EBalanceUpdateType.escharge_refund
                , "充电预扣费退款"
                , OrderSN
                , null
                , testId
        );
    }
}
