package com.evcharge.entity.user;


import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户接收订阅消息记录表;
 *
 * @author : JED
 * @date : 2024-1-9
 */
public class UserReceiveWechatMsgLogEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 模版id
     */
    public String tmpl_id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 运行时间
     */
    public long run_time;
    /**
     * 状态 1=待执行 2=执行结束 -1=执行失败
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
    public static UserReceiveWechatMsgLogEntity getInstance() {
        return new UserReceiveWechatMsgLogEntity();
    }

    /**
     * 添加消息订阅
     *
     * @param uid
     * @param tmplId
     * @param runTime
     * @return
     */
    public SyncResult addSubscribeMsg(long uid, String tmplId, long runTime) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uid", uid);
        data.put("tmpl_id", tmplId);
        data.put("run_time", runTime);
        data.put("status", 1);
        if (this.insert(data) == 0) {
            return new SyncResult(1, "更新数据失败");
        }

        return new SyncResult(0, "更新成功");

    }

    /**
     * 更新日志状态
     * @param id
     * @param status
     * @return
     */
    public SyncResult updateLogStatus(long id, int status) {
//        Map<String, Object> data = new LinkedHashMap<>();
//        data.put("status", status);
//        if (this.where("id", id).update(data) == 0) {
//            return new SyncResult(1, "更新数据失败");
//        }
//        return new SyncResult(0, "更新成功");
        return DataService.getMainDB().beginTransaction(connection -> {
            return updateLogStatusTransaction(connection,id,status);
        });
    }

    /**
     * 批量更新日志
     * @param connection
     * @param id
     * @param status
     * @return
     */
    public SyncResult updateLogStatusTransaction(Connection connection,long id, int status) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", status);
            if (this.where("id", id).updateTransaction(connection,data) == 0) {
                return new SyncResult(1, "更新数据失败");
            }
            return new SyncResult(0, "更新成功");
        }catch (Exception e){
            return new SyncResult(1, "更新数据失败");
        }
    }

}