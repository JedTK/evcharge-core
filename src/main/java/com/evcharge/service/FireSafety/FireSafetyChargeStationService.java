package com.evcharge.service.FireSafety;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.sys.SysAreaEntity;
import com.evcharge.entity.sys.SysCityEntity;
import com.evcharge.entity.sys.SysProvinceEntity;
import com.evcharge.entity.sys.SysStreetEntity;
import com.evcharge.service.GeneralDevice.GeneralDeviceService;
import com.evcharge.service.Inspect.InspectChargeStationService;
import com.evcharge.utils.ChargeStationUtils;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.VerifyUtil;
import com.xyzs.utils.common;
import org.springframework.lang.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消防站点业务逻辑层
 * <p>
 * 负责处理与消防站点相关的业务逻辑，包括站点的新增、更新、查询，以及设备与站点的绑定和解绑操作。
 * 当前站点数据采集自巡检的站点数据。
 */
public class FireSafetyChargeStationService {
    // 单例模式的静态实例，使用volatile关键字确保可见性和防止指令重排
    private volatile static FireSafetyChargeStationService _this;

    /**
     * 获取单例实例
     * <p>
     * 采用双重检查锁定机制（Double-Check Locking）确保线程安全，懒加载单例模式。
     *
     * @return {@link FireSafetyChargeStationService}的单例实例
     */
    public static FireSafetyChargeStationService getInstance() {
        if (_this == null) {
            synchronized (FireSafetyChargeStationService.class) {
                if (_this == null) _this = new FireSafetyChargeStationService();
            }
        }
        return _this;
    }

    // region 地图相关

    /**
     * 获取附近的站点数据
     * <p>
     * 根据用户当前位置的经纬度和指定的搜索半径，查询组织内在该范围内的站点列表。
     * 支持分页查询，并对结果进行缓存以优化查询性能。
     *
     * @param organize_code 组织代码，非空，用于标识不同的组织
     * @param lon           用户所在位置的经度
     * @param lat           用户所在位置的纬度
     * @param radius        搜索半径（单位：米）
     * @param page          当前页码（从1开始）
     * @param limit         每页的记录数
     * @return {@link JSONObject} 包含分页数据的结果，包含站点列表及分页信息
     */
    public ISyncResult getNearbyData(@NonNull String organize_code
            , double lon
            , double lat
            , double radius
            , int page
            , int limit
    ) {
        // 校验组织代码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        // 构建缓存的 Key，用于标识缓存数据
        String cacheKey = common.md5(String.format("%s%s%s%s"
                , organize_code
                , lon
                , lat
                , radius
        ));

        // 从缓存中读取总记录数
        long count = DataService.getMainCache().getInt(String.format("FireSafety:MAP:NearbyCS:%s:TotalCount", cacheKey), -1);
        if (count != -1) {
            // 如果缓存中存在总数数据，则尝试从缓存中获取对应的分页数据
            List<Map<String, Object>> list = DataService.getMainCache()
                    .getList(String.format("FireSafety:MAP:NearbyCS:%s:%s_%s", cacheKey, limit, page));
            if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
        }

        // 从数据库中查询符合条件的站点总数
        count = DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION)
                .table(InspectChargeStationService.DB_TABLE_CHARGE_STATION)
                .where("organize_code", organize_code)
                .where(String.format("ST_Distance_Sphere(POINT(lon, lat), POINT(%s, %s))", lon, lat), "<=", radius)
                .count();
        if (count == 0) return new SyncResult(1, "");

        // 查询数据库中符合条件的分页数据
        List<Map<String, Object>> list = DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION)
                .table(InspectChargeStationService.DB_TABLE_CHARGE_STATION)
                .field(String.format("CSId,name,status,lon,lat,ROUND(ST_Distance_Sphere(POINT(lon, lat), POINT(%s, %s))) AS distance", lon, lat))
                .where("organize_code", organize_code)
                .where(String.format("ST_Distance_Sphere(POINT(lon, lat), POINT(%s, %s))", lon, lat), "<=", radius)
                .page(page, limit)
                .select();

        if (list == null || list.isEmpty()) return new SyncResult(1, "");

        // 将查询结果存入缓存
        DataService.getMainCache().setList(String.format("FireSafety:MAP:NearbyCS:%s:%s_%s", cacheKey, limit, page), list);
        DataService.getMainCache().set(String.format("FireSafety:MAP:NearbyCS:%s:TotalCount", cacheKey), count);

        // 返回包含分页信息和站点列表的JSON对象
        return new SyncListResult(count, page, limit, list);
    }

    // endregion

    /**
     * 新建站点
     * <p>
     * 根据传入的组织代码、地址信息和地理坐标，在数据库中添加新的站点记录。
     *
     * @param organize_code 组织代码，非空，用于标识站点所属的组织
     * @param name          站点名称，非空，必须唯一
     * @param province_code 省代码，非空，用于标识省份
     * @param city_code     市代码，非空，用于标识城市
     * @param district_code 区代码，非空，用于标识行政区
     * @param street_code   街道代码，非空，用于标识街道
     * @param communities   社区名称，非空
     * @param roads         道路名称，非空
     * @param address       详细地址，非空
     * @param lon           经度，必填，用于标识站点的地理位置
     * @param lat           纬度，必填，用于标识站点的地理位置
     * @return {@link JSONObject} 返回操作结果，包含成功或失败的信息
     */
    public ISyncResult add(@NonNull String organize_code
            , @NonNull String name
            , @NonNull String province_code
            , @NonNull String city_code
            , @NonNull String district_code
            , @NonNull String street_code
            , @NonNull String communities
            , @NonNull String roads
            , @NonNull String address
            , double lon
            , double lat) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        // 校验必填参数是否为空
        if (VerifyUtil.isEmpty(name)) return new SyncResult(2, "请输入站点名");
        if (VerifyUtil.isEmpty(province_code)) return new SyncResult(2, "请选择省份");
        if (VerifyUtil.isEmpty(city_code)) return new SyncResult(2, "请选择城市");
        if (VerifyUtil.isEmpty(district_code)) return new SyncResult(2, "请选择行政区域");
        if (VerifyUtil.isEmpty(street_code)) return new SyncResult(2, "请选择街道");

        // 校验经纬度是否有效
        if (lon == 0 || lat == 0) return new SyncResult(2, "请选择站点地图坐标");

        // 检查是否存在相同名称的站点
        if (DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION)
                .table(InspectChargeStationService.DB_TABLE_CHARGE_STATION)
                .where("organize_code", organize_code)
                .where("name", name).exist()) {
            return new SyncResult(3, "存在相同名字的站点");
        }

        // 验证省、市、区、街道的代码是否有效
        SysProvinceEntity provinceEntity = SysProvinceEntity.getInstance().getWithCode(province_code);
        if (provinceEntity == null || provinceEntity.province_id == 0) return new SyncResult(2, "请正确选择省份");

        SysCityEntity cityEntity = SysCityEntity.getInstance().getWithCode(city_code);
        if (cityEntity == null || cityEntity.city_id == 0) return new SyncResult(2, "请正确选择城市");

        SysAreaEntity areaEntity = SysAreaEntity.getInstance().getWithCode(district_code);
        if (areaEntity == null || areaEntity.area_id == 0) return new SyncResult(2, "请正确选择行政区域");

        SysStreetEntity streetEntity = SysStreetEntity.getInstance().getWithCode(street_code);
        if (streetEntity == null || streetEntity.street_id == 0) return new SyncResult(2, "请正确选择街道");

        // 生成唯一的站点ID
        String CSId = common.md5(common.getUUID());

        // 构建站点数据
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("organize_code", organize_code);
        data.put("name", name);
        data.put("CSId", CSId);

        data.put("province", provinceEntity.province_name);
        data.put("province_code", province_code);
        data.put("city", cityEntity.city_name);
        data.put("city_code", city_code);
        data.put("district", areaEntity.area_name);
        data.put("district_code", district_code);
        data.put("street", streetEntity.street_name);
        data.put("street_code", street_code);

        data.put("communities", communities);
        data.put("roads", roads);
        data.put("address", address);
        data.put("lon", lon);
        data.put("lat", lat);
        data.put("source", FireSafetyService.FIRE_SAFETY_SOURCE);

        // 向数据库插入新站点记录
        int noquery = DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION)
                .table(InspectChargeStationService.DB_TABLE_CHARGE_STATION)
                .insert(data);

        // 构建返回数据
        Map<String, Object> cb_data = new LinkedHashMap<>();
        cb_data.put("CSId", CSId);

        // 返回操作结果
        if (noquery > 0) return new SyncResult(0, "", cb_data);
        return new SyncResult(1, "");
    }

    /**
     * 更新站点信息
     * <p>
     * 根据传入的站点ID和新的站点信息，更新数据库中的站点记录。
     *
     * @param organize_code 组织代码，非空，用于标识站点所属的组织
     * @param CSId          站点唯一标识编码，非空
     * @param name          站点名称，非空，必须唯一
     * @param province_code 省代码，非空，用于标识省份
     * @param city_code     市代码，非空，用于标识城市
     * @param district_code 区代码，非空，用于标识行政区
     * @param street_code   街道代码，非空，用于标识街道
     * @param communities   社区名称，非空
     * @param roads         道路名称，非空
     * @param address       详细地址，非空
     * @param lon           经度，必填，用于标识站点的地理位置
     * @param lat           纬度，必填，用于标识站点的地理位置
     * @return {@link JSONObject} 返回操作结果，包含成功或失败的信息
     */
    public ISyncResult update(@NonNull String organize_code
            , @NonNull String CSId
            , @NonNull String name
            , @NonNull String province_code
            , @NonNull String city_code
            , @NonNull String district_code
            , @NonNull String street_code
            , @NonNull String communities
            , @NonNull String roads
            , @NonNull String address
            , double lon
            , double lat) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        // 校验必填参数是否为空
        if (VerifyUtil.isEmpty(name)) return new SyncResult(2, "请输入站点名");
        if (VerifyUtil.isEmpty(province_code)) return new SyncResult(2, "请选择省份");
        if (VerifyUtil.isEmpty(city_code)) return new SyncResult(2, "请选择城市");
        if (VerifyUtil.isEmpty(district_code)) return new SyncResult(2, "请选择行政区域");
        if (VerifyUtil.isEmpty(street_code)) return new SyncResult(2, "请选择街道");

        // 校验经纬度是否有效
        if (lon == 0 || lat == 0) return new SyncResult(2, "请选择站点地图坐标");

        // 检查是否存在相同名称的其他站点
        if (DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION)
                .table(InspectChargeStationService.DB_TABLE_CHARGE_STATION)
                .where("organize_code", organize_code)
                .where("name", name)
                .where("CSId", "<>", CSId)
                .exist()) {
            return new SyncResult(3, "存在相同名字的站点");
        }

        // 验证省、市、区、街道的代码是否有效
        SysProvinceEntity provinceEntity = SysProvinceEntity.getInstance().getWithCode(province_code);
        if (provinceEntity == null || provinceEntity.province_id == 0) return new SyncResult(2, "请正确选择省份");

        SysCityEntity cityEntity = SysCityEntity.getInstance().getWithCode(city_code);
        if (cityEntity == null || cityEntity.city_id == 0) return new SyncResult(2, "请正确选择城市");

        SysAreaEntity areaEntity = SysAreaEntity.getInstance().getWithCode(district_code);
        if (areaEntity == null || areaEntity.area_id == 0) return new SyncResult(2, "请正确选择行政区域");

        SysStreetEntity streetEntity = SysStreetEntity.getInstance().getWithCode(street_code);
        if (streetEntity == null || streetEntity.street_id == 0) return new SyncResult(2, "请正确选择街道");

        // 构建更新的数据
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);

        data.put("province", provinceEntity.province_name);
        data.put("province_code", province_code);
        data.put("city", cityEntity.city_name);
        data.put("city_code", city_code);
        data.put("district", areaEntity.area_name);
        data.put("district_code", district_code);
        data.put("street", streetEntity.street_name);
        data.put("street_code", street_code);

        data.put("communities", communities);
        data.put("roads", roads);
        data.put("address", address);
        data.put("lon", lon);
        data.put("lat", lat);

        // 更新数据库中的站点记录
        int noquery = DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION)
                .table(InspectChargeStationService.DB_TABLE_CHARGE_STATION)
                .where("CSId", CSId)
                .update(data);

        // 返回操作结果
        if (noquery > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }

    /**
     * 获取站点列表
     * <p>
     * 根据组织代码和搜索条件获取站点列表，支持模糊搜索和分页查询。
     *
     * @param organize_code 组织代码，非空，用于标识所属组织
     * @param search_text   搜索文本，非空，用于模糊匹配站点名称、编号及相关地址信息
     * @param page          当前页码，从1开始
     * @param limit         每页记录数
     * @return {@link JSONObject} 包含站点列表及分页信息的结果
     */
    public ISyncResult getList(@NonNull String organize_code
            , @NonNull String search_text
            , int page
            , int limit) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        // 构建缓存的 Key，用于标识缓存数据
        String cacheKey = common.md5(String.format("%s%s"
                , organize_code
                , search_text
        ));

        // 从缓存中读取总记录数
        long count = DataService.getMainCache().getInt(String.format("FireSafety:CSList:%s:TotalCount", cacheKey), -1);
        if (count != -1) {
            // 如果缓存中存在总数数据，则尝试从缓存中获取对应的分页数据
            List<Map<String, Object>> list = DataService.getMainCache()
                    .getList(String.format("FireSafety:CSList:%s:%s_%s", cacheKey, limit, page));
            if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
        }

        // 从数据库中查询符合条件的站点总数
        count = DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION)
                .table(InspectChargeStationService.DB_TABLE_CHARGE_STATION)
                .where("organize_code", organize_code)
                .whereBuilder("AND", "(", "name", "like", String.format("%%%s%%", search_text), "")
                .whereBuilder("OR", "station_number", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "province", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "city", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "district", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "street", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "communities", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "roads", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "", "address", "like", String.format("%%%s%%", search_text), ")")
                .count();
        if (count == 0) return new SyncResult(1, "");

        // 查询数据库中符合条件的分页数据
        List<Map<String, Object>> list = DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION)
                .table(InspectChargeStationService.DB_TABLE_CHARGE_STATION)
                .field("CSId,name,station_number,status,province,city,district,street,communities,roads,address,arch,total_socket,ad_panel_count,online_time")
                .where("organize_code", organize_code)
                .whereBuilder("AND", "(", "name", "like", String.format("%%%s%%", search_text), "")
                .whereBuilder("OR", "station_number", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "province", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "city", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "district", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "street", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "communities", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "roads", "like", String.format("%%%s%%", search_text))
                .whereBuilder("OR", "", "address", "like", String.format("%%%s%%", search_text), ")")
                .page(page, limit)
                .select();

        if (list == null || list.isEmpty()) return new SyncResult(1, "");

        // 将查询结果存入缓存
        DataService.getMainCache().setList(String.format("FireSafety:CSList:%s:%s_%s", cacheKey, limit, page), list);
        DataService.getMainCache().set(String.format("FireSafety:CSList:%s:TotalCount", cacheKey), count);

        // 返回包含分页信息和站点列表的JSON对象
        return new SyncListResult(count, page, limit, list);
    }

    /**
     * 获取站点详情
     * <p>
     * 查询指定组织下的站点详情，支持通过站点的唯一标识（CSId）查询。
     *
     * @param organize_code 组织代码，非空，用于标识所属组织
     * @param CSId          站点唯一标识编码，非空
     * @return {@link JSONObject} 返回站点详情数据
     */
    public ISyncResult getDetail(@NonNull String organize_code, @NonNull String CSId) {
        // 校验组织编码和站点ID是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");
        if (VerifyUtil.isEmpty(CSId)) return new SyncResult(2, "无站点数据");

        // 优先从缓存中根据唯一标识查询站点详情
        Map<String, Object> data = DataService.getMainCache().getMap(String.format("FireSafety:CSDetail:%s", CSId), null);
        if (data == null || data.isEmpty()) {
            // 从数据库中查询站点详情
            data = DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION)
                    .table(InspectChargeStationService.DB_TABLE_CHARGE_STATION)
                    .field("CSId,name,status,main_image,online_time" +
                            ",province,city,district,street,communities,roads,address" +
                            ",arch,total_socket,ad_panel_count,platform_code,organize_code")
                    .where("CSId", CSId)
                    .find();
            if (data == null || data.isEmpty()) return new SyncResult(1, "");

            // 将站点详情存入缓存
            DataService.getMainCache().setObj(String.format("FireSafety:CSDetail:%s", CSId), data);
        }

        // 权限校验，确保只能查询所属组织的站点
        if (!organize_code.equalsIgnoreCase(MapUtil.getString(data, "organize_code"))) {
            return new SyncResult(99, "无权查询");
        }

        // 返回站点详情数据
        return new SyncResult(0, "", data);
    }

    /**
     * 为站点绑定设备
     * <p>
     * 将设备绑定到指定的站点上，确保设备和站点均属于同一组织，并进行权限校验。
     *
     * @param organize_code 组织代码，非空，用于权限校验
     * @param serialNumber  设备序列号，非空
     * @param CSId          站点唯一标识编码，非空
     * @return {@link JSONObject} 返回绑定操作的结果
     */
    public ISyncResult bindDevice(@NonNull String organize_code, @NonNull String CSId, @NonNull String serialNumber) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");
        try {
            // 查询设备信息
            GeneralDeviceEntity deviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(serialNumber, false);
            if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "设备不存在，请检查设备码是否正确");

            // 检查设备是否已绑定到其他站点
            if (!ChargeStationUtils.isEmptyCSId(deviceEntity.CSId) && !deviceEntity.CSId.equalsIgnoreCase(CSId)) {
                LogsUtil.info(FireSafetyService.TAG, "此设备已绑定到其他站点上! 设备：%s 站点：%s 组织：%s", serialNumber, CSId, organize_code);
                return new SyncResult(4, "此设备已绑定到其他站点上");
            }
            if (!ChargeStationUtils.isEmptyCSId(deviceEntity.CSId) && deviceEntity.CSId.equalsIgnoreCase(CSId)) {
                return new SyncResult(5, "设备已绑定，无需再次操作");
            }

            // 查询站点信息
            Map<String, Object> cs_data = InspectChargeStationService.getInstance().getBaseInfo(CSId);
            if (MapUtil.isEmpty(cs_data)) {
                return new SyncResult(6, "站点不存在，请选择正确的站点信息");
            }

            // 权限校验，确保站点属于当前组织
            if (!organize_code.equalsIgnoreCase(MapUtil.getString(cs_data, "organize_code"))) {
                return new SyncResult(99, "无权操作");
            }

            // 构建更新的数据
            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("organize_code", MapUtil.getString(cs_data, "organize_code"));
            set_data.put("platform_code", MapUtil.getString(cs_data, "platform_code"));
            set_data.put("CSId", MapUtil.getString(cs_data, "CSId"));

            // 更新设备信息
            GeneralDeviceEntity.getInstance().where("serialNumber", serialNumber).update(set_data);

            // 更新子设备信息
            GeneralDeviceEntity.getInstance().where("mainSerialNumber", serialNumber).update(set_data);

            // 返回操作结果
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, FireSafetyService.TAG, "设备绑定站点发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 解除站点与设备的绑定
     * <p>
     * 将设备从指定的站点上解绑，确保设备和站点均属于同一组织，并进行权限校验。
     *
     * @param organize_code 组织代码，非空，用于权限校验
     * @param serialNumber  设备序列号，非空
     * @param CSId          站点唯一标识编码，非空
     * @return {@link JSONObject} 返回解绑操作的结果
     */
    public ISyncResult unBindDevice(@NonNull String organize_code, @NonNull String CSId, @NonNull String serialNumber) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");
        try {
            // 查询设备信息
            GeneralDeviceEntity deviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(serialNumber, false);
            if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "设备不存在，请检查设备码是否正确");

            // 检查设备是否绑定到指定的站点
            if (!ChargeStationUtils.isEmptyCSId(deviceEntity.CSId) && !deviceEntity.CSId.equalsIgnoreCase(CSId)) {
                LogsUtil.info(FireSafetyService.TAG, "此设备已绑定到其他站点上，无法进行解绑! 设备：%s 站点：%s 组织：%s", serialNumber, CSId, organize_code);
                return new SyncResult(4, "此设备已绑定到其他站点上，无法进行解绑");
            }

            // 查询站点信息
            Map<String, Object> cs_data = InspectChargeStationService.getInstance().getBaseInfo(CSId);
            if (MapUtil.isEmpty(cs_data)) {
                return new SyncResult(6, "站点不存在，请选择正确的站点信息");
            }

            // 权限校验，确保站点属于当前组织
            if (!organize_code.equalsIgnoreCase(MapUtil.getString(cs_data, "organize_code"))) {
                return new SyncResult(99, "无权操作");
            }

            // 构建更新的数据，将组织编码和站点ID清空
            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("organize_code", "");
            set_data.put("platform_code", "");
            set_data.put("CSId", "");

            // 更新设备信息
            GeneralDeviceEntity.getInstance().where("serialNumber", serialNumber).update(set_data);

            // 更新子设备信息
            GeneralDeviceEntity.getInstance().where("mainSerialNumber", serialNumber).update(set_data);

            // 返回操作结果
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, FireSafetyService.TAG, "设备解绑站点发生错误");
        }
        return new SyncResult(1, "");
    }
}