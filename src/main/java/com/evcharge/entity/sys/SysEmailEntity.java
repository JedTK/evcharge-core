package com.evcharge.entity.sys;

import com.evcharge.entity.order.ChargeOrderV3Entity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.user.UserEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件
 */
public class SysEmailEntity extends BaseEntity implements Serializable {

    private JavaMailSenderImpl mSender;
    private String senderUserName;

    private static SysEmailEntity _this;

    /**
     * 获得一个实例
     */
    public static SysEmailEntity getInstance() {
        if (_this == null) {
            _this = new SysEmailEntity();
        }
        return _this;
    }

    /**
     * 初始化配置
     */
    private boolean init() {
        try {
            if (mSender == null) {
                List<Map<String, Object>> config = initCache().getObj("System:Email:Config");
                if (config == null || config.size() == 0) {
                    config = this.where("status", 1).select();
                    if (config.size() > 0) initCache().setObj("System:Email:Config", config, 86400000 * 7);
                }
                if (config == null || config.size() == 0) {
                    LogsUtil.error("SysEmail", "Email初始化出错，没有找到对应的配置");
                    return false;
                }

                mSender = new JavaMailSenderImpl();
                Properties mailProperties = new Properties();
                Iterator it = config.iterator();
                while (it.hasNext()) {
                    Map<String, Object> data = (Map<String, Object>) it.next();
                    String name = MapUtil.getString(data, "name");
                    String value = MapUtil.getString(data, "value");

                    if ("host".equalsIgnoreCase(name)) {
                        mSender.setHost(value);
                    } else if ("port".equalsIgnoreCase(name)) {
                        mSender.setPort(Convert.toInt(value));
                    } else if ("username".equalsIgnoreCase(name)) {
                        mSender.setUsername(value);
                        senderUserName = value;
                    } else if ("password".equalsIgnoreCase(name)) {
                        mSender.setPassword(value);
                    } else {
                        mailProperties.setProperty(name, value);
                    }
                }
                mSender.setJavaMailProperties(mailProperties);
                return true;
            }
        } catch (Exception e) {
            LogsUtil.error(e, "SysEmail", "Email初始化出错");
        }
        return false;
    }

    /**
     * 发送简单文字邮件
     *
     * @param toEmailList 发送给目标Email地址，多个用英文逗号分隔
     * @param subject     标题
     * @param text        内容
     */
    public SyncResult sendText(String toEmailList, String subject, String text) {
        if (!StringUtils.hasLength(toEmailList)) return new SyncResult(3000, "邮箱列表为空");
        if (!StringUtils.hasLength(subject)) return new SyncResult(3001, "标题不能为空");
        if (!StringUtils.hasLength(text)) return new SyncResult(3002, "内容不能为空");

        if (init()) {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom(senderUserName);
            simpleMailMessage.setTo(toEmailList.split(","));
            simpleMailMessage.setSubject(subject);
            simpleMailMessage.setText(text);
            mSender.send(simpleMailMessage);
        }
        return new SyncResult(0, "");
    }

    /**
     * 异步多线程发送简单文字邮件
     *
     * @param toEmailList 发送给目标Email地址，多个用英文逗号分隔
     * @param subject     标题
     * @param text        内容
     */
    public SyncResult sendTextAsyn(String toEmailList, String subject, String text) {
        if (!StringUtils.hasLength(toEmailList)) return new SyncResult(3000, "邮箱列表为空");
        if (!StringUtils.hasLength(subject)) return new SyncResult(3001, "标题不能为空");
        if (!StringUtils.hasLength(text)) return new SyncResult(3002, "内容不能为空");

        ThreadUtil.getInstance().execute("", () -> {
            if (init()) {
                SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
                simpleMailMessage.setFrom(senderUserName);
                simpleMailMessage.setTo(toEmailList.split(","));
                simpleMailMessage.setSubject(subject);
                simpleMailMessage.setText(text);
                mSender.send(simpleMailMessage);
            }
        });
        return new SyncResult(0, "");
    }

    /**
     * 发送开发者通知
     */
    public void sendDevNotice(String subject, String text, Object... args) {
        sendDevNotice(null, subject, text, args);
    }

    /**
     * 发送开发者通知
     */
    public void sendDevNotice(Exception e, String subject, String text, Object... args) {
        String toEmailList = SysGlobalConfigEntity.getString("DeveloperEmail");
        if (!StringUtils.hasLength(toEmailList)) return;

        if (StringUtils.hasLength(text) && (args != null && args.length > 0)) {
            text = String.format(text, args);
        }
        if (e != null) {
            text += String.format("\r\n[异常信息]：%s \r\n[堆栈调用]：", e.getMessage());

            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            Throwable cause = e.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            printWriter.close();
            text += String.format("%s", writer);
        }
        sendTextAsyn(toEmailList, subject, text);
    }

    /**
     * 根据停止原因发送管理员通知（暂时使用）
     *
     * @param stopReasonCode 停止原因code
     * @param orderEntity    订单信息
     */
    public void sendWarnToAdminWithStopReasonCode(int stopReasonCode, @NonNull ChargeOrderEntity orderEntity) {
        ThreadUtil.getInstance().execute(() -> {
            Map<String, Object> user_data = UserEntity.getInstance()
                    .field("id,nickname,phone")
                    .where("id", orderEntity.uid)
                    .find();
            String nickname = MapUtil.getString(user_data, "nickname");
            String phone = MapUtil.getString(user_data, "phone");

            String title = "危险充电通知";
            String message = String.format("订单号：%s\r\n用户ID：%s\r\n用户昵称：%s\n联系手机：%s\n充电功率：%s\r\n"
                    , orderEntity.OrderSN
                    , orderEntity.uid
                    , nickname
                    , phone
                    , orderEntity.maxPower);

            switch (stopReasonCode) {
                case 6://负载过大
                case 8://动态过载
                    title = "充电功过载通知";
                    message += "用户充电功率过载，系统已停止本次充电，可以建议用户升级为超级快充，或修改车辆的最大功率：个人中心》爱车管理》选择爱车》修改充电功率》选择充电器可充的最大功率";
                    break;
                case 7://停止充电通知
                    message += "系统检查到有危险充电的可能，系统主动停止本次充电";
                    break;
                case 9://功率过小
                    return;
                case 10://环境温度过高
                case 11://端口温度过高
                    message += "系统检测充电过程中环境或端口温度过高，设备主动停止本次充电";
                    break;
                case 12://过流
                    message += "系统检测充电过程中有过流的情况，设备主动停止本次充电";
                    break;
                case 14://无功率停止，可能是接触不良或保险丝烧断故障
                case 15://预检-继电器坏或保险丝断
                    title = "设备故障通知";
                    message += "设备有可能发生故障，设备主动停止本次充电";
                    break;
                case 16://水浸断电
                    message += "设备因水浸断电，设备主动停止本次充电";
                    break;
                case 17://灭火结算（本端口）
                case 18://灭火结算（非本端口）
                    message += "灭火结算，设备主动停止本次充电";
                    break;
                case 20://未关好柜门
                    return;
                case 21://外部操作打开柜门
                case 1://充满自停
                case 2://达到最大充电时间
                case 3://达到预设时间
                case 4://达到预设电量
                case 5://用户主动拔出插头
                case 13://用户主动拔出插头，可能是插座弹片卡住
                case 19://用户密码开柜断电
                default:
                    return;
            }

            SysEmailEntity.getInstance().sendText(SysGlobalConfigEntity.getString("ChargeWarningEmailList"), title, message);
        });
    }

    /**
     * 根据停止原因发送管理员通知（暂时使用）
     *
     * @param stopReasonCode 停止原因code
     * @param orderEntity    订单信息
     */
    public void sendWarnToAdminWithStopReasonCode(int stopReasonCode, @NonNull ChargeOrderV3Entity orderEntity) {
        ThreadUtil.getInstance().execute(() -> {
            Map<String, Object> user_data = UserEntity.getInstance()
                    .field("id,nickname,phone")
                    .where("id", orderEntity.uid)
                    .find();
            String nickname = MapUtil.getString(user_data, "nickname");
            String phone = MapUtil.getString(user_data, "phone");

            String title = "危险充电通知";
            String message = String.format("订单号：%s\r\n用户ID：%s\r\n用户昵称：%s\n联系手机：%s\n充电功率：%s\r\n"
                    , orderEntity.OrderSN
                    , orderEntity.uid
                    , nickname
                    , phone
                    , orderEntity.maxPower);

            switch (stopReasonCode) {
                case 6://负载过大
                case 8://动态过载
                    title = "充电功过载通知";
                    message += "用户充电功率过载，系统已停止本次充电，可以建议用户升级为超级快充，或修改车辆的最大功率：个人中心》爱车管理》选择爱车》修改充电功率》选择充电器可充的最大功率";
                    break;
                case 7://停止充电通知
                    message += "系统检查到有危险充电的可能，系统主动停止本次充电";
                    break;
                case 9://功率过小
                    return;
                case 10://环境温度过高
                case 11://端口温度过高
                    message += "系统检测充电过程中环境或端口温度过高，设备主动停止本次充电";
                    break;
                case 12://过流
                    message += "系统检测充电过程中有过流的情况，设备主动停止本次充电";
                    break;
                case 14://无功率停止，可能是接触不良或保险丝烧断故障
                case 15://预检-继电器坏或保险丝断
                    title = "设备故障通知";
                    message += "设备有可能发生故障，设备主动停止本次充电";
                    break;
                case 16://水浸断电
                    message += "设备因水浸断电，设备主动停止本次充电";
                    break;
                case 17://灭火结算（本端口）
                case 18://灭火结算（非本端口）
                    message += "灭火结算，设备主动停止本次充电";
                    break;
                case 20://未关好柜门
                    return;
                case 21://外部操作打开柜门
                case 1://充满自停
                case 2://达到最大充电时间
                case 3://达到预设时间
                case 4://达到预设电量
                case 5://用户主动拔出插头
                case 13://用户主动拔出插头，可能是插座弹片卡住
                case 19://用户密码开柜断电
                default:
                    return;
            }

            SysEmailEntity.getInstance().sendText(SysGlobalConfigEntity.getString("ChargeWarningEmailList"), title, message);
        });
    }
}
