package com.evcharge.entity.ad.alipay;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 支付宝骑行险记录;
 * @author : Jay
 * @date : 2024-3-25
 */
public class AliPayCyclingInsuranceLogEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 用户id
     */
    public long uid ;
    /**
     * 支付宝openid
     */
    public String open_id ;
    /**
     * 任务唯一ID,幂等用
     */
    public String biz_id ;
    /**
     * 任务类型编码
     */
    public String task_type_code ;
    /**
     * 任务创建/领取时间. 格式: "yyyy-MM-dd HH:mm:ss"
     */
    public String apply_time ;
    /**
     * 用户完成任务的时间(仅仅是用户触发任务完成的时间,不包含服务端计费,发奖,消息通知等后置处理的完成) 格式: "yyyy-MM-dd HH:mm:ss"
     */
    public String finish_time ;
    /**
     * 广告ID
     */
    public String ad_id ;
    /**
     * 媒体展位spaceCode
     */
    public String space_code ;
    /**
     * 发奖金额(人民币单位分)
     */
    public String reward_number ;
    /**
     * 发奖数量(业务域单位)
     */
    public String reward_amount ;
    /**
     * 商家名/店铺名称
     */
    public String merchant_name ;
    /**
     * 商家logo
     */
    public String ad_merchant_logo ;
    /**
     * 任务标题
     */
    public String task_title ;
    /**
     * 副标题
     */
    public String sub_task_title ;
    /**
     * 任务描述
     */
    public String task_description ;
    /**
     * 扩展字段，其中包含嵌入式插件扩展参数中传入的 rtaExtMap，对应的 key='mediaRtaExtMap' 样例
     */
    public String extend_info ;
    /**
     * 状态
     */
    public int status ;
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
    public static AliPayCyclingInsuranceLogEntity getInstance() {
        return new AliPayCyclingInsuranceLogEntity();
    }
}