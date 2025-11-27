package com.evcharge.service.GeneralDevice;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceUnitEntity;
import com.evcharge.entity.device.GeneralDeviceConfigEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.libsdk.tq.TQDianBiaoSDK;
import com.evcharge.service.ChargeStation.ChargeStationService;
import com.evcharge.service.notify.ITransDataBuilder;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用设备 - 业务逻辑层
 * 负责处理通用设备的业务逻辑
 */
public class GeneralDeviceService {

    private final static String TAG = "通用设备";

    /**
     * 获得一个实例
     */
    public static GeneralDeviceService getInstance() {
        return new GeneralDeviceService();
    }

    /**
     * 自动注册设备的方法。此方法用于根据设备序列号（serialNumber）和SPU编码（spuCode）
     * 在数据库中自动创建新的设备记录和配置记录。
     *
     * @param serialNumber 设备序列号，用于唯一标识设备。
     * @param spuCode      设备的SPU编码，用于识别设备单元信息。
     * @return 返回一个包含结果状态和消息的 {@link SyncResult} 对象。
     * - 如果设备单元信息不存在，返回状态码为 1，并包含错误信息。
     * - 如果设备已存在（即序列号已经存在于数据库中），返回状态码为 1，并包含错误信息。
     * - 如果设备数据和配置数据成功插入，返回状态码为 0。
     * - 如果插入设备数据失败，返回状态码为 11，并包含错误信息。
     * - 如果插入配置数据失败，返回状态码为 12，并包含错误信息。
     */
    @NonNull
    public SyncResult Register(@NonNull String serialNumber, @NonNull String spuCode) {
        return Register(new GeneralDeviceEntity(), serialNumber, spuCode);
    }

    /**
     * 自动注册设备的方法。此方法用于根据设备序列号（serialNumber）和SPU编码（spuCode）
     * 在数据库中自动创建新的设备记录和配置记录。
     *
     * @param serialNumber 设备序列号，用于唯一标识设备。
     * @param spuCode      设备的SPU编码，用于识别设备单元信息。
     * @return 返回一个包含结果状态和消息的 {@link SyncResult} 对象。
     * - 如果设备单元信息不存在，返回状态码为 1，并包含错误信息。
     * - 如果设备已存在（即序列号已经存在于数据库中），返回状态码为 1，并包含错误信息。
     * - 如果设备数据和配置数据成功插入，返回状态码为 0。
     * - 如果插入设备数据失败，返回状态码为 11，并包含错误信息。
     * - 如果插入配置数据失败，返回状态码为 12，并包含错误信息。
     */
    @NonNull
    public SyncResult Register(@NonNull GeneralDeviceEntity entity, @NonNull String serialNumber, @NonNull String spuCode) {
        // 查询设备单元信息
        DeviceUnitEntity unitEntity = DeviceUnitEntity.getInstance().getWithSpuCode(spuCode);
        if (unitEntity == null) {
            // 如果设备单元信息不存在，返回错误结果
            return new SyncResult(1, String.format("%s - %s 自动注册失败，无设备单元数据", spuCode, serialNumber));
        }

        // 检查是否存在相同序列号的设备
        GeneralDeviceEntity device = getWithSerialNumber(serialNumber);
        if (device != null && device.id != 0) {
            // 如果设备已存在，返回错误结果
            return new SyncResult(1, String.format("%s - %s 自动注册失败，存在相同数据", spuCode, serialNumber));
        }

        entity.serialNumber = serialNumber;
        entity.spuCode = spuCode;
        if (!StringUtils.hasLength(entity.simCode)) entity.simCode = "";
        if (!StringUtils.hasLength(entity.batchNumber)) entity.batchNumber = "";
        if (!StringUtils.hasLength(entity.organize_code)) entity.organize_code = "";
        if (!StringUtils.hasLength(entity.platform_code)) entity.platform_code = "";

        // 开始事务处理，插入设备数据和配置数据
        return GeneralDeviceEntity.getInstance().beginTransaction(connection -> {
            // 准备插入的设备数据
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("deviceName", unitEntity.name);                 // 设备名称
            data.put("serialNumber", serialNumber);                  // 设备序列号
            data.put("CSId", "");                                    // CSId字段，暂时为空
            data.put("spuCode", spuCode);                            // 设备的SPU编码
            data.put("brandCode", unitEntity.brandCode);             // 品牌编码
            data.put("typeCode", unitEntity.typeCode);               // 类型编码
            data.put("online_status", 0);                            // 在线状态，0表示离线，1表示在线
            data.put("status", 1);                                   // 状态，1表示正常，0表示删除
            data.put("simCode", entity.simCode);                            // SIM卡编码
            data.put("batchNumber", entity.batchNumber);                    // 批次号
            data.put("spec", unitEntity.spec);                       // 规格信息
            data.put("dynamic_info", "{}");                          // 动态信息，默认为空的JSON对象
            data.put("mainSerialNumber", "");                        // 主设备序列号，默认为空
            data.put("remark", "");                                  // 备注信息，默认为空
            data.put("organize_code", entity.organize_code);                // 组织代码
            data.put("platform_code", entity.platform_code);                // 平台代码
            data.put("create_time", TimeUtil.getTimestamp());        // 创建时间戳
            data.put("update_time", TimeUtil.getTimestamp());        // 更新时间戳
            data.put("delete_time", 0);                              // 删除时间，默认为0

            // 尝试插入设备数据
            if (GeneralDeviceEntity.getInstance().insertTransaction(connection, data) == 0) {
                return new SyncResult(11, "新增通用设备数据失败");
            }

            // 准备插入的配置数据
            Map<String, Object> config_data = new LinkedHashMap<>();
            config_data.put("serialNumber", serialNumber);           // 设备序列号
            config_data.put("config", unitEntity.config);            // 设备配置数据

            // 尝试插入设备配置数据
            if (GeneralDeviceConfigEntity.getInstance().insertTransaction(connection, config_data) == 0) {
                return new SyncResult(12, "新增通用设备配置数据失败");
            }

            LogsUtil.info(GeneralDeviceEntity.class.getSimpleName(), "[%s] 自动注册通用设备数据成功 - %s", serialNumber, spuCode);

            return new SyncResult(0, "");
        });
    }

    /**
     * 读取设备信息
     *
     * @param serialNumber 序列号
     * @return 监控设备实体类
     */
    public GeneralDeviceEntity getWithSerialNumber(String serialNumber) {
        return getWithSerialNumber(serialNumber, true);
    }

    /**
     * 读取设备信息
     *
     * @param serialNumber 序列号
     * @param inCache      是否优先读取缓存
     * @return 监控设备实体类
     */
    public GeneralDeviceEntity getWithSerialNumber(String serialNumber, boolean inCache) {
        if (!StringUtils.hasLength(serialNumber)) return null;
        if (inCache) GeneralDeviceEntity.getInstance().cache(String.format("GeneralDevice:%s:Details", serialNumber));
        return GeneralDeviceEntity.getInstance().where("serialNumber", serialNumber).findEntity();
    }

    /**
     * 传入设备信息，会根据CSId、mainSerialNumber、serialNumber顺序查询到对应的子设备信息
     *
     * @param deviceEntity 设备信息
     * @param typeCode     设备类型
     */
    public GeneralDeviceEntity getSubDevice(GeneralDeviceEntity deviceEntity, String typeCode) {
        return getSubDevice(deviceEntity, typeCode, true);
    }

    /**
     * 传入设备信息，会根据CSId、mainSerialNumber、serialNumber顺序查询到对应的子设备信息
     *
     * @param deviceEntity 设备信息
     * @param typeCode     设备类型
     * @param inCache      优先从缓存中获取
     */
    public GeneralDeviceEntity getSubDevice(GeneralDeviceEntity deviceEntity, String typeCode, boolean inCache) {
        GeneralDeviceEntity device = getSubDevice(deviceEntity.mainSerialNumber, typeCode, inCache);
        if (device == null) {
            device = getSubDevice(deviceEntity.serialNumber, typeCode, inCache);
        }
        return device;
    }

    /**
     * 根据 序列号、设备类型 读取子设备：例如：根据摄像头获取4G远程控制开关等关联起来的设备
     *
     * @param mainSerialNumber 主设备序列号
     * @param typeCode         设备类型
     */
    public GeneralDeviceEntity getSubDevice(String mainSerialNumber, String typeCode) {
        return getSubDevice(mainSerialNumber, typeCode, true);
    }

    /**
     * 根据 序列号、设备类型 读取子设备：例如：根据摄像头获取4G远程控制开关等关联起来的设备
     *
     * @param mainSerialNumber 主设备序列号
     * @param typeCode         设备类型
     */
    public GeneralDeviceEntity getSubDevice(String mainSerialNumber, String typeCode, boolean inCache) {
        if (!StringUtils.hasLength(mainSerialNumber)) return null;
        if (!StringUtils.hasLength(typeCode)) return null;

        GeneralDeviceEntity main = getWithSerialNumber(mainSerialNumber);
        if (main == null || main.id == 0) return null;

        GeneralDeviceEntity entity = GeneralDeviceEntity.getInstance();
        if (inCache) entity.cache(String.format("GeneralDevice:%s:Sub:%s", mainSerialNumber, typeCode));

        return entity.where("mainSerialNumber", mainSerialNumber)
                .where("typeCode", typeCode)
                .order("id")
                .findEntity();
    }

    /**
     * 绑定充电站站点
     *
     * @param serialNumber 设备序列号
     * @param CSId         充电站站点编码
     */
    public SyncResult bindChargeStation(String serialNumber, String CSId) {
        if (!StringUtil.hasLength(serialNumber)) return new SyncResult(2, "请选择设备");
        if (!StringUtil.hasLength(CSId)) return new SyncResult(2, "请选择站点");

        // 查询设备数据
        GeneralDeviceEntity deviceEntity = getWithSerialNumber(serialNumber, false);
        if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "无效的主设备");

        // 检查绑定
        if (!StringUtils.hasLength(deviceEntity.CSId) || !"0".equalsIgnoreCase(deviceEntity.CSId)) {
            return new SyncResult(4, "设备已绑定到其他站点上");
        }

        int noquery = GeneralDeviceEntity.getInstance()
                .whereOr("serialNumber", serialNumber)
                .whereOr("mainSerialNumber", serialNumber)
                .update(new LinkedHashMap<>() {{
                    put("CSId", CSId);
                }});
        if (noquery == 0) return new SyncResult(1, "操作失败");
        return new SyncResult(0, "绑定成功");
    }

    /**
     * 解绑充电站站点
     *
     * @param serialNumber 设备序列号
     */
    public SyncResult unbindChargeStation(String serialNumber) {
        if (!StringUtil.hasLength(serialNumber)) return new SyncResult(2, "请选择设备");

        // 查询设备数据
        GeneralDeviceEntity deviceEntity = getWithSerialNumber(serialNumber, false);
        if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "无效的主设备");
        int noquery = GeneralDeviceEntity.getInstance()
                .whereOr("serialNumber", serialNumber)
                .whereOr("mainSerialNumber", serialNumber)
                .update(new LinkedHashMap<>() {{
                    put("CSId", "0");
                }});
        if (noquery == 0) return new SyncResult(1, "操作失败");
        return new SyncResult(0, "绑定成功");
    }

    /**
     * 子设备绑定操作
     * 该方法用于将一个子设备绑定到一个主设备上。主设备和子设备通过各自的序列号进行标识。
     *
     * @param mainSerialNumber 主设备的序列号
     * @param serialNumber     子设备的序列号
     * @return SyncResult 返回绑定操作的结果。可能的结果代码包括：
     * - 0: 绑定成功
     * - 1: 操作失败
     * - 2: 参数不完整或无效（主设备或子设备序列号为空）
     * - 3: 主设备或子设备无效（未找到对应的设备）
     * - 4: 子设备已绑定到其他设备
     */
    public SyncResult bindSubDevice(String mainSerialNumber, String serialNumber) {
        // 检查主设备序列号是否为空
        if (!StringUtil.hasLength(mainSerialNumber)) return new SyncResult(2, "无效主设备序列号");

        // 检查子设备序列号是否为空
        if (!StringUtil.hasLength(serialNumber)) return new SyncResult(2, "无效子设备序列号");

        // 查询主设备数据，通过主设备的序列号获取设备实例
        GeneralDeviceEntity mainDeviceEntity = getWithSerialNumber(mainSerialNumber, false);
        // 如果未找到主设备或主设备ID为0，返回无效的主设备错误
        if (mainDeviceEntity == null || mainDeviceEntity.id == 0) return new SyncResult(3, "无效的主设备");

        // 查询子设备数据，通过子设备的序列号获取设备实例
        GeneralDeviceEntity subDeviceEntity = getWithSerialNumber(serialNumber, false);
        // 如果未找到子设备或子设备ID为0，返回无效的子设备错误
        if (subDeviceEntity == null || subDeviceEntity.id == 0) return new SyncResult(3, "无效的子设备");

        // 检查子设备是否已经绑定到其他设备上
        // 如果子设备的主设备序列号不为空且不等于当前主设备的序列号，则说明子设备已经绑定到其他设备
        if (StringUtils.hasLength(subDeviceEntity.mainSerialNumber)
                && !mainDeviceEntity.serialNumber.equalsIgnoreCase(subDeviceEntity.mainSerialNumber)) {
            return new SyncResult(4, "子设备已绑定到其他设备上");
        }

        // 检查子设备是否已经绑定到当前主设备
        // 如果子设备的主设备序列号和CSId都与当前主设备相同，则表示设备已经成功绑定
        if (mainDeviceEntity.serialNumber.equalsIgnoreCase(subDeviceEntity.mainSerialNumber)
                && mainDeviceEntity.CSId.equalsIgnoreCase(subDeviceEntity.CSId)) {
            return new SyncResult(0, "设备已绑定");
        }

        // 更新子设备的主设备序列号和CSId
        // 通过序列号查询子设备并更新其主设备序列号和CSId
        int noquery = GeneralDeviceEntity.getInstance()
                .whereOr("serialNumber", mainSerialNumber)
                .whereOr("serialNumber", serialNumber)
                .update(new LinkedHashMap<>() {{
                    put("mainSerialNumber", mainSerialNumber);
                    put("CSId", mainDeviceEntity.CSId);
                }});

        if (noquery == 0) return new SyncResult(1, "操作失败");
        return new SyncResult(0, "设备绑定成功");
    }

    /**
     * 子设备解绑操作
     * 该方法用于将一个子设备从其绑定的主设备上解绑。解绑后，子设备的主设备序列号和CSId将被清空。
     *
     * @param serialNumber 子设备的序列号
     * @return SyncResult 返回解绑操作的结果。可能的结果代码包括：
     * - 0: 解绑成功
     * - 1: 操作失败
     * - 2: 参数不完整或无效（子设备序列号为空）
     * - 3: 子设备无效（未找到对应的设备）
     */
    public SyncResult unBindSubDevice(String serialNumber) {
        // 检查子设备序列号是否为空
        if (!StringUtil.hasLength(serialNumber)) return new SyncResult(2, "请输入设备序列号");

        // 查询子设备数据，通过子设备的序列号获取设备实例
        GeneralDeviceEntity subDeviceEntity = getWithSerialNumber(serialNumber, false);
        if (subDeviceEntity == null || subDeviceEntity.id == 0) return new SyncResult(3, "无效的设备");

        // 更新子设备的主设备序列号和CSId，将其清空
        // 通过序列号查询子设备并更新其主设备序列号和CSId为空值
        int noquery = GeneralDeviceEntity.getInstance()
                .where("serialNumber", serialNumber)
                .update(new LinkedHashMap<>() {{
                    put("mainSerialNumber", "");  // 清空主设备序列号
                    put("CSId", "");               // 清空CSId
                }});

        if (noquery == 0) return new SyncResult(1, "操作失败");
        return new SyncResult(0, "设备解绑成功");
    }

    /**
     * 根据 充电桩、设备类型 读取设备
     *
     * @param CSId     充电桩
     * @param typeCode 设备类型
     */
    public GeneralDeviceEntity getWithCSId(String CSId, String typeCode) {
        return getWithCSId(CSId, typeCode, true);
    }

    /**
     * 根据 充电桩、设备类型 读取设备
     *
     * @param CSId     充电桩
     * @param typeCode 设备类型
     */
    public GeneralDeviceEntity getWithCSId(String CSId, String typeCode, boolean inCache) {
        if (!StringUtils.hasLength(CSId)) return null;
        if ("0".equalsIgnoreCase(CSId)) return null;
        if (!StringUtils.hasLength(typeCode)) return null;

        GeneralDeviceEntity entity = GeneralDeviceEntity.getInstance();
        if (inCache) entity.cache(String.format("GeneralDevice:%s:%s", CSId, typeCode));
        return entity.where("CSId", CSId)
                .where("typeCode", typeCode)
                .order("id")
                .findEntity();
    }

    /**
     * 根据 充电桩、设备类型 读取设备
     *
     * @param CSId     充电桩
     * @param typeCode 设备类型
     */
    public List<GeneralDeviceEntity> getListWithCSId(String CSId, String typeCode) {
        return getListWithCSId(CSId, typeCode, true);
    }

    /**
     * 根据 充电桩、设备类型 读取设备
     *
     * @param CSId     充电桩
     * @param typeCode 设备类型
     */
    public List<GeneralDeviceEntity> getListWithCSId(String CSId, String typeCode, boolean inCache) {
        GeneralDeviceEntity entity = GeneralDeviceEntity.getInstance();
        if (inCache) entity.cache(String.format("GeneralDevice:%s:%s:List", CSId, typeCode));
        return entity.where("CSId", CSId)
                .where("typeCode", typeCode)
                .order("id")
                .selectList();
    }

    /**
     * 获取绑定的充电桩信息
     *
     * @param serialNumber 序列号
     * @return 充电桩实体类信息
     */
    public ChargeStationEntity getChargeStation(String serialNumber) {
        return GeneralDeviceEntity.getInstance().field("a.*")
                .cache(String.format("ChargeStation:GeneralDevice:%s", serialNumber))
                .alias("a")
                .join(GeneralDeviceEntity.getInstance().theTableName(), "b", "a.CSId = b.CSId")
                .where("b.serialNumber", serialNumber)
                .findEntity();
    }

    /**
     * 更新在线状态
     *
     * @param online_status 在线状态：0=离线，1=在线
     * @return 是否更新成功
     */
    public boolean updateOnlineStatus(@NonNull String serialNumber, int online_status) {
        online_status = online_status == 1 ? 1 : 0;
        DataService.getMainCache().set(String.format("GeneralDevice:%s:status", serialNumber), online_status, 30 * ECacheTime.MINUTE);

        Map<String, Object> set_data = new LinkedHashMap<>();
        set_data.put("online_status", online_status);//在线状态：0=离线，1=在线
        set_data.put("update_time", TimeUtil.getTimestamp());
        return GeneralDeviceEntity.getInstance()
                .where("serialNumber", serialNumber)
                .update(set_data) > 0;
    }

    /**
     * 更新动态信息
     *
     * @param info 动态信息
     * @return 是否更新成功
     */
    public boolean updateDynamicInfo(@NonNull String serialNumber, JSONObject info) {
        GeneralDeviceEntity entity = GeneralDeviceEntity.getInstance();

        JSONObject dynamic_info = DataService.getMainCache().getJSONObject(String.format("GeneralDevice:%s:DynamicInfo", serialNumber));
        if (dynamic_info == null) {
            Map<String, Object> data = entity.field("dynamic_info")
                    .where("serialNumber", serialNumber)
                    .find();
            if (data != null && !data.isEmpty()) {
                String dynamic_info_string = MapUtil.getString(data, "dynamic_info");
                dynamic_info = JsonUtil.toJSON(dynamic_info_string, new JSONObject());
            } else dynamic_info = new JSONObject();
        }
        dynamic_info.putAll(info);

        Map<String, Object> set_data = new LinkedHashMap<>();
        set_data.put("dynamic_info", dynamic_info.toJSONString());
        set_data.put("update_time", TimeUtil.getTimestamp());

        return entity.where("serialNumber", serialNumber).update(set_data) > 0;
    }

    /**
     * 修复simCode
     *
     * @param serialNumber 序列号
     * @param simCode      sim编码
     */
    public boolean updateSimCode(String serialNumber, String simCode) {
        // 参数有效性校验
        if (!StringUtils.hasLength(serialNumber) || !StringUtils.hasLength(simCode)) {
            return false;
        }

        GeneralDeviceEntity deviceEntity = getWithSerialNumber(serialNumber);
        // 设备不存在，直接返回false
        if (deviceEntity == null) return false;
        // 如果sim已经更新并且设备已是主机或已有有效的主机设备ID，无需进一步更新
        if (deviceEntity.simCode.equalsIgnoreCase(simCode)) return true;

        Map<String, Object> set_data = new LinkedHashMap<>();
        set_data.put("simCode", simCode);
        // 更新设备信息
        deviceEntity.where("serialNumber", serialNumber).update(set_data);
        LogsUtil.info(this.getClass().getSimpleName(), "[%s] 修复监控设备simCode", serialNumber);

        DataService.getMainCache().del(String.format("GeneralDevice:%s:Details", serialNumber));
        return true;
    }

    //region 设备监控

    /**
     * 设备监控任务
     *
     * @param serialNumber 序列号
     */
    @Deprecated
    public SyncResult monitorTask(String serialNumber) {
        // 获取设备状态
        int status = DataService.getMainCache().getInt(String.format("MonitorDevice:%s:status", serialNumber), -1);
        if (status != -1) return new SyncResult(0, "");

        // 获取设备实体
        GeneralDeviceEntity deviceEntity = getWithSerialNumber(serialNumber, false);
        if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "设备信息不完整。");

        // 检查设备是否被标记为已拆除
        if (deviceEntity.online_status == 99) return new SyncResult(99, "设备已拆除。");

        // 更新设备的离线状态
        updateOnlineStatus(deviceEntity.serialNumber, 0);
        return new SyncResult(1, "设备已离线。");
    }

    /**
     * 2024-10-10 新版本通知系统，透传参数组装器
     * 该参数组装器负责根据设备的唯一标识符（unique_code）和设备配置代码（config_code）生成用于通知系统的数据。
     */
    public final static ITransDataBuilder iNotifyServiceTransDataBuilder = (unique_code, config_code, notifyType, transData) -> {
        // 获取设备信息
        // 根据设备的唯一标识符查询设备实体
        GeneralDeviceEntity deviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(unique_code);
        if (deviceEntity == null) return null; // 如果未找到设备，返回 null

        // 读取设备配置
        // 获取该设备的详细配置信息，并存储在 JSON 对象中
        JSONObject deviceConfig = GeneralDeviceConfigService.getInstance().getJSONObject(unique_code);

        // 读取[自动化火灾监测喷淋集控柜]设备的配置信息
        // 如果该设备有主设备，获取主设备的配置信息
        JSONObject mainDeviceConfig = GeneralDeviceConfigService.getInstance().getJSONObject(deviceEntity.mainSerialNumber);

        // 组织充电站点信息，可能会不存在
        // 获取设备的充电站点ID，如果设备的站点ID有效，则使用它，否则为空
        String CSId = "";
        if (!"".equals(deviceEntity.CSId) && !"0".equals(deviceEntity.CSId)) CSId = deviceEntity.CSId;
        String CSName = deviceEntity.deviceName; // 默认站点名称为设备名称

        // region 获取站点名信息或地址信息
        // 通过设备的配置信息，获取设备的详细地址信息，包括省、市、区、街道、社区和具体地址行信息。
        // 如果这些信息在设备配置中缺失，则从主设备配置中获取默认值。
        String province = JsonUtil.getString(deviceConfig, "province", JsonUtil.getString(mainDeviceConfig, "province"));
        String city = JsonUtil.getString(deviceConfig, "city", JsonUtil.getString(mainDeviceConfig, "city"));
        String district = JsonUtil.getString(deviceConfig, "district", JsonUtil.getString(mainDeviceConfig, "district"));
        String street = JsonUtil.getString(deviceConfig, "street", JsonUtil.getString(mainDeviceConfig, "street"));
        String communities = JsonUtil.getString(deviceConfig, "communities", JsonUtil.getString(mainDeviceConfig, "communities"));
        String roads = JsonUtil.getString(deviceConfig, "roads", JsonUtil.getString(mainDeviceConfig, "roads"));
        String address_line1 = JsonUtil.getString(deviceConfig, "address", JsonUtil.getString(mainDeviceConfig, "address"));

        // 尝试通过站点 ID 获取详细的站点信息，如果成功获取，则覆盖之前的站点地址信息
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(deviceEntity.CSId);
        if (chargeStationEntity != null) {
            CSName = chargeStationEntity.name; // 更新站点名称
            province = chargeStationEntity.province;
            city = chargeStationEntity.city;
            district = chargeStationEntity.district;
            street = chargeStationEntity.street;
            communities = chargeStationEntity.communities;
            roads = chargeStationEntity.roads;
            address_line1 = chargeStationEntity.address;
        }
        // endregion

        // 基础站点信息
        // 将基础的站点信息放入传输数据中，包括站点ID、站点名称、当前时间、设备序列号、组织代码、设备的SPU编码、设备类型编码、设备品牌编码和主设备序列号
        transData.put("CSId", CSId);
        transData.put("CSName", CSName);
        transData.put("time", TimeUtil.getTimeString());
        transData.put("serialNumber", deviceEntity.serialNumber);
        transData.put("organize_code", deviceEntity.organize_code);
        transData.put("spuCode", deviceEntity.spuCode);
        transData.put("typeCode", deviceEntity.typeCode);
        transData.put("brandCode", deviceEntity.brandCode);
        transData.put("mainSerialNumber", deviceEntity.mainSerialNumber);

        // 兼容字段
        transData.put("deviceNumber", deviceEntity.serialNumber);
        transData.put("deviceTypeName", deviceEntity.typeCode);

        // 添加详细的地址信息
        // 将获取到的省、市、区、街道、社区、道路和具体地址行信息加入传输数据中
        transData.put("province", province);
        transData.put("city", city);
        transData.put("district", district);
        transData.put("street", street);
        transData.put("communities", communities);
        transData.put("roads", roads);
        transData.put("address_line1", address_line1);

        // 组装完整的地址
        // 将上述的地址元素拼接成完整的地址字符串，并放入传输数据中
        transData.put("address", String.format("%s%s%s%s%s%s%s",
                province, city, district, street, communities, roads, address_line1
        ));

        // 默认设置 HexColor
        // 如果传输数据中不包含 “HexColor” 键，默认设置其值为 “#E88E35”
        if (!transData.containsKey("HexColor")) {
            transData.put("HexColor", "#E88E35");
        }

        if (!transData.containsKey("color")) {
            transData.put("color", "info");
        }

        return transData; // 返回组装后的传输数据
    };

    // endregion

    /**
     * @param excelFile  excel文件
     * @param sheetIndex 表格索引(从0开始)
     * @param headerRow  头部物理行号(从1开始数)
     */
    public void checkSimCode(File excelFile, int sheetIndex, int headerRow, String simCodeCellName) {
        // 使用try-with-resources确保资源正确关闭
        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            int headerRowIndex = headerRow - 1; // 头部行索引
            int lastRowNum = sheet.getLastRowNum();

            // 预先获取最后一列的索引
            int lastCellNum = sheet.getRow(headerRowIndex).getLastCellNum();

            ChargeStationService chargeStationService = ChargeStationService.getInstance();

            // 遍历表格
            for (Row row : sheet) {
                int rowNum = row.getRowNum();
                if (rowNum < headerRowIndex) continue;  // 跳过表头行

                if (rowNum == headerRowIndex) {
                    // 添加新的列头
                    ExcelUtil.createCell(row, lastCellNum + 1, "设备名");
                    ExcelUtil.createCell(row, lastCellNum + 2, "设备序列号");
                    ExcelUtil.createCell(row, lastCellNum + 3, "站点");
                    ExcelUtil.createCell(row, lastCellNum + 4, "型号");
                    ExcelUtil.createCell(row, lastCellNum + 5, "品牌");
                    ExcelUtil.createCell(row, lastCellNum + 6, "类型");
                    continue;
                }

                LogsUtil.info(TAG, "正在检索：[%s/%s]", row.getRowNum(), lastRowNum);

                String simCode = ExcelUtil.getString(row, simCodeCellName);
                if (StringUtil.isEmpty(simCode)) continue;  // 如果SIM码为空，跳过该行

                // 查询通用设备
                GeneralDeviceEntity generalDeviceEntity = GeneralDeviceEntity.getInstance()
                        .where("simCode", simCode)
                        .findEntity();

                if (generalDeviceEntity != null && generalDeviceEntity.id != 0) {
                    ExcelUtil.createCell(row, lastCellNum + 1, generalDeviceEntity.deviceName);
                    ExcelUtil.createCell(row, lastCellNum + 2, generalDeviceEntity.serialNumber);
                    ExcelUtil.createCell(row, lastCellNum + 3, chargeStationService.getNameByCSId(generalDeviceEntity.CSId));
                    ExcelUtil.createCell(row, lastCellNum + 4, generalDeviceEntity.spuCode);
                    ExcelUtil.createCell(row, lastCellNum + 5, generalDeviceEntity.brandCode);
                    ExcelUtil.createCell(row, lastCellNum + 6, generalDeviceEntity.typeCode);
                    continue;
                }

                // 查询充电设备
                DeviceEntity deviceEntity = DeviceEntity.getInstance()
                        .where("simCode", simCode)
                        .where("isHost", 1)
//                        .order("isHost DESC") // 一般情况下只有主机才有SIM卡，这里用排序主要是担心有些单独的从机也有
                        .findEntity();

                if (deviceEntity != null && deviceEntity.id != 0) {
                    // 找到充电设备
                    ExcelUtil.createCell(row, lastCellNum + 1, deviceEntity.deviceName);
                    ExcelUtil.createCell(row, lastCellNum + 2, deviceEntity.deviceNumber);
                    ExcelUtil.createCell(row, lastCellNum + 3, chargeStationService.getNameByCSId(deviceEntity.CSId));
                    ExcelUtil.createCell(row, lastCellNum + 4, deviceEntity.spuCode);
                    ExcelUtil.createCell(row, lastCellNum + 5, deviceEntity.brandCode);
                    ExcelUtil.createCell(row, lastCellNum + 6, deviceEntity.typeCode);
                }
            }

            // 保存处理后的Excel文件
            try (FileOutputStream out = new FileOutputStream(excelFile)) {
                workbook.write(out);
            }

        } catch (IOException e) {
            LogsUtil.error(e, TAG, "处理Excel文件出错");
        }
    }

    // region remark - Excel导入

    /**
     * 下载导入设备的格式文件
     */
    public void downloadExcelTemplate(HttpServletResponse response) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            // 读取配置中的上传根目录，拼接模板相对路径
            String save_path = ConfigManager.getString("upload.local_save_path");
            if (StringUtil.isEmpty(save_path)) {
                common.apicbWithResponse(response, HttpStatus.OK.value(), common.apicb(3, "系统没有配置存储路径"));
                return;
            }
            // 检查 save_path 非空，且末尾是否自带/，避免路径拼错
            if (!"/".equalsIgnoreCase(save_path.substring(save_path.length() - 1))) {
                save_path += "/";
            }

            final String templateName = "ImportGeneralDevices.xlsx";
            String path = String.format("%sTemplate/%s", save_path, templateName);
            File file = new File(path);
            if (!file.exists()) {
                // 模板文件不存在时直接返回 400，并写一段 JSON
                common.apicbWithResponse(response, HttpStatus.BAD_REQUEST.value(), common.apicb(1, String.format("模板文件不存在：%s", path)));
                return;
            }

            /* 设置内容类型
             * 老的 .xls  类型：application/vnd.ms-excel
             * 新的 .xlsx 类型：application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
             */
//            response.setContentType("application/vnd.ms-excel");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            // 设置下载文件名
//            response.setHeader("Content-Disposition", String.format("attachment; filename=%s", templateName));
            // 文件名编码（兼容大多数浏览器）
            String encoded = java.net.URLEncoder.encode(templateName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            String contentDisposition = "attachment; filename=\"" + templateName + "\"; filename*=UTF-8''" + encoded;
            response.setHeader("Content-Disposition", contentDisposition);

            // 流式拷贝文件到响应
            inputStream = new FileInputStream(file);
            outputStream = response.getOutputStream();

            byte[] buffer = new byte[8192]; // 8KB 缓冲
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // 刷新输出流
            outputStream.flush();
        } catch (Exception e) {
            LogsUtil.error(e, "下载导入设备Excel模板失败", "");
            // 异常时写入 JSON 提示
            common.apicbWithResponse(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), common.apicb(1, "下载模板失败"));
        } finally {
            // 关闭资源
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    /**
     * 通过Excel导入设备
     */
    public ISyncResult importFromExcel(HttpServletRequest request) throws IOException {
        // 从请求中取文件
        File imFile = HttpRequestUtil.getFile(request, "file");
        int headerRowIndex = HttpRequestUtil.getInt(request, "headerRowIndex", 1); // 头部行号索引（从0开始）
        if (imFile == null) return new SyncResult(1, "请上传文件");

        try (FileInputStream fis = new FileInputStream(imFile)) {
            Workbook workbook = new XSSFWorkbook(fis); // 读取 .xlsx，内存占用与数据量正相关
            Sheet sheet = workbook.getSheetAt(0);

            // 列映射（按字母列）
            String deviceNameCol = "A";
            String serialNumberCol = "B";
            String spuCodeCol = "C";
            String organizeCodeCol = "D";
            String platformCodeCol = "E";
            String simCodeCol = "F";
            String batchNumberCol = "G";

            DeviceUnitEntity unitEntity = null;

            List<String> errList = new ArrayList<>();
            List<String> serialNumberList = new ArrayList<>();

            for (Row row : sheet) {
                int currentRowIndex = row.getRowNum();
                if (currentRowIndex < headerRowIndex) continue; // 忽略头部

                // 逐列读取
                String deviceName = ExcelUtil.getString(row, deviceNameCol, "");
                String serialNumber = ExcelUtil.getString(row, serialNumberCol).trim();
                String spuCode = ExcelUtil.getString(row, spuCodeCol).trim();
                String organize_code = ExcelUtil.getString(row, organizeCodeCol);
                String platform_code = ExcelUtil.getString(row, platformCodeCol);
                String simCode = ExcelUtil.getString(row, simCodeCol);
                String batchNumber = ExcelUtil.getString(row, batchNumberCol);

                // 当序列号与SPU都为空，跳过
                if (!StringUtils.hasLength(serialNumber) && !StringUtils.hasLength(spuCode)) continue;

                serialNumberList.add(serialNumber);

                //region 查询设备单元信息
                // 首次查询
                if (unitEntity == null) unitEntity = DeviceUnitEntity.getInstance().getWithSpuCode(spuCode);
                if (unitEntity == null) {
                    errList.add(String.format("%s - %s - %s 导入失败，无设备单元数据", spuCode, serialNumber, deviceName));
                    continue;
                }
                //endregion

                // 如果设备名为空，回退为设备单元名称
                if (!StringUtil.hasLength(deviceName)) deviceName = unitEntity.name;

                // 查重：按序列号查已有设备
                GeneralDeviceEntity device = GeneralDeviceService.getInstance().getWithSerialNumber(serialNumber);
                if (device != null && device.id != 0) {
                    errList.add(String.format("%s - %s - %s 导入失败，存在相同设备数据", spuCode, serialNumber, deviceName));
                    continue;
                }

                String finalDeviceName = deviceName;
                // 设备表数据
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("deviceName", finalDeviceName);
                data.put("serialNumber", serialNumber);
                data.put("CSId", "");
                data.put("spuCode", spuCode);
                data.put("brandCode", unitEntity.brandCode);
                data.put("typeCode", unitEntity.typeCode);
                data.put("online_status", 0);// 0-离线，1-在线
                data.put("status", 1);// 0-删除，1-正常
                data.put("simCode", simCode);
                data.put("batchNumber", batchNumber);
                data.put("spec", unitEntity.spec);
                data.put("dynamic_info", "{}");
                data.put("mainSerialNumber", "");
                data.put("remark", "");
                data.put("organize_code", organize_code);
                data.put("platform_code", platform_code);
                long timeStamp = TimeUtil.getTimestamp();
                data.put("create_time", timeStamp);
                data.put("update_time", timeStamp);
                data.put("delete_time", 0);

                if (GeneralDeviceEntity.getInstance().insert(data) == 0) {
                    errList.add(String.format("%s - %s - %s 导入失败", spuCode, serialNumber, deviceName));
                    return new SyncResult(11, "新增通用设备数据失败");
                }

                // 设备配置表
                Map<String, Object> config_data = new LinkedHashMap<>();
                config_data.put("serialNumber", serialNumber);
                config_data.put("config", unitEntity.config);
                if (GeneralDeviceConfigEntity.getInstance().insert(config_data) == 0) {
                    return new SyncResult(12, "新增通用设备配置数据失败");
                }
            }

            if (!serialNumberList.isEmpty() && unitEntity != null) importAfterSyncRecord(serialNumberList, unitEntity);

            return new SyncResult(0, "", errList);
        }
    }

    /**
     * 导入后处理
     *
     * @param serialNumberList
     * @param deviceUnitEntity
     * @return
     */
    private boolean importAfterSyncRecord(List<String> serialNumberList, DeviceUnitEntity deviceUnitEntity) {
        if (serialNumberList == null || serialNumberList.isEmpty()) return false;
        if (deviceUnitEntity == null) return false;

        if ("CHTQDQ-4G-DDSY6607-2P".equalsIgnoreCase(deviceUnitEntity.spuCode)) {
            TQDianBiaoSDK.getInstance().meterAdd(serialNumberList);
        }
        return false;
    }

    // endregion
}