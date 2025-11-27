package com.evcharge.service.GeneralDevice.Meter;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.meter.SmartMeterRecordEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;

import java.math.BigDecimal;

/**
 * 智能电表抄表记录 - 业务逻辑;
 *
 * @author : JED
 * @date : 2025-4-16
 */
public class SmartMeterRecordService {

    private final static String TAG = "智能电表抄表业务逻辑";

    public static SmartMeterRecordService getInstance() {
        return new SmartMeterRecordService();
    }

    /**
     * 新增抄表记录
     *
     * @param meter_no            电表编号
     * @param record_time         抄表时间戳（毫秒）
     * @param total_active_energy 总有功电能（kWh）
     * @param remark              备注
     * @param extra_data          额外参数
     * @return 新增成功同步结果
     */
    public ISyncResult add(String meter_no, long record_time, BigDecimal total_active_energy, String remark, JSONObject extra_data) {
        // 减少抄表插入数据间隔过快
        int count = DataService.getMainCache().getInt(String.format("TEMP:SmartMeterRecord:%s", meter_no), 0);
        if (count > 0) return new SyncResult(3, "请勿抄表过快，稍后再试");

        SmartMeterRecordEntity entity = new SmartMeterRecordEntity();
        entity.meter_no = meter_no;
        entity.record_time = record_time;
        entity.total_active_energy = total_active_energy;
        entity.remark = remark;
        if (extra_data != null) entity.extra_data = extra_data.toJSONString();
        else entity.extra_data = "";
        entity.create_time = TimeUtil.getTimestamp();

        // 读取上一次抄表数据
        SmartMeterRecordEntity lastEntity = SmartMeterRecordEntity.getInstance()
                .where("meter_no", meter_no)
                .where("record_time", "<", record_time)
                .order("record_time DESC")
                .findEntity();
        if (lastEntity != null) {
            // 计算：期间用电量，当前值 - 上一值
            entity.consumption_interval = total_active_energy.subtract(lastEntity.total_active_energy);
            entity.last_record_time = lastEntity.last_record_time;
        } else {
            entity.consumption_interval = total_active_energy;
            entity.last_record_time = 0;
        }

        int noquery = entity.insert();
        if (noquery > 0) {
            count++;
            DataService.getMainCache().set(String.format("TEMP:SmartMeterRecord:%s", meter_no), count, ECacheTime.MINUTE);

            return new SyncResult(0, "");
        }
        return new SyncResult(1, "");
    }
}
