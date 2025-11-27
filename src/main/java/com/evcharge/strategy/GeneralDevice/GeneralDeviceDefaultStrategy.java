package com.evcharge.strategy.GeneralDevice;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.GeneralDeviceStrategyMapping;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;
import org.springframework.stereotype.Component;


/**
 * 通用设备默认策略实现类。
 * <p>
 * 该类实现了通用设备策略接口 `GeneralDeviceStrategy`，作为当设备无法匹配到具体的SPU编码、品牌编码、
 * 或类型编码策略时执行的默认策略。
 * <p>
 * 当调用者请求执行设备操作时，系统首先会尝试根据设备的SPU编码（spuCode）查找对应的策略；
 * 如果找不到SPU策略，则会继续查找品牌编码（brandCode）对应的策略；
 * 如果仍未找到品牌策略，系统将根据设备类型编码（typeCode）查找策略。
 * <p>
 * 如果以上所有步骤都未能找到匹配的策略实现，系统将最终执行此默认策略。
 * <p>
 * 默认策略的行为是返回一个表示操作未定义的 `SyncResult` 对象，状态码为 `1`，并附带提示消息"该设备没有对应的执行能力"。
 * 通过这种方式，调用者可以明确知道该设备的操作未被定义或支持。
 * <p>
 * 该类使用 `@GeneralDeviceStrategyMapping` 注解进行标注，但不指定任何参数值，
 * 以确保在策略查找失败时系统能够自动使用此策略。
 */
@Component
@GeneralDeviceStrategyMapping()
public class GeneralDeviceDefaultStrategy implements IGeneralDeviceStrategy {

    /**
     * 执行设备的默认操作。
     * <p>
     * 当设备无法匹配到具体的策略时，系统将调用此方法。此默认方法不执行任何实际的设备操作，
     * 而是返回一个表示操作未定义的 `SyncResult` 对象，状态码为 `1`。
     * <p>
     * 虽然方法接收设备的序列号（serialNumber）和相关数据（data），
     * 但这些参数在默认实现中并未被使用。实际开发中可以根据需要扩展此默认策略，或者在具体设备策略实现中替代此默认行为。
     *
     * @param serialNumber 设备的唯一序列号，用于标识具体的设备。
     *                     虽然此默认策略并未使用该参数，但在其他具体策略实现中，序列号通常用于定位设备。
     * @param data         包含执行操作所需参数的 Map。
     *                     该参数在此默认策略中未被使用，但可以由具体策略实现类进行扩展使用。
     *                     具体参数内容通常由不同的设备策略实现类定义。
     * @return SyncResult 返回表示操作未定义的 `SyncResult` 对象，状态码为 `1`，并附带提示信息"该设备没有对应的执行能力"。
     */
    @Override
    public SyncResult execute(String serialNumber, JSONObject data) {
        return new SyncResult(1, "该设备没有对应的执行能力");
    }

    @Override
    public SyncResult executeAsync(String serialNumber, JSONObject data, IAsyncListener iAsyncListener) {
        return new SyncResult(1, "该设备没有对应的执行能力");
    }
}