package com.evcharge.entity.station.bill;


import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.sql.Connection;

/**
 * 电表与充电桩关联表;
 *
 * @author : Jay
 * @date : 2024-2-20
 */
public class EMeterToCStationEntity extends BaseEntity implements Serializable {
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
    public static EMeterToCStationEntity getInstance() {
        return new EMeterToCStationEntity();
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

}