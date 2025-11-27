package com.evcharge.entity.sys;

import com.evcharge.entity.order.ChargeOrderV3Entity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.ThreadUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.sql.Connection;

/**
 * 站内消息;
 *
 * @author : JED
 * @date : 2022-11-10
 */
public class SysMessageEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 发送用户ID，0为系统
     */
    public long sender_uid;
    /**
     * 接收用户ID
     */
    public long receiver_uid;
    /**
     * 消息id
     */
    public long message_id;
    /**
     * 状态：0=未查看，1=已查看
     */
    public int status;
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
    public static SysMessageEntity getInstance() {
        return new SysMessageEntity();
    }

    /**
     * 发送系统通知
     *
     * @param receiver_uid 接收信息的用户ID
     * @param title        消息标题
     * @param content      消息内容
     */
    public void sendSysNoticeAsync(long receiver_uid, String title, String content) {
        ThreadUtil.getInstance().execute("异步发送系统通知", () -> {
            DataService.getMainDB().beginTransaction(connection -> sendSysNoticeTransaction(connection, receiver_uid, title, content, ""));
        });
    }

    /**
     * 异步发送系统通知
     *
     * @param receiver_uid 接收信息的用户ID
     * @param title        消息标题
     * @param content      消息内容
     * @param path         路径
     */
    public void sendSysNoticeAsync(long receiver_uid, String title, String content, String path) {
        ThreadUtil.getInstance().execute("异步发送系统通知", () -> {
            DataService.getMainDB().beginTransaction(connection -> sendSysNoticeTransaction(connection, receiver_uid, title, content, path));
        });
    }

    /**
     * 发送系统通知
     *
     * @param receiver_uid 接收信息的用户ID
     * @param title        消息标题
     * @param content      消息内容
     * @return 同步结果
     */
    public SyncResult sendSysNotice(long receiver_uid, String title, String content) {
        return DataService.getMainDB().beginTransaction(connection -> sendSysNoticeTransaction(connection, receiver_uid, title, content, ""));
    }

    /**
     * 发送系统通知
     *
     * @param receiver_uid 接收信息的用户ID
     * @param title        消息标题
     * @param content      消息内容
     * @param path         路径
     * @return 同步结果
     */
    public SyncResult sendSysNotice(long receiver_uid, String title, String content, String path) {
        return DataService.getMainDB().beginTransaction(connection -> {
            return sendSysNoticeTransaction(connection, receiver_uid, title, content, path);
        });
    }

    /**
     * 发送系统通知
     *
     * @param receiver_uid 接收信息的用户ID
     * @param title        消息标题
     * @param content      消息内容
     * @return 同步结果
     */
    public SyncResult sendSysNoticeTransaction(Connection connection, long receiver_uid, String title, String content) {
        return sendSysNoticeTransaction(connection, receiver_uid, title, content, "");
    }

    /**
     * 发送系统通知
     *
     * @param receiver_uid 接收信息的用户ID
     * @param title        消息标题
     * @param content      消息内容
     * @param path         路径
     * @return 同步结果
     */
    public SyncResult sendSysNoticeTransaction(Connection connection, long receiver_uid, String title, String content, String path) {
        try {
            SysMessageBodyEntity messageBodyEntity = SysMessageBodyEntity.getInstance();
            messageBodyEntity.title = title;
            messageBodyEntity.content = content;
            messageBodyEntity.typeId = 1;//消息类型，目前只有系统消息
            messageBodyEntity.path = path;
            messageBodyEntity.create_time = TimeUtil.getTimestamp();
            messageBodyEntity.id = messageBodyEntity.insertGetIdTransaction(connection);
            if (messageBodyEntity.id == 0) return new SyncResult(700, "创建消息失败");

            SysMessageEntity messageEntity = SysMessageEntity.getInstance();
            messageEntity.sender_uid = 0;//系统为0
            messageEntity.receiver_uid = receiver_uid;
            messageEntity.message_id = messageBodyEntity.id;
            messageEntity.status = 0;//状态：0=未查看，1=已查看
            messageEntity.create_time = TimeUtil.getTimestamp();
            messageEntity.id = messageEntity.insertGetIdTransaction(connection);
            if (messageEntity.id == 0) return new SyncResult(701, "发送消息失败");

            DataService.getMainCache().del(String.format("User:%s:SysMessage:UnreadCount", receiver_uid));
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "发送系统消息失败");
        }
        return new SyncResult(1, "发送失败");
    }

    /**
     * 读取用户未阅读的消息通知数
     *
     * @param uid 用户ID
     * @return
     */
    public int unreadCount(long uid) {
        int daysAgo = SysGlobalConfigEntity.getInt("SysMesssage:Unread:DaysAgo", 90);
        return this.cache(String.format("User:%s:SysMessage:UnreadCount", uid))
                .where("receiver_uid", uid)
                .where("status", 0)
                .where("create_time", ">=", TimeUtil.getTime00(-daysAgo))
                .count();
    }

    /**
     * 根据停止原因发送用户通知（暂时使用）
     *
     * @param stopReasonCode 停止原因code
     * @param orderEntity    订单信息
     */
    public void sendChargeNoticeToUserWithStopReasonCode(int stopReasonCode, @NonNull ChargeOrderEntity orderEntity) {
        ThreadUtil.getInstance().execute(() -> {
            String title = "停止充电通知";
            String message = "";
            //根据停止原因发送用户通知
            switch (stopReasonCode) {
                case 6://负载过大
                case 8://动态过载
                    title = "充电功过载通知";
//                    message = String.format("亲爱的用户，系统检测您的爱车充电功率达到%sW，为了您的爱车安全，系统将停止供电，请悉知。%s。如有疑问请联系客服微信处理。"
//                            , orderEntity.maxPower
//                            , "（元气充提示您：可申请升级超级快充，或修改车辆的最大功率：个人中心》爱车管理》选择爱车》修改充电功率》选择充电器可充的最大功率）");
                    message = String.format("亲爱的用户，系统检测您的爱车充电功率达到%sW，为了您的爱车安全，系统将停止供电，请悉知。%s。如有疑问请联系客服微信处理。"
                            , orderEntity.maxPower
                            , "（元气充提示您：扫码充电时在 [收费标准] 中选择大功率充电");
                    break;
                case 7://停止充电通知
                    message = "亲爱的用户，系统检测充电过程中有可能发生危险，为了您的爱车安全，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 9://功率过小
                    message = "亲爱的用户，因您的爱车充电功率过小，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 10://环境温度过高
                case 11://端口温度过高
                    title = "危险充电通知";
                    message = "亲爱的用户，系统检测充电过程中有温度过高的情况，您的爱车充电有可能发生危险，为了您的爱车安全，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 12://过流
                    title = "危险充电通知";
                    message = "亲爱的用户，系统检测充电过程中有过流的情况，您的爱车充电有可能发生危险，为了您的爱车安全，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 14://无功率停止，可能是接触不良或保险丝烧断故障
                case 15://预检-继电器坏或保险丝断
                    message = "亲爱的用户，因插座发生故障，您的爱车充电有可能发生危险，为了您的爱车安全，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 16://水浸断电
                case 17://灭火结算（本端口）
                case 18://灭火结算（非本端口）
                    title = "危险充电通知";
                    message = "亲爱的用户，系统检测充电过程中有危险充电的情况发生，您的爱车充电有可能发生危险，为了您的爱车安全，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 20://未关好柜门
                    message = "亲爱的用户，因您没有关好充电柜柜门，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 21://外部操作打开柜门
                case 1://充满自停
                case 2://达到最大充电时间
                case 3://达到预设时间
                case 4://达到预设电量
                case 5://用户主动拔出插头
                case 13://用户主动拔出插头，可能是插座弹片卡住
                case 19://用户密码开柜断电
//                    title = "完成充电通知";
//                    message = String.format("亲爱的用户，恭喜您完成本次充电");
                    return;
                default:
                    return;
            }
            //发送站内通知给用户
            SysMessageEntity.getInstance().sendSysNotice(orderEntity.uid, title, message);
        });
    }

    /**
     * 根据停止原因发送用户通知（暂时使用）
     *
     * @param stopReasonCode 停止原因code
     * @param orderEntity    订单信息
     */
    public void sendChargeNoticeToUserWithStopReasonCode(int stopReasonCode, @NonNull ChargeOrderV3Entity orderEntity) {
        ThreadUtil.getInstance().execute(() -> {
            String title = "停止充电通知";
            String message = "";
            //根据停止原因发送用户通知
            switch (stopReasonCode) {
                case 6://负载过大
                case 8://动态过载
                    title = "充电功过载通知";
//                    message = String.format("亲爱的用户，系统检测您的爱车充电功率达到%sW，为了您的爱车安全，系统将停止供电，请悉知。%s。如有疑问请联系客服微信处理。"
//                            , orderEntity.maxPower
//                            , "（元气充提示您：可申请升级超级快充，或修改车辆的最大功率：个人中心》爱车管理》选择爱车》修改充电功率》选择充电器可充的最大功率）");
                    message = String.format("亲爱的用户，系统检测您的爱车充电功率达到%sW，为了您的爱车安全，系统将停止供电，请悉知。%s。如有疑问请联系客服微信处理。"
                            , orderEntity.maxPower
                            , "（元气充提示您：扫码充电时在 [收费标准] 中选择大功率充电");
                    break;
                case 7://停止充电通知
                    message = "亲爱的用户，系统检测充电过程中有可能发生危险，为了您的爱车安全，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 9://功率过小
                    message = "亲爱的用户，因您的爱车充电功率过小，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 10://环境温度过高
                case 11://端口温度过高
                    title = "危险充电通知";
                    message = "亲爱的用户，系统检测充电过程中有温度过高的情况，您的爱车充电有可能发生危险，为了您的爱车安全，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 12://过流
                    title = "危险充电通知";
                    message = "亲爱的用户，系统检测充电过程中有过流的情况，您的爱车充电有可能发生危险，为了您的爱车安全，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 14://无功率停止，可能是接触不良或保险丝烧断故障
                case 15://预检-继电器坏或保险丝断
                    message = "亲爱的用户，因插座发生故障，您的爱车充电有可能发生危险，为了您的爱车安全，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 16://水浸断电
                case 17://灭火结算（本端口）
                case 18://灭火结算（非本端口）
                    title = "危险充电通知";
                    message = "亲爱的用户，系统检测充电过程中有危险充电的情况发生，您的爱车充电有可能发生危险，为了您的爱车安全，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 20://未关好柜门
                    message = "亲爱的用户，因您没有关好充电柜柜门，系统将停止供电，请悉知，如有疑问请联系客服微信处理。";
                    break;
                case 21://外部操作打开柜门
                case 1://充满自停
                case 2://达到最大充电时间
                case 3://达到预设时间
                case 4://达到预设电量
                case 5://用户主动拔出插头
                case 13://用户主动拔出插头，可能是插座弹片卡住
                case 19://用户密码开柜断电
//                    title = "完成充电通知";
//                    message = String.format("亲爱的用户，恭喜您完成本次充电");
                    return;
                default:
                    return;
            }
            //发送站内通知给用户
            SysMessageEntity.getInstance().sendSysNotice(orderEntity.uid, title, message);
        });
    }
}
