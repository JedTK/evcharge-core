package com.evcharge.entity.station;


import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 充电桩不允许使用日期配置;
 *
 * @author : JED
 * @date : 2024-1-30
 */
public class ChargeStationNotAllowedUseDateConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电桩唯一编号，新增
     */
    public String CSId;
    /**
     * 公告标题
     */
    public String title;
    /**
     * 公告内容
     */
    public String content;
    /**
     * 开始时间
     */
    public long start_time;
    /**
     * 结束时间
     */
    public long end_time;
    /**
     * 状态：0-不启用，1-启用
     */
    public int status;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStationNotAllowedUseDateConfigEntity getInstance() {
        return new ChargeStationNotAllowedUseDateConfigEntity();
    }

    /**
     * 新增限制充电配置
     *
     * @return
     */
    public boolean add() {
        List<ChargeStationNotAllowedUseDateConfigEntity> list = DataService.getMainCache().getList(String.format("ChargeStationNotAllowedUseDateConfig:%s:List", this.CSId));
        if (list == null) list = new LinkedList<>();

        long nowTime = TimeUtil.getTimestamp();
        // 使用迭代器安全移除不符合条件的配置
        // 检查ID是否匹配以及时间范围是否覆盖当前时间
        // 不匹配或时间不符合则移除
        list.removeIf(nd -> !nd.CSId.equalsIgnoreCase(CSId) || nd.start_time >= nowTime || nd.end_time <= nowTime);
        //如果现在的时间已经超过了结束时间，则不需要添加进去
        if (this.end_time <= nowTime) return false;

        this.create_time = TimeUtil.getTimestamp();
        this.id = this.insertGetId();
        if (this.id > 0) {
            list.add(this); // 添加新配置
            DataService.getMainCache().setList(String.format("ChargeStationNotAllowedUseDateConfig:%s:List", this.CSId), list, ECacheTime.YEAR);
            return true;
        }
        return false;
    }

    /**
     * 移除配置
     *
     * @return
     */
    public boolean remove() {
        List<ChargeStationNotAllowedUseDateConfigEntity> list = DataService.getMainCache().getList(String.format("ChargeStationNotAllowedUseDateConfig:%s:List", this.CSId));
        if (list == null) list = new LinkedList<>();

        long nowTime = TimeUtil.getTimestamp();

        // 使用迭代器安全移除不符合条件的配置
        Iterator<ChargeStationNotAllowedUseDateConfigEntity> iterator = list.iterator();
        while (iterator.hasNext()) {
            ChargeStationNotAllowedUseDateConfigEntity nd = iterator.next();
            // 检查ID是否匹配以及时间范围是否覆盖当前时间
            if (!nd.CSId.equalsIgnoreCase(CSId) || nd.start_time >= nowTime || nd.end_time <= nowTime) {
                iterator.remove(); // 不匹配或时间不符合则移除
            }
            if (nd.CSId.equalsIgnoreCase(CSId) && nd.start_time == this.start_time && nd.end_time == this.end_time) {
                iterator.remove(); // 移除
            }
        }
        int noquery = this.where("id", this.id).del();
        if (noquery > 0) {
            DataService.getMainCache().setList(String.format("ChargeStationNotAllowedUseDateConfig:%s:List", this.CSId), list, ECacheTime.YEAR);
            return true;
        }
        return false;
    }

    /**
     * 通过站点id查询限制充电时间配置（只从缓存中读取数据）
     *
     * @param CSId
     * @return
     */
    public ChargeStationNotAllowedUseDateConfigEntity getConfigWithCSId(String CSId) {
        List<ChargeStationNotAllowedUseDateConfigEntity> list = DataService.getMainCache()
                .getList(String.format("ChargeStationNotAllowedUseDateConfig:%s:List", CSId));
        if (list == null || list.isEmpty()) return null;

        long nowTime = TimeUtil.getTimestamp();
        for (ChargeStationNotAllowedUseDateConfigEntity nd : list) {
            if (!nd.CSId.equalsIgnoreCase(CSId)) continue;
            if (nd.start_time >= nowTime || nd.end_time <= nowTime) continue;
            return nd;
        }

        DataService.getMainCache().del(String.format("ChargeStationNotAllowedUseDateConfig:%s:List", CSId));
        return null;
    }
}
