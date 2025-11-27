package com.evcharge.entity.sys;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 系统从数据库链接信息;
 *
 * @author : JED
 * @date : 2024-5-7
 */
public class SysSlaveDBConnectEntity extends BaseEntity implements Serializable {

    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 别名，内部系统使用
     */
    public String aliasName;
    /**
     * 链接类型，read/write
     */
    public String connectType;
    /**
     * JdbcUrl
     */
    public String url;
    /**
     * 数据库登录名
     */
    public String username;
    /**
     * 数据库登录密码
     */
    public String password;
    /**
     * 数据表前缀
     */
    public String tableprefix;
    /**
     * 当从池中借出连接时，愿意等待多长时间。如果超时，将抛出 SQLException
     */
    public int connectionTimeout;
    /**
     * 一个连接在池里闲置多久时会被抛弃,当 minimumIdle < maximumPoolSize 才生效,默认值 600000 ms，最小值为 10000 ms，0表示禁用该功能
     */
    public int idleTimeout;
    /**
     * 当一个连接存活了足够久，HikariCP 将会在它空闲时把它抛弃, 默认 1800000  ms，最小值为 30000 ms，0 表示禁用该功能。
     */
    public int maxLifetime;
    /**
     * 池中最小容纳多少连接
     */
    public int minimumIdle;
    /**
     * 池中最多容纳多少连接（包括空闲的和在用的）
     */
    public int maximumPoolSize;
    /**
     * 是否开启预编译，开启预编译有性能提升
     */
    public int useServerPrepStmts;
    /**
     * 数据库是否区分大小写,0-表名将按指定方式存储，并且在对比表名时区分大小写,1-表名将以小写形式存储在磁盘上，在对比表名时不区分大小写,2-则表名按给定格式存储，但以小写形式进行比较
     */
    public int lower_case_table_names;
    /**
     * 状态：0-删除，1-启动
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
     */
    public static SysSlaveDBConnectEntity getInstance() {
        return new SysSlaveDBConnectEntity();
    }
}
