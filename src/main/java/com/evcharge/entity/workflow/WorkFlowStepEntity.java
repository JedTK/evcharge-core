package com.evcharge.entity.workflow;


import com.xyzs.entity.BaseEntity;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * 工作流步骤;
 *
 * @author : JED
 * @date : 2023-10-17
 */
public class WorkFlowStepEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 工作流id
     */
    public long workflow_id;
    /**
     * 步骤，排序使用
     */
    public int step;
    /**
     * 步骤代码，排序使用
     */
    public String step_code;
    /**
     * 任务名，与函数绑定
     */
    public String task_name;
    /**
     * 说明
     */
    public String desc;
    /**
     * 父级任务
     */
    public String parentTask;
    /**
     * 上一个任务
     */
    public String previousTask;
    /**
     * 下一个任务
     */
    public String nextTask;
    /**
     * 是否允许忽略，0-允许，1-不允许
     */
    public int ignore;
    /**
     * 角色
     */
    public String role;
    /**
     * 权限
     */
    public String permission;
    /**
     * 状态：0-未完成，1-已完成
     */
    public int status;
    /**
     * 重复提交，0-不允许，1-允许
     */
    public int repeatedSubmit;
    /**
     * 回退重置状态,0-否，1-是
     */
    public int backResetStatus;
    /**
     * 回退原因
     */
    public String backReason;
    /**
     * 操作管理管理员id
     */
    public long op_admin_id;
    /**
     * 操作管理员姓名
     */
    public String op_admin_name;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static WorkFlowStepEntity getInstance() {
        return new WorkFlowStepEntity();
    }

    /**
     * 根据任务名和工作流id获取任务步骤
     *
     * @param task_name   任务吗
     * @param workflow_id 工作流id
     * @return
     */
    public WorkFlowStepEntity getWithTaskName(String task_name, long workflow_id) {
        if (!StringUtils.hasLength(task_name)) {
            return WorkFlowStepEntity.getInstance()
                    .where("workflow_id", workflow_id)
                    .order("step")
                    .findModel();
        }
        return WorkFlowStepEntity.getInstance()
//                .cache(String.format("WorkFlow:%s:Step:%s", workflow_id, task_name))
                .where("workflow_id", workflow_id)
                .where("task_name", task_name)
                .order("step")
                .findModel();
    }
}