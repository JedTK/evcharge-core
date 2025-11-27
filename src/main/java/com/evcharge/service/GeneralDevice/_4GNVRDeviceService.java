package com.evcharge.service.GeneralDevice;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceConfigEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.lang.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class _4GNVRDeviceService {
    private final static String TAG = "4GNVR业务逻辑";

    public static _4GNVRDeviceService getInstance() {
        return new _4GNVRDeviceService();
    }

    /**
     * 获取监控设备列表
     * <p>
     * 获取组织下的NVR设备列表，用于监控功能。支持分页查询，并进行权限校验。
     *
     * @param organize_code 组织编码，非空，用于权限校验
     * @param search_text   搜索文本
     * @param page          当前页码，从1开始
     * @param limit         每页显示的记录数
     * @return {@link JSONObject} 包含监控设备列表的分页数据
     */
    public ISyncResult getList(@NonNull String organize_code
            , String search_text
            , int page
            , int limit) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");
        try {
            String countCacheKey = String.format("GeneralDevice:NVR:List:%s:%s:TotalCount", organize_code, common.md5(search_text));
            String listCacheKey = String.format("GeneralDevice:NVR:List:%s:%s:%s_%s", organize_code, common.md5(search_text), limit, page);

            // region 优先从缓存中获取数据
            long count = DataService.getMainCache().getInt(countCacheKey);
            if (count > 0) {
                List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
                if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
            }
            // endregion

            String cs_table_name = ChargeStationEntity.getInstance().theTableName();
            String device_config_name = GeneralDeviceConfigEntity.getInstance().theTableName();

            // 创建用于统计总记录数的实体
            GeneralDeviceEntity countEntity = GeneralDeviceEntity.getInstance();
            countEntity
                    .alias("gd")
                    .leftJoin(cs_table_name, "cs", "cs.CSId = gd.CSId")
                    .rightJoin(device_config_name, "gdc", "gd.serialNumber = gdc.serialNumber")
                    .where("gd.organize_code", organize_code)
                    .where("gd.status", 1)
                    .whereIn("gd.typeCode", new String[]{"4GNVR", "NVR"});
            if (!StringUtil.isEmpty(search_text)) {
                countEntity.whereBuilder("AND", "(", "gd.deviceName", "like", String.format("%%%s%%", search_text), "")
                        .whereBuilder("OR", "gd.serialNumber", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.CSId", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.name", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "gd.spec", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "gd.simCode", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.province", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.city", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.district", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.street", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.communities", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.roads", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "", "cs.address", "like", String.format("%%%s%%", search_text), ")");

            }

            count = countEntity.count();
            if (count == 0) return new SyncResult(1, "");

            GeneralDeviceEntity listEntity = GeneralDeviceEntity.getInstance();
            listEntity.field("gd.serialNumber,deviceName,spuCode,brandCode,typeCode,gd.CSId,config,simCode" +
                            ",cs.name AS cs_name,province,city,district,street,communities,roads,address")
                    .alias("gd")
                    .leftJoin(cs_table_name, "cs", "cs.CSId = gd.CSId")
                    .rightJoin(device_config_name, "gdc", "gd.serialNumber = gdc.serialNumber")
                    .where("gd.organize_code", organize_code)
                    .where("gd.status", 1)
                    .whereIn("gd.typeCode", new String[]{"4GNVR", "NVR"});
            if (!StringUtil.isEmpty(search_text)) {
                listEntity.whereBuilder("AND", "(", "gd.deviceName", "like", String.format("%%%s%%", search_text), "")
                        .whereBuilder("OR", "gd.serialNumber", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.CSId", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.name", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "gd.spec", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "gd.simCode", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.province", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.city", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.district", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.street", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.communities", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "cs.roads", "like", String.format("%%%s%%", search_text))
                        .whereBuilder("OR", "", "cs.address", "like", String.format("%%%s%%", search_text), ")");
            }

            // 从数据库中获取数据
            List<Map<String, Object>> list = listEntity
                    .page(page, limit)
                    .select();
            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            // 将结果缓存
            DataService.getMainCache().set(countCacheKey, count);
            DataService.getMainCache().setList(listCacheKey, list);

            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(TAG, "获取监控列表发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 设备详情
     *
     * @param organize_code 组织编码
     * @param serialNumber  序列号
     * @return
     */
    public ISyncResult getDetail(@NonNull String organize_code, @NonNull String serialNumber) {
        Map<String, Object> data = new LinkedHashMap<>();

        try {
            if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(2, "无权操作");
            if (VerifyUtil.isEmpty(serialNumber)) return new SyncResult(2, "无效设备");

            GeneralDeviceEntity deviceEntity = GeneralDeviceEntity.getInstance().getBySerialNumber(serialNumber, true);
            if (deviceEntity == null) return new SyncResult(3, "查无此设备数据");

            if (!organize_code.equalsIgnoreCase(deviceEntity.organize_code)) {
                return new SyncResult(98, "设备不属于您，无权操作");
            }

            data.put("serialNumber", deviceEntity.serialNumber);
            data.put("deviceName", deviceEntity.deviceName);
            data.put("CSId", deviceEntity.CSId);
            data.put("typeCode", deviceEntity.typeCode);
            data.put("brandCode", deviceEntity.brandCode);
            data.put("spuCode", deviceEntity.spuCode);
            data.put("dynamic_info", deviceEntity.dynamic_info);
            data.put("online_status", deviceEntity.online_status);
            data.put("simCode", deviceEntity.simCode);
            data.put("spec", deviceEntity.spec);

            GeneralDeviceConfigEntity configEntity = GeneralDeviceConfigEntity.getInstance().getBySerialNumber(serialNumber, true);
            if (configEntity != null && !StringUtil.isEmpty(configEntity.config)) {
                data.put("config", JSONArray.parseArray(configEntity.config));
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "获取设备详情发生错误");
        }
        return new SyncResult(0, "", data);
    }
}
