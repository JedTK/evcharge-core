package com.evcharge.service.FireSafety;

import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.service.Inspect.InspectChargeStationService;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.VerifyUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 消防逻辑层
 */
public class FireSafetyService {
    /**
     * 标签
     */
    protected final static String TAG = "消防安全";

    /**
     * 消防系统来源
     */
    protected final static String FIRE_SAFETY_SOURCE = "FireSafetySystem";

    // 单例模式的静态实例
    private volatile static FireSafetyService _this;

    /**
     * 获取单例
     * 采用双重检查锁定机制确保线程安全
     *
     * @return
     */
    public static FireSafetyService getInstance() {
        if (_this == null) {
            synchronized (FireSafetyService.class) {
                if (_this == null) _this = new FireSafetyService();
            }
        }
        return _this;
    }

    /**
     * 获取指定组织下的站点总数、监控设备总数和消防设备总数
     *
     * @param organize_code 组织代码，用于标识不同的组织
     * @return JSONObject 包含站点、监控设备、消防设备总数的数据
     * 格式：
     * {
     * "cs_count": int, // 站点总数
     * "nvr_count": int, // 监控设备总数
     * "iafds_count": int // 消防设备总数
     * }
     * 逻辑说明：
     * 1. 从数据库中查询指定组织下的站点总数，表名为 "ChargeStation"。
     * 2. 根据设备类型代码 "4GNVR" 查询监控设备（如摄像头）的总数。
     * 3. 根据设备类型代码 "IAFDS" 查询消防设备（如消防柜）的总数。
     * 4. 将查询结果封装到 JSON 对象并返回。
     */
    public ISyncResult getSummaryCount(String organize_code) {
        if (VerifyUtil.isEmpty(organize_code)) return new SyncResult(99, "无权操作");

        Map<String, Object> cb_data = DataService.getMainCache().getMap(String.format("FireSafety:SummaryCount:%s", organize_code));
        if (!MapUtil.isEmpty(cb_data)) return new SyncResult(0, "", cb_data);

        // 获取站点总数（数据库表：ChargeStation）
        int cs_count = InspectChargeStationService.getInstance().getTotalCountWitOrganize(organize_code);

        // 获取监控设备总数（设备类型代码：4GNVR）
        int nvr_count = GeneralDeviceEntity.getInstance()
                .where("typeCode", "4GNVR")
                .where("organize_code", organize_code)
                .count();

        // 获取消防设备总数（设备类型代码：IAFDS）
        int iafds_count = GeneralDeviceEntity.getInstance()
                .where("typeCode", "IAFDS")
                .where("organize_code", organize_code)
                .count();

        // 将结果封装到 LinkedHashMap 以保持顺序
        cb_data = new LinkedHashMap<>();
        cb_data.put("cs_count", cs_count); // 添加站点总数
        cb_data.put("nvr_count", nvr_count); // 添加监控设备总数
        cb_data.put("iafds_count", iafds_count); // 添加消防设备总数

        DataService.getMainCache().setMap(String.format("FireSafety:SummaryCount:%s", organize_code), cb_data, ECacheTime.MINUTE * 10);

        // 返回统一格式的 JSON 对象
        return new SyncResult(0, "", cb_data);
    }
}
