package com.evcharge.enumdata;

/**
 * SPU+SKU资源图应用范围类型
 */
public enum ESpkuResApplyType {
    /**
     * 主图
     */
    MainImage(1),
    /**
     * 主缩略图
     */
    MainThumbnail(2),
    /**
     * 预览图
     */
    PreviewImage(3),
    /**
     * 详情图
     */
    DetailImage(4),
    /**
     * 其他
     */
    Other(999);
    public final int index;

    ESpkuResApplyType(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return String.valueOf(this.index);
    }
}
