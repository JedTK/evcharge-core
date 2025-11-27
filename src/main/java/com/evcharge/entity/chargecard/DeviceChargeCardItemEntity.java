package com.evcharge.entity.chargecard;


import com.evcharge.entity.device.DeviceEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import org.springframework.util.StringUtils;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设备-充电卡 n-n;
 *
 * @author : JED
 * @date : 2022-10-9
 */
public class DeviceChargeCardItemEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 关联的设备ID
     */
    public long deviceId;
    /**
     * 充电卡id(待删除)
     */
    @Deprecated
    public long cardConfigId;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     * 排序
     */
    public int sortIndex;

    //endregion

    /**
     * 获得一个实例
     */
    public static DeviceChargeCardItemEntity getInstance() {
        return new DeviceChargeCardItemEntity();
    }

    /**
     * 关联数据
     */
    public SyncResult add() {
        if (deviceId == 0) return new SyncResult(2, "请选择正确的设备");
        if (cardConfigId == 0) return new SyncResult(2, "请选择正确的充电卡");

        if (!DeviceEntity.getInstance().exist(deviceId)) return new SyncResult(2, "请选择正确的设备");
        if (!ChargeCardConfigEntity.getInstance().exist(cardConfigId)) return new SyncResult(2, "请选择正确的充电卡");

        id = insertGetId();
        if (id > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }

    /**
     * 设备批量允许使用的充电卡
     *
     * @param deviceId      设备id
     * @param cardIdList    充电卡id
     * @param sortIndexList 排序
     */
    public SyncResult batAdd(long deviceId, String cardIdList, String sortIndexList) {
        return DataService.getMainDB().beginTransaction(connection -> batAddTransaction(connection, deviceId, cardIdList, sortIndexList));
    }

    /**
     * 设备批量允许使用的充电卡（事务）
     *
     * @param connection    JDBC事务链接
     * @param deviceId      设备id
     * @param cardIdList    充电卡id
     * @param sortIndexList 排序
     */
    public SyncResult batAddTransaction(Connection connection, long deviceId, String cardIdList, String sortIndexList) {
        if (deviceId == 0) return new SyncResult(2, "请选择正确的设备");
        if (!StringUtils.hasLength(cardIdList)) return new SyncResult(2, "请选择正确的充电卡");

        try {
            if (!DeviceEntity.getInstance().existTransaction(connection)) {
                return new SyncResult(2, "请选择正确的设备");
            }

            String[] itemIds = cardIdList.split(",");
            String[] sortIndexs = sortIndexList.split(",");
            if (itemIds.length == 0) return new SyncResult(3, "充电卡集合格式不对");

            ChargeCardConfigEntity chargeCardConfigEntity = new ChargeCardConfigEntity();

            for (int i = 0; i < itemIds.length; i++) {
                long cardId = Long.valueOf(itemIds[i]);
                int sortIndex = 100;
                if (i < sortIndexs.length) sortIndex = Integer.valueOf(sortIndexs[i]);
                if (!chargeCardConfigEntity.existTransaction(connection, cardId)) continue;

                Map<String, Object> insert_data = new LinkedHashMap<>();
                insert_data.put("deviceId", deviceId);
                insert_data.put("cardConfigId", cardId);
                insert_data.put("sortIndex", sortIndex);
                int noquery = this.insertTransaction(connection, insert_data);
                if (noquery == 0) return new SyncResult(1, "操作失败");
            }
        } catch (Exception e) {
            LogsUtil.error(e, "", "批量关联数据发生错误");
            return new SyncResult(1, e.getMessage());
        }

        return new SyncResult(0, "");
    }

    /**
     * 批量操作设备绑定支持的充电卡
     *
     * @param cardId    充电卡id
     * @param deviceIds 设备id集合
     */
    public SyncResult batBindSupportCardId(long cardId, long[] deviceIds) throws SQLException {
        return this.beginTransaction(connection -> batBindSupportCardIdTransaction(connection, cardId, deviceIds));
    }

    /**
     * 批量操作设备绑定支持的充电卡
     *
     * @param cardId    充电卡id
     * @param deviceIds 设备id集合
     */
    public SyncResult batBindSupportCardIdTransaction(Connection connection, long cardId, long[] deviceIds) throws SQLException {
        for (long deviceId : deviceIds) {
            if (this.where("deviceId", deviceId)
                    .where("cardConfigId", cardId)
                    .existTransaction(connection)) continue;
            int noquery = this.insertTransaction(connection, new LinkedHashMap<>() {{
                put("deviceId", deviceId);
                put("cardConfigId", cardId);
            }});
            if (noquery == 0) return new SyncResult(1, "批量设备绑定 单一充电卡 发生错误");
        }
        return new SyncResult(0, "");
    }

    /**
     * 批量操作设备解绑支持的充电卡
     *
     * @param cardId    充电卡id
     * @param deviceIds 设备id集合
     */
    public SyncResult batUnBindSupportCardId(long cardId, long[] deviceIds) throws SQLException {
        return this.beginTransaction(connection -> batUnBindSupportCardIdTransaction(connection, cardId, deviceIds));
    }

    /**
     * 批量操作设备解绑支持的充电卡
     *
     * @param cardId    充电卡id
     * @param deviceIds 设备id集合
     */
    public SyncResult batUnBindSupportCardIdTransaction(Connection connection, long cardId, long[] deviceIds) throws SQLException {
        for (long deviceId : deviceIds) {
            int noquery = this.where("deviceId", deviceId)
                    .where("cardConfigId", cardId)
                    .delTransaction(connection);
            if (noquery == 0) return new SyncResult(1, "批量设备解绑 单一充电卡 发生错误");
        }
        return new SyncResult(0, "");
    }


}
