package com.evcharge.service.Inspect;

import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.utils.StringUtil;
import lombok.NonNull;

import java.util.Map;

/**
 * 巡检站点服务类
 * <p>
 * 提供与巡检站点相关的操作，例如获取站点总数、获取站点基础信息等功能。
 */
public class InspectChargeStationService {
    /**
     * 站点数据库名
     */
    public final static String DB_CHARGE_STATION = "inspect";
    /**
     * 站点数据表名
     */
    public final static String DB_TABLE_CHARGE_STATION = "ChargeStation";

    /**
     * 单例模式的静态实例，使用volatile关键字确保可见性和防止指令重排
     */
    private volatile static InspectChargeStationService _this;

    /**
     * 获取单例实例
     * <p>
     * 采用双重检查锁定机制（Double-Check Locking）确保线程安全，懒加载单例模式。
     *
     * @return {@link InspectChargeStationService}的单例实例
     */
    public static InspectChargeStationService getInstance() {
        if (_this == null) {
            synchronized (InspectChargeStationService.class) {
                if (_this == null) _this = new InspectChargeStationService();
            }
        }
        return _this;
    }

    /**
     * 获取指定组织下的站点总数
     *
     * @param organize_code 组织编码，不能为空
     * @return 该组织下的站点总数，整数类型
     */
    public int getTotalCountWitOrganize(@NonNull String organize_code) {
        if (StringUtil.isEmpty(organize_code)) return 0;
        // 使用DataService连接指定的数据库和数据表，添加查询条件并统计数量
        return DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION)
                .table(InspectChargeStationService.DB_TABLE_CHARGE_STATION)
                .where("organize_code", organize_code)
                .count();
    }

    /**
     * 获取站点的基础信息，包括CSId、站点名、编号、详细地址、组织编码、平台编码等
     *
     * @param CSId 站点的唯一标识符
     * @return 包含站点基础信息的Map集合
     */
    public Map<String, Object> getBaseInfo(@NonNull String CSId) {
        // 默认优先从缓存中获取数据
        return getBaseInfo(CSId, true, true);
    }

    /**
     * 获取站点的基础信息，包括CSId、站点名、编号、详细地址、组织编码、平台编码等
     *
     * @param CSId                    站点的唯一标识符
     * @param inCache                 是否优先从缓存中获取数据，true表示从缓存中获取，false表示直接从数据库获取
     * @param allowMultipleDataSource 允许多个数据源采集（优先从巡检站点数据源采集数据-->元气充站点数据）
     * @return 包含站点基础信息的Map集合，如果未找到则返回null
     */
    public Map<String, Object> getBaseInfo(@NonNull String CSId, boolean inCache, boolean allowMultipleDataSource) {
        if (StringUtil.isEmpty(CSId)) return null;

        BaseEntity entity = new BaseEntity();
        // 如果需要从缓存中获取数据，则设置缓存键
        if (inCache) {
            String cacheKey = String.format("BaseData:%s_DB:%s:BaseInfo:%s",
                    InspectChargeStationService.DB_CHARGE_STATION,
                    InspectChargeStationService.DB_TABLE_CHARGE_STATION,
                    CSId);
            entity.cache(cacheKey);
        }

        // 设置数据库和表名
        entity.setDB(DataService.getDB(InspectChargeStationService.DB_CHARGE_STATION))
                .setTable(InspectChargeStationService.DB_TABLE_CHARGE_STATION);
        entity.field("CSId,name,station_number,province,city,district,street,communities,roads,address,lon,lat,platform_code,organize_code");
        entity.where("CSId", CSId);

        // 执行查询并返回结果
        Map<String, Object> data = entity.find();
        if ((data == null || data.isEmpty()) && allowMultipleDataSource)
            return getBaseInfoByGenkigoDataSource(CSId, inCache);
        return data;
    }

    /**
     * 通过元气充数据源获取站点的基础信息，包括CSId、站点名、编号、详细地址、组织编码、平台编码等
     *
     * @param CSId    站点的唯一标识符
     * @param inCache 是否优先从缓存中获取数据，true表示从缓存中获取，false表示直接从数据库获取
     * @return 包含站点基础信息的Map集合，如果未找到则返回null
     */
    public Map<String, Object> getBaseInfoByGenkigoDataSource(@NonNull String CSId, boolean inCache) {
        if (StringUtil.isEmpty(CSId)) return null;

        ChargeStationEntity entity = new ChargeStationEntity();
        // 如果需要从缓存中获取数据，则设置缓存键
        if (inCache) {
            String cacheKey = String.format("BaseData:Genkigo:ChargeStation:BaseInfo:%s", CSId);
            entity.cache(cacheKey);
        }
        // 指定需要查询的字段
        return entity.field("CSId,name,stationNumber AS station_number,province,city,district,street,communities,roads,address,lon,lat,platform_code,organize_code")
                .where("CSId", CSId)
                .find();
    }
}