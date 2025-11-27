package com.evcharge.libsdk.openai;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import lombok.NonNull;
import okhttp3.*;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 与 OpenAI 兼容风格的API SDK
 */
public class OpenAI implements IOpenAI {
    private final static String TAG = OpenAI.class.getSimpleName();
    /**
     * API基础Url
     */
    private String base_url = "https://api.openai.com";
    /**
     * API秘钥
     */
    private final String api_key;
    /**
     * http请求客户端
     */
    private OkHttpClient client;
    /**
     * 最大并发请求数
     */
    private int maxRequests;
    /**
     * 每个主机最大并发请求数
     */
    private int maxRequestsPerHost;
    /**
     * http请求连接超时时间，秒
     */
    private int connectTimeout;
    /**
     * http读取超时时间，秒
     */
    private int readTimeout;
    /**
     * http写入超时时间，秒
     */
    private int writeTimeout;

    /**
     * 构造函数 - 多例模式：每个实例都会创建自己的 OkHttpClient 和 Dispatcher
     *
     * @param api_key API秘钥
     */
    public OpenAI(@NonNull String api_key) {
        if (StringUtil.isEmpty(api_key)) throw new IllegalArgumentException("API key cannot be null or empty");
        this.api_key = api_key;
        this.connectTimeout = 30;
        this.readTimeout = 60;
        this.writeTimeout = 60;
        this.client = null; // 实例化时不初始化客户端减少资源生成，实现懒加载
    }

    /**
     * 构造函数 - 多例模式：每个实例都会创建自己的 OkHttpClient 和 Dispatcher
     *
     * @param base_url API基础Url
     * @param api_key  API秘钥
     */
    public OpenAI(@NonNull String base_url, @NonNull String api_key) {
        if (StringUtil.isEmpty(base_url)) throw new IllegalArgumentException("base url cannot be null or empty");
        if (StringUtil.isEmpty(api_key)) throw new IllegalArgumentException("API key cannot be null or empty");
        this.base_url = base_url;
        this.api_key = api_key;
        this.connectTimeout = 30;
        this.readTimeout = 60;
        this.writeTimeout = 60;
        this.client = null; // 实例化时不初始化客户端减少资源生成，实现懒加载
    }

    /**
     * 构造函数 - 单例模式：传入共享的 OkHttpClient
     *
     * @param api_key API秘钥
     * @param client  自定义http请求客户端，一般用于单例模式，可以控制并发请求和性能
     */
    public OpenAI(@NonNull String api_key, @NonNull OkHttpClient client) {
        if (StringUtil.isEmpty(api_key)) throw new IllegalArgumentException("API key cannot be null or empty");
        this.api_key = api_key;
        this.connectTimeout = 30;
        this.readTimeout = 60;
        this.writeTimeout = 60;
        this.client = client;
    }

    /**
     * 构造函数 - 单例模式：传入共享的 OkHttpClient
     *
     * @param base_url API基础Url
     * @param api_key  API秘钥
     * @param client   自定义http请求客户端，一般用于单例模式，可以控制并发请求和性能
     */
    public OpenAI(@NonNull String base_url, @NonNull String api_key, @NonNull OkHttpClient client) {
        if (StringUtil.isEmpty(base_url)) throw new IllegalArgumentException("base url cannot be null or empty");
        if (StringUtil.isEmpty(api_key)) throw new IllegalArgumentException("API key cannot be null or empty");
        this.base_url = base_url;
        this.api_key = api_key;
        this.connectTimeout = 60;
        this.readTimeout = 60;
        this.writeTimeout = 60;
        this.client = client;
    }

    /**
     * 设置最大并发请求数,仅在多例生效
     */
    public OpenAI setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
        return this;
    }

    /**
     * 设置每个主机最大并发请求数,仅在多例生效
     */
    public OpenAI setMaxRequestsPerHost(int maxRequestsPerHost) {
        this.maxRequestsPerHost = maxRequestsPerHost;
        return this;
    }

    /**
     * 设置 http请求连接超时时间，秒,仅在多例生效
     */
    public OpenAI setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * 设置 http读取超时时间，秒,仅在多例生效
     *
     * @param readTimeout
     * @return
     */
    public OpenAI setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * 设置 http写入超时时间，秒,仅在多例生效
     *
     * @param writeTimeout
     * @return
     */
    public OpenAI setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
        return this;
    }

    /**
     * 初始化资源
     */
    private void init() {
        if (this.client == null) {
            Dispatcher dispatcher = new Dispatcher();
            if (this.maxRequests > 0) dispatcher.setMaxRequests(this.maxRequests);
            if (this.maxRequestsPerHost > 0) dispatcher.setMaxRequestsPerHost(this.maxRequestsPerHost);

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.dispatcher(dispatcher);
            if (this.connectTimeout > 0) builder.connectTimeout(this.connectTimeout, TimeUnit.SECONDS);
            if (this.readTimeout > 0) builder.readTimeout(this.readTimeout, TimeUnit.SECONDS);
            if (this.writeTimeout > 0) builder.writeTimeout(this.writeTimeout, TimeUnit.SECONDS);
            this.client = builder.build();
        }
    }

    /**
     * 构造请求
     */
    private Request buildRequest(@NonNull String path, @NotNull String method, @NotNull JSONObject params) {
        RequestBody body = RequestBody.create(params.toString(), MediaType.get("application/json"));
        Request.Builder request = new Request.Builder();
        request.header("Content-Type", "application/json");
        request.header("Authorization", "Bearer " + this.api_key);
        request.url(String.format("%s%s", this.base_url, path)).method(method, body);
        return request.build();
    }

    /**
     * 获取可用的模型引擎列表
     *
     * @return
     */
    public JSONObject models() {
        try {
            this.init();
            Request request = buildRequest("/models", "GET", new JSONObject());
            final Call call = this.client.newCall(request);
            Response response = call.execute();
            if (!response.isSuccessful()) {
                LogsUtil.warn(TAG, "/models 同步请求失败:response.code=%s response.body=%s", response.code(), response.body() == null ? "" : response.body().string());
                return null;
            }
            if (response.body() == null) {
                LogsUtil.warn(TAG, "/models 无响应数据");
                return null;
            }
            return JSONObject.parse(response.body().string());
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "/models 同步请求发生错误");
        }
        return null;
    }

    /**
     * 对话补全 异步请求
     * <p>
     * 当params中的stream=true时也会一次性返回结果的，因为同步没有逐行读取能力
     *
     * @param messages 对话消息
     * @param params   参数
     */
    public String completions(@NotNull JSONArray messages, @NotNull JSONObject params) {
        try {
            this.init();
            params.put("messages", messages);

            Request request = buildRequest("/chat/completions", "POST", params);
            final Call call = this.client.newCall(request);
            Response response = call.execute();
            if (!response.isSuccessful()) {
                LogsUtil.warn(TAG, "/completions 同步请求失败:response.code=%s response.body=%s", response.code(), response.body() == null ? "" : response.body().string());
                return null;
            }
            if (response.body() == null) {
                LogsUtil.warn(TAG, "/completions 同步请求无响应数据");
                return null;
            }

            // 不以流式返回结果
            if (!JsonUtil.getBoolean(params, "stream", false)) return response.body().string();

            // 以流式返回结果
            StringBuilder result_text = new StringBuilder();
            try (BufferedSource source = response.body().source()) {
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    result_text.append(String.format("%s", line));
                }
            } catch (IOException ignored) {

            }
            return result_text.toString();
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "/completions 同步请求发生错误：%s", e.getMessage());
        }
        return null;
    }

    /**
     * 对话补全 异步请求
     * <p>
     * 当params中的stream=true时会以流式返回结果
     *
     * @param messages       对话消息
     * @param params         参数
     * @param iAsyncListener 异步请求监听器
     */
    public void completionsAsync(@NotNull JSONArray messages, @NotNull JSONObject params, @NonNull IAsyncListener iAsyncListener) {
        try {
            this.init();

            params.put("messages", messages);
            if (!params.containsKey("model")) params.put("model", "deepseek-r1");
            if (!params.containsKey("max_tokens")) params.put("max_tokens", 2048);
            if (!params.containsKey("stream")) params.put("stream", false);
            if (!params.containsKey("temperature")) params.put("temperature", 1);
            if (!params.containsKey("top_p")) params.put("top_p", 1);

            Request request = buildRequest("/chat/completions", "POST", params);
            final Call call = this.client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    LogsUtil.error(e, TAG, "/completions 异步请求失败：%s", e.getMessage());
                    iAsyncListener.onResult(1001, "response error");
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        LogsUtil.warn(TAG, "/completions 异步请求失败:response.code=%s response.body=%s", response.code(), response.body() == null ? "" : response.body().string());
                        iAsyncListener.onResult(1001, "response error");
                        return;
                    }
                    if (response.body() == null) {
                        LogsUtil.warn(TAG, "/completions 异步请求无响应数据");
                        iAsyncListener.onResult(1002, "no response data");
                    }

                    // 不以流式返回结果
                    if (!JsonUtil.getBoolean(params, "stream", false)) {
                        iAsyncListener.onResult(0, response.body().string());
                        return;
                    }

                    // 以流式返回结果
                    try (BufferedSource source = response.body().source()) {
                        while (!source.exhausted()) {
                            String line = source.readUtf8Line();
                            if (line == null || line.isEmpty()) continue;
                            if (!line.startsWith("data: ")) continue;

                            String text = line.substring(6).trim();
                            if ("[DONE]".equalsIgnoreCase(text)) {
                                iAsyncListener.onResult(-1, "done");
                                break;
                            }
                            iAsyncListener.onResult(0, text);
                        }
                    } catch (IOException e) {
                        LogsUtil.warn(TAG, "/completions 异步请求响应结束");
                        iAsyncListener.onResult(-1, "response stop");
                    }
                }
            });
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "/completions 异步请求发生错误");
            iAsyncListener.onResult(1001, e.getMessage());
        }
    }
}
