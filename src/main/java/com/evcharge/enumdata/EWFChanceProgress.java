package com.evcharge.enumdata;

/**
 * 充电桩立项商机（拟建）
 */
public enum EWFChanceProgress {
    PREPARATION("拟建"),
    ANNOUNCEMENT("公示中"),
    PENDING_APPROVAL("待盖章"),
    UNDER_CONSTRUCTION("正在建设"),
    SUSPENDED("暂缓");

    private final String description;

    EWFChanceProgress(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static EWFChanceProgress fromDescription(String description) {
        for (EWFChanceProgress status : EWFChanceProgress.values()) {
            if (status.getDescription().equals(description)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No ProgressStatus with description " + description + " found");
    }
}
