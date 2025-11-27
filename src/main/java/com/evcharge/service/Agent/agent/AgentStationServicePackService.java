package com.evcharge.service.Agent.agent;

import com.evcharge.entity.agent.config.AgentStationServicePackConfigEntity;
import com.evcharge.entity.agent.agent.AgentStationServicePackFeeEntity;
import com.evcharge.entity.agent.config.ServicePackConfigEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class AgentStationServicePackService {

    public static final String BILLING_CYCLE_DAY = "day";
    public static final String BILLING_CYCLE_MONTH = "month";
    public static final String BILLING_CYCLE_QUARTER = "quarter";
    public static final String BILLING_CYCLE_YEAR = "year";

    public static AgentStationServicePackService getInstance() {
        return new AgentStationServicePackService();
    }

    /**
     * 获取服务包费用
     *
     * @param organizeCode String
     * @return BigDecimal
     */
    public BigDecimal getFreezeServicePackFee(String organizeCode) {
        long startTime = TimeUtil.getMonthBegin00(+1);
        long endTime = TimeUtil.getMonthBegin00(+2);
        Map<String, Object> splitAmountData = AgentStationServicePackFeeEntity.getInstance()
                .field("IFNULL(SUM(amount),0) AS amount")
                .where("organize_code", organizeCode)
                .where("start_time", ">=", startTime)
                .where("end_time", "<=", endTime)
                .where("status", 1)
                .find();

        return MapUtil.getBigDecimal(splitAmountData, "amount", 2, RoundingMode.HALF_UP);

    }
    /**
     * 获取服务包费用
     *
     * @param organizeCode String
     * @return BigDecimal
     */
    public BigDecimal getServicePackFee(String organizeCode) {
        long endTime = TimeUtil.getMonthBegin(+1);
        Map<String, Object> splitAmountData = AgentStationServicePackFeeEntity.getInstance()
                .field("IFNULL(SUM(amount),0) AS amount")
                .where("organize_code", organizeCode)
//                .where("start_time", ">=", startTime)
                .where("end_time", "<=", endTime)
                .where("status", 1)
                .find();

        return MapUtil.getBigDecimal(splitAmountData, "amount", 2, RoundingMode.HALF_UP);

    }

    /**
     * 获取未扣费服务包明细
     *
     * @param organizeCode String
     * @return List
     */
    public List<AgentStationServicePackFeeEntity> getServicePackFeeDetail(String organizeCode) {
        long endTime = TimeUtil.getMonthBegin(+1);
        List<AgentStationServicePackFeeEntity> list = AgentStationServicePackFeeEntity.getInstance()
                .field("*")
                .where("organize_code", organizeCode)
//                .where("start_time", ">=", startTime)
                .where("end_time", "<=", endTime)
                .where("status", 1)
                .selectList();

        if (list.isEmpty()) return null;
        return list;
    }

    /**
     * 根据订单号获取费用明细
     *
     * @param organizeCode String
     * @param orderSn      String
     * @return
     */
    public List<AgentStationServicePackFeeEntity> getServicePackFeeDetailByOrderSn(String organizeCode, String orderSn) {
        List<AgentStationServicePackFeeEntity> list = AgentStationServicePackFeeEntity.getInstance()
                .field("*")
                .where("organize_code", organizeCode)
//                .where("start_time", ">=", startTime)
                .where("withdraw_order_sn", orderSn)
                .selectList();

        if (list.isEmpty()) return null;
        return list;
    }


    /**
     * 批量冻结费用
     *
     * @param organizeCode String
     * @param orderSn      String
     */
    public void batchFreezeServiceFee(String organizeCode, String orderSn) {
        List<AgentStationServicePackFeeEntity> list = getServicePackFeeDetail(organizeCode);
        if (list.isEmpty()) return;
        for (AgentStationServicePackFeeEntity nd : list) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", 2); // 状态 1=未提现 2=提现申请中  3=已提现
            data.put("withdraw_order_sn", orderSn);
            data.put("update_time", TimeUtil.getTimestamp());
            AgentStationServicePackFeeEntity.getInstance()
                    .where("id", nd.id)
                    .update(data);
        }
    }

    /**
     * 批量处理服务费用
     *
     * @param organizeCode String
     * @param orderSn      String
     * @param status       int  状态 1=未提现 2=提现申请中  3=已提现
     */
    public void batchHandleServiceFee(String organizeCode, String orderSn, int status) {
        List<AgentStationServicePackFeeEntity> list = getServicePackFeeDetailByOrderSn(organizeCode, orderSn);

        if (list.isEmpty()) return;
        for (AgentStationServicePackFeeEntity nd : list) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", status); // 状态 1=未提现 2=提现申请中  3=已提现
            data.put("update_time", TimeUtil.getTimestamp());

            if (status == 1) {
                data.put("withdraw_order_sn", "");
            }
            AgentStationServicePackFeeEntity.getInstance()
                    .where("id", nd.id)
                    .update(data);
        }

    }


    /**
     * 配置站点服务包
     *
     * @param servicePackConfigEntity Entity
     * @param organizeCode            String
     * @param CSIds                   String
     * @param price                   BigDecimal
     * @param startTime               long
     * @param endTime                 long
     * @return ISyncResult
     */
    public ISyncResult createServicePackConfig(ServicePackConfigEntity servicePackConfigEntity
            , String organizeCode
            , String CSIds
            , BigDecimal price
            , long startTime
            , long endTime
    ) {
        if (servicePackConfigEntity == null) return new SyncResult(1, "服务包配置不存在");
        if (!StringUtils.hasLength(CSIds)) return new SyncResult(1, "站点不存在");

        Map<String, Object> data = new LinkedHashMap<>();

        String[] CSIdsArr = CSIds.split(",");
        int cycleCount = calculateTotalPeriods(servicePackConfigEntity.billing_cycle, startTime, endTime);

        for (String CSId : CSIdsArr) {
            try {

                BigDecimal finalPrice = BigDecimal.valueOf(0);
                ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId);
                //检查是否存在服务包
                long currentTime=TimeUtil.getTimestamp();
                AgentStationServicePackConfigEntity check=AgentStationServicePackConfigEntity.getInstance()
                        .where("organize_code",organizeCode)
                        .where("CSId",CSId)
                        .where("start_time",">=",currentTime)
                        .where("end_time","<=",currentTime)
                        .findEntity();

                if(check!=null) continue;

                switch (servicePackConfigEntity.billing_unit) {
                    case "station":
                        finalPrice = price;
                        break;
                    case "socket":
                        finalPrice = price.multiply(BigDecimal.valueOf(chargeStationEntity.totalSocket));
                        break;
                }

                data.put("organize_code", organizeCode);
                data.put("CSId", CSId);
                data.put("service_code", servicePackConfigEntity.service_code);
                data.put("start_time", startTime);
                data.put("end_time", endTime);
                data.put("price", finalPrice);
                data.put("cycle_count", cycleCount);
                data.put("create_time", TimeUtil.getTimestamp());
                AgentStationServicePackConfigEntity.getInstance().insertGetId(data);

                AgentStationServicePackConfigEntity agentStationServicePackConfigEntity = new AgentStationServicePackConfigEntity();

                agentStationServicePackConfigEntity.organize_code = organizeCode;
                agentStationServicePackConfigEntity.CSId = CSId;
                agentStationServicePackConfigEntity.service_code = servicePackConfigEntity.service_code;
                agentStationServicePackConfigEntity.start_time = startTime;
                agentStationServicePackConfigEntity.end_time = endTime;
                agentStationServicePackConfigEntity.price = finalPrice;
                agentStationServicePackConfigEntity.cycle_count = cycleCount;
                LogsUtil.info(this.getClass().getName(), String.format("创建站点服务包配置文件，organize_code=%s,CSId=%s,service_code=%s,start_time=%s,end_time=%s"
                        , organizeCode
                        , CSId
                        , servicePackConfigEntity.service_code
                        , TimeUtil.toTimeString(startTime)
                        , TimeUtil.toTimeString(endTime)
                ));
                ISyncResult r = createServicePackFee(agentStationServicePackConfigEntity, cycleCount);
            }catch (Exception e){
                return new SyncResult(1,e.getMessage());
            }
        }

        return new SyncResult(0, "success");
    }


    /**
     * 创建费用明细
     *
     * @param agentStationServicePackConfigEntity Entity
     * @return Object
     */
    public ISyncResult createServicePackFee(AgentStationServicePackConfigEntity agentStationServicePackConfigEntity, int cycleCount) {

        ServicePackConfigEntity servicePackConfigEntity = ServicePackConfigEntity.getInstance().getConfigByCode(agentStationServicePackConfigEntity.service_code);
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(agentStationServicePackConfigEntity.CSId);

        if (chargeStationEntity == null) return new SyncResult(1, "站点信息不存在");

        long startTime = 0;
        long endTime = 0;
        if (servicePackConfigEntity == null) return new SyncResult(1, "服务包配置不存在");
        //周期计算 日 月 季度 年
        for (int i = 1; i <= cycleCount; i++) {
            //需要重新计算每个服务包的开始时间和结束时间
            try {
                long[] periodTime = calculatePeriodTime(
                        i
                        , servicePackConfigEntity.billing_cycle
                        , agentStationServicePackConfigEntity.start_time
                        , agentStationServicePackConfigEntity.end_time);

                startTime = periodTime[0];
                endTime = periodTime[1];

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("organize_code", agentStationServicePackConfigEntity.organize_code);
                data.put("CSId", agentStationServicePackConfigEntity.CSId);
                data.put("service_code", agentStationServicePackConfigEntity.service_code);
                data.put("start_time", startTime);
                data.put("end_time", endTime);
                data.put("amount", agentStationServicePackConfigEntity.price);
                data.put("status", 1); //状态 1=未提现 2=提现申请中  3=已提现
                data.put("create_time", TimeUtil.getTimestamp());
                AgentStationServicePackFeeEntity.getInstance().insertGetId(data);
            }catch (Exception e){
                System.out.println(this.getClass().getName()+":"+e.getMessage());
                return new SyncResult(1,e.getMessage());
            }
        }


        return new SyncResult(0, "创建成功");
    }


    /**
     * 计算指定周期的服务时间范围
     *
     * @param n            第几个周期（从1开始）
     * @param billingCycle 计费周期（day/month/quarter/year）
     * @param startTime    服务包开始时间（毫秒时间戳）
     * @param endTime      服务包结束时间（毫秒时间戳）
     * @return long[]{周期开始时间, 周期结束时间}
     */
    public static long[] calculatePeriodTime(int n, String billingCycle, long startTime, long endTime) {
        // 参数校验
        if (n < 1) {
            throw new IllegalArgumentException("周期数必须大于0");
        }
        if (startTime >= endTime) {
            throw new IllegalArgumentException("开始时间必须小于结束时间");
        }

        // 转换时间戳为LocalDateTime
        LocalDateTime startDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
        LocalDateTime endDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(endTime), ZoneId.systemDefault());

        // 计算周期开始时间
        LocalDateTime periodStart = startDateTime;
        switch (billingCycle.toLowerCase()) {
            case BILLING_CYCLE_DAY:
                periodStart = startDateTime.plusDays(n - 1);
                break;
            case BILLING_CYCLE_MONTH:
                periodStart = startDateTime.plusMonths(n - 1);
                break;
            case BILLING_CYCLE_QUARTER:
                periodStart = startDateTime.plusMonths((n - 1) * 3L);
                break;
            case BILLING_CYCLE_YEAR:
                periodStart = startDateTime.plusYears(n - 1);
                break;
            default:
                throw new IllegalArgumentException("无效的计费周期：" + billingCycle);
        }

        // 计算周期结束时间
        LocalDateTime periodEnd;
        switch (billingCycle.toLowerCase()) {
            case BILLING_CYCLE_DAY:
                periodEnd = periodStart.plusDays(1);
                break;
            case BILLING_CYCLE_MONTH:
                periodEnd = periodStart.plusMonths(1);
                break;
            case BILLING_CYCLE_QUARTER:
                periodEnd = periodStart.plusMonths(3);
                break;
            case BILLING_CYCLE_YEAR:
                periodEnd = periodStart.plusYears(1);
                break;
            default:
                throw new IllegalArgumentException("无效的计费周期：" + billingCycle);
        }

        // 确保不超过服务包结束时间
        if (periodEnd.isAfter(endDateTime)) {
            periodEnd = endDateTime;
        }

        // 转换回时间戳
        long periodStartTime = periodStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long periodEndTime = periodEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        return new long[]{periodStartTime, periodEndTime};
    }

    /**
     * 计算总周期数
     *
     * @param billingCycle 计费周期（day/month/quarter/year）
     * @param startTime    服务包开始时间（毫秒时间戳）
     * @param endTime      服务包结束时间（毫秒时间戳）
     * @return 总周期数
     */
    public static int calculateTotalPeriods(String billingCycle, long startTime, long endTime) {
        LocalDateTime startDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
        LocalDateTime endDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(endTime), ZoneId.systemDefault());

        long periods;
        switch (billingCycle.toLowerCase()) {
            case BILLING_CYCLE_DAY:
                periods = ChronoUnit.DAYS.between(startDateTime, endDateTime) + 1;
                break;
            case BILLING_CYCLE_MONTH:
                periods = ChronoUnit.MONTHS.between(startDateTime, endDateTime) + 1;
                break;
            case BILLING_CYCLE_QUARTER:
                periods = (ChronoUnit.MONTHS.between(startDateTime, endDateTime) + 3) / 3;
                break;
            case BILLING_CYCLE_YEAR:
                periods = ChronoUnit.YEARS.between(startDateTime, endDateTime) + 1;
                break;
            default:
                throw new IllegalArgumentException("无效的计费周期：" + billingCycle);
        }
        return (int) periods;
    }


}
