package com.evcharge.strategy.GeneralDevice;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;

/**
 * 通用设备策略接口。
 * <p>
 * 该接口定义了通用设备的策略行为，所有实现该接口的类都将提供通用设备的操作实现。
 * 主要用于执行设备的固有能力操作，例如远程开关、获取监控数据等。
 * 不同设备类型可以有不同的策略实现，确保各自的固有能力能够正确执行。
 */
public interface IGeneralDeviceStrategy {

    /**
     * 执行设备的固有能力。
     * <p>
     * 该方法用于执行具体的设备操作，这些操作可能包括远程开关设备、获取设备的监控数据等。
     * 执行操作时，需要传入设备的序列号以及一个包含相关数据的 Map。Map 中的键值对用于传递必要的参数，
     * 序列号用于标识具体的设备。
     *
     * @param serialNumber 设备的序列号，用于唯一标识设备。每个设备都有一个唯一的序列号，
     *                     用于在执行操作时确定操作目标。
     * @param data         包含执行操作所需参数的 Map。
     *                     - 键：参数名称
     *                     - 值：参数值
     *                     具体参数内容由不同的设备策略实现类定义，可能包括操作类型、时间戳、控制指令等信息。
     * @return SyncResult 包含执行结果的对象。
     * SyncResult 对象用于表示操作的结果状态，可能包含操作是否成功、执行过程中产生的错误信息等。
     * 该结果对象能够帮助调用者理解操作的执行情况以及是否需要采取进一步的措施。
     */
    SyncResult execute(String serialNumber, JSONObject data);

    /**
     * 异步执行设备的固有能力。
     * <p>
     * 该方法与 `execute` 方法类似，但它以异步方式执行设备的操作。调用此方法时，操作将立即返回，
     * 而实际的操作结果将通过传入的 `IAsyncListener` 回调接口进行通知。
     * <p>
     * 该方法用于那些可能需要较长时间执行的操作，或者需要在后台执行而不阻塞调用者的情况。
     * 例如，远程开关设备可能涉及网络通信或其他耗时操作，在这些情况下，异步执行可以提高系统的响应性。
     *
     * @param serialNumber   设备的序列号，用于唯一标识设备。每个设备都有一个唯一的序列号，
     *                       用于在执行操作时确定操作目标。
     * @param data           包含执行操作所需参数的 Map。
     *                       - 键：参数名称
     *                       - 值：参数值
     *                       具体参数内容由不同的设备策略实现类定义，可能包括操作类型、时间戳、控制指令等信息。
     * @param iAsyncListener 异步操作回调接口。调用者可以通过该接口接收异步操作的结果通知。
     *                       - `onSuccess(SyncResult result)`: 当操作成功时调用，返回 `SyncResult` 对象，包含操作结果。
     *                       - `onFailure(Throwable t)`: 当操作失败时调用，传递异常信息。
     * @return SyncResult 初步的执行结果，可能是操作已开始的确认信息。具体的操作结果将通过 `IAsyncListener` 回调接口通知。
     */
    SyncResult executeAsync(String serialNumber, JSONObject data, IAsyncListener iAsyncListener);
}