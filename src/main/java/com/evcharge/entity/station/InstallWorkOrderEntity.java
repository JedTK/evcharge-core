package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Convert;
import com.xyzs.utils.HttpRequestUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 安装工单;
 *
 * @author : JED
 * @date : 2022-10-18
 */
public class InstallWorkOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 工单订单号
     */
    public String OrderSN;
    /**
     * 安装工作人员id
     */
    public long installer_id;
    /**
     * 充电站ID
     */
    public String CSId;
    /**
     * 状态：0=待确认，1=已确认，2=已撤销，3=进行中，4=竣工
     */
    public int status;
    /**
     * 创建者ID
     */
    public long creator_id;
    /**
     * 创建IP
     */
    public String create_ip;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static InstallWorkOrderEntity getInstance() {
        return new InstallWorkOrderEntity();
    }


//    /**
//     * 创建订单
//     *
//     * @param CSId        站点ID
//     * @param installerId 安装ID
//     */
//    public SyncResult createOrder(String CSId, long installerId) {
//        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance()
//                .where("id", CSId)
//                .findEntity();
//        if (chargeStationEntity == null || chargeStationEntity.id == 0) {
//            return new SyncResult(1, "查询不到此充电桩数据");
//        }
//        //状态：0=删除，1=正常，2=在建
//        if (chargeStationEntity.status != 2) {
//            return new SyncResult(1, "此充电桩已在运行中，无需要下安装工单");
//        }
//
//        InstallWorkOrderEntity orderEntity = InstallWorkOrderEntity.getInstance();
//        if (orderEntity.where("CSId", Convert.toLong(CSId)).exist()) {
//            return new SyncResult(1, "已存在工单，无需再创建");
//        }
//
//        orderEntity.OrderSN = common.randomStr(2).toUpperCase()
//                + TimeUtil.toTimeString("yyyyMMddHHmmssSSS")
//                + common.randomInt(100, 999);
//        orderEntity.installer_id = installerId;
//        orderEntity.CSId = CSId;
//        orderEntity.status = 1;//状态：0=待确认，1=已确认，2=已撤销，3=进行中，4=竣工
//        orderEntity.creator_id = 1;//默认1
//        orderEntity.create_ip = HttpRequestUtil.getIP();
//        orderEntity.create_time = TimeUtil.getTimestamp();
//        if (orderEntity.insert() == 0) {
//            return new SyncResult(1, "创建工单失败");
//        }
//        return new SyncResult(0, "创建工单成功");
//    }
}
