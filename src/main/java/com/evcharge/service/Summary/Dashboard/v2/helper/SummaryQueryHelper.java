package com.evcharge.service.Summary.Dashboard.v2.helper;

import com.evcharge.dto.summary.RegionRequest;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.service.Summary.Dashboard.v2.builder.RegionQueryBuilder;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class SummaryQueryHelper {
    private final RegionRequest request;

    public SummaryQueryHelper(RegionRequest request) {
        this.request = request;
    }

    /**
     * 获取某段时间设备使用率
     * @param startOffset int
     * @param endOffset int
     * @return BigDecimal
     */
    public BigDecimal getSocketUseRateBetweenDays(int startOffset, int endOffset) {

        Map<String, Object> deviceSocketUsedCountData = applyRegion(DataService.getDB().name("ChargeOrderView"))
                .field("COUNT(DISTINCT deviceCode, port) AS deviceSocketUseRate")
                .where("status", 2)
                .where("create_time", ">=", TimeUtil.getTime00(startOffset))
                .where("create_time", "<=", TimeUtil.getTime24(endOffset))
                .find();
        int deviceSocketCount = getAllSocket();

        BigDecimal currentDeviceSocketUseRate = null;
        long deviceSocketUseRate = MapUtil.getLong(deviceSocketUsedCountData, "deviceSocketUseRate");
        if (deviceSocketUseRate > 0) {
            currentDeviceSocketUseRate = new BigDecimal(deviceSocketUseRate).divide(new BigDecimal(deviceSocketCount), 6, RoundingMode.HALF_UP);
        }
        return currentDeviceSocketUseRate;
    }

    /**
     * 获取设备口数量
     * @return int
     */
    public int getAllSocket() {
        String[] deviceIds = getDeviceIds();

        return applyRegion(DataService.getDB().name("DeviceSocketView"))
                .whereIn("deviceId", deviceIds)
                .count();

    }

    /**
     * 获取解决异常状况数据
     * @return int
     */
    public int getAnomaliesResolved(){
        return applyRegion(DataService.getDB()
                .name("ChargeStationErrorManageView"))
                .count();
    }

    /**
     * 获取设备使用情况
     * @param status String
     * @return int
     */
    public int getSocketUsage(String status) {
        String[] deviceIds = getDeviceIds();
        return applyRegion(DataService.getDB().name("DeviceSocketView"))
                .whereIn("deviceId", deviceIds)
                .whereIn("status", status)//状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
                .count();
    }

    /**
     * 获取设备id列表
     * @return String[]
     */
    private String[] getDeviceIds() {
        return DeviceEntity.getInstance()
                .where("isHost", 0)//主机：0=否，1=是
                .selectForStringArray("id");
    }

    /**
     * 获取站点数量
     * @return int[]
     */
    public int stationCount() {
        return applyRegion(DataService.getDB("inspect").name("ChargeStation"))
                .where("status", 1)
                .count();
    }

    /**
     * 获取设备口
     * @return int
     */
    public int socketCount() {
        return applyRegion(DataService.getDB("inspect").name("ChargeStation"))
                .where("status", 1)
                .sum("total_socket");
    }

    /**
     * 获取充电订单数量
     * @return int
     */
    public int safeChargeCount() {
        return applyRegion(DataService.getDB().name("ChargeOrderView"))
                .where("status", 2)
                .count();
    }

    /**
     * 统计某个时间区间站点上线数量
     *
     * @param startOffset int
     * @param endOffset int
     * @return int
     */
    public int countStationBetweenDays(int startOffset, int endOffset) {
        return applyRegion(DataService.getDB().name("ChargeStation"))
                .where("online_time", ">=", TimeUtil.getTime00(startOffset))
                .where("online_time", "<=", TimeUtil.getTime24(endOffset))
                .where("status", 1)
                .count();
    }

    /**
     *
     * @param days
     * @return
     */
    public int sumStationSocket(int days) {
        return applyRegion(DataService.getDB().name("ChargeStation"))
                .where("online_time", ">=", TimeUtil.getTime00(days))
                .where("status", 1)
                .sum("totalSocket");
    }

    public int countOrder(int days) {
        return applyRegion(DataService.getDB().name("ChargeOrderView"))
                .where("create_time", ">=", TimeUtil.getTime00(days))
                .where("status", 2)
                .count();
    }


    private ISqlDBObject applyRegion(ISqlDBObject db) {
        return new RegionQueryBuilder(request).applyTo(db);
    }

}
