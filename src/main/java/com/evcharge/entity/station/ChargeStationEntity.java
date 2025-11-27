package com.evcharge.entity.station;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.sys.SysMessageEntity;
import com.evcharge.enumdata.ENotifyType;
import com.evcharge.mqtt.XMQTT3AsyncClient;
import com.evcharge.service.notify.NotifyService;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;


/**
 * 充电桩，记录充电桩具体地址、经纬度等信息;
 *
 * @author : JED
 * @date : 2022-9-15
 */
public class ChargeStationEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * uuid,项目唯一编码，充电联盟专用
     */
    public String uuid;
    /**
     * 地址的唯一值 用于确保 省市区地址经纬度不会修改
     */
    public String md5;

    /**
     * 项目ID，自定义生成（根据省市区等生成）
     */
    public String projectId;
    /**
     * 充电桩唯一编号，新增
     */
    public String CSId;
    /**
     * 暂定属性
     * 充电站属性 1=充电桩 2=充电柜 3=四轮慢充 4=四轮快充
     */
    public int station_attr;
    /**
     * 充电桩编号
     */
    public long stationNumber;
    /**
     * 名称
     */
    public String name;
    /**
     * 状态：0=删除，1=运营中，2=建设中
     */
    public int status;
    /**
     * 省
     */
    public String province;
    /**
     * 市
     */
    public String city;
    /**
     * 区
     */
    public String district;
    /**
     * 街道，可能为空字串
     */
    public String street;
    /**
     * 省份代码
     */
    public String province_code ;
    /**
     * 市代码
     */
    public String city_code ;
    /**
     * 区代码
     */
    public String district_code ;
    /**
     * 街道代码
     */
    public String street_code;
    /**
     * 城市社区/乡村
     */
    public String communities;
    /**
     * 路
     */
    public String roads;
    /**
     * 具体地址，门牌
     */
    public String address;
    /**
     * 经度
     */
    public double lon;
    /**
     * 纬度
     */
    public double lat;
    /**
     * 充电桩等级
     */
    public int station_level;
    /**
     * 总的插座
     */
    public int totalSocket;
    /**
     * 总的空闲插座
     */
    public int totalIdleSocket;
    /**
     * 主图
     */
    public String mainImage;
    /**
     * 正式上线时间
     */
    public long online_time;
    /**
     * 项目经理管理员ID
     */
    public long projectManagerAdminId;
    /**
     * 是否为测试，0=否，1=是
     */
    public int isTest;
    /**
     * 是否私有的，0-否，1-是
     */
    public int is_private;
    /**
     * 是否受限制，0-否，1-是，必须要白名单才能访问
     */
    public int is_restricted;
    /**
     * 结构：0-无，1-棚，2-架，3-墙 ，4-柱，99-其他
     */
    public int arch;
    /**
     * 人流量
     */
    public int humanTraffic;
    /**
     * 充电平台代码，表示此充电桩所属平台
     */
    public String platform_code;
    /**
     * 充电平台充电桩代码，表示此充电桩所在平台的唯一编码
     */
    public String platform_cs_id;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 充电桩分组id，关联充电桩，一般填写主站充电桩id
     */
    public String group_cs_id;

    /**
     * 广告版数量
     */
    public int ad_panel_count;

    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static ChargeStationEntity getInstance() {
        return new ChargeStationEntity();
    }

    /**
     * 同步充电桩使用数和空闲数
     *
     * @param CSId 充电桩id
     */
    public void syncSocketCount(String CSId) {
        String DeviceTableName = DeviceEntity.getInstance().theTableName();
        //总电位数
        int totalSocket = DeviceSocketEntity.getInstance()
                .alias("ds")
                .join(DeviceTableName, "d", "ds.deviceId = d.id")
                .where("d.CSId", CSId)
                .count("1");

        //空闲端口数
        int totalIdleSocket = DeviceSocketEntity.getInstance()
                .alias("ds")
                .join(DeviceTableName, "d", "ds.deviceId = d.id")
                .where("d.CSId", CSId)
                .where("status", 0)//状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
                .count();

        Map<String, Object> set_data = new HashMap<>();
        set_data.put("totalSocket", totalSocket);
        set_data.put("totalIdleSocket", totalIdleSocket);
        this.update(id, set_data);
    }

    /**
     * 通过DeviceCode查询充电桩信息
     *
     * @param deviceCode 设备编码
     * @return 充电桩信息
     */
    public ChargeStationEntity getWithDeviceCode(String deviceCode) {
        return this.field("a.*")
                .cache(String.format("ChargeStation:Device:%s", deviceCode))
                .alias("a")
                .join(DeviceEntity.getInstance().theTableName(), "b", "a.CSId = b.CSId")
                .where("b.deviceCode", deviceCode)
                .findEntity();
    }

    /**
     * 通过DeviceCode查询充电桩信息名称
     *
     * @param deviceCode 设备编码
     * @return 充电桩站名
     */
    public String getChargeStandardNameWithDeviceCode(String deviceCode) {
        String ChargeStandardName = "";
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithDeviceCode(deviceCode);
        if (chargeStationEntity != null && chargeStationEntity.id > 0) ChargeStandardName = chargeStationEntity.name;
        return ChargeStandardName;
    }

    /**
     * 充电桩组织鉴权
     *
     * @param chargingStationId 充电桩 ID
     * @param organizeCodes     组织代码（也可能是多个组织代码，比如：组织A,组织B,组织C）
     * @return 鉴权是否通过
     */
    public boolean authOrganizeCode(String chargingStationId, String[] organizeCodes) {
        if (!StringUtils.hasLength(chargingStationId) || organizeCodes == null || organizeCodes.length == 0) {
            return false;
        }
        String csOrganizeCode = getOrganizeCode(chargingStationId, true);
        if (!StringUtils.hasLength(csOrganizeCode)) return false;
        return Arrays.asList(organizeCodes).contains(csOrganizeCode);
    }

    /**
     * 根据uuid获取充电站信息
     *
     * @param uuid uuid
     * @return
     */
    public ChargeStationEntity getWithUUID(String uuid) {
        return getWithUUID(uuid, true);
    }

    /**
     * 根据uuid获取充电站信息
     *
     * @param uuid    uuid
     * @param inCache 是否缓存
     * @return
     */
    public ChargeStationEntity getWithUUID(String uuid, boolean inCache) {
        if (!StringUtils.hasLength(uuid)) return null;
        this.where("uuid", uuid);
        if (inCache) this.cache(String.format("ChargeStation:uuid:%s", uuid));
        return this.findEntity();
    }


    /**
     * 获取充电桩组织代码
     *
     * @param CSId 充电桩
     * @return 组织代码
     */
    public String getOrganizeCode(String CSId) {
        return getOrganizeCode(CSId, true);
    }

    /**
     * 获取充电桩组织代码
     *
     * @param CSId    充电桩
     * @param inCache 优先从缓存中获取
     * @return 组织代码
     */
    public String getOrganizeCode(String CSId, boolean inCache) {
        if (!StringUtils.hasLength(CSId)) return "";
        this.field("organize_code");
        if (inCache) this.cache(String.format("ChargeStation:Organize:%s", CSId));

        Map<String, Object> data = this.where("CSId", CSId).find();
        if (data == null || data.size() == 0) return "";
        return MapUtil.getString(data, "organize_code");
    }

    /**
     * 获取最早的充电桩上线时间
     */
    public long getEarliestOnlineTime() {
        Map<String, Object> data = ChargeStationEntity.getInstance()
                .field("create_time")
                .order("create_time")
                .find();
        long date_timestamp = 0;
        if (data != null && data.size() > 0) {
            date_timestamp = MapUtil.getLong(data, "create_time");
        }
//        if (date_timestamp == 0) {
//            date_timestamp = TimeUtil.getTimestamp();
//        }
        return date_timestamp;
    }

    /**
     * 确认充电桩上线
     */
    public SyncResult confirmOnline(String CSId, long online_time) {
        if (!StringUtil.hasLength(CSId)) return new SyncResult(2, "请选择充电桩");
        if (online_time <= 0) return new SyncResult(2, "请选择上线时间");

        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId, false);
        if (chargeStationEntity == null || chargeStationEntity.id == 0) return new SyncResult(3, "无效的充电桩id");

        if (chargeStationEntity.status == 2) {
            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("status", 1);//状态：0=删除，1=正常，2=在建
            set_data.put("online_time", online_time);
            chargeStationEntity.where("CSId", CSId).update(set_data);
        }
        return new SyncResult(0, "");
    }

    /**
     * 根据经纬度获取用户最近的充电桩
     */
    public ChargeStationEntity getNearbyChargeStation(String lon, String lat) {
        String field = String.format("id,name,lon,lat,status,( 6371 * ACOS(COS(RADIANS(%s)) * COS(RADIANS(lat)) *   COS(RADIANS(lon) - RADIANS(%s)) +  SIN(RADIANS(%s)) * SIN(RADIANS(lat)))) as distance", lat, lon, lat);

        return this.where("status", 1)
                .field(field)
                .order("distance asc")
                .limit(1)
                .findEntity();


    }

    /**
     * 通过站点ID查询充电桩信息
     *
     * @param CSId 站点ID
     * @return 充电桩信息
     */
    public ChargeStationEntity getWithCSId(String CSId) {
        return getWithCSId(CSId, true);
    }

    /**
     * 通过站点ID查询充电桩信息
     *
     * @param CSId    站点ID
     * @param inCache 是否优先从缓冲中获取
     * @return 充电桩信息
     */
    public ChargeStationEntity getWithCSId(String CSId, boolean inCache) {
        if ("".equals(CSId) || "0".equals(CSId)) return null;
        this.where("CSId", CSId);
        if (inCache) this.cache(String.format("ChargeStation:%s", CSId));
        return this.findEntity();
    }

    /**
     * 根据充电桩结构来结束充电
     *
     * @param arch    结构
     * @param title   消息标题
     * @param message 消息内容
     */
    public SyncResult stopChargeByArch(int arch, String title, String message) {
        List<Map<String, Object>> list = ChargeOrderEntity.getInstance()
                .field("co.id,uid,deviceCode,port,ChargeMode,OrderSN,co.CSId,cs.name")
                .alias("co")
                .join(ChargeStationEntity.getInstance().theTableName(), "cs", "co.CSId = cs.CSId")
                .where("cs.arch", arch) // 结构：0-无，1-棚，2-架
                .where("co.status", 1)//状态,-1=错误，0=待启动，1=充电中，2=已完成
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(10, "没有正在充电中的订单");

        int successCount = 0;
        int errorCount = 0;

        for (Map<String, Object> nd : list) {
            long uid = MapUtil.getLong(nd, "uid");
            String deviceCode = MapUtil.getString(nd, "deviceCode");

            JSONObject json = new JSONObject();
            json.put("deviceCode", deviceCode);
            json.put("port", MapUtil.getInt(nd, "port"));//端口
            json.put("ChargeMode", MapUtil.getInt(nd, "ChargeMode"));//计时
            json.put("OrderSN", MapUtil.getString(nd, "OrderSN"));

            //发送站内通知给用户
            if (StringUtils.hasLength(title) && StringUtils.hasLength(message)) {
                SysMessageEntity.getInstance().sendSysNotice(uid, title, message);
            }
            DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);
            if (deviceEntity == null) {
                errorCount++;
                continue;
            }

            XMQTT3AsyncClient.getInstance().publish(String.format("%s/%s/command/stopCharge", deviceEntity.appChannelCode, deviceCode), json);
            successCount++;
        }

        // region 2024-10-10 新版本通知系统
        JSONObject notifyTransData = new JSONObject();
        notifyTransData.put("title", title);
        notifyTransData.put("message", message);
        notifyTransData.put("time", TimeUtil.toTimeString());
        notifyTransData.put("success_count", successCount);
        notifyTransData.put("error_count", errorCount);
        NotifyService.getInstance().asyncPush(CSId
                , "SYSTEM.CS.STOP.CHARGING"
                , ENotifyType.WECHATCORPBOT
                , notifyTransData);
        //endregion

        return new SyncResult(0, "");
    }

    /**
     * 根据充电桩结构来结束充电
     *
     * @param CSIds   充电桩id
     * @param title   消息标题
     * @param message 消息内容
     */
    public SyncResult stopChargeByCSIds(String CSIds, String title, String message) {
        List<Map<String, Object>> list = ChargeOrderEntity.getInstance()
                .field("co.id,uid,deviceCode,port,ChargeMode,OrderSN,co.CSId,cs.name")
                .alias("co")
                .join(ChargeStationEntity.getInstance().theTableName(), "cs", "co.CSId = cs.CSId")
                .whereIn("cs.CSId", CSIds) // 结构：0-无，1-棚，2-架
                .where("co.status", 1)//状态,-1=错误，0=待启动，1=充电中，2=已完成
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(10, "没有正在充电中的订单");

        int successCount = 0;
        int errorCount = 0;

        for (Map<String, Object> nd : list) {
            long uid = MapUtil.getLong(nd, "uid");
            String deviceCode = MapUtil.getString(nd, "deviceCode");

            JSONObject json = new JSONObject();
            json.put("deviceCode", deviceCode);
            json.put("port", MapUtil.getInt(nd, "port"));//端口
            json.put("ChargeMode", MapUtil.getInt(nd, "ChargeMode"));//计时
            json.put("OrderSN", MapUtil.getString(nd, "OrderSN"));

            //发送站内通知给用户
            if (StringUtils.hasLength(title) && StringUtils.hasLength(message)) {
                SysMessageEntity.getInstance().sendSysNotice(uid, title, message);
            }
            DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);
            if (deviceEntity == null) {
                errorCount++;
                continue;
            }

            XMQTT3AsyncClient.getInstance().publish(String.format("%s/%s/command/stopCharge", deviceEntity.appChannelCode, deviceCode), json);
            successCount++;
        }

        // region 2024-10-10 新版本通知系统
        JSONObject notifyTransData = new JSONObject();
        notifyTransData.put("title", title);
        notifyTransData.put("message", message);
        notifyTransData.put("time", TimeUtil.toTimeString());
        notifyTransData.put("success_count", successCount);
        notifyTransData.put("error_count", errorCount);
        NotifyService.getInstance().asyncPush(CSId
                , "SYSTEM.CS.STOP.CHARGING"
                , ENotifyType.WECHATCORPBOT
                , notifyTransData);
        //endregion

        return new SyncResult(0, "");
    }
}
