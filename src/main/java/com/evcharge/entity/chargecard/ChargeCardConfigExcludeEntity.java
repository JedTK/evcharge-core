package com.evcharge.entity.chargecard;

import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * 充电卡排斥表;
 *
 * @author : JED
 * @date : 2022-10-9
 */
@Deprecated
public class ChargeCardConfigExcludeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电卡id
     */
    public long cardId1;
    /**
     * 充电卡id
     */
    public long cardId2;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeCardConfigExcludeEntity getInstance() {
        return new ChargeCardConfigExcludeEntity();
    }

    /**
     * 批量关联数据
     *
     * @param cardId1
     * @param cardId2List
     * @return
     */
    public SyncResult batAdd(long cardId1, String cardId2List) {
        if (cardId1 == 0) return new SyncResult(2, "cardId1不能为空");
        if (!StringUtils.hasLength(cardId2List)) return new SyncResult(2, "cardId2集合不能为空");

        ChargeCardConfigEntity chargeCardConfigEntity = new ChargeCardConfigEntity();
        if (!chargeCardConfigEntity.exist(cardId1)) return new SyncResult(2, "cardId1不正确");

        return this.beginTransaction(connection -> {
            try {
                String[] itemIds = cardId2List.split(",");
                if (itemIds.length == 0) return new SyncResult(3, "充电卡集合格式不对");

                for (int i = 0; i < itemIds.length; i++) {
                    long cardId2 = Long.valueOf(itemIds[i]);
                    if (!chargeCardConfigEntity.existTransaction(connection, cardId2)) continue;

                    if (!this.where("cardId1", cardId1)
                            .where("cardId2", cardId2)
                            .exist()) {
                        Map<String, Object> insert1_data = new LinkedHashMap<>();
                        insert1_data.put("cardId1", cardId1);
                        insert1_data.put("cardId2", cardId2);
                        int noquery = this.insertTransaction(connection, insert1_data);
                        if (noquery == 0) return new SyncResult(1, "操作失败");
                    }

                    if (cardId1 == cardId2) continue;

                    if (!this.where("cardId1", cardId2)
                            .where("cardId2", cardId1)
                            .exist()) {
                        Map<String, Object> insert2_data = new LinkedHashMap<>();
                        insert2_data.put("cardId1", cardId2);
                        insert2_data.put("cardId2", cardId1);
                        int noquery = this.insertTransaction(connection, insert2_data);
                        if (noquery == 0) return new SyncResult(1, "操作失败");
                    }
                }
            } catch (Exception e) {
                return new SyncResult(1, "操作失败");
            }

            return new SyncResult(0, "");
        });
    }

    /**
     * 获取互斥关系的卡id列表
     *
     * @param cardId1
     * @return
     */
    public List<Object> getListWithCard1(long cardId1) {
        List<Object> ids = new LinkedList<>();

        List<Map<String, Object>> list = this.field("cardId2")
                .where("cardId1", cardId1)
                .select();

        for (Map<String, Object> nd : list) {
            long cardId2 = MapUtil.getLong(nd, "cardId2");
            ids.add(cardId2);
        }
        return ids;
    }
}
