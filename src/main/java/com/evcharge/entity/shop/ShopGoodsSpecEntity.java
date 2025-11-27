package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 规格表;
 * @author : Jay
 * @date : 2022-12-20
 */
@TargetDB("evcharge_shop")
public class ShopGoodsSpecEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 用户ID
     */
    public long goods_id ;
    /**
     * 规格名称
     */
    public String spec_name ;
    /**
     * 排序
     */
    public int sort ;
    /**
     * 创建时间
     */
    public long create_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ShopGoodsSpecEntity getInstance() {
        return new ShopGoodsSpecEntity();
    }

    /**
     * 通过产品id获取产品规格
     * @param goodsId
     * @return
     */
    public SyncResult getGoodsSpecByGoodsId(long goodsId){

        if(goodsId==0) return new SyncResult(1,"goods_id不能为空");

        List<Map<String,Object>> list = this.where("goods_id",goodsId).field("id,goods_id,spec_name,sort")
                .order("sort desc").select();

        if(list.size()==0) return new SyncResult(1,"");

        Iterator it = list.iterator();

        while (it.hasNext()){
            Map<String, Object> nd = (Map<String, Object>) it.next();
            List<Map<String,Object>> specItem= ShopGoodsSpecItemEntity.getInstance()
                    .where("spec_id", MapUtil.getLong(nd,"id")).select();

            nd.put("spec_item",specItem);
        }

        return new SyncResult(0,"success",list);

    }

    /**
     * 通过产品id获取产品规格，只有规格
     * @param goodsId
     * @return
     */
    public SyncResult getGoodsSpecItemByGoodsId(long goodsId){

        if(goodsId==0) return new SyncResult(1,"goods_id不能为空");

        List<Map<String,Object>> list = this.where("goods_id",goodsId).field("id,goods_id,spec_name,sort")
                .order("sort desc").select();

        List<List<Map<String,Object>>> itemList = new ArrayList<>();

        if(list.size()==0) return new SyncResult(1,"");

        Iterator it = list.iterator();

        while (it.hasNext()){
            Map<String, Object> nd = (Map<String, Object>) it.next();
            List<Map<String,Object>> specItem= ShopGoodsSpecItemEntity.getInstance()
                    .where("spec_id", MapUtil.getLong(nd,"id")).select();

            itemList.add(specItem);
        }

        return new SyncResult(0,"success",itemList);

    }



}