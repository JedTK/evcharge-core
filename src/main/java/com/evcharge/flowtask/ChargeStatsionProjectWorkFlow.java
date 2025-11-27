package com.evcharge.flowtask;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.FlowTask;
import com.evcharge.entity.workflow.WorkFlowEntity;
import com.evcharge.entity.workflow.WorkFlowStepEntity;
import com.xyzs.entity.SyncResult;

/**
 * 充电桩立项工作流程
 */
public class ChargeStatsionProjectWorkFlow {

    @FlowTask(id = "start", desc = "商务部:发起立项")
    public SyncResult start(WorkFlowEntity workFlowEntity
            , WorkFlowStepEntity stepEntity
            , JSONObject jsonObject
            , long op_adminId) {

        return new SyncResult(0, "");
    }

    @FlowTask(id = "Task_1", desc = "商务部:补充资料")
    public SyncResult Task_1(long adminId, JSONObject jsonObject) {

        return new SyncResult(0, "");
    }

    @FlowTask(id = "Task_2", desc = "设计部:开始设计工作")
    public SyncResult Task_2(long adminId, JSONObject jsonObject) {

        return new SyncResult(0, "");
    }

    @FlowTask(id = "Task_3", desc = "工程部:场地勘察")
    public SyncResult Task_3(long adminId, JSONObject jsonObject) {

        return new SyncResult(0, "");
    }

    @FlowTask(id = "Task_4", desc = "内务部:准备物料")
    public SyncResult create_task(long adminId, JSONObject jsonObject) {

        return new SyncResult(0, "");
    }


    @FlowTask(id = "Task_5", desc = "商务部:提交项目申请")
    public SyncResult Task_5(long adminId, JSONObject jsonObject) {

        return new SyncResult(0, "");
    }


    @FlowTask(id = "done", desc = "总务部:项目完成")
    public SyncResult done(long adminId, JSONObject jsonObject) {

        return new SyncResult(0, "");
    }
}
