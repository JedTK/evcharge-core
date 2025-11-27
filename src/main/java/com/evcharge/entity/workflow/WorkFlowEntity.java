package com.evcharge.entity.workflow;


import com.evcharge.entity.admin.AdminBaseEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流;
 *
 * @author : JED
 * @date : 2023-10-17
 */
public class WorkFlowEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 工作流标题
     */
    public String title;
    /**
     * 描述
     */
    public String description;
    /**
     * 模板id，值为0时为主模板，其他值为复制其他模板
     */
    public long template_id;
    /**
     * 类名，例如：com.evcharge.flowtask.newTask
     */
    public String class_name;
    /**
     * 当前进度任务
     */
    public String progressTask;
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
     *
     * @return
     */
    public static WorkFlowEntity getInstance() {
        return new WorkFlowEntity();
    }

    /**
     * 通过项目id获取工作流数据
     *
     * @param workflow_id 工作流id
     * @return 工作流
     */
    public WorkFlowEntity getWorkFlowWithId(long workflow_id) {
        return getWorkFlowWithId(workflow_id, true);
    }

    /**
     * 通过项目id获取工作流数据
     *
     * @param workflow_id 工作流id
     * @param inCache     是否优先从缓冲中获取
     * @return 工作流
     */
    public WorkFlowEntity getWorkFlowWithId(long workflow_id, boolean inCache) {
        WorkFlowEntity workFlowEntity = new WorkFlowEntity();
        if (inCache) workFlowEntity.cache(String.format("WorkFlow:%s", workflow_id));
        workFlowEntity.findEntity(workflow_id);
        return workFlowEntity;
    }

    /**
     * 创建工作流
     *
     * @param  templateId 模版id
     * @return
     */
    public SyncResult createTransaction(Connection connection, String title, long templateId) throws SQLException, IllegalAccessException {
        if (templateId == 0) return null;
        WorkFlowEntity workFlowEntity = WorkFlowEntity.getInstance().findEntity(templateId);
        if (workFlowEntity == null) return new SyncResult(2, "无效的模板id");

        workFlowEntity.id = 0;
        workFlowEntity.title = title;
        workFlowEntity.description = "";
        workFlowEntity.template_id = templateId;
        workFlowEntity.progressTask = "";
        workFlowEntity.create_time = TimeUtil.getTimestamp();
        workFlowEntity.update_time = workFlowEntity.create_time;
        workFlowEntity.id = workFlowEntity.insertGetIdTransaction(connection);
        if (workFlowEntity.id == 0) return new SyncResult(3, "新增工作流数据失败");

        List<WorkFlowStepEntity> list = WorkFlowStepEntity.getInstance()
                .where("workflow_id", templateId)
                .selectList();
        if (list == null || list.isEmpty()) return new SyncResult(4, "无法新增工作流程");

        for (int i = 0; i < list.size(); i++) {
            WorkFlowStepEntity stepEntity = list.get(i);

            stepEntity.id = 0;
            if (i == 0) stepEntity.status = 1;//状态：0-未完成，1-已完成
            stepEntity.workflow_id = workFlowEntity.id;
            stepEntity.op_admin_id = 0;
            stepEntity.create_time = TimeUtil.getTimestamp();
            stepEntity.update_time = stepEntity.create_time;
            int noQuery = stepEntity.insertTransaction(connection);
            if (noQuery == 0) return new SyncResult(5, "新增工作流程步骤过程中发生错误");
        }
        return new SyncResult(0, "", workFlowEntity);
    }

    /**
     * 检查上一级任务是否已完成
     *
     * @param task_name 任务名
     * @return
     */
    public SyncResult checkPreviousTask(String task_name) {
        //检查上级任务还没完成的数量
        List<Map<String, Object>> list = WorkFlowStepEntity.getInstance()
//                .cache(String.format("WorkFlow:%s:Step:%s:Previous", this.id, task_name))
                .field("id,task_name,desc")
                .where("workflow_id", this.id)
                .where("nextTask", task_name)
                .where("`ignore`", 1) //是否允许忽略，0-允许，1-不允许
                .where("status", 0)
                .order("step")
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(0, "");

        StringBuilder msg = new StringBuilder("无法提交");
        for (Map<String, Object> nd : list) {
            msg.append(String.format("[%s] ", MapUtil.getString(nd, "desc")));
        }
        msg.append("还没有完成");
        return new SyncResult(1, msg.toString());
    }

    /**
     * 检查子任务是否完成
     *
     * @param task_name 任务名
     * @return
     */
    public SyncResult checkSubTask(String task_name) {
        WorkFlowStepEntity stepEntity = WorkFlowStepEntity.getInstance().getWithTaskName(task_name, this.id);
        //判断是否允许前进: 检查是否有子任务
        int subTaskCount = WorkFlowStepEntity.getInstance()
                .where("workflow_id", this.id)
                .where("parentTask", stepEntity.task_name)
                .where("`ignore`", 1) //是否允许忽略，0-允许，1-不允许
                .count();
        if (subTaskCount > 0) {
            //获取还没
            List<Map<String, Object>> list = WorkFlowStepEntity.getInstance()
                    .field("id,task_name,desc")
                    .where("workflow_id", this.id)
                    .where("parentTask", stepEntity.task_name)
                    .where("`ignore`", 1) //是否允许忽略，0-允许，1-不允许
                    .where("status", 0)
                    .select();
            if (list == null || list.isEmpty()) return new SyncResult(0, "");

            StringBuilder msg = new StringBuilder("无法提交");
            for (Map<String, Object> nd : list) {
                msg.append(String.format("[%s] ", MapUtil.getString(nd, "desc")));
            }
            msg.append("还没有完成");
            return new SyncResult(1, msg.toString());
        }
        return new SyncResult(0, "");
    }

    /**
     * 检查重复提交
     *
     * @param task_name 任务名
     * @return
     */
    public SyncResult checkRepeatedSubmit(String task_name) {
        WorkFlowStepEntity stepEntity = WorkFlowStepEntity.getInstance().getWithTaskName(task_name, this.id);
        //状态：0-未完成，1-已完成
        if (stepEntity.status == 1 && stepEntity.repeatedSubmit == 0) {
            return new SyncResult(100, "任务已完成，不能重复提交");
        }
        if (StringUtils.hasLength(stepEntity.nextTask)) {
            //检查下个任务是否完成
            WorkFlowStepEntity nextStep = WorkFlowStepEntity.getInstance().getWithTaskName(stepEntity.nextTask, this.id);
            //表示下个任务已经进行了，无需要再提交
            if (nextStep.status == 1) return new SyncResult(101, "任务已完成，无需重复提交");
        }
        return new SyncResult(0, "");
    }

    /**
     * 完成一个任务
     *
     * @param task_name   任务名
     * @param op_admin_id 操作管理员id
     * @return 同步结果
     */
    public SyncResult doneTask(String task_name, long op_admin_id) {
        if (!StringUtils.hasLength(task_name)) return new SyncResult(2, "任务名不能为空");
        WorkFlowEntity workFlowEntity = getWorkFlowWithId(this.id);
        if (workFlowEntity == null) return new SyncResult(100, "无效的工作流");

        WorkFlowStepEntity step = WorkFlowStepEntity.getInstance().getWithTaskName(task_name, this.id);
        if (step == null) return new SyncResult(101, "无效的工作步骤");

        AdminBaseEntity adminBaseEntity = AdminBaseEntity.getInstance().getWithId(op_admin_id);
        step.update(step.id, new LinkedHashMap<>() {{
            put("status", 1);//状态：0-未完成，1-已完成
            put("op_admin_id", op_admin_id);
            put("op_admin_name", adminBaseEntity.last_name + adminBaseEntity.first_name);
            put("update_time", TimeUtil.getTimestamp());
        }});
        initCache().del(String.format("WorkFlow:%s:Step:%s", this.id, task_name));
        return new SyncResult(1, "");
    }

    /**
     * 回退任务
     *
     * @param task_name   任务名
     * @param op_admin_id 操作管理员id
     * @param backReason  回退原因
     * @return 同步结果
     */
    public SyncResult backTask(String task_name, long op_admin_id, String backReason) {
        if (!StringUtils.hasLength(task_name)) return new SyncResult(2, "任务名不能为空");
        WorkFlowEntity workFlowEntity = getWorkFlowWithId(this.id);
        if (workFlowEntity == null) return new SyncResult(100, "无效的工作流");

        WorkFlowStepEntity step = WorkFlowStepEntity.getInstance().getWithTaskName(task_name, this.id);
        if (step == null) return new SyncResult(101, "无效的工作步骤");

        if (!StringUtils.hasLength(step.previousTask)) return new SyncResult(1, "无需回退");

        WorkFlowStepEntity previousStep = WorkFlowStepEntity.getInstance().getWithTaskName(step.previousTask, this.id);
        //回退重置状态
        if (previousStep.backResetStatus == 1) {
            previousStep.update(previousStep.id, new LinkedHashMap<>() {{
                put("status", 0);//状态：0-未完成，1-已完成
                put("update_time", TimeUtil.getTimestamp());
            }});
            initCache().del(String.format("WorkFlow:%s:Step:%s", this.id, previousStep.task_name));
        }

        AdminBaseEntity adminBaseEntity = AdminBaseEntity.getInstance().getWithId(op_admin_id);
        step.update(step.id, new LinkedHashMap<>() {{
            put("status", 0);//状态：0-未完成，1-已完成
            put("op_admin_id", op_admin_id);
            put("op_admin_name", adminBaseEntity.last_name + adminBaseEntity.first_name);
            put("backReason", backReason);
            put("update_time", TimeUtil.getTimestamp());
        }});
        initCache().del(String.format("WorkFlow:%s:Step:%s", this.id, task_name));
        return new SyncResult(1, "");
    }
}
