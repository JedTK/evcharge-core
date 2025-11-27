package com.evcharge.entity.inspect.station;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 充电站消防物料对照表;
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("inspect")
public class ChargeStationFireSafetyEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 充电站ID
     */
    public String cs_uuid ;
    /**
     * 使用的模板ID
     */
    public long template_id ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ChargeStationFireSafetyEntity getInstance() {
        return new ChargeStationFireSafetyEntity();
    }


    public ChargeStationFireSafetyEntity getWithUUID(String uuid){
        return getWithUUID(uuid, true);
    }

    public ChargeStationFireSafetyEntity getWithUUID(String uuid, boolean inCache) {
        if (!StringUtils.hasLength(uuid)) return null;
        this.where("cs_uuid", uuid);
        if (inCache) this.cache(String.format("ChargeStation:FireSafety:uuid:%s", uuid));
        return this.findEntity();
    }


    /**
     * 复制信息
     * @param uuid 目标uuid
     * @param newUUID 新uuid
     * @return
     */
    public SyncResult copy(String uuid, String newUUID){
        ChargeStationFireSafetyEntity chargeStationFireSafetyEntity=getWithUUID(uuid);


        return DataService.getMainDB().beginTransaction(connection -> {
            Map<String,Object> data=new LinkedHashMap<>();
            data.put("cs_uuid",newUUID);
            data.put("template_id",chargeStationFireSafetyEntity.template_id);
            data.put("create_time", TimeUtil.getTimestamp());
            long safetyId= ChargeStationFireSafetyEntity.getInstance().insertGetIdTransaction(connection,data);

            if(safetyId==0){
                return new SyncResult(1,"复制失败，ChargeStationFireSafety新增失败");
            }

            List<ChargeStationFireSafetyDetailEntity> list= ChargeStationFireSafetyDetailEntity.getInstance()
                    .where("safety_id",chargeStationFireSafetyEntity.id).selectList();

            if(list.isEmpty()){
                return new SyncResult(1,"复制失败。ChargeStationFireSafetyDetail列表为空");
            }

            for (ChargeStationFireSafetyDetailEntity nd:list){
                Map<String,Object> detail=new LinkedHashMap<>();

                detail.put("safety_id",safetyId);
                detail.put("material_id",nd.material_id);
                detail.put("data_value",nd.data_value);
                detail.put("create_time",TimeUtil.getTimestamp());

                long detailId=  ChargeStationFireSafetyDetailEntity.getInstance().insertGetIdTransaction(connection,detail);

                if(detailId==0){
                    return new SyncResult(1,"复制失败，ChargeStationFireSafetyDetail新增失败");
                }
            }
            return new SyncResult(0,"success");
        });




    }
}
