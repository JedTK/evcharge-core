package com.evcharge.entity.spusku;


import com.evcharge.enumdata.ESpkuResApplyType;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * SPU+SKU图片资源;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSpuskuResourceEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     * sku编码
     */
    public String sku_code;
    /**
     * 资源路径
     */
    public String path;
    /**
     * 应用类型，1-主图，2-主图缩略图，3-预览图，4-详情图，999-其他
     */
    public int applyType;
    /**
     * 排序索引
     */
    public int sort_index;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static zSpuskuResourceEntity getInstance() {
        return new zSpuskuResourceEntity();
    }

    /**
     * 根据sku编码获取主图
     *
     * @param sku_code sku编码
     * @return
     */
    public String getMainImageWithSkuCode(String sku_code) {
        String path = DataService.getMainCache().getString(String.format("BaseData:Sku:%s:Resource:%s", sku_code, ESpkuResApplyType.MainImage));
        if (StringUtils.hasLength(path)) return path;

        Map<String, Object> data = this.field("id,path")
                .where("sku_code", sku_code)
                .where("applyType", ESpkuResApplyType.MainImage.index)
                .order("sort_index DESC,create_time,id")
                .find();
        if (data.size() == 0) return "";

        path = MapUtil.getString(data, "path");
        DataService.getMainCache().set(String.format("BaseData:Sku:%s:Resource:%s", sku_code, ESpkuResApplyType.MainImage), path);
        return path;
    }

    /**
     * 根据sku编码获取对应的图片资源
     *
     * @param sku_code          sku编码
     * @param eSpkuResApplyType 图片资源类型
     * @return
     */
    public List<String> getPathWithSkuCode(String sku_code, ESpkuResApplyType eSpkuResApplyType) {
        List<String> path_list = DataService.getMainCache().getList(String.format("BaseData:Sku:%s:Resource:%s", sku_code, eSpkuResApplyType.index));
        if (path_list != null && path_list.size() > 0) return path_list;

        List<Map<String, Object>> list = this.field("id,path")
                .where("sku_code", sku_code)
                .where("applyType", eSpkuResApplyType.index)
                .order("sort_index DESC,create_time,id")
                .select();
        if (list.size() == 0) return new LinkedList<>();

        path_list = new LinkedList<>();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Map<String, Object> nd = (Map<String, Object>) it.next();
            String path = MapUtil.getString(nd, "path");
            path_list.add(path);
        }
        DataService.getMainCache().setList(String.format("BaseData:Sku:%s:Resource:%s", sku_code, eSpkuResApplyType.index), path_list);
        return path_list;
    }
}
