package com.evcharge.entity.sys;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import lombok.Getter;
import lombok.Setter;
import org.quartz.*;

import java.util.Date;

/**
 * WechatCorpBotNotifyDelayedMessageTaskJob 类实现了延迟发送企业微信消息的功能。
 * 该类使用 Quartz 调度框架，允许在特定时间延迟执行消息发送任务。
 * 它包含了添加任务的方法以及任务执行的逻辑。
 */
@Setter
@Getter
@Deprecated
public class WechatCorpBotNotifyDelayedMessageTaskJob implements Job {

    /**
     * 监控文本，用于日志记录。
     */
    protected final static String TAG = "企业微信消息延迟推送";
    /**
     * 单例模式，确保只有一个实例。
     */
    private static volatile WechatCorpBotNotifyDelayedMessageTaskJob _this;

    /**
     * 获取 WechatCorpBotNotifyDelayedMessageTaskJob 的单例实例。
     *
     * @return WechatCorpBotNotifyDelayedMessageTaskJob 单例实例。
     */
    public static WechatCorpBotNotifyDelayedMessageTaskJob getInstance() {
        if (_this == null) {
            synchronized (WechatCorpBotNotifyDelayedMessageTaskJob.class) {
                if (_this == null) { // 双重检查锁定（Double-checked locking）确保线程安全
                    _this = new WechatCorpBotNotifyDelayedMessageTaskJob();
                }
            }
        }
        return _this;
    }

    /**
     * 将需要延迟发送的消息添加到调度器中。
     *
     * @param bot 包含消息内容和相关元数据的 WechatCorpBotNotify 对象。
     * @return SyncResult 返回添加任务的结果。
     */
    public SyncResult add(WechatCorpBotNotify bot) {
        try {
            // 生成唯一的任务名称
            String task_name = common.getUUID();

            // 创建一个 Quartz 触发器，设置延迟时间为当前时间 + 1 分钟 5 秒，且只执行一次
            Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(new Date(TimeUtil.getTimestamp() + ECacheTime.MINUTE + ECacheTime.SECOND * 5))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withRepeatCount(0)) // 只触发一次
                    .build();

            // 设置任务的参数
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("organize_code", bot.organize_code);
            jobDataMap.put("notify_type", bot.notify_type);
            jobDataMap.put("message", bot.message.toJSONString());

            // 将任务添加到调度器中，并返回添加结果
            SyncResult r = QuartzSchedulerManager.getInstance().add(task_name, this.getClass().getSimpleName(), trigger, this.getClass(), jobDataMap);
            if (r.code == 0) {
                LogsUtil.info(TAG, "[%s] 消息入队成功", task_name);
            }
            return r;
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "添加任务发生错误");
        }
        return new SyncResult(1, "添加失败");
    }

    /**
     * 执行任务的逻辑，负责实际的消息发送。
     *
     * @param jobExecutionContext Quartz 提供的任务执行上下文，包含任务的相关数据。
     * @throws JobExecutionException 当任务执行失败时抛出此异常。
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 获取任务参数
        JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();
        String organize_code = MapUtil.getString(data, "organize_code");
        String notify_type = MapUtil.getString(data, "notify_type");
        String messageJson = MapUtil.getString(data, "message");

        // 创建一个新的 WechatCorpBotNotify 对象，并设置相关属性
        WechatCorpBotNotify bot = new WechatCorpBotNotify();
        bot.organize_code = organize_code;
        bot.notify_type = notify_type;
        bot.message = JSONObject.parse(messageJson);

        // 发送消息，并记录发送结果
        SyncResult r = bot.send();
        if (r.code == 0) {
            LogsUtil.info(TAG, "发送成功");
        } else {
            LogsUtil.info(TAG, "发送失败，将推迟到下次发送");
        }
    }
}