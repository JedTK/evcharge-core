package com.evcharge.entity.admin;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 管理员信息表;
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class AdminSecretEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id ;
    /**
     * 管理员ID
     */
    public long admin_id ;
    /**
     * 加密的密码：md5(md5(password)+password_secret)
     */
    public String password ;
    /**
     * 密码加密的密钥
     */
    public String password_secret ;

    //endregion

    public static AdminSecretEntity getInstance(){
        return new AdminSecretEntity();
    }
}
