package com.evcharge.service.User;

import com.evcharge.entity.gamemate.skin.SkinEntity;
import com.evcharge.entity.gamemate.skin.SkinPositionEntity;
import com.evcharge.entity.gamemate.skin.UserSkinEntity;
import com.evcharge.service.GameMate.SkinService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserSkinService {

    @Autowired
    private SkinService skinService;


    /**
     * 检查用户是否拥有某个皮肤
     *
     * @param userId uid
     * @param skinId skin_id
     * @return
     */
    public boolean checkUserSkin(long userId, long skinId) {
        int count = UserSkinEntity.getInstance()
                .where("uid", userId)
                .where("skin_id", skinId)
                .where("status", 1)
                .count();


        return count > 0;

    }


    /**
     * 获取用户皮肤列表
     *
     * @param userId uid
     * @return List<Map < String, Object>>
     */
    public List<Map<String, Object>> getUserSkinsByUserId(Long userId) {
        long currentTime = TimeUtil.getTimestamp();

        List<UserSkinEntity> list = UserSkinEntity.getInstance()
                .where("uid", userId)
                .where("start_time", "<=", currentTime)
                .where("end_time", ">=", currentTime)
                .where("status", 1)
                .selectList();


        if (list.isEmpty()) return null;


        List<Map<String, Object>> userSkinList = new ArrayList<>();


        for (UserSkinEntity userSkinEntity : list) {
            SkinEntity skinEntity = skinService.getSkinInfoById(userSkinEntity.skin_id);
            List<SkinPositionEntity> skinPositionEntityList = skinService.getSkinPositionInfoById(skinEntity.id);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", userSkinEntity.id);
            map.put("uid", userId);
            map.put("skin_id", userSkinEntity.skin_id);
            map.put("start_time", userSkinEntity.start_time);
            map.put("end_time", userSkinEntity.end_time);
            map.put("is_default", userSkinEntity.is_default);
            map.put("description", skinEntity.description);
            map.put("rights", skinEntity.rights);
            map.put("status", 1);


            map.put("skin_title", skinEntity.name);
            map.put("skin_main_image", skinEntity.main_image);
            map.put("skin_positions", skinPositionEntityList);

            userSkinList.add(map);

        }

        return userSkinList;


    }

    /**
     * 获取用户默认皮肤
     *
     * @param userId 用户uid
     * @return Map<String, Object>
     */
    public Map<String, Object> getUserDefaultSkin(Long userId) {
        long currentTime = TimeUtil.getTimestamp();
        UserSkinEntity userSkinEntity = UserSkinEntity.getInstance()
                .where("uid", userId)
                .where("start_time", "<=", currentTime)
                .where("end_time", ">=", currentTime)
                .where("status", 1)
                .where("is_default", 1)
                .order("is_default desc")
                .page(1, 1)
                .findEntity();


        if (userSkinEntity == null) return null;
        SkinEntity skinEntity = skinService.getSkinInfoById(userSkinEntity.skin_id);
        List<SkinPositionEntity> skinPositionEntityList = skinService.getSkinPositionInfoById(skinEntity.id);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", userSkinEntity.id);
        map.put("skin_id", userSkinEntity.skin_id);
        map.put("uid", userId);
        map.put("description", skinEntity.description);
        map.put("rights", skinEntity.rights);
        map.put("is_default", userSkinEntity.is_default);
        map.put("status", 1);
        map.put("start_time", userSkinEntity.start_time);
        map.put("end_time", userSkinEntity.end_time);
        map.put("create_time", userSkinEntity.create_time);


        map.put("skin_title", skinEntity.name);
        map.put("skin_main_image", skinEntity.main_image);
        map.put("skin_positions", skinPositionEntityList);


        return map;
    }


    /**
     * 设置默认皮肤
     *
     * @param uid
     * @param skinId
     * @return
     */
    public SyncResult setDefault(long uid, long skinId) {

        UserSkinEntity.getInstance()
                .where("uid", uid)
                .update(new HashMap<>() {{
                    put("is_default", 0);
                }});

        SkinEntity skinEntity = skinService.getSkinInfoById(skinId);

        if (skinEntity.type_code.equals("system")) return new SyncResult(0, "success");

        Map<String, Object> data = new HashMap<>();
        data.put("is_default", 1);
        data.put("update_time", TimeUtil.getTimestamp());

        long updateId = UserSkinEntity.getInstance()
                .where("uid", uid)
                .where("skin_id", skinId)
                .update(data);

        if (updateId == 0) return new SyncResult(1, "更新失败，请稍后再试");

        return new SyncResult(0, "success");
    }

    /**
     * 添加皮肤 绑定物理卡用到
     *
     * @param uid        用户uid
     * @param cardId     卡id
     * @param cardNumber 卡编号
     * @return SyncResult
     */
    public SyncResult addSkinForCardId(long uid, long cardId, String cardNumber) {
        SkinEntity skinEntity = skinService.getSkinInfoByCardId(cardId);
        if (skinEntity == null) return new SyncResult(1, "");
        return addSkin(uid, skinEntity.id, "card", cardNumber);
    }

    /**
     * 移除皮肤 解绑的时候需要用到
     *
     * @param uid    用户uid
     * @param cardId 卡id
     * @return SyncResult
     */
    public SyncResult removeSkinForCardId(long uid, long cardId) {
        SkinEntity skinEntity = skinService.getSkinInfoByCardId(cardId);
        if (skinEntity == null) return new SyncResult(1, "");
        return removeSkin(uid, skinEntity.id);
    }

    /**
     * 系统派发皮肤
     *
     * @param uid    用户id
     * @param skinId 皮肤id
     * @return SyncResult
     */
    public SyncResult addSkinForSystem(long uid, long skinId) {
        return addSkin(uid, skinId, "system", "");
    }

    /**
     * 添加皮肤
     *
     * @param uid     用户id
     * @param skinId  皮肤id
     * @param source  皮肤来源
     * @param orderSn 订单编号
     * @return SyncResult
     */
    public SyncResult addSkin(long uid, long skinId, String source, String orderSn) {
        long currentTime = TimeUtil.getTimestamp();
        UserSkinEntity userSkinEntity = UserSkinEntity.getInstance()
                .where("uid", uid)
                .where("skin_id", skinId)
                .where("start_time", ">=", currentTime)
                .where("end_time", "<=", currentTime)
                .where("status", 1)
                .findEntity();

        if (userSkinEntity != null) return new SyncResult(1, "已经拥有该皮肤");

        SkinEntity skinEntity = skinService.getSkinInfoById(skinId);

        if (skinEntity == null) return new SyncResult(1, "皮肤不存在");

        long endTime = TimeUtil.getTime00();
        if (skinEntity.duration_days == 0) {
            endTime = TimeUtil.toTimestamp("2099-12-31 23:59:59");
        } else {
            endTime = endTime + (long) skinEntity.duration_days * 24 * 60 * 60 * 1000;
        }

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("uid", uid);
        data.put("skin_id", skinEntity.id);
        data.put("source", source);
        data.put("order_sn", orderSn);
        data.put("start_time", TimeUtil.getTime00());
        data.put("end_time", endTime);
        data.put("status", 1);
        data.put("create_time", TimeUtil.getTimestamp());

        long id = UserSkinEntity.getInstance().insertGetId(data);

        if (id == 0) return new SyncResult(1, "添加失败，请稍后再试");

        return new SyncResult(0, "success");

    }

    /**
     * 移除皮肤
     *
     * @param uid    uid
     * @param skinId skinId
     * @return SyncResult
     */
    public SyncResult removeSkin(long uid, long skinId) {
        UserSkinEntity userSkinEntity = UserSkinEntity.getInstance()
                .where("uid", uid)
                .where("skin_id", skinId)
                .where("status", 1)
                .findEntity();

        if (userSkinEntity == null) return new SyncResult(1, "没有皮肤信息");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", 0);
        map.put("update_time", TimeUtil.getTimestamp());
        long id = UserSkinEntity.getInstance().where("id", userSkinEntity.id).update(map);
        if (id == 0) return new SyncResult(1, "移除失败");
        return new SyncResult(0, "success");
    }


}
