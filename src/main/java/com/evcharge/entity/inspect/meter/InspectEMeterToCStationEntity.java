package com.evcharge.entity.inspect.meter;


import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 电表与充电桩关联表;
 *
 * @author : Jay
 * @date : 2024-2-20
 */
public class InspectEMeterToCStationEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 电表id
     */
    public long meter_id;
    /**
     * 充电站id
     */
    public String uuid ;
    /**
     * 充电站id
     */
    public long cs_id;
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
    public static InspectEMeterToCStationEntity getInstance() {
        return new InspectEMeterToCStationEntity();
    }


    /**
     * 更新充电桩
     * @param meterId
     * @param csIds
     * @return
     */
    public SyncResult updateStation(long meterId, String[] csIds) {
        if (this.where("meter_id", meterId).count() > 0) {
            this.where("meter_id", meterId).del();
        }
        if (csIds != null) {
            return DataService.getMainDB().beginTransaction(connection -> {
                for (String csId : csIds) {
                     addStation(connection, meterId, Long.parseLong(csId));
                }
                return new SyncResult(0, "success");
            });
        }
        return new SyncResult(1, "csIds不能为空");
    }

    /**
     * 添加充电桩
     * @param meterId
     * @param csId
     * @return
     */
    public SyncResult addStation(long meterId, long csId) {
        return DataService.getMainDB().beginTransaction(connection -> {
            addStation(connection, meterId, csId);
            return new SyncResult(0, "success");
        });
    }

    /**
     * 添加充电桩
     * @param connection
     * @param meterId
     * @param csId
     * @return
     */
    public SyncResult addStation(Connection connection, long meterId, long csId) {
        try {
            this.meter_id = meterId;
            this.cs_id = csId;
            this.status = 1;
            this.create_time = TimeUtil.getTimestamp();
            this.insertTransaction(connection);
            return new SyncResult(0, "success");
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(), e.getMessage());
            return new SyncResult(1, e.getMessage());
        }
    }

    /**
     * 只考虑了一个电表对应一个桩
     * @param uuid
     * @param newUUID
     * @return
     */
    public SyncResult copy(String uuid,String newUUID,long csId){
        InspectEMeterToCStationEntity eMeterToCStationEntity=this.where("uuid",uuid).findEntity();

        if(eMeterToCStationEntity==null) return new SyncResult(1,"还没有绑定电表");
        Map<String,Object> data=new LinkedHashMap<>();

        data.put("meter_id",eMeterToCStationEntity.meter_id);
        data.put("uuid",newUUID);
        data.put("cs_id",csId);
        data.put("create_time",TimeUtil.getTimestamp());

        if(InspectEMeterToCStationEntity.getInstance().insertGetId(data)==0){
            return new SyncResult(1,"复制电表信息失败");
        }
        return new SyncResult(0,"复制成功");

    }


}