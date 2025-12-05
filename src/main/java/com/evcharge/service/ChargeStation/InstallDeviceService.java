package com.evcharge.service.ChargeStation;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.admin.AdminBaseEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceUnitEntity;
import com.evcharge.entity.device.EGDeviceViewEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.station.InstallWorkOrderEntity;
import com.evcharge.qrcore.parser.QRCoreParser;
import com.evcharge.qrcore.parser.base.QRContent;
import com.evcharge.service.GeneralDevice.GeneralDeviceService;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

/**
 * 安装设备业务逻辑
 */
public class InstallDeviceService {

    public static InstallDeviceService getInstance() {
        return new InstallDeviceService();
    }

    /**
     * 创建订单
     *
     * @param CSId        站点ID
     * @param installerId 安装ID
     */
    public SyncResult createOrder(String CSId, long installerId) {
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance()
                .where("id", CSId)
                .findEntity();
        if (chargeStationEntity == null || chargeStationEntity.id == 0) {
            return new SyncResult(1, "查询不到此充电桩数据");
        }
        //状态：0=删除，1=正常，2=在建
        if (chargeStationEntity.status != 2) {
            return new SyncResult(1, "此充电桩已在运行中，无需要下安装工单");
        }

        InstallWorkOrderEntity orderEntity = InstallWorkOrderEntity.getInstance();
        if (orderEntity.where("CSId", Convert.toLong(CSId)).exist()) {
            return new SyncResult(1, "已存在工单，无需再创建");
        }

        orderEntity.OrderSN = common.randomStr(2).toUpperCase()
                + TimeUtil.toTimeString("yyyyMMddHHmmssSSS")
                + common.randomInt(100, 999);
        orderEntity.installer_id = installerId;
        orderEntity.CSId = CSId;
        orderEntity.status = 1;//状态：0=待确认，1=已确认，2=已撤销，3=进行中，4=竣工
        orderEntity.creator_id = 1;//默认1
        orderEntity.create_ip = HttpRequestUtil.getIP();
        orderEntity.create_time = TimeUtil.getTimestamp();
        if (orderEntity.insert() == 0) {
            return new SyncResult(1, "创建工单失败");
        }
        return new SyncResult(0, "创建工单成功");
    }

    /**
     * 充电桩安装设备时扫描二维码内容进行解析
     *
     * @param qrCoreContent 二维码内容
     */
    public ISyncResult QRParserHandle(String qrCoreContent) {
        QRContent result = QRCoreParser.parse(qrCoreContent);
        if (!result.success) return new SyncResult(3, "解析二维码失败");
        if (StringUtil.isEmpty(result.device_code)) return new SyncResult(4, "无法识别到设备编码");
        Map<String, Object> data = new LinkedHashMap<>();
        switch (result.type) {
            case EvDevice:
                data = getDeviceProfileByDeviceCode(result.device_code);
                break;
            case GeneraDevice:
                data = getGeneralDeviceProfileBySerialNumber(result.device_code);
                break;
            case URL:
            case TEXT:
                break;
            default:
                return new SyncResult(1, "未知二维码，无法识别设备");
        }
        return new SyncResult(0, "", data);
    }

    // region remark - 绑定设备

    /**
     * 绑定充电设备
     *
     * @param OrderSN    安装订单号
     * @param deviceCode 设备编号-通过扫描二维码获得
     * @param adminId    操作管理员id
     */
    public ISyncResult bindDevice(String OrderSN, String deviceCode, long adminId) {
        if (!StringUtils.hasLength(OrderSN)) new SyncResult(2, "缺少[订单号]参数");
        if (!StringUtils.hasLength(deviceCode)) new SyncResult(2, "缺少[设备码]参数");

        InstallWorkOrderEntity installWorkOrderEntity = InstallWorkOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();
        if (installWorkOrderEntity == null || installWorkOrderEntity.id == 0) return new SyncResult(3, "工单不存在");
        if (installWorkOrderEntity.installer_id != adminId) return new SyncResult(4, "您没有权限操作此工单");
        // 状态：0=待确认，1=已确认，2=已撤销，3=进行中，4=竣工
        if (installWorkOrderEntity.status == 1) {
            InstallWorkOrderEntity.getInstance().where("id", installWorkOrderEntity.id).update(new HashMap<>() {{
                put("status", 3);//进行中
            }});
        }

        //查询充电桩信息
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance()
                .where("CSId", installWorkOrderEntity.CSId)
                .findEntity();
        if (chargeStationEntity == null || chargeStationEntity.id == 0) return new SyncResult(5, "充电桩信息错误");

        // 查询充电设备
        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode, false);
        if (deviceEntity != null && deviceEntity.id != 0) {
            // 检查SIM卡号
            if (StringUtil.isEmpty(deviceEntity.simCode)) return new SyncResult(7, "设备SIM卡数据还没上传，请稍后再试");
            ISyncResult r = bindEvDevice(chargeStationEntity, deviceEntity);
            if (r.isSuccess()) {
                // 清空缓存操作
                List<Map<String, Object>> list = DeviceEntity.getInstance()
                        .field("id,deviceNumber,deviceCode")
                        .where("CSId", chargeStationEntity.CSId)
                        .select();
                if (list != null) {
                    for (Map<String, Object> d : list) {
                        long id = MapUtil.getLong(d, "id");
                        String number = MapUtil.getString(d, "deviceNumber");
                        String code = MapUtil.getString(d, "deviceCode");

                        DataService.getMainCache().del(String.format("Device:%s:Details", number));
                        DataService.getMainCache().del(String.format("Device:%s:Details", code));
                        DataService.getMainCache().del(String.format("Device:%s:ChargeStation", id));
                    }
                }

                chargeStationEntity.syncSocketCount(chargeStationEntity.CSId);

                LogsUtil.info("", "管理员[%s] 在 %s 中安装绑定了设备[%s][%s - %s]"
                        , AdminBaseEntity.getInstance().getWithId(adminId).account
                        , chargeStationEntity.name
                        , deviceEntity.deviceName
                        , deviceEntity.deviceCode
                        , deviceEntity.deviceNumber);
            }
            return r;
        }

        // 查询通用设备
        GeneralDeviceEntity generalDeviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(deviceCode, false);
        if (generalDeviceEntity != null && generalDeviceEntity.id != 0) {
            return bindGeneralDevice(chargeStationEntity, generalDeviceEntity);
        }
        return new SyncResult(1, "操作失败");
    }

    /**
     * 绑定充电设备
     * <p>
     * 开始事务
     * 1、查询主机设备ID
     * 2、通过simCode更新分机的信息
     * 3、通过simCode更新主机的信息
     *
     * @param chargeStationEntity 站点实体类
     * @param deviceEntity        充电设备实体类
     */
    public ISyncResult bindEvDevice(ChargeStationEntity chargeStationEntity, DeviceEntity deviceEntity) {
        return DataService.getMainDB().beginTransaction(connection -> {
            long hostDeviceId = 0;
            //检查是否是主机
            if (deviceEntity.isHost == 1) hostDeviceId = deviceEntity.id;
            if (hostDeviceId == 0) {
                DeviceEntity hostDeviceEntity = DeviceEntity.getInstance()
                        .where("simCode", deviceEntity.simCode)
                        .where("isHost", 1)
                        .findEntity();
                if (hostDeviceEntity == null || hostDeviceEntity.id == 0) {
                    return new SyncResult(10, "无法通过SIM卡号查询主机");
                }
                hostDeviceId = hostDeviceEntity.id;
            }

            //通过simCode更新分机的信息
            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("hostDeviceId", hostDeviceId);
            set_data.put("organize_code", chargeStationEntity.organize_code);
            set_data.put("CSId", chargeStationEntity.CSId);
            DeviceEntity.getInstance()
                    .where("simCode", deviceEntity.simCode)
                    .where("isHost", 0)
                    .update(set_data);

            //通过simCode更新主机的信息
            DeviceEntity.getInstance()
                    .where("id", hostDeviceId)
                    .update(new LinkedHashMap<>() {{
                        put("CSId", chargeStationEntity.CSId);
                    }});
            return new SyncResult(0, "");
        });
    }

    /**
     * 绑定通用设备
     *
     * @param chargeStationEntity 站点实体类
     * @param deviceEntity        通用设备实体类
     */
    public ISyncResult bindGeneralDevice(ChargeStationEntity chargeStationEntity, GeneralDeviceEntity deviceEntity) {
        deviceEntity.where("serialNumber", deviceEntity.serialNumber)
                .update(new LinkedHashMap<>() {{
                    put("CSId", chargeStationEntity.CSId);
                }});
        return new SyncResult(0, "");
    }

    // endregion

    // region remark - 解绑设备

    /**
     * 解绑设备
     */
    public ISyncResult unbindDevice(String OrderSN, String deviceCode, long adminId) {
        if (!StringUtils.hasLength(OrderSN)) return new SyncResult(2, "缺少[订单号]参数");
        if (!StringUtils.hasLength(deviceCode)) return new SyncResult(2, "缺少[设备码]参数");

        InstallWorkOrderEntity installWorkOrderEntity = InstallWorkOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();
        if (installWorkOrderEntity == null || installWorkOrderEntity.id == 0) return new SyncResult(3, "工单不存在");
        if (installWorkOrderEntity.installer_id != adminId) return new SyncResult(4, "您没有权限操作此工单");

        // 查询充电设备
        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode, false);
        if (deviceEntity != null && deviceEntity.id != 0) {
            return unbindEvDevice(installWorkOrderEntity, deviceEntity);
        }

        GeneralDeviceEntity generalDeviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(deviceCode, false);
        if (generalDeviceEntity != null && generalDeviceEntity.id != 0) {
            return unbindGeneralDevice(installWorkOrderEntity, generalDeviceEntity);
        }
        return new SyncResult(1, "操作失败");
    }

    /**
     * 解绑充电设备
     *
     * @param installWorkOrderEntity 安装订单实体类
     * @param deviceEntity           充电设备实体类
     */
    public ISyncResult unbindEvDevice(InstallWorkOrderEntity installWorkOrderEntity, DeviceEntity deviceEntity) {
        if ("".equals(deviceEntity.CSId) || "0".equals(deviceEntity.CSId)) {
            return new SyncResult(6, "此设备没有进行过任何绑定");
        }
        if (!Objects.equals(deviceEntity.CSId, installWorkOrderEntity.CSId)) {
            return new SyncResult(7, "您没有权限解绑其他的设备");
        }

        //如果是主机的话，所有从机都会进行解绑，需要工程师重新扫码绑定所有从机
        SyncResult r = DataService.getMainDB().beginTransaction(connection -> {
            //设定待解绑的列表
            List<DeviceEntity> unbindList = new LinkedList<>();
            if (deviceEntity.isHost == 1) {
                //如果是主机则读取所有的从机
                unbindList = DeviceEntity.getInstance()
                        .where("hostDeviceId", deviceEntity.id)
                        .selectList();
                unbindList.add(deviceEntity);
            }
            unbindList.add(deviceEntity);

            List<Object> ids = new LinkedList<>();
            for (DeviceEntity nd : unbindList) {
                ids.add(nd.id);
            }

            int noquery = DeviceEntity.getInstance()
                    .whereIn("id", ids)
                    .update(new HashMap<>() {{
                        put("hostDeviceId", 0);
                    }});
            if (noquery == 0) return new SyncResult(11, "更新设备主机ID失败");

            return new SyncResult(0, "");
        });
        if (r.code != 0) return r;

        ChargeStationEntity.getInstance().syncSocketCount(installWorkOrderEntity.CSId);
        LogsUtil.info("", "管理员[%s] 在 %s 中解绑设备[%s][%s - %s]"
                , AdminBaseEntity.getInstance().getWithId(installWorkOrderEntity.installer_id).account
                , installWorkOrderEntity.OrderSN
                , deviceEntity.deviceName
                , deviceEntity.deviceCode
                , deviceEntity.deviceNumber);

        DataService.getMainCache().del(String.format("Device:%s:Details", deviceEntity.deviceNumber));
        DataService.getMainCache().del(String.format("Device:%s:Details", deviceEntity.deviceCode));
        DataService.getMainCache().del(String.format("Device:%s:ChargeStation", deviceEntity.id));

        return r;
    }

    /**
     * 解绑充电设备
     *
     * @param installWorkOrderEntity 安装订单实体类
     * @param deviceEntity           充电设备实体类
     */
    public ISyncResult unbindGeneralDevice(InstallWorkOrderEntity installWorkOrderEntity, GeneralDeviceEntity deviceEntity) {
        if ("".equals(deviceEntity.CSId) || "0".equals(deviceEntity.CSId)) {
            return new SyncResult(6, "此设备没有进行过任何绑定");
        }
        if (!Objects.equals(deviceEntity.CSId, installWorkOrderEntity.CSId)) {
            return new SyncResult(7, "您没有权限解绑其他的设备");
        }

        GeneralDeviceEntity.getInstance()
                .whereIn("serialNumber", deviceEntity.serialNumber)
                .update(new HashMap<>() {{
                    put("CSId", "0");
                }});
        return new SyncResult(0, "");
    }

    // endregion

    /**
     * 获取已经绑定的设备列表
     */
    public ISyncResult getBindDeviceList(String OrderSN, long admin_id, int page, int limit) {
        if (!StringUtils.hasLength(OrderSN)) common.apicb(2, "缺少[订单号]参数");

        if (page <= 0) page = 1;
        if (limit <= 0) limit = 100;

        InstallWorkOrderEntity installWorkOrderEntity = InstallWorkOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();
        if (installWorkOrderEntity == null || installWorkOrderEntity.id == 0) return new SyncResult(3, "工单不存在");
        if (installWorkOrderEntity.installer_id != admin_id) return new SyncResult(4, "您没有权限操作此工单");

        int count = EGDeviceViewEntity.getInstance()
                .where("CSId", installWorkOrderEntity.CSId)
                .count();
        if (count == 0) return new SyncResult(1, "");

        List<EGDeviceViewEntity> list = EGDeviceViewEntity.getInstance()
                .where("CSId", installWorkOrderEntity.CSId)
                .page(page, limit)
                .selectList();
        if (list.isEmpty()) return new SyncResult(1, "");
        return new SyncListResult(count, page, limit, list);
    }

    /**
     * 通过设备码获取设备的所有信息
     *
     * @param deviceCode 设备码
     */
    public Map<String, Object> getDeviceProfileByDeviceCode(@NonNull String deviceCode) {
        Map<String, Object> params = new LinkedHashMap<>();

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode, false);
        if (deviceEntity == null || deviceEntity.id == 0) return null;

        params.put("id", deviceEntity.id);
        params.put("deviceName", deviceEntity.deviceName);
        params.put("deviceCode", deviceEntity.deviceCode);
        params.put("deviceNumber", deviceEntity.deviceNumber);
        params.put("isHost", deviceEntity.isHost);

        Map<String, Object> deviceUnit = DeviceUnitEntity.getInstance().getBySpuCode(deviceEntity.spuCode);
        if (deviceUnit != null && !deviceUnit.isEmpty()) {
            params.put("deviceUnitName", deviceUnit.get("name"));
            params.put("previewImage", deviceUnit.get("previewImage"));
            params.put("maxPower", deviceUnit.get("maxPower"));
            params.put("brandName", deviceUnit.get("brandName"));
            params.put("TypeName", deviceUnit.get("TypeName"));
        }
        return params;
    }

    /**
     * 通过设备码获取设备的所有信息
     *
     * @param serialNumber 设备码
     */
    public Map<String, Object> getGeneralDeviceProfileBySerialNumber(@NonNull String serialNumber) {
        Map<String, Object> params = new LinkedHashMap<>();

        GeneralDeviceEntity deviceEntity = GeneralDeviceEntity.getInstance().getBySerialNumber(serialNumber, false);
        if (deviceEntity == null || deviceEntity.id == 0) return null;

        params.put("id", deviceEntity.id);
        params.put("deviceName", deviceEntity.deviceName);
        params.put("deviceCode", deviceEntity.serialNumber);
        params.put("deviceNumber", deviceEntity.serialNumber);
        params.put("isHost", 0);

        Map<String, Object> deviceUnit = DeviceUnitEntity.getInstance().getBySpuCode(deviceEntity.spuCode);
        if (deviceUnit != null && !deviceUnit.isEmpty()) {
            params.put("deviceUnitName", deviceUnit.get("name"));
            params.put("previewImage", deviceUnit.get("previewImage"));
            params.put("maxPower", deviceUnit.get("maxPower"));
            params.put("brandName", deviceUnit.get("brandName"));
            params.put("TypeName", deviceUnit.get("TypeName"));
        }
        return params;
    }
}
