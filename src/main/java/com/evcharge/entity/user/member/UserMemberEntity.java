package com.evcharge.entity.user.member;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 用户会员表;用户会员表
 *
 * @author : Jay
 * @date : 2025-10-11
 */
public class UserMemberEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 编号,;
     */
    public long id;
    /**
     * 用户ID,;
     */
    public long uid;
    /**
     * 会员编号,;
     */
    public String member_number;
    /**
     * 等级ID,;
     */
    public long level_id;
    /**
     * 状态（0=失效，1=有效，2=已过期，3=已经续期，4=已经退款）,;
     */
    public int status;
    /**
     * 订单编号,;
     */
    public String order_sn;
    /**
     * 开始时间,;
     */
    public long start_time;
    /**
     * 结束时间（时间戳）,;
     */
    public long end_time;
    /**
     * 创建时间,;
     */
    public long create_time;
    /**
     * 更新时间,;
     */
    public long update_time;
    //endregion
    /**
     * 获得一个实例
     */
    public static UserMemberEntity getInstance() {
        return new UserMemberEntity();
    }
}