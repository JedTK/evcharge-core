package com.evcharge.entity.platform;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.station.ElectricityMeterEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.libsdk.genkigo.GenkigoSDK;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * 充电桩平台;
 *
 * @author : JED
 * @date : 2023-11-22
 */
public class EvPlatformEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 平台名
     */
    public String name;
    /**
     * 平台代码
     */
    public String code;
    /**
     * 备注
     */
    public String remark;
    /**
     * 联系人姓名
     */
    public String contacts;
    /**
     * 联系电话
     */
    public String contactsPhone;
    /**
     * 状态：0-关闭，1-正常
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static EvPlatformEntity getInstance() {
        return new EvPlatformEntity();
    }

    /**
     * 通过平台代码获取平台信息
     *
     * @param platform_code
     * @return
     */
    public EvPlatformEntity getByCode(String platform_code) {
        return this.cache(String.format("BaseData:EvPlatform:%s", platform_code))
                .where("code", platform_code)
                .findEntity();
    }

    /**
     * 检查充电桩是否为子平台的充电桩，如果是则 请求 同步充电桩信息和设备
     *
     * @param CSId           上层平台充电桩ID
     * @param installOrderSN （可选）安装订单号
     * @param meterNo        （可选）电表编号
     */
    public void checkSyncCSDeviceToPlatform(String CSId, String installOrderSN, String meterNo) {
        //查询充电桩数据是否存在
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance()
                .where("CSId", CSId)
                .findEntity();
        if (chargeStationEntity == null) {
            LogsUtil.warn("同步充电桩和设备数据", "无法查询到充电桩数据，CSId=%s", CSId);
            return;
        }

        //查询上层平台代码
        String platform_code = SysGlobalConfigEntity.getString("System:EvPlatform:Code");
        //检查此充电桩的平台代码是否为空，如果空则不需要进行同步
        //检查此充电桩的平台代码是否和上层平台代码相同，如果相同则不需要进行同步
        if (!StringUtils.hasLength(chargeStationEntity.platform_code)
                || platform_code.equalsIgnoreCase(chargeStationEntity.platform_code)) {
            LogsUtil.warn("同步充电桩和设备数据", "充电桩不属于任何子平台");
            return;
        }

        //检查此充电桩的平台代码是否配置信息，如果没有则不需要进行同步
        EvPlatformConfigEntity evPlatformConfigEntity = EvPlatformConfigEntity.getInstance().getWithPlatformCode(chargeStationEntity.platform_code);
        if (evPlatformConfigEntity == null) {
            LogsUtil.warn("同步充电桩和设备数据", "无法查询平台配置 platform_code=%s", platform_code);
            return;
        }

        //根据充电桩平台代码的配置初始化SDK
        GenkigoSDK genkigoSDK = new GenkigoSDK()
                .setPlatformCode(platform_code)
                .setAppSecret(evPlatformConfigEntity.RestAppSecret)
                .setRestAPIUrl(evPlatformConfigEntity.RestAPI);

        //region 同步充电桩数据
        JSONObject csParams = new JSONObject();
        csParams.put("platform_code", platform_code);
        csParams.put("projectId", chargeStationEntity.projectId);
        csParams.put("CSId", chargeStationEntity.CSId);
        csParams.put("stationNumber", chargeStationEntity.stationNumber);
        csParams.put("name", chargeStationEntity.name);
        csParams.put("status", chargeStationEntity.status);
        csParams.put("province", chargeStationEntity.province);
        csParams.put("city", chargeStationEntity.city);
        csParams.put("district", chargeStationEntity.district);
        csParams.put("street", chargeStationEntity.street);
        csParams.put("street_code", chargeStationEntity.street_code);
        csParams.put("communities", chargeStationEntity.communities);
        csParams.put("roads", chargeStationEntity.roads);
        csParams.put("address", chargeStationEntity.address);
        csParams.put("lon", chargeStationEntity.lon);
        csParams.put("lat", chargeStationEntity.lat);
        csParams.put("arch", chargeStationEntity.arch);
        csParams.put("online_time", chargeStationEntity.online_time);
        SyncResult csResult = genkigoSDK.syncChargeStation(chargeStationEntity.CSId, chargeStationEntity.name, csParams);
        if (csResult.code != 0) {
            LogsUtil.warn("同步充电桩和设备数据", "同步充电桩数据失败，原因：%s", csResult.msg);
            return;
        }
        //endregion

        //region 同步充电桩设备数据
        Map<String, Object> csData = (Map<String, Object>) csResult.data;
        final String platform_cs_id = MapUtil.getString(csData, "platform_cs_id");
        DeviceEntity.getInstance().where("CSId", chargeStationEntity.CSId).update(new LinkedHashMap<>() {{
            this.put("platform_cs_id", platform_cs_id);
        }});
        JSONObject params = new JSONObject();
        params.put("platform_code", platform_code);
        params.put("projectId", chargeStationEntity.projectId);
        params.put("CSId", chargeStationEntity.CSId);
        params.put("OrderSN", installOrderSN);
        params.put("meterNo", meterNo);
        JSONObject host = new JSONObject();
        JSONArray devices = new JSONArray();
        List<DeviceEntity> deviceList = DeviceEntity.getInstance().where("CSId", CSId).selectList();
        Iterator it = deviceList.iterator();

        while (it.hasNext()) {
            DeviceEntity deviceEntity = (DeviceEntity) it.next();
            JSONObject device = new JSONObject();
            device.put("deviceCode", deviceEntity.deviceCode);
            device.put("deviceName", deviceEntity.deviceName);
            device.put("deviceNumber", deviceEntity.deviceNumber);
            device.put("deviceUnitId", deviceEntity.deviceUnitId);
            device.put("simCode", deviceEntity.simCode);
            device.put("isHost", deviceEntity.isHost);
            device.put("chargeStandardConfigId", deviceEntity.chargeStandardConfigId);
            device.put("chargeTimeConfigId", deviceEntity.chargeTimeConfigId);
            device.put("parkingConfigId", deviceEntity.parkingConfigId);
            device.put("safeCharge", deviceEntity.safeCharge);
            device.put("safeChargeFee", deviceEntity.safeChargeFee);
            device.put("display_status", deviceEntity.display_status);
            if (deviceEntity.isHost == 1) {
                host = device;
            } else {
                devices.add(device);
            }
        }

        SyncResult deviceResult = genkigoSDK.syncDevice(CSId, params, host, devices);
        if (deviceResult.code != 0) {
            LogsUtil.warn("同步充电桩和设备数据", "同步充电桩充电设备数据失败，原因：%s", deviceResult.msg);
            return;
        }

        //endregion
    }

    //region 响应方处理

    /**
     * 请求获取授权码
     *
     * @return 同步结果集合
     */
    public SyncResult requestAccessToken() {
        if (!StringUtils.hasLength(this.code)) return new SyncResult(10002, "缺少平台代码");

        EvPlatformConfigEntity configEntity = EvPlatformConfigEntity.getInstance().getWithPlatformCode(this.code);
        if (configEntity == null || configEntity.id == 0) return new SyncResult(10003, "无效平台代码");

        long expiredMillisecond = ECacheTime.HOUR;

        Map<String, Object> data = new LinkedHashMap<>();
        String accessToken = JWTUtil.getInstance()
                .setSecretKeyHMAC256(configEntity.RestAppSecret)
                .create(configEntity.platform_code, expiredMillisecond + ECacheTime.MINUTE * 30);
        if (StringUtils.hasLength(accessToken)) {
            DataService.getMainCache().set(String.format("GenkigoSDK:App:%s:AccessToken", this.code), accessToken, expiredMillisecond + ECacheTime.MINUTE * 5);
            DataService.getMainCache().set(String.format("GenkigoSDK:App:AccessToken:%s", common.md5(accessToken)), this.code, expiredMillisecond + ECacheTime.MINUTE * 5);

            data.put("AccessToken", accessToken);
            data.put("expired", expiredMillisecond);
            return new SyncResult(0, "", data);
        }
        return new SyncResult(10001, "获取授权码失败");
    }

    /**
     * 验证授权码是否可用
     *
     * @param accessToken 授权码
     * @return 是否可用
     */
    public boolean verifyAccessToken(String accessToken) {
        if (!StringUtils.hasLength(accessToken)) return false;
        String PlatformCode = DataService.getMainCache().getString(String.format("GenkigoSDK:App:AccessToken:%s", common.md5(accessToken)));

        EvPlatformConfigEntity configEntity = EvPlatformConfigEntity.getInstance().getWithPlatformCode(PlatformCode);
        if (configEntity == null || configEntity.id == 0) return false;

        DecodedJWT decodedJWT = JWTUtil.getInstance()
                .setSecretKeyHMAC256(configEntity.RestAppSecret)
                .verify(accessToken);
        if (decodedJWT == null) return false;
        if (!PlatformCode.equalsIgnoreCase(decodedJWT.getIssuer())) return false;

        // 获取JWT的过期时间
        Date expiresAt = decodedJWT.getExpiresAt();
        // 比较当前时间和过期时间
        if (expiresAt != null && expiresAt.before(new Date())) {
            // 如果过期时间在当前时间之前，则返回false
            return false;
        }
        // 如果以上检查都通过，则返回true，表示授权码有效
        return true;
    }

    /**
     * 使用ResAPI：同步一个充电桩的信息，建议在竣工的时候操作同步
     * {
     * "platform_code": "85",
     * "projectId": "46",
     * "CSId": "8",
     * "stationNumber": "67",
     * "name": "形长处",
     * "status": 42,
     * "province": "宁夏回族自治区",
     * "city": "锦州市",
     * "district": "蛟河市",
     * "street": "minim fugiat",
     * "street_code": "68",
     * "communities": "id Ut dolore ex adipisicing",
     * "roads": "exercitation",
     * "address": "辽宁省上海市-",
     * "lon": 96,
     * "lat": 63,
     * "arch": 57,
     * "online_time": 557245603389
     * }
     *
     * @return
     */
    public SyncResult syncChargeStation(JSONObject params) {
        if (params == null) return new SyncResult(1299, "缺少主要数据");
        try {
            //上层平台代码
            String platform_code = params.getString("platform_code");
            //上层项目ID(不一定有)
            String projectId = params.getString("projectId");
            //上层充电桩ID
            String platform_cs_id = params.getString("CSId");
            //上层充电桩编号（可作参考使用）
            String stationNumber = params.getString("stationNumber");

            //region 获取参数和设置参数
            String name = params.getString("name");
            int status = params.getIntValue("status");
            String province = params.getString("province");
            String city = params.getString("city");
            String district = params.getString("district");
            String street = params.getString("street");
            String streetCode = params.getString("street_code");
            String communities = params.getString("communities");
            String roads = params.getString("roads");
            String address = params.getString("address");
            double lon = params.getDoubleValue("lon");
            double lat = params.getDoubleValue("lat");
            int arch = params.getIntValue("arch");
            //上线时间戳
            long online_time = params.getLong("online_time");

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("projectId", projectId);
            data.put("stationNumber", stationNumber);
            data.put("name", name);
            data.put("status", status);
            data.put("province", province);
            data.put("city", city);
            data.put("district", district);
            data.put("street", street);
            data.put("street_code", streetCode);
            data.put("communities", communities);
            data.put("roads", roads);
            data.put("address", address);
            data.put("lon", lon);
            data.put("lat", lat);
            data.put("arch", arch);
            if (online_time != 0) data.put("online_time", online_time);
            //endregion

            String uniqueId = "";
            EvPlatformToChargeStationEntity evPlatformToChargeStationEntity = EvPlatformToChargeStationEntity.getInstance()
                    .where("platform_code", platform_code)
                    .where("platform_cs_id", platform_cs_id)
                    .findEntity();
            if (evPlatformToChargeStationEntity != null && evPlatformToChargeStationEntity.id != 0) {
                //表示已经存在数据
                ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance()
                        .where("CSId", evPlatformToChargeStationEntity.CSId)
                        .findEntity();
                if (chargeStationEntity == null) {
                    //表示不存在数据，这种情况应该很少出现
                    SyncResult r = ChargeStationEntity.getInstance().beginTransaction(connection -> {
                        ChargeStationEntity entity = ChargeStationEntity.getInstance();
                        data.put("create_time", TimeUtil.getTimestamp());
                        long id = entity.insertGetIdTransaction(connection, data);
                        if (id == 0) return new SyncResult(10101, "新增失败");

                        String CSId = String.format("%s", id);
                        Map<String, Object> setData = new LinkedHashMap<>();
                        setData.put("CSId", CSId);
                        if (entity.updateTransaction(connection, id, setData) == 0) {
                            return new SyncResult(10101, "新增失败");
                        }
                        return new SyncResult(0, "", setData);
                    });
                    if (r.code != 0) return r;
                    Map<String, Object> rdata = (Map<String, Object>) r.data;
                    uniqueId = MapUtil.getString(rdata, "CSId");
                } else {
                    //状态：0=删除，1=运营中，2=建设中
                    data.remove("online_time");//移除上线时间，不允许更新上线时间
                    if (chargeStationEntity.status == 2) chargeStationEntity.update(chargeStationEntity.id, data);
                    uniqueId = chargeStationEntity.CSId;
                }
            } else {
                //表示不存在数据
                SyncResult r = ChargeStationEntity.getInstance().beginTransaction(connection -> {
                    ChargeStationEntity entity = ChargeStationEntity.getInstance();
                    data.put("create_time", TimeUtil.getTimestamp());
                    long id = entity.insertGetIdTransaction(connection, data);
                    if (id == 0) return new SyncResult(10101, "新增失败");

                    String CSId = String.format("%s", id);
                    Map<String, Object> setData = new LinkedHashMap<>();
                    setData.put("CSId", CSId);
                    if (entity.updateTransaction(connection, id, setData) == 0) {
                        return new SyncResult(10101, "新增失败");
                    }

                    //新增关联
                    EvPlatformToChargeStationEntity evPlatformToChargeStation = EvPlatformToChargeStationEntity.getInstance();
                    evPlatformToChargeStation.platform_code = platform_code;
                    evPlatformToChargeStation.platform_cs_id = platform_cs_id;
                    evPlatformToChargeStation.CSId = String.format("%s", id);
                    if (evPlatformToChargeStation.insertTransaction(connection) == 0) {
                        return new SyncResult(10101, "新增失败");
                    }
                    return new SyncResult(0, "", setData);
                });
                if (r.code != 0) return r;
                Map<String, Object> rdata = (Map<String, Object>) r.data;
                uniqueId = MapUtil.getString(rdata, "CSId");
            }

            Map<String, Object> cbdata = new LinkedHashMap<>();
            cbdata.put("platform_cs_id", uniqueId);
            return new SyncResult(0, "", cbdata);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "RestAPI 新增充电桩信息发生错误");
        }
        return new SyncResult(10201, "新增失败");
    }

    /**
     * 使用ResAPI：同步设备信息
     */
    public SyncResult syncDevice(JSONObject params) {
        if (params == null) return new SyncResult(1299, "缺少主要数据");
        try {
            //上层平台代码
            String platform_code = params.getString("platform_code");
            //上层项目ID(不一定有)
            String projectId = params.getString("projectId");
            //上层充电桩ID
            String platform_cs_id = params.getString("CSId");

            //安装工程工单（不一定有）
            String OrderSN = params.getString("OrderSN");//此次工单订单号
            //电表编号
            String meterNo = params.getString("meterNo");//电表编号

            ChargeStationEntity chargeStationEntity;

            //region 平台信息处理
            EvPlatformToChargeStationEntity evPlatformToChargeStationEntity = EvPlatformToChargeStationEntity.getInstance()
                    .where("platform_code", platform_code)
                    .where("platform_cs_id", platform_cs_id)
                    .findEntity();
            if (evPlatformToChargeStationEntity == null) return new SyncResult(10203, "无效的充电桩ID");
            if (!StringUtils.hasLength(evPlatformToChargeStationEntity.CSId)) {
                return new SyncResult(10204, "无法找到匹配的充电桩信息");
            }
            chargeStationEntity = ChargeStationEntity.getInstance()
                    .where("CSId", evPlatformToChargeStationEntity.CSId)
                    .findEntity();
            if (chargeStationEntity == null || chargeStationEntity.id == 0)
                return new SyncResult(10203, "无效的充电桩ID");
            //endregion

            String CSId = chargeStationEntity.CSId;

            //主机信息
            JSONObject host = params.getJSONObject("host");
            //需要绑定的设备信息
            JSONArray devices = params.getJSONArray("devices");

            long meterId = 0;
            //region 电表信息
            if (StringUtils.hasLength(meterNo)) {
                ElectricityMeterEntity meterEntity = ElectricityMeterEntity.getInstance()
                        .where("meterNo", meterNo)
                        .where("CSId", CSId)
                        .findEntity();
                if (meterEntity == null) {
                    meterEntity = new ElectricityMeterEntity();
                    meterEntity.CSId = CSId;
                    meterEntity.meterNo = meterNo;
                    meterEntity.status = 1;
                    meterEntity.create_time = TimeUtil.getTimestamp();
                    meterEntity.id = meterEntity.insertGetId();
                }
                meterId = meterEntity.id;
            }
            //endregion

            DeviceEntity hostDeviceEntity = null;
            //region 主机设备同步
            String hostDeviceCode = "";
            if (host != null) {
                hostDeviceCode = host.getString("deviceCode");//主机设备码
                hostDeviceEntity = DeviceEntity.getInstance()
                        .where("deviceCode", hostDeviceCode)
                        .findEntity();
                if (hostDeviceEntity == null) {
                    //不存在数据，新增到数据库
                    hostDeviceEntity = new DeviceEntity();
                    hostDeviceEntity.deviceCode = hostDeviceCode;
                    hostDeviceEntity.deviceName = host.getString("deviceName");
                    hostDeviceEntity.deviceNumber = host.getString("deviceNumber");
                    hostDeviceEntity.deviceUnitId = host.getLong("deviceUnitId");
                    hostDeviceEntity.simCode = host.getString("simCode");
                    hostDeviceEntity.isHost = host.getIntValue("isHost", 1);
                    hostDeviceEntity.CSId = CSId;
                    hostDeviceEntity.meterId = meterId;
                    hostDeviceEntity.chargeStandardConfigId = SysGlobalConfigEntity.getInt("Default:ChargeStandard:ConfigId");
                    hostDeviceEntity.chargeTimeConfigId = SysGlobalConfigEntity.getInt("Default:ChargeTime:ConfigId");
                    hostDeviceEntity.parkingConfigId = host.getLong("parkingConfigId");
                    hostDeviceEntity.safeCharge = host.getIntValue("safeCharge", 0);
                    hostDeviceEntity.safeChargeFee = host.getDoubleValue("safeChargeFee");
                    hostDeviceEntity.display_status = host.getIntValue("safeCharge", 0);
                    hostDeviceEntity.id = hostDeviceEntity.insertGetId();

//                    if (hostDeviceEntity.id > 0) {
//                        //添加充电桩 - 设备关系
//                        ChargeStationDeviceEntity chargeStationDeviceEntity = new ChargeStationDeviceEntity();
//                        chargeStationDeviceEntity.CSId = CSId;
//                        chargeStationDeviceEntity.deviceId = hostDeviceEntity.id;
//                        chargeStationDeviceEntity.deviceCode = hostDeviceCode;
//                        chargeStationDeviceEntity.display_status = hostDeviceEntity.display_status;
//                        chargeStationDeviceEntity.addDevice();
//                    }
                }
            }
            //endregion

            //分机设备同步
            if (devices == null) return new SyncResult(10202, "缺少设备信息");

            long finalMeterId = meterId;
            DeviceEntity finalHostDeviceEntity = hostDeviceEntity;
            for (int i = 0; i < devices.size(); i++) {
                JSONObject device = devices.getJSONObject(i);

                String deviceCode = device.getString("deviceCode");//设备码
                if (!StringUtils.hasLength(deviceCode)) continue;

                //查询是否存在需要绑定的设备，如果不存在则新增
                DeviceEntity deviceEntity = DeviceEntity.getInstance()
                        .where("deviceCode", deviceCode)
                        .findEntity();

                //不存在数据，新增到数据库
                if (deviceEntity == null) {
                    deviceEntity = new DeviceEntity();
                    deviceEntity.deviceCode = deviceCode;
                    deviceEntity.deviceName = device.getString("deviceName");
                    deviceEntity.deviceNumber = device.getString("deviceNumber");
                    deviceEntity.deviceUnitId = device.getLong("deviceUnitId");
                    deviceEntity.simCode = device.getString("simCode");
                    deviceEntity.isHost = device.getIntValue("isHost", 0);
                    deviceEntity.CSId = CSId;
                    deviceEntity.meterId = finalMeterId;
                    if (finalHostDeviceEntity != null && deviceEntity.isHost != 1) {
                        deviceEntity.hostDeviceId = finalHostDeviceEntity.id;
                    }
                    deviceEntity.chargeStandardConfigId = SysGlobalConfigEntity.getInt("Default:ChargeStandard:ConfigId");
                    deviceEntity.chargeTimeConfigId = SysGlobalConfigEntity.getInt("Default:ChargeTime:ConfigId");
                    deviceEntity.parkingConfigId = SysGlobalConfigEntity.getInt("Default:Parking:ConfigId");
                    deviceEntity.safeCharge = device.getIntValue("safeCharge", 0);
                    deviceEntity.safeChargeFee = device.getDoubleValue("safeChargeFee");
                    deviceEntity.display_status = device.getIntValue("safeCharge", 0);
                    deviceEntity.id = deviceEntity.insertGetId();

                    DeviceSocketEntity.getInstance().addSocket(deviceEntity.id, 1, 2);
                }
                //存在数据，并且主机数据不为空
                else if (finalHostDeviceEntity != null) {
                    if (deviceEntity.hostDeviceId != finalHostDeviceEntity.id) {
                        Map<String, Object> setData = new LinkedHashMap<>();
                        setData.put("hostDeviceId", finalHostDeviceEntity.id);
                        deviceEntity.update(deviceEntity.id, setData);
                    }
                }
                //存在数据，但是主机信息为空，更新数据
                else {
                    Map<String, Object> setData = new LinkedHashMap<>();
                    setData.put("deviceName", device.get("deviceName"));
                    setData.put("deviceNumber", device.get("deviceNumber"));
                    setData.put("deviceUnitId", device.get("deviceUnitId"));
                    setData.put("simCode", device.get("simCode"));
                    setData.put("isHost", device.get("isHost"));
//                    setData.put("chargeStandardConfigId", SysGlobalConfigEntity.getInt("Default:ChargeStandard:ConfigId"));
//                    setData.put("chargeTimeConfigId", device.get("chargeTimeConfigId"));
//                    setData.put("parkingConfigId", device.get("parkingConfigId"));
//                    setData.put("safeCharge", device.get("safeCharge"));
//                    setData.put("safeChargeFee", device.get("safeChargeFee"));
                    setData.put("meterId", finalMeterId);
                    if ("".equals(deviceEntity.CSId) || "0".equals(deviceEntity.CSId)) {
                        setData.put("CSId", device.get("CSId"));
                    }
                    deviceEntity.update(deviceEntity.id, setData);
                }
            }
            return new SyncResult(0, "");

        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "RestAPI 绑定设备发生错误");
        }
        return new SyncResult(10201, "绑定失败");
    }
    //endregion
}
