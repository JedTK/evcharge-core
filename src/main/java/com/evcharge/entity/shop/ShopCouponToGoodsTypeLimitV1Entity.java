package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.sql.Connection;
import java.util.*;

/**
 * 优惠券商品限制：限制购买的商品类型关联表;
 * @author : Jay
 * @date : 2022-12-22
 */

@TargetDB("evcharge_shop")
public class ShopCouponToGoodsTypeLimitV1Entity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 规则id
     */
    public long rule_id ;
    /**
     * 商品类型id
     */
    public long goods_type_id ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ShopCouponToGoodsTypeLimitV1Entity getInstance() {
        return new ShopCouponToGoodsTypeLimitV1Entity();
    }


    /**
     * 添加商品类型
     *
     * @param ruleId
     * @param goodsTypeIds
     */
    public void addGoodsType(long ruleId, String[] goodsTypeIds) {
        if (ruleId == 0) return;

        this.beginTransaction(connection -> {
            return this.addGoodsTypeTransaction(connection, ruleId, goodsTypeIds);
        });
    }

    /**
     * 重置商品类型
     *
     * @param ruleId
     * @param goodsTypeIds
     */
    public void resetGoodsType(long ruleId, String[] goodsTypeIds) {
        if (ruleId == 0) return;
        this.where("rule_id", ruleId).del();
        this.beginTransaction(connection -> {
            return this.addGoodsTypeTransaction(connection, ruleId, goodsTypeIds);
        });
    }

    /**
     * 添加商品类型
     * @param connection
     * @param ruleId
     * @param goodsTypeIds
     * @return
     */
    public SyncResult addGoodsTypeTransaction(Connection connection, long ruleId, String[] goodsTypeIds) {

        List<String> list = Arrays.asList(goodsTypeIds);
        Iterator it = list.iterator();
        try {
            while (it.hasNext()) {
                String nd = (String) it.next();
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("rule_id", ruleId);
                data.put("goods_type_id", nd);
                long r = this.insertGetIdTransaction(connection, data);
                if (r == 0) return new SyncResult(1, "添加失败");
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            return new SyncResult(1, String.format("更新失败，失败原因：%s", e.getMessage()));
        }

    }

}