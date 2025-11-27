package com.evcharge;

import com.evcharge.entity.sys.SysSlaveDBConnectEntity;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.database.mysql.MysqlDB;
import com.xyzs.database.mysql.MysqlDBConnect;
import com.xyzs.database.mysql.MysqlDBReadConnect;
import com.xyzs.database.mysql.MysqlDBWriteConnect;
import com.xyzs.entity.DataService;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * 服务管理器，用于管理数据库连接
 */
public class ServiceManager {
    // 日志标签
    private final static String TAG = ServiceManager.class.getSimpleName();

    /**
     * 利用Caffeine高性能缓存存储读连接
     */
    private final static Cache<String, MysqlDBReadConnect> mysqlDBReadConnect = Caffeine.newBuilder()
            .maximumSize(100)
            .build();

    /**
     * 利用Caffeine高性能缓存存储读连接
     */
    private final static Cache<String, MysqlDBWriteConnect> mysqlDBWriteConnect = Caffeine.newBuilder()
            .maximumSize(100)
            .build();

    /**
     * 注册主数据库
     *
     * @return 主数据库连接对象
     */
    public static ISqlDBObject regMainDB() {
        // 返回主数据库连接对象
        return new MysqlDB(MysqlDBReadConnect.getInstance(), MysqlDBWriteConnect.getInstance());
    }

    /**
     * 注册主/从数据库
     *
     * @param dbAliasName 数据库别名
     * @return 数据库连接对象
     */
    public static ISqlDBObject regDB(String dbAliasName) {
        // 如果数据库别名为空，则判断为主数据库
        if (!StringUtils.hasLength(dbAliasName)) {
            return regMainDB();
        }

        // 获取数据库连接对象
        MysqlDBReadConnect readConnect = mysqlDBReadConnect.get(dbAliasName, c -> null);
        MysqlDBWriteConnect writeConnect = mysqlDBWriteConnect.get(dbAliasName, c -> null);

        // 如果已存在连接，则返回连接对象
        if (readConnect != null && writeConnect != null) {
            return new MysqlDB(readConnect, writeConnect);
        }

        // 否则，从数据库获取连接信息
        ISqlDBObject dbObject = Objects.requireNonNull(DataService.getMainDB());
        dbObject.table(SysSlaveDBConnectEntity.getInstance().theTableName())
                .where("status", 1)
                .where("aliasName", dbAliasName)
                .order("id");//状态：0-删除，1-启动
        Map<String, Map<String, Object>> connectionData = dbObject.selectForKey("connectType");

        // 如果连接信息为空，则切换至主数据库
        if (connectionData == null || connectionData.isEmpty()) {
            LogsUtil.warn(TAG, "%s - 无法获取从数据信息,已切换主数据库...", dbAliasName);
            return new MysqlDB(MysqlDBReadConnect.getInstance(), MysqlDBWriteConnect.getInstance());
        }

        // 解析连接信息并创建连接对象
        readConnect = parseConnection(connectionData.get("read"), new MysqlDBReadConnect());
        writeConnect = parseConnection(connectionData.get("write"), new MysqlDBWriteConnect());

        // 检查连接信息是否完整
        if (readConnect == null || !StringUtils.hasLength(readConnect.url)) {
            LogsUtil.warn(TAG, "%s - 数据库遵从读写分离，当前数据库信息没有配置[读]链接信息", dbAliasName);
            return regMainDB();
        }
        if (writeConnect == null || !StringUtils.hasLength(writeConnect.url)) {
            LogsUtil.warn(TAG, "%s - 数据库遵从读写分离，当前数据库信息没有配置[写]链接信息", dbAliasName);
            return regMainDB();
        }

        // 缓存连接对象
        mysqlDBReadConnect.put(dbAliasName, readConnect);
        mysqlDBWriteConnect.put(dbAliasName, writeConnect);

        LogsUtil.info(TAG, "%s - 成功创建读/写实列", dbAliasName);
        return new MysqlDB(readConnect, writeConnect);
    }

    /**
     * 解析连接信息并创建连接对象
     *
     * @param data    连接信息数据
     * @param connect 连接对象
     * @param <T>     连接对象类型
     * @return 连接对象
     */
    private static <T extends MysqlDBConnect> T parseConnection(Map<String, Object> data, T connect) {
        if (data == null) return null;
        connect.url = MapUtil.getString(data, "url");
        connect.username = MapUtil.getString(data, "username");
        connect.password = MapUtil.getString(data, "password");
        connect.prefix = MapUtil.getString(data, "tableprefix");
        connect.connectionTimeout = MapUtil.getInt(data, "connectionTimeout");
        connect.idleTimeout = MapUtil.getInt(data, "idleTimeout");
        connect.maxLifetime = MapUtil.getInt(data, "maxLifetime");
        connect.minimumIdle = MapUtil.getInt(data, "minimumIdle");
        connect.maximumPoolSize = MapUtil.getInt(data, "maximumPoolSize");
        connect.useServerPrepStmts = MapUtil.getInt(data, "useServerPrepStmts") == 1;
        connect.lower_case_table_names = MapUtil.getInt(data, "lower_case_table_names");
        return connect;
    }
}