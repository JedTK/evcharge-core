package com.evcharge.libsdk.openai;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.IAsyncListener;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * OpenAI 的 API 兼容接口
 */
public interface IOpenAI {
    /**
     * 获取可用的模型引擎列表
     */
    JSONObject models();

    /**
     * 对话补全 异步请求
     * <p>
     * 当params中的stream=true时也会一次性返回结果的，因为同步没有逐行读取能力
     *
     * @param messages 对话消息
     * @param params   参数
     */
    String completions(@NotNull JSONArray messages, @NotNull JSONObject params);

    /**
     * 对话补全 异步请求
     * <p>
     * 当params中的stream=true时会以流式返回结果
     *
     * @param messages       对话消息
     * @param params         参数
     * @param iAsyncListener 异步请求监听器
     */
    void completionsAsync(@NotNull JSONArray messages, @NotNull JSONObject params, @NonNull IAsyncListener iAsyncListener);
}
