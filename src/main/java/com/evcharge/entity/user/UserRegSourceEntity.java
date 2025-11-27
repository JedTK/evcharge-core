package com.evcharge.entity.user;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 注册来源;
 * @author : Jay
 * @date : 2022-9-15
 */
public class UserRegSourceEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    private long id ;
    /**
     * 标题
     */
    private String title ;
    /**
     * appid
     */
    private String appId ;
    /**
     * 创建时间戳
     */
    private long createTime ;

    /** id */
    public long getId(){
        return this.id;
    }
    /** id */
    public void setId(long id){
        this.id=id;
    }
    /** 标题 */
    public String getTitle(){
        return this.title;
    }
    /** 标题 */
    public void setTitle(String title){
        this.title=title;
    }
    /** appid */
    public String getAppId(){
        return this.appId;
    }
    /** appid */
    public void setAppId(String appId){
        this.appId=appId;
    }
    /** 创建时间戳 */
    public long getCreateTime(){
        return this.createTime;
    }
    /** 创建时间戳 */
    public void setCreateTime(long createTime){
        this.createTime=createTime;
    }
    //endregion
}
