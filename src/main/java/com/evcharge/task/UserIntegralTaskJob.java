package com.evcharge.task;

import com.evcharge.entity.user.UserIntegralDetailEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UserIntegralTaskJob implements Job {
    protected final static String mMonitorText = "用户积分检查任务";
    private final String mGroupName = this.getClass().getSimpleName();
    private static UserIntegralTaskJob _this;

    public static UserIntegralTaskJob getInstance(){
        if(_this==null) _this=new UserIntegralTaskJob();
        return _this;
    }

    /**
     * 添加任务
     * @param detailId
     * @return
     */
    public SyncResult add(String detailId){
        try {
            if (!StringUtils.hasLength(detailId)) return new SyncResult(1, "");

            //每天凌晨00：00：00 执行任务
            String cron = "0 0 * * *";
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .startNow()
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                            .withMisfireHandlingInstructionDoNothing())
                    .build();

            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("detail_id", detailId);

            SyncResult r = QuartzSchedulerManager.getInstance().add(detailId
                    , mGroupName
                    , trigger
                    , this.getClass()
                    , jobDataMap);

            if (r.code == 0) {
                LogsUtil.info(mMonitorText, "\033[1;91m 添加任务成功：detailId=%s 下次执行：%s\033[0m", detailId, TimeUtil.toTimeString(trigger.getNextFireTime()));
            } else {
                LogsUtil.info(mMonitorText, "\033[1;91m 添加任务错误：detailId=%s 原因=%s \033[0m", detailId, r.msg);
            }
            return r;

        }catch (Exception e){
            return  new SyncResult(1,"添加失败");
        }
    }

    /**
     * 恢复任务
     */
    public void resume(){
        ThreadUtil.getInstance().execute(String.format("恢复[%s]",mMonitorText),()->{
            ThreadUtil.sleep(3000);
            LogsUtil.info(mMonitorText, "\033[1;91m 恢复任务 \033[0m");
            List<Map<String,Object>> list= UserIntegralDetailEntity.getInstance()
                    .where("status",0)
                    .where("expired_time","<",0)
                    .where("expired_time",">",0)
                    .selectList();
            if (list.isEmpty()) return;

            for (Map<String, Object> nd : list) {
                add(String.format("%s", MapUtil.getLong(nd, "id")));
            }
        });
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            String group = jobExecutionContext.getJobDetail().getKey().getGroup();
            String name = jobExecutionContext.getJobDetail().getKey().getName();

            JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();
            long detailId = MapUtil.getLong(data, "detail_id");
            LogsUtil.info(mMonitorText, "\033[1;92m 执行中... CSId=%s 下次执行：%s\033[0m", detailId, TimeUtil.toTimeString(jobExecutionContext.getNextFireTime()));
            SyncResult r = UserIntegralDetailEntity.getInstance().dealExpiredIntegral(detailId,"系统处理过期任务");
            if (r.code == 0) {
                QuartzSchedulerManager.getInstance().del(name, group);
                LogsUtil.error(mMonitorText, "%s 任务实行完成... JobDataMap: %s", r.msg, MapUtil.toJSONString(data));
                return;
            }
            if (r.code != 0 && r.code != 1) {
                LogsUtil.error(mMonitorText, "执行期间发生错误: %s  JobDataMap: %s", r.msg, MapUtil.toJSONString(data));
            }
        }catch (Exception e){
            LogsUtil.error(e, "", "执行期间发生错误");
        }
    }

}
