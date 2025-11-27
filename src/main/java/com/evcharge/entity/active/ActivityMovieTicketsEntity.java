package com.evcharge.entity.active;

import com.evcharge.entity.recharge.RechargeOrderEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 活动电影票;
 *
 * @author : Jay
 * @date : 2023-6-13
 */
public class ActivityMovieTicketsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 电影名称
     */
    public String movie_title;
    /**
     * 海报地址
     */
    public String poster_url;
    /**
     * 播放地址
     */
    public String video_url;
    /**
     * 充值金额
     */
    public long recharge_price;
    /**
     * 主演
     */
    public String main_player;
    /**
     * 上映日期
     */
    public String release_date;
    /**
     * 总数量
     */
    public int total_amount;
    /**
     * 库存
     */
    public int stock;
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

//    public ActivityMovieTicketsEntity(){
//        movieId=SysGlobalConfigEntity.getLong("Activity:MovieTickets");
//    }

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ActivityMovieTicketsEntity getInstance() {
        return new ActivityMovieTicketsEntity();
    }

    /**
     * 获取电影票信息
     *
     * @param id
     * @return
     */
    public ActivityMovieTicketsEntity getInfo(long id) {
        return this.cache(String.format("Activity:Movie:Tickets:Info:%s", id), 86400 * 1000)
                .where("id", id)
                .findModel();
    }


    /**
     * 获取实时库存
     *
     * @return
     */
    public int getStock(long movieId) {
        int stock = DataService.getMainCache().getInt(String.format("Activity:Movie:Tickets:Stock:%s", movieId), -999);
        if (stock == 0) {
            return 0;
        }
        if (stock == -999) {
            ActivityMovieTicketsEntity activityMovieTicketsEntity = getInfo(movieId);
            if (activityMovieTicketsEntity.stock == 0) {
                return 0;
            }
            stock = activityMovieTicketsEntity.stock;
            DataService.getMainCache().set(String.format("Activity:Movie:Tickets:Stock:%s", movieId), stock);
            return stock;
        }
        return stock;
    }

    /**
     * 减库存
     */
    public static void decrMovieStock(long movieId) {
//        long movieId = SysGlobalConfigEntity.getLong("Activity:MovieTickets");
        System.out.println("decr movieid=" + movieId);
        DataService.getMainCache().decr(String.format("Activity:Movie:Tickets:Stock:%s", movieId), 1);
    }

    /**
     * 加库存
     */
    public static void incrMovieStock(long movieId) {
//        long movieId = SysGlobalConfigEntity.getLong("Activity:MovieTickets");
        System.out.println("incr movieid=" + movieId);
        DataService.getMainCache().incr(String.format("Activity:Movie:Tickets:Stock:%s", movieId), 1);
    }


    /**
     * 为某个用户派送电影票
     */
    public void sendCdKey(String orderSn){
        long movieId = SysGlobalConfigEntity.getLong("Activity:MovieTickets");
        RechargeOrderEntity orderInfo = RechargeOrderEntity
                .getInstance()
                .getOrderInfoByOrderSn(orderSn);

        ActivityMovieTicketsCDKeyEntity activityMovieTicketsCDKeyEntity = new ActivityMovieTicketsCDKeyEntity();
        ActivityMovieTicketsEntity activityMovieTicketsEntity = ActivityMovieTicketsEntity.getInstance().getInfo(movieId);
        long stock = activityMovieTicketsEntity.stock;
        stock = stock - 1;

        activityMovieTicketsCDKeyEntity = activityMovieTicketsCDKeyEntity.getUnUseKey(movieId);

        if(activityMovieTicketsEntity==null){
//            return RechargeOrderEntity.getInstance().HmCallback(orderSn, 100, sandData);
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uid", orderInfo.uid);
        data.put("recharge_ordersn", orderInfo.ordersn);
        data.put("status", 1);
        data.put("pickup_time", TimeUtil.getTimestamp());
        ActivityMovieTicketsCDKeyEntity finalActivityMovieTicketsCDKeyEntity = activityMovieTicketsCDKeyEntity;
        long finalStock = stock;
        SyncResult r = DataService.getMainDB().beginTransaction(connection -> {
            if (finalActivityMovieTicketsCDKeyEntity.where("id", finalActivityMovieTicketsCDKeyEntity.id).updateTransaction(connection, data) == 0) {
                LogsUtil.info(this.getClass().getName(), "更新活动失败，失败原因");
                return new SyncResult(1, "更新失败");
            }
            if (activityMovieTicketsEntity.where("id", activityMovieTicketsEntity.id).updateTransaction(connection, new LinkedHashMap<>() {{
                put("stock", finalStock);
            }}) == 0) {
                LogsUtil.info(this.getClass().getName(), "更新活动失败，失败原因");
                return new SyncResult(2, "更新失败");
            }
            DataService.getMainCache().del(String.format("Activity:Movie:Tickets:Info:%s", movieId));
            return new SyncResult(0, "");
        });
    }

    /**
     *
     * @param orderSn
     * @param sandData
     * @return
     */
//    public String callback(String orderSn, JSONObject sandData) {
//        //获取订单信息
//
//        //电影id
//        long movieId = SysGlobalConfigEntity.getLong("Activity:MovieTickets");
//        RechargeOrderEntity orderInfo = RechargeOrderEntity
//                .getInstance()
//                .getOrderInfoByOrderSn(orderSn);
//
//        if (!("SUCCESS").equals(sandData.getString("sub_code"))) { //订单支付失败
//            //库存加1
//            DataService.getMainCache().incr(String.format("Activity:Movie:Tickets:Stock:%s", movieId), 1);
//            LogsUtil.info("", "回调信息 状态为%s，订单编号=%s", sandData.getString("sub_code"), orderSn);
//            return "error";
//        }
//        Map<String, Object> data = new LinkedHashMap<>();
//        String reqReserved = sandData.getString("req_reserved");
//        int orderType = 0;
//
//        if (StringUtils.hasLength(reqReserved)) {
//            JSONObject param = JSONObject.parseObject(reqReserved);
//            orderType = param.getInteger("order_type");
//        }
//        ActivityMovieTicketsCDKeyEntity activityMovieTicketsCDKeyEntity = new ActivityMovieTicketsCDKeyEntity();
//        ActivityMovieTicketsEntity activityMovieTicketsEntity = ActivityMovieTicketsEntity.getInstance().getInfo(movieId);
//        long stock = activityMovieTicketsEntity.stock;
//
//        if (orderType == 1) {
//            stock = stock - 1;
//            activityMovieTicketsCDKeyEntity = activityMovieTicketsCDKeyEntity.getUnUseKey(movieId);
//        } else {
//            activityMovieTicketsCDKeyEntity = activityMovieTicketsCDKeyEntity.getUnUseKeyNoTicket(movieId);
//
//        }
//        if(activityMovieTicketsEntity==null){
//            return RechargeOrderEntity.getInstance().HmCallback(orderSn, 100, sandData);
//        }
//        int status = 1;
////        if ((activityMovieTicketsCDKeyEntity.id % 2) == 0) {
////            status = 2;
////        }
//        data.put("uid", orderInfo.uid);
//        data.put("recharge_ordersn", orderInfo.ordersn);
//        data.put("status", status);
//        data.put("pickup_time", TimeUtil.getTimestamp());
//
//        ActivityMovieTicketsCDKeyEntity finalActivityMovieTicketsCDKeyEntity = activityMovieTicketsCDKeyEntity;
//        long finalStock = stock;
//        SyncResult r = DataService.getMainDB().beginTransaction(connection -> {
//            if (finalActivityMovieTicketsCDKeyEntity.where("id", finalActivityMovieTicketsCDKeyEntity.id).updateTransaction(connection, data) == 0) {
//                LogsUtil.info("evcharge", "更新活动失败，失败原因");
//                return new SyncResult(1, "更新失败");
//            }
//            if (activityMovieTicketsEntity.where("id", activityMovieTicketsEntity.id).updateTransaction(connection, new LinkedHashMap<>() {{
//                put("stock", finalStock);
//            }}) == 0) {
//                LogsUtil.info("evcharge", "更新活动失败，失败原因");
//                return new SyncResult(2, "更新失败");
//            }
//            DataService.getMainCache().del(String.format("Activity:Movie:Tickets:Info:%s", movieId));
//            return new SyncResult(0, "");
//        });
//        if (r.code == 0) {
//            return RechargeOrderEntity.getInstance().HmCallback(orderSn,orderInfo.config_id, sandData);
//        } else {
//            return "error";
//        }
//    }

    /**
     * 随机获取电影票回调
     * @param orderSn
     * @return
     */
    public String randomCallback(String orderSn){
        long movieId = SysGlobalConfigEntity.getLong("Activity:MovieTickets");
        RechargeOrderEntity orderInfo = RechargeOrderEntity
                .getInstance()
                .getOrderInfoByOrderSn(orderSn);

        long uid =orderInfo.uid;
        ActivityMovieTicketsCDKeyEntity userMovieTicketsCDKeyEntity = ActivityMovieTicketsCDKeyEntity
                .getInstance()
                .getUserKeyByMovieId(uid,movieId);

        if(userMovieTicketsCDKeyEntity!=null){ //如果已经获得了userkey
            LogsUtil.info(this.getClass().getName(),String.format("用户已获得cdkey，ordersn=%s",orderSn));
            return "error";
        }
        Map<String, Object> data = new LinkedHashMap<>();
        ActivityMovieTicketsCDKeyEntity activityMovieTicketsCDKeyEntity = ActivityMovieTicketsCDKeyEntity.getInstance().getUnUseKey(movieId);

        ActivityMovieTicketsEntity activityMovieTicketsEntity = ActivityMovieTicketsEntity.getInstance().getInfo(movieId);
        if(activityMovieTicketsEntity==null){ //如果有cdkey
            return "error";
            // return RechargeOrderEntity.getInstance().HmCallback(orderSn, sandData);
        }
        long stock = activityMovieTicketsEntity.getStock(movieId);
        System.out.println("random callback,stock="+stock);
        if(stock<=0){ //如果库存没有了，直接跳转到支付回调接口
            LogsUtil.info(this.getClass().getName(),String.format("没有cdkey库存了，ordersn=%s",orderSn));
            return "error";
        }
        ActivityMovieTicketsEntity.decrMovieStock(movieId);//库存减1
        stock = stock - 1;
//        activityMovieTicketsCDKeyEntity = activityMovieTicketsCDKeyEntity.getUnUseKey(movieId);//获取cdkey
//        if (orderType == 1) {
//            stock = stock - 1;
//            activityMovieTicketsCDKeyEntity = activityMovieTicketsCDKeyEntity.getUnUseKey(movieId);
//        } else {
//            activityMovieTicketsCDKeyEntity = activityMovieTicketsCDKeyEntity.getUnUseKeyNoTicket(movieId);
//
//        }
        int status = 1;
        int seeds=SysGlobalConfigEntity.getInt("Activity:MovieTickets:Recharge:Rate",3);
        //(number - 1) % 4 == 0;
        System.out.println(String.format("order_id=%s,间隔%s,判断结果=%b",orderInfo.id,seeds,(orderInfo.id -1)%(seeds+1) == 0));
        if ((orderInfo.id -1)%(seeds+1) != 0) { //获取算法
//            status = 2;
            ActivityMovieTicketsEntity.incrMovieStock(movieId);//库存加1
            return "error";
        }
        data.put("uid", orderInfo.uid);
        data.put("recharge_ordersn", orderInfo.ordersn);
        data.put("status", status);
        data.put("pickup_time", TimeUtil.getTimestamp());
        System.out.println(data);
        long finalStock = stock;
        SyncResult r = DataService.getMainDB().beginTransaction(connection -> {
            if (ActivityMovieTicketsCDKeyEntity.getInstance()
                    .where("id", activityMovieTicketsCDKeyEntity.id).updateTransaction(connection, data) == 0) {
                LogsUtil.info(this.getClass().getName(), "更新活动失败，失败原因");
                return new SyncResult(1, "更新失败");
            }
            if (ActivityMovieTicketsEntity.getInstance().where("id", activityMovieTicketsEntity.id).updateTransaction(connection, new LinkedHashMap<>() {{
                put("stock", finalStock);
            }}) == 0) {
                LogsUtil.info(this.getClass().getName(), "更新活动失败，失败原因");
                return new SyncResult(2, "更新失败");
            }
            DataService.getMainCache().del(String.format("Activity:Movie:Tickets:Info:%s", movieId));
            return new SyncResult(0, "");
        });
        if (r.code == 0) {
            //return RechargeOrderEntity.getInstance().HmCallback(orderSn, sandData);
            return "success";
        } else {
            return "error";
        }
    }


}