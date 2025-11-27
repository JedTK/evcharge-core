package com.evcharge.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceUnitEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.libsdk.ys7.YSSDK;
import com.evcharge.service.GeneralDevice.GeneralDeviceConfigService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 萤石云平台 - 监控设备 - 数据同步任务
 */
public class YSMonitorDeviceSyncTaskJob implements Job {
    protected final static String TAG = "萤石云平台监控设备数据同步";
    private final static String mTaskName = YSMonitorDeviceSyncTaskJob.class.getSimpleName();
    private final static String mGroupName = "YS_NVR";

    private static YSMonitorDeviceSyncTaskJob _this;

    public static YSMonitorDeviceSyncTaskJob getInstance() {
        if (_this == null) _this = new YSMonitorDeviceSyncTaskJob();
        return _this;
    }

    /**
     * 初始化任务
     */
    public SyncResult init() {
        String cron = "0 0 12,17,22 * * ? *";
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();

        //添加任务到调度器
        SyncResult r = QuartzSchedulerManager.getInstance().add(mTaskName
                , mGroupName
                , trigger
                , this.getClass()
                , null);

        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
        return r;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        execute();
    }

    public void execute() {
        LogsUtil.info(TAG, " - 开始执行 - ");

        String appKey = SysGlobalConfigEntity.getString("YSS:AppKey");
        String appSecret = SysGlobalConfigEntity.getString("YSS:AppSecret");
        if (!StringUtils.hasLength(appKey)) {
            LogsUtil.warn(TAG, "请配置萤石云开放平台 AppKey");
            return;
        }
        if (!StringUtils.hasLength(appSecret)) {
            LogsUtil.warn(TAG, "请配置萤石云开放平台 AppSecret");
            return;
        }
        YSSDK yssdk = new YSSDK()
                .setAppKey(appKey)
                .setAppSecret(appSecret);

        int pageStart = 0;
        int pageSize = 50;
        int pages;

        do {
            SyncResult r = yssdk.deviceList(pageStart, pageSize);
            if (r.code != 0) break;

            Map<String, Object> data = (Map<String, Object>) r.data;
            JSONObject page = (JSONObject) data.get("page");
            JSONArray list = (JSONArray) data.get("data");

            int totalCount = page.getIntValue("total", 0);
            pages = (int) Math.ceil(totalCount * 1.0 / pageSize);
            pageStart++;

            String organize_code_defaultValue = SysGlobalConfigEntity.getString("System:EvPlatform:Code");

            for (Object o : list) {
                JSONObject json = (JSONObject) o;
                String deviceSerial = json.getString("deviceSerial");
                if (!StringUtils.hasLength(deviceSerial)) continue;

                String deviceName = StringUtil.removeLineBreaksAndSpaces(json.getString("deviceName"));
                String deviceType = json.getString("deviceType");

                int status = json.getIntValue("status");//在线状态：0-不在线，1-在线

                // 根据型号查询设备单元信息
                DeviceUnitEntity deviceUnitEntity = DeviceUnitEntity.getInstance().getWithProductNumber(deviceType);
                if (deviceUnitEntity == null) {
                    LogsUtil.warn(TAG, "检测到有新的监控设备，但是设备单元没有入库，未知设备具体信息：%s", deviceType);
                    continue;
                }
                JSONArray unitConfig = JSONArray.parse(deviceUnitEntity.config);
                if (unitConfig == null) unitConfig = new JSONArray();

                // 根据设备名尝试查询对应的充电站数据
                ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance()
                        .whereLike("name", String.format("%%%s%%", deviceName))
                        .findEntity();
                String CSName = "";
                String CSId = "0";
                if (chargeStationEntity != null) {
                    CSName = chargeStationEntity.name;
                    CSId = chargeStationEntity.CSId;
                }

                // 查询通用设备表
                GeneralDeviceEntity generalDeviceEntity = GeneralDeviceEntity.getInstance()
                        .where("serialNumber", deviceSerial)
                        .where("spuCode", deviceUnitEntity.spuCode)
                        .findEntity();
                if (generalDeviceEntity == null || generalDeviceEntity.id == 0) {
                    //region 新增设备数据
                    generalDeviceEntity = new GeneralDeviceEntity();
                    generalDeviceEntity.serialNumber = deviceSerial;
                    generalDeviceEntity.CSId = CSId;
                    generalDeviceEntity.deviceName = deviceName;
                    generalDeviceEntity.spuCode = deviceUnitEntity.spuCode;
                    generalDeviceEntity.brandCode = deviceUnitEntity.brandCode;
                    generalDeviceEntity.typeCode = deviceUnitEntity.typeCode;
                    generalDeviceEntity.simCode = yssdk.getDeviceSimCode(deviceSerial);
                    generalDeviceEntity.online_status = status;//设备在线状态，1-在线；0-离线
                    generalDeviceEntity.status = 1;
                    generalDeviceEntity.dynamic_info = new JSONObject() {{
                        put("IP", JsonUtil.getString(json, "netAddress"));
                    }}.toJSONString();

                    int riskLevel = JsonUtil.getInt(json, "riskLevel");
                    generalDeviceEntity.spec = new JSONObject() {{
                        put("固件版本号", JsonUtil.getString(json, "deviceVersion"));
                        put("安全等级", riskLevel == 0 ? "安全" : String.format("分险系数（%s）", riskLevel));
                    }}.toJSONString();
                    generalDeviceEntity.organize_code = organize_code_defaultValue;
                    generalDeviceEntity.create_time = JsonUtil.getLong(json, "addTime");
                    generalDeviceEntity.update_time = TimeUtil.getTimestamp();
                    generalDeviceEntity.insert();

                    GeneralDeviceConfigService.getInstance().setConfig(generalDeviceEntity.serialNumber, unitConfig);

                    LogsUtil.info(TAG, "新增监控摄像机设备 - %s %s %s", deviceSerial, deviceName, deviceType);
                    //endregion
                    continue;
                }

                Map<String, Object> set_data = new LinkedHashMap<>();
                set_data.put("online_status", status);////在线状态：0-不在线，1-在线
                set_data.put("update_time", TimeUtil.getTimestamp());

                if (!StringUtil.isEmpty(generalDeviceEntity.CSId) && !"0".equals(generalDeviceEntity.CSId)) {
                    chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(generalDeviceEntity.CSId);
                    if (chargeStationEntity != null) {
                        CSName = StringUtil.removeLineBreaksAndSpaces(chargeStationEntity.name);
                        set_data.put("deviceName", CSName);
                    }
                    if (StringUtils.hasLength(CSName) && !CSName.equalsIgnoreCase(deviceName)) {
                        //进行同步数据
                        yssdk.deviceUpdateName(deviceSerial, CSName);
                    }
                } else if (!StringUtil.isEmpty(CSName)) {
                    set_data.put("deviceName", CSName);
                    set_data.put("CSId", CSId);
                    if (!CSName.equalsIgnoreCase(deviceName)) yssdk.deviceUpdateName(deviceSerial, CSName); //进行同步数据
                }

                String simCode = yssdk.getDeviceSimCode(deviceSerial);
                set_data.put("simCode", simCode);

                int riskLevel = JsonUtil.getInt(json, "riskLevel");
                JSONObject spec = JSONObject.parse(generalDeviceEntity.spec);
                if (spec == null) spec = new JSONObject();
                spec.put("固件版本号", JsonUtil.getString(json, "deviceVersion"));
                spec.put("安全等级", riskLevel == 0 ? "安全" : String.format("分险系数（%s）", riskLevel));
                set_data.put("spec", spec.toJSONString());

                JSONObject dynamic_info = JSONObject.parse(generalDeviceEntity.dynamic_info);
                if (dynamic_info == null) dynamic_info = new JSONObject();
                dynamic_info.put("IP", JsonUtil.getString(json, "netAddress"));
                set_data.put("dynamic_info", dynamic_info.toJSONString());

                JSONArray config = GeneralDeviceConfigService.getInstance().getJSONArray(generalDeviceEntity.serialNumber);
                if (config == null) config = unitConfig;

                GeneralDeviceConfigService.getInstance().setConfig(generalDeviceEntity.serialNumber, config);
                if (!deviceName.equalsIgnoreCase(CSName)) {
                    set_data.put("deviceName", deviceName);
                }
                if (!set_data.isEmpty()) generalDeviceEntity.where("serialNumber", deviceSerial).update(set_data);

                LogsUtil.info(TAG, "同步监控摄像机设备 - %s %s %s ===> [%s]%s", deviceSerial, deviceName, deviceType, CSId, CSName);
            }
            ThreadUtil.sleep(3000);
        } while (pageStart <= pages);

        LogsUtil.info(TAG, " - 执行结束 - ");
    }
}
