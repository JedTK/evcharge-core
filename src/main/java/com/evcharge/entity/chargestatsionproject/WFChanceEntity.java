package com.evcharge.entity.chargestatsionproject;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩商机-ChargeStation;
 *
 * @author : JED
 * @date : 2023-11-16
 */
public class WFChanceEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 项目ID，自定义生成
     */
    public String projectId;
    /**
     * 状态：-1-删除，0-待审核，1-进行中，2-项目完成，3-暂停中，4-取消
     */
    public int status;
    /**
     * 进度：拟建-公示中-待盖章-正在建设-暂缓
     */
    public String progress;
    /**
     * 省（省份）
     */
    public String province;
    /**
     * 市（城市
     */
    public String city;
    /**
     * 区（行政区划）
     */
    public String districts;
    /**
     * 街道/城镇
     */
    public String street;
    /**
     * 街道代码
     */
    public String street_code;
    /**
     * 城市社区和乡村
     */
    public String communities;
    /**
     * 道路
     */
    public String roads;
    /**
     * 城市和农村地址
     */
    public String addresses;
    /**
     * 参考对象
     */
    public String reference;
    /**
     * 经度
     */
    public double lon;
    /**
     * 纬度
     */
    public double lat;
    /**
     * 结构：0-无，1-棚，2-架
     */
    public int arch;
    /**
     * 场宽
     */
    public float fieldWidth;
    /**
     * 备注
     */
    public String remark;
    /**
     * 来源，''为元气充内部，用户-user,街道-street
     */
    public String source;
    /**
     * 创建者标识，管理员id，用户id
     */
    public String creatorId;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static WFChanceEntity getInstance() {
        return new WFChanceEntity();
    }
}
