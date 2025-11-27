package com.evcharge.libsdk.openai;

import lombok.NonNull;
import okhttp3.*;

/**
 * Deepseek官方API
 * <a href="https://api-docs.deepseek.com/zh-cn/">Deepseek官方API</a>
 */
public class DeepSeekAPI extends OpenAI {
    public DeepSeekAPI(@NonNull String api_key) {
        super("https://api.deepseek.com", api_key);
    }

    public DeepSeekAPI(@NonNull String base_url, @NonNull String api_key) {
        super(base_url, api_key);
    }

    public DeepSeekAPI(@NonNull String api_key, @NonNull OkHttpClient client) {
        super("https://api.deepseek.com", api_key, client);
    }

    public DeepSeekAPI(@NonNull String base_url, @NonNull String api_key, @NonNull OkHttpClient client) {
        super(base_url, api_key, client);
    }
}
