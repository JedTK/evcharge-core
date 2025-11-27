package com.evcharge.strategy.GeneralDevice.RemoteController;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.GeneralDeviceStrategyMapping;
import com.evcharge.strategy.GeneralDevice.IGeneralDeviceStrategy;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;
import org.springframework.stereotype.Service;

/**
 * 4G远程控制开关的默认策略实现类。  使用例子，请不要删除
 * <p>
 * 该类实现了通用设备策略接口 `GeneralDeviceStrategy`，并为所有使用4G远程开关的设备提供默认的策略执行逻辑。
 * 当设备类型编码为 "4GRCS"，且没有找到与设备品牌（brandCode）或SPU编码（spuCode）对应的策略时，
 * 将会执行此默认策略。
 * <p>
 * 通过 `@GeneralDeviceStrategyMapping` 注解指定了该策略的类型编码为 "4GRCS"，表示此策略应用于所有
 * 4G远程控制开关的设备。
 */
@Service
@GeneralDeviceStrategyMapping(typeCode = "4GRCS")
public class _4GRCSDefaultStrategy implements IGeneralDeviceStrategy {

    /**
     * 执行4G远程开关的默认操作。
     * <p>
     * 该方法实现了设备的固有能力逻辑，在没有找到具体的品牌或SPU策略时，执行此默认策略。
     * 由于这是默认策略，所以方法中并未执行具体的控制逻辑，只返回一个默认的 `SyncResult` 对象。
     *
     * @param data 包含执行操作所需参数的 Map。
     *             该参数在此默认策略中未被使用，但可以由具体策略实现类进行扩展使用。
     * @return SyncResult 返回一个默认的同步结果对象，状态码为 0，表示操作成功但无具体动作执行。
     */
    @Override
    public SyncResult execute(String serialNumber, JSONObject data) {
        return executeAsync(serialNumber, data, null);
    }

    @Override
    public SyncResult executeAsync(String serialNumber, JSONObject data, IAsyncListener iAsyncListener) {
        return new SyncResult(1, "该设备没有对应的执行能力");
    }
}