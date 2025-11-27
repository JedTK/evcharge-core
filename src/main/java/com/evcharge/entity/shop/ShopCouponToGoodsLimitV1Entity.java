package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.sql.Connection;
import java.util.*;

/**
 * 优惠券商品限制：限制购买的商品关联表;
 *
 * @author : Jay
 * @date : 2022-12-16
 */
@TargetDB("evcharge_shop")
public class ShopCouponToGoodsLimitV1Entity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 优惠券规则id
     */
    public long rule_id;
    /**
     * 商品id
     */
    public long goods_id;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ShopCouponToGoodsLimitV1Entity getInstance() {
        return new ShopCouponToGoodsLimitV1Entity();
    }


    /**
     * 添加商品
     *
     * @param ruleId
     * @param goodsIds
     */
    public void addGoods(long ruleId, String[] goodsIds) {
        if (ruleId == 0) return;

        this.beginTransaction(connection -> {
            return this.addGoodsTransaction(connection, ruleId, goodsIds);
        });
    }

    /**
     * 重置商品
     *
     * @param ruleId
     * @param goodsIds
     */
    public void resetGoods(long ruleId, String[] goodsIds) {
        if (ruleId == 0) return;
        this.where("rule_id", ruleId).del();
        this.beginTransaction(connection -> {
            return this.addGoodsTransaction(connection, ruleId, goodsIds);
        });
    }

    /**
     * 添加商品
     * @param connection
     * @param ruleId
     * @param goodsIds
     * @return
     */
    public SyncResult addGoodsTransaction(Connection connection, long ruleId, String[] goodsIds) {

        List<String> list = Arrays.asList(goodsIds);
        Iterator it = list.iterator();
        try {
            while (it.hasNext()) {
                String nd = (String) it.next();
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("rule_id", ruleId);
                data.put("goods_id", nd);
                long r = this.insertGetIdTransaction(connection, data);
                if (r == 0) return new SyncResult(1, "添加失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            return new SyncResult(1, String.format("更新失败，失败原因：%s", e.getMessage()));
        }

    }


}