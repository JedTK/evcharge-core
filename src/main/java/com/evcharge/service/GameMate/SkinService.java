package com.evcharge.service.GameMate;


import com.evcharge.entity.gamemate.skin.SkinEntity;
import com.evcharge.entity.gamemate.skin.SkinPositionEntity;
import com.xyzs.cache.ECacheTime;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkinService {


    /**
     * 获取皮肤信息
     *
     * @param id
     * @return
     */
    public SkinEntity getSkinInfoById(long id) {
        SkinEntity skinEntity = new SkinEntity();
        return skinEntity
                .cache(String.format("GameMate:Skin:Info:%s", id), ECacheTime.DAY)
                .where("id", id)
                .where("status", 1)
                .findEntity();

    }

    /**
     * 获取皮肤的位置明细
     *
     * @param skinId
     * @return
     */
    public List<SkinPositionEntity> getSkinPositionInfoById(long skinId) {
        SkinPositionEntity skinPositionEntity = new SkinPositionEntity();
        return skinPositionEntity
                .cache(String.format("GameMate:SkinPositionInfo:Position:%s", skinId), ECacheTime.DAY)
                .where("skin_id", skinId)
                .selectList();
    }


    /**
     * 获取系统皮肤
     *
     * @return SkinEntity
     */
    public SkinEntity getSystemSkin() {
        SkinEntity skinEntity = new SkinEntity();

        return skinEntity
                .cache("GameMate:Skin:System", ECacheTime.DAY)
                .where("type_code", "system")
                .where("status", 1)
                .findEntity();

    }

    /**
     * 获取系统皮肤明细
     * @return Map<String, Object>
     */
    public Map<String, Object> getSystemSkinDetail() {
        SkinEntity skinEntity = getSystemSkin();
        if (skinEntity == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", skinEntity.id);
        map.put("skin_id", skinEntity.id);
        map.put("name", skinEntity.name);
        map.put("type_code", skinEntity.type_code);
        map.put("rarity", skinEntity.rarity);
        map.put("tags", skinEntity.tags);
        map.put("description", skinEntity.description);
        map.put("rights", skinEntity.rights);
        map.put("main_image", skinEntity.main_image);

        List<SkinPositionEntity> list = getSkinPositionInfoById(skinEntity.id);

        map.put("skin_positions", list);
        return map;
    }
}
