package com.evcharge.entity.payment;

import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.HttpRequestUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付回调数据;
 * @author : JED
 * @date : 2022-9-26
 */
public class PaymentCallbackLogEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 订单编号
     */
    public String ordersn ;
    /**
     * 支付信息
     */
    public String content ;
    /**
     * ip地址
     */
    public String ip ;
    /**
     * 支付类型
     */
    public String paytype_id ;
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
    public static PaymentCallbackLogEntity getInstance() {
        return new PaymentCallbackLogEntity();
    }

    /**
     * 添加支付日志
     * @param orderSn
     * @param content
     */
    public void addLog(String orderSn,String content){
        Map<String,Object> data= new LinkedHashMap<>();
        data.put("ordersn",orderSn);
        data.put("content",content);
        data.put("ip", HttpRequestUtil.getIP());
        data.put("create_time", TimeUtil.getTimestamp());
        this.insert(data);
    }


}