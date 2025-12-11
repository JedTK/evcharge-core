package com.evcharge.service.ChargeStation;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.RSProfit.RSProfitIncomeLogsEntity;
import com.evcharge.entity.admin.AdminBaseEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.station.*;
import com.evcharge.entity.sys.SysMessageEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.enumdata.ENotifyType;
import com.evcharge.mqtt.XMQTTFactory;
import com.evcharge.service.meter.TQ4GMeterService;
import com.evcharge.service.notify.NotifyService;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 充电桩-业务逻辑层; 慢慢转移到这里
 *
 * @author : JED
 * @date : 2024-11-18
 */
public class ChargeStationService {

    private final static String TAG = "站点业务";

    /**
     * 获得一个实例
     */
    public static ChargeStationService getInstance() {
        return new ChargeStationService();
    }

    /**
     * 获取设备使用情况
     *
     * @param CSId 站点编码
     */
    public ISyncResult getDeviceUsage(String CSId) {
        Map<String, Object> data = DataService.getMainCache().getMap(String.format("ChargeStation:DeviceUsage:%s", CSId));
        if (data != null && !data.isEmpty()) return new SyncResult(0, "", data);

        int total_count = DeviceSocketEntity.getInstance()
                .alias("ds")
                .join(DeviceEntity.getInstance().theTableName(), "d", "d.id = ds.deviceId")
                .join(ChargeStationEntity.getInstance().theTableName(), "cs", "d.CSId = cs.CSId")
                .where("cs.CSId", CSId)
                .count();

        int idle_count = DeviceSocketEntity.getInstance()
                .alias("ds")
                .join(DeviceEntity.getInstance().theTableName(), "d", "d.id = ds.deviceId")
                .join(ChargeStationEntity.getInstance().theTableName(), "cs", "d.CSId = cs.CSId")
                .where("cs.CSId", CSId)
                .where("ds.status", 0)
                .count();

        int use_count = DeviceSocketEntity.getInstance()
                .alias("ds")
                .join(DeviceEntity.getInstance().theTableName(), "d", "d.id = ds.deviceId")
                .join(ChargeStationEntity.getInstance().theTableName(), "cs", "d.CSId = cs.CSId")
                .where("cs.CSId", CSId)
                .whereIn("ds.status", "1,2")
                .count();

        int full_count = DeviceSocketEntity.getInstance()
                .alias("ds")
                .join(DeviceEntity.getInstance().theTableName(), "d", "d.id = ds.deviceId")
                .join(ChargeStationEntity.getInstance().theTableName(), "cs", "d.CSId = cs.CSId")
                .where("cs.CSId", CSId)
                .where("ds.status", 3)
                .count();

        data = new LinkedHashMap<>();
        data.put("total_count", total_count);
        data.put("idle_count", idle_count);
        data.put("use_count", use_count);
        data.put("full_count", full_count);
        DataService.getMainCache().setMap(String.format("ChargeStation:DeviceUsage:%s", CSId), data, ECacheTime.MINUTE * 10);

        return new SyncResult(0, "", data);
    }

    /**
     * 获取站点名
     *
     * @param CSId 站点编码
     * @return 站点名
     */
    public String getNameByCSId(String CSId) {
        ChargeStationEntity entity = ChargeStationEntity.getInstance().getWithCSId(CSId);
        if (entity == null) return "";
        return entity.name;
    }

    /**
     * 停止充电
     * <p>
     * 根据筛选条件（区域/结构/站点）分页查询当前处于充电中的订单，
     * 逐条下发停止充电命令，并推送系统通知和站内消息。
     *
     * @param filter  筛选条件（包括区域、结构、站点编码等）
     * @param title   站内通知标题（可为空）
     * @param message 站内通知内容（可为空）
     * @return 停止充电操作的统计结果（成功数、失败数）
     */
    public ISyncResult stopCharge(SiteControlFilter filter, String title, String message) {
        try {
            // 分页参数
            int page = 1;
            int rows = 100;

            // 记录成功和失败数量
            int successCount = 0;
            int errorCount = 0;

            // 构造用于日志与通知的筛选文本
            String area_text = filter.toAreaText();
            String arch_text = filter.toArchText();
            String cs_text = filter.toCSIdsText();

            LogsUtil.info(TAG, "停止充电操作准备 - 区域：%s - 结构：%s - 站点：%s"
                    , area_text
                    , arch_text
                    , cs_text);

            while (true) {
                // 构造查询实体
                ChargeOrderEntity entity = ChargeOrderEntity.getInstance();
                entity.field("co.id,uid,deviceCode,port,ChargeMode,OrderSN,co.CSId,cs.name")
                        .alias("co")
                        .join(ChargeStationEntity.getInstance().theTableName(), "cs", "co.CSId = cs.CSId");

                // 添加筛选条件
                if (filter.cs_ids != null && filter.cs_ids.length > 0) {
                    // 精确指定站点
                    entity.whereIn("cs.CSId", filter.cs_ids);
                } else {
                    // 区域条件筛选
                    if (StringUtil.hasText(filter.communities)) entity.where("cs.communities", filter.communities);
                    if (StringUtil.hasText(filter.street)) entity.where("cs.street", filter.street);
                    if (StringUtil.hasText(filter.district)) entity.where("cs.district", filter.district);
                    if (StringUtil.hasText(filter.city)) entity.where("cs.city", filter.city);
                    if (StringUtil.hasText(filter.province)) entity.where("cs.province", filter.province);
                    // 结构条件筛选
                    if (filter.arch != null && filter.arch.length > 0) entity.whereIn("cs.arch", filter.arch);
                }

                // 查询当前页数据，仅选取状态为“充电中”（status = 1）的订单
                List<Map<String, Object>> list = entity
                        .where("co.status", 1)
                        .page(page, rows)
                        .select();

                // 若本页无数据，结束循环，返回统计结果
                if (list == null || list.isEmpty()) {
                    Map<String, Object> cb_data = new LinkedHashMap<>();
                    cb_data.put("success", successCount);
                    cb_data.put("error", errorCount);
                    return new SyncResult(0, "", cb_data);
                }

                LogsUtil.info(TAG, "停止充电操作(%s) - 区域：%s - 结构：%s - 站点：%s"
                        , page
                        , area_text
                        , arch_text
                        , cs_text);

                page++; // 继续下一页

                for (Map<String, Object> nd : list) {
                    long uid = MapUtil.getLong(nd, "uid"); // 用户ID
                    String deviceCode = MapUtil.getString(nd, "deviceCode"); // 设备编码

                    // 构建停止充电命令的透传数据
                    JSONObject json = new JSONObject();
                    json.put("deviceCode", deviceCode);
                    json.put("port", MapUtil.getInt(nd, "port"));
                    json.put("ChargeMode", MapUtil.getInt(nd, "ChargeMode"));
                    json.put("OrderSN", MapUtil.getString(nd, "OrderSN"));

                    // 发送站内通知（若有配置）
                    if (StringUtils.hasLength(title) && StringUtils.hasLength(message)) {
                        SysMessageEntity.getInstance().sendSysNotice(uid, title, message);
                    }

                    // 获取设备信息（用于获取 MQTT 通道）
                    DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);
                    if (deviceEntity == null) {
                        errorCount++; // 无法找到设备，视为失败
                        continue;
                    }

                    // MQTT 下发停止充电命令
                    XMQTTFactory.getInstance().publish(
                            String.format("%s/%s/command/stopCharge", deviceEntity.appChannelCode, deviceCode),
                            json
                    );
                    successCount++;
                }

                // region 新版通知系统（2024-10-10新增）
                JSONObject notifyTransData = new JSONObject();
                notifyTransData.put("title", title);
                notifyTransData.put("message", message);
                notifyTransData.put("time", TimeUtil.toTimeString()); // 当前时间
                notifyTransData.put("area", area_text);
                notifyTransData.put("arch", arch_text);
                notifyTransData.put("cs_ids", cs_text);
                notifyTransData.put("success_count", successCount);
                notifyTransData.put("error_count", errorCount);

                NotifyService.getInstance().asyncPush(
                        "", // 若有需要可指定通知人或群组
                        "SYSTEM.CS.STOP.CHARGING",
                        ENotifyType.WECHATCORPBOT, // 微信企业群机器人通知
                        notifyTransData
                );
                // endregion
            }
        } catch (Exception e) {
            LogsUtil.error(e, "站点停止充电", "操作停止充电时发生错误");
        }

        // 出现异常，返回失败
        return new SyncResult(1, "");
    }

    /**
     * 控制有智能空开的站点通电/断电
     * <p>
     * 根据传入的筛选条件与目标开关状态，分页查找智能空开主机设备，
     * 逐个下发 MQTT 命令控制设备的“通电”或“断电”操作。
     *
     * @param filter     筛选条件，包括区域、结构、站点等
     * @param gateStatus 开关目标状态（0=断电，1=通电）
     * @return 操作结果统计
     */
    public ISyncResult dispatchBreakerSwitchTask(SiteControlFilter filter, int gateStatus) {
        try {
            int page = 1; // 当前页码
            int rows = 50; // 每页处理设备数

            int successCount = 0; // 成功下发命令数量
            int errorCount = 0;   // 异常或失败数量

            // 文本化状态用于日志输出
            String gate_text = gateStatus == 0 ? "断电" : "通电";

            // 处理用于记录的筛选条件文本
            String area_text = filter.toAreaText();
            String arch_text = filter.toArchText();
            String cs_text = filter.toCSIdsText();

            LogsUtil.info(TAG, "智能空开主机[%s]准备 - 区域：%s - 结构：%s - 站点：%s"
                    , gate_text
                    , area_text
                    , arch_text
                    , cs_text);

            while (true) {
                // 获取设备实体对象，准备分页查询
                DeviceEntity entity = DeviceEntity.getInstance();
                entity.field("deviceCode,appChannelCode") // 只选必要字段
                        .alias("d")
                        .join(ChargeStationEntity.getInstance().theTableName(), "cs", "d.CSId = cs.CSId"); // 关联站点表用于筛选

                // 精确站点筛选
                if (filter.cs_ids != null && filter.cs_ids.length > 0) {
                    entity.whereIn("cs.CSId", filter.cs_ids);
                } else {
                    // 区域筛选（从细到粗）
                    if (StringUtil.hasText(filter.communities)) entity.where("cs.communities", filter.communities);
                    if (StringUtil.hasText(filter.street)) entity.where("cs.street", filter.street);
                    if (StringUtil.hasText(filter.district)) entity.where("cs.district", filter.district);
                    if (StringUtil.hasText(filter.city)) entity.where("cs.city", filter.city);
                    if (StringUtil.hasText(filter.province)) entity.where("cs.province", filter.province);
                    // 结构筛选
                    if (filter.arch != null && filter.arch.length > 0) entity.whereIn("cs.arch", filter.arch);
                }

                // 筛选智能空开主机（类型码为 SCBP）
                List<Map<String, Object>> list = entity
                        .where("typeCode", "SCBP")
                        .page(page, rows)
                        .select();

                // 若本页无数据，结束循环并返回结果
                if (list == null || list.isEmpty()) {
                    Map<String, Object> cb_data = new LinkedHashMap<>();
                    cb_data.put("success", successCount);
                    cb_data.put("error", errorCount);
                    return new SyncResult(0, "", cb_data);
                }

                LogsUtil.info(TAG, "智能空开主机[%s](%s) - 区域：%s - 结构：%s - 站点：%s"
                        , gate_text
                        , page
                        , area_text
                        , arch_text
                        , cs_text);

                page++; // 下一页

                for (Map<String, Object> nd : list) {
                    String deviceCode = MapUtil.getString(nd, "deviceCode");
                    String appChannelCode = MapUtil.getString(nd, "appChannelCode");

                    if (StringUtil.isEmpty(deviceCode) || StringUtil.isEmpty(appChannelCode)) {
                        errorCount++;
                        continue;
                    }

                    // region MQTT 控制命令下发
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("deviceCode", deviceCode);
                    requestBody.put("status", gateStatus); // 0=断电，1=通电

                    // 主题格式：{appChannelCode}/{deviceCode}/command/gateOp
                    XMQTTFactory.getInstance().publish(
                            String.format("%s/%s/command/gateOp", appChannelCode, deviceCode),
                            requestBody,
                            1 // QoS = 1：至少一次
                    );
                    // endregion

                    successCount++;

                    // 防止短时间内连续发送压力过大
                    ThreadUtil.sleep(100);
                }
            }
        } catch (Exception e) {
            LogsUtil.error(e, "站点智能空开", "操作通断电发生错误");
        }

        // 异常时返回失败结果
        return new SyncResult(1, "");
    }

    /**
     * 控制 有 4G电表 的站点通电/断电
     * <p>
     * 根据传入的筛选条件与目标开关状态，分页查找4G电表设备，
     * 逐个下发 MQTT 命令控制设备的“通电”或“断电”操作。
     *
     * @param filter     筛选条件，包括区域、结构、站点等
     * @param gateStatus 开关目标状态（0=断电，1=通电）
     * @return 操作结果统计
     */
    public ISyncResult dispatch4GMeterSwitchTask(SiteControlFilter filter, int gateStatus) {
        try {
            int page = 1; // 当前页码
            int rows = 50; // 每页处理设备数

            int successCount = 0; // 成功下发命令数量
            int errorCount = 0;   // 异常或失败数量

            // 文本化状态用于日志输出
            String gate_text = gateStatus == 0 ? "断电" : "通电";

            // 处理用于记录的筛选条件文本
            String area_text = filter.toAreaText();
            String arch_text = filter.toArchText();
            String cs_text = filter.toCSIdsText();

            LogsUtil.info(TAG, "智能空开主机[%s]准备 - 区域：%s - 结构：%s - 站点：%s"
                    , gate_text
                    , area_text
                    , arch_text
                    , cs_text);

            while (true) {
                // 获取设备实体对象，准备分页查询
                GeneralDeviceEntity entity = GeneralDeviceEntity.getInstance();
                entity.field("serialNumber,appChannelCode") // 只选必要字段
                        .alias("d")
                        .join(ChargeStationEntity.getInstance().theTableName(), "cs", "d.CSId = cs.CSId")// 关联站点表用于筛选
                        .where("typeCode", "4GEM")
                ;

                // 精确站点筛选
                if (filter.cs_ids != null && filter.cs_ids.length > 0) {
                    entity.whereIn("cs.CSId", filter.cs_ids);
                } else {
                    // 区域筛选（从细到粗）
                    if (StringUtil.hasText(filter.communities)) entity.where("cs.communities", filter.communities);
                    if (StringUtil.hasText(filter.street)) entity.where("cs.street", filter.street);
                    if (StringUtil.hasText(filter.district)) entity.where("cs.district", filter.district);
                    if (StringUtil.hasText(filter.city)) entity.where("cs.city", filter.city);
                    if (StringUtil.hasText(filter.province)) entity.where("cs.province", filter.province);
                    // 结构筛选
                    if (filter.arch != null && filter.arch.length > 0) entity.whereIn("cs.arch", filter.arch);
                }

                // 筛选智能空开主机（类型码为 SCBP）
                List<Map<String, Object>> list = entity
                        .where("typeCode", "SCBP")
                        .page(page, rows)
                        .select();

                // 若本页无数据，结束循环并返回结果
                if (list == null || list.isEmpty()) {
                    Map<String, Object> cb_data = new LinkedHashMap<>();
                    cb_data.put("success", successCount);
                    cb_data.put("error", errorCount);
                    return new SyncResult(0, "", cb_data);
                }

                LogsUtil.info(TAG, "智能空开主机[%s](%s) - 区域：%s - 结构：%s - 站点：%s"
                        , gate_text
                        , page
                        , area_text
                        , arch_text
                        , cs_text);

                page++; // 下一页

                for (Map<String, Object> nd : list) {
                    String serialNumber = MapUtil.getString(nd, "serialNumber");
                    String appChannelCode = MapUtil.getString(nd, "appChannelCode");

                    if (StringUtil.isEmpty(serialNumber) || StringUtil.isEmpty(appChannelCode)) {
                        errorCount++;
                        continue;
                    }

                    TQ4GMeterService.getInstance().sendMQTTGateSwitch(serialNumber, gateStatus, 1, appChannelCode);
                    successCount++;

                    // 防止短时间内连续发送压力过大
                    ThreadUtil.sleep(100);
                }
            }
        } catch (Exception e) {
            LogsUtil.error(e, "站点智能空开", "操作通断电发生错误");
        }

        // 异常时返回失败结果
        return new SyncResult(1, "");
    }

    /**
     * 设置充电桩智能空开主机 - 分/合 闸 状态
     *
     * @param CSIds      充电桩站点，如果存在多个用逗号分割
     * @param gateStatus 闸门状态：0-分闸，1-合闸
     */
    public SyncResult dispatchBreakerSwitchTask(String CSIds, int gateStatus) {
        return dispatchBreakerSwitchTask(StringUtil.splitFast(CSIds), gateStatus);
    }

    /**
     * 设置充电桩智能空开主机 - 分/合 闸 状态
     *
     * @param CSIds      充电桩id列表，多个以英文逗号分隔
     * @param gateStatus 闸门状态：0-分闸，1-合闸
     */
    public SyncResult dispatchBreakerSwitchTask(String[] CSIds, int gateStatus) {
        try {
            List<Map<String, Object>> list = DeviceEntity.getInstance()
                    .field("id,deviceCode,appChannelCode")
                    .whereIn("CSId", CSIds)
                    .where("typeCode", "SCBP") // 智能空开+主机 类型Code
                    .select();
            if (list == null || list.isEmpty()) return new SyncResult(10, "当前所操作充电桩无智能空开设备");

            for (Map<String, Object> nd : list) {
                String deviceCode = MapUtil.getString(nd, "deviceCode");
                String appChannelCode = MapUtil.getString(nd, "appChannelCode");

                if (!StringUtils.hasLength(deviceCode)) {
                    LogsUtil.warn(this.getClass().getSimpleName(), "设置充电桩智能空开主机闸门状态 - 设备缺少[deviceCode]");
                    continue;
                }

                //region 通过MQTT下发命令
                JSONObject requestBody = new JSONObject();
                requestBody.put("deviceCode", deviceCode);
                requestBody.put("status", gateStatus);
                XMQTTFactory.getInstance().publish(String.format("%s/%s/command/gateOp", appChannelCode, deviceCode), requestBody, 1);
                //endregion

                ThreadUtil.sleep(100);
            }
        } catch (Exception e) {
            LogsUtil.warn(this.getClass().getSimpleName(), "设置充电桩智能空开主机闸门状态 - 发生错误");
        }
        return new SyncResult(0, "");
    }

    /**
     * 导出站点数据、投建成本、每月的充值、消费金额、电费、分润等数据
     */
    public void exportFinanceData20241118(@NonNull String CSIds, long start_time, long end_time, String out_path) {
        String TAG = "财务数据v20241118-站点统计表";
        LogsUtil.info(TAG, "开始导出文件操作");
        // 创建 Excel 工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            // region 创建Sheet基本格式
            // 创建 Sheet
            Sheet sheet = workbook.createSheet("站点统计表");
            LogsUtil.info(TAG, "创建工作薄");

            // 创建第一行
            int firstRowNum = 0;
            // 表头行索引
            int headRowNum = firstRowNum + 1;

            // 创建样式：内容居中
            CellStyle centeredStyle = workbook.createCellStyle();
            centeredStyle.setAlignment(HorizontalAlignment.CENTER); // 水平居中
            centeredStyle.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直居中
            // 创建样式：内容居右
            CellStyle rightStyle = workbook.createCellStyle();
            rightStyle.setAlignment(HorizontalAlignment.RIGHT); // 水平居中
            rightStyle.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直居中

            // 处理第一行：一般都是合并内容
            Row firstRow = sheet.createRow(firstRowNum);

            // 创建基础信息合并行
            sheet.addMergedRegion(new CellRangeAddress(firstRowNum, firstRowNum, ExcelUtil.columnIndexByName("A"), ExcelUtil.columnIndexByName("I")));
            Cell basicInfoCell = firstRow.createCell(0);
            basicInfoCell.setCellValue("基础信息");
            basicInfoCell.setCellStyle(centeredStyle); // 应用样式
            LogsUtil.info(TAG, "基础信息列");

            // 设置表头行
            Row headRow = sheet.createRow(headRowNum);
            ExcelUtil.createCell(headRow, "A", "序号", centeredStyle);
            ExcelUtil.createCell(headRow, "B", "CSId", centeredStyle);
            ExcelUtil.createCell(headRow, "C", "商务", centeredStyle);
            ExcelUtil.createCell(headRow, "D", "商务联系电话", centeredStyle);
            ExcelUtil.createCell(headRow, "E", "站点名称", centeredStyle);
            ExcelUtil.createCell(headRow, "F", "地址", centeredStyle);
            ExcelUtil.createCell(headRow, "G", "直投自营/直管/代理*", centeredStyle);
            ExcelUtil.createCell(headRow, "H", "合作模式", centeredStyle);
            ExcelUtil.createCell(headRow, "I", "充电口", centeredStyle);
            ExcelUtil.createCell(headRow, "J", "上线时间", centeredStyle);
            ExcelUtil.createCell(headRow, "K", "是否正常运营", centeredStyle);
            ExcelUtil.createCell(headRow, "L", "结构", centeredStyle);
//            ExcelUtil.createCell(headRow, "K", "项目建安投入（基础+材料+设备+人工+当年保险+商务费用）", centeredStyle);

            // region 动态生成月份标题列
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(start_time);

            // 月份开始行索引
            int monthStartColIndex = ExcelUtil.columnIndexByName("M");
            int monthCellCount = 6;
            List<String> DATE_LIST = new LinkedList<>(); //用于插入月份数据，可以避免前面月份不存在而把数据往前顶

            int columnTempIndex = monthStartColIndex; // 临时单元索引值
            while (calendar.getTimeInMillis() <= end_time) {
                // 计算月份标题
                String title = sdf.format(calendar.getTime()) + "（单位：元）";
                LogsUtil.info(TAG, "创建%s", title);
                DATE_LIST.add(TimeUtil.toTimeString(calendar.getTime(), "yyyy-MM"));

                // 合并单元格
                sheet.addMergedRegion(new CellRangeAddress(firstRowNum, firstRowNum, columnTempIndex, columnTempIndex + monthCellCount - 1));
                Cell monthCell = firstRow.createCell(columnTempIndex);
                monthCell.setCellValue(title);
                monthCell.setCellStyle(centeredStyle);

                ExcelUtil.createCell(headRow, columnTempIndex, "时长使用率（APR）", centeredStyle);
                ExcelUtil.createCell(headRow, columnTempIndex + 1, "充值流水", centeredStyle);
                ExcelUtil.createCell(headRow, columnTempIndex + 2, "消费金额", centeredStyle);
                ExcelUtil.createCell(headRow, columnTempIndex + 3, "电费", centeredStyle);
                ExcelUtil.createCell(headRow, columnTempIndex + 4, "场地方分润", centeredStyle);
                ExcelUtil.createCell(headRow, columnTempIndex + 5, "渠道分润", centeredStyle);
                ExcelUtil.createCell(headRow, columnTempIndex + 6, "项目投资人分润", centeredStyle);

                // 设置下一个月
                calendar.add(Calendar.MONTH, 1);
                columnTempIndex += monthCellCount; // 每组占用6列
            }
            // endregion

            // endregion

            int page = 1;
            int limit = 1000;
            int currentRowNum = headRowNum;
            int index = 0; // 序号

            while (true) {
                ChargeStationEntity chargeStationEntity = new ChargeStationEntity();
                if (!CSIds.equalsIgnoreCase("ALL")) chargeStationEntity.whereIn("CSId", CSIds);
                List<Map<String, Object>> list = chargeStationEntity
                        .field("CSId,projectManagerAdminId,name,status,arch,totalSocket AS total_socket,online_time,op_mode_code,partner_type_code,province,city,district,street,communities,roads,address,is_private,is_restricted,isTest")
                        .whereIn("status", "0,1")
//                        .where("is_private", 0)
//                        .where("is_restricted", 0)
//                        .where("isTest", 0)
                        .page(page, limit)
                        .order("online_time")
                        .select();

                if (list == null || list.isEmpty()) break;
                page++;

                for (Map<String, Object> nd : list) {
                    index++;
                    currentRowNum++;
                    String CSId = MapUtil.getString(nd, "CSId");
                    Row row = sheet.createRow(currentRowNum);

                    String adminName = "";
                    String phoneNum = "";
                    long projectManagerAdminId = MapUtil.getLong(nd, "projectManagerAdminId");
                    AdminBaseEntity adminBaseEntity = AdminBaseEntity.getInstance().getWithId(projectManagerAdminId);
                    if (adminBaseEntity != null) {
                        adminName = adminBaseEntity.last_name + adminBaseEntity.first_name;
                        phoneNum = adminBaseEntity.phone_num;
                    }

                    //region 插入基础数据
                    String name = MapUtil.getString(nd, "name");
                    if (MapUtil.getBool(nd, "is_private")) {
                        name += "(私人)";
                    }
                    if (MapUtil.getBool(nd, "is_restricted")) {
                        name += "(限制使用)";
                    }
                    if (MapUtil.getBool(nd, "isTest")) {
                        name += "(测试)";
                    }

                    //运营模式
                    String opModeName = CSOperationModeEntity.getInstance().getModeName(MapUtil.getString(nd, "op_mode_code"));
                    //合作类型
                    String partnerTypeName = CSPartnerTypeEntity.getInstance().getTypeName(MapUtil.getString(nd, "partner_type_code"));


                    String address = String.format("%s%s%s%s%s%s%s"
                            , MapUtil.getString(nd, "province")
                            , MapUtil.getString(nd, "city")
                            , MapUtil.getString(nd, "district")
                            , MapUtil.getString(nd, "street")
                            , MapUtil.getString(nd, "communities")
                            , MapUtil.getString(nd, "roads")
                            , MapUtil.getString(nd, "address")
                    );
                    int status = MapUtil.getInt(nd, "status");
                    String status_text = "";
                    switch (status) {
                        case 0:
                            status_text = "拆除";
                            break;
                        case 1:
                            status_text = "运营中";
                            break;
                        case 2:
                            status_text = "建设中";
                            break;
                    }

                    int arch = MapUtil.getInt(nd, "arch");
                    String arch_text = "";
                    switch (arch) {
                        case 0:
                            arch_text = "无";
                            break;
                        case 1:
                            arch_text = "棚";
                            break;
                        case 2:
                            arch_text = "架";
                            break;
                        case 3:
                            arch_text = "墙";
                            break;
                        case 4:
                            arch_text = "柱";
                            break;
                        case 99:
                            arch_text = "其他";
                            break;
                    }

                    int total_socket = MapUtil.getInt(nd, "total_socket");

                    ExcelUtil.createCell(row, "A", index, centeredStyle);
                    ExcelUtil.createCell(row, "B", CSId, centeredStyle);
                    ExcelUtil.createCell(row, "C", adminName, centeredStyle);
                    ExcelUtil.createCell(row, "D", phoneNum);
                    ExcelUtil.createCell(row, "E", name);
                    ExcelUtil.createCell(row, "F", address);
                    ExcelUtil.createCell(row, "G", opModeName, centeredStyle);
                    ExcelUtil.createCell(row, "H", partnerTypeName, centeredStyle);
                    ExcelUtil.createCell(row, "I", total_socket, centeredStyle);
                    ExcelUtil.createCell(row, "J", TimeUtil.toTimeString(MapUtil.getLong(nd, "online_time")), centeredStyle);
                    ExcelUtil.createCell(row, "K", status_text, centeredStyle);
                    ExcelUtil.createCell(row, "L", arch_text, centeredStyle);

                    LogsUtil.info(TAG, "[%s] 创建行数据...", name);
                    //endregion

                    // 查询站点对应月份的数据：充值金额、消费金额、电费
                    List<Map<String, Object>> monthSummaryList = ChargeStationMonthSummaryV2Entity.getInstance()
                            .field("CSId,date,date_time"
                                    + ",charge_time_use_rate"
                                    + ",recharge_amount,recharge_refund_amount"
                                    + ",recharge_adjust_amount,recharge_refund_adjust_amount"
                                    + ",charge_card_amount,charge_card_refund_amount"
                                    + ",pay_per_charge_amount,pay_per_adjustment_charge_amount"
                                    + ",card_charge_amount,card_adjustment_charge_amount"
                                    + ",electricity_fee")
                            .where("CSId", CSId)
                            .where("date_time", ">=", start_time)
                            .where("date_time", "<=", end_time)
                            .order("date_time")
                            .select();
                    // region 插入每月的：(%)时长使用率（APR）、充值金额、消费金额、电费
                    for (int i = 0; i < DATE_LIST.size(); i++) {
                        String date_str = DATE_LIST.get(i);
                        Map<String, Object> map = null;
                        for (Map<String, Object> summary : monthSummaryList) {
                            String date = MapUtil.getString(summary, "date");
                            if (date.equalsIgnoreCase(date_str)) {
                                map = summary;
                                break;
                            }
                        }
                        if (map == null || map.isEmpty()) continue;

                        long date_time = MapUtil.getLong(map, "date_time");

                        // 插入 (%)时长使用率（APR）
                        ExcelUtil.createCell(row, monthStartColIndex + i * monthCellCount
                                , String.format("%s%%", MapUtil.getBigDecimal(map, "charge_time_use_rate")
                                        .multiply(new BigDecimal(100))
                                        .setScale(4, RoundingMode.HALF_UP)
                                        .doubleValue())
                                , rightStyle);

                        // region 插入充值流水
                        BigDecimal recharge_amount = MapUtil.getBigDecimal(map, "recharge_amount");
                        BigDecimal recharge_adjust_amount = MapUtil.getBigDecimal(map, "recharge_adjust_amount");
                        BigDecimal recharge_refund_amount = MapUtil.getBigDecimal(map, "recharge_refund_amount");
                        BigDecimal recharge_refund_adjust_amount = MapUtil.getBigDecimal(map, "recharge_refund_adjust_amount");
                        BigDecimal charge_card_amount = MapUtil.getBigDecimal(map, "charge_card_amount");
                        BigDecimal charge_card_refund_amount = MapUtil.getBigDecimal(map, "charge_card_refund_amount");

                        BigDecimal pay_per_charge_amount = MapUtil.getBigDecimal(map, "pay_per_charge_amount");
                        BigDecimal pay_per_adjustment_charge_amount = MapUtil.getBigDecimal(map, "pay_per_adjustment_charge_amount");
                        BigDecimal card_charge_amount = MapUtil.getBigDecimal(map, "card_charge_amount");
                        BigDecimal card_adjustment_charge_amount = MapUtil.getBigDecimal(map, "card_adjustment_charge_amount");

                        BigDecimal electricity_fee = MapUtil.getBigDecimal(map, "electricity_fee");

                        // 充值流水 = 充值金额 + 充值退款 + 充电卡金额 + 充电卡退款金额
                        ExcelUtil.createCell(row, monthStartColIndex + i * monthCellCount + 1
                                , recharge_amount
                                        .add(recharge_adjust_amount)
                                        .add(recharge_refund_amount)
                                        .add(recharge_refund_adjust_amount)
                                        .add(charge_card_amount)
                                        .add(charge_card_refund_amount)
                                        .setScale(4, RoundingMode.HALF_UP)
                                        .doubleValue()
                                , rightStyle);

                        // 消费金额 = 计次消费金额 - 计次消费调整金额 + 充电卡消费金额 - 充电卡消费调整金额
                        ExcelUtil.createCell(row, monthStartColIndex + i * monthCellCount + 2
                                , pay_per_charge_amount.subtract(pay_per_adjustment_charge_amount)
                                        .add(card_charge_amount)
                                        .subtract(card_adjustment_charge_amount)
                                        .setScale(4, RoundingMode.HALF_UP)
                                        .doubleValue()
                                , rightStyle);

                        // 电费
                        ExcelUtil.createCell(row, monthStartColIndex + i * monthCellCount + 3
                                , electricity_fee.setScale(4, RoundingMode.HALF_UP).doubleValue()
                                , rightStyle);

                        LogsUtil.info(TAG, "[%s] 插入充值流水、消费金额、电费", name);
                        // endregion

                        // 查询站点分润数据
                        List<Map<String, Object>> rsprofitRoleSumList = RSProfitIncomeLogsEntity.getInstance()
                                .field("channel_role, SUM(amount) AS amount")
                                .where("cs_id", CSId)
                                .where("date_time", date_time)
                                .group("channel_role")
                                .select();
                        for (Map<String, Object> rspData : rsprofitRoleSumList) {
                            BigDecimal amount = MapUtil.getBigDecimal(rspData, "amount");
                            int channel_role = MapUtil.getInt(rspData, "channel_role");

                            // 1-场地方，2-居间人，3-商务，4-物业，5-合作伙伴
                            switch (channel_role) {
                                case 1:
                                case 4:
                                    // 场地方分润
                                    ExcelUtil.createCell(row, monthStartColIndex + i * monthCellCount + 4, amount.setScale(4, RoundingMode.HALF_UP).doubleValue());
                                    break;
                                case 5:
                                    // 项目投资人分润
                                    ExcelUtil.createCell(row, monthStartColIndex + i * monthCellCount + 6, amount.setScale(4, RoundingMode.HALF_UP).doubleValue());
                                    break;
                                default:
                                    // 渠道分润
                                    ExcelUtil.createCell(row, monthStartColIndex + i * monthCellCount + 5, amount.setScale(4, RoundingMode.HALF_UP).doubleValue());
                                    break;
                            }

                            LogsUtil.info(TAG, "[%s] 插入分润数据", name);
                        }
                    }
                    // endregion
                }
            }

            // 在写入文件之前，设置列宽自适应
            // 设置整个表格最合适的列宽
            for (int colIndex = 0; colIndex <= sheet.getRow(headRowNum).getLastCellNum(); colIndex++) {
                // 自动调整列宽
                sheet.autoSizeColumn(colIndex);

                // 获取自动调整后的列宽值
                int currentWidth = sheet.getColumnWidth(colIndex);

                // 为了防止某些内容过长导致列宽太大，可以设置一个最大列宽限制（比如 10000）
                int maxWidth = 10000; // 最大列宽
                if (currentWidth > maxWidth) {
                    sheet.setColumnWidth(colIndex, maxWidth);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(out_path)) {
                workbook.write(fos);
                LogsUtil.info(TAG, "文件保存成功：%s", out_path);
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "导出财务20241118版本数据发生错误");
        }
    }

    /**
     * 导出站点数据及设备编码：站点地址明细，上线时间，充电口数量，设备编码
     */
    public void exportAllDeviceReport20250103(String out_path) {
        String TAG = "导出站点数据及设备编码";
        LogsUtil.info(TAG, "开始导出文件操作");

        // 创建 Excel 工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            // region 创建Sheet基本格式
            // 创建 Sheet
            Sheet sheet = workbook.createSheet("站点数据");

            // 表头行索引
            int headRowNum = 0;

            CellStyle centerCellStyle = ExcelUtil.getCenterCellStyle(workbook);
            CellStyle leftCellStyle = ExcelUtil.getLeftCellStyle(workbook);

            // 设置表头行
            Row headRow = sheet.createRow(headRowNum);
            ExcelUtil.createCell(headRow, "A", "序号", centerCellStyle);
            ExcelUtil.createCell(headRow, "B", "CSId", centerCellStyle);
            ExcelUtil.createCell(headRow, "C", "站点名", centerCellStyle);
            ExcelUtil.createCell(headRow, "D", "地址", centerCellStyle);
            ExcelUtil.createCell(headRow, "E", "端口数", centerCellStyle);
            ExcelUtil.createCell(headRow, "F", "上线时间", centerCellStyle);
            ExcelUtil.createCell(headRow, "G", "设备编码", centerCellStyle);
            // endregion

            int currentRowNum = headRowNum;
            List<Map<String, Object>> list = ChargeStationEntity.getInstance()
                    .field("CSId,name,online_time,province,city,district,street,communities,roads,address")
                    .where("status", 1)
                    .where("isTest", 0)
                    .where("is_private", 0)
                    .order("name")
                    .select();
            for (int i = 0; i < list.size(); i++) {
                currentRowNum = currentRowNum + 1;

                Map<String, Object> nd = list.get(i);
                String CSId = MapUtil.getString(nd, "CSId");
                String name = MapUtil.getString(nd, "name");

                LogsUtil.info(TAG, "[%s] 正在处理...", name);

                // 组装地址
                String address = String.format("%s%s%s%s%s%s%s"
                        , MapUtil.getString(nd, "province")
                        , MapUtil.getString(nd, "city")
                        , MapUtil.getString(nd, "district")
                        , MapUtil.getString(nd, "street")
                        , MapUtil.getString(nd, "communities")
                        , MapUtil.getString(nd, "roads")
                        , MapUtil.getString(nd, "address")
                );

                // 查询端口数
                int totalSocket = DeviceSocketEntity.getInstance()
                        .alias("ds")
                        .join(DeviceEntity.getInstance().theTableName(), "d", "ds.deviceId = d.id")
                        .where("d.CSId", CSId)
                        .where("d.isHost", 0)
                        .count();

                Row row = sheet.createRow(currentRowNum);
                ExcelUtil.createCell(row, "A", i + 1);
                ExcelUtil.createCell(row, "B", MapUtil.getString(nd, "CSId"), centerCellStyle);
                ExcelUtil.createCell(row, "C", name, leftCellStyle);
                ExcelUtil.createCell(row, "D", address, leftCellStyle);
                ExcelUtil.createCell(row, "E", totalSocket, centerCellStyle);
                ExcelUtil.createCell(row, "F", TimeUtil.toTimeString(MapUtil.getLong(nd, "online_time"), "yyyy-MM-dd"), centerCellStyle);

                // 查询设备编码
                StringBuilder device_str = new StringBuilder();
                List<Map<String, Object>> devices = DeviceEntity.getInstance()
                        .field("deviceNumber")
                        .where("CSId", CSId)
                        .where("isHost", 0)
                        .select();
                for (Map<String, Object> device : devices) {
                    device_str.append(String.format(",%s", MapUtil.getString(device, "deviceNumber")));
                }
                if (device_str.length() > 0) {
                    device_str = new StringBuilder(device_str.substring(1, device_str.length() - 1));
                }
                ExcelUtil.createCell(row, "G", String.format("`%s", device_str), leftCellStyle);
            }

            try (FileOutputStream fos = new FileOutputStream(out_path)) {
                workbook.write(fos);
                LogsUtil.info(TAG, "文件保存成功：%s", out_path);
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "导出财务20241118版本数据发生错误");
        }
    }

    /**
     * 绑定新主机
     * <p>
     * 业务语义：
     * 1. 每个站点只能有一个主机设备（isHost=1）。
     * 2. 调用该方法时，先解绑旧的主机，再把新的设备绑定到指定站点。
     * 3. 为保险起见，还会更新同一 simCode 下的从机设备，让它们也绑定到新主机所在站点。
     *
     * @param CSId          站点编号
     * @param device_number 主机设备物理编号（唯一标识设备）
     * @return ISyncResult  结果对象，包含状态码和提示信息
     */
    public ISyncResult bindNewHostDevice(String CSId, String device_number) {
        // 参数校验：必须传站点编号
        if (StringUtil.isEmpty(CSId)) return new SyncResult(2, "请选择站点");
        // 参数校验：必须传设备编号
        if (StringUtil.isEmpty(device_number)) return new SyncResult(2, "请输入设备物理编号");

        // 根据物理编号查询要绑定的新主机
        DeviceEntity newHost = DeviceEntity.getInstance()
                .where("deviceNumber", device_number)
                .findEntity();
        if (newHost == null || newHost.id == 0) {
            return new SyncResult(2, "设备物理编号不正确，无法找到设备数据");
        }

        // 幂等判断：如果新主机已经是该站点的主机，直接返回成功，避免重复绑定
        if (CSId.equalsIgnoreCase(newHost.CSId) && newHost.isHost == 1) {
            return new SyncResult(0, "已是当前站点主机，无需重复绑定");
        }

        // 查询该站点当前绑定的旧主机（isHost=1）
        DeviceEntity oldHostDeviceEntity = DeviceEntity.getInstance()
                .where("CSId", CSId)
                .where("isHost", 1)
                .findEntity();

        // 启动事务，确保解绑旧主机和绑定新主机是原子操作
        return DataService.getMainDB().beginTransaction(connection -> {
            // 1. 解绑旧主机：把 CSId 更新为 "0"
            if (oldHostDeviceEntity != null && oldHostDeviceEntity.id != 0) {
                DeviceEntity.getInstance()
                        .where("deviceNumber", oldHostDeviceEntity.deviceNumber)
                        .updateTransaction(connection, new HashMap<>() {{
                            put("CSId", "0");
                        }});
            }

            // 2. 绑定新主机：把它的 CSId 更新为目标站点
            int noquery = DeviceEntity.getInstance()
                    .where("deviceNumber", device_number)
                    .updateTransaction(connection, new HashMap<>() {{
                        put("CSId", CSId);
                    }});
            if (noquery == 0) return new SyncResult(3, "绑定新主机失败，请重试");

            // 3. 更新从机：同一个 simCode 的设备（主从关系）绑定到同一站点
            //    一般情况下从机会自动绑定到主机，但这里额外做一次兜底更新
            DeviceEntity.getInstance()
                    .where("simCode", newHost.simCode)
                    .updateTransaction(connection, new HashMap<>() {{
                        put("CSId", CSId);
                    }});

            // 成功返回
            return new SyncResult(0, "");
        });
    }
}
