package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 合作人置表;
 *
 * @author : JED
 * @date : 2023-2-2
 */
public class OrganizeConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 组织ID
     */
    public long organize_id;
    /**
     * 配置名
     */
    public String name_code;
    /**
     * 配置值
     */
    public String value;
    /**
     * 说明
     */
    public String desc;
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
    public static OrganizeConfigEntity getInstance() {
        return new OrganizeConfigEntity();
    }

    /**
     * 读取所有配置信息（根据管理员或者组织id）
     *
     * @param organize_id 或者组织id
     * @return
     */
    public Map<String, Object> getConfigWithOrganizeId(long organize_id) {
        return getConfigWithOrganizeId(organize_id, null);
    }

    /**
     * 读取配置信息（根据管理员或者组织id）
     *
     * @param organize_id 或者组织id
     * @param keys        对应的key集合
     * @return
     */
    public Map<String, Object> getConfigWithOrganizeId(long organize_id, String[] keys) {
        Map<String, Object> configs = new LinkedHashMap<>();

        this.field("id,name_code,value");
        if (keys != null && keys.length > 0) {
            this.whereIn("name_code", keys);
        }
        this.where("organize_id", organize_id);

        List<Map<String, Object>> partnerConfigList = this.select();
        if (partnerConfigList.size() == 0) return configs;

        Iterator it = partnerConfigList.iterator();
        while (it.hasNext()) {
            Map<String, Object> nd = (Map<String, Object>) it.next();
            String name_code = MapUtil.getString(nd, "name_code");
            Object value = nd.get("value");
            configs.put(name_code, value);
        }
        return configs;
    }

    /**
     * 保存配置（如果不存在则新增，存在则更新）
     *
     * @param organize_id 组织id
     * @param key         配置key
     * @param value       配置值
     * @param desc        当新增时插入的说明
     * @return
     */
    public boolean saveConfig(long organize_id, String key, String value, String desc) {
        SyncResult r = this.beginTransaction(connection -> {
            //如果对应的key不存在，则直接新建一个key，并且赋对应的值
            if (!this.where("organize_id", organize_id)
                    .where("name_code", key)
                    .existTransaction(connection)) {
                this.organize_id = organize_id;
                this.name_code = key;
                this.value = value;
                this.desc = desc;
                this.create_time = TimeUtil.getTimestamp();
                this.update_time = this.create_time;
                this.id = this.insertGetIdTransaction(connection);
                if (this.id > 0) return new SyncResult(0, "");
                return new SyncResult(1, "");
            }

            int noquery = this.where("organize_id", organize_id)
                    .where("name_code", key)
                    .updateTransaction(connection, new LinkedHashMap<>() {{
                        put("value", value);
                        put("update_time", TimeUtil.getTimestamp());
                    }});
            if (noquery == 0) return new SyncResult(1, "");
            return new SyncResult(0, "");
        });
        if (r.code == 0) return true;
        return false;
    }
}
