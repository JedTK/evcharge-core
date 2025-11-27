package com.evcharge.entity.inspect.station;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@TargetDB("inspect")
public class ChargeStationExtraInfoEntity extends BaseEntity implements Serializable {

//region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 站点uuid
     */
    public String station_uuid;
    /**
     * 充电桩模版信息id
     */
    public long template_id;
    /**
     * 实际值
     */
    public String data_value;
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
    public static ChargeStationExtraInfoEntity getInstance() {
        return new ChargeStationExtraInfoEntity();
    }


    /**
     * 获取额外信息
     * @param uuid 站点uuid
     * @return
     */
    public List<Map<String, Object>> getExtraInfo(String uuid){
        if (!StringUtil.hasLength(uuid)) {
            return null;
        }
        return this
                .field("a.*,b.name,b.description,b.unit,b.value_type,b.options")
                .alias("a")
                .leftJoin(ChargeStationInfoTemplateEntity.getInstance().theTableName(), "b","a.template_id=b.id")
                .where("station_uuid",uuid)
                .select();

    }


    /**
     * 创建模版
     * @param uuid 站点uuid
     */
    public void createTemplate(String uuid) {
        List<Map<String, Object>> list = ChargeStationInfoTemplateEntity.getInstance().getDefaultTemplate();

        if (list.isEmpty()) {
            return;
        }
        for (Map<String, Object> nd : list) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("template_id", MapUtil.getInt(nd, "id"));
            data.put("station_uuid", uuid);
            data.put("create_time", TimeUtil.getTimestamp());
            this.insert(data);
        }
    }


    /**
     * 更新模版信息
     *
     * @param uuid       站点uuid
     * @param templateId 模版id
     * @param dataValue  模版值
     */
    public void updateTemplateValue(String uuid, long templateId, String dataValue) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("data_value", dataValue);
        long r = this.where("station_uuid", uuid)
                .where("template_id", templateId)
                .update(data);

        if (r == 0) {
            new SyncResult(1, "更新失败");
            return;
        }
        new SyncResult(0, "更新成功");
    }
}
