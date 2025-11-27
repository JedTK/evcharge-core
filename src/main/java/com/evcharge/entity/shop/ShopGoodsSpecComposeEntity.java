package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 规格组合表;
 *
 * @author : Jay
 * @date : 2022-12-20
 */
@TargetDB("evcharge_shop")
public class ShopGoodsSpecComposeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 用户ID
     */
    public long goods_id;
    /**
     * 组合id，对应spec_item表
     */
    public String compose_ids;
    /**
     * 标题（自动生成）
     */
    public String title;
    /**
     * 原价
     */
    public double original_price;
    /**
     * 总数
     */
    public int total_amount;
    /**
     * 库存
     */
    public int stock;
    /**
     * 售价
     */
    public double sale_price;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ShopGoodsSpecComposeEntity getInstance() {
        return new ShopGoodsSpecComposeEntity();
    }

    /**
     * 重置规格组合
     *
     * @param goodsId
     * @return
     */
    public SyncResult resetSpecCompose(long goodsId) {
        //获取最新的全部规格
        SyncResult r = ShopGoodsSpecEntity.getInstance().getGoodsSpecItemByGoodsId(goodsId);
        if (r.code != 0) return new SyncResult(r.code, r.msg);
        List<List<Map<String, Object>>> list = (List<List<Map<String, Object>>>) r.data;
        //重组新的sku组合
        //删除原来的规格组合
        //开始事务
        // Iterator it = list.iterator();
        System.out.println(list);
//        List<Map<String, Object>> result = this.cartesian(list);
//        System.out.println(result);
        return new SyncResult(1, "success");
    }



    public void descartes(){
        Map<String,Object> point = new LinkedHashMap<>();
        List<Map<String,Object>> result =new ArrayList<>();
        String pIndex = "";
        int tempCount = 0;
        List<Map<String,Object>> temp =new ArrayList<>();




    }


}