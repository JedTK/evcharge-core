package com.evcharge.libsdk.openai;

import lombok.NonNull;
import okhttp3.*;

/**
 * 阿里云的百练API
 */
public class BailianAPI extends OpenAI {
    public BailianAPI(@NonNull String api_key) {
        super("https://dashscope.aliyuncs.com/compatible-mode/v1", api_key);
    }

    public BailianAPI(@NonNull String base_url, @NonNull String api_key) {
        super(base_url, api_key);
    }

    public BailianAPI(@NonNull String api_key, @NonNull OkHttpClient client) {
        super("https://dashscope.aliyuncs.com/compatible-mode/v1", api_key, client);
    }

    public BailianAPI(@NonNull String base_url, @NonNull String api_key, @NonNull OkHttpClient client) {
        super(base_url, api_key, client);
    }
}
