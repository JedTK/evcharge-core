package com.evcharge.entity.admin;


import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.common;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 管理员-街道（管理员可查看街道的数据）;
 *
 * @author : JED
 * @date : 2023-8-30
 */
@TargetDB("evcharge_rbac")
public class AdminToStreetEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 街道代码
     */
    public String street_code;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static AdminToStreetEntity getInstance() {
        return new AdminToStreetEntity();
    }

    /**
     * 获取街道代码列表
     *
     * @param admin_id 管理员id
     * @return
     */
    public List<Object> getStreetCode(long admin_id) {
        List<Object> street_code_array = this.where("admin_id", admin_id)
                .cache(String.format("MegaData:Admin:%s:StreetCode", admin_id))
                .selectForArray("street_code");
        if (street_code_array == null && street_code_array.isEmpty()) return null;

        Iterator it = street_code_array.iterator();
        while (it.hasNext()) {
            String street_code = (String) it.next();
            if ("ALL".equalsIgnoreCase(street_code)) {
                street_code_array = ChargeStationEntity.getInstance()
                        .field("street_code")
                        .group("street_code")
                        .selectForArray("street_code");
                initCache().setList(String.format("MegaData:Admin:%s:StreetCode", admin_id), street_code_array);
                return street_code_array;
            }
        }
        return street_code_array;
    }

    /**
     * 通过街道代码查询管理员
     *
     * @param street_code
     * @return
     */
    public List<Long> getAdminId(String[] street_code) {
        return getAdminId(street_code, true);
    }

    /**
     * 通过街道代码查询管理员
     *
     * @param street_code
     * @return
     */
    public List<Long> getAdminId(String[] street_code, boolean inCache) {
        if (inCache) {
            String cacheKey = String.format("Admin:Street:%s", Arrays.toString(street_code));
            this.cache(cacheKey, ECacheTime.MINUTE * 5);
        }
        return this.whereIn("street_code", street_code).selectList();
    }

    /**
     * 检查 管理员是否有权限查询这个街道的数据
     *
     * @param admin_id    管理员id
     * @param street_code 街道代码
     * @return
     */
    public boolean checkAuth(long admin_id, String street_code) {
        List<Object> street_code_array = AdminToStreetEntity.getInstance().getStreetCode(admin_id);
        if (street_code_array == null || street_code_array.isEmpty()) return false;
        return street_code_array.contains(street_code);
    }
}
