package com.evcharge.entity.inspect.station;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.sys.SysAreaEntity;
import com.evcharge.entity.sys.SysCityEntity;
import com.evcharge.entity.sys.SysStreetEntity;
import com.evcharge.utils.TxMapToolsUtils;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 巡检站点信息
 */
@TargetDB("inspect")
public class ChargeStationEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 项目ID，自定义生成（根据省市区等生成）
     */

    /**
     * 暂定属性
     * 充电站属性 1=充电桩 2=充电柜 3=四轮慢充 4=四轮快充 参考SysUniversalEnum通用枚举表
     */
    public int station_attr;
    public String project_id;
    /**
     * uuid,项目唯一编码，充电联盟专用
     */
    public String uuid;
    /**
     * 地址的唯一值 用于确保 省市区地址经纬度不会修改
     */
    public String md5;
    /**
     * 充电桩唯一编号
     */
    public String CSId;
    /**
     * 充电桩编号
     */
    public long station_number;
    /**
     * 名称
     */
    public String name;
    /**
     * 省
     */
    public String province;
    /**
     * 省代码
     */
    public String province_code;
    /**
     * 市
     */
    public String city;
    /**
     * 市代码
     */
    public String city_code;
    /**
     * 区
     */
    public String district;
    /**
     * 区代码
     */
    public String district_code;
    /**
     * 街道，可能为空字串
     */
    public String street;
    /**
     * 街道代码
     */
    public String street_code;
    /**
     * 城市社区/乡村
     */
    public String communities;
    /**
     * 结构：0-无，1-棚，2-架
     */
    public long arch;
    /**
     * 路
     */
    public String roads;
    /**
     * 具体地址，门牌
     */
    public String address;
    /**
     * 经度
     */
    public double lon;
    /**
     * 纬度
     */
    public double lat;
    /**
     * 总的插座
     */
    public int total_socket;
    /**
     * 总的空闲插座
     */
    public int total_idle_socket;
    /**
     * 广告版数量
     */
    public int ad_panel_count;
    /**
     * 主图
     */
    public String main_image;
    /**
     * 正式上线时间
     */
    public long online_time;
    /**
     * 充电平台代码，表示此充电桩所属平台
     */
    public String platform_code;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 充电平台充电桩代码，表示此充电桩所在平台的唯一编码
     */
    public String platform_cs_id;
    /**
     * 充电桩分组id，关联充电桩，一般填写主站充电桩id
     */
    public String group_cs_id;
    /**
     * 备注
     */
    public String remark;
    /**
     * 状态：0=删除，1=运营中，2=建设中
     */
    public int status;
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
    public static ChargeStationEntity getInstance() {
        return new ChargeStationEntity();
    }


    /**
     * 根据uuid获取充电站信息
     *
     * @param uuid uuid
     * @return
     */
    public ChargeStationEntity getWithUUID(String uuid) {
        return getWithUUID(uuid,99, true);
    }
    /**
     * 根据uuid获取充电站信息
     *
     * @param uuid uuid
     * @return
     */
    public ChargeStationEntity getWithUUID(String uuid, int status) {
        return getWithUUID(uuid,status, true);
    }
    /**
     * 根据uuid获取充电站信息
     *
     * @param uuid    uuid
     * @param inCache 是否缓存
     * @return
     */
    public ChargeStationEntity getWithUUID(String uuid, int status, boolean inCache) {
        if (!StringUtils.hasLength(uuid)) return null;
        this.where("uuid", uuid);
        if(status!=99){
            this.where("status",status);
        }

        if (inCache) this.cache(String.format("ChargeStation:uuid:%s", uuid));
        return this.findEntity();
    }


    /**
     * 根据id获取充电站信息
     *
     * @param id uuid
     * @return
     */
    public ChargeStationEntity getWithId(long id) {
        return getWithId(id, true);
    }


    /**
     * 根据id获取充电站信息
     *
     * @param id      uuid
     * @param inCache 是否缓存
     * @return
     */
    public ChargeStationEntity getWithId(long id, boolean inCache) {
        if (id == 0) return null;
        this.where("id", id);
        if (inCache) this.cache(String.format("ChargeStation:id:%s", uuid));
        return this.findEntity();
    }

    public void updateInfo() {
        List<ChargeStationEntity> chargeStationEntities = ChargeStationEntity.getInstance()
                .selectList();
        if (chargeStationEntities.isEmpty()) {
            LogsUtil.error(this.getClass().getName(), "无站点数据，地址为空");

            return;
        }


        for (ChargeStationEntity ChargeStationEntity : chargeStationEntities) {
            Map<String, Object> data = new LinkedHashMap<>();
            SysCityEntity sysCityEntity = SysCityEntity.getInstance()
                    .where("city_name", ChargeStationEntity.city)
                    .findEntity();

            if (!StringUtils.hasLength(ChargeStationEntity.district_code)) {
                SysAreaEntity sysAreaEntity = SysAreaEntity.getInstance()
                        .where("city_code", sysCityEntity.city_code)
                        .where("area_name", ChargeStationEntity.district).findEntity();

                if (sysAreaEntity != null) {
                    data.put("district_code", sysAreaEntity.area_code);
                }
            }

            if (!StringUtils.hasLength(ChargeStationEntity.street_code)) {
                SysAreaEntity sysAreaEntity = SysAreaEntity.getInstance()
                        .where("area_name", ChargeStationEntity.district)
                        .findEntity();

                if (sysAreaEntity != null) {
                    SysStreetEntity sysStreetEntity = SysStreetEntity.getInstance()
                            .where("area_code", sysAreaEntity.area_code)
                            .where("street_name", ChargeStationEntity.street)
                            .findEntity();

                    if (sysStreetEntity != null) {
                        data.put("street_code", sysStreetEntity.street_code);
                    }
                }
            }

            if (ChargeStationEntity.lon == 0 || ChargeStationEntity.lat == 0) {
                String address = ChargeStationEntity.province + ChargeStationEntity.city + ChargeStationEntity.district + ChargeStationEntity.street + ChargeStationEntity.address;

                if (!StringUtils.hasLength(address)) {
                    LogsUtil.error(this.getClass().getName(), String.format("站点%s无法更新坐标，地址为空。", ChargeStationEntity.name));

                    continue;
                }
                SyncResult r = TxMapToolsUtils.getAddressPoi(address);
                if (r.code != 0) {
                    LogsUtil.error(this.getClass().getName(), String.format("站点%s无法更新坐标，失败原因:%s。", ChargeStationEntity.name, r.msg));
                    continue;
                }
                JSONObject jsonObject = (JSONObject) r.data;
                data.put("lon", jsonObject.getBigDecimal("lng"));
                data.put("lat", jsonObject.getBigDecimal("lat"));
            }

            if (!data.isEmpty()) {
                long i = getInstance().where("id", ChargeStationEntity.id).update(data);
                if (i == 0) {
                    LogsUtil.error(this.getClass().getName(), String.format("站点%s无法更新数据，更新数据:%s。", ChargeStationEntity.name, data));
                }
                LogsUtil.info(this.getClass().getName(), String.format("站点%s更新数据成功，更新数据:%s。", ChargeStationEntity.name, data));

            } else {
                LogsUtil.error(this.getClass().getName(), String.format("站点%s无法更新数据，更新数据为空。", ChargeStationEntity.name));
            }
        }


    }

}
