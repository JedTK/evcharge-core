package com.evcharge.entity.device;

import com.evcharge.entity.basedata.SpecUnitEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备单元-规格 n-n关联，设备拥有的规格数据
 *
 * @author : JED
 * @date : 2022-9-15
 */
public class DeviceUnitSpecEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 设备ID
     */
    public long deviceUnitId;
    /**
     * 规格ID
     */
    public long specId;
    /**
     * 规格具体值
     */
    public String specValue;
    /**
     * 排序
     */
    public int sort_index;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static DeviceUnitSpecEntity getInstance() {
        return new DeviceUnitSpecEntity();
    }

    /**
     * 批量添加规格
     *
     * @param deviceUnitId  设备单元ID
     * @param specIdList    规格id列表
     * @param specValueList 规格值列表
     * @return
     */
    public SyncResult batAddSpec(long deviceUnitId, String[] specIdList, String[] specValueList, String[] sortIndexList) {
        //检查参数是否为空
        if (deviceUnitId <= 0) return new SyncResult(2, "请检查正确的设备ID");
        if (specIdList == null || specIdList.length == 0) return new SyncResult(2, "规格id列表不能为空");

        //检查数据是否存在
        if (!DeviceUnitEntity.getInstance().exist(deviceUnitId)) return new SyncResult(4, "请检查正确的设备ID");

        return this.beginTransaction(connection -> {
            //删除已移除的菜单
            this.where("deviceUnitId", deviceUnitId).whereNotIn("specId", specIdList).delTransaction(connection);

            //循环添加数据
            for (int i = 0; i < specIdList.length; i++) {
                String specId = specIdList[i];

                //检查规格id是否正确,不存在则不添加，继续循环
                if (!SpecUnitEntity.getInstance().where("id", specId).existTransaction(connection)) continue;

                String specValue = "";
                if (i < specValueList.length) specValue = specValueList[i];

                int sort_index = 100;
                try {
                    if (i < sortIndexList.length) sort_index = Integer.valueOf(sortIndexList[i]);
                } catch (Exception e) {
                }
                Map<String, Object> data = new HashMap<>();
                data.put("specValue", specValue);
                data.put("sort_index", sort_index);

                //检查是否拥有了此规格，如果拥有则更新
                if (this.where("deviceUnitId", deviceUnitId)
                        .where("specId", specId)
                        .existTransaction(connection)
                ) {
                    this.where("deviceUnitId", deviceUnitId)
                            .where("specId", specId)
                            .updateTransaction(connection, data);
                } else {
                    data.put("deviceUnitId", deviceUnitId);
                    data.put("specId", specId);
                    if (insertTransaction(connection, data) == 0) {
                        return new SyncResult(1, "添加失败");
                    }
                }
            }
            return new SyncResult(0, "");
        });
    }

    /**
     * 批量删除规格
     *
     * @param deviceUnitId   设备单元ID
     * @param specIdList 规格id列表
     * @return
     */
    public SyncResult batDelSpec(long deviceUnitId, String[] specIdList) {
        //检查参数是否为空
        if (deviceUnitId <= 0) return new SyncResult(2, "请检查正确的设备ID");
        if (specIdList == null || specIdList.length == 0) return new SyncResult(2, "规格id列表不能为空");

        //检查数据是否存在
        if (!SocketUnitEntity.getInstance().exist(deviceUnitId)) return new SyncResult(4, "请检查正确的设备ID");

        if (this.where("deviceUnitId", deviceUnitId).whereIn("specId", specIdList).del() > 0) return new SyncResult(0, "");
        return new SyncResult(1, "操作失败");
    }
}
