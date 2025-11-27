package com.evcharge.entity.agent.agent;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 代理信息汇总表;
 *
 * @author : Jay
 * @date : 2025-2-17
 */
@TargetDB("evcharge_agent")
public class AgentSummaryV1Entity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 用户ID
     */
    public long admin_id;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 当前收益
     */
    public BigDecimal income;
    /**
     * 总收益
     */
    public BigDecimal total_income;
    /**
     * 总提现金额
     */
    public BigDecimal withdraw_total_amount;
    /**
     * 冻结金额
     */
    public BigDecimal freeze_amount;
    /**
     * 备注
     */
    public String remark;
    /**
     * 状态
     */
    public int status;
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
    public static AgentSummaryV1Entity getInstance() {
        return new AgentSummaryV1Entity();
    }

    private final static String TAG = "元气充代理-数据汇总v1";


    /**
     * 初始化代理统计
     * @param adminId long
     * @param organizeCode String
     */
    public void initSummary(long adminId, String organizeCode) {
        //查询代理是否已经初始化
        if(this.where("organize_code",organizeCode)!=null){
            return;
        }

        //没有该用户，则新建
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("admin_id", adminId);
        data.put("organize_code", organizeCode);
        data.put("create_time", TimeUtil.getTimestamp());
//        DataService.getMainCache().set("IsUserInitSummary",1);
        this.insert(data);
    }


}