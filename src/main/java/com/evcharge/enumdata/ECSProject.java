package com.evcharge.enumdata;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 充电桩项目立项枚举
 */
public class ECSProject {

    /**
     * 项目状态
     */
    public enum PStatus {
        /**
         * 待审核
         */
        PendingApproval(0),
        /**
         * 活动
         */
        Active(1),
        /**
         * 暂停
         */
        Paused(2),
        /**
         * 已完成
         */
        Completed(3),
        /**
         * 已取消
         */
        Cancelled(4),
        /**
         * 已删除
         */
        Deleted(5),
        /**
         * 未定义
         */
        None(0);

        public final int index;

        private static Map<Integer, PStatus> map = Stream.of(PStatus.values())
                .collect(Collectors.toMap(e -> e.index, e -> e));

        PStatus(int index) {
            this.index = index;
        }

        public static PStatus valueOf(int index) {
            PStatus v = map.get(index);
            if (v == null) return None;
            return v;
        }

        @Override
        public String toString() {
            return String.valueOf(this.index);
        }
    }

    /**
     * 项目进度状态
     */
    public enum PProgress {
        /**
         * 待审批
         */
        PendingApproval(1),
        /**
         * 已批准
         */
        Approved(2),
        /**
         * 规划中
         */
        Planning(3),
        /**
         * 招标中
         */
        Bidding(4),
        /**
         * 合同签署
         */
        ContractSigned(5),
        /**
         * 施工准备
         */
        PreConstruction(6),
        /**
         * 施工中
         */
        UnderConstruction(7),
        /**
         * 待验收
         */
        AwaitingInspection(8),
        /**
         * 验收不通过
         */
        InspectionFailed(9),
        /**
         * 验收通过
         */
        InspectionPassed(10),
        /**
         * 维护中
         */
        Maintenance(11),
        /**
         * 未定义
         */
        None(0);

        public final int index;

        private static Map<Integer, PProgress> map = Stream.of(PProgress.values())
                .collect(Collectors.toMap(e -> e.index, e -> e));

        PProgress(int index) {
            this.index = index;
        }

        public static PProgress valueOf(int index) {
            PProgress v = map.get(index);
            if (v == null) return None;
            return v;
        }

        @Override
        public String toString() {
            return String.valueOf(this.index);
        }
    }
}
