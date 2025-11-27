package com.evcharge.entity.inspect.station;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
/**
 * 站点与nfc绑定表;
 *
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("inspect")
public class ChargeStationToNFCEntity extends BaseEntity implements Serializable {

    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 站点uuid
     */
    public String cs_uuid;
    /**
     * nfc编码
     */
    public String nfc_code;
    /**
     * 备注
     */
    public String remark ;
    /**
     * 状态
     */
    public int status;
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
    public static ChargeStationToNFCEntity getInstance() {
        return new ChargeStationToNFCEntity();
    }


    /**
     * 获取nfcx信息
     * @param uuid uuid
     * @return
     */
    public ChargeStationToNFCEntity findNfcBYUUID(String uuid) {
        return this
                .cache(String.format("ChargeStation:NFC:%s",uuid),86400*1000)
                .where("cs_uuid", uuid)
                .where("status",0)
                .findEntity();
    }
}
