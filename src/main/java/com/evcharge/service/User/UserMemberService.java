package com.evcharge.service.User;

import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.member.MemberConfigEntity;
import com.evcharge.entity.user.member.UserMemberEntity;
import com.xyzs.utils.TimeUtil;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class UserMemberService {
    // 注入用户服务
    @Autowired
    private UserService userService;

    /**
     * 检查用户是否是会员
     *
     * @param uid uid
     * @return boolean
     */
    public boolean checkUserIsMember(long uid) {
        long currentTime = TimeUtil.getTimestamp();
        UserMemberEntity userMemberEntity = this.getUserMember(uid);
        if (userMemberEntity == null) return false;
        if (userMemberEntity.end_time > currentTime) {
            return true;
        }
        //更新状态为已过期
        userMemberEntity.where("id", userMemberEntity.id).update(new LinkedHashMap<>() {{
            put("status", 2);
        }});
        return false;
    }

    /**
     * 获取会员信息
     *
     * @param uid uid
     * @return UserMemberEntity
     */
    public UserMemberEntity getUserMember(long uid) {
        return UserMemberEntity.getInstance()
                .where("uid", uid)
                .where("status", 1)
                .findEntity();
    }


    //购买会员 Purchase membership
    public void purchaseMembership(long uid, long configId, String orderSn) {
//        UserEntity userEntity = userService.findUserByUid(uid);
        MemberConfigEntity memberConfigEntity = MemberConfigEntity.getInstance().findEntity(configId);
        if (memberConfigEntity == null) {
            return;
        }
        /**
         * 检查是否为会员，如果是会员，需要旧数据状态设置为-1，结束时间设置为0，新建一条信息，同时添加备注信息
         * 如果不是会员，则新建会员信息
         */
        if (this.checkUserIsMember(uid)) {
            UserMemberEntity userMemberEntity = this.getUserMember(uid);
            Map<String,Object> userOldMember = new LinkedHashMap<>();
            UserMemberEntity userNewMember = new UserMemberEntity();

            userOldMember.put("status",3);
            userOldMember.put("end_time",0);
            userOldMember.put("update_time", TimeUtil.getTimestamp());
            UserMemberEntity.getInstance().where("id",userMemberEntity.id).update(userOldMember);


            //插入新的数据
            userNewMember.uid = uid;
            /**
             * TODO 后续有会员等级需要补充会员等级
             */
            userNewMember.level_id = 1;
            userNewMember.status=1;
            userNewMember.start_time = userMemberEntity.start_time;
            userNewMember.order_sn=orderSn;
            long oneDayMillis = 24L * 60 * 60 * 1000; // 一天的毫秒数

            userNewMember.end_time = userMemberEntity.end_time + (memberConfigEntity.expire_day + 1) * oneDayMillis;
            userNewMember.create_time = TimeUtil.getTimestamp();

            userNewMember.insert();
        }else{
            UserMemberEntity userNewMember = new UserMemberEntity();
            userNewMember.uid = uid;
            /**
             * TODO 后续有会员等级需要补充会员等级
             */
            userNewMember.level_id = 1;
            userNewMember.status=1;
            userNewMember.start_time = TimeUtil.getTime00();
            userNewMember.order_sn=orderSn;
            long oneDayMillis = 24L * 60 * 60 * 1000; // 一天的毫秒数

            userNewMember.end_time = TimeUtil.getTime00() + (memberConfigEntity.expire_day + 1) * oneDayMillis-1;
            userNewMember.create_time = TimeUtil.getTimestamp();

            userNewMember.insert();
        }


    }

    /**
     * 注销/退款
     * @param uid  uid
     */
    public void cancelMembership(long uid) {
        if (!this.checkUserIsMember(uid)) {
            return;
        }
        UserMemberEntity userMemberEntity = this.getUserMember(uid);
        UserMemberEntity userMember = new UserMemberEntity();
        Map<String,Object> info = new LinkedHashMap<>();
        info.put("status",4);
        info.put("end_time",0);
        info.put("update_time", TimeUtil.getTimestamp());
        UserMemberEntity.getInstance().where("id",userMemberEntity.id).update(info);
    }

}
