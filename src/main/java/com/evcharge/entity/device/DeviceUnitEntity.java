package com.evcharge.entity.device;


import com.xyzs.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.apache.coyote.http11.filters.IdentityOutputFilter;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备最小单元，用于定义;
 *
 * @author : JED
 * @date : 2022-9-19
 */
@Getter
@Setter
public class DeviceUnitEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 设备名
     */
    public String name;
    /**
     * spu编码-规范设备的编码，可用于编程识别，慢慢过渡
     */
    public String spuCode;
    /**
     * 产品编号
     */
    public String productNumber;
    /**
     * （待删除）设备牌子Id
     */
    @Deprecated
    public long brandId;
    /**
     * 品牌代码
     */
    public String brandCode;
    /**
     * （待删除）设备类型ID
     */
    @Deprecated
    public long deviceTypeId;
    /**
     * 设备类型代码
     */
    public String typeCode;
    /**
     * 规格
     */
    public String spec;
    /**
     * 配置信息
     */
    public String config;
    /**
     * （待删除）支持最大功率
     */
    @Deprecated
    public double maxPower;
    /**
     * （待删除）限制充电功率
     */
    @Deprecated
    public double limitChargePower;
    /**
     * 预览图片
     */
    public String previewImage;
    /**
     * （待删除）主机：0=否 1=是
     */
    @Deprecated
    public int isHost;
    /**
     * （待删除）0-不显示，1-显示
     */
    @Deprecated
    public int display_status;
    /**
     * 应用通道编码
     */
    public String appChannelCode;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static DeviceUnitEntity getInstance() {
        return new DeviceUnitEntity();
    }

    /**
     * 根据单元ID查询设备单元数据
     *
     * @param deviceUnitId 设备单元id
     */
    public Map<String, Object> getWithUnitId(long deviceUnitId) {
        return DeviceUnitEntity.getInstance()
                .field("a.*,b.brandName,c.name as TypeName")
                .alias("a")
                .leftJoin(DeviceBrandEntity.getInstance().theTableName(), "b", "a.brandId = b.id")
                .rightJoin(DeviceTypeEntity.getInstance().theTableName(), "c", "a.deviceTypeId = c.id")
                .where("a.id", deviceUnitId)
                .find();
    }

    /**
     * 根据单元ID查询设备单元数据
     *
     * @param spu_code 设备SPU编码
     */
    public Map<String, Object> getBySpuCode(String spu_code) {
        return DeviceUnitEntity.getInstance()
                .field("a.*,b.brandName,c.name as TypeName")
                .alias("a")
                .leftJoin(DeviceBrandEntity.getInstance().theTableName(), "b", "a.brandId = b.id")
                .rightJoin(DeviceTypeEntity.getInstance().theTableName(), "c", "a.deviceTypeId = c.id")
                .where("a.spuCode", spu_code)
                .find();
    }

    /**
     * 根据单元ID查询设备单元数据集合
     *
     * @param deviceUnitIds 设备单元id集合
     */
    @Deprecated
    public Map<String, Map<String, Object>> getListWithUnitIds(List<Object> deviceUnitIds) {
        if (deviceUnitIds == null || deviceUnitIds.isEmpty()) return new LinkedHashMap<>();
        return DeviceUnitEntity.getInstance()
                .field("a.*,b.brandName,c.name as TypeName")
                .alias("a")
                .leftJoin(DeviceBrandEntity.getInstance().theTableName(), "b", "a.brandId = b.id")
                .rightJoin(DeviceTypeEntity.getInstance().theTableName(), "c", "a.deviceTypeId = c.id")
                .whereIn("a.id", deviceUnitIds)
                .selectForKey();
    }

    /**
     * 查询设备单元信息
     *
     * @param brandCode     品牌代码
     * @param productNumber 产品型号
     * @return 设备单元信息
     */
    public DeviceUnitEntity getWithBrandCode(String brandCode, String productNumber) {
        return getWithBrandCode(brandCode, productNumber, true);
    }

    /**
     * 查询设备单元信息
     *
     * @param brandCode     品牌代码
     * @param productNumber 产品型号
     * @param inCache       优先从缓存中获得
     * @return 设备单元信息
     */
    public DeviceUnitEntity getWithBrandCode(String brandCode, String productNumber, boolean inCache) {
        if (!StringUtils.hasLength(brandCode)) return null;
        if (!StringUtils.hasLength(productNumber)) return null;
        if (inCache) this.cache(String.format("BaseData:DeviceUnit:%s:%s", brandCode, productNumber));
        return this.where("brandCode", brandCode)
                .where("productNumber", productNumber)
                .findEntity();
    }

    /**
     * 查询设备单元信息
     *
     * @param productNumber 产品型号
     * @return 设备单元信息
     */
    public DeviceUnitEntity getWithProductNumber(String productNumber) {
        return getWithProductNumber(productNumber, true);
    }

    /**
     * 查询设备单元信息
     *
     * @param productNumber 产品型号
     * @param inCache       优先从缓存中获得
     * @return 设备单元信息
     */
    public DeviceUnitEntity getWithProductNumber(String productNumber, boolean inCache) {
        if (!StringUtils.hasLength(productNumber)) return null;
        if (inCache) this.cache(String.format("BaseData:DeviceUnit:%s", productNumber));
        return this.where("productNumber", productNumber).findEntity();
    }

    /**
     * 查询设备单元信息
     *
     * @param spuCode spu编码-规范设备的编码
     * @return 设备单元信息
     */
    public DeviceUnitEntity getWithSpuCode(String spuCode) {
        return getWithSpuCode(spuCode, true);
    }

    /**
     * 查询设备单元信息
     *
     * @param spuCode spu编码-规范设备的编码
     * @param inCache 优先从缓存中获得
     * @return 设备单元信息
     */
    public DeviceUnitEntity getWithSpuCode(String spuCode, boolean inCache) {
        if (!StringUtils.hasLength(spuCode)) return null;
        if (inCache) this.cache(String.format("BaseData:DeviceUnit:SPU:%s", spuCode));
        return this.where("spuCode", spuCode).findEntity();
    }
}
