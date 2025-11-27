package com.evcharge.service.GameMate.UserEvent;


import com.evcharge.entity.gamemate.badge.UserBadgeEntity;
import com.xyzs.utils.TimeUtil;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserBadgeService {

    private void grantBadge(long uid, long badgeId) {
        // 检查是否已拥有
        if (this.exists(uid, badgeId)) {
            return;
        }
        Map<String, Object> info=new LinkedHashMap<>();

        info.put("uid", uid);
        info.put("badge_id", badgeId);
        info.put("create_time", TimeUtil.getTimestamp());


        UserBadgeEntity.getInstance().insert(info);

        // 可选：发推送、通知、积分奖励
    }




    private boolean exists(long uid, long badgeId) {
        return false;
    }



}
