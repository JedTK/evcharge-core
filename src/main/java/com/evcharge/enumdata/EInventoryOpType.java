package com.evcharge.enumdata;

/**
 * 仓库出入库操作类型
 */
public enum EInventoryOpType {
    /**
     * 入库
     */
    STORAGE(1),

    /**
     * 出库
     */
    TAKEOUT(2),

    /**
     * 盘点
     */
    REVISE(3);


    public final int index;

    EInventoryOpType(int index) {
        this.index = index;
    }

    public static EInventoryOpType valueOf(int index) {
        EInventoryOpType v;
        switch (index) {
            case 1:
                v = STORAGE;
                break;
            case 2:
                v = TAKEOUT;
                break;
            case 3:
                v = REVISE;
                break;
            default:
                throw new IllegalArgumentException("argument out of range");
        }
        return v;
    }

    @Override
    public String toString() {
        return String.valueOf(this.index);
    }
}
