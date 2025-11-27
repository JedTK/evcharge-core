package com.evcharge.entity.basedata;

import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 收费标准项;
 *
 * @author : JED
 * @date : 2022-10-8
 */
public class ChargeStandardItemEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 配置ID
     */
    public long configId;
    /**
     * 功率范围最小值
     */
    public double minPower;
    /**
     * 功率范围最大值
     */
    public double maxPower;
    /**
     * 电费单价(元/度)
     */
    public BigDecimal electricityFeePrice;
    /**
     * 服务费单价(元/小时)
     */
    public BigDecimal serviceFeePrice;
    /**
     * 价格,单位：元，元/每小时
     */
    public double price;
    /**
     * 对应充电卡日消耗倍率
     */
    public double chargeCardConsumeTimeRate;
    /**
     * 人民币对比积分比率
     */
    public double integralConsumeRate;
    /**
     * 调整价格最小值
     */
    public double minPrice;
    /**
     * 调整价格最大值
     */
    public double maxPrice;

    /**
     * 计费间隔，单位：秒
     */
    public int billingInterval;
    /**
     * 计费单价，单位：元，结合计费间隔计算费用
     */
    public double billingPrice;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStandardItemEntity getInstance() {
        return new ChargeStandardItemEntity();
    }

    /**
     * 根据配置ID和充电功率查询适配的项
     *
     * @param configId
     * @param chargePower
     * @return
     */
    public ChargeStandardItemEntity getItemWithConfig(long configId, double chargePower) {
        ChargeStandardItemEntity chargeStandardItemEntity = this
                .where("configId", configId)
                .where("minPower", "<", chargePower)
                .where("maxPower", ">=", chargePower)
                .order("maxPower DESC")
                .findEntity();
        if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
            //查询不到则查询最小功率的一条来计算,不然按照最高的来扣费，会被用户骂死
            chargeStandardItemEntity = this.where("configId", configId)
                    .order("maxPower ASC")
                    .findEntity();
        }
        return chargeStandardItemEntity;
    }

    /**
     * 根据配置ID和充电功率查询适配的项
     *
     * @param id item id
     * @return
     */
    public ChargeStandardItemEntity getItemWithId(long id) {
        return this.cache(String.format("BaseData:ChargeStandardItem:%s", id))
                .order("maxPower DESC")
                .findEntity(id);
    }

    /**
     * 根据用户的车辆信息、设备信息、端口 得出当前适配的充电收费项目
     *
     * @param uid           用户ID
     * @param user_ebike_id 用户车辆
     * @param deviceEntity  设备信息
     * @return
     */
    public ChargeStandardItemEntity getItemWithUserBike(long uid, long user_ebike_id, DeviceEntity deviceEntity) {
        return getItemWithUserBike(uid, user_ebike_id, deviceEntity, 0);
    }

    /**
     * 根据用户的车辆信息、设备信息、端口 得出当前适配的充电收费项目
     *
     * @param uid           用户ID
     * @param user_ebike_id 用户车辆
     * @param deviceEntity  设备信息
     * @param port          端口
     * @return
     */
    public ChargeStandardItemEntity getItemWithUserBike(long uid, long user_ebike_id, DeviceEntity deviceEntity, int port) {
        //根据用户的车辆信息查询限制的充电功率
        double limitChargePower = ChargeOrderEntity.getInstance()
                .getLimitChargePowerWithUserHabit2(uid, user_ebike_id, deviceEntity, port);
        //如果匹配不上，则默认使用200w充电功率
        if (limitChargePower <= 0) {
            limitChargePower = SysGlobalConfigEntity.getDouble("Default:ChargeSafePower", 200);
        }

        //根据限制的充电功率查询对应的收费标准
        ChargeStandardItemEntity chargeStandardItemEntity = this.getItemWithConfig(deviceEntity.chargeStandardConfigId, limitChargePower);
        if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
            chargeStandardItemEntity = this.where("configId", deviceEntity.chargeStandardConfigId)
                    .order("minPower ASC")
                    .findEntity();
        }
        return chargeStandardItemEntity;
    }

    /**
     * 获取功率排名
     *
     * @param configId 配置ID
     * @return 功率排名，从小到大
     */
    public double[][] getPowerRanges(long configId) {
        List<Map<String, Object>> list = this.field("id,configId,minPower,maxPower")
                .cache(String.format("BaseData:ChargeStandardItem:PowerRanges:%s", configId))
                .where("configId", configId)
                .order("minPower")
                .select();
        // 初始化二维数组，大小为list的大小，表示每一行包含minPower和maxPower两个值
        double[][] powerRanges = new double[list.size()][2];

        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> item = list.get(i);

            // 获取minPower和maxPower并转换为double
            double minPower = MapUtil.getDouble(item, "minPower");
            double maxPower = MapUtil.getDouble(item, "maxPower");

            // 将minPower和maxPower保存到二维数组的对应行
            powerRanges[i][0] = minPower;
            powerRanges[i][1] = maxPower;
        }
        return powerRanges;
    }

    /**
     * 通过站点编码获取收费标准列表
     *
     * @param CSId 站点编码
     * @return 收费标准列表
     */
    public List<Map<String, Object>> getListByCSId(String CSId, boolean inCache) {
        List<Map<String, Object>> list;
        if (inCache) {
            list = initCache().getList(String.format("BaseData:%s:ChargeStandardItem", CSId));
            if (list != null && !list.isEmpty()) return list;
        }
        // 因收费标准的配置是在设备的配置上，所以通过站点id获取其中一个设备的配置信息来获取收费标准
        Map<String, Object> device = DeviceEntity.getInstance()
                .field("chargeStandardConfigId")
                .where("CSId", CSId)
                .order("id")
                .find();
        if (device == null || device.isEmpty()) return null;
        long configId = MapUtil.getLong(device, "chargeStandardConfigId");

        list = this.field("minPower,maxPower,electricityFeePrice,serviceFeePrice")
                .where("configId", configId)
                .order("minPower")
                .select();
        if (list == null || list.isEmpty()) return null;

        initCache().setList(String.format("BaseData:%s:ChargeStandardItem", CSId), list, ECacheTime.DAY * 7);
        return list;
    }
}
