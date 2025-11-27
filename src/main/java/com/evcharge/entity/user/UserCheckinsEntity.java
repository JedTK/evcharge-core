package com.evcharge.entity.user;


import com.evcharge.entity.active.integral.ActiveIntegralTempConfigV1Entity;
import com.evcharge.entity.active.integral.ActiveIntegralTempV1Entity;
import com.evcharge.entity.basedata.CheckInsConfigEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.enumdata.EUserIntegralType;
import com.evcharge.libsdk.wechat.WechatSubscribeTmplSDK;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.*;

/**
 * 用户签到表;
 *
 * @author : Jay
 * @date : 2024-1-2
 */
public class UserCheckinsEntity extends BaseEntity implements Serializable {
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
     * 观看广告id
     */
    public long log_id;
    /**
     * 签到时间
     */
    public long checkin_time;
    /**
     * 签到时间(日期)
     */
    public String checkin_date;
    /**
     * 签到排序
     */
    public int checkin_index;
    /**
     * 获取积分数
     */
    public int get_integral;
    /**
     * 充电桩id
     */
    public long cs_id;
    /**
     * 设备号
     */
    public String device_code;
    /**
     * 端口号
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
     *
     * @return
     */
    public static UserCheckinsEntity getInstance() {
        return new UserCheckinsEntity();
    }

    /**
     * 检查今天是否签到
     *
     * @param uid
     * @return
     */
    public SyncResult checkForToday(long uid) {
        //获取当前时间
        long currentTime = TimeUtil.toTimestamp(TimeUtil.toTimeString(Calendar.getInstance().getTimeInMillis(), "yyyy-MM-dd"), "yyyy-MM-dd");

        UserCheckinsEntity userCheckinsEntity = this
                .where("checkin_time", currentTime)
                .where("uid", uid)
                .cache(String.format("User:%s:Checkin:%s", uid, currentTime))
                .findEntity();

        if (userCheckinsEntity == null) return new SyncResult(1, "暂未签到");

        return new SyncResult(0, "你已经签到，请勿重复操作");
    }

    /**
     * 获取用户连续签到天数
     *
     * @param uid
     * @return
     */
    public int getConsecutiveDays(long uid) {
        //获取最新一条签到记录
        long currentTime = TimeUtil.toTimestamp(TimeUtil.toTimeString(Calendar.getInstance().getTimeInMillis(), "yyyy-MM-dd"), "yyyy-MM-dd");
        //获取昨天签到，如果没有数据，则是断签
        // currentTime = currentTime - 86400 * 1000;
        UserCheckinsEntity userCheckinsEntity = UserCheckinsEntity.getInstance()
                .where("uid", uid)
//                .where("checkin_time", currentTime)
                .order("checkin_time desc")
                .cache(String.format("User:%s:Checkin:LastDay:%s", uid, currentTime), 3600 * 1000)
                .findEntity();
        int checkIndex = 0;
        if (userCheckinsEntity != null) {
//            return 0;
            //判断今天是否签到
            if (currentTime == userCheckinsEntity.checkin_time) {
                checkIndex = userCheckinsEntity.checkin_index;

                if (userCheckinsEntity.checkin_index >= 7) {
                    checkIndex = 0;
                }

            } else if ((currentTime - 86400000) == (userCheckinsEntity.checkin_time)) { //是不是昨天已经签到了
                checkIndex = userCheckinsEntity.checkin_index;
                if (userCheckinsEntity.checkin_index >= 7) {
                    checkIndex = 0;
                }
            }
        }

        return checkIndex;

    }

    /**
     * 签到
     *
     * @param uid
     * @return
     */
    public SyncResult checkin(long uid) {
        return checkin(uid, 0, 0, "", 0);
    }


    /**
     * 签到
     *
     * @param uid
     * @return
     */
    public SyncResult checkin(long uid, long logId, long csId, String deviceCode, int port) {
        //查询今天是否已经签到了
        long currentTime = TimeUtil.toTimestamp(TimeUtil.toTimeString(Calendar.getInstance().getTimeInMillis(), "yyyy-MM-dd"), "yyyy-MM-dd");
        SyncResult checkRes = checkForToday(uid);

        //已经签到了
        if (checkRes.code == 0) return new SyncResult(1, checkRes.msg);

        if (logId == 0) {
            return new SyncResult(1, "1.签到失败，请稍后再试");
        }

        int checkIndex = 1;
        //获取最新一条签到记录
        UserCheckinsEntity userCheckinsEntity = UserCheckinsEntity.getInstance()
                .where("uid", uid)
                .order("checkin_time desc")
                .findEntity();
        //如果有签到记录
        if (userCheckinsEntity != null) {
            //获取今天时间戳
            //如果相等，证明是连续签到
            if ((currentTime - 86400 * 1000 == (userCheckinsEntity.checkin_time)) && userCheckinsEntity.checkin_index < 7) {
                checkIndex = userCheckinsEntity.checkin_index + 1;
            }
        }
        //获取签到配置
        CheckInsConfigEntity checkInsConfigEntity = CheckInsConfigEntity.getInstance().getConfigBySort(checkIndex);
        ActiveIntegralTempV1Entity activeIntegralTempV1Entity;
        if (checkInsConfigEntity == null) return new SyncResult(1, "签到配置不存在");

        if (checkInsConfigEntity.integral_temp_id != 0) {
            activeIntegralTempV1Entity = ActiveIntegralTempV1Entity.getInstance().getRuleById(checkInsConfigEntity.integral_temp_id);
            if (activeIntegralTempV1Entity == null) {
                return new SyncResult(2, "积分配置不存在");
            }


        } else {
            activeIntegralTempV1Entity = null;
        }
        Map<String, Object> checkinData = new LinkedHashMap<>();

        checkinData.put("uid", uid);
        checkinData.put("log_id", logId);
        checkinData.put("checkin_time", currentTime);
        checkinData.put("checkin_date", TimeUtil.toTimeString(currentTime, "yyyy-MM-dd"));
        checkinData.put("checkin_index", checkIndex);
        if (activeIntegralTempV1Entity != null) {
            checkinData.put("get_integral", activeIntegralTempV1Entity.integral);
        }
        checkinData.put("cs_id", csId);
        checkinData.put("device_code", deviceCode);
        checkinData.put("port", port);
        checkinData.put("create_time", TimeUtil.getTimestamp());

        return DataService.getMainDB().beginTransaction(connection -> {
            long id = this.insertGetIdTransaction(connection, checkinData);
            if (id == 0) {
                return new SyncResult(1, "更新签到数据失败");
            }
            if (activeIntegralTempV1Entity != null) {
                //计算过期时间
                long expiredTime = 0;
                ActiveIntegralTempConfigV1Entity activeIntegralTempConfigV1Entity = ActiveIntegralTempConfigV1Entity.getInstance().getCouponConfigV1ById(activeIntegralTempV1Entity.setting_id);
                //有效期限制：0-无期限，1-固定日期，2-领取积分当日开始N天内有效
                if (activeIntegralTempConfigV1Entity.expired_stint == 0 || activeIntegralTempConfigV1Entity.expired_stint == 1) {
                    expiredTime = activeIntegralTempV1Entity.end_time;
                }

                if (activeIntegralTempConfigV1Entity.expired_stint == 2) {
                    expiredTime = currentTime + (activeIntegralTempV1Entity.n_day * 86400L * 1000);
                }

                SyncResult r = UserIntegralDetailEntity.getInstance().addIntegral(uid
                        , activeIntegralTempV1Entity.integral
                        , EUserIntegralType.Checkin
                        , id
                        , expiredTime
                        , "签到获取积分"
                );

                if (r.code != 0) return r;
            }
            DataService.getMainCache().del(String.format("User:%s:Checkin:LastDay:%s", uid, currentTime));
            return new SyncResult(0, "success");
        });
    }


    /**
     * 执行任务
     */
    public void runWechatSubscribeMsgTask() {
        //获取当天需要执行的任务
        //获取当天时间
        //Wechat:App:CheckinNotice:TemplateId
        String templateId = SysGlobalConfigEntity.getString("Wechat:App:CheckinNotice:TemplateId");
        long currentTime = TimeUtil.toTimestamp(TimeUtil.toTimeString(Calendar.getInstance().getTimeInMillis(), "yyyy-MM-dd"), "yyyy-MM-dd");
//        TimeUtil.getTimeString("yyyy-MM-dd");
        List<UserReceiveWechatMsgLogEntity> list = UserReceiveWechatMsgLogEntity.getInstance()
                .where("tmpl_id", templateId)
                .where("run_time", currentTime)
                .where("status", 1)
                .selectList();
        if (list.isEmpty()) return;
        try {
            DataService.getMainDB().beginTransaction(connection -> {
                for (UserReceiveWechatMsgLogEntity nd : list) {
                    SyncResult r = sendWechatSubscribeMsg(nd.uid);
                    if (r.code == 0) {
                        UserReceiveWechatMsgLogEntity.getInstance().updateLogStatusTransaction(connection, nd.id, 2);
                    } else {
                        UserReceiveWechatMsgLogEntity.getInstance().updateLogStatusTransaction(connection, nd.id, -1);

                    }
                }
                return new SyncResult(0, "success");
            });

        } catch (Exception e) {
            LogsUtil.info(this.getClass().getSimpleName(), e.getMessage());
        }
    }


    /**
     * 发送消息推送
     *
     * @param uid
     * @return
     */
    public SyncResult sendWechatSubscribeMsg(long uid) {
        long currentTime = TimeUtil.toTimestamp(TimeUtil.toTimeString(Calendar.getInstance().getTimeInMillis(), "yyyy-MM-dd"), "yyyy-MM-dd");

        UserSourceInfoEntity userSourceInfoEntity = UserSourceInfoEntity.getInstance().getInfo(uid);
        if (userSourceInfoEntity == null) {
            return new SyncResult(1, "用户信息不存在");
        }

        Map<String, Object> msgData = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();

        WechatSubscribeTmplSDK wechatSubscribeTmplSDK = new WechatSubscribeTmplSDK();
        int integral=0;
        int checkIndex = 1;
        String integralText="签到领积分";

        //获取用户最新签到

        UserCheckinsEntity userCheckinsEntity = UserCheckinsEntity.getInstance()
                .where("uid", uid)
                .order("checkin_time desc")
                .findEntity();

        //如果有签到记录
        if (userCheckinsEntity != null) {
            if(userCheckinsEntity.checkin_time==create_time){
                return new SyncResult(1, "用户已签到，无需提醒");
            }
            //获取今天时间戳
            //如果相等，证明是连续签到
            if ((currentTime - 86400 * 1000 == (userCheckinsEntity.checkin_time)) && userCheckinsEntity.checkin_index < 7) {
                checkIndex = userCheckinsEntity.checkin_index + 1;
            }
        }

        CheckInsConfigEntity checkInsConfigEntity = CheckInsConfigEntity.getInstance().getConfigBySort(checkIndex);

        ActiveIntegralTempV1Entity activeIntegralTempV1Entity= ActiveIntegralTempV1Entity.getInstance().getRuleById(checkInsConfigEntity.integral_temp_id);

        if(activeIntegralTempV1Entity!=null){
            integral=activeIntegralTempV1Entity.integral;
        }

        if(integral!=0){
             integralText=String.format("签到领%s积分",integral);

        }
        //short_thing1={OrderSN}&time2={ChargeStandardName}&thing3={totalAmount}&thing4={ChargeTime}&thing9={stopReasonText}
        data.put("msgTitle", "签到通知");
        data.put("checkinTime", TimeUtil.toTimeString("yyyy-MM-dd"));
        data.put("tips", "连续签到获取积分");
        data.put("integralText", integralText);
        String templateData = "short_thing1={msgTitle}&time2={checkinTime}&thing3={tips}&thing4={integralText}";

        msgData = wechatSubscribeTmplSDK.createTemplateData(templateData, data);
        return wechatSubscribeTmplSDK.sendSubscribeMessage(
                userSourceInfoEntity.open_id
                , "/pages_user/user_checkin/index"
                , "GgH2u1z0s7WshPFWNnv09mjaIRxuvLIXAUkTCk-rozs"
                , msgData
        );
    }





}