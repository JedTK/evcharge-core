package com.evcharge.DBSyncer.core;

/**
 * 同步数据表 用到的字段映射结构
 */
public class DBSyncTableField {
    /**
     * 主表字段
     */
    public String mainField;
    /**
     * 子表字段
     */
    public String slaveField;

    public DBSyncTableField(String mainField, String slaveField) {
        this.mainField = mainField;
        this.slaveField = slaveField;
    }
}
