package com.evcharge.libsdk.sky1088;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 天将军4G远程防水遥控开关SDK
 * <p>
 * 该类提供了操作天将军4G远程防水遥控开关的功能，通过REST API与设备进行通信。
 * 主要功能包括设备开关操作、状态查询以及异步操作支持。
 * 使用此SDK可以方便地控制和管理天将军智能开关设备。
 */
public class SkySmartSwitch {

    private final static String TAG = "天将军4G远程防水遥控开关";
    private final static String APIBaseUrl = "http://open.api.v1.sky1099.com";

    /**
     * 设备序列号
     */
    private String deviceNumber;

    /**
     * 设备token
     */
    private String token;

    /**
     * 等待执行的开关列表
     * <p>
     * 存储待执行的开关操作，每个操作都包含开关的索引、开关状态以及是否保存状态。
     */
    private JSONArray awaitExecuteSwitchList;

    /**
     * 设置设备序列号。
     *
     * @param deviceNumber 设备的唯一序列号
     * @return SkySmartSwitch 当前对象的实例，用于链式调用
     */
    public SkySmartSwitch setDeviceNumber(String deviceNumber) {
        this.deviceNumber = deviceNumber;
        return this;
    }

    /**
     * 设置设备的token。
     *
     * @param token 设备的认证token
     * @return SkySmartSwitch 当前对象的实例，用于链式调用
     */
    public SkySmartSwitch setToken(String token) {
        this.token = token;
        return this;
    }

    /**
     * 获取 SkySmartSwitch 实例。
     *
     * @return SkySmartSwitch 新的 SkySmartSwitch 实例
     */
    public static SkySmartSwitch getInstance() {
        return new SkySmartSwitch("", "");
    }

    /**
     * 构造函数
     *
     * @param deviceNumber 设备的唯一序列号
     * @param token        设备的认证token
     */
    public SkySmartSwitch(String deviceNumber, String token) {
        this.deviceNumber = deviceNumber;
        this.token = token;
    }

    /**
     * 添加一个开关操作进入队列。
     * <p>
     * 该方法允许你将多个开关操作添加到等待执行的列表中，
     * 这些操作将在稍后一次性执行。
     *
     * @param index  开关索引，从0开始
     * @param on_off 开关状态：true 表示开启，false 表示关闭
     * @param isSave 断电后是否保存断电前的状态：true 表示保存，false 表示不保存
     * @return SkySmartSwitch 当前对象的实例，用于链式调用
     */
    public SkySmartSwitch addSwitch(int index, boolean on_off, boolean isSave) {
        if (this.awaitExecuteSwitchList == null) this.awaitExecuteSwitchList = new JSONArray();
        this.awaitExecuteSwitchList.add(new JSONObject() {{
            put("index", index);
            put("on_off", on_off);
            put("save", isSave);
        }});
        return this;
    }

    /**
     * 批量添加开关操作进入队列。
     * <p>
     * 该方法允许你将多个开关操作作为一个列表批量添加到等待执行的队列中，
     * 这些操作将在稍后一次性执行。
     *
     * @param array 需要操作的开关队列，包含多个开关操作的JSONArray。
     *              每个操作应当是一个包含以下键值对的JSONObject：
     *              <ul>
     *                <li><strong>index</strong> (int)：开关索引，从0开始，表示要操作的开关在设备中的位置。</li>
     *                <li><strong>on_off</strong> (boolean)：开关状态，true 表示开启，false 表示关闭。</li>
     *                <li><strong>save</strong> (boolean)：断电后是否保存断电前的状态，true 表示保存，false 表示不保存。</li>
     *              </ul>
     *              例如，以下是一个有效的JSONArray结构：
     *              <pre>
     *              [
     *                  {"index": 0, "on_off": true, "save": false},
     *                  {"index": 1, "on_off": false, "save": true},
     *                  {"index": 2, "on_off": true, "save": true}
     *              ]
     *              </pre>
     * @return SkySmartSwitch 当前对象的实例，用于链式调用。
     */
    public SkySmartSwitch addSwitch(JSONArray array) {
        this.awaitExecuteSwitchList = array;
        return this;
    }

    /**
     * 异步执行指令（不会阻塞）。
     * <p>
     * 该方法会在后台线程中执行添加到队列中的所有开关操作，并通过回调接口返回执行结果。
     *
     * @param iAsyncListener 异步回调接口，用于接收操作结果
     */
    public void executeAsync(IAsyncListener iAsyncListener) {
        if (!StringUtils.hasLength(this.deviceNumber)) {
            if (iAsyncListener != null) iAsyncListener.onResult(2, "无效设备");
            return;
        }
        if (!StringUtils.hasLength(this.token)) {
            if (iAsyncListener != null) iAsyncListener.onResult(2, "无效设备token");
            return;
        }
        if (this.awaitExecuteSwitchList == null || this.awaitExecuteSwitchList.isEmpty()) {
            if (iAsyncListener != null) iAsyncListener.onResult(2, "无开关操作");
            return;
        }
        // 启动多线程进行排队操作
        ThreadUtil.getInstance().execute(String.format("[%s]-%s", TAG, this.deviceNumber), () -> {
            Map<Integer, SyncResult> report = new LinkedHashMap<>();
            for (Object o : this.awaitExecuteSwitchList) {
                JSONObject data = (JSONObject) o;
                int index = data.getIntValue("index");
                boolean on_off = data.getBooleanValue("on_off", false);
                boolean save = data.getBooleanValue("save", false);
                SyncResult r = executeSync(index, on_off, save);
                report.put(index, r);
            }
            if (iAsyncListener != null) iAsyncListener.onResult(0, report);
        });
    }

    /**
     * 同步执行指令（会阻塞）。
     * <p>
     * 该方法同步执行单个开关操作，调用此方法将会阻塞当前线程直到操作完成。
     * <p>
     * 注意：如果设备正在执行其他操作，可能会导致并发问题。
     *
     * @param switchIndex 开关索引，从0开始
     * @param on_off      开关状态：true 表示开启，false 表示关闭
     * @param isSave      断电后是否保存断电前的状态：true 表示保存，false 表示不保存
     * @return SyncResult 操作的结果，包括状态码和消息
     */
    public SyncResult executeSync(int switchIndex, boolean on_off, boolean isSave) {
        try {
            if (!StringUtils.hasLength(this.deviceNumber)) return new SyncResult(2, "缺少设备序列号");
            if (!StringUtils.hasLength(this.token)) return new SyncResult(2, "缺少设备token");

            LogsUtil.info(TAG, "开关索引：%s %s", switchIndex, on_off ? "on" : "off");

            // 线路索引，从1开始
            switchIndex++;
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("Token", this.token);

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("serialNumber", this.deviceNumber);
            // type为类型，具体内容如下：DY__ 智能开关（实时指令，服务端定时）
            params.put("type", "YA34");
            // 实时指令：线路Index_状态 例：打开线路 2：2_1，关闭线路 3：3_0
            params.put("command", String.format("%s_%s", switchIndex, on_off ? 1 : 0));

            // save:可选字段(断电后是否保存断电前的状态，保存传 1，其他不保存)
            String url = String.format("%s/feasible-cmd/real-time?save=%s", APIBaseUrl, isSave ? 1 : 0);
            String responseText = Http2Util.post(url, params, header);
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn(TAG, "[%s] - 推送指令请求发生错误,响应内容为空，token:%s params:%s", deviceNumber, token, JSONObject.toJSONString(params));
                return new SyncResult(1, "下发指令失败");
            }

            LogsUtil.info(TAG, "开关索引：%s %s - 结果：%s", switchIndex, on_off ? "on" : "off", responseText);

            JSONObject response = JSONObject.parse(responseText);
            int code = JsonUtil.getInt(response, "code");
            switch (code) {
                case 200:
                    break;
                case -108: // {"code":-108,"message":"设备已离线","data":null}
                    LogsUtil.warn(TAG, "[%s] - 设备已离线", deviceNumber);
                    return new SyncResult(-1, "设备已离线");
                default:
                    String message = JsonUtil.getString(response, "message", "请求错误");
                    LogsUtil.warn(TAG, "[%s] - 推送指令请求发生错误 - %s token:%s params:%s response:%s", deviceNumber, message, token, JSONObject.toJSONString(params), responseText);
                    return new SyncResult(10, message);
            }
            String cmdId = JsonUtil.getString(response, "data");

            // 等待检查结果
            boolean executeResult = checkExecuteResult(deviceNumber, cmdId);
            if (!executeResult) return new SyncResult(11, "执行失败");
            return new SyncResult(0, "", new LinkedHashMap<>() {{
                put("cmdId", cmdId);
            }});
        } catch (Exception e) {
            LogsUtil.warn(TAG, "[%s] - 推送指令请求发生错误", deviceNumber);
        }
        return new SyncResult(1, "下发指令失败");
    }

    /**
     * 下发命令后，通过该方法检查执行状态。
     * <p>
     * 该方法通过轮询的方式，反复查询指令的执行状态，直到成功或超时。
     *
     * @param deviceNumber 设备序列号
     * @param cmdId        上一步请求返回的命令ID
     * @return boolean 执行结果：true 表示执行成功，false 表示执行失败
     */
    public boolean checkExecuteResult(String deviceNumber, String cmdId) {
        final int RETRY_INTERVAL_MS = 2500; // 每次查询间隔时间，单位：毫秒
        final int TIMEOUT_MS = 30000; // 超时时间，单位：毫秒
        final long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
            boolean result = sendExecuteResult(deviceNumber, cmdId);
            if (!result) {
                // 暂停一下，重新再发送请求
                ThreadUtil.sleep(RETRY_INTERVAL_MS);
                continue;
            }
            return true;
        }
        LogsUtil.warn(TAG, "[%s] - 指令执行超时", deviceNumber);
        return false;
    }

    /**
     * 实际的状态检查逻辑。
     * <p>
     * 该方法每隔2-3秒查询一次指令的执行状态，如果30秒内指令未成功，则认为执行失败。
     *
     * @param deviceNumber 设备序列号
     * @param cmdId        上一步请求返回的命令ID
     * @return boolean 执行结果：true 表示执行成功，false 表示执行失败
     */
    private boolean sendExecuteResult(String deviceNumber, String cmdId) {
        try {
            String url = String.format("%s/feasible-cmd/status/%s/%s", APIBaseUrl, deviceNumber, cmdId);

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("Token", token);

            String responseText = Http2Util.get(url, null, header);
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn(TAG, "[%s] - 获取执行状态,响应内容为空，%s", deviceNumber, url);
                return false;
            }
            JSONObject response = JSONObject.parse(responseText);
            int code = JsonUtil.getInt(response, "code");
            if (code != 200) {
                String message = JsonUtil.getString(response, "message", "请求错误");
                LogsUtil.warn(TAG, "[%s] - 获取执行状态错误 - %s 请求详情：%s response:%s", deviceNumber, message, url, responseText);
                return false;
            }
            JSONObject data = response.getJSONObject("data");
            if (data == null) return false;

            int status = JsonUtil.getInt(data, "status", 0);
            return status >= 1;
        } catch (Exception e) {
            LogsUtil.warn(TAG, "[%s] - 推送指令请求发生错误", deviceNumber);
        }
        return false;
    }
}