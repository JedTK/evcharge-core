package com.evcharge.service.notify;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.enumdata.ENotifyType;

/**
 * ITransDataBuilder 接口 - 用于透传参数的构建与整合。
 * <p>
 * 该接口定义了一个通用的 `build` 方法，以动态构建和修改 `transData` 参数。
 * 当 `transData` 数据在多个地方具有相同或相似的结构时，可通过实现该接口
 * 来避免重复的代码逻辑，提升代码复用性和可维护性。
 * </p>
 * <p>
 * 使用场景示例：
 * <ul>
 *   <li>在不同的业务流程中，需要构建相似的 JSON 格式数据结构用于传输。</li>
 *   <li>简化 `asyncPush` 等方法调用时的数据整合步骤，减少每次生成 `transData` 的代码冗余。</li>
 *   <li>数据结构一致但内容稍有不同时，通过不同的实现类定制 `transData` 的具体内容。</li>
 * </ul>
 * </p>
 * <p>
 * 实现类应根据具体业务需求，实现 `build` 方法来构建并返回 `transData` 对象。
 *
 * @see JSONObject
 * @see com.evcharge.service.notify.NotifyService
 */
public interface ITransDataBuilder {

    /**
     * 构建或整合 `transData` 数据。
     * <p>
     * 实现该方法以便构建自定义的 `transData` JSON 对象。通常用于在重复的数据处理场景中，
     * 根据业务逻辑和上下文参数，将不同的数据字段组合到 `transData` 中。
     * </p>
     *
     * @param transData 原始的 `transData` JSON 数据对象，通常包含初始数据。
     *                  在实现中可以对其进行扩展或修改。
     * @return 构建后的 `JSONObject` 对象，包含最终整合的数据。
     */
    JSONObject build(String unique_code, String config_code, ENotifyType notifyType, JSONObject transData);
}