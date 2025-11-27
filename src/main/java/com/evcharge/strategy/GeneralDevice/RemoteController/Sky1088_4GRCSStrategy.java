package com.evcharge.strategy.GeneralDevice.RemoteController;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.GeneralDeviceStrategyMapping;
import com.evcharge.libsdk.sky1088.SkySmartSwitch;
import com.evcharge.service.GeneralDevice.GeneralDeviceConfigService;
import com.evcharge.strategy.GeneralDevice.IGeneralDeviceStrategy;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.JsonUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 天将军4G远程控制器策略实现类。
 * <p>
 * 该类实现了 GeneralDeviceStrategy 接口，提供了天将军品牌的 4G 远程控制器的策略实现。
 * 具体实现针对品牌编码为 "sky1088"、SPU编码为 "smart-switch-c" 的设备。
 * 通过此策略类，可以为这些设备执行特定的远程控制操作。
 */
@Service
@GeneralDeviceStrategyMapping(typeCode = "4GRCS", brandCode = "sky1088", spuCode = "smart-switch-c")
public class Sky1088_4GRCSStrategy implements IGeneralDeviceStrategy {

    /**
     * 执行设备的固有能力操作。
     * <p>
     * 本方法用于实现对天将军4G远程控制器的具体操作逻辑。
     * 根据传入的设备序列号（serialNumber）和操作数据（data），执行相应的远程控制指令。
     *
     * @param serialNumber 设备的唯一序列号，用于标识具体的远程控制器。
     * @param data         一个包含操作参数的 JSONObject，其中包含执行远程控制所需的各种数据。
     *                     <ul>
     *                       <li><strong>index</strong> (int)：开关索引，从0开始，表示要操作的开关在设备中的位置。</li>
     *                       <li><strong>on_off</strong> (boolean)：开关状态，true 表示开启，false 表示关闭。</li>
     *                       <li><strong>save</strong> (boolean)：断电后是否保存断电前的状态，true 表示保存，false 表示不保存。</li>
     *                     </ul>
     * @return SyncResult 返回执行操作的结果对象，包含操作的状态码和结果消息。
     * <ul>
     *   <li><strong>状态码 (code)</strong>：表示操作的执行结果，0 表示成功，其他值表示失败。</li>
     *   <li><strong>消息 (message)</strong>：提供关于操作执行的详细信息或错误描述。</li>
     * </ul>
     */
    @Override
    public SyncResult execute(String serialNumber, JSONObject data) {
        // 从配置实体中获取设备的配置信息
        JSONObject config = GeneralDeviceConfigService.getInstance().getJSONObject(serialNumber);

        // 从配置信息中提取token
        String token = JsonUtil.getString(config, "token");
        if (!StringUtils.hasLength(token)) return new SyncResult(2, "缺少token");

        // 从请求数据中提取开关操作参数
        int switchIndex = JsonUtil.getInt(data, "index");
        boolean on_off = JsonUtil.getBoolean(data, "on_off");
        boolean isSave = JsonUtil.getBoolean(data, "save", true);

        // 创建SkySmartSwitch实例并同步执行开关操作
        SkySmartSwitch smartSwitch = new SkySmartSwitch(serialNumber, token);
        return smartSwitch.executeSync(switchIndex, on_off, isSave);
    }

    /**
     * 异步执行设备的固有能力操作。
     * <p>
     * 本方法实现了对天将军4G远程控制器的异步操作逻辑。
     * 根据传入的设备序列号（serialNumber）和操作数据（data），执行异步的远程控制指令，并通过回调接口返回结果。
     *
     * @param serialNumber   设备的唯一序列号，用于标识具体的远程控制器。
     * @param data           一个包含操作参数的 JSONObject，其中包含多个开关操作的 JSONArray。
     *                       <ul>
     *                         <li><strong>switch</strong> (JSONArray)：一个包含多个开关操作的数组。每个元素应为一个 JSONObject，结构如下：
     *                           <ul>
     *                             <li><strong>index</strong> (int)：开关索引，从0开始，表示要操作的开关在设备中的位置。</li>
     *                             <li><strong>on_off</strong> (boolean)：开关状态，true 表示开启，false 表示关闭。</li>
     *                             <li><strong>save</strong> (boolean)：断电后是否保存断电前的状态，true 表示保存，false 表示不保存。</li>
     *                           </ul>
     *                         </li>
     *                       </ul>
     * @param iAsyncListener 异步操作回调接口。用于接收异步操作的结果通知。
     * @return SyncResult 返回初步的执行结果，表示命令是否已成功下发。实际执行结果将在回调接口中返回。
     */
    @Override
    public SyncResult executeAsync(String serialNumber, JSONObject data, IAsyncListener iAsyncListener) {
        // 从配置实体中获取设备的配置信息
        JSONObject config = GeneralDeviceConfigService.getInstance().getJSONObject(serialNumber);

        // 从配置信息中提取token
        String token = JsonUtil.getString(config, "token");
        if (!StringUtils.hasLength(token)) return new SyncResult(2, "缺少token");

        // 从请求数据中提取多个开关操作参数的JSONArray
        JSONArray switchArray = JsonUtil.getJSONArray(data, "switch");

        // 创建SkySmartSwitch实例并添加多个开关操作
        SkySmartSwitch smartSwitch = new SkySmartSwitch(serialNumber, token)
                .addSwitch(switchArray);

        // 异步执行开关操作，并通过回调接口返回结果
        smartSwitch.executeAsync(iAsyncListener);
        return new SyncResult(0, "命令已下发");
    }
}