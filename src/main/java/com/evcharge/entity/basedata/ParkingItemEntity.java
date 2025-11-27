package com.evcharge.entity.basedata;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 占用费配置项;
 *
 * @author : JED
 * @date : 2022-10-18
 */
public class ParkingItemEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 配置ID
     */
    public long configId;
    /**
     * 项名
     */
    public String ItemName;
    /**
     * 开始时间(hh:mm)
     */
    public String startDate;
    /**
     * 结束时间(hh:mm)
     */
    public String endDate;
    /**
     * 价格，单位：元，元/每小时
     */
    public double price;
    /**
     * 调整价格最小值
     */
    public double minPrice;
    /**
     * 调整价格最大值
     */
    public double maxPrice;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ParkingItemEntity getInstance() {
        return new ParkingItemEntity();
    }

    /**
     * 根据时间格式获取时钟（获取到的时钟时间不是当天的）只是用于计算小时
     *
     * @param dateString
     * @return
     * @throws ParseException
     */
    public Calendar getCalendar(String dateString) throws ParseException {
        //设置好开始和结束时间的格式
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        //初始化：开始时间的日历
        Calendar cal = Calendar.getInstance();
        Date date = df.parse(dateString);
        cal.setTime(date);
        return cal;
    }
}
