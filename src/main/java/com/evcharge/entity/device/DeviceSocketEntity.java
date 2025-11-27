package com.evcharge.entity.device;


import com.xyzs.cache.ECacheTime;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

import java.io.Serializable;
import java.util.*;

/**
 * 设备-插座 n-n 关联，一个设备拥有什么类型的插座
 *
 * @author : JED
 * @date : 2022-9-15
 */
public class DeviceSocketEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 关联的设备ID
     */
    public long deviceId;
    /**
     * 设备编码
     */
    public String deviceCode;
    /**
     * 关联的插座单元ID,表示此设备拥有什么插座
     */
    public long socketId;
    /**
     * 插座编号
     */
    public int index;
    /**
     * 端口号
     */
    public int port;
    /**
     * 状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充，6=定时预约，7=等待用户确认
     */
    public int status;
    /**
     * 状态补充说明
     */
    public String status_msg;
    /**
     * 限制充电功率
     */
    public double limitChargePower;
    /**
     * 当前使用中的功率，心跳包数据更新
     */
    public double usePower;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;
    /**
     * 充电柜使用，门状态：0-关闭，1-打开，-1-无
     */
    public int door_status;
    /**
     * 充电柜使用，风扇状态：0-关闭，1-打开，-1-无
     */
    public int fan_status;
    /**
     * 输入电压（当前，心跳包数据更新）
     */
    public double inputVoltage;
    /**
     * 输出电压（当前，心跳包数据更新）
     */
    public double outputVoltage;
    /**
     * 端口温度（摄氏度）
     */
    public double temperature;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static DeviceSocketEntity getInstance() {
        return new DeviceSocketEntity();
    }

    /**
     * 设备绑定插座
     *
     * @return
     */
    public SyncResult addSocket() {
        if (deviceId == 0) return new SyncResult(2, "请选择正确的设备");
        if (socketId == 0) return new SyncResult(2, "请选择正确的插座");

        if (!DeviceEntity.getInstance().exist(deviceId)) return new SyncResult(2, "请选择正确的设备");
        if (!SocketUnitEntity.getInstance().exist(socketId)) return new SyncResult(2, "请选择正确的插座");

        //如果插座位置索引为0，则自动计算index
        if (index == -1) {
            index = 1;
            Map<String, Object> data = this.field("index")
                    .where("deviceId", deviceId)
                    .where("socketId", socketId)
                    .order("`index` DESC")
                    .find();
            if (data != null && !data.isEmpty()) {
                index = MapUtil.getInt(data, "index");
                index++;
            }
        }

        //如果插座位置索引为0，则自动计算index
        if (port == -1) {
            port = 0;
            Map<String, Object> data = this.field("port")
                    .where("deviceId", deviceId)
                    .where("socketId", socketId)
                    .order("`port` DESC")
                    .find();
            if (data != null && !data.isEmpty()) {
                port = MapUtil.getInt(data, "port");
                port++;
            }
        }

        status = 0;//状态：0=空闲，1=充电中，2=故障
        status_msg = "";
        create_time = TimeUtil.getTimestamp();
        update_time = TimeUtil.getTimestamp();

        id = insertGetId();
        if (id > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }

    /**
     * 设备批量绑定插座
     *
     * @return
     */
    public SyncResult addSocket(long deviceId, long socketId, int count) {
        if (deviceId == 0) return new SyncResult(2, "请选择正确的设备");
        if (socketId == 0) return new SyncResult(2, "请选择正确的插座");

        if (!DeviceEntity.getInstance().exist(deviceId)) return new SyncResult(2, "请选择正确的设备");
        if (!SocketUnitEntity.getInstance().exist(socketId)) return new SyncResult(2, "请选择正确的插座");

        return this.beginTransaction(connection -> {
            //如果插座位置索引为0，则自动计算index
            int lastIndex = 0;

            Map<String, Object> index_data = this.field("index")
                    .where("deviceId", deviceId)
                    .where("socketId", socketId)
                    .order("`index` DESC")
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
            if (!index_data.isEmpty()) {
                lastIndex = MapUtil.getInt(index_data, "index");
            }

            int lastPort = -1;
            Map<String, Object> port_data = this.field("port")
                    .where("deviceId", deviceId)
                    .where("socketId", socketId)
                    .order("`port` DESC")
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
            if (!port_data.isEmpty()) {
                lastPort = MapUtil.getInt(port_data, "port");
            }

            for (int i = 0; i < count; i++) {
                lastIndex++;
                lastPort++;
                Map<String, Object> insertData = new LinkedHashMap<>();
                insertData.put("deviceId", deviceId);
                insertData.put("socketId", socketId);
                insertData.put("index", lastIndex);
                insertData.put("port", lastPort);
                insertData.put("limitChargePower", 1000);
                insertData.put("status", 0);//状态：0=空闲，1=充电中，2=故障
                insertData.put("status_msg", "");
                insertData.put("create_time", TimeUtil.getTimestamp());
                insertData.put("update_time", TimeUtil.getTimestamp());

                long noquery = insertGetIdTransaction(connection, insertData);
                if (noquery == 0) return new SyncResult(1, "操作失败");
            }
            return new SyncResult(0, "");
        });
    }

    /**
     * 根据设备ID获取插座列表
     *
     * @param deviceIds
     * @return
     */
    public Map<Long, List<Map<String, Object>>> getSocketListWithDeviceIds(List<Object> deviceIds) {
        List<DeviceSocketEntity> list = this.whereIn("deviceId", deviceIds)
                .selectList();
        if (list.isEmpty()) return new LinkedHashMap<>();
        Map<Long, List<Map<String, Object>>> cbdata = new LinkedHashMap<>();

        for (DeviceSocketEntity nd : list) {
            List<Map<String, Object>> dslist = cbdata.get(nd.deviceId);
            if (dslist == null || dslist.isEmpty()) dslist = new LinkedList<>();

            Map<String, Object> socket = new LinkedHashMap<>();
//            socket.put("deviceId", nd.deviceId);
//            socket.put("socketId", nd.socketId);
            socket.put("index", nd.index);
            socket.put("port", nd.port);
            socket.put("status", nd.status);
            socket.put("status_msg", nd.status_msg);

            dslist.add(socket);
            if (!cbdata.containsKey(nd.deviceId)) cbdata.put(nd.deviceId, dslist);
        }

        return cbdata;
    }

    /**
     * 根据设备码和端口读取插座信息
     *
     * @param deviceCode 设备码
     * @param port       端口
     * @return
     */
    public DeviceSocketEntity getWithDeviceCode(String deviceCode, int port) {
        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);
        if (deviceEntity == null || deviceEntity.id == 0) return null;

        return DeviceSocketEntity.getInstance()
                .where("deviceId", deviceEntity.id)
                .where("port", port)
                .findEntity();
    }

    /**
     * 根据设备码获取插座简单信息
     *
     * @param deviceCode 设备码
     * @return 简单的插座信息
     */
    public List<Map<String, Object>> getSocketSimpleListWithDeviceCode(String deviceCode) {
        return getSocketSimpleListWithDeviceCode(deviceCode, true);
    }

    /**
     * 根据设备码获取插座简单信息
     *
     * @param deviceCode 设备码
     * @param inCache    优先从缓存中获取
     * @return 简单的插座信息
     */
    public List<Map<String, Object>> getSocketSimpleListWithDeviceCode(String deviceCode, boolean inCache) {
        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);
        if (deviceEntity == null || deviceEntity.id == 0) return null;
        if (inCache) this.cache(String.format("Device:%s:Socket:List", deviceCode), ECacheTime.MINUTE * 10);
        return this.field("id,index,port,status,limitChargePower,door_status,fan_status")
                .where("deviceId", deviceEntity.id)
                .order("`index`")
                .select();
    }
}
