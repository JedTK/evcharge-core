package com.evcharge.entity.basedata;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * 占用费配置;
 *
 * @author : JED
 * @date : 2022-10-18
 */
public class ParkingConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 配置名
     */
    public String configName;
    /**
     * 充电停止N秒后开始收费，单位：秒
     */
    public long delayTime;
    /**
     * 备注
     */
    public String remark;
    /**
     * 拥有者id
     */
    public long owner_id;
    /**
     * 组织id
     */
    public long organize_id;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ParkingConfigEntity getInstance() {
        return new ParkingConfigEntity();
    }

    /**
     * 一天的毫秒数
     */

    public static final long ONE_DAY_MILLIS = 1 * 24 * 60 * 60 * 1000;

    /**
     * 一小时的毫秒数
     */

    public static final long ONE_HOUR_MILLIS = 1 * 60 * 60 * 1000;

    /**
     * 根据占用费配置，从开始时间到结束时间计算占用费用
     * <p>
     * 思路：
     * 如果占用费时间不足1小时，就算跨了时间段也按照一小时计算收费
     * 如果占用费时间大于1小时以上，按时间实际收费
     * 如果占用费时间大于1日以上，应该优化算法：
     * 1、通过开始时间戳和结束时间戳计算出一共停了多少分钟
     * 2、根据开始时间戳和结束时间戳计算出期间有多少分钟免费占用费
     * 3、总占用费分钟数 - 免费占用费分钟数 = 实际收费占用费分钟数
     *
     * @param config_id  占用费配置id
     * @param start_time 占用费开始时间戳
     * @param end_time   占用费结束时间戳
     * @return
     */
    public double getFee(long config_id, long start_time, long end_time) {
        double totalAmount = 0.0;

        List<ParkingItemEntity> parkingItemEntityList = ParkingItemEntity.getInstance()
                .cache(String.format("ParkingConfig:%s:Items", config_id))
                .where("configId", config_id)
                .page(1, 10)
                .order("id")
                .selectList();
        if (parkingItemEntityList.size() == 0) return 0.0;

        ParkingItemEntity parkingItemEntity = null;
        long checkTime = start_time;

        Calendar end_timeCal = Calendar.getInstance();
        end_timeCal.setTime(new Date(end_time));

        try {
            while (checkTime <= end_time) {
                System.out.println(String.format("\r\r开始计费时间：%s", TimeUtil.toTimeString(checkTime)));

                //算出此时落在哪个收费时段
                if (parkingItemEntity == null || !TimeUtil.isBelongPeriodTime(checkTime, parkingItemEntity.startDate, parkingItemEntity.endDate)) {
                    parkingItemEntity = null;
                    Iterator it = parkingItemEntityList.iterator();
                    while (it.hasNext()) {
                        ParkingItemEntity nd = (ParkingItemEntity) it.next();
                        if (!TimeUtil.isBelongPeriodTime(checkTime, nd.startDate, nd.endDate)) continue;
                        parkingItemEntity = nd;
                        break;
                    }
                }
                if (parkingItemEntity == null) continue;

                Calendar checkTimeCal = Calendar.getInstance();
                checkTimeCal.setTime(new Date(checkTime));

                //获取计费结束时段时钟
                Calendar endDateCal = parkingItemEntity.getCalendar(parkingItemEntity.endDate);
                //计算下一时段收费时钟
                Calendar nextCal = Calendar.getInstance();
                nextCal.setTime(new Date(checkTime));
                nextCal.set(Calendar.HOUR_OF_DAY, endDateCal.get(Calendar.HOUR_OF_DAY));
                nextCal.set(Calendar.MINUTE, endDateCal.get(Calendar.MINUTE));
                nextCal.set(Calendar.SECOND, endDateCal.get(Calendar.SECOND));
                //如果下一时段的时间早于检查时间，证明是跨日了
                if (nextCal.before(checkTimeCal)) nextCal.add(Calendar.DATE, 1);
                //如果占用费结束时间早于下一时段，证明在此期间结束了
                if (end_timeCal.before(nextCal)) nextCal = end_timeCal;

                long nextTime = nextCal.getTime().getTime();
                System.out.println(String.format("\r\r结束计费时间：%s", TimeUtil.toTimeString(nextTime)));

                int hourInterval = TimeUtil.convertToHourFull((nextTime - checkTime) / 1000);
                double fee = hourInterval * parkingItemEntity.price;

                System.out.println(String.format("\r\r计费时段结算：%s - %s %s小时 应收费为：%s元"
                        , TimeUtil.toTimeString(checkTimeCal)
                        , TimeUtil.toTimeString(nextCal)
                        , hourInterval
                        , fee));

                totalAmount += fee;

                checkTime = nextTime + 1000;
            }
        } catch (Exception e) {
            LogsUtil.error(e, "", "");
        }

        System.out.println(String.format("总结：%s - %s 总共停了%s小时 应收费为：%s元 \r\n"
                , TimeUtil.toTimeString(start_time)
                , TimeUtil.toTimeString(end_time)
                , TimeUtil.convertToHourFull((end_time - start_time) / 1000)
                , totalAmount));

        return totalAmount;
    }

    public static void test() {
        String startString = "";
        String endString = "";
        long start_time = 0;
        long end_time = 0;

        startString = "2022-10-31 9:45:00";
        endString = "2022-10-31 10:00:00";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);

        startString = "2022-10-31 9:45:00";
        endString = "2022-10-31 10:00:01";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);

        startString = "2022-10-31 9:45:00";
        endString = "2022-10-31 10:45:01";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);


        startString = "2022-10-31 9:45:00";
        endString = "2022-10-31 20:00:00";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);


        startString = "2022-10-31 9:45:00";
        endString = "2022-10-31 22:00:00";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);


        startString = "2022-10-31 9:45:00";
        endString = "2022-10-31 23:00:00";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);

        startString = "2022-10-31 9:45:00";
        endString = "2022-11-1 5:00:00";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);


        startString = "2022-10-31 9:45:00";
        endString = "2022-11-1 9:45:00";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);

        startString = "2022-10-31 9:45:00";
        endString = "2022-11-2 23:15:00";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);


        startString = "2022-10-31 9:45:00";
        endString = "2022-11-3 8:15:00";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);

        startString = "2022-09-1 9:45:00";
        endString = "2022-09-2 8:15:00";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);

        startString = "2022-09-1 23:00:00";
        endString = "2022-09-3 2:00:00";
        start_time = TimeUtil.toTimestamp(startString);
        end_time = TimeUtil.toTimestamp(endString);
        ParkingConfigEntity.getInstance().getFee(1, start_time, end_time);
    }
}
