package com.evcharge.service.FireSafety;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceTypeEntity;
import com.evcharge.entity.device.GeneralDeviceConfigEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.service.GeneralDevice.GeneralDeviceConfigService;
import com.evcharge.service.GeneralDevice.GeneralDeviceService;
import com.evcharge.service.Inspect.InspectChargeStationService;
import com.evcharge.utils.JSONFormatConfig;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消防设备业务逻辑层
 * <p>
 * 提供与消防设备相关的业务逻辑处理，包括设备列表查询、设备详情获取、设备配置获取、设备绑定和解绑等功能。
 */
public class FireSafetyDeviceService {
    // 单例模式的静态实例，使用volatile关键字确保线程安全
    private volatile static FireSafetyDeviceService _this;

    /**
     * 获取单例实例
     * <p>
     * 采用双重检查锁定机制（Double-Check Locking）确保线程安全，懒加载单例模式。
     *
     * @return {@link FireSafetyDeviceService}的单例实例
     */
    public static FireSafetyDeviceService getInstance() {
        if (_this == null) {
            synchronized (FireSafetyDeviceService.class) {
                if (_this == null) _this = new FireSafetyDeviceService();
            }
        }
        return _this;
    }

    /**
     * 获取设备列表
     * <p>
     * 根据组织编码、站点ID、主设备序列号和搜索文本，查询指定条件下的设备列表。支持分页查询，并可根据搜索文本进行模糊查询。
     *
     * @param organize_code    组织编码，非空，用于标识设备所属的组织
     * @param CSId             （可选）站点唯一标识编码，可以是 CSId、uuid 或 id
     * @param mainSerialNumber （可选）主设备序列号，用于查询子设备列表
     * @param search_text      （可选）搜索文本，用于模糊查询设备名称、序列号等
     * @param page             当前页码，从1开始
     * @param limit            每页显示的记录数
     * @return {@link JSONObject} 包含设备列表的分页数据
     */
    public ISyncResult getList(@NonNull String organize_code
            , String CSId
            , String mainSerialNumber
            , String search_text
            , int page
            , int limit) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        // 创建用于统计总记录数的实体
        GeneralDeviceEntity countEntity = GeneralDeviceEntity.getInstance();
        countEntity
                .alias("gd")
                .leftJoin(DeviceTypeEntity.getInstance().theTableName(), "dt", "gd.typeCode = dt.typeCode")
                .where("organize_code", organize_code)
                .where("status", 1);

        // 创建用于查询记录的实体
        GeneralDeviceEntity entity = GeneralDeviceEntity.getInstance();
        entity.field("serialNumber,deviceName,spuCode,brandCode,dt.typeCode,dt.name AS typeName,online_status")
                .alias("gd")
                .leftJoin(DeviceTypeEntity.getInstance().theTableName(), "dt", "gd.typeCode = dt.typeCode")
                .where("organize_code", organize_code)
                .where("status", 1);

        // 根据站点ID过滤
        if (!VerifyUtil.isEmpty(CSId)) {
            countEntity.where("CSId", CSId);
            entity.where("CSId", CSId);
        }

        // 根据主设备序列号过滤（查询子设备）
        if (!VerifyUtil.isEmpty(mainSerialNumber)) {
            countEntity.where("mainSerialNumber", mainSerialNumber)
                    .where("serialNumber", "!=", mainSerialNumber);

            entity.where("mainSerialNumber", mainSerialNumber)
                    .where("serialNumber", "!=", mainSerialNumber);
        }

        // 根据搜索文本进行模糊查询
        if (!VerifyUtil.isEmpty(search_text)) {
            String likePattern = String.format("%%%s%%", search_text);
            countEntity.whereBuilder("AND", "(", "deviceName", "like", likePattern, "")
                    .whereBuilder("OR", "serialNumber", "like", likePattern)
                    .whereBuilder("OR", "spuCode", "like", likePattern)
                    .whereBuilder("OR", "brandCode", "like", likePattern)
                    .whereBuilder("OR", "gd.typeCode", "like", likePattern)
                    .whereBuilder("OR", "dt.name", "like", likePattern, ")");

            entity.whereBuilder("AND", "(", "deviceName", "like", likePattern, "")
                    .whereBuilder("OR", "serialNumber", "like", likePattern)
                    .whereBuilder("OR", "spuCode", "like", likePattern)
                    .whereBuilder("OR", "brandCode", "like", likePattern)
                    .whereBuilder("OR", "gd.typeCode", "like", likePattern)
                    .whereBuilder("OR", "dt.name", "like", likePattern, ")");
        }

        // 获取总记录数
        long count = countEntity.count();
        if (count == 0) return new SyncResult(1, "无数据");

        // 分页查询记录列表
        List<Map<String, Object>> list = entity.page(page, limit).select();
        if (list == null || list.isEmpty()) return new SyncResult(1, "");

        // 返回包含分页信息和记录列表的JSON对象
        return new SyncListResult(count, page, limit, list);
    }

    /**
     * 查询设备详情
     * <p>
     * 根据组织编码和设备序列号查询设备的详细信息，同时进行权限校验，确保只能访问所属组织下的设备信息。
     *
     * @param organize_code 组织编码，非空，用于权限校验
     * @param serialNumber  设备序列号，非空，用于唯一标识设备
     * @return {@link JSONObject} 包含设备详细信息的JSON对象
     */
    public ISyncResult getDetail(@NonNull String organize_code, @NonNull String serialNumber) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        // 查询设备详细信息
        Map<String, Object> data = GeneralDeviceEntity.getInstance()
                .field("serialNumber,deviceName,CSId,spuCode,brandCode,gd.typeCode,dt.name AS typeName" +
                        ",online_status,simCode,spec,dynamic_info,mainSerialNumber,gd.remark,organize_code,platform_code")
                .cache(String.format("FireSafety:GeneralDevice:%s", serialNumber))
                .alias("gd")
                .leftJoin(DeviceTypeEntity.getInstance().theTableName(), "dt", "gd.typeCode = dt.typeCode")
                .where("serialNumber", serialNumber)
                .where("status", 1)
                .find();

        // 判断设备是否存在
        if (data == null || data.isEmpty()) return new SyncResult(1, "");

        // 权限校验，确保只能查询所属组织的设备
        if (!organize_code.equalsIgnoreCase(MapUtil.getString(data, "organize_code"))) {
            return new SyncResult(99, "无权查询");
        }

        // 解析spec字段为JSON对象
        JSONObject spec = new JSONObject();
        if (!StringUtil.isEmpty(MapUtil.getString(data, "spec"))) {
            spec = JSONObject.parse(MapUtil.getString(data, "spec"));
        }
        data.put("spec", spec);

        // 解析dynamic_info字段为JSON对象
        JSONObject dynamic_info = new JSONObject();
        if (!StringUtil.isEmpty(MapUtil.getString(data, "dynamic_info"))) {
            dynamic_info = JSONObject.parse(MapUtil.getString(data, "dynamic_info"));
        }
        data.put("dynamic_info", dynamic_info);

        // 获取站点信息
        String CSId = MapUtil.getString(data, "CSId");
        data.put("cs_info", InspectChargeStationService.getInstance().getBaseInfo(CSId));

        // 移除不必要的字段
        data.remove("organize_code");
        data.remove("platform_code");

        // 返回设备详细信息
        return new SyncResult(0, "", data);
    }

    /**
     * 查询设备简要信息
     * <p>
     * 用于绑定前查询未绑定的设备简要信息，不包含敏感信息。
     *
     * @param organize_code 组织编码，非空，用于权限校验
     * @param serialNumber  设备序列号，非空，用于唯一标识设备
     * @return {@link JSONObject} 包含设备简要信息的JSON对象
     */
    public ISyncResult getBriefDetail(@NonNull String organize_code, @NonNull String serialNumber) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        // 查询设备简要信息
        Map<String, Object> data = GeneralDeviceEntity.getInstance()
                .field("serialNumber,deviceName,spuCode,brandCode,gd.typeCode,dt.name AS typeName,spec,organize_code,platform_code")
                .cache(String.format("FireSafety:GeneralDevice:BriefDetail:%s", serialNumber))
                .alias("gd")
                .leftJoin(DeviceTypeEntity.getInstance().theTableName(), "dt", "gd.typeCode = dt.typeCode")
                .where("status", 1)
                .find();

        // 判断设备是否存在
        if (MapUtil.isEmpty(data)) return new SyncResult(1, "");

        // 获取设备的组织编码
        String device_organize_code = MapUtil.getString(data, "organize_code");

        // 如果设备已绑定其他组织，权限校验
        if (!VerifyUtil.isEmpty(device_organize_code) && !organize_code.equalsIgnoreCase(device_organize_code)) {
            return new SyncResult(99, "无权查询");
        }

        // 解析spec字段为JSON对象
        JSONObject spec = new JSONObject();
        if (!StringUtil.isEmpty(MapUtil.getString(data, "spec"))) {
            spec = JSONObject.parse(MapUtil.getString(data, "spec"));
        }
        data.put("spec", spec);

        // 解析dynamic_info字段为JSON对象
        JSONObject dynamic_info = new JSONObject();
        if (!StringUtil.isEmpty(MapUtil.getString(data, "dynamic_info"))) {
            dynamic_info = JSONObject.parse(MapUtil.getString(data, "dynamic_info"));
        }
        data.put("dynamic_info", dynamic_info);

        // 移除不必要的字段
        data.remove("organize_code");
        data.remove("platform_code");

        // 返回设备简要信息
        return new SyncResult(0, "", data);
    }

    /**
     * 查询设备的配置数据
     * <p>
     * 根据组织编码和设备序列号，获取设备的配置数据，确保只能访问所属组织下的设备配置。
     *
     * @param organize_code 组织编码，非空，用于权限校验
     * @param serialNumber  设备序列号，非空，用于唯一标识设备
     * @return {@link JSONObject} 包含设备配置数据的JSON对象
     */
    public ISyncResult getConfig(@NonNull String organize_code, @NonNull String serialNumber) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        // 根据设备序列号获取设备实体
        GeneralDeviceEntity deviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(serialNumber);
        if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "无效设备码");

        // 权限校验，确保只能查询所属组织的设备配置
        if (!organize_code.equalsIgnoreCase(deviceEntity.organize_code)) {
            return new SyncResult(99, "无权查询");
        }

        // 获取设备的配置数据
        JSONObject config = GeneralDeviceConfigService.getInstance().getJSONObject(serialNumber);

        // 返回配置数据
        return new SyncResult(0, "", config);
    }

    /**
     * 设备绑定子设备
     * <p>
     * 将子设备绑定到主设备上，确保设备均属于同一组织并进行权限校验。
     *
     * @param organize_code    组织编码，非空，用于权限校验
     * @param mainSerialNumber 主设备序列号，非空
     * @param serialNumber     子设备序列号，非空
     * @return {@link JSONObject} 返回绑定操作的结果
     */
    public ISyncResult bindSubDevice(@NonNull String organize_code, @NonNull String mainSerialNumber, @NonNull String serialNumber) {
        // 校验组织编码和设备序列号是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");
        if (VerifyUtil.isEmpty(mainSerialNumber)) return new SyncResult(2, "无效主设备序列号");
        if (VerifyUtil.isEmpty(serialNumber)) return new SyncResult(2, "无效子设备序列号");

        try {
            // 查询主设备信息
            GeneralDeviceEntity mainDeviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(mainSerialNumber, false);
            if (mainDeviceEntity == null || mainDeviceEntity.id == 0) {
                return new SyncResult(3, "主设备不存在，请检查设备码是否正确");
            }

            // 权限校验，确保主设备属于当前组织
            if (!StringUtil.isEmpty(mainDeviceEntity.organize_code) && !organize_code.equalsIgnoreCase(mainDeviceEntity.organize_code)) {
                return new SyncResult(99, "无权操作");
            }

            // 调用设备服务进行绑定操作
            return GeneralDeviceService.getInstance().bindSubDevice(mainSerialNumber, serialNumber);
        } catch (Exception e) {
            LogsUtil.error(e, FireSafetyService.TAG, "设备绑定子设备发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 设备解绑子设备
     * <p>
     * 将子设备从主设备上解绑，确保设备均属于同一组织并进行权限校验。
     *
     * @param organize_code    组织编码，非空，用于权限校验
     * @param mainSerialNumber 主设备序列号，非空
     * @param serialNumber     子设备序列号，非空
     * @return {@link JSONObject} 返回解绑操作的结果
     */
    public ISyncResult unbindSubDevice(@NonNull String organize_code, @NonNull String mainSerialNumber, @NonNull String serialNumber) {
        // 校验组织编码和设备序列号是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");
        if (VerifyUtil.isEmpty(serialNumber)) return new SyncResult(2, "无效子设备序列号");

        try {
            // 查询子设备信息
            GeneralDeviceEntity subDeviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(serialNumber, false);
            if (subDeviceEntity == null || subDeviceEntity.id == 0) {
                return new SyncResult(3, "子设备不存在，请检查设备码是否正确");
            }

            // 校验子设备是否已绑定到指定的主设备
            if (!StringUtil.isEmpty(subDeviceEntity.mainSerialNumber) && !subDeviceEntity.mainSerialNumber.equalsIgnoreCase(mainSerialNumber)) {
                return new SyncResult(4, "子设备已绑定到其他设备上，请在其他设备上进行解绑");
            }

            // 权限校验，确保子设备属于当前组织
            if (!organize_code.equalsIgnoreCase(subDeviceEntity.organize_code)) {
                return new SyncResult(99, "无权操作");
            }

            // 调用设备服务进行解绑操作
            return GeneralDeviceService.getInstance().unBindSubDevice(serialNumber);
        } catch (Exception e) {
            LogsUtil.error(e, FireSafetyService.TAG, "设备解绑子设备发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 获取监控设备列表
     * <p>
     * 获取组织下的NVR设备列表，用于监控功能。支持分页查询，并进行权限校验。
     *
     * @param organize_code 组织编码，非空，用于权限校验
     * @param page          当前页码，从1开始
     * @param limit         每页显示的记录数
     * @return {@link JSONObject} 包含监控设备列表的分页数据
     */
    public ISyncResult getNVRList(@NonNull String organize_code
            , int page
            , int limit) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");
        try {
            // 创建用于统计总记录数的实体
            GeneralDeviceEntity countEntity = GeneralDeviceEntity.getInstance();
            countEntity.cache(String.format("FireSafety:GeneralDevice:NVR:List:%s:TotalCount", organize_code))
                    .where("organize_code", organize_code)
                    .where("status", 1)
                    .whereIn("typeCode", new String[]{"4GNVR", "NVR"});

            // 获取总记录数
            long count = countEntity.count();
            if (count == 0) return new SyncResult(1, "");

            // 优先从缓存中获取数据
            List<Map<String, Object>> list = DataService.getMainCache().getList(String.format("FireSafety:GeneralDevice:NVR:List:%s:%s_%s", organize_code, limit, page));
            if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);

            // 从数据库中获取数据
            GeneralDeviceEntity entity = GeneralDeviceEntity.getInstance();
            entity.field("gd.serialNumber,deviceName,spuCode,brandCode,typeCode,CSId,config")
                    .cache(String.format("FireSafety:GeneralDevice:NVR:List:%s:%s_%s", organize_code, limit, page))
                    .alias("gd")
                    .leftJoin(GeneralDeviceConfigEntity.getInstance().theTableName(), "gdc", "gd.serialNumber = gdc.serialNumber")
                    .where("organize_code", organize_code)
                    .where("status", 1)
                    .whereIn("typeCode", new String[]{"4GNVR", "NVR"});

            // 分页查询记录列表
            list = entity.page(page, limit).select();
            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            // 处理config字段和站点信息
            for (Map<String, Object> data : list) {
                // 处理配置数据
                String config = MapUtil.getString(data, "config");
                if (VerifyUtil.isEmpty(config)) {
                    data.put("config", new HashMap<>());
                } else {
                    data.put("config", JSONFormatConfig.format(JSONArray.parse(config)));
                }

                // 获取站点信息
                String CSId = MapUtil.getString(data, "CSId");
                data.put("cs_info", InspectChargeStationService.getInstance().getBaseInfo(CSId));
            }

            // 将结果缓存
            DataService.getMainCache().setList(String.format("FireSafety:GeneralDevice:NVR:List:%s:%s_%s", organize_code, limit, page), list);

            // 返回包含分页信息和记录列表的JSON对象
            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(FireSafetyService.TAG, "获取监控列表发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 获取监控设备
     * <p>
     * 获取组织下的NVR设备列表，用于监控功能。支持分页查询，并进行权限校验。
     *
     * @param organize_code 组织编码，非空，用于权限校验
     * @param serialNumber  设备序列号
     * @return {@link JSONObject} 包含监控设备列表的分页数据
     */
    public ISyncResult getNVRDetail(@NonNull String organize_code, String serialNumber) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");
        if (VerifyUtil.isEmpty(serialNumber)) return new SyncResult(2, "无效设备序列号");
        try {
            GeneralDeviceEntity entity = GeneralDeviceEntity.getInstance();
            entity.field("gd.serialNumber,deviceName,spuCode,brandCode,typeCode,CSId,config")
                    .cache(String.format("FireSafety:GeneralDevice:NVR:Detail:%s:%s", organize_code, serialNumber))
                    .alias("gd")
                    .leftJoin(GeneralDeviceConfigEntity.getInstance().theTableName(), "gdc", "gd.serialNumber = gdc.serialNumber")
                    .where("gd.serialNumber", serialNumber)
                    .where("organize_code", organize_code)
                    .where("typeCode", "4GNVR")
                    .where("brandCode", "WAPA")
                    .where("status", 1)
                    .whereIn("typeCode", new String[]{"4GNVR", "NVR"});

            // 分页查询记录列表
            Map<String, Object> data = entity.find();
            if (data == null || data.isEmpty()) return new SyncResult(1, "");

            // 获取站点信息
            data.put("cs_info", InspectChargeStationService.getInstance().getBaseInfo(MapUtil.getString(data, "CSId")));

            // 返回包含分页信息和记录列表的JSON对象
            return new SyncResult(0, "", data);
        } catch (Exception e) {
            LogsUtil.error(FireSafetyService.TAG, "获取监控设备发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 获取监控设备
     * <p>
     * 获取组织下的NVR设备列表，用于监控功能。支持分页查询，并进行权限校验。
     *
     * @param organize_code 组织编码，非空，用于权限校验
     * @param CSId          站点唯一编码
     * @return {@link JSONObject} 包含监控设备列表的分页数据
     */
    public ISyncResult getNVRDetailByCSId(@NonNull String organize_code, String CSId) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");
        if (VerifyUtil.isEmpty(CSId)) return new SyncResult(2, "无效站点编码");
        try {
            GeneralDeviceEntity entity = GeneralDeviceEntity.getInstance();
            entity.field("gd.serialNumber,deviceName,spuCode,brandCode,typeCode,CSId,config")
                    .cache(String.format("FireSafety:GeneralDevice:NVR:Detail:%s:%s", organize_code, CSId))
                    .alias("gd")
                    .leftJoin(GeneralDeviceConfigEntity.getInstance().theTableName(), "gdc", "gd.serialNumber = gdc.serialNumber")
                    .where("CSId", CSId)
                    .where("organize_code", organize_code)
                    .where("typeCode", "4GNVR")
                    .where("brandCode", "WAPA")
                    .where("status", 1)
                    .whereIn("typeCode", new String[]{"4GNVR", "NVR"});

            // 分页查询记录列表
            Map<String, Object> data = entity.find();
            if (data == null || data.isEmpty()) return new SyncResult(1, "");

            // 获取站点信息
            data.put("cs_info", InspectChargeStationService.getInstance().getBaseInfo(CSId));

            // 返回包含分页信息和记录列表的JSON对象
            return new SyncResult(0, "", data);
        } catch (Exception e) {
            LogsUtil.error(FireSafetyService.TAG, "获取监控设备发生错误");
        }
        return new SyncResult(1, "");
    }
}