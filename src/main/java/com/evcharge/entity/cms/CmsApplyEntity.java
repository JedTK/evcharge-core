package com.evcharge.entity.cms;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 报名相关;
 * @author : Jay
 * @date : 2022-11-29
 */
public class CmsApplyEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 站点id,;
     */
    public String CSId;
    /**
     * 设备物理编号,;
     */
    public String device_number;
    /**
     * 省份
     */
    public String province ;
    /**
     * 市
     */
    public String city ;
    /**
     * 地区
     */
    public String district ;
    /**
     * 标题
     */
    public String title ;
    /**
     * 内容
     */
    public String content ;
    /**
     * 姓名
     */
    public String real_name ;
    /**
     * 手机号码
     */
    public String phone ;
    /**
     * 类型;install_contact-安装联系 Cooperation-合作加盟
     */
    public String type ;
    /**
     * 额外参数
     */
    public String form_param ;
    /**
     * 备注信息
     */
    public String memo ;
    /**
     * 图片资源,;
     */
    public String image_url;
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
    public static CmsApplyEntity getInstance() {
        return new CmsApplyEntity();
    }
}