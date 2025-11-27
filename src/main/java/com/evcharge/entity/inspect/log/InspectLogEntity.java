package com.evcharge.entity.inspect.log;


import com.evcharge.entity.inspect.EInspectCycle;
import com.evcharge.entity.inspect.station.ChargeStationEntity;
import com.evcharge.utils.DateUtils;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import com.evcharge.entity.inspect.EInspectType;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 巡检日志表;
 *
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("inspect")
public class InspectLogEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 巡检人员id
     */
    public long admin_id;
    /**
     * 充电站ID
     */
    public String cs_uuid;
    /**
     * 使用的模板ID
     */
    public long template_id;
    /**
     * 巡检周期 周/月/季度/年 等
     */
    public String inspect_cycle;
    /**
     * 巡检日期 月份 2024-10 即将弃用
     */
    public String inspect_date;
    /**
     * 开始时间
     */
    public long start_time;
    /**
     * 结束时间
     */
    public long end_time;
    /**
     * 巡检日期 时间戳 真正巡检的日期
     */
    public long inspect_time;
    /**
     * 总体状态 0=未巡检 1=巡检中 2=巡检正常 3=巡检异常
     */
    public int overall_status;
    /**
     * 备注
     */
    public String remark;
    /**
     * 状态 0=未巡检 1=巡检中 2=巡检完成
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;


    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static InspectLogEntity getInstance() {
        return new InspectLogEntity();
    }

    /**
     * 创建巡检日志， 无需传入月份，默认创建下个月的巡检日志，比如当前月份是11月，调用该方法生成12月的巡检日志  DateUtils.getCurrentMonth()可获取当前月份
     * @param uuid 站点uuid
     * @param cycle 巡检周期 目前默认月度巡检
     */
//    public void createInspectLog(String uuid,String cycle){
//
//        createInspectLog(uuid,cycle, DateUtils.getCurrentMonth()+1);
//    }

    /**
     * 创建巡检日志，  DateUtils.getCurrentMonth()可获取当前月份
     * @param uuid 站点uuid
     * @param cycle 巡检周期 目前默认月度巡检
     * @param yearMonth 生成巡检日志月份
     */
    public void createInspectLog(String uuid,String cycle,String yearMonth){
        ChargeStationEntity chargeStation = ChargeStationEntity.getInstance();
        chargeStation.where("status", 1);
        chargeStation.where("uuid",uuid);

        ChargeStationEntity chargeStationEntity=chargeStation.findEntity();

        if (!StringUtils.hasLength(chargeStationEntity.uuid)) {
            return;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cs_uuid", chargeStationEntity.uuid);
        data.put("create_time", TimeUtil.getTimestamp());
        long[]  con=getStartEndTime(cycle,yearMonth);
        long startTime=con[0];
        long endTime=con[1];
        InspectTemplateEntity inspectTemplateEntity = InspectTemplateEntity.getInstance().getTempByAttr(chargeStationEntity.station_attr, EInspectType.Normal);

        if (inspectTemplateEntity == null) {
            LogsUtil.error(this.getClass().getName(), "创建日志失败，失败原因：模版不存在,cs_uuid=" + chargeStationEntity.uuid);
            return;
        }

        data.put("template_id", inspectTemplateEntity.id);
        data.put("inspect_date", yearMonth);
        data.put("start_time",startTime);
        data.put("end_time",endTime);
        data.put("inspect_cycle",cycle);
        if (this.where("cs_uuid", chargeStationEntity.uuid).where("start_time",startTime).where("end_time",endTime).count() == 0) {
            long logId = this.insertGetId(data);

            if (logId == 0) {
                return;
            }
            LogsUtil.info(this.getClass().getName(),String.format("%s创建巡检日志，时间为%s",uuid,yearMonth));
            List<InspectItemEntity> itemList = InspectItemEntity.getInstance().getItemByTempId(inspectTemplateEntity.id);

            if (itemList.isEmpty()) {
                return;
            }

            for (InspectItemEntity inspectItemEntity : itemList) {
                Map<String, Object> itemData = new LinkedHashMap<>();
                itemData.put("log_id", logId);
                itemData.put("item_id", inspectItemEntity.id);
                itemData.put("item_name", inspectItemEntity.item_name);
                itemData.put("create_time", TimeUtil.getTimestamp());
                InspectLogDetailEntity.getInstance().insert(itemData);
            }
        }



    }



    /**
     * 根据不同周期获取不同的开始和结束时间
     *
     * @param cycle 周期
     */
    public long[] getStartEndTime(String cycle,String yearMonth) {
        long currentTIme = TimeUtil.getTimestamp();
        long startTime = 0;
        long endTime = 0;
//        int currentMonth = DateUtils.getCurrentMonth();
//        int currentMonth = 10;
        switch (cycle) {
            case EInspectCycle.Month:
                startTime = DateUtils.getMonthStartTimestamp(yearMonth);
                endTime = DateUtils.getMonthEndTimestamp(yearMonth);
                break;

            case EInspectCycle.Quarter:
                //获取当前月份的第一天
                //获取三个月之后月份的最后一天 比如今天是9月15日，巡检开始时间应该是10月1日，三个月之后是12月，最后一天则是12月31日 23:59:59
//                int quarterEndMont = currentMonth + 2;
//                startTime = DateUtils.getMonthStartTimestamp(currentMonth);
//                endTime = DateUtils.getMonthEndTimestamp(quarterEndMont);
                break;

            case EInspectCycle.Year:
//                int yearEndMont = currentMonth + 11;
//                startTime = DateUtils.getMonthStartTimestamp(currentMonth);
//                endTime = DateUtils.getMonthEndTimestamp(yearEndMont);
                break;
            default:
                break;
        }


        return new long[]{startTime, endTime};
    }


}