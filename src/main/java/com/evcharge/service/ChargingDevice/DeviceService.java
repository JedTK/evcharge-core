package com.evcharge.service.ChargingDevice;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceGeneralDataEntity;
import com.xyzs.entity.DataService;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.ThreadPoolManager;
import lombok.NonNull;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 充电设备-业务逻辑层; 慢慢转移到这里
 *
 * @author : JED
 * @date : 2024-11-18
 */
public class DeviceService {

    private final static String TAG = "充电设备业务";

    /**
     * 获得一个实例
     */
    public static DeviceService getInstance() {
        return new DeviceService();
    }

    /**
     * 修复 simCode
     * <p>
     * 逻辑说明：
     * 1. 通过 deviceCode 查询设备。
     * 2. 如果是主机(isHost == 1)：
     * - 只更新 simCode，不更新 CSId。
     * - 如果 simCode 相同则直接返回 true，避免无谓更新。
     * 3. 如果不是主机：
     * - 一定会尝试更新 simCode（如果与旧值不同）。
     * - 尝试根据 simCode 找主机：
     * a) 找到主机：维护 hostDeviceId；
     * b) 除了 AP262-L-4G / AP262-BL 之外的设备，同步 CSId 为主机的 CSId（非空、非"0"、且与当前不同）。
     * - 找不到主机时不会视为失败，只是不能维护 hostDeviceId / CSId。
     */
    public boolean updateSimCode(@NonNull String deviceCode, @NonNull String simCode) {
        if (StringUtil.isEmpty(deviceCode)) return false;
        if (StringUtil.isEmpty(simCode)) return false;

        deviceCode = deviceCode.trim();
        simCode = simCode.trim();

        try {
            DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);
            if (deviceEntity == null) return false;

            Map<String, Object> setData = new LinkedHashMap<>();

            // ✅ 1) 防 NPE 的 sameSim（同时建议做 trim，避免不可见字符导致“看起来一样其实不一样”）
            String oldSim = deviceEntity.simCode == null ? "" : deviceEntity.simCode.trim();
            boolean sameSim = simCode.equalsIgnoreCase(oldSim);

            if (deviceEntity.isHost == 1) {
                if (sameSim) return true;
                setData.put("simCode", simCode);
            } else {
                DeviceEntity hostDeviceEntity = DeviceEntity.getInstance().getHostWithSimCode(simCode);

                if (hostDeviceEntity != null) {
                    if (!java.util.Objects.equals(deviceEntity.hostDeviceId, hostDeviceEntity.id)) {
                        setData.put("hostDeviceId", hostDeviceEntity.id);
                    }

                    if (!StringUtil.isEmpty(hostDeviceEntity.CSId)
                            && !"0".equalsIgnoreCase(hostDeviceEntity.CSId)
                            && !"AP262-L-4G".equals(deviceEntity.spuCode)
                            && !"AP262-BL".equals(deviceEntity.spuCode)) {

                        if (deviceEntity.CSId == null || !hostDeviceEntity.CSId.equalsIgnoreCase(deviceEntity.CSId)) {
                            setData.put("CSId", hostDeviceEntity.CSId);
                        }
                    }
                }

                if (!sameSim) setData.put("simCode", simCode);
            }

            // ✅ 2) 兜底：如果 setData 里只有 simCode，且值没变 -> 不更新
            // （顺手也能防未来有人误改逻辑）
            if (setData.containsKey("simCode")) {
                String newSim = String.valueOf(setData.get("simCode"));
                boolean stillSame = newSim != null && newSim.trim().equalsIgnoreCase(oldSim);
                if (stillSame) {
                    setData.remove("simCode");
                }
            }

            if (setData.isEmpty()) return true;

            DeviceEntity.getInstance()
                    .where("deviceCode", deviceCode)
                    .update(setData);

            LogsUtil.info(TAG, "[%s] 设备更新：更新字段=%s"
                    , deviceCode
                    , new JSONObject(setData).toJSONString()
            );

            if (setData.containsKey("simCode")) {
                Map<String, Object> generalData = new LinkedHashMap<>();
                generalData.put("simCode", simCode);
                DeviceGeneralDataEntity.getInstance().updateData(deviceCode, generalData);
            }

            DataService.getMainCache().del(String.format("Device:%s:Details", deviceCode));
            return true;
        } catch (Exception e) {
            LogsUtil.error(TAG, "[%s] 更新SIM失败：%s", deviceCode, e.getMessage());
            return false;
        }
    }

    /**
     * 自动更新设备的程序渠道编码。
     *
     * @param deviceCode     设备编码，不能为空。
     * @param appChannelCode 新的程序渠道编码，不能为空。
     * @return 如果更新成功，或者无需更新（编码未变）返回true，如果参数不合法或设备不存在，返回false。
     */
    public boolean updateAppChannelCode(@NonNull String deviceCode, @NonNull String appChannelCode) {
        if (!StringUtil.hasLength(deviceCode)) return false;
        if (!StringUtil.hasLength(appChannelCode)) return false;

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);
        if (deviceEntity == null) return false;
        if (deviceEntity.appChannelCode.equals(appChannelCode)) return true;

        Map<String, Object> set_data = new LinkedHashMap<>();
        set_data.put("appChannelCode", appChannelCode);

        try {
            if (deviceEntity.isHost == 1) {
                // 如果是主机，顺便更新从机的appChannelCode
                DeviceEntity nd = new DeviceEntity();
                nd.field("id,deviceCode,deviceNumber")
                        .whereOr("deviceCode", deviceCode)
                        .whereOr("hostDeviceId", deviceEntity.id);
                if (StringUtil.isNotEmpty(deviceEntity.simCode)) nd.whereOr("simCode", deviceEntity.simCode);

                List<Map<String, Object>> list = nd.select();

                if (list != null && !list.isEmpty()) {
                    List<Object> ids = new LinkedList<>();
                    for (Map<String, Object> map : list) {
                        long id_temp = MapUtil.getLong(map, "id");
                        String deviceCode_temp = MapUtil.getString(map, "deviceCode");
                        String deviceNumber_temp = MapUtil.getString(map, "deviceNumber");

                        DataService.getMainCache().del(String.format("Device:%s:Details", deviceCode_temp));
                        DataService.getMainCache().del(String.format("Device:%s:Details", deviceNumber_temp));

                        ids.add(id_temp);
                    }

                    nd = new DeviceEntity();
                    nd.whereIn("id", ids).update(set_data);
                    LogsUtil.info(getClass().getSimpleName(), "设备appChannelCode更新为%s，相关缓存已清理。", appChannelCode);
                    return true;
                }
            }

            deviceEntity.where("deviceCode", deviceEntity.deviceCode).update(set_data);
            DataService.getMainCache().del(String.format("Device:%s:Details", deviceEntity.deviceCode));
            DataService.getMainCache().del(String.format("Device:%s:Details", deviceEntity.deviceNumber));

            LogsUtil.info(getClass().getSimpleName(), "设备appChannelCode更新为%s，相关缓存已清理。", appChannelCode);
            return true;
        } catch (Exception e) {
            LogsUtil.error(getClass().getSimpleName(), "更新设备渠道编码失败", e);
            return false;
        }
    }

    /**
     * 修复设备数据中的 CSId 和主机设备 ID。
     *
     * <p>功能描述：</p>
     * 1. 针对标记为主机设备 (isHost=1) 的记录，分页读取设备信息；
     * 2. 根据主机设备的 CSId 和 ID，更新从机设备的 hostDeviceId 和 CSId；
     * 3. 任务通过多线程方式异步执行，并记录处理过程的日志。
     * <p>
     * 注意事项：
     * - 确保分页查询能够覆盖所有数据，避免漏处理。
     * - 设备表中的 simCode 字段值唯一，且非空。
     * - 确保 DeviceEntity 的线程安全性和正确的数据库连接管理。
     * - 为避免性能问题，考虑批量更新（当前为逐条更新）。
     */
    public void repairCSId() {
        // 记录任务启动日志
        LogsUtil.info("修复CSId", "启动任务...");

        // 异步执行任务
        ThreadPoolManager.getInstance().execute(() -> {
            int page = 1; // 起始页码
            int limit = 100; // 每页数据量

            while (true) {
                // 分页查询主机设备数据
                List<Map<String, Object>> list = DeviceEntity.getInstance()
                        .field("id,deviceCode,CSId,simCode") // 查询字段：设备ID、设备代码、CSId、simCode
                        .where("isHost", 1) // 筛选条件：isHost=1 表示主机设备
                        .page(page, limit) // 分页条件：指定页码和每页数据量
                        .order("id") // 按 ID 升序排序
                        .select(); // 执行查询

                // 如果查询结果为空或没有数据，则退出循环
                if (list == null || list.isEmpty()) break;

                // 记录当前页修复日志
                LogsUtil.info("修复CSId", "正在修复[%s,%s]...", page, limit);

                // 遍历当前页数据进行更新
                for (Map<String, Object> data : list) {
                    long id = MapUtil.getLong(data, "id"); // 获取主机设备的 ID
                    String deviceCode = MapUtil.getString(data, "deviceCode"); // 获取设备代码
                    String CSId = MapUtil.getString(data, "CSId"); // 获取主机设备的 CSId
                    String simCode = MapUtil.getString(data, "simCode"); // 获取主机设备的 SIM 码

                    // 日志记录：当前正在修复的设备信息
                    LogsUtil.info("修复CSId", "正在修复[%s]的从机数据...", deviceCode);

                    // 更新从机设备的主机 ID 和 CSId 信息
                    DeviceEntity.getInstance()
                            .where("simCode", simCode) // 条件：匹配从机设备的 simCode
                            .update(new LinkedHashMap<>() {{ // 更新字段
                                put("hostDeviceId", id); // 设置主机设备 ID
                                put("CSId", CSId); // 设置主机设备 CSId
                            }});
                }
                // 处理完成当前页后，处理下一页
                page++;
            }
        });
    }
}
