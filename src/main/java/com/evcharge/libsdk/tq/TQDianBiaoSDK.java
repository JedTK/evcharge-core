package com.evcharge.libsdk.tq;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.util.*;

/**
 * 电子式电表 SDK 封装（浙江拓强电气有限公司）。
 * <p>
 * 开发文档地址：<a href="http://doc-api.tqdianbiao.com/">...</a>
 * <p>
 * 后台管理地址：<a href="https://168.tqdianbiao.com/admin/">...</a>
 * <p>
 * 功能说明：
 * 1. 电表添加、删除、清零操作
 * 2. 抄表读数操作
 * 3. 远程拉闸、合闸、保电、解除保电控制
 * 4. 采集器状态查询
 * 5. 签名生成与回调签名校验
 * 6. 异步回调上下文缓存与读取
 */
public class TQDianBiaoSDK {

    /**
     * 日志标识前缀
     */
    private static final String TAG = "电子式电表SDK";

    /**
     * API 基础链接，例如：<a href="https://168.tqdianbiao.com">...</a>
     */
    private final String base_url;

    /**
     * 授权代码。
     * 从系统后台「主菜单 > 接口授权 > 授权配置」中获取。
     */
    private final String auth_code;

    /**
     * 随机数（nonce）。
     * 从系统后台「主菜单 > 接口授权 > 授权配置」中获取。
     * 用于签名加盐，提升安全性。
     */
    private final String nonce;

    /**
     * 默认通知地址。
     * 当调用方法未传入 notifyUrl 时，将使用此地址作为回调 URL。
     */
    public String default_notifyUrl;

    /**
     * 单例实例，懒加载方式创建。
     */
    private static volatile TQDianBiaoSDK instance = null;

    /**
     * 获取公共单例实例。
     * 优先从 SysGlobalConfigEntity 中读取配置，不存在时再从 ConfigManager 中读取。
     * <p>
     * 配置键说明：
     * - TQMeter.BaseUrl   API 基础地址
     * - TQMeter.AuthCode  授权码
     * - TQMeter.Nonce     签名随机数
     * - TQMeter.NotifyUrl 默认回调地址
     *
     * @return 已初始化的 TQDianBiaoSDK 单例
     */
    public static TQDianBiaoSDK getInstance() {
        if (instance == null) {
            synchronized (TQDianBiaoSDK.class) {
                if (instance == null) {
                    String base_url = SysGlobalConfigEntity.getString("TQMeter.BaseUrl", ConfigManager.getString("TQMeter.BaseUrl"));
                    String auth_code = SysGlobalConfigEntity.getString("TQMeter.AuthCode", ConfigManager.getString("TQMeter.AuthCode"));
                    String nonce = SysGlobalConfigEntity.getString("TQMeter.Nonce", ConfigManager.getString("TQMeter.Nonce"));
                    String default_notifyUrl = SysGlobalConfigEntity.getString("TQMeter.NotifyUrl", ConfigManager.getString("TQMeter.NotifyUrl"));
                    instance = new TQDianBiaoSDK(base_url, auth_code, nonce, default_notifyUrl);
                }
            }
        }
        return instance;
    }

    /**
     * 实例化一个 SDK 对象。
     * 一般情况下推荐使用 getInstance() 获取默认单例。
     *
     * @param base_url          API 基础链接
     * @param auth_code         授权码
     * @param nonce             签名随机数
     * @param default_notifyUrl 默认通知地址，用于未显式传入 notifyUrl 时的回调地址
     */
    public TQDianBiaoSDK(String base_url, String auth_code, String nonce, String default_notifyUrl) {
        this.base_url = base_url;
        this.auth_code = auth_code;
        this.nonce = nonce;
        this.default_notifyUrl = default_notifyUrl;
    }

    /**
     * 实例化一个 SDK 对象。
     * 与上一个构造函数相比，不设置默认通知地址。
     *
     * @param base_url  API 基础链接
     * @param auth_code 授权码
     * @param nonce     签名随机数
     */
    public TQDianBiaoSDK(String base_url, String auth_code, String nonce) {
        this.base_url = base_url;
        this.auth_code = auth_code;
        this.nonce = nonce;
    }

    // region 电表添加 / 删除 / 清零操作

    /**
     * 添加电能表（入网操作）。
     * <p>
     * 注意：
     * - 一般在新设备首次接入平台时使用；
     * - 需要保证 cid 为拓强平台中的唯一设备标识。
     *
     * @param cidList 电表设备唯一标识列表（每个元素为 cid）
     * @return ISyncResult
     * code = 0 表示添加成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterAdd(List<String> cidList) {
        return sendMeterOperation(cidList, "/Api_v2/ele_meter/add", true);
    }

    /**
     * 删除电能表（移除设备，解除绑定）。
     * <p>
     * 注意：
     * - 删除后设备将不再受当前账号管理；
     * - 一般用于设备报废或更换等场景。
     *
     * @param cidList 电表设备唯一标识列表（每个元素为 cid）
     * @return ISyncResult
     * code = 0 表示删除成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterDel(List<String> cidList) {
        return sendMeterOperation(cidList, "/Api_v2/ele_meter/delete", false);
    }

    /**
     * 统一执行电表添加或删除操作，构建请求数据并发送请求。
     * 内部使用的通用方法，外部使用 meterAdd、meterDel 即可。
     *
     * @param cidList         电表设备唯一标识列表（每个元素为 cid）
     * @param apiPath         接口路径，例如 "/Api_v2/ele_meter/add" 或 "/Api_v2/ele_meter/delete"
     * @param withDescription 是否附加描述字段（添加时需要，删除时可忽略）
     * @return ISyncResult
     * code = 0 表示操作成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    private ISyncResult sendMeterOperation(List<String> cidList, String apiPath, boolean withDescription) {
        final String url = String.format("%s%s", this.base_url, apiPath);

        // 1. 组装基础参数
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auth_code", this.auth_code);
        data.put("timestamp", TimeUtil.getTimestamp() / 1000);

        // 2. 构建 request_content
        JSONArray requestContent = new JSONArray();
        for (String cid : cidList) {
            JSONObject meter = new JSONObject();
            meter.put("cid", cid);
            meter.put("address", cid);
            if (withDescription) {
                // 说明字段，可按需填写，自行在上层调用时扩展
                meter.put("description", "");
            }
            requestContent.add(meter);
        }

        data.put("request_content", requestContent.toJSONString());
        data.put("sign", generateSignature(data));

        // 3. 发起请求
        JSONObject response = request(url, data);
        if (response == null) {
            LogsUtil.warn(TAG, "电表操作请求无响应: %s", url);
            return new SyncResult(10, "");
        }

        String status = JsonUtil.getString(response, "status", "FAIL");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return new SyncResult(0, "");
        }
        LogsUtil.warn(TAG, "请求API成功，但返回失败结果: %s - %s", url, response.toJSONString());
        return new SyncResult(1, "操作失败");
    }

    /**
     * 电表清零操作（慎用）。
     * 通常用于设备入库初始化，会清除原有电量等数据，并设置为后付费模式。
     * <p>
     * 注意：
     * 1. 请仅对新设备或需要重置的设备执行；
     * 2. 默认设置为后付费模式（noprepay），如需预付费请自行调整；
     * 3. 操作为异步任务，通过回调通知执行结果。
     *
     * @param cidList   电表设备唯一标识符列表（每个元素为 cid）
     * @param notifyUrl 异步回调通知地址，若为空则使用 default_notifyUrl
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 2 表示参数缺失
     * code = 10 表示请求无响应
     */
    public ISyncResult meterReset(List<String> cidList, String notifyUrl) {
        final int RETRY_TIMES = 3;
        final String PAYMENT_MODE = "noprepay"; // 后付费模式

        // 1. 构建基础参数
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auth_code", this.auth_code);
        data.put("timestamp", TimeUtil.getTimestamp() / 1000);

        // 2. 构建设备清零请求项
        JSONArray requestContent = new JSONArray();

        for (String cid : cidList) {
            // 缓存上下文（便于回调中解析）
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("cid", cid);
            context.put("address", cid);

            String oprId = cacheOperationContext(context);

            // 构造请求内容
            JSONObject meter = new JSONObject();
            meter.put("opr_id", oprId);
            meter.put("cid", cid);
            meter.put("address", cid);
            meter.put("retry_times", RETRY_TIMES);
            meter.put("params", new JSONObject() {{
                put("paymentmode", PAYMENT_MODE); // 设置后付费模式
            }});

            requestContent.add(meter);
        }

        // 3. 填充请求体
        data.put("request_content", requestContent.toJSONString());

        if (StringUtil.isEmpty(notifyUrl)) notifyUrl = this.default_notifyUrl;
        data.put("notify_url", notifyUrl);

        data.put("sign", generateSignature(data));

        // 4. 发送请求
        String url = String.format("%s/Api_v2/ele_security/reset", this.base_url);
        JSONObject response = request(url, data);

        // 5. 响应判断
        if (response == null) {
            LogsUtil.warn(TAG, "电表清零请求无响应: %s", url);
            return new SyncResult(10, "请求API无响应");
        }

        String status = JsonUtil.getString(response, "status", "FAIL");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return new SyncResult(0, "");
        }
        LogsUtil.warn(TAG, "请求API成功，但返回失败结果: %s - %s", url, response.toJSONString());
        return new SyncResult(1, "操作失败");
    }

    // endregion

    // region 电表抄表

    /**
     * 发起单个电表的抄表请求。
     * <p>
     * 默认抄表数据类型为：
     * 3  正向有功总电能
     * <p>
     * 内部会为每个请求生成唯一操作 ID（opr_id），并将 cid 等上下文写入缓存，
     * 方便在异步回调中根据 opr_id 还原电表号等信息。
     *
     * @param cid       电表设备唯一标识符
     * @param notifyUrl 异步回调通知地址，若为空则使用 default_notifyUrl
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 2 表示参数缺失
     * code = 10 表示请求无响应
     */
    public ISyncResult meterRead(String cid, String notifyUrl) {
        return meterRead(new ArrayList<String>() {{
            add(cid);
        }}, notifyUrl);
    }

    /**
     * 发起多个电表的抄表请求。
     * <p>
     * 默认抄表数据类型为：
     * 3  正向有功总电能
     * <p>
     * 内部会为每个请求生成唯一操作 ID（opr_id），并将 cid 等上下文写入缓存，
     * 方便在异步回调中根据 opr_id 还原电表号等信息。
     *
     * @param cidList   电表设备唯一标识符列表（每个元素为 cid）
     * @param notifyUrl 异步回调通知地址，若为空则使用 default_notifyUrl
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 2 表示参数缺失
     * code = 10 表示请求无响应
     */
    public ISyncResult meterRead(List<String> cidList, String notifyUrl) {
        // 1. 组装基础请求参数
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auth_code", this.auth_code);
        data.put("timestamp", TimeUtil.getTimestamp() / 1000);

        // 目前只支持单一 type 抄表
        final int type = 3; // 默认抄表数据类型：正向有功总电能

        // 2. 构建请求内容数组（一个设备一个请求项）
        JSONArray requestContent = new JSONArray();
        for (String cid : cidList) {
            // 构建上下文数据用于缓存
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("cid", cid);
            context.put("address", cid);
            context.put("type", type);

            // 缓存操作ID -> 设备上下文
            String oprId = cacheOperationContext(context);

            // 构造单个设备的请求对象
            JSONObject meter = new JSONObject();
            meter.put("opr_id", oprId);
            meter.put("cid", cid);
            meter.put("address", cid);
            meter.put("retry_times", 3); // 默认重试3次
            meter.put("type", type);

            requestContent.add(meter);
        }

        data.put("request_content", requestContent.toJSONString());

        if (StringUtil.isEmpty(notifyUrl)) notifyUrl = this.default_notifyUrl;
        if (StringUtil.isEmpty(notifyUrl)) {
            LogsUtil.warn(TAG, "缺少抄表回调地址");
            return new SyncResult(2, "缺少抄表回调地址");
        }
        data.put("notify_url", notifyUrl);

        // 3. 签名生成
        String sign = generateSignature(data);
        data.put("sign", sign);

        // 4. 发送请求
        String url = String.format("%s/Api_v2/ele_read", this.base_url);
        JSONObject response = request(url, data);

        // 5. 响应解析
        if (response == null) {
            LogsUtil.warn(TAG, "请求API无响应: %s", url);
            return new SyncResult(10, "请求API无响应");
        }

        String status = JsonUtil.getString(response, "status", "FAIL");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return new SyncResult(0, "");
        }
        LogsUtil.warn(TAG, "请求API成功，但返回失败结果: %s - %s", url, response.toJSONString());
        return new SyncResult(1, "操作失败");
    }

    // endregion

    // region 电表闸门操作：通电、断电、保电、解除保电

    /**
     * 控制单个电表的远程拉闸或合闸操作（断电或送电）。
     *
     * @param cid        需要操作的电表设备唯一标识符
     * @param gateStatus 通断电状态，true 表示合闸（送电），false 表示拉闸（断电）
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterGateSwitch(String cid, boolean gateStatus) {
        return meterGateSwitch(new ArrayList<>() {{
            add(cid);
        }}, gateStatus, "");
    }

    /**
     * 控制单个电表的远程拉闸或合闸操作（断电或送电），可指定回调地址。
     *
     * @param cid        需要操作的电表设备唯一标识符
     * @param gateStatus 通断电状态，true 表示合闸（送电），false 表示拉闸（断电）
     * @param notifyUrl  异步操作结果回调 URL，若为空则使用 default_notifyUrl
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterGateSwitch(String cid, boolean gateStatus, String notifyUrl) {
        return meterGateSwitch(new ArrayList<>() {{
            add(cid);
        }}, gateStatus, notifyUrl);
    }

    /**
     * 批量控制多个电表的远程拉闸或合闸操作（断电或送电）。
     *
     * @param cidList    需要操作的电表设备唯一标识符列表
     * @param gateStatus 通断电状态，true 表示合闸（送电），false 表示拉闸（断电）
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterGateSwitch(List<String> cidList, boolean gateStatus) {
        return meterGateSwitch(cidList, gateStatus, "");
    }

    /**
     * 批量控制多个电表的远程拉闸或合闸操作（断电或送电），可指定回调地址。
     * <p>
     * 操作类型：
     * 10  拉闸（断电）
     * 11  合闸（送电）
     *
     * @param cidList    电表设备唯一标识符列表
     * @param gateStatus 通断电状态，true 表示合闸（送电），false 表示拉闸（断电）
     * @param notifyUrl  异步操作结果回调 URL，若为空则使用 default_notifyUrl
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterGateSwitch(List<String> cidList, boolean gateStatus, String notifyUrl) {
        final int TYPE_GATE_ON = 11;
        final int TYPE_GATE_OFF = 10;
        return meterGateOP(cidList, gateStatus ? TYPE_GATE_ON : TYPE_GATE_OFF, notifyUrl);
    }

    /**
     * 批量电表保电（禁止因欠费等原因自动拉闸），使用默认回调地址。
     *
     * @param cid       电表设备唯一标识符
     * @param keepPower 保电/解除保电
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterKeepAlive(String cid, boolean keepPower) {
        return meterKeepAlive(new ArrayList<>() {{
            add(cid);
        }}, keepPower, "");
    }

    /**
     * 批量电表保电（禁止因欠费等原因自动拉闸），使用默认回调地址。
     *
     * @param cid       电表设备唯一标识符
     * @param keepPower 保电/解除保电
     * @param notifyUrl 异步通知回调
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterKeepAlive(String cid, boolean keepPower, String notifyUrl) {
        return meterKeepAlive(new ArrayList<>() {{
            add(cid);
        }}, keepPower, notifyUrl);
    }

    /**
     * 批量电表保电（禁止因欠费等原因自动拉闸），使用默认回调地址。
     *
     * @param cidList   电表设备唯一标识符列表
     * @param keepPower 保电/解除保电
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterKeepAlive(List<String> cidList, boolean keepPower) {
        return meterKeepAlive(cidList, keepPower, "");
    }

    /**
     * 批量电表保电（禁止因欠费等原因自动拉闸），使用默认回调地址。
     *
     * @param cidList   电表设备唯一标识符列表
     * @param keepPower 保电/解除保电
     * @param notifyUrl 异步通知回调
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterKeepAlive(List<String> cidList, boolean keepPower, String notifyUrl) {
        final int TYPE_GATE_KEEP_POWER_ON = 63;
        final int TYPE_GATE_KEEP_POWER_OFF = 220;
        return meterGateOP(cidList, keepPower ? TYPE_GATE_KEEP_POWER_ON : TYPE_GATE_KEEP_POWER_OFF, notifyUrl);
    }

    /**
     * 批量控制电表闸门操作。
     * <p>
     * 操作类型说明：
     * 10  拉闸（断电）
     * 11  合闸（送电）
     * 63  保电
     * 220 解除保电
     * <p>
     * 内部会为每个请求生成唯一操作 ID（opr_id），并将 cid、type 等上下文写入缓存，
     * 方便在异步回调中根据 opr_id 还原电表号和操作类型。
     *
     * @param cidList   电表设备唯一标识符列表
     * @param gate_type 闸门操作类型（10/11/63/220）
     * @param notifyUrl 异步操作结果回调 URL，若为空则使用 default_notifyUrl
     * @return ISyncResult
     * code = 0 表示请求受理成功
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult meterGateOP(List<String> cidList, int gate_type, String notifyUrl) {
        final int RETRY_TIMES = 3;

        // 1. 基础参数
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auth_code", this.auth_code);
        data.put("timestamp", TimeUtil.getTimestamp() / 1000);

        // 2. 构建设备控制指令列表
        JSONArray requestContent = new JSONArray();

        for (String cid : cidList) {
            // 缓存上下文信息以便回调时解析
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("cid", cid);
            context.put("address", cid);
            context.put("type", gate_type);

            String oprId = cacheOperationContext(context);

            // 构造请求项
            JSONObject meter = new JSONObject();
            meter.put("opr_id", oprId);
            meter.put("cid", cid);
            meter.put("address", cid);
            meter.put("retry_times", RETRY_TIMES);
            meter.put("type", gate_type);

            requestContent.add(meter);
        }

        // 3. 组装请求体
        data.put("request_content", requestContent.toJSONString());

        if (StringUtil.isEmpty(notifyUrl)) notifyUrl = this.default_notifyUrl;
        data.put("notify_url", notifyUrl);

        data.put("sign", generateSignature(data));

        // 4. 发送请求
        String url = String.format("%s/Api_v2/ele_control", this.base_url);
        JSONObject response = request(url, data);

        // 5. 响应处理
        if (response == null) {
            LogsUtil.warn(TAG, "电表闸门操作请求无响应: %s", url);
            return new SyncResult(10, "请求API无响应");
        }

        String status = JsonUtil.getString(response, "status", "FAIL");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            JSONArray response_content = JsonUtil.getJSONArray(response, "response_content");
            return new SyncResult(0, "", response_content);
        }

        LogsUtil.warn(TAG, "请求API成功，但返回失败结果: %s - %s", url, response.toJSONString());
        return new SyncResult(1, "操作失败");
    }

    // endregion

    // region 采集器列表查询

    /**
     * 查询采集器列表和当前状态。
     * <p>
     * 请求说明：
     * GET {base_url}/Api/Collector?type=json&auth={auth_code}
     * <p>
     * 返回示例（简化）：
     * {
     * "status": "1",
     * "data": [
     * { "collector_id": "...", "status": "...", ... },
     * ...
     * ]
     * }
     *
     * @return ISyncResult
     * code = 0 表示成功，并在 data 中返回 List<Map<String, Object>> 类型的采集器列表
     * code = 1 表示业务失败
     * code = 10 表示请求无响应
     */
    public ISyncResult getCollector() {
        String url = String.format("%s/Api/Collector?type=json&auth=%s", this.base_url, this.auth_code);
        JSONObject response = Http2Util.getJson(url);
        if (response == null) {
            LogsUtil.warn(TAG, "请求API无响应: %s", url);
            return new SyncResult(10, "请求API无响应");
        }

        String status = JsonUtil.getString(response, "status");
        if (!"1".equalsIgnoreCase(status)) {
            LogsUtil.warn(TAG, "请求API成功，但返回失败结果: %s - %s", url, response.toJSONString());
            return new SyncResult(1, "");
        }

        JSONArray array = JsonUtil.getJSONArray(response, "data");
        if (array == null) {
            LogsUtil.warn(TAG, "请求API成功，但返回数据结构错误: %s - %s", url, response.toJSONString());
            return new SyncResult(1, "");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) (List<?>) array.toJavaList(Map.class);
        return new SyncResult(0, "", list);
    }

    // endregion

    // region 辅助函数

    /**
     * 校验回调请求的签名合法性，用于防止伪造或篡改请求。
     * <p>
     * 验证步骤：
     * 1. 从 data 中取出原始 sign 字段；
     * 2. 使用相同参数集合和算法重新计算签名；
     * 3. 比较计算后的签名与原始签名是否一致。
     * <p>
     * 注意：
     * - data 中必须包含 sign 字段；
     * - 校验失败时会记录详细日志，便于追踪问题。
     *
     * @param data 回调传入的参数 Map（包含 sign 字段）
     * @return ISyncResult
     * code = 0 表示签名验证通过
     * code = 1 表示签名不一致或校验失败
     * code = 2 表示缺少 sign 参数
     */
    public ISyncResult verifyCallbackSignature(Map<String, Object> data) {
        String sign = MapUtil.getString(data, "sign");

        // 1. 缺少签名字段
        if (StringUtil.isEmpty(sign)) {
            LogsUtil.warn(TAG, "回调签名校验失败：缺少 sign 参数 - data=%s", MapUtil.toJSONString(data));
            return new SyncResult(2, "缺少 sign 参数");
        }

        // 2. 重新计算签名
        String calculatedSign = generateSignature(data);

        // 3. 比较签名
        if (!calculatedSign.equals(sign)) {
            LogsUtil.warn(TAG, "回调签名校验失败：sign 不一致 - expected=%s, actual=%s, data=%s",
                    calculatedSign, sign, MapUtil.toJSONString(data));
            return new SyncResult(1, "签名校验失败");
        }

        return new SyncResult(0, "");
    }

    /**
     * 向指定 URL 发送 POST 请求，并解析返回的 JSON 字符串。
     * <p>
     * 请求说明：
     * - Content-Type 使用 application/x-www-form-urlencoded；
     * - 请求参数为普通表单键值对；
     * <p>
     * 返回说明：
     * - 当响应内容为空或解析失败时，返回 null；
     * - 上层调用需根据 null 进行统一的错误处理和日志记录。
     *
     * @param url    请求地址
     * @param params 请求参数 Map
     * @return 成功时返回解析后的 JSONObject，失败或解析异常时返回 null
     */
    private JSONObject request(String url, Map<String, Object> params) {
        // 1. 发起 POST 请求
        String responseText = Http2Util.post(url, params, "application/x-www-form-urlencoded");

        // 2. 空响应处理
        if (StringUtil.isEmpty(responseText)) {
            LogsUtil.warn(TAG, "请求接口无响应: %s", url);
            return null;
        }

        // 3. 解析响应内容为 JSON
        try {
            return JSONObject.parseObject(responseText);
        } catch (Exception e) {
            String preview = responseText.length() > 100
                    ? responseText.substring(0, 100) + "..."
                    : responseText;
            LogsUtil.warn(TAG, "响应解析失败 - url=%s - 响应内容片段=%s - 错误=%s", url, preview, e.getMessage());
        }

        return null;
    }

    /**
     * 根据参数 Map 生成签名字符串。
     * <p>
     * 签名规则：
     * 1. 将所有参数 key 按字典序（忽略大小写）升序排序；
     * 2. 排除 key 为 "sign" 的字段；
     * 3. 按排序后的顺序，依次拼接每个字段的 value（不拼接 key）；
     * 4. 在末尾拼接固定的随机字符串 nonce；
     * 5. 对最终字符串进行 MD5 摘要，得到 32 位签名字符串。
     *
     * @param data 请求参数键值对集合
     * @return 签名字符串（32位 MD5）
     */
    private String generateSignature(Map<String, Object> data) {
        // 1. 获取并排序参数 key 列表（升序）
        List<String> keys = new ArrayList<>(data.keySet());
        keys.sort(String::compareToIgnoreCase);

        // 2. 拼接参数值字符串（排除 sign 字段）
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if ("sign".equalsIgnoreCase(key)) {
                continue;
            }
            Object value = data.get(key);
            if (value != null) {
                sb.append(value);
            }
        }

        // 3. 拼接随机加盐字段（nonce）
        sb.append(this.nonce);

        // 4. 返回 MD5 摘要作为签名
        return common.md5(sb.toString());
    }

    /**
     * 生成唯一的操作 ID，并将其与指定的数据映射关系缓存。
     * <p>
     * 应用场景：
     * - 用于电表操作（抄表、拉闸、合闸、保电、解除保电等）异步回调；
     * - 拓强平台在回调接口中只会携带 opr_id，不会携带原始 cid 等信息；
     * - 通过本方法将 opr_id 和上下文数据建立映射，方便回调中反查。
     *
     * @param contextData 要关联的上下文数据，例如电表号、操作类型等
     * @return 操作 ID（唯一标识当前操作）
     */
    private static String cacheOperationContext(Map<String, Object> contextData) {
        return cacheOperationContext("", contextData);
    }

    /**
     * 生成或使用指定的操作 ID，并将其与上下文数据建立缓存映射。
     *
     * @param oprId       如果为空则自动生成；不为空时使用外部传入的 ID（需确保唯一）
     * @param contextData 要关联的上下文数据，例如电表号、操作类型等
     * @return 操作 ID（唯一标识当前操作）
     */
    private static String cacheOperationContext(String oprId, Map<String, Object> contextData) {
        if (StringUtil.isEmpty(oprId)) {
            oprId = common.md5(String.valueOf(TimeUtil.getTimestamp()));
        }

        String cacheKey = String.format("TEMP:TQDianBiaoSDK:OPR_ID:%s", oprId);
        DataService.getMainCache().setMap(cacheKey, contextData, ECacheTime.MINUTE * 30);
        return oprId;
    }

    /**
     * 根据操作 ID 获取先前缓存的上下文数据。
     * <p>
     * 常用于回调接口中，根据 opr_id 还原原始的电表编号、操作类型等信息。
     *
     * @param oprId 操作 ID
     * @return 关联的上下文数据 Map，如果不存在或已过期，可能返回 null
     */
    public static Map<String, Object> getOperationContext(String oprId) {
        String cacheKey = String.format("TEMP:TQDianBiaoSDK:OPR_ID:%s", oprId);
        return DataService.getMainCache().getMap(cacheKey);
    }

    // endregion
}
