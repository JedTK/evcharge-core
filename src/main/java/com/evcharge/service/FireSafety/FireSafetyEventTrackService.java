package com.evcharge.service.FireSafety;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.LogTrack.FireSafetyEventTrackEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.enumdata.EEventLevel;
import com.evcharge.enumdata.EFireSafetyEventType;
import com.evcharge.service.GeneralDevice.GeneralDeviceService;
import com.evcharge.service.Inspect.InspectChargeStationService;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.lang.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消防事件跟踪服务类
 * <p>
 * 提供与消防安全事件跟踪相关的操作，包括新增事件日志、获取事件列表、获取事件详情等功能。
 */
public class FireSafetyEventTrackService {

    // 单例模式的静态实例，使用volatile关键字确保可见性和防止指令重排
    private volatile static FireSafetyEventTrackService _this;

    /**
     * 获取单例实例
     * <p>
     * 采用双重检查锁定机制（Double-Check Locking）确保线程安全，懒加载单例模式。
     *
     * @return {@link FireSafetyDeviceService}的单例实例
     */
    public static FireSafetyEventTrackService getInstance() {
        if (_this == null) {
            synchronized (FireSafetyEventTrackService.class) {
                if (_this == null) _this = new FireSafetyEventTrackService();
            }
        }
        return _this;
    }

    // region 新增日志跟踪

    /**
     * 新增消防安全事件跟踪日志
     * <p>
     * 重载方法，使用事件标题、事件类型、事件等级、设备编码、站点编码、平台编码和组织编码。
     *
     * @param event_title   事件标题
     * @param event_type    事件类型，枚举类型{@link EFireSafetyEventType}
     * @param event_level   事件等级，枚举类型{@link EEventLevel}
     * @param status        事件状态，0-忽略，1-进行中，2-结束
     * @param device_code   设备编码
     * @param cs_id         站点编码
     * @param platform_code 平台编码
     * @param organize_code 组织编码
     * @return 新增的事件ID
     */
    public String add(String event_title
            , EFireSafetyEventType event_type
            , EEventLevel event_level
            , int status
            , String device_code
            , String cs_id
            , String platform_code
            , String organize_code
    ) {
        // 调用重载方法，传入null值的事件数据、设备信息、站点信息和空的主事件ID
        return add(event_title, event_type, event_level, status, null, device_code, null, cs_id, null, "", platform_code, organize_code);
    }

    /**
     * 新增消防安全事件跟踪日志
     * <p>
     * 重载方法，增加了主事件ID参数，用于关联主事件。
     *
     * @param event_title   事件标题
     * @param event_type    事件类型，枚举类型{@link EFireSafetyEventType}
     * @param event_level   事件等级，枚举类型{@link EEventLevel}
     * @param status        事件状态，0-忽略，1-进行中，2-结束
     * @param device_code   设备编码
     * @param cs_id         站点编码
     * @param main_event_id 主事件ID
     * @param platform_code 平台编码
     * @param organize_code 组织编码
     * @return 新增的事件ID
     */
    public String add(String event_title
            , EFireSafetyEventType event_type
            , EEventLevel event_level
            , int status
            , String device_code
            , String cs_id
            , String main_event_id
            , String platform_code
            , String organize_code
    ) {
        // 调用重载方法，传入null值的事件数据、设备信息和站点信息
        return add(event_title, event_type, event_level, status, null, device_code, null, cs_id, null, main_event_id, platform_code, organize_code);
    }

    /**
     * 新增消防安全事件跟踪日志
     * <p>
     * 重载方法，增加了事件数据、设备信息、站点信息等参数。
     *
     * @param event_title   事件标题
     * @param event_type    事件类型，枚举类型{@link EFireSafetyEventType}
     * @param event_level   事件等级，枚举类型{@link EEventLevel}
     * @param status        事件状态，0-忽略，1-进行中，2-结束
     * @param event_data    事件数据，JSON格式
     * @param device_code   设备编码
     * @param device_info   设备信息，JSON格式
     * @param cs_id         站点编码
     * @param cs_info       站点信息，JSON格式
     * @param main_event_id 主事件ID
     * @param platform_code 平台编码
     * @param organize_code 组织编码
     * @return 新增的事件ID
     */
    public String add(String event_title
            , EFireSafetyEventType event_type
            , EEventLevel event_level
            , int status
            , JSONObject event_data
            , String device_code
            , JSONObject device_info
            , String cs_id
            , JSONObject cs_info
            , String main_event_id
            , String platform_code
            , String organize_code
    ) {
        // 将事件类型转换为字符串并调用下一层重载方法
        return add(event_title, event_type.toString(), event_level, status, event_data, device_code, device_info, cs_id, cs_info, main_event_id, platform_code, organize_code);
    }

    /**
     * 新增消防安全事件跟踪日志
     * <p>
     * 重载方法，事件类型使用字符串形式。
     *
     * @param event_title   事件标题
     * @param event_type    事件类型，字符串形式
     * @param event_level   事件等级，枚举类型{@link EEventLevel}
     * @param status        事件状态，0-忽略，1-进行中，2-结束
     * @param device_code   设备编码
     * @param cs_id         站点编码
     * @param platform_code 平台编码
     * @param organize_code 组织编码
     * @return 新增的事件ID
     */
    public String add(String event_title
            , String event_type
            , EEventLevel event_level
            , int status
            , String device_code
            , String cs_id
            , String platform_code
            , String organize_code
    ) {
        // 调用重载方法，传入null值的事件数据、设备信息、站点信息和空的主事件ID
        return add(event_title, event_type, event_level, status, null, device_code, null, cs_id, null, "", platform_code, organize_code);
    }

    /**
     * 新建消防安全事件跟踪日志
     * <p>
     * 重载方法，增加了主事件ID参数。
     *
     * @param event_title   事件标题
     * @param event_type    事件类型，字符串形式
     * @param event_level   事件等级，枚举类型{@link EEventLevel}
     * @param status        事件状态，0-忽略，1-进行中，2-结束
     * @param device_code   设备编码
     * @param cs_id         站点编码
     * @param main_event_id 主事件ID
     * @param platform_code 平台编码
     * @param organize_code 组织编码
     * @return 新增的事件ID
     */
    public String add(String event_title
            , String event_type
            , EEventLevel event_level
            , int status
            , String device_code
            , String cs_id
            , String main_event_id
            , String platform_code
            , String organize_code
    ) {
        // 调用重载方法，传入null值的事件数据、设备信息和站点信息
        return add(event_title, event_type, event_level, status, null, device_code, null, cs_id, null, main_event_id, platform_code, organize_code);
    }

    /**
     * 新建消防安全事件跟踪日志
     * <p>
     * 实际执行新增事件日志的核心方法，包含所有参数。
     *
     * @param event_title   事件标题
     * @param event_type    事件类型，字符串形式
     * @param event_level   事件等级，枚举类型{@link EEventLevel}
     * @param status        事件状态，0-忽略，1-进行中，2-结束
     * @param event_data    事件数据，JSON格式
     * @param device_code   设备编码
     * @param device_info   设备信息，JSON格式
     * @param cs_id         站点编码
     * @param cs_info       站点信息，JSON格式
     * @param main_event_id 主事件ID
     * @param platform_code 平台编码
     * @param organize_code 组织编码
     * @return 新增的事件ID
     */
    public String add(String event_title
            , String event_type
            , EEventLevel event_level
            , int status
            , JSONObject event_data
            , String device_code
            , JSONObject device_info
            , String cs_id
            , JSONObject cs_info
            , String main_event_id
            , String platform_code
            , String organize_code
    ) {
        // 生成唯一的事件ID
        String event_id = common.md5(common.getUUID());

        // 创建事件跟踪实体对象
        FireSafetyEventTrackEntity entity = new FireSafetyEventTrackEntity();
        entity.event_id = event_id;
        entity.event_type = event_type;
        entity.event_title = event_title;
        entity.event_level = event_level.getIndex();
        entity.event_data = "";
        entity.status = status;
        // 如果事件数据不为空，转换为JSON字符串
        if (event_data != null) entity.event_data = event_data.toJSONString();

        // 设置设备编码
        entity.device_code = device_code;
        entity.device_info = "";
        // 如果设备信息不为空，转换为JSON字符串
        if (device_info != null) entity.device_info = device_info.toJSONString();
        if (StringUtil.isEmpty(entity.device_info)) {
            // 自动注入设备数据
            GeneralDeviceEntity deviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(device_code);
            if (deviceEntity != null && deviceEntity.id != 0) {
                entity.device_info = new JSONObject() {{
                    put("deviceName", deviceEntity.deviceName);
                    put("serialNumber", deviceEntity.serialNumber);
                    put("mainSerialNumber", deviceEntity.mainSerialNumber);
                    put("spuCode", deviceEntity.spuCode);
                    put("typeCode", deviceEntity.typeCode);
                    put("brandCode", deviceEntity.brandCode);
                }}.toJSONString();
            }
        }

        entity.cs_id = cs_id;
        Map<String, Object> cs_data = null;
        // 设置站点编码
        if (!StringUtil.isEmpty(cs_id)) {
            // 获取站点基础信息
            cs_data = InspectChargeStationService.getInstance().getBaseInfo(cs_id);
            entity.event_title = String.format("%s%s", MapUtil.getString(cs_data, "name"), event_title);
        }
        entity.cs_info = "";
        // 如果站点信息不为空，转换为JSON字符串
        if (cs_info != null) entity.cs_info = cs_info.toJSONString();
        if (StringUtil.isEmpty(entity.cs_info)) entity.cs_info = MapUtil.toJSONString(cs_data);

        // 设置主事件ID
        if (!StringUtil.hasLength(main_event_id)) entity.main_event_id = main_event_id;

        // 设置平台编码和组织编码
        entity.platform_code = platform_code;
        entity.organize_code = organize_code;

        // 设置创建时间为当前时间戳
        entity.create_time = TimeUtil.getTimestamp();
        // 将实体插入数据库
        entity.insert();

        // 返回生成的事件ID
        return event_id;
    }

    // endregion

    /**
     * 获取设备事件日志列表
     *
     * @param organize_code 组织编码，不能为空
     * @param device_code   设备编码
     * @param page          当前页码
     * @param limit         每页显示的记录数
     * @return 包含设备事件日志列表的JSON对象
     */
    public ISyncResult getDeviceEventList(@NonNull String organize_code
            , String device_code
            , int page
            , int limit) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");
        // 校验设备编码是否为空
        if (VerifyUtil.isEmpty(device_code)) return new SyncResult(2, "无效设备");

        // 根据设备编码获取设备实体
        GeneralDeviceEntity deviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(device_code);
        if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(2, "无效设备");

        // 校验设备所属组织是否与请求的组织一致
        if (!organize_code.equalsIgnoreCase(deviceEntity.organize_code)) {
            return new SyncResult(99, "无权查询其他组织设备信息");
        }

        // 生成缓存键，用于缓存查询结果
        String cacheKey = common.md5(String.format("%s%s", organize_code, device_code));

        // 创建用于统计总记录数的实体
        FireSafetyEventTrackEntity countEntity = new FireSafetyEventTrackEntity();
        countEntity.cache(String.format("FireSafety:EventTrack:%s:List:%s:TotalCount", EFireSafetyEventType.DEVICE_EVENT.toString(), cacheKey))
                .where("event_type", EFireSafetyEventType.DEVICE_EVENT)
                .where("organize_code", organize_code);

        // 创建用于查询记录的实体
        FireSafetyEventTrackEntity entity = new FireSafetyEventTrackEntity();
        entity.cache(String.format("FireSafety:EventTrack:%s:List:%s:%s_%s", EFireSafetyEventType.DEVICE_EVENT.toString(), cacheKey, limit, page))
                .where("event_type", EFireSafetyEventType.DEVICE_EVENT)
                .where("organize_code", organize_code);

        // 如果设备关联了站点，则根据站点ID查询，否则根据设备编码查询
        if (!StringUtil.isEmpty(deviceEntity.CSId)) {
            countEntity.where("CSId", deviceEntity.CSId);
            entity.where("CSId", deviceEntity.CSId);
        } else {
            countEntity.where("device_code", device_code);
            entity.where("device_code", device_code);
        }

        // 获取总记录数
        long count = countEntity.count();
        if (count == 0) return new SyncResult(1, "");

        // 分页查询记录列表
        List<Map<String, Object>> list = entity.page(page, limit)
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(1, "");

        // 返回包含分页信息和记录列表的JSON对象
        return new SyncListResult(count, page, limit, list);
    }

    /**
     * 根据事件类型获取日志列表
     *
     * @param organize_code 组织编码，不能为空
     * @param event_type    事件类型，枚举类型{@link EFireSafetyEventType}
     * @param page          当前页码
     * @param limit         每页显示的记录数
     * @return 包含事件日志列表的JSON对象
     */
    public ISyncResult getList(@NonNull String organize_code
            , EFireSafetyEventType event_type
            , int page
            , int limit) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        // 生成缓存键
        String cacheKey = common.md5(String.format("%s%s", organize_code, event_type.toString()));

        // 创建用于统计总记录数的实体
        FireSafetyEventTrackEntity countEntity = new FireSafetyEventTrackEntity();
        countEntity.cache(String.format("FireSafety:EventTrack:List:%s:TotalCount", cacheKey), ECacheTime.MINUTE)
                .where("event_type", event_type.toString())
                .where("organize_code", organize_code)
                .where("create_time", ">=", TimeUtil.getTimestamp() - ECacheTime.HOUR * 3);

        // 创建用于查询记录的实体
        FireSafetyEventTrackEntity entity = new FireSafetyEventTrackEntity();
        entity.cache(String.format("FireSafety:EventTrack:List:%s:%s_%s", cacheKey, limit, page), ECacheTime.MINUTE)
                .field("event_id,event_type,event_title,event_level,event_data,status,device_code,device_info,cs_id,cs_info,main_event_id,platform_code,organize_code,create_time")
                .where("event_type", event_type.toString())
                .where("organize_code", organize_code)
                .where("create_time", ">=", TimeUtil.getTimestamp() - ECacheTime.HOUR * 3)
                .order("create_time desc")
                .page(page, limit);

        // 获取总记录数
        long count = countEntity.count();
        if (count == 0) return new SyncResult(1, "");

        // 分页查询记录列表
        List<Map<String, Object>> list = entity.select();
        if (list == null || list.isEmpty()) return new SyncResult(1, "");

        // 返回包含分页信息和记录列表的JSON对象
        return new SyncListResult(count, page, limit, list);
    }

    /**
     * 获取事件详情
     *
     * @param organize_code 组织编码，不能为空
     * @param event_id      事件ID
     * @return 包含事件详细信息的JSON对象
     */
    public ISyncResult getDetail(@NonNull String organize_code, String event_id) {
        // 校验组织编码是否为空
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        // 根据事件ID获取事件实体
        FireSafetyEventTrackEntity entity = FireSafetyEventTrackEntity.getInstance()
                .where("event_id", event_id)
                .findEntity();
        if (entity == null || entity.id == 0) return new SyncResult(1, "无效事件");

        // 校验事件所属组织是否与请求的组织一致
        if (!organize_code.equalsIgnoreCase(entity.organize_code)) {
            return new SyncResult(99, "无权查询其他组织设备信息");
        }

        JSONObject event_data = new JSONObject();
        if (!StringUtil.isEmpty(entity.event_data)) {
            event_data = JSONObject.parse(entity.event_data);
        }

        // 创建数据映射，用于存储返回的数据
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("event_id", entity.event_id);
        data.put("event_type", entity.event_type);
        data.put("event_title", entity.event_title);
        data.put("event_level", entity.event_level);
        // 解析事件数据
        data.put("event_data", event_data);
        data.put("device_code", entity.device_code);
        data.put("cs_id", entity.cs_id);
        data.put("main_event_id", entity.main_event_id);
        data.put("create_time", entity.create_time);

        // 注入设备信息
        JSONObject device_info = new JSONObject();
        if (StringUtil.hasLength(entity.device_info)) device_info = JSONObject.parse(entity.device_info);
        if (StringUtil.hasLength(entity.device_code)) {
            GeneralDeviceEntity deviceEntity = GeneralDeviceService.getInstance().getWithSerialNumber(entity.device_code);
            if (deviceEntity != null && deviceEntity.id != 0) {
                device_info.put("deviceName", deviceEntity.deviceName);
                device_info.put("serialNumber", deviceEntity.serialNumber);
                device_info.put("spuCode", deviceEntity.spuCode);
                device_info.put("brandCode", deviceEntity.brandCode);
                device_info.put("typeCode", deviceEntity.typeCode);
                device_info.put("online_status", deviceEntity.online_status);
                device_info.put("status", deviceEntity.status);
                device_info.put("spec", StringUtil.hasLength(deviceEntity.spec) ? JSONObject.parse(deviceEntity.spec) : new JSONObject());
                device_info.put("dynamic_info", StringUtil.hasLength(deviceEntity.dynamic_info) ? JSONObject.parse(deviceEntity.dynamic_info) : new JSONObject());
                device_info.put("remark", deviceEntity.remark);
            }
        }

        // 注入站点信息
        JSONObject cs_info = new JSONObject();
        if (StringUtil.hasLength(entity.cs_info)) cs_info = JSONObject.parse(entity.cs_info);
        if (StringUtil.hasLength(entity.cs_id)) {
            Map<String, Object> cs_data = InspectChargeStationService.getInstance().getBaseInfo(entity.cs_id);
            if (cs_data != null && !cs_data.isEmpty()) {
                cs_info = MapUtil.toJSONObject(cs_data);
            }
        }

        // 将设备信息和站点信息加入返回数据
        data.put("device_info", device_info);
        data.put("cs_info", cs_info);

        // 返回包含事件详情的JSON对象
        return new SyncResult(0, "", data);
    }
}