package com.evcharge.service.Agent.agent;

import com.evcharge.entity.agent.agent.AgentToStationEntity;
import com.xyzs.entity.SyncResult;

import java.util.List;

public class AgentService {


    public SyncResult task(String organizeCode){
        //按照组织
        List<AgentToStationEntity> list = AgentToStationEntity.getInstance()
                .where("organize_code",organizeCode)
                .where("status",0)
                .selectList();


        if(list.isEmpty()) return new SyncResult(1,"没有绑定站点信息");


//        for (AgentToStationEntity agentToStationEntity :list){
//
//        }






        return new SyncResult(1,"");
    }


    //按日统计，从上线到现在
    //固定时间范围，开始时间-结束时间
    //从充电订单统计
    //计算分账比例
    //写入数据库
    //更新收益表




}
