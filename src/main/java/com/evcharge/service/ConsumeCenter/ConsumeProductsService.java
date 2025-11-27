package com.evcharge.service.ConsumeCenter;

import com.evcharge.entity.consumecenter.product.ConsumeProductsEntity;
import com.xyzs.cache.ECacheTime;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ConsumeProductsService {


//    public static ConsumeProductsService getInstance() {
//        return new ConsumeProductsService();
//    }

    /**
     * 统计符合条件的产品数量
     */
    public int getCount(Map<String, Object> param) {
        ConsumeProductsEntity query = buildQuery(param);
        return query.count();
    }

    /**
     * 获取分页产品列表（默认按 id desc 排序）
     */
    public List<ConsumeProductsEntity> getList(Map<String, Object> param, int offset, int limit) {
        return getList(param, offset, limit, "sort asc");
    }

    /**
     * 获取分页产品列表（支持自定义排序）
     */
    public List<ConsumeProductsEntity> getList(Map<String, Object> param, int offset, int limit, String orderBy) {
        ConsumeProductsEntity consumeProductsEntity=buildQuery(param);
        return consumeProductsEntity
                .order(orderBy)
                .page(offset, limit).selectList();
    }

    /**
     * 获取产品详情（带缓存）
     */
    public ConsumeProductsEntity getProductById(long productId) {
        if (productId <= 0) {
            return null;
        }

        return ConsumeProductsEntity.getInstance()
                .where("id", productId)
                .cache(String.format("ConsumeCenter:Product:Info:%d", productId), ECacheTime.DAY)
                .findEntity();
    }
    /**
     * 构建通用查询条件
     */
    private ConsumeProductsEntity buildQuery(Map<String, Object> param) {
        ConsumeProductsEntity entity = new ConsumeProductsEntity();

        if (param == null || param.isEmpty()) {
            return entity;
        }

        for (Map.Entry<String, Object> entry : param.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (Objects.isNull(value)) continue;

            if ("default_type".equals(key)) {
                String[] arr = ((String) value).split(",");
                entity.whereIn("type_code", arr);
            } else {
                entity.where(key, value);
            }
        }

        return entity;
    }
}
