package com.evcharge.entity.user;


import com.evcharge.entity.active.integral.ActiveIntegralTempConfigV1Entity;
import com.evcharge.entity.active.integral.ActiveIntegralTempV1Entity;
import com.evcharge.enumdata.EUserIntegralType;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户积分明细表;
 *
 * @author : Jay
 * @date : 2023-11-30
 */
public class UserIntegralDetailEntity extends BaseEntity implements Serializable {
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
     * 类型  1=系统执行任务  2=充电扣积分 3=活动获取 4
     */
    public int type_id;
    /**
     * 事件id 不同的类型不同的事件id
     */
    public long event_id;
    /**
     * 获得的积分
     */
    public int change_integral;
    /**
     * 过期时间
     */
    public long expired_time;
    /**
     * 状态 -1=已过期 1=已使用 这个状态针对用户获取积分的过期和使用状态管理
     */
    public int status;
    /**
     * 测试id
     */
    public long test_id;
    /**
     * 备注
     */
    public String memo;
    /**
     * 额外参数
     */
    public String extra_data;
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
    public static UserIntegralDetailEntity getInstance() {
        return new UserIntegralDetailEntity();
    }

    /**
     * 获取用户过期的积分，默认7天
     *
     * @param uid
     * @return
     */
    public long getIsExpiredIntegral(long uid) {
        return getIsExpiredIntegral(uid, 7);
    }

    /**
     * 获取用户即将过期的积分
     *
     * @param uid
     * @param expireDay
     * @return
     */
    public long getIsExpiredIntegral(long uid, int expireDay) {
        long currentTimestamp = TimeUtil.getTimestamp();

        String sql = String.format("select sum(change_integral) as change_integral from UserIntegralDetail where uid = %s and status=0 and expired_time BETWEEN %s and %s", uid, currentTimestamp, currentTimestamp * 86400 * expireDay);
        List<Map<String, Object>> userIntegralTemp = this.query(sql);

        return MapUtil.getLong(userIntegralTemp.get(0), "change_integral");
//        return this.where("uid", uid)
//                .where("expired_time", ">", 0)
//                .where("expired_time", "BETWEEN", "UNIX_TIMESTAMP() and (UNIX_TIMESTAMP() + " + expireDay * 86400 * 1000 + ")")
//                .where("change_integral", ">", 0)
//                .where("status", 0)
//                .sum("change_integral");
    }


    /**
     * 领取活动积分
     * @param uid
     * @param ruleKey
     * @return
     */
    public SyncResult receiveActiveIntegral(long uid, String ruleKey,long eventId) {
        return receiveActiveIntegral(uid, ruleKey, EUserIntegralType.Active,eventId,"");
    }
    public SyncResult receiveActiveIntegral(long uid, String ruleKey,int typeId,String memo) {
        return receiveActiveIntegral(uid, ruleKey, typeId,0,memo);
    }

    /**
     * 领取活动积分
     * @param uid
     * @param ruleKey
     * @param typeId
     * @return
     */
    public SyncResult receiveActiveIntegral(long uid, String ruleKey,int typeId,long eventId,String memo) {
        ActiveIntegralTempV1Entity activeIntegralTempV1Entity = ActiveIntegralTempV1Entity.getInstance().getRuleByKey(ruleKey);
        if (activeIntegralTempV1Entity == null) return new SyncResult(1, "请检查优惠券规则配置");
        if (activeIntegralTempV1Entity.status == 1) return new SyncResult(2, "此优惠活动已关闭");
        ActiveIntegralTempConfigV1Entity activeIntegralTempConfigV1Entity = ActiveIntegralTempConfigV1Entity.getInstance().getCouponConfigV1ById(activeIntegralTempV1Entity.setting_id);
        //数量领取限制：0-不限制，1-指定领取数量
        long count = 0;
        long ruleId = activeIntegralTempV1Entity.id;
        if (activeIntegralTempConfigV1Entity.count_get_stint == 1) {
            count = this.where("uid", uid)
                    .where("type_id", typeId)
                    .where("event_id", ruleId).count();
            if (count >= activeIntegralTempV1Entity.can_get_count) {
                return new SyncResult(1, String.format("每个用户限领%s张", activeIntegralTempV1Entity.can_get_count));
            }
        }
        //发放总量：0-不限制，1-限制
        if (activeIntegralTempConfigV1Entity.count_stint == 1) {
            count = this.where("type_id", typeId)
                    .where("event_id", ruleId).count();
            if (count > activeIntegralTempV1Entity.count) {
                return new SyncResult(1, "优惠券已领取完毕");
            }
        }


        long expiredTime = 0;
        long startTime = TimeUtil.getTimestamp();
        //有效期限制：0-无期限，1-固定日期，2-领取卷当日开始N天内有效，3-领到卷次日开始N天内有效
        switch (activeIntegralTempConfigV1Entity.expired_stint) {
            case 0:
                expiredTime = 4102415999000L; //2099-12-31 23:59:59
                // endTime = 0; //2033-12-31 00:00:00
                break;
            case 1: //固定日期
                expiredTime = activeIntegralTempV1Entity.end_time;
                break;
            case 2:
                expiredTime = TimeUtil.getAddDayTimestamp(startTime, activeIntegralTempV1Entity.n_day);
                break;
            case 3:
                expiredTime = TimeUtil.getAddDayTimestamp(startTime + 86400 * 1000, activeIntegralTempV1Entity.n_day);
                break;

        }
//        Map<String, Object> data = new LinkedHashMap<>();
//
//        data.put("uid",uid);
//        data.put("type_id",typeId);
//        data.put("event_id",activeIntegralTempV1Entity.id);
//        data.put("change_integral",activeIntegralTempV1Entity.integral);
//        data.put("expired_time",expiredTime);
//        data.put("status",0);
//        data.put("memo","");
//        data.put("create_time",TimeUtil.getTimestamp());

        long finalExpiredTime = expiredTime;
        return DataService.getMainDB().beginTransaction(connection -> {
//           if(this.insertTransaction(connection,data)==0){
//               return new SyncResult(1,"领取积分失败");
//           }

            return addIntegralTransaction(connection
                    ,uid
                    ,activeIntegralTempV1Entity.integral
                    ,typeId
                    ,memo
                    ,finalExpiredTime
                    ,eventId
                    ,null
                    ,0
                    );
        });

    }

    /**
     * 处理过期积分
     *
     * @param id
     * @return
     */
    public SyncResult dealExpiredIntegral(long id) {
        return DataService.getMainDB().beginTransaction(connection -> {
            return dealExpiredIntegralTransaction(connection, id, "");
        });
    }

    /**
     * 处理过期积分
     *
     * @param id
     * @param memo
     * @return
     */
    public SyncResult dealExpiredIntegral(long id, String memo) {
        return DataService.getMainDB().beginTransaction(connection -> {
            return dealExpiredIntegralTransaction(connection, id, memo);
        });
    }

    /**
     * 处理过期积分
     *
     * @param connection
     * @param id
     * @param memo
     * @return
     */
    public SyncResult dealExpiredIntegralTransaction(Connection connection, long id, String memo) {
        try {
            UserIntegralDetailEntity userIntegralDetailEntity = UserIntegralDetailEntity.getInstance().where("id", id).findEntityTransaction(connection);
            if (userIntegralDetailEntity.status != 0) {
                return new SyncResult(1, "积分状态不正确");
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", -1);
            data.put("memo", memo);
            data.put("update_time", TimeUtil.getTimestamp());
            if (this.where("id", id).updateTransaction(connection, data) == 0) {
                return new SyncResult(1, "更新过期积分失败");
            }
            
            int chargeIntegral = -userIntegralDetailEntity.change_integral;
            Map<String, Object> extraData = new LinkedHashMap<>();
            extraData.put("detail_id", userIntegralDetailEntity.id);
            SyncResult r = UserIntegralLogEntity.getInstance().updateIntegralTransaction(connection, userIntegralDetailEntity.uid, chargeIntegral, 1, "已过期积分更新状态", 0, 0, extraData, 0);
            if (r.code != 0) {
                return r;
            }
            return new SyncResult(0, "更新成功");
        } catch (Exception e) {
            return new SyncResult(1, e.getMessage());
        }
    }

    /**
     * 新增用户积分余额
     *
     * @param uid
     * @param changeIntegral
     * @param updateType
     * @param memo
     * @param expiredTime
     * @param eventId
     * @return
     */
    public SyncResult addIntegral(long uid
            , int changeIntegral
            , int updateType
            , long eventId
            , long expiredTime
            , String memo
    ) {
        return DataService.getMainDB().beginTransaction(connection -> {
            return addIntegralTransaction(connection, uid, changeIntegral, updateType, memo, expiredTime, eventId, null, 0);
        });
    }

    /**
     * 更新用户余额
     *
     * @param connection
     * @param uid
     * @param changeIntegral
     * @param updateType
     * @param memo
     * @param expiredTime
     * @param eventId
     * @param extraData
     * @param testId
     * @return
     */
    public SyncResult addIntegralTransaction(Connection connection
            , long uid
            , int changeIntegral
            , int updateType
            , String memo
            , long expiredTime
            , long eventId
            , Map<String, Object> extraData
            , long testId) {

        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("uid", uid);
            data.put("change_integral", changeIntegral);
            data.put("expired_time", expiredTime);
            data.put("type_id", updateType);
            data.put("memo", memo);
            data.put("test_id", testId);
            data.put("event_id", eventId);

            if (extraData != null && extraData.size() > 0) {
                data.put("extra_data", MapUtil.toJSONString(extraData));
            }
            data.put("create_time", TimeUtil.getTimestamp());
            if (this.insertTransaction(connection, data) == 0) {
                return new SyncResult(11, "新增用户积分操作日志失败");
            }
            SyncResult r = UserIntegralLogEntity.getInstance().updateIntegralTransaction(
                    connection, uid, changeIntegral, updateType, memo, expiredTime, eventId, extraData, testId
            );
            if (r.code != 0) {
                return r;
            }
            return new SyncResult(0, "更新成功");
        } catch (Exception e) {
            return new SyncResult(1, e.getMessage());
        }
    }


    /**
     * 减少积分
     *
     * @param uid
     * @param changeIntegral
     * @param updateType
     * @param memo
     * @param eventId
     * @return
     */
    public SyncResult decrIntegral(long uid
            , int changeIntegral
            , int updateType
            , long eventId
            , String memo
    ) {

        return DataService.getMainDB().beginTransaction(connection -> {
            return decrIntegralTransaction(connection, uid, changeIntegral, updateType, memo, eventId, null, 0);
        });
    }

    /**
     * 消耗/减少用户积分
     *
     * @param connection
     * @param uid
     * @param changeIntegral
     * @param updateType
     * @param memo
     * @param eventId
     * @param extraData
     * @param testId
     * @return
     */
    public SyncResult decrIntegralTransaction(Connection connection
            , long uid
            , int changeIntegral
            , int updateType
            , String memo
            , long eventId
            , Map<String, Object> extraData
            , long testId
    ) {
        long currentTimestamp = TimeUtil.getTimestamp();
        /**
         * 获取用户未过期积分
         * 未过期积分-消耗积分 大于0 继续，小于0返回积分不足
         * 获取用户的积分列表
         * 循环积分列表 用扣除积分-每次获取的积分，直到积分<0的时候跳出循环
         */
        String sql = String.format("select sum(change_integral) as change_integral from UserIntegralDetail where  uid = %s and  status=0 and expired_time > %s", uid, currentTimestamp);

        try {

            List<Map<String, Object>> userIntegralTemp = this.queryTransaction(connection,sql);

            long userIntegral = MapUtil.getLong(userIntegralTemp.get(0), "change_integral");

            if (userIntegral < Math.abs(changeIntegral)) {
                return new SyncResult(1, "积分不足");
            }
            sql = String.format("select * from UserIntegralDetail where uid = %s and status=0 and expired_time > %s", uid, currentTimestamp);

            List<Map<String, Object>> userIntegralList = this.queryTransaction(connection,sql);

            int len = userIntegralList.size();
            long lastIntegral = Math.abs(changeIntegral);

            for (int i = 0; i < len; i++) {
                Map<String, Object> nd = userIntegralList.get(i);
                lastIntegral = lastIntegral - MapUtil.getLong(nd, "change_integral");
                System.out.println("lastIntegral=" + lastIntegral);
                System.out.println("i=" + i);
                System.out.println("i=" + nd.toString());
                //更新已经减去的积分为使用状态
                if (lastIntegral > 0) {
                    nd.put("status", 1);
                    nd.put("update_time", TimeUtil.getTimestamp());

                    this.where("id", MapUtil.getLong(nd, "id"))
                            .updateTransaction(connection, nd);
                } else if (lastIntegral == 0) {
                    nd.put("status", 1);
                    nd.put("update_time", TimeUtil.getTimestamp());
                    this.where("id", MapUtil.getLong(nd, "id"))
                            .updateTransaction(connection, nd);
                    break;
                } else {
                    //
                    nd.put("status", 1);
                    nd.put("update_time", TimeUtil.getTimestamp());

                    this.where("id", MapUtil.getLong(nd, "id"))
                            .update(nd);

                    Map<String, Object> newData = new LinkedHashMap<>();
                    newData.put("uid", MapUtil.getLong(nd, "uid"));
                    newData.put("type_id", MapUtil.getInt(nd, "type_id"));
                    newData.put("event_id", MapUtil.getLong(nd, "event_id"));
                    newData.put("expired_time", MapUtil.getLong(nd, "expired_time"));
                    newData.put("test_id", MapUtil.getLong(nd, "test_id"));
                    newData.put("memo", MapUtil.getString(nd, "memo"));
                    newData.put("extra_data", MapUtil.getString(nd, "extra_data"));
                    newData.put("create_time", TimeUtil.getTimestamp());
                    newData.put("change_integral", Math.abs(lastIntegral));
                    try {
                        this.insertTransaction(connection, newData);
                    } catch (Exception e) {
                        return new SyncResult(1, e.getMessage());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            return new SyncResult(1, e.getMessage());
        }
        UserIntegralLogEntity.getInstance().updateIntegralTransaction(
                connection, uid, changeIntegral, updateType, memo, 0, eventId, extraData, testId
        );
        return new SyncResult(0, "success");
    }


    /**
     * 积分退款
     * @param uid
     * @param updateType
     * @param eventId
     * @return
     */
    public SyncResult refundIntegral(long uid,int updateType,long eventId){
        return DataService.getMainDB().beginTransaction(connection ->{
            return refundIntegralTransaction(connection,uid,updateType,eventId);
        });
    }

    /**
     * 积分退款
     * @param connection
     * @param uid
     * @param updateType
     * @param eventId
     * @return
     */
    public SyncResult refundIntegralTransaction(Connection connection,long uid,int updateType,long eventId){
        if(uid==0 || eventId==0){
            return new SyncResult(1,"缺少参数");
        }
        try {
            List<UserIntegralDetailEntity> list  = UserIntegralDetailEntity.getInstance()
                    .where("uid",uid)
                    .where("type_id",updateType)
                    .where("event_id",eventId)
                    .where("status",0)
                    .selectListTransaction(connection);
            if(list.size()==0) return new SyncResult(1,"暂无可退款的积分");

            for (UserIntegralDetailEntity nd:list){
                addIntegralTransaction(connection
                        ,uid
                        ,nd.change_integral
                        ,nd.type_id
                        ,nd.memo
                        ,nd.expired_time
                        ,nd.event_id
                ,null,0);
            }


        }catch (Exception e){
            return new SyncResult(1,e.getMessage());
        }



        return new SyncResult(1,"");

    }
}